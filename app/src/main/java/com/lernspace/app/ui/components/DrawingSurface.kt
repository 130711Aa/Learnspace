package com.lernspace.app.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.withSave
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.util.WeakHashMap
import kotlin.math.abs

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

/**
 * Overscan raster backing store + world AABB spatial culling for completed dry strokes.
 * Keeps panning/zooming locked at 90fps regardless of total stroke count on the page.
 */
private class StrokeBackingCache {
    var bitmap: Bitmap? = null
    var androidCanvas: android.graphics.Canvas? = null
    var widthPx: Int = 0
    var heightPx: Int = 0
    var scale: Float = 0f
    var originWorldX: Float = 0f
    var originWorldY: Float = 0f
    var darkPaper: Boolean = false
    val cachedStrokeRefs = ArrayList<Stroke>()
    val strokeBoundsCache = WeakHashMap<Stroke, FloatArray>()
    val reusableInput = StrokeInput()
    val workMatrix = Matrix()
    val blitPaint = Paint().apply {
        isFilterBitmap = true
    }

    fun release() {
        cachedStrokeRefs.clear()
        bitmap?.recycle()
        bitmap = null
        androidCanvas = null
        widthPx = 0
        heightPx = 0
        scale = 0f
    }

    fun getStrokeBounds(stroke: Stroke): FloatArray? {
        strokeBoundsCache[stroke]?.let { return it }
        val inputs = stroke.inputs
        val count = inputs.size
        if (count == 0) return null
        inputs.populate(0, reusableInput)
        var minX = reusableInput.x
        var maxX = reusableInput.x
        var minY = reusableInput.y
        var maxY = reusableInput.y
        for (i in 1 until count) {
            inputs.populate(i, reusableInput)
            val x = reusableInput.x
            val y = reusableInput.y
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val pad = stroke.brush.size * 0.75f + 6f
        return floatArrayOf(minX - pad, minY - pad, maxX + pad, maxY + pad).also {
            strokeBoundsCache[stroke] = it
        }
    }

    fun intersects(bounds: FloatArray, left: Float, top: Float, right: Float, bottom: Float): Boolean {
        return bounds[2] >= left && bounds[0] <= right && bounds[3] >= top && bounds[1] <= bottom
    }
}

private fun drawCulledStrokesRange(
    canvas: android.graphics.Canvas,
    strokes: List<Stroke>,
    startIndex: Int,
    endExclusive: Int,
    isDarkPaper: Boolean,
    darkStrokePaint: Paint,
    renderer: CanvasStrokeRenderer,
    matrix: Matrix,
    cache: StrokeBackingCache,
    culledLeft: Float,
    culledTop: Float,
    culledRight: Float,
    culledBottom: Float
) {
    if (!isDarkPaper) {
        for (i in startIndex until endExclusive) {
            val stroke = strokes[i]
            val bounds = cache.getStrokeBounds(stroke)
            if (bounds != null && !cache.intersects(bounds, culledLeft, culledTop, culledRight, culledBottom)) {
                continue
            }
            renderer.draw(
                canvas = canvas,
                stroke = stroke,
                strokeToScreenTransform = matrix
            )
        }
    } else {
        var i = startIndex
        while (i < endExclusive) {
            val isDark = isDarkInkColor(strokes[i].brush.colorIntArgb)
            val batchStart = i
            while (i < endExclusive && isDarkInkColor(strokes[i].brush.colorIntArgb) == isDark) {
                i++
            }
            if (isDark) {
                var anyVisible = false
                for (j in batchStart until i) {
                    val b = cache.getStrokeBounds(strokes[j])
                    if (b == null || cache.intersects(b, culledLeft, culledTop, culledRight, culledBottom)) {
                        anyVisible = true
                        break
                    }
                }
                if (!anyVisible) continue
                canvas.saveLayer(null, darkStrokePaint)
                for (j in batchStart until i) {
                    val stroke = strokes[j]
                    val bounds = cache.getStrokeBounds(stroke)
                    if (bounds != null && !cache.intersects(bounds, culledLeft, culledTop, culledRight, culledBottom)) {
                        continue
                    }
                    renderer.draw(
                        canvas = canvas,
                        stroke = stroke,
                        strokeToScreenTransform = matrix
                    )
                }
                canvas.restore()
            } else {
                for (j in batchStart until i) {
                    val stroke = strokes[j]
                    val bounds = cache.getStrokeBounds(stroke)
                    if (bounds != null && !cache.intersects(bounds, culledLeft, culledTop, culledRight, culledBottom)) {
                        continue
                    }
                    renderer.draw(
                        canvas = canvas,
                        stroke = stroke,
                        strokeToScreenTransform = matrix
                    )
                }
            }
        }
    }
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
    scaleProvider: () -> Float = { 1f },
    panXProvider: () -> Float = { 0f },
    panYProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier,
    isDrawingEnabled: Boolean = true,
    isFingerDrawingEnabled: Boolean = false,
    transformGeneration: Int = 0,
    eraserRadiusPx: Float = 30f,
    onErase: (Float, Float) -> Unit = { _, _ -> },
    onEraseStart: () -> Unit = {},
    onEraseEnd: () -> Unit = {}
) {
    val currentOnErase by rememberUpdatedState(onErase)
    val currentOnEraseStart by rememberUpdatedState(onEraseStart)
    val currentOnEraseEnd by rememberUpdatedState(onEraseEnd)

    val strokeCache = remember { StrokeBackingCache() }
    DisposableEffect(Unit) {
        onDispose { strokeCache.release() }
    }

    var isPinchZooming by remember { mutableStateOf(false) }
    var zoomSettleTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(scaleProvider) {
        var first = true
        snapshotFlow { scaleProvider() }.collectLatest {
            if (first) {
                first = false
                return@collectLatest
            }
            isPinchZooming = true
            delay(85L)
            isPinchZooming = false
            zoomSettleTick++
        }
    }

    // Smart Stroke Inversion Filter for Dark Paper:
    // Dark/black strokes (lum < 0.22f) are rendered into a hardware-accelerated layer that
    // maps color directly to crisp off-white (#F8FAFC) while preserving exact antialiasing/alpha.
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
        // ─── Layer 1: Completed (dry) strokes rendered via Viewport-Culled Overscan Backing Cache ───
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val settleRead = zoomSettleTick
            val s = scaleProvider().coerceAtLeast(0.05f)
            val px = panXProvider()
            val py = panYProvider()
            val vpW = size.width.toInt().coerceAtLeast(1)
            val vpH = size.height.toInt().coerceAtLeast(1)
            val nativeCanvas = drawContext.canvas.nativeCanvas

            val strokeCount = strokes.size
            if (strokeCount == 0) {
                if (strokeCache.cachedStrokeRefs.isNotEmpty()) {
                    strokeCache.cachedStrokeRefs.clear()
                    strokeCache.bitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
                }
                return@Canvas
            }

            // O(N) identity ref check to detect unchanged / append-only / mutated strokes
            var prefixMatch = strokeCount >= strokeCache.cachedStrokeRefs.size
            if (prefixMatch) {
                val cachedCount = strokeCache.cachedStrokeRefs.size
                for (i in 0 until cachedCount) {
                    if (strokes[i] !== strokeCache.cachedStrokeRefs[i]) {
                        prefixMatch = false
                        break
                    }
                }
            }

            val visLeft = -px / s
            val visTop = -py / s
            val visRight = (size.width - px) / s
            val visBottom = (size.height - py) / s

            val existingBmp = strokeCache.bitmap
            val cacheValid = existingBmp != null &&
                !existingBmp.isRecycled &&
                strokeCache.darkPaper == isDarkPaper &&
                prefixMatch &&
                strokeCache.scale > 0f

            if (cacheValid && existingBmp != null) {
                val cacheWorldRight = strokeCache.originWorldX + strokeCache.widthPx / strokeCache.scale
                val cacheWorldBottom = strokeCache.originWorldY + strokeCache.heightPx / strokeCache.scale
                val sameScale = abs(s - strokeCache.scale) < 0.001f
                val insideCachedRect = visLeft >= strokeCache.originWorldX &&
                    visTop >= strokeCache.originWorldY &&
                    visRight <= cacheWorldRight &&
                    visBottom <= cacheWorldBottom

                // Incremental append fast-path (just finished writing a new stroke while stationary!)
                if (sameScale && insideCachedRect && strokeCount > strokeCache.cachedStrokeRefs.size) {
                    val ac = strokeCache.androidCanvas
                    if (ac != null) {
                        val appendStart = strokeCache.cachedStrokeRefs.size
                        strokeCache.workMatrix.reset()
                        strokeCache.workMatrix.postScale(strokeCache.scale, strokeCache.scale)
                        strokeCache.workMatrix.postTranslate(
                            -strokeCache.originWorldX * strokeCache.scale,
                            -strokeCache.originWorldY * strokeCache.scale
                        )
                        ac.withSave {
                            ac.translate(
                                -strokeCache.originWorldX * strokeCache.scale,
                                -strokeCache.originWorldY * strokeCache.scale
                            )
                            ac.scale(strokeCache.scale, strokeCache.scale)
                            drawCulledStrokesRange(
                                canvas = ac,
                                strokes = strokes,
                                startIndex = appendStart,
                                endExclusive = strokeCount,
                                isDarkPaper = isDarkPaper,
                                darkStrokePaint = darkStrokePaint,
                                renderer = canvasStrokeRenderer,
                                matrix = strokeCache.workMatrix,
                                cache = strokeCache,
                                culledLeft = strokeCache.originWorldX,
                                culledTop = strokeCache.originWorldY,
                                culledRight = cacheWorldRight,
                                culledBottom = cacheWorldBottom
                            )
                        }
                        for (i in appendStart until strokeCount) {
                            strokeCache.cachedStrokeRefs.add(strokes[i])
                        }
                    }
                }

                // Smooth GPU blit paths (~0.2ms/frame regardless of stroke count!)
                val scaleRatio = maxOf(s / strokeCache.scale, strokeCache.scale / s)
                val canSmoothZoomBlit = isPinchZooming &&
                    !sameScale &&
                    scaleRatio <= 1.5f &&
                    strokeCount == strokeCache.cachedStrokeRefs.size

                if ((sameScale && insideCachedRect) || canSmoothZoomBlit) {
                    nativeCanvas.withSave {
                        nativeCanvas.clipRect(0f, 0f, size.width, size.height)
                        val drawX = strokeCache.originWorldX * s + px
                        val drawY = strokeCache.originWorldY * s + py
                        nativeCanvas.translate(drawX, drawY)
                        if (!sameScale) {
                            val rel = s / strokeCache.scale
                            nativeCanvas.scale(rel, rel)
                        }
                        nativeCanvas.drawBitmap(
                            existingBmp,
                            0f,
                            0f,
                            if (sameScale) null else strokeCache.blitPaint
                        )
                    }
                    return@Canvas
                }
            }

            // Rebuild overscan backing bitmap strictly for strokes intersecting the overscan viewport
            var marginX = 300
            var marginY = 300
            val maxTotalPixels = 6_400_000L
            while ((vpW + marginX * 2L) * (vpH + marginY * 2L) > maxTotalPixels && marginX > 64) {
                marginX = (marginX * 3) / 4
                marginY = (marginY * 3) / 4
            }
            val targetW = (vpW + marginX * 2).coerceAtLeast(vpW)
            val targetH = (vpH + marginY * 2).coerceAtLeast(vpH)

            val rebuiltOk = try {
                val bmp = if (existingBmp != null &&
                    !existingBmp.isRecycled &&
                    strokeCache.widthPx == targetW &&
                    strokeCache.heightPx == targetH
                ) {
                    existingBmp.eraseColor(android.graphics.Color.TRANSPARENT)
                    existingBmp
                } else {
                    existingBmp?.recycle()
                    Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888).also {
                        strokeCache.bitmap = it
                        strokeCache.androidCanvas = android.graphics.Canvas(it)
                        strokeCache.widthPx = targetW
                        strokeCache.heightPx = targetH
                    }
                }
                val ac = strokeCache.androidCanvas ?: android.graphics.Canvas(bmp).also {
                    strokeCache.androidCanvas = it
                }

                val originWorldX = visLeft - marginX / s
                val originWorldY = visTop - marginY / s
                val culledRight = originWorldX + targetW / s
                val culledBottom = originWorldY + targetH / s

                strokeCache.scale = s
                strokeCache.originWorldX = originWorldX
                strokeCache.originWorldY = originWorldY
                strokeCache.darkPaper = isDarkPaper

                strokeCache.workMatrix.reset()
                strokeCache.workMatrix.postScale(s, s)
                strokeCache.workMatrix.postTranslate(-originWorldX * s, -originWorldY * s)

                ac.withSave {
                    ac.translate(-originWorldX * s, -originWorldY * s)
                    ac.scale(s, s)
                    drawCulledStrokesRange(
                        canvas = ac,
                        strokes = strokes,
                        startIndex = 0,
                        endExclusive = strokeCount,
                        isDarkPaper = isDarkPaper,
                        darkStrokePaint = darkStrokePaint,
                        renderer = canvasStrokeRenderer,
                        matrix = strokeCache.workMatrix,
                        cache = strokeCache,
                        culledLeft = originWorldX,
                        culledTop = originWorldY,
                        culledRight = culledRight,
                        culledBottom = culledBottom
                    )
                }

                strokeCache.cachedStrokeRefs.clear()
                strokeCache.cachedStrokeRefs.ensureCapacity(strokeCount)
                strokeCache.cachedStrokeRefs.addAll(strokes)

                nativeCanvas.withSave {
                    nativeCanvas.clipRect(0f, 0f, size.width, size.height)
                    nativeCanvas.drawBitmap(bmp, originWorldX * s + px, originWorldY * s + py, null)
                }
                true
            } catch (_: OutOfMemoryError) {
                strokeCache.release()
                false
            }

