package com.lernspace.app.ui.workspace.document

import com.lernspace.app.ui.workspace.components.LatexMathFormatter
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/**
 * Universal LaTeX Math Engine for Learnspace.
 * Converts LaTeX equations into:
 * 1. ISO/IEC 29500 Office Math Markup Language (OMML) for Microsoft Word, WPS Office, Google Docs, and LibreOffice Writer.
 * 2. W3C MathML Core for Android Chromium WebView high-fidelity native math rendering.
 * 3. Clean Unicode text representation for PlainText export and Note snippets.
 */
object LatexMathCodec {

    // ── GREEK & SYMBOL MAPPINGS ───────────────────────────────────────────────
    val SYMBOLS = mapOf(
        "pm" to "±", "mp" to "∓", "times" to "×", "div" to "÷", "cdot" to "·",
        "ast" to "*", "star" to "★", "circ" to "°", "bullet" to "•",
        "approx" to "≈", "neq" to "≠", "ne" to "≠", "le" to "≤", "leq" to "≤",
        "ge" to "≥", "geq" to "≥", "equiv" to "≡", "sim" to "∼", "cong" to "≅",
        "infty" to "∞", "partial" to "∂", "nabla" to "∇", "hbar" to "ℏ",
        "forall" to "∀", "exists" to "∃", "nexists" to "∄", "in" to "∈", "notin" to "∉",
        "subset" to "⊂", "subseteq" to "⊆", "supset" to "⊃", "supseteq" to "⊇",
        "cup" to "∪", "cap" to "∩", "setminus" to "\\", "emptyset" to "∅",
        "to" to "→", "rightarrow" to "→", "leftarrow" to "←", "leftrightarrow" to "↔",
        "Rightarrow" to "⇒", "Leftarrow" to "⇐", "Leftrightarrow" to "⇔",
        "implies" to "⇒", "iff" to "⇔",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ",
        "pi" to "π", "varpi" to "ϖ", "rho" to "ρ", "varrho" to "ϱ",
        "sigma" to "σ", "varsigma" to "ς", "tau" to "τ", "upsilon" to "υ",
        "phi" to "φ", "varphi" to "ϕ", "chi" to "χ", "psi" to "ψ", "omega" to "ω",
        "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ",
        "Xi" to "Ξ", "Pi" to "Π", "Sigma" to "Σ", "Upsilon" to "Υ",
        "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω"
    )

    val KNOWN_MATH_FUNCTIONS = setOf(
        "sin", "cos", "tan", "ln", "log", "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "sec", "csc", "cot", "exp"
    )

    fun cleanLatex(raw: String): String {
        return raw.trim()
            .removePrefix("$$").removeSuffix("$$")
            .removePrefix("\\[").removeSuffix("\\]")
            .removePrefix("\\(").removeSuffix("\\)")
            .removePrefix("$").removeSuffix("$")
            .trim()
    }

    // ── AST TREE REPRESENTATION ───────────────────────────────────────────────
    sealed class MathNode {
        data class Text(val text: String) : MathNode()
        data class Fraction(val num: MathNode, val den: MathNode) : MathNode()
        data class Sup(val base: MathNode, val sup: MathNode) : MathNode()
        data class Sub(val base: MathNode, val sub: MathNode) : MathNode()
        data class SubSup(val base: MathNode, val sub: MathNode, val sup: MathNode) : MathNode()
        data class Radical(val deg: MathNode?, val rad: MathNode) : MathNode()
        data class Nary(val symbol: String, val sub: MathNode?, val sup: MathNode?, val body: MathNode) : MathNode()
        data class Delimited(val left: String, val right: String, val body: MathNode) : MathNode()
        data class Matrix(val rows: List<List<MathNode>>) : MathNode()
        data class Lim(val target: MathNode, val body: MathNode) : MathNode()
        data class Group(val children: List<MathNode>) : MathNode()
    }

    // ── PARSER ────────────────────────────────────────────────────────────────
    fun parse(latex: String): MathNode {
        val clean = cleanLatex(latex)
        val tokens = tokenize(clean)
        val (nodes, _) = parseSequence(tokens, 0)
        return if (nodes.size == 1) nodes[0] else MathNode.Group(nodes)
    }

    private data class Token(val type: TokenType, val value: String)
    private enum class TokenType {
        TEXT, COMMAND, LBRACE, RBRACE, LBRACKET, RBRACKET, CARET, UNDERSCORE, AMP, NEWLINE
    }

