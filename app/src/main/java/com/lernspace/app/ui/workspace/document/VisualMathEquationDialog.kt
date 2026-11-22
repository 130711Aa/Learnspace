package com.lernspace.app.ui.workspace.document

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.ui.theme.*
import java.util.UUID

// ── MATH EXPRESSION TREE (GEOGEBRA RETEX SEQUENCE MODEL) ─────────────────────
/**
 * Immutable tree-based math expression model based on GeoGebra's RetEX engine.
 * A MathSequence is a horizontal sequence of math items (Atoms or nested structures).
 * The editor's cursor is represented as (activeSeqId, cursorOffset), enabling
 * seamless intra-word and intra-formula navigation without artificial placeholder blocks.
 */
sealed class MathExpr {
    abstract val id: String

    /** Leaf visual atom: variable, digit, operator, symbol */
    data class Atom(val text: String, override val id: String = UUID.randomUUID().toString()) : MathExpr()

    /** Sequence of math items — if empty, renders as a GeoGebra-style placeholder slot */
    data class Sequence(
        val items: List<MathExpr> = emptyList(),
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** a/b */
    data class Fraction(
        val num: Sequence,
        val den: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** base^exp */
    data class Power(
        val base: MathExpr,
        val exp: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** base_{sub} */
    data class Subscript(
        val base: MathExpr,
        val sub: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** base_{sub}^{exp} */
    data class SubSup(
        val base: MathExpr,
        val sub: Sequence,
        val exp: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** √[deg]{rad} */
    data class Root(
        val degree: Sequence?,
        val radicand: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** ∫_{lower}^{upper} integrand d var */
    data class Integral(
        val lower: Sequence,
        val upper: Sequence,
        val integrand: Sequence,
        val variable: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** ∑_{lower}^{upper} body */
    data class Summation(
        val lower: Sequence,
        val upper: Sequence,
        val body: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** lim_{var → target} body */
    data class Limit(
        val variable: Sequence,
        val target: Sequence,
        val body: Sequence,
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()

    /** (body) with delimiters */
    data class Parens(
        val body: Sequence,
        val left: String = "(",
        val right: String = ")",
        override val id: String = UUID.randomUUID().toString()
    ) : MathExpr()
}

// ── LATEX SERIALIZER ─────────────────────────────────────────────────────────
fun MathExpr.toLatex(): String = when (this) {
    is MathExpr.Atom -> {
        if (text in LatexMathCodec.KNOWN_MATH_FUNCTIONS) "\\$text "
        else text
    }
    is MathExpr.Sequence -> items.joinToString("") { it.toLatex() }
    is MathExpr.Fraction -> "\\frac{${num.toLatex()}}{${den.toLatex()}}"
    is MathExpr.Power -> "${base.toLatex()}^{${exp.toLatex()}}"
    is MathExpr.Subscript -> "${base.toLatex()}_{${sub.toLatex()}}"
    is MathExpr.SubSup -> "${base.toLatex()}_{${sub.toLatex()}}^{${exp.toLatex()}}"
    is MathExpr.Root -> if (degree != null) "\\sqrt[${degree.toLatex()}]{${radicand.toLatex()}}" else "\\sqrt{${radicand.toLatex()}}"
    is MathExpr.Integral -> {
        val low = lower.toLatex().trim()
        val up = upper.toLatex().trim()
        val bounds = when {
            low.isNotEmpty() && up.isNotEmpty() -> "_{$low}^{$up}"
            low.isNotEmpty() -> "_{$low}"
            up.isNotEmpty() -> "^{$up}"
            else -> ""
        }
        val varStr = variable.toLatex().trim()
        val diff = if (varStr.isNotEmpty()) " \\, d$varStr" else ""
        "\\int$bounds ${integrand.toLatex()}$diff"
    }
    is MathExpr.Summation -> "\\sum_{${lower.toLatex()}}^{${upper.toLatex()}} ${body.toLatex()}"
    is MathExpr.Limit -> "\\lim_{${variable.toLatex()} \\to ${target.toLatex()}} ${body.toLatex()}"
    is MathExpr.Parens -> {
        when {
            left == "(" && right == ")" -> "\\left( ${body.toLatex()} \\right)"
            left == "[" && right == "]" -> "\\left[ ${body.toLatex()} \\right]"
            left == "{" && right == "}" -> "\\left\\{ ${body.toLatex()} \\right\\}"
            left == "|" && right == "|" -> "\\left| ${body.toLatex()} \\right|"
            else -> "\\left$left ${body.toLatex()} \\right$right"
        }
    }
}

// ── TREE MANIPULATION HELPERS ────────────────────────────────────────────────
fun replaceSequence(tree: MathExpr, targetSeqId: String, newSeq: MathExpr.Sequence): MathExpr {
    return when (tree) {
        is MathExpr.Sequence -> {
            if (tree.id == targetSeqId) newSeq
            else tree.copy(items = tree.items.map { replaceSequence(it, targetSeqId, newSeq) })
        }
        is MathExpr.Atom -> tree
        is MathExpr.Fraction -> MathExpr.Fraction(
            num = replaceSequence(tree.num, targetSeqId, newSeq) as MathExpr.Sequence,
            den = replaceSequence(tree.den, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Power -> MathExpr.Power(
            base = replaceSequence(tree.base, targetSeqId, newSeq),
            exp = replaceSequence(tree.exp, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Subscript -> MathExpr.Subscript(
            base = replaceSequence(tree.base, targetSeqId, newSeq),
            sub = replaceSequence(tree.sub, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.SubSup -> MathExpr.SubSup(
            base = replaceSequence(tree.base, targetSeqId, newSeq),
            sub = replaceSequence(tree.sub, targetSeqId, newSeq) as MathExpr.Sequence,
            exp = replaceSequence(tree.exp, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Root -> MathExpr.Root(
            degree = tree.degree?.let { replaceSequence(it, targetSeqId, newSeq) as MathExpr.Sequence },
            radicand = replaceSequence(tree.radicand, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Integral -> MathExpr.Integral(
            lower = replaceSequence(tree.lower, targetSeqId, newSeq) as MathExpr.Sequence,
            upper = replaceSequence(tree.upper, targetSeqId, newSeq) as MathExpr.Sequence,
            integrand = replaceSequence(tree.integrand, targetSeqId, newSeq) as MathExpr.Sequence,
            variable = replaceSequence(tree.variable, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Summation -> MathExpr.Summation(
            lower = replaceSequence(tree.lower, targetSeqId, newSeq) as MathExpr.Sequence,
            upper = replaceSequence(tree.upper, targetSeqId, newSeq) as MathExpr.Sequence,
            body = replaceSequence(tree.body, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Limit -> MathExpr.Limit(
            variable = replaceSequence(tree.variable, targetSeqId, newSeq) as MathExpr.Sequence,
            target = replaceSequence(tree.target, targetSeqId, newSeq) as MathExpr.Sequence,
            body = replaceSequence(tree.body, targetSeqId, newSeq) as MathExpr.Sequence,
            id = tree.id
        )
        is MathExpr.Parens -> MathExpr.Parens(
            body = replaceSequence(tree.body, targetSeqId, newSeq) as MathExpr.Sequence,
            left = tree.left, right = tree.right, id = tree.id
        )
    }
}

fun findSequence(tree: MathExpr, seqId: String): MathExpr.Sequence? {
    if (tree is MathExpr.Sequence) {
        if (tree.id == seqId) return tree
        for (item in tree.items) {
            findSequence(item, seqId)?.let { return it }
        }
    }
    return when (tree) {
        is MathExpr.Fraction -> findSequence(tree.num, seqId) ?: findSequence(tree.den, seqId)
        is MathExpr.Power -> findSequence(tree.base, seqId) ?: findSequence(tree.exp, seqId)
        is MathExpr.Subscript -> findSequence(tree.base, seqId) ?: findSequence(tree.sub, seqId)
        is MathExpr.SubSup -> findSequence(tree.base, seqId) ?: findSequence(tree.sub, seqId) ?: findSequence(tree.exp, seqId)
        is MathExpr.Root -> (tree.degree?.let { findSequence(it, seqId) }) ?: findSequence(tree.radicand, seqId)
        is MathExpr.Integral -> findSequence(tree.upper, seqId) ?: findSequence(tree.lower, seqId) ?:
                findSequence(tree.integrand, seqId) ?: findSequence(tree.variable, seqId)
        is MathExpr.Summation -> findSequence(tree.upper, seqId) ?: findSequence(tree.lower, seqId) ?: findSequence(tree.body, seqId)
        is MathExpr.Limit -> findSequence(tree.variable, seqId) ?: findSequence(tree.target, seqId) ?: findSequence(tree.body, seqId)
        is MathExpr.Parens -> findSequence(tree.body, seqId)
        else -> null
    }
}

data class ContainerInfo(
    val container: MathExpr,
    val parentSeq: MathExpr.Sequence,
    val indexInParentSeq: Int
)

fun findContainerInfo(tree: MathExpr, childSeqId: String): ContainerInfo? {
    fun search(seq: MathExpr.Sequence): ContainerInfo? {
        seq.items.forEachIndexed { idx, item ->
            when (item) {
                is MathExpr.Fraction -> {
                    if (item.num.id == childSeqId || item.den.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    search(item.num)?.let { return it }
                    search(item.den)?.let { return it }
                }
                is MathExpr.Power -> {
                    if (item.exp.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    if (item.base is MathExpr.Sequence) {
                        if (item.base.id == childSeqId) return ContainerInfo(item, seq, idx)
                        search(item.base)?.let { return it }
                    }
                    search(item.exp)?.let { return it }
                }
                is MathExpr.Subscript -> {
                    if (item.sub.id == childSeqId) return ContainerInfo(item, seq, idx)
                    if (item.base is MathExpr.Sequence) {
                        if (item.base.id == childSeqId) return ContainerInfo(item, seq, idx)
                        search(item.base)?.let { return it }
                    }
                    search(item.sub)?.let { return it }
                }
                is MathExpr.SubSup -> {
                    if (item.sub.id == childSeqId || item.exp.id == childSeqId) return ContainerInfo(item, seq, idx)
                    if (item.base is MathExpr.Sequence) {
                        if (item.base.id == childSeqId) return ContainerInfo(item, seq, idx)
                        search(item.base)?.let { return it }
                    }
                    search(item.sub)?.let { return it }
                    search(item.exp)?.let { return it }
                }
                is MathExpr.Root -> {
                    if ((item.degree != null && item.degree.id == childSeqId) || item.radicand.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    item.degree?.let { search(it) }?.let { return it }
                    search(item.radicand)?.let { return it }
                }
                is MathExpr.Integral -> {
                    if (item.upper.id == childSeqId || item.lower.id == childSeqId ||
                        item.integrand.id == childSeqId || item.variable.id == childSeqId
                    ) {
                        return ContainerInfo(item, seq, idx)
                    }
                    search(item.upper)?.let { return it }
                    search(item.lower)?.let { return it }
                    search(item.integrand)?.let { return it }
                    search(item.variable)?.let { return it }
                }
                is MathExpr.Summation -> {
                    if (item.upper.id == childSeqId || item.lower.id == childSeqId || item.body.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    search(item.upper)?.let { return it }
                    search(item.lower)?.let { return it }
                    search(item.body)?.let { return it }
                }
                is MathExpr.Limit -> {
                    if (item.variable.id == childSeqId || item.target.id == childSeqId || item.body.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    search(item.variable)?.let { return it }
                    search(item.target)?.let { return it }
                    search(item.body)?.let { return it }
                }
                is MathExpr.Parens -> {
                    if (item.body.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    search(item.body)?.let { return it }
                }
                is MathExpr.Sequence -> {
                    if (item.id == childSeqId) {
                        return ContainerInfo(item, seq, idx)
                    }
                    search(item)?.let { return it }
                }
                else -> {}
            }
        }
        return null
    }

    return if (tree is MathExpr.Sequence) search(tree) else null
}

// ── LATEX TO MATHEXPR PARSER ─────────────────────────────────────────────────
fun normalizeLatexForCodec(raw: String): String {
    var text = raw.trim().removeSurrounding("$$", "$$").removeSurrounding("$", "$").trim()
    if (text.isBlank()) return ""

    // 0. Hilangkan perkalian eksplisit untuk perkalian implisit alami: 2*x -> 2x, a*x -> ax
    text = com.lernspace.app.core.math.MathPlotHelper.denormalizeImplicitMultiplication(text)

    // 1. Normalisasi varian unicode integral ∫ ke command LaTeX \int
    // Pola kurung siku: ∫[a, b] f(x) dx atau ∫[a,b] f(x) dx
    val bracketRegex = Regex("""∫\s*\[\s*([^,\]]+)\s*,\s*([^\]]+)\s*\]""")
    text = bracketRegex.replace(text) { m ->
        "\\int_{${m.groupValues[1].trim()}}^{${m.groupValues[2].trim()}} "
    }

    // Pola range: ∫ a..b f(x) dx
    val rangeRegex = Regex("""∫\s*([^\s\.]+)\s*\.\.\s*([^\s]+)""")
    text = rangeRegex.replace(text) { m ->
        "\\int_{${m.groupValues[1].trim()}}^{${m.groupValues[2].trim()}} "
    }

    // Pola sub / sup dengan simbol unicode: ∫_{a}^{b} atau ∫_a^b
    text = text.replace(Regex("""∫\s*_"""), "\\int_")
    text = text.replace(Regex("""∫\s*\^"""), "\\int^")

    // Pola integral tanpa batas: ∫ f(x) dx
    text = text.replace("∫", "\\int ")

    // 2. Normalisasi kalkulus / operator lain jika memakai simbol unicode
    text = text.replace(Regex("""∑\s*_"""), "\\sum_")
    text = text.replace(Regex("""∑\s*\^"""), "\\sum^")
    text = text.replace("∑", "\\sum ")

    text = text.replace(Regex("""∏\s*_"""), "\\prod_")
    text = text.replace(Regex("""∏\s*\^"""), "\\prod^")
    text = text.replace("∏", "\\prod ")

    text = text.replace("√", "\\sqrt ")

    return text.trim()
}

fun latexToMathExpr(latex: String): MathExpr.Sequence {
    val clean = normalizeLatexForCodec(latex)
    if (clean.isBlank()) return MathExpr.Sequence()
    return try {
        val parsed = LatexMathCodec.parse(clean)
        ensureSequence(convertCodecNodeToExpr(parsed))
    } catch (_: Exception) {
        MathExpr.Sequence(clean.map { MathExpr.Atom(it.toString()) })
    }
}

fun ensureSequence(expr: MathExpr): MathExpr.Sequence = when (expr) {
    is MathExpr.Sequence -> expr
    else -> MathExpr.Sequence(listOf(expr))
}

fun unwrapSingleAtom(expr: MathExpr): MathExpr {
    if (expr is MathExpr.Sequence && expr.items.size == 1) {
        return expr.items.first()
    }
    return expr
}

private fun sanitizeIntegrandItems(items: List<MathExpr>): List<MathExpr> {
    val result = mutableListOf<MathExpr>()
    for (i in items.indices) {
        val current = items[i]
        if (current is MathExpr.Atom && (current.text == "*" || current.text == "·")) {
            val prev = items.getOrNull(i - 1)
            val next = items.getOrNull(i + 1)
            val isPrevOperand = prev is MathExpr.Atom && (prev.text.all { it.isDigit() } || prev.text.all { it.isLetter() })
            val isNextOperand = next is MathExpr.Atom && next.text.all { it.isLetter() }
            if (isPrevOperand && isNextOperand) {
                continue // Lewati tanda bintang perkalian implisit
            }
        }
        result.add(current)
    }
    return result
}

fun extractIntegralBodyAndVar(bodySeq: MathExpr.Sequence): Pair<MathExpr.Sequence, MathExpr.Sequence> {
    val cleanItems = bodySeq.items.dropLastWhile { it is MathExpr.Atom && (it.text.isBlank() || it.text == ",") }
    if (cleanItems.isEmpty()) return MathExpr.Sequence() to MathExpr.Sequence(listOf(MathExpr.Atom("x")))

    val last = cleanItems.last()
    if (last is MathExpr.Atom && last.text.startsWith("d") && last.text.length > 1) {
        val varChar = last.text.substring(1)
        val rest = cleanItems.dropLast(1).filterNot { it is MathExpr.Atom && (it.text.isBlank() || it.text == ",") }
        return MathExpr.Sequence(sanitizeIntegrandItems(rest)) to MathExpr.Sequence(listOf(MathExpr.Atom(varChar)))
    }
    if (cleanItems.size >= 2) {
        val secondLast = cleanItems[cleanItems.size - 2]
        if (secondLast is MathExpr.Atom && secondLast.text.trim() == "d" && last is MathExpr.Atom && last.text.length == 1 && last.text.first().isLetter()) {
            val rest = cleanItems.dropLast(2).filterNot { it is MathExpr.Atom && (it.text.isBlank() || it.text == ",") }
            return MathExpr.Sequence(sanitizeIntegrandItems(rest)) to MathExpr.Sequence(listOf(last))
        }
    }
    return MathExpr.Sequence(sanitizeIntegrandItems(cleanItems)) to MathExpr.Sequence(listOf(MathExpr.Atom("x")))
}

private fun convertCodecNodeToExpr(node: LatexMathCodec.MathNode): MathExpr = when (node) {
    is LatexMathCodec.MathNode.Text -> {
        if (node.text in LatexMathCodec.KNOWN_MATH_FUNCTIONS) {
            MathExpr.Sequence(listOf(MathExpr.Atom(node.text)))
        } else {
            MathExpr.Sequence(node.text.map { MathExpr.Atom(it.toString()) })
        }
    }
    is LatexMathCodec.MathNode.Fraction -> MathExpr.Fraction(
        num = ensureSequence(convertCodecNodeToExpr(node.num)),
        den = ensureSequence(convertCodecNodeToExpr(node.den))
    )
    is LatexMathCodec.MathNode.Sup -> MathExpr.Power(
        base = unwrapSingleAtom(convertCodecNodeToExpr(node.base)),
        exp = ensureSequence(convertCodecNodeToExpr(node.sup))
    )
    is LatexMathCodec.MathNode.Sub -> MathExpr.Subscript(
        base = unwrapSingleAtom(convertCodecNodeToExpr(node.base)),
        sub = ensureSequence(convertCodecNodeToExpr(node.sub))
    )
    is LatexMathCodec.MathNode.SubSup -> MathExpr.SubSup(
        base = unwrapSingleAtom(convertCodecNodeToExpr(node.base)),
        sub = ensureSequence(convertCodecNodeToExpr(node.sub)),
        exp = ensureSequence(convertCodecNodeToExpr(node.sup))
    )
    is LatexMathCodec.MathNode.Radical -> MathExpr.Root(
        degree = node.deg?.let { ensureSequence(convertCodecNodeToExpr(it)) },
        radicand = ensureSequence(convertCodecNodeToExpr(node.rad))
    )
    is LatexMathCodec.MathNode.Nary -> {
        if (node.symbol in setOf("∫", "∬", "∭", "∮") || node.symbol.contains("int")) {
            val bodySeq = ensureSequence(convertCodecNodeToExpr(node.body))
            val (integrandSeq, varSeq) = extractIntegralBodyAndVar(bodySeq)
            MathExpr.Integral(
                lower = ensureSequence(node.sub?.let { convertCodecNodeToExpr(it) } ?: MathExpr.Sequence()),
                upper = ensureSequence(node.sup?.let { convertCodecNodeToExpr(it) } ?: MathExpr.Sequence()),
                integrand = integrandSeq,
                variable = varSeq
            )
        } else {
            MathExpr.Summation(
                lower = ensureSequence(node.sub?.let { convertCodecNodeToExpr(it) } ?: MathExpr.Sequence()),
                upper = ensureSequence(node.sup?.let { convertCodecNodeToExpr(it) } ?: MathExpr.Sequence()),
                body = ensureSequence(convertCodecNodeToExpr(node.body))
            )
        }
    }
    is LatexMathCodec.MathNode.Delimited -> MathExpr.Parens(
        body = ensureSequence(convertCodecNodeToExpr(node.body)),
        left = node.left,
        right = node.right
    )
    is LatexMathCodec.MathNode.Lim -> MathExpr.Limit(
        variable = ensureSequence(convertCodecNodeToExpr(node.target)),
        target = MathExpr.Sequence(),
        body = ensureSequence(convertCodecNodeToExpr(node.body))
    )
    is LatexMathCodec.MathNode.Group -> {
        val flattened = node.children.flatMap { child ->
            val expr = convertCodecNodeToExpr(child)
            if (expr is MathExpr.Sequence) expr.items else listOf(expr)
        }
        MathExpr.Sequence(flattened)
    }
    else -> MathExpr.Sequence(node.toString().map { MathExpr.Atom(it.toString()) })
}

// ── SNAPSHOT FOR UNDO / REDO HISTORY ─────────────────────────────────────────
data class EditorSnapshot(
    val root: MathExpr.Sequence,
    val activeSeqId: String,
    val cursorOffset: Int
)

// ── MAIN DIALOG ───────────────────────────────────────────────────────────────
@Composable
fun VisualMathEquationDialog(
    initialEquation: String = "",
    onDismiss: () -> Unit,
    onInsertEquation: (latex: String) -> Unit
) {
    // Equation tree state (root is a MathSequence)
    var root by remember { mutableStateOf(latexToMathExpr(initialEquation)) }

    // Active focused sequence and character/item offset within that sequence
    var activeSeqId by remember { mutableStateOf(root.id) }
    var cursorOffset by remember { mutableIntStateOf(root.items.size) }

    // Undo / Redo history stacks
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }

    fun saveSnapshot() {
        if (undoStack.size >= 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(EditorSnapshot(root, activeSeqId, cursorOffset))
        redoStack.clear()
    }

    fun handleUndo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(EditorSnapshot(root, activeSeqId, cursorOffset))
        root = previous.root
        if (findSequence(previous.root, previous.activeSeqId) != null) {
            activeSeqId = previous.activeSeqId
            cursorOffset = previous.cursorOffset
        } else {
            activeSeqId = previous.root.id
            cursorOffset = previous.root.items.size
        }
    }

    fun handleRedo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(EditorSnapshot(root, activeSeqId, cursorOffset))
        root = next.root
        if (findSequence(next.root, next.activeSeqId) != null) {
            activeSeqId = next.activeSeqId
            cursorOffset = next.cursorOffset
        } else {
            activeSeqId = next.root.id
            cursorOffset = next.root.items.size
        }
    }

    // Current active keyboard tab (0=123, 1=f(x), 2=abc, 3=αβγ)
    var keyboardTab by remember { mutableIntStateOf(0) }

    // Hardware keyboard focus requester
    val focusRequester = remember { FocusRequester() }

    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursorAnim")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // ── SMART INPUT & STRUCTURE EXPANSION (GEOGEBRA RETEX CONTROLLER) ──

    fun insertText(char: String) {
        saveSnapshot()
        if (char == "/" || char == " / ") {
            // Intercept fraction
            val currentSeq = findSequence(root, activeSeqId) ?: return
            val items = currentSeq.items.toMutableList()

            if (cursorOffset > 0) {
                val lastIdx = cursorOffset - 1
                val lastItem = items[lastIdx]

                if (lastItem is MathExpr.Parens) {
                    val numSeq = lastItem.body
                    val frac = MathExpr.Fraction(num = numSeq, den = MathExpr.Sequence(emptyList()))
                    items[lastIdx] = frac
                    root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                    activeSeqId = frac.den.id
                    cursorOffset = 0
                    return
                }

                // Scan backward for alphanumeric token (e.g. 2x, 15, x)
                var startIdx = lastIdx
                while (startIdx >= 0) {
                    val itm = items[startIdx]
                    if (itm is MathExpr.Atom && itm.text.all { it.isLetterOrDigit() || it == '.' }) {
                        startIdx--
                    } else {
                        break
                    }
                }
                val stolenStart = startIdx + 1
                if (stolenStart <= lastIdx) {
                    val stolenList = items.subList(stolenStart, lastIdx + 1).toList()
                    val numSeq = MathExpr.Sequence(stolenList)
                    val frac = MathExpr.Fraction(num = numSeq, den = MathExpr.Sequence(emptyList()))
                    val newItems = items.toMutableList()
                    for (k in lastIdx downTo stolenStart) {
                        newItems.removeAt(k)
                    }
                    newItems.add(stolenStart, frac)
                    root = replaceSequence(root, activeSeqId, currentSeq.copy(items = newItems)) as MathExpr.Sequence
                    activeSeqId = frac.den.id
                    cursorOffset = 0
                    return
                }
            }

            val frac = MathExpr.Fraction(num = MathExpr.Sequence(emptyList()), den = MathExpr.Sequence(emptyList()))
            items.add(cursorOffset, frac)
            root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            activeSeqId = frac.num.id
            cursorOffset = 0
            return
        }

        if (char == "^") {
            // Intercept power
            val currentSeq = findSequence(root, activeSeqId) ?: return
            val items = currentSeq.items.toMutableList()

            if (cursorOffset > 0) {
                val lastIdx = cursorOffset - 1
                val lastItem = items[lastIdx]
                val pow = MathExpr.Power(base = lastItem, exp = MathExpr.Sequence(emptyList()))
                items[lastIdx] = pow
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                activeSeqId = pow.exp.id
                cursorOffset = 0
                return
            }

            val pow = MathExpr.Power(base = MathExpr.Sequence(emptyList()), exp = MathExpr.Sequence(emptyList()))
            items.add(cursorOffset, pow)
            root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            activeSeqId = pow.exp.id
            cursorOffset = 0
            return
        }

        // Regular character input: add Atom at cursorOffset
        val currentSeq = findSequence(root, activeSeqId) ?: return
        val items = currentSeq.items.toMutableList()

        val newAtom = MathExpr.Atom(char)
        items.add(cursorOffset, newAtom)
        root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        cursorOffset++
    }

    fun interceptPowerSquare() {
        saveSnapshot()
        val currentSeq = findSequence(root, activeSeqId) ?: return
        val items = currentSeq.items.toMutableList()

        if (cursorOffset > 0) {
            val lastIdx = cursorOffset - 1
            val lastItem = items[lastIdx]
            val pow = MathExpr.Power(base = lastItem, exp = MathExpr.Sequence(listOf(MathExpr.Atom("2"))))
            items[lastIdx] = pow
            root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            cursorOffset = lastIdx + 1
            return
        }

        val pow = MathExpr.Power(base = MathExpr.Atom("x"), exp = MathExpr.Sequence(listOf(MathExpr.Atom("2"))))
        items.add(cursorOffset, pow)
        root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        cursorOffset = 1
    }

    fun insertFunctionWithParens(funcName: String) {
        saveSnapshot()
        val currentSeq = findSequence(root, activeSeqId) ?: return
        val items = currentSeq.items.toMutableList()
        val fnAtom = MathExpr.Atom(funcName)
        val parens = MathExpr.Parens(body = MathExpr.Sequence(emptyList()))
        items.add(cursorOffset, fnAtom)
        items.add(cursorOffset + 1, parens)
        root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        activeSeqId = parens.body.id
        cursorOffset = 0
    }

    fun insertDerivativeWithParens() {
        saveSnapshot()
        val currentSeq = findSequence(root, activeSeqId) ?: return
        val items = currentSeq.items.toMutableList()
        val ddx = MathExpr.Fraction(
            num = MathExpr.Sequence(listOf(MathExpr.Atom("d"))),
            den = MathExpr.Sequence(listOf(MathExpr.Atom("d"), MathExpr.Atom("x")))
        )
        val parens = MathExpr.Parens(body = MathExpr.Sequence(emptyList()))
        items.add(cursorOffset, ddx)
        items.add(cursorOffset + 1, parens)
        root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        activeSeqId = parens.body.id
        cursorOffset = 0
    }

    fun insertStructure(builder: () -> MathExpr) {
        saveSnapshot()
        val template = builder()
        if (template is MathExpr.Fraction) {
            insertText("/")
            return
        }
        if (template is MathExpr.Power) {
            insertText("^")
            return
        }

        val currentSeq = findSequence(root, activeSeqId) ?: return
        val items = currentSeq.items.toMutableList()

        when (template) {
            is MathExpr.Root -> {
                items.add(cursorOffset, template)
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                activeSeqId = template.degree?.id ?: template.radicand.id
                cursorOffset = 0
            }
            is MathExpr.Parens -> {
                items.add(cursorOffset, template)
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                activeSeqId = template.body.id
                cursorOffset = 0
            }
            is MathExpr.Integral -> {
                items.add(cursorOffset, template)
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                activeSeqId = if (template.upper.items.isEmpty()) template.upper.id else template.integrand.id
                cursorOffset = 0
            }
            else -> {
                items.add(cursorOffset, template)
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                cursorOffset++
            }
        }
    }

    // ── RECURSIVE GEOGEBRA-STYLE NAVIGATION ──

    fun handleNavLeft() {
        val currentSeq = findSequence(root, activeSeqId) ?: return
        if (cursorOffset > 0) {
            val prevItem = currentSeq.items[cursorOffset - 1]
            when (prevItem) {
                is MathExpr.Fraction -> {
                    activeSeqId = prevItem.den.id
                    cursorOffset = prevItem.den.items.size
                }
                is MathExpr.Power -> {
                    activeSeqId = prevItem.exp.id
                    cursorOffset = prevItem.exp.items.size
                }
                is MathExpr.Root -> {
                    activeSeqId = prevItem.radicand.id
                    cursorOffset = prevItem.radicand.items.size
                }
                is MathExpr.Integral -> {
                    activeSeqId = prevItem.variable.id
                    cursorOffset = prevItem.variable.items.size
                }
                is MathExpr.Parens -> {
                    activeSeqId = prevItem.body.id
                    cursorOffset = prevItem.body.items.size
                }
                else -> {
                    cursorOffset--
                }
            }
        } else {
            // At beginning of sequence, climb up out of container
            val info = findContainerInfo(root, activeSeqId)
            if (info != null) {
                when (info.container) {
                    is MathExpr.Power -> {
                        activeSeqId = info.parentSeq.id
                        cursorOffset = info.indexInParentSeq
                    }
                    is MathExpr.Fraction -> {
                        if (activeSeqId == info.container.den.id) {
                            activeSeqId = info.container.num.id
                            cursorOffset = info.container.num.items.size
                        } else {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Integral -> {
                        if (activeSeqId == info.container.variable.id) {
                            activeSeqId = info.container.integrand.id
                            cursorOffset = info.container.integrand.items.size
                        } else if (activeSeqId == info.container.integrand.id) {
                            activeSeqId = info.container.lower.id
                            cursorOffset = info.container.lower.items.size
                        } else if (activeSeqId == info.container.lower.id) {
                            activeSeqId = info.container.upper.id
                            cursorOffset = info.container.upper.items.size
                        } else {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Root -> {
                        if (info.container.degree != null && activeSeqId == info.container.radicand.id) {
                            activeSeqId = info.container.degree.id
                            cursorOffset = info.container.degree.items.size
                        } else {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Parens -> {
                        activeSeqId = info.parentSeq.id
                        cursorOffset = info.indexInParentSeq
                    }
                    else -> {
                        activeSeqId = info.parentSeq.id
                        cursorOffset = info.indexInParentSeq
                    }
                }
            }
        }
    }

    fun handleNavRight() {
        val currentSeq = findSequence(root, activeSeqId) ?: return
        if (cursorOffset < currentSeq.items.size) {
            val nextItem = currentSeq.items[cursorOffset]
            when (nextItem) {
                is MathExpr.Fraction -> {
                    activeSeqId = nextItem.num.id
                    cursorOffset = 0
                }
                is MathExpr.Power -> {
                    activeSeqId = nextItem.exp.id
                    cursorOffset = 0
                }
                is MathExpr.Root -> {
                    activeSeqId = (nextItem.degree ?: nextItem.radicand).id
                    cursorOffset = 0
                }
                is MathExpr.Integral -> {
                    activeSeqId = nextItem.upper.id
                    cursorOffset = 0
                }
                is MathExpr.Parens -> {
                    activeSeqId = nextItem.body.id
                    cursorOffset = 0
                }
                else -> {
                    cursorOffset++
                }
            }
        } else {
            // At end of sequence, climb up out of container to the right
            val info = findContainerInfo(root, activeSeqId)
            if (info != null) {
                when (info.container) {
                    is MathExpr.Power -> {
                        activeSeqId = info.parentSeq.id
                        cursorOffset = info.indexInParentSeq + 1
                    }
                    is MathExpr.Fraction -> {
                        if (activeSeqId == info.container.num.id) {
                            activeSeqId = info.container.den.id
                            cursorOffset = 0
                        } else {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Integral -> {
                        if (activeSeqId == info.container.upper.id) {
                            activeSeqId = info.container.lower.id
                            cursorOffset = 0
                        } else if (activeSeqId == info.container.lower.id) {
                            activeSeqId = info.container.integrand.id
                            cursorOffset = 0
                        } else if (activeSeqId == info.container.integrand.id) {
                            activeSeqId = info.container.variable.id
                            cursorOffset = 0
                        } else {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Root -> {
                        if (info.container.degree != null && activeSeqId == info.container.degree.id) {
                            activeSeqId = info.container.radicand.id
                            cursorOffset = 0
                        } else {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Parens -> {
                        activeSeqId = info.parentSeq.id
                        cursorOffset = info.indexInParentSeq + 1
                    }
                    else -> {
                        activeSeqId = info.parentSeq.id
                        cursorOffset = info.indexInParentSeq + 1
                    }
                }
            }
        }
    }

    fun handleNavDown() {
        val info = findContainerInfo(root, activeSeqId)
        if (info != null) {
            when (info.container) {
                is MathExpr.Power -> {
                    // GeoGebra moveOutOfSuperSubScript: drop down out of exponent to baseline!
                    activeSeqId = info.parentSeq.id
                    cursorOffset = info.indexInParentSeq + 1
                    return
                }
                is MathExpr.Fraction -> {
                    if (activeSeqId == info.container.num.id) {
                        activeSeqId = info.container.den.id
                        cursorOffset = cursorOffset.coerceAtMost(info.container.den.items.size)
                        return
                    }
                }
                is MathExpr.Integral -> {
                    if (activeSeqId == info.container.upper.id) {
                        activeSeqId = info.container.lower.id
                        cursorOffset = 0
                        return
                    }
                }
                else -> {}
            }
        }
        handleNavRight()
    }

    fun handleNavUp() {
        val info = findContainerInfo(root, activeSeqId)
        if (info != null) {
            when (info.container) {
                is MathExpr.Fraction -> {
                    if (activeSeqId == info.container.den.id) {
                        activeSeqId = info.container.num.id
                        cursorOffset = cursorOffset.coerceAtMost(info.container.num.items.size)
                        return
                    }
                }
                is MathExpr.Integral -> {
                    if (activeSeqId == info.container.lower.id) {
                        activeSeqId = info.container.upper.id
                        cursorOffset = 0
                        return
                    }
                }
                else -> {}
            }
        }
        handleNavLeft()
    }

    fun handleBackspace() {
        saveSnapshot()
        val currentSeq = findSequence(root, activeSeqId) ?: return
        if (cursorOffset > 0) {
            val items = currentSeq.items.toMutableList()
            val item = items[cursorOffset - 1]
            if (item is MathExpr.Atom) {
                items.removeAt(cursorOffset - 1)
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                cursorOffset--
            } else if (item is MathExpr.Power) {
                if (item.exp.items.isEmpty()) {
                    items[cursorOffset - 1] = item.base
                    root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                } else {
                    activeSeqId = item.exp.id
                    cursorOffset = item.exp.items.size
                }
            } else if (item is MathExpr.Fraction) {
                activeSeqId = item.den.id
                cursorOffset = item.den.items.size
            } else {
                items.removeAt(cursorOffset - 1)
                root = replaceSequence(root, activeSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                cursorOffset--
            }
        } else {
            // At offset 0: dissolve structure if empty
            val info = findContainerInfo(root, activeSeqId)
            if (info != null) {
                when (info.container) {
                    is MathExpr.Fraction -> {
                        if (activeSeqId == info.container.den.id && info.container.den.items.isEmpty()) {
                            // Dissolve fraction, restore numerator to parent sequence
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems.removeAt(info.indexInParentSeq)
                            pItems.addAll(info.indexInParentSeq, info.container.num.items)
                            root = replaceSequence(root, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq + info.container.num.items.size
                        } else if (activeSeqId == info.container.num.id && info.container.num.items.isEmpty()) {
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Power -> {
                        if (activeSeqId == info.container.exp.id && info.container.exp.items.isEmpty()) {
                            // Dissolve power, restore base to parent sequence
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems[info.indexInParentSeq] = info.container.base
                            root = replaceSequence(root, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Root -> {
                        if (activeSeqId == info.container.radicand.id && info.container.radicand.items.isEmpty()) {
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems.removeAt(info.indexInParentSeq)
                            root = replaceSequence(root, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Parens -> {
                        if (activeSeqId == info.container.body.id && info.container.body.items.isEmpty()) {
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems.removeAt(info.indexInParentSeq)
                            root = replaceSequence(root, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            activeSeqId = info.parentSeq.id
                            cursorOffset = info.indexInParentSeq
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun handleClear() {
        saveSnapshot()
        val newRoot = MathExpr.Sequence(emptyList())
        root = newRoot
        activeSeqId = newRoot.id
        cursorOffset = 0
    }

    val currentLatex = root.toLatex().trim()

    // Auto-focus on hardware keyboard listener
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(min = 540.dp, max = 680.dp)
                .fillMaxHeight(0.92f)
                .padding(12.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (keyEvent.isCtrlPressed) {
                            when (keyEvent.key) {
                                Key.Z -> {
                                    if (keyEvent.isShiftPressed) handleRedo() else handleUndo()
                                    return@onKeyEvent true
                                }
                                Key.Y -> {
                                    handleRedo()
                                    return@onKeyEvent true
                                }
                            }
                        }
                        when (keyEvent.key) {
                            Key.Slash, Key.NumPadDivide -> {
                                insertText("/")
                                true
                            }
                            Key.DirectionLeft -> {
                                handleNavLeft()
                                true
                            }
                            Key.DirectionRight -> {
                                handleNavRight()
                                true
                            }
                            Key.DirectionDown -> {
                                handleNavDown()
                                true
                            }
                            Key.DirectionUp -> {
                                handleNavUp()
                                true
                            }
                            Key.Backspace -> {
                                handleBackspace()
                                true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                if (currentLatex.isNotBlank()) {
                                    onInsertEquation(currentLatex)
                                    onDismiss()
                                }
                                true
                            }
                            else -> {
                                val code = keyEvent.utf16CodePoint
                                if (code > 0) {
                                    val c = code.toChar()
                                    when (c) {
                                        '/' -> { insertText("/"); true }
                                        '^' -> { insertText("^"); true }
                                        else -> if (!c.isISOControl()) {
                                            insertText(c.toString())
                                            true
                                        } else false
                                    }
                                } else false
                            }
                        }
                    } else false
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── HEADER ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LernspaceSurfaceLow,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Functions, null, tint = LernspaceDark, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column {
                            Text("Editor Rumus Matematika", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Text("Gaya kalkulator GeoGebra visual", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { handleUndo() },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "↶",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (undoStack.isNotEmpty()) LernspaceDark else Color.LightGray
                            )
                        }
                        IconButton(
                            onClick = { handleRedo() },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "↷",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (redoStack.isNotEmpty()) LernspaceDark else Color.LightGray
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        TextButton(onClick = { handleClear() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                            Text("Hapus", color = Color(0xFFEF4444), fontSize = 13.sp)
                        }
                        TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("Batal", color = LernspaceTextSecondary, fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                if (currentLatex.isNotBlank()) {
                                    onInsertEquation(currentLatex)
                                    onDismiss()
                                }
                            },
                            enabled = currentLatex.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Sisipkan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // ── FORMULA DISPLAY STAGE (CLEAN MATH RENDERING) ──
                val canvasScrollState = rememberScrollState()

                LaunchedEffect(activeSeqId, cursorOffset, root) {
                    if (activeSeqId == root.id && cursorOffset >= root.items.size) {
                        canvasScrollState.animateScrollTo(canvasScrollState.maxValue)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.5.dp, LernspaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp, max = 160.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // Clicking empty canvas space focuses the end of the root sequence
                            activeSeqId = root.id
                            cursorOffset = root.items.size
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(canvasScrollState)
                        ) {
                            MathExprView(
                                expr = root,
                                activeSeqId = activeSeqId,
                                cursorOffset = cursorOffset,
                                cursorAlpha = cursorAlpha,
                                fontSize = 24.sp,
                                onSelectCursor = { seqId, offset ->
                                    activeSeqId = seqId
                                    cursorOffset = offset
                                }
                            )
                        }
                    }
                }

                // ── KEYBOARD TAB SELECTOR ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("123", "f(x)", "abc", "αβγ").forEachIndexed { idx, label ->
                        val isSelected = keyboardTab == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) LernspaceSurfaceCard else LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clickable { keyboardTab = idx }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) LernspaceDark else LernspaceTextSecondary
                                )
                            }
                        }
                    }
                }

                // ── DOCKED GEOGEBRA MATH KEYBOARD ──
                Box(modifier = Modifier.weight(1f)) {
                    when (keyboardTab) {
                        0 -> GeoGebraKeypad123(
                            onChar = { insertText(it) },
                            onStructure = { insertStructure(it) },
                            onFraction = { insertText("/") },
                            onPower = { insertText("^") },
                            onPowerSquare = { interceptPowerSquare() },
                            onBackspace = { handleBackspace() },
                            onNavLeft = { handleNavLeft() },
                            onNavRight = { handleNavRight() },
                            onEnter = {
                                if (currentLatex.isNotBlank()) {
                                    onInsertEquation(currentLatex)
                                    onDismiss()
                                }
                            }
                        )
                        1 -> GeoGebraKeypadFunctions(
                            onChar = { insertText(it) },
                            onFunction = { insertFunctionWithParens(it) },
                            onDerivative = { insertDerivativeWithParens() },
                            onStructure = { insertStructure(it) },
                            onFraction = { insertText("/") },
                            onBackspace = { handleBackspace() },
                            onNavLeft = { handleNavLeft() },
                            onNavRight = { handleNavRight() },
                            onEnter = {
                                if (currentLatex.isNotBlank()) {
                                    onInsertEquation(currentLatex)
                                    onDismiss()
                                }
                            }
                        )
                        2 -> GeoGebraKeypadAbc(
                            onChar = { insertText(it) },
                            onBackspace = { handleBackspace() },
                            onNavLeft = { handleNavLeft() },
                            onNavRight = { handleNavRight() },
                            onEnter = {
                                if (currentLatex.isNotBlank()) {
                                    onInsertEquation(currentLatex)
                                    onDismiss()
                                }
                            }
                        )
                        else -> GeoGebraKeypadGreek(
                            onChar = { insertText(it) },
                            onBackspace = { handleBackspace() },
                            onNavLeft = { handleNavLeft() },
                            onNavRight = { handleNavRight() },
                            onEnter = {
                                if (currentLatex.isNotBlank()) {
                                    onInsertEquation(currentLatex)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }

                // ── LATEX CODE FOOTER PREVIEW ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLatex.isBlank()) "Kode LaTeX: (kosong)" else "LaTeX: $$ $currentLatex $$",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LernspaceTextSecondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ── RECURSIVE VISUAL RENDERER (GEOGEBRA MATH ENGINE) ─────────────────────────
@Composable
fun MathExprView(
    expr: MathExpr,
    activeSeqId: String,
    cursorOffset: Int,
    cursorAlpha: Float,
    fontSize: TextUnit = 24.sp,
    textColor: Color = LernspaceDark,
    onSelectCursor: (seqId: String, offset: Int) -> Unit
) {
    val fracFs = (fontSize.value * 0.82f).coerceAtLeast(16f).sp
    val scriptFs = (fontSize.value * 0.70f).coerceAtLeast(13f).sp
    val tinyFs = (fontSize.value * 0.58f).coerceAtLeast(12f).sp

    when (expr) {
        is MathExpr.Sequence -> {
            MathSequenceView(
                seq = expr,
                activeSeqId = activeSeqId,
                cursorOffset = cursorOffset,
                cursorAlpha = cursorAlpha,
                fontSize = fontSize,
                textColor = textColor,
                onSelectCursor = onSelectCursor
            )
        }
        is MathExpr.Atom -> {
            AtomView(expr = expr, fontSize = fontSize, textColor = textColor)
        }
        is MathExpr.Fraction -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .defaultMinSize(minWidth = 28.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Numerator
                Box(
                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MathSequenceView(expr.num, activeSeqId, cursorOffset, cursorAlpha, fracFs, textColor, onSelectCursor)
                }
                // Fraction horizontal line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minWidth = 28.dp)
                        .height(2.dp)
                        .background(textColor)
                )
                // Denominator
                Box(
                    modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MathSequenceView(expr.den, activeSeqId, cursorOffset, cursorAlpha, fracFs, textColor, onSelectCursor)
                }
            }
        }
        is MathExpr.Power -> {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                // Base
                MathExprView(expr.base, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)

                // Exponent
                Box(
                    modifier = Modifier
                        .align(Alignment.Top)
                        .offset(y = -(fontSize.value * 0.10f).dp)
                        .padding(start = 1.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    MathSequenceView(expr.exp, activeSeqId, cursorOffset, cursorAlpha, scriptFs, textColor, onSelectCursor)
                }
            }
        }
        is MathExpr.Subscript -> {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                MathExprView(expr.base, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
                Box(
                    modifier = Modifier
                        .offset(y = (fontSize.value * 0.22f).dp)
                        .padding(start = 1.dp)
                ) {
                    MathSequenceView(expr.sub, activeSeqId, cursorOffset, cursorAlpha, scriptFs, textColor, onSelectCursor)
                }
            }
        }
        is MathExpr.SubSup -> {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                MathExprView(expr.base, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 1.dp)
                ) {
                    Box(modifier = Modifier.offset(y = -(fontSize.value * 0.10f).dp)) {
                        MathSequenceView(expr.exp, activeSeqId, cursorOffset, cursorAlpha, scriptFs, textColor, onSelectCursor)
                    }
                    Box(modifier = Modifier.offset(y = (fontSize.value * 0.18f).dp)) {
                        MathSequenceView(expr.sub, activeSeqId, cursorOffset, cursorAlpha, scriptFs, textColor, onSelectCursor)
                    }
                }
            }
        }
        is MathExpr.Integral -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                val hasBounds = expr.lower.items.isNotEmpty() || expr.upper.items.isNotEmpty() ||
                        activeSeqId == expr.lower.id || activeSeqId == expr.upper.id
                if (hasBounds) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.padding(bottom = 2.dp), contentAlignment = Alignment.Center) {
                            MathSequenceView(expr.upper, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                        }
                        Text(
                            "∫",
                            fontSize = (fontSize.value * 2.2f).sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Serif,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.padding(top = 2.dp), contentAlignment = Alignment.Center) {
                            MathSequenceView(expr.lower, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                        }
                    }
                } else {
                    Text(
                        "∫",
                        fontSize = (fontSize.value * 2.2f).sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Serif,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
                MathSequenceView(expr.integrand, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
                Spacer(Modifier.width(4.dp))
                Text(
                    "d",
                    fontSize = fontSize,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    color = textColor
                )
                MathSequenceView(expr.variable, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
            }
        }
        is MathExpr.Root -> {
            val radicalColor = textColor.copy(alpha = 0.9f)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (expr.degree != null) {
                    Box(modifier = Modifier.offset(x = 6.dp, y = -(fontSize.value * 0.35f).dp)) {
                        MathSequenceView(expr.degree, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                    }
                }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.drawBehind {
                            val strokeW = 1.8.dp.toPx()
                            val armW = fontSize.value.dp.toPx() * 0.35f
                            val diagW = fontSize.value.dp.toPx() * 0.7f
                            drawLine(
                                color = radicalColor,
                                start = Offset(strokeW, size.height * 0.58f),
                                end = Offset(armW, size.height - strokeW),
                                strokeWidth = strokeW
                            )
                            drawLine(
                                color = radicalColor,
                                start = Offset(armW, size.height - strokeW),
                                end = Offset(diagW, strokeW),
                                strokeWidth = strokeW
                            )
                            drawLine(
                                color = radicalColor,
                                start = Offset(diagW, strokeW),
                                end = Offset(size.width, strokeW),
                                strokeWidth = strokeW
                            )
                        }
                    ) {
                        Spacer(Modifier.width(fontSize.value.dp * 0.75f))
                        Box(modifier = Modifier.padding(top = 5.dp, end = 3.dp, bottom = 2.dp)) {
                            MathSequenceView(expr.radicand, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
                        }
                    }
                }
            }
        }
        is MathExpr.Summation -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MathSequenceView(expr.upper, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                    Text("∑", fontSize = (fontSize.value * 1.6f).sp, color = textColor, textAlign = TextAlign.Center)
                    MathSequenceView(expr.lower, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                }
                MathSequenceView(expr.body, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
            }
        }
        is MathExpr.Limit -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("lim", fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = textColor)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MathSequenceView(expr.variable, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                        Text("→", fontSize = tinyFs, color = textColor)
                        MathSequenceView(expr.target, activeSeqId, cursorOffset, cursorAlpha, tinyFs, textColor, onSelectCursor)
                    }
                }
                MathSequenceView(expr.body, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
            }
        }
        is MathExpr.Parens -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(expr.left, fontSize = (fontSize.value * 1.4f).sp, fontWeight = FontWeight.Light, color = textColor)
                MathSequenceView(expr.body, activeSeqId, cursorOffset, cursorAlpha, fontSize, textColor, onSelectCursor)
                Text(expr.right, fontSize = (fontSize.value * 1.4f).sp, fontWeight = FontWeight.Light, color = textColor)
            }
        }
    }
}

// ── HORIZONTAL SEQUENCE COMPOSABLE WITH INTER-CHARACTER BLINKING CURSOR ──────
@Composable
fun MathSequenceView(
    seq: MathExpr.Sequence,
    activeSeqId: String,
    cursorOffset: Int,
    cursorAlpha: Float,
    fontSize: TextUnit,
    textColor: Color = LernspaceDark,
    onSelectCursor: (seqId: String, offset: Int) -> Unit
) {
    val isActive = seq.id == activeSeqId

    if (seq.items.isEmpty()) {
        // Empty placeholder block
        MathEmptySlot(
            isActive = isActive,
            cursorAlpha = cursorAlpha,
            size = fontSize.value.dp,
            cursorColor = textColor,
            onClick = { onSelectCursor(seq.id, 0) }
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onSelectCursor(seq.id, seq.items.size)
            }
        ) {
            seq.items.forEachIndexed { index, item ->
                // Blinking cursor before item
                if (isActive && cursorOffset == index) {
                    BlinkingCursor(cursorAlpha = cursorAlpha, fontSize = fontSize, color = textColor)
                }

                Box(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onSelectCursor(seq.id, index + 1)
                    }
                ) {
                    MathExprView(
                        expr = item,
                        activeSeqId = activeSeqId,
                        cursorOffset = cursorOffset,
                        cursorAlpha = cursorAlpha,
                        fontSize = fontSize,
                        textColor = textColor,
                        onSelectCursor = onSelectCursor
                    )
                }
            }

            // Blinking cursor after last item
            if (isActive && cursorOffset >= seq.items.size) {
                BlinkingCursor(cursorAlpha = cursorAlpha, fontSize = fontSize, color = textColor)
            }
        }
    }
}

// ── CLEAN TYPOGRAPHY ATOM VIEW (NO ARTIFICIAL BACKGROUND/BORDERS) ─────────────
@Composable
fun AtomView(expr: MathExpr.Atom, fontSize: TextUnit, textColor: Color = LernspaceDark) {
    val text = expr.text
    val isSingleLetter = text.length == 1 && text.first().isLetter()
    val isBinaryOp = text in setOf("+", "-", "×", "÷", "=", "≠", "<", ">", "≤", "≥", "≈", "·", "→")
    val hPad = if (isBinaryOp) 3.dp else 0.dp
    Box(modifier = Modifier.padding(horizontal = hPad)) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Serif,
            fontStyle = if (isSingleLetter) FontStyle.Italic else FontStyle.Normal,
            color = textColor
        )
    }
}

// ── BLINKING CURSOR BAR ───────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlinkingCursor(cursorAlpha: Float, fontSize: TextUnit, color: Color = LernspaceDark) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(bringIntoViewRequester) {
        bringIntoViewRequester.bringIntoView()
    }
    Box(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .width(2.dp)
            .height((fontSize.value * 1.05f).dp)
            .background(color.copy(alpha = cursorAlpha))
    )
}

// ── GEOGEBRA-STYLE EMPTY PLACEHOLDER SLOT (NEUTRAL SHADING) ───────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MathEmptySlot(
    isActive: Boolean,
    cursorAlpha: Float,
    size: Dp,
    cursorColor: Color = LernspaceDark,
    onClick: () -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    if (isActive) {
        LaunchedEffect(bringIntoViewRequester) {
            bringIntoViewRequester.bringIntoView()
        }
    }
    val slotBg = if (isActive) Color(0xFFD1D5DB).copy(alpha = 0.9f) else Color(0xFFE5E7EB).copy(alpha = 0.85f)
    val slotBorder = if (isActive) cursorColor.copy(alpha = 0.6f) else Color.Transparent

    val slotW = (size.value * 0.65f).dp.coerceIn(10.dp, 18.dp)
    val slotH = (size.value * 1.0f).dp.coerceIn(14.dp, 26.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(if (isActive) Modifier.bringIntoViewRequester(bringIntoViewRequester) else Modifier)
            .width(slotW)
            .height(slotH)
            .clip(RoundedCornerShape(3.dp))
            .background(slotBg)
            .border(width = if (isActive) 1.5.dp else 0.dp, color = slotBorder, shape = RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(1.8.dp)
                    .height(slotH * 0.7f)
                    .background(cursorColor.copy(alpha = cursorAlpha))
            )
        }
    }
}

// ── REUSABLE KEYBOARD KEY ─────────────────────────────────────────────────────
@Composable
fun GeoGebraKey(
    text: String,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    isAction: Boolean = false,
    isNav: Boolean = false,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when {
        isPrimary -> AccentBlue
        isAction -> Color(0xFFEF4444).copy(alpha = 0.12f)
        isSpecial || isNav -> LernspaceSurfaceLow
        else -> LernspaceSurfaceCard
    }
    val contentColor = when {
        isPrimary -> Color.White
        isAction -> Color(0xFFEF4444)
        else -> LernspaceDark
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, if (isPrimary) AccentBlue else LernspaceBorder),
        shadowElevation = if (isSpecial || isNav || isPrimary) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = if (text.length > 3) 12.sp else 15.sp,
                fontWeight = if (isSpecial || isNav || isAction || isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = if (text.length == 1 && text.first().isLetter()) FontFamily.Serif else FontFamily.Default,
                fontStyle = if (text.length == 1 && text.first().isLetter()) FontStyle.Italic else FontStyle.Normal,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── KEYPAD TAB 123 (GEOGEBRA 2-ZONE KEYBOARD) ────────────────────────────────
@Composable
fun GeoGebraKeypad123(
    onChar: (String) -> Unit,
    onStructure: (() -> MathExpr) -> Unit,
    onFraction: () -> Unit,
    onPower: () -> Unit,
    onPowerSquare: () -> Unit,
    onBackspace: () -> Unit,
    onNavLeft: () -> Unit,
    onNavRight: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: x, y, z, π | spacer 0.2 | 7, 8, 9, ×, ÷
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("x", Modifier.weight(1f), isSpecial = true) { onChar("x") }
            GeoGebraKey("y", Modifier.weight(1f), isSpecial = true) { onChar("y") }
            GeoGebraKey("z", Modifier.weight(1f), isSpecial = true) { onChar("z") }
            GeoGebraKey("π", Modifier.weight(1f), isSpecial = true) { onChar("π") }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("7", Modifier.weight(1f)) { onChar("7") }
            GeoGebraKey("8", Modifier.weight(1f)) { onChar("8") }
            GeoGebraKey("9", Modifier.weight(1f)) { onChar("9") }
            GeoGebraKey("×", Modifier.weight(1f), isSpecial = true) { onChar("·") }
            GeoGebraKey("÷", Modifier.weight(1f), isSpecial = true) { onFraction() }
        }

        // Row 2: x², xⁿ, √x, e | spacer 0.2 | 4, 5, 6, +, -
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("x²", Modifier.weight(1f), isSpecial = true) { onPowerSquare() }
            GeoGebraKey("xⁿ", Modifier.weight(1f), isSpecial = true) { onPower() }
            GeoGebraKey("√x", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Root(null, MathExpr.Sequence()) }
            }
            GeoGebraKey("e", Modifier.weight(1f), isSpecial = true) { onChar("e") }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("4", Modifier.weight(1f)) { onChar("4") }
            GeoGebraKey("5", Modifier.weight(1f)) { onChar("5") }
            GeoGebraKey("6", Modifier.weight(1f)) { onChar("6") }
            GeoGebraKey("+", Modifier.weight(1f), isSpecial = true) { onChar("+") }
            GeoGebraKey("-", Modifier.weight(1f), isSpecial = true) { onChar("-") }
        }

        // Row 3: <, >, ≤, ≥ | spacer 0.2 | 1, 2, 3, =, ⌫
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("<", Modifier.weight(1f), isSpecial = true) { onChar("<") }
            GeoGebraKey(">", Modifier.weight(1f), isSpecial = true) { onChar(">") }
            GeoGebraKey("≤", Modifier.weight(1f), isSpecial = true) { onChar("≤") }
            GeoGebraKey("≥", Modifier.weight(1f), isSpecial = true) { onChar("≥") }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("1", Modifier.weight(1f)) { onChar("1") }
            GeoGebraKey("2", Modifier.weight(1f)) { onChar("2") }
            GeoGebraKey("3", Modifier.weight(1f)) { onChar("3") }
            GeoGebraKey("=", Modifier.weight(1f), isSpecial = true) { onChar("=") }
            GeoGebraKey("⌫", Modifier.weight(1f), isNav = true) { onBackspace() }
        }

        // Row 4: (, ), |x|, , | spacer 0.2 | 0, ., ◀, ▶, ↵
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("(", Modifier.weight(1f), isSpecial = true) { onChar("(") }
            GeoGebraKey(")", Modifier.weight(1f), isSpecial = true) { onChar(")") }
            GeoGebraKey("|x|", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Parens(MathExpr.Sequence(), "|", "|") }
            }
            GeoGebraKey(",", Modifier.weight(1f), isSpecial = true) { onChar(",") }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("0", Modifier.weight(1f)) { onChar("0") }
            GeoGebraKey(".", Modifier.weight(1f)) { onChar(".") }
            GeoGebraKey("◀", Modifier.weight(1f), isNav = true) { onNavLeft() }
            GeoGebraKey("▶", Modifier.weight(1f), isNav = true) { onNavRight() }
            GeoGebraKey("↵", Modifier.weight(1f), isPrimary = true) { onEnter() }
        }
    }
}

// ── KEYPAD TAB f(x) (FUNCTIONS & CALCULUS) ───────────────────────────────────
@Composable
fun GeoGebraKeypadFunctions(
    onChar: (String) -> Unit,
    onFunction: (String) -> Unit,
    onDerivative: () -> Unit,
    onStructure: (() -> MathExpr) -> Unit,
    onFraction: () -> Unit,
    onBackspace: () -> Unit,
    onNavLeft: () -> Unit,
    onNavRight: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: sin, cos, tan, ln, log | spacer 0.2 | 7, 8, 9, ⌫
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("sin", Modifier.weight(1f), isSpecial = true) { onFunction("sin") }
            GeoGebraKey("cos", Modifier.weight(1f), isSpecial = true) { onFunction("cos") }
            GeoGebraKey("tan", Modifier.weight(1f), isSpecial = true) { onFunction("tan") }
            GeoGebraKey("ln", Modifier.weight(1f), isSpecial = true) { onFunction("ln") }
            GeoGebraKey("log", Modifier.weight(1f), isSpecial = true) { onFunction("log") }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("7", Modifier.weight(1f)) { onChar("7") }
            GeoGebraKey("8", Modifier.weight(1f)) { onChar("8") }
            GeoGebraKey("9", Modifier.weight(1f)) { onChar("9") }
            GeoGebraKey("⌫", Modifier.weight(1f), isNav = true) { onBackspace() }
        }

        // Row 2: sin⁻¹, cos⁻¹, tan⁻¹, eˣ, 10ˣ | spacer 0.2 | 4, 5, 6, ×
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("sin⁻¹", Modifier.weight(1f), isSpecial = true) { onFunction("arcsin") }
            GeoGebraKey("cos⁻¹", Modifier.weight(1f), isSpecial = true) { onFunction("arccos") }
            GeoGebraKey("tan⁻¹", Modifier.weight(1f), isSpecial = true) { onFunction("arctan") }
            GeoGebraKey("eˣ", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Power(MathExpr.Atom("e"), MathExpr.Sequence()) }
            }
            GeoGebraKey("10ˣ", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Power(MathExpr.Atom("10"), MathExpr.Sequence()) }
            }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("4", Modifier.weight(1f)) { onChar("4") }
            GeoGebraKey("5", Modifier.weight(1f)) { onChar("5") }
            GeoGebraKey("6", Modifier.weight(1f)) { onChar("6") }
            GeoGebraKey("×", Modifier.weight(1f), isSpecial = true) { onChar("·") }
        }

        // Row 3: ∫ₐᵇ, ∫, d/dx, ∑, lim | spacer 0.2 | 1, 2, 3, -
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("∫ₐᵇ", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Integral(MathExpr.Sequence(), MathExpr.Sequence(), MathExpr.Sequence(), MathExpr.Sequence(listOf(MathExpr.Atom("x")))) }
            }
            GeoGebraKey("∫", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Integral(MathExpr.Sequence(), MathExpr.Sequence(), MathExpr.Sequence(), MathExpr.Sequence(listOf(MathExpr.Atom("x")))) }
            }
            GeoGebraKey("d/dx", Modifier.weight(1f), isSpecial = true) {
                onDerivative()
            }
            GeoGebraKey("∑", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Summation(MathExpr.Sequence(), MathExpr.Sequence(), MathExpr.Sequence()) }
            }
            GeoGebraKey("lim", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Limit(MathExpr.Sequence(listOf(MathExpr.Atom("x"))), MathExpr.Sequence(), MathExpr.Sequence()) }
            }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("1", Modifier.weight(1f)) { onChar("1") }
            GeoGebraKey("2", Modifier.weight(1f)) { onChar("2") }
            GeoGebraKey("3", Modifier.weight(1f)) { onChar("3") }
            GeoGebraKey("-", Modifier.weight(1f), isSpecial = true) { onChar("-") }
        }

        // Row 4: ≠, ≈, ∞, →, xₙ | spacer 0.2 | 0, ., ◀, ▶, ↵
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("≠", Modifier.weight(1f), isSpecial = true) { onChar("≠") }
            GeoGebraKey("≈", Modifier.weight(1f), isSpecial = true) { onChar("≈") }
            GeoGebraKey("∞", Modifier.weight(1f), isSpecial = true) { onChar("∞") }
            GeoGebraKey("→", Modifier.weight(1f), isSpecial = true) { onChar("→") }
            GeoGebraKey("xₙ", Modifier.weight(1f), isSpecial = true) {
                onStructure { MathExpr.Subscript(MathExpr.Atom("x"), MathExpr.Sequence()) }
            }
            Spacer(Modifier.weight(0.2f))
            GeoGebraKey("0", Modifier.weight(1f)) { onChar("0") }
            GeoGebraKey("◀", Modifier.weight(1f), isNav = true) { onNavLeft() }
            GeoGebraKey("▶", Modifier.weight(1f), isNav = true) { onNavRight() }
            GeoGebraKey("↵", Modifier.weight(1f), isPrimary = true) { onEnter() }
        }
    }
}

// ── KEYPAD TAB abc (ALPHABET KEYBOARD) ───────────────────────────────────────
@Composable
fun GeoGebraKeypadAbc(
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onNavLeft: () -> Unit,
    onNavRight: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // QWERTY Row 1: q w e r t y u i o p
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").forEach { letter ->
                GeoGebraKey(letter, Modifier.weight(1f)) { onChar(letter) }
            }
        }

        // QWERTY Row 2: a s d f g h j k l ⌫
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach { letter ->
                GeoGebraKey(letter, Modifier.weight(1f)) { onChar(letter) }
            }
            GeoGebraKey("⌫", Modifier.weight(1.2f), isNav = true) { onBackspace() }
        }

        // QWERTY Row 3: z x c v b n m , ◀ ▶
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("z", "x", "c", "v", "b", "n", "m", ",").forEach { letter ->
                GeoGebraKey(letter, Modifier.weight(1f)) { onChar(letter) }
            }
            GeoGebraKey("◀", Modifier.weight(1.1f), isNav = true) { onNavLeft() }
            GeoGebraKey("▶", Modifier.weight(1.1f), isNav = true) { onNavRight() }
        }

        // QWERTY Row 4: Space bar, =, ↵
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            GeoGebraKey("spasi", Modifier.weight(4.5f), isSpecial = true) { onChar(" ") }
            GeoGebraKey("=", Modifier.weight(1.5f), isSpecial = true) { onChar(" = ") }
            GeoGebraKey("↵", Modifier.weight(2f), isPrimary = true) { onEnter() }
        }
    }
}

// ── KEYPAD TAB αβγ (GREEK SYMBOLS) ───────────────────────────────────────────
@Composable
fun GeoGebraKeypadGreek(
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onNavLeft: () -> Unit,
    onNavRight: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("α" to "\\alpha ", "β" to "\\beta ", "γ" to "\\gamma ", "δ" to "\\delta ", "ε" to "\\epsilon ", "θ" to "\\theta ").forEach { (sym, lat) ->
                GeoGebraKey(sym, Modifier.weight(1f), isSpecial = true) { onChar(lat) }
            }
            GeoGebraKey("⌫", Modifier.weight(1.2f), isNav = true) { onBackspace() }
        }

        // Row 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("λ" to "\\lambda ", "μ" to "\\mu ", "π" to "\\pi ", "ρ" to "\\rho ", "σ" to "\\sigma ", "τ" to "\\tau ").forEach { (sym, lat) ->
                GeoGebraKey(sym, Modifier.weight(1f), isSpecial = true) { onChar(lat) }
            }
            GeoGebraKey("◀", Modifier.weight(1.2f), isNav = true) { onNavLeft() }
        }

        // Row 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("φ" to "\\phi ", "ψ" to "\\psi ", "ω" to "\\omega ", "Δ" to "\\Delta ", "Ω" to "\\Omega ", "Γ" to "\\Gamma ").forEach { (sym, lat) ->
                GeoGebraKey(sym, Modifier.weight(1f), isSpecial = true) { onChar(lat) }
            }
            GeoGebraKey("▶", Modifier.weight(1.2f), isNav = true) { onNavRight() }
        }

        // Row 4
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            listOf("∞" to "\\infty ", "∂" to "\\partial ", "∇" to "\\nabla ", "±" to "\\pm ", "·" to "\\cdot ").forEach { (sym, lat) ->
                GeoGebraKey(sym, Modifier.weight(1f), isSpecial = true) { onChar(lat) }
            }
            GeoGebraKey("↵", Modifier.weight(2f), isPrimary = true) { onEnter() }
        }
    }
}
