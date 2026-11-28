package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.math.MathPlotHelper
import com.lernspace.app.ui.workspace.model.InteractiveModuleElement
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Interactive graph module embedded directly on the note canvas.
 * Automatically detects symbolic parameters (e.g. m, c in y = mx + c)
 * and generates live interactive sliders that re-plot the curve at 60fps.
 */
@Composable
fun InteractiveGraphModule(
    module: InteractiveModuleElement,
    isDarkTheme: Boolean = false,
    scaleProvider: () -> Float = { 1f },
    panXProvider: () -> Float = { 0f },
    panYProvider: () -> Float = { 0f },
    onCommitMove: (Float, Float) -> Unit,
    onCommitResize: (Float, Float) -> Unit,
    onUpdateModuleState: (equation: String, parameters: Map<String, Float>) -> Unit,
    onSelect: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var worldX by remember(module.id) { mutableFloatStateOf(module.x) }
    var worldY by remember(module.id) { mutableFloatStateOf(module.y) }
    var worldWidth by remember(module.id) { mutableFloatStateOf(module.width) }
    var worldHeight by remember(module.id) { mutableFloatStateOf(module.height) }

    LaunchedEffect(module.x, module.y) {
        worldX = module.x
        worldY = module.y
    }
    LaunchedEffect(module.width, module.height) {
        worldWidth = module.width
        worldHeight = module.height
    }

    var equationText by remember(module.id) { mutableStateOf(module.equation) }
    val paramValues = remember(module.id) {
        mutableStateMapOf<String, Float>().apply {
            putAll(module.parameters)
        }
    }

    LaunchedEffect(module.equation) {
        if (module.equation != equationText) {
            equationText = module.equation
        }
    }
    LaunchedEffect(module.parameters) {
        module.parameters.forEach { (k, v) ->
            if (paramValues[k] != v) {
                paramValues[k] = v
            }
        }
    }

    // Auto-detect parameters whenever equation changes
    val detectedParams: List<String> = remember(equationText) {
        MathPlotHelper.extractParameters(equationText).map { it.toString() }
    }

    LaunchedEffect(detectedParams) {
        var changed = false
        for (p in detectedParams) {
            if (!paramValues.containsKey(p)) {
                paramValues[p] = when (p) {
                    "a", "b", "m", "k" -> 1f
                    "r" -> 3f
                    else -> 0f
                }
                changed = true
            }
        }
        val toRemove = paramValues.keys.filter { it !in detectedParams }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { paramValues.remove(it) }
            changed = true
        }
        if (changed) {
            onUpdateModuleState(equationText, paramValues.toMap())
        }
    }

    var viewRange by remember(module.id) { mutableFloatStateOf(8f) }

    val currentScaleProvider by rememberUpdatedState(scaleProvider)
    val currentOnCommitMove by rememberUpdatedState(onCommitMove)
    val currentOnCommitResize by rememberUpdatedState(onCommitResize)
    val currentOnUpdateState by rememberUpdatedState(onUpdateModuleState)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnToggleLock by rememberUpdatedState(onToggleLock)
    val currentOnDelete by rememberUpdatedState(onDelete)

    val bgColor = if (isDarkTheme) Color(0xFF1E222A) else Color(0xFFFFFFFF)
    val headerBg = if (isDarkTheme) Color(0xFF262B36) else Color(0xFFF3F6FC)
    val inputBg = if (isDarkTheme) Color(0xFF14171D) else Color(0xFFFFFFFF)
    val borderColor = if (module.isSelected) {
        if (module.isLocked) Color(0xFFF59E0B) else Color(0xFF2563EB)
    } else {
        if (isDarkTheme) Color(0xFF333A48) else Color(0xFFD6DCE8)
    }
    val textPrimary = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val gridColor = if (isDarkTheme) Color(0xFF2B313E) else Color(0xFFE8EEF5)
    val axisColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
    val curveColor = Color(0xFF2563EB)

    val worldWidthDp = with(density) { worldWidth.toDp() }
    val worldHeightDp = with(density) { worldHeight.toDp() }

    // Compile substituted expression for fast 60fps rendering
    val charParams = remember(paramValues.toMap()) {
        paramValues.entries.mapNotNull { (k, v) ->
            k.firstOrNull()?.let { it to v }
        }.toMap()
    }
    val substitutedExpr = remember(equationText, charParams) {
        val sub = MathPlotHelper.substituteParameters(equationText, charParams)
        MathPlotHelper.extractFunctionBody(sub)
    }
    val compiledExpr = remember(substitutedExpr) {
        if (substitutedExpr.isBlank()) null else MathPlotHelper.compile(substitutedExpr)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = currentScaleProvider()
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = worldX * s + panXProvider()
                translationY = worldY * s + panYProvider()
            }
            .size(width = worldWidthDp, height = worldHeightDp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (module.isSelected) 10.dp else 4.dp, RoundedCornerShape(14.dp))
                .border(
                    width = if (module.isSelected) 2.dp else 1.2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            color = bgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Top Drag & Action Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .then(
                            if (!module.isLocked) {
                                Modifier.pointerInput(module.id) {
                                    detectDragGestures(
                                        onDragStart = { currentOnSelect() },
                                        onDragEnd = { currentOnCommitMove(worldX, worldY) },
                                        onDragCancel = {
                                            worldX = module.x
                                            worldY = module.y
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            worldX += dragAmount.x
                                            worldY += dragAmount.y
                                        }
                                    )
                                }
                            } else {
                                Modifier.clickable { currentOnSelect() }
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragIndicator,
                            contentDescription = "Geser Modul",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = curveColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Modul Grafik",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Zoom Out / In / Reset
                        ModuleSmallIconButton(
                            onClick = { viewRange = (viewRange * 1.4f).coerceAtMost(40f) },
                            bg = inputBg,
                            border = borderColor
                        ) {
                            Icon(Icons.Filled.Remove, "Zoom Out", tint = textSecondary, modifier = Modifier.size(13.dp))
                        }
                        ModuleSmallIconButton(
                            onClick = { viewRange = (viewRange / 1.4f).coerceAtLeast(2f) },
                            bg = inputBg,
                            border = borderColor
                        ) {
                            Icon(Icons.Filled.Add, "Zoom In", tint = textSecondary, modifier = Modifier.size(13.dp))
                        }
                        ModuleSmallIconButton(
                            onClick = { viewRange = 8f },
                            bg = inputBg,
                            border = borderColor
                        ) {
                            Icon(Icons.Filled.Refresh, "Reset", tint = textSecondary, modifier = Modifier.size(13.dp))
                        }
                        // Lock / Unlock
                        ModuleSmallIconButton(
                            onClick = { currentOnToggleLock() },
                            bg = if (module.isLocked) Color(0xFFF59E0B).copy(alpha = 0.16f) else inputBg,
                            border = if (module.isLocked) Color(0xFFF59E0B) else borderColor
                        ) {
                            Icon(
                                imageVector = if (module.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Kunci Posisi",
                                tint = if (module.isLocked) Color(0xFFF59E0B) else textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        // Delete (hidden when module is locked)
                        if (!module.isLocked) {
                            ModuleSmallIconButton(
                                onClick = { currentOnDelete() },
                                bg = Color(0xFFEF4444).copy(alpha = 0.12f),
                                border = Color(0xFFEF4444).copy(alpha = 0.4f)
                            ) {
                                Icon(Icons.Filled.Close, "Hapus Modul", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }

                // ── Equation Input Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "f(x)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = curveColor,
                        fontFamily = FontFamily.Serif
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(inputBg)
                            .border(1.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (equationText.isEmpty()) {
                            Text(
                                text = "Contoh: y = mx + c atau y = ax^2 + bx + c",
                                fontSize = 11.5.sp,
                                color = textSecondary.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = equationText,
                            onValueChange = { newText ->
                                equationText = newText
                                currentOnUpdateState(newText, paramValues.toMap())
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(curveColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Live 2D Cartesian Plot Canvas ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkTheme) Color(0xFF14171D) else Color(0xFFFAFBFD))
                        .border(1.dp, gridColor, RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        if (w <= 0f || h <= 0f) return@Canvas

                        val xMin = -viewRange
                        val xMax = viewRange
                        val aspect = h / w
                        val yMin = -viewRange * aspect
                        val yMax = viewRange * aspect

                        fun toScreenX(wx: Float): Float = ((wx - xMin) / (xMax - xMin)) * w
                        fun toScreenY(wy: Float): Float = h - ((wy - yMin) / (yMax - yMin)) * h

                        val step = when {
                            viewRange <= 4f -> 1f
                            viewRange <= 12f -> 2f
                            viewRange <= 24f -> 4f
                            else -> 8f
                        }

                        // Grid lines
                        var gx = ceil(xMin / step) * step
                        while (gx <= xMax) {
                            val sx = toScreenX(gx)
                            drawLine(gridColor, Offset(sx, 0f), Offset(sx, h), strokeWidth = 1.2f)
                            gx += step
                        }
                        var gy = ceil(yMin / step) * step
                        while (gy <= yMax) {
                            val sy = toScreenY(gy)
                            drawLine(gridColor, Offset(0f, sy), Offset(w, sy), strokeWidth = 1.2f)
                            gy += step
                        }

                        // Axes
                        val originX = toScreenX(0f)
                        val originY = toScreenY(0f)
                        if (originX in 0f..w) {
                            drawLine(axisColor, Offset(originX, 0f), Offset(originX, h), strokeWidth = 2.2f)
                        }
                        if (originY in 0f..h) {
                            drawLine(axisColor, Offset(0f, originY), Offset(w, originY), strokeWidth = 2.2f)
                        }

                        // Plot compiled expression
                        val expr = compiledExpr
                        if (expr != null) {
                            val samples = 220
                            val path = Path()
                            var hasStarted = false
                            var prevSy = 0f

                            for (i in 0..samples) {
                                val wx = xMin + (xMax - xMin) * (i.toFloat() / samples)
                                val wy = expr.eval(wx.toDouble()).toFloat()
                                if (wy.isNaN() || wy.isInfinite()) {
                                    hasStarted = false
                                    continue
                                }
                                val sx = toScreenX(wx)
                                val sy = toScreenY(wy)
                                if (!hasStarted || abs(sy - prevSy) > h * 0.9f) {
                                    path.moveTo(sx, sy)
                                    hasStarted = true
                                } else {
                                    path.lineTo(sx, sy)
                                }
                                prevSy = sy
                            }

                            drawPath(
                                path = path,
                                color = curveColor,
                                style = Stroke(
                                    width = 4.2f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Range badge overlay
                    Text(
                        text = "[-${formatCompact(viewRange)}, ${formatCompact(viewRange)}]",
                        fontSize = 9.5.sp,
                        color = textSecondary,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    )
                }

                // ── Auto-Detected Interactive Parameter Sliders ──
                if (detectedParams.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 165.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        detectedParams.forEach { paramName ->
                            val currentVal = paramValues[paramName] ?: 0f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = curveColor.copy(alpha = 0.12f),
                                    modifier = Modifier.width(78.dp)
                                ) {
                                    Text(
                                        text = "$paramName = ${formatCompact(currentVal)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = curveColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Slider(
                                    value = currentVal.coerceIn(-10f, 10f),
                                    onValueChange = { newVal ->
                                        val snapped = (newVal * 10f).roundToInt() / 10f
                                        paramValues[paramName] = snapped
                                    },
                                    onValueChangeFinished = {
                                        currentOnUpdateState(equationText, paramValues.toMap())
                                    },
                                    valueRange = -10f..10f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = curveColor,
                                        activeTrackColor = curveColor,
                                        inactiveTrackColor = gridColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Tip: tambahkan parameter seperti m, c, a, b untuk slider otomatis",
                        fontSize = 10.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Bottom-Right Resize Handle (when selected and unlocked) ──
        if (module.isSelected && !module.isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(module.id) {
                        detectDragGestures(
                            onDragEnd = { currentOnCommitResize(worldWidth, worldHeight) },
                            onDragCancel = {
                                worldWidth = module.width
                                worldHeight = module.height
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                worldWidth = (worldWidth + dragAmount.x).coerceIn(360f, 1400f)
                                worldHeight = (worldHeight + dragAmount.y).coerceIn(320f, 1200f)
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun ModuleSmallIconButton(
    onClick: () -> Unit,
    bg: Color,
    border: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .size(22.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

private fun formatCompact(v: Float): String {
    return if (abs(v - v.roundToInt()) < 0.05f) {
        v.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", v)
    }
}
