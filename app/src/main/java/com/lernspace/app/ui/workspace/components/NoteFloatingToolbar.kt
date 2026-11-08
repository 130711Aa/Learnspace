package com.lernspace.app.ui.workspace.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.LernspaceBorder
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.workspace.model.GeometricShapeType
import com.lernspace.app.ui.workspace.model.NoteTool

@Composable
fun NoteFloatingToolbar(
    activeTool: NoteTool,
    onSelectTool: (NoteTool) -> Unit,
    selectedShapeType: GeometricShapeType = GeometricShapeType.RECTANGLE,
    onSelectShapeType: (GeometricShapeType) -> Unit = {},
    isAutoRefineEnabled: Boolean = false,
    onToggleAutoRefine: () -> Unit = {},
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    selectedThickness: Float,
    onSelectThickness: (Float) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorPresets = listOf(
        Color(0xFF0F172A), // Dark slate / Black
        Color(0xFF2563EB), // Blue
        Color(0xFFDC2626), // Red
        Color(0xFF16A34A), // Green
        Color(0xFFEAB308), // Yellow / Highlighter
        Color(0xFF9333EA)  // Purple
    )

    val thicknessPresets = listOf(
        3.5f to 4.dp,   // Thin
        7f to 8.dp,     // Medium
        14f to 12.dp    // Thick
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Main Tools Capsule ──
        Surface(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp))
                .border(1.dp, LernspaceBorder, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp)),
            color = Color.White.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pen Tool
                ToolIconButton(
                    icon = Icons.Filled.Edit,
                    label = "Pen",
                    isSelected = activeTool == NoteTool.PEN,
                    onClick = { onSelectTool(NoteTool.PEN) }
                )

                // Highlighter Tool
                ToolIconButton(
                    icon = Icons.Filled.BorderColor,
                    label = "Highlighter",
                    isSelected = activeTool == NoteTool.HIGHLIGHTER,
                    onClick = { onSelectTool(NoteTool.HIGHLIGHTER) }
                )

                // Eraser Tool
                ToolIconButton(
                    icon = Icons.Filled.AutoFixNormal,
                    label = "Eraser",
                    isSelected = activeTool == NoteTool.ERASER,
                    onClick = { onSelectTool(NoteTool.ERASER) }
                )

                // Shape Tool (Geometri)
                ToolIconButton(
                    icon = Icons.Filled.Category,
                    label = "Bentuk Geometri",
                    isSelected = activeTool == NoteTool.SHAPE,
                    onClick = { onSelectTool(NoteTool.SHAPE) }
                )

                // Auto-Refine Shape Toggle (Magic Wand - Smart Auto-Snap)
                Surface(
                    shape = CircleShape,
                    color = if (isAutoRefineEnabled) Color(0xFFFEF3C7) else Color.Transparent,
                    border = if (isAutoRefineEnabled) BorderStroke(1.dp, Color(0xFFFDE68A)) else null,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable(onClick = onToggleAutoRefine)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = "Bentuk Rapi Otomatis",
                            tint = if (isAutoRefineEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Lasso Tool (Seleksi & Pindahkan)
                ToolIconButton(
                    icon = Icons.Filled.Gesture,
                    label = "Lasso (Seleksi & Geser)",
                    isSelected = activeTool == NoteTool.LASSO,
                    onClick = { onSelectTool(NoteTool.LASSO) }
                )

                // Text Box Tool
                ToolIconButton(
                    icon = Icons.Filled.TextFields,
                    label = "Text Box",
                    isSelected = activeTool == NoteTool.TEXT,
                    onClick = { onSelectTool(NoteTool.TEXT) }
                )

                // Image Tool
                ToolIconButton(
                    icon = Icons.Filled.Image,
                    label = "Add Image",
                    isSelected = activeTool == NoteTool.IMAGE,
                    onClick = {
                        onSelectTool(NoteTool.IMAGE)
                        onPickImage()
                    }
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Color Swatches
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPresets.forEach { color ->
                        val isChosen = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isChosen) 2.dp else 1.dp,
                                    color = if (isChosen) LernspaceDark else Color(0x33000000),
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Stroke Thickness Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    thicknessPresets.forEach { (sizeFloat, dotDp) ->
                        val isChosen = selectedThickness == sizeFloat
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isChosen) Color(0xFFF1F5F9) else Color.Transparent)
                                .clickable { onSelectThickness(sizeFloat) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotDp)
                                    .clip(CircleShape)
                                    .background(if (isChosen) LernspaceDark else Color(0xFF94A3B8))
                            )
                        }
                    }
                }
            }
        }

        // ── Secondary Sub-Bar: Geometric Shape Picker (Visible when SHAPE tool is active) ──
        AnimatedVisibility(
            visible = activeTool == NoteTool.SHAPE,
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
                                        GeometricShapeType.ARROW -> Icons.Filled.TrendingFlat
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

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) LernspaceDark else Color.Transparent,
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
