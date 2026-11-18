package com.lernspace.app.ui.workspace.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Matrix
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.asAndroidBitmap
import com.lernspace.app.ui.workspace.NotePaneState
import com.lernspace.app.ui.workspace.model.CanvasConstants
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.TextBoxAlign
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import kotlin.math.roundToInt

/**
 * Captures high-fidelity, software-backed ARGB_8888 bitmap snapshots of the currently active note page,
 * combining base PDF/paper, inserted images, handwritten vector ink strokes, and text boxes.
 */
object PageCaptureHelper {

    fun capturePageBitmap(
        context: Context,
        state: NotePaneState,
        targetWidth: Int = 1200
    ): Bitmap? {
        return try {
            if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) {
                capturePdfPage(context, state, targetWidth)
            } else {
                captureInkingPage(context, state, targetWidth)
            }
        } catch (e: Exception) {
            android.util.Log.e("PageCaptureHelper", "Gagal mengambil snapshot halaman: ${e.message}", e)
            null
        }
    }

    private fun capturePdfPage(
        context: Context,
        state: NotePaneState,
        targetWidth: Int
    ): Bitmap? {
        val pdfUriStr = state.note.pdfUri ?: return null
        val pdfUri = Uri.parse(pdfUriStr)
        val pageIdx = state.currentPageIndex

        // 1. Try rendering high-res page directly from PdfNoteHelper
        var baseResult = PdfNoteHelper.renderPdfPageWithBounds(
            context = context,
            uri = pdfUri,
            pageIndex = pageIdx,
            targetWidth = targetWidth
        )

        val baseBmp: Bitmap = if (baseResult != null) {
            val src = baseResult.bitmap
            if (src.config == Bitmap.Config.HARDWARE) {
                src.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                src.copy(Bitmap.Config.ARGB_8888, true)
            }
        } else {
            // Fallback to active state pdfPageBitmap
            val stateBmp = state.pdfPageBitmap?.asAndroidBitmap() ?: return null
            if (stateBmp.config == Bitmap.Config.HARDWARE) {
                stateBmp.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                stateBmp.copy(Bitmap.Config.ARGB_8888, true)
            }
        }

        val worldW = if (baseResult != null) baseResult.worldWidth.coerceAtLeast(1f) else state.pdfPageWorldWidth.coerceAtLeast(1f)
        val worldH = if (baseResult != null) baseResult.worldHeight.coerceAtLeast(1f) else state.pdfPageWorldHeight.coerceAtLeast(1f)

        val scaleX = baseBmp.width.toFloat() / worldW
        val scaleY = baseBmp.height.toFloat() / worldH

        val canvas = Canvas(baseBmp)
        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
        }

        // Draw Images
        for (img in state.imageElements) {
            var tempSafeImg: Bitmap? = null
            try {
                val raw = img.bitmap.asAndroidBitmap()
                val safeImg = if (raw.config == Bitmap.Config.HARDWARE) {
                    val copied = raw.copy(Bitmap.Config.ARGB_8888, false)
                    tempSafeImg = copied
                    copied
                } else raw
                val dstRect = RectF(
                    img.x * scaleX,
                    img.y * scaleY,
                    (img.x + img.width) * scaleX,
                    (img.y + img.height) * scaleY
                )
                canvas.drawBitmap(safeImg, null, dstRect, null)
            } catch (_: Exception) {
            } finally {
                tempSafeImg?.recycle()
            }
        }

        // Draw Strokes
        val strokeRenderer = state.canvasStrokeRenderer
        if (strokeRenderer != null) {
            // Highlighters first
            for (st in state.strokes) {
                if (st.brush.family == androidx.ink.brush.StockBrushes.highlighter()) {
                    strokeRenderer.draw(canvas, st, matrix)
                }
            }
            // Pens on top
            for (st in state.strokes) {
                if (st.brush.family != androidx.ink.brush.StockBrushes.highlighter()) {
                    strokeRenderer.draw(canvas, st, matrix)
                }
            }
        }

        // Draw Text Boxes
        drawTextBoxesOnCanvas(canvas, state.textBoxes, scaleX, scaleY, isDarkPaper = state.isDarkPaper)

        // Rescale if larger than 1280px on max dimension to optimize Gemini latency
        val maxSide = maxOf(baseBmp.width, baseBmp.height)
        return if (maxSide > 1280) {
            val factor = 1280f / maxSide
            val scaled = Bitmap.createScaledBitmap(
                baseBmp,
                (baseBmp.width * factor).roundToInt().coerceAtLeast(1),
                (baseBmp.height * factor).roundToInt().coerceAtLeast(1),
                true
            )
            baseBmp.recycle()
            scaled
        } else {
            baseBmp
        }
    }

    private fun captureInkingPage(
        context: Context,
        state: NotePaneState,
        targetWidth: Int
    ): Bitmap? {
        var maxX = CanvasConstants.PAGE_WIDTH
        var maxY = CanvasConstants.PAGE_HEIGHT

        for (st in state.strokes) {
            val inputs = st.inputs
            for (i in 0 until inputs.size) {
                val pt = inputs.get(i)
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
        }
        for (img in state.imageElements) {
            if (img.x + img.width > maxX) maxX = img.x + img.width
            if (img.y + img.height > maxY) maxY = img.y + img.height
        }
        for (tb in state.textBoxes) {
            if (tb.x + tb.width > maxX) maxX = tb.x + tb.width
            if (tb.y + tb.height > maxY) maxY = tb.y + tb.height
        }

        val worldW = (maxX + 60f).coerceAtLeast(CanvasConstants.PAGE_WIDTH)
        val worldH = (maxY + 60f).coerceAtLeast(CanvasConstants.PAGE_HEIGHT)

        val scale = targetWidth.toFloat() / worldW
        val targetH = (worldH * scale).roundToInt().coerceIn(400, 2400)

        val bmp = Bitmap.createBitmap(targetWidth, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background paper color
        val bgColor = if (state.isDarkPaper) android.graphics.Color.rgb(30, 30, 38) else android.graphics.Color.WHITE
        canvas.drawColor(bgColor)

        // Draw paper lines scaled
        canvas.save()
        canvas.scale(scale, scale)
        PdfNoteHelper.drawPaperLinesOnCanvas(canvas, state.paperStyle, worldW.toInt(), worldH.toInt())
        canvas.restore()

        // Draw Images
        for (img in state.imageElements) {
            var tempSafeImg: Bitmap? = null
            try {
                val raw = img.bitmap.asAndroidBitmap()
                val safeImg = if (raw.config == Bitmap.Config.HARDWARE) {
                    val copied = raw.copy(Bitmap.Config.ARGB_8888, false)
                    tempSafeImg = copied
                    copied
                } else raw
                val dstRect = RectF(
                    img.x * scale,
                    img.y * scale,
                    (img.x + img.width) * scale,
                    (img.y + img.height) * scale
                )
                canvas.drawBitmap(safeImg, null, dstRect, null)
            } catch (_: Exception) {
            } finally {
                tempSafeImg?.recycle()
            }
        }

        // Draw Strokes
        val strokeRenderer = state.canvasStrokeRenderer
        if (strokeRenderer != null) {
            val matrix = Matrix().apply {
                postScale(scale, scale)
            }
            // Highlighters first
            for (st in state.strokes) {
                if (st.brush.family == androidx.ink.brush.StockBrushes.highlighter()) {
                    strokeRenderer.draw(canvas, st, matrix)
                }
            }
            // Pens on top
            for (st in state.strokes) {
                if (st.brush.family != androidx.ink.brush.StockBrushes.highlighter()) {
                    strokeRenderer.draw(canvas, st, matrix)
                }
            }
        }

        // Draw Text Boxes
        drawTextBoxesOnCanvas(canvas, state.textBoxes, scale, scale, isDarkPaper = state.isDarkPaper)

        return bmp
    }

    private fun drawTextBoxesOnCanvas(
        canvas: Canvas,
        textBoxes: List<com.lernspace.app.ui.workspace.model.TextBoxElement>,
        scaleX: Float,
        scaleY: Float,
        isDarkPaper: Boolean
    ) {
        for (box in textBoxes) {
            if (box.text.isNotBlank()) {
                val renderedText = com.lernspace.app.ui.workspace.components.LatexMathFormatter.toPlainReadableText(box.text)
                val styleFlag = when {
                    box.isBold && box.isItalic -> android.graphics.Typeface.BOLD_ITALIC
                    box.isBold -> android.graphics.Typeface.BOLD
                    box.isItalic -> android.graphics.Typeface.ITALIC
                    else -> android.graphics.Typeface.NORMAL
                }
                val textPaint = TextPaint().apply {
                    color = box.colorArgb ?: if (isDarkPaper) android.graphics.Color.parseColor("#F8FAFC") else android.graphics.Color.parseColor("#0F172A")
                    textSize = (box.fontSize * scaleX).coerceIn(10f, 64f)
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, styleFlag)
                    isAntiAlias = true
                }
                val align = when (box.textAlign) {
                    TextBoxAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                    TextBoxAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                    TextBoxAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
                }
                canvas.save()
                canvas.translate(box.x * scaleX, box.y * scaleY)
                val layoutWidth = (box.width * scaleX).toInt().coerceAtLeast(60)
                try {
                    val staticLayout = StaticLayout.Builder.obtain(
                        renderedText, 0, renderedText.length, textPaint, layoutWidth
                    ).setAlignment(align).build()
                    staticLayout.draw(canvas)
                } catch (_: Exception) {}
                canvas.restore()
            }
        }
    }

    /**
     * Extracts text snippets from the active page's text boxes to augment AI multimodal context.
     */
    fun extractPageText(state: NotePaneState): String {
        val pageNumber = state.currentPageIndex + 1
        val total = state.totalPages
        val textList = state.textBoxes.mapNotNull { it.text.trim().takeIf { t -> t.isNotEmpty() } }

        return buildString {
            append("Catatan: \"${state.noteTitle}\" (${state.subject})\n")
            append("Halaman: $pageNumber dari $total (${state.canvasType.title})\n")
            if (textList.isNotEmpty()) {
                append("Teks terpasang pada halaman:\n")
                for ((idx, t) in textList.withIndex()) {
                    append("[Teks ${idx + 1}]: $t\n")
                }
            }
        }
    }
}
