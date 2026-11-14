package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.theme.*

/**
 * Unified Markdown + LaTeX math engine and Jetpack Compose renderer.
 * Features:
 * - 2D stacked fractions (horizontal bar matching width)
 * - Square roots with vinculum overline bar
 * - Authentic math typography: italic variables (x, y, z), upright numbers (0-9), upright functions (sin, cos, ln)
 * - Multi-line aligned equations and matrices
 * - Flowing inline math inside sentences without card fragmentation
 * - Seamless monochrome design matching Lernspace UI
 */
object LatexMathFormatter {

    val SYMBOL_MAP = mapOf(
        // Greek lowercase
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ",
        "pi" to "π", "varpi" to "ϖ", "rho" to "ρ", "varrho" to "ϱ",
        "sigma" to "σ", "varsigma" to "ς", "tau" to "τ", "upsilon" to "υ",
        "phi" to "φ", "varphi" to "ϕ", "chi" to "χ", "psi" to "ψ", "omega" to "ω",

        // Greek uppercase
        "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ",
        "Xi" to "Ξ", "Pi" to "Π", "Sigma" to "Σ", "Upsilon" to "Υ",
        "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω",

        // Operators & Relations
        "times" to " × ", "cdot" to " · ", "div" to " ÷ ", "pm" to " ± ", "mp" to " ∓ ",
        "ast" to " * ", "star" to " ★ ", "bullet" to " • ",
        "approx" to " ≈ ", "neq" to " ≠ ", "ne" to " ≠ ",
        "leq" to " ≤ ", "le" to " ≤ ", "geq" to " ≥ ", "ge" to " ≥ ",
        "equiv" to " ≡ ", "propto" to " ∝ ", "sim" to " ∼ ", "simeq" to " ≃ ", "cong" to " ≅ ",
        "ll" to " ≪ ", "gg" to " ≫ ",

        // Arrows
        "to" to " → ", "rightarrow" to " → ", "leftarrow" to " ← ", "leftrightarrow" to " ↔ ",
        "Rightarrow" to " ⇒ ", "Leftarrow" to " ⇐ ", "Leftrightarrow" to " ⇔ ",
        "implies" to " ⇒ ", "iff" to " ⇔ ", "uparrow" to " ↑ ", "downarrow" to " ↓ ",
        "nearrow" to " ↗ ", "searrow" to " ↘ ", "nwarrow" to " ↖ ", "swarrow" to " ↙ ",

        // Logic & Sets
        "in" to " ∈ ", "notin" to " ∉ ", "ni" to " ∋ ",
        "subset" to " ⊂ ", "subseteq" to " ⊆ ", "supset" to " ⊃ ", "supseteq" to " ⊇ ",
        "cup" to " ∪ ", "cap" to " ∩ ", "setminus" to " \\ ",
        "emptyset" to "∅", "varnothing" to "∅",
        "forall" to "∀", "exists" to "∃", "nexists" to "∄",
        "land" to " ∧ ", "wedge" to " ∧ ", "lor" to " ∨ ", "vee" to " ∨ ",
        "neg" to "¬", "lnot" to "¬",
        "therefore" to "∴", "because" to "∵",

        // Calculus, Geometry & Primes
        "infty" to "∞", "partial" to "∂", "nabla" to "∇",
        "int" to "∫", "iint" to "∬", "iiint" to "∭", "oint" to "∮",
        "sum" to "∑", "prod" to "∏", "coprod" to "∐",
        "circ" to "°", "degree" to "°", "angle" to "∠", "perp" to "⊥", "parallel" to "∥",
        "triangle" to "△", "prime" to "′",

        // Dots & Ellipses
        "ldots" to "…", "cdots" to "⋯", "dots" to "…", "vdots" to "⋮", "ddots" to "⋱",

        // Standard Functions
        "sin" to "sin", "cos" to "cos", "tan" to "tan", "cot" to "cot",
        "sec" to "sec", "csc" to "csc", "arcsin" to "arcsin", "arccos" to "arccos", "arctan" to "arctan",
        "sinh" to "sinh", "cosh" to "cosh", "tanh" to "tanh",
        "log" to "log", "ln" to "ln", "exp" to "exp",
        "lim" to "lim", "max" to "max", "min" to "min", "det" to "det",
        "gcd" to "gcd", "lcm" to "lcm", "mod" to "mod", "deg" to "deg",

        // Delimiters & Spacing
        "quad" to "  ", "qquad" to "    ", "enspace" to " ", "thinspace" to " ",
        "," to " ", ";" to " ", ":" to " ", "!" to "", " " to " ",
        "%" to "%", "$" to "$", "{" to "{", "}" to "}", "&" to "&", "_" to "_"
    )

