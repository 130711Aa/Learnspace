package com.lernspace.app.core.math

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Categorization of critical mathematical key points on a 2D function or circle graph.
 */
enum class KeyPointType(val displayName: String, val badgeColorArgb: Int) {
    X_INTERCEPT("Titik Potong X (Akar)", 0xFF2563EB.toInt()),    // Blue
    Y_INTERCEPT("Titik Potong Y", 0xFF059669.toInt()),            // Emerald Green
    VERTEX_MIN("Titik Puncak Min", 0xFF7C3AED.toInt()),          // Violet
    VERTEX_MAX("Titik Puncak Maks", 0xFFDC2626.toInt()),         // Red
    INFLECTION("Titik Belok", 0xFF0891B2.toInt()),                // Cyan
    CIRCLE_CENTER("Pusat Lingkaran P(h, k)", 0xFF2563EB.toInt()),// Blue
    CIRCLE_EXTREMA("Titik Ekstrem Lingkaran", 0xFFD97706.toInt()) // Amber
}

/**
 * Represents a key mathematical coordinate on the graph.
 */
data class KeyPoint(
    val x: Float,
    val y: Float,
    val type: KeyPointType,
    val label: String,
    val detail: String
)

/**
 * Geometric parameters of a parsed 2D circle equation.
 */
data class CircleData(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val rawEquation: String,
    val standardEquation: String,
    val generalEquation: String,
    val diameter: Float = radius * 2f,
    val circumference: Float = (2 * Math.PI * radius).toFloat(),
    val area: Float = (Math.PI * radius * radius).toFloat()
)

/**
 * Linear equation parameters (ax + by + c = 0, y = mx + k, or vertical line x = k).
 */
data class LinearEquationData(
    val a: Float,
    val b: Float,
    val c: Float,
    val isVertical: Boolean,
    val verticalX: Float = 0f,
    val slope: Float = 0f,
    val yIntercept: Float = 0f,
    val standardEquation: String,
    val functionExpr: String
)

/**
 * Coordinate of an intersection point between two curves.
 */
data class IntersectionPoint(
    val x: Float,
    val y: Float,
    val label: String = ""
)

/**
 * Generic curve specification used for intersection detection and multi-plotting.
 */
data class CurvePlotDefinition(
    val id: String = "",
    val cleanExpr: String = "",
    val isCircle: Boolean = false,
    val circleData: CircleData? = null,
    val isVerticalLine: Boolean = false,
    val verticalLineX: Float = 0f
)

/**
 * Individual curve plot item for multi-graph plotting on the same plane.
 */
data class MultiPlotCurve(
    val label: String,
    val colorArgb: Int,
    val points: List<Offset> = emptyList(),
    val circleData: CircleData? = null,
    val keyPoints: List<KeyPoint> = emptyList(),
    val isVerticalLine: Boolean = false,
    val verticalLineX: Float = 0f
)

/**
 * Data bundle representing a sampled mathematical function plot or circle plot.
 */
data class MathPlotData(
    val functionExpression: String = "",
    val variable: Char = 'x',
    val xMin: Float = -10f,
    val xMax: Float = 10f,
    val yMin: Float = -10f,
    val yMax: Float = 10f,
    val points: List<Offset> = emptyList(),
    val keyPoints: List<KeyPoint> = emptyList(),
    val circleData: CircleData? = null,
    val multiCurves: List<MultiPlotCurve> = emptyList(),
    val intersections: List<IntersectionPoint> = emptyList(),
    val isVerticalLine: Boolean = false,
    val verticalLineX: Float = 0f,
    val isPiGridMode: Boolean = false,
    val definiteIntegral: DefiniteIntegralData? = null,
    val definiteIntegrals: List<DefiniteIntegralData> = emptyList()
) {
    val allDefiniteIntegrals: List<DefiniteIntegralData>
        get() = if (definiteIntegrals.isNotEmpty()) definiteIntegrals
                else definiteIntegral?.let { listOf(it) } ?: emptyList()
}

enum class RiemannType(val label: String) {
    SMOOTH("Arsiran Kontinu"),
    MIDPOINT("Riemann Tengah"),
    TRAPEZOIDAL("Trapesium"),
    LEFT("Riemann Kiri"),
    RIGHT("Riemann Kanan")
}

data class DefiniteIntegralData(
    val functionExpr: String,
    val displayLabel: String = "",
    val lowerBoundA: Float = 0f,
    val upperBoundB: Float = 2f,
    val integralValue: Double = 0.0,
    val totalArea: Double = 0.0,
    val colorArgb: Int = 0xFF2563EB.toInt(),
    val riemannType: RiemannType = RiemannType.SMOOTH,
    val partitionCount: Int = 12
)

data class DefiniteIntegralResult(
    val netIntegral: Double,
    val totalArea: Double,
    val isSuccess: Boolean = true
)

data class ParsedDefiniteIntegral(
    val functionExpr: String,
    val lowerBound: Float = 0f,
    val upperBound: Float = 2f
)

/**
 * High-performance, zero-allocation compiled mathematical expression tree.
 * Allows evaluating f(x) hundreds of thousands of times per second with zero garbage collection overhead.
 */
fun interface CompiledExpr {
    fun eval(x: Double): Double
}

/**
 * High-performance 2D Function Graph Plotter & Mathematical Analyzer for Learnspace.
 *
 * Capabilities:
 * - Detects functions like "y = x^2 - 4", "f(x) = -2x^2 + 4x + 6", "sin(x)", "x^3 - 3x"
 * - Accurately parses and visualizes Circle Equations:
 *   * Standard form: (x - h)^2 + (y - k)^2 = r^2
 *   * General form: x^2 + y^2 + Ax + By + C = 0
 * - High-speed Compiled AST evaluator: 100% lag-free pan & zoom navigation
 * - Computes critical key points (Roots, Intercepts, Vertex, Circle Center, Radius)
 * - Renders ultra-high-resolution, crystal-crisp Cartesian coordinate graph Bitmaps for note canvas
 */
object MathPlotHelper {

    /**
     * Checks if a math/LaTeX string represents a plottable equation (circle, linear equation, 2D function of 'x', or definite integral).
     */
    fun isPlottableFunction(raw: String): Boolean {
        if (parseDefiniteIntegral(raw) != null) return true
        if (parseCircle(raw) != null) return true
        if (parseLinearEquation(raw) != null) return true

        val clean = cleanLatexForPlot(raw).lowercase().replace(" ", "")
        val hasVar = clean.contains('x')
        val isEquation = clean.startsWith("y=") || clean.startsWith("f(x)=") ||
                clean.startsWith("g(x)=") ||
                (hasVar && !clean.contains("=") && !clean.contains("\\"))
        return isEquation
    }

    /**
     * Parses definite integral syntax from LaTeX (\int_{a}^{b} f(x) dx) or standard notations (∫[a, b] f(x) dx, int(f(x), a, b)).
     */
    fun parseDefiniteIntegral(raw: String): ParsedDefiniteIntegral? {
        val trimmed = raw.trim()
        if (!trimmed.contains("int", ignoreCase = true) && !trimmed.contains("∫")) {
            return null
        }

        // 1. LaTeX format: \int_{a}^{b} f(x) dx or \int_a^b f(x) dx
        val latexRegex = Regex("""\\int(?:_\{?([+-]?\d*\.?\d+)\}?)?(?:\^\{?([+-]?\d*\.?\d+)\}?)?\s*(.*?)(?:d\s*x|$)""", RegexOption.IGNORE_CASE)
        val matchLatex = latexRegex.find(trimmed)
        if (matchLatex != null) {
            val aStr = matchLatex.groupValues[1]
            val bStr = matchLatex.groupValues[2]
            var body = matchLatex.groupValues[3].trim()
            if (body.endsWith("dx", ignoreCase = true)) {
                body = body.dropLast(2).trim()
            }
            if (body.isNotBlank()) {
                val aVal = aStr.toFloatOrNull() ?: 0f
                val bVal = bStr.toFloatOrNull() ?: 2f
                return ParsedDefiniteIntegral(
                    functionExpr = extractFunctionBody(body),
                    lowerBound = aVal,
                    upperBound = bVal
                )
            }
        }

        // 2. Unicode integral: ∫[a, b] f(x) or ∫ a..b f(x) or ∫ f(x) dx
        val unicodeRegex = Regex("""∫\s*(?:\[\s*([+-]?\d*\.?\d+)\s*,\s*([+-]?\d*\.?\d+)\s*\]|([+-]?\d*\.?\d+)\s*\.\.\s*([+-]?\d*\.?\d+))?\s*(.*?)(?:d\s*x|$)""", RegexOption.IGNORE_CASE)
        val matchUni = unicodeRegex.find(trimmed)
        if (matchUni != null) {
            val aStr = matchUni.groupValues[1].ifEmpty { matchUni.groupValues[3] }
            val bStr = matchUni.groupValues[2].ifEmpty { matchUni.groupValues[4] }
            var body = matchUni.groupValues[5].trim()
            if (body.endsWith("dx", ignoreCase = true)) {
                body = body.dropLast(2).trim()
            }
            if (body.isNotBlank()) {
                val aVal = aStr.toFloatOrNull() ?: 0f
                val bVal = bStr.toFloatOrNull() ?: 2f
                return ParsedDefiniteIntegral(
                    functionExpr = extractFunctionBody(body),
                    lowerBound = aVal,
                    upperBound = bVal
                )
            }
        }

        // 3. Functional syntax: int(f(x), a, b) or integral(f(x), a, b)
        val funcRegex = Regex("""(?:int|integral)\s*\(\s*(.*?)\s*,\s*([+-]?\d*\.?\d+)\s*,\s*([+-]?\d*\.?\d+)\s*\)""", RegexOption.IGNORE_CASE)
        val matchFunc = funcRegex.find(trimmed)
        if (matchFunc != null) {
            val body = matchFunc.groupValues[1].trim()
            val aVal = matchFunc.groupValues[2].toFloatOrNull() ?: 0f
            val bVal = matchFunc.groupValues[3].toFloatOrNull() ?: 2f
            if (body.isNotBlank()) {
                return ParsedDefiniteIntegral(
                    functionExpr = extractFunctionBody(body),
                    lowerBound = aVal,
                    upperBound = bVal
                )
            }
        }

        return null
    }

