package com.lernspace.app.ui.components

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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

    Box(
        modifier = modifier
            // 2-finger Pinch to Zoom & Pan interceptor:
            // When 2 fingers touch, compute pan/zoom and consume so InProgressStrokes doesn't draw lines between fingers.
            // When 1 pointer touches (Stylus or pen), it is NOT consumed at all, so InProgressStrokes draws smoothly!
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            currentOnTransform(centroid, pan, zoom)
                            event.changes.forEach { it.consume() }
                        } else if (pressed.isEmpty() && event.changes.size == 1 && !event.changes[0].isConsumed) {
                            // Canvas tap to deselect active objects
                            currentOnCanvasTap()
                        }
                    }
                }
            }
    ) {
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

        // 2. Active Inking / Eraser Layer
        if (isDrawingEnabled) {
            if (isEraserMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { currentOnEraseStart() },
                                onDragEnd = { currentOnEraseEnd() },
                                onDragCancel = { currentOnEraseEnd() },
                                onDrag = { change, _ ->
                                    currentOnErase(change.position.x, change.position.y)
                                    change.consume()
                                },
                            )
                        }
                )
            } else {
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
}