    private fun tokenize(src: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c.isWhitespace() -> i++
                c == '{' -> { tokens.add(Token(TokenType.LBRACE, "{")); i++ }
                c == '}' -> { tokens.add(Token(TokenType.RBRACE, "}")); i++ }
                c == '[' -> { tokens.add(Token(TokenType.LBRACKET, "[")); i++ }
                c == ']' -> { tokens.add(Token(TokenType.RBRACKET, "]")); i++ }
                c == '^' -> { tokens.add(Token(TokenType.CARET, "^")); i++ }
                c == '_' -> { tokens.add(Token(TokenType.UNDERSCORE, "_")); i++ }
                c == '&' -> { tokens.add(Token(TokenType.AMP, "&")); i++ }
                c == '\\' -> {
                    if (i + 1 < src.length && src[i + 1] == '\\') {
                        tokens.add(Token(TokenType.NEWLINE, "\\\\"))
                        i += 2
                    } else if (i + 1 < src.length && !src[i + 1].isLetter()) {
                        tokens.add(Token(TokenType.COMMAND, src.substring(i + 1, i + 2)))
                        i += 2
                    } else {
                        var j = i + 1
                        while (j < src.length && src[j].isLetter()) j++
                        tokens.add(Token(TokenType.COMMAND, src.substring(i + 1, j)))
                        i = j
                    }
                }
                c.isDigit() -> {
                    var j = i
                    while (j < src.length && (src[j].isDigit() || src[j] == '.' || src[j] == ',')) j++
                    tokens.add(Token(TokenType.TEXT, src.substring(i, j)))
                    i = j
                }
                c in setOf('+', '-', '=', '<', '>', '(', ')', '!', '*', '/', '|', ':', ';', '·', '±', '∓', '×', '÷') -> {
                    tokens.add(Token(TokenType.TEXT, c.toString()))
                    i++
                }
                else -> {
                    var j = i
                    while (j < src.length && !src[j].isWhitespace() && src[j] !in setOf('{', '}', '[', ']', '^', '_', '&', '\\', '+', '-', '=', '<', '>', '(', ')', '!', '*', '/', '|', ':', ';', '·', '±', '∓', '×', '÷') && !src[j].isDigit()) {
                        j++
                    }
                    if (j > i) {
                        tokens.add(Token(TokenType.TEXT, src.substring(i, j)))
                        i = j
                    } else {
                        tokens.add(Token(TokenType.TEXT, src.substring(i, i + 1)))
                        i++
                    }
                }
            }
        }
        return tokens
    }

    private fun parseSequence(tokens: List<Token>, startIdx: Int, stopTokens: Set<TokenType> = emptySet()): Pair<List<MathNode>, Int> {
        val list = mutableListOf<MathNode>()
        var idx = startIdx

        while (idx < tokens.size) {
            val tok = tokens[idx]
            if (tok.type in stopTokens) break

            val (node, nextIdx) = parseSingle(tokens, idx)
            if (node != null) {
                // Check if followed by ^ or _
                var combined = node
                var cur = nextIdx
                var pendingSub: MathNode? = null
                var pendingSup: MathNode? = null

                while (cur < tokens.size) {
                    if (tokens[cur].type == TokenType.CARET && pendingSup == null) {
                        val (supNode, afterSup) = parseArgument(tokens, cur + 1)
                        pendingSup = supNode
                        cur = afterSup
                    } else if (tokens[cur].type == TokenType.UNDERSCORE && pendingSub == null) {
                        val (subNode, afterSub) = parseArgument(tokens, cur + 1)
                        pendingSub = subNode
                        cur = afterSub
                    } else {
                        break
                    }
                }

                if (pendingSub != null && pendingSup != null) {
                    combined = MathNode.SubSup(combined, pendingSub, pendingSup)
                    idx = cur
                } else if (pendingSup != null) {
                    combined = MathNode.Sup(combined, pendingSup)
                    idx = cur
                } else if (pendingSub != null) {
                    combined = MathNode.Sub(combined, pendingSub)
                    idx = cur
                } else {
                    idx = nextIdx
                }

                list.add(combined)
            } else {
                idx = nextIdx
            }
        }
        return list to idx
    }

    private fun parseSingle(tokens: List<Token>, idx: Int): Pair<MathNode?, Int> {
        if (idx >= tokens.size) return null to idx
        val tok = tokens[idx]

        when (tok.type) {
            TokenType.LBRACE -> {
                val (inner, after) = parseSequence(tokens, idx + 1, setOf(TokenType.RBRACE))
                val next = if (after < tokens.size && tokens[after].type == TokenType.RBRACE) after + 1 else after
                return (if (inner.size == 1) inner[0] else MathNode.Group(inner)) to next
            }
            TokenType.TEXT -> {
                return MathNode.Text(tok.value) to idx + 1
            }
            TokenType.COMMAND -> {
                val cmd = tok.value
                when (cmd) {
                    "frac", "dfrac", "tfrac" -> {
                        val (num, afterNum) = parseArgument(tokens, idx + 1)
                        val (den, afterDen) = parseArgument(tokens, afterNum)
                        return MathNode.Fraction(num, den) to afterDen
                    }
                    "sqrt" -> {
                        var next = idx + 1
                        var deg: MathNode? = null
                        if (next < tokens.size && tokens[next].type == TokenType.LBRACKET) {
                            val (bracketInner, afterBracket) = parseSequence(tokens, next + 1, setOf(TokenType.RBRACKET))
                            deg = if (bracketInner.size == 1) bracketInner[0] else MathNode.Group(bracketInner)
                            next = if (afterBracket < tokens.size && tokens[afterBracket].type == TokenType.RBRACKET) afterBracket + 1 else afterBracket
                        }
                        val (rad, afterRad) = parseArgument(tokens, next)
                        return MathNode.Radical(deg, rad) to afterRad
                    }
                    "int", "iint", "iiint", "oint" -> {
                        val sym = when (cmd) {
                            "iint" -> "∬"
                            "iiint" -> "∭"
                            "oint" -> "∮"
                            else -> "∫"
                        }
                        var cur = idx + 1
                        var sub: MathNode? = null
                        var sup: MathNode? = null
                        while (cur < tokens.size) {
                            if (tokens[cur].type == TokenType.UNDERSCORE && sub == null) {
                                val (s, a) = parseArgument(tokens, cur + 1)
                                sub = s; cur = a
                            } else if (tokens[cur].type == TokenType.CARET && sup == null) {
                                val (s, a) = parseArgument(tokens, cur + 1)
                                sup = s; cur = a
                            } else break
                        }
                        val (body, afterBody) = parseSequence(tokens, cur)
                        val bodyNode = if (body.size == 1) body[0] else MathNode.Group(body)
                        return MathNode.Nary(sym, sub, sup, bodyNode) to afterBody
                    }
                    "sum", "prod" -> {
                        val sym = if (cmd == "sum") "∑" else "∏"
                        var cur = idx + 1
                        var sub: MathNode? = null
                        var sup: MathNode? = null
                        while (cur < tokens.size) {
                            if (tokens[cur].type == TokenType.UNDERSCORE && sub == null) {
                                val (s, a) = parseArgument(tokens, cur + 1)
                                sub = s; cur = a
                            } else if (tokens[cur].type == TokenType.CARET && sup == null) {
                                val (s, a) = parseArgument(tokens, cur + 1)
                                sup = s; cur = a
                            } else break
                        }
                        val (body, afterBody) = parseSequence(tokens, cur)
                        val bodyNode = if (body.size == 1) body[0] else MathNode.Group(body)
                        return MathNode.Nary(sym, sub, sup, bodyNode) to afterBody
                    }
                    "lim" -> {
                        var cur = idx + 1
                        var target: MathNode = MathNode.Text("x→0")
                        if (cur < tokens.size && tokens[cur].type == TokenType.UNDERSCORE) {
                            val (s, a) = parseArgument(tokens, cur + 1)
                            target = s; cur = a
                        }
                        val (body, afterBody) = parseSequence(tokens, cur)
                        val bodyNode = if (body.size == 1) body[0] else MathNode.Group(body)
                        return MathNode.Lim(target, bodyNode) to afterBody
                    }
                    "left" -> {
                        val leftDelim = if (idx + 1 < tokens.size) tokens[idx + 1].value else "("
                        val (inner, afterInner) = parseSequenceUntilRight(tokens, idx + 2)
                        var rightDelim = ")"
                        var next = afterInner
                        if (next < tokens.size && tokens[next].type == TokenType.COMMAND && tokens[next].value == "right") {
                            rightDelim = if (next + 1 < tokens.size) tokens[next + 1].value else ")"
                            next += 2
                        }
                        val bodyNode = if (inner.size == 1) inner[0] else MathNode.Group(inner)
                        return MathNode.Delimited(leftDelim, rightDelim, bodyNode) to next
                    }
                    "begin" -> {
                        // Matrix or cases
                        val (envNameNode, afterEnv) = parseArgument(tokens, idx + 1)
                        val envName = (envNameNode as? MathNode.Text)?.text.orEmpty().trim('{', '}')
                        val (matrixNode, afterMatrix) = parseMatrix(tokens, afterEnv, envName)
                        return matrixNode to afterMatrix
                    }
                    ",", "thinspace" -> {
                        return MathNode.Text("\u2009") to idx + 1
                    }
                    ";", "thickspace" -> {
                        return MathNode.Text("\u2005") to idx + 1
                    }
                    "!", "negthinspace" -> {
                        return MathNode.Text("") to idx + 1
                    }
                    "quad" -> {
                        return MathNode.Text("\u2003") to idx + 1
                    }
                    "qquad" -> {
                        return MathNode.Text("\u2003\u2003") to idx + 1
                    }
                    else -> {
                        val sym = SYMBOLS[cmd] ?: cmd
                        return MathNode.Text(sym) to idx + 1
                    }
                }
            }
            else -> return null to idx + 1
        }
    }

    private fun parseArgument(tokens: List<Token>, idx: Int): Pair<MathNode, Int> {
        if (idx >= tokens.size) return MathNode.Text("") to idx
        if (tokens[idx].type == TokenType.LBRACE) {
            val (inner, after) = parseSequence(tokens, idx + 1, setOf(TokenType.RBRACE))
            val next = if (after < tokens.size && tokens[after].type == TokenType.RBRACE) after + 1 else after
            return (if (inner.size == 1) inner[0] else MathNode.Group(inner)) to next
        }
        val (node, next) = parseSingle(tokens, idx)
        return (node ?: MathNode.Text("")) to next
    }

    private fun parseSequenceUntilRight(tokens: List<Token>, startIdx: Int): Pair<List<MathNode>, Int> {
        val list = mutableListOf<MathNode>()
        var idx = startIdx
        while (idx < tokens.size) {
            if (tokens[idx].type == TokenType.COMMAND && tokens[idx].value == "right") {
                break
            }
            val (node, nextIdx) = parseSingle(tokens, idx)
            if (node != null) list.add(node)
            idx = nextIdx
        }
        return list to idx
    }

    private fun parseMatrix(tokens: List<Token>, startIdx: Int, envName: String): Pair<MathNode, Int> {
        val rows = mutableListOf<MutableList<MathNode>>()
        var currentRow = mutableListOf<MathNode>()
        var currentCellTokens = mutableListOf<Token>()
        var idx = startIdx

        fun flushCell() {
            if (currentCellTokens.isNotEmpty()) {
                val (nodes, _) = parseSequence(currentCellTokens, 0)
                currentRow.add(if (nodes.size == 1) nodes[0] else MathNode.Group(nodes))
                currentCellTokens.clear()
            }
        }

        while (idx < tokens.size) {
            val t = tokens[idx]
            if (t.type == TokenType.COMMAND && t.value == "end") {
                flushCell()
                if (currentRow.isNotEmpty()) rows.add(currentRow)
                idx += 2 // skip end and {envName}
                break
            }
            if (t.type == TokenType.AMP) {
                flushCell()
                idx++
                continue
            }
            if (t.type == TokenType.NEWLINE) {
                flushCell()
                rows.add(currentRow)
                currentRow = mutableListOf()
                idx++
                continue
            }
            currentCellTokens.add(t)
            idx++
        }

        val matrixNode = MathNode.Matrix(rows)
        val (l, r) = when (envName) {
            "pmatrix" -> "(" to ")"
            "bmatrix" -> "[" to "]"
            "vmatrix" -> "|" to "|"
            else -> "" to ""
        }
        val finalNode = if (l.isNotEmpty()) MathNode.Delimited(l, r, matrixNode) else matrixNode
        return finalNode to idx
    }

    // ── OMML (OFFICE MATH MARKUP LANGUAGE) GENERATOR FOR WORD / WPS / DOCS ───
    fun toOmml(latex: String): String {
        val root = parse(latex)
        val body = renderOmmlNode(root)
        return """<m:oMath xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math">$body</m:oMath>"""
    }

    private fun renderOmmlNode(node: MathNode): String = when (node) {
        is MathNode.Text -> {
            when (node.text) {
                "\u2009", "\u2005", "\u2003", "\u2003\u2003" -> """<m:r><m:t xml:space="preserve"> </m:t></m:r>"""
                else -> {
                    val esc = escapeXml(node.text)
                    "<m:r><m:t>$esc</m:t></m:r>"
                }
            }
        }
        is MathNode.Group -> node.children.joinToString("") { renderOmmlNode(it) }
        is MathNode.Fraction -> {
            val n = renderOmmlNode(node.num)
            val d = renderOmmlNode(node.den)
            "<m:f><m:fPr/><m:num>$n</m:num><m:den>$d</m:den></m:f>"
        }
        is MathNode.Sup -> {
            val b = renderOmmlNode(node.base)
            val s = renderOmmlNode(node.sup)
            "<m:sSup><m:sSupPr/><m:e>$b</m:e><m:sup>$s</m:sup></m:sSup>"
        }
        is MathNode.Sub -> {
            val b = renderOmmlNode(node.base)
            val s = renderOmmlNode(node.sub)
            "<m:sSub><m:sSubPr/><m:e>$b</m:e><m:sub>$s</m:sub></m:sSub>"
        }
        is MathNode.SubSup -> {
            val b = renderOmmlNode(node.base)
            val sub = renderOmmlNode(node.sub)
            val sup = renderOmmlNode(node.sup)
            "<m:sSubSup><m:sSubSupPr/><m:e>$b</m:e><m:sub>$sub</m:sub><m:sup>$sup</m:sup></m:sSubSup>"
        }
        is MathNode.Radical -> {
            val r = renderOmmlNode(node.rad)
            if (node.deg != null) {
                val d = renderOmmlNode(node.deg)
                "<m:rad><m:radPr/><m:deg>$d</m:deg><m:e>$r</m:e></m:rad>"
            } else {
                "<m:rad><m:radPr><m:degHide m:val=\"1\"/></m:radPr><m:deg/><m:e>$r</m:e></m:rad>"
            }
        }
        is MathNode.Nary -> {
            val chr = escapeXml(node.symbol)
            val sub = node.sub?.let { renderOmmlNode(it) } ?: ""
            val sup = node.sup?.let { renderOmmlNode(it) } ?: ""
            val body = renderOmmlNode(node.body)
            val subXml = if (sub.isNotEmpty()) "<m:sub>$sub</m:sub>" else "<m:sub/>"
            val supXml = if (sup.isNotEmpty()) "<m:sup>$sup</m:sup>" else "<m:sup/>"
            val subHide = if (sub.isEmpty()) """<m:subHide m:val="1"/>""" else ""
            val supHide = if (sup.isEmpty()) """<m:supHide m:val="1"/>""" else ""
            "<m:nary><m:naryPr><m:chr m:val=\"$chr\"/><m:limLoc m:val=\"undOvr\"/><m:grow m:val=\"1\"/>$subHide$supHide</m:naryPr>$subXml$supXml<m:e>$body</m:e></m:nary>"
        }
        is MathNode.Lim -> {
            val tgt = renderOmmlNode(node.target)
            val body = renderOmmlNode(node.body)
            "<m:limLow><m:limLowPr/><m:e><m:r><m:rPr><m:scr m:val=\"roman\"/></m:rPr><m:t>lim</m:t></m:r></m:e><m:lim>$tgt</m:lim></m:limLow><m:r><m:t xml:space=\"preserve\"> </m:t></m:r>$body"
        }
        is MathNode.Delimited -> {
            val b = escapeXml(node.left)
            val e = escapeXml(node.right)
            val body = renderOmmlNode(node.body)
            "<m:d><m:dPr><m:begChr m:val=\"$b\"/><m:endChr m:val=\"$e\"/></m:dPr><m:e>$body</m:e></m:d>"
        }
        is MathNode.Matrix -> {
            val rowsXml = node.rows.joinToString("") { r ->
                val cellsXml = r.joinToString("") { c -> "<m:e>${renderOmmlNode(c)}</m:e>" }
                "<m:mr>$cellsXml</m:mr>"
            }
            "<m:m><m:mPr/>$rowsXml</m:m>"
        }
    }

    // ── MATHML GENERATOR FOR CHROMIUM ANDROID WEBVIEW ────────────────────────
    fun toMathML(latex: String): String {
        val clean = cleanLatex(latex)
        val root = parse(clean)
        val body = renderMathMLNode(root)
        val esc = clean.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
        return """<math xmlns="http://www.w3.org/1998/Math/MathML" display="inline" data-latex="$esc">$body</math>"""
    }

    private fun renderMathMLNode(node: MathNode): String = when (node) {
        is MathNode.Text -> renderTextToMathML(node.text)
        is MathNode.Group -> "<mrow>" + node.children.joinToString("") { renderMathMLNode(it) } + "</mrow>"
        is MathNode.Fraction -> {
            val n = renderMathMLNode(node.num)
            val d = renderMathMLNode(node.den)
            "<mfrac><mrow>$n</mrow><mrow>$d</mrow></mfrac>"
        }
        is MathNode.Sup -> {
            val b = renderMathMLNode(node.base)
            val s = renderMathMLNode(node.sup)
            "<msup><mrow>$b</mrow><mrow>$s</mrow></msup>"
        }
        is MathNode.Sub -> {
            val b = renderMathMLNode(node.base)
            val s = renderMathMLNode(node.sub)
            "<msub><mrow>$b</mrow><mrow>$s</mrow></msub>"
        }
        is MathNode.SubSup -> {
            val b = renderMathMLNode(node.base)
            val sub = renderMathMLNode(node.sub)
            val sup = renderMathMLNode(node.sup)
            "<msubsup><mrow>$b</mrow><mrow>$sub</mrow><mrow>$sup</mrow></msubsup>"
        }
        is MathNode.Radical -> {
            val r = renderMathMLNode(node.rad)
            if (node.deg != null) {
                val d = renderMathMLNode(node.deg)
                "<mroot><mrow>$r</mrow><mrow>$d</mrow></mroot>"
            } else {
                "<msqrt><mrow>$r</mrow></msqrt>"
            }
        }
        is MathNode.Nary -> {
            val sym = "<mo>${escapeXml(node.symbol)}</mo>"
            val sub = node.sub?.let { renderMathMLNode(it) } ?: ""
            val sup = node.sup?.let { renderMathMLNode(it) } ?: ""
            val body = renderMathMLNode(node.body)
            val op = when {
                sub.isNotEmpty() && sup.isNotEmpty() -> "<msubsup>$sym<mrow>$sub</mrow><mrow>$sup</mrow></msubsup>"
                sub.isNotEmpty() -> "<msub>$sym<mrow>$sub</mrow></msub>"
                sup.isNotEmpty() -> "<msup>$sym<mrow>$sup</mrow></msup>"
                else -> sym
            }
            "<mrow>$op<mrow>$body</mrow></mrow>"
        }
        is MathNode.Lim -> {
            val tgt = renderMathMLNode(node.target)
            val body = renderMathMLNode(node.body)
            "<mrow><munder><mi mathvariant=\"normal\">lim</mi><mrow>$tgt</mrow></munder><mspace width=\"0.2em\"/><mrow>$body</mrow></mrow>"
        }
        is MathNode.Delimited -> {
            val b = "<mo>${escapeXml(node.left)}</mo>"
            val e = "<mo>${escapeXml(node.right)}</mo>"
            val body = renderMathMLNode(node.body)
            "<mrow>$b<mrow>$body</mrow>$e</mrow>"
        }
        is MathNode.Matrix -> {
            val rowsXml = node.rows.joinToString("") { r ->
                val cellsXml = r.joinToString("") { c -> "<mtd>${renderMathMLNode(c)}</mtd>" }
                "<mtr>$cellsXml</mtr>"
            }
            "<mtable>$rowsXml</mtable>"
        }
    }

    private fun renderTextToMathML(raw: String): String {
        if (raw.isEmpty()) return ""
        when (raw) {
            "\u2009" -> return """<mspace width="0.17em"/>"""
            "\u2005" -> return """<mspace width="0.28em"/>"""
            "\u2003" -> return """<mspace width="1em"/>"""
            "\u2003\u2003" -> return """<mspace width="2em"/>"""
        }
        if (raw in KNOWN_MATH_FUNCTIONS) {
            return "<mi mathvariant=\"normal\">${escapeXml(raw)}</mi>"
        }
        if (raw == "dx" || raw == "dy" || raw == "dt" || raw == "dz" || raw == "du" || raw == "dv" || raw == "dr") {
            return """<mspace width="0.17em"/><mi mathvariant="normal">d</mi><mi>${raw.substring(1)}</mi>"""
        }
        if (raw.all { it.isDigit() || it == '.' || it == ',' }) {
            return "<mn>${escapeXml(raw)}</mn>"
        }
        if (raw.length == 1) {
            val c = raw[0]
            return when {
                c.isLetter() -> "<mi>${escapeXml(raw)}</mi>"
                c.isDigit() -> "<mn>${escapeXml(raw)}</mn>"
                c in setOf('+', '-', '×', '÷', '·', '=', '≠', '<', '>', '≤', '≥', '±', '∓', '≈', '→', '⇒', '!', '(', ')', '[', ']', '|', ':', ';', ',') -> "<mo>${escapeXml(raw)}</mo>"
                else -> "<mo>${escapeXml(raw)}</mo>"
            }
        }

        // Multi-char token fallback (e.g. "2x", "2x+1", "x_1", etc.)
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                c.isWhitespace() -> {
                    sb.append("""<mspace width="0.22em"/>""")
                    i++
                }
                c.isDigit() -> {
                    var j = i
                    while (j < raw.length && (raw[j].isDigit() || raw[j] == '.' || raw[j] == ',')) j++
                    sb.append("<mn>").append(escapeXml(raw.substring(i, j))).append("</mn>")
                    i = j
                }
                c.isLetter() -> {
                    val remaining = raw.substring(i)
                    val matchedFunc = KNOWN_MATH_FUNCTIONS.firstOrNull { remaining.startsWith(it) && (remaining.length == it.length || !remaining[it.length].isLetter()) }
                    if (matchedFunc != null) {
                        sb.append("<mi mathvariant=\"normal\">").append(escapeXml(matchedFunc)).append("</mi>")
                        i += matchedFunc.length
                    } else if (remaining.startsWith("dx") || remaining.startsWith("dy") || remaining.startsWith("dt") || remaining.startsWith("dz") || remaining.startsWith("du") || remaining.startsWith("dv") || remaining.startsWith("dr")) {
                        val dVar = remaining.substring(1, 2)
                        sb.append("""<mspace width="0.17em"/><mi mathvariant="normal">d</mi><mi>""").append(escapeXml(dVar)).append("</mi>")
                        i += 2
                    } else {
                        sb.append("<mi>").append(escapeXml(c.toString())).append("</mi>")
                        i++
                    }
                }
                c in setOf('+', '-', '×', '÷', '·', '*', '/', '=', '≠', '<', '>', '≤', '≥', '±', '∓', '≈', '→', '⇒', '!', '(', ')', '[', ']', '|', ':', ';', ',') -> {
                    val op = if (c == '*') "·" else c.toString()
                    sb.append("<mo>").append(escapeXml(op)).append("</mo>")
                    i++
                }
                else -> {
                    sb.append("<mo>").append(escapeXml(c.toString())).append("</mo>")
                    i++
                }
            }
        }
        return sb.toString()
    }

    // ── HTML / WEBVIEW HELPERS ───────────────────────────────────────────────
    fun toHtmlSpan(latex: String): String {
        val clean = cleanLatex(latex)
        val mathMl = toMathML(clean)
        val esc = clean.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
        return """<span class="math-inline" data-latex="$esc" contenteditable="false" title="Ketuk untuk mengedit rumus">$mathMl</span>"""
    }

    // ── PLAIN TEXT UNICODE CONVERTER ─────────────────────────────────────────
    fun toPlainText(latex: String): String {
        return try {
            val root = parse(latex)
            renderPlainText(root).trim()
        } catch (_: Exception) {
            cleanLatex(latex)
        }
    }

    private fun renderPlainText(node: MathNode): String = when (node) {
        is MathNode.Text -> node.text
        is MathNode.Group -> node.children.joinToString("") { renderPlainText(it) }
        is MathNode.Fraction -> "(${renderPlainText(node.num)})/(${renderPlainText(node.den)})"
        is MathNode.Sup -> "${renderPlainText(node.base)}^(${renderPlainText(node.sup)})"
        is MathNode.Sub -> "${renderPlainText(node.base)}_(${renderPlainText(node.sub)})"
        is MathNode.SubSup -> "${renderPlainText(node.base)}_(${renderPlainText(node.sub)})^(${renderPlainText(node.sup)})"
        is MathNode.Radical -> if (node.deg != null) "√[${renderPlainText(node.deg)}](${renderPlainText(node.rad)})" else "√(${renderPlainText(node.rad)})"
        is MathNode.Nary -> {
            val sym = node.symbol
            val sub = node.sub?.let { "_{${renderPlainText(it)}}" } ?: ""
            val sup = node.sup?.let { "^{${renderPlainText(it)}}" } ?: ""
            "$sym$sub$sup ${renderPlainText(node.body)}"
        }
        is MathNode.Lim -> "lim_{${renderPlainText(node.target)}} ${renderPlainText(node.body)}"
        is MathNode.Delimited -> "${node.left}${renderPlainText(node.body)}${node.right}"
        is MathNode.Matrix -> "[" + node.rows.joinToString("; ") { r -> r.joinToString(", ") { renderPlainText(it) } } + "]"
    }

    // ── OMML TO LATEX PARSER (FOR WORD / WPS / GOOGLE DOCS IMPORT) ────────────
    fun parseOmmlToLatex(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1

        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                val name = localName(parser.name)
                when (name) {
                    "t" -> {
                        sb.append(parser.nextText())
                        depth-- // nextText consumes END_TAG
                    }
                    "f" -> {
                        val (num, den) = parseOmmlFraction(parser)
                        depth--
                        sb.append("\\frac{").append(num).append("}{").append(den).append("}")
                    }
                    "sSup" -> {
                        val (base, sup) = parseOmmlBaseAndPart(parser, "sup")
                        depth--
                        sb.append(base).append("^{").append(sup).append("}")
                    }
                    "sSub" -> {
                        val (base, sub) = parseOmmlBaseAndPart(parser, "sub")
                        depth--
                        sb.append(base).append("_{").append(sub).append("}")
                    }
                    "sSubSup" -> {
                        val (base, sub, sup) = parseOmmlSubSup(parser)
                        depth--
                        sb.append(base).append("_{").append(sub).append("}^{").append(sup).append("}")
                    }
                    "rad" -> {
                        val (deg, rad) = parseOmmlRadical(parser)
                        depth--
                        if (deg.isNotBlank()) sb.append("\\sqrt[").append(deg).append("]{").append(rad).append("}")
                        else sb.append("\\sqrt{").append(rad).append("}")
                    }
                    "nary" -> {
                        val (sym, sub, sup, body) = parseOmmlNary(parser)
                        depth--
                        val cmd = when (sym) {
                            "∫" -> "\\int"
                            "∬" -> "\\iint"
                            "∭" -> "\\iiint"
                            "∮" -> "\\oint"
                            "∑" -> "\\sum"
                            "∏" -> "\\prod"
                            else -> sym
                        }
                        sb.append(cmd)
                        if (sub.isNotBlank()) sb.append("_{").append(sub).append("}")
                        if (sup.isNotBlank()) sb.append("^{").append(sup).append("}")
                        if (body.isNotBlank()) sb.append(" ").append(body)
                    }
                    "d" -> {
                        val (l, r, body) = parseOmmlDelimited(parser)
                        depth--
                        sb.append("\\left").append(l.ifEmpty { "(" }).append(body).append("\\right").append(r.ifEmpty { ")" })
                    }
                    "limLow" -> {
                        val (target, body) = parseOmmlLimLow(parser)
                        depth--
                        sb.append("\\lim_{").append(target).append("} ").append(body)
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return sb.toString().trim()
    }

    private fun parseOmmlFraction(parser: XmlPullParser): Pair<String, String> {
        var num = ""
        var den = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "num" -> { num = parseOmmlInner(parser); depth-- }
                    "den" -> { den = parseOmmlInner(parser); depth-- }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return num to den
    }

    private fun parseOmmlBaseAndPart(parser: XmlPullParser, partName: String): Pair<String, String> {
        var base = ""
        var part = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "e" -> { base = parseOmmlInner(parser); depth-- }
                    partName -> { part = parseOmmlInner(parser); depth-- }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return base to part
    }

    private fun parseOmmlSubSup(parser: XmlPullParser): Triple<String, String, String> {
        var base = ""
        var sub = ""
        var sup = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "e" -> { base = parseOmmlInner(parser); depth-- }
                    "sub" -> { sub = parseOmmlInner(parser); depth-- }
                    "sup" -> { sup = parseOmmlInner(parser); depth-- }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return Triple(base, sub, sup)
    }

    private fun parseOmmlRadical(parser: XmlPullParser): Pair<String, String> {
        var deg = ""
        var rad = ""
        var isDegHidden = false
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "degHide" -> isDegHidden = true
                    "deg" -> {
                        deg = parseOmmlInner(parser)
                        depth--
                    }
                    "e" -> {
                        rad = parseOmmlInner(parser)
                        depth--
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return (if (isDegHidden) "" else deg) to rad
    }

    private fun parseOmmlNary(parser: XmlPullParser): Tuple4<String, String, String, String> {
        var sym = "∫"
        var sub = ""
        var sup = ""
        var body = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "chr" -> {
                        val chrVal = parser.getAttributeValue(null, "val")
                            ?: parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/math", "val")
                        if (!chrVal.isNullOrEmpty()) sym = chrVal
                    }
                    "sub" -> { sub = parseOmmlInner(parser); depth-- }
                    "sup" -> { sup = parseOmmlInner(parser); depth-- }
                    "e" -> { body = parseOmmlInner(parser); depth-- }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return Tuple4(sym, sub, sup, body)
    }

    private fun parseOmmlDelimited(parser: XmlPullParser): Triple<String, String, String> {
        var beg = "("
        var end = ")"
        var body = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "begChr" -> {
                        val v = parser.getAttributeValue(null, "val")
                        if (!v.isNullOrEmpty()) beg = v
                    }
                    "endChr" -> {
                        val v = parser.getAttributeValue(null, "val")
                        if (!v.isNullOrEmpty()) end = v
                    }
                    "e" -> { body = parseOmmlInner(parser); depth-- }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return Triple(beg, end, body)
    }

    private fun parseOmmlLimLow(parser: XmlPullParser): Pair<String, String> {
        var lim = ""
        var body = ""
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                when (localName(parser.name)) {
                    "lim" -> { lim = parseOmmlInner(parser); depth-- }
                    "e" -> { body = parseOmmlInner(parser); depth-- }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return lim to body
    }

    private fun parseOmmlInner(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                depth++
                val name = localName(parser.name)
                when (name) {
                    "t" -> {
                        sb.append(parser.nextText())
                        depth--
                    }
                    "f" -> {
                        val (n, d) = parseOmmlFraction(parser)
                        depth--
                        sb.append("\\frac{").append(n).append("}{").append(d).append("}")
                    }
                    "sSup" -> {
                        val (b, s) = parseOmmlBaseAndPart(parser, "sup")
                        depth--
                        sb.append(b).append("^{").append(s).append("}")
                    }
                    "sSub" -> {
                        val (b, s) = parseOmmlBaseAndPart(parser, "sub")
                        depth--
                        sb.append(b).append("_{").append(s).append("}")
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return sb.toString().trim()
    }

    private fun localName(raw: String?): String {
        if (raw == null) return ""
        val idx = raw.indexOf(':')
        return if (idx >= 0) raw.substring(idx + 1) else raw
    }

    data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