    /**
     * Extracts the core function expression body (e.g. "x^2 - 4" from "y = x^2 - 4").
     */
    fun extractFunctionBody(raw: String): String {
        val parsedInt = parseDefiniteIntegral(raw)
        if (parsedInt != null) {
            return parsedInt.functionExpr
        }

        // If it's a circle, return the standard equation representation
        val circle = parseCircle(raw)
        if (circle != null) {
            return circle.standardEquation
        }

        // If it's a linear equation (e.g. 2x + 3y = 6 or x = 3)
        val linear = parseLinearEquation(raw)
        if (linear != null) {
            return if (linear.isVertical) linear.standardEquation else linear.functionExpr
        }

        var clean = cleanLatexForPlot(raw)
        if (clean.contains("=")) {
            val parts = clean.split("=")
            clean = if (parts[0].trim().lowercase() in listOf("y", "f(x)", "g(x)")) {
                parts[1].trim()
            } else {
                parts[0].trim()
            }
        }
        return normalizeMathExpression(clean.trim())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CIRCLE EQUATION PARSER & GEOMETRY ANALYZER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses standard or general circle equations into structured CircleData.
     * Supports both fixed numerical equations and dynamic parameter variables (e.g. 'r', 'h', 'k').
     * Supported formats:
     * - (x - h)^2 + (y - k)^2 = r^2   (e.g. (x-2)^2 + (y-3)^2 = 16 or (x-h)^2 + (y-k)^2 = r^2)
     * - (x + h)^2 + (y + k)^2 = r^2   (e.g. (x+1)^2 + (y-4)^2 = 25)
     * - x^2 + y^2 = r                 (e.g. x^2 + y^2 = r, where 'r' is direct radius parameter)
     * - x^2 + y^2 = r^2               (e.g. x^2 + y^2 = 25 or x^2 + y^2 = r^2)
     * - x^2 + (y - k)^2 = r^2         (e.g. x^2 + (y-2)^2 = 9)
     * - (x - h)^2 + y^2 = r^2         (e.g. (x-3)^2 + y^2 = 4)
     * - General form: x^2 + y^2 + Ax + By + C = 0
     * - General form with RHS: x^2 + y^2 + Ax + By = D
     */
    fun parseCircle(raw: String, params: Map<Char, Float> = emptyMap()): CircleData? {
        if (raw.isBlank()) return null

        // 1. Normalize LaTeX and syntax
        var s = cleanLatexForPlot(raw).lowercase().replace(" ", "")
        s = s.replace("\\left", "").replace("\\right", "")
        s = s.replace("^{2.0}", "^2").replace("^{2}", "^2")
        s = s.replace("^{3}", "^3") // will fail circle check if cubed
        s = s.replace("{", "(").replace("}", ")")
        s = s.replace("--", "+").replace("+-", "-").replace("-+", "-")
        s = s.trim('$', ' ')

        if (!s.contains('x') || !s.contains('y') || !s.contains('=')) return null
        if (s.contains("^3") || s.contains("^4") || s.contains("z")) return null

        // Determine effective parameter map (with intelligent defaults if not yet populated)
        val detected = extractParameters(s)
        val effParams = if (detected.isNotEmpty()) {
            val map = HashMap<Char, Float>()
            for (p in detected) {
                map[p] = params[p] ?: if (p in listOf('a', 'b', 'm')) 1f else if (p == 'r') 3f else 0f
            }
            map
        } else {
            params
        }

        // Check 1: Standard Form (x - h)^2 + (y - k)^2 = R (with parameter support)
        val stdCircle = parseStandardCircle(s, raw, effParams)
        if (stdCircle != null) return stdCircle

        // Check 2: General Form x^2 + y^2 + Ax + By + C = 0 (with parameter support)
        val substituted = if (effParams.isNotEmpty()) substituteParameters(s, effParams) else s
        val genCircle = parseGeneralCircle(substituted, raw)
        if (genCircle != null) return genCircle

        return null
    }

    private fun parseStandardCircle(s: String, rawOriginal: String, params: Map<Char, Float> = emptyMap()): CircleData? {
        val parts = s.split("=")
        if (parts.size != 2) return null

        val rawLhs = parts[0]
        val rawRhs = parts[1]

        // 1. Evaluate radius from RHS
        // Case A: RHS is explicitly a single radius parameter (e.g. "= r")
        val radius: Float = if (rawRhs == "r" || rawRhs == "(r)") {
            val rVal = params['r'] ?: 3f
            abs(rVal).coerceAtLeast(0.01f)
        } else {
            // Substitute params in RHS if any
            val subRhs = if (params.isNotEmpty()) substituteParameters(rawRhs, params) else rawRhs
            val rSquared = evaluateExpression(subRhs) ?: return null
            if (rSquared <= 1e-6) return null
            sqrt(rSquared).toFloat()
        }

        // 2. Normalize and substitute LHS
        val subLhs = if (params.isNotEmpty()) substituteParameters(rawLhs, params) else rawLhs

        // Normalize substituted parentheses:
        // Replace -(number) or +(number) e.g. -(2) -> -2, +(3) -> +3
        var lhsNorm = subLhs
        lhsNorm = Regex("""([+-])\(([+-]?\d+(?:\.\d+)?)\)""").replace(lhsNorm) { mr ->
            val sign = mr.groupValues[1]
            val inner = mr.groupValues[2]
            if (sign == "-") {
                if (inner.startsWith("-")) "+${inner.drop(1)}"
                else if (inner.startsWith("+")) "-${inner.drop(1)}"
                else "-$inner"
            } else {
                if (inner.startsWith("+")) "+${inner.drop(1)}"
                else if (inner.startsWith("-")) inner
                else "+$inner"
            }
        }
        // Normalize (x-(0))^2 or (x-0)^2 or (x+0)^2 or (x)^2 -> x^2
        lhsNorm = Regex("""\(([xy])[+-]0(?:\.0+)?\)\^2""").replace(lhsNorm) { mr ->
            "${mr.groupValues[1]}^2"
        }
        lhsNorm = Regex("""\(([xy])\)\^2""").replace(lhsNorm) { mr ->
            "${mr.groupValues[1]}^2"
        }

        // Match term X: (x - h)^2 or (x + h)^2 or x^2
        var h = 0f
        var k = 0f
        var foundX = false
        var foundY = false

        // Pattern for (x - a)^2 or (x + a)^2
        val xParenMatch = Regex("""\((?:x([+-][0-9.]+))\)\^2""").find(lhsNorm)
        if (xParenMatch != null) {
            val signVal = xParenMatch.groupValues[1].toDoubleOrNull() ?: return null
            h = -signVal.toFloat() // (x - 2)^2 -> h = 2
            foundX = true
        } else if (lhsNorm.contains("x^2") || lhsNorm.contains("(x)^2")) {
            h = 0f
            foundX = true
        }

        // Pattern for (y - b)^2 or (y + b)^2
        val yParenMatch = Regex("""\((?:y([+-][0-9.]+))\)\^2""").find(lhsNorm)
        if (yParenMatch != null) {
            val signVal = yParenMatch.groupValues[1].toDoubleOrNull() ?: return null
            k = -signVal.toFloat() // (y - 3)^2 -> k = 3
            foundY = true
        } else if (lhsNorm.contains("y^2") || lhsNorm.contains("(y)^2")) {
            k = 0f
            foundY = true
        }

        if (!foundX || !foundY) return null

        // Build clean standard & general strings
        val stdStr = formatStandardCircleString(h, k, radius)
        val genStr = formatGeneralCircleString(h, k, radius)

        return CircleData(
            centerX = h,
            centerY = k,
            radius = radius,
            rawEquation = rawOriginal,
            standardEquation = stdStr,
            generalEquation = genStr
        )
    }

    private fun parseGeneralCircle(s: String, rawOriginal: String): CircleData? {
        // Form: lhs = rhs -> lhs - (rhs) = 0
        val parts = s.split("=")
        if (parts.size != 2) return null

        val leftStr = parts[0]
        val rightStr = parts[1]

        // Parse polynomial terms of lhs and rhs
        val lhsTerms = extractPolynomialTerms(leftStr) ?: return null
        val rhsTerms = extractPolynomialTerms(rightStr) ?: return null

        // Combine into single equation: coeff_left - coeff_right
        val aX2 = lhsTerms.coeffX2 - rhsTerms.coeffX2
        val aY2 = lhsTerms.coeffY2 - rhsTerms.coeffY2
        val aXY = lhsTerms.coeffXY - rhsTerms.coeffXY
        val aX = lhsTerms.coeffX - rhsTerms.coeffX
        val aY = lhsTerms.coeffY - rhsTerms.coeffY
        val aConst = lhsTerms.coeffConst - rhsTerms.coeffConst

        // Conic check for circle:
        // 1. No xy term
        // 2. coeff(x^2) == coeff(y^2) > 0
        if (abs(aXY) > 1e-5) return null
        if (abs(aX2 - aY2) > 1e-4) return null
        if (aX2 <= 1e-6) return null

        // Normalize dividing by M = coeff(x^2)
        val m = aX2
        val capA = aX / m
        val capB = aY / m
        val capC = aConst / m

        // Center: h = -A/2, k = -B/2
        val h = (-capA / 2.0).toFloat()
        val k = (-capB / 2.0).toFloat()

        // r^2 = h^2 + k^2 - C
        val rSq = (h.toDouble() * h.toDouble() + k.toDouble() * k.toDouble() - capC)
        if (rSq <= 1e-5) return null

        val radius = sqrt(rSq).toFloat()
        val stdStr = formatStandardCircleString(h, k, radius)
        val genStr = formatGeneralCircleString(h, k, radius)

        return CircleData(
            centerX = h,
            centerY = k,
            radius = radius,
            rawEquation = rawOriginal,
            standardEquation = stdStr,
            generalEquation = genStr
        )
    }

    private data class PolyTerms(
        val coeffX2: Double = 0.0,
        val coeffY2: Double = 0.0,
        val coeffXY: Double = 0.0,
        val coeffX: Double = 0.0,
        val coeffY: Double = 0.0,
        val coeffConst: Double = 0.0
    )

    private fun extractPolynomialTerms(expr: String): PolyTerms? {
        if (expr.isEmpty()) return PolyTerms()
        var cur = expr.replace("-", "+-")
        if (cur.startsWith("+-")) cur = "-" + cur.substring(2)
        val tokens = cur.split("+").filter { it.isNotBlank() }

        var cx2 = 0.0
        var cy2 = 0.0
        var cxy = 0.0
        var cx = 0.0
        var cy = 0.0
        var cconst = 0.0

        for (tok in tokens) {
            val t = tok.trim()
            when {
                t.contains("xy") || t.contains("yx") -> {
                    val coeffStr = t.replace("xy", "").replace("yx", "").replace("*", "")
                    val coeff = parseCoeff(coeffStr) ?: return null
                    cxy += coeff
                }
                t.contains("x^2") || t.contains("x*x") -> {
                    val coeffStr = t.replace("x^2", "").replace("x*x", "").replace("*", "")
                    val coeff = parseCoeff(coeffStr) ?: return null
                    cx2 += coeff
                }
                t.contains("y^2") || t.contains("y*y") -> {
                    val coeffStr = t.replace("y^2", "").replace("y*y", "").replace("*", "")
                    val coeff = parseCoeff(coeffStr) ?: return null
                    cy2 += coeff
                }
                t.contains("x") -> {
                    val coeffStr = t.replace("x", "").replace("*", "")
                    val coeff = parseCoeff(coeffStr) ?: return null
                    cx += coeff
                }
                t.contains("y") -> {
                    val coeffStr = t.replace("y", "").replace("*", "")
                    val coeff = parseCoeff(coeffStr) ?: return null
                    cy += coeff
                }
                else -> {
                    val v = evaluateExpression(t) ?: return null
                    cconst += v
                }
            }
        }
        return PolyTerms(cx2, cy2, cxy, cx, cy, cconst)
    }

    private fun parseCoeff(s: String): Double? {
        if (s.isEmpty() || s == "+") return 1.0
        if (s == "-") return -1.0
        val cleaned = if (s.startsWith("/")) "1$s" else if (s.startsWith("-/")) "-1" + s.substring(1) else if (s.startsWith("+/")) "1" + s.substring(1) else s
        return evaluateExpression(cleaned)
    }

    private fun formatStandardCircleString(h: Float, k: Float, r: Float): String {
        val xPart = when {
            abs(h) < 0.001f -> "x^2"
            h > 0 -> "(x - ${formatNumber(h)})^2"
            else -> "(x + ${formatNumber(-h)})^2"
        }
        val yPart = when {
            abs(k) < 0.001f -> "y^2"
            k > 0 -> "(y - ${formatNumber(k)})^2"
            else -> "(y + ${formatNumber(-k)})^2"
        }
        val rSq = r * r
        return "$xPart + $yPart = ${formatNumber(rSq)}"
    }

    private fun formatGeneralCircleString(h: Float, k: Float, r: Float): String {
        val a = -2f * h
        val b = -2f * k
        val c = h * h + k * k - r * r

        val sb = StringBuilder("x^2 + y^2")
        if (abs(a) > 0.001f) {
            sb.append(if (a > 0) " + ${formatNumber(a)}x" else " - ${formatNumber(-a)}x")
        }
        if (abs(b) > 0.001f) {
            sb.append(if (b > 0) " + ${formatNumber(b)}y" else " - ${formatNumber(-b)}y")
        }
        if (abs(c) > 0.001f) {
            sb.append(if (c > 0) " + ${formatNumber(c)}" else " - ${formatNumber(-c)}")
        }
        sb.append(" = 0")
        return sb.toString()
    }

    /**
     * Extracts mathematical key points for a circle (Center, Extrema, Intercepts).
     */
    fun getCircleKeyPoints(circle: CircleData): List<KeyPoint> {
        val res = mutableListOf<KeyPoint>()

        // 1. Center P(h, k)
        res.add(
            KeyPoint(
                x = circle.centerX,
                y = circle.centerY,
                type = KeyPointType.CIRCLE_CENTER,
                label = "Pusat Lingkaran",
                detail = "P(${formatNumber(circle.centerX)}, ${formatNumber(circle.centerY)})"
            )
        )

        // 2. Extrema points (Top, Bottom, Left, Right)
        val h = circle.centerX
        val k = circle.centerY
        val r = circle.radius

        res.add(KeyPoint(h, k + r, KeyPointType.CIRCLE_EXTREMA, "Puncak Atas", "(${formatNumber(h)}, ${formatNumber(k + r)})"))
        res.add(KeyPoint(h, k - r, KeyPointType.CIRCLE_EXTREMA, "Puncak Bawah", "(${formatNumber(h)}, ${formatNumber(k - r)})"))
        res.add(KeyPoint(h - r, k, KeyPointType.CIRCLE_EXTREMA, "Paling Kiri", "(${formatNumber(h - r)}, ${formatNumber(k)})"))
        res.add(KeyPoint(h + r, k, KeyPointType.CIRCLE_EXTREMA, "Paling Kanan", "(${formatNumber(h + r)}, ${formatNumber(k)})"))

        // 3. X-intercepts (y = 0): (x - h)^2 + k^2 = r^2 -> x = h +- sqrt(r^2 - k^2)
        if (abs(k) <= r) {
            val deltaX = sqrt((r * r - k * k).toDouble()).toFloat()
            val x1 = h - deltaX
            val x2 = h + deltaX
            res.add(KeyPoint(x1, 0f, KeyPointType.X_INTERCEPT, "Potong X", "(${formatNumber(x1)}, 0)"))
            if (abs(x1 - x2) > 0.01f) {
                res.add(KeyPoint(x2, 0f, KeyPointType.X_INTERCEPT, "Potong X", "(${formatNumber(x2)}, 0)"))
            }
        }

        // 4. Y-intercepts (x = 0): h^2 + (y - k)^2 = r^2 -> y = k +- sqrt(r^2 - h^2)
        if (abs(h) <= r) {
            val deltaY = sqrt((r * r - h * h).toDouble()).toFloat()
            val y1 = k - deltaY
            val y2 = k + deltaY
            res.add(KeyPoint(0f, y1, KeyPointType.Y_INTERCEPT, "Potong Y", "(0, ${formatNumber(y1)})"))
            if (abs(y1 - y2) > 0.01f) {
                res.add(KeyPoint(0f, y2, KeyPointType.Y_INTERCEPT, "Potong Y", "(0, ${formatNumber(y2)})"))
            }
        }

        return res
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LINEAR & IMPLICIT EQUATION PARSER (2x + 3y = 6, x - y = 0, x = 3, 2x = 8)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses implicit or explicit linear equations:
     * - Standard implicit form: 2x + 3y = 6, x - y = 0, 3x - 4y + 12 = 0
     * - Vertical lines: x = 3, 2x = 8, x = -2
     * - Horizontal lines: y = 4, 3y = 12
     */
    fun parseLinearEquation(raw: String): LinearEquationData? {
        val clean = cleanLatexForPlot(raw).replace(" ", "").lowercase()
            .replace("\\cdot", "*").replace("\\times", "*")

        // Exclude higher degrees and trigonometric/transcendental functions
        if (clean.contains("^") || clean.contains("sin") || clean.contains("cos") ||
            clean.contains("tan") || clean.contains("sqrt") || clean.contains("log") ||
            clean.contains("ln") || clean.contains("abs")
        ) {
            return null
        }

        if (!clean.contains("=")) return null
        val parts = clean.split("=")
        if (parts.size != 2) return null

        val lhsTerms = extractPolynomialTerms(parts[0]) ?: return null
        val rhsTerms = extractPolynomialTerms(parts[1]) ?: return null

        // Must not have quadratic or cross terms
        if (abs(lhsTerms.coeffX2) > 1e-5 || abs(rhsTerms.coeffX2) > 1e-5 ||
            abs(lhsTerms.coeffY2) > 1e-5 || abs(rhsTerms.coeffY2) > 1e-5 ||
            abs(lhsTerms.coeffXY) > 1e-5 || abs(rhsTerms.coeffXY) > 1e-5
        ) {
            return null
        }

        val a = (lhsTerms.coeffX - rhsTerms.coeffX).toFloat()
        val b = (lhsTerms.coeffY - rhsTerms.coeffY).toFloat()
        val c = (lhsTerms.coeffConst - rhsTerms.coeffConst).toFloat()

        // If no variables present (e.g. 5 = 5), not a line
        if (abs(a) < 1e-5f && abs(b) < 1e-5f) return null

        return if (abs(b) < 1e-5f) {
            // Vertical line: a * x + c = 0 -> x = -c / a
            val vx = -c / a
            LinearEquationData(
                a = a,
                b = 0f,
                c = c,
                isVertical = true,
                verticalX = vx,
                slope = Float.POSITIVE_INFINITY,
                yIntercept = Float.NaN,
                standardEquation = "x = ${formatNumber(vx)}",
                functionExpr = ""
            )
        } else {
            // Slope-intercept form: y = (-a/b)x + (-c/b)
            val slope = -a / b
            val yInt = -c / b
            val functionExpr = "(-($a) * x - ($c)) / ($b)"

            val stdEq = when {
                abs(a) < 1e-4f -> "y = ${formatNumber(yInt)}"
                abs(b - 1f) < 1e-4f && abs(slope - 1f) < 1e-4f && abs(yInt) < 1e-4f -> "y = x"
                else -> {
                    val aStr = if (abs(a - 1f) < 1e-4f) "x" else if (abs(a + 1f) < 1e-4f) "-x" else "${formatNumber(a)}x"
                    val bStr = when {
                        abs(b - 1f) < 1e-4f -> " + y"
                        abs(b + 1f) < 1e-4f -> " - y"
                        b > 0 -> " + ${formatNumber(b)}y"
                        else -> " - ${formatNumber(-b)}y"
                    }
                    val rhsConst = -c
                    "$aStr$bStr = ${formatNumber(rhsConst)}"
                }
            }

            LinearEquationData(
                a = a,
                b = b,
                c = c,
                isVertical = false,
                verticalX = 0f,
                slope = slope,
                yIntercept = yInt,
                standardEquation = stdEq,
                functionExpr = functionExpr
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HIGH-SPEED COMPILED AST EXPRESSION EVALUATOR
    // ─────────────────────────────────────────────────────────────────────────

    private val compiledCache = java.util.concurrent.ConcurrentHashMap<String, CompiledExpr>()

    /**
     * Compiles a mathematical expression of 'x' into an optimized AST.
     * Evaluates in nanoseconds with zero object allocations and zero regex per step.
     */
    fun compile(exprStr: String): CompiledExpr? {
        val clean = cleanLatexForPlot(exprStr)
            .replace(" ", "")
            .replace("\\cdot", "*")
            .replace("\\times", "*")
        return compiledCache.getOrPut(clean) {
            try {
                val tokens = tokenize(clean)
                val parser = ExprAstParser(tokens)
                parser.parse()
            } catch (_: Exception) {
                // Fallback to safe string evaluation if syntax is complex
                val fallback = { x: Double -> evaluateAt(exprStr, x) ?: Double.NaN }
                CompiledExpr(fallback)
            }
        }
    }

    private enum class TokenType {
        NUMBER, VAR_X, PLUS, MINUS, STAR, SLASH, CARET, PERCENT, LPAREN, RPAREN, FUNC, CONST_PI, CONST_E
    }

    private data class Token(val type: TokenType, val numVal: Double = 0.0, val strVal: String = "")

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        fun canInsertImplicitMul(): Boolean {
            if (tokens.isEmpty()) return false
            val prev = tokens.last().type
            return prev == TokenType.NUMBER || prev == TokenType.VAR_X ||
                    prev == TokenType.RPAREN || prev == TokenType.CONST_PI || prev == TokenType.CONST_E
        }

        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c in '0'..'9' || c == '.' -> {
                    if (canInsertImplicitMul()) tokens.add(Token(TokenType.STAR))
                    val start = i
                    while (i < text.length && (text[i] in '0'..'9' || text[i] == '.' ||
                                ((text[i] == 'e' || text[i] == 'E') && i + 1 < text.length &&
                                        (text[i + 1] in '0'..'9' || text[i + 1] == '+' || text[i + 1] == '-')))) {
                        i++
                    }
                    val num = text.substring(start, i).toDouble()
                    tokens.add(Token(TokenType.NUMBER, numVal = num))
                }
                c == 'x' || c == 'X' -> {
                    if (canInsertImplicitMul()) tokens.add(Token(TokenType.STAR))
                    tokens.add(Token(TokenType.VAR_X))
                    i++
                }
                c == '+' -> { tokens.add(Token(TokenType.PLUS)); i++ }
                c == '-' -> { tokens.add(Token(TokenType.MINUS)); i++ }
                c == '*' -> { tokens.add(Token(TokenType.STAR)); i++ }
                c == '/' -> { tokens.add(Token(TokenType.SLASH)); i++ }
                c == '^' -> { tokens.add(Token(TokenType.CARET)); i++ }
                c == '%' -> { tokens.add(Token(TokenType.PERCENT)); i++ }
                c == '(' -> {
                    if (canInsertImplicitMul()) tokens.add(Token(TokenType.STAR))
                    tokens.add(Token(TokenType.LPAREN))
                    i++
                }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN)); i++ }
                c.isLetter() -> {
                    val start = i
                    while (i < text.length && text[i].isLetter()) i++
                    val word = text.substring(start, i).lowercase(Locale.ROOT)
                    when (word) {
                        "pi" -> {
                            if (canInsertImplicitMul()) tokens.add(Token(TokenType.STAR))
                            tokens.add(Token(TokenType.CONST_PI))
                        }
                        "e" -> {
                            if (canInsertImplicitMul()) tokens.add(Token(TokenType.STAR))
                            tokens.add(Token(TokenType.CONST_E))
                        }
                        else -> {
                            if (canInsertImplicitMul()) tokens.add(Token(TokenType.STAR))
                            tokens.add(Token(TokenType.FUNC, strVal = word))
                        }
                    }
                }
                else -> i++
            }
        }
        return tokens
    }

    private class ExprAstParser(private val tokens: List<Token>) {
        private var idx = 0

        private fun peek(): Token? = if (idx < tokens.size) tokens[idx] else null
        private fun next(): Token = tokens[idx++]

        fun parse(): CompiledExpr {
            val expr = parseExpression()
            return expr
        }

        private fun parseExpression(): CompiledExpr {
            var left = parseTerm()
            while (peek()?.type == TokenType.PLUS || peek()?.type == TokenType.MINUS) {
                val op = next().type
                val right = parseTerm()
                val prevLeft = left
                left = if (op == TokenType.PLUS) {
                    CompiledExpr { x -> prevLeft.eval(x) + right.eval(x) }
                } else {
                    CompiledExpr { x -> prevLeft.eval(x) - right.eval(x) }
                }
            }
            return left
        }

        private fun parseTerm(): CompiledExpr {
            var left = parseUnary()
            while (peek()?.type == TokenType.STAR || peek()?.type == TokenType.SLASH || peek()?.type == TokenType.PERCENT) {
                val op = next().type
                val right = parseUnary()
                val prevLeft = left
                left = when (op) {
                    TokenType.STAR -> CompiledExpr { x -> prevLeft.eval(x) * right.eval(x) }
                    TokenType.SLASH -> CompiledExpr { x ->
                        val d = right.eval(x)
                        if (abs(d) < 1e-13) Double.NaN else prevLeft.eval(x) / d
                    }
                    else -> CompiledExpr { x -> prevLeft.eval(x) % right.eval(x) }
                }
            }
            return left
        }

        private fun parseUnary(): CompiledExpr {
            if (peek()?.type == TokenType.PLUS) {
                next()
                return parseUnary()
            }
            if (peek()?.type == TokenType.MINUS) {
                next()
                val inner = parsePower() // Unary minus binds looser than exponentiation (-x^2 = -(x^2))
                return CompiledExpr { x -> -inner.eval(x) }
            }
            return parsePower()
        }

        private fun parsePower(): CompiledExpr {
            val base = parsePrimary()
            if (peek()?.type == TokenType.CARET) {
                next()
                val exp = parseUnary() // Right-associative with unary
                return CompiledExpr { x -> base.eval(x).pow(exp.eval(x)) }
            }
            return base
        }

        private fun parsePrimary(): CompiledExpr {
            val t = peek() ?: return CompiledExpr { Double.NaN }
            return when (t.type) {
                TokenType.NUMBER -> {
                    next()
                    val c = t.numVal
                    CompiledExpr { c }
                }
                TokenType.VAR_X -> {
                    next()
                    CompiledExpr { x -> x }
                }
                TokenType.CONST_PI -> {
                    next()
                    CompiledExpr { Math.PI }
                }
                TokenType.CONST_E -> {
                    next()
                    CompiledExpr { Math.E }
                }
                TokenType.LPAREN -> {
                    next()
                    val expr = parseExpression()
                    if (peek()?.type == TokenType.RPAREN) next()
                    expr
                }
                TokenType.FUNC -> {
                    val fnToken = next()
                    val funcName = fnToken.strVal
                    val arg = if (peek()?.type == TokenType.LPAREN) {
                        next()
                        val e = parseExpression()
                        if (peek()?.type == TokenType.RPAREN) next()
                        e
                    } else {
                        parseUnary()
                    }
                    CompiledExpr { x ->
                        val a = arg.eval(x)
                        when (funcName) {
                            "sin" -> sin(a)
                            "cos" -> cos(a)
                            "tan" -> tan(a)
                            "asin" -> asin(a)
                            "acos" -> acos(a)
                            "atan" -> atan(a)
                            "sqrt" -> if (a < 0.0) Double.NaN else sqrt(a)
                            "cbrt" -> cbrt(a)
                            "abs" -> abs(a)
                            "ln" -> if (a <= 0.0) Double.NaN else ln(a)
                            "log" -> if (a <= 0.0) Double.NaN else log10(a)
                            "exp" -> exp(a)
                            else -> a
                        }
                    }
                }
                else -> {
                    next()
                    CompiledExpr { Double.NaN }
                }
            }
        }
    }

    /**
     * Evaluates f(x) safely at a specific numerical value of x using compiled AST or fallback.
     */
    fun evaluateAt(expr: String, xVal: Double): Double? {
        val clean = extractFunctionBody(expr)
        val xFormatted = String.format(Locale.US, "%.8f", xVal).trimEnd('0').let { if (it.endsWith('.')) "${it}0" else it }
        val substituted = clean
            .replace(Regex("(\\d)(x)", RegexOption.IGNORE_CASE), "$1*($xFormatted)")
            .replace(Regex("(\\))(\\()"), "$1*$2")
            .replace(Regex("(\\))(x)", RegexOption.IGNORE_CASE), "$1*($xFormatted)")
            .replace(Regex("(x)(\\()", RegexOption.IGNORE_CASE), "($xFormatted)*$2")
            .replace(Regex("(x)(\\d)", RegexOption.IGNORE_CASE), "($xFormatted)*$2")
            .replace("x", "($xFormatted)", ignoreCase = true)
        return evaluateExpression(substituted)
    }

    /**
     * Finds critical mathematical points (X-intercepts, Y-intercept, local Min/Max extrema).
     * Uses fast compiled evaluator and exact analytical quadratic formula.
     */
    fun findKeyPoints(
        functionBody: String,
        searchXMin: Float = -20f,
        searchXMax: Float = 20f
    ): List<KeyPoint> {
        val circle = parseCircle(functionBody)
        if (circle != null) {
            return getCircleKeyPoints(circle)
        }

        val compiled = compile(functionBody) ?: return emptyList()
        val results = mutableListOf<KeyPoint>()

        // 1. Y-intercept: x = 0
        val y0 = compiled.eval(0.0)
        if (!y0.isNaN() && !y0.isInfinite() && abs(y0) < 500.0) {
            val yVal = y0.toFloat()
            results.add(
                KeyPoint(
                    x = 0f,
                    y = yVal,
                    type = KeyPointType.Y_INTERCEPT,
                    label = "Potong Y",
                    detail = "(0, ${formatNumber(yVal)})"
                )
            )
        }

        // 2. Analytical Check for Quadratic f(x) = ax^2 + bx + c
        val c = y0
        val f1 = compiled.eval(1.0)
        val fMinus1 = compiled.eval(-1.0)
        val f2 = compiled.eval(2.0)

        if (!c.isNaN() && !f1.isNaN() && !fMinus1.isNaN() && !f2.isNaN()) {
            val a = (f1 + fMinus1 - 2.0 * c) / 2.0
            val b = (f1 - fMinus1) / 2.0
            val expectedF2 = 4.0 * a + 2.0 * b + c

            if (abs(a) > 1e-4 && abs(f2 - expectedF2) < 1e-3) {
                // Exact roots for quadratic
                val disc = b * b - 4.0 * a * c
                if (disc >= -1e-6) {
                    val sqrtD = if (disc > 0.0) sqrt(disc) else 0.0
                    val r1 = (-b - sqrtD) / (2.0 * a)
                    val r2 = (-b + sqrtD) / (2.0 * a)

                    val r1F = r1.toFloat()
                    results.add(
                        KeyPoint(
                            x = r1F,
                            y = 0f,
                            type = KeyPointType.X_INTERCEPT,
                            label = "Akar X",
                            detail = "(${formatNumber(r1F)}, 0)"
                        )
                    )
                    if (abs(r1 - r2) > 0.01) {
                        val r2F = r2.toFloat()
                        results.add(
                            KeyPoint(
                                x = r2F,
                                y = 0f,
                                type = KeyPointType.X_INTERCEPT,
                                label = "Akar X",
                                detail = "(${formatNumber(r2F)}, 0)"
                            )
                        )
                    }
                }

                // Exact Vertex for quadratic: x = -b / (2a)
                val vx = -b / (2.0 * a)
                val vy = compiled.eval(vx).let { if (it.isNaN()) c - (b * b) / (4.0 * a) else it }
                val vxF = vx.toFloat()
                val vyF = vy.toFloat()
                val isMin = a > 0.0
                results.add(
                    KeyPoint(
                        x = vxF,
                        y = vyF,
                        type = if (isMin) KeyPointType.VERTEX_MIN else KeyPointType.VERTEX_MAX,
                        label = if (isMin) "Puncak Min (Lembah)" else "Puncak Maks",
                        detail = "(${formatNumber(vxF)}, ${formatNumber(vyF)})"
                    )
                )

                return results.distinctBy { Triple(it.type, (it.x * 100).roundToInt(), (it.y * 100).roundToInt()) }
            }
        }

        // 3. General Numerical Root-Finding (Sign Change + Bisection)
        val scanSteps = 300
        val step = (searchXMax - searchXMin) / scanSteps.toDouble()
        var prevX = searchXMin.toDouble()
        var prevY = compiled.eval(prevX)

        val foundRoots = mutableListOf<Double>()

        for (i in 1..scanSteps) {
            val curX = searchXMin + i * step
            val curY = compiled.eval(curX)

            if (!curY.isNaN() && !curY.isInfinite()) {
                if (abs(curY) < 1e-5) {
                    if (foundRoots.none { abs(it - curX) < 0.05 }) {
                        foundRoots.add(curX)
                    }
                } else if (!prevY.isNaN() && !prevY.isInfinite() && (prevY * curY < 0.0)) {
                    var low = prevX
                    var high = curX
                    var rootX = (low + high) / 2.0
                    for (iter in 0 until 16) {
                        val mid = (low + high) / 2.0
                        val midY = compiled.eval(mid)
                        if (midY.isNaN()) break
                        if (abs(midY) < 1e-6) {
                            rootX = mid
                            break
                        }
                        if (midY * compiled.eval(low) < 0) {
                            high = mid
                        } else {
                            low = mid
                        }
                        rootX = mid
                    }
                    if (foundRoots.none { abs(it - rootX) < 0.05 }) {
                        foundRoots.add(rootX)
                    }
                }
            }
            prevX = curX
            prevY = curY
        }

        for (rx in foundRoots) {
            val rxFloat = rx.toFloat()
            results.add(
                KeyPoint(
                    x = rxFloat,
                    y = 0f,
                    type = KeyPointType.X_INTERCEPT,
                    label = "Akar X",
                    detail = "(${formatNumber(rxFloat)}, 0)"
                )
            )
        }

        // 4. Local Extrema (Vertex / Min / Max) using numerical first derivative sign changes
        val h = 0.001
        fun deriv(x: Double): Double {
            val fPlus = compiled.eval(x + h)
            val fMinus = compiled.eval(x - h)
            if (fPlus.isNaN() || fMinus.isNaN()) return Double.NaN
            return (fPlus - fMinus) / (2.0 * h)
        }

        var prevD = deriv(searchXMin.toDouble())
        var prevDx = searchXMin.toDouble()
        val extremaX = mutableListOf<Pair<Double, Boolean>>()

        for (i in 1..scanSteps) {
            val curDx = searchXMin + i * step
            val curD = deriv(curDx)

            if (!curD.isNaN() && !prevD.isNaN()) {
                if (prevD * curD < 0.0 && abs(prevD - curD) < 100.0) {
                    var low = prevDx
                    var high = curDx
                    var critX = (low + high) / 2.0
                    for (iter in 0 until 14) {
                        val mid = (low + high) / 2.0
                        val dMid = deriv(mid)
                        if (dMid.isNaN()) break
                        if (abs(dMid) < 1e-5) {
                            critX = mid
                            break
                        }
                        if (dMid * deriv(low) < 0) {
                            high = mid
                        } else {
                            low = mid
                        }
                        critX = mid
                    }

                    val isMin = prevD < 0 && curD > 0
                    if (extremaX.none { abs(it.first - critX) < 0.08 }) {
                        extremaX.add(critX to isMin)
                    }
                }
            }
            prevDx = curDx
            prevD = curD
        }

        for ((ex, isMin) in extremaX) {
            val yVal = compiled.eval(ex)
            if (!yVal.isNaN() && !yVal.isInfinite() && abs(yVal) < 500.0) {
                val exFloat = ex.toFloat()
                val eyFloat = yVal.toFloat()
                results.add(
                    KeyPoint(
                        x = exFloat,
                        y = eyFloat,
                        type = if (isMin) KeyPointType.VERTEX_MIN else KeyPointType.VERTEX_MAX,
                        label = if (isMin) "Puncak Min" else "Puncak Maks",
                        detail = "(${formatNumber(exFloat)}, ${formatNumber(eyFloat)})"
                    )
                )
            }
        }

        return results
    }

    /**
     * Formats floating numbers cleanly (e.g. 2, 2.5, -4, 0).
     */
    fun formatNumber(value: Float): String {
        return if (abs(value - value.roundToInt()) < 0.001f) {
            value.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERSECTION SOLVER (TITIK POTONG ANTAR-GRAFIK)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Solves for all intersection points between visible curves, including
     * explicit functions, linear lines (vertical, horizontal, slanted), and circles.
     */
    fun findIntersections(
        curves: List<CurvePlotDefinition>,
        xMin: Float,
        xMax: Float
    ): List<IntersectionPoint> {
        if (curves.size < 2) return emptyList()
        val results = mutableListOf<IntersectionPoint>()

        for (i in 0 until curves.size - 1) {
            for (j in i + 1 until curves.size) {
                val c1 = curves[i]
                val c2 = curves[j]
                findIntersectionsPair(c1, c2, xMin, xMax, results)
            }
        }

        // Deduplicate points within distance 0.08
        val unique = mutableListOf<IntersectionPoint>()
        for (pt in results) {
            if (unique.none { abs(it.x - pt.x) < 0.08f && abs(it.y - pt.y) < 0.08f }) {
                unique.add(pt)
            }
        }
        return unique.sortedWith(compareBy({ it.x }, { it.y }))
    }

    private fun findIntersectionsPair(
        c1: CurvePlotDefinition,
        c2: CurvePlotDefinition,
        xMin: Float,
        xMax: Float,
        results: MutableList<IntersectionPoint>
    ) {
        when {
            // Case 1: Both are vertical lines (parallel, no discrete intersection)
            c1.isVerticalLine && c2.isVerticalLine -> return

            // Case 2: One is a vertical line
            c1.isVerticalLine || c2.isVerticalLine -> {
                val vert = if (c1.isVerticalLine) c1 else c2
                val other = if (c1.isVerticalLine) c2 else c1
                val vx = vert.verticalLineX

                if (other.isCircle && other.circleData != null) {
                    val cd = other.circleData
                    val dx = abs(vx - cd.centerX)
                    if (dx <= cd.radius + 1e-4f) {
                        val dySq = max(0f, cd.radius * cd.radius - dx * dx)
                        val dy = sqrt(dySq)
                        val y1 = cd.centerY + dy
                        val y2 = cd.centerY - dy
                        results.add(IntersectionPoint(vx, y1, "(${formatNumber(vx)}, ${formatNumber(y1)})"))
                        if (dy > 0.005f) {
                            results.add(IntersectionPoint(vx, y2, "(${formatNumber(vx)}, ${formatNumber(y2)})"))
                        }
                    }
                } else {
                    val compiled = compile(other.cleanExpr)
                    if (compiled != null) {
                        val vy = compiled.eval(vx.toDouble())
                        if (!vy.isNaN() && !vy.isInfinite() && abs(vy) < 10000.0) {
                            results.add(IntersectionPoint(vx, vy.toFloat(), "(${formatNumber(vx)}, ${formatNumber(vy.toFloat())})"))
                        }
                    }
                }
            }

            // Case 3: Both are circles
            c1.isCircle && c2.isCircle -> {
                val cd1 = c1.circleData ?: return
                val cd2 = c2.circleData ?: return
                val dx = cd2.centerX - cd1.centerX
                val dy = cd2.centerY - cd1.centerY
                val d = sqrt(dx * dx + dy * dy)
                if (d > cd1.radius + cd2.radius + 1e-4f || d < abs(cd1.radius - cd2.radius) - 1e-4f || d < 1e-5f) {
                    return
                }
                val a = (cd1.radius * cd1.radius - cd2.radius * cd2.radius + d * d) / (2f * d)
                val hSq = max(0f, cd1.radius * cd1.radius - a * a)
                val h = sqrt(hSq)
                val x0 = cd1.centerX + a * dx / d
                val y0 = cd1.centerY + a * dy / d
                val rx = -dy * (h / d)
                val ry = dx * (h / d)
                val p1X = x0 + rx
                val p1Y = y0 + ry
                results.add(IntersectionPoint(p1X, p1Y, "(${formatNumber(p1X)}, ${formatNumber(p1Y)})"))
                if (h > 0.005f) {
                    val p2X = x0 - rx
                    val p2Y = y0 - ry
                    results.add(IntersectionPoint(p2X, p2Y, "(${formatNumber(p2X)}, ${formatNumber(p2Y)})"))
                }
            }

            // Case 4: One is a circle, one is a function
            c1.isCircle != c2.isCircle -> {
                val circleDef = if (c1.isCircle) c1 else c2
                val funcDef = if (c1.isCircle) c2 else c1
                val cd = circleDef.circleData ?: return
                val compiled = compile(funcDef.cleanExpr) ?: return
                val leftX = max(xMin, cd.centerX - cd.radius)
                val rightX = min(xMax, cd.centerX + cd.radius)
                if (leftX >= rightX) return

                val rSq = cd.radius.toDouble() * cd.radius.toDouble()
                val cxD = cd.centerX.toDouble()
                val cyD = cd.centerY.toDouble()

                fun topY(x: Double): Double {
                    val dx = x - cxD
                    val diff = rSq - dx * dx
                    return if (diff < 0.0) Double.NaN else cyD + sqrt(diff)
                }

                fun botY(x: Double): Double {
                    val dx = x - cxD
                    val diff = rSq - dx * dx
                    return if (diff < 0.0) Double.NaN else cyD - sqrt(diff)
                }

                findFunctionRootsInterval(
                    f = { x -> compiled.eval(x) - topY(x) },
                    evalY = { x -> compiled.eval(x) },
                    start = leftX.toDouble(),
                    end = rightX.toDouble(),
                    steps = 150,
                    results = results
                )
                findFunctionRootsInterval(
                    f = { x -> compiled.eval(x) - botY(x) },
                    evalY = { x -> compiled.eval(x) },
                    start = leftX.toDouble(),
                    end = rightX.toDouble(),
                    steps = 150,
                    results = results
                )
            }

            // Case 5: Both are functions
            else -> {
                val comp1 = compile(c1.cleanExpr) ?: return
                val comp2 = compile(c2.cleanExpr) ?: return
                findFunctionRootsInterval(
                    f = { x -> comp1.eval(x) - comp2.eval(x) },
                    evalY = { x -> comp1.eval(x) },
                    start = xMin.toDouble(),
                    end = xMax.toDouble(),
                    steps = 250,
                    results = results
                )
            }
        }
    }

    private fun findFunctionRootsInterval(
        f: (Double) -> Double,
        evalY: (Double) -> Double,
        start: Double,
        end: Double,
        steps: Int,
        results: MutableList<IntersectionPoint>
    ) {
        val step = (end - start) / steps.toDouble()
        var prevX = start
        var prevDiff = f(prevX)

        for (i in 1..steps) {
            val curX = start + i * step
            val curDiff = f(curX)

            if (!curDiff.isNaN() && !curDiff.isInfinite()) {
                if (abs(curDiff) < 1e-5) {
                    val y = evalY(curX)
                    if (!y.isNaN() && !y.isInfinite() && abs(y) < 10000.0) {
                        val rx = curX.toFloat()
                        val ry = y.toFloat()
                        results.add(IntersectionPoint(rx, ry, "(${formatNumber(rx)}, ${formatNumber(ry)})"))
                    }
                } else if (!prevDiff.isNaN() && !prevDiff.isInfinite() && (prevDiff * curDiff < 0.0)) {
                    var low = prevX
                    var high = curX
                    var rootX = (low + high) / 2.0
                    for (iter in 0 until 14) {
                        val mid = (low + high) / 2.0
                        val midDiff = f(mid)
                        if (midDiff.isNaN()) break
                        if (abs(midDiff) < 1e-6) {
                            rootX = mid
                            break
                        }
                        if (midDiff * f(low) < 0.0) {
                            high = mid
                        } else {
                            low = mid
                        }
                        rootX = mid
                    }
                    val y = evalY(rootX)
                    if (!y.isNaN() && !y.isInfinite() && abs(y) < 10000.0) {
                        val rx = rootX.toFloat()
                        val ry = y.toFloat()
                        results.add(IntersectionPoint(rx, ry, "(${formatNumber(rx)}, ${formatNumber(ry)})"))
                    }
                }
            }
            prevX = curX
            prevDiff = curDiff
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARAMETER EXTRACTION & DYNAMIC SLIDER SUBSTITUTION (ALA DESMOS)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Normalizes implicit algebraic multiplication (e.g. "ax + 5" -> "a*x + 5", "2x" -> "2*x", "a(x)" -> "a*(x)").
     * Preserves mathematical function names (sin, cos, tan, sqrt, log, etc.) and constants (pi, e).
     */
    fun normalizeMathExpression(expr: String): String {
        if (expr.isBlank()) return expr
        val s = expr.trim()

        val reservedWords = listOf(
            "sqrt", "cbrt", "asin", "acos", "atan", "sinh", "cosh", "tanh",
            "sin", "cos", "tan", "sec", "csc", "cot", "log", "ln", "abs",
            "exp", "max", "min", "pi"
        )

        val sb = StringBuilder()
        var i = 0
        var prevCanMul = false

        fun appendMulIfNeeded() {
            if (prevCanMul) {
                while (sb.isNotEmpty() && sb.last().isWhitespace()) {
                    sb.deleteCharAt(sb.length - 1)
                }
                sb.append('*')
            }
        }

        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> {
                    sb.append(c)
                    i++
                }
                c in '0'..'9' || c == '.' -> {
                    appendMulIfNeeded()
                    while (i < s.length && (s[i] in '0'..'9' || s[i] == '.')) {
                        sb.append(s[i])
                        i++
                    }
                    prevCanMul = true
                }
                c == '(' -> {
                    appendMulIfNeeded()
                    sb.append('(')
                    prevCanMul = false
                    i++
                }
                c == ')' -> {
                    sb.append(')')
                    prevCanMul = true
                    i++
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '=' || c == ',' -> {
                    sb.append(c)
                    prevCanMul = false
                    i++
                }
                c.isLetter() -> {
                    val remaining = s.substring(i).lowercase(Locale.ROOT)
                    val matchedFunc = reservedWords.firstOrNull {
                        remaining.startsWith(it) && (remaining.length == it.length || !remaining[it.length].isLetter())
                    }

                    if (matchedFunc != null) {
                        appendMulIfNeeded()
                        sb.append(s.substring(i, i + matchedFunc.length))
                        i += matchedFunc.length
                        prevCanMul = (matchedFunc == "pi" || matchedFunc == "e")
                    } else {
                        // Single-letter variable or parameter (e.g. 'a', 'x', 'm', 'y')
                        appendMulIfNeeded()
                        sb.append(c)
                        prevCanMul = true
                        i++
                    }
                }
                else -> {
                    sb.append(c)
                    prevCanMul = false
                    i++
                }
            }
        }
        return sb.toString()
    }

    /**
     * Converts machine/evaluator explicit multiplication syntax (e.g. "2*x", "a*x", "2*(x+1)")
     * back into natural algebraic implicit notation (e.g. "2x", "ax", "2(x+1)") for display and editing.
     */
    fun denormalizeImplicitMultiplication(expr: String): String {
        if (expr.isBlank()) return expr
        var res = expr
        // 1. Angka diikuti * diikuti huruf: 2*x -> 2x, 4*x^2 -> 4x^2
        res = res.replace(Regex("""(\d+)\s*[\*·]\s*([a-zA-Z])"""), "$1$2")
        // 2. Huruf diikuti * diikuti huruf: a*x -> ax, m*x -> mx
        res = res.replace(Regex("""([a-zA-Z])\s*[\*·]\s*([a-zA-Z])"""), "$1$2")
        // 3. Angka / huruf diikuti * diikuti kurung buka: 2*(x+1) -> 2(x+1)
        res = res.replace(Regex("""(\d+|[a-zA-Z])\s*[\*·]\s*\("""), "$1(")
        // 4. Kurung tutup diikuti * diikuti kurung buka / huruf: (x+1)*(x-1) -> (x+1)(x-1)
        res = res.replace(Regex("""\)\s*[\*·]\s*(\(|[a-zA-Z])"""), ")$1")
        return res
    }

    /**
     * Extracts all single-letter parameter symbols (e.g. 'a', 'b', 'c', 'k', 'm', 'r') from an equation.
     * Excludes independent variable 'x', dependent 'y', base 'e', and calculus keywords / differentials (int, dx, etc.).
     */
    fun extractParameters(expr: String): List<Char> {
        if (expr.isBlank()) return emptyList()

        // 1. Jika merupakan integral, ambil fungsi integrannya saja (jangan sertakan notasi \int atau dx)
        val parsedInt = parseDefiniteIntegral(expr)
        val source = if (parsedInt != null) parsedInt.functionExpr else expr

        // 2. Bersihkan sintaks LaTeX dan normalisasi ekspresi matematika
        val cleanLatex = cleanLatexForPlot(source)
        val normalized = normalizeMathExpression(cleanLatex)
        var clean = normalized.trim()
        if (clean.contains("=")) {
            val parts = clean.split("=")
            clean = if (parts[0].trim().lowercase() in listOf("y", "f(x)", "g(x)")) {
                parts[1].trim()
            } else {
                clean
            }
        }
        var stripped = clean.lowercase()
            .replace("sqrt", " ")
            .replace("cbrt", " ")
            .replace("asin", " ")
            .replace("acos", " ")
            .replace("atan", " ")
            .replace("sinh", " ")
            .replace("cosh", " ")
            .replace("tanh", " ")
            .replace("sin", " ")
            .replace("cos", " ")
            .replace("tan", " ")
            .replace("sec", " ")
            .replace("csc", " ")
            .replace("cot", " ")
            .replace("log", " ")
            .replace("ln", " ")
            .replace("abs", " ")
            .replace("exp", " ")
            .replace("max", " ")
            .replace("min", " ")
            .replace("pi", " ")
            .replace("integral", " ")
            .replace("iiint", " ")
            .replace("iint", " ")
            .replace("oint", " ")
            .replace("int", " ")
            .replace("sum", " ")
            .replace("prod", " ")
            .replace("lim", " ")
            .replace("frac", " ")
            .replace("left", " ")
            .replace("right", " ")

        // Hapus diferensial seperti dx, dy, dt, du, dv, dz, dr
        stripped = stripped.replace(Regex("""\bd[xyzuvtr]\b"""), " ")
            .replace(Regex("""(?<=[^a-zA-Z]|^)d[xyzuvtr](?=[^a-zA-Z]|$)"""), " ")

        val allowedParams = ('a'..'z').toSet() - setOf('x', 'y', 'e')
        val found = mutableListOf<Char>()
        for (i in stripped.indices) {
            val ch = stripped[i]
            if (ch in allowedParams && ch !in found) {
                found.add(ch)
            }
        }
        return found.sorted()
    }

    /**
     * Dynamically substitutes slider parameter values into a mathematical formula expression.
     */
    fun substituteParameters(expr: String, params: Map<Char, Float>): String {
        if (params.isEmpty()) return expr
        val normalized = normalizeMathExpression(expr)
        var res = normalized
        for ((param, value) in params) {
            val pattern = Regex("""(?<![a-zA-Z])$param(?![a-zA-Z])""")
            val valStr = if (abs(value - value.roundToInt()) < 0.001f) {
                value.roundToInt().toString()
            } else {
                String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
            }
            res = pattern.replace(res, "($valStr)")
        }
        return res
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PI-BASED GRID & TRIGONOMETRIC FORMATTING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a formula expression contains trigonometric functions or pi.
     */
    fun isTrigonometric(expr: String): Boolean {
        val lower = expr.lowercase()
        return lower.contains("sin") || lower.contains("cos") || lower.contains("tan") ||
                lower.contains("sec") || lower.contains("csc") || lower.contains("cot") ||
                lower.contains("pi") || lower.contains("π")
    }

    /**
     * Calculates an aesthetically spaced step size in terms of pi fractions for the X-axis grid.
     */
    fun calculateNicePiStep(worldSpanX: Float): Float {
        val piVal = Math.PI.toFloat()
        return when {
            worldSpanX >= 16f * piVal -> 4f * piVal
            worldSpanX >= 8f * piVal -> 2f * piVal
            worldSpanX >= 4f * piVal -> piVal
            worldSpanX >= 2f * piVal -> piVal / 2f
            worldSpanX >= 1f * piVal -> piVal / 4f
            worldSpanX >= 0.5f * piVal -> piVal / 6f
            else -> piVal / 12f
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = abs(a)
        var y = abs(b)
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return if (x == 0) 1 else x
    }

    /**
     * Formats numerical coordinate values as exact textbook pi fractions (e.g. "π/2", "π", "3π/2", "2π").
     */
    fun formatPiFraction(value: Float): String {
        if (abs(value) < 0.005f) return "0"
        val piVal = Math.PI.toFloat()
        val q = value / piVal

        val denominators = intArrayOf(1, 2, 3, 4, 6, 12)
        for (d in denominators) {
            val n = (q * d).roundToInt()
            if (abs(q - n.toFloat() / d) < 0.02f) {
                val g = gcd(n, d)
                val redN = n / g
                val redD = d / g

                return when {
                    redD == 1 -> {
                        when (redN) {
                            1 -> "π"
                            -1 -> "-π"
                            else -> "${redN}π"
                        }
                    }
                    redN == 1 -> "π/$redD"
                    redN == -1 -> "-π/$redD"
                    else -> "${redN}π/$redD"
                }
            }
        }
        return formatNumber(value)
    }

    /**
     * Dynamically samples the function curve across the visible [xMin, xMax] domain.
     */
    fun sampleFunction(
        functionBody: String,
        xMin: Float = -10f,
        xMax: Float = 10f,
        steps: Int = 300
    ): MathPlotData? {
        val circle = parseCircle(functionBody)
        if (circle != null) {
            val span = max(12f, circle.radius * 2.8f)
            return MathPlotData(
                functionExpression = circle.standardEquation,
                variable = 'x',
                xMin = circle.centerX - span / 2f,
                xMax = circle.centerX + span / 2f,
                yMin = circle.centerY - span / 2f,
                yMax = circle.centerY + span / 2f,
                points = emptyList(),
                keyPoints = getCircleKeyPoints(circle),
                circleData = circle
            )
        }

        val points = sampleFunctionDynamic(functionBody, xMin, xMax, steps)
        if (points.size < 4) return null

        var computedYMin = Float.POSITIVE_INFINITY
        var computedYMax = Float.NEGATIVE_INFINITY
        for (pt in points) {
            computedYMin = min(computedYMin, pt.y)
            computedYMax = max(computedYMax, pt.y)
        }

        val yMargin = max((computedYMax - computedYMin) * 0.15f, 2f)
        val finalYMin = (computedYMin - yMargin).coerceIn(-100f, 100f)
        val finalYMax = (computedYMax + yMargin).coerceIn(-100f, 100f)

        val keys = findKeyPoints(functionBody, xMin, xMax)

        return MathPlotData(
            functionExpression = functionBody,
            variable = 'x',
            xMin = xMin,
            xMax = xMax,
            yMin = finalYMin,
            yMax = finalYMax,
            points = points,
            keyPoints = keys
        )
    }

    /**
     * Ultra-fast compiled sampling of curve points across any dynamic viewport [xMin, xMax].
     * Zero regex, zero string allocation.
     */
    fun sampleFunctionDynamic(
        functionBody: String,
        xMin: Float,
        xMax: Float,
        steps: Int = 300
    ): List<Offset> {
        val compiled = compile(functionBody) ?: return emptyList()
        val stepSize = (xMax - xMin) / steps.toFloat()
        val points = ArrayList<Offset>(steps + 1)

        for (i in 0..steps) {
            val x = xMin + i * stepSize
            val yVal = compiled.eval(x.toDouble())
            if (!yVal.isNaN() && !yVal.isInfinite() && abs(yVal) < 2000.0) {
                points.add(Offset(x, yVal.toFloat()))
            }
        }
        return points
    }

    /**
     * Calculates definite integral \int_{a}^{b} f(x) dx using high-precision Simpson's 1/3 numerical quadrature.
     * Also computes geometric total area |f(x)|.
     */
    fun calculateDefiniteIntegral(
        exprStr: String,
        a: Float,
        b: Float,
        steps: Int = 1000
    ): DefiniteIntegralResult {
        val clean = extractFunctionBody(exprStr)
        val compiled = compile(clean) ?: return DefiniteIntegralResult(0.0, 0.0, false)
        val start = minOf(a, b).toDouble()
        val end = maxOf(a, b).toDouble()
        val sign = if (a <= b) 1.0 else -1.0
        val dist = end - start
        if (dist <= 1e-7) {
            return DefiniteIntegralResult(0.0, 0.0, true)
        }

        val n = if (steps % 2 != 0) steps + 1 else steps
        val h = dist / n

        var netSum = 0.0
        var areaSum = 0.0

        val y0 = compiled.eval(start).takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0
        val yn = compiled.eval(end).takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0
        netSum += y0 + yn
        areaSum += abs(y0) + abs(yn)

        for (i in 1 until n) {
            val x = start + i * h
            val y = compiled.eval(x).takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0
            val w = if (i % 2 == 1) 4.0 else 2.0
            netSum += w * y
            areaSum += w * abs(y)
        }

        val netIntegral = (h / 3.0) * netSum * sign
        val totalArea = (h / 3.0) * areaSum

        return DefiniteIntegralResult(
            netIntegral = if (abs(netIntegral) < 1e-10) 0.0 else netIntegral,
            totalArea = if (abs(totalArea) < 1e-10) 0.0 else totalArea,
            isSuccess = true
        )
    }

    /**
     * Formats a clean, publication-ready LaTeX equation string representing the definite integral
     * along with its evaluated numerical quadrature result (and geometric area if signed difference exists).
     */
    fun formatDefiniteIntegralLatex(data: DefiniteIntegralData): String {
        val aStr = formatNumber(data.lowerBoundA)
        val bStr = formatNumber(data.upperBoundB)
        val valStr = formatNumber(data.integralValue.toFloat())
        val areaStr = formatNumber(data.totalArea.toFloat())
        val body = extractFunctionBody(data.functionExpr)
        val baseLatex = "\\int_{$aStr}^{$bStr} $body \\, dx = $valStr"
        return if (abs(data.integralValue - data.totalArea) > 0.01) {
            "$baseLatex \\quad (\\text{Luas} = $areaStr)"
        } else {
            baseLatex
        }
    }

    data class OptimalBounds(
        val xMin: Float,
        val xMax: Float,
        val yMin: Float,
        val yMax: Float
    )

    /**
     * Computes optimal bounding box for snapshot export so that critical features:
     * - Quadratic & polynomial vertices / extrema (peaks & valleys, min & max)
     * - Roots (X-intercepts) & Y-intercept
     * - Definite integral interval [a, b] and shaded area
     * - Circles & lines
     * are guaranteed to be fully visible and framed with comfortable padding (never cut off).
     */
    fun calculateOptimalSnapshotBounds(
        curves: List<MultiPlotCurve>,
        circleData: CircleData? = null,
        definiteIntegral: DefiniteIntegralData? = null,
        definiteIntegrals: List<DefiniteIntegralData> = emptyList(),
        targetAspect: Float = 1600f / 1200f
    ): OptimalBounds {
        val pointsToInclude = mutableListOf<Offset>()

        val allIntegrals = if (definiteIntegrals.isNotEmpty()) definiteIntegrals
            else definiteIntegral?.let { listOf(it) } ?: emptyList()

        if (circleData != null) {
            val r = circleData.radius
            pointsToInclude.add(Offset(circleData.centerX - r, circleData.centerY - r))
            pointsToInclude.add(Offset(circleData.centerX + r, circleData.centerY + r))
        }

        for (curve in curves) {
            if (curve.circleData != null) {
                val c = curve.circleData
                val r = c.radius
                pointsToInclude.add(Offset(c.centerX - r, c.centerY - r))
                pointsToInclude.add(Offset(c.centerX + r, c.centerY + r))
                continue
            }
            if (curve.isVerticalLine) {
                pointsToInclude.add(Offset(curve.verticalLineX, -6f))
                pointsToInclude.add(Offset(curve.verticalLineX, 6f))
                continue
            }

            // Key points (Vertex Min/Max, Intercepts, etc.)
            for (kp in curve.keyPoints) {
                pointsToInclude.add(Offset(kp.x, kp.y))
            }

            // Sample around vertex or curve center to capture the curve curvature
            val vertex = curve.keyPoints.firstOrNull {
                it.type == KeyPointType.VERTEX_MIN || it.type == KeyPointType.VERTEX_MAX
            }
            val cleanFunc = extractFunctionBody(curve.label)
            val compiled = compile(cleanFunc)
            if (compiled != null) {
                if (vertex != null) {
                    val spread = 4.0f
                    for (offset in listOf(-spread, -spread / 2f, spread / 2f, spread)) {
                        val sampleX = vertex.x + offset
                        val y = compiled.eval(sampleX.toDouble()).toFloat()
                        if (!y.isNaN() && !y.isInfinite() && abs(y) < 500f) {
                            pointsToInclude.add(Offset(sampleX, y))
                        }
                    }
                } else if (curve.keyPoints.isEmpty()) {
                    for (sampleX in listOf(-6f, -3f, 0f, 3f, 6f)) {
                        val y = compiled.eval(sampleX.toDouble()).toFloat()
                        if (!y.isNaN() && !y.isInfinite() && abs(y) < 500f) {
                            pointsToInclude.add(Offset(sampleX, y))
                        }
                    }
                }
            }
        }

        // Include Definite Integrals bounds & curve
        for (defInt in allIntegrals) {
            val a = defInt.lowerBoundA
            val b = defInt.upperBoundB
            val xStart = min(a, b)
            val xEnd = max(a, b)
            pointsToInclude.add(Offset(xStart, 0f))
            pointsToInclude.add(Offset(xEnd, 0f))

            val compiled = compile(defInt.functionExpr)
            if (compiled != null) {
                val ya = compiled.eval(a.toDouble()).toFloat()
                val yb = compiled.eval(b.toDouble()).toFloat()
                if (!ya.isNaN() && !ya.isInfinite() && abs(ya) < 500f) pointsToInclude.add(Offset(a, ya))
                if (!yb.isNaN() && !yb.isInfinite() && abs(yb) < 500f) pointsToInclude.add(Offset(b, yb))

                val steps = 10
                val dx = (xEnd - xStart) / steps
                for (i in 0..steps) {
                    val sx = xStart + i * dx
                    val sy = compiled.eval(sx.toDouble()).toFloat()
                    if (!sy.isNaN() && !sy.isInfinite() && abs(sy) < 500f) {
                        pointsToInclude.add(Offset(sx, sy))
                    }
                }
            }
        }

        // Always encourage showing origin (0, 0) if reasonable
        if (pointsToInclude.isEmpty() || pointsToInclude.all { abs(it.x) < 25f && abs(it.y) < 25f }) {
            pointsToInclude.add(Offset(0f, 0f))
        }

        var minX = pointsToInclude.minOfOrNull { it.x } ?: -8f
        var maxX = pointsToInclude.maxOfOrNull { it.x } ?: 8f
        var minY = pointsToInclude.minOfOrNull { it.y } ?: -6f
        var maxY = pointsToInclude.maxOfOrNull { it.y } ?: 6f

        // Ensure minimum span
        if (maxX - minX < 4f) {
            val cx = (minX + maxX) / 2f
            minX = cx - 3f
            maxX = cx + 3f
        }
        if (maxY - minY < 4f) {
            val cy = (minY + maxY) / 2f
            minY = cy - 2.5f
            maxY = cy + 2.5f
        }

        // Add comfortable padding margin (22%)
        val padX = max((maxX - minX) * 0.22f, 1.2f)
        val padY = max((maxY - minY) * 0.22f, 1.2f)
        minX -= padX
        maxX += padX
        minY -= padY
        maxY += padY

        // Adjust to target aspect ratio (targetAspect = width / height)
        val spanX = maxX - minX
        val spanY = maxY - minY
        val currentAspect = spanX / spanY

        if (currentAspect < targetAspect) {
            val neededSpanX = spanY * targetAspect
            val diffX = (neededSpanX - spanX) / 2f
            minX -= diffX
            maxX += diffX
        } else {
            val neededSpanY = spanX / targetAspect
            val diffY = (neededSpanY - spanY) / 2f
            minY -= diffY
            maxY += diffY
        }

        return OptimalBounds(minX, maxX, minY, maxY)
    }

    /**
     * Renders a publication-quality, razor-sharp High-Resolution Bitmap (1600x1200)
     * complete with grid lines, numerical axis ticks, smooth function/circle curve, and critical key points.
     */
    fun renderGraphBitmap(
        plotData: MathPlotData,
        width: Int = 1600,
        height: Int = 1200,
        isDarkTheme: Boolean = false
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Palette
        val bgColor = if (isDarkTheme) Color.rgb(20, 24, 33) else Color.rgb(255, 255, 255)
        val cardBgColor = if (isDarkTheme) Color.rgb(28, 33, 44) else Color.rgb(248, 250, 252)
        val gridColor = if (isDarkTheme) Color.argb(45, 255, 255, 255) else Color.argb(40, 15, 23, 42)
        val axisColor = if (isDarkTheme) Color.rgb(148, 163, 184) else Color.rgb(71, 85, 105)
        val curveColor = if (isDarkTheme) Color.rgb(56, 189, 248) else Color.rgb(2, 132, 199)
        val textColor = if (isDarkTheme) Color.rgb(226, 232, 240) else Color.rgb(30, 41, 59)
        val secondaryTextColor = if (isDarkTheme) Color.rgb(148, 163, 184) else Color.rgb(100, 116, 139)

        canvas.drawColor(bgColor)

        val padding = 76f
        val plotW = width - padding * 2f
        val plotH = height - padding * 2f

        val xRange = (plotData.xMax - plotData.xMin).coerceAtLeast(1f)
        val yRange = (plotData.yMax - plotData.yMin).coerceAtLeast(1f)

        fun toScreenX(worldX: Float): Float = padding + ((worldX - plotData.xMin) / xRange) * plotW
        fun toScreenY(worldY: Float): Float = height - padding - ((worldY - plotData.yMin) / yRange) * plotH

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        }

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor
            strokeWidth = 4.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = curveColor
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // 1. Draw Grid Lines and Numerical Labels
        if (plotData.isPiGridMode) {
            val piStep = calculateNicePiStep(xRange)
            val firstGx = (floor(plotData.xMin / piStep) * piStep).toFloat()
            var gx = firstGx
            while (gx <= plotData.xMax + piStep) {
                val sx = toScreenX(gx)
                if (sx in padding..(width - padding)) {
                    canvas.drawLine(sx, padding, sx, height - padding, gridPaint)
                    if (abs(gx) > 1e-4f) {
                        val piLabel = formatPiFraction(gx)
                        canvas.drawText(piLabel, sx, toScreenY(0f).coerceIn(padding + 40f, height - padding - 20f) + 36f, textPaint)
                    }
                }
                gx += piStep
            }
        } else {
            val xStep = max(1, (xRange / 10).roundToInt())
            var gx = (plotData.xMin / xStep).toInt() * xStep
            while (gx <= plotData.xMax) {
                val sx = toScreenX(gx.toFloat())
                if (sx in padding..(width - padding)) {
                    canvas.drawLine(sx, padding, sx, height - padding, gridPaint)
                    if (gx != 0) {
                        canvas.drawText("$gx", sx, toScreenY(0f).coerceIn(padding + 40f, height - padding - 20f) + 36f, textPaint)
                    }
                }
                gx += xStep
            }
        }

        val yStep = max(1, (yRange / 8).roundToInt())
        var gy = ((plotData.yMin / yStep).toInt()) * yStep
        while (gy <= plotData.yMax) {
            val sy = toScreenY(gy.toFloat())
            if (sy in padding..(height - padding)) {
                canvas.drawLine(padding, sy, width - padding, sy, gridPaint)
                if (gy != 0) {
                    val labelX = toScreenX(0f).coerceIn(padding + 20f, width - padding - 50f)
                    val alignBackup = textPaint.textAlign
                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("$gy", labelX - 12f, sy + 10f, textPaint)
                    textPaint.textAlign = alignBackup
                }
            }
            gy += yStep
        }

        // 2. Draw Axes X and Y
        val originX = toScreenX(0f).coerceIn(padding, width - padding)
        val originY = toScreenY(0f).coerceIn(padding, height - padding)

        // X-axis (horizontal)
        canvas.drawLine(padding, originY, width - padding, originY, axisPaint)
        // Y-axis (vertical)
        canvas.drawLine(originX, padding, originX, height - padding, axisPaint)

        // Axis name tags
        val axisTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 34f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("x", width - padding + 20f, originY + 12f, axisTagPaint)
        canvas.drawText("y", originX - 12f, padding - 24f, axisTagPaint)

        // 2.5 Draw Definite Integral Shaded Area & Riemann Partitions (if present)
        plotData.allDefiniteIntegrals.forEachIndexed { intIdx, defInt ->
            val a = defInt.lowerBoundA
            val b = defInt.upperBoundB
            val xStart = minOf(a, b)
            val xEnd = maxOf(a, b)
            val compiled = compile(defInt.functionExpr)

            if (compiled != null && xEnd - xStart > 1e-4f) {
                val baseColor = defInt.colorArgb
                val r = Color.red(baseColor)
                val g = Color.green(baseColor)
                val blueC = Color.blue(baseColor)

                val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(64, r, g, blueC)
                    style = Paint.Style.FILL
                }
                val boundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = baseColor
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
                }
                val boundDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = baseColor
                    style = Paint.Style.FILL
                }
                val boundRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                val boundTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }

                if (defInt.riemannType == RiemannType.SMOOTH) {
                    val shadePath = Path()
                    val syZero = toScreenY(0f)
                    shadePath.moveTo(toScreenX(xStart), syZero)

                    val steps = 200
                    val dx = (xEnd - xStart) / steps
                    for (i in 0..steps) {
                        val curX = xStart + i * dx
                        val curY = compiled.eval(curX.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                        shadePath.lineTo(toScreenX(curX), toScreenY(curY))
                    }
                    shadePath.lineTo(toScreenX(xEnd), syZero)
                    shadePath.close()
                    canvas.drawPath(shadePath, shadePaint)
                } else {
                    val n = defInt.partitionCount.coerceIn(2, 60)
                    val dx = (xEnd - xStart) / n
                    val barBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(160, r, g, blueC)
                        strokeWidth = 2f
                        style = Paint.Style.STROKE
                    }

                    for (i in 0 until n) {
                        val x0 = xStart + i * dx
                        val x1 = x0 + dx
                        val syZero = toScreenY(0f)

                        when (defInt.riemannType) {
                            RiemannType.TRAPEZOIDAL -> {
                                val y0 = compiled.eval(x0.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                val y1 = compiled.eval(x1.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                val trapPath = Path().apply {
                                    moveTo(toScreenX(x0), syZero)
                                    lineTo(toScreenX(x0), toScreenY(y0))
                                    lineTo(toScreenX(x1), toScreenY(y1))
                                    lineTo(toScreenX(x1), syZero)
                                    close()
                                }
                                canvas.drawPath(trapPath, shadePaint)
                                canvas.drawPath(trapPath, barBorderPaint)
                            }
                            else -> {
                                val evalX = when (defInt.riemannType) {
                                    RiemannType.LEFT -> x0
                                    RiemannType.RIGHT -> x1
                                    else -> (x0 + x1) / 2f
                                }
                                val yH = compiled.eval(evalX.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                val left = toScreenX(x0)
                                val right = toScreenX(x1)
                                val top = minOf(syZero, toScreenY(yH))
                                val bottom = maxOf(syZero, toScreenY(yH))
                                canvas.drawRect(left, top, right, bottom, shadePaint)
                                canvas.drawRect(left, top, right, bottom, barBorderPaint)
                            }
                        }
                    }
                }

                // Vertical boundary lines at a and b
                val ya = compiled.eval(a.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                val yb = compiled.eval(b.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                val sxa = toScreenX(a)
                val sxb = toScreenX(b)
                val syZero = toScreenY(0f)

                canvas.drawLine(sxa, syZero, sxa, toScreenY(ya), boundLinePaint)
                canvas.drawLine(sxb, syZero, sxb, toScreenY(yb), boundLinePaint)

                canvas.drawCircle(sxa, toScreenY(ya), 8f, boundDotPaint)
                canvas.drawCircle(sxa, toScreenY(ya), 8f, boundRingPaint)
                canvas.drawCircle(sxb, toScreenY(yb), 8f, boundDotPaint)
                canvas.drawCircle(sxb, toScreenY(yb), 8f, boundRingPaint)

                // Subscripts a and b on X-axis
                canvas.drawText("a = ${formatNumber(a)}", sxa, syZero.coerceIn(padding + 30f, height - padding - 15f) + 32f, boundTextPaint)
                canvas.drawText("b = ${formatNumber(b)}", sxb, syZero.coerceIn(padding + 30f, height - padding - 15f) + 32f, boundTextPaint)

                // Informational Badge for Definite Integral
                val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 28f
                    isFakeBoldText = true
                    textAlign = Paint.Align.LEFT
                }
                val badgeSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = secondaryTextColor
                    textSize = 22f
                    textAlign = Paint.Align.LEFT
                }
                val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cardBgColor
                    style = Paint.Style.FILL
                    setShadowLayer(8f, 0f, 4f, Color.argb(45, 0, 0, 0))
                }
                val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(160, r, g, blueC)
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }

                val titleLine = "∫[${formatNumber(a)}, ${formatNumber(b)}] ${defInt.displayLabel.ifBlank { "f(x)" }} dx = ${formatNumber(defInt.integralValue.toFloat())}"
                val subLine = "Luas Daerah = ${formatNumber(defInt.totalArea.toFloat())}" +
                        if (defInt.riemannType != RiemannType.SMOOTH) " (${defInt.riemannType.label}, n=${defInt.partitionCount})" else ""

                val tW1 = badgeTextPaint.measureText(titleLine)
                val tW2 = badgeSubTextPaint.measureText(subLine)
                val badgeW = maxOf(tW1, tW2) + 40f
                val badgeH = 80f
                val badgeL = width - padding - badgeW - 10f
                val badgeT = height - padding - (badgeH + 12f) * (intIdx + 1)

                canvas.drawRoundRect(badgeL, badgeT, badgeL + badgeW, badgeT + badgeH, 12f, 12f, badgeBgPaint)
                canvas.drawRoundRect(badgeL, badgeT, badgeL + badgeW, badgeT + badgeH, 12f, 12f, badgeBorderPaint)
                canvas.drawText(titleLine, badgeL + 20f, badgeT + 34f, badgeTextPaint)
                canvas.drawText(subLine, badgeL + 20f, badgeT + 66f, badgeSubTextPaint)
            }
        }

        // 3. Draw Curves (Single or Multiple)
        if (plotData.multiCurves.isNotEmpty()) {
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.WHITE
            }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

            for (mc in plotData.multiCurves) {
                val mcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mc.colorArgb
                    strokeWidth = 6.5f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                if (mc.isVerticalLine) {
                    val sx = toScreenX(mc.verticalLineX)
                    if (sx in (padding - 20f)..(width - padding + 20f)) {
                        canvas.drawLine(sx, padding, sx, height - padding, mcPaint)
                    }
                } else {
                    val mcCircle = mc.circleData
                    if (mcCircle != null) {
                        val cxScreen = toScreenX(mcCircle.centerX)
                        val cyScreen = toScreenY(mcCircle.centerY)
                        val radiusPx = (mcCircle.radius / xRange) * plotW

                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(25, Color.red(mc.colorArgb), Color.green(mc.colorArgb), Color.blue(mc.colorArgb))
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(cxScreen, cyScreen, radiusPx, fillPaint)
                        canvas.drawCircle(cxScreen, cyScreen, radiusPx, mcPaint)

                        val rPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = mc.colorArgb
                            strokeWidth = 2.5f
                            style = Paint.Style.STROKE
                            pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
                        }
                        canvas.drawLine(cxScreen, cyScreen, cxScreen + radiusPx, cyScreen, rPaint)
                    } else if (mc.points.isNotEmpty()) {
                        val curvePath = Path()
                        var isFirst = true
                        for (pt in mc.points) {
                            val sx = toScreenX(pt.x)
                            val sy = toScreenY(pt.y)
                            if (sx in (padding - 20f)..(width - padding + 20f) && sy in (padding - 20f)..(height - padding + 20f)) {
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
                        canvas.drawPath(curvePath, mcPaint)
                    }
                }
            }

            // Draw Intersections on high-res bitmap if present
            if (plotData.intersections.isNotEmpty()) {
                val interHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(60, 217, 119, 6)
                    style = Paint.Style.FILL
                }
                val interRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5f
                }
                val interDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(217, 119, 6)
                    style = Paint.Style.FILL
                }
                val interTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val interBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cardBgColor
                    style = Paint.Style.FILL
                }
                val interBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(160, 217, 119, 6)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }

                for (pt in plotData.intersections) {
                    val sx = toScreenX(pt.x)
                    val sy = toScreenY(pt.y)
                    if (sx in padding..(width - padding) && sy in padding..(height - padding)) {
                        canvas.drawCircle(sx, sy, 18f, interHaloPaint)
                        canvas.drawCircle(sx, sy, 10f, interDotPaint)
                        canvas.drawCircle(sx, sy, 10f, interRingPaint)

                        val labelStr = "(${formatNumber(pt.x)}, ${formatNumber(pt.y)})"
                        val textW = interTextPaint.measureText(labelStr)
                        val bW = textW + 24f
                        val bH = 36f
                        val bL = sx - bW / 2f
                        val bT = sy - 44f
                        canvas.drawRoundRect(bL, bT, bL + bW, bT + bH, 8f, 8f, interBadgePaint)
                        canvas.drawRoundRect(bL, bT, bL + bW, bT + bH, 8f, 8f, interBorderPaint)
                        canvas.drawText(labelStr, sx, bT + 26f, interTextPaint)
                    }
                }
            }

            // Legend Header Pills for multiple curves
            var legendLeft = padding
            val legendTop = padding - 58f
            val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = 28f
                isFakeBoldText = true
            }
            val legendBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cardBgColor
                style = Paint.Style.FILL
                setShadowLayer(6f, 0f, 3f, Color.argb(40, 0, 0, 0))
            }
            val legendBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val legendLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 4.5f
                strokeCap = Paint.Cap.ROUND
            }

            for (mc in plotData.multiCurves) {
                val label = formatFormulaForDisplay(mc.label)
                val textW = legendTextPaint.measureText(label)
                val badgeW = textW + 54f
                val badgeH = 44f

                legendBorderPaint.color = mc.colorArgb
                legendLinePaint.color = mc.colorArgb
                canvas.drawRoundRect(legendLeft, legendTop, legendLeft + badgeW, legendTop + badgeH, 12f, 12f, legendBgPaint)
                canvas.drawRoundRect(legendLeft, legendTop, legendLeft + badgeW, legendTop + badgeH, 12f, 12f, legendBorderPaint)

                // Sleek curve color indicator line instead of circle dot
                canvas.drawLine(legendLeft + 18f, legendTop + 22f, legendLeft + 34f, legendTop + 22f, legendLinePaint)
                canvas.drawText(label, legendLeft + 42f, legendTop + 31f, legendTextPaint)

                legendLeft += badgeW + 14f
                if (legendLeft > width - padding - 100f) break
            }
        } else {
            // Single Curve Fallback
            val circle = plotData.circleData
            if (plotData.isVerticalLine) {
                val sx = toScreenX(plotData.verticalLineX)
                if (sx in (padding - 20f)..(width - padding + 20f)) {
                    canvas.drawLine(sx, padding, sx, height - padding, curvePaint)
                }
            } else if (circle != null) {
                val cxScreen = toScreenX(circle.centerX)
                val cyScreen = toScreenY(circle.centerY)
                val radiusPx = (circle.radius / xRange) * plotW

                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDarkTheme) Color.argb(35, 37, 99, 235) else Color.argb(25, 37, 99, 235)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cxScreen, cyScreen, radiusPx, fillPaint)
                canvas.drawCircle(cxScreen, cyScreen, radiusPx, curvePaint)

                val radiusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = curveColor
                    strokeWidth = 3.5f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
                canvas.drawLine(cxScreen, cyScreen, cxScreen + radiusPx, cyScreen, radiusPaint)

                val rTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = curveColor
                    textSize = 28f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("r = ${formatNumber(circle.radius)}", cxScreen + radiusPx / 2f, cyScreen - 12f, rTextPaint)
            } else {
                val curvePath = Path()
                var isFirst = true

                for (pt in plotData.points) {
                    val sx = toScreenX(pt.x)
                    val sy = toScreenY(pt.y)
                    if (sx in (padding - 20f)..(width - padding + 20f) && sy in (padding - 20f)..(height - padding + 20f)) {
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
            }

            // Header Title Card
            val rawTitle = if (circle != null) "Lingkaran: ${circle.standardEquation}" else if (plotData.functionExpression.isNotBlank()) "f(x) = ${plotData.functionExpression}" else ""
            if (rawTitle.isNotBlank()) {
                val headerTitle = formatFormulaForDisplay(rawTitle)
                val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 38f
                    isFakeBoldText = true
                    textAlign = Paint.Align.LEFT
                }
                val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cardBgColor
                    style = Paint.Style.FILL
                    setShadowLayer(8f, 0f, 4f, Color.argb(60, 0, 0, 0))
                }

                val headerW = headerPaint.measureText(headerTitle) + 40f
                canvas.drawRoundRect(padding, padding - 60f, padding + headerW, padding + 18f, 14f, 14f, headerBgPaint)
                canvas.drawText(headerTitle, padding + 20f, padding - 14f, headerPaint)
            }
        }

        return bmp
    }

    /**
     * Converts raw formula strings, carets (^2, ^3, etc.), and LaTeX syntax (\times, \cdot, etc.)
     * into clean, readable textbook mathematical format for graph titles, legends, and UI badges.
     */
    fun formatFormulaForDisplay(formula: String): String {
        var res = formula
            .replace("\\cdot", "·")
            .replace("\\times", "×")
            .replace("\\pm", "±")
            .replace("\\pi", "π")
            .replace("\\,", " ")
            .replace("\\quad", " ")
            .replace("\\qquad", " ")
            .replace("\\sin", "sin")
            .replace("\\cos", "cos")
            .replace("\\tan", "tan")
            .replace("\\ln", "ln")
            .replace("\\log", "log")
            .replace("\\sum", "∑")
            .replace("\\prod", "∏")
            .replace("\\lim", "lim")

        // Konversi \frac{a}{b} -> (a)/(b)
        val fracRegex = Regex("""\\(?:d|t)?frac\s*\{([^{}]+)\}\s*\{([^{}]+)\}""")
        res = fracRegex.replace(res) { "(${it.groupValues[1]})/(${it.groupValues[2]})" }

        // Konversi \sqrt{a} -> √(a)
        val sqrtRegex = Regex("""\\sqrt\s*\{([^{}]+)\}""")
        res = sqrtRegex.replace(res) { "√(${it.groupValues[1]})" }

        // Konversi \int_{a}^{b} -> ∫_{a}^{b} atau \int -> ∫
        res = res.replace("\\int", "∫")

        // Superscript exponents
        res = res
            .replace("^{2}", "²")
            .replace("^{3}", "³")
            .replace("^{4}", "⁴")
            .replace("^{5}", "⁵")
            .replace("^{6}", "⁶")
            .replace("^{7}", "⁷")
            .replace("^{8}", "⁸")
            .replace("^{9}", "⁹")
            .replace("^{0}", "⁰")
            .replace("^{1}", "¹")
            .replace("^2", "²")
            .replace("^3", "³")
            .replace("^4", "⁴")
            .replace("^5", "⁵")
            .replace("^6", "⁶")
            .replace("^7", "⁷")
            .replace("^8", "⁸")
            .replace("^9", "⁹")
            .replace("^0", "⁰")
            .replace("^1", "¹")
            .replace("^{*}", "*")
            .replace("*", "·")

        return res.trim()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRAPH EXPRESSION EVALUATOR & LATEX NORMALIZER
    // ─────────────────────────────────────────────────────────────────────────

    fun cleanLatexForPlot(raw: String): String {
        var text = raw.trim()

        if (text.startsWith("```latex")) text = text.removePrefix("```latex").trim()
        else if (text.startsWith("```tex")) text = text.removePrefix("```tex").trim()
        else if (text.startsWith("```")) text = text.removePrefix("```").trim()
        if (text.endsWith("```")) text = text.removeSuffix("```").trim()

        if (text.startsWith("$$") && text.endsWith("$$") && text.length >= 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("$") && text.endsWith("$") && text.length >= 2) {
            text = text.substring(1, text.length - 1).trim()
        } else if (text.startsWith("\\[") && text.endsWith("\\]")) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("\\(") && text.endsWith("\\)")) {
            text = text.substring(2, text.length - 2).trim()
        }

        text = text.replace("\\times", "*")
            .replace("\\cdot", "*")
            .replace("\\div", "/")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("\\pm", "+-")
            .replace("\\pi", "pi")
            .replace("\\left(", "(")
            .replace("\\right)", ")")
            .replace("\\left[", "(")
            .replace("\\right]", ")")
            .replace("\\{", "(")
            .replace("\\}", ")")
            .replace("\\,", " ")
            .replace("\\;", " ")
            .replace("\\quad", " ")
            .replace("\\qquad", " ")

        text = text.replace(Regex("(\\d+)\\s*[xX]\\s*(\\d+)"), "$1 * $2")
        text = text.replace(Regex("(\\d+)\\s*:\\s*(\\d+)"), "$1 / $2")

        if (!text.contains("\\lim")) {
            text = text.replace(Regex("""\s*(?:\\rightarrow|\\to|\\Rightarrow|\\implies|-->|->|==>|=>|→|⇒)\s*"""), " = ")
        }
        text = text.replace(Regex("""\s*>\s*$"""), " =")
        text = text.replace(Regex("""\s*(?:--|__|―|—)\s*$"""), " =")
        text = text.replace(Regex("""={2,}"""), "=")
        text = text.replace(Regex("""\s*=\s*"""), " = ")
        if (text.endsWith(" = ")) {
            text = text.trimEnd()
        }

        text = convertFractions(text)
        text = convertSquareRoots(text)

        text = text.replace("\\sin", "sin")
            .replace("\\cos", "cos")
            .replace("\\tan", "tan")
            .replace("\\ln", "ln")
            .replace("\\log", "log")
            .replace("\\sqrt", "sqrt")
            .replace("\\abs", "abs")

        return text.trim()
    }

    private fun convertFractions(input: String): String {
        var str = input
        val fracKeyword = "\\frac"
        while (str.contains(fracKeyword)) {
            val idx = str.indexOf(fracKeyword)
            var cursor = idx + fracKeyword.length
            while (cursor < str.length && str[cursor].isWhitespace()) cursor++

            if (cursor >= str.length || str[cursor] != '{') break
            val numClose = findClosingBrace(str, cursor)
            if (numClose == -1) break
            val num = str.substring(cursor + 1, numClose)

            cursor = numClose + 1
            while (cursor < str.length && str[cursor].isWhitespace()) cursor++
            if (cursor >= str.length || str[cursor] != '{') break
            val denClose = findClosingBrace(str, cursor)
            if (denClose == -1) break
            val den = str.substring(cursor + 1, denClose)

            val replacement = "(($num)/($den))"
            str = str.substring(0, idx) + replacement + str.substring(denClose + 1)
        }
        return str
    }

    private fun convertSquareRoots(input: String): String {
        var str = input
        val sqrtKeyword = "\\sqrt"
        while (str.contains(sqrtKeyword)) {
            val idx = str.indexOf(sqrtKeyword)
            var cursor = idx + sqrtKeyword.length
            while (cursor < str.length && str[cursor].isWhitespace()) cursor++

            var rootIndex: String? = null
            if (cursor < str.length && str[cursor] == '[') {
                val closeBracket = str.indexOf(']', cursor)
                if (closeBracket != -1) {
                    rootIndex = str.substring(cursor + 1, closeBracket)
                    cursor = closeBracket + 1
                }
            }

            while (cursor < str.length && str[cursor].isWhitespace()) cursor++
            if (cursor >= str.length || str[cursor] != '{') break
            val closeBrace = findClosingBrace(str, cursor)
            if (closeBrace == -1) break
            val content = str.substring(cursor + 1, closeBrace)

            val replacement = if (rootIndex != null) {
                "(($content)^(1/($rootIndex)))"
            } else {
                "sqrt($content)"
            }
            str = str.substring(0, idx) + replacement + str.substring(closeBrace + 1)
        }
        return str
    }

    private fun findClosingBrace(str: String, openIdx: Int): Int {
        var depth = 0
        for (i in openIdx until str.length) {
            if (str[i] == '{') depth++
            else if (str[i] == '}') {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    fun evaluateExpression(str: String): Double? {
        val cleaned = str.replace(" ", "")
            .replace("pi", Math.PI.toString())
            .replace("e", Math.E.toString())

        return try {
            val parser = NumericExpressionParser(cleaned)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private class NumericExpressionParser(private val text: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            pos++
            ch = if (pos < text.length) text[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < text.length) throw RuntimeException("Unexpected: '$ch'")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (abs(divisor) < 1e-12) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos

            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if (ch in '0'..'9' || ch == '.') {
                while (ch in '0'..'9' || ch == '.' || ch == 'e' || ch == 'E' || ((ch == '+' || ch == '-') && pos > startPos && (text[pos - 1] == 'e' || text[pos - 1] == 'E'))) nextChar()
                x = text.substring(startPos, pos).toDouble()
            } else if (ch in 'a'..'z' || ch in 'A'..'Z') {
                while (ch in 'a'..'z' || ch in 'A'..'Z') nextChar()
                val func = text.substring(startPos, pos).lowercase(Locale.ROOT)
                val arg = if (eat('(')) {
                    val a = parseExpression()
                    eat(')')
                    a
                } else {
                    parseFactor()
                }

                x = when (func) {
                    "sqrt" -> sqrt(arg)
                    "cbrt" -> cbrt(arg)
                    "sin" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan" -> tan(arg)
                    "asin" -> asin(arg)
                    "acos" -> acos(arg)
                    "atan" -> atan(arg)
                    "ln" -> ln(arg)
                    "log" -> log10(arg)
                    "abs" -> abs(arg)
                    "floor" -> floor(arg)
                    "ceil" -> ceil(arg)
                    "round" -> kotlin.math.round(arg)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected: '$ch'")
            }

            if (eat('^')) x = x.pow(parseFactor())
            return x
        }
    }
}
