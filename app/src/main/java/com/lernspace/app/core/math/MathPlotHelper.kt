package com.lernspace.app.core.math

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Data bundle representing a sampled mathematical function plot.
 */
data class MathPlotData(
    val functionExpression: String,
    val variable: Char = 'x',
    val xMin: Float = -10f,
    val xMax: Float = 10f,
    val yMin: Float = -10f,
    val yMax: Float = 10f,
    val points: List<Offset>
)

/**
 * High-performance 2D Function Graph Plotter for Learnspace.
 *
 * Capabilities:
 * - Detects functions like "y = x^2 - 4", "f(x) = sin(x)", "y = 2x + 1", "x^3 - 3x"
 * - Samples points across the domain
 * - Renders a publication-quality Cartesian coordinate graph Bitmap (Grid, Axes, Curve, Labels)
 * - Directly insertable into note pages as an ImageElement
 */
object MathPlotHelper {

    /**
     * Checks if a math/LaTeX string represents a plotable function of 'x'.
     */
    fun isPlottableFunction(raw: String): Boolean {
        val clean = MathSolver.cleanLatexForSolver(raw).lowercase()
        val hasVar = clean.contains('x')
        val isEquation = clean.startsWith("y=") || clean.startsWith("y =") ||
                clean.startsWith("f(x)=") || clean.startsWith("f(x) =") ||
                (hasVar && !clean.contains("=") && !clean.contains("\\"))
        return isEquation
    }

    /**
     * Extracts the pure f(x) expression string for evaluation:
     * e.g. "y = x^2 - 4" -> "x^2 - 4"
     * e.g. "f(x) = 2x + 1" -> "2x + 1"
     */
    fun extractFunctionBody(raw: String): String {
        var clean = MathSolver.cleanLatexForSolver(raw)
        if (clean.contains("=")) {
            val parts = clean.split("=")
            clean = if (parts[0].trim().lowercase() in listOf("y", "f(x)", "g(x)")) {
                parts[1].trim()
            } else {
                parts[0].trim()
            }
        }
        return clean
    }

    /**
     * Samples the function curve across domain [xMin, xMax].
     */
    fun sampleFunction(
        functionBody: String,
        xMin: Float = -10f,
        xMax: Float = 10f,
        steps: Int = 300
    ): MathPlotData? {
        val stepSize = (xMax - xMin) / steps.toFloat()
        val points = mutableListOf<Offset>()

        var computedYMin = Float.POSITIVE_INFINITY
        var computedYMax = Float.NEGATIVE_INFINITY

        for (i in 0..steps) {
            val x = xMin + i * stepSize
            val yVal = evaluateAt(functionBody, x.toDouble())
            if (yVal != null && !yVal.isNaN() && !yVal.isInfinite() && abs(yVal) < 200.0) {
                val y = yVal.toFloat()
                points.add(Offset(x, y))
                computedYMin = min(computedYMin, y)
                computedYMax = max(computedYMax, y)
            }
        }

        if (points.size < 10) return null

        val yMargin = max((computedYMax - computedYMin) * 0.15f, 2f)
        val finalYMin = (computedYMin - yMargin).coerceIn(-100f, 100f)
        val finalYMax = (computedYMax + yMargin).coerceIn(-100f, 100f)

        return MathPlotData(
            functionExpression = functionBody,
            variable = 'x',
            xMin = xMin,
            xMax = xMax,
            yMin = finalYMin,
            yMax = finalYMax,
            points = points
        )
    }

    private fun evaluateAt(expr: String, xVal: Double): Double? {
        // Substitute x with value, handling e.g. 2x -> 2*(val)
        val substituted = expr.replace(Regex("(\\d)(x)", RegexOption.IGNORE_CASE), "$1*($xVal)")
            .replace("x", "($xVal)", ignoreCase = true)
        return MathSolver.evaluateExpression(substituted)
    }

    /**
     * Renders a clean Cartesian graph Bitmap for the note canvas.
     */
    fun renderGraphBitmap(
        plotData: MathPlotData,
        width: Int = 640,
        height: Int = 480,
        isDarkTheme: Boolean = false
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Palette
        val bgColor = if (isDarkTheme) Color.rgb(24, 28, 36) else Color.rgb(252, 253, 255)
        val gridColor = if (isDarkTheme) Color.argb(45, 255, 255, 255) else Color.argb(35, 15, 23, 42)
        val axisColor = if (isDarkTheme) Color.rgb(148, 163, 184) else Color.rgb(71, 85, 105)
        val curveColor = if (isDarkTheme) Color.rgb(56, 189, 248) else Color.rgb(2, 132, 199)
        val textColor = if (isDarkTheme) Color.rgb(226, 232, 240) else Color.rgb(15, 23, 42)

        canvas.drawColor(bgColor)

        val padding = 44f
        val plotW = width - padding * 2f
        val plotH = height - padding * 2f

        val xRange = (plotData.xMax - plotData.xMin).coerceAtLeast(1f)
        val yRange = (plotData.yMax - plotData.yMin).coerceAtLeast(1f)

        fun toScreenX(worldX: Float): Float = padding + ((worldX - plotData.xMin) / xRange) * plotW
        fun toScreenY(worldY: Float): Float = height - padding - ((worldY - plotData.yMin) / yRange) * plotH

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor
            strokeWidth = 2.4f
            style = Paint.Style.STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }

        val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = curveColor
            strokeWidth = 3.8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Draw Grid Lines
        for (gx in -10..10 step 2) {
            val sx = toScreenX(gx.toFloat())
            if (sx in padding..(width - padding)) {
                canvas.drawLine(sx, padding, sx, height - padding, gridPaint)
            }
        }
        val yStep = max(2, (yRange / 8).toInt())
        var gy = ((plotData.yMin / yStep).toInt()) * yStep
        while (gy <= plotData.yMax) {
            val sy = toScreenY(gy.toFloat())
            if (sy in padding..(height - padding)) {
                canvas.drawLine(padding, sy, width - padding, sy, gridPaint)
            }
            gy += yStep
        }

        // Draw Axes X and Y
        val originX = toScreenX(0f).coerceIn(padding, width - padding)
        val originY = toScreenY(0f).coerceIn(padding, height - padding)

        // X-axis (horizontal)
        canvas.drawLine(padding, originY, width - padding, originY, axisPaint)
        // Y-axis (vertical)
        canvas.drawLine(originX, padding, originX, height - padding, axisPaint)

        // Draw Axis Labels
        canvas.drawText("x", width - padding + 16f, originY + 6f, textPaint)
        canvas.drawText("y", originX, padding - 12f, textPaint)

        // Draw Function Curve
        val curvePath = Path()
        var isFirst = true

        for (pt in plotData.points) {
            val sx = toScreenX(pt.x)
            val sy = toScreenY(pt.y)
            if (sx in (padding - 10f)..(width - padding + 10f) && sy in (padding - 10f)..(height - padding + 10f)) {
                if (isFirst) {
                    curvePath.moveTo(sx, sy)
                    isFirst = false
                } else {
                    curvePath.lineTo(sx, sy)
                }
            } else {
                isFirst = true
            }
        }

        canvas.drawPath(curvePath, curvePaint)

        // Draw Title / Function Equation Badge
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("f(x) = ${plotData.functionExpression}", padding + 8f, padding + 20f, titlePaint)

        return bmp
    }
}
