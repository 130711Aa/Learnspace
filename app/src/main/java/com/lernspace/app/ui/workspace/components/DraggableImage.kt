package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

    val selectionColor = Color(0xFF2563EB)

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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (image.isSelected) {
                        Modifier
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .border(2.dp, selectionColor, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ) {
            Image(
                bitmap = image.bitmap,
                contentDescription = "Note illustration",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Tap or Drag to Move directly:
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image.id, scale) {
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

        if (image.isSelected) {
            // Delete Button (Integrated top-right corner inside image)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete Image",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom-Right Corner Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 14.dp)
                    .size(44.dp)
                    .pointerInput(image.id, scale) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(worldWidth, worldHeight) },
                            onDragCancel = { onCommitResize(worldWidth, worldHeight) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaX = (dragAmount.x / density.density) / scale
                                val deltaY = (dragAmount.y / density.density) / scale
                                worldWidth = maxOf(100f, worldWidth + deltaX)
                                worldHeight = maxOf(60f, worldHeight + deltaY)
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
