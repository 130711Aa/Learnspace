package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // Tap overlay when unselected
        if (!image.isSelected) {
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

        if (image.isSelected) {
            // Move Grip (Top-Center, 40dp clearance)
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
                        contentDescription = "Move Image",
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

            // Delete Button (Top-Right)
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
                    contentDescription = "Delete Image",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom-Right Corner Resize Handle
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
                            widthDp = maxOf(120f, widthDp + deltaX)
                            heightDp = maxOf(80f, heightDp + deltaY)
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
