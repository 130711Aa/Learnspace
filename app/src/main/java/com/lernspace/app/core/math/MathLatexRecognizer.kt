package com.lernspace.app.core.math

import android.graphics.Bitmap
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
    val source: String, // "offline_mlkit", "on_device_vision", "gemini_vision"
    val isSolved: Boolean = solution?.isSolved == true
)

/**
 * Hybrid Vector & Vision Math Recognition Engine for Learnspace.
 *
 * Architecture:
 * 1. Vector Strokes (Digital Ink 'zxx-Zsym-x-math'): Direct vector trajectory parsing (offline, instant).
 *    Evaluates multiple candidates and converts handwriting arrow misclassifications (\rightarrow, \to) to '='.
 * 2. On-Device Vision OCR: Fast offline bitmap OCR for printed/raster equations.
 * 3. Gemini Multimodal Vision AI: Cloud AI for complex 2D structures (matrices, large integrals).
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
            val success = suspendCancellableCoroutine<Boolean> { cont ->
                remote.download(model, conditions)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(true) }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Download failed", e)
                        if (cont.isActive) cont.resume(false)
                    }
                    .addOnCanceledListener { if (cont.isActive) cont.resume(false) }
            }
            if (success) isMathModelReady = true
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download offline math model", e)
            false
        }
    }

    /**
     * Recognizes handwriting vector strokes and/or bitmap as LaTeX math.
     */
    suspend fun recognizeMath(
        strokes: List<Stroke>,
        cropBitmap: Bitmap?,
        forceVision: Boolean = false
    ): MathRecognitionResult = withContext(Dispatchers.Default) {
        var fallbackResult: MathRecognitionResult? = null

        // ── 1. Vector Stroke Recognition (ML Kit 'zxx-Zsym-x-math' Model) ──
        if (!forceVision && strokes.isNotEmpty()) {
            val inkCandidates = recognizeOfflineInkMathCandidates(strokes)
            for (rawCandidate in inkCandidates) {
                val clean = cleanLatex(rawCandidate)
                if (clean.isNotBlank()) {
                    val solution = MathSolver.solve(clean) ?: MathSolver.solve(clean + " =")
                    val result = MathRecognitionResult(
                        latex = clean,
                        solution = solution,
                        source = "offline_mlkit"
                    )
                    if (solution != null && solution.isSolved) {
                        Log.d(TAG, "Vector Ink math solved: '$clean' -> ${solution.result}")
                        return@withContext result
                    }
                    if (fallbackResult == null) {
                        fallbackResult = result
                    }
                }
            }
        }

        // ── 2. On-Device Vision OCR (Fast offline text recognizer) ──
        if (cropBitmap != null) {
            val localVisionText = recognizeOnDeviceVision(cropBitmap)
            if (!localVisionText.isNullOrBlank()) {
                val clean = cleanLatex(localVisionText)
                if (clean.isNotBlank()) {
                    val solution = MathSolver.solve(clean) ?: MathSolver.solve(clean + " =")
                    val result = MathRecognitionResult(
                        latex = clean,
                        solution = solution,
                        source = "on_device_vision"
                    )
                    if (solution != null && solution.isSolved) {
                        Log.d(TAG, "Vision OCR math solved: '$clean' -> ${solution.result}")
                        return@withContext result
                    }
                    if (fallbackResult == null) {
                        fallbackResult = result
                    }
                }
            }
        }

        // Return solved or offline result if available and Gemini not configured
        if (fallbackResult != null && (fallbackResult.isSolved || !GeminiApiClient.isConfigured())) {
            return@withContext fallbackResult
        }

        // ── 3. Gemini Multimodal Vision AI (Cloud AI for complex 2D math) ──
        if (cropBitmap != null && GeminiApiClient.isConfigured()) {
            val visionLatex = recognizeVisionMath(cropBitmap)
            if (!visionLatex.isNullOrBlank()) {
                val clean = cleanLatex(visionLatex)
                val solution = MathSolver.solve(clean) ?: MathSolver.solve(clean + " =")
                Log.d(TAG, "Gemini Vision OCR succeeded: '$clean' -> ${solution?.result}")
                return@withContext MathRecognitionResult(
                    latex = clean,
                    solution = solution,
                    source = "gemini_vision"
                )
            }
        }

        fallbackResult ?: MathRecognitionResult(
            latex = "",
            solution = null,
            source = "none"
        )
    }

    /**
     * Performs offline ML Kit Digital Ink recognition and returns all candidates.
     */
    private suspend fun recognizeOfflineInkMathCandidates(strokes: List<Stroke>): List<String> {
        return try {
            val recognizer = getOrInitRecognizer() ?: return emptyList()
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
            result?.candidates?.mapNotNull { it.text } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Offline ink recognition failed", e)
            emptyList()
        }
    }

    /**
     * Instant on-device vision OCR using Google Play Services ML Kit.
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

    private suspend fun getOrInitRecognizer(): DigitalInkRecognizer? {
        if (mathRecognizer != null) return mathRecognizer
        val remote = RemoteModelManager.getInstance()

        val mathId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(MATH_MODEL_TAG) ?: return null
        val mathModel = DigitalInkRecognitionModel.builder(mathId).build()
        if (awaitTask(remote.isModelDownloaded(mathModel)) == true) {
            mathRecognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(mathModel).setMaxResultCount(5).build()
            )
            return mathRecognizer
        } else {
            downloadOfflineMathModel()
            return null
        }
    }

    /**
     * Calls Gemini Vision API with a strict Math OCR system prompt.
     */
    private suspend fun recognizeVisionMath(bitmap: Bitmap): String? {
        val sysPrompt = """
            Kamu adalah mesin konversi tulisan tangan matematika ke LaTeX (Math & LaTeX OCR Engine) tingkat tinggi.
            - Konversikan seluruh tulisan tangan angka, pecahan, eksponen, akar, simbol operasi, dan simbol sama dengan (=) yang ada pada gambar menjadi rumus LaTeX murni.
            - PENTING: Jangan pernah mengubah simbol sama dengan (=) menjadi tanda panah (->, \rightarrow, \to). Simbol sama dengan harus dituliskan persis sebagai '='.
            - HANYA keluarkan rumus dalam blok LaTeX ($$...$$ atau $...$) tanpa sapaan, tanpa kata pengantar, tanpa penjelasan langkah, dan tanpa komentar apapun.
        """.trimIndent()

        val userPrompt = "Ekstrak tulisan tangan matematika pada gambar ini menjadi rumus LaTeX murni tanpa teks pengantar:"

        val response = GeminiApiClient.generateTextFromImages(
            systemPrompt = sysPrompt,
            userPrompt = userPrompt,
            imageBitmaps = listOf(bitmap)
        )

        val resStr = response.getOrNull()
        Log.d(TAG, "recognizeVisionMath raw response: '$resStr'")
        return resStr
    }

    /**
     * Cleans raw LaTeX output to ensure it is valid, pure LaTeX.
     * Crucially converts any misrecognized arrow tokens (\rightarrow, \to, ->) to '='.
     */
    fun cleanLatex(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```latex")) text = text.removePrefix("```latex").trim()
        else if (text.startsWith("```tex")) text = text.removePrefix("```tex").trim()
        else if (text.startsWith("```")) text = text.removePrefix("```").trim()
        if (text.endsWith("```")) text = text.removeSuffix("```").trim()

        // Strip surrounding dollar signs if present
        if (text.startsWith("$$") && text.endsWith("$$") && text.length >= 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("$") && text.endsWith("$") && text.length >= 2) {
            text = text.substring(1, text.length - 1).trim()
        }

        if (text in listOf("\\varnothing", "\\emptyset", "\\phi", "{}", "")) {
            return ""
        }

        // Normalize OCR lines for fractions (e.g. "3\n-\n2" or "3\n2" -> "\frac{3}{2}")
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size == 2) {
            val top = lines[0]
            val bottom = lines[1]
            if (top.isNotEmpty() && bottom.isNotEmpty() && !top.contains("=") && !bottom.contains("=")) {
                text = "\\frac{$top}{$bottom}"
            }
        } else if (lines.size == 3) {
            val top = lines[0]
            val mid = lines[1]
            val bottom = lines[2]
            if (mid in listOf("-", "--", "—", "_", "__", "―", "=") && top.isNotEmpty() && bottom.isNotEmpty()) {
                text = "\\frac{$top}{$bottom}"
            }
        } else if (lines.size > 1) {
            text = text.replace("\n", " ").trim()
        }

        // Clean common OCR misrecognitions for math
        text = text.replace("–", "-")
            .replace("—", "-")
            .replace("−", "-")
            .replace("⁄", "/")

        // Normalize misrecognized arrows to '='
        // In handwritten arithmetic (e.g. "2 + 2 ="), ML Kit commonly misclassifies '=' as:
        // '\rightarrow', '\to', '\Rightarrow', '->', '-->', '=>', '→', '⇒', or trailing '>'
        if (!text.contains("\\lim")) {
            text = text.replace(Regex("""\s*(?:\\rightarrow|\\to|\\Rightarrow|\\implies|-->|->|==>|=>|→|⇒)\s*"""), " = ")
        }
        text = text.replace(Regex("""\s*>\s*$"""), " =")
        text = text.replace(Regex("""\s*(?:--|__|―|—)\s*$"""), " =")
        text = text.replace(Regex("""={2,}"""), "=")
        text = text.replace(Regex("""\s*=\s*"""), " = ")
        if (text.endsWith(" = ")) {
            text = text.trimEnd()
        }

        return text.trim()
    }

    /**
     * Formats LaTeX string for rendering in Canvas.
     */
    fun formatForCanvas(latex: String): String {
        val clean = cleanLatex(latex)
        val stripped = clean.removePrefix("$$").removeSuffix("$$").removePrefix("$").removeSuffix("$").trim()
        return "$$\n$stripped\n$$"
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
