package com.lernspace.app.ui.workspace.math

import androidx.compose.ui.geometry.Offset
import androidx.ink.brush.Brush
import androidx.ink.strokes.Stroke
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders mathematical answers as smooth, well-proportioned handwritten-style
 * AndroidX Ink vector strokes so auto-solved numbers, fractions, and algebraic
 * answers (`x = 5`, `a = 3.25`, `x = -3; 3`) match handwritten canvas ink naturally.
 */
object StrokeVectorFont {

    private data class GlyphDef(
        val polylines: List<List<Pair<Float, Float>>>,
        val widthRatio: Float = 0.58f
    )

    // --- Smooth polyline builders in normalized [0..1] x [0..1] glyph box ---

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float): List<Pair<Float, Float>> =
        listOf(x1 to y1, x2 to y2)

    private fun quad(
        x0: Float, y0: Float,
        cx: Float, cy: Float,
        x1: Float, y1: Float,
        steps: Int = 8
    ): List<Pair<Float, Float>> = (0..steps).map { i ->
        val t = i.toFloat() / steps
        val u = 1f - t
        val x = u * u * x0 + 2f * u * t * cx + t * t * x1
        val y = u * u * y0 + 2f * u * t * cy + t * t * y1
        x to y
    }

    private fun arc(
        cx: Float, cy: Float,
        rx: Float, ry: Float,
        startDeg: Float, endDeg: Float,
        steps: Int = 14
    ): List<Pair<Float, Float>> = (0..steps).map { i ->
        val deg = startDeg + (endDeg - startDeg) * (i.toFloat() / steps)
        val rad = deg * (PI.toFloat() / 180f)
        (cx + rx * cos(rad)) to (cy + ry * sin(rad))
    }

    private fun chain(vararg segments: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val out = ArrayList<Pair<Float, Float>>()
        for (seg in segments) {
            for (pt in seg) {
                if (out.isEmpty() || out.last() != pt) out.add(pt)
            }
        }
        return out
    }

    private val glyphMap: Map<Char, GlyphDef> = mapOf(
        // --- Digits (0-9) with smooth curves & balanced proportions ---
        '0' to GlyphDef(
            listOf(arc(0.50f, 0.50f, 0.34f, 0.45f, -70f, 290f, steps = 20)),
            widthRatio = 0.58f
        ),
        '1' to GlyphDef(
            listOf(
                chain(line(0.28f, 0.23f, 0.54f, 0.05f), line(0.54f, 0.05f, 0.54f, 0.95f)),
                line(0.26f, 0.95f, 0.80f, 0.95f)
            ),
            widthRatio = 0.42f
        ),
        '2' to GlyphDef(
            listOf(
                chain(
                    arc(0.49f, 0.27f, 0.33f, 0.22f, -155f, 25f, steps = 12),
                    quad(0.79f, 0.36f, 0.58f, 0.62f, 0.14f, 0.95f, steps = 8),
                    line(0.14f, 0.95f, 0.86f, 0.95f)
                )
            ),
            widthRatio = 0.58f
        ),
        '3' to GlyphDef(
            listOf(
                chain(
                    arc(0.46f, 0.26f, 0.32f, 0.21f, -150f, 75f, steps = 12),
                    arc(0.48f, 0.72f, 0.35f, 0.24f, -80f, 155f, steps = 14)
                )
            ),
            widthRatio = 0.56f
        ),
        '4' to GlyphDef(
            listOf(
                chain(line(0.68f, 0.05f, 0.12f, 0.68f), line(0.12f, 0.68f, 0.88f, 0.68f)),
                line(0.68f, 0.05f, 0.68f, 0.95f)
            ),
            widthRatio = 0.60f
        ),
        '5' to GlyphDef(
            listOf(
                line(0.26f, 0.05f, 0.82f, 0.05f),
                chain(
                    line(0.26f, 0.05f, 0.22f, 0.44f),
                    quad(0.22f, 0.44f, 0.42f, 0.36f, 0.62f, 0.40f, steps = 5),
                    arc(0.48f, 0.68f, 0.34f, 0.27f, -65f, 155f, steps = 14)
                )
            ),
            widthRatio = 0.58f
        ),
        '6' to GlyphDef(
            listOf(
                chain(
                    quad(0.76f, 0.11f, 0.42f, -0.02f, 0.18f, 0.42f, steps = 10),
                    arc(0.50f, 0.69f, 0.33f, 0.26f, 180f, -165f, steps = 18)
                )
            ),
            widthRatio = 0.58f
        ),
        '7' to GlyphDef(
            listOf(
                chain(
                    line(0.14f, 0.05f, 0.86f, 0.05f),
                    quad(0.86f, 0.05f, 0.58f, 0.46f, 0.34f, 0.95f, steps = 8)
                )
            ),
            widthRatio = 0.56f
        ),
        '8' to GlyphDef(
            listOf(
                chain(
                    arc(0.50f, 0.26f, 0.28f, 0.21f, -90f, 90f, steps = 10),
                    arc(0.50f, 0.72f, 0.34f, 0.24f, -90f, 270f, steps = 16),
                    arc(0.50f, 0.26f, 0.28f, 0.21f, 90f, 270f, steps = 10)
                )
            ),
            widthRatio = 0.58f
        ),
        '9' to GlyphDef(
            listOf(
                arc(0.48f, 0.30f, 0.32f, 0.24f, 10f, 370f, steps = 16),
                quad(0.80f, 0.30f, 0.78f, 0.96f, 0.24f, 0.90f, steps = 10)
            ),
            widthRatio = 0.58f
        ),

        // --- Math punctuation & operators ---
        '.' to GlyphDef(listOf(arc(0.50f, 0.91f, 0.08f, 0.05f, 0f, 360f, steps = 8)), widthRatio = 0.24f),
        ',' to GlyphDef(listOf(quad(0.52f, 0.86f, 0.55f, 0.98f, 0.32f, 1.10f, steps = 6)), widthRatio = 0.26f),
        ';' to GlyphDef(
            listOf(
                arc(0.52f, 0.48f, 0.08f, 0.05f, 0f, 360f, steps = 8),
                quad(0.52f, 0.86f, 0.55f, 0.98f, 0.32f, 1.10f, steps = 6)
            ),
            widthRatio = 0.28f
        ),
        ':' to GlyphDef(
            listOf(
                arc(0.50f, 0.36f, 0.08f, 0.05f, 0f, 360f, steps = 8),
                arc(0.50f, 0.78f, 0.08f, 0.05f, 0f, 360f, steps = 8)
            ),
            widthRatio = 0.26f
        ),
        '-' to GlyphDef(listOf(line(0.14f, 0.54f, 0.86f, 0.54f)), widthRatio = 0.48f),
        '+' to GlyphDef(
            listOf(
                line(0.14f, 0.54f, 0.86f, 0.54f),
                line(0.50f, 0.20f, 0.50f, 0.88f)
            ),
            widthRatio = 0.54f
        ),
        '=' to GlyphDef(
            listOf(
                line(0.12f, 0.42f, 0.88f, 0.42f),
                line(0.12f, 0.66f, 0.88f, 0.66f)
            ),
            widthRatio = 0.56f
        ),
        '/' to GlyphDef(listOf(line(0.78f, 0.08f, 0.22f, 0.95f)), widthRatio = 0.44f),
        '(' to GlyphDef(listOf(quad(0.68f, 0.05f, 0.24f, 0.52f, 0.68f, 0.98f, steps = 10)), widthRatio = 0.34f),
        ')' to GlyphDef(listOf(quad(0.32f, 0.05f, 0.76f, 0.52f, 0.32f, 0.98f, steps = 10)), widthRatio = 0.34f),
        '^' to GlyphDef(
            listOf(chain(line(0.20f, 0.42f, 0.50f, 0.12f), line(0.50f, 0.12f, 0.80f, 0.42f))),
            widthRatio = 0.44f
        ),

        // --- Lowercase algebraic variables (a-z) ---
        'a' to GlyphDef(
            listOf(
                chain(
                    arc(0.46f, 0.46f, 0.28f, 0.12f, -155f, 10f, steps = 8),
                    line(0.74f, 0.46f, 0.74f, 0.95f)
                ),
                arc(0.46f, 0.78f, 0.28f, 0.17f, 0f, 355f, steps = 14)
            ),
            widthRatio = 0.54f
        ),
        'b' to GlyphDef(
            listOf(
                line(0.22f, 0.06f, 0.22f, 0.95f),
                arc(0.50f, 0.68f, 0.30f, 0.27f, 180f, -175f, steps = 16)
            ),
            widthRatio = 0.54f
        ),
        'c' to GlyphDef(
            listOf(arc(0.54f, 0.68f, 0.34f, 0.27f, -45f, -315f, steps = 16)),
            widthRatio = 0.50f
        ),
        'd' to GlyphDef(
            listOf(
                line(0.76f, 0.06f, 0.76f, 0.95f),
                arc(0.48f, 0.68f, 0.28f, 0.27f, 0f, 355f, steps = 16)
            ),
            widthRatio = 0.54f
        ),
        'e' to GlyphDef(
            listOf(
                chain(
                    line(0.18f, 0.66f, 0.82f, 0.66f),
                    arc(0.50f, 0.68f, 0.32f, 0.27f, -5f, -315f, steps = 18)
                )
            ),
            widthRatio = 0.52f
        ),
        'f' to GlyphDef(
            listOf(
                chain(
                    arc(0.62f, 0.20f, 0.20f, 0.14f, -20f, -180f, steps = 8),
                    line(0.42f, 0.20f, 0.42f, 0.95f)
                ),
                line(0.20f, 0.48f, 0.68f, 0.48f)
            ),
            widthRatio = 0.42f
        ),
        'g' to GlyphDef(
            listOf(
                arc(0.48f, 0.66f, 0.28f, 0.25f, 0f, 355f, steps = 16),
                chain(
                    line(0.76f, 0.42f, 0.76f, 0.98f),
                    arc(0.48f, 0.98f, 0.28f, 0.16f, 0f, 155f, steps = 10)
                )
            ),
            widthRatio = 0.54f
        ),
        'h' to GlyphDef(
            listOf(
                line(0.22f, 0.06f, 0.22f, 0.95f),
                chain(
                    arc(0.50f, 0.58f, 0.28f, 0.18f, 180f, 0f, steps = 10),
                    line(0.78f, 0.58f, 0.78f, 0.95f)
                )
            ),
            widthRatio = 0.54f
        ),
        'i' to GlyphDef(
            listOf(
                line(0.50f, 0.42f, 0.50f, 0.95f),
                arc(0.50f, 0.22f, 0.06f, 0.04f, 0f, 360f, steps = 8)
            ),
            widthRatio = 0.28f
        ),
        'j' to GlyphDef(
            listOf(
                chain(
                    line(0.60f, 0.42f, 0.60f, 0.96f),
                    arc(0.40f, 0.96f, 0.20f, 0.16f, 0f, 160f, steps = 8)
                ),
                arc(0.60f, 0.22f, 0.06f, 0.04f, 0f, 360f, steps = 8)
            ),
            widthRatio = 0.34f
        ),
        'k' to GlyphDef(
            listOf(
                line(0.22f, 0.06f, 0.22f, 0.95f),
                line(0.76f, 0.42f, 0.24f, 0.70f),
                line(0.40f, 0.62f, 0.80f, 0.95f)
            ),
            widthRatio = 0.52f
        ),
        'l' to GlyphDef(
            listOf(chain(line(0.44f, 0.06f, 0.44f, 0.86f), quad(0.44f, 0.86f, 0.48f, 0.96f, 0.68f, 0.94f, steps = 5))),
            widthRatio = 0.30f
        ),
        'm' to GlyphDef(
            listOf(
                line(0.12f, 0.40f, 0.12f, 0.95f),
                chain(arc(0.32f, 0.56f, 0.20f, 0.16f, 180f, 0f, steps = 8), line(0.52f, 0.56f, 0.52f, 0.95f)),
                chain(arc(0.70f, 0.56f, 0.18f, 0.16f, 180f, 0f, steps = 8), line(0.88f, 0.56f, 0.88f, 0.95f))
            ),
            widthRatio = 0.72f
        ),
        'n' to GlyphDef(
            listOf(
                line(0.22f, 0.40f, 0.22f, 0.95f),
                chain(arc(0.50f, 0.58f, 0.28f, 0.18f, 180f, 0f, steps = 10), line(0.78f, 0.58f, 0.78f, 0.95f))
            ),
            widthRatio = 0.54f
        ),
        'o' to GlyphDef(
            listOf(arc(0.50f, 0.68f, 0.32f, 0.27f, 0f, 360f, steps = 16)),
            widthRatio = 0.54f
        ),
        'p' to GlyphDef(
            listOf(
                line(0.22f, 0.40f, 0.22f, 1.14f),
                arc(0.50f, 0.68f, 0.28f, 0.26f, 180f, -175f, steps = 16)
            ),
            widthRatio = 0.54f
        ),
        'q' to GlyphDef(
            listOf(
                arc(0.48f, 0.68f, 0.28f, 0.26f, 0f, 355f, steps = 16),
                line(0.76f, 0.42f, 0.76f, 1.14f)
            ),
            widthRatio = 0.54f
        ),
        'r' to GlyphDef(
            listOf(
                line(0.26f, 0.40f, 0.26f, 0.95f),
                arc(0.52f, 0.56f, 0.26f, 0.16f, 180f, -20f, steps = 8)
            ),
            widthRatio = 0.44f
        ),
        's' to GlyphDef(
            listOf(
                chain(
                    arc(0.50f, 0.54f, 0.28f, 0.14f, -20f, -260f, steps = 10),
                    arc(0.50f, 0.82f, 0.30f, 0.14f, -90f, 165f, steps = 10)
                )
            ),
            widthRatio = 0.50f
        ),
        't' to GlyphDef(
            listOf(
                chain(line(0.46f, 0.16f, 0.46f, 0.86f), quad(0.46f, 0.86f, 0.52f, 0.96f, 0.74f, 0.92f, steps = 5)),
                line(0.22f, 0.42f, 0.72f, 0.42f)
            ),
            widthRatio = 0.44f
        ),
        'u' to GlyphDef(
            listOf(
                chain(line(0.22f, 0.42f, 0.22f, 0.74f), arc(0.50f, 0.74f, 0.28f, 0.21f, 180f, 0f, steps = 10)),
                line(0.78f, 0.42f, 0.78f, 0.95f)
            ),
            widthRatio = 0.54f
        ),
        'v' to GlyphDef(
            listOf(chain(line(0.18f, 0.42f, 0.50f, 0.95f), line(0.50f, 0.95f, 0.82f, 0.42f))),
            widthRatio = 0.52f
        ),
        'w' to GlyphDef(
            listOf(
                chain(
                    line(0.10f, 0.42f, 0.30f, 0.95f),
                    line(0.30f, 0.95f, 0.50f, 0.56f),
                    line(0.50f, 0.56f, 0.70f, 0.95f),
                    line(0.70f, 0.95f, 0.90f, 0.42f)
                )
            ),
            widthRatio = 0.68f
        ),
        'x' to GlyphDef(
            listOf(
                quad(0.20f, 0.42f, 0.50f, 0.68f, 0.80f, 0.95f, steps = 6),
                quad(0.80f, 0.42f, 0.50f, 0.68f, 0.20f, 0.95f, steps = 6)
            ),
            widthRatio = 0.52f
        ),
        'y' to GlyphDef(
            listOf(
                chain(line(0.20f, 0.42f, 0.50f, 0.90f)),
                quad(0.80f, 0.42f, 0.54f, 0.90f, 0.26f, 1.16f, steps = 10)
            ),
            widthRatio = 0.52f
        ),
        'z' to GlyphDef(
            listOf(
                chain(
                    line(0.18f, 0.44f, 0.82f, 0.44f),
                    line(0.82f, 0.44f, 0.18f, 0.95f),
                    line(0.18f, 0.95f, 0.82f, 0.95f)
                )
            ),
            widthRatio = 0.52f
        )
    )

    /**
     * Generates AndroidX Ink Strokes representing [text] starting at ([startX], [topY])
     * scaled gracefully to [charHeight].
     */
    fun createStrokesForText(
        text: String,
        startX: Float,
        topY: Float,
        charHeight: Float,
        brush: Brush
    ): List<Stroke> {
        val h = charHeight.coerceIn(38f, 180f)
        val result = mutableListOf<Stroke>()
        var cursorX = startX

        for (rawCh in text) {
            val ch = rawCh.lowercaseChar()
            if (ch == ' ') {
                cursorX += h * 0.28f
                continue
            }
            val glyph = glyphMap[ch]
            if (glyph == null) {
                cursorX += h * 0.34f
                continue
            }
            val charWidth = h * glyph.widthRatio
            for (poly in glyph.polylines) {
                if (poly.size < 2) continue
                val points = poly.map { (nx, ny) ->
                    Offset(cursorX + nx * charWidth, topY + ny * h)
                }
                result.add(ShapeRecognizer.createStrokeFromPoints(points, brush))
            }
            cursorX += charWidth + h * 0.12f
        }
        return result
    }
}