    val MATH_FUNCTIONS = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc",
        "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh",
        "log", "ln", "exp", "lim", "max", "min", "det", "gcd", "lcm", "mod", "deg"
    )

    val UNICODE_FRACTIONS = mapOf(
        "1/2" to "½", "1/3" to "⅓", "2/3" to "⅔",
        "1/4" to "¼", "3/4" to "¾",
        "1/5" to "⅕", "2/5" to "⅖", "3/5" to "⅗", "4/5" to "⅘",
        "1/6" to "⅙", "5/6" to "⅚",
        "1/8" to "⅛", "3/8" to "⅜", "5/8" to "⅝", "7/8" to "⅞"
    )

    val UNICODE_SUP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ',
        'x' to 'ˣ', 'y' to 'ʸ', 'k' to 'ᵏ', 'm' to 'ᵐ', 't' to 'ᵗ'
    )

    val UNICODE_SUB = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'i' to 'ᵢ', 'o' to 'ₒ', 'x' to 'ₓ',
        'k' to 'ₖ', 'm' to 'ₘ', 'n' to 'ₙ', 'p' to 'ₚ', 't' to 'ₜ'
    )

    fun readBracedGroup(src: String, startIdx: Int): Pair<String, Int>? {
        var i = startIdx
        while (i < src.length && src[i] == ' ') i++
        if (i >= src.length || src[i] != '{') return null
        var depth = 0
        val contentStart = i + 1
        while (i < src.length) {
            when (src[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return src.substring(contentStart, i) to (i + 1)
                    }
                }
            }
            i++
        }
        return src.substring(contentStart) to src.length
    }

    fun readBracketGroup(src: String, startIdx: Int): Pair<String, Int>? {
        var i = startIdx
        while (i < src.length && src[i] == ' ') i++
        if (i >= src.length || src[i] != '[') return null
        var depth = 0
        val contentStart = i + 1
        while (i < src.length) {
            when (src[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return src.substring(contentStart, i) to (i + 1)
                }
            }
            i++
        }
        return null
    }

    fun readTokenOrBraced(src: String, startIdx: Int): Pair<String, Int> {
        var i = startIdx
        while (i < src.length && src[i] == ' ') i++
        if (i >= src.length) return "" to i
        if (src[i] == '{') {
            return readBracedGroup(src, i) ?: ("" to src.length)
        }
        if (src[i] == '\\') {
            var j = i + 1
            while (j < src.length && src[j].isLetter()) j++
            if (j < src.length && src[j] == '{') {
                val grp = readBracedGroup(src, j)
                if (grp != null) return src.substring(i, grp.second) to grp.second
            }
            return src.substring(i, j.coerceAtLeast(i + 1)) to j.coerceAtLeast(i + 1)
        }
        return src[i].toString() to (i + 1)
    }

    fun appendLatexMath(
        builder: AnnotatedString.Builder,
        expr: String,
        baseFontSize: TextUnit,
        mathColor: Color
    ) {
        val style = SpanStyle(
            color = mathColor,
            fontFamily = FontFamily.Serif
        )
        builder.pushStyle(style)
        appendLatexInner(builder, expr.trim(), baseFontSize, scaleFactor = 1f)
        builder.pop()
    }

    fun appendLatexInner(
        builder: AnnotatedString.Builder,
        expr: String,
        baseFontSize: TextUnit,
        scaleFactor: Float
    ) {
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch == '\\' -> {
                    if (i + 1 < expr.length && expr[i + 1] == '\\') {
                        builder.append('\n')
                        i += 2
                        continue
                    }
                    if (i + 1 < expr.length && !expr[i + 1].isLetter()) {
                        val sym = expr[i + 1].toString()
                        builder.append(SYMBOL_MAP[sym] ?: sym)
                        i += 2
                        continue
                    }
                    var j = i + 1
                    while (j < expr.length && expr[j].isLetter()) j++
                    val cmd = expr.substring(i + 1, j)
                    when (cmd) {
                        "displaystyle", "limits", "nolimits" -> {
                            i = j
                        }
                        "left", "right" -> {
                            var k = j
                            while (k < expr.length && expr[k] == ' ') k++
                            if (k < expr.length && expr[k] == '.') {
                                i = k + 1
                            } else {
                                i = j
                            }
                        }
                        "begin", "end" -> {
                            val grp = readBracedGroup(expr, j)
                            i = grp?.second ?: j
                        }
                        "text", "mathrm" -> {
                            val grp = readBracedGroup(expr, j)
                            if (grp != null) {
                                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Default))
                                builder.append(grp.first)
                                builder.pop()
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "textbf", "mathbf" -> {
                            val grp = readBracedGroup(expr, j)
                            if (grp != null) {
                                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Normal))
                                builder.append(grp.first)
                                builder.pop()
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "mathit" -> {
                            val grp = readBracedGroup(expr, j)
                            if (grp != null) {
                                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                                builder.append(grp.first)
                                builder.pop()
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "mathbb" -> {
                            val grp = readBracedGroup(expr, j)
                            if (grp != null) {
                                val bbMap = mapOf('R' to "ℝ", 'N' to "ℕ", 'Z' to "ℤ", 'Q' to "ℚ", 'C' to "ℂ", 'P' to "ℙ")
                                val mapped = grp.first.map { bbMap[it] ?: it.toString() }.joinToString("")
                                builder.append(mapped)
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "frac", "dfrac", "tfrac" -> {
                            val num = readTokenOrBraced(expr, j)
                            val den = readTokenOrBraced(expr, num.second)
                            if (num.first.isNotEmpty() && den.first.isNotEmpty()) {
                                val numClean = num.first.trim()
                                val denClean = den.first.trim()
                                val fracKey = "$numClean/$denClean"
                                val unicodeGlyph = UNICODE_FRACTIONS[fracKey]
                                if (unicodeGlyph != null) {
                                    builder.append(unicodeGlyph)
                                } else if (numClean.length <= 3 && denClean.length <= 3 &&
                                    numClean.all { it.isDigit() } && denClean.all { it.isDigit() }
                                ) {
                                    val supNum = numClean.map { UNICODE_SUP[it] ?: it }.joinToString("")
                                    val subDen = denClean.map { UNICODE_SUB[it] ?: it }.joinToString("")
                                    builder.append("$supNum⁄$subDen")
                                } else {
                                    appendLatexInner(builder, numClean, baseFontSize, scaleFactor)
                                    builder.append(" / ")
                                    appendLatexInner(builder, denClean, baseFontSize, scaleFactor)
                                }
                                i = den.second
                            } else {
                                i = j
                            }
                        }
                        "sqrt" -> {
                            var k = j
                            val rootOrder = readBracketGroup(expr, k)
                            if (rootOrder != null) {
                                val orderSup = rootOrder.first.map { UNICODE_SUP[it] ?: it }.joinToString("")
                                builder.append(orderSup)
                                k = rootOrder.second
                            }
                            val inner = readTokenOrBraced(expr, k)
                            if (inner.first.isNotEmpty()) {
                                val trimmedInner = inner.first.trim()
                                builder.append("√")
                                val needsParens = trimmedInner.contains('+') || trimmedInner.contains('-') || trimmedInner.contains(' ')
                                if (needsParens) builder.append('(')
                                appendLatexInner(builder, trimmedInner, baseFontSize, scaleFactor)
                                if (needsParens) builder.append(')')
                                i = inner.second
                            } else {
                                builder.append('√')
                                i = k
                            }
                        }
                        "vec" -> {
                            val grp = readTokenOrBraced(expr, j)
                            if (grp.first.isNotEmpty()) {
                                appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                                builder.append('\u20D7')
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "bar", "overline" -> {
                            val grp = readTokenOrBraced(expr, j)
                            if (grp.first.isNotEmpty()) {
                                appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                                builder.append('\u0304')
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "hat" -> {
                            val grp = readTokenOrBraced(expr, j)
                            if (grp.first.isNotEmpty()) {
                                appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                                builder.append('\u0302')
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        else -> {
                            val mapped = SYMBOL_MAP[cmd]
                            if (mapped != null) {
                                builder.append(mapped)
                            } else if (cmd in MATH_FUNCTIONS) {
                                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Default))
                                builder.append(cmd)
                                builder.pop()
                                builder.append(" ")
                            } else if (cmd.isNotEmpty()) {
                                builder.append(cmd)
                            }
                            i = j
                        }
                    }
                }
                ch == '^' -> {
                    val (expText, nextIdx) = readTokenOrBraced(expr, i + 1)
                    if (expText == "\\circ" || expText == "circ") {
                        builder.append('°')
                    } else if (expText.isNotEmpty()) {
                        val allUnicodeSup = expText.all { it in UNICODE_SUP }
                        if (allUnicodeSup) {
                            val supStr = expText.map { UNICODE_SUP[it] ?: it }.joinToString("")
                            builder.append(supStr)
                        } else {
                            builder.pushStyle(
                                SpanStyle(
                                    fontSize = baseFontSize * (scaleFactor * 0.72f),
                                    baselineShift = BaselineShift.Superscript
                                )
                            )
                            appendLatexInner(builder, expText, baseFontSize, scaleFactor * 0.78f)
                            builder.pop()
                        }
                    }
                    i = nextIdx
                }
                ch == '_' -> {
                    val (subText, nextIdx) = readTokenOrBraced(expr, i + 1)
                    if (subText.isNotEmpty()) {
                        val allUnicodeSub = subText.all { it in UNICODE_SUB }
                        if (allUnicodeSub) {
                            val subStr = subText.map { UNICODE_SUB[it] ?: it }.joinToString("")
                            builder.append(subStr)
                        } else {
                            builder.pushStyle(
                                SpanStyle(
                                    fontSize = baseFontSize * (scaleFactor * 0.72f),
                                    baselineShift = BaselineShift.Subscript
                                )
                            )
                            appendLatexInner(builder, subText, baseFontSize, scaleFactor * 0.78f)
                            builder.pop()
                        }
                    }
                    i = nextIdx
                }
                ch == '{' -> {
                    val grp = readBracedGroup(expr, i)
                    if (grp != null) {
                        appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                        i = grp.second
                    } else {
                        i++
                    }
                }
                ch == '}' -> i++
                ch == '&' -> {
                    builder.append(" ")
                    i++
                }
                ch == '*' -> {
                    builder.append(" · ")
                    i++
                }
                ch == '-' -> {
                    // Use standard math minus sign
                    val prevIsSpace = i > 0 && expr[i - 1].isWhitespace()
                    val nextIsSpace = i + 1 < expr.length && expr[i + 1].isWhitespace()
                    if (prevIsSpace && nextIsSpace) {
                        builder.append(" − ")
                    } else {
                        builder.append("−")
                    }
                    i++
                }
                ch.isDigit() -> {
                    // Numbers in math are upright
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Normal))
                    builder.append(ch)
                    builder.pop()
                    i++
                }
                ch.isLetter() -> {
                    // Check if starts a math function like sin, cos, tan, log, ln, lim
                    var k = i
                    while (k < expr.length && expr[k].isLetter()) k++
                    val word = expr.substring(i, k)
                    if (word in MATH_FUNCTIONS) {
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Default))
                        builder.append(word)
                        builder.pop()
                        builder.append(" ")
                        i = k
                    } else {
                        // Standard math variable: single letter in italic serif
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif))
                        builder.append(ch)
                        builder.pop()
                        i++
                    }
                }
                else -> {
                    builder.append(ch)
                    i++
                }
            }
        }
    }

    /**
     * Parses inline Markdown (**bold**, *italic*, `code`) and inline/display LaTeX
     * ($$...$$, \[...\], $...$, \(...\), and bare \commands) into a styled AnnotatedString.
     */
    fun formatInlineMarkdownAndLatex(
        raw: String,
        baseFontSize: TextUnit,
        textColor: Color,
        mathColor: Color = textColor,
        codeBgColor: Color = Color(0xFFE2E8F0).copy(alpha = 0.65f)
    ): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            // 1) Display $$ ... $$ inside inline text
            if (raw.startsWith("$$", i)) {
                val close = raw.indexOf("$$", i + 2)
                if (close > i + 2) {
                    appendLatexMath(this, raw.substring(i + 2, close), baseFontSize, mathColor)
                    i = close + 2
                    continue
                }
            }

            // 2) Display \[ ... \] inside inline text
            if (raw.startsWith("\\[", i)) {
                val close = raw.indexOf("\\]", i + 2)
                if (close > i + 2) {
                    appendLatexMath(this, raw.substring(i + 2, close), baseFontSize, mathColor)
                    i = close + 2
                    continue
                }
            }

            // 3) Inline \( ... \)
            if (raw.startsWith("\\(", i)) {
                val close = raw.indexOf("\\)", i + 2)
                if (close > i + 2) {
                    appendLatexMath(this, raw.substring(i + 2, close), baseFontSize, mathColor)
                    i = close + 2
                    continue
                }
            }

            // 4) Single $ ... $ (inline LaTeX)
            if (raw[i] == '$' && (i + 1 >= raw.length || raw[i + 1] != '$')) {
                val close = raw.indexOf('$', i + 1)
                if (close > i + 1 && !raw.substring(i + 1, close).contains('\n')) {
                    appendLatexMath(this, raw.substring(i + 1, close), baseFontSize, mathColor)
                    i = close + 1
                    continue
                }
            }

            // 5) Bare LaTeX command without $ (e.g. \frac{1}{2}, \sqrt{3}, \times, \alpha)
            if (raw[i] == '\\' && i + 1 < raw.length && raw[i + 1].isLetter()) {
                var k = i + 1
                while (k < raw.length && raw[k].isLetter()) k++
                val word = raw.substring(i + 1, k)
                val isMathCmd = word in SYMBOL_MAP || word in MATH_FUNCTIONS ||
                        word in setOf("frac", "dfrac", "tfrac", "sqrt", "binom", "mathbb", "text", "mathrm", "vec", "hat", "bar", "overline")
                if (isMathCmd) {
                    var end = k
                    var braceDepth = 0
                    while (end < raw.length) {
                        val c = raw[end]
                        if (c == '{') braceDepth++
                        else if (c == '}') braceDepth = maxOf(0, braceDepth - 1)
                        else if (braceDepth == 0 && (c == '\n' || c == '$' || c == '`')) break
                        end++
                    }
                    val mathSnippet = raw.substring(i, end).trim()
                    if (mathSnippet.isNotEmpty()) {
                        appendLatexMath(this, mathSnippet, baseFontSize, mathColor)
                        i = end
                        continue
                    }
                }
            }

            // 6) Inline `code`
            if (raw[i] == '`') {
                val close = raw.indexOf('`', i + 1)
                if (close > i + 1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBgColor,
                            fontSize = baseFontSize * 0.92f
                        )
                    )
                    append(raw.substring(i + 1, close))
                    pop()
                    i = close + 1
                    continue
                }
            }

            // 7) Bold **...**
            if (raw.startsWith("**", i)) {
                val close = raw.indexOf("**", i + 2)
                if (close > i + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    val inner = formatInlineMarkdownAndLatex(
                        raw.substring(i + 2, close), baseFontSize, textColor, mathColor, codeBgColor
                    )
                    append(inner)
                    pop()
                    i = close + 2
                    continue
                }
            }

            // 8) Italic *...*
            if (raw[i] == '*' && i + 1 < raw.length && raw[i + 1] != ' ') {
                val close = raw.indexOf('*', i + 1)
                if (close > i + 1 && raw[close - 1] != ' ') {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    val inner = formatInlineMarkdownAndLatex(
                        raw.substring(i + 1, close), baseFontSize, textColor, mathColor, codeBgColor
                    )
                    append(inner)
                    pop()
                    i = close + 1
                    continue
                }
            }

            // 9) Inline Superscript: ^2, ^{12}, ^n, etc.
            if (raw[i] == '^') {
                val (expText, nextIdx) = readTokenOrBraced(raw, i + 1)
                if (expText.isNotEmpty()) {
                    val allUnicodeSup = expText.all { it in UNICODE_SUP }
                    if (allUnicodeSup) {
                        val supStr = expText.map { UNICODE_SUP[it] ?: it }.joinToString("")
                        append(supStr)
                    } else {
                        pushStyle(
                            SpanStyle(
                                fontSize = baseFontSize * 0.75f,
                                baselineShift = BaselineShift.Superscript
                            )
                        )
                        append(expText)
                        pop()
                    }
                    i = nextIdx
                    continue
                }
            }

            // 10) Inline Subscript: _1, _{10}, _i, etc.
            if (raw[i] == '_') {
                val (subText, nextIdx) = readTokenOrBraced(raw, i + 1)
                if (subText.isNotEmpty()) {
                    val allUnicodeSub = subText.all { it in UNICODE_SUB }
                    if (allUnicodeSub) {
                        val subStr = subText.map { UNICODE_SUB[it] ?: it }.joinToString("")
                        append(subStr)
                    } else {
                        pushStyle(
                            SpanStyle(
                                fontSize = baseFontSize * 0.75f,
                                baselineShift = BaselineShift.Subscript
                            )
                        )
                        append(subText)
                        pop()
                    }
                    i = nextIdx
                    continue
                }
            }

            append(raw[i])
            i++
        }
    }

    /**
     * Converts Markdown + LaTeX into clean plain Unicode text for PDF StaticLayout export.
     */
    fun toPlainReadableText(raw: String): String {
        val annotated = buildAnnotatedString {
            val lines = raw.lines()
            lines.forEachIndexed { idx, line ->
                val trimmed = line.trim()
                val cleanedLine = when {
                    trimmed.startsWith("### ") -> trimmed.removePrefix("### ").trim()
                    trimmed.startsWith("## ") -> trimmed.removePrefix("## ").trim()
                    trimmed.startsWith("# ") -> trimmed.removePrefix("# ").trim()
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> "• " + trimmed.drop(2)
                    trimmed == "$$" -> ""
                    trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length > 4 ->
                        trimmed.removeSurrounding("$$").trim()
                    else -> line
                }
                val formatted = formatInlineMarkdownAndLatex(cleanedLine, 14.sp, Color.Black)
                append(unicodeifyPowers(formatted))
                if (idx < lines.lastIndex) append('\n')
            }
        }
        return annotated.text
    }

    private fun unicodeifyPowers(annotated: AnnotatedString): String {
        val text = annotated.text
        val sb = StringBuilder(text.length)
        var idx = 0
        while (idx < text.length) {
            val sup = annotated.spanStyles.any {
                it.start <= idx && idx < it.end && it.item.baselineShift == BaselineShift.Superscript
            }
            val sub = annotated.spanStyles.any {
                it.start <= idx && idx < it.end && it.item.baselineShift == BaselineShift.Subscript
            }
            val ch = text[idx]
            when {
                sup && ch in UNICODE_SUP -> sb.append(UNICODE_SUP[ch])
                sub && ch in UNICODE_SUB -> sb.append(UNICODE_SUB[ch])
                else -> sb.append(ch)
            }
            idx++
        }
        return sb.toString()
    }
}

