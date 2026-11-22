package com.lernspace.app.ui.workspace.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.workspace.document.GeoGebraKeypad123
import com.lernspace.app.ui.workspace.document.GeoGebraKeypadFunctions
import com.lernspace.app.ui.workspace.document.GeoGebraKeypadAbc
import com.lernspace.app.ui.workspace.document.GeoGebraKeypadGreek
import com.lernspace.app.ui.workspace.document.MathExpr
import com.lernspace.app.ui.workspace.document.MathExprView
import com.lernspace.app.ui.workspace.document.findSequence
import com.lernspace.app.ui.workspace.document.replaceSequence
import com.lernspace.app.ui.workspace.document.findContainerInfo
import com.lernspace.app.ui.workspace.document.latexToMathExpr
import com.lernspace.app.ui.workspace.document.toLatex
import com.lernspace.app.ui.workspace.document.EditorSnapshot
import com.lernspace.app.core.math.CircleData
import com.lernspace.app.core.math.CurvePlotDefinition
import com.lernspace.app.core.math.IntersectionPoint
import com.lernspace.app.core.math.KeyPoint
import com.lernspace.app.core.math.KeyPointType
import com.lernspace.app.core.math.MathPlotData
import com.lernspace.app.core.math.MathPlotHelper
import com.lernspace.app.core.math.MultiPlotCurve
import com.lernspace.app.core.math.RiemannType
import com.lernspace.app.core.math.DefiniteIntegralData
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * GeoGebra clean color palette for multi-graph plotting.
 */
private val GEOGEBRA_PALETTE = listOf(
    Color(0xFF2563EB), // Classic GeoGebra Royal Blue
    Color(0xFFDC2626), // Crimson Red
    Color(0xFF059669), // Emerald Green
    Color(0xFF7C3AED), // Violet
    Color(0xFFD97706), // Amber / Orange
    Color(0xFF0891B2)  // Cyan
)

/**
 * Represents a single plotted function, vertical line, or circle on the shared Cartesian plane.
 */
data class GraphPlotItem(
    val id: String = UUID.randomUUID().toString(),
    val rawEquation: String,
    val displayLabel: String,
    val color: Color,
    val isVisible: Boolean = true,
    val isCircle: Boolean = false,
    val circleData: CircleData? = null,
    val isVerticalLine: Boolean = false,
    val verticalLineX: Float = 0f,
    val cleanExpr: String = "",
    val keyPoints: List<KeyPoint> = emptyList(),
    val hasIntegral: Boolean = false,
    val integralLowerA: Float = 0f,
    val integralUpperB: Float = 2f,
    val integralRiemannType: RiemannType = RiemannType.SMOOTH,
    val integralPartitions: Int = 12,
    val isXFunctionOfY: Boolean = false,
    val isDualBranch: Boolean = false,
    val yRelationData: MathPlotHelper.YRelationData? = null
)

/**
 * Strips markdown code fences, surrounding $$ or $, and LaTeX math blocks.
 */
fun cleanEquationInput(raw: String): String {
    var text = raw.trim()
    if (text.startsWith("```latex")) text = text.removePrefix("```latex").trim()
    else if (text.startsWith("```tex")) text = text.removePrefix("```tex").trim()
    else if (text.startsWith("```")) text = text.removePrefix("```").trim()
    if (text.endsWith("```")) text = text.removeSuffix("```").trim()

    var changed = true
    while (changed) {
        val prev = text
        if (text.startsWith("$$") && text.endsWith("$$") && text.length >= 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("$") && text.endsWith("$") && text.length >= 2) {
            text = text.substring(1, text.length - 1).trim()
        } else if (text.startsWith("\\[") && text.endsWith("\\]") && text.length >= 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("\\(") && text.endsWith("\\)") && text.length >= 4) {
            text = text.substring(2, text.length - 2).trim()
        }
        changed = text != prev
    }

    return text.replace("·", "*").trim()
}

/**
 * High-Performance Floating GeoGebra-style Graph Window for Learnspace.
 *
 * Core Capabilities:
 * 1. Manual Equation Input: students can type any equation directly inside the window!
 * 2. Multi-Graph Plotting: multiple functions and circles displayed simultaneously on the SAME Cartesian plane.
 * 3. Continuous Freeform Resizing: drag bottom-right corner to freely adjust window dimensions.
 * 4. Zero Bleeding / Clipping Fix: canvas strictly clips all drawing within its viewport bounds.
 * 5. Minimalist Aesthetic: clean, academic GeoGebra palette with subtle lines and crisp keypoint dots.
 * 6. High-Resolution Snapshot: insert multi-graph composite bitmap into active note canvas.
 */
@Composable
fun FloatingGraphWindow(
    equationRaw: String = "",
    isDarkTheme: Boolean = false,
    onClose: () -> Unit,
    onInsertGraphToNote: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingGraphWindow(
        equationRaw = equationRaw,
        isDarkTheme = isDarkTheme,
        onClose = onClose,
        onInsertGraphToNote = { bmp, _ -> onInsertGraphToNote(bmp) },
        modifier = modifier
    )
}

/**
 * GeoGebra-style High-Performance Interactive 2D Function & Circle Floating Graph Window.
 *
 * Supports:
 * - Dynamic continuous resize handle at bottom-right corner.
 * - Multi-graph simultaneous rendering on the same Cartesian plane.
 * - Strict clipping so curves never bleed through header or borders.
 * - Empty initial state when opened without forcing any starter equation.
 * - Full deletion support for single / AI-generated equations.
 */
