package com.lernspace.app.ui.workspace.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID

enum class NoteTool {
    PEN,
    HIGHLIGHTER,
    ERASER,
    TEXT,
    IMAGE
}

enum class PaperStyle(val displayName: String) {
    RULED("Ruled Lines"),
    GRID("Math Grid"),
    DOTTED("Dot Grid"),
    BLANK("Blank Paper")
}

data class TextBoxElement(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var text: String = "",
    var color: Color = Color(0xFF1E293B),
    var isFocused: Boolean = false
)

data class ImageElement(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    val bitmap: ImageBitmap,
    var width: Float = 260f,
    var height: Float = 200f
)

data class NoteUiModel(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val subject: String,
    val date: String,
    val snippet: String
)