/**
 * Checks if a LaTeX snippet requires 2D rendering (fractions, square roots, binom, matrix).
 */
fun has2DMath(latex: String): Boolean {
    val l = latex.lowercase()
    return l.contains("\\frac") || l.contains("\\dfrac") || l.contains("\\tfrac") ||
            l.contains("\\sqrt") || l.contains("\\binom") || l.contains("\\begin") ||
            l.contains("\\pmatrix") || l.contains("\\bmatrix")
}

sealed class LineSegment {
    data class Text(val content: String) : LineSegment()
    data class Math(val latex: String) : LineSegment()
}

/**
 * Parses a single line into alternating text and math segments.
 */
fun parseLineSegments(line: String): List<LineSegment> {
    val segments = mutableListOf<LineSegment>()
    var i = 0
    val textBuf = StringBuilder()

    fun flushText() {
        if (textBuf.isNotEmpty()) {
            segments.add(LineSegment.Text(textBuf.toString()))
            textBuf.clear()
        }
    }

    while (i < line.length) {
        // 1) $$ ... $$
        if (line.startsWith("$$", i)) {
            val close = line.indexOf("$$", i + 2)
            if (close > i + 2) {
                flushText()
                val math = line.substring(i + 2, close).trim()
                if (math.isNotEmpty()) segments.add(LineSegment.Math(math))
                i = close + 2
                continue
            }
        }

        // 2) \[ ... \]
        if (line.startsWith("\\[", i)) {
            val close = line.indexOf("\\]", i + 2)
            if (close > i + 2) {
                flushText()
                val math = line.substring(i + 2, close).trim()
                if (math.isNotEmpty()) segments.add(LineSegment.Math(math))
                i = close + 2
                continue
            }
        }

        // 3) \( ... \)
        if (line.startsWith("\\(", i)) {
            val close = line.indexOf("\\)", i + 2)
            if (close > i + 2) {
                flushText()
                val math = line.substring(i + 2, close).trim()
                if (math.isNotEmpty()) segments.add(LineSegment.Math(math))
                i = close + 2
                continue
            }
        }

        // 4) $ ... $ (inline math)
        if (line[i] == '$' && (i + 1 >= line.length || line[i + 1] != '$')) {
            val close = line.indexOf('$', i + 1)
            if (close > i + 1 && !line.substring(i + 1, close).contains('\n')) {
                flushText()
                val math = line.substring(i + 1, close).trim()
                if (math.isNotEmpty()) segments.add(LineSegment.Math(math))
                i = close + 1
                continue
            }
        }

        // 5) Bare 2D math commands like \frac{...}{...}, \sqrt{...}, \binom{...}{...}
        if (line.startsWith("\\frac", i) || line.startsWith("\\dfrac", i) ||
            line.startsWith("\\tfrac", i) || line.startsWith("\\sqrt", i) ||
            line.startsWith("\\binom", i) || line.startsWith("\\begin{", i)
        ) {
            var end = i
            var braceDepth = 0
            while (end < line.length) {
                val c = line[end]
                if (c == '{') braceDepth++
                else if (c == '}') braceDepth = maxOf(0, braceDepth - 1)
                else if (braceDepth == 0 && (c == ' ' || c == '\n' || c == '$' || c == '`')) {
                    // Check if directly followed by another braced group (e.g. \frac{a}{b})
                    var next = end
                    while (next < line.length && line[next] == ' ') next++
                    if (next < line.length && line[next] == '{') {
                        end = next
                        continue
                    }
                    break
                }
                end++
            }
            flushText()
            val math = line.substring(i, end).trim()
            if (math.isNotEmpty()) segments.add(LineSegment.Math(math))
            i = end
            continue
        }

        textBuf.append(line[i])
        i++
    }
    flushText()
    return segments
}

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Bullet(val text: String) : MdBlock()
    data class Numbered(val number: String, val text: String) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class Code(val code: String) : MdBlock()
    data class DisplayMath(val latex: String) : MdBlock()
    object Divider : MdBlock()
}

