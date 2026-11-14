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
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
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
    val source: String, // "offline_mlkit" or "gemini_vision"
    val isSolved: Boolean = solution?.isSolved == true
)

/**
 * Dedicated Math & LaTeX OCR Recognition Engine for Learnspace.
 *
 * Pipeline:
 * 1. Offline Mode: ML Kit Digital Ink Recognition with 'zxx-Zsym-x-math' (symbol & formula model).
 * 2. Vision OCR Mode: Gemini Vision with specialized Math & LaTeX OCR prompt for 2D structures (matrices, integrals, fractions).
 * 3. Automatic Math Solver: Pipes output through MathSolver to calculate results for equations or arithmetic expressions.
 */
object MathLatexRecognizer {

    private const val TAG = "MathLatexRecognizer"
    private const val MATH_MODEL_TAG = "zxx-Zsym-x-math"

    private var mathRecognizer: DigitalInkRecognizer? = null
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
     * Tries offline ML Kit Math first, or uses Gemini Vision OCR when requested or online.
     */
    suspend fun recognizeMath(
        strokes: List<Stroke>,
        cropBitmap: Bitmap?,
        forceVision: Boolean = false
    ): MathRecognitionResult = withContext(Dispatchers.Default) {
        // 1. Try offline ML Kit Math if available and not forced to vision
        if (!forceVision && strokes.isNotEmpty() && isOfflineMathModelDownloaded()) {
            val offlineLatex = recognizeOfflineInkMath(strokes)
            if (!offlineLatex.isNullOrBlank()) {
                val clean = cleanLatex(offlineLatex)
                val solution = MathSolver.solve(clean)
                return@withContext MathRecognitionResult(
                    latex = clean,
                    solution = solution,
                    source = "offline_mlkit"
                )
            }
        }

        // 2. Vision LaTeX OCR (Gemini Vision with Math OCR prompt)
        if (cropBitmap != null && GeminiApiClient.isConfigured()) {
            val visionLatex = recognizeVisionMath(cropBitmap)
            if (!visionLatex.isNullOrBlank()) {
                val clean = cleanLatex(visionLatex)
                val solution = MathSolver.solve(clean)
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
     * Performs offline ML Kit Digital Ink recognition with the math model.
     */
    private suspend fun recognizeOfflineInkMath(strokes: List<Stroke>): String? {
        return try {
            val recognizer = getOrInitMathRecognizer() ?: return null
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

    private suspend fun getOrInitMathRecognizer(): DigitalInkRecognizer? {
        if (mathRecognizer != null) return mathRecognizer
        val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(MATH_MODEL_TAG) ?: return null
        val model = DigitalInkRecognitionModel.builder(id).build()
        mathRecognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).setMaxResultCount(3).build()
        )
        return mathRecognizer
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

