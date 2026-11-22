package com.lernspace.app.ui.mimolab.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lernspace.app.core.mimolab.MimoSimulation
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Movable, resizable Picture-in-Picture floating window for Mimo Lab simulations.
 * Floats seamlessly above NoteEditorScreen / InkSpace without interrupting handwritten notes.
 */
@Composable
fun MimoFloatingSimWindow(
    simulation: MimoSimulation,
    isDarkTheme: Boolean = true,
    onInsertSnapshotToNote: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onClose()
    }

    val density = LocalDensity.current.density
    val config = LocalConfiguration.current
    val screenWidthPx = config.screenWidthDp * density
    val screenHeightPx = config.screenHeightDp * density

    var isFullscreen by remember { mutableStateOf(false) }
    var windowWidthDp by remember { mutableFloatStateOf(520f) }
    var windowHeightDp by remember { mutableFloatStateOf(420f) }

    var windowOffsetX by remember { mutableFloatStateOf(60f * density) }
    var windowOffsetY by remember { mutableFloatStateOf(80f * density) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isFullscreen) {
            MimoSimulationViewer(
                simulation = simulation,
                isDarkTheme = isDarkTheme,
                isFullscreen = true,
                onToggleFullscreen = { isFullscreen = false },
                onInsertSnapshotToNote = onInsertSnapshotToNote,
                onClose = onClose,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val winW = windowWidthDp.coerceIn(340f, config.screenWidthDp.toFloat() - 20f)
            val winH = windowHeightDp.coerceIn(280f, config.screenHeightDp.toFloat() - 40f)

            Surface(
                modifier = Modifier
                    .offset { IntOffset(windowOffsetX.roundToInt(), windowOffsetY.roundToInt()) }
                    .width(winW.dp)
                    .height(winH.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.2.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color.White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MimoSimulationViewer(
                        simulation = simulation,
                        isDarkTheme = isDarkTheme,
                        isFullscreen = false,
                        onToggleFullscreen = { isFullscreen = true },
                        onInsertSnapshotToNote = onInsertSnapshotToNote,
                        onClose = onClose,
                        modifier = Modifier.fillMaxSize()
                    )

                    // ── Invisible Draggable Titlebar Overlay (Top 40dp) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(end = 120.dp) // Leave right buttons clickable
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val winPxW = winW * density
                                    val winPxH = winH * density
                                    windowOffsetX = (windowOffsetX + dragAmount.x).coerceIn(8f, max(8f, screenWidthPx - winPxW))
                                    windowOffsetY = (windowOffsetY + dragAmount.y).coerceIn(8f, max(8f, screenHeightPx - winPxH))
                                }
                            }
                    )

                    // ── Resize Grip Handle (Bottom-Right Corner) ──
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaDpX = dragAmount.x / density
                                    val deltaDpY = dragAmount.y / density
                                    windowWidthDp = (windowWidthDp + deltaDpX).coerceIn(340f, config.screenWidthDp.toFloat() - 20f)
                                    windowHeightDp = (windowHeightDp + deltaDpY).coerceIn(280f, config.screenHeightDp.toFloat() - 40f)
                                }
                            },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xCC334155) else Color(0xCCCBD5E1),
                            modifier = Modifier
                                .padding(5.dp)
                                .size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.OpenWith,
                                    contentDescription = "Resize Window",
                                    tint = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
