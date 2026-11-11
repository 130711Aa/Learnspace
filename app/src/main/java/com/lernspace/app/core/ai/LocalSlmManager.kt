package com.lernspace.app.core.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GeneratedFlashcard(
    val front: String,
    val back: String,
    val hint: String = ""
)

/**
 * Central On-Device SLM Manager powered by llama.cpp (via Llamatik GGUF).
 * Implements Lazy-Load & Auto-Unload so RAM usage is 0 MB during normal note-taking
 * and stays safe on 8 GB RAM tablets.
 */
object LocalSlmManager {

    private const val PREFS_NAME = "lernspace_slm_prefs"
    private const val KEY_ACTIVE_MODEL_FILE = "active_model_file"
    private const val KEY_AUTO_UNLOAD = "auto_unload_enabled"
    private const val AUTO_UNLOAD_DELAY_MS = 30_000L

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val inferenceMutex = Mutex()
    private var autoUnloadJob: Job? = null
    private var downloadJob: Job? = null
    @Volatile
    private var cancelDownloadRequested = false

    // Observable UI State
    var activeModelFileName by mutableStateOf<String?>(null)
        private set
    var isModelLoadedInRam by mutableStateOf(false)
        private set
    var isGenerating by mutableStateOf(false)
        private set
    var isDownloading by mutableStateOf(false)
        private set
    var downloadingModelId by mutableStateOf<String?>(null)
        private set
    var downloadProgress by mutableFloatStateOf(0f)
        private set
    var downloadStatusText by mutableStateOf("")
        private set
    var statusMessage by mutableStateOf("")
        private set
    var autoUnloadEnabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        autoUnloadEnabled = prefs.getBoolean(KEY_AUTO_UNLOAD, true)
        val savedName = prefs.getString(KEY_ACTIVE_MODEL_FILE, null)
        if (savedName != null && getModelFile(context, savedName).exists()) {
            activeModelFileName = savedName
        } else {
            // Auto-detect any existing .gguf file in models dir
            val existing = getModelsDir(context).listFiles()?.firstOrNull {
                it.isFile && it.name.endsWith(".gguf", ignoreCase = true) && it.length() > 10_000_000L
            }
            activeModelFileName = existing?.name
        }
    }

    fun setAutoUnload(context: Context, enabled: Boolean) {
        autoUnloadEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_UNLOAD, enabled).apply()
        if (enabled && isModelLoadedInRam && !isGenerating) {
            scheduleAutoUnload()
        } else if (!enabled) {
            autoUnloadJob?.cancel()
        }
    }

    fun getModelsDir(context: Context): File {
        return File(context.filesDir, "slm_models").apply { if (!exists()) mkdirs() }
    }

    fun getModelFile(context: Context, fileName: String): File {
        return File(getModelsDir(context), fileName)
    }

    fun isModelDownloaded(context: Context, spec: SlmModelSpec): Boolean {
        val file = getModelFile(context, spec.fileName)
        return file.exists() && file.length() > (spec.expectedBytes * 0.85).toLong()
    }

    fun getDownloadedCustomModels(context: Context): List<File> {
        val builtInNames = SlmModelSpec.entries.map { it.fileName }.toSet()
        return getModelsDir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".gguf", ignoreCase = true) && it.name !in builtInNames }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun hasReadyModel(context: Context): Boolean {
        val name = activeModelFileName ?: return false
        val file = getModelFile(context, name)
        return file.exists() && file.length() > 10_000_000L
    }

    fun selectActiveModel(context: Context, fileName: String) {
        if (activeModelFileName != fileName && isModelLoadedInRam) {
            unloadModelNow()
        }
        activeModelFileName = fileName
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE_MODEL_FILE, fileName).apply()
    }

    fun deleteModel(context: Context, fileName: String) {
        if (activeModelFileName == fileName) {
            unloadModelNow()
            activeModelFileName = null
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_ACTIVE_MODEL_FILE).apply()
        }
        val file = getModelFile(context, fileName)
        if (file.exists()) file.delete()
        val partFile = File(getModelsDir(context), "$fileName.part")
        if (partFile.exists()) partFile.delete()
        // Re-select another downloaded model if any exists
        if (activeModelFileName == null) {
            init(context)
        }
    }

    fun cancelDownload() {
        cancelDownloadRequested = true
        downloadJob?.cancel()
        isDownloading = false
        downloadingModelId = null
        downloadProgress = 0f
        downloadStatusText = "Unduhan dibatalkan"
    }

    fun startModelDownload(context: Context, spec: SlmModelSpec) {
        if (isDownloading) return
        cancelDownloadRequested = false
        isDownloading = true
        downloadingModelId = spec.id
        downloadProgress = 0f
        downloadStatusText = "Menghubungkan ke server model..."

        val appCtx = context.applicationContext
        downloadJob = scope.launch {
            val dir = getModelsDir(appCtx)
            val partFile = File(dir, "${spec.fileName}.part")
            val finalFile = File(dir, spec.fileName)

            try {
                var currentUrl = spec.downloadUrl
                var connection: HttpURLConnection? = null
                var redirects = 0
                while (redirects < 6) {
                    val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 20_000
                        readTimeout = 30_000
                        setRequestProperty("User-Agent", "Lernspace-Android/1.1")
                    }
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val next = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (next.isNullOrBlank()) break
                        currentUrl = if (next.startsWith("http")) next else URL(URL(currentUrl), next).toString()
                        redirects++
                    } else {
                        connection = conn
                        break
                    }
                }

                if (connection == null) {
                    throw IllegalStateException("Gagal terhubung ke URL model.")
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    connection.disconnect()
                    throw IllegalStateException("Server mengembalikan kode HTTP $responseCode")
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: spec.expectedBytes
                connection.inputStream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastUiUpdate = 0L
                        while (true) {
                            if (cancelDownloadRequested) {
                                throw InterruptedException("Cancelled")
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate > 200L) {
                                lastUiUpdate = now
                                val pct = (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                                val dlMb = downloaded / (1024f * 1024f)
                                val totMb = totalBytes / (1024f * 1024f)
                                withContext(Dispatchers.Main) {
                                    downloadProgress = pct
                                    downloadStatusText = String.format("%.1f MB / %.1f MB", dlMb, totMb)
                                }
                            }
                        }
                        output.flush()
                    }
                }
                connection.disconnect()

                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    partFile.copyTo(finalFile, overwrite = true)
                    partFile.delete()
                }

                withContext(Dispatchers.Main) {
                    isDownloading = false
                    downloadingModelId = null
                    downloadProgress = 1f
                    downloadStatusText = "Model siap digunakan!"
                    selectActiveModel(appCtx, spec.fileName)
                }
            } catch (e: Exception) {
                if (partFile.exists()) partFile.delete()
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    downloadingModelId = null
                    downloadProgress = 0f
                    downloadStatusText = if (cancelDownloadRequested) {
                        "Unduhan dibatalkan"
                    } else {
                        "Gagal mengunduh: ${e.message ?: "Periksa koneksi internet"}"
                    }
                }
            }
        }
    }

    suspend fun importLocalGguf(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            var fileName = "custom-model-${System.currentTimeMillis()}.gguf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        val raw = cursor.getString(idx)
                        if (!raw.isNullOrBlank()) fileName = raw
                    }
                }
            }
            if (!fileName.endsWith(".gguf", ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("File harus berformat .gguf"))
            }

            val dest = getModelFile(context, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Tidak dapat membaca file"))

            withContext(Dispatchers.Main) {
                selectActiveModel(context.applicationContext, fileName)
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun unloadModelNow() {
        autoUnloadJob?.cancel()
        scope.launch {
            inferenceMutex.withLock {
                if (isModelLoadedInRam) {
                    try {
                        LlamaBridge.shutdown()
                    } catch (_: Exception) {}
                    withContext(Dispatchers.Main) {
                        isModelLoadedInRam = false
                        statusMessage = "Memori RAM dibebaskan"
                    }
                }
            }
        }
    }

    private fun scheduleAutoUnload() {
        autoUnloadJob?.cancel()
        if (!autoUnloadEnabled) return
        autoUnloadJob = scope.launch {
            delay(AUTO_UNLOAD_DELAY_MS)
            if (!isGenerating && isModelLoadedInRam) {
                inferenceMutex.withLock {
                    if (!isGenerating && isModelLoadedInRam) {
                        try {
                            LlamaBridge.shutdown()
                        } catch (_: Exception) {}
                        withContext(Dispatchers.Main) {
                            isModelLoadedInRam = false
                            statusMessage = "Model otomatis di-unload (hemat RAM)"
                        }
                    }
                }
            }
        }
    }

    private suspend fun ensureModelLoadedLocked(context: Context): Boolean {
        if (isModelLoadedInRam) return true
        val fileName = activeModelFileName ?: return false
        val modelFile = getModelFile(context, fileName)
        if (!modelFile.exists()) return false

        withContext(Dispatchers.Main) {
            statusMessage = "Memuat model ke RAM..."
        }
        try {
            System.loadLibrary("c++_shared")
        } catch (_: Throwable) {}
        val ok = LlamaBridge.initGenerateModel(modelFile.absolutePath)
        if (ok) {
            LlamaBridge.updateGenerateParams(
                0.25f,  // temperature (focused, deterministic)
                420,    // maxTokens (tight cap for mobile CPU speed)
                0.9f,   // topP
                40,     // topK
                1.08f   // repeatPenalty
            )
            withContext(Dispatchers.Main) {
                isModelLoadedInRam = true
                statusMessage = "Model aktif di RAM"
            }
        }
        return ok
    }

    fun cancelGeneration() {
        try {
            LlamaBridge.nativeCancelGenerate()
        } catch (_: Exception) {}
    }

    private fun isChatMlModel(): Boolean {
        val name = activeModelFileName?.lowercase() ?: return true
        return name.contains("qwen") || name.contains("smollm") || name.contains("deepseek")
    }

    private fun buildPromptForActiveModel(
        systemPrompt: String,
        userPrompt: String,
        assistantPrefill: String = ""
    ): String {
        return if (isChatMlModel()) {
            buildString {
                append("<|im_start|>system\n")
                append(systemPrompt)
                append("<|im_end|>\n<|im_start|>user\n")
                append(userPrompt)
                append("<|im_end|>\n<|im_start|>assistant\n")
                append(assistantPrefill)
            }
        } else {
            buildString {
                append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n")
                append(systemPrompt)
                append("<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n")
                append(userPrompt)
                append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
                append(assistantPrefill)
            }
        }
    }

    /**
     * Generates structured Flashcards from source study material using the on-device SLM.
     * Uses native ChatML streaming with JSON prefill & early-stop as soon as JSON closes.
     */
    suspend fun generateFlashcardsFromText(
        context: Context,
        sourceText: String,
        topicTitle: String,
        maxCards: Int = 6
    ): Result<List<GeneratedFlashcard>> = withContext(Dispatchers.IO) {
        val trimmed = sourceText.trim().take(2000)
        if (trimmed.length < 15) {
            return@withContext Result.failure(IllegalArgumentException("Teks materi terlalu pendek untuk dirangkum menjadi flashcard."))
        }

        inferenceMutex.withLock {
            autoUnloadJob?.cancel()
            withContext(Dispatchers.Main) { isGenerating = true }
            try {
                val loaded = ensureModelLoadedLocked(context.applicationContext)
                if (!loaded) {
                    return@withContext Result.failure(IllegalStateException("Model SLM belum diunduh atau gagal dimuat."))
                }

                // Generous token budget per card so the last card never gets truncated mid-JSON;
                // brace-depth tracking cancels generation immediately once the root '}' closes.
                val tokenBudget = (maxCards * 120).coerceIn(360, 1280)
                LlamaBridge.updateGenerateParams(0.25f, tokenBudget, 0.9f, 40, 1.08f)

                withContext(Dispatchers.Main) {
                    statusMessage = "SLM membaca materi..."
                }

                val systemPrompt = """
                    Buat tepat $maxCards flashcard tanya-jawab singkat dalam Bahasa Indonesia (tiap jawaban maksimal 1 kalimat ringkas).
                    Balas HANYA JSON kompak tanpa spasi indentasi atau baris baru:
                    {"cards":[{"front":"Pertanyaan?","back":"Jawaban singkat."}]}
                """.trimIndent()

                val userPrompt = """
                    Topik: $topicTitle
                    Materi:
                    $trimmed
                """.trimIndent()

                val fullPrompt = buildPromptForActiveModel(systemPrompt, userPrompt, "{\"cards\":[")
                val outputBuffer = StringBuilder("{\"cards\":[")
                var braceDepth = 1
                var inString = false
                var escaped = false
                var completedEarly = false
                var streamError: String? = null
                var lastUiUpdate = 0L
                val doneLatch = java.util.concurrent.CountDownLatch(1)

                Log.d("LocalSlm", "Starting generateStream. promptLen=${fullPrompt.length}, budget=$tokenBudget")

                LlamaBridge.generateStream(
                    fullPrompt,
                    object : GenStream {
                        override fun onDelta(text: String) {
                            if (completedEarly) return
                            outputBuffer.append(text)

                            if (outputBuffer.contains("<|im_end|>") || outputBuffer.contains("<|eot_id|>")) {
                                completedEarly = true
                                try { LlamaBridge.nativeCancelGenerate() } catch (_: Exception) {}
                                doneLatch.countDown()
                                return
                            }

                            for (ch in text) {
                                if (escaped) {
                                    escaped = false
                                    continue
                                }
                                if (ch == '\\' && inString) {
                                    escaped = true
                                    continue
                                }
                                if (ch == '"') {
                                    inString = !inString
                                    continue
                                }
                                if (!inString) {
                                    if (ch == '{') braceDepth++
                                    else if (ch == '}') {
                                        braceDepth--
                                        if (braceDepth <= 0) {
                                            completedEarly = true
                                            try { LlamaBridge.nativeCancelGenerate() } catch (_: Exception) {}
                                            doneLatch.countDown()
                                            break
                                        }
                                    }
                                }
                            }

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate > 300L) {
                                lastUiUpdate = now
                                val cardHits = Regex("\"front\"").findAll(outputBuffer).count()
                                scope.launch(Dispatchers.Main) {
                                    statusMessage = if (cardHits > 0) {
                                        "Menyusun kartu... ($cardHits kartu)"
                                    } else {
                                        "SLM sedang menulis (${outputBuffer.length} kar)..."
                                    }
                                }
                            }
                        }

                        override fun onComplete() {
                            Log.d("LocalSlm", "onComplete called. len=${outputBuffer.length}")
                            doneLatch.countDown()
                        }

                        override fun onError(message: String) {
                            Log.e("LocalSlm", "onError called: $message")
                            streamError = message
                            doneLatch.countDown()
                        }
                    }
                )

                doneLatch.await(90, java.util.concurrent.TimeUnit.SECONDS)

                val rawClean = outputBuffer.toString()
                    .replace("<|im_end|>", "")
                    .replace("<|eot_id|>", "")

                Log.d("LocalSlm", "rawClean=${rawClean.take(500)}")

                if (streamError != null && rawClean.length <= 15) {
                    return@withContext Result.failure(IllegalStateException("Error SLM: $streamError"))
                }

                val parsed = parseFlashcardsFromModelOutput(rawClean)
                if (parsed.isEmpty()) {
                    val preview = rawClean.removePrefix("{\"cards\":[").trim().take(120)
                    Result.failure(
                        IllegalStateException(
                            if (preview.isBlank()) "SLM tidak mengeluarkan teks (${streamError ?: "output kosong"})."
                            else "Format output SLM belum terbaca: $preview"
                        )
                    )
                } else {
                    Result.success(parsed.take(maxCards))
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    statusMessage = "Selesai"
                }
                scheduleAutoUnload()
            }
        }
    }

    /**
     * Quick single-turn inference for testing the SLM or short explanations.
     */
    suspend fun generateQuickResponse(
        context: Context,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            autoUnloadJob?.cancel()
            withContext(Dispatchers.Main) { isGenerating = true }
            try {
                val loaded = ensureModelLoadedLocked(context.applicationContext)
                if (!loaded) {
                    return@withContext Result.failure(IllegalStateException("Model SLM belum siap."))
                }
                LlamaBridge.updateGenerateParams(0.35f, 256, 0.9f, 40, 1.1f)
                val fullPrompt = buildPromptForActiveModel(systemPrompt, userPrompt)
                val sb = StringBuilder()
                LlamaBridge.generateStream(
                    fullPrompt,
                    object : GenStream {
                        override fun onDelta(text: String) {
                            sb.append(text)
                            if (sb.contains("<|im_end|>") || sb.contains("<|eot_id|>")) {
                                try { LlamaBridge.nativeCancelGenerate() } catch (_: Exception) {}
                            }
                        }
                        override fun onComplete() {}
                        override fun onError(message: String) {}
                    }
                )
                val response = sb.toString()
                    .substringBefore("<|im_end|>")
                    .substringBefore("<|eot_id|>")
                    .trim()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                withContext(Dispatchers.Main) { isGenerating = false }
                scheduleAutoUnload()
            }
        }
    }

    /**
     * Streaming explanation / study Q&A (ideal for Lasso Explain & In-Note Assistant).
     */
    fun streamStudyResponse(
        context: Context,
        systemPrompt: String,
        studyContext: String,
        userPrompt: String
    ): Flow<String> = callbackFlow {
        val job = scope.launch {
            inferenceMutex.withLock {
                autoUnloadJob?.cancel()
                withContext(Dispatchers.Main) { isGenerating = true }
                try {
                    val loaded = ensureModelLoadedLocked(context.applicationContext)
                    if (!loaded) {
                        close(IllegalStateException("Model SLM belum diunduh."))
                        return@withLock
                    }

                    LlamaBridge.updateGenerateParams(0.35f, 380, 0.9f, 40, 1.1f)
                    val combinedUser = if (studyContext.isNotBlank()) {
                        "Materi:\n${studyContext.take(2000)}\n\nPertanyaan: $userPrompt"
                    } else {
                        userPrompt
                    }
                    val fullPrompt = buildPromptForActiveModel(systemPrompt, combinedUser)

                    LlamaBridge.generateStream(
                        fullPrompt,
                        object : GenStream {
                            override fun onDelta(text: String) {
                                if (text.contains("<|im_end|>") || text.contains("<|eot_id|>")) {
                                    val clean = text.substringBefore("<|im_end|>").substringBefore("<|eot_id|>")
                                    if (clean.isNotEmpty()) trySend(clean)
                                    try { LlamaBridge.nativeCancelGenerate() } catch (_: Exception) {}
                                    close()
                                } else {
                                    trySend(text)
                                }
                            }

                            override fun onComplete() {
                                close()
                            }

                            override fun onError(message: String) {
                                close(IllegalStateException(message))
                            }
                        }
                    )
                } finally {
                    withContext(Dispatchers.Main) { isGenerating = false }
                    scheduleAutoUnload()
                }
            }
        }
        awaitClose {
            try {
                LlamaBridge.nativeCancelGenerate()
            } catch (_: Exception) {}
            job.cancel()
        }
    }

    private fun parseFlashcardsFromModelOutput(raw: String): List<GeneratedFlashcard> {
        val cleaned = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx != -1 && endIdx > startIdx) {
            val jsonSlice = cleaned.substring(startIdx, endIdx + 1)
            try {
                val obj = JSONObject(jsonSlice)
                val arr = obj.optJSONArray("cards")
                if (arr != null) {
                    val list = parseJsonArray(arr)
                    if (list.isNotEmpty()) return list
                }
            } catch (_: Exception) {}
        }

        // Fallback: array directly
        val arrStart = cleaned.indexOf('[')
        val arrEnd = cleaned.lastIndexOf(']')
        if (arrStart != -1 && arrEnd > arrStart) {
            try {
                val arr = JSONArray(cleaned.substring(arrStart, arrEnd + 1))
                val list = parseJsonArray(arr)
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }

        // Fallback: salvage any complete {"front":...,"back":...} objects if generation stopped mid-array
        val salvaged = mutableListOf<GeneratedFlashcard>()
        val objRegex = Regex("""\{[^{}]*"(?:front|question)"\s*:\s*"[^"]*"[^{}]*\}""")
        objRegex.findAll(cleaned).forEach { match ->
            try {
                val item = JSONObject(match.value)
                val front = item.optString("front").ifBlank { item.optString("question") }.trim()
                val back = item.optString("back").ifBlank { item.optString("answer") }.trim()
                val hint = item.optString("hint", "").trim()
                if (front.isNotBlank() && back.isNotBlank()) {
                    salvaged.add(GeneratedFlashcard(front = front, back = back, hint = hint))
                }
            } catch (_: Exception) {}
        }
        return salvaged
    }

    private fun parseJsonArray(arr: JSONArray): List<GeneratedFlashcard> {
        val results = mutableListOf<GeneratedFlashcard>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val front = item.optString("front").ifBlank { item.optString("question") }.trim()
            val back = item.optString("back").ifBlank { item.optString("answer") }.trim()
            val hint = item.optString("hint", "").trim()
            if (front.isNotBlank() && back.isNotBlank()) {
                results.add(GeneratedFlashcard(front = front, back = back, hint = hint))
            }
        }
        return results
    }
}
