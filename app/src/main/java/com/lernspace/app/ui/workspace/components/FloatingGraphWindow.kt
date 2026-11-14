package com.lernspace.app.ui.workspace.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.math.CircleData
import com.lernspace.app.core.math.CurvePlotDefinition
import com.lernspace.app.core.math.IntersectionPoint
import com.lernspace.app.core.math.KeyPoint
import com.lernspace.app.core.math.KeyPointType
import com.lernspace.app.core.math.MathPlotData
import com.lernspace.app.core.math.MathPlotHelper
import com.lernspace.app.core.math.MultiPlotCurve
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
    val keyPoints: List<KeyPoint> = emptyList()
)

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
    // ── Multi-Graph Equation List State ──
    val graphItems = remember { mutableStateListOf<GraphPlotItem>() }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var inputEquationText by remember { mutableStateOf("") }

    // Helper to parse any raw input into a GraphPlotItem
    fun parseToItem(raw: String, color: Color): GraphPlotItem {
        val trimmed = raw.trim()
        val circle = MathPlotHelper.parseCircle(trimmed)
        if (circle != null) {
            return GraphPlotItem(
                rawEquation = trimmed,
                displayLabel = circle.standardEquation,
                color = color,
                isCircle = true,
                circleData = circle,
                isVerticalLine = false,
                cleanExpr = circle.standardEquation,
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
                GraphPlotItem(
                    rawEquation = trimmed,
                    displayLabel = if (trimmed.contains("=")) trimmed else linear.standardEquation,
                    color = color,
                    isCircle = false,
                    isVerticalLine = false,
                    cleanExpr = linear.functionExpr,
                    keyPoints = MathPlotHelper.findKeyPoints(linear.functionExpr, -50f, 50f)
                )
            }
        }

        val clean = MathPlotHelper.extractFunctionBody(trimmed)
        return GraphPlotItem(
            rawEquation = trimmed,
            displayLabel = if (clean.startsWith("y=") || clean.startsWith("f(x)")) clean else "y = $clean",
            color = color,
            isCircle = false,
            isVerticalLine = false,
            cleanExpr = clean,
            keyPoints = MathPlotHelper.findKeyPoints(clean, -50f, 50f)
        )
    }

    // ── Intersections State ──
    var showIntersections by remember { mutableStateOf(false) }

    val visibleCurvesDef = remember(graphItems.toList()) {
        graphItems.filter { it.isVisible }.map {
            CurvePlotDefinition(
                id = it.id,
                cleanExpr = it.cleanExpr,
                isCircle = it.isCircle,
                circleData = it.circleData,
                isVerticalLine = it.isVerticalLine,
                verticalLineX = it.verticalLineX
            )
        }
    }

    // ── Viewport & Coordinate Navigation State ──
    var centerWorldX by remember { mutableFloatStateOf(0f) }
    var centerWorldY by remember { mutableFloatStateOf(0f) }
    var worldSpanX by remember { mutableFloatStateOf(18f) }

    val intersections = remember(visibleCurvesDef, centerWorldX, worldSpanX) {
        if (visibleCurvesDef.size >= 2) {
            MathPlotHelper.findIntersections(
                visibleCurvesDef,
                centerWorldX - worldSpanX / 2f,
                centerWorldX + worldSpanX / 2f
            )
        } else {
            emptyList()
        }
    }

    // ── Window Floating Position & Sizing State ──
    var windowOffsetX by remember { mutableFloatStateOf(60f) }
    var windowOffsetY by remember { mutableFloatStateOf(70f) }
    var windowWidthDp by remember { mutableFloatStateOf(480f) }
    var windowHeightDp by remember { mutableFloatStateOf(440f) }
    var savedCustomWidthDp by remember { mutableFloatStateOf(480f) }
    var savedCustomHeightDp by remember { mutableFloatStateOf(440f) }

    var isMinimized by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // Initialize equations list from initial equationRaw
    LaunchedEffect(equationRaw) {
        val targetEq = equationRaw.trim()
        if (targetEq.isNotBlank()) {
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

                    // ── Header Bar: Title, Move Handle & Window Actions ──
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
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title & Drag indicator
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
                        }

                        // Action Controls: Sisipkan, Expand, Minimize, Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Sisipkan snapshot to canvas
                            Button(
                                onClick = {
                                    val visibleCurves = graphItems.filter { it.isVisible }.map { item ->
                                        MultiPlotCurve(
                                            label = MathPlotHelper.formatFormulaForDisplay(item.displayLabel),
                                            colorArgb = item.color.toArgb(),
                                            points = if (item.isCircle || item.isVerticalLine) emptyList() else MathPlotHelper.sampleFunctionDynamic(
                                                item.cleanExpr,
                                                centerWorldX - worldSpanX / 2f,
                                                centerWorldX + worldSpanX / 2f,
                                                steps = 400
                                            ),
                                            circleData = item.circleData,
                                            keyPoints = item.keyPoints,
                                            isVerticalLine = item.isVerticalLine,
                                            verticalLineX = item.verticalLineX
                                        )
                                    }

                                    val aspect = (currentW / currentH).coerceAtLeast(0.5f)
                                    val spanY = worldSpanX / aspect

                                    val plotData = MathPlotData(
                                        functionExpression = activeItem?.cleanExpr ?: "",
                                        xMin = centerWorldX - worldSpanX / 2f,
                                        xMax = centerWorldX + worldSpanX / 2f,
                                        yMin = centerWorldY - spanY / 2f,
                                        yMax = centerWorldY + spanY / 2f,
                                        circleData = activeItem?.circleData,
                                        multiCurves = visibleCurves,
                                        intersections = if (showIntersections) intersections else emptyList(),
                                        isVerticalLine = activeItem?.isVerticalLine ?: false,
                                        verticalLineX = activeItem?.verticalLineX ?: 0f
                                    )

                                    val highResBmp = MathPlotHelper.renderGraphBitmap(
                                        plotData = plotData,
                                        width = 1600,
                                        height = 1200,
                                        isDarkTheme = isDarkTheme
                                    )
                                    val visibleEquations = graphItems.filter { it.isVisible }.map { it.rawEquation }
                                    onInsertGraphToNote(highResBmp, visibleEquations)
                                },
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

                    // ── Manual Input & Equations Management Bar ──
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                            // Input Field Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(inputBg)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Formula input text field
                                Box(modifier = Modifier.weight(1f)) {
                                    if (inputEquationText.isEmpty()) {
                                        Text(
                                            text = "Ketik persamaan (cth: y = x^2 - 4 atau x^2 + y^2 = 16)...",
                                            color = textSecondary.copy(alpha = 0.7f),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = inputEquationText,
                                        onValueChange = { inputEquationText = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = textPrimary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (inputEquationText.isNotBlank()) {
                                                val c = GEOGEBRA_PALETTE[graphItems.size % GEOGEBRA_PALETTE.size]
                                                val item = parseToItem(inputEquationText, c)
                                                graphItems.add(item)
                                                selectedItemId = item.id
                                                inputEquationText = ""
                                            }
                                        }),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Add button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (inputEquationText.isNotBlank()) Color(0xFF2563EB) else cardBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (inputEquationText.isNotBlank()) Color(0xFF2563EB) else borderColor
                                    ),
                                    modifier = Modifier.clickable(enabled = inputEquationText.isNotBlank()) {
                                        val c = GEOGEBRA_PALETTE[graphItems.size % GEOGEBRA_PALETTE.size]
                                        val item = parseToItem(inputEquationText, c)
                                        graphItems.add(item)
                                        selectedItemId = item.id
                                        inputEquationText = ""
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Tambah",
                                            tint = if (inputEquationText.isNotBlank()) Color.White else textSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Tambah",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (inputEquationText.isNotBlank()) Color.White else textSecondary
                                        )
                                    }
                                }
                            }

                            // ── Quick Math Keypad Strip (One-Tap Fast Input) ──
                            val mathKeys = remember {
                                listOf("x", "y", "x²", "x³", "+", "-", "·", "=", "/", "√", "sin", "cos", "tan", "(", ")", "π", "⌫", "C")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                mathKeys.forEach { key ->
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = if (key == "C" || key == "⌫") cardBg else inputBg,
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            if (key == "C" || key == "⌫") borderColor else borderColor
                                        ),
                                        modifier = Modifier.clickable {
                                            when (key) {
                                                "x" -> inputEquationText += "x"
                                                "y" -> inputEquationText += "y"
                                                "x²" -> inputEquationText += "^2"
                                                "x³" -> inputEquationText += "^3"
                                                "+" -> inputEquationText += "+"
                                                "-" -> inputEquationText += "-"
                                                "·" -> inputEquationText += "*"
                                                "=" -> inputEquationText += "="
                                                "/" -> inputEquationText += "/"
                                                "√" -> inputEquationText += "sqrt("
                                                "sin" -> inputEquationText += "sin("
                                                "cos" -> inputEquationText += "cos("
                                                "tan" -> inputEquationText += "tan("
                                                "(" -> inputEquationText += "("
                                                ")" -> inputEquationText += ")"
                                                "π" -> inputEquationText += "pi"
                                                "⌫" -> if (inputEquationText.isNotEmpty()) inputEquationText = inputEquationText.dropLast(1)
                                                "C" -> inputEquationText = ""
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 11.sp,
                                            fontWeight = if (key in listOf("x", "y", "x²", "x³", "π")) FontWeight.Bold else FontWeight.Medium,
                                            color = if (key == "C") Color(0xFFDC2626) else if (key == "⌫") Color(0xFFD97706) else textPrimary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                        )
                                    }
                                }
                            }

                            // Equations List Chips (Collapsible / Scrollable)
                            if (graphItems.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Intersections Toggle Pill when >= 2 curves are visible
                                    if (visibleCurvesDef.size >= 2) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (showIntersections) Color(0xFF2563EB).copy(alpha = 0.12f) else inputBg,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (showIntersections) Color(0xFF2563EB) else borderColor
                                            ),
                                            modifier = Modifier.clickable { showIntersections = !showIntersections }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = if (intersections.isNotEmpty()) "🎯 Potong (${intersections.size})" else "🎯 Potong",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (showIntersections) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (showIntersections) Color(0xFF2563EB) else textSecondary
                                                )
                                            }
                                        }
                                    }

                                    graphItems.forEachIndexed { index, item ->
                                        val isSelected = item.id == selectedItemId
                                        val chipBg = if (!item.isVisible) inputBg.copy(alpha = 0.5f) else if (isSelected) item.color.copy(alpha = 0.12f) else inputBg
                                        val chipBorder = if (!item.isVisible) borderColor.copy(alpha = 0.6f) else item.color

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = chipBg,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                                            modifier = Modifier.clickable {
                                                graphItems[index] = item.copy(isVisible = !item.isVisible)
                                                selectedItemId = item.id
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Equation text styled with curve color
                                                Text(
                                                    text = MathPlotHelper.formatFormulaForDisplay(item.displayLabel),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (item.isVisible) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (item.isVisible) item.color else textSecondary,
                                                    fontFamily = FontFamily.Monospace
                                                )

                                                // Delete button (✕)
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Hapus",
                                                    tint = textSecondary,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable {
                                                            graphItems.removeAt(index)
                                                            if (selectedItemId == item.id) {
                                                                selectedItemId = graphItems.firstOrNull()?.id
                                                            }
                                                        }
                                                )
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
                            .fillMaxWidth()
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

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val dx = pan.x * (worldSpanX / viewWidth)
                                        val dy = pan.y * (worldSpanY / viewHeight)
                                        centerWorldX -= dx
                                        centerWorldY += dy

                                        if (zoom != 1f) {
                                            worldSpanX = (worldSpanX / zoom).coerceIn(0.2f, 250f)
                                        }
                                    }
                                }
                        ) {
                            // CRITICAL GUARANTEE: wrap drawing within clipRect(0, 0, size.width, size.height)
                            // This ensures NOT A SINGLE PIXEL of curves, circles, or labels can penetrate header or borders!
                            clipRect(0f, 0f, size.width, size.height) {
                                // 1. Grid Lines
                                val firstGridX = (floor(xMin / gridStep) * gridStep).toFloat()
                                var gx = firstGridX
                                while (gx <= xMax + gridStep) {
                                    val sx = toScreenX(gx)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(sx, 0f),
                                        end = Offset(sx, viewHeight),
                                        strokeWidth = 1f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                    gx += gridStep
                                }

                                val firstGridY = (floor(yMin / gridStep) * gridStep).toFloat()
                                var gy = firstGridY
                                while (gy <= yMax + gridStep) {
                                    val sy = toScreenY(gy)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, sy),
                                        end = Offset(viewWidth, sy),
                                        strokeWidth = 1f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                    gy += gridStep
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
                                while (lx <= xMax + gridStep) {
                                    if (abs(lx) > 1e-4) {
                                        val sx = toScreenX(lx)
                                        val labelY = (originY + 22f).coerceIn(24f, viewHeight - 6f)
                                        nativeCanvas.drawText(MathPlotHelper.formatNumber(lx), sx, labelY, tickTextPaint)
                                    }
                                    lx += gridStep
                                }

                                val yTextPaint = android.graphics.Paint(tickTextPaint).apply {
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                                var ly = firstGridY
                                while (ly <= yMax + gridStep) {
                                    if (abs(ly) > 1e-4) {
                                        val sy = toScreenY(ly)
                                        val labelX = (originX - 7f).coerceIn(34f, viewWidth - 6f)
                                        nativeCanvas.drawText(MathPlotHelper.formatNumber(ly), labelX, sy + 6f, yTextPaint)
                                    }
                                    ly += gridStep
                                }

                                // 4. Draw All Visible Functions, Lines, and Circles Simultaneously on the Same Plane
                                for (item in graphItems) {
                                    if (!item.isVisible) continue

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
                                    } else if (item.isCircle && item.circleData != null) {
                                        val cData = item.circleData
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
                                    } else {
                                        // Function curve
                                        val pts = MathPlotHelper.sampleFunctionDynamic(item.cleanExpr, xMin, xMax, steps = 300)
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


