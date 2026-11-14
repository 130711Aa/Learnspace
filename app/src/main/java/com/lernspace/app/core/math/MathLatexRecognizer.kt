package com.lernspace.app.core.math

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.ink.strokes.Stroke
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lernspace.app.core.ai.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Recognition & Solution bundle for mathematical handwriting.
 */
data class MathRecognitionResult(
    val latex: String,
    val solution: MathSolution?,
    val source: String, // "on_device_vision", "offline_mlkit", "gemini_vision"
    val isSolved: Boolean = solution?.isSolved == true
)

/**
 * Dedicated Math & LaTeX OCR Recognition Engine for Learnspace.
 *
 * Multi-Tier Pipeline:
 * 1. Tier 1: On-Device Vision OCR (Built-in ML Kit Text Recognition) - Instant (~15ms), 100% Offline, no download needed!
 * 2. Tier 2: ML Kit Digital Ink Recognition ('zxx-Zsym-x-math' or 'en-US'/'id') - Vector stroke trajectory parsing.
 * 3. Tier 3: Gemini Vision OCR - Cloud LLM for complex 2D structures (matrices, large integrals, systems of equations).
 */
object MathLatexRecognizer {

    private const val TAG = "MathLatexRecognizer"
    private const val MATH_MODEL_TAG = "zxx-Zsym-x-math"

    private var mathRecognizer: DigitalInkRecognizer? = null
    private var latinRecognizer: DigitalInkRecognizer? = null
    private var isMathModelReady = false

