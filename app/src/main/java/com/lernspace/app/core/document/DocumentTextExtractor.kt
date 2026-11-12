package com.lernspace.app.core.document

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.resume

data class ExtractedDocumentResult(
    val fileName: String,
    val text: String,
    val pageCount: Int,
    val wordCount: Int,
    val mimeType: String
)

object DocumentTextExtractor {

    private const val TAG = "DocumentTextExtractor"

    suspend fun extractTextFromUri(
        context: Context,
        uri: Uri,
        onProgress: (page: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<ExtractedDocumentResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(context, uri)
            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFileName(fileName)

            when {
                // PDF Document
                mimeType.contains("pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    extractFromPdf(context, uri, fileName, onProgress)
                }

                // Plain Text / Markdown / Code / JSON / CSV
                isTextFile(fileName, mimeType) -> {
                    val text = contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }.orEmpty()
                    val words = countWords(text)
                    Result.success(
                        ExtractedDocumentResult(
                            fileName = fileName,
                            text = text.trim(),
                            pageCount = 1,
                            wordCount = words,
                            mimeType = mimeType
                        )
                    )
                }

                // Image file (PNG, JPG, WEBP)
                mimeType.startsWith("image/", ignoreCase = true) || isImageFile(fileName) -> {
                    val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                    if (bitmap == null) {
                        return@withContext Result.failure(IllegalStateException("Gagal memuat gambar."))
                    }
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val visionResult = awaitTask(recognizer.process(inputImage))
                    val text = visionResult?.text?.trim().orEmpty()
                    val words = countWords(text)
                    bitmap.recycle()
                    Result.success(
                        ExtractedDocumentResult(
                            fileName = fileName,
                            text = text,
                            pageCount = 1,
                            wordCount = words,
                            mimeType = mimeType
                        )
                    )
                }

                else -> {
                    // Fallback: try reading as plain text stream
                    val text = contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }.orEmpty()
                    if (text.isNotBlank()) {
                        Result.success(
                            ExtractedDocumentResult(
                                fileName = fileName,
                                text = text.trim(),
                                pageCount = 1,
                                wordCount = countWords(text),
                                mimeType = "text/plain"
                            )
                        )
                    } else {
                        Result.failure(
                            IllegalArgumentException("Format file '$fileName' tidak didukung. Harap gunakan PDF, teks (.txt/.md), atau gambar.")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengekstrak teks dari uri: $uri", e)
            Result.failure(e)
        }
    }

    private suspend fun extractFromPdf(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (page: Int, total: Int) -> Unit
    ): Result<ExtractedDocumentResult> {
        val pageCount = PdfNoteHelper.getPdfPageCount(context, uri)
        if (pageCount <= 0) {
            return Result.failure(IllegalStateException("PDF tidak memiliki halaman atau tidak dapat dibuka."))
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = StringBuilder()
        // Process up to 15 pages to keep memory & performance safe on tablet
        val pagesToProcess = minOf(pageCount, 15)

        for (p in 0 until pagesToProcess) {
            onProgress(p + 1, pagesToProcess)
            val bmp = PdfNoteHelper.renderPdfPage(context, uri, p, targetWidth = 1400)
            if (bmp != null) {
                try {
                    val inputImage = InputImage.fromBitmap(bmp, 0)
                    val visionResult = awaitTask(recognizer.process(inputImage))
                    val pageText = visionResult?.text?.trim().orEmpty()
                    if (pageText.isNotBlank()) {
                        if (sb.isNotEmpty()) sb.append("\n\n")
                        sb.append("--- Halaman ${p + 1} ---\n").append(pageText)
                    }
                } finally {
                    bmp.recycle()
                }
            }
        }

        val fullText = sb.toString().trim()
        if (fullText.isBlank()) {
            return Result.failure(IllegalStateException("Tidak ada teks yang terdeteksi dari halaman PDF ini."))
        }

        return Result.success(
            ExtractedDocumentResult(
                fileName = fileName,
                text = fullText,
                pageCount = pageCount,
                wordCount = countWords(fullText),
                mimeType = "application/pdf"
            )
        )
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment.orEmpty().substringAfterLast('/')
        }
        return if (name.isBlank()) "Dokumen" else name
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "md", "markdown" -> "text/markdown"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    private fun isTextFile(fileName: String, mime: String): Boolean {
        if (mime.startsWith("text/")) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in setOf("txt", "md", "markdown", "csv", "json", "rtf", "log")
    }

    private fun isImageFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in setOf("png", "jpg", "jpeg", "webp", "bmp")
    }

    private fun countWords(str: String): Int {
        if (str.isBlank()) return 0
        return str.trim().split(Regex("""\s+""")).size
    }

    private suspend fun <T> awaitTask(task: Task<T>): T? =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }.addOnFailureListener { e ->
                Log.w(TAG, "MLKit Task failed: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }.addOnCanceledListener {
                if (cont.isActive) cont.resume(null)
            }
        }
}
