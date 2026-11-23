package com.lernspace.app.ui.math3d

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lernspace.app.core.math.MathPlotHelper
import com.lernspace.app.core.math3d.GeoElementModel
import com.lernspace.app.core.math3d.GeoElementType
import com.lernspace.app.core.math3d.loadElementsFromPrefs
import com.lernspace.app.core.math3d.saveElementsToPrefs
import com.lernspace.app.ui.math3d.components.GeoNixAlgebraView
import com.lernspace.app.ui.math3d.components.Math3DWebView
import com.lernspace.app.ui.math3d.components.Math3DWebViewController
import com.lernspace.app.ui.math3d.components.formatFormulaToLatex
import com.lernspace.app.ui.math3d.components.latexToJsExpr
import com.lernspace.app.ui.workspace.document.GeoNixKeypad123
import com.lernspace.app.ui.workspace.document.GeoNixKeypadFunctions
import com.lernspace.app.ui.workspace.document.GeoNixKeypadAbc
import com.lernspace.app.ui.workspace.document.GeoNixKeypadGreek
import com.lernspace.app.ui.workspace.document.MathExpr
import com.lernspace.app.ui.workspace.document.findContainerInfo
import com.lernspace.app.ui.workspace.document.findSequence
import com.lernspace.app.ui.workspace.document.latexToMathExpr
import com.lernspace.app.ui.workspace.document.replaceSequence
import com.lernspace.app.ui.workspace.document.toLatex
import kotlin.math.*

enum class CalculatorSuiteMode(val title: String) {
    GRAPHING("Grafik 2D"),
    CALCULATOR_3D("Kalkulator 3D"),
    GEOMETRY("Geometri"),
    CAS("CAS"),
    PROBABILITY("Probabilitas"),
    SCIENTIFIC("Ilmiah")
}

