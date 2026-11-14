package com.lernspace.app.core.math

import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Recognition match containing spatial info for a detected '=' sign.
 */
data class EqualsSignMatch(
    val bounds: Rect,
    val equalsStrokes: List<Stroke>,
    val rightEdgeX: Float,
    val centerY: Float,
    val baselineY: Float
)

/**
 * Result bundle for real-time HMER (Handwritten Math Expression Recognition)
 * triggered by writing '=' on canvas.
 */
data class MathAutoSolveResult(
    val equalsMatch: EqualsSignMatch,
    val expressionStrokes: List<Stroke>,
    val recognizedLatex: String,
    val solution: MathSolution,
    val resultText: String,
    val insertX: Float,
    val insertY: Float
)

/**
 * Zero-allocation recognizer for '=' sign gestures and spatial line clustering
 * of handwritten mathematical expressions (HMER - Apple Math Notes style).
 */
object MathEqualsRecognizer {

    private const val TAG = "MathEquals"
    private const val MIN_STROKE_POINTS = 2
    private const val MAX_EQUALS_WIDTH = 220f
    private const val MIN_EQUALS_WIDTH = 8f
    private const val MAX_EQUALS_HEIGHT = 85f
    private const val MIN_ASPECT_RATIO = 1.15f // width / height for a horizontal dash
    private const val MAX_GAP_BETWEEN_BARS = 85f
    private const val MIN_GAP_BETWEEN_BARS = 4f

    /**
     * Checks if the most recent stroke(s) form an '=' sign.
     * Looks at the last stroke and the one before it.
     */
    fun detectEqualsSign(allStrokes: List<Stroke>): EqualsSignMatch? {
        if (allStrokes.isEmpty()) return null

        val lastStroke = allStrokes.last()
        val lastBounds = getStrokeBounds(lastStroke) ?: return null

        // ── Case 1: Two-stroke '=' (most common: two parallel horizontal bars) ──
        if (allStrokes.size >= 2) {
            val prevStroke = allStrokes[allStrokes.size - 2]
            val prevBounds = getStrokeBounds(prevStroke)
            if (prevBounds != null) {
                val match = evaluateTwoStrokeEquals(prevStroke, prevBounds, lastStroke, lastBounds)
                if (match != null) {
                    Log.d(TAG, "Detected 2-stroke '=' at ${match.bounds}")
                    return match
                }
            }

            // Fallback: check if the equals sign was drawn right before a tiny micro-tap / palm touch
            if (allStrokes.size >= 3) {
                val penultStroke = allStrokes[allStrokes.size - 3]
                val penultBounds = getStrokeBounds(penultStroke)
                if (penultBounds != null && prevBounds != null && lastBounds.width < 12f && lastBounds.height < 12f) {
                    val match2 = evaluateTwoStrokeEquals(penultStroke, penultBounds, prevStroke, prevBounds)
                    if (match2 != null) {
                        Log.d(TAG, "Detected 2-stroke '=' with trailing noise tap at ${match2.bounds}")
                        return match2
                    }
                }
            }
        }

        // ── Case 2: Single continuous stroke '=' (drawn in one motion) ──
        val singleMatch = evaluateSingleStrokeEquals(lastStroke, lastBounds)
        if (singleMatch != null) {
            Log.d(TAG, "Detected single-stroke '=' at ${singleMatch.bounds}")
            return singleMatch
        }

        return null
    }

    /**
     * Evaluates whether two consecutive strokes form an '=' sign.
     */
    private fun evaluateTwoStrokeEquals(
        s1: Stroke,
        b1: Rect,
        s2: Stroke,
        b2: Rect
    ): EqualsSignMatch? {
        val w1 = b1.width
        val h1 = b1.height
        val w2 = b2.width
        val h2 = b2.height

        // Check if both strokes are roughly horizontal dashes
        if (w1 !in MIN_EQUALS_WIDTH..MAX_EQUALS_WIDTH || w2 !in MIN_EQUALS_WIDTH..MAX_EQUALS_WIDTH) return null
        if (h1 > MAX_EQUALS_HEIGHT || h2 > MAX_EQUALS_HEIGHT) return null

        val aspect1 = w1 / max(h1, 1f)
        val aspect2 = w2 / max(h2, 1f)
        if (aspect1 < 1.05f || aspect2 < 1.05f) return null

        // Check length similarity
        val lengthRatio = min(w1, w2) / max(w1, w2)
        if (lengthRatio < 0.28f) return null

        // Check horizontal overlap
        val overlapLeft = max(b1.left, b2.left)
        val overlapRight = min(b1.right, b2.right)
        val overlapWidth = overlapRight - overlapLeft
        val minOverlapNeeded = min(w1, w2) * 0.20f
        if (overlapWidth < minOverlapNeeded) return null

        // Check vertical gap between the centers
        val centerGapY = abs(b1.center.y - b2.center.y)
        if (centerGapY !in MIN_GAP_BETWEEN_BARS..95f) return null

        // Combined bounds for the '=' sign
        val combinedBounds = Rect(
            left = min(b1.left, b2.left),
            top = min(b1.top, b2.top),
            right = max(b1.right, b2.right),
            bottom = max(b1.bottom, b2.bottom)
        )

        return EqualsSignMatch(
            bounds = combinedBounds,
            equalsStrokes = listOf(s1, s2),
            rightEdgeX = combinedBounds.right,
            centerY = combinedBounds.center.y,
            baselineY = combinedBounds.bottom
        )
    }

