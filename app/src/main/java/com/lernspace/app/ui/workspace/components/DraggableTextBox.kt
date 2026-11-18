package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.workspace.model.TextBoxAlign
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
    val worldWidthDp = with(density) { worldWidth.toDp() }
    val worldHeightDp = with(density) { worldHeight.toDp() }
    val worldFontSize = box.fontSize.coerceIn(12f, 120f)
    // Keep text layout in unscaled world units and let GPU graphicsLayer scale/translate it.
    // This prevents per-frame text re-measurement, line-wrap shifts, and subpixel jitter during zoom/pan.
    val baseFontSize = with(density) { (worldFontSize / density.fontScale.coerceAtLeast(0.5f)).toSp() }
    val baseLineHeight = with(density) { ((worldFontSize * 1.44f) / density.fontScale.coerceAtLeast(0.5f)).toSp() }
    val baseTextPad = with(density) { 8f.toDp() }
    val textScrollState = rememberScrollState()
    val isScrollable = textScrollState.maxValue > 0
    val resolvedTextColor = box.colorArgb?.let { Color(it) } ?: if (isDarkPaper) Color(0xFFF8FAFC) else LernspaceDark
    val resolvedWeight = if (box.isBold) FontWeight.Bold else FontWeight.Normal
    val resolvedStyle = if (box.isItalic) FontStyle.Italic else FontStyle.Normal
    val resolvedAlign = when (box.textAlign) {
        TextBoxAlign.LEFT -> TextAlign.Start
        TextBoxAlign.CENTER -> TextAlign.Center
        TextBoxAlign.RIGHT -> TextAlign.End
    }

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = worldX * scale + panXProvider()
                translationY = worldY * scale + panYProvider()
            }
            .requiredSize(width = worldWidthDp, height = worldHeightDp)
    ) {
            val boxBgColor = when {
                box.isSelected -> if (isDarkPaper) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
                box.hasBackground -> if (isDarkPaper) Color(0xFF1E293B).copy(alpha = 0.88f) else Color(0xFFF8FAFC).copy(alpha = 0.94f)
                else -> Color.Transparent
            }
            val boxBorderModifier = when {
                box.isSelected -> Modifier
                    .shadow(2.dp, RoundedCornerShape(6.dp))
                    .border(1.5.dp, selectionColor, RoundedCornerShape(6.dp))
                box.hasBackground -> Modifier
                    .shadow(1.dp, RoundedCornerShape(8.dp))
                    .border(1.dp, if (isDarkPaper) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                textContent.isEmpty() -> Modifier
                    .border(1.dp, if (isDarkPaper) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                else -> Modifier
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .then(boxBorderModifier),
                color = boxBgColor,
                shape = RoundedCornerShape(if (box.hasBackground && !box.isSelected) 8.dp else 6.dp)
            ) {
                // Inner Text Display / Input Area with IDENTICAL padding in both states -> ZERO jump!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .padding(baseTextPad)
                ) {
                    if (box.isSelected) {
                        if (textContent.isEmpty()) {
                            Text(
                                text = "Ketik catatan...",
                                color = Color(0xFF94A3B8),
                                fontSize = baseFontSize,
                                lineHeight = baseLineHeight,
                                textAlign = resolvedAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        BasicTextField(
                            value = textContent,
                            onValueChange = { newText ->
                                textContent = newText
                                onTextChange(newText)
                            },
                            textStyle = TextStyle(
                                color = resolvedTextColor,
                                fontSize = baseFontSize,
                                lineHeight = baseLineHeight,
                                fontWeight = resolvedWeight,
                                fontStyle = resolvedStyle,
                                textAlign = resolvedAlign
                            ),
                            cursorBrush = SolidColor(selectionColor),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(textScrollState)
                                .focusRequester(focusRequester)
                        )
                    } else {
                        if (textContent.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(textScrollState)
                                        .pointerInput(box.id) {
                                            detectTapGestures(onTap = { onSelect() })
                                        }
                                ) {
                                    MarkdownLatexContent(
                                        markdown = textContent,
                                        baseFontSize = baseFontSize,
                                        lineHeight = baseLineHeight,
                                        textColor = resolvedTextColor,
                                        accentColor = if (isDarkPaper) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                        isDarkSurface = isDarkPaper,
                                        defaultBold = box.isBold,
                                        defaultItalic = box.isItalic,
                                        textAlign = resolvedAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (isScrollable) {
                                    val thumbAlpha = if (textScrollState.value < textScrollState.maxValue) 0.45f else 0.18f
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(3.dp)
                                            .fillMaxHeight(0.35f)
                                            .background(resolvedTextColor.copy(alpha = thumbAlpha), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Teks kosong",
                                color = Color(0xFF94A3B8),
                                fontSize = baseFontSize,
                                lineHeight = baseLineHeight
                            )
                        }
                    }
                }
            }

            // When unselected and content doesn't overflow: tap selects or drag anywhere moves immediately.
            // When scrollable (content overflows): no overlay blocks verticalScroll; tapping Column selects cleanly.
            if (!box.isSelected && !isScrollable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(box.id) {
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
                                    // Inside graphicsLayer(scaleX = scale, scaleY = scale), pointer deltas are already in world units.
                                    worldX += overSlop.x
                                    worldY += overSlop.y
                                    drag(dragChange.id) { change ->
                                        change.consume()
                                        worldX += change.positionChange().x
                                        worldY += change.positionChange().y
                                    }
                                    onCommitMove(worldX, worldY)
                                } else {
                                    onSelect()
                                }
                            }
                        }
                )
            }

            // Controls & Edge Resize Handles (Shown only when selected, strictly attached to this card)
            if (box.isSelected) {
                // Drag Button centered above top border
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp)
                        .size(56.dp, 36.dp)
                        .pointerInput(box.id) {
                            detectDragGestures(
                                onDragEnd = { onCommitMove(worldX, worldY) },
                                onDragCancel = { onCommitMove(worldX, worldY) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    worldX += dragAmount.x
                                    worldY += dragAmount.y
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

                // Left Border Drag Zone (drag along the left border edge to move)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .width(14.dp)
                        .fillMaxHeight()
                        .pointerInput(box.id) {
                            detectDragGestures(
                                onDragEnd = { onCommitMove(worldX, worldY) },
                                onDragCancel = { onCommitMove(worldX, worldY) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    worldX += dragAmount.x
                                    worldY += dragAmount.y
                                }
                            )
                        }
                )

                // Small Top-Right Delete Circle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-12).dp)
                        .size(36.dp, 36.dp)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Right-Edge Horizontal Resize Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .size(32.dp, 48.dp)
                        .pointerInput(box.id) {
                            detectDragGestures(
                                onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                                onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    worldWidth = maxOf(120f, worldWidth + dragAmount.x)
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
                        .offset(y = 10.dp)
                        .size(48.dp, 32.dp)
                        .pointerInput(box.id) {
                            detectDragGestures(
                                onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                                onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    worldHeight = maxOf(64f, worldHeight + dragAmount.y)
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

                // Bottom-Right Corner Resize Handle (Bulatan Resize)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 12.dp)
                        .size(40.dp)
                        .pointerInput(box.id) {
                            detectDragGestures(
                                onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                                onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    worldWidth = maxOf(120f, worldWidth + dragAmount.x)
                                    worldHeight = maxOf(64f, worldHeight + dragAmount.y)
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
