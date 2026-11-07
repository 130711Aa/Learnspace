package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.workspace.model.ImageElement
import kotlin.math.roundToInt

@Composable
fun DraggableImage(
    image: ImageElement,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var offsetX by remember(image.x) { mutableFloatStateOf(image.x) }
    var offsetY by remember(image.y) { mutableFloatStateOf(image.y) }
    var widthDp by remember(image.width) { mutableFloatStateOf(image.width) }
    var heightDp by remember(image.height) { mutableFloatStateOf(image.height) }

    val selectionColor = Color(0xFF2563EB)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clickable { onSelect() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (image.isSelected) {
                        Modifier
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .border(1.5.dp, selectionColor, RoundedCornerShape(8.dp))
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

        if (image.isSelected) {
            // Move Grip (Top-Center)
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
                    contentDescription = "Move Image",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Delete Button (Top-Right)
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
                    contentDescription = "Delete Image",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Bottom-Right Corner Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, selectionColor, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaX = dragAmount.x / density.density
                            val deltaY = dragAmount.y / density.density
                            widthDp = maxOf(120f, widthDp + deltaX)
                            heightDp = maxOf(80f, heightDp + deltaY)
                            onResize(widthDp, heightDp)
                        }
                    }
            )
        }
    }
}
