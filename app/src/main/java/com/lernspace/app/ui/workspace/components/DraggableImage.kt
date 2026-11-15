package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.ui.workspace.model.ImageElement

@Composable
fun DraggableImage(
    image: ImageElement,
    scaleProvider: () -> Float = { 1f },
    panXProvider: () -> Float = { 0f },
    panYProvider: () -> Float = { 0f },
    onCommitMove: (Float, Float) -> Unit,
    onCommitResize: (Float, Float) -> Unit,
    onSelect: () -> Unit,
    onToggleLock: () -> Unit = {},
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

    val currentScaleProvider by rememberUpdatedState(scaleProvider)
    val currentOnCommitMove by rememberUpdatedState(onCommitMove)
    val currentOnCommitResize by rememberUpdatedState(onCommitResize)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnToggleLock by rememberUpdatedState(onToggleLock)
    val currentOnDelete by rememberUpdatedState(onDelete)

    val selectionColor = Color(0xFF2563EB)
    val aspectRatio = remember(image.bitmap) {
        (image.bitmap.width.toFloat() / image.bitmap.height.toFloat()).coerceIn(0.2f, 5f)
    }

    // Fixed unscaled world dimensions in Dp.
    // The GPU graphicsLayer scales & translates this Box atomically on the RenderNode thread,
    // exactly matching DrawingSurface and PaperBackground without any CPU layout drift or subpixel jitter!
    val worldWidthDp = with(density) { worldWidth.toDp() }
    val worldHeightDp = with(density) { worldHeight.toDp() }

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = currentScaleProvider()
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = worldX * s + panXProvider()
                translationY = worldY * s + panYProvider()
            }
            .size(width = worldWidthDp, height = worldHeightDp)
    ) {
        // Image Body with clean surface and no intrusive colored borders
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (image.isSelected && !image.isLocked) {
                        Modifier
                            .shadow(6.dp, RoundedCornerShape(10.dp))
                            .border(2.5.dp, selectionColor, RoundedCornerShape(10.dp))
                    } else {
                        Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                    }
                ),
            shape = RoundedCornerShape(if (image.isSelected && !image.isLocked) 10.dp else 8.dp),
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

        // Tap to Select (when unselected and unlocked)
        // Multi-touch gestures (zoom/pan) pass cleanly to the canvas with zero accidental movement!
        if (!image.isSelected && !image.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { currentOnSelect() }
            )
        }

        // Move Drag Handler (Active ONLY when intentionally selected and NOT locked)
        if (image.isSelected && !image.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(image.id) {
                        detectDragGestures(
                            onDragEnd = { currentOnCommitMove(worldX, worldY) },
                            onDragCancel = {
                                worldX = image.x
                                worldY = image.y
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val curScale = currentScaleProvider().coerceAtLeast(0.05f)
                                worldX += dragAmount.x / curScale
                                worldY += dragAmount.y / curScale
                            }
                        )
                    }
            )
        }

        // Lock Indicator Pill: Placed OUTSIDE the top-end corner of the image!
        // Never obstructs image/graph content, uses subtle slate/dark styling (not glaring bright yellow)
        if (image.isLocked) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC0F172A),
                border = BorderStroke(1.dp, Color(0x33475569)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-12).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { currentOnToggleLock() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Posisi Terkunci (Ketuk untuk Buka)",
                        tint = Color(0xCC94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Overlay Controls when Image is Selected & Unlocked
        if (image.isSelected && !image.isLocked) {
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

            // Action Pill (Top-Start): Move Indicator + Lock Position Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-14).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenWith,
                            contentDescription = "Move",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Geser",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color(0xFF475569))
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currentOnToggleLock() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Kunci Posisi",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Kunci",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                                val curScale = currentScaleProvider().coerceAtLeast(0.05f)
                                val deltaWorldX = dragAmount.x / curScale
                                val deltaWorldY = dragAmount.y / curScale
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
