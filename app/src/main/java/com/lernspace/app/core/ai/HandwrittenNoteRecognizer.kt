package com.lernspace.app.core.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.util.LruCache
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.NoteUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class HandwritingRecognitionResult(
    val text: String,
    val sourceLabel: String,
    val modelDownloading: Boolean = false
)

/**
 * Converts handwritten Ink strokes across note pages into clean study text
 * using ML Kit Digital Ink (offline line-by-line) or Gemini Vision (when online & configured).
 */
object HandwrittenNoteRecognizer {

    private var activeLangTag: String? = null
    private var inkModel: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null

    private val offlineTextCache = LruCache<String, String>(24)
    private val visionTextCache = LruCache<String, String>(24)

    fun countHandwrittenStrokes(pages: List<NotePageData>): Int {
        var total = 0
        for (page in pages) {
            for (s in page.strokes) {
                if (s.brush.family != StockBrushes.highlighter() && s.inputs.size > 0) {
                    total++
                }
            }
        }
        return total
    }

    private suspend fun getReadyOfflineRecognizer(): Pair<DigitalInkRecognizer, String>? {
        val remote = RemoteModelManager.getInstance()
        val candidateTags = listOf("id", "en-US", "en")

        // Prefer whichever model is already downloaded locally for zero-wait offline recognition
        for (tag in candidateTags) {
            val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: continue
            val model = DigitalInkRecognitionModel.builder(id).build()
            val downloaded = awaitTask(remote.isModelDownloaded(model)) == true
            if (downloaded) {
                if (activeLangTag != tag || recognizer == null) {
                    recognizer?.close()
                    activeLangTag = tag
                    inkModel = model
                    recognizer = DigitalInkRecognition.getClient(
                        DigitalInkRecognizerOptions.builder(model).setMaxResultCount(3).build()
                    )
                }
                return recognizer!! to tag
            }
        }
        return null
    }

    suspend fun triggerOfflineModelDownload(): Boolean = withContext(Dispatchers.IO) {
        val remote = RemoteModelManager.getInstance()
        val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag("id")
            ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            ?: return@withContext false
        val model = DigitalInkRecognitionModel.builder(id).build()
        val already = awaitTask(remote.isModelDownloaded(model)) == true
        if (already) return@withContext true
        awaitTask(remote.download(model, DownloadConditions.Builder().build())) != null
    }

    suspend fun recognizeNoteOffline(note: NoteUiModel): HandwritingRecognitionResult =
        recognizePagesOffline(cacheKeyId = note.id, pages = note.pages)

    suspend fun recognizePagesOffline(
        cacheKeyId: String,
        pages: List<NotePageData>
    ): HandwritingRecognitionResult = withContext(Dispatchers.IO) {
        val strokeCount = countHandwrittenStrokes(pages)
        if (strokeCount == 0) {
            return@withContext HandwritingRecognitionResult("", "Tidak ada coretan tulisan tangan")
        }

        val cacheKey = "$cacheKeyId#$strokeCount"
        offlineTextCache.get(cacheKey)?.let { cached ->
            return@withContext HandwritingRecognitionResult(
                text = cached,
                sourceLabel = "Tulisan tangan (ML Kit Offline - cache)"
            )
        }

        val ready = getReadyOfflineRecognizer()
        if (ready == null) {
            val startedOk = triggerOfflineModelDownload()
            val retryReady = if (startedOk) getReadyOfflineRecognizer() else null
            if (retryReady == null) {
                return@withContext HandwritingRecognitionResult(
                    text = "",
                    sourceLabel = "Model tulisan tangan offline belum siap (sedang diunduh ~20 MB)",
                    modelDownloading = true
                )
            }
            return@withContext recognizeWithClient(cacheKey, pages, retryReady.first, retryReady.second)
        }

        recognizeWithClient(cacheKey, pages, ready.first, ready.second)
    }

    private suspend fun recognizeWithClient(
        cacheKey: String,
        pages: List<NotePageData>,
        client: DigitalInkRecognizer,
        langTag: String
    ): HandwritingRecognitionResult {
        val nonEmptyPages = pages.filter { page ->
            page.strokes.any { it.brush.family != StockBrushes.highlighter() && it.inputs.size > 0 }
        }
        val multiPage = nonEmptyPages.size > 1
        val pageTexts = mutableListOf<String>()

        for (page in nonEmptyPages) {
            val writingStrokes = page.strokes.filter {
                it.brush.family != StockBrushes.highlighter() && it.inputs.size > 0
            }
            if (writingStrokes.isEmpty()) continue

            val lines = groupStrokesIntoLines(writingStrokes)
            val recognizedLines = mutableListOf<String>()
            var prevContext = ""

            for (lineStrokes in lines) {
                val ink = buildMlKitInk(lineStrokes)
                val ctx = RecognitionContext.builder()
                    .setPreContext(prevContext.takeLast(80))
                    .build()
                val res = awaitTask(client.recognize(ink, ctx))
                val best = res?.candidates?.firstOrNull()?.text?.trim()
                if (!best.isNullOrBlank()) {
                    recognizedLines.add(best)
                    prevContext = if (prevContext.isEmpty()) best else "$prevContext $best"
                }
            }

            if (recognizedLines.isNotEmpty()) {
                val joined = recognizedLines.joinToString("\n")
                if (multiPage) {
                    pageTexts.add("[Halaman ${page.pageIndex + 1}]\n$joined")
                } else {
                    pageTexts.add(joined)
                }
            }
        }

        val finalStr = pageTexts.joinToString("\n\n").trim()
        if (finalStr.isNotBlank()) {
            offlineTextCache.put(cacheKey, finalStr)
        }
        return HandwritingRecognitionResult(
            text = finalStr,
            sourceLabel = "Tulisan tangan dikonversi otomatis (ML Kit '$langTag' Offline)"
        )
    }

