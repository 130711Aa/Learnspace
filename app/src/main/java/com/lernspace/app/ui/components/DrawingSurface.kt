package com.lernspace.app.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.setFrom
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

/**
 * Checks if a 32-bit ARGB color has low luminance (black, dark slate, charcoal ink).
 */
private fun isDarkInkColor(colorInt: Int): Boolean {
    val r = (colorInt shr 16) and 0xFF
    val g = (colorInt shr 8) and 0xFF
    val b = colorInt and 0xFF
    val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    return lum < 0.22f
}

@Composable
fun DrawingSurface(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    currentBrush: Brush,
    onGetNextBrush: () -> Brush,
    onStrokesFinished: (List<Stroke>) -> Unit,
    isEraserMode: Boolean,
    isDarkPaper: Boolean = false,
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
    val currentOnErase by rememberUpdatedState(onErase)
    val currentOnEraseStart by rememberUpdatedState(onEraseStart)
    val currentOnEraseEnd by rememberUpdatedState(onEraseEnd)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnCanvasTap by rememberUpdatedState(onCanvasTap)
    val currentScale by rememberUpdatedState(scale)
    val currentPanX by rememberUpdatedState(panX)
    val currentPanY by rememberUpdatedState(panY)

    val strokeToScreenMatrix = remember { Matrix() }
    val identityComposeMatrix = remember { androidx.compose.ui.graphics.Matrix() }

    val pointerEventToWorldMatrix = remember(scale, panX, panY) {
        val renderMatrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(panX, panY)
        }
        val inv = Matrix()
        renderMatrix.invert(inv)
        androidx.compose.ui.graphics.Matrix().apply {
            setFrom(inv)
        }
    }

    // Smart Stroke Inversion Filter for Dark Paper:
    // Dark/black strokes (lum < 0.22f) are rendered into a hardware-accelerated layer that
    // maps color directly to crisp off-white (#F8FAFC) while preserving the exact stroke antialiasing/alpha.
    val darkStrokePaint = remember {
        val cm = ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 248f,
            0f, 0f, 0f, 0f, 250f,
            0f, 0f, 0f, 0f, 252f,
            0f, 0f, 0f, 1f, 0f
        ))
        Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
    }

    Box(
        modifier = modifier.clipToBounds()
    ) {
        // ─── Layer 1: Completed (dry) strokes rendered with hardware canvas transform & vector LOD ───
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = currentScale
            val px = currentPanX
            val py = currentPanY

            strokeToScreenMatrix.reset()
            strokeToScreenMatrix.postScale(s, s)
            strokeToScreenMatrix.postTranslate(px, py)

            val nativeCanvas = drawContext.canvas.nativeCanvas
            nativeCanvas.withSave {
                // Strict clipping to DrawingSurface viewport:
                // Prevents strokes from ever bleeding outside their pane in split-screen mode!
                nativeCanvas.clipRect(0f, 0f, size.width, size.height)

                nativeCanvas.translate(px, py)
                nativeCanvas.scale(s, s)

                if (!isDarkPaper) {
                    strokes.fastForEach { stroke ->
                        canvasStrokeRenderer.draw(
                            canvas = nativeCanvas,
                            stroke = stroke,
                            strokeToScreenTransform = strokeToScreenMatrix,
                        )
                    }
                } else {
                    // Smart Stroke Inversion on Dark Paper:
                    // Batch consecutive strokes to maintain exact z-order with minimal layer allocations
                    var i = 0
                    val count = strokes.size
                    while (i < count) {
                        val isDark = isDarkInkColor(strokes[i].brush.colorIntArgb)
                        val batchStart = i
                        while (i < count && isDarkInkColor(strokes[i].brush.colorIntArgb) == isDark) {
                            i++
                        }
                        if (isDark) {
                            nativeCanvas.saveLayer(null, darkStrokePaint)
                            for (j in batchStart until i) {
                                canvasStrokeRenderer.draw(
                                    canvas = nativeCanvas,
                                    stroke = strokes[j],
                                    strokeToScreenTransform = strokeToScreenMatrix,
                                )
                            }
                            nativeCanvas.restore()
                        } else {
                            for (j in batchStart until i) {
                                canvasStrokeRenderer.draw(
                                    canvas = nativeCanvas,
                                    stroke = strokes[j],
                                    strokeToScreenTransform = strokeToScreenMatrix,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── Layer 2: Active inking / eraser (STYLUS ONLY) ───
        if (isDrawingEnabled) {
            if (isEraserMode) {
                // Eraser: convert stylus screen position to world space, then erase
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
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
                // Active inking via AndroidX Ink.
                // Wrapped in clipToBounds to strictly stay inside pane boundaries.
                val effectiveCurrentBrush = remember(currentBrush, isDarkPaper) {
                    if (isDarkPaper && isDarkInkColor(currentBrush.colorIntArgb)) {
                        Brush.createWithColorIntArgb(
                            family = currentBrush.family,
                            colorIntArgb = 0xFFF8FAFC.toInt(),
                            size = currentBrush.size,
                            epsilon = currentBrush.epsilon
                        )
                    } else {
                        currentBrush
                    }
                }
                val effectiveGetNextBrush = remember(onGetNextBrush, isDarkPaper) {
                    {
                        val b = onGetNextBrush()
                        if (isDarkPaper && isDarkInkColor(b.colorIntArgb)) {
                            Brush.createWithColorIntArgb(
                                family = b.family,
                                colorIntArgb = 0xFFF8FAFC.toInt(),
                                size = b.size,
                                epsilon = b.epsilon
                            )
                        } else {
                            b
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    key(effectiveCurrentBrush) {
                        InProgressStrokes(
                            defaultBrush = effectiveCurrentBrush,
                            nextBrush = effectiveGetNextBrush,
                            pointerEventToWorldTransform = pointerEventToWorldMatrix,
                            strokeToWorldTransform = identityComposeMatrix,
                            onStrokesFinished = { finishedStrokes ->
                                val normalizedStrokes = if (isDarkPaper && isDarkInkColor(currentBrush.colorIntArgb)) {
                                    finishedStrokes.map { stroke ->
                                        // Store stroke with true logical brush (e.g. black)
                                        // so that PDF export and light paper toggling remain perfectly preserved!
                                        if (stroke.brush.colorIntArgb == 0xFFF8FAFC.toInt()) {
                                            Stroke(currentBrush, stroke.inputs)
                                        } else {
                                            stroke
                                        }
                                    }
                                } else {
                                    finishedStrokes
                                }
                                onStrokesFinished(normalizedStrokes)
                            }
                        )
                    }
                }
            }
        }
    }
}