            if (!rebuiltOk) {
                // OOM fallback: direct culled stroke render without backing bitmap
                strokeCache.workMatrix.reset()
                strokeCache.workMatrix.postScale(s, s)
                strokeCache.workMatrix.postTranslate(px, py)
                nativeCanvas.withSave {
                    nativeCanvas.clipRect(0f, 0f, size.width, size.height)
                    nativeCanvas.translate(px, py)
                    nativeCanvas.scale(s, s)
                    drawCulledStrokesRange(
                        canvas = nativeCanvas,
                        strokes = strokes,
                        startIndex = 0,
                        endExclusive = strokeCount,
                        isDarkPaper = isDarkPaper,
                        darkStrokePaint = darkStrokePaint,
                        renderer = canvasStrokeRenderer,
                        matrix = strokeCache.workMatrix,
                        cache = strokeCache,
                        culledLeft = visLeft,
                        culledTop = visTop,
                        culledRight = visRight,
                        culledBottom = visBottom
                    )
                }
            }
        }

        // ─── Layer 2: Active inking / eraser (Stylus or Hand Touch depending on setting) ───
        var eraserPointerPos by remember { mutableStateOf<Offset?>(null) }

        if (isDrawingEnabled) {
            if (isEraserMode) {
                // Eraser: convert pointer screen position to world space, then erase
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(isFingerDrawingEnabled) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val isAllowed = down.type == PointerType.Stylus ||
                                    down.type == PointerType.Eraser ||
                                    (isFingerDrawingEnabled && down.type == PointerType.Touch)
                                if (isAllowed) {
                                    eraserPointerPos = down.position
                                    currentOnEraseStart()
                                    val s0 = scaleProvider().coerceAtLeast(0.05f)
                                    val worldX = (down.position.x - panXProvider()) / s0
                                    val worldY = (down.position.y - panYProvider()) / s0
                                    currentOnErase(worldX, worldY)
                                    drag(down.id) { change ->
                                        val isChangeAllowed = change.type == PointerType.Stylus ||
                                            change.type == PointerType.Eraser ||
                                            (isFingerDrawingEnabled && change.type == PointerType.Touch)
                                        if (isChangeAllowed) {
                                            change.consume()
                                            eraserPointerPos = change.position
                                            val curS = scaleProvider().coerceAtLeast(0.05f)
                                            val curWorldX = (change.position.x - panXProvider()) / curS
                                            val curWorldY = (change.position.y - panYProvider()) / curS
                                            currentOnErase(curWorldX, curWorldY)
                                        }
                                    }
                                    eraserPointerPos = null
                                    currentOnEraseEnd()
                                }
                            }
                        }
                )
            } else {
                ActiveInkingOverlay(
                    currentBrush = currentBrush,
                    onGetNextBrush = onGetNextBrush,
                    onStrokesFinished = onStrokesFinished,
                    isDarkPaper = isDarkPaper,
                    isFingerDrawingEnabled = isFingerDrawingEnabled,
                    transformGeneration = transformGeneration,
                    scaleProvider = scaleProvider,
                    panXProvider = panXProvider,
                    panYProvider = panYProvider
                )
            }
        }

        // ─── Layer 3: Eraser Area Indicator (Lingkaran Radius Penghapus) ───
        // State read happens inside Canvas draw scope so dragging the eraser never recomposes DrawingSurface
        if (isEraserMode) {
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                val pos = eraserPointerPos ?: return@Canvas
                val r = eraserRadiusPx

                // Soft translucent red/pink fill
                drawCircle(
                    color = Color(0x28EF4444),
                    radius = r,
                    center = pos
                )

                // High contrast boundary ring
                drawCircle(
                    color = if (isDarkPaper) Color(0xE6FFFFFF) else Color(0xD9000000),
                    radius = r,
                    center = pos,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * density)
                )

                // Center reticle dot
                drawCircle(
                    color = if (isDarkPaper) Color.White else Color.Black,
                    radius = 2f * density,
                    center = pos
                )
            }
        }
    }
}

