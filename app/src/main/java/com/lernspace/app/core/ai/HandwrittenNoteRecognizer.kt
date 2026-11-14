package com.lernspace.app.core.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.LruCache
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.NoteUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

data class HandwritingRecognitionResult(
    val text: String,
    val sourceLabel: String,
    val modelDownloading: Boolean = false
)

/**
 * Pure AI-Powered Handwriting Recognizer for Learnspace.
 * Uses Gemini Multimodal Vision AI to accurately transcribe handwritten notes, formulas, and diagrams.
 */
object HandwrittenNoteRecognizer {

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

        if (!GeminiApiClient.isConfigured()) {
            return@withContext HandwritingRecognitionResult(
                text = "",
                sourceLabel = "Gemini API Key belum diatur di Pengaturan. Masukkan API Key gratis untuk membaca tulisan tangan dengan AI."
            )
        }

        val visionRes = recognizePagesWithGeminiVision(cacheKeyId, pages, "Catatan")
        visionRes.getOrElse {
            HandwritingRecognitionResult(
                text = "",
                sourceLabel = it.message ?: "Gagal membaca tulisan tangan via AI."
            )
        }
    }

    /**
     * Renders handwritten pages to clean bitmaps and uses Gemini Vision
     * to transcribe handwriting, Indonesian notes, formulas, and diagrams accurately.
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
                    Pertahankan istilah penting, definisi, poin-poin, hubungan sebab-akibat, serta rumus matematika/kimia jika ada.
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

    /**
     * Renders vector strokes into a high-contrast bitmap for AI OCR.
     */
    /**
     * Renders vector ink strokes to a high-contrast bitmap suitable for OCR and Gemini Multimodal Vision AI.
     * Uses both CanvasStrokeRenderer and a deterministic StrokeInput Path fallback to guarantee strokes are never lost
     * on software canvases.
     */
    fun renderStrokesToBitmap(
        strokes: List<Stroke>,
        renderer: CanvasStrokeRenderer? = null,
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

        val pad = 28f
        minX -= pad
        minY -= pad
        maxX += pad
        maxY += pad

        val worldW = (maxX - minX).coerceAtLeast(60f)
        val worldH = (maxY - minY).coerceAtLeast(60f)

        // Ensure ideal OCR scale so text has sufficient pixel height (~240px - 600px)
        val targetH = 320f
        val targetW = 800f
        val optimalScale = min(targetW / worldW, targetH / worldH)
        val scale = min(optimalScale, min(maxDim.toFloat() / worldW, maxDim.toFloat() / worldH)).coerceIn(0.6f, 4.0f)

        val bmpW = (worldW * scale).roundToInt().coerceIn(64, maxDim)
        val bmpH = (worldH * scale).roundToInt().coerceIn(64, maxDim)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor ?: Color.WHITE)

        val matrix = Matrix().apply {
            postTranslate(-minX, -minY)
            postScale(scale, scale)
        }

        // 1. Attempt hardware/software CanvasStrokeRenderer
        if (renderer != null) {
            for (stroke in strokes) {
                try {
                    renderer.draw(canvas, stroke, matrix)
                } catch (_: Exception) {}
            }
        }

        // 2. Deterministic high-contrast Path rendering from StrokeInput
        // Guarantees ink is 100% visible to Vision AI / OCR even on offscreen software canvases
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val dotPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = Path()

        for (stroke in strokes) {
            val isHighlighter = stroke.brush.family == androidx.ink.brush.StockBrushes.highlighter()
            val baseColor = stroke.brush.colorIntArgb
            val r = Color.red(baseColor)
            val g = Color.green(baseColor)
            val b = Color.blue(baseColor)
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            val drawColor = if (isHighlighter) {
                Color.argb(100, r, g, b)
            } else if (lum > 0.65f) {
                // Invert light ink (e.g. white stylus on dark paper) to dark ink for crisp contrast
                Color.rgb(15, 23, 42)
            } else {
                baseColor
            }

            val strokeWidth = if (isHighlighter) {
                (stroke.brush.size * scale * 1.5f).coerceIn(12f, 48f)
            } else {
                (stroke.brush.size * scale).coerceIn(3.5f, 22f)
            }

            strokePaint.color = drawColor
            strokePaint.strokeWidth = strokeWidth
            dotPaint.color = drawColor

            val inputs = stroke.inputs
            val n = inputs.size
            if (n == 1) {
                inputs.populate(0, temp)
                val px = (temp.x - minX) * scale
                val py = (temp.y - minY) * scale
                canvas.drawCircle(px, py, strokeWidth * 0.5f, dotPaint)
            } else if (n > 1) {
                path.reset()
                for (i in 0 until n) {
                    inputs.populate(i, temp)
                    val px = (temp.x - minX) * scale
                    val py = (temp.y - minY) * scale
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, strokePaint)
            }
        }

        return bitmap
    }
}
