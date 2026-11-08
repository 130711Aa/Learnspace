package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
    scale: Float = 1f,
    panX: Float = 0f,
    panY: Float = 0f,
    onCommitMove: (Float, Float) -> Unit,
    onCommitResize: (Float, Float) -> Unit,
    onTextChange: (String) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // World coordinates (stored in TextBoxElement)
    var worldX by remember(box.id) { mutableFloatStateOf(box.x) }
    var worldY by remember(box.id) { mutableFloatStateOf(box.y) }
    var worldWidth by remember(box.id) { mutableFloatStateOf(box.width) }
    var worldHeight by remember(box.id) { mutableFloatStateOf(box.height) }
    var textContent by remember(box.id) { mutableStateOf(box.text) }

    LaunchedEffect(box.x, box.y) {
        worldX = box.x
        worldY = box.y
    }
    LaunchedEffect(box.width, box.height) {
        worldWidth = box.width
        worldHeight = box.height
    }
    LaunchedEffect(box.text) {
        if (textContent != box.text) {
            textContent = box.text
        }
    }

    // Auto focus text cursor and show keyboard when selected; clear when unselected
    LaunchedEffect(box.isSelected) {
        if (box.isSelected) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // ignore
            }
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val selectionColor = Color(0xFF2563EB) // MS Word Selection Blue

    val dynamicFontSize = (16 * scale.coerceIn(0.7f, 2.2f)).sp
    val dynamicLineHeight = (24 * scale.coerceIn(0.7f, 2.2f)).sp

    Box(
        modifier = modifier
            .offset {
                val screenX = worldX * scale + panX
                val screenY = worldY * scale + panY
                IntOffset(screenX.roundToInt(), screenY.roundToInt())
            }
            .width((worldWidth * scale).dp)
            .height((worldHeight * scale).dp)
    ) {
        // Main Box Container
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (box.isSelected) {
                        Modifier
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                            .border(1.5.dp, selectionColor, RoundedCornerShape(6.dp))
                    } else if (textContent.isEmpty()) {
                        Modifier.border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                    } else {
                        Modifier
                    }
                ),
            color = if (box.isSelected) Color.White.copy(alpha = 0.98f) else Color.Transparent,
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // MS Word Style Top Drag Header (Visible only when selected)
                if (box.isSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color(0xFFEFF6FF))
                            .pointerInput(box.id, scale) {
                                detectDragGestures(
                                    onDragEnd = { onCommitMove(worldX, worldY) },
                                    onDragCancel = { onCommitMove(worldX, worldY) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        worldX += dragAmount.x / scale
                                        worldY += dragAmount.y / scale
                                    }
                                )
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = "Drag to move",
                                tint = selectionColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Text Box",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = selectionColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .clickable { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Inner Text Display / Input Area (Vector FreeType fonts - Zero Blur)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(if (box.isSelected) 8.dp else 4.dp)
                ) {
                    if (box.isSelected) {
                        // EDIT MODE: BasicTextField with active cursor and keyboard
                        if (textContent.isEmpty()) {
                            Text(
                                text = "Type notes here...",
                                color = Color(0xFF94A3B8),
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicLineHeight
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
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicLineHeight
                            ),
                            cursorBrush = SolidColor(selectionColor),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        // UNSELECTED MODE: Pure Text - ZERO cursor, ZERO focus
                        if (textContent.isNotEmpty()) {
                            Text(
                                text = textContent,
                                style = TextStyle(
                                    color = LernspaceDark,
                                    fontSize = dynamicFontSize,
                                    lineHeight = dynamicLineHeight
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "Empty note",
                                color = Color(0xFF94A3B8),
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicLineHeight
                            )
                        }
                    }
                }
            }
        }

        // Tap or Drag to Move when UNSELECTED:
        if (!box.isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(box.id, scale) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var moved = false
                            var overSlop = Offset.Zero
                            val dragChange = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                change.consume()
                                overSlop = over
                                moved = true
                            }
                            if (dragChange != null && moved) {
                                onSelect()
                                worldX += overSlop.x / scale
                                worldY += overSlop.y / scale
                                drag(dragChange.id) { change ->
                                    change.consume()
                                    worldX += change.positionChange().x / scale
                                    worldY += change.positionChange().y / scale
                                }
                                onCommitMove(worldX, worldY)
                            } else {
                                onSelect()
                            }
                        }
                    }
            )
        }

        // 120 FPS Resizing Handles & Border Drag Zones (Shown only when selected)
        if (box.isSelected) {
            // Left Border Drag Zone to Move Box
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(12.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitMove(worldX, worldY) },
                            onDragCancel = { onCommitMove(worldX, worldY) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                worldX += dragAmount.x / scale
                                worldY += dragAmount.y / scale
                            }
                        )
                    }
            )

            // Right-Edge Horizontal Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 16.dp)
                    .size(width = 36.dp, height = 48.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDp = (dragAmount.x / density.density) / scale
                                worldWidth = maxOf(100f, worldWidth + deltaDp)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 24.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(1.5.dp, selectionColor, RoundedCornerShape(4.dp))
                )
            }

            // Bottom-Edge Vertical Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 16.dp)
                    .size(width = 48.dp, height = 36.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDp = (dragAmount.y / density.density) / scale
                                worldHeight = maxOf(48f, worldHeight + deltaDp)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 8.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(1.5.dp, selectionColor, RoundedCornerShape(4.dp))
                )
            }

            // Bottom-Right Corner Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 14.dp)
                    .size(44.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaX = (dragAmount.x / density.density) / scale
                                val deltaY = (dragAmount.y / density.density) / scale
                                worldWidth = maxOf(100f, worldWidth + deltaX)
                                worldHeight = maxOf(48f, worldHeight + deltaY)
                            }
                        )
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