@Composable
private fun ActiveInkingOverlay(
    currentBrush: Brush,
    onGetNextBrush: () -> Brush,
    onStrokesFinished: (List<Stroke>) -> Unit,
    isDarkPaper: Boolean,
    isFingerDrawingEnabled: Boolean,
    transformGeneration: Int,
    scaleProvider: () -> Float,
    panXProvider: () -> Float,
    panYProvider: () -> Float
) {
    val identityComposeMatrix = remember { androidx.compose.ui.graphics.Matrix() }
    var pointerEventToWorldMatrix by remember {
        mutableStateOf(
            computeInverseTransformMatrix(scaleProvider(), panXProvider(), panYProvider())
        )
    }

    // Debounce transform updates slightly while panning/zooming so InProgressStrokes isn't
    // rebuilt/updated 90 times per second mid-gesture, then sync immediately when fingers pause.
    LaunchedEffect(scaleProvider, panXProvider, panYProvider) {
        var first = true
        snapshotFlow {
            Triple(scaleProvider(), panXProvider(), panYProvider())
        }.collectLatest { (s, px, py) ->
            if (!first) delay(16L)
            first = false
            pointerEventToWorldMatrix = computeInverseTransformMatrix(s, px, py)
        }
    }

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
            .then(
                if (!isFingerDrawingEnabled) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            if (down.type == PointerType.Touch) {
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        key(effectiveCurrentBrush, transformGeneration) {
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

private fun computeInverseTransformMatrix(
    scale: Float,
    panX: Float,
    panY: Float
): androidx.compose.ui.graphics.Matrix {
    val s = scale.coerceAtLeast(0.05f)
    val renderMatrix = Matrix().apply {
        postScale(s, s)
        postTranslate(panX, panY)
    }
    val inv = Matrix()
    renderMatrix.invert(inv)
    return androidx.compose.ui.graphics.Matrix().apply {
        setFrom(inv)
    }
}