private fun parseMarkdownLatexBlocks(input: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = input.replace("\r\n", "\n").lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Fenced code block
        if (trimmed.startsWith("```")) {
            val sb = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(lines[i])
                i++
            }
            i++
            blocks.add(MdBlock.Code(sb.toString()))
            continue
        }

        // Standalone Multiline Display math block ($$...$$ or \[...\])
        if (trimmed.startsWith("$$") || trimmed.startsWith("\\[")) {
            val isBracket = trimmed.startsWith("\\[")
            val openTok = if (isBracket) "\\[" else "$$"
            val closeTok = if (isBracket) "\\]" else "$$"
            val singleClose = trimmed.indexOf(closeTok, openTok.length)
            if (singleClose >= openTok.length) {
                val inner = trimmed.substring(openTok.length, singleClose).trim()
                if (inner.isNotEmpty()) blocks.add(MdBlock.DisplayMath(inner))
                i++
                continue
            } else {
                val sb = StringBuilder(trimmed.removePrefix(openTok).trim())
                i++
                while (i < lines.size) {
                    val t = lines[i].trim()
                    val endIdx = t.indexOf(closeTok)
                    if (endIdx >= 0) {
                        val before = t.substring(0, endIdx).trim()
                        if (before.isNotEmpty()) {
                            if (sb.isNotEmpty()) sb.append('\n')
                            sb.append(before)
                        }
                        i++
                        break
                    } else {
                        if (sb.isNotEmpty() && t.isNotEmpty()) sb.append('\n')
                        sb.append(t)
                        i++
                    }
                }
                if (sb.isNotEmpty()) blocks.add(MdBlock.DisplayMath(sb.toString()))
                continue
            }
        }

        if (trimmed == "---" || trimmed == "***") {
            blocks.add(MdBlock.Divider)
            i++
            continue
        }

        // Headings
        val headingMatch = Regex("""^(#{1,4})\s+(.+)$""").matchEntire(trimmed)
        if (headingMatch != null) {
            blocks.add(MdBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2].trim()))
            i++
            continue
        }

        // Bullet list
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            blocks.add(MdBlock.Bullet(trimmed.drop(2).trim()))
            i++
            continue
        }

        // Numbered list
        val numMatch = Regex("""^(\d+\.)\s+(.+)$""").matchEntire(trimmed)
        if (numMatch != null) {
            blocks.add(MdBlock.Numbered(numMatch.groupValues[1], numMatch.groupValues[2].trim()))
            i++
            continue
        }

        // Blockquote
        if (trimmed.startsWith("> ")) {
            blocks.add(MdBlock.Quote(trimmed.removePrefix("> ").trim()))
            i++
            continue
        }

        // Standalone pure math line (e.g. starts with \frac, \sqrt, or is $...$, or bare formula like x^2)
        if (isStandaloneMath(trimmed)) {
            val innerMath = when {
                trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length >= 4 -> trimmed.removeSurrounding("$$").trim()
                trimmed.startsWith("$") && trimmed.endsWith("$") && trimmed.length >= 2 -> trimmed.removeSurrounding("$").trim()
                trimmed.startsWith("\\[") && trimmed.endsWith("\\]") && trimmed.length >= 4 -> trimmed.removePrefix("\\[").removeSuffix("\\]").trim()
                trimmed.startsWith("\\(") && trimmed.endsWith("\\)") && trimmed.length >= 4 -> trimmed.removePrefix("\\(").removeSuffix("\\)").trim()
                else -> trimmed
            }
            if (innerMath.isNotEmpty()) {
                blocks.add(MdBlock.DisplayMath(innerMath))
                i++
                continue
            }
        }

        // Normal paragraph line
        val paraSb = StringBuilder(trimmed)
        i++
        while (i < lines.size) {
            val next = lines[i].trim()
            if (next.isEmpty() || next.startsWith("#") || next.startsWith("- ") ||
                next.startsWith("* ") || next.startsWith("```") || next.startsWith("$$") ||
                next.startsWith("\\[") || next.startsWith("> ") ||
                Regex("""^\d+\.\s+.*""").matches(next)
            ) {
                break
            }
            paraSb.append(' ').append(next)
            i++
        }
        blocks.add(MdBlock.Paragraph(paraSb.toString()))
    }
    return blocks
}

