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
    panXProvider: () -> Float = { 0f },
    panYProvider: () -> Float = { 0f },
    isDarkPaper: Boolean = false,
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
    val sideMarginDp = 16.dp
    val topMarginDp = 34.dp
    val sideMarginPx = with(density) { sideMarginDp.toPx() }
    val topMarginPx = with(density) { topMarginDp.toPx() }

    val screenWidthDp = with(density) { (worldWidth * scale).toDp() }
    val screenHeightDp = with(density) { (worldHeight * scale).toDp() }
    val dynamicFontSize = with(density) { ((28f * scale) / density.fontScale.coerceAtLeast(0.5f)).toSp() }
    val dynamicLineHeight = with(density) { ((40f * scale) / density.fontScale.coerceAtLeast(0.5f)).toSp() }
    val constantTextPad = with(density) { (6f * scale).toDp().coerceIn(3.dp, 12.dp) }

    // Outer Box reserves margin when selected for corner move/delete pills and edge resize handles,
    // while keeping the TextBox body strictly at (worldX, worldY) with ZERO text jump on deselect.
    Box(
        modifier = modifier
            .offset {
                val padX = if (box.isSelected) sideMarginPx else 0f
                val padY = if (box.isSelected) topMarginPx else 0f
                val screenX = worldX * scale + panXProvider() - padX
                val screenY = worldY * scale + panYProvider() - padY
                IntOffset(screenX.roundToInt(), screenY.roundToInt())
            }
            .width(screenWidthDp + if (box.isSelected) sideMarginDp * 2 else 0.dp)
            .height(screenHeightDp + if (box.isSelected) topMarginDp + sideMarginDp else 0.dp)
    ) {
        // Main TextBox Surface anchored strictly at (worldX, worldY) with exact worldWidth x worldHeight
        Box(
            modifier = Modifier
                .padding(
                    start = if (box.isSelected) sideMarginDp else 0.dp,
                    top = if (box.isSelected) topMarginDp else 0.dp
                )
                .width(screenWidthDp)
                .height(screenHeightDp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (box.isSelected) {
                            Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .border(1.5.dp, selectionColor, RoundedCornerShape(6.dp))
                        } else if (textContent.isEmpty()) {
                            Modifier.border(1.dp, if (isDarkPaper) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    ),
                color = if (box.isSelected) (if (isDarkPaper) Color(0xFF1E293B).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ) {
                // Inner Text Display / Input Area with IDENTICAL padding in both states -> ZERO jump!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(constantTextPad)
                ) {
                    if (box.isSelected) {
                        if (textContent.isEmpty()) {
                            Text(
                                text = "Ketik catatan...",
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
                                color = if (isDarkPaper) Color(0xFFF8FAFC) else LernspaceDark,
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicLineHeight
                            ),
                            cursorBrush = SolidColor(selectionColor),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        if (textContent.isNotEmpty()) {
                            Text(
                                text = textContent,
                                style = TextStyle(
                                    color = if (isDarkPaper) Color(0xFFF8FAFC) else LernspaceDark,
                                    fontSize = dynamicFontSize,
                                    lineHeight = dynamicLineHeight
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "Teks kosong",
                                color = Color(0xFF94A3B8),
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicLineHeight
                            )
                        }
                    }
                }
            }

            // When unselected: tap selects, drag anywhere moves box directly!
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
        }

        // Floating Small Controls & Edge Resize Handles (Shown only when selected)
        if (box.isSelected) {
            // Left Border Drag Zone (drag along the left border edge to move)
            Box(
                modifier = Modifier
                    .padding(start = sideMarginDp, top = topMarginDp)
                    .align(Alignment.TopStart)
                    .width(14.dp)
                    .height(screenHeightDp)
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

            // Small Word-style circular Drag Button centered neatly ABOVE the top border
            // Visual circle stays tiny (22.dp) with clean gap above border, while touch target is generous (56x34.dp)!
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 56.dp, height = 34.dp)
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
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, selectionColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenWith,
                        contentDescription = "Geser TextBox",
                        tint = selectionColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Small Top-Right Delete Circle with generous 36x32dp tap target
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 2.dp)
                    .size(width = 36.dp, height = 32.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(1.2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Right-Edge Horizontal Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = topMarginDp / 2f)
                    .size(width = 32.dp, height = 48.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                worldWidth = maxOf(120f, worldWidth + dragAmount.x / scale)
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
                    .size(width = 48.dp, height = 32.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                worldHeight = maxOf(64f, worldHeight + dragAmount.y / scale)
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
                    .size(34.dp)
                    .pointerInput(box.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                worldWidth = maxOf(120f, worldWidth + dragAmount.x / scale)
                                worldHeight = maxOf(64f, worldHeight + dragAmount.y / scale)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, selectionColor, CircleShape)
                )
            }
        }
    }
}
