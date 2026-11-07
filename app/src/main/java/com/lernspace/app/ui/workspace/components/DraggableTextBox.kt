package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.workspace.model.TextBoxElement
import kotlin.math.roundToInt

@Composable
fun DraggableTextBox(
    box: TextBoxElement,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onTextChange: (String) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var offsetX by remember(box.x) { mutableFloatStateOf(box.x) }
    var offsetY by remember(box.y) { mutableFloatStateOf(box.y) }
    var widthDp by remember(box.width) { mutableFloatStateOf(box.width) }
    var heightDp by remember(box.height) { mutableFloatStateOf(box.height) }

    val selectionColor = Color(0xFF2563EB) // Word-style accent blue

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clickable { onSelect() }
    ) {
        // Main Box Container
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (box.isSelected) {
                        Modifier
                            .shadow(3.dp, RoundedCornerShape(4.dp))
                            .border(1.5.dp, selectionColor, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }
                ),
            color = if (box.isSelected) Color.White.copy(alpha = 0.95f) else Color.Transparent,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (box.isSelected) 8.dp else 4.dp)
            ) {
                // Interior Text Area (word wrap, takes full space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (box.text.isEmpty()) {
                        Text(
                            text = "Type text here...",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                    BasicTextField(
                        value = box.text,
                        onValueChange = {
                            onSelect()
                            onTextChange(it)
                        },
                        textStyle = TextStyle(
                            color = LernspaceDark,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(LernspaceDark),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onSelect() }
                    )
                }
            }
        }

        // Selected State Overlays (Move Grip, Delete button, and Corner Resize Handles)
        if (box.isSelected) {
            // 1. Move Drag Handle (Top-Center pill)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(width = 32.dp, height = 18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(selectionColor)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            onMove(offsetX, offsetY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenWith,
                    contentDescription = "Move text box",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // 2. Delete Button (Top-Right corner circle)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete text box",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            // 3. Right-Edge Horizontal Resize Handle (Mid-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp)
                    .size(width = 12.dp, height = 24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
                    .border(1.5.dp, selectionColor, RoundedCornerShape(3.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.x / density.density
                            widthDp = maxOf(100f, widthDp + deltaDp)
                            onResize(widthDp, heightDp)
                        }
                    }
            )

            // 4. Bottom-Edge Vertical Resize Handle (Bottom-Center)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .size(width = 24.dp, height = 12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
                    .border(1.5.dp, selectionColor, RoundedCornerShape(3.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.y / density.density
                            heightDp = maxOf(45f, heightDp + deltaDp)
                            onResize(widthDp, heightDp)
                        }
                    }
            )

            // 5. Bottom-Right Corner Resize Handle (Diagonal Resize)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, selectionColor, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaX = dragAmount.x / density.density
                            val deltaY = dragAmount.y / density.density
                            widthDp = maxOf(100f, widthDp + deltaX)
                            heightDp = maxOf(45f, heightDp + deltaY)
                            onResize(widthDp, heightDp)
                        }
                    }
            )
        }
    }
}
