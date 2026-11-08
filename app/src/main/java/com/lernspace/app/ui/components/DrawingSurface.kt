package com.lernspace.app.ui.components

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.withSave
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

fun createDefaultPenBrush(): Brush {
    return Brush.createWithComposeColor(
        family = StockBrushes.pressurePen(),
        color = Color.Black,
        size = 5f,
        epsilon = 0.1f,
    )
}

@Composable
fun DrawingSurface(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    currentBrush: Brush,
    onGetNextBrush: () -> Brush,
    onStrokesFinished: (List<Stroke>) -> Unit,
    isEraserMode: Boolean,
    scale: Float = 1f,
    panX: Float = 0f,
    panY: Float = 0f,
    modifier: Modifier = Modifier,
    isDrawingEnabled: Boolean = true,
    onErase: (Float, Float) -> Unit = { _, _ -> },
    onEraseStart: () -> Unit = {},
    onEraseEnd: () -> Unit = {},
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit = { _, _, _ -> },
    onCanvasTap: () -> Unit = {}
) {
    var pendingStrokes by remember { mutableStateOf<List<Stroke>>(emptyList()) }

    SideEffect {
        if (pendingStrokes.isNotEmpty()) {
            pendingStrokes = if (strokes.isEmpty()) {
                emptyList()
            } else {
                pendingStrokes.filter { pending -> strokes.none { it === pending } }
            }
        }
    }

    val strokesToDraw = remember(strokes, pendingStrokes) {
        if (pendingStrokes.isEmpty()) {
            strokes
        } else {
            strokes + pendingStrokes.filter { pending -> strokes.none { it === pending } }
        }
    }

    // High-Precision Vector Transform Matrix:
    // Transforms vector strokes to screen coordinates dynamically at native resolution.
    // ZERO BLUR at any zoom level!
    val screenMatrix = remember { Matrix() }
    screenMatrix.reset()
    screenMatrix.postScale(scale, scale)
    screenMatrix.postTranslate(panX, panY)

    val currentOnErase by rememberUpdatedState(onErase)
    val currentOnEraseStart by rememberUpdatedState(onEraseStart)
    val currentOnEraseEnd by rememberUpdatedState(onEraseEnd)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnCanvasTap by rememberUpdatedState(onCanvasTap)
    val currentScale by rememberUpdatedState(scale)
    val currentPanX by rememberUpdatedState(panX)
    val currentPanY by rememberUpdatedState(panY)

    val screenToWorldMatrix = remember { androidx.compose.ui.graphics.Matrix() }
    screenToWorldMatrix.reset()
    screenToWorldMatrix.scale(1f / scale, 1f / scale, 1f)
    screenToWorldMatrix.translate(-panX, -panY)

    val identityUiMatrix = remember { androidx.compose.ui.graphics.Matrix() }

    Box(
        modifier = modifier
            // STYLUS vs FINGER DISPATCHER:
            // Key is strictly Unit so the gesture loop is NEVER cancelled mid-drag/pinch!
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    if (down.type == PointerType.Touch) {
                        // FINGER GESTURE: Panning & Pinch-to-Zoom only (Never Inking!)
                        down.consume()

                        var isTransforming = false
                        var accumulatedPan = Offset.Zero
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                            // Palm rejection: If stylus is active on screen, don't pan!
                            val hasStylus = event.changes.any { it.type == PointerType.Stylus || it.type == PointerType.Eraser }
                            if (hasStylus) {
                                event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                                continue
                            }

                            val pressedFingers = event.changes.filter { it.type == PointerType.Touch && it.pressed }
                            if (pressedFingers.isEmpty()) {
                                if (!isTransforming && accumulatedPan.getDistance() < touchSlop) {
                                    currentOnCanvasTap()
                                }
                                break
                            }

                            if (pressedFingers.size >= 2) {
                                // 2-finger pinch-to-zoom & pan
                                isTransforming = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                event.changes.forEach { it.consume() }
                                currentOnTransform(centroid, pan, zoom)
                            } else if (pressedFingers.size == 1) {
                                // 1-finger pan
                                val change = pressedFingers[0]
                                val panDelta = change.positionChange()
                                change.consume()

                                if (!isTransforming) {
                                    accumulatedPan += panDelta
                                    if (accumulatedPan.getDistance() > touchSlop) {
                                        isTransforming = true
                                        currentOnTransform(change.position, accumulatedPan, 1f)
                                    }
                                } else {
                                    currentOnTransform(change.position, panDelta, 1f)
                                }
                            }
                        }
                    } else {
                        // STYLUS: Let InProgressStrokes or Eraser handle it!
                    }
                }
            }
    ) {
        // 1. Render Completed Strokes (Native Vector Tessellation - 100% Crisp & Sharp)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nativeCanvas = drawContext.canvas.nativeCanvas
            strokesToDraw.fastForEach { stroke ->
                nativeCanvas.withSave {
                    canvasStrokeRenderer.draw(
                        canvas = nativeCanvas,
                        stroke = stroke,
                        strokeToScreenTransform = screenMatrix,
                    )
                }
            }
        }

        // 2. Active Inking / Eraser Layer (STYLUS ONLY)
        if (isDrawingEnabled) {
            if (isEraserMode) {
                // Eraser mode for Stylus: erases nearby strokes in world coordinates
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.type == PointerType.Stylus || down.type == PointerType.Eraser) {
                                    currentOnEraseStart()
                                    val worldX = (down.position.x - currentPanX) / currentScale
                                    val worldY = (down.position.y - currentPanY) / currentScale
                                    currentOnErase(worldX, worldY)
                                    drag(down.id) { change ->
                                        if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) {
                                            change.consume()
                                            val curWorldX = (change.position.x - currentPanX) / currentScale
                                            val curWorldY = (change.position.y - currentPanY) / currentScale
                                            currentOnErase(curWorldX, curWorldY)
                                        }
                                    }
                                    currentOnEraseEnd()
                                }
                            }
                        }
                )
            } else {
                // Active Inking with AndroidX Ink (Vector-sharp with screenToWorldMatrix)
                InProgressStrokes(
                    defaultBrush = currentBrush,
                    nextBrush = onGetNextBrush,
                    pointerEventToWorldTransform = screenToWorldMatrix,
                    strokeToWorldTransform = identityUiMatrix,
                    onStrokesFinished = { newStrokes ->
                        pendingStrokes = pendingStrokes + newStrokes
                        onStrokesFinished(newStrokes)
                    }
                )
            }
        }
    }
}