private fun isStandaloneMath(line: String): Boolean {
    val t = line.trim()
    if (t.isEmpty()) return false
    if (t.startsWith("$$") && t.endsWith("$$") && t.length >= 4) return true
    if (t.startsWith("$") && t.endsWith("$") && t.length >= 2) return true
    if (t.startsWith("\\[") && t.endsWith("\\]") && t.length >= 4) return true
    if (t.startsWith("\\(") && t.endsWith("\\)") && t.length >= 4) return true
    if (t.startsWith("\\frac") || t.startsWith("\\dfrac") || t.startsWith("\\tfrac") ||
        t.startsWith("\\sqrt") || t.startsWith("\\sum") || t.startsWith("\\int") ||
        t.startsWith("\\prod") || t.startsWith("\\binom") || t.startsWith("\\begin{")
    ) return true

    // Check if it's an algebraic formula or equation like x^2, x^2 + y^2 = 25, (x-1)^2 = 4, y = 2x + 1
    val hasMathIndicator = t.contains('^') || t.contains('_') || t.contains('\\') ||
            (t.contains('=') && (t.contains('x') || t.contains('y') || t.contains('+') || t.contains('-') || t.contains('*') || t.contains('/'))) ||
            t.startsWith("y=") || t.startsWith("f(x)=") || t.startsWith("g(x)=")
    if (!hasMathIndicator) return false

    // Ensure it doesn't contain conversational prose sentences
    val mathKeywords = setOf("sin", "cos", "tan", "cot", "sec", "csc", "log", "ln", "lim", "exp", "abs", "sqrt", "cbrt", "frac", "cdot", "times")
    val words = t.split(Regex("[^a-zA-Z]+")).filter { it.length >= 4 }
    val hasProse = words.any { it.lowercase() !in mathKeywords }
    return !hasProse
}