@Composable
fun Math3DLabScreen(
    onNavigateBack: () -> Unit = {},
    onInsertSnapshotToNote: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewController by remember { mutableStateOf<Math3DWebViewController?>(null) }

    // Suite Mode state: Default starts with permanent 3D Space (CALCULATOR_3D)
    var currentMode by remember { mutableStateOf(CalculatorSuiteMode.CALCULATOR_3D) }
    var showModeDropdown by remember { mutableStateOf(false) }

    // UI Panel Visibility & Settings
    var isSidebarVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCameraPresetsDialog by remember { mutableStateOf(false) }
    var isGridEnabled by remember { mutableStateOf(true) }
    var isAxesEnabled by remember { mutableStateOf(true) }
    var isDarkCanvas by remember { mutableStateOf(false) }

    // ── Native 2D Cartesian Engine State (Hardware accelerated 120 FPS, unlimited canvas) ──
    var centerWorldX by remember { mutableFloatStateOf(0f) }
    var centerWorldY by remember { mutableFloatStateOf(0f) }
    var worldSpanX by remember { mutableFloatStateOf(18f) }

    // ── Math RetEX Equation Input State (100% Mirroring Notes / FloatingGraphWindow) ──
    var mathRoot by remember { mutableStateOf(latexToMathExpr("")) }
    var mathActiveSeqId by remember { mutableStateOf(mathRoot.id) }
    var mathCursorOffset by remember { mutableIntStateOf(mathRoot.items.size) }
    var mathCursorAlpha by remember { mutableFloatStateOf(1f) }
    var showMathKeypad by remember { mutableStateOf(false) }
    var keyboardTab by remember { mutableIntStateOf(0) }
    var editingElement by remember { mutableStateOf<GeoElementModel?>(null) }

    // Blinking cursor timer
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            mathCursorAlpha = if (mathCursorAlpha > 0.5f) 0f else 1f
        }
    }

    // Undo / Redo history tracking
    val undoStack = remember { mutableStateListOf<List<GeoElementModel>>() }
    val redoStack = remember { mutableStateListOf<List<GeoElementModel>>() }

    // GeoNix Construction Elements: Loaded from last saved session, clean without dummy elements
    val savedElements = remember { loadElementsFromPrefs(context) }
    val elements = remember {
        mutableStateListOf<GeoElementModel>().apply {
            addAll(savedElements)
        }
    }

    fun syncWithEngine(saveHistory: Boolean = true) {
        if (saveHistory) {
            undoStack.add(elements.toList())
            redoStack.clear()
        }
        saveElementsToPrefs(context, elements.toList())
        webViewController?.setElements(elements.toList())
    }

    fun capture2DSnapshot(width: Int = 1600, height: Int = 1200): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val isDark = isDarkCanvas
        val bgColor = if (isDark) android.graphics.Color.rgb(20, 24, 33) else android.graphics.Color.WHITE
        canvas.drawColor(bgColor)

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val aspect = (viewW / viewH).coerceAtLeast(0.1f)
        val spanY = worldSpanX / aspect
        val xMin = centerWorldX - worldSpanX / 2f
        val xMax = centerWorldX + worldSpanX / 2f
        val yMin = centerWorldY - spanY / 2f
        val yMax = centerWorldY + spanY / 2f

        fun toSx(wx: Float): Float = ((wx - xMin) / worldSpanX) * viewW
        fun toSy(wy: Float): Float = viewH - ((wy - yMin) / spanY) * viewH

        val gridStep = calculateNiceStep(worldSpanX)
        val gridPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.argb(40, 255, 255, 255) else android.graphics.Color.argb(45, 0, 0, 0)
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
        }
        val axisPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.argb(180, 255, 255, 255) else android.graphics.Color.argb(190, 0, 0, 0)
            strokeWidth = 3.5f
            style = android.graphics.Paint.Style.STROKE
        }
        val textPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.rgb(180, 190, 205) else android.graphics.Color.rgb(70, 80, 95)
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        if (isGridEnabled) {
            var gx = (floor(xMin / gridStep) * gridStep).toFloat()
            while (gx <= xMax + gridStep) {
                val sx = toSx(gx)
                canvas.drawLine(sx, 0f, sx, viewH, gridPaint)
                gx += gridStep
            }
            var gy = (floor(yMin / gridStep) * gridStep).toFloat()
            while (gy <= yMax + gridStep) {
                val sy = toSy(gy)
                canvas.drawLine(0f, sy, viewW, sy, gridPaint)
                gy += gridStep
            }
        }

        if (isAxesEnabled) {
            val ox = toSx(0f).coerceIn(-100f, viewW + 100f)
            val oy = toSy(0f).coerceIn(-100f, viewH + 100f)
            canvas.drawLine(0f, oy, viewW, oy, axisPaint)
            canvas.drawLine(ox, 0f, ox, viewH, axisPaint)

            var lx = (floor(xMin / gridStep) * gridStep).toFloat()
            while (lx <= xMax + gridStep) {
                if (abs(lx) > 1e-4) {
                    val sx = toSx(lx)
                    val labelY = (oy + 38f).coerceIn(40f, viewH - 12f)
                    canvas.drawText(MathPlotHelper.formatNumber(lx), sx, labelY, textPaint)
                }
                lx += gridStep
            }

            val yPaint = android.graphics.Paint(textPaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
            var ly = (floor(yMin / gridStep) * gridStep).toFloat()
            while (ly <= yMax + gridStep) {
                if (abs(ly) > 1e-4) {
                    val sy = toSy(ly)
                    val labelX = (ox - 14f).coerceIn(60f, viewW - 12f)
                    canvas.drawText(MathPlotHelper.formatNumber(ly), labelX, sy + 10f, yPaint)
                }
                ly += gridStep
            }
        }

        for (elem in elements) {
            if (!elem.visible || elem.type != GeoElementType.FUNCTION_2D) continue
            val raw = if (elem.expr.isNotBlank()) elem.expr else elem.definition
            val clean = cleanEquationForEval(raw)
            val pts = MathPlotHelper.sampleFunctionDynamic(clean, xMin, xMax, steps = 500)
            if (pts.isNotEmpty()) {
                val c = try { android.graphics.Color.parseColor(elem.color) } catch (_: Exception) { android.graphics.Color.BLUE }
                val cPaint = android.graphics.Paint().apply {
                    color = c
                    strokeWidth = 3.2f
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
                val path = android.graphics.Path()
                var first = true
                for (p in pts) {
                    val sx = toSx(p.x)
                    val sy = toSy(p.y)
                    if (sy.isFinite() && sy in -viewH..(viewH * 2)) {
                        if (first) { path.moveTo(sx, sy); first = false } else { path.lineTo(sx, sy) }
                    } else {
                        first = true
                    }
                }
                canvas.drawPath(path, cPaint)

                val isImplicitY2 = raw.contains("y^2") || raw.contains("y^{2}") || raw.contains("y²") || raw.contains("y*y")
                if (isImplicitY2) {
                    val negPath = android.graphics.Path()
                    var firstNeg = true
                    for (p in pts) {
                        val sx = toSx(p.x)
                        val sy = toSy(-p.y)
                        if (sy.isFinite() && sy in -viewH..(viewH * 2)) {
                            if (firstNeg) { negPath.moveTo(sx, sy); firstNeg = false } else { negPath.lineTo(sx, sy) }
                        } else {
                            firstNeg = true
                        }
                    }
                    canvas.drawPath(negPath, cPaint)
                }
            }
        }

        return bmp
    }

    // ── Math RetEX Input Manipulations (Fractions, Powers, Roots, Text) ──
    fun insertMathText(char: String) {
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
        }

        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        val items = currentSeq.items.toMutableList()
        val atom = MathExpr.Atom(mappedChar)
        items.add(mathCursorOffset, atom)
        mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
        mathCursorOffset++
    }

    fun insertMathStructure(builder: () -> MathExpr) {
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
                mathActiveSeqId = if (template.isDefinite) template.lower.id else template.integrand.id
                mathCursorOffset = 0
            }
            is MathExpr.Summation -> {
                items.add(mathCursorOffset, template)
                mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                mathActiveSeqId = if (template.lower.items.isEmpty()) template.lower.id else template.body.id
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

    fun insertMathFunctionWithParens(funcName: String) {
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

    fun interceptMathPowerSquare() {
        val currentSeq = findSequence(mathRoot, mathActiveSeqId) ?: return
        if (mathCursorOffset > 0) {
            val items = currentSeq.items.toMutableList()
            val lastIdx = mathCursorOffset - 1
            val lastItem = items[lastIdx]
            val pow = MathExpr.Power(base = lastItem, exp = MathExpr.Sequence(listOf(MathExpr.Atom("2"))))
            items[lastIdx] = pow
            mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
            mathCursorOffset = lastIdx + 1
        }
    }

    fun handleMathBackspace() {
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
            } else if (item is MathExpr.Root) {
                if (item.radicand.items.isEmpty()) {
                    items.removeAt(mathCursorOffset - 1)
                    mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                    mathCursorOffset--
                } else {
                    mathActiveSeqId = item.radicand.id
                    mathCursorOffset = item.radicand.items.size
                }
            } else if (item is MathExpr.Parens) {
                if (item.body.items.isEmpty()) {
                    items.removeAt(mathCursorOffset - 1)
                    mathRoot = replaceSequence(mathRoot, mathActiveSeqId, currentSeq.copy(items = items)) as MathExpr.Sequence
                    mathCursorOffset--
                } else {
                    mathActiveSeqId = item.body.id
                    mathCursorOffset = item.body.items.size
                }
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
                is MathExpr.Parens -> {
                    mathActiveSeqId = prevItem.body.id
                    mathCursorOffset = prevItem.body.items.size
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
                    is MathExpr.Integral -> {
                        if (mathActiveSeqId == info.container.variable.id) {
                            mathActiveSeqId = info.container.integrand.id
                            mathCursorOffset = info.container.integrand.items.size
                        } else if (mathActiveSeqId == info.container.integrand.id) {
                            mathActiveSeqId = if (info.container.isDefinite) info.container.upper.id else info.parentSeq.id
                            mathCursorOffset = if (info.container.isDefinite) info.container.upper.items.size else info.indexInParentSeq
                        } else if (mathActiveSeqId == info.container.upper.id) {
                            mathActiveSeqId = info.container.lower.id
                            mathCursorOffset = info.container.lower.items.size
                        } else {
                            mathActiveSeqId = info.parentSeq.id
                            mathCursorOffset = info.indexInParentSeq
                        }
                    }
                    is MathExpr.Summation -> {
                        if (mathActiveSeqId == info.container.body.id) {
                            mathActiveSeqId = info.container.upper.id
                            mathCursorOffset = info.container.upper.items.size
                        } else if (mathActiveSeqId == info.container.upper.id) {
                            mathActiveSeqId = info.container.lower.id
                            mathCursorOffset = info.container.lower.items.size
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
                is MathExpr.Parens -> {
                    mathActiveSeqId = nextItem.body.id
                    mathCursorOffset = 0
                }
                is MathExpr.Integral -> {
                    mathActiveSeqId = if (nextItem.isDefinite) nextItem.lower.id else nextItem.integrand.id
                    mathCursorOffset = 0
                }
                is MathExpr.Summation -> {
                    mathActiveSeqId = nextItem.lower.id
                    mathCursorOffset = 0
                }
                is MathExpr.Limit -> {
                    mathActiveSeqId = nextItem.variable.id
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
                    is MathExpr.Integral -> {
                        if (mathActiveSeqId == info.container.lower.id) {
                            mathActiveSeqId = info.container.upper.id
                            mathCursorOffset = 0
                        } else if (mathActiveSeqId == info.container.upper.id) {
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
                        if (mathActiveSeqId == info.container.lower.id) {
                            mathActiveSeqId = info.container.upper.id
                            mathCursorOffset = 0
                        } else if (mathActiveSeqId == info.container.upper.id) {
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
                    else -> {
                        mathActiveSeqId = info.parentSeq.id
                        mathCursorOffset = info.indexInParentSeq + 1
                    }
                }
            }
        }
    }

    // Save or Add equation from input box
    fun saveEquation() {
        val latex = mathRoot.toLatex().trim()
        if (latex.isBlank()) return

        val parsedDefInt = MathPlotHelper.parseDefiniteIntegral(latex)
        val hasDefInt = parsedDefInt != null
        val targetExpr = if (hasDefInt) parsedDefInt!!.functionExpr else latex
        val jsExpr = latexToJsExpr(targetExpr)

        if (editingElement != null) {
            val targetId = editingElement!!.id
            val idx = elements.indexOfFirst { it.id == targetId }
            if (idx != -1) {
                val cur = elements[idx]
                if (cur.type == GeoElementType.SOLID_OF_REVOLUTION) {
                    elements[idx] = cur.copy(
                        definition = "Putar $targetExpr [${cur.a.toInt()}, ${cur.b.toInt()}] ${cur.angle.toInt()}° Sumbu-${cur.axis.uppercase()}",
                        expr = jsExpr
                    )
                } else {
                    elements[idx] = cur.copy(
                        definition = latex,
                        expr = jsExpr,
                        hasIntegral = if (hasDefInt) true else cur.hasIntegral,
                        a = if (hasDefInt) parsedDefInt!!.lowerBound else cur.a,
                        b = if (hasDefInt) parsedDefInt!!.upperBound else cur.b
                    )
                }
                syncWithEngine()
            }
            editingElement = null
            mathRoot = latexToMathExpr("")
            mathCursorOffset = 0
        } else {
            val palette = listOf("#2563EB", "#059669", "#D97706", "#DC2626", "#7C3AED", "#DB2777")
            val nextColor = palette[elements.size % palette.size]
            val newName = "f_${elements.count { it.type == GeoElementType.FUNCTION_2D } + 1}"
            val newElem = GeoElementModel(
                type = GeoElementType.FUNCTION_2D,
                name = newName,
                definition = latex,
                expr = jsExpr,
                color = nextColor,
                visible = true,
                hasIntegral = hasDefInt,
                a = if (hasDefInt) parsedDefInt!!.lowerBound else 0f,
                b = if (hasDefInt) parsedDefInt!!.upperBound else 2f
            )
            elements.add(newElem)
            syncWithEngine()
            mathRoot = latexToMathExpr("")
            mathCursorOffset = 0
        }
    }

    // ── Mode Switching Logic (2D Cartesian vs 3D) ──
    LaunchedEffect(currentMode, webViewController) {
        val controller = webViewController ?: return@LaunchedEffect
        controller.setMode("3d")
        // Activate visibility of 3D objects
        elements.forEachIndexed { idx, elem ->
            if (elem.type in listOf(
                    GeoElementType.SOLID_OF_REVOLUTION,
                    GeoElementType.VECTOR_3D,
                    GeoElementType.SURFACE_3D,
                    GeoElementType.POLYHEDRON,
                    GeoElementType.PLANE_3D
                ) && !elem.visible
            ) {
                elements[idx] = elem.copy(visible = true)
            }
        }
        controller.setElements(elements.toList())
    }

    // ==========================================
    // Fullscreen Layout Container
    // ==========================================
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 1. Full-Width Top Bar ──
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = if (isDarkCanvas) Color(0xFF1E293B) else Color.White,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDarkCanvas) Color(0xFF334155) else Color(0xFFE5E7EB)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = if (isDarkCanvas) Color.White else Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Brand: Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "GN",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GeoNix",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkCanvas) Color.White else Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Calculator Suite",
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = if (isDarkCanvas) Color(0xFF94A3B8) else Color(0xFF4B5563)
                    )
                }

                // Suite Mode Dropdown Pill Button
                Box {
                    Surface(
                        onClick = { showModeDropdown = true },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkCanvas) Color(0xFF334155) else Color(0xFFF9FAFB),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkCanvas) Color(0xFF475569) else Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (currentMode) {
                                    CalculatorSuiteMode.GRAPHING -> Icons.AutoMirrored.Filled.ShowChart
                                    CalculatorSuiteMode.CALCULATOR_3D -> Icons.Default.ViewInAr
                                    CalculatorSuiteMode.GEOMETRY -> Icons.Default.Category
                                    CalculatorSuiteMode.CAS -> Icons.Default.Functions
                                    CalculatorSuiteMode.PROBABILITY -> Icons.Default.BarChart
                                    CalculatorSuiteMode.SCIENTIFIC -> Icons.Default.Calculate
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentMode.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkCanvas) Color.White else Color(0xFF1F2937)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.size(16.dp),
                                tint = if (isDarkCanvas) Color(0xFF94A3B8) else Color(0xFF6B7280)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showModeDropdown,
                        onDismissRequest = { showModeDropdown = false }
                    ) {
                        CalculatorSuiteMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        when (mode) {
                                            CalculatorSuiteMode.GRAPHING -> Icons.AutoMirrored.Filled.ShowChart
                                            CalculatorSuiteMode.CALCULATOR_3D -> Icons.Default.ViewInAr
                                            CalculatorSuiteMode.GEOMETRY -> Icons.Default.Category
                                            CalculatorSuiteMode.CAS -> Icons.Default.Functions
                                            CalculatorSuiteMode.PROBABILITY -> Icons.Default.BarChart
                                            CalculatorSuiteMode.SCIENTIFIC -> Icons.Default.Calculate
                                        },
                                        contentDescription = null,
                                        tint = if (currentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    currentMode = mode
                                    showModeDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Undo & Redo
                IconButton(
                    onClick = {
                        if (undoStack.isNotEmpty()) {
                            redoStack.add(elements.toList())
                            val prev = undoStack.removeLast()
                            elements.clear()
                            elements.addAll(prev)
                            syncWithEngine(saveHistory = false)
                        }
                    },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            undoStack.add(elements.toList())
                            val next = redoStack.removeLast()
                            elements.clear()
                            elements.addAll(next)
                            syncWithEngine(saveHistory = false)
                        }
                    },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Sisipkan Snapshot ke Catatan
                Button(
                    onClick = {
                        if (currentMode == CalculatorSuiteMode.GRAPHING) {
                            val bmp = capture2DSnapshot(1600, 1200)
                            if (onInsertSnapshotToNote != null) {
                                onInsertSnapshotToNote(bmp)
                                Toast.makeText(context, "Snapshot grafik 2D berhasil disisipkan ke Catatan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Snapshot grafik 2D berhasil diambil!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            webViewController?.captureSnapshot()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (onInsertSnapshotToNote != null) "Sisipkan ke Catatan" else "Snapshot",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Tombol Fullscreen (Layar Penuh Murni / Hide Header & Sidebar)
                IconButton(
                    onClick = {
                        isFullscreen = true
                        isSidebarVisible = false
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Layar Penuh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Settings
                IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

        val sidebarWidth = 420.dp
        val buttonWidth = 18.dp
        val sidebarOffset by animateDpAsState(
            targetValue = if (isSidebarVisible) 0.dp else -sidebarWidth,
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "sidebarAnim"
        )

        // ── 2. Middle Workspace (Sidebar Drawer + Canvas) ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Right Area: Interactive Canvas (2D Cartesian / 3D Ruang) with animated start padding
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = (sidebarWidth + sidebarOffset).coerceAtLeast(0.dp))
            ) {
                // ── 3D Ruang (Three.js WebView) ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (currentMode == CalculatorSuiteMode.GRAPHING) Modifier.size(0.dp) else Modifier
                        )
                ) {
                    Math3DWebView(
                        onControllerReady = { controller ->
                            webViewController = controller
                            controller.setElements(elements.toList())
                        },
                        onMetricsUpdated = { _ -> },
                        onSnapshotReady = { bitmap ->
                            if (onInsertSnapshotToNote != null) {
                                onInsertSnapshotToNote(bitmap)
                                Toast.makeText(context, "Snapshot grafis berhasil disisipkan ke Catatan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Snapshot berhasil diambil (${bitmap.width}x${bitmap.height} px)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                if (currentMode == CalculatorSuiteMode.GRAPHING) {
                    // ── 2D Cartesian Graph Canvas (100% Mirroring Notes / FloatingGraphWindow) ──
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .background(if (isDarkCanvas) Color(0xFF141821) else Color.White)
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

                        val gridStep = remember(worldSpanX) { calculateNiceStep(worldSpanX) }
                        val gridStepX = gridStep
                        val gridStepY = gridStep

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .pointerInput(viewWidth, viewHeight) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val currentSpanX = worldSpanX
                                        val currentSpanY = currentSpanX / aspectRatio

                                        val wxPrev = centerWorldX + ((centroid.x - pan.x) - viewWidth / 2f) * (currentSpanX / viewWidth)
                                        val wyPrev = centerWorldY - ((centroid.y - pan.y) - viewHeight / 2f) * (currentSpanY / viewHeight)

                                        val newSpanX = (currentSpanX / zoom).coerceIn(0.01f, 5000f)
                                        val newSpanY = newSpanX / aspectRatio

                                        centerWorldX = wxPrev - (centroid.x - viewWidth / 2f) * (newSpanX / viewWidth)
                                        centerWorldY = wyPrev + (centroid.y - viewHeight / 2f) * (newSpanY / viewHeight)
                                        worldSpanX = newSpanX
                                    }
                                }
                        ) {
                            clipRect(0f, 0f, size.width, size.height) {
                                val gridColor = if (isDarkCanvas) Color(0x35FFFFFF) else Color(0xFFE2E8F0)
                                val subGridColor = if (isDarkCanvas) Color(0x15FFFFFF) else Color(0xFFF1F5F9)
                                val axisColor = if (isDarkCanvas) Color(0xEEFFFFFF) else Color(0xFF334155)

                                // 1. Grid Lines
                                if (isGridEnabled) {
                                    val subStep = gridStep / 5f
                                    if (viewWidth / (worldSpanX / subStep) > 10f) {
                                        val firstSubX = (Math.floor(xMin.toDouble() / subStep) * subStep).toFloat()
                                        var subX = firstSubX
                                        while (subX <= xMax + subStep) {
                                            val sx = toScreenX(subX)
                                            drawLine(
                                                color = subGridColor,
                                                start = Offset(sx, 0f),
                                                end = Offset(sx, viewHeight),
                                                strokeWidth = 1f
                                            )
                                            subX += subStep
                                        }

                                        val firstSubY = (Math.floor(yMin.toDouble() / subStep) * subStep).toFloat()
                                        var subY = firstSubY
                                        while (subY <= yMax + subStep) {
                                            val sy = toScreenY(subY)
                                            drawLine(
                                                color = subGridColor,
                                                start = Offset(0f, sy),
                                                end = Offset(viewWidth, sy),
                                                strokeWidth = 1f
                                            )
                                            subY += subStep
                                        }
                                    }

                                    val firstGridX = (Math.floor(xMin.toDouble() / gridStepX) * gridStepX).toFloat()
                                    var gx = firstGridX
                                    while (gx <= xMax + gridStepX) {
                                        val sx = toScreenX(gx)
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(sx, 0f),
                                            end = Offset(sx, viewHeight),
                                            strokeWidth = 1.2f
                                        )
                                        gx += gridStepX
                                    }

                                    val firstGridY = (Math.floor(yMin.toDouble() / gridStepY) * gridStepY).toFloat()
                                    var gy = firstGridY
                                    while (gy <= yMax + gridStepY) {
                                        val sy = toScreenY(gy)
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(0f, sy),
                                            end = Offset(viewWidth, sy),
                                            strokeWidth = 1.2f
                                        )
                                        gy += gridStepY
                                    }
                                }

                                // 2. Axes (X & Y)
                                if (isAxesEnabled) {
                                    val originSx = toScreenX(0f)
                                    val originSy = toScreenY(0f)

                                    if (originSy in -100f..(viewHeight + 100f)) {
                                        drawLine(
                                            color = axisColor,
                                            start = Offset(0f, originSy),
                                            end = Offset(viewWidth, originSy),
                                            strokeWidth = 2.0f
                                        )
                                    }
                                    if (originSx in -100f..(viewWidth + 100f)) {
                                        drawLine(
                                            color = axisColor,
                                            start = Offset(originSx, 0f),
                                            end = Offset(originSx, viewHeight),
                                            strokeWidth = 2.0f
                                        )
                                    }

                                    // Axis Labels / Numbers (Large & crisp on tablet)
                                    val textPaint = android.graphics.Paint().apply {
                                        color = if (isDarkCanvas) android.graphics.Color.rgb(200, 210, 225) else android.graphics.Color.rgb(70, 85, 105)
                                        textSize = 28f
                                        isAntiAlias = true
                                        isFakeBoldText = true
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }

                                    val clampedY = (originSy + 34f).coerceIn(36f, viewHeight - 12f)
                                    val firstLX = (Math.floor(xMin.toDouble() / gridStepX) * gridStepX).toFloat()
                                    var lx = firstLX
                                    while (lx <= xMax + gridStepX) {
                                        if (Math.abs(lx) > 1e-4) {
                                            val sx = toScreenX(lx)
                                            if (sx in 0f..viewWidth) {
                                                drawContext.canvas.nativeCanvas.drawText(
                                                    MathPlotHelper.formatNumber(lx),
                                                    sx,
                                                    clampedY,
                                                    textPaint
                                                )
                                            }
                                        }
                                        lx += gridStepX
                                    }

                                    val yPaint = android.graphics.Paint(textPaint).apply {
                                        textAlign = android.graphics.Paint.Align.RIGHT
                                    }
                                    val clampedX = (originSx - 12f).coerceIn(55f, viewWidth - 12f)
                                    val firstLY = (Math.floor(yMin.toDouble() / gridStepY) * gridStepY).toFloat()
                                    var ly = firstLY
                                    while (ly <= yMax + gridStepY) {
                                        if (Math.abs(ly) > 1e-4) {
                                            val sy = toScreenY(ly)
                                            if (sy in 0f..viewHeight) {
                                                drawContext.canvas.nativeCanvas.drawText(
                                                    MathPlotHelper.formatNumber(ly),
                                                    clampedX,
                                                    sy + 10f,
                                                    yPaint
                                                )
                                            }
                                        }
                                        ly += gridStepY
                                    }
                                }

                                // 3. Curves / Functions (Thin & Crisp stroke width = 2.2f matching FloatingGraphWindow.kt!)
                                val curveStroke = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                elements.forEach { elem ->
                                    if (!elem.visible) return@forEach
                                    val col = try { Color(android.graphics.Color.parseColor(elem.color)) } catch (_: Exception) { Color(0xFF2563EB) }
                                    val isDual = elem.expr.contains("y^2") || elem.expr.contains("y^{2}") || elem.expr.contains("y²") || elem.expr.contains("y*y") ||
                                                 elem.definition.contains("y^2") || elem.definition.contains("y^{2}") || elem.definition.contains("y²") || elem.definition.contains("y*y")

                                    when (elem.type) {
                                        GeoElementType.FUNCTION_2D -> {
                                            val clean = cleanEquationForEval(if (elem.expr.isNotBlank()) elem.expr else elem.definition)

                                            // Definite Integral Shading & Riemann Sums (Identik Notes / FloatingGraphWindow)
                                            if (elem.hasIntegral) {
                                                val a = elem.a
                                                val b = elem.b
                                                val xStart = minOf(a, b)
                                                val xEnd = maxOf(a, b)
                                                val compiled = MathPlotHelper.compile(clean)
                                                if (compiled != null && xEnd - xStart > 1e-4f) {
                                                    val shadeColor = col.copy(alpha = 0.28f)
                                                    val syZero = toScreenY(0f)

                                                    if (elem.integralRiemannType == "SMOOTH") {
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
                                                        val n = elem.integralPartitions.coerceIn(2, 60)
                                                        val dx = (xEnd - xStart) / n
                                                        for (i in 0 until n) {
                                                            val x0 = xStart + i * dx
                                                            val x1 = x0 + dx
                                                            when (elem.integralRiemannType) {
                                                                "TRAPEZOIDAL" -> {
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
                                                                    drawPath(trapPath, color = col.copy(alpha = 0.5f), style = Stroke(width = 1.2f))
                                                                }
                                                                else -> {
                                                                    val evalX = when (elem.integralRiemannType) {
                                                                        "LEFT" -> x0
                                                                        "RIGHT" -> x1
                                                                        else -> (x0 + x1) / 2f
                                                                    }
                                                                    val yH = compiled.eval(evalX.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                                                    val sLeft = toScreenX(x0)
                                                                    val sRight = toScreenX(x1)
                                                                    val sTop = minOf(syZero, toScreenY(yH))
                                                                    val sHeight = kotlin.math.abs(syZero - toScreenY(yH))
                                                                    drawRect(
                                                                        color = shadeColor,
                                                                        topLeft = Offset(sLeft, sTop),
                                                                        size = androidx.compose.ui.geometry.Size(sRight - sLeft, sHeight)
                                                                    )
                                                                    drawRect(
                                                                        color = col.copy(alpha = 0.5f),
                                                                        topLeft = Offset(sLeft, sTop),
                                                                        size = androidx.compose.ui.geometry.Size(sRight - sLeft, sHeight),
                                                                        style = Stroke(width = 1.2f)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Boundary lines at a and b
                                                    val ya = compiled.eval(a.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                                    val yb = compiled.eval(b.toDouble()).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() ?: 0f
                                                    val sxa = toScreenX(a)
                                                    val sxb = toScreenX(b)

                                                    drawLine(
                                                        color = col,
                                                        start = Offset(sxa, syZero),
                                                        end = Offset(sxa, toScreenY(ya)),
                                                        strokeWidth = 2.2f,
                                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                                    )
                                                    drawLine(
                                                        color = col,
                                                        start = Offset(sxb, syZero),
                                                        end = Offset(sxb, toScreenY(yb)),
                                                        strokeWidth = 2.2f,
                                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                                    )

                                                    drawCircle(color = col, radius = 5.dp.toPx(), center = Offset(sxa, toScreenY(ya)))
                                                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(sxa, toScreenY(ya)), style = Stroke(width = 1.5.dp.toPx()))
                                                    drawCircle(color = col, radius = 5.dp.toPx(), center = Offset(sxb, toScreenY(yb)))
                                                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(sxb, toScreenY(yb)), style = Stroke(width = 1.5.dp.toPx()))

                                                    val intRes = MathPlotHelper.calculateDefiniteIntegral(clean, a, b)
                                                    val labelVal = "∫ = " + MathPlotHelper.formatNumber(intRes.netIntegral.toFloat())
                                                    val valPaint = android.graphics.Paint().apply {
                                                        color = android.graphics.Color.rgb(
                                                            (col.red * 255).toInt(),
                                                            (col.green * 255).toInt(),
                                                            (col.blue * 255).toInt()
                                                        )
                                                        textSize = 24f
                                                        isFakeBoldText = true
                                                        isAntiAlias = true
                                                        textAlign = android.graphics.Paint.Align.CENTER
                                                    }
                                                    val midX = (sxa + sxb) / 2f
                                                    val midY = (syZero + toScreenY((ya + yb) / 2f)) / 2f
                                                    drawContext.canvas.nativeCanvas.drawText(labelVal, midX, midY.coerceIn(24f, viewHeight - 24f), valPaint)
                                                }
                                            }

                                            val pts = MathPlotHelper.sampleFunctionDynamic(clean, xMin, xMax, steps = 500)
                                            if (pts.isNotEmpty()) {
                                                val pth = Path()
                                                var isFirst = true
                                                for (pt in pts) {
                                                    val sx = toScreenX(pt.x)
                                                    val sy = toScreenY(pt.y)
                                                    if (sy.isFinite() && sy in -viewHeight..(viewHeight * 2)) {
                                                        if (isFirst) {
                                                            pth.moveTo(sx, sy)
                                                            isFirst = false
                                                        } else {
                                                            pth.lineTo(sx, sy)
                                                        }
                                                    } else {
                                                        isFirst = true
                                                    }
                                                }
                                                drawPath(pth, col, style = curveStroke)

                                                if (isDual) {
                                                    val pthNeg = Path()
                                                    var isFirstNeg = true
                                                    for (pt in pts) {
                                                        val sx = toScreenX(pt.x)
                                                        val sy = toScreenY(-pt.y)
                                                        if (sy.isFinite() && sy in -viewHeight..(viewHeight * 2)) {
                                                            if (isFirstNeg) {
                                                                pthNeg.moveTo(sx, sy)
                                                                isFirstNeg = false
                                                            } else {
                                                                pthNeg.lineTo(sx, sy)
                                                            }
                                                        } else {
                                                            isFirstNeg = true
                                                        }
                                                    }
                                                    drawPath(pthNeg, col, style = curveStroke)
                                                }
                                            }
                                        }
                                        GeoElementType.SOLID_OF_REVOLUTION -> {
                                            val aX = elem.a ?: 0f
                                            val bX = elem.b ?: 4f
                                            val clean = cleanEquationForEval(if (elem.expr.isNotBlank()) elem.expr else elem.definition)
                                            val pts = MathPlotHelper.sampleFunctionDynamic(clean, aX, bX, steps = 300)
                                            if (pts.isNotEmpty()) {
                                                val fillPath = Path()
                                                val baselineY = toScreenY(0f)
                                                fillPath.moveTo(toScreenX(aX), baselineY)
                                                for (pt in pts) {
                                                    fillPath.lineTo(toScreenX(pt.x), toScreenY(pt.y))
                                                }
                                                fillPath.lineTo(toScreenX(bX), baselineY)
                                                fillPath.close()
                                                drawPath(fillPath, col.copy(alpha = 0.22f))

                                                val curvePath = Path()
                                                var isFirst = true
                                                for (pt in pts) {
                                                    val sx = toScreenX(pt.x)
                                                    val sy = toScreenY(pt.y)
                                                    if (isFirst) {
                                                        curvePath.moveTo(sx, sy)
                                                        isFirst = false
                                                    } else {
                                                        curvePath.lineTo(sx, sy)
                                                    }
                                                }
                                                drawPath(curvePath, col, style = curveStroke)
                                            }
                                        }
                                        GeoElementType.POINT_3D -> {
                                            val px = elem.x ?: 0f
                                            val py = elem.y ?: 0f
                                            drawCircle(col, radius = 5.dp.toPx(), center = Offset(toScreenX(px), toScreenY(py)))
                                        }
                                        GeoElementType.VECTOR_3D -> {
                                            val vx = elem.x ?: 1f
                                            val vy = elem.y ?: 1f
                                            val startP = Offset(toScreenX(0f), toScreenY(0f))
                                            val endP = Offset(toScreenX(vx), toScreenY(vy))
                                            drawLine(col, startP, endP, strokeWidth = 2.2f)
                                            drawCircle(col, radius = 4.dp.toPx(), center = endP)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }




                // Floating Exit Fullscreen Button when header is hidden
                if (isFullscreen) {
                    Surface(
                        onClick = {
                            isFullscreen = false
                            isSidebarVisible = true
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 4.dp,
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.FullscreenExit,
                                contentDescription = "Keluar Layar Penuh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom-Right Floating Controls (Authentic GeoNix Canvas Tools)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkCanvas) Color(0xFF1E293B) else Color.White,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkCanvas) Color(0xFF334155) else Color(0xFFE5E7EB)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(3.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Home
                        IconButton(
                            onClick = {
                                if (currentMode == CalculatorSuiteMode.GRAPHING) {
                                    centerWorldX = 0f
                                    centerWorldY = 0f
                                    worldSpanX = 18f
                                } else {
                                    webViewController?.setCameraView("iso")
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (isDarkCanvas) Color.White else Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.width(22.dp),
                            color = if (isDarkCanvas) Color(0xFF334155) else Color(0xFFE5E7EB)
                        )

                        // Zoom In
                        IconButton(
                            onClick = {
                                if (currentMode == CalculatorSuiteMode.GRAPHING) {
                                    worldSpanX = (worldSpanX / 1.35f).coerceAtLeast(0.01f)
                                } else {
                                    webViewController?.zoomIn()
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = if (isDarkCanvas) Color.White else Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Zoom Out
                        IconButton(
                            onClick = {
                                if (currentMode == CalculatorSuiteMode.GRAPHING) {
                                    worldSpanX = (worldSpanX * 1.35f).coerceAtMost(5000f)
                                } else {
                                    webViewController?.zoomOut()
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = if (isDarkCanvas) Color.White else Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.width(22.dp),
                            color = if (isDarkCanvas) Color(0xFF334155) else Color(0xFFE5E7EB)
                        )

                        // Fullscreen Toggle
                        IconButton(
                            onClick = {
                                isFullscreen = !isFullscreen
                                if (isFullscreen) {
                                    isSidebarVisible = false
                                } else {
                                    isSidebarVisible = true
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = if (isDarkCanvas) Color.White else Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Left Sidebar Drawer + Attached Toggle Handle (Glides in 100% lockstep)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(sidebarWidth + buttonWidth)
                    .offset(x = sidebarOffset)
                    .zIndex(10f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(sidebarWidth)
                ) {
                    GeoNixAlgebraView(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    elements = elements,
                    is3DMode = (currentMode == CalculatorSuiteMode.CALCULATOR_3D || currentMode == CalculatorSuiteMode.GEOMETRY),
                    onElementToggleVisibility = { id ->
                        val index = elements.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val current = elements[index]
                            elements[index] = current.copy(visible = !current.visible)
                            syncWithEngine()
                        }
                    },
                    onElementDelete = { id ->
                        elements.removeAll { it.id == id }
                        if (editingElement?.id == id) {
                            editingElement = null
                            mathRoot = latexToMathExpr("")
                            mathCursorOffset = 0
                        }
                        syncWithEngine()
                    },
                    onElementDuplicate = { elem ->
                        val copy = elem.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            name = "${elem.name}_copy"
                        )
                        elements.add(copy)
                        syncWithEngine()
                    },
                    onElementColorChange = { id, colorHex ->
                        val index = elements.indexOfFirst { it.id == id }
                        if (index != -1) {
                            elements[index] = elements[index].copy(color = colorHex)
                            syncWithEngine()
                        }
                    },
                    onElementVariableChange = { varName, newVal ->
                        webViewController?.setVariable(varName, newVal)
                    },
                    onElementPropertyChange = { id, updater ->
                        val index = elements.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val oldElem = elements[index]
                            val newElem = updater(oldElem)
                            elements[index] = newElem
                            val isAngleOnly = oldElem.copy(angle = newElem.angle) == newElem
                            if (isAngleOnly) {
                                // Transient fast update: no disk/undo spam for smooth 60fps animation
                                webViewController?.setElements(elements.toList())
                            } else {
                                syncWithEngine()
                            }
                        }
                    },
                    onAddElement = { newElem ->
                        elements.add(newElem)
                        syncWithEngine()
                    },
                    onCameraPreset = { preset ->
                        webViewController?.setCameraView(preset)
                    },
                    onSelect3DMode = {
                        currentMode = CalculatorSuiteMode.CALCULATOR_3D
                    },
                    onConvertToSolidOfRevolution = { elem ->
                        val solidElem = GeoElementModel(
                            id = java.util.UUID.randomUUID().toString(),
                            type = GeoElementType.SOLID_OF_REVOLUTION,
                            name = "Putar ${elem.name}",
                            definition = "Putar(${elem.name}, 360°, SumbuX)",
                            expr = elem.expr.ifBlank { elem.definition },
                            a = 0f,
                            b = 4f,
                            axis = "x",
                            angle = 360f,
                            color = elem.color,
                            visible = true
                        )
                        elements.add(solidElem)
                        currentMode = CalculatorSuiteMode.CALCULATOR_3D
                        syncWithEngine()
                    },
                    mathRoot = mathRoot,
                    mathActiveSeqId = mathActiveSeqId,
                    mathCursorOffset = mathCursorOffset,
                    mathCursorAlpha = mathCursorAlpha,
                    onSelectCursor = { seqId, offset ->
                        mathActiveSeqId = seqId
                        mathCursorOffset = offset
                        showMathKeypad = true
                    },
                    showMathKeypad = showMathKeypad,
                    onToggleMathKeypad = { showMathKeypad = !showMathKeypad },
                    editingElement = editingElement,
                    onStartEdit = { elem ->
                        editingElement = elem
                        val formulaStr = if (elem.type == GeoElementType.SOLID_OF_REVOLUTION) {
                            val raw = if (elem.definition.contains("Putar ")) {
                                elem.definition.substringAfter("Putar ").substringBefore(" [").trim()
                            } else {
                                elem.expr.ifBlank { "sqrt(x)" }
                            }
                            formatFormulaToLatex(raw)
                        } else {
                            formatFormulaToLatex(elem.definition.ifBlank { elem.expr })
                        }
                        val parsed = try {
                            latexToMathExpr(formulaStr)
                        } catch (_: Exception) {
                            latexToMathExpr(elem.definition.ifBlank { elem.expr })
                        }
                        mathRoot = parsed
                        mathActiveSeqId = parsed.id
                        mathCursorOffset = parsed.items.size
                        showMathKeypad = true
                    },
                    onCancelEdit = {
                        editingElement = null
                    },
                    onSaveEquation = {
                        saveEquation()
                    },
                    onSelectPreset = { presetVal ->
                        if (presetVal.contains("Benda Putar")) {
                            currentMode = CalculatorSuiteMode.CALCULATOR_3D
                            val revElem = GeoElementModel(
                                id = java.util.UUID.randomUUID().toString(),
                                type = GeoElementType.SOLID_OF_REVOLUTION,
                                name = "BendaPutar_${elements.count { it.type == GeoElementType.SOLID_OF_REVOLUTION } + 1}",
                                definition = "Putar \\sqrt{x} [0, 4] 360° Sumbu-X",
                                expr = "sqrt(x)",
                                a = 0f,
                                b = 4f,
                                axis = "x",
                                angle = 360f,
                                color = "#7C3AED",
                                visible = true
                            )
                            elements.add(revElem)
                            syncWithEngine()
                        } else if (presetVal.contains("u =") || presetVal.contains("Vektor")) {
                            currentMode = CalculatorSuiteMode.CALCULATOR_3D
                            val vecElem = GeoElementModel(
                                id = java.util.UUID.randomUUID().toString(),
                                type = GeoElementType.VECTOR_3D,
                                name = "u_${elements.count { it.type == GeoElementType.VECTOR_3D } + 1}",
                                definition = "u = (3, 2, 4)",
                                x = 3f,
                                y = 2f,
                                z = 4f,
                                color = "#DC2626",
                                visible = true
                            )
                            elements.add(vecElem)
                            syncWithEngine()
                        } else {
                            val parsed = latexToMathExpr(presetVal)
                            mathRoot = parsed
                            mathActiveSeqId = parsed.id
                            mathCursorOffset = parsed.items.size
                            saveEquation()
                        }
                    }
                )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.5.dp,
                        color = if (isDarkCanvas) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                }

                // Sleek, Compact Sidebar Toggle Pill Handle (Anchored to sidebar edge)
                Surface(
                    onClick = { isSidebarVisible = !isSidebarVisible },
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    color = if (isDarkCanvas) Color(0xFF1E293B) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkCanvas) Color(0xFF334155) else Color(0xFFCBD5E1)
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = sidebarWidth)
                        .width(buttonWidth)
                        .height(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isSidebarVisible) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                            contentDescription = "Toggle Sidebar",
                            tint = if (isDarkCanvas) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // ── 3. Bottom-Docked Full-Width (100% Width) GeoNix Keypad ──
        AnimatedVisibility(
            visible = showMathKeypad,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Keypad Tab Header (123, f(x), abc, αβγ) + Close
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
                                color = if (isSelected) Color(0xFF1E293B)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (isSelected) Color(0xFF1E293B)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showMathKeypad = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup Keypad",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Keypad Body (Full 100% Width)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (keyboardTab) {
                            0 -> GeoNixKeypad123(
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
                            1 -> GeoNixKeypadFunctions(
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
                            2 -> GeoNixKeypadAbc(
                                onChar = { insertMathText(it) },
                                onBackspace = { handleMathBackspace() },
                                onNavLeft = { handleMathNavLeft() },
                                onNavRight = { handleMathNavRight() },
                                onEnter = { saveEquation() }
                            )
                            else -> GeoNixKeypadGreek(
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

    // ── Quick Settings Dialog ──
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Pengaturan Kanvas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tampilkan Sumbu (Axes)", fontSize = 13.sp)
                        Switch(
                            checked = isAxesEnabled,
                            onCheckedChange = {
                                isAxesEnabled = it
                                webViewController?.toggleAxes(it)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tampilkan Kisi-Kisi (Grid)", fontSize = 13.sp)
                        Switch(
                            checked = isGridEnabled,
                            onCheckedChange = {
                                isGridEnabled = it
                                webViewController?.toggleGrid(it)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tema Gelap Kanvas", fontSize = 13.sp)
                        Switch(
                            checked = isDarkCanvas,
                            onCheckedChange = {
                                isDarkCanvas = it
                                webViewController?.setTheme(if (it) "dark" else "light")
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun calculateNiceStep(range: Float): Float {
    val rawStep = (range / 8f).coerceAtLeast(0.0001f)
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

private fun parseHexColor(hex: String, fallback: Color = Color(0xFF2563EB)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = if (clean.length == 6) {
            (0xFF000000 or clean.toLong(16)).toInt()
        } else {
            clean.toLong(16).toInt()
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

private fun cleanEquationForEval(raw: String): String {
    var text = raw.trim()
    val eqIdx = text.indexOf('=')
    if (eqIdx != -1) {
        val lhs = text.substring(0, eqIdx).trim().replace(" ", "")
        val rhs = text.substring(eqIdx + 1).trim()
        val y2Pattern = Regex("""^([+-]?\d*\.?\d*\*?)?y(\^2|\^\{2\}|²|\*y)$""", RegexOption.IGNORE_CASE)
        val lhsMatch = y2Pattern.matchEntire(lhs)
        if (lhsMatch != null) {
            val coeffStr = lhsMatch.groupValues[1].replace("*", "")
            val coeff = if (coeffStr.isEmpty() || coeffStr == "+") 1.0 else if (coeffStr == "-") -1.0 else coeffStr.toDoubleOrNull() ?: 1.0
            val inner = if (coeff == 1.0) rhs else "($rhs)/$coeff"
            return MathPlotHelper.cleanLatexForPlot("sqrt($inner)")
        }
        val rhsMatch = y2Pattern.matchEntire(rhs.replace(" ", ""))
        if (rhsMatch != null && (lhs.equals("x", ignoreCase = true) || lhs.equals("x(y)", ignoreCase = true))) {
            val coeffStr = rhsMatch.groupValues[1].replace("*", "")
            val coeff = if (coeffStr.isEmpty() || coeffStr == "+") 1.0 else if (coeffStr == "-") -1.0 else coeffStr.toDoubleOrNull() ?: 1.0
            val inner = if (coeff == 1.0) "x" else "x/$coeff"
            return MathPlotHelper.cleanLatexForPlot("sqrt($inner)")
        }
        if (lhs.equals("y", ignoreCase = true) || lhs.startsWith("f(", ignoreCase = true) || lhs.startsWith("g(", ignoreCase = true)) {
            text = rhs
        } else {
            text = rhs
        }
    }
    return MathPlotHelper.cleanLatexForPlot(text)
}
