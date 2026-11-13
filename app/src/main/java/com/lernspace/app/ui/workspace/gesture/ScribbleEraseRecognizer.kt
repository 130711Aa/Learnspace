package com.lernspace.app.ui.workspace.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance, zero-allocation recognizer for "Scribble to Erase" gestures.
 * Detects deliberate scratch-out / back-and-forth zigzag motions over existing handwriting or text.
 */
object ScribbleEraseRecognizer {

    private const val MIN_RAW_INPUTS = 10
    private const val MIN_RESAMPLED_POINTS = 6
    private const val RESAMPLE_STEP_DISTANCE = 5.0f // px
    private const val MIN_DIAGONAL = 14.0f // px
    private const val MIN_TOTAL_LENGTH = 50.0f // px
    private const val MIN_PATH_DENSITY = 2.4f // totalLength / diagonal
    private const val MIN_REVERSALS = 3 // Minimum direction reversals

    data class ScribbleResult(
        val bounds: Rect,
        val totalLength: Float,
        val reversalCount: Int
    )

    /**
     * Checks if a newly finished [Stroke] represents a scribble / scratch-out gesture.
     * Returns [ScribbleResult] if it qualifies, or `null` if it is a regular drawing/writing stroke.
     */
    fun analyzeStroke(stroke: Stroke): ScribbleResult? {
        val inputs = stroke.inputs
        val count = inputs.size
        if (count < MIN_RAW_INPUTS) return null

        val temp = StrokeInput()
        val resampled = ArrayList<Offset>(count / 2)

        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var totalLength = 0f

        inputs.populate(0, temp)
        var lastX = temp.x
        var lastY = temp.y
        resampled.add(Offset(lastX, lastY))
        minX = min(minX, lastX)
        maxX = max(maxX, lastX)
        minY = min(minY, lastY)
        maxY = max(maxY, lastY)

        for (i in 1 until count) {
            inputs.populate(i, temp)
            val cx = temp.x
            val cy = temp.y
            minX = min(minX, cx)
            maxX = max(maxX, cx)
            minY = min(minY, cy)
            maxY = max(maxY, cy)

            val dist = hypot(cx - lastX, cy - lastY)
            if (dist >= RESAMPLE_STEP_DISTANCE) {
                totalLength += dist
                resampled.add(Offset(cx, cy))
                lastX = cx
                lastY = cy
            }
        }

        if (resampled.size < MIN_RESAMPLED_POINTS) return null

        val width = maxX - minX
        val height = maxY - minY
        val diagonal = hypot(width, height)

        if (diagonal < MIN_DIAGONAL || totalLength < MIN_TOTAL_LENGTH) return null

        val density = totalLength / diagonal
        if (density < MIN_PATH_DENSITY) return null

        // Count directional reversals and measure spatial overlap
        var reversals = 0
        var sweepDist = 0f
        var sweepDirX = 0f
        var sweepDirY = 0f
        var hasInitialDir = false
        var backwardOverlapCount = 0

        for (i in 0 until resampled.size - 1) {
            val p1 = resampled[i]
            val p2 = resampled[i + 1]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val segLen = hypot(dx, dy)
            if (segLen < 1e-4f) continue

            val udx = dx / segLen
            val udy = dy / segLen

            if (!hasInitialDir) {
                sweepDirX = udx
                sweepDirY = udy
                sweepDist = segLen
                hasInitialDir = true
            } else {
                val dot = (udx * sweepDirX) + (udy * sweepDirY)
                if (dot <= -0.20f && sweepDist >= 8.0f) {
                    // Pen sharply reversed direction (angle > 102 degrees) after significant travel
                    reversals++
                    backwardOverlapCount++
                    sweepDirX = udx
                    sweepDirY = udy
                    sweepDist = segLen
                } else {
                    sweepDist += segLen
                    // Gradually blend sweep direction for slight curved arcs
                    sweepDirX = sweepDirX * 0.7f + udx * 0.3f
                    sweepDirY = sweepDirY * 0.7f + udy * 0.3f
                    val blendLen = hypot(sweepDirX, sweepDirY)
                    if (blendLen > 1e-4f) {
                        sweepDirX /= blendLen
                        sweepDirY /= blendLen
                    }
                }
            }
        }

        // A valid scribble must have at least 3 distinct direction reversals and high density,
        // or >= 4 reversals for standard scratch-outs.
        val isValidScribble = (reversals >= 4 && density >= 2.4f) ||
            (reversals >= 3 && density >= 3.0f && backwardOverlapCount >= 2)

        if (!isValidScribble) return null

        return ScribbleResult(
            bounds = Rect(minX, minY, maxX, maxY),
            totalLength = totalLength,
            reversalCount = reversals
        )
    }

    /**
     * Determines whether the given stroke should be treated as a scribble gesture.
     */
    fun isScribble(stroke: Stroke): Boolean {
        return analyzeStroke(stroke) != null
    }
}