private sealed class DisplayMathSegment {
    data class Text(val latex: String) : DisplayMathSegment()
    data class Fraction(val num: String, val den: String) : DisplayMathSegment()
    data class Sqrt(val order: String?, val inner: String) : DisplayMathSegment()
    data class Binom(val n: String, val k: String) : DisplayMathSegment()
    data class Matrix(val bracket: Char?, val rows: List<List<String>>) : DisplayMathSegment()
}

private fun splitDisplaySegments(latex: String): List<DisplayMathSegment> {
    val segments = mutableListOf<DisplayMathSegment>()
    val textBuf = StringBuilder()
    var i = 0
    while (i < latex.length) {
        // 1) Matrices: \begin{pmatrix}, \begin{bmatrix}, \begin{matrix}
        if (latex.startsWith("\\begin{pmatrix}", i) || latex.startsWith("\\begin{bmatrix}", i) || latex.startsWith("\\begin{matrix}", i)) {
            val env = when {
                latex.startsWith("\\begin{pmatrix}", i) -> "pmatrix"
                latex.startsWith("\\begin{bmatrix}", i) -> "bmatrix"
                else -> "matrix"
            }
            val openTag = "\\begin{$env}"
            val closeTag = "\\end{$env}"
            val endIdx = latex.indexOf(closeTag, i + openTag.length)
            if (endIdx > 0) {
                val matrixContent = latex.substring(i + openTag.length, endIdx).trim()
                val rowStrings = matrixContent.split(Regex("""\\\\|\n""")).map { it.trim() }.filter { it.isNotEmpty() }
                val matrixRows = rowStrings.map { row ->
                    row.split('&').map { it.trim() }
                }
                if (textBuf.isNotEmpty()) {
                    segments.add(DisplayMathSegment.Text(textBuf.toString()))
                    textBuf.clear()
                }
                val bracket = when (env) {
                    "pmatrix" -> '('
                    "bmatrix" -> '['
                    else -> null
                }
                segments.add(DisplayMathSegment.Matrix(bracket, matrixRows))
                i = endIdx + closeTag.length
                continue
            }
        }

        // 2) Fractions: \frac, \dfrac, \tfrac
        if (latex.startsWith("\\frac", i) || latex.startsWith("\\dfrac", i) || latex.startsWith("\\tfrac", i)) {
            val cmdLen = when {
                latex.startsWith("\\dfrac", i) || latex.startsWith("\\tfrac", i) -> 6
                else -> 5
            }
            val num = LatexMathFormatter.readTokenOrBraced(latex, i + cmdLen)
            val den = LatexMathFormatter.readTokenOrBraced(latex, num.second)
            if (num.first.isNotEmpty() && den.first.isNotEmpty()) {
                if (textBuf.isNotEmpty()) {
                    segments.add(DisplayMathSegment.Text(textBuf.toString()))
                    textBuf.clear()
                }
                segments.add(DisplayMathSegment.Fraction(num.first.trim(), den.first.trim()))
                i = den.second
                continue
            }
        }

        // 3) Binomial coefficient: \binom{n}{k}
        if (latex.startsWith("\\binom", i)) {
            val num = LatexMathFormatter.readTokenOrBraced(latex, i + 6)
            val den = LatexMathFormatter.readTokenOrBraced(latex, num.second)
            if (num.first.isNotEmpty() && den.first.isNotEmpty()) {
                if (textBuf.isNotEmpty()) {
                    segments.add(DisplayMathSegment.Text(textBuf.toString()))
                    textBuf.clear()
                }
                segments.add(DisplayMathSegment.Binom(num.first.trim(), den.first.trim()))
                i = den.second
                continue
            }
        }

        // 4) Square roots: \sqrt
        if (latex.startsWith("\\sqrt", i)) {
            var k = i + 5
            val rootOrder = LatexMathFormatter.readBracketGroup(latex, k)
            if (rootOrder != null) {
                k = rootOrder.second
            }
            val inner = LatexMathFormatter.readTokenOrBraced(latex, k)
            if (inner.first.isNotEmpty()) {
                if (textBuf.isNotEmpty()) {
                    segments.add(DisplayMathSegment.Text(textBuf.toString()))
                    textBuf.clear()
                }
                segments.add(DisplayMathSegment.Sqrt(rootOrder?.first?.trim(), inner.first.trim()))
                i = inner.second
                continue
            }
        }

        textBuf.append(latex[i])
        i++
    }
    if (textBuf.isNotEmpty()) segments.add(DisplayMathSegment.Text(textBuf.toString()))
    return segments
}

