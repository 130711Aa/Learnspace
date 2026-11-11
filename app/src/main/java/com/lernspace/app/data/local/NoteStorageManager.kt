package com.lernspace.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.storage.StrokeInputBatchSerialization
import androidx.ink.strokes.Stroke
import com.lernspace.app.ui.workspace.model.ImageElement
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.TextBoxElement
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

data class LoadedNoteContent(
    val pages: List<NotePageData>,
    val isDarkPaper: Boolean
)

/**
 * High-performance local storage manager for note canvas contents
 * (AndroidX Ink Strokes, TextBoxes, and Images).
 */
class NoteStorageManager(private val context: Context) {

    private val noteLocks = ConcurrentHashMap<String, Any>()
    private fun lockFor(noteId: String): Any = noteLocks.getOrPut(noteId) { Any() }

    private val notesDir: File
        get() = File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }

    private val imagesDir: File
        get() = File(notesDir, "images").apply { if (!exists()) mkdirs() }

    private val pdfsDir: File
        get() = File(notesDir, "pdfs").apply { if (!exists()) mkdirs() }

    companion object {
        /**
         * Safely copies an external PDF Uri into the app's internal private storage directory
         * (files/notes/pdfs/<noteId>.pdf) so it stays permanently readable across app restarts.
         */
        fun copyPdfToInternal(context: Context, uri: Uri, noteId: String): String? {
            return try {
                val notesDir = File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }
                val pdfsDir = File(notesDir, "pdfs").apply { if (!exists()) mkdirs() }
                val targetFile = File(pdfsDir, "$noteId.pdf")

                if (uri.scheme == "file" && uri.path == targetFile.absolutePath && targetFile.exists() && targetFile.length() > 0L) {
                    return Uri.fromFile(targetFile).toString()
                }

                val inputStream = if (uri.scheme == "file" && uri.path != null) {
                    val srcFile = File(uri.path!!)
                    if (srcFile.exists()) srcFile.inputStream() else null
                } else {
                    context.contentResolver.openInputStream(uri)
                } ?: return null

                inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }

                if (targetFile.exists() && targetFile.length() > 0L) {
                    Uri.fromFile(targetFile).toString()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Ensures a note's PDF resides in internal storage, migrating legacy content:// URIs if readable.
         */
        fun ensurePdfInternal(context: Context, noteId: String, currentUriString: String): String? {
            val notesDir = File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }
            val pdfsDir = File(notesDir, "pdfs").apply { if (!exists()) mkdirs() }
            val targetFile = File(pdfsDir, "$noteId.pdf")
            if (targetFile.exists() && targetFile.length() > 0L) {
                return Uri.fromFile(targetFile).toString()
            }
            return copyPdfToInternal(context, Uri.parse(currentUriString), noteId)
        }

        /**
         * Safely copies an external image Uri into the app's internal private storage directory
         * (files/notes/images/<imageId>.png) with automatic memory-safe downsampling for huge photos.
         */
        fun copyImageToInternal(context: Context, uri: Uri, imageId: String): Bitmap? {
            return try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                val rawW = options.outWidth
                val rawH = options.outHeight
                if (rawW <= 0 || rawH <= 0) return null

                // Downsample large camera photos to safe bounds (max dimension <= 2048px)
                var sampleSize = 1
                val maxDim = maxOf(rawW, rawH)
                while (maxDim / sampleSize > 2048) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return null

                val notesDir = File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }
                val imagesDir = File(notesDir, "images").apply { if (!exists()) mkdirs() }
                val targetFile = File(imagesDir, "$imageId.png")

                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }

                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Saves all pages of a note into a compact JSON file.
     */
    fun saveNotePages(noteId: String, pages: List<NotePageData>, isDarkPaper: Boolean = false) {
        synchronized(lockFor(noteId)) {
        try {
            val rootObj = JSONObject()
            rootObj.put("noteId", noteId)
            rootObj.put("updatedAt", System.currentTimeMillis())
            rootObj.put("isDarkPaper", isDarkPaper)

            val pagesArray = JSONArray()

            pages.forEach { page ->
                val pageObj = JSONObject()
                pageObj.put("pageIndex", page.pageIndex)

                // 1. Text Boxes
                val textBoxesArray = JSONArray()
                page.textBoxes.forEach { tb ->
                    val tbObj = JSONObject().apply {
                        put("id", tb.id)
                        put("x", tb.x.toDouble())
                        put("y", tb.y.toDouble())
                        put("width", tb.width.toDouble())
                        put("height", tb.height.toDouble())
                        put("text", tb.text)
                    }
                    textBoxesArray.put(tbObj)
                }
                pageObj.put("textBoxes", textBoxesArray)

                // 2. Images
                val imagesArray = JSONArray()
                page.imageElements.forEach { img ->
                    val imageFile = File(imagesDir, "${img.id}.png")
                    if (!imageFile.exists()) {
                        try {
                            FileOutputStream(imageFile).use { out ->
                                img.bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 90, out)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val imgObj = JSONObject().apply {
                        put("id", img.id)
                        put("x", img.x.toDouble())
                        put("y", img.y.toDouble())
                        put("width", img.width.toDouble())
                        put("height", img.height.toDouble())
                        put("fileName", imageFile.name)
                    }
                    imagesArray.put(imgObj)
                }
                pageObj.put("images", imagesArray)

                // 3. AndroidX Ink Strokes (Compressed binary protobuf via androidx.ink:ink-storage)
                val strokesArray = JSONArray()
                val baos = ByteArrayOutputStream(512)
                page.strokes.forEach { stroke ->
                    if (stroke.inputs.size > 0) {
                        val strokeObj = JSONObject()
                        strokeObj.put("size", stroke.brush.size.toDouble())
                        strokeObj.put("color", stroke.brush.colorIntArgb)
                        strokeObj.put(
                            "brushFamily",
                            if (stroke.brush.family == StockBrushes.highlighter()) "highlighter" else "pen"
                        )
                        baos.reset()
                        StrokeInputBatchSerialization.encode(stroke.inputs, baos)
                        strokeObj.put(
                            "batchB64",
                            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        )
                        strokesArray.put(strokeObj)
                    }
                }
                pageObj.put("strokes", strokesArray)

                pagesArray.put(pageObj)
            }

            rootObj.put("pages", pagesArray)

            val noteFile = File(notesDir, "$noteId.json")
            val tempFile = File(notesDir, "$noteId.json.tmp")
            tempFile.writeText(rootObj.toString())
            try {
                Files.move(
                    tempFile.toPath(),
                    noteFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: Exception) {
                if (noteFile.exists()) noteFile.delete()
                if (!tempFile.renameTo(noteFile)) {
                    tempFile.copyTo(noteFile, overwrite = true)
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        }
    }

    /**
     * Loads both pages and metadata (e.g. isDarkPaper) in a single pass.
     */
    fun loadNoteWithMeta(noteId: String): LoadedNoteContent? {
        synchronized(lockFor(noteId)) {
            val noteFile = File(notesDir, "$noteId.json")
            if (!noteFile.exists()) return null
            return try {
                val jsonText = noteFile.readText()
                val rootObj = JSONObject(jsonText)
                val isDark = rootObj.optBoolean("isDarkPaper", false)
                val pages = parsePagesFromRoot(rootObj) ?: return null
                LoadedNoteContent(pages = pages, isDarkPaper = isDark)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Loads and reconstructs all pages, strokes, text boxes, and images from disk.
     */
    fun loadNotePages(noteId: String): List<NotePageData>? {
        return loadNoteWithMeta(noteId)?.pages
    }

    private fun parsePagesFromRoot(rootObj: JSONObject): List<NotePageData>? {
        return try {
            val pagesArray = rootObj.optJSONArray("pages") ?: return null

            val resultPages = mutableListOf<NotePageData>()

            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                val pageIndex = pageObj.optInt("pageIndex", i)
                val pageData = NotePageData(pageIndex = pageIndex)

                // 1. Text Boxes
                val textBoxesArray = pageObj.optJSONArray("textBoxes")
                if (textBoxesArray != null) {
                    for (j in 0 until textBoxesArray.length()) {
                        val tbObj = textBoxesArray.getJSONObject(j)
                        pageData.textBoxes.add(
                            TextBoxElement(
                                id = tbObj.optString("id"),
                                x = tbObj.optDouble("x").toFloat(),
                                y = tbObj.optDouble("y").toFloat(),
                                width = tbObj.optDouble("width", 260.0).toFloat(),
                                height = tbObj.optDouble("height", 110.0).toFloat(),
                                text = tbObj.optString("text", ""),
                                isSelected = false
                            )
                        )
                    }
                }

                // 2. Images
                val imagesArray = pageObj.optJSONArray("images")
                if (imagesArray != null) {
                    for (j in 0 until imagesArray.length()) {
                        val imgObj = imagesArray.getJSONObject(j)
                        val fileName = imgObj.optString("fileName")
                        val imageFile = File(imagesDir, fileName)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                            if (bitmap != null) {
                                pageData.imageElements.add(
                                    ImageElement(
                                        id = imgObj.optString("id"),
                                        x = imgObj.optDouble("x").toFloat(),
                                        y = imgObj.optDouble("y").toFloat(),
                                        bitmap = bitmap.asImageBitmap(),
                                        width = imgObj.optDouble("width", 500.0).toFloat(),
                                        height = imgObj.optDouble("height", 350.0).toFloat(),
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. AndroidX Ink Strokes (Binary ink-storage with seamless legacy JSON fallback)
                val strokesArray = pageObj.optJSONArray("strokes")
                if (strokesArray != null) {
                    for (j in 0 until strokesArray.length()) {
                        val strokeObj = strokesArray.getJSONObject(j)
                        val size = strokeObj.optDouble("size", 4.0).toFloat()
                        val colorInt = strokeObj.optInt("color", 0xFF191C1E.toInt())
                        val finalColor = if (colorInt != 0) colorInt else 0xFF191C1E.toInt()
                        val familyName = strokeObj.optString("brushFamily", "pen")
                        val family = if (familyName == "highlighter") {
                            StockBrushes.highlighter()
                        } else {
                            StockBrushes.pressurePen()
                        }
                        val brush = Brush.createWithColorIntArgb(
                            family = family,
                            colorIntArgb = finalColor,
                            size = size,
                            epsilon = 0.1f
                        )

                        val batchB64 = strokeObj.optString("batchB64", "")
                        if (batchB64.isNotEmpty()) {
                            try {
                                val bytes = Base64.decode(batchB64, Base64.NO_WRAP)
                                val batch = StrokeInputBatchSerialization.decode(ByteArrayInputStream(bytes))
                                if (batch.size > 0) {
                                    pageData.strokes.add(Stroke(brush, batch))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            val pointsArray = strokeObj.optJSONArray("points")
                            if (pointsArray != null && pointsArray.length() >= 2) {
                                val points = mutableListOf<Offset>()
                                for (k in 0 until pointsArray.length()) {
                                    val ptObj = pointsArray.getJSONObject(k)
                                    points.add(
                                        Offset(
                                            ptObj.optDouble("x").toFloat(),
                                            ptObj.optDouble("y").toFloat()
                                        )
                                    )
                                }
                                try {
                                    val stroke = ShapeRecognizer.createStrokeFromPoints(points, brush)
                                    pageData.strokes.add(stroke)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                resultPages.add(pageData)
            }

            if (resultPages.isEmpty()) listOf(NotePageData(pageIndex = 0)) else resultPages
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads whether a note uses dark paper.
     */
    fun isDarkPaper(noteId: String): Boolean {
        val noteFile = File(notesDir, "$noteId.json")
        if (!noteFile.exists()) return false
        return try {
            val jsonText = noteFile.readText()
            val rootObj = JSONObject(jsonText)
            rootObj.optBoolean("isDarkPaper", false)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes note storage file and any internal PDF copy.
     */
    fun deleteNoteStorage(noteId: String) {
        val noteFile = File(notesDir, "$noteId.json")
        if (noteFile.exists()) {
            noteFile.delete()
        }
        val pdfFile = File(pdfsDir, "$noteId.pdf")
        if (pdfFile.exists()) {
            pdfFile.delete()
        }
    }

    /**
     * Wipes all note JSON and asset files on disk.
     */
    fun clearAllFiles() {
        try {
            notesDir.listFiles()?.forEach { it.delete() }
            imagesDir.listFiles()?.forEach { it.delete() }
            pdfsDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

