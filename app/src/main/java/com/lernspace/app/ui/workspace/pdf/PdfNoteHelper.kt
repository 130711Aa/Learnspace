package com.lernspace.app.ui.workspace.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.lernspace.app.ui.workspace.model.CanvasConstants
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle
import java.io.File
import java.io.FileOutputStream

import kotlin.math.roundToInt

data class PdfPageRenderResult(
    val bitmap: Bitmap,
    val worldWidth: Float,
    val worldHeight: Float
)

object PdfNoteHelper {

    private val pdfRendererLock = Any()

    private fun openPdfDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                val file = File(uri.path!!)
                if (file.exists()) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    null
                }
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Document.pdf"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        } catch (_: Exception) {}
        return name
    }

    fun getPdfPageCount(context: Context, uri: Uri): Int {
        synchronized(pdfRendererLock) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            return try {
                pfd = openPdfDescriptor(context, uri)
                if (pfd != null) {
                    renderer = PdfRenderer(pfd)
                    renderer.pageCount
                } else 1
            } catch (e: Exception) {
                1
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }
    }

    fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = CanvasConstants.PAGE_WIDTH.toInt()
    ): Bitmap? {
        return renderPdfPageWithBounds(context, uri, pageIndex, targetWidth)?.bitmap
    }

    /**
     * Renders a full PDF page at [targetWidth] pixels while returning its canonical
     * world dimensions anchored to [CanvasConstants.PAGE_WIDTH] so strokes remain 1:1 aligned.
     */
    fun renderPdfPageWithBounds(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 2400
    ): PdfPageRenderResult? {
        synchronized(pdfRendererLock) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null
            return try {
                pfd = openPdfDescriptor(context, uri) ?: return null
                renderer = PdfRenderer(pfd)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                page = renderer.openPage(pageIndex)

                val pageWidth = page.width.toFloat().coerceAtLeast(1f)
                val pageHeight = page.height.toFloat().coerceAtLeast(1f)
                val aspect = pageHeight / pageWidth

                val bmpWidth = targetWidth.coerceAtLeast(1)
                val bmpHeight = (bmpWidth * aspect).roundToInt().coerceAtLeast(1)

                val worldWidth = CanvasConstants.PAGE_WIDTH
                val worldHeight = worldWidth * aspect

                val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                PdfPageRenderResult(
                    bitmap = bitmap,
                    worldWidth = worldWidth,
                    worldHeight = worldHeight
                )
            } catch (e: Exception) {
                null
            } finally {
                try { page?.close() } catch (_: Exception) {}
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Renders a specific world-space rectangle of a PDF page directly from PDF vectors
     * at the exact requested pixel resolution for razor-sharp text & diagrams at any zoom level.
     */
    fun renderPdfRegion(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        worldLeft: Float,
        worldTop: Float,
        worldRight: Float,
        worldBottom: Float,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        synchronized(pdfRendererLock) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null
            return try {
                pfd = openPdfDescriptor(context, uri) ?: return null
                renderer = PdfRenderer(pfd)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                page = renderer.openPage(pageIndex)

                val pageWidth = page.width.toFloat().coerceAtLeast(1f)
                val pageHeight = page.height.toFloat().coerceAtLeast(1f)
                val worldWidth = CanvasConstants.PAGE_WIDTH
                val worldHeight = worldWidth * (pageHeight / pageWidth)

                val regionW = (worldRight - worldLeft).coerceAtLeast(1f)
                val regionH = (worldBottom - worldTop).coerceAtLeast(1f)
                val bmpW = targetWidth.coerceAtLeast(1)
                val bmpH = targetHeight.coerceAtLeast(1)

                val scaleX = bmpW.toFloat() / regionW
                val scaleY = bmpH.toFloat() / regionH

                val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                val matrix = Matrix().apply {
                    postScale(worldWidth / pageWidth, worldHeight / pageHeight)
                    postTranslate(-worldLeft, -worldTop)
                    postScale(scaleX, scaleY)
                }

                val destClip = Rect(0, 0, bmpW, bmpH)
                page.render(bitmap, destClip, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (e: Exception) {
                null
            } finally {
                try { page?.close() } catch (_: Exception) {}
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Export the multi-page note or PDF annotation into a clean standard PDF document.
     * Guaranteed 100% pixel-perfect alignment between viewport inking and exported PDF.
     */
    fun exportNoteToPdf(
        context: Context,
        note: NoteUiModel,
        strokeRenderer: CanvasStrokeRenderer
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val uri = note.pdfUri?.let { Uri.parse(it) }
            val totalPages = if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) note.pdfTotalPages else note.pages.size

            for (pageIdx in 0 until totalPages) {
                var exportWidth = CanvasConstants.PAGE_WIDTH.toInt()
                var exportHeight = CanvasConstants.PAGE_HEIGHT.toInt()

                var pdfResult: PdfPageRenderResult? = null
                if (note.canvasType == NoteCanvasType.PDF_DOCUMENT && uri != null) {
                    pdfResult = renderPdfPageWithBounds(
                        context = context,
                        uri = uri,
                        pageIndex = pageIdx,
                        targetWidth = (CanvasConstants.PAGE_WIDTH * 2f).roundToInt()
                    )
                    if (pdfResult != null) {
                        exportWidth = pdfResult.worldWidth.roundToInt()
                        exportHeight = pdfResult.worldHeight.roundToInt()
                    }
                } else {
                    // For PAGED_NOTE or INFINITE_SCRATCHPAD:
                    // Check if strokes or elements exceed default page dimensions so nothing is ever clipped
                    if (pageIdx < note.pages.size) {
                        val pageData = note.pages[pageIdx]
                        var maxX = CanvasConstants.PAGE_WIDTH
                        var maxY = CanvasConstants.PAGE_HEIGHT
                        for (stroke in pageData.strokes) {
                            val inputs = stroke.inputs
                            for (i in 0 until inputs.size) {
                                val pt = inputs.get(i)
                                if (pt.x > maxX) maxX = pt.x
                                if (pt.y > maxY) maxY = pt.y
                            }
                        }
                        for (img in pageData.imageElements) {
                            if (img.x + img.width > maxX) maxX = img.x + img.width
                            if (img.y + img.height > maxY) maxY = img.y + img.height
                        }
                        for (tb in pageData.textBoxes) {
                            if (tb.x + tb.width > maxX) maxX = tb.x + tb.width
                            if (tb.y + tb.height > maxY) maxY = tb.y + tb.height
                        }
                        exportWidth = (maxX + 60f).toInt().coerceAtLeast(CanvasConstants.PAGE_WIDTH.toInt())
                        exportHeight = (maxY + 60f).toInt().coerceAtLeast(CanvasConstants.PAGE_HEIGHT.toInt())
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(exportWidth, exportHeight, pageIdx + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Background
                if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) {
                    if (pdfResult != null) {
                        val dstRect = RectF(0f, 0f, exportWidth.toFloat(), exportHeight.toFloat())
                        val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                        canvas.drawBitmap(pdfResult.bitmap, null, dstRect, bitmapPaint)
                    } else {
                        canvas.drawColor(android.graphics.Color.WHITE)
                    }
                } else {
                    canvas.drawColor(android.graphics.Color.WHITE)
                    drawPaperLinesOnCanvas(canvas, note.paperStyle, exportWidth, exportHeight)
                }

                // 2. Draw Images for this page
                if (pageIdx < note.pages.size) {
                    val pageData = note.pages[pageIdx]
                    for (img in pageData.imageElements) {
                        try {
                            val androidBmp = img.bitmap.asAndroidBitmap()
                            val dstRect = RectF(img.x, img.y, img.x + img.width, img.y + img.height)
                            canvas.drawBitmap(androidBmp, null, dstRect, null)
                        } catch (_: Exception) {}
                    }
                }

                // 3. Draw Vector Strokes for this page
                if (pageIdx < note.pages.size) {
                    val pageData = note.pages[pageIdx]
                    val identityMatrix = android.graphics.Matrix()
                    for (stroke in pageData.strokes) {
                        strokeRenderer.draw(canvas, stroke, identityMatrix)
                    }
                }

                // 4. Draw Text Boxes for this page
                if (pageIdx < note.pages.size) {
                    val pageData = note.pages[pageIdx]
                    val textPaint = TextPaint().apply {
                        color = android.graphics.Color.parseColor("#0F172A")
                        textSize = 28f
                        isAntiAlias = true
                    }
                    for (box in pageData.textBoxes) {
                        if (box.text.isNotBlank()) {
                            canvas.save()
                            canvas.translate(box.x, box.y)
                            val layoutWidth = box.width.toInt().coerceAtLeast(100)
                            val staticLayout = StaticLayout.Builder.obtain(
                                box.text, 0, box.text.length, textPaint, layoutWidth
                            ).build()
                            staticLayout.draw(canvas)
                            canvas.restore()
                        }
                    }
                }

                pdfDoc.finishPage(page)
                pdfResult?.bitmap?.recycle()
            }

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val sanitizedTitle = note.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val outputFile = File(exportDir, "${sanitizedTitle}_Lernspace.pdf")

            val outStream = FileOutputStream(outputFile)
            pdfDoc.writeTo(outStream)
            outStream.flush()
            outStream.close()
            pdfDoc.close()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawPaperLinesOnCanvas(
        canvas: Canvas,
        style: PaperStyle,
        width: Int,
        height: Int
    ) {
        when (style) {
            PaperStyle.RULED -> {
                // Red vertical notebook margin line (exact CanvasConstants.RULED_MARGIN_X)
                val marginPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FCA5A5")
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawLine(
                    CanvasConstants.RULED_MARGIN_X,
                    0f,
                    CanvasConstants.RULED_MARGIN_X,
                    height.toFloat(),
                    marginPaint
                )

                // Horizontal ruled lines (exact CanvasConstants.RULED_FIRST_LINE_Y & RULED_LINE_SPACING)
                val linePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#E2E8F0")
                    strokeWidth = 1.8f
                    isAntiAlias = true
                }
                var y = CanvasConstants.RULED_FIRST_LINE_Y
                while (y < height - 40f) {
                    canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
                    y += CanvasConstants.RULED_LINE_SPACING
                }
            }
            PaperStyle.GRID -> {
                val gridPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#EEF2F6")
                    strokeWidth = 1.5f
                    isAntiAlias = true
                }
                var x = CanvasConstants.GRID_SPACING
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                    x += CanvasConstants.GRID_SPACING
                }
                var y = CanvasConstants.GRID_SPACING
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                    y += CanvasConstants.GRID_SPACING
                }
            }
            PaperStyle.DOTTED -> {
                val dotPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#CBD5E1")
                    isAntiAlias = true
                }
                var x = CanvasConstants.DOT_SPACING
                while (x < width) {
                    var y = CanvasConstants.DOT_SPACING
                    while (y < height) {
                        canvas.drawCircle(x, y, 3f, dotPaint)
                        y += CanvasConstants.DOT_SPACING
                    }
                    x += CanvasConstants.DOT_SPACING
                }
            }
            PaperStyle.BLANK -> {
                // Pure clean white canvas
            }
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Note"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

