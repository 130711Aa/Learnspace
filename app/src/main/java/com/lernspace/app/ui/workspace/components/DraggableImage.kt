package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.workspace.model.ImageElement
import kotlin.math.roundToInt

@Composable
fun DraggableImage(
    image: ImageElement,
    scale: Float = 1f,
    panX: Float = 0f,
    panY: Float = 0f,
    onCommitMove: (Float, Float) -> Unit,
    onCommitResize: (Float, Float) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var worldX by remember(image.id) { mutableFloatStateOf(image.x) }
    var worldY by remember(image.id) { mutableFloatStateOf(image.y) }
    var worldWidth by remember(image.id) { mutableFloatStateOf(image.width) }
    var worldHeight by remember(image.id) { mutableFloatStateOf(image.height) }

    LaunchedEffect(image.x, image.y) {
        worldX = image.x
        worldY = image.y
    }
    LaunchedEffect(image.width, image.height) {
        worldWidth = image.width
        worldHeight = image.height
    }

    val currentScale by rememberUpdatedState(scale)
    val currentOnCommitMove by rememberUpdatedState(onCommitMove)
    val currentOnCommitResize by rememberUpdatedState(onCommitResize)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnDelete by rememberUpdatedState(onDelete)

    val selectionColor = Color(0xFF2563EB)
    val aspectRatio = remember(image.bitmap) {
        (image.bitmap.width.toFloat() / image.bitmap.height.toFloat()).coerceIn(0.2f, 5f)
    }

    // Convert world canvas pixels (worldWidth * scale) into Compose Dp without density inflation
    val screenWidthDp = with(density) { (worldWidth * scale).toDp() }
    val screenHeightDp = with(density) { (worldHeight * scale).toDp() }

    Box(
        modifier = modifier
            .offset {
                val screenX = worldX * scale + panX
                val screenY = worldY * scale + panY
                IntOffset(screenX.roundToInt(), screenY.roundToInt())
            }
            .requiredSize(width = screenWidthDp, height = screenHeightDp)
    ) {
        // Image Body with crisp proportional scaling
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (image.isSelected) {
                        Modifier
                            .shadow(6.dp, RoundedCornerShape(10.dp))
                            .border(2.5.dp, selectionColor, RoundedCornerShape(10.dp))
                    } else {
                        Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                    }
                ),
            shape = RoundedCornerShape(if (image.isSelected) 10.dp else 8.dp),
            color = Color.White
        ) {
            Image(
                bitmap = image.bitmap,
                contentDescription = "Imported Note Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.FillBounds
            )
        }

        // Tap to Select & Direct Smooth Dragging (120 FPS, never cancelled mid-drag)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isDragging = false
                        var accumulated = Offset.Zero
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()

                            // Multi-finger gesture detected: let canvas pinch-to-zoom handle it!
                            if (event.changes.count { it.pressed } >= 2) {
                                if (isDragging) {
                                    currentOnCommitMove(worldX, worldY)
                                }
                                break
                            }

                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    currentOnSelect()
                                } else {
                                    currentOnCommitMove(worldX, worldY)
                                }
                                break
                            }
                            val delta = change.positionChange()
                            if (!isDragging) {
                                accumulated += delta
                                if (accumulated.getDistance() > touchSlop) {
                                    isDragging = true
                                    currentOnSelect()
                                    change.consume()
                                    worldX += accumulated.x / currentScale
                                    worldY += accumulated.y / currentScale
                                }
                            } else {
                                change.consume()
                                worldX += delta.x / currentScale
                                worldY += delta.y / currentScale
                            }
                        }
                    }
                }
        )

        // Overlay Controls when Image is Selected
        if (image.isSelected) {
            // Delete Button (Top-Right Circular Action)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(30.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { currentOnDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete Image",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Move Indicator Pill (Top-Left)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = selectionColor,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-8).dp, y = (-10).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenWith,
                        contentDescription = "Move",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Proportional Resize Handle (Bottom-Right Corner)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .size(44.dp)
                    .pointerInput(image.id) {
                        detectDragGestures(
                            onDragEnd = { currentOnCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { currentOnCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaWorldX = dragAmount.x / currentScale
                                val deltaWorldY = dragAmount.y / currentScale
                                val delta = (deltaWorldX + deltaWorldY * aspectRatio) / 2f
                                val newWidth = (worldWidth + delta).coerceIn(120f, 2500f)
                                worldWidth = newWidth
                                worldHeight = newWidth / aspectRatio
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, selectionColor, CircleShape)
                )
            }
        }
    }
}
