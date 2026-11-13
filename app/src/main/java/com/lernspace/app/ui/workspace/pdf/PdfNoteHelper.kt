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
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
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

    // Keep up to 24 MB of pre-rendered PDF pages in memory for instant Next/Prev flips without kswapd pressure
    private val pageCache = object : LruCache<String, PdfPageRenderResult>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: PdfPageRenderResult): Int {
            return value.bitmap.allocationByteCount.coerceAtLeast(1)
        }
    }

    private var cachedUriString: String? = null
    private var cachedPfd: ParcelFileDescriptor? = null
    private var cachedRenderer: PdfRenderer? = null

    private fun closeCachedRendererLocked() {
        try { cachedRenderer?.close() } catch (_: Exception) {}
        try { cachedPfd?.close() } catch (_: Exception) {}
        cachedRenderer = null
        cachedPfd = null
        cachedUriString = null
    }

    private fun getOrOpenRendererLocked(context: Context, uri: Uri): PdfRenderer? {
        val uriStr = uri.toString()
        if (cachedUriString == uriStr && cachedRenderer != null) {
            return cachedRenderer
        }
        closeCachedRendererLocked()
        val pfd = openPdfDescriptor(context, uri) ?: return null
        return try {
            val renderer = PdfRenderer(pfd)
            cachedUriString = uriStr
            cachedPfd = pfd
            cachedRenderer = renderer
            renderer
        } catch (_: Exception) {
            try { pfd.close() } catch (_: Exception) {}
            null
        }
    }

    fun invalidatePdfCache(uriString: String? = null) {
        synchronized(pdfRendererLock) {
            if (uriString == null || cachedUriString == uriString) {
                closeCachedRendererLocked()
            }
            if (uriString == null) {
                pageCache.evictAll()
            } else {
                val prefix = "$uriString#"
                pageCache.snapshot().keys
                    .filter { it.startsWith(prefix) }
                    .forEach { pageCache.remove(it) }
            }
        }
    }

    fun getCachedPage(uri: Uri, pageIndex: Int, targetWidth: Int = 2400): PdfPageRenderResult? {
        val cached = pageCache.get("${uri}#$pageIndex#$targetWidth")
        return if (cached != null && !cached.bitmap.isRecycled) cached else null
    }

    fun preloadPage(context: Context, uri: Uri, pageIndex: Int, targetWidth: Int = 2400) {
        if (pageIndex < 0) return
        if (getCachedPage(uri, pageIndex, targetWidth) != null) return
        renderPdfPageWithBounds(context, uri, pageIndex, targetWidth)
    }

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
            return try {
                getOrOpenRendererLocked(context, uri)?.pageCount ?: 1
            } catch (_: Exception) {
                closeCachedRendererLocked()
                try {
                    getOrOpenRendererLocked(context, uri)?.pageCount ?: 1
                } catch (_: Exception) {
                    1
                }
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
        val cacheKey = "${uri}#$pageIndex#$targetWidth"
        pageCache.get(cacheKey)?.let { cached ->
            if (!cached.bitmap.isRecycled) return cached
        }

        synchronized(pdfRendererLock) {
            pageCache.get(cacheKey)?.let { cached ->
                if (!cached.bitmap.isRecycled) return cached
            }
            for (attempt in 0..1) {
                var page: PdfRenderer.Page? = null
                try {
                    val renderer = getOrOpenRendererLocked(context, uri) ?: return null
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
                    val result = PdfPageRenderResult(
                        bitmap = bitmap,
                        worldWidth = worldWidth,
                        worldHeight = worldHeight
                    )
                    pageCache.put(cacheKey, result)
                    return result
                } catch (_: Exception) {
                    closeCachedRendererLocked()
                    if (attempt == 1) return null
                } finally {
                    try { page?.close() } catch (_: Exception) {}
                }
            }
            return null
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
            for (attempt in 0..1) {
                var page: PdfRenderer.Page? = null
                try {
                    val renderer = getOrOpenRendererLocked(context, uri) ?: return null
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
                    return bitmap
                } catch (_: Exception) {
                    closeCachedRendererLocked()
                    if (attempt == 1) return null
                } finally {
                    try { page?.close() } catch (_: Exception) {}
                }
            }
            return null
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

                // 3. Draw Vector Strokes for this page (Under-ink rendering: Highlighters first, Pens on top)
                if (pageIdx < note.pages.size) {
                    val pageData = note.pages[pageIdx]
                    val identityMatrix = android.graphics.Matrix()

                    // 3a. Pass 1: Draw highlighters underneath
                    for (stroke in pageData.strokes) {
                        if (stroke.brush.family == androidx.ink.brush.StockBrushes.highlighter()) {
                            strokeRenderer.draw(canvas, stroke, identityMatrix)
                        }
                    }

                    // 3b. Pass 2: Draw pens and shapes on top (with smart inversion for dark-mode strokes)
                    for (stroke in pageData.strokes) {
                        if (stroke.brush.family != androidx.ink.brush.StockBrushes.highlighter()) {
                            // If stroke was drawn in dark paper off-white (#F8FAFC), render as dark ink (#191C1E) on white PDF
                            if (stroke.brush.colorIntArgb == 0xFFF8FAFC.toInt()) {
                                val correctedBrush = androidx.ink.brush.Brush.createWithColorIntArgb(
                                    family = stroke.brush.family,
                                    colorIntArgb = 0xFF191C1E.toInt(),
                                    size = stroke.brush.size,
                                    epsilon = stroke.brush.epsilon
                                )
                                val correctedStroke = androidx.ink.strokes.Stroke(correctedBrush, stroke.inputs)
                                strokeRenderer.draw(canvas, correctedStroke, identityMatrix)
                            } else {
                                strokeRenderer.draw(canvas, stroke, identityMatrix)
                            }
                        }
                    }
                }

                // 4. Draw Text Boxes for this page
                if (pageIdx < note.pages.size) {
                    val pageData = note.pages[pageIdx]
                    for (box in pageData.textBoxes) {
                        if (box.text.isNotBlank()) {
                            val renderedText = com.lernspace.app.ui.workspace.components.LatexMathFormatter.toPlainReadableText(box.text)
                            val styleFlag = when {
                                box.isBold && box.isItalic -> android.graphics.Typeface.BOLD_ITALIC
                                box.isBold -> android.graphics.Typeface.BOLD
                                box.isItalic -> android.graphics.Typeface.ITALIC
                                else -> android.graphics.Typeface.NORMAL
                            }
                            val textPaint = TextPaint().apply {
                                color = box.colorArgb ?: if (note.isDarkPaper) android.graphics.Color.parseColor("#F8FAFC") else android.graphics.Color.parseColor("#0F172A")
                                textSize = box.fontSize.coerceIn(12f, 96f)
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, styleFlag)
                                isAntiAlias = true
                            }
                            val align = when (box.textAlign) {
                                com.lernspace.app.ui.workspace.model.TextBoxAlign.CENTER -> android.text.Layout.Alignment.ALIGN_CENTER
                                com.lernspace.app.ui.workspace.model.TextBoxAlign.RIGHT -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                                com.lernspace.app.ui.workspace.model.TextBoxAlign.LEFT -> android.text.Layout.Alignment.ALIGN_NORMAL
                            }
                            canvas.save()
                            canvas.translate(box.x, box.y)
                            val layoutWidth = box.width.toInt().coerceAtLeast(100)
                            val staticLayout = StaticLayout.Builder.obtain(
                                renderedText, 0, renderedText.length, textPaint, layoutWidth
                            ).setAlignment(align).build()
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

    /**
     * Saves the exported PDF file into the public Downloads directory so the student
     * can easily find, read offline, or backup their document using any file manager.
     */
    fun savePdfToDownloads(context: Context, file: File): Boolean {
        return try {
            val fileName = file.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                true
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                file.copyTo(destFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

