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
    val currentOnErase by rememberUpdatedState(onErase)
    val currentOnEraseStart by rememberUpdatedState(onEraseStart)
    val currentOnEraseEnd by rememberUpdatedState(onEraseEnd)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnCanvasTap by rememberUpdatedState(onCanvasTap)
    val currentScale by rememberUpdatedState(scale)
    val currentPanX by rememberUpdatedState(panX)
    val currentPanY by rememberUpdatedState(panY)

    // =========================================================================
    // COORDINATE SYSTEM (World Space):
    //   screenX = worldX * scale + panX   →  worldX = (screenX - panX) / scale
    //   screenY = worldY * scale + panY   →  worldY = (screenY - panY) / scale
    //
    // strokeToScreen (android.graphics.Matrix, used by CanvasStrokeRenderer):
    //   postScale(scale, scale)  → postTranslate(panX, panY)
    //   Maps: world → screen
    //
    // pointerEventToWorld (androidx.compose.ui.graphics.Matrix, expected by InProgressStrokes):
    //   Compose Matrix operations post-multiply: M_new = M_prev × M_op
    //   Compose Matrix.scale(sx,sy) then .translate(tx,ty) maps:
    //     (x,y) → ((x + tx)*sx, (y + ty)*sy)
    //   We want worldX = (screenX - panX) / scale, so:
    //     tx = -panX, sx = 1/scale  →  scale(1/s, 1/s) FIRST, then translate(-panX, -panY)
    //   This is the EXACT inverse of the strokeToScreen transform.
    // =========================================================================

    val strokeToScreenMatrix = remember { Matrix() }
    val identityComposeMatrix = remember { androidx.compose.ui.graphics.Matrix() }

    // Stable instance to prevent InProgressStrokes from constantly recreating shaders during pinch gestures
    val pointerEventToWorldMatrix = remember { androidx.compose.ui.graphics.Matrix() }
    val renderMatrix = remember { Matrix() }
    val invMatrix = remember { Matrix() }

    // Update the matrix values in-place whenever scale or pan changes
    LaunchedEffect(scale, panX, panY) {
        renderMatrix.reset()
        renderMatrix.postScale(scale, scale)
        renderMatrix.postTranslate(panX, panY)
        renderMatrix.invert(invMatrix)
        pointerEventToWorldMatrix.setFrom(invMatrix)
    }

    Box(
        modifier = modifier
            // STYLUS vs FINGER DISPATCHER:
            // pointerInput(Unit) so the coroutine is NEVER cancelled mid-drag/pinch.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    if (down.type == PointerType.Touch) {
                        // FINGER: pan & pinch-to-zoom ONLY — never inking!
                        down.consume()

                        var isTransforming = false
                        var accumulatedPan = Offset.Zero
                        val touchSlop = viewConfiguration.touchSlop
                        var previousFingerCount = 1

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                            // Palm rejection: ignore finger if stylus is on-screen
                            val hasStylus = event.changes.any {
                                it.type == PointerType.Stylus || it.type == PointerType.Eraser
                            }
                            if (hasStylus) {
                                event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                                continue
                            }

                            val pressedFingers = event.changes.filter {
                                it.type == PointerType.Touch && it.pressed
                            }
                            if (pressedFingers.isEmpty()) {
                                if (!isTransforming && accumulatedPan.getDistance() < touchSlop) {
                                    currentOnCanvasTap()
                                }
                                break
                            }

                            // If finger count changed (e.g. 2nd finger touched down or 1 finger lifted),
                            // skip transform on this transition frame to eliminate centroid jump & stutter!
                            if (pressedFingers.size != previousFingerCount) {
                                previousFingerCount = pressedFingers.size
                                pressedFingers.forEach { it.consume() }
                                continue
                            }

                            if (pressedFingers.size >= 2) {
                                // 2-finger pinch-to-zoom & pan
                                isTransforming = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                pressedFingers.forEach { it.consume() }

                                if (!zoom.isNaN() && zoom > 0f) {
                                    val significantZoom = if (kotlin.math.abs(zoom - 1f) > 0.001f) zoom else 1f
                                    currentOnTransform(centroid, pan, significantZoom)
                                }
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
                    }
                    // STYLUS: handled by InProgressStrokes or Eraser Box below
                }
            }
    ) {
        // ─── Layer 1: Completed (dry) strokes rendered with hardware canvas transform & vector LOD ───
        // nativeCanvas.translate + nativeCanvas.scale physically locks strokes to the notebook paper,
        // while strokeToScreenTransform = strokeToScreenMatrix feeds dynamic zoom LOD to CanvasStrokeRenderer!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = currentScale
            val px = currentPanX
            val py = currentPanY

            strokeToScreenMatrix.reset()
            strokeToScreenMatrix.postScale(s, s)
            strokeToScreenMatrix.postTranslate(px, py)

            val nativeCanvas = drawContext.canvas.nativeCanvas
            nativeCanvas.withSave {
                nativeCanvas.translate(px, py)
                nativeCanvas.scale(s, s)

                strokes.fastForEach { stroke ->
                    canvasStrokeRenderer.draw(
                        canvas = nativeCanvas,
                        stroke = stroke,
                        strokeToScreenTransform = strokeToScreenMatrix,
                    )
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
                // pointerEventToWorldMatrix is the exact mathematical inverse of strokeToScreen,
                // so "wet" (in-progress) strokes land in the same world space as "dry" strokes.
                InProgressStrokes(
                    defaultBrush = currentBrush,
                    nextBrush = onGetNextBrush,
                    pointerEventToWorldTransform = pointerEventToWorldMatrix,
                    strokeToWorldTransform = identityComposeMatrix,
                    onStrokesFinished = onStrokesFinished
                )
            }
        }
    }
}
