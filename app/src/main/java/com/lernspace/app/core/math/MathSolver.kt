package com.lernspace.app.core.math

import java.util.Locale
import kotlin.math.*

/**
 * Result data class for MathSolver operations.
 */
data class MathSolution(
    val originalExpression: String,
    val normalizedExpression: String,
    val result: String,
    val fullEquationLatex: String,
    val steps: List<String> = emptyList(),
    val isSolved: Boolean = true
)

/**
 * Academic Math Solver & Expression Evaluator for Learnspace.
 *
 * Capabilities:
 * - Arithmetic: +, -, *, /, %, ^, parentheses
 * - LaTeX conversions: \frac{a}{b}, \times, \div, \cdot, \sqrt{x}, \pi, e
 * - Functions: sin, cos, tan, asin, acos, atan, sqrt, cbrt, log, ln, abs, floor, ceil, factorial (!)
 * - Equation solving:
 *   - Auto-calculate expressions ending with '=' (e.g. "12 \times 5 + 8 =")
 *   - Linear equations in 1 variable: "2x + 10 = 24" -> x = 7
 *   - Quadratic equations: "x^2 - 5x + 6 = 0" -> x1 = 3, x2 = 2
 * - Step-by-step resolution logs for students
 */
object MathSolver {

    /**
     * Primary entry point: parses raw math or LaTeX string and produces a solution.
     */
    fun solve(rawInput: String): MathSolution? {
        val trimmed = cleanLatexForSolver(rawInput)
        if (trimmed.isBlank()) return null

        return try {
            // Check if input is an equation containing '='
            if (trimmed.contains('=')) {
                solveEquationOrCalculation(trimmed, rawInput)
            } else {
                // Standalone expression (e.g. "15 * 4 + 20")
                solveExpression(trimmed, rawInput)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cleans raw LaTeX math into a normalized arithmetic/algebra string.
     */
    fun cleanLatexForSolver(raw: String): String {
        var text = raw.trim()

        // Strip markdown code fences
        if (text.startsWith("```latex")) text = text.removePrefix("```latex").trim()
        else if (text.startsWith("```tex")) text = text.removePrefix("```tex").trim()
        else if (text.startsWith("```")) text = text.removePrefix("```").trim()
        if (text.endsWith("```")) text = text.removeSuffix("```").trim()

        // Strip LaTeX math delimiters
        if (text.startsWith("$$") && text.endsWith("$$") && text.length >= 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("$") && text.endsWith("$") && text.length >= 2) {
            text = text.substring(1, text.length - 1).trim()
        } else if (text.startsWith("\\[") && text.endsWith("\\]")) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("\\(") && text.endsWith("\\)")) {
            text = text.substring(2, text.length - 2).trim()
        }

        // Replace common LaTeX symbols
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

        // Handle 'x' or 'X' between numbers as multiplication (e.g. "4 x 5" -> "4 * 5")
        text = text.replace(Regex("(\\d+)\\s*[xX]\\s*(\\d+)"), "$1 * $2")

        // Handle ':' between numbers as division (common in Indonesian notation: "63 : 7" -> "63 / 7")
        text = text.replace(Regex("(\\d+)\\s*:\\s*(\\d+)"), "$1 / $2")

        // Replace fractions: \frac{A}{B} -> ((A)/(B))
        text = convertFractions(text)

        // Replace square roots: \sqrt{A} -> sqrt(A), \sqrt[N]{A} -> (A)^(1/(N))
        text = convertSquareRoots(text)

        // Clean redundant spaces and backslashes before known function names
        text = text.replace("\\sin", "sin")
            .replace("\\cos", "cos")
            .replace("\\tan", "tan")
            .replace("\\ln", "ln")
            .replace("\\log", "log")
            .replace("\\sqrt", "sqrt")
            .replace("\\abs", "abs")

        return text.trim()
    }

    /**
     * Solves equations or arithmetic calculations ending with '='.
     */
    private fun solveEquationOrCalculation(normalized: String, originalRaw: String): MathSolution? {
        val parts = normalized.split('=')
        if (parts.size > 2) return null // Multiple equals signs currently not handled

        val leftSide = parts[0].trim()
        val rightSide = parts[1].trim()

        // Case A: Right side is empty or placeholder like "?" (e.g. "12 * 5 + 8 =")
        if (rightSide.isEmpty() || rightSide == "?" || rightSide == "x" && !leftSide.contains("x")) {
            val evalResult = evaluateExpression(leftSide) ?: return null
            val formattedRes = formatNumber(evalResult)
            val fullEq = "${originalRaw.trim().removeSuffix("=").trim()} = $formattedRes"
            val steps = listOf(
                "Ekspresi: $leftSide",
                "Hitung nilai: $formattedRes"
            )
            return MathSolution(
                originalExpression = originalRaw,
                normalizedExpression = "$leftSide =",
                result = formattedRes,
                fullEquationLatex = "$$ $fullEq $$",
                steps = steps,
                isSolved = true
            )
        }

        // Case B: Algebraic equation in 1 variable (e.g. "2x + 10 = 24" or "x^2 - 5x + 6 = 0")
        val variable = detectVariable(normalized)
        if (variable != null) {
            val algebraSol = solveAlgebraicEquation(leftSide, rightSide, variable, originalRaw)
            if (algebraSol != null) return algebraSol
        }

        // Case C: Equation verification (e.g. "5 + 5 = 10")
        val leftVal = evaluateExpression(leftSide)
        val rightVal = evaluateExpression(rightSide)
        if (leftVal != null && rightVal != null) {
            val isTrue = abs(leftVal - rightVal) < 1e-6
            val status = if (isTrue) "Benar (Identitas)" else "Salah ($leftVal ≠ $rightVal)"
            return MathSolution(
                originalExpression = originalRaw,
                normalizedExpression = normalized,
                result = status,
                fullEquationLatex = "$$ $normalized \\quad \\text{($status)} $$",
                steps = listOf("Ruas Kiri = $leftVal", "Ruas Kanan = $rightVal", "Hasil: $status"),
                isSolved = isTrue
            )
        }

        return null
    }

    /**
     * Solves standalone arithmetic or function expression.
     */
    private fun solveExpression(normalized: String, originalRaw: String): MathSolution? {
        val eval = evaluateExpression(normalized) ?: return null
        val formatted = formatNumber(eval)
        val steps = listOf("Ekspresi: $normalized", "Hasil evaluasi: $formatted")
        return MathSolution(
            originalExpression = originalRaw,
            normalizedExpression = normalized,
            result = formatted,
            fullEquationLatex = "$$ $originalRaw = $formatted $$",
            steps = steps,
            isSolved = true
        )
    }

    /**
     * Detects a single variable character (e.g. 'x', 'y', 'n', 't') in an equation.
     */
    private fun detectVariable(equation: String): Char? {
        val candidates = listOf('x', 'y', 'z', 't', 'n', 'a', 'b')
        for (c in candidates) {
            if (equation.contains(c, ignoreCase = false)) {
                // Make sure it's not part of function name like 'sin', 'cos', 'tan', 'sqrt', 'log', 'ln', 'pi'
                val sanitized = equation.replace("sin", "")
                    .replace("cos", "")
                    .replace("tan", "")
                    .replace("sqrt", "")
                    .replace("log", "")
                    .replace("ln", "")
                    .replace("pi", "")
                    .replace("abs", "")
                if (sanitized.contains(c)) return c
            }
        }
        return null
    }

    /**
     * Solves linear and quadratic equations in 1 variable:
     * e.g. "2x + 10 = 24" -> x = 7
     * e.g. "x^2 - 5x + 6 = 0" -> x = 2, x = 3
     */
    private fun solveAlgebraicEquation(
        leftSide: String,
        rightSide: String,
        variable: Char,
        originalRaw: String
    ): MathSolution? {
        // Bring all terms to left side: leftSide - (rightSide) = 0
        val combinedExpr = "($leftSide) - ($rightSide)"

        // Check if linear: f(x) = ax + b
        // Test f(0) and f(1) to find a and b
        val f0 = evaluateExpressionWithVar(combinedExpr, variable, 0.0) ?: return null
        val f1 = evaluateExpressionWithVar(combinedExpr, variable, 1.0) ?: return null
        val f2 = evaluateExpressionWithVar(combinedExpr, variable, 2.0) ?: return null

        val b = f0
        val a = f1 - f0

        // Check linearity: if f(2) == 2*a + b within tolerance
        val expectedF2 = 2 * a + b
        if (abs(f2 - expectedF2) < 1e-5 && abs(a) > 1e-9) {
            // Linear equation: a*x + b = 0 -> x = -b / a
            val xVal = -b / a
            val formattedX = formatNumber(xVal)
            val resStr = "$variable = $formattedX"
            val steps = listOf(
                "Persamaan: $leftSide = $rightSide",
                "Bentuk standar: ${formatNumber(a)}$variable + ${formatNumber(b)} = 0",
                "Penyelesaian: $variable = -(${formatNumber(b)}) / ${formatNumber(a)}",
                "Hasil: $variable = $formattedX"
            )
            return MathSolution(
                originalExpression = originalRaw,
                normalizedExpression = "$leftSide = $rightSide",
                result = resStr,
                fullEquationLatex = "$$ $variable = $formattedX $$",
                steps = steps,
                isSolved = true
            )
        }

        // Check quadratic: f(x) = A*x^2 + B*x + C
        // f(0) = C
        // f(1) = A + B + C
        // f(-1) = A - B + C
        val fMinus1 = evaluateExpressionWithVar(combinedExpr, variable, -1.0) ?: return null
        val quadC = f0
        val quadA = (f1 + fMinus1 - 2 * quadC) / 2.0
        val quadB = f1 - quadA - quadC

        // Verify quadratic with f(2): should equal A*4 + B*2 + C
        val expectedQuadF2 = quadA * 4 + quadB * 2 + quadC
        if (abs(f2 - expectedQuadF2) < 1e-4 && abs(quadA) > 1e-6) {
            val disc = quadB * quadB - 4 * quadA * quadC
            val steps = mutableListOf(
                "Persamaan: $leftSide = $rightSide",
                "Bentuk kuadrat: ${formatNumber(quadA)}$variable^2 + ${formatNumber(quadB)}$variable + ${formatNumber(quadC)} = 0",
                "Diskriminan (D): ${formatNumber(disc)}"
            )

            return if (disc >= 0) {
                val sqrtDisc = sqrt(disc)
                val x1 = (-quadB + sqrtDisc) / (2 * quadA)
                val x2 = (-quadB - sqrtDisc) / (2 * quadA)
                val resStr = if (abs(x1 - x2) < 1e-6) {
                    "$variable = ${formatNumber(x1)}"
                } else {
                    "${variable}_1 = ${formatNumber(x1)}, \\; ${variable}_2 = ${formatNumber(x2)}"
                }
                steps.add("Akar-akar: $resStr")
                MathSolution(
                    originalExpression = originalRaw,
                    normalizedExpression = "$leftSide = $rightSide",
                    result = resStr,
                    fullEquationLatex = "$$ $resStr $$",
                    steps = steps,
                    isSolved = true
                )
            } else {
                val realPart = -quadB / (2 * quadA)
                val imagPart = sqrt(-disc) / (2 * quadA)
                val resStr = "$variable = ${formatNumber(realPart)} \\pm ${formatNumber(imagPart)}i"
                steps.add("Akar imajiner/kompleks: $resStr")
                MathSolution(
                    originalExpression = originalRaw,
                    normalizedExpression = "$leftSide = $rightSide",
                    result = resStr,
                    fullEquationLatex = "$$ $resStr $$",
                    steps = steps,
                    isSolved = true
                )
            }
        }

        return null
    }

    /**
     * Evaluates an expression string with a substituted variable value.
     */
    private fun evaluateExpressionWithVar(expr: String, variable: Char, value: Double): Double? {
        val substituted = expr.replace(Regex("(\\d)($variable)"), "$1*($value)")
            .replace(variable.toString(), "($value)")
        return evaluateExpression(substituted)
    }

    /**
     * Core recursive-descent math parser and evaluator.
     * Supports +, -, *, /, %, ^, parentheses, functions, and scientific notation.
     */
    fun evaluateExpression(str: String): Double? {
        val cleaned = str.replace(" ", "")
            .replace("pi", Math.PI.toString())
            .replace("e", Math.E.toString())

        return try {
            val parser = Parser(cleaned)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parser implementation for evaluating mathematical expressions.
     */
    private class Parser(private val text: String) {
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
                while (ch in '0'..'9' || ch == '.') nextChar()
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
                    "round" -> round(arg)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected: '$ch'")
            }

            // Exponentiation operator ^
            if (eat('^')) x = x.pow(parseFactor())

            // Factorial operator !
            if (eat('!')) x = factorial(x.roundToInt()).toDouble()

            return x
        }

        private fun factorial(n: Int): Long {
            if (n < 0 || n > 20) return 0L
            var res = 1L
            for (i in 2..n) res *= i
            return res
        }
    }

    /**
     * Converts \frac{numerator}{denominator} -> ((numerator)/(denominator))
     */
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

    /**
     * Converts \sqrt{A} -> sqrt(A) and \sqrt[N]{A} -> ((A)^(1/(N)))
     */
    private fun convertSquareRoots(input: String): String {
        var str = input
        val sqrtKeyword = "\\sqrt"
        while (str.contains(sqrtKeyword)) {
            val idx = str.indexOf(sqrtKeyword)
            var cursor = idx + sqrtKeyword.length
            while (cursor < str.length && str[cursor].isWhitespace()) cursor++

            // Optional root index [N]
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

    /**
     * Formats Double numbers nicely:
     * - Strips .0 from integers
     * - Limits decimal places to max 6 digits
     */
    fun formatNumber(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

        val rounded = (value * 1000000.0).roundToLong() / 1000000.0
        return if (abs(rounded - rounded.toLong()) < 1e-9) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
        }
    }
}

