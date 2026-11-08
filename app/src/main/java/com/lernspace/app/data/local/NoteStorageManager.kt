package com.lernspace.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.strokes.Stroke
import com.lernspace.app.ui.workspace.model.ImageElement
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.TextBoxElement
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance local storage manager for note canvas contents
 * (AndroidX Ink Strokes, TextBoxes, and Images).
 */
class NoteStorageManager(private val context: Context) {

    private val notesDir: File
        get() = File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }

    private val imagesDir: File
        get() = File(notesDir, "images").apply { if (!exists()) mkdirs() }

    /**
     * Saves all pages of a note into a compact JSON file.
     */
    fun saveNotePages(noteId: String, pages: List<NotePageData>) {
        try {
            val rootObj = JSONObject()
            rootObj.put("noteId", noteId)
            rootObj.put("updatedAt", System.currentTimeMillis())

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

                // 3. AndroidX Ink Strokes
                val strokesArray = JSONArray()
                page.strokes.forEach { stroke ->
                    val strokeObj = JSONObject()
                    strokeObj.put("size", stroke.brush.size.toDouble())
                    val colorInt = stroke.brush.colorLong.toInt()
                    strokeObj.put("color", colorInt)

                    val pointsArray = JSONArray()
                    val points = ShapeRecognizer.extractPoints(stroke)
                    points.forEach { pt ->
                        val ptObj = JSONObject()
                        ptObj.put("x", pt.x.toDouble())
                        ptObj.put("y", pt.y.toDouble())
                        pointsArray.put(ptObj)
                    }
                    strokeObj.put("points", pointsArray)
                    strokesArray.put(strokeObj)
                }
                pageObj.put("strokes", strokesArray)

                pagesArray.put(pageObj)
            }

            rootObj.put("pages", pagesArray)

            val noteFile = File(notesDir, "$noteId.json")
            noteFile.writeText(rootObj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads and reconstructs all pages, strokes, text boxes, and images from disk.
     */
    fun loadNotePages(noteId: String): List<NotePageData>? {
        val noteFile = File(notesDir, "$noteId.json")
        if (!noteFile.exists()) return null

        return try {
            val jsonText = noteFile.readText()
            val rootObj = JSONObject(jsonText)
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
                                        width = imgObj.optDouble("width", 260.0).toFloat(),
                                        height = imgObj.optDouble("height", 200.0).toFloat(),
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. AndroidX Ink Strokes
                val strokesArray = pageObj.optJSONArray("strokes")
                if (strokesArray != null) {
                    for (j in 0 until strokesArray.length()) {
                        val strokeObj = strokesArray.getJSONObject(j)
                        val size = strokeObj.optDouble("size", 4.0).toFloat()
                        val colorInt = strokeObj.optInt("color", 0xFF191C1E.toInt())
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
                                val brush = Brush.createWithComposeColor(
                                    family = StockBrushes.pressurePen(),
                                    color = Color(colorInt),
                                    size = size,
                                    epsilon = 0.1f
                                )
                                val stroke = ShapeRecognizer.createStrokeFromPoints(points, brush)
                                pageData.strokes.add(stroke)
                            } catch (e: Exception) {
                                e.printStackTrace()
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
     * Deletes note storage file.
     */
    fun deleteNoteStorage(noteId: String) {
        val noteFile = File(notesDir, "$noteId.json")
        if (noteFile.exists()) {
            noteFile.delete()
        }
    }
}

