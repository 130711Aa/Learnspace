package com.lernspace.app.ui.components

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
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
    val identityMatrix = remember { Matrix() }

    val currentOnErase by rememberUpdatedState(onErase)
    val currentOnEraseStart by rememberUpdatedState(onEraseStart)
    val currentOnEraseEnd by rememberUpdatedState(onEraseEnd)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnCanvasTap by rememberUpdatedState(onCanvasTap)

    Box(modifier = modifier) {
        // 1. Render Completed Strokes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nativeCanvas = drawContext.canvas.nativeCanvas
            strokesToDraw.fastForEach { stroke ->
                nativeCanvas.withSave {
                    canvasStrokeRenderer.draw(
                        canvas = nativeCanvas,
                        stroke = stroke,
                        strokeToScreenTransform = identityMatrix,
                    )
                }
            }
        }

        // 2. Active Inking / Eraser Layer (STYLUS ONLY)
        if (isDrawingEnabled) {
            if (isEraserMode) {
                // Eraser: Only triggers when touched by Stylus
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.type == PointerType.Stylus || down.type == PointerType.Eraser) {
                                    currentOnEraseStart()
                                    currentOnErase(down.position.x, down.position.y)
                                    drag(down.id) { change ->
                                        if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) {
                                            change.consume()
                                            currentOnErase(change.position.x, change.position.y)
                                        }
                                    }
                                    currentOnEraseEnd()
                                }
                            }
                        }
                )
            } else {
                // InProgressStrokes: Only triggers for Stylus.
                // We consume all PointerType.Touch in Initial pass so fingers NEVER draw ink!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    for (change in event.changes) {
                                        if (change.type == PointerType.Touch) {
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    InProgressStrokes(
                        defaultBrush = currentBrush,
                        nextBrush = onGetNextBrush,
                        onStrokesFinished = { newStrokes ->
                            pendingStrokes = pendingStrokes + newStrokes
                            onStrokesFinished(newStrokes)
                        }
                    )
                }
            }
        }

        // 3. Finger Gesture Detector (Pan & Pinch-to-Zoom & Canvas Tap)
        // Stylus is completely ignored here so it draws at full speed!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectFingerTransformGestures(
                        onGesture = { centroid, pan, zoom ->
                            currentOnTransform(centroid, pan, zoom)
                        },
                        onTap = {
                            currentOnCanvasTap()
                        }
                    )
                }
        )
    }
}

/**
 * Custom transform gesture detector that strictly responds to FINGER touches (PointerType.Touch).
 * Stylus inputs (PointerType.Stylus / PointerType.Eraser) are ignored so they pass through to inking!
 */
private suspend fun PointerInputScope.detectFingerTransformGestures(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onTap: () -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        val down = awaitFirstDown(requireUnconsumed = false)
        // If touched by stylus, exit immediately!
        if (down.type != PointerType.Touch) {
            return@awaitEachGesture
        }

        var isTransforming = false

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pressedFingers = event.changes.filter { it.type == PointerType.Touch && it.pressed }
            if (pressedFingers.isEmpty()) {
                if (!pastTouchSlop && !isTransforming) {
                    onTap()
                }
                break
            }

            if (pressedFingers.size >= 2) {
                // 2-finger Pinch to Zoom and Pan
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = true)

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = true)
                    val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                        isTransforming = true
                    }
                }

                if (pastTouchSlop) {
                    event.changes.forEach { it.consume() }
                    onGesture(centroid, panChange, zoomChange)
                }
            } else if (pressedFingers.size == 1) {
                // 1-finger Pan
                val change = pressedFingers[0]
                val panChange = change.positionChange()

                if (!pastTouchSlop) {
                    pan += panChange
                    if (pan.getDistance() > touchSlop) {
                        pastTouchSlop = true
                        isTransforming = true
                    }
                }

                if (pastTouchSlop) {
                    change.consume()
                    onGesture(change.position, panChange, 1f)
                }
            }
        }
    }
}