    /**
     * Checks if offline ML Kit Math model is downloaded and ready.
     */
    suspend fun isOfflineMathModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remote = RemoteModelManager.getInstance()
            val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(MATH_MODEL_TAG) ?: return@withContext false
            val model = DigitalInkRecognitionModel.builder(id).build()
            awaitTask(remote.isModelDownloaded(model)) == true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check math model status", e)
            false
        }
    }

    /**
     * Triggers background download for the offline ML Kit Math model.
     */
    suspend fun downloadOfflineMathModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remote = RemoteModelManager.getInstance()
            val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(MATH_MODEL_TAG) ?: return@withContext false
            val model = DigitalInkRecognitionModel.builder(id).build()
            if (awaitTask(remote.isModelDownloaded(model)) == true) {
                isMathModelReady = true
                return@withContext true
            }
            val conditions = DownloadConditions.Builder().build()
            val success = awaitTask(remote.download(model, conditions)) != null
            if (success) isMathModelReady = true
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download offline math model", e)
            false
        }
    }

    /**
     * Recognizes handwriting strokes as LaTeX math.
     */
    suspend fun recognizeMath(
        strokes: List<Stroke>,
        cropBitmap: Bitmap?,
        forceVision: Boolean = false
    ): MathRecognitionResult = withContext(Dispatchers.Default) {
        // ── Tier 1: On-Device Vision OCR (Built-in, Instant 15ms, 100% Offline) ──
        if (cropBitmap != null) {
            val localVisionText = recognizeOnDeviceVision(cropBitmap)
            if (!localVisionText.isNullOrBlank()) {
                val clean = cleanLatex(localVisionText)
                if (clean.isNotBlank()) {
                    val solution = MathSolver.solve(clean + " =") ?: MathSolver.solve(clean)
                    Log.d(TAG, "Tier 1 On-Device Vision OCR succeeded: '$clean' -> ${solution?.result}")
                    return@withContext MathRecognitionResult(
                        latex = clean,
                        solution = solution,
                        source = "on_device_vision"
                    )
                }
            }
        }

        // ── Tier 2: Offline ML Kit Digital Ink Recognition ──
        if (!forceVision && strokes.isNotEmpty()) {
            val offlineInkText = recognizeOfflineInkMath(strokes)
            if (!offlineInkText.isNullOrBlank()) {
                val clean = cleanLatex(offlineInkText)
                val solution = MathSolver.solve(clean + " =") ?: MathSolver.solve(clean)
                Log.d(TAG, "Tier 2 Digital Ink Recognition succeeded: '$clean' -> ${solution?.result}")
                return@withContext MathRecognitionResult(
                    latex = clean,
                    solution = solution,
                    source = "offline_mlkit"
                )
            }
        }

        // ── Tier 3: Gemini Vision OCR (Cloud API for complex 2D math) ──
        if (cropBitmap != null && GeminiApiClient.isConfigured()) {
            val visionLatex = recognizeVisionMath(cropBitmap)
            if (!visionLatex.isNullOrBlank()) {
                val clean = cleanLatex(visionLatex)
                val solution = MathSolver.solve(clean + " =") ?: MathSolver.solve(clean)
                Log.d(TAG, "Tier 3 Gemini Vision OCR succeeded: '$clean' -> ${solution?.result}")
                return@withContext MathRecognitionResult(
                    latex = clean,
                    solution = solution,
                    source = "gemini_vision"
                )
            }
        }

        // Fallback: empty result
        MathRecognitionResult(
            latex = "",
            solution = null,
            source = "none"
        )
    }

    /**
     * Instant on-device vision OCR using built-in Google Play Services ML Kit.
     * Takes ~15ms and recognizes handwriting and printed text completely offline.
     */
    private suspend fun recognizeOnDeviceVision(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = awaitTask(recognizer.process(image))
            val text = visionText?.text?.trim()
            if (!text.isNullOrBlank()) text else null
        } catch (e: Exception) {
            Log.w(TAG, "On-device vision OCR failed", e)
            null
        }
    }

    /**
     * Performs offline ML Kit Digital Ink recognition.
     */
    private suspend fun recognizeOfflineInkMath(strokes: List<Stroke>): String? {
        return try {
            val recognizer = getOrInitRecognizer() ?: return null
            val inkBuilder = Ink.builder()

            for (stroke in strokes) {
                val strokeBuilder = Ink.Stroke.builder()
                val inputs = stroke.inputs
                for (i in 0 until inputs.size) {
                    val input = inputs[i]
                    strokeBuilder.addPoint(
                        Ink.Point.create(
                            input.x,
                            input.y,
                            input.elapsedTimeMillis
                        )
                    )
                }
                inkBuilder.addStroke(strokeBuilder.build())
            }

            val ink = inkBuilder.build()
            val result = awaitTask(recognizer.recognize(ink))
            result?.candidates?.firstOrNull()?.text
        } catch (e: Exception) {
            Log.w(TAG, "Offline ink recognition failed", e)
            null
        }
    }

    private suspend fun getOrInitRecognizer(): DigitalInkRecognizer? {
        // Try Math model first
        if (mathRecognizer != null) return mathRecognizer
        val remote = RemoteModelManager.getInstance()

        val mathId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(MATH_MODEL_TAG)
        if (mathId != null) {
            val mathModel = DigitalInkRecognitionModel.builder(mathId).build()
            if (awaitTask(remote.isModelDownloaded(mathModel)) == true) {
                mathRecognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(mathModel).setMaxResultCount(3).build()
                )
                return mathRecognizer
            } else {
                // Auto-trigger background download for math model
                downloadOfflineMathModel()
            }
        }

        // Fallback to standard Latin model ('en-US' or 'id')
        if (latinRecognizer != null) return latinRecognizer
        val candidateTags = listOf("en-US", "id", "en")
        for (tag in candidateTags) {
            val latId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: continue
            val latModel = DigitalInkRecognitionModel.builder(latId).build()
            if (awaitTask(remote.isModelDownloaded(latModel)) == true) {
                latinRecognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(latModel).setMaxResultCount(3).build()
                )
                return latinRecognizer
            }
        }

        return null
    }

    /**
     * Calls Gemini Vision API with a strict Math OCR system prompt.
     */
    private suspend fun recognizeVisionMath(bitmap: Bitmap): String? {
        val sysPrompt = """
            Kamu adalah mesin konversi tulisan tangan matematika ke LaTeX (Math & LaTeX OCR Engine) tingkat tinggi.
            - Konversikan seluruh tulisan tangan, simbol pecahan, eksponen, akar, integral, limit, turunan, matriks, dan notasi matematika/sains yang ada pada gambar menjadi kode LaTeX murni.
            - HANYA keluarkan rumus dalam blok LaTeX ($$...$$ atau $...$) tanpa sapaan, tanpa kata pengantar, tanpa penjelasan langkah, dan tanpa komentar apapun.
            - Jika terdiri dari beberapa persamaan, susun menggunakan format align atau baris baru yang rapi.
        """.trimIndent()

        val userPrompt = "Ekstrak tulisan tangan matematika pada gambar ini menjadi rumus LaTeX murni tanpa teks pengantar:"

        val response = GeminiApiClient.generateTextFromImages(
            systemPrompt = sysPrompt,
            userPrompt = userPrompt,
            imageBitmaps = listOf(bitmap)
        )

        return response.getOrNull()
    }

    /**
     * Cleans raw LaTeX output to ensure it is valid, pure LaTeX.
     */
    fun cleanLatex(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```latex")) text = text.removePrefix("```latex").trim()
        else if (text.startsWith("```tex")) text = text.removePrefix("```tex").trim()
        else if (text.startsWith("```")) text = text.removePrefix("```").trim()
        if (text.endsWith("```")) text = text.removeSuffix("```").trim()
        return text
    }

    /**
     * Formats LaTeX string for rendering in Canvas.
     */
    fun formatForCanvas(latex: String): String {
        val clean = cleanLatex(latex)
        return if (clean.startsWith("$") && clean.endsWith("$")) {
            clean
        } else {
            "$$\n$clean\n$$"
        }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T? = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }.addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }.addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }
}