    /**
     * Renders handwritten pages to clean bitmaps and uses Gemini Flash-Lite Vision
     * to transcribe messy handwriting, Indonesian notes, formulas, and diagrams accurately.
     */
    suspend fun recognizePagesWithGeminiVision(
        cacheKeyId: String,
        pages: List<NotePageData>,
        topicTitle: String
    ): Result<HandwritingRecognitionResult> = withContext(Dispatchers.IO) {
        if (!GeminiApiClient.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API Key belum diatur di Settings."))
        }
        val strokeCount = countHandwrittenStrokes(pages)
        if (strokeCount == 0) {
            return@withContext Result.success(
                HandwritingRecognitionResult("", "Tidak ada coretan tulisan tangan")
            )
        }

        val cacheKey = "$cacheKeyId#$strokeCount#${GeminiApiClient.selectedModelId}"
        visionTextCache.get(cacheKey)?.let { cached ->
            return@withContext Result.success(
                HandwritingRecognitionResult(cached, "Tulisan tangan (${GeminiApiClient.selectedModelId} Vision)")
            )
        }

        val bitmaps = mutableListOf<Bitmap>()
        try {
            val renderer = CanvasStrokeRenderer.create(true)
            val nonEmptyPages = pages.filter { p ->
                p.strokes.any { it.brush.family != StockBrushes.highlighter() && it.inputs.size > 0 }
            }.take(4) // Keep payload fast & token-efficient

            for (page in nonEmptyPages) {
                val bmp = renderStrokesToBitmap(page.strokes, renderer, maxDim = 1280)
                if (bmp != null) bitmaps.add(bmp)
            }
            if (bitmaps.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Gagal merender coretan halaman."))
            }

            val res = GeminiApiClient.generateTextFromImages(
                systemPrompt = """
                    Kamu adalah asisten transkripsi catatan kuliah.
                    Baca gambar tulisan tangan siswa/mahasiswa ini dengan teliti lalu ubah menjadi teks materi belajar yang rapi dalam Bahasa Indonesia.
                    Pertahankan istilah penting, definisi, poin-poin, hubungan sebab-akibat/panah, serta rumus matematika/kimia jika ada.
                    Jangan mengarang hal yang tidak tertulis di gambar. Balas HANYA teks hasil transkripsinya.
                """.trimIndent(),
                userPrompt = "Topik catatan: $topicTitle\nTolong transkripsikan seluruh isi tulisan tangan pada ${bitmaps.size} halaman gambar berikut menjadi teks terstruktur.",
                imageBitmaps = bitmaps
            )

            res.map { transcribed ->
                val cleaned = transcribed.trim()
                if (cleaned.isNotBlank()) {
                    visionTextCache.put(cacheKey, cleaned)
                }
                HandwritingRecognitionResult(
                    text = cleaned,
                    sourceLabel = "Tulisan tangan dibaca via ${GeminiApiClient.selectedModelId} Vision"
                )
            }
        } finally {
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    fun renderStrokesToBitmap(
        strokes: List<Stroke>,
        renderer: CanvasStrokeRenderer,
        maxDim: Int = 1280,
        backgroundColor: Int? = null
    ): Bitmap? {
        if (strokes.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        val temp = StrokeInput()
        for (stroke in strokes) {
            val inputs = stroke.inputs
            for (i in 0 until inputs.size) {
                inputs.populate(i, temp)
                if (temp.x < minX) minX = temp.x
                if (temp.y < minY) minY = temp.y
                if (temp.x > maxX) maxX = temp.x
                if (temp.y > maxY) maxY = temp.y
            }
        }
        if (!minX.isFinite() || (maxX <= minX && maxY <= minY)) return null

        val pad = 24f
        minX -= pad
        minY -= pad
        maxX += pad
        maxY += pad

        val worldW = (maxX - minX).coerceAtLeast(60f)
        val worldH = (maxY - minY).coerceAtLeast(60f)

        // Ensure ideal OCR scale so text has sufficient pixel height (~220px - 400px)
        val targetH = 280f
        val targetW = 720f
        val optimalScale = min(targetW / worldW, targetH / worldH)
        val scale = min(optimalScale, min(maxDim.toFloat() / worldW, maxDim.toFloat() / worldH)).coerceIn(0.5f, 4.0f)

        val bmpW = (worldW * scale).roundToInt().coerceIn(64, maxDim)
        val bmpH = (worldH * scale).roundToInt().coerceIn(64, maxDim)

        val avgLum = strokes.map { st ->
            val c = st.brush.colorIntArgb
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        }.average()

        val isLightInk = avgLum > 0.60
        // If ink is light (e.g. white stylus on dark paper), render on black then invert to pure black-on-white
        val drawBg = if (isLightInk) Color.BLACK else Color.WHITE

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(drawBg)

        val matrix = Matrix().apply {
            postTranslate(-minX, -minY)
            postScale(scale, scale)
        }

        // Draw strokes using the transformation matrix directly (DO NOT transform canvas again)
        for (stroke in strokes) {
            renderer.draw(canvas, stroke, matrix)
        }

        if (isLightInk) {
            val inverted = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val invertPaint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            Canvas(inverted).drawBitmap(bitmap, 0f, 0f, invertPaint)
            bitmap.recycle()
            return inverted
        }

        return bitmap
    }

    private fun groupStrokesIntoLines(strokes: List<Stroke>): List<List<Stroke>> {
        data class BoundedStroke(val stroke: Stroke, val bounds: RectF)

        val items = strokes.mapNotNull { st ->
            strokeBounds(st)?.let { BoundedStroke(st, it) }
        }
        if (items.isEmpty()) return emptyList()

        val heights = items.map { it.bounds.height() }.sorted()
        val medianH = heights[heights.size / 2].coerceIn(22f, 120f)
        val lineThresholdY = max(32f, medianH * 0.78f)

        // Sort roughly top-to-bottom first, cluster by vertical band, then left-to-right per line
        val sortedByTop = items.sortedBy { it.bounds.centerY() }
        val lineGroups = mutableListOf<MutableList<BoundedStroke>>()
        val lineCenterYs = mutableListOf<Float>()

        for (item in sortedByTop) {
            val cy = item.bounds.centerY()
            var matchedIdx = -1
            var bestDist = Float.MAX_VALUE
            for (idx in lineGroups.indices) {
                val dist = abs(cy - lineCenterYs[idx])
                if (dist <= lineThresholdY && dist < bestDist) {
                    bestDist = dist
                    matchedIdx = idx
                }
            }
            if (matchedIdx != -1) {
                val grp = lineGroups[matchedIdx]
                grp.add(item)
                lineCenterYs[matchedIdx] = grp.map { it.bounds.centerY() }.average().toFloat()
            } else {
                lineGroups.add(mutableListOf(item))
                lineCenterYs.add(cy)
            }
        }

        return lineGroups
            .sortedBy { grp -> grp.minOf { it.bounds.top } }
            .map { grp -> grp.sortedBy { it.bounds.left }.map { it.stroke } }
    }

    private fun strokeBounds(stroke: Stroke): RectF? {
        val inputs = stroke.inputs
        val count = inputs.size
        if (count == 0) return null
        val temp = StrokeInput()
        inputs.populate(0, temp)
        var minX = temp.x
        var maxX = temp.x
        var minY = temp.y
        var maxY = temp.y
        for (i in 1 until count) {
            inputs.populate(i, temp)
            if (temp.x < minX) minX = temp.x
            if (temp.x > maxX) maxX = temp.x
            if (temp.y < minY) minY = temp.y
            if (temp.y > maxY) maxY = temp.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

    private fun buildMlKitInk(strokes: List<Stroke>): Ink {
        val builder = Ink.builder()
        val temp = StrokeInput()
        var timeOffsetMs = 0L

        for (stroke in strokes) {
            val inputs = stroke.inputs
            val count = inputs.size
            if (count == 0) continue

            val strokeBuilder = Ink.Stroke.builder()
            var lastT = 0L
            for (i in 0 until count) {
                inputs.populate(i, temp)
                val t = timeOffsetMs + temp.elapsedTimeMillis
                lastT = max(lastT, temp.elapsedTimeMillis)
                strokeBuilder.addPoint(Ink.Point.create(temp.x, temp.y, t))
            }
            builder.addStroke(strokeBuilder.build())
            timeOffsetMs += lastT + 20L
        }
        return builder.build()
    }

    private suspend fun <T> awaitTask(task: Task<T>): T? =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }.addOnFailureListener { e ->
                Log.w("HandwritingNoteOCR", "Task failed: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }.addOnCanceledListener {
                if (cont.isActive) cont.resume(null)
            }
        }
}
