package com.lernspace.app.core.document

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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
                    val text = if (GeminiApiClient.isConfigured()) {
                        GeminiApiClient.generateTextFromImages(
                            systemPrompt = "Ekstrak seluruh teks yang ada pada gambar dokumen ini menjadi teks terstruktur tanpa kata pengantar.",
                            userPrompt = "Ekstrak seluruh tulisan pada gambar ini:",
                            imageBitmaps = listOf(bitmap)
                        ).getOrNull()?.trim().orEmpty()
                    } else ""
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

        val sb = StringBuilder()
        val pagesToProcess = minOf(pageCount, 10)

        for (p in 0 until pagesToProcess) {
            onProgress(p + 1, pagesToProcess)
            val bmp = PdfNoteHelper.renderPdfPage(context, uri, p, targetWidth = 1200)
            if (bmp != null) {
                try {
                    val pageText = if (GeminiApiClient.isConfigured()) {
                        GeminiApiClient.generateTextFromImages(
                            systemPrompt = "Ekstrak seluruh materi teks, soal, dan tabel pada halaman dokumen ini secara terstruktur tanpa sapaan.",
                            userPrompt = "Ekstrak teks pada halaman dokumen ini:",
                            imageBitmaps = listOf(bmp)
                        ).getOrNull()?.trim().orEmpty()
                    } else ""

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
                pageCount = pagesToProcess,
                wordCount = countWords(fullText),
                mimeType = "application/pdf"
            )
        )
    }

    private fun isTextFile(fileName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/", ignoreCase = true)) return true
        val extensions = listOf(".txt", ".md", ".json", ".csv", ".kt", ".java", ".py", ".js", ".ts", ".html", ".css", ".xml")
        return extensions.any { fileName.endsWith(it, ignoreCase = true) }
    }

    private fun isImageFile(fileName: String): Boolean {
        val extensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".bmp")
        return extensions.any { fileName.endsWith(it, ignoreCase = true) }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "dokumen"
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            name = cursor.getString(index) ?: "dokumen"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mendapatkan nama file dari ContentResolver", e)
            }
        } else if (uri.scheme == "file") {
            name = uri.lastPathSegment ?: "dokumen"
        }
        return name
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
            fileName.endsWith(".md", ignoreCase = true) -> "text/markdown"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
}
