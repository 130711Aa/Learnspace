package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.background
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

/**
 * Pure-Kotlin Markdown + LaTeX math formatter and Jetpack Compose renderer.
 * Supports Markdown headings, bold/italic, inline code, bullet & numbered lists,
 * inline LaTeX ($...$, \(...\)), and display LaTeX ($$...$$, \[...\]) with
 * stacked \frac{a}{b} fractions, superscripts, subscripts, roots, and physics/math symbols.
 */
object LatexMathFormatter {

    private val SYMBOL_MAP = mapOf(
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ",
        "pi" to "π", "rho" to "ρ", "sigma" to "σ", "tau" to "τ",
        "upsilon" to "υ", "phi" to "φ", "varphi" to "ϕ", "chi" to "χ",
        "psi" to "ψ", "omega" to "ω",
        "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ",
        "Xi" to "Ξ", "Pi" to "Π", "Sigma" to "Σ", "Phi" to "Φ",
        "Psi" to "Ψ", "Omega" to "Ω",
        "times" to " × ", "cdot" to " · ", "div" to " ÷ ", "pm" to " ± ", "mp" to " ∓ ",
        "approx" to " ≈ ", "neq" to " ≠ ", "ne" to " ≠ ", "leq" to " ≤ ", "le" to " ≤ ",
        "geq" to " ≥ ", "ge" to " ≥ ", "equiv" to " ≡ ", "propto" to " ∝ ", "sim" to " ∼ ",
        "to" to " → ", "rightarrow" to " → ", "leftarrow" to " ← ",
        "Rightarrow" to " ⇒ ", "Leftarrow" to " ⇐ ", "Leftrightarrow" to " ⇔ ",
        "implies" to " ⇒ ", "iff" to " ⇔ ",
        "infty" to "∞", "partial" to "∂", "nabla" to "∇",
        "int" to "∫", "iint" to "∬", "oint" to "∮", "sum" to "∑", "prod" to "∏",
        "circ" to "°", "degree" to "°", "angle" to "∠", "perp" to "⊥", "parallel" to "∥",
        "therefore" to "∴", "because" to "∵", "in" to " ∈ ", "notin" to " ∉ ",
        "subset" to " ⊂ ", "subseteq" to " ⊆ ", "cup" to " ∪ ", "cap" to " ∩ ",
        "forall" to "∀", "exists" to "∃", "ldots" to "…", "cdots" to "⋯",
        "quad" to "  ", "qquad" to "    ", "," to " ", ";" to " ", " " to " ",
        "sin" to "sin", "cos" to "cos", "tan" to "tan", "cot" to "cot",
        "sec" to "sec", "csc" to "csc", "log" to "log", "ln" to "ln",
        "lim" to "lim", "max" to "max", "min" to "min", "exp" to "exp",
        "%" to "%", "$" to "$", "{" to "{", "}" to "}"
    )

