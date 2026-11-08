package com.lernspace.app.ui.workspace.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle
import java.io.File
import java.io.FileOutputStream

object PdfNoteHelper {

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
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
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

    fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1800
    ): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            page = renderer.openPage(pageIndex)

            val pageWidth = page.width
            val pageHeight = page.height
            val scale = targetWidth.toFloat() / pageWidth.toFloat()
            val targetHeight = (pageHeight * scale).toInt()

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // Fill with pure white background first
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Export the multi-page note or PDF annotation into a clean standard PDF document.
     */
    fun exportNoteToPdf(
        context: Context,
        note: NoteUiModel,
        strokeRenderer: CanvasStrokeRenderer
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val exportWidth = 1200
            val exportHeight = 1600

            val uri = note.pdfUri?.let { Uri.parse(it) }
            val totalPages = if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) note.pdfTotalPages else note.pages.size

            for (pageIdx in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(exportWidth, exportHeight, pageIdx + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Background
                if (note.canvasType == NoteCanvasType.PDF_DOCUMENT && uri != null) {
                    val pdfBitmap = renderPdfPage(context, uri, pageIdx, exportWidth)
                    if (pdfBitmap != null) {
                        canvas.drawBitmap(pdfBitmap, 0f, 0f, null)
                    } else {
                        canvas.drawColor(android.graphics.Color.WHITE)
                    }
                } else {
                    canvas.drawColor(android.graphics.Color.WHITE)
                    // Draw paper lines or grid
                    drawPaperLinesOnCanvas(canvas, note.paperStyle, exportWidth, exportHeight)
                }

                // 2. Draw Vector Strokes for this page
                if (pageIdx < note.pages.size) {
                    val pageData = note.pages[pageIdx]
                    val identityMatrix = android.graphics.Matrix()
                    for (stroke in pageData.strokes) {
                        strokeRenderer.draw(canvas, stroke, identityMatrix)
                    }
                }

                pdfDoc.finishPage(page)
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
        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            strokeWidth = 2f
            isAntiAlias = true
        }

        when (style) {
            PaperStyle.RULED -> {
                val lineSpacing = 40f
                var y = 80f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, paint)
                    y += lineSpacing
                }
            }
            PaperStyle.GRID -> {
                val gridSize = 40f
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), paint)
                    x += gridSize
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, paint)
                    y += gridSize
                }
            }
            PaperStyle.DOTTED -> {
                val dotSpacing = 40f
                val dotPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#CBD5E1")
                    isAntiAlias = true
                }
                var x = 20f
                while (x < width) {
                    var y = 20f
                    while (y < height) {
                        canvas.drawCircle(x, y, 2.5f, dotPaint)
                        y += dotSpacing
                    }
                    x += dotSpacing
                }
            }
            PaperStyle.BLANK -> {
                // Pure white canvas
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

