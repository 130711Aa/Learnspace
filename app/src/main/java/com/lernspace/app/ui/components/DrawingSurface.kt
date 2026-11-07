package com.lernspace.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.ink.strokes.Stroke
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import android.graphics.Matrix
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.compose.ui.util.fastForEach
import androidx.ink.brush.compose.createWithComposeColor
import androidx.core.graphics.withSave

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
    onErase: (Float, Float) -> Unit = { _, _ -> },
    onEraseStart: () -> Unit = {},
    onEraseEnd: () -> Unit = {},
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

    Box(modifier = modifier) {
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