    private val UNICODE_SUP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ'
    )

    private val UNICODE_SUB = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'i' to 'ᵢ', 'o' to 'ₒ', 'x' to 'ₓ'
    )

    fun readBracedGroup(src: String, startIdx: Int): Pair<String, Int>? {
        if (startIdx >= src.length || src[startIdx] != '{') return null
        var depth = 0
        var i = startIdx
        while (i < src.length) {
            when (src[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return src.substring(startIdx + 1, i) to (i + 1)
                    }
                }
            }
            i++
        }
        return src.substring(startIdx + 1) to src.length
    }

    private fun readBracketGroup(src: String, startIdx: Int): Pair<String, Int>? {
        if (startIdx >= src.length || src[startIdx] != '[') return null
        var depth = 0
        var i = startIdx
        while (i < src.length) {
            when (src[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return src.substring(startIdx + 1, i) to (i + 1)
                }
            }
            i++
        }
        return null
    }

    private fun readTokenOrBraced(src: String, startIdx: Int): Pair<String, Int> {
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

    private fun appendLatexInner(
        builder: AnnotatedString.Builder,
        expr: String,
        baseFontSize: TextUnit,
        scaleFactor: Float
    ) {
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            when (ch) {
                '\\' -> {
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
                        "left", "right", "displaystyle", "limits" -> {
                            i = j
                        }
                        "text", "mathrm", "textbf", "mathbf", "mathit", "operatorname" -> {
                            val grp = if (j < expr.length && expr[j] == '{') readBracedGroup(expr, j) else null
                            if (grp != null) {
                                if (cmd == "textbf" || cmd == "mathbf") {
                                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                    builder.append(grp.first)
                                    builder.pop()
                                } else {
                                    builder.append(grp.first)
                                }
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "frac", "dfrac", "tfrac" -> {
                            var k = j
                            while (k < expr.length && expr[k] == ' ') k++
                            val num = readBracedGroup(expr, k)
                            val den = num?.let {
                                var d = it.second
                                while (d < expr.length && expr[d] == ' ') d++
                                readBracedGroup(expr, d)
                            }
                            if (num != null && den != null) {
                                val wrapNum = num.first.length > 1 && !num.first.all { it.isLetterOrDigit() }
                                val wrapDen = den.first.length > 1 && !den.first.all { it.isLetterOrDigit() }
                                if (wrapNum) builder.append('(')
                                appendLatexInner(builder, num.first, baseFontSize, scaleFactor)
                                if (wrapNum) builder.append(')')
                                builder.append('/')
                                if (wrapDen) builder.append('(')
                                appendLatexInner(builder, den.first, baseFontSize, scaleFactor)
                                if (wrapDen) builder.append(')')
                                i = den.second
                            } else {
                                i = j
                            }
                        }
                        "sqrt" -> {
                            var k = j
                            while (k < expr.length && expr[k] == ' ') k++
                            val rootOrder = readBracketGroup(expr, k)
                            if (rootOrder != null) {
                                builder.pushStyle(
                                    SpanStyle(
                                        fontSize = baseFontSize * (scaleFactor * 0.68f),
                                        baselineShift = BaselineShift.Superscript
                                    )
                                )
                                builder.append(rootOrder.first)
                                builder.pop()
                                k = rootOrder.second
                            }
                            while (k < expr.length && expr[k] == ' ') k++
                            val inner = readBracedGroup(expr, k)
                            if (inner != null) {
                                builder.append("√(")
                                appendLatexInner(builder, inner.first, baseFontSize, scaleFactor)
                                builder.append(')')
                                i = inner.second
                            } else {
                                builder.append('√')
                                i = k
                            }
                        }
                        "vec" -> {
                            val grp = if (j < expr.length && expr[j] == '{') readBracedGroup(expr, j) else null
                            if (grp != null) {
                                appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                                builder.append('\u20D7')
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "bar", "overline" -> {
                            val grp = if (j < expr.length && expr[j] == '{') readBracedGroup(expr, j) else null
                            if (grp != null) {
                                appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                                builder.append('\u0304')
                                i = grp.second
                            } else {
                                i = j
                            }
                        }
                        "hat" -> {
                            val grp = if (j < expr.length && expr[j] == '{') readBracedGroup(expr, j) else null
                            if (grp != null) {
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
                            } else if (cmd.isNotEmpty()) {
                                builder.append(cmd)
                            }
                            i = j
                        }
                    }
                }
                '^' -> {
                    val (expText, nextIdx) = readTokenOrBraced(expr, i + 1)
                    if (expText == "\\circ" || expText == "circ") {
                        builder.append('°')
                    } else if (expText.isNotEmpty()) {
                        builder.pushStyle(
                            SpanStyle(
                                fontSize = baseFontSize * (scaleFactor * 0.72f),
                                baselineShift = BaselineShift.Superscript
                            )
                        )
                        appendLatexInner(builder, expText, baseFontSize, scaleFactor * 0.78f)
                        builder.pop()
                    }
                    i = nextIdx
                }
                '_' -> {
                    val (subText, nextIdx) = readTokenOrBraced(expr, i + 1)
                    if (subText.isNotEmpty()) {
                        builder.pushStyle(
                            SpanStyle(
                                fontSize = baseFontSize * (scaleFactor * 0.72f),
                                baselineShift = BaselineShift.Subscript
                            )
                        )
                        appendLatexInner(builder, subText, baseFontSize, scaleFactor * 0.78f)
                        builder.pop()
                    }
                    i = nextIdx
                }
                '{' -> {
                    val grp = readBracedGroup(expr, i)
                    if (grp != null) {
                        appendLatexInner(builder, grp.first, baseFontSize, scaleFactor)
                        i = grp.second
                    } else {
                        i++
                    }
                }
                '}' -> i++
                '*' -> {
                    builder.append(" · ")
                    i++
                }
                else -> {
                    builder.append(ch)
                    i++
                }
            }
        }
    }

    /**
     * Parses inline Markdown (**bold**, *italic*, `code`) and inline LaTeX ($...$, \(...\))
     * into a styled AnnotatedString.
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
            // 1) Display/inline \( ... \)
            if (raw.startsWith("\\(", i)) {
                val close = raw.indexOf("\\)", i + 2)
                if (close > i + 2) {
                    appendLatexMath(this, raw.substring(i + 2, close), baseFontSize, mathColor)
                    i = close + 2
                    continue
                }
            }
            // 2) Inline $ ... $ (single $)
            if (raw[i] == '$' && (i + 1 >= raw.length || raw[i + 1] != '$')) {
                val close = raw.indexOf('$', i + 1)
                if (close > i + 1 && !raw.substring(i + 1, close).contains('\n')) {
                    appendLatexMath(this, raw.substring(i + 1, close), baseFontSize, mathColor)
                    i = close + 1
                    continue
                }
            }
            // 3) Inline `code`
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
            // 4) Bold **...**
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
            // 5) Italic *...* (only when not spacing asterisk)
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

        // Display math block: $$...$$ or \[...\]
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
                            if (sb.isNotEmpty()) sb.append(' ')
                            sb.append(before)
                        }
                        i++
                        break
                    } else {
                        if (sb.isNotEmpty() && t.isNotEmpty()) sb.append(' ')
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

        // Normal paragraph line (merge consecutive plain lines into one paragraph unless next is special block)
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

private sealed class DisplayMathSegment {
    data class Text(val latex: String) : DisplayMathSegment()
    data class Fraction(val num: String, val den: String) : DisplayMathSegment()
}

private fun splitTopLevelFractions(latex: String): List<DisplayMathSegment> {
    val segments = mutableListOf<DisplayMathSegment>()
    val textBuf = StringBuilder()
    var i = 0
    while (i < latex.length) {
        if (latex.startsWith("\\frac", i) || latex.startsWith("\\dfrac", i) || latex.startsWith("\\tfrac", i)) {
            val cmdLen = when {
                latex.startsWith("\\dfrac", i) || latex.startsWith("\\tfrac", i) -> 6
                else -> 5
            }
            var k = i + cmdLen
            while (k < latex.length && latex[k] == ' ') k++
            val num = LatexMathFormatter.readBracedGroup(latex, k)
            val den = num?.let {
                var d = it.second
                while (d < latex.length && latex[d] == ' ') d++
                LatexMathFormatter.readBracedGroup(latex, d)
            }
            if (num != null && den != null) {
                if (textBuf.isNotEmpty()) {
                    segments.add(DisplayMathSegment.Text(textBuf.toString()))
                    textBuf.clear()
                }
                segments.add(DisplayMathSegment.Fraction(num.first.trim(), den.first.trim()))
                i = den.second
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
private fun LatexDisplayExpression(
    latex: String,
    fontSize: TextUnit,
    mathColor: Color,
    depth: Int = 0
) {
    val segments = remember(latex) { splitTopLevelFractions(latex) }
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
                    if (depth >= 2) {
                        val fallback = remember(seg, fontSize, mathColor) {
                            buildAnnotatedString {
                                LatexMathFormatter.appendLatexMath(
                                    this, "\\frac{${seg.num}}{${seg.den}}", fontSize, mathColor
                                )
                            }
                        }
                        Text(text = fallback, fontSize = fontSize, color = mathColor)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(IntrinsicSize.Max)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                LatexDisplayExpression(
                                    latex = seg.num,
                                    fontSize = fontSize * 0.88f,
                                    mathColor = mathColor,
                                    depth = depth + 1
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(mathColor.copy(alpha = 0.85f))
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                LatexDisplayExpression(
                                    latex = seg.den,
                                    fontSize = fontSize * 0.88f,
                                    mathColor = mathColor,
                                    depth = depth + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders formatted Markdown + LaTeX math (including stacked fractions) cleanly in Compose.
 */
@Composable
fun MarkdownLatexContent(
    markdown: String,
    baseFontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = (baseFontSize.value * 1.48f).sp,
    textColor: Color = Color(0xFF0F172A),
    accentColor: Color = Color(0xFF2563EB),
    isDarkSurface: Boolean = false,
    defaultBold: Boolean = false,
    defaultItalic: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdownLatexBlocks(markdown) }
    val density = LocalDensity.current
    val emDp = with(density) { baseFontSize.toDp() }
    val blockGap = (emDp * 0.40f).coerceIn(2.dp, 14.dp)
    val mathColor = if (isDarkSurface) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val mathCardBg = if (isDarkSurface) Color(0xFF0F172A).copy(alpha = 0.65f) else Color(0xFFEFF6FF)
    val codeBg = if (isDarkSurface) Color(0xFF334155) else Color(0xFFE2E8F0).copy(alpha = 0.7f)

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
                    val formatted = remember(block.text, baseFontSize, textColor, isDarkSurface) {
                        LatexMathFormatter.formatInlineMarkdownAndLatex(
                            block.text, baseFontSize * scale, accentColor, mathColor, codeBg
                        )
                    }
                    Text(
                        text = formatted,
                        style = baseStyle.copy(
                            fontSize = baseFontSize * scale,
                            lineHeight = (baseFontSize.value * scale * 1.35f).sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
                is MdBlock.Paragraph -> {
                    val formatted = remember(block.text, baseFontSize, textColor, isDarkSurface) {
                        LatexMathFormatter.formatInlineMarkdownAndLatex(
                            block.text, baseFontSize, textColor, mathColor, codeBg
                        )
                    }
                    Text(
                        text = formatted,
                        style = baseStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is MdBlock.Bullet -> {
                    val formatted = remember(block.text, baseFontSize, textColor, isDarkSurface) {
                        LatexMathFormatter.formatInlineMarkdownAndLatex(
                            block.text, baseFontSize, textColor, mathColor, codeBg
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•", style = baseStyle.copy(fontWeight = FontWeight.Bold, color = accentColor))
                        Text(text = formatted, style = baseStyle, modifier = Modifier.weight(1f))
                    }
                }
                is MdBlock.Numbered -> {
                    val formatted = remember(block.text, baseFontSize, textColor, isDarkSurface) {
                        LatexMathFormatter.formatInlineMarkdownAndLatex(
                            block.text, baseFontSize, textColor, mathColor, codeBg
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = block.number, style = baseStyle.copy(fontWeight = FontWeight.Bold, color = accentColor))
                        Text(text = formatted, style = baseStyle, modifier = Modifier.weight(1f))
                    }
                }
                is MdBlock.Quote -> {
                    val formatted = remember(block.text, baseFontSize, textColor, isDarkSurface) {
                        LatexMathFormatter.formatInlineMarkdownAndLatex(
                            block.text, baseFontSize, textColor, mathColor, codeBg
                        )
                    }
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
                        Text(text = formatted, style = baseStyle.copy(fontStyle = FontStyle.Italic))
                    }
                }
                is MdBlock.DisplayMath -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((emDp * 0.6f).coerceIn(4.dp, 14.dp)))
                            .background(mathCardBg)
                            .horizontalScroll(rememberScrollState())
                            .padding(
                                horizontal = (emDp * 0.75f).coerceIn(4.dp, 18.dp),
                                vertical = (emDp * 0.45f).coerceIn(3.dp, 12.dp)
                            )
                    ) {
                        LatexDisplayExpression(
                            latex = block.latex,
                            fontSize = baseFontSize * 1.12f,
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
                        color = textColor.copy(alpha = 0.16f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