@Composable
fun LatexDisplayExpression(
    latex: String,
    fontSize: TextUnit,
    mathColor: Color,
    depth: Int = 0
) {
    // Strip environment wrappers and clean alignment characters
    val cleaned = remember(latex) {
        latex.replace(Regex("""\\(begin|end)\{[a-zA-Z*]+\}"""), "")
            .replace("&=", "=")
            .replace("&", " ")
            .trim()
    }

    // Split multi-line equations (\\ or \n)
    val lines = remember(cleaned) {
        cleaned.split(Regex("""\\\\|\n""")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    if (lines.size > 1) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (line in lines) {
                LatexDisplayLine(
                    line = line,
                    fontSize = fontSize,
                    mathColor = mathColor,
                    depth = depth
                )
            }
        }
    } else if (lines.isNotEmpty()) {
        LatexDisplayLine(
            line = lines[0],
            fontSize = fontSize,
            mathColor = mathColor,
            depth = depth
        )
    }
}

@Composable
fun LatexDisplayLine(
    line: String,
    fontSize: TextUnit,
    mathColor: Color,
    depth: Int
) {
    val segments = remember(line) { splitDisplaySegments(line) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (seg in segments) {
            when (seg) {
                is DisplayMathSegment.Text -> {
                    val annotated = remember(seg.latex, fontSize, mathColor) {
                        buildAnnotatedString {
                            LatexMathFormatter.appendLatexMath(this, seg.latex, fontSize, mathColor)
                        }
                    }
                    Text(
                        text = annotated,
                        fontSize = fontSize,
                        color = mathColor
                    )
                }
                is DisplayMathSegment.Fraction -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(IntrinsicSize.Max)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            LatexDisplayExpression(
                                latex = seg.num,
                                fontSize = fontSize * 0.90f,
                                mathColor = mathColor,
                                depth = depth + 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.3.dp)
                                .background(mathColor.copy(alpha = 0.90f))
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            LatexDisplayExpression(
                                latex = seg.den,
                                fontSize = fontSize * 0.90f,
                                mathColor = mathColor,
                                depth = depth + 1
                            )
                        }
                    }
                }
                is DisplayMathSegment.Sqrt -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (!seg.order.isNullOrBlank()) {
                            Text(
                                text = seg.order,
                                fontSize = fontSize * 0.65f,
                                color = mathColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = "√",
                            fontSize = fontSize * 1.18f,
                            fontWeight = FontWeight.Light,
                            color = mathColor
                        )
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .padding(top = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.2.dp)
                                    .background(mathColor.copy(alpha = 0.90f))
                            )
                            Box(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                LatexDisplayExpression(
                                    latex = seg.inner,
                                    fontSize = fontSize * 0.92f,
                                    mathColor = mathColor,
                                    depth = depth + 1
                                )
                            }
                        }
                    }
                }
                is DisplayMathSegment.Binom -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text("(", fontSize = fontSize * 1.35f, fontWeight = FontWeight.Light, color = mathColor)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 2.dp).width(IntrinsicSize.Max)
                        ) {
                            LatexDisplayExpression(seg.n, fontSize * 0.88f, mathColor, depth + 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            LatexDisplayExpression(seg.k, fontSize * 0.88f, mathColor, depth + 1)
                        }
                        Text(")", fontSize = fontSize * 1.35f, fontWeight = FontWeight.Light, color = mathColor)
                    }
                }
                is DisplayMathSegment.Matrix -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val bracketScale = 1.15f + 0.32f * seg.rows.size
                        if (seg.bracket == '(') {
                            Text("(", fontSize = fontSize * bracketScale, fontWeight = FontWeight.Light, color = mathColor)
                        } else if (seg.bracket == '[') {
                            Text("[", fontSize = fontSize * bracketScale, fontWeight = FontWeight.Light, color = mathColor)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            for (row in seg.rows) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (cell in row) {
                                        Box(contentAlignment = Alignment.Center) {
                                            LatexDisplayExpression(cell, fontSize * 0.90f, mathColor, depth + 1)
                                        }
                                    }
                                }
                            }
                        }
                        if (seg.bracket == '(') {
                            Text(")", fontSize = fontSize * bracketScale, fontWeight = FontWeight.Light, color = mathColor)
                        } else if (seg.bracket == '[') {
                            Text("]", fontSize = fontSize * bracketScale, fontWeight = FontWeight.Light, color = mathColor)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders line segments (alternating text and math) with 2D stacked math where needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RenderLineSegments(
    segments: List<LineSegment>,
    baseFontSize: TextUnit,
    textColor: Color,
    accentColor: Color,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    // Case 1: Pure single math segment (or standalone formula)
    if (segments.size == 1 && segments[0] is LineSegment.Math) {
        val math = (segments[0] as LineSegment.Math).latex
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = when (textAlign) {
                TextAlign.Center -> Alignment.Center
                TextAlign.End -> Alignment.CenterEnd
                else -> Alignment.CenterStart
            }
        ) {
            LatexDisplayExpression(
                latex = math,
                fontSize = baseFontSize * 1.05f,
                mathColor = textColor
            )
        }
        return
    }

    // Case 2: Check if any segment has 2D math (fractions, roots, binom, matrix)
    val has2D = segments.any { it is LineSegment.Math && has2DMath(it.latex) }

    if (has2D) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (seg in segments) {
                when (seg) {
                    is LineSegment.Text -> {
                        val formatted = remember(seg.content, baseFontSize, textColor) {
                            LatexMathFormatter.formatInlineMarkdownAndLatex(
                                seg.content, baseFontSize, textColor, textColor
                            )
                        }
                        Text(
                            text = formatted,
                            fontSize = baseFontSize,
                            color = textColor,
                            lineHeight = (baseFontSize.value * 1.45f).sp
                        )
                    }
                    is LineSegment.Math -> {
                        LatexDisplayLine(
                            line = seg.latex,
                            fontSize = baseFontSize,
                            mathColor = textColor,
                            depth = 0
                        )
                    }
                }
            }
        }
    } else {
        // Plain text with inline math: single Compose Text for fast, smooth text wrapping
        val fullRaw = segments.joinToString("") { seg ->
            when (seg) {
                is LineSegment.Text -> seg.content
                is LineSegment.Math -> "$${seg.latex}$"
            }
        }
        val formatted = remember(fullRaw, baseFontSize, textColor) {
            LatexMathFormatter.formatInlineMarkdownAndLatex(
                fullRaw, baseFontSize, textColor, textColor
            )
        }
        Text(
            text = formatted,
            fontSize = baseFontSize,
            lineHeight = (baseFontSize.value * 1.48f).sp,
            color = textColor,
            textAlign = textAlign,
            modifier = modifier.fillMaxWidth()
        )
    }
}

