package com.lernspace.app.ui.workspace.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.ink.strokes.Stroke
import java.util.UUID

enum class NoteTool {
    PEN,
    HIGHLIGHTER,
    ERASER,
    SHAPE,
    LASSO,
    TEXT,
    IMAGE
}

enum class GeometricShapeType(val label: String) {
    LINE("Garis Lurus"),
    RECTANGLE("Kotak / Persegi"),
    CIRCLE("Lingkaran / Oval"),
    TRIANGLE("Segitiga"),
    ARROW("Panah")
}

enum class PaperStyle(val displayName: String) {
    RULED("Ruled Lines"),
    GRID("Math Grid"),
    DOTTED("Dot Grid"),
    BLANK("Blank Paper")
}

/**
 * Fundamental canvas medium type chosen strictly at creation time and immutable afterwards.
 */
enum class NoteCanvasType(val title: String, val description: String) {
    PAGED_NOTE("Buku Berhalaman", "Halaman berurutan dengan template garis tetap (Page 1 of N)"),
    PDF_DOCUMENT("Dokumen PDF", "Anotasi & coret-coret langsung di atas file PDF yang diimpor"),
    INFINITE_SCRATCHPAD("Scratchpad Bebas", "Papan kanvas tak terbatas untuk coretan cepat & mind mapping")
}

object CanvasConstants {
    const val PAGE_WIDTH = 1800f
    const val PAGE_HEIGHT = 2545f // Standard ISO 216 A4 ratio: 1800 * 1.414 ≈ 2545f

    // Standard Ruled Paper Metrics (exact 1:1 synchronization between Compose & PdfRenderer)
    const val RULED_MARGIN_X = 140f
    const val RULED_FIRST_LINE_Y = 160f
    const val RULED_LINE_SPACING = 60f

    // Standard Grid Paper Metrics
    const val GRID_SPACING = 60f

    // Standard Dot Paper Metrics
    const val DOT_SPACING = 60f
}

data class TextBoxElement(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float = 260f,  // width in dp
    var height: Float = 110f, // height in dp
    var text: String = "",
    var isSelected: Boolean = true
)

data class ImageElement(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    val bitmap: ImageBitmap,
    var width: Float = 260f,
    var height: Float = 200f,
    var isSelected: Boolean = true
)

/**
 * Isolated content per page in a paged note or PDF document.
 */
data class NotePageData(
    val id: String = UUID.randomUUID().toString(),
    var pageIndex: Int,
    val strokes: MutableList<Stroke> = mutableListOf(),
    val textBoxes: MutableList<TextBoxElement> = mutableListOf(),
    val imageElements: MutableList<ImageElement> = mutableListOf()
)

data class NoteUiModel(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val subject: String,
    val date: String,
    val snippet: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val topicName: String? = null,
    val isScratchpad: Boolean = false,
    val canvasType: NoteCanvasType = if (isScratchpad) NoteCanvasType.INFINITE_SCRATCHPAD else NoteCanvasType.PAGED_NOTE,
    val paperStyle: PaperStyle = PaperStyle.RULED, // Immutable format set at creation
    var pdfUri: String? = null,
    var pdfTotalPages: Int = 1,
    val pages: MutableList<NotePageData> = mutableListOf(NotePageData(pageIndex = 0))
)
