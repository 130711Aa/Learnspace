package com.lernspace.app.ui.workspace.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class MathSolveResult(
    val normalizedExpression: String,
    val formattedAnswer: String,
    val numericValue: Double? = null
)

/**
 * Pure Kotlin 100% offline deterministic math expression evaluator and 1-variable linear equation solver.
 */
object MathExpressionEvaluator {

    fun normalizeMathString(raw: String): String {
        var s = raw.trim()
            .replace("×", "*")
            .replace("·", "*")
            .replace("÷", "/")
            .replace(":", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("√", "sqrt")
            .replace(",", ".")
            .replace(" ", "")

        // Replace 'x' or 'X' between digits/closing-paren and digits/opening-paren with '*'
        // e.g. "2x3" -> "2*3", "(2+1)x4" -> "(2+1)*4"
        s = s.replace(Regex("(?<=[0-9)])[xX](?=[0-9(])"), "*")
        return s
    }

    /**
     * Checks if a recognized candidate looks like a valid mathematical expression (before '=').
     */
    fun isPlausibleMathExpression(raw: String): Boolean {
        val norm = normalizeMathString(raw).trimEnd('=')
        if (norm.isBlank()) return false
        val hasDigit = norm.any { it.isDigit() }
        if (!hasDigit) return false
        // Only allow math characters and known function/variable letters
        val strippedFuncs = norm
            .replace("sqrt", "")
            .replace("sin", "")
            .replace("cos", "")
            .replace("tan", "")
            .replace("log", "")
            .replace("ln", "")
            .replace("abs", "")
        return strippedFuncs.all { ch ->
            ch.isDigit() || ch in "+-*/^().%=xXyYaAbBcCnN"
        }
    }

    fun solve(rawInput: String): MathSolveResult? {
        val norm = normalizeMathString(rawInput)
        if (norm.isBlank()) return null

        // Case 1: Trailing '=' (e.g. "2+2=" or "2+2") -> evaluate LHS
        if (!norm.contains('=') || norm.endsWith('=')) {
            val expr = norm.trimEnd('=')
            if (expr.isBlank() || expr.none { it.isDigit() }) return null
            val value = tryEvaluate(expr) ?: return null
            if (value.isNaN() || value.isInfinite()) return null
            return MathSolveResult(
                normalizedExpression = prettyPrintExpr(expr),
                formattedAnswer = formatNumber(value),
                numericValue = value
            )
        }

        // Case 2: Equation "LHS = RHS"
        val eqParts = norm.split('=')
        if (eqParts.size != 2) return null
        val lhs = eqParts[0]
        val rhs = eqParts[1]
        if (lhs.isBlank() || rhs.isBlank()) return null

        // Find variable letter if any (ignoring known function names)
        val variable = findSingleVariable(lhs + rhs)
        if (variable == null) {
            // Check numeric equality or evaluate LHS
            val lVal = tryEvaluate(lhs) ?: return null
            val rVal = tryEvaluate(rhs) ?: return null
            val isEqual = abs(lVal - rVal) < 1e-6
            return MathSolveResult(
                normalizedExpression = "${prettyPrintExpr(lhs)} = ${prettyPrintExpr(rhs)}",
                formattedAnswer = if (isEqual) "Benar (${formatNumber(lVal)})" else "Salah (${formatNumber(lVal)} ≠ ${formatNumber(rVal)})",
                numericValue = lVal
            )
        }

        // Solve linear equation f(v) = LHS(v) - RHS(v) = 0 by probing at v=0 and v=1
        val f0 = (tryEvaluate(lhs, variable, 0.0) ?: return null) -
            (tryEvaluate(rhs, variable, 0.0) ?: return null)
        val f1 = (tryEvaluate(lhs, variable, 1.0) ?: return null) -
            (tryEvaluate(rhs, variable, 1.0) ?: return null)
        val f2 = (tryEvaluate(lhs, variable, 2.0) ?: return null) -
            (tryEvaluate(rhs, variable, 2.0) ?: return null)

        // Verify linearity: f(2) - f(1) == f(1) - f(0)
        val slope1 = f1 - f0
        val slope2 = f2 - f1
        if (abs(slope1 - slope2) < 1e-5) {
            if (abs(slope1) < 1e-9) return null
            val solution = -f0 / slope1
            return MathSolveResult(
                normalizedExpression = "${prettyPrintExpr(lhs)} = ${prettyPrintExpr(rhs)}",
                formattedAnswer = "$variable = ${formatNumber(solution)}",
                numericValue = solution
            )
        }

        return null
    }

    private fun findSingleVariable(s: String): Char? {
        val cleaned = s
            .replace("sqrt", "")
            .replace("sin", "")
            .replace("cos", "")
            .replace("tan", "")
            .replace("log", "")
            .replace("ln", "")
            .replace("abs", "")
        val vars = cleaned.filter { it.isLetter() }.map { it.lowercaseChar() }.distinct()
        return if (vars.size == 1) vars.first() else null
    }

    private fun prettyPrintExpr(expr: String): String {
        return expr
            .replace("*", " × ")
            .replace("/", " ÷ ")
            .replace("+", " + ")
            .replace(Regex("(?<=\\d|\\))-(?=\\d|\\()"), " - ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun formatNumber(value: Double): String {
        if (abs(value) < 1e-10) return "0"
        val rounded = value.roundToLong()
        if (abs(value - rounded.toDouble()) < 1e-9) {
            return rounded.toString()
        }
        val formatted = String.format(java.util.Locale.US, "%.6f", value)
            .trimEnd('0')
            .trimEnd('.')
        return if (formatted == "-0") "0" else formatted
    }

    private fun tryEvaluate(expr: String, variable: Char? = null, varValue: Double = 0.0): Double? {
        return try {
            val tokens = tokenize(expr)
            val parser = Parser(tokens, variable, varValue)
            val result = parser.parseExpression()
            if (parser.hasMore()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Ident(val name: String) : Token()
        data class Op(val ch: Char) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object Percent : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var dotSeen = ch == '.'
                    i++
                    while (i < input.length && (input[i].isDigit() || (!dotSeen && input[i] == '.'))) {
                        if (input[i] == '.') dotSeen = true
                        i++
                    }
                    val num = input.substring(start, i).toDouble()
                    if (out.lastOrNull() is Token.RParen || out.lastOrNull() is Token.Ident) {
                        out.add(Token.Op('*'))
                    }
                    out.add(Token.Num(num))
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < input.length && input[i].isLetter()) i++
                    val name = input.substring(start, i).lowercase()
                    if (out.lastOrNull() is Token.Num || out.lastOrNull() is Token.RParen || out.lastOrNull() is Token.Percent) {
                        out.add(Token.Op('*'))
                    }
                    out.add(Token.Ident(name))
                }
                ch == '(' -> {
                    val last = out.lastOrNull()
                    if (last is Token.Num || last is Token.RParen || last is Token.Percent) {
                        out.add(Token.Op('*'))
                    } else if (last is Token.Ident && last.name !in setOf("sqrt", "sin", "cos", "tan", "log", "ln", "abs")) {
                        out.add(Token.Op('*'))
                    }
                    out.add(Token.LParen)
                    i++
                }
                ch == ')' -> {
                    out.add(Token.RParen)
                    i++
                }
                ch == '%' -> {
                    out.add(Token.Percent)
                    i++
                }
                ch in "+-*/^" -> {
                    out.add(Token.Op(ch))
                    i++
                }
                else -> throw IllegalArgumentException("Unexpected char: $ch")
            }
        }
        return out
    }