/**
 * Renders formatted Markdown + LaTeX math cleanly and consistently in Jetpack Compose.
 */
@Composable
fun MarkdownLatexContent(
    markdown: String,
    baseFontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = (baseFontSize.value * 1.48f).sp,
    textColor: Color = LernspaceTextPrimary,
    accentColor: Color = LernspaceDark,
    isDarkSurface: Boolean = false,
    defaultBold: Boolean = false,
    defaultItalic: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdownLatexBlocks(markdown) }
    val density = LocalDensity.current
    val emDp = with(density) { baseFontSize.toDp() }
    val blockGap = (emDp * 0.38f).coerceIn(2.dp, 12.dp)
    val mathColor = textColor
    val codeBg = LernspaceSurfaceLow

    val baseStyle = TextStyle(
        color = textColor,
        fontSize = baseFontSize,
        lineHeight = lineHeight,
        fontWeight = if (defaultBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (defaultItalic) FontStyle.Italic else FontStyle.Normal,
        textAlign = textAlign
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(blockGap)
    ) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> {
                    val scale = when (block.level) {
                        1 -> 1.24f
                        2 -> 1.14f
                        else -> 1.06f
                    }
                    val segments = remember(block.text) { parseLineSegments(block.text) }
                    RenderLineSegments(
                        segments = segments,
                        baseFontSize = baseFontSize * scale,
                        textColor = accentColor,
                        accentColor = accentColor,
                        textAlign = textAlign,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                is MdBlock.Paragraph -> {
                    val segments = remember(block.text) { parseLineSegments(block.text) }
                    RenderLineSegments(
                        segments = segments,
                        baseFontSize = baseFontSize,
                        textColor = textColor,
                        accentColor = accentColor,
                        textAlign = textAlign
                    )
                }
                is MdBlock.Bullet -> {
                    val segments = remember(block.text) { parseLineSegments(block.text) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•", style = baseStyle.copy(fontWeight = FontWeight.Bold, color = accentColor))
                        Box(modifier = Modifier.weight(1f)) {
                            RenderLineSegments(
                                segments = segments,
                                baseFontSize = baseFontSize,
                                textColor = textColor,
                                accentColor = accentColor,
                                textAlign = textAlign
                            )
                        }
                    }
                }
                is MdBlock.Numbered -> {
                    val segments = remember(block.text) { parseLineSegments(block.text) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = block.number, style = baseStyle.copy(fontWeight = FontWeight.Bold, color = accentColor))
                        Box(modifier = Modifier.weight(1f)) {
                            RenderLineSegments(
                                segments = segments,
                                baseFontSize = baseFontSize,
                                textColor = textColor,
                                accentColor = accentColor,
                                textAlign = textAlign
                            )
                        }
                    }
                }
                is MdBlock.Quote -> {
                    val segments = remember(block.text) { parseLineSegments(block.text) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(codeBg.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .heightIn(min = 18.dp)
                                .background(accentColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            RenderLineSegments(
                                segments = segments,
                                baseFontSize = baseFontSize,
                                textColor = textColor,
                                accentColor = accentColor,
                                textAlign = textAlign
                            )
                        }
                    }
                }
                is MdBlock.DisplayMath -> {
                    // Clean, centered display formula without heavy bordered card boxes
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        LatexDisplayExpression(
                            latex = block.latex,
                            fontSize = baseFontSize * 1.15f,
                            mathColor = mathColor
                        )
                    }
                }
                is MdBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((emDp * 0.5f).coerceIn(4.dp, 12.dp)))
                            .background(codeBg)
                            .border(BorderStroke(1.dp, LernspaceBorder), RoundedCornerShape((emDp * 0.5f).coerceIn(4.dp, 12.dp)))
                            .horizontalScroll(rememberScrollState())
                            .padding((emDp * 0.65f).coerceIn(4.dp, 14.dp))
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = baseFontSize * 0.90f,
                            color = textColor
                        )
                    }
                }
                is MdBlock.Divider -> {
                    HorizontalDivider(
                        color = LernspaceBorder,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