@Composable
fun FloatingGraphWindow(
    equationRaw: String = "",
    isDarkTheme: Boolean = false,
    onClose: () -> Unit,
    onInsertGraphToNote: (Bitmap, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware/gesture Back: close only the floating graph window, never the note!
    BackHandler {
        onClose()
    }

    // ── Multi-Graph Equation List State ──
    val graphItems = remember { mutableStateListOf<GraphPlotItem>() }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var editingItemId by remember { mutableStateOf<String?>(null) }
    var showMathKeypad by remember { mutableStateOf(false) }
    var keyboardTab by remember { mutableIntStateOf(0) }

    // ── Visual RetEX Math Model State ──
    var mathRoot by remember { mutableStateOf(MathExpr.Sequence()) }
    var mathActiveSeqId by remember { mutableStateOf(mathRoot.id) }
    var mathCursorOffset by remember { mutableIntStateOf(0) }
    val mathUndoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val mathRedoStack = remember { mutableStateListOf<EditorSnapshot>() }

    fun saveMathSnapshot() {
        if (mathUndoStack.size >= 50) {
            mathUndoStack.removeAt(0)
        }
        mathUndoStack.add(EditorSnapshot(mathRoot, mathActiveSeqId, mathCursorOffset))
        mathRedoStack.clear()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "graphMathCursorAnim")
    val mathCursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "graphMathCursorAlpha"
    )

    fun insertMathText(char: String) {
        saveMathSnapshot()
        val mappedChar = if (char == "·") "*" else char
        if (mappedChar == "/" || mappedChar == " / ") {
            val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
            val items = currentSeq.items.toMutableList()

            if (mathCursorOffset > 0) {
                val lastIdx = mathCursorOffset - 1
                val lastItem = items[lastIdx]

                if (lastItem is MathExpr.Parens) {
                    val numSeq = lastItem.body
                    val frac = MathExpr.Fraction(num = numSeq, den = MathExpr.Sequence(emptyList()))
                    items[lastIdx] = frac
                    mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                    mathActiveSeqId = frac.den.id
                    mathCursorOffset = 0
                    return
                }

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
                    mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = newItems)) as MathExpr.Sequence
                    mathActiveSeqId = frac.den.id
                    mathCursorOffset = 0
                    return
                }
            }

            val frac = MathExpr.Fraction(num = MathExpr.Sequence(emptyList()), den = MathExpr.Sequence(emptyList()))
            items.add(mathCursorOffset, frac)
            mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            mathActiveSeqId = frac.num.id
            mathCursorOffset = 0
            return
        }

        if (mappedChar == "^") {
            val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
            val items = currentSeq.items.toMutableList()

            if (mathCursorOffset > 0) {
                val lastIdx = mathCursorOffset - 1
                val lastItem = items[lastIdx]
                val pow = MathExpr.Power(base = lastItem, exp = MathExpr.Sequence(emptyList()))
                items[lastIdx] = pow
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = pow.exp.id
                mathCursorOffset = 0
                return
            }

            val pow = MathExpr.Power(base = MathExpr.Sequence(emptyList()), exp = MathExpr.Sequence(emptyList()))
            items.add(mathCursorOffset, pow)
            mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            mathActiveSeqId = pow.exp.id
            mathCursorOffset = 0
            return
        }

        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        val items = currentSeq.items.toMutableList()
        val newAtom = MathExpr.Atom(mappedChar)
        items.add(mathCursorOffset, newAtom)
        mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        mathCursorOffset++
    }

    fun interceptMathPowerSquare() {
        saveMathSnapshot()
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        val items = currentSeq.items.toMutableList()

        if (mathCursorOffset > 0) {
            val lastIdx = mathCursorOffset - 1
            val lastItem = items[lastIdx]
            val pow = MathExpr.Power(base = lastItem, exp = MathExpr.Sequence(listOf(MathExpr.Atom("2"))))
            items[lastIdx] = pow
            mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            mathCursorOffset = lastIdx + 1
            return
        }

        val pow = MathExpr.Power(base = MathExpr.Atom("x"), exp = MathExpr.Sequence(listOf(MathExpr.Atom("2"))))
        items.add(mathCursorOffset, pow)
        mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        mathCursorOffset = 1
    }

    fun insertMathFunctionWithParens(funcName: String) {
        saveMathSnapshot()
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        val items = currentSeq.items.toMutableList()
        val fnAtom = MathExpr.Atom(funcName)
        val parens = MathExpr.Parens(body = MathExpr.Sequence(emptyList()))
        items.add(mathCursorOffset, fnAtom)
        items.add(mathCursorOffset + 1, parens)
        mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        mathActiveSeqId = parens.body.id
        mathCursorOffset = 0
    }

    fun insertMathDerivativeWithParens() {
        saveMathSnapshot()
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        val items = currentSeq.items.toMutableList()
        val ddx = MathExpr.Fraction(
            num = MathExpr.Sequence(listOf(MathExpr.Atom("d"))),
            den = MathExpr.Sequence(listOf(MathExpr.Atom("d"), MathExpr.Atom("x")))
        )
        val parens = MathExpr.Parens(body = MathExpr.Sequence(emptyList()))
        items.add(mathCursorOffset, ddx)
        items.add(mathCursorOffset + 1, parens)
        mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        mathActiveSeqId = parens.body.id
        mathCursorOffset = 0
    }

    fun insertMathStructure(builder: () -> MathExpr) {
        saveMathSnapshot()
        val template = builder()
        if (template is MathExpr.Fraction) {
            insertMathText("/")
            return
        }
        if (template is MathExpr.Power) {
            insertMathText("^")
            return
        }

        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        val items = currentSeq.items.toMutableList()

        when (template) {
            is MathExpr.Root -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = template.degree?.id ?: template.radicand.id
                mathCursorOffset = 0
            }
            is MathExpr.Parens -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = template.body.id
                mathCursorOffset = 0
            }
            is MathExpr.Integral -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = if (template.upper.items.isEmpty()) template.upper.id else template.integrand.id
                mathCursorOffset = 0
            }
            is MathExpr.Summation -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = if (template.upper.items.isEmpty()) template.upper.id else template.body.id
                mathCursorOffset = 0
            }
            is MathExpr.Limit -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = if (template.target.items.isEmpty()) template.target.id else template.body.id
                mathCursorOffset = 0
            }
            else -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathCursorOffset++
            }
        }
    }

    fun handleMathNavLeft() {
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        if (mathCursorOffset > 0) {
            val prevItem = currentSeq.items[mathCursorOffset - 1]
            when (prevItem) {
                is MathExpr.Fraction -> {
                    mathActiveSeqId = prevItem.den.id
                    mathCursorOffset = prevItem.den.items.size
                }
                is MathExpr.Power -> {
                    mathActiveSeqId = prevItem.exp.id
                    mathCursorOffset = prevItem.exp.items.size
                }
                is MathExpr.Root -> {
                    mathActiveSeqId = prevItem.radicand.id
                    mathCursorOffset = prevItem.radicand.items.size
                }
                is MathExpr.Integral -> {
                    mathActiveSeqId = prevItem.variable.id
                    mathCursorOffset = prevItem.variable.items.size
                }
                is MathExpr.Summation -> {
                    mathActiveSeqId = prevItem.body.id
                    mathCursorOffset = prevItem.body.items.size
                }
                is MathExpr.Limit -> {
                    mathActiveSeqId = prevItem.body.id
                    mathCursorOffset = prevItem.body.items.size
                }
                is MathExpr.Parens -> {
                    mathActiveSeqId = prevItem.body.id
                    mathCursorOffset = prevItem.body.items.size
                }
                else -> {
                    mathCursorOffset--
                }
            }
        } else {
            val info = findContainerInfo(mathRoot, mathActiveSeqId)
            if (info != null) {
                when (info.container) {
                    is MathExpr.Power -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq
                    }
                    is MathExpr.Fraction -> {
                        if (mathActiveSeqId == info.container.den.id) {
                            mathActiveSeqId = info.container.num.id
                            mathCursorOffset = info.container.num.items.size
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Integral -> {
                        if (mathActiveSeqId == info.container.variable.id) {
                            mathActiveSeqId = info.container.integrand.id
                            mathCursorOffset = info.container.integrand.items.size
                        } else if (mathActiveSeqId == info.container.integrand.id) {
                            mathActiveSeqId = info.container.lower.id
                            mathCursorOffset = info.container.lower.items.size
                        } else if (mathActiveSeqId == info.container.lower.id) {
                            mathActiveSeqId = info.container.upper.id
                            mathCursorOffset = info.container.upper.items.size
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Summation -> {
                        if (mathActiveSeqId == info.container.body.id) {
                            mathActiveSeqId = info.container.lower.id
                            mathCursorOffset = info.container.lower.items.size
                        } else if (mathActiveSeqId == info.container.lower.id) {
                            mathActiveSeqId = info.container.upper.id
                            mathCursorOffset = info.container.upper.items.size
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Limit -> {
                        if (mathActiveSeqId == info.container.body.id) {
                            mathActiveSeqId = info.container.target.id
                            mathCursorOffset = info.container.target.items.size
                        } else if (mathActiveSeqId == info.container.target.id) {
                            mathActiveSeqId = info.container.variable.id
                            mathCursorOffset = info.container.variable.items.size
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Root -> {
                        if (info.container.degree != null && mathActiveSeqId == info.container.radicand.id) {
                            mathActiveSeqId = info.container.degree.id
                            mathCursorOffset = info.container.degree.items.size
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Parens -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq
                    }
                    else -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq
                    }
                }
            }
        }
    }

    fun handleMathNavRight() {
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        if (mathCursorOffset < currentSeq.items.size) {
            val nextItem = currentSeq.items[mathCursorOffset]
            when (nextItem) {
                is MathExpr.Fraction -> {
                    mathActiveSeqId = nextItem.num.id
                    mathCursorOffset = 0
                }
                is MathExpr.Power -> {
                    mathActiveSeqId = nextItem.exp.id
                    mathCursorOffset = 0
                }
                is MathExpr.Root -> {
                    if (nextItem.degree != null) {
                        mathActiveSeqId = nextItem.degree.id
                        mathCursorOffset = 0
                    } else {
                        mathActiveSeqId = nextItem.radicand.id
                        mathCursorOffset = 0
                    }
                }
                is MathExpr.Integral -> {
                    mathActiveSeqId = nextItem.upper.id
                    mathCursorOffset = 0
                }
                is MathExpr.Summation -> {
                    mathActiveSeqId = nextItem.upper.id
                    mathCursorOffset = 0
                }
                is MathExpr.Limit -> {
                    mathActiveSeqId = nextItem.variable.id
                    mathCursorOffset = 0
                }
                is MathExpr.Parens -> {
                    mathActiveSeqId = nextItem.body.id
                    mathCursorOffset = 0
                }
                else -> {
                    mathCursorOffset++
                }
            }
        } else {
            val info = findContainerInfo(mathRoot, mathActiveSeqId)
            if (info != null) {
                when (info.container) {
                    is MathExpr.Power -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq + 1
                    }
                    is MathExpr.Fraction -> {
                        if (mathActiveSeqId == info.container.num.id) {
                            mathActiveSeqId = info.container.den.id
                            mathCursorOffset = 0
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Integral -> {
                        if (mathActiveSeqId == info.container.upper.id) {
                            mathActiveSeqId = info.container.lower.id
                            mathCursorOffset = 0
                        } else if (mathActiveSeqId == info.container.lower.id) {
                            mathActiveSeqId = info.container.integrand.id
                            mathCursorOffset = 0
                        } else if (mathActiveSeqId == info.container.integrand.id) {
                            mathActiveSeqId = info.container.variable.id
                            mathCursorOffset = 0
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Summation -> {
                        if (mathActiveSeqId == info.container.upper.id) {
                            mathActiveSeqId = info.container.lower.id
                            mathCursorOffset = 0
                        } else if (mathActiveSeqId == info.container.lower.id) {
                            mathActiveSeqId = info.container.body.id
                            mathCursorOffset = 0
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Limit -> {
                        if (mathActiveSeqId == info.container.variable.id) {
                            mathActiveSeqId = info.container.target.id
                            mathCursorOffset = 0
                        } else if (mathActiveSeqId == info.container.target.id) {
                            mathActiveSeqId = info.container.body.id
                            mathCursorOffset = 0
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Root -> {
                        if (info.container.degree != null && mathActiveSeqId == info.container.degree.id) {
                            mathActiveSeqId = info.container.radicand.id
                            mathCursorOffset = 0
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Parens -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq + 1
                    }
                    else -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq + 1
                    }
                }
            }
        }
    }

    fun handleMathBackspace() {
        saveMathSnapshot()
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        if (mathCursorOffset > 0) {
            val items = currentSeq.items.toMutableList()
            val item = items[mathCursorOffset - 1]
            if (item is MathExpr.Atom) {
                items.removeAt(mathCursorOffset - 1)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathCursorOffset--
            } else if (item is MathExpr.Power) {
                if (item.exp.items.isEmpty()) {
                    items[mathCursorOffset - 1] = item.base
                    mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                } else {
                    mathActiveSeqId = item.exp.id
                    mathCursorOffset = item.exp.items.size
                }
            } else if (item is MathExpr.Fraction) {
                mathActiveSeqId = item.den.id
                mathCursorOffset = item.den.items.size
            } else {
                items.removeAt(mathCursorOffset - 1)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathCursorOffset--
            }
        } else {
            val info = findContainerInfo(mathRoot, mathActiveSeqId)
            if (info != null) {
                when (info.container) {
                    is MathExpr.Fraction -> {
                        if (mathActiveSeqId == info.container.den.id && info.container.den.items.isEmpty()) {
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems.removeAt(info.indexInParentSeq)
                            pItems.addAll(info.indexInParentSeq, info.container.num.items)
                            mathRoot = replaceSequence(mathRoot, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + info.container.num.items.size
                        } else if (mathActiveSeqId == info.container.num.id && info.container.num.items.isEmpty()) {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Power -> {
                        if (mathActiveSeqId == info.container.exp.id && info.container.exp.items.isEmpty()) {
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems[info.indexInParentSeq] = info.container.base
                            mathRoot = replaceSequence(mathRoot, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq + 1
                        }
                    }
                    is MathExpr.Root -> {
                        if (mathActiveSeqId == info.container.radicand.id && info.container.radicand.items.isEmpty()) {
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems.removeAt(info.indexInParentSeq)
                            mathRoot = replaceSequence(mathRoot, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Parens -> {
                        if (mathActiveSeqId == info.container.body.id && info.container.body.items.isEmpty()) {
                            val pItems = info.parentSeq.items.toMutableList()
                            pItems.removeAt(info.indexInParentSeq)
                            mathRoot = replaceSequence(mathRoot, info.parentSeq.id, info.parentSeq.copy(items = pItems)) as MathExpr.Sequence
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // ── Definite Integral State ──
    var showIntegral by remember { mutableStateOf(false) }
    var integralLowerA by remember { mutableFloatStateOf(0f) }
    var integralUpperB by remember { mutableFloatStateOf(2f) }
    var integralRiemannType by remember { mutableStateOf(RiemannType.SMOOTH) }
    var integralPartitions by remember { mutableFloatStateOf(10f) }
    var inputLowerAText by remember { mutableStateOf("0") }
    var inputUpperBText by remember { mutableStateOf("2") }

    LaunchedEffect(integralLowerA) {
        val str = MathPlotHelper.formatNumber(integralLowerA)
        if (inputLowerAText.toFloatOrNull() != integralLowerA) {
            inputLowerAText = str
        }
    }
    LaunchedEffect(integralUpperB) {
        val str = MathPlotHelper.formatNumber(integralUpperB)
        if (inputUpperBText.toFloatOrNull() != integralUpperB) {
            inputUpperBText = str
        }
    }

    // Helper to parse any raw input into a GraphPlotItem
    fun parseToItem(raw: String, color: Color): GraphPlotItem {
        val trimmed = cleanEquationInput(raw)

        val parsedInt = MathPlotHelper.parseDefiniteIntegral(trimmed)
        if (parsedInt != null) {
            val clean = MathPlotHelper.denormalizeImplicitMultiplication(parsedInt.functionExpr)
            val hasBounds = trimmed.contains("_") || trimmed.contains("[") || trimmed.contains("..") || trimmed.contains("^")
            val lowStr = MathPlotHelper.formatNumber(parsedInt.lowerBound)
            val upStr = MathPlotHelper.formatNumber(parsedInt.upperBound)
            val rawEq = if (trimmed.contains("\\int")) {
                MathPlotHelper.denormalizeImplicitMultiplication(trimmed)
            } else if (hasBounds) {
                "\\int_{$lowStr}^{$upStr} $clean \\, dx"
            } else {
                "\\int $clean \\, dx"
            }
            val dispLabel = if (hasBounds) {
                "∫_{$lowStr}^{$upStr} $clean dx"
            } else {
                "∫ $clean dx"
            }
            return GraphPlotItem(
                rawEquation = rawEq,
                displayLabel = dispLabel,
                color = color,
                isCircle = false,
                isVerticalLine = false,
                cleanExpr = clean,
                keyPoints = MathPlotHelper.findKeyPoints(clean, -50f, 50f),
                hasIntegral = true,
                integralLowerA = parsedInt.lowerBound,
                integralUpperB = parsedInt.upperBound
            )
        }

        val circle = MathPlotHelper.parseCircle(trimmed)
        if (circle != null) {
            val cleanCircle = MathPlotHelper.denormalizeImplicitMultiplication(circle.standardEquation)
            return GraphPlotItem(
                rawEquation = MathPlotHelper.denormalizeImplicitMultiplication(trimmed),
                displayLabel = cleanCircle,
                color = color,
                isCircle = true,
                circleData = circle,
                isVerticalLine = false,
                cleanExpr = cleanCircle,
                keyPoints = MathPlotHelper.getCircleKeyPoints(circle)
            )
        }

        val linear = MathPlotHelper.parseLinearEquation(trimmed)
        if (linear != null) {
            return if (linear.isVertical) {
                GraphPlotItem(
                    rawEquation = trimmed,
                    displayLabel = linear.standardEquation,
                    color = color,
                    isCircle = false,
                    isVerticalLine = true,
                    verticalLineX = linear.verticalX,
                    cleanExpr = linear.standardEquation,
                    keyPoints = listOf(
                        KeyPoint(
                            x = linear.verticalX,
                            y = 0f,
                            type = KeyPointType.X_INTERCEPT,
                            label = "Potong X",
                            detail = "(${MathPlotHelper.formatNumber(linear.verticalX)}, 0)"
                        )
                    )
                )
            } else {
                val cleanLinear = MathPlotHelper.denormalizeImplicitMultiplication(linear.functionExpr)
                val rawLinear = MathPlotHelper.denormalizeImplicitMultiplication(trimmed)
                GraphPlotItem(
                    rawEquation = rawLinear,
                    displayLabel = if (trimmed.contains("=")) rawLinear else linear.standardEquation,
                    color = color,
                    isCircle = false,
                    isVerticalLine = false,
                    cleanExpr = cleanLinear,
                    keyPoints = MathPlotHelper.findKeyPoints(cleanLinear, -50f, 50f)
                )
            }
        }

        val yRel = MathPlotHelper.parseYRelation(trimmed)
        if (yRel != null) {
            val rawEq = MathPlotHelper.denormalizeImplicitMultiplication(trimmed)
            return GraphPlotItem(
                rawEquation = rawEq,
                displayLabel = rawEq,
                color = color,
                isCircle = false,
                isVerticalLine = false,
                cleanExpr = if (yRel.isXFunctionOfY) yRel.xExprOfY else yRel.fxExprOfX,
                keyPoints = yRel.keyPoints,
                isXFunctionOfY = yRel.isXFunctionOfY,
                isDualBranch = yRel.isDualBranch,
                yRelationData = yRel
            )
        }

        val clean = MathPlotHelper.denormalizeImplicitMultiplication(MathPlotHelper.extractFunctionBody(trimmed))
        val rawClean = MathPlotHelper.denormalizeImplicitMultiplication(trimmed)
        return GraphPlotItem(
            rawEquation = rawClean,
            displayLabel = if (trimmed.contains("=") || trimmed.startsWith("f(x)")) rawClean else "y = $clean",
            color = color,
            isCircle = false,
            isVerticalLine = false,
            cleanExpr = clean,
            keyPoints = MathPlotHelper.findKeyPoints(clean, -50f, 50f)
        )
    }

    val saveEquation: () -> Unit = {
        val currentLatex = mathRoot.toLatex().trim()
        val cleanInput = cleanEquationInput(currentLatex)
        if (cleanInput.isNotBlank()) {
            val currentEditId = editingItemId
            if (currentEditId != null) {
                val idx = graphItems.indexOfFirst { it.id == currentEditId }
                if (idx != -1) {
                    val existing = graphItems[idx]
                    val updated = parseToItem(cleanInput, existing.color).copy(
                        id = existing.id,
                        isVisible = existing.isVisible
                    )
                    graphItems[idx] = updated
                }
                editingItemId = null
                mathRoot = MathExpr.Sequence()
                mathActiveSeqId = mathRoot.id
                mathCursorOffset = 0
            } else {
                val color = GEOGEBRA_PALETTE[graphItems.size % GEOGEBRA_PALETTE.size]
                val newItem = parseToItem(cleanInput, color)
                graphItems.add(newItem)
                selectedItemId = newItem.id
                mathRoot = MathExpr.Sequence()
                mathActiveSeqId = mathRoot.id
                mathCursorOffset = 0
            }
        }
    }

    val startEdit: (GraphPlotItem) -> Unit = { item ->
        editingItemId = item.id
        val parsed = latexToMathExpr(item.rawEquation)
        mathRoot = parsed
        val singleExpr = parsed.items.singleOrNull()
        if (singleExpr is MathExpr.Integral) {
            mathActiveSeqId = singleExpr.integrand.id
            mathCursorOffset = singleExpr.integrand.items.size
        } else {
            mathActiveSeqId = parsed.id
            mathCursorOffset = parsed.items.size
        }
        selectedItemId = item.id
        showMathKeypad = true
    }

    val cancelEdit: () -> Unit = {
        editingItemId = null
        mathRoot = MathExpr.Sequence()
        mathActiveSeqId = mathRoot.id
        mathCursorOffset = 0
    }

    // ── Parameter Sliders State (Ala Desmos) ──
    val paramValues = remember { mutableStateMapOf<Char, Float>() }
    val detectedParams = remember(graphItems.toList()) {
        graphItems.flatMap { item ->
            val target = if (item.cleanExpr.isNotBlank()) item.cleanExpr else item.rawEquation
            MathPlotHelper.extractParameters(target)
        }.distinct().sorted()
    }

    LaunchedEffect(detectedParams) {
        for (p in detectedParams) {
            if (!paramValues.containsKey(p)) {
                paramValues[p] = if (p in listOf('a', 'b', 'm')) 1f else if (p == 'r') 3f else 0f
            }
        }
    }

    // ── Trigonometric Pi-Grid Mode State ──
    var isPiGridMode by remember { mutableStateOf(false) }

    LaunchedEffect(graphItems.toList()) {
        if (graphItems.any { MathPlotHelper.isTrigonometric(it.rawEquation) }) {
            isPiGridMode = true
        }
    }

    fun getEffectiveExpr(item: GraphPlotItem): String {
        return if (detectedParams.isNotEmpty()) {
            MathPlotHelper.substituteParameters(item.cleanExpr, paramValues)
        } else {
            item.cleanExpr
        }
    }

    fun getEffectiveCircle(item: GraphPlotItem): CircleData? {
        if (!item.isCircle) return null
        if (detectedParams.isEmpty()) return item.circleData
        return MathPlotHelper.parseCircle(item.rawEquation, paramValues) ?: item.circleData
    }

    // ── Intersections State ──
    var showIntersections by remember { mutableStateOf(false) }

    val visibleCurvesDef = remember(graphItems.toList(), paramValues.toMap()) {
        graphItems.filter { it.isVisible }.map { item ->
            CurvePlotDefinition(
                id = item.id,
                cleanExpr = getEffectiveExpr(item),
                isCircle = item.isCircle,
                circleData = getEffectiveCircle(item),
                isVerticalLine = item.isVerticalLine,
                verticalLineX = item.verticalLineX
            )
        }
    }

    // ── Viewport & Coordinate Navigation State ──
    var centerWorldX by remember { mutableFloatStateOf(0f) }
    var centerWorldY by remember { mutableFloatStateOf(0f) }
    var worldSpanX by remember { mutableFloatStateOf(18f) }

    val intersections = remember(showIntersections, visibleCurvesDef) {
        if (showIntersections && visibleCurvesDef.size >= 2) {
            MathPlotHelper.findIntersections(
                visibleCurvesDef,
                -100f,
                100f
            )
        } else {
            emptyList()
        }
    }


    // ── Window Floating Position & Sizing State ──
    var isSidebarOpen by remember { mutableStateOf(true) }
    var windowOffsetX by remember { mutableFloatStateOf(50f) }
    var windowOffsetY by remember { mutableFloatStateOf(60f) }
    var windowWidthDp by remember { mutableFloatStateOf(620f) }
    var windowHeightDp by remember { mutableFloatStateOf(460f) }
    var savedCustomWidthDp by remember { mutableFloatStateOf(620f) }
    var savedCustomHeightDp by remember { mutableFloatStateOf(460f) }

    var isMinimized by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // Initialize equations list from initial equationRaw
    LaunchedEffect(equationRaw) {
        val targetEq = cleanEquationInput(equationRaw)
        if (targetEq.isNotBlank()) {
            val parsedInt = MathPlotHelper.parseDefiniteIntegral(targetEq)
            if (parsedInt != null) {
                showIntegral = true
                integralLowerA = parsedInt.lowerBound
                integralUpperB = parsedInt.upperBound
            }

            val existing = graphItems.firstOrNull { it.rawEquation == targetEq }
            if (existing != null) {
                selectedItemId = existing.id
            } else {
                val color = GEOGEBRA_PALETTE[graphItems.size % GEOGEBRA_PALETTE.size]
                val newItem = parseToItem(targetEq, color)
                graphItems.add(newItem)
                selectedItemId = newItem.id

                // Auto-center viewport on new equation
                if (newItem.circleData != null) {
                    centerWorldX = newItem.circleData.centerX
                    centerWorldY = newItem.circleData.centerY
                    worldSpanX = max(12f, newItem.circleData.radius * 2.8f)
                } else if (newItem.isVerticalLine) {
                    centerWorldX = newItem.verticalLineX
                    centerWorldY = 0f
                    worldSpanX = 18f
                } else {
                    val vertex = newItem.keyPoints.firstOrNull { it.type == KeyPointType.VERTEX_MIN || it.type == KeyPointType.VERTEX_MAX }
                    if (vertex != null) {
                        centerWorldX = vertex.x
                        centerWorldY = vertex.y
                        worldSpanX = max(16f, abs(vertex.x) * 2.8f)
                    }
                }
            }
        }
        // If equationRaw is blank, do NOT force default equation; start completely empty!
    }

    // Active item for properties and keypoints inspection
    val activeItem = remember(graphItems, selectedItemId) {
        graphItems.firstOrNull { it.id == selectedItemId } ?: graphItems.firstOrNull()
    }

    // Theme Palette (GeoGebra Minimalist)
    val bgColor = if (isDarkTheme) Color(0xFF111827) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkTheme) Color(0xFF1F2937) else Color(0xFFF8FAFC)
    val borderColor = if (isDarkTheme) Color(0xFF374151) else Color(0xFFE2E8F0)
    val gridColor = if (isDarkTheme) Color(0x2894A3B8) else Color(0x3094A3B8)
    val axisColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textPrimary = if (isDarkTheme) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val inputBg = if (isDarkTheme) Color(0xFF111827) else Color(0xFFFFFFFF)

    val density = LocalDensity.current.density

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentW = constraints.maxWidth.toFloat().coerceAtLeast(100f)
        val parentH = constraints.maxHeight.toFloat().coerceAtLeast(100f)
        val parentWDp = parentW / density
        val parentHDp = parentH / density

        // Automatically expand window height if needed when docked keypad opens
        LaunchedEffect(showMathKeypad) {
            if (showMathKeypad && windowHeightDp < 520f) {
                windowHeightDp = 520f.coerceAtMost(parentHDp - 24f)
            }
        }

        // Note: Floating window lets touches outside fall through to the canvas so user can pan/zoom note freely.

        // ── 1. Minimized Mode (Compact Floating Capsule) ──
        if (isMinimized) {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(windowOffsetX.roundToInt(), windowOffsetY.roundToInt()) }
                    .width(300.dp)
                    .height(48.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.2.dp, Color(0xFF2563EB), RoundedCornerShape(24.dp)),
                color = cardBg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                windowOffsetX = (windowOffsetX + dragAmount.x).coerceIn(12f, parentW - 310f)
                                windowOffsetY = (windowOffsetY + dragAmount.y).coerceIn(12f, parentH - 60f)
                            }
                        }
                        .clickable { isMinimized = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = "Geser",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = activeItem?.color ?: Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (graphItems.isEmpty()) "Grafik Kosong" else if (graphItems.size > 1) "${graphItems.size} Grafik Aktif" else MathPlotHelper.formatFormulaForDisplay(activeItem?.displayLabel ?: "Grafik"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { isMinimized = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.OpenInFull, contentDescription = "Buka", tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = textSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            return@BoxWithConstraints
        }

        // ── 2. Full Floating Window Mode (Continuous Dynamic Resizing) ──
        val currentW = if (isExpanded) (parentWDp - 24f).coerceAtLeast(340f) else windowWidthDp
        val currentH = if (isExpanded) (parentHDp - 36f).coerceAtLeast(280f) else windowHeightDp

        val insertCurrentToNote: () -> Unit = {
            val activeIntegrals = graphItems.filter { it.isVisible && it.hasIntegral && !it.isCircle && !it.isVerticalLine }.map { item ->
                val effExpr = getEffectiveExpr(item)
                val res = MathPlotHelper.calculateDefiniteIntegral(effExpr, item.integralLowerA, item.integralUpperB)
                DefiniteIntegralData(
                    functionExpr = effExpr,
                    displayLabel = item.displayLabel,
                    lowerBoundA = item.integralLowerA,
                    upperBoundB = item.integralUpperB,
                    integralValue = res.netIntegral,
                    totalArea = res.totalArea,
                    colorArgb = item.color.toArgb(),
                    riemannType = item.integralRiemannType,
                    partitionCount = item.integralPartitions
                )
            }

            // Compute optimal auto-framed bounds ensuring all vertices max/min, roots, and integral regions are fully visible
            val metaCurves = graphItems.filter { it.isVisible }.map { item ->
                val effExpr = getEffectiveExpr(item)
                MultiPlotCurve(
                    label = item.displayLabel,
                    colorArgb = item.color.toArgb(),
                    points = emptyList(),
                    circleData = getEffectiveCircle(item),
                    keyPoints = item.keyPoints,
                    isVerticalLine = item.isVerticalLine,
                    verticalLineX = item.verticalLineX
                )
            }
            val optimal = MathPlotHelper.calculateOptimalSnapshotBounds(
                curves = metaCurves,
                circleData = activeItem?.let { getEffectiveCircle(it) },
                definiteIntegrals = activeIntegrals,
                targetAspect = 1600f / 1200f
            )

            // Re-sample curves across optimal bounds with high resolution (500 steps)
            val visibleCurves = graphItems.filter { it.isVisible }.map { item ->
                val effExpr = getEffectiveExpr(item)
                val effCircle = getEffectiveCircle(item)
                MultiPlotCurve(
                    label = MathPlotHelper.formatFormulaForDisplay(
                        if (detectedParams.isNotEmpty()) {
                            "${item.displayLabel} (${MathPlotHelper.extractFunctionBody(effExpr)})"
                        } else {
                            item.displayLabel
                        }
                    ),
                    colorArgb = item.color.toArgb(),
                    points = when {
                        item.isCircle || item.isVerticalLine -> emptyList()
                        item.isXFunctionOfY && item.yRelationData != null -> MathPlotHelper.sampleXAsFunctionOfY(
                            item.yRelationData.xExprOfY,
                            optimal.yMin,
                            optimal.yMax,
                            steps = 500
                        )
                        else -> MathPlotHelper.sampleFunctionDynamic(
                            effExpr,
                            optimal.xMin,
                            optimal.xMax,
                            steps = 500,
                            keyPoints = item.keyPoints
                        )
                    },
                    circleData = effCircle,
                    keyPoints = item.keyPoints,
                    isVerticalLine = item.isVerticalLine,
                    verticalLineX = item.verticalLineX
                )
            }

            val plotData = MathPlotData(
                functionExpression = activeItem?.let { getEffectiveExpr(it) } ?: "",
                xMin = optimal.xMin,
                xMax = optimal.xMax,
                yMin = optimal.yMin,
                yMax = optimal.yMax,
                circleData = activeItem?.let { getEffectiveCircle(it) },
                multiCurves = visibleCurves,
                intersections = if (showIntersections) intersections else emptyList(),
                isVerticalLine = activeItem?.isVerticalLine ?: false,
                verticalLineX = activeItem?.verticalLineX ?: 0f,
                isPiGridMode = isPiGridMode,
                definiteIntegrals = activeIntegrals
            )

            val highResBmp = MathPlotHelper.renderGraphBitmap(
                plotData = plotData,
                width = 1600,
                height = 1200,
                isDarkTheme = isDarkTheme
            )

            val visibleEquations = mutableListOf<String>()
            for (intData in activeIntegrals) {
                visibleEquations.add(MathPlotHelper.formatDefiniteIntegralLatex(intData))
            }
            visibleEquations.addAll(graphItems.filter { it.isVisible }.map { it.rawEquation })

            onInsertGraphToNote(highResBmp, visibleEquations)
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(windowOffsetX.roundToInt(), windowOffsetY.roundToInt()) }
                .width(currentW.dp)
                .height(currentH.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .border(1.2.dp, borderColor, RoundedCornerShape(16.dp)),
            color = bgColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Header Bar: Title, Aljabar Toggle, Sisipkan & Window Actions ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val winPxW = currentW * density
                                    val winPxH = currentH * density
                                    windowOffsetX = (windowOffsetX + dragAmount.x).coerceIn(8f, max(8f, parentW - winPxW))
                                    windowOffsetY = (windowOffsetY + dragAmount.y).coerceIn(8f, max(8f, parentH - winPxH))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title, Drag Handle & Sidebar Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = "Tarik untuk memindahkan",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Grafik Kartesius",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textPrimary
                            )

                            // GeoGebra-style Algebra Sidebar Toggle Button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSidebarOpen) Color(0xFF2563EB).copy(alpha = 0.14f) else inputBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSidebarOpen) Color(0xFF2563EB) else borderColor
                                ),
                                modifier = Modifier.clickable { isSidebarOpen = !isSidebarOpen }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ShowChart,
                                        contentDescription = "Aljabar",
                                        tint = if (isSidebarOpen) Color(0xFF2563EB) else textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Aljabar",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSidebarOpen) Color(0xFF2563EB) else textSecondary
                                    )
                                }
                            }
                        }

                        // Action Controls: Single Sisipkan Button, Expand, Minimize, Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Single Unified Sisipkan button
                            Button(
                                onClick = { insertCurrentToNote() },
                                shape = RoundedCornerShape(7.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sisipkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Expand / Restore
                            IconButton(
                                onClick = {
                                    if (!isExpanded) {
                                        savedCustomWidthDp = windowWidthDp
                                        savedCustomHeightDp = windowHeightDp
                                        isExpanded = true
                                    } else {
                                        isExpanded = false
                                        windowWidthDp = savedCustomWidthWidthSafe(savedCustomWidthDp)
                                        windowHeightDp = savedCustomHeightDp
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = "Ukuran Layar",
                                    tint = textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Minimize
                            IconButton(
                                onClick = { isMinimized = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Kecilkan", tint = textSecondary, modifier = Modifier.size(17.dp))
                            }

                            // Close
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = textSecondary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    // ── Main Workspace Body: Left Sidebar + Right Canvas ──
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // ── Left Collapsible Sidebar (GeoGebra Algebra Pane) ──
                        AnimatedVisibility(
                            visible = isSidebarOpen,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .width(290.dp)
                                    .fillMaxHeight(),
                                color = cardBg,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Sidebar Header Tools (Title, Intersections toggle)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Persamaan (${graphItems.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textSecondary
                                        )

                                        if (visibleCurvesDef.size >= 2) {
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = if (showIntersections) Color(0xFF2563EB).copy(alpha = 0.15f) else inputBg,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    0.8.dp,
                                                    if (showIntersections) Color(0xFF2563EB) else borderColor
                                                ),
                                                modifier = Modifier.clickable { showIntersections = !showIntersections }
                                            ) {
                                                Text(
                                                    text = if (intersections.isNotEmpty()) "🎯 Titik Potong (${intersections.size})" else "🎯 Potong",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (showIntersections) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (showIntersections) Color(0xFF2563EB) else textSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // ── Top-Mounted Input Section & Math Keypad Strip ──
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = cardBg,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                            // Edit mode indicator banner
                                            if (editingItemId != null) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Edit,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "Edit Persamaan",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF2563EB)
                                                        )
                                                    }
                                                    Text(
                                                        text = "Batal",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFFDC2626),
                                                        modifier = Modifier.clickable { cancelEdit() }
                                                    )
                                                }
                                            }

                                            // Visual RetEX Math Input Row (Auto-scrollable formula stage)
                                            val mathCanvasScroll = rememberScrollState()
                                            LaunchedEffect(mathActiveSeqId, mathCursorOffset, mathRoot) {
                                                if (mathActiveSeqId == mathRoot.id && mathCursorOffset >= mathRoot.items.size) {
                                                    mathCanvasScroll.animateScrollTo(mathCanvasScroll.maxValue)
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 52.dp, max = 96.dp)
                                                    .background(inputBg, RoundedCornerShape(8.dp))
                                                    .border(
                                                        if (editingItemId != null) 1.5.dp else 1.dp,
                                                        if (editingItemId != null) Color(0xFF2563EB) else borderColor,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() }
                                                        ) {
                                                            mathActiveSeqId = mathRoot.id
                                                            mathCursorOffset = mathRoot.items.size
                                                            showMathKeypad = true
                                                        }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(mathCanvasScroll)
                                                    ) {
                                                        MathExprView(
                                                            expr = mathRoot,
                                                            activeSeqId = mathActiveSeqId,
                                                            cursorOffset = mathCursorOffset,
                                                            cursorAlpha = mathCursorAlpha,
                                                            fontSize = 15.sp,
                                                            textColor = textPrimary,
                                                            onSelectCursor = { seqId, offset ->
                                                                mathActiveSeqId = seqId
                                                                mathCursorOffset = offset
                                                                showMathKeypad = true
                                                            }
                                                        )
                                                    }
                                                }

                                                // Keypad toggle button (Clean Keyboard Vector Icon)
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = if (showMathKeypad) {
                                                        if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                                    } else inputBg,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.8.dp,
                                                        if (showMathKeypad) {
                                                            if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                                                        } else borderColor
                                                    ),
                                                    modifier = Modifier.clickable { showMathKeypad = !showMathKeypad }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Keyboard,
                                                        contentDescription = "Toggle Keypad Matematika",
                                                        tint = if (showMathKeypad) {
                                                            if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF1E293B)
                                                        } else textSecondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).size(15.dp)
                                                    )
                                                }

                                                // Action Button (Check for Save, Add for New)
                                                val hasContent = mathRoot.items.isNotEmpty()
                                                if (editingItemId != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = Color(0xFF059669),
                                                        modifier = Modifier.clickable(enabled = hasContent) {
                                                            saveEquation()
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.Check,
                                                                contentDescription = "Simpan",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = "Simpan",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = if (hasContent) Color(0xFF2563EB) else cardBg,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            0.8.dp,
                                                            if (hasContent) Color(0xFF2563EB) else borderColor
                                                        ),
                                                        modifier = Modifier.clickable(enabled = hasContent) {
                                                            saveEquation()
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Add,
                                                            contentDescription = "Tambah",
                                                            tint = if (hasContent) Color.White else textSecondary,
                                                            modifier = Modifier.padding(3.dp).size(14.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Quick Preset Chips for Easy Line Equations & Curves
                                            val linePresets = remember {
                                                listOf(
                                                    "y = 2x + 1" to "Garis",
                                                    "y = mx + c" to "Linear m",
                                                    "x = 3" to "Vertikal",
                                                    "y = -x + 4" to "Gradien -",
                                                    "y = x^2" to "Parabola",
                                                    "x^2 + y^2 = 25" to "Lingkaran",
                                                    "y = sin(x)" to "Sinus"
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                linePresets.forEach { (presetEq, label) ->
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = inputBg,
                                                        border = androidx.compose.foundation.BorderStroke(0.6.dp, borderColor),
                                                        modifier = Modifier.clickable {
                                                            val parsed = latexToMathExpr(presetEq)
                                                            mathRoot = parsed
                                                            mathActiveSeqId = parsed.id
                                                            mathCursorOffset = parsed.items.size
                                                        }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = textSecondary,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Scrollable Equations List with Per-Item Integral Controls
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (graphItems.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Belum ada persamaan.\nKetik persamaan di bawah.",
                                                    fontSize = 11.5.sp,
                                                    color = textSecondary.copy(alpha = 0.7f),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }

                                        // Parameter Sliders inside Sidebar if any exist
                                        if (detectedParams.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = inputBg,
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, borderColor),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = "🎛️ Parameter Slider",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    detectedParams.forEach { param ->
                                                        val curVal = paramValues[param] ?: if (param in listOf('a', 'b', 'm')) 1f else if (param == 'r') 3f else 0f
                                                        val minVal = if (param == 'r') 0.5f else -10f
                                                        val maxVal = if (param == 'r') 15f else 10f
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "$param = ${MathPlotHelper.formatNumber(curVal)}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = textPrimary
                                                            )
                                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = cardBg,
                                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                                                    modifier = Modifier.clickable {
                                                                        paramValues[param] = (curVal - 0.5f).coerceIn(minVal, maxVal)
                                                                    }
                                                                ) {
                                                                    Text("-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                                                }
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = cardBg,
                                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                                                    modifier = Modifier.clickable {
                                                                        paramValues[param] = (curVal + 0.5f).coerceIn(minVal, maxVal)
                                                                    }
                                                                ) {
                                                                    Text("+", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                                                }
                                                            }
                                                        }
                                                        androidx.compose.material3.Slider(
                                                            value = curVal,
                                                            onValueChange = { newVal ->
                                                                paramValues[param] = ((newVal * 10f).roundToInt() / 10f).coerceIn(minVal, maxVal)
                                                            },
                                                            valueRange = minVal..maxVal,
                                                            modifier = Modifier.height(24.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Function Cards List
                                        graphItems.forEachIndexed { index, item ->
                                            val isSelected = item.id == selectedItemId
                                            val isEditingThis = item.id == editingItemId
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isEditingThis) Color(0xFF2563EB).copy(alpha = 0.08f) else inputBg,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    if (isEditingThis) 1.8.dp else if (isSelected) 1.5.dp else 0.8.dp,
                                                    if (isEditingThis) Color(0xFF2563EB) else if (isSelected) item.color else borderColor
                                                ),
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    selectedItemId = item.id
                                                    startEdit(item)
                                                }
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    // Header of Card: Dot, Formula text, Integral Toggle & Delete
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            // Visibility Dot
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = if (item.isVisible) item.color else textSecondary.copy(alpha = 0.3f),
                                                                border = androidx.compose.foundation.BorderStroke(
                                                                    1.dp,
                                                                    if (item.isVisible) item.color else textSecondary
                                                                ),
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .clickable {
                                                                        graphItems[index] = item.copy(isVisible = !item.isVisible)
                                                                    }
                                                            ) {}

                                                            // Formula Visual Math Node & Badge (GeoGebra High-Fidelity Style)
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                val itemExpr = remember(item.rawEquation, item.displayLabel) {
                                                                    val source = if (item.rawEquation.isNotBlank()) item.rawEquation else item.displayLabel
                                                                    latexToMathExpr(source)
                                                                }
                                                                val isSimpleFunc = !item.rawEquation.contains("=") &&
                                                                        !item.hasIntegral && !item.isCircle && !item.isVerticalLine

                                                                if (itemExpr.items.isNotEmpty()) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                                                    ) {
                                                                        if (isSimpleFunc) {
                                                                            Text(
                                                                                text = "f(x) = ",
                                                                                fontSize = 13.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                fontFamily = FontFamily.Serif,
                                                                                fontStyle = FontStyle.Italic,
                                                                                color = if (item.isVisible) item.color else textSecondary
                                                                            )
                                                                        }
                                                                        MathExprView(
                                                                            expr = itemExpr,
                                                                            activeSeqId = "",
                                                                            cursorOffset = -1,
                                                                            cursorAlpha = 0f,
                                                                            fontSize = 13.sp,
                                                                            textColor = if (item.isVisible) item.color else textSecondary,
                                                                            onSelectCursor = { _, _ ->
                                                                                startEdit(item)
                                                                            }
                                                                        )
                                                                    }
                                                                } else {
                                                                    Text(
                                                                        text = MathPlotHelper.formatFormulaForDisplay(item.displayLabel),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = if (item.isVisible) item.color else textSecondary,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                if (isEditingThis) {
                                                                    Text(
                                                                        text = "✏️ Sedang diedit...",
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF2563EB),
                                                                        modifier = Modifier.padding(top = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Controls: Edit, Toggle Integral & Delete
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            // Edit ✏️
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = if (isEditingThis) Color(0xFF2563EB).copy(alpha = 0.15f) else cardBg,
                                                                border = androidx.compose.foundation.BorderStroke(
                                                                    0.8.dp,
                                                                    if (isEditingThis) Color(0xFF2563EB) else borderColor
                                                                ),
                                                                modifier = Modifier.clickable {
                                                                    if (isEditingThis) cancelEdit() else startEdit(item)
                                                                }
                                                            ) {
                                                                Icon(
                                                                    Icons.Filled.Edit,
                                                                    contentDescription = "Edit Persamaan",
                                                                    tint = if (isEditingThis) Color(0xFF2563EB) else textSecondary,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).size(13.dp)
                                                                )
                                                            }

                                                            // Delete ✕
                                                            Icon(
                                                                Icons.Filled.Close,
                                                                contentDescription = "Hapus",
                                                                tint = textSecondary,
                                                                modifier = Modifier
                                                                    .size(15.dp)
                                                                    .clickable {
                                                                        if (editingItemId == item.id) cancelEdit()
                                                                        graphItems.removeAt(index)
                                                                        if (selectedItemId == item.id) {
                                                                            selectedItemId = graphItems.firstOrNull()?.id
                                                                        }
                                                                    }
                                                            )
                                                        }
                                                    }

                                                    // ── Definite Integral Sub-Panel for this specific function ──
                                                    if (item.hasIntegral && !item.isCircle && !item.isVerticalLine) {
                                                        val effExpr = getEffectiveExpr(item)
                                                        val intResult = remember(effExpr, item.integralLowerA, item.integralUpperB) {
                                                            MathPlotHelper.calculateDefiniteIntegral(effExpr, item.integralLowerA, item.integralUpperB)
                                                        }

                                                        Spacer(Modifier.height(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = item.color.copy(alpha = 0.08f),
                                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, item.color.copy(alpha = 0.35f)),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.padding(6.dp)) {
                                                                // Evaluated result label
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "∫ f(x) dx",
                                                                        fontSize = 10.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = item.color
                                                                    )
                                                                    Text(
                                                                        text = "= ${MathPlotHelper.formatNumber(intResult.netIntegral.toFloat())}",
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = item.color
                                                                    )
                                                                }

                                                                Spacer(Modifier.height(4.dp))

                                                                // Bounds Row: a and b manual input + steppers
                                                                val isIntegralItem = item.rawEquation.contains("int") || item.rawEquation.contains("∫")
                                                                fun updateItemBounds(newA: Float, newB: Float) {
                                                                    val lowStr = MathPlotHelper.formatNumber(newA)
                                                                    val upStr = MathPlotHelper.formatNumber(newB)
                                                                    val cleanFunction = MathPlotHelper.denormalizeImplicitMultiplication(item.cleanExpr)
                                                                    val updatedRaw = if (isIntegralItem) "\\int_{$lowStr}^{$upStr} $cleanFunction \\, dx" else item.rawEquation
                                                                    val updatedDisp = if (isIntegralItem) "∫_{$lowStr}^{$upStr} $cleanFunction dx" else item.displayLabel
                                                                    graphItems[index] = item.copy(
                                                                        integralLowerA = newA,
                                                                        integralUpperB = newB,
                                                                        rawEquation = updatedRaw,
                                                                        displayLabel = updatedDisp
                                                                    )
                                                                }

                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    // Batas Bawah a
                                                                    var rawAText by remember(item.id, item.integralLowerA) {
                                                                        mutableStateOf(MathPlotHelper.formatNumber(item.integralLowerA))
                                                                    }
                                                                    Surface(
                                                                        shape = RoundedCornerShape(5.dp),
                                                                        color = cardBg,
                                                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, borderColor),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text("a=", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                                                            BasicTextField(
                                                                                value = rawAText,
                                                                                onValueChange = { newVal ->
                                                                                    val clean = newVal.trim()
                                                                                    if (clean.isEmpty() || clean == "-" || clean.matches(Regex("""^-?\d*\.?\d*$"""))) {
                                                                                        rawAText = clean
                                                                                        clean.toFloatOrNull()?.let { newA ->
                                                                                            updateItemBounds(newA, item.integralUpperB)
                                                                                        }
                                                                                    }
                                                                                },
                                                                                textStyle = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = FontFamily.Monospace),
                                                                                singleLine = true,
                                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                                                            )
                                                                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                                                Text(
                                                                                    "-",
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = textPrimary,
                                                                                    modifier = Modifier.clickable {
                                                                                        val newA = ((item.integralLowerA - 0.5f) * 10f).roundToInt() / 10f
                                                                                        rawAText = MathPlotHelper.formatNumber(newA)
                                                                                        updateItemBounds(newA, item.integralUpperB)
                                                                                    }.padding(horizontal = 3.dp)
                                                                                )
                                                                                Text(
                                                                                    "+",
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = textPrimary,
                                                                                    modifier = Modifier.clickable {
                                                                                        val newA = ((item.integralLowerA + 0.5f) * 10f).roundToInt() / 10f
                                                                                        rawAText = MathPlotHelper.formatNumber(newA)
                                                                                        updateItemBounds(newA, item.integralUpperB)
                                                                                    }.padding(horizontal = 3.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }

                                                                    // Batas Atas b
                                                                    var rawBText by remember(item.id, item.integralUpperB) {
                                                                        mutableStateOf(MathPlotHelper.formatNumber(item.integralUpperB))
                                                                    }
                                                                    Surface(
                                                                        shape = RoundedCornerShape(5.dp),
                                                                        color = cardBg,
                                                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, borderColor),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text("b=", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                                                            BasicTextField(
                                                                                value = rawBText,
                                                                                onValueChange = { newVal ->
                                                                                    val clean = newVal.trim()
                                                                                    if (clean.isEmpty() || clean == "-" || clean.matches(Regex("""^-?\d*\.?\d*$"""))) {
                                                                                        rawBText = clean
                                                                                        clean.toFloatOrNull()?.let { newB ->
                                                                                            updateItemBounds(item.integralLowerA, newB)
                                                                                        }
                                                                                    }
                                                                                },
                                                                                textStyle = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = FontFamily.Monospace),
                                                                                singleLine = true,
                                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                                                            )
                                                                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                                                Text(
                                                                                    "-",
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = textPrimary,
                                                                                    modifier = Modifier.clickable {
                                                                                        val newB = ((item.integralUpperB - 0.5f) * 10f).roundToInt() / 10f
                                                                                        rawBText = MathPlotHelper.formatNumber(newB)
                                                                                        updateItemBounds(item.integralLowerA, newB)
                                                                                    }.padding(horizontal = 3.dp)
                                                                                )
                                                                                Text(
                                                                                    "+",
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = textPrimary,
                                                                                    modifier = Modifier.clickable {
                                                                                        val newB = ((item.integralUpperB + 0.5f) * 10f).roundToInt() / 10f
                                                                                        rawBText = MathPlotHelper.formatNumber(newB)
                                                                                        updateItemBounds(item.integralLowerA, newB)
                                                                                    }.padding(horizontal = 3.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                // Quick Presets Row
                                                                Spacer(Modifier.height(3.dp))
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    listOf(
                                                                        Pair(0f, 1f),
                                                                        Pair(0f, 2f),
                                                                        Pair(-1f, 1f),
                                                                        Pair(-2f, 2f)
                                                                    ).forEach { (pa, pb) ->
                                                                        val isCur = abs(item.integralLowerA - pa) < 0.05f && abs(item.integralUpperB - pb) < 0.05f
                                                                        Surface(
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            color = if (isCur) item.color.copy(alpha = 0.25f) else cardBg,
                                                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                                                            modifier = Modifier.clickable {
                                                                                updateItemBounds(pa, pb)
                                                                            }
                                                                        ) {
                                                                            Text(
                                                                                text = "[${MathPlotHelper.formatNumber(pa)}, ${MathPlotHelper.formatNumber(pb)}]",
                                                                                fontSize = 9.sp,
                                                                                fontWeight = FontWeight.Medium,
                                                                                color = textPrimary,
                                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                // Riemann Type selector (Smooth vs Numerical)
                                                                Spacer(Modifier.height(3.dp))
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    RiemannType.values().forEach { rType ->
                                                                        val isSel = item.integralRiemannType == rType
                                                                        Surface(
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            color = if (isSel) item.color else cardBg,
                                                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isSel) item.color else borderColor),
                                                                            modifier = Modifier.clickable {
                                                                                graphItems[index] = item.copy(integralRiemannType = rType)
                                                                            }
                                                                        ) {
                                                                            Text(
                                                                                text = rType.label,
                                                                                fontSize = 8.5.sp,
                                                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                                                color = if (isSel) Color.White else textPrimary,
                                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Cartesian Graph Viewport Area (Strictly Clipped) ──
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clipToBounds() // CRITICAL: strictly clips viewport so graph lines NEVER bleed through header
                        ) {
                        val viewWidth = constraints.maxWidth.toFloat().coerceAtLeast(100f)
                        val viewHeight = constraints.maxHeight.toFloat().coerceAtLeast(100f)
                        val aspectRatio = (viewWidth / viewHeight).coerceAtLeast(0.1f)
                        val worldSpanY = worldSpanX / aspectRatio

                        val xMin = centerWorldX - worldSpanX / 2f
                        val xMax = centerWorldX + worldSpanX / 2f
                        val yMin = centerWorldY - worldSpanY / 2f
                        val yMax = centerWorldY + worldSpanY / 2f

                        fun toScreenX(wx: Float): Float = ((wx - xMin) / worldSpanX) * viewWidth
                        fun toScreenY(wy: Float): Float = viewHeight - ((wy - yMin) / worldSpanY) * viewHeight
                        fun toWorldX(sx: Float): Float = xMin + (sx / viewWidth) * worldSpanX
                        fun toWorldY(sy: Float): Float = yMin + ((viewHeight - sy) / viewHeight) * worldSpanY

                        // Adaptive Grid Step
                        val gridStep = remember(worldSpanX) { calculateNiceStep(worldSpanX) }
                        val gridStepX = remember(worldSpanX, isPiGridMode) {
                            if (isPiGridMode) MathPlotHelper.calculateNicePiStep(worldSpanX) else gridStep
                        }
                        val gridStepY = gridStep

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .pointerInput(viewWidth, viewHeight) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val currentSpanX = worldSpanX
                                        val currentSpanY = currentSpanX / aspectRatio

                                        // 1. Compute the world coordinate that was under (centroid - pan) before this frame
                                        val wxPrev = centerWorldX + ((centroid.x - pan.x) - viewWidth / 2f) * (currentSpanX / viewWidth)
                                        val wyPrev = centerWorldY - ((centroid.y - pan.y) - viewHeight / 2f) * (currentSpanY / viewHeight)

                                        // 2. Compute smooth new zoom span
                                        val newSpanX = (currentSpanX / zoom).coerceIn(0.2f, 250f)
                                        val newSpanY = newSpanX / aspectRatio

                                        // 3. Align new center so wxPrev and wyPrev stay exactly under current touch centroid
                                        centerWorldX = wxPrev - (centroid.x - viewWidth / 2f) * (newSpanX / viewWidth)
                                        centerWorldY = wyPrev + (centroid.y - viewHeight / 2f) * (newSpanY / viewHeight)
                                        worldSpanX = newSpanX
                                    }
                                }
                        ) {
                            // CRITICAL GUARANTEE: wrap drawing within clipRect(0, 0, size.width, size.height)
                            // This ensures NOT A SINGLE PIXEL of curves, circles, or labels can penetrate header or borders!
                            clipRect(0f, 0f, size.width, size.height) {
                                // 1. Grid Lines
                                val firstGridX = (floor(xMin / gridStepX) * gridStepX).toFloat()
                                var gx = firstGridX
                                while (gx <= xMax + gridStepX) {
                                    val sx = toScreenX(gx)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(sx, 0f),
                                        end = Offset(sx, viewHeight),
                                        strokeWidth = 1f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                    gx += gridStepX
                                }

                                val firstGridY = (floor(yMin / gridStepY) * gridStepY).toFloat()
                                var gy = firstGridY
                                while (gy <= yMax + gridStepY) {
                                    val sy = toScreenY(gy)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, sy),
                                        end = Offset(viewWidth, sy),
                                        strokeWidth = 1f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                    gy += gridStepY
                                }

                                // 2. Axes X and Y
                                val originX = toScreenX(0f).coerceIn(-100f, viewWidth + 100f)
                                val originY = toScreenY(0f).coerceIn(-100f, viewHeight + 100f)

                                // X-axis
                                drawLine(
                                    color = axisColor,
                                    start = Offset(0f, originY),
                                    end = Offset(viewWidth, originY),
                                    strokeWidth = 1.8f,
                                    cap = StrokeCap.Round
                                )
                                // Y-axis
                                drawLine(
                                    color = axisColor,
                                    start = Offset(originX, 0f),
                                    end = Offset(originX, viewHeight),
                                    strokeWidth = 1.8f,
                                    cap = StrokeCap.Round
                                )

                                // 3. Axis Numerical Labels
                                val nativeCanvas = drawContext.canvas.nativeCanvas
                                val tickTextPaint = android.graphics.Paint().apply {
                                    color = if (isDarkTheme) android.graphics.Color.rgb(156, 163, 175) else android.graphics.Color.rgb(100, 116, 139)
                                    textSize = 19f
                                    isAntiAlias = true
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }

                                var lx = firstGridX
                                while (lx <= xMax + gridStepX) {
                                    if (abs(lx) > 1e-4) {
                                        val sx = toScreenX(lx)
                                        val labelY = (originY + 22f).coerceIn(24f, viewHeight - 6f)
                                        val labelStr = if (isPiGridMode) MathPlotHelper.formatPiFraction(lx) else MathPlotHelper.formatNumber(lx)
                                        nativeCanvas.drawText(labelStr, sx, labelY, tickTextPaint)
                                    }
                                    lx += gridStepX
                                }

                                val yTextPaint = android.graphics.Paint(tickTextPaint).apply {
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                                var ly = firstGridY
                                while (ly <= yMax + gridStepY) {
                                    if (abs(ly) > 1e-4) {
                                        val sy = toScreenY(ly)
                                        val labelX = (originX - 7f).coerceIn(34f, viewWidth - 6f)
                                        nativeCanvas.drawText(MathPlotHelper.formatNumber(ly), labelX, sy + 6f, yTextPaint)
                                    }
                                    ly += gridStepY
                                }

                                // 3.5 Draw Definite Integral Shading for All Active Functions simultaneously on Canvas
                                val activeIntegralItems = graphItems.filter { it.isVisible && it.hasIntegral && !it.isCircle && !it.isVerticalLine }
                                for (intItem in activeIntegralItems) {
                                    val effExpr = getEffectiveExpr(intItem)
                                    val a = intItem.integralLowerA
                                    val b = intItem.integralUpperB
                                    val xStart = minOf(a, b)
                                    val xEnd = maxOf(a, b)
                                    val compiled = MathPlotHelper.compile(effExpr)

                                    if (compiled != null && xEnd - xStart > 1e-4f) {
                                        val intColor = intItem.color
                                        val shadeColor = intColor.copy(alpha = 0.28f)
                                        val syZero = toScreenY(0f)

                                        if (intItem.integralRiemannType == RiemannType.SMOOTH) {
                                            val shadePath = Path()
                                            shadePath.moveTo(toScreenX(xStart), syZero)
                                            val sampleSteps = 150
                                            val dx = (xEnd - xStart) / sampleSteps
                                            for (step in 0..sampleSteps) {
                                                val curX = xStart + step * dx
                                                val curY = compiled.eval(curX.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                                shadePath.lineTo(toScreenX(curX), toScreenY(curY))
                                            }
                                            shadePath.lineTo(toScreenX(xEnd), syZero)
                                            shadePath.close()
                                            drawPath(shadePath, color = shadeColor)
                                        } else {
                                            val n = intItem.integralPartitions.coerceIn(2, 60)
                                            val dx = (xEnd - xStart) / n
                                            for (i in 0 until n) {
                                                val x0 = xStart + i * dx
                                                val x1 = x0 + dx
                                                when (intItem.integralRiemannType) {
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
                                                        drawPath(trapPath, color = shadeColor)
                                                        drawPath(trapPath, color = intColor.copy(alpha = 0.5f), style = Stroke(width = 1.2f))
                                                    }
                                                    else -> {
                                                        val evalX = when (intItem.integralRiemannType) {
                                                            RiemannType.LEFT -> x0
                                                            RiemannType.RIGHT -> x1
                                                            else -> (x0 + x1) / 2f
                                                        }
                                                        val yH = compiled.eval(evalX.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                                        val sLeft = toScreenX(x0)
                                                        val sRight = toScreenX(x1)
                                                        val sTop = minOf(syZero, toScreenY(yH))
                                                        val sHeight = abs(syZero - toScreenY(yH))
                                                        drawRect(
                                                            color = shadeColor,
                                                            topLeft = Offset(sLeft, sTop),
                                                            size = androidx.compose.ui.geometry.Size(sRight - sLeft, sHeight)
                                                        )
                                                        drawRect(
                                                            color = intColor.copy(alpha = 0.5f),
                                                            topLeft = Offset(sLeft, sTop),
                                                            size = androidx.compose.ui.geometry.Size(sRight - sLeft, sHeight),
                                                            style = Stroke(width = 1.2f)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Dashed Boundary lines at a and b
                                        val ya = compiled.eval(a.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                        val yb = compiled.eval(b.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                        val sxa = toScreenX(a)
                                        val sxb = toScreenX(b)

                                        drawLine(
                                            color = intColor,
                                            start = Offset(sxa, syZero),
                                            end = Offset(sxa, toScreenY(ya)),
                                            strokeWidth = 2.2f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )
                                        drawLine(
                                            color = intColor,
                                            start = Offset(sxb, syZero),
                                            end = Offset(sxb, toScreenY(yb)),
                                            strokeWidth = 2.2f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )

                                        drawCircle(color = intColor, radius = 4.5f, center = Offset(sxa, toScreenY(ya)))
                                        drawCircle(color = Color.White, radius = 4.5f, center = Offset(sxa, toScreenY(ya)), style = Stroke(width = 1.5f))
                                        drawCircle(color = intColor, radius = 4.5f, center = Offset(sxb, toScreenY(yb)))
                                        drawCircle(color = Color.White, radius = 4.5f, center = Offset(sxb, toScreenY(yb)), style = Stroke(width = 1.5f))

                                        // GeoGebra-style evaluated label on canvas (e.g. ∫ = 1.83)
                                        val intRes = MathPlotHelper.calculateDefiniteIntegral(effExpr, a, b)
                                        val labelVal = "∫ = ${MathPlotHelper.formatNumber(intRes.netIntegral.toFloat())}"
                                        val valPaint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.rgb(
                                                (intColor.red * 255).toInt(),
                                                (intColor.green * 255).toInt(),
                                                (intColor.blue * 255).toInt()
                                            )
                                            textSize = 20f
                                            isFakeBoldText = true
                                            isAntiAlias = true
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                        val midX = (sxa + sxb) / 2f
                                        val midY = (toScreenY(0f) + toScreenY((ya + yb) / 2f)) / 2f
                                        nativeCanvas.drawText(labelVal, midX, midY.coerceIn(24f, viewHeight - 24f), valPaint)
                                    }
                                }

                                // 4. Draw All Visible Functions, Lines, and Circles Simultaneously on the Same Plane
                                for (item in graphItems) {
                                    if (!item.isVisible) continue

                                    val effExpr = getEffectiveExpr(item)
                                    val effCircle = getEffectiveCircle(item)

                                    if (item.isVerticalLine) {
                                        val sx = toScreenX(item.verticalLineX)
                                        if (sx in -10f..(viewWidth + 10f)) {
                                            drawLine(
                                                color = item.color,
                                                start = Offset(sx, 0f),
                                                end = Offset(sx, viewHeight),
                                                strokeWidth = 3.2f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    } else if (item.isCircle && effCircle != null) {
                                        val cData = effCircle
                                        val cxScreen = toScreenX(cData.centerX)
                                        val cyScreen = toScreenY(cData.centerY)
                                        val radiusPx = (cData.radius / worldSpanX) * viewWidth

                                        // Subtle clean circle fill
                                        drawCircle(
                                            color = item.color.copy(alpha = 0.08f),
                                            radius = radiusPx,
                                            center = Offset(cxScreen, cyScreen)
                                        )
                                        // Crisp boundary curve
                                        drawCircle(
                                            color = item.color,
                                            radius = radiusPx,
                                            center = Offset(cxScreen, cyScreen),
                                            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                                        )
                                        // Dashed radius indicator
                                        drawLine(
                                            color = item.color.copy(alpha = 0.6f),
                                            start = Offset(cxScreen, cyScreen),
                                            end = Offset(cxScreen + radiusPx, cyScreen),
                                            strokeWidth = 1.6f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )
                                    } else if (item.isXFunctionOfY && item.yRelationData != null) {
                                        // Horizontal parabola / relation x = f(y), e.g. y^2 = x + 1 => x = y^2 - 1
                                        val pts = MathPlotHelper.sampleXAsFunctionOfY(item.yRelationData.xExprOfY, yMin, yMax, steps = 300)
                                        val path = Path()
                                        var isFirst = true
                                        for (pt in pts) {
                                            val sx = toScreenX(pt.x)
                                            val sy = toScreenY(pt.y)
                                            if (sx.isFinite() && sx in -viewWidth..(viewWidth * 2)) {
                                                if (isFirst) {
                                                    path.moveTo(sx, sy)
                                                    isFirst = false
                                                } else {
                                                    path.lineTo(sx, sy)
                                                }
                                            } else {
                                                isFirst = true
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = item.color,
                                            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    } else if (item.isDualBranch && item.yRelationData != null) {
                                        // Dual branch y = k ± sqrt(f(x))
                                        val kVal = item.yRelationData.yOffsetK
                                        val topBody = if (kVal == 0f) "sqrt(${item.yRelationData.fxExprOfX})" else "($kVal) + sqrt(${item.yRelationData.fxExprOfX})"
                                        val botBody = if (kVal == 0f) "-sqrt(${item.yRelationData.fxExprOfX})" else "($kVal) - sqrt(${item.yRelationData.fxExprOfX})"
                                        val ptsTop = MathPlotHelper.sampleFunctionDynamic(topBody, xMin, xMax, steps = 300, keyPoints = item.keyPoints)
                                        val ptsBot = MathPlotHelper.sampleFunctionDynamic(botBody, xMin, xMax, steps = 300, keyPoints = item.keyPoints)
                                        val path = Path()
                                        var isFirst = true
                                        for (pt in ptsTop) {
                                            val sx = toScreenX(pt.x)
                                            val sy = toScreenY(pt.y)
                                            if (sy.isFinite() && sy in -viewHeight..(viewHeight * 2)) {
                                                if (isFirst) { path.moveTo(sx, sy); isFirst = false } else { path.lineTo(sx, sy) }
                                            } else { isFirst = true }
                                        }
                                        isFirst = true
                                        for (pt in ptsBot) {
                                            val sx = toScreenX(pt.x)
                                            val sy = toScreenY(pt.y)
                                            if (sy.isFinite() && sy in -viewHeight..(viewHeight * 2)) {
                                                if (isFirst) { path.moveTo(sx, sy); isFirst = false } else { path.lineTo(sx, sy) }
                                            } else { isFirst = true }
                                        }
                                        drawPath(
                                            path = path,
                                            color = item.color,
                                            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    } else {
                                        // Function curve
                                        val pts = MathPlotHelper.sampleFunctionDynamic(effExpr, xMin, xMax, steps = 300, keyPoints = item.keyPoints)
                                        val path = Path()
                                        var isFirst = true
                                        for (pt in pts) {
                                            val sx = toScreenX(pt.x)
                                            val sy = toScreenY(pt.y)
                                            if (sy.isFinite() && sy in -viewHeight..(viewHeight * 2)) {
                                                if (isFirst) {
                                                    path.moveTo(sx, sy)
                                                    isFirst = false
                                                } else {
                                                    path.lineTo(sx, sy)
                                                }
                                            } else {
                                                isFirst = true
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = item.color,
                                            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }

                                // 5. Draw Intersections (Titik Potong Antar-Grafik) when toggled
                                if (showIntersections && intersections.isNotEmpty()) {
                                    for (pt in intersections) {
                                        val sx = toScreenX(pt.x)
                                        val sy = toScreenY(pt.y)
                                        if (sx in -20f..(viewWidth + 20f) && sy in -20f..(viewHeight + 20f)) {
                                            // Subtle outer halo
                                            drawCircle(
                                                color = Color(0xFFD97706).copy(alpha = 0.25f),
                                                radius = 7.dp.toPx(),
                                                center = Offset(sx, sy)
                                            )
                                            // White crisp ring
                                            drawCircle(
                                                color = Color.White,
                                                radius = 4.dp.toPx(),
                                                center = Offset(sx, sy)
                                            )
                                            // Amber center dot
                                            drawCircle(
                                                color = Color(0xFFD97706),
                                                radius = 3.dp.toPx(),
                                                center = Offset(sx, sy)
                                            )

                                            // Coordinate label badge
                                            val labelStr = "(${MathPlotHelper.formatNumber(pt.x)}, ${MathPlotHelper.formatNumber(pt.y)})"
                                            val labelPaint = android.graphics.Paint().apply {
                                                color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.rgb(15, 23, 42)
                                                textSize = 19f
                                                isFakeBoldText = true
                                                isAntiAlias = true
                                                textAlign = android.graphics.Paint.Align.CENTER
                                            }
                                            val bgP = android.graphics.Paint().apply {
                                                color = if (isDarkTheme) android.graphics.Color.argb(220, 31, 41, 55) else android.graphics.Color.argb(220, 255, 255, 255)
                                                style = android.graphics.Paint.Style.FILL
                                            }
                                            val borderP = android.graphics.Paint().apply {
                                                color = android.graphics.Color.argb(140, 217, 119, 6)
                                                style = android.graphics.Paint.Style.STROKE
                                                strokeWidth = 1.5f
                                                isAntiAlias = true
                                            }

                                            val textW = labelPaint.measureText(labelStr)
                                            val padH = 6f
                                            val padV = 3f
                                            val bLeft = sx - textW / 2f - padH
                                            val bTop = sy - 26f - padV
                                            val bRight = sx + textW / 2f + padH
                                            val bBot = sy - 8f + padV

                                            nativeCanvas.drawRoundRect(bLeft, bTop, bRight, bBot, 6f, 6f, bgP)
                                            nativeCanvas.drawRoundRect(bLeft, bTop, bRight, bBot, 6f, 6f, borderP)
                                            nativeCanvas.drawText(labelStr, sx, sy - 12f, labelPaint)
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Navigation Toolbar (Bottom Right)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 28.dp, bottom = 8.dp), // spaced away from resize corner handle
                            shape = RoundedCornerShape(10.dp),
                            color = cardBg.copy(alpha = 0.94f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Zoom In
                                FilledTonalIconButton(
                                    onClick = { worldSpanX = (worldSpanX / 1.35f).coerceAtLeast(0.2f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Perbesar", modifier = Modifier.size(14.dp))
                                }

                                // Zoom Out
                                FilledTonalIconButton(
                                    onClick = { worldSpanX = (worldSpanX * 1.35f).coerceAtMost(250f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Perkecil", modifier = Modifier.size(14.dp))
                                }

                                // Toggle Pi-Grid Mode
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (isPiGridMode) Color(0xFF2563EB).copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (isPiGridMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB)) else null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { isPiGridMode = !isPiGridMode }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "π",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif,
                                            color = if (isPiGridMode) Color(0xFF2563EB) else textSecondary
                                        )
                                    }
                                }

                                // Reset Center (Home)
                                FilledTonalIconButton(
                                    onClick = {
                                        centerWorldX = 0f
                                        centerWorldY = 0f
                                        worldSpanX = 18f
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Home, contentDescription = "Pusat Awal", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                    // ── GeoGebra RetEX Full-Width Bottom-Docked Keypad ──
                    AnimatedVisibility(
                        visible = showMathKeypad,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            color = cardBg,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // ── Keypad Tab Header (123, f(x), abc, αβγ) + Close Keypad Action ──
                                val keyboardTabs = listOf("123", "f(x)", "abc", "αβγ")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    keyboardTabs.forEachIndexed { idx, label ->
                                        val isSelected = keyboardTab == idx
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) {
                                                if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                            } else inputBg,
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.8.dp,
                                                if (isSelected) Color(0xFF2563EB) else borderColor
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable { keyboardTab = idx }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) {
                                                        if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                                    } else textSecondary
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { showMathKeypad = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Tutup Keypad",
                                            tint = textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // ── Keypad Body: Tab Contents ──
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    when (keyboardTab) {
                                        0 -> GeoGebraKeypad123(
                                            onChar = { insertMathText(it) },
                                            onStructure = { insertMathStructure(it) },
                                            onFraction = { insertMathText("/") },
                                            onPower = { insertMathText("^") },
                                            onPowerSquare = { interceptMathPowerSquare() },
                                            onBackspace = { handleMathBackspace() },
                                            onNavLeft = { handleMathNavLeft() },
                                            onNavRight = { handleMathNavRight() },
                                            onEnter = { saveEquation() }
                                        )
                                        1 -> GeoGebraKeypadFunctions(
                                            onChar = { insertMathText(it) },
                                            onFunction = { insertMathFunctionWithParens(it) },
                                            onDerivative = { insertMathDerivativeWithParens() },
                                            onStructure = { insertMathStructure(it) },
                                            onFraction = { insertMathText("/") },
                                            onBackspace = { handleMathBackspace() },
                                            onNavLeft = { handleMathNavLeft() },
                                            onNavRight = { handleMathNavRight() },
                                            onEnter = { saveEquation() }
                                        )
                                        2 -> GeoGebraKeypadAbc(
                                            onChar = { insertMathText(it) },
                                            onBackspace = { handleMathBackspace() },
                                            onNavLeft = { handleMathNavLeft() },
                                            onNavRight = { handleMathNavRight() },
                                            onEnter = { saveEquation() }
                                        )
                                        else -> GeoGebraKeypadGreek(
                                            onChar = { insertMathText(it) },
                                            onBackspace = { handleMathBackspace() },
                                            onNavLeft = { handleMathNavLeft() },
                                            onNavRight = { handleMathNavRight() },
                                            onEnter = { saveEquation() }
                                        )
                                    }
                                }
                            }
                        }
                    }
            }

                // ── 3. Freeform Continuous Corner Drag-Resize Handle (Bottom-Right) ──
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dX = dragAmount.x / density
                                val dY = dragAmount.y / density
                                if (isExpanded) isExpanded = false
                                windowWidthDp = (windowWidthDp + dX).coerceIn(320f, parentWDp - 16f)
                                windowHeightDp = (windowHeightDp + dY).coerceIn(240f, parentHDp - 24f)
                            }
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val gripColor = if (isDarkTheme) Color(0xFF6B7280) else Color(0xFF94A3B8)
                        // Diagonal corner grip bars
                        drawLine(gripColor, Offset(w * 0.75f, h), Offset(w, h * 0.75f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                        drawLine(gripColor, Offset(w * 0.45f, h), Offset(w, h * 0.45f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                        drawLine(gripColor, Offset(w * 0.15f, h), Offset(w, h * 0.15f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

private fun savedCustomWidthWidthSafe(w: Float): Float = w.coerceAtLeast(320f)

/**
 * Calculates a standard "nice" step (1, 2, 5, 10, ...) for grid lines based on view range.
 */
private fun calculateNiceStep(range: Float): Float {
    val rawStep = (range / 8f).coerceAtLeast(0.001f)
    val exponent = floor(log10(rawStep.toDouble())).toInt()
    val fraction = rawStep / 10.0.pow(exponent)
    val niceFraction = when {
        fraction < 1.5 -> 1.0
        fraction < 3.5 -> 2.0
        fraction < 7.5 -> 5.0
        else -> 10.0
    }
    return (niceFraction * 10.0.pow(exponent)).toFloat()
}


