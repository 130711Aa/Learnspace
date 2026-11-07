package com.lernspace.app.ui.workspace.components

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
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.theme.LernspaceBorder
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.workspace.model.NoteTool

@Composable
fun NoteFloatingToolbar(
    activeTool: NoteTool,
    onSelectTool: (NoteTool) -> Unit,
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
        Color(0xFF16A34A), // Green (matching user's reference photo)
        Color(0xFFEAB308), // Yellow / Highlighter
        Color(0xFF9333EA)  // Purple
    )

    val thicknessPresets = listOf(
        3.5f to 4.dp,   // Thin
        7f to 8.dp,     // Medium
        14f to 12.dp    // Thick
    )

    Surface(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp)),
        color = Color.White.copy(alpha = 0.96f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    .height(24.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // Color Swatches
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colorPresets.forEach { color ->
                    val isChosen = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(24.dp)
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
                                    .size(6.dp)
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
                    .height(24.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // Stroke Thickness Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                thicknessPresets.forEach { (sizeFloat, dotDp) ->
                    val isChosen = selectedThickness == sizeFloat
                    Box(
                        modifier = Modifier
                            .size(26.dp)
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
            .size(36.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
