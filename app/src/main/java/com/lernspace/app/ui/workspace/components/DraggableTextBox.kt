package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Local reactive state for smooth real-time 120Hz gesture interaction
    var offsetX by remember(box.id) { mutableFloatStateOf(box.x) }
    var offsetY by remember(box.id) { mutableFloatStateOf(box.y) }
    var widthDp by remember(box.id) { mutableFloatStateOf(box.width) }
    var heightDp by remember(box.id) { mutableFloatStateOf(box.height) }
    var textContent by remember(box.id) { mutableStateOf(box.text) }

    // Synchronize if parent state changes
    LaunchedEffect(box.x, box.y) {
        offsetX = box.x
        offsetY = box.y
    }
    LaunchedEffect(box.width, box.height) {
        widthDp = box.width
        heightDp = box.height
    }
    LaunchedEffect(box.text) {
        if (textContent != box.text) {
            textContent = box.text
        }
    }

    // Auto focus text cursor and show keyboard when selected
    LaunchedEffect(box.isSelected) {
        if (box.isSelected) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val selectionColor = Color(0xFF2563EB) // MS Word Accent Blue

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(widthDp.dp)
            .height(heightDp.dp)
    ) {
        // 1. Main Box Container
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (box.isSelected) {
                        Modifier
                            .shadow(4.dp, RoundedCornerShape(6.dp))
                            .border(1.5.dp, selectionColor, RoundedCornerShape(6.dp))
                    } else {
                        Modifier
                    }
                ),
            color = if (box.isSelected) Color.White.copy(alpha = 0.95f) else Color.Transparent,
            shape = RoundedCornerShape(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (box.isSelected) 10.dp else 4.dp)
            ) {
                if (textContent.isEmpty() && box.isSelected) {
                    Text(
                        text = "Type notes here...",
                        color = Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }

                BasicTextField(
                    value = textContent,
                    onValueChange = { newText ->
                        textContent = newText
                        onTextChange(newText)
                    },
                    textStyle = TextStyle(
                        color = LernspaceDark,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(selectionColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                )
            }
        }

        // 2. CRITICAL FIX: Click-to-Select Overlay when Unselected
        // When unselected, any tap on the text immediately re-selects it and reveals the Word frame and handles!
        if (!box.isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSelect()
                    }
            )
        }

        // 3. Active Selection Overlays (Floating Move Pill, Delete Button, and Big Touch Resize Handles)
        if (box.isSelected) {
            // Move Handle: Elevated pill with generous 38dp clearance above border
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .size(width = 76.dp, height = 30.dp)
                    .shadow(4.dp, RoundedCornerShape(15.dp))
                    .clip(RoundedCornerShape(15.dp))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenWith,
                        contentDescription = "Move box",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Move",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Delete Button (Top-Right, aligned with Move handle)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-40).dp)
                    .size(30.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete text box",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Right-Edge Horizontal Resize Handle (38dp wide hit area)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp)
                    .size(width = 38.dp, height = 48.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.x / density.density
                            widthDp = maxOf(100f, widthDp + deltaDp)
                            onResize(widthDp, heightDp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 26.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(1.5.dp, selectionColor, RoundedCornerShape(4.dp))
                )
            }

            // Bottom-Edge Vertical Resize Handle (38dp high hit area)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 18.dp)
                    .size(width = 48.dp, height = 38.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.y / density.density
                            heightDp = maxOf(48f, heightDp + deltaDp)
                            onResize(widthDp, heightDp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = 8.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(1.5.dp, selectionColor, RoundedCornerShape(4.dp))
                )
            }

            // Bottom-Right Corner Resize Handle (Diagonal Resize, 44dp x 44dp comfortable target)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 16.dp)
                    .size(44.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaX = dragAmount.x / density.density
                            val deltaY = dragAmount.y / density.density
                            widthDp = maxOf(100f, widthDp + deltaX)
                            heightDp = maxOf(48f, heightDp + deltaY)
                            onResize(widthDp, heightDp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, selectionColor, CircleShape)
                )
            }
        }
    }
}
