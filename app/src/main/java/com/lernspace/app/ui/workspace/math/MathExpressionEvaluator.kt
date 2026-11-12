package com.lernspace.app.ui.workspace.math

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.E
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class MathSolveResult(
    val normalizedExpression: String,
    val formattedAnswer: String,
    val numericValue: Double? = null
)

/**
 * Pure Kotlin offline recursive-descent mathematical expression evaluator,
 * algebraic variable-aware solver (linear, quadratic, and numeric 1-variable roots),
 * power/exponent evaluator, and stacked-fraction friendly parser.
 */
object MathExpressionEvaluator {

    private val FUNCTIONS = setOf("sqrt", "sin", "cos", "tan", "log", "ln", "abs")
    private val CONSTANTS = setOf("pi", "e")

    private val SUPERSCRIPT_MAP = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-', '⁽' to '(', '⁾' to ')'
    )

    private val UNICODE_FRACTION_MAP = mapOf(
        '½' to "(1/2)", '⅓' to "(1/3)", '⅔' to "(2/3)",
        '¼' to "(1/4)", '¾' to "(3/4)", '⅕' to "(1/5)",
        '⅖' to "(2/5)", '⅗' to "(3/5)", '⅘' to "(4/5)",
        '⅙' to "(1/6)", '⅚' to "(5/6)", '⅛' to "(1/8)",
        '⅜' to "(3/8)", '⅝' to "(5/8)", '⅞' to "(7/8)"
    )

    fun normalizeMathString(raw: String, declaredVariables: Set<String> = emptySet()): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""

        // Expand Unicode vulgar fractions and superscript runs before character replacements
        s = expandUnicodeMathChars(s)

        // Handle assignment with := or colon
        s = s.replace(":=", "=")
        s = s.replace(Regex("""(?<=^[a-zA-Z])\s*:\s*(?=[-+0-9])"""), "=")

        s = s.replace('·', '*')
            .replace('∙', '*')
            .replace('÷', '/')
            .replace('∶', '/')
            .replace(':', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace("√", "sqrt")
            .replace('[', '(')
            .replace(']', ')')
            .replace('{', '(')
            .replace('}', ')')
            .replace(" ", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")

        // Comma as decimal separator (e.g. "2,5" -> "2.5")
        s = s.replace(Regex("""(?<=\d),(?=\d)"""), ".")

        // Disambiguate Unicode '×': ML Kit often reads handwritten variable 'x' as '×'.
        // Keep '×' as '*' ONLY when flanked by valid left & right operands; otherwise restore 'x'.
        if ('×' in s) {
            val sb = StringBuilder(s.length)
            for (i in s.indices) {
                val ch = s[i]
                if (ch == '×') {
                    val leftOk = i > 0 && (s[i - 1].isLetterOrDigit() || s[i - 1] == ')')
                    val rightOk = i < s.lastIndex && (s[i + 1].isLetterOrDigit() || s[i + 1] == '(')
                    sb.append(if (leftOk && rightOk) '*' else 'x')
                } else {
                    sb.append(ch)
                }
            }
            s = sb.toString()
        }

        // Common OCR 'x' multiplication fixes:
        // Only treat digit-x-digit as multiplication when 'x' is NOT a declared variable.
        val xIsDeclared = "x" in declaredVariables.map { it.lowercase() }
        if (!xIsDeclared) {
            s = s.replace(Regex("""(?<=[0-9)])[xX](?=[0-9(])"""), "*")
        }

        // Normalize trailing question mark
        s = s.trimEnd('?')
        return s
    }

    private fun expandUnicodeMathChars(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val frac = UNICODE_FRACTION_MAP[c]
            if (frac != null) {
                sb.append(frac)
                i++
                continue
            }
            if (c in SUPERSCRIPT_MAP) {
                val run = StringBuilder()
                while (i < input.length && input[i] in SUPERSCRIPT_MAP) {
                    run.append(SUPERSCRIPT_MAP[input[i]])
                    i++
                }
                val expStr = run.toString()
                if (expStr.length == 1 && expStr[0].isDigit()) {
                    sb.append('^').append(expStr)
                } else {
                    sb.append("^(").append(expStr).append(')')
                }
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /**
     * Quick plausibility check so non-math words (e.g. "hello", "cat")
     * don't get mis-evaluated as algebra or noise.
     */
    fun isPlausibleMathExpression(raw: String, variables: Map<String, Double> = emptyMap()): Boolean {
        val preMulti = raw.trim().replace(Regex("""(?<=\d),(?=\d)"""), ".")
        val stmts = preMulti.split(Regex("""[\n,;]+""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (stmts.size >= 2) {
            val running = variables.toMutableMap()
            for (i in 0 until stmts.lastIndex) {
                val assign = parseVariableAssignment(stmts[i], running) ?: return false
                running[assign.first] = assign.second
            }
            return isSinglePlausibleExpr(stmts.last(), running)
        }
        return isSinglePlausibleExpr(raw, variables)
    }

    private fun isSinglePlausibleExpr(raw: String, variables: Map<String, Double>): Boolean {
        val norm = normalizeMathString(raw, variables.keys)
        if (norm.isEmpty()) return false

        val lowerVars = variables.keys.map { it.lowercase() }.toSet()
        // Reject unknown multi-letter English/Indonesian words like "hello + 2" or "cat = 5"
        for (match in Regex("""[a-zA-Z]{2,}""").findAll(norm)) {
            val word = match.value.lowercase()
            val allDeclaredVars = word.all { it.toString() in lowerVars }
            val knownWithFnPrefix = word in FUNCTIONS || word in CONSTANTS ||
                FUNCTIONS.any { word.endsWith(it) && word.dropLast(it.length).all { ch -> ch.toString() in lowerVars } }
            val declaredPlusOneUnknown = word.length == 2 &&
                word.count { it.toString() in lowerVars } >= 1 &&
                '=' in norm
            if (!allDeclaredVars && !knownWithFnPrefix && !declaredPlusOneUnknown) return false
        }

        val sides = norm.trimEnd('=').split('=')
        if (sides.size > 2 || sides.any { it.isEmpty() }) return false
        val tokens = sides.flatMap { side ->
            runCatching { tokenize(side) }.getOrNull() ?: return false
        }
        if (tokens.isEmpty()) return false

        val hasDigit = norm.any { it.isDigit() }
        val hasMathOperator = norm.any { it in "+-*/^=%" }
        val hasDeclaredVar = tokens.any { it is Token.Ident && it.name in lowerVars }
        val hasSingleVarEquation = '=' in norm && tokens.any { it is Token.Ident && it.name.length == 1 }

        if (!hasDigit && !hasDeclaredVar && !hasSingleVarEquation) return false
        if (!hasMathOperator && !hasDeclaredVar && norm.none { it == '(' }) {
            return false
        }
        return true
    }

    /**
     * Parses a variable declaration line such as "x = 5", "a = 2.5", "b = 1/2", or "y = 2x + 1".
     * Also supports multiple comma/semicolon/newline-separated declarations like "x = 5, y = 3".
     */
    fun parseVariableAssignments(
        raw: String,
        existingVars: Map<String, Double> = emptyMap()
    ): Map<String, Double> {
        val preNormalized = raw.trim().replace(Regex("""(?<=\d),(?=\d)"""), ".")
        val parts = preNormalized.split(Regex("""[\n,;]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        val running = existingVars.toMutableMap()
        for (part in parts) {
            val parsed = parseVariableAssignment(part, running) ?: return emptyMap()
            result[parsed.first] = parsed.second
            running[parsed.first] = parsed.second
        }
        return result
    }

    fun parseVariableAssignment(
        raw: String,
        existingVars: Map<String, Double> = emptyMap()
    ): Pair<String, Double>? {
        val norm = normalizeMathString(raw, existingVars.keys)
        val eqIdx = norm.indexOf('=')
        if (eqIdx <= 0 || norm.indexOf('=', eqIdx + 1) != -1) return null

        val lhs = norm.substring(0, eqIdx).trim()
        val rhs = norm.substring(eqIdx + 1).trim().trimEnd('.')
        if (rhs.isEmpty()) return null

        // Single-letter variable
        if (!Regex("""^[a-zA-Z]$""").matches(lhs)) return null
        val varName = lhs.lowercase()
        if (varName in FUNCTIONS || varName == "pi") return null

        val value = tryEvaluate(rhs, existingVars) ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return varName to value
    }

    fun solve(
        rawInput: String,
        variables: Map<String, Double> = emptyMap()
    ): MathSolveResult? {
        val lowerVars = variables.entries.associate { (k, v) -> k.lowercase() to v }

        // Support multi-statement expressions separated by newline / ';' / ',' (ignoring decimal commas)
        val multiPre = rawInput.trim().replace(Regex("""(?<=\d),(?=\d)"""), ".")
        val rawSegments = multiPre.split(Regex("""[\n,;]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (rawSegments.size >= 2) {
            val scopedVars = lowerVars.toMutableMap()
            var prefixOk = true
            for (i in 0 until rawSegments.lastIndex) {
                val assign = parseVariableAssignment(rawSegments[i], scopedVars)
                if (assign != null) {
                    scopedVars[assign.first] = assign.second
                } else {
                    prefixOk = false
                    break
                }
            }
            if (prefixOk) {
                return solveSingleStatement(rawSegments.last(), scopedVars)
            }
        }

        return solveSingleStatement(rawInput, lowerVars)
    }

    private fun solveSingleStatement(
        rawInput: String,
        lowerVars: Map<String, Double>
    ): MathSolveResult? {
        val normalized = normalizeMathString(rawInput, lowerVars.keys)
        if (normalized.isEmpty()) return null

        // Strip trailing '=' when user writes "2 + 3 =" or "2x + y ="
        val cleaned = normalized.trimEnd('=')
        if (cleaned.isEmpty()) return null

        // If cleaned string contains '=', treat as equation or variable assignment
        if ('=' in cleaned) {
            val parts = cleaned.split('=')
            if (parts.size != 2) return null
            val lhs = parts[0]
            val rhs = parts[1]
            if (lhs.isEmpty() || rhs.isEmpty()) return null

            // Case 1: Simple declaration like "x = 5" or "x = 2 + 3" -> show resolved value
            val assign = parseVariableAssignment(cleaned, lowerVars)
            if (assign != null) {
                return MathSolveResult(
                    normalizedExpression = cleaned,
                    formattedAnswer = "${assign.first} = ${formatNumber(assign.second)}",
                    numericValue = assign.second
                )
            }

            // Case 2: Identify unknown variables not present in lowerVars
            val unknowns = extractUnknownIdentifiers(lhs, rhs, lowerVars.keys)
            if (unknowns.isEmpty()) {
                val lhsVal = tryEvaluate(lhs, lowerVars)
                val rhsVal = tryEvaluate(rhs, lowerVars)
                if (lhsVal != null && rhsVal != null && abs(lhsVal - rhsVal) < 1e-6) {
                    return MathSolveResult(cleaned, formatNumber(lhsVal), lhsVal)
                }
                // Even if x was declared earlier on the page, solve a standalone 1-var equation like "2x + 1 = 15"
                val allUsedVars = extractUnknownIdentifiers(lhs, rhs, emptySet())
                if (allUsedVars.size == 1) {
                    val v = allUsedVars.first()
                    return solveSingleVariableEquation(cleaned, lhs, rhs, v, lowerVars - v)
                }
                return null
            }

            if (unknowns.size == 1) {
                return solveSingleVariableEquation(cleaned, lhs, rhs, unknowns.first(), lowerVars)
            }
            return null
        }

        // Pure numeric or variable-substituted expression
        val value = tryEvaluate(cleaned, lowerVars) ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return MathSolveResult(
            normalizedExpression = cleaned,
            formattedAnswer = formatNumber(value),
            numericValue = value
        )
    }

    private fun extractUnknownIdentifiers(lhs: String, rhs: String, knownVars: Set<String>): Set<String> {
        val tokens = (runCatching { tokenize(lhs) }.getOrNull() ?: return emptySet()) +
            (runCatching { tokenize(rhs) }.getOrNull() ?: return emptySet())
        val unknowns = LinkedHashSet<String>()
        for (tok in tokens) {
            if (tok is Token.Ident) {
                if (tok.name !in FUNCTIONS && tok.name !in CONSTANTS && tok.name !in knownVars) {
                    unknowns.add(tok.name)
                }
            }
        }
        return unknowns
    }

    private fun solveSingleVariableEquation(
        fullExpr: String,
        lhs: String,
        rhs: String,
        variable: String,
        knownVars: Map<String, Double>
    ): MathSolveResult? {
        fun evalDiff(v: Double): Double? {
            val env = knownVars + (variable to v)
            val l = tryEvaluate(lhs, env) ?: return null
            val r = tryEvaluate(rhs, env) ?: return null
            return l - r
        }

        val f0 = evalDiff(0.0)
        val f1 = evalDiff(1.0)
        val f2 = evalDiff(2.0)

        if (f0 != null && f1 != null && f2 != null) {
            val d1 = f1 - f0
            val d2 = f2 - f1

            // 1) Check Linear: f(v) = m*v + c
            if (abs(d2 - d1) < 1e-7 && abs(d1) > 1e-9) {
                val fHalf = evalDiff(0.5)
                val fTen = evalDiff(10.0)
                if (fHalf != null && abs(fHalf - (f0 + 0.5 * d1)) < 1e-6 &&
                    fTen != null && abs(fTen - (f0 + 10.0 * d1)) < 1e-5
                ) {
                    val root = -f0 / d1
                    val check = evalDiff(root)
                    if (check != null && abs(check) < 1e-5) {
                        return MathSolveResult(
                            normalizedExpression = fullExpr,
                            formattedAnswer = "$variable = ${formatNumber(root)}",
                            numericValue = root
                        )
                    }
                }
            }

            // 2) Check Quadratic: f(v) = a*v^2 + b*v + c
            val a = (d2 - d1) / 2.0
            val b = d1 - a
            val c = f0
            if (abs(a) > 1e-9) {
                val fNeg1 = evalDiff(-1.0)
                val f3 = evalDiff(3.0)
                val matchesQuad = fNeg1 != null && abs(fNeg1 - (a - b + c)) < 1e-5 &&
                    f3 != null && abs(f3 - (9 * a + 3 * b + c)) < 1e-5
                if (matchesQuad) {
                    val disc = b * b - 4.0 * a * c
                    if (disc < -1e-7) return null
                    if (abs(disc) <= 1e-7) {
                        val root = -b / (2.0 * a)
                        return MathSolveResult(
                            normalizedExpression = fullExpr,
                            formattedAnswer = "$variable = ${formatNumber(root)}",
                            numericValue = root
                        )
                    }
                    val sqrtD = sqrt(disc)
                    val r1 = (-b - sqrtD) / (2.0 * a)
                    val r2 = (-b + sqrtD) / (2.0 * a)
                    val low = minOf(r1, r2)
                    val high = maxOf(r1, r2)
                    val formatted = if (abs(low + high) < 1e-7 && abs(high) > 1e-7) {
                        "$variable = ${formatNumber(low)}; ${formatNumber(high)}"
                    } else {
                        "$variable = ${formatNumber(low)}; ${formatNumber(high)}"
                    }
                    return MathSolveResult(
                        normalizedExpression = fullExpr,
                        formattedAnswer = formatted,
                        numericValue = high
                    )
                }
            }
        }

        // 3) Numeric root scan + bisection fallback for higher-order / fractional powers (e.g. x^3 = 27)
        val samples = (-200..200).map { it * 0.5 }
        val roots = mutableListOf<Double>()
        var prevX: Double? = null
        var prevY: Double? = null

        for (x in samples) {
            val y = evalDiff(x)
            if (y == null || y.isNaN() || y.isInfinite()) {
                prevX = null
                prevY = null
                continue
            }
            if (abs(y) < 1e-7) {
                addUniqueRoot(roots, x)
            } else if (prevX != null && prevY != null && prevY * y < 0.0) {
                val bisected = bisectRoot(::evalDiff, prevX, x)
                if (bisected != null) addUniqueRoot(roots, bisected)
            }
            prevX = x
            prevY = y
        }

        if (roots.isEmpty()) return null
        val sorted = roots.sorted().take(3)
        val formatted = if (sorted.size == 1) {
            "$variable = ${formatNumber(sorted[0])}"
        } else {
            "$variable = " + sorted.joinToString("; ") { formatNumber(it) }
        }
        return MathSolveResult(
            normalizedExpression = fullExpr,
            formattedAnswer = formatted,
            numericValue = sorted.last()
        )
    }

    private fun addUniqueRoot(roots: MutableList<Double>, candidate: Double) {
        if (roots.none { abs(it - candidate) < 1e-4 }) {
            roots.add(candidate)
        }
    }

    private fun bisectRoot(f: (Double) -> Double?, loInit: Double, hiInit: Double): Double? {
        var lo = loInit
        var hi = hiInit
        var fLo = f(lo) ?: return null
        for (iter in 0 until 50) {
            val mid = (lo + hi) * 0.5
            val fMid = f(mid) ?: return null
            if (abs(fMid) < 1e-9 || abs(hi - lo) < 1e-9) return mid
            if (fLo * fMid <= 0.0) {
                hi = mid
            } else {
                lo = mid
                fLo = fMid
            }
        }
        val finalMid = (lo + hi) * 0.5
        val finalErr = f(finalMid) ?: return null
        return if (abs(finalErr) < 1e-4) finalMid else null
    }

    fun tryEvaluate(expr: String, variables: Map<String, Double> = emptyMap()): Double? {
        return try {
            val tokens = tokenize(expr)
            if (tokens.isEmpty()) return null
            val parser = Parser(tokens, variables)
            val result = parser.parseExpression()
            if (parser.hasMore()) return null
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    fun formatNumber(value: Double): String {
        val clean = if (abs(value) < 1e-9) 0.0 else value
        val roundedInt = kotlin.math.round(clean)
        if (abs(clean - roundedInt) < 1e-7 && abs(roundedInt) < 1e12) {
            return roundedInt.toLong().toString()
        }
        return BigDecimal.valueOf(clean)
            .setScale(4, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    // --- Tokenizer & Recursive-Descent Parser ---

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Ident(val name: String) : Token()
        data class Op(val ch: Char) : Token()
        object LParen : Token()
        object RParen : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val out = ArrayList<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    var dotSeen = c == '.'
                    i++
                    while (i < expr.length && (expr[i].isDigit() || (expr[i] == '.' && !dotSeen))) {
                        if (expr[i] == '.') dotSeen = true
                        i++
                    }
                    val numStr = expr.substring(start, i)
                    val num = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Bad num: $numStr")
                    out.add(Token.Num(num))
                }
                c.isLetter() -> {
                    val start = i
                    i++
                    while (i < expr.length && expr[i].isLetter()) i++
                    val rawWord = expr.substring(start, i).lowercase()
                    if (rawWord in FUNCTIONS || rawWord in CONSTANTS || rawWord.length == 1) {
                        out.add(Token.Ident(rawWord))
                    } else {
                        // Split adjacent variables like "xy" or "ax" into individual variable tokens
                        // unless trailing letters match a known function like "xsin" (rare)
                        for (ch in rawWord) {
                            out.add(Token.Ident(ch.toString()))
                        }
                    }
                }
                c == '(' -> {
                    out.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    out.add(Token.RParen)
                    i++
                }
                c in "+-*/^%" -> {
                    out.add(Token.Op(c))
                    i++
                }
                else -> throw IllegalArgumentException("Unexpected char: $c")
            }
        }
        return out
    }

    private class Parser(
        private val tokens: List<Token>,
        private val variables: Map<String, Double>
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
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parsePower()
            while (true) {
                val t = peek()
                when {
                    t is Token.Op && (t.ch == '*' || t.ch == '/') -> {
                        consume()
                        val right = parsePower()
                        left = if (t.ch == '*') left * right else {
                            if (abs(right) < 1e-12) Double.NaN else left / right
                        }
                    }
                    // Implicit multiplication: 2x, 2(x+1), (a+1)(b+2), xy, 2sqrt(9)
                    t is Token.LParen || t is Token.Ident -> {
                        val right = parsePower()
                        left *= right
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            val t = peek()
            return if (t is Token.Op && t.ch == '^') {
                consume()
                val exponent = parsePower()
                base.pow(exponent)
            } else {
                base
            }
        }

        private fun parseUnary(): Double {
            val t = peek()
            return if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                consume()
                val valInner = parseUnary()
                if (t.ch == '-') -valInner else valInner
            } else {
                parsePostfix()
            }
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Token.Op && t.ch == '%') {
                    consume()
                    value /= 100.0
                } else {
                    break
                }
            }
            return value
        }

        private fun parsePrimary(): Double {
            val t = peek() ?: throw IllegalStateException("Unexpected end")
            return when (t) {
                is Token.Num -> {
                    consume()
                    t.value
                }
                is Token.LParen -> {
                    consume()
                    val v = parseExpression()
                    if (peek() !is Token.RParen) throw IllegalStateException("Missing )")
                    consume()
                    v
                }
                is Token.Ident -> {
                    consume()
                    val name = t.name
                    when {
                        name == "pi" -> PI
                        name in FUNCTIONS -> {
                            val arg = if (peek() is Token.LParen) {
                                consume()
                                val inner = parseExpression()
                                if (peek() !is Token.RParen) throw IllegalStateException("Missing )")
                                consume()
                                inner
                            } else {
                                parsePower()
                            }
                            when (name) {
                                "sqrt" -> if (arg < 0.0) Double.NaN else sqrt(arg)
                                "sin" -> sin(Math.toRadians(arg))
                                "cos" -> cos(Math.toRadians(arg))
                                "tan" -> tan(Math.toRadians(arg))
                                "log" -> if (arg <= 0.0) Double.NaN else log10(arg)
                                "ln" -> if (arg <= 0.0) Double.NaN else ln(arg)
                                "abs" -> abs(arg)
                                else -> throw IllegalStateException("Unknown fn $name")
                            }
                        }
                        name in variables -> variables.getValue(name)
                        name == "e" -> E
                        else -> throw IllegalStateException("Unbound variable: $name")
                    }
                }
                else -> throw IllegalStateException("Unexpected token: $t")
            }
        }
    }
}