    /**
     * Evaluates whether a single stroke represents an '=' sign drawn without lifting the pen.
     */
    private fun evaluateSingleStrokeEquals(stroke: Stroke, bounds: Rect): EqualsSignMatch? {
        val w = bounds.width
        val h = bounds.height
        if (w !in MIN_EQUALS_WIDTH..MAX_EQUALS_WIDTH || h !in MIN_GAP_BETWEEN_BARS..MAX_EQUALS_HEIGHT) return null
        if (w / max(h, 1f) !in 1.0f..4.0f) return null

        val inputs = stroke.inputs
        if (inputs.size < 6) return null

        // Check for horizontal reversals (drawn right, left/down, right)
        var rightMoves = 0
        val temp = StrokeInput()
        inputs.populate(0, temp)
        var lastX = temp.x

        for (i in 1 until inputs.size) {
            inputs.populate(i, temp)
            val dx = temp.x - lastX
            if (dx > 2.5f) rightMoves++
            lastX = temp.x
        }

        if (rightMoves < 3) return null

        return EqualsSignMatch(
            bounds = bounds,
            equalsStrokes = listOf(stroke),
            rightEdgeX = bounds.right,
            centerY = bounds.center.y,
            baselineY = bounds.bottom
        )
    }

    /**
     * Gathers all handwritten strokes on the same line to the LEFT of the '=' sign.
     */
    fun collectExpressionStrokes(
        allStrokes: List<Stroke>,
        equalsMatch: EqualsSignMatch
    ): List<Stroke> {
        val equalsLeft = equalsMatch.bounds.left
        val centerY = equalsMatch.centerY
        val lineTolerance = max(equalsMatch.bounds.height * 4.0f, 160f)

        val candidateStrokes = mutableListOf<Pair<Stroke, Rect>>()

        for (s in allStrokes) {
            if (equalsMatch.equalsStrokes.contains(s)) continue
            val b = getStrokeBounds(s) ?: continue

            // Must be strictly to the left of the '=' sign
            if (b.right > equalsLeft + 8f) continue

            // Must intersect the horizontal line band of the equation
            val strokeCenterY = b.center.y
            if (abs(strokeCenterY - centerY) <= lineTolerance) {
                candidateStrokes.add(s to b)
            }
        }

        if (candidateStrokes.isEmpty()) return emptyList()

        // Sort candidates from right to left (closest to '=' first)
        candidateStrokes.sortByDescending { it.second.right }

        val expressionStrokes = mutableListOf<Stroke>()
        var lastRightX = equalsLeft

        for ((stroke, b) in candidateStrokes) {
            val gap = lastRightX - b.right
            // Stop if there is a large horizontal blank space (> 350px) indicating end of equation
            if (gap > 350f) break

            expressionStrokes.add(stroke)
            lastRightX = min(lastRightX, b.left)
        }

        // Return strokes in normal left-to-right order
        val result = expressionStrokes.reversed()
        Log.d(TAG, "Collected ${result.size} expression strokes to the left of '='")
        return result
    }

    private fun getStrokeBounds(stroke: Stroke): Rect? {
        val inputs = stroke.inputs
        val count = inputs.size
        if (count < MIN_STROKE_POINTS) return null

        val temp = StrokeInput()
        inputs.populate(0, temp)
        var minX = temp.x
        var maxX = temp.x
        var minY = temp.y
        var maxY = temp.y

        for (i in 1 until count) {
            inputs.populate(i, temp)
            minX = min(minX, temp.x)
            maxX = max(maxX, temp.x)
            minY = min(minY, temp.y)
            maxY = max(maxY, temp.y)
        }

        return Rect(minX, minY, maxX, maxY)
    }
}
