package com.lernspace.app.core.math

import androidx.compose.ui.geometry.Offset
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

    private const val MIN_STROKE_POINTS = 3
    private const val MAX_EQUALS_WIDTH = 180f
    private const val MIN_EQUALS_WIDTH = 12f
    private const val MAX_EQUALS_HEIGHT = 70f
    private const val MIN_ASPECT_RATIO = 1.6f // width / height for a horizontal dash
    private const val MAX_GAP_BETWEEN_BARS = 65f
    private const val MIN_GAP_BETWEEN_BARS = 6f
    private const val MAX_TIME_BETWEEN_BARS_MS = 2000L

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
                if (match != null) return match
            }
        }

        // ── Case 2: Single continuous stroke '=' (drawn in one motion) ──
        return evaluateSingleStrokeEquals(lastStroke, lastBounds)
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
        if (aspect1 < MIN_ASPECT_RATIO || aspect2 < MIN_ASPECT_RATIO) return null

        // Check length similarity
        val lengthRatio = min(w1, w2) / max(w1, w2)
        if (lengthRatio < 0.42f) return null

        // Check horizontal overlap
        val overlapLeft = max(b1.left, b2.left)
        val overlapRight = min(b1.right, b2.right)
        val overlapWidth = overlapRight - overlapLeft
        val minOverlapNeeded = min(w1, w2) * 0.45f
        if (overlapWidth < minOverlapNeeded) return null

        // Check vertical gap between the centers
        val centerGapY = abs(b1.center.y - b2.center.y)
        if (centerGapY !in MIN_GAP_BETWEEN_BARS..MAX_GAP_BETWEEN_BARS) return null

        // Check timing between strokes
        val t1 = getStrokeEndTime(s1)
        val t2 = getStrokeStartTime(s2)
        if (t1 > 0 && t2 > 0 && abs(t2 - t1) > MAX_TIME_BETWEEN_BARS_MS) return null

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
        if (w / max(h, 1f) !in 1.1f..3.8f) return null

        // Must have enough points to form a loop/zig-zag (e.g. top bar -> diagonal down -> bottom bar)
        val inputs = stroke.inputs
        if (inputs.size < 8) return null

        // Check for horizontal reversals (drawn right, left/down, right)
        var rightMoves = 0
        val temp = StrokeInput()
        inputs.populate(0, temp)
        var lastX = temp.x

        for (i in 1 until inputs.size) {
            inputs.populate(i, temp)
            val dx = temp.x - lastX
            if (dx > 3.0f) rightMoves++
            lastX = temp.x
        }

        if (rightMoves < 4) return null

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
        val lineTolerance = max(equalsMatch.bounds.height * 2.8f, 75f)

        val candidateStrokes = mutableListOf<Pair<Stroke, Rect>>()

        for (s in allStrokes) {
            if (equalsMatch.equalsStrokes.contains(s)) continue
            val b = getStrokeBounds(s) ?: continue

            // Must be strictly to the left of the '=' sign
            if (b.right > equalsLeft + 12f) continue

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
            // Stop if there is a large horizontal blank space (> 280px) indicating end of equation
            if (gap > 280f) break

            expressionStrokes.add(stroke)
            lastRightX = min(lastRightX, b.left)
        }

        // Return strokes in normal left-to-right order
        return expressionStrokes.reversed()
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

    private fun getStrokeStartTime(stroke: Stroke): Long {
        if (stroke.inputs.size == 0) return 0L
        val temp = StrokeInput()
        stroke.inputs.populate(0, temp)
        return temp.elapsedTimeMillis
    }

    private fun getStrokeEndTime(stroke: Stroke): Long {
        val size = stroke.inputs.size
        if (size == 0) return 0L
        val temp = StrokeInput()
        stroke.inputs.populate(size - 1, temp)
        return temp.elapsedTimeMillis
    }
}

