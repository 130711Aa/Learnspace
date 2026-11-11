package com.lernspace.app.ui.workspace.math

import androidx.compose.ui.geometry.Offset
import androidx.ink.brush.Brush
import androidx.ink.strokes.Stroke
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer

/**
 * Renders mathematical answers as natural handwritten-style AndroidX Ink vector strokes,
 * preserving Ink's organic curve smoothing and the user's active pen brush.
 */
object StrokeVectorFont {

    // Sparse control points so AndroidX Ink renders smooth, organic handwritten curves
    private val glyphMap: Map<Char, List<List<Pair<Float, Float>>>> = mapOf(
        '0' to listOf(
            listOf(0.32f to 0.05f, 0.12f to 0.14f, 0.06f to 0.50f, 0.12f to 0.86f, 0.32f to 0.95f, 0.52f to 0.86f, 0.58f to 0.50f, 0.52f to 0.14f, 0.32f to 0.05f)
        ),
        '1' to listOf(
            listOf(0.16f to 0.22f, 0.34f to 0.05f, 0.34f to 0.95f),
            listOf(0.16f to 0.95f, 0.52f to 0.95f)
        ),
        '2' to listOf(
            listOf(0.10f to 0.22f, 0.22f to 0.07f, 0.44f to 0.07f, 0.56f to 0.22f, 0.52f to 0.42f, 0.10f to 0.95f, 0.58f to 0.95f)
        ),
        '3' to listOf(
            listOf(0.10f to 0.15f, 0.28f to 0.05f, 0.48f to 0.10f, 0.55f to 0.28f, 0.46f to 0.46f, 0.26f to 0.49f, 0.48f to 0.54f, 0.57f to 0.72f, 0.48f to 0.90f, 0.28f to 0.95f, 0.10f to 0.86f)
        ),
        '4' to listOf(
            listOf(0.42f to 0.05f, 0.08f to 0.65f, 0.58f to 0.65f),
            listOf(0.44f to 0.26f, 0.44f to 0.95f)
        ),
        '5' to listOf(
            listOf(0.54f to 0.05f, 0.14f to 0.05f, 0.12f to 0.44f, 0.36f to 0.40f, 0.54f to 0.52f, 0.56f to 0.76f, 0.42f to 0.93f, 0.16f to 0.93f, 0.08f to 0.84f)
        ),
        '6' to listOf(
            listOf(0.48f to 0.10f, 0.26f to 0.06f, 0.10f to 0.28f, 0.08f to 0.70f, 0.18f to 0.92f, 0.42f to 0.94f, 0.56f to 0.78f, 0.52f to 0.56f, 0.34f to 0.46f, 0.14f to 0.54f, 0.08f to 0.70f)
        ),
        '7' to listOf(
            listOf(0.08f to 0.05f, 0.58f to 0.05f, 0.24f to 0.95f)
        ),
        '8' to listOf(
            listOf(0.32f to 0.48f, 0.14f to 0.38f, 0.12f to 0.18f, 0.32f to 0.05f, 0.52f to 0.18f, 0.50f to 0.38f, 0.32f to 0.48f, 0.10f to 0.60f, 0.08f to 0.82f, 0.32f to 0.95f, 0.56f to 0.82f, 0.54f to 0.60f, 0.32f to 0.48f)
        ),
        '9' to listOf(
            listOf(0.54f to 0.34f, 0.42f to 0.52f, 0.22f to 0.52f, 0.08f to 0.36f, 0.12f to 0.14f, 0.32f to 0.05f, 0.52f to 0.16f, 0.56f to 0.54f, 0.44f to 0.86f, 0.22f to 0.95f, 0.12f to 0.88f)
        ),
        '.' to listOf(
            listOf(0.24f to 0.86f, 0.28f to 0.94f, 0.20f to 0.94f, 0.24f to 0.86f)
        ),
        ',' to listOf(
            listOf(0.28f to 0.84f, 0.22f to 0.98f)
        ),
        '-' to listOf(
            listOf(0.10f to 0.52f, 0.54f to 0.52f)
        ),
        '+' to listOf(
            listOf(0.10f to 0.52f, 0.54f to 0.52f),
            listOf(0.32f to 0.26f, 0.32f to 0.78f)
        ),
        '=' to listOf(
            listOf(0.10f to 0.40f, 0.54f to 0.40f),
            listOf(0.10f to 0.64f, 0.54f to 0.64f)
        ),
        '/' to listOf(
            listOf(0.50f to 0.08f, 0.14f to 0.92f)
        ),
        'x' to listOf(
            listOf(0.12f to 0.34f, 0.52f to 0.88f),
            listOf(0.52f to 0.34f, 0.12f to 0.88f)
        ),
        'y' to listOf(
            listOf(0.12f to 0.34f, 0.32f to 0.78f),
            listOf(0.52f to 0.34f, 0.22f to 1.04f, 0.10f to 1.08f)
        )
    )

    fun createStrokesForText(
        text: String,
        startX: Float,
        topY: Float,
        charHeight: Float,
        brush: Brush
    ): List<Stroke> {
        val h = charHeight.coerceIn(52f, 160f)
        val cellAdvance = h * 0.70f
        val dotAdvance = h * 0.34f
        val spaceAdvance = h * 0.42f

        val result = mutableListOf<Stroke>()
        var cursorX = startX

        for (rawCh in text) {
            val ch = rawCh.lowercaseChar()
            if (ch == ' ') {
                cursorX += spaceAdvance
                continue
            }
            val polylines = glyphMap[ch]
            if (polylines == null) {
                cursorX += spaceAdvance
                continue
            }
            for (poly in polylines) {
                val points = poly.map { (nx, ny) ->
                    Offset(cursorX + nx * h, topY + ny * h)
                }
                if (points.size >= 2) {
                    result.add(ShapeRecognizer.createStrokeFromPoints(points, brush))
                }
            }
            cursorX += if (ch == '.' || ch == ',') dotAdvance else cellAdvance
        }

        return result
    }
}
