package com.lernspace.app.ui.workspace.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.LernspaceBorder
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.workspace.model.GeometricShapeType
import com.lernspace.app.ui.workspace.model.NoteTool
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteFloatingToolbar(
    activeTool: NoteTool,
    onSelectTool: (NoteTool) -> Unit,
    selectedShapeType: GeometricShapeType = GeometricShapeType.RECTANGLE,
    onSelectShapeType: (GeometricShapeType) -> Unit = {},
    penColor: Color,
    onSelectPenColor: (Color) -> Unit,
    penThickness: Float,
    onSelectPenThickness: (Float) -> Unit,
    highlighterColor: Color,
    onSelectHighlighterColor: (Color) -> Unit,
    highlighterThickness: Float,
    onSelectHighlighterThickness: (Float) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Independent color presets for popovers
    val penColorPresets = remember {
        listOf(
            Color(0xFF0F172A), // Dark Slate / Black
            Color(0xFF2563EB), // Royal Blue
            Color(0xFFDC2626), // Crimson Red
            Color(0xFF16A34A), // Forest Green
            Color(0xFF9333EA), // Purple
            Color(0xFFEA580C)  // Amber / Orange
        )
    }

    val highlighterColorPresets = remember {
        listOf(
            Color(0xFFFDE047), // Soft Lemon Yellow
            Color(0xFF86EFAC), // Soft Sage Green
            Color(0xFF7DD3FC), // Soft Sky Blue
            Color(0xFFF472B6), // Soft Coral Pink
            Color(0xFFFDBA74), // Soft Peach Orange
            Color(0xFFC084FC)  // Soft Lavender Purple
        )
    }

    // Active tool configuration card state (opened via tap-again or long-press)
    var openConfigTool by remember { mutableStateOf<NoteTool?>(null) }

    val isHighlighter = activeTool == NoteTool.HIGHLIGHTER
    val activeThickness = if (isHighlighter) highlighterThickness else penThickness

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Main Tools Capsule (Clean, Minimal, No redundant color chips) ──
        Surface(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp))
                .border(1.dp, LernspaceBorder, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp)),
            color = Color.White.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Pen Tool (Circle background IS the active penColor!)
                ToolIconButton(
                    icon = Icons.Filled.Edit,
                    label = "Pen",
                    isSelected = activeTool == NoteTool.PEN,
                    activeColor = penColor,
                    inactiveColorBadge = penColor,
                    isConfigOpen = openConfigTool == NoteTool.PEN,
                    onClick = {
                        if (activeTool == NoteTool.PEN) {
                            openConfigTool = if (openConfigTool == NoteTool.PEN) null else NoteTool.PEN
                        } else {
                            onSelectTool(NoteTool.PEN)
                            openConfigTool = null
                        }
                    },
                    onLongClick = {
                        onSelectTool(NoteTool.PEN)
                        openConfigTool = NoteTool.PEN
                    }
                )

                // 2. Highlighter Tool (Circle background IS the active highlighterColor!)
                ToolIconButton(
                    icon = Icons.Filled.BorderColor,
                    label = "Highlighter",
                    isSelected = activeTool == NoteTool.HIGHLIGHTER,
                    activeColor = highlighterColor,
                    inactiveColorBadge = highlighterColor,
                    isConfigOpen = openConfigTool == NoteTool.HIGHLIGHTER,
                    onClick = {
                        if (activeTool == NoteTool.HIGHLIGHTER) {
                            openConfigTool = if (openConfigTool == NoteTool.HIGHLIGHTER) null else NoteTool.HIGHLIGHTER
                        } else {
                            onSelectTool(NoteTool.HIGHLIGHTER)
                            openConfigTool = null
                        }
                    },
                    onLongClick = {
                        onSelectTool(NoteTool.HIGHLIGHTER)
                        openConfigTool = NoteTool.HIGHLIGHTER
                    }
                )

                // 3. Eraser Tool
                ToolIconButton(
                    icon = Icons.Filled.AutoFixNormal,
                    label = "Eraser",
                    isSelected = activeTool == NoteTool.ERASER,
                    activeColor = LernspaceDark,
                    onClick = {
                        openConfigTool = null
                        onSelectTool(NoteTool.ERASER)
                    }
                )

                // 4. Shape Tool (Geometri)
                ToolIconButton(
                    icon = Icons.Filled.Category,
                    label = "Bentuk Geometri",
                    isSelected = activeTool == NoteTool.SHAPE,
                    activeColor = LernspaceDark,
                    onClick = {
                        openConfigTool = null
                        onSelectTool(NoteTool.SHAPE)
                    }
                )

                // 5. Lasso Tool (Seleksi & Pindahkan)
                ToolIconButton(
                    icon = Icons.Filled.Gesture,
                    label = "Lasso (Seleksi & Geser)",
                    isSelected = activeTool == NoteTool.LASSO,
                    activeColor = LernspaceDark,
                    onClick = {
                        openConfigTool = null
                        onSelectTool(NoteTool.LASSO)
                    }
                )

                // 6. Text Box Tool
                ToolIconButton(
                    icon = Icons.Filled.TextFields,
                    label = "Text Box",
                    isSelected = activeTool == NoteTool.TEXT,
                    activeColor = LernspaceDark,
                    onClick = {
                        openConfigTool = null
                        onSelectTool(NoteTool.TEXT)
                    }
                )

                // 7. Image Tool
                ToolIconButton(
                    icon = Icons.Filled.Image,
                    label = "Add Image",
                    isSelected = activeTool == NoteTool.IMAGE,
                    activeColor = LernspaceDark,
                    onClick = {
                        openConfigTool = null
                        onSelectTool(NoteTool.IMAGE)
                        onPickImage()
                    }
                )

                // Clean Divider before Thickness Badge (only when Pen or Highlighter is active)
                if (activeTool == NoteTool.PEN || activeTool == NoteTool.HIGHLIGHTER) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    // Minimal Thickness Badge (Tap to toggle px slider popover)
                    val isCurrentConfigOpen = openConfigTool == (if (isHighlighter) NoteTool.HIGHLIGHTER else NoteTool.PEN)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentConfigOpen) AccentBlue.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isCurrentConfigOpen) AccentBlue else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            val target = if (isHighlighter) NoteTool.HIGHLIGHTER else NoteTool.PEN
                            openConfigTool = if (openConfigTool == target) null else target
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "${activeThickness.roundToInt()} px",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentConfigOpen) AccentBlue else LernspaceDark
                            )
                        }
                    }
                }
            }
        }

        // ── Tool Configuration Card (Opened via Tap-Again, Long-Press, or Thickness Pill) ──
        AnimatedVisibility(
            visible = openConfigTool != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (openConfigTool) {
                NoteTool.PEN -> {
                    ToolConfigCard(
                        title = "Pengaturan Pen",
                        icon = Icons.Filled.Edit,
                        iconTint = penColor,
                        colorPresets = penColorPresets,
                        selectedColor = penColor,
                        onSelectColor = onSelectPenColor,
                        thickness = penThickness,
                        minThickness = 1f,
                        maxThickness = 20f,
                        onSelectThickness = onSelectPenThickness,
                        presetSizes = listOf(2f, 4f, 7f, 12f, 18f),
                        isHighlighter = false,
                        onClose = { openConfigTool = null }
                    )
                }
                NoteTool.HIGHLIGHTER -> {
                    ToolConfigCard(
                        title = "Pengaturan Highlighter",
                        icon = Icons.Filled.BorderColor,
                        iconTint = Color(0xFFCA8A04),
                        colorPresets = highlighterColorPresets,
                        selectedColor = highlighterColor,
                        onSelectColor = onSelectHighlighterColor,
                        thickness = highlighterThickness,
                        minThickness = 10f,
                        maxThickness = 60f,
                        onSelectThickness = onSelectHighlighterThickness,
                        presetSizes = listOf(16f, 24f, 36f, 48f),
                        isHighlighter = true,
                        onClose = { openConfigTool = null }
                    )
                }
                else -> Unit
            }
        }

        // ── Secondary Sub-Bar: Geometric Shape Picker (Visible when SHAPE tool is active) ──
        AnimatedVisibility(
            visible = activeTool == NoteTool.SHAPE && openConfigTool == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.98f),
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GeometricShapeType.values().forEach { shape ->
                        val isSelected = selectedShapeType == shape
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) LernspaceDark else Color(0xFFF8FAFC),
                            border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { onSelectShapeType(shape) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (shape) {
                                        GeometricShapeType.LINE -> Icons.Filled.HorizontalRule
                                        GeometricShapeType.RECTANGLE -> Icons.Filled.CropSquare
                                        GeometricShapeType.CIRCLE -> Icons.Filled.RadioButtonUnchecked
                                        GeometricShapeType.TRIANGLE -> Icons.Filled.ChangeHistory
                                        GeometricShapeType.ARROW -> Icons.AutoMirrored.Filled.TrendingFlat
                                    },
                                    contentDescription = shape.label,
                                    tint = if (isSelected) Color.White else LernspaceDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = shape.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else LernspaceDark
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
 * Dedicated Tool Configuration Popover Card
 * with live interactive thickness slider (in px) and color presets.
 */
@Composable
private fun ToolConfigCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    colorPresets: List<Color>,
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    thickness: Float,
    minThickness: Float,
    maxThickness: Float,
    onSelectThickness: (Float) -> Unit,
    presetSizes: List<Float>,
    isHighlighter: Boolean,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.98f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.widthIn(min = 340.dp, max = 390.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Icon + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onClose)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Section 1: Color Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isHighlighter) "Pilih Warna Stabilo" else "Pilih Warna Tinta",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPresets.forEach { color ->
                        val isChosen = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) LernspaceDark else Color(0x33000000),
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(getContrastingIconColor(color))
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Section 2: Thickness Slider with Live Stroke Preview
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ketebalan Garis",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${thickness.roundToInt()} px",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Live Visual Stroke Preview Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height / 2f
                        val startX = 24.dp.toPx()
                        val endX = size.width - 24.dp.toPx()
                        val strokePx = thickness.dp.toPx().coerceIn(2f, 32.dp.toPx())

                        if (isHighlighter) {
                            val rectHeight = strokePx.coerceAtLeast(10f)
                            drawRoundRect(
                                color = selectedColor.copy(alpha = 0.45f),
                                topLeft = Offset(startX, midY - rectHeight / 2f),
                                size = Size(endX - startX, rectHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        } else {
                            drawLine(
                                color = selectedColor,
                                start = Offset(startX, midY),
                                end = Offset(endX, midY),
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Slider
                Slider(
                    value = thickness,
                    onValueChange = { onSelectThickness((it * 2).roundToInt() / 2f) },
                    valueRange = minThickness..maxThickness,
                    colors = SliderDefaults.colors(
                        thumbColor = LernspaceDark,
                        activeTrackColor = AccentBlue,
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )

                // Quick Size Presets Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetSizes.forEach { preset ->
                        val isSelected = (thickness - preset).let { kotlin.math.abs(it) < 0.8f }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) LernspaceDark else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { onSelectThickness(preset) }
                        ) {
                            Text(
                                text = "${preset.roundToInt()} px",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean tool icon button whose circular background directly reflects
 * the ACTIVE COLOR when selected, eliminating redundant color chips!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color = LernspaceDark,
    inactiveColorBadge: Color? = null,
    isConfigOpen: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val backgroundColor = when {
        isSelected -> activeColor
        else -> Color.Transparent
    }

    val iconColor = when {
        isSelected -> getContrastingIconColor(activeColor)
        else -> Color(0xFF475569)
    }

    Surface(
        shape = CircleShape,
        color = backgroundColor,
        border = if (isSelected) {
            BorderStroke(1.5.dp, if (isConfigOpen) AccentBlue else Color(0x33000000))
        } else null,
        modifier = Modifier
            .size(34.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(17.dp)
            )

            // When inactive, show a tiny dot below the tool to indicate its configured color
            if (!isSelected && inactiveColorBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.5.dp)
                        .size(4.dp)
                        .background(inactiveColorBadge, CircleShape)
                        .border(0.5.dp, Color(0x33000000), CircleShape)
                )
            }
        }
    }
}

/**
 * Computes high-contrast color (white or dark slate) depending on background luminance.
 */
private fun getContrastingIconColor(bgColor: Color): Color {
    val luminance = 0.299f * bgColor.red + 0.587f * bgColor.green + 0.114f * bgColor.blue
    return if (luminance > 0.52f) Color(0xFF0F172A) else Color.White
}
