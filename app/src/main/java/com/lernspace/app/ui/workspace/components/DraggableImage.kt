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
    onCommitMove: (Float, Float) -> Unit,
    onCommitResize: (Float, Float) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var offsetX by remember(image.id) { mutableFloatStateOf(image.x) }
    var offsetY by remember(image.id) { mutableFloatStateOf(image.y) }
    var widthDp by remember(image.id) { mutableFloatStateOf(image.width) }
    var heightDp by remember(image.id) { mutableFloatStateOf(image.height) }

    LaunchedEffect(image.x, image.y) {
        offsetX = image.x
        offsetY = image.y
    }
    LaunchedEffect(image.width, image.height) {
        widthDp = image.width
        heightDp = image.height
    }

    val selectionColor = Color(0xFF2563EB)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(widthDp.dp)
            .height(heightDp.dp)
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
        // Users can drag the image anywhere to move smoothly at 120 FPS without parent recomposition!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image.id) {
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
                            offsetX += overSlop.x
                            offsetY += overSlop.y
                            drag(dragChange.id) { change ->
                                change.consume()
                                offsetX += change.positionChange().x
                                offsetY += change.positionChange().y
                            }
                            onCommitMove(offsetX, offsetY)
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

            // Bottom-Right Corner Resize Handle (Smooth diagonal resize, 44x44dp hit area)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 14.dp)
                    .size(44.dp)
                    .pointerInput(image.id) {
                        detectDragGestures(
                            onDragEnd = { onCommitResize(widthDp, heightDp) },
                            onDragCancel = { onCommitResize(widthDp, heightDp) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaX = dragAmount.x / density.density
                                val deltaY = dragAmount.y / density.density
                                widthDp = maxOf(100f, widthDp + deltaX)
                                heightDp = maxOf(60f, heightDp + deltaY)
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
