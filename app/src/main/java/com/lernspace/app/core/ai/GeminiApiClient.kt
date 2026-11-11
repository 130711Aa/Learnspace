package com.lernspace.app.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight, zero-dependency Gemini REST client (BYOK) targeting Flash-Lite models
 * for fast, context-accurate flashcards, explanations, and schedule parsing.
 */
object GeminiApiClient {

    private const val PREFS_NAME = "lernspace_gemini_prefs"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_MODEL_ID = "gemini_model_id"
    private const val KEY_PREFER_ONLINE = "prefer_gemini_online"

    val availableModels = listOf(
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite (Terbaru)",
        "gemini-2.5-flash-lite" to "Gemini 2.5 Flash-Lite"
    )

    var apiKey by mutableStateOf("")
        private set
    var selectedModelId by mutableStateOf("gemini-3.5-flash-lite")
        private set
    var preferGeminiWhenOnline by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, "")?.trim() ?: ""
        selectedModelId = prefs.getString(KEY_MODEL_ID, "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
        preferGeminiWhenOnline = prefs.getBoolean(KEY_PREFER_ONLINE, true)
    }

    fun updateApiKey(context: Context, newKey: String) {
        val cleaned = newKey.trim()
        apiKey = cleaned
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_KEY, cleaned).apply()
    }

    fun updateModelId(context: Context, modelId: String) {
        selectedModelId = modelId
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODEL_ID, modelId).apply()
    }

    fun setPreferGemini(context: Context, prefer: Boolean) {
        preferGeminiWhenOnline = prefer
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PREFER_ONLINE, prefer).apply()
    }

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    suspend fun testConnection(): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API Key belum diisi."))
        }
        return generateText(
            systemPrompt = "Jawab singkat dalam 1 kalimat Bahasa Indonesia.",
            userPrompt = "Balas dengan: Koneksi Gemini Flash-Lite Learnspace siap!"
        )
    }

    suspend fun generateFlashcards(
        sourceText: String,
        topicTitle: String,
        maxCards: Int
    ): Result<List<GeneratedFlashcard>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API Key belum diatur."))
        }

        val trimmed = sourceText.trim().take(12_000)
        if (trimmed.length < 15) {
            return@withContext Result.failure(IllegalArgumentException("Teks materi terlalu pendek untuk dirangkum."))
        }

        val systemPrompt = """
            Kamu adalah asisten belajar mahasiswa yang membuat kartu Active Recall (Flashcards) dalam Bahasa Indonesia.
            Buat TEPAT $maxCards kartu tanya-jawab berkualitas tinggi HANYA berdasarkan fakta di materi yang diberikan.
            Jangan mencampur konsep yang berbeda dan jangan mengarang informasi di luar materi.
            Balas dalam JSON valid sesuai format:
            {"cards":[{"front":"Pertanyaan singkat dan jelas","back":"Jawaban padat dan akurat","hint":"Petunjuk singkat opsional"}]}
        """.trimIndent()

        val userPrompt = """
            Topik: $topicTitle
            Materi:
            $trimmed
        """.trimIndent()

        val responseResult = callGenerateContent(
            systemPrompt = systemPrompt,
            parts = JSONArray().put(JSONObject().put("text", userPrompt)),
            expectJson = true,
            temperature = 0.25
        )

        responseResult.fold(
            onSuccess = { text ->
                val cards = parseCardsJson(text)
                if (cards.isEmpty()) {
                    Result.failure(IllegalStateException("Gemini tidak mengembalikan kartu yang valid."))
                } else {
                    Result.success(cards.take(maxCards))
                }
            },
            onFailure = { err -> Result.failure(err) }
        )
    }

    suspend fun generateText(
        systemPrompt: String,
        userPrompt: String,
        imageBitmap: Bitmap? = null
    ): Result<String> {
        return generateTextFromImages(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBitmaps = if (imageBitmap != null) listOf(imageBitmap) else emptyList()
        )
    }

    suspend fun generateTextFromImages(
        systemPrompt: String,
        userPrompt: String,
        imageBitmaps: List<Bitmap>
    ): Result<String> = withContext(Dispatchers.IO) {
        val parts = JSONArray()
        for (bmp in imageBitmaps) {
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", b64)
                )
            )
        }
        parts.put(JSONObject().put("text", userPrompt))
        callGenerateContent(
            systemPrompt = systemPrompt,
            parts = parts,
            expectJson = false,
            temperature = 0.25
        )
    }

    private suspend fun callGenerateContent(
        systemPrompt: String,
        parts: JSONArray,
        expectJson: Boolean,
        temperature: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModelId:generateContent"
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            val genConfig = JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", 4096)
                if (expectJson) {
                    put("responseMimeType", "application/json")
                    put(
                        "responseSchema",
                        JSONObject().apply {
                            put("type", "OBJECT")
                            put(
                                "properties",
                                JSONObject().put(
                                    "cards",
                                    JSONObject().apply {
                                        put("type", "ARRAY")
                                        put(
                                            "items",
                                            JSONObject().apply {
                                                put("type", "OBJECT")
                                                put(
                                                    "properties",
                                                    JSONObject()
                                                        .put("front", JSONObject().put("type", "STRING"))
                                                        .put("back", JSONObject().put("type", "STRING"))
                                                        .put("hint", JSONObject().put("type", "STRING"))
                                                )
                                                put("required", JSONArray().put("front").put("back"))
                                            }
                                        )
                                    }
                                )
                            )
                            put("required", JSONArray().put("cards"))
                        }
                    )
                }
            }

            val payload = JSONObject().apply {
                put(
                    "system_instruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                )
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("parts", parts)
                    )
                )
                put("generationConfig", genConfig)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            Log.d("GeminiApiClient", "model=$selectedModelId code=$code len=${body.length}")

            if (code !in 200..299) {
                Log.e("GeminiApiClient", "HTTP $code body=${body.take(500)}")
                val apiMsg = try {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                } catch (_: Exception) {
                    null
                }
                val friendly = when (code) {
                    429 -> "Rate limit Gemini tercapai (HTTP 429)"
                    401, 403 -> "API Key Gemini tidak valid / ditolak (HTTP $code)"
                    else -> apiMsg ?: "HTTP $code"
                }
                return@withContext Result.failure(GeminiHttpException(code, friendly))
            }

            val root = JSONObject(body)
            val cand0 = root.optJSONArray("candidates")?.optJSONObject(0)
            val partsArr = cand0?.optJSONObject("content")?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    val part = partsArr.optJSONObject(i) ?: continue
                    if (part.optBoolean("thought", false)) continue
                    val t = part.optString("text")
                    if (t.isNotBlank()) textBuilder.append(t)
                }
            }
            val text = textBuilder.toString().trim()

            if (text.isBlank()) {
                val finishReason = cand0?.optString("finishReason").orEmpty()
                Log.e("GeminiApiClient", "Empty text. finishReason=$finishReason body=${body.take(400)}")
                val reasonSuffix = if (finishReason.isNotBlank()) " ($finishReason)" else ""
                Result.failure(IllegalStateException("Respons Gemini kosong$reasonSuffix."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Request error: ${e.message}", e)
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseCardsJson(raw: String): List<GeneratedFlashcard> {
        val cleaned = raw.replace("```json", "").replace("```", "").trim()
        val jsonCandidate = extractJsonPayload(cleaned) ?: cleaned
        val results = mutableListOf<GeneratedFlashcard>()
        try {
            val parsed = JSONTokener(jsonCandidate).nextValue()
            val arr = when (parsed) {
                is JSONObject -> parsed.optJSONArray("cards")
                    ?: parsed.optJSONArray("flashcards")
                    ?: parsed.optJSONArray("items")
                is JSONArray -> parsed
                else -> null
            } ?: return emptyList()

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val front = item.optString("front")
                    .ifBlank { item.optString("question") }
                    .trim()
                val back = item.optString("back")
                    .ifBlank { item.optString("answer") }
                    .trim()
                val hint = item.optString("hint", "").trim()
                if (front.isNotBlank() && back.isNotBlank()) {
                    results.add(GeneratedFlashcard(front = front, back = back, hint = hint))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "JSON parse failed: ${e.message} raw=${raw.take(300)}")
        }
        return results
    }

    private fun extractJsonPayload(text: String): String? {
        val objStart = text.indexOf('{')
        val arrStart = text.indexOf('[')
        val start = when {
            objStart >= 0 && arrStart >= 0 -> minOf(objStart, arrStart)
            objStart >= 0 -> objStart
            arrStart >= 0 -> arrStart
            else -> return null
        }
        val endChar = if (text[start] == '{') '}' else ']'
        val end = text.lastIndexOf(endChar)
        return if (end > start) text.substring(start, end + 1) else null
    }
}

class GeminiHttpException(val statusCode: Int, message: String) : Exception(message)