    private class Parser(
        private val tokens: List<Token>,
        private val variable: Char?,
        private val varValue: Double
    ) {
        private var pos = 0

        fun hasMore(): Boolean = pos < tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (t.ch == '+') left + right else left - right
                } else break
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parsePower()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.ch == '*' || t.ch == '/')) {
                    consume()
                    val right = parsePower()
                    left = if (t.ch == '*') left * right else left / right
                } else break
            }
            return left
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            val t = peek()
            return if (t is Token.Op && t.ch == '^') {
                consume()
                val exp = parsePower()
                base.pow(exp)
            } else {
                base
            }
        }

        private fun parseUnary(): Double {
            val t = peek()
            if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                consume()
                val v = parseUnary()
                return if (t.ch == '-') -v else v
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (peek() is Token.Percent) {
                consume()
                v /= 100.0
            }
            return v
        }

        private fun parsePrimary(): Double {
            return when (val t = consume()) {
                is Token.Num -> t.value
                is Token.LParen -> {
                    val v = parseExpression()
                    val close = consume()
                    require(close is Token.RParen)
                    v
                }
                is Token.Ident -> {
                    if (t.name in setOf("sqrt", "sin", "cos", "tan", "log", "ln", "abs")) {
                        val arg = if (peek() is Token.LParen) {
                            consume()
                            val inner = parseExpression()
                            val close = consume()
                            require(close is Token.RParen)
                            inner
                        } else {
                            parseUnary()
                        }
                        when (t.name) {
                            "sqrt" -> sqrt(arg)
                            "sin" -> sin(Math.toRadians(arg))
                            "cos" -> cos(Math.toRadians(arg))
                            "tan" -> tan(Math.toRadians(arg))
                            "log" -> log10(arg)
                            "ln" -> ln(arg)
                            "abs" -> abs(arg)
                            else -> throw IllegalArgumentException("Unknown func")
                        }
                    } else if (variable != null && t.name.length == 1 && t.name[0] == variable) {
                        varValue
                    } else if (t.name == "pi") {
                        Math.PI
                    } else if (t.name == "e") {
                        Math.E
                    } else {
                        throw IllegalArgumentException("Unknown identifier: ${t.name}")
                    }
                }
                else -> throw IllegalArgumentException("Unexpected token: $t")
            }
        }
    }
}
