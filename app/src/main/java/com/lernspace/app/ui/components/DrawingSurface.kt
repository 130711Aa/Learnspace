package com.lernspace.app.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
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
import kotlin.math.ceil
import kotlin.math.floor

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

private const val CHUNK_WORLD_SIZE = 512f

private class StrokeBoundsIndex {
    val boundsCache = WeakHashMap<Stroke, FloatArray>()
    val reusableInput = StrokeInput()

    fun getStrokeBounds(stroke: Stroke): FloatArray? {
        boundsCache[stroke]?.let { return it }
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
            boundsCache[stroke] = it
        }
    }

    fun intersects(bounds: FloatArray, left: Float, top: Float, right: Float, bottom: Float): Boolean {
        return bounds[2] >= left && bounds[0] <= right && bounds[3] >= top && bounds[1] <= bottom
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private class WorldStrokeChunk(
    val key: Long
) {
    val strokes = ArrayList<Stroke>()
    var minX: Float = Float.POSITIVE_INFINITY
    var minY: Float = Float.POSITIVE_INFINITY
    var maxX: Float = Float.NEGATIVE_INFINITY
    var maxY: Float = Float.NEGATIVE_INFINITY
    val renderNode = RenderNode("StrokeChunk").apply {
        setClipToBounds(false)
    }
    var recorded: Boolean = false
    var recordedDarkPaper: Boolean = false
    private val localMatrix = Matrix()

    fun addStroke(stroke: Stroke, bounds: FloatArray?) {
        strokes.add(stroke)
        if (bounds != null) {
            if (bounds[0] < minX) minX = bounds[0]
            if (bounds[1] < minY) minY = bounds[1]
            if (bounds[2] > maxX) maxX = bounds[2]
            if (bounds[3] > maxY) maxY = bounds[3]
        }
        recorded = false
    }

    fun replaceIfChanged(nextList: List<Stroke>, boundsIndex: StrokeBoundsIndex): Boolean {
        if (strokes.size == nextList.size) {
            var allSame = true
            for (i in 0 until strokes.size) {
                if (strokes[i] !== nextList[i]) {
                    allSame = false
                    break
                }
            }
            if (allSame) return false
        }
        strokes.clear()
        strokes.ensureCapacity(nextList.size)
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        for (i in 0 until nextList.size) {
            val s = nextList[i]
            strokes.add(s)
            val b = boundsIndex.getStrokeBounds(s)
            if (b != null) {
                if (b[0] < minX) minX = b[0]
                if (b[1] < minY) minY = b[1]
                if (b[2] > maxX) maxX = b[2]
                if (b[3] > maxY) maxY = b[3]
            }
        }
        recorded = false
        return true
    }

    fun intersects(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        if (strokes.isEmpty() || minX == Float.POSITIVE_INFINITY) return true
        return maxX >= left && minX <= right && maxY >= top && minY <= bottom
    }

    fun ensureRecorded(
        isDarkPaper: Boolean,
        darkStrokePaint: Paint,
        renderer: CanvasStrokeRenderer
    ) {
        if (recorded && recordedDarkPaper == isDarkPaper && renderNode.hasDisplayList()) {
            return
        }
        val w = if (maxX > minX) ceil(maxX - minX).toInt().coerceAtLeast(1) else 512
        val h = if (maxY > minY) ceil(maxY - minY).toInt().coerceAtLeast(1) else 512
        val ox = if (minX.isFinite()) minX else 0f
        val oy = if (minY.isFinite()) minY else 0f

        renderNode.setPosition(0, 0, w, h)
        localMatrix.reset()
        localMatrix.postTranslate(-ox, -oy)

        val rc = renderNode.beginRecording(w, h)
        try {
            rc.translate(-ox, -oy)
            if (!isDarkPaper) {
                for (i in 0 until strokes.size) {
                    renderer.draw(
                        canvas = rc,
                        stroke = strokes[i],
                        strokeToScreenTransform = localMatrix
                    )
                }
            } else {
                var i = 0
                val count = strokes.size
                while (i < count) {
                    val isDark = isDarkInkColor(strokes[i].brush.colorIntArgb)
                    val batchStart = i
                    while (i < count && isDarkInkColor(strokes[i].brush.colorIntArgb) == isDark) {
                        i++
                    }
                    if (isDark) {
                        rc.saveLayer(null, darkStrokePaint)
                        for (j in batchStart until i) {
                            renderer.draw(
                                canvas = rc,
                                stroke = strokes[j],
                                strokeToScreenTransform = localMatrix
                            )
                        }
                        rc.restore()
                    } else {
                        for (j in batchStart until i) {
                            renderer.draw(
                                canvas = rc,
                                stroke = strokes[j],
                                strokeToScreenTransform = localMatrix
                            )
                        }
                    }
                }
            }
        } finally {
            renderNode.endRecording()
        }
        recorded = true
        recordedDarkPaper = isDarkPaper
    }

    fun discard() {
        renderNode.discardDisplayList()
        strokes.clear()
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private class GpuChunkCache(
    private val boundsIndex: StrokeBoundsIndex
) {
    val cachedStrokeRefs = ArrayList<Stroke>()
    val chunksMap = HashMap<Long, WorldStrokeChunk>()
    val chunksList = ArrayList<WorldStrokeChunk>()
    private val tempBuckets = HashMap<Long, ArrayList<Stroke>>()
    var lastDarkPaper: Boolean = false

    fun release() {
        for (i in 0 until chunksList.size) {
            chunksList[i].discard()
        }
        chunksList.clear()
        chunksMap.clear()
        cachedStrokeRefs.clear()
    }

    private fun chunkKeyForStroke(stroke: Stroke): Long {
        val bounds = boundsIndex.getStrokeBounds(stroke) ?: return 0L
        val cx = (bounds[0] + bounds[2]) * 0.5f
        val cy = (bounds[1] + bounds[3]) * 0.5f
        val gx = floor(cx / CHUNK_WORLD_SIZE).toInt()
        val gy = floor(cy / CHUNK_WORLD_SIZE).toInt()
        return (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)
    }

    fun syncStrokes(strokes: List<Stroke>, isDarkPaper: Boolean) {
        val count = strokes.size
        if (count == 0) {
            if (cachedStrokeRefs.isNotEmpty()) release()
            lastDarkPaper = isDarkPaper
            return
        }

        var prefixMatch = count >= cachedStrokeRefs.size
        if (prefixMatch) {
            val prevCount = cachedStrokeRefs.size
            for (i in 0 until prevCount) {
                if (strokes[i] !== cachedStrokeRefs[i]) {
                    prefixMatch = false
                    break
                }
            }
        }

        if (prefixMatch && count == cachedStrokeRefs.size && lastDarkPaper == isDarkPaper) {
            return
        }

        if (prefixMatch) {
            for (i in cachedStrokeRefs.size until count) {
                val s = strokes[i]
                addStrokeToChunk(s)
                cachedStrokeRefs.add(s)
            }
        } else {
            // Smart per-chunk diff: only chunks whose strokes changed (e.g. under eraser) re-record!
            tempBuckets.values.forEach { it.clear() }
            for (i in 0 until count) {
                val s = strokes[i]
                val key = chunkKeyForStroke(s)
                var list = tempBuckets[key]
                if (list == null) {
                    list = ArrayList()
                    tempBuckets[key] = list
                }
                list.add(s)
            }

            // Discard chunks that are now empty
            for (idx in chunksList.indices.reversed()) {
                val ch = chunksList[idx]
                val bucket = tempBuckets[ch.key]
                if (bucket == null || bucket.isEmpty()) {
                    ch.discard()
                    chunksMap.remove(ch.key)
                    chunksList.removeAt(idx)
                }
            }

            // Diff existing chunks and add new ones
            for ((key, bucket) in tempBuckets) {
                if (bucket.isEmpty()) continue
                val existing = chunksMap[key]
                if (existing != null) {
                    existing.replaceIfChanged(bucket, boundsIndex)
                } else {
                    val newChunk = WorldStrokeChunk(key)
                    newChunk.replaceIfChanged(bucket, boundsIndex)
                    chunksMap[key] = newChunk
                    chunksList.add(newChunk)
                }
            }

            cachedStrokeRefs.clear()
            cachedStrokeRefs.ensureCapacity(count)
            cachedStrokeRefs.addAll(strokes)
        }
        lastDarkPaper = isDarkPaper
    }

    private fun addStrokeToChunk(stroke: Stroke) {
        val key = chunkKeyForStroke(stroke)
        val chunk = chunksMap.getOrPut(key) {
            WorldStrokeChunk(key).also { chunksList.add(it) }
        }
        chunk.addStroke(stroke, boundsIndex.getStrokeBounds(stroke))
    }
}

private fun drawCulledStrokesDirect(
    canvas: android.graphics.Canvas,
    strokes: List<Stroke>,
    isDarkPaper: Boolean,
    darkStrokePaint: Paint,
    renderer: CanvasStrokeRenderer,
    matrix: Matrix,
    boundsIndex: StrokeBoundsIndex,
    visLeft: Float,
    visTop: Float,
    visRight: Float,
    visBottom: Float
) {
    val count = strokes.size
    if (!isDarkPaper) {
        for (i in 0 until count) {
            val stroke = strokes[i]
            val bounds = boundsIndex.getStrokeBounds(stroke)
            if (bounds != null && !boundsIndex.intersects(bounds, visLeft, visTop, visRight, visBottom)) {
                continue
            }
            renderer.draw(
                canvas = canvas,
                stroke = stroke,
                strokeToScreenTransform = matrix
            )
        }
    } else {
        var i = 0
        while (i < count) {
            val isDark = isDarkInkColor(strokes[i].brush.colorIntArgb)
            val batchStart = i
            while (i < count && isDarkInkColor(strokes[i].brush.colorIntArgb) == isDark) {
                i++
            }
            if (isDark) {
                var anyVisible = false
                for (j in batchStart until i) {
                    val b = boundsIndex.getStrokeBounds(strokes[j])
                    if (b == null || boundsIndex.intersects(b, visLeft, visTop, visRight, visBottom)) {
                        anyVisible = true
                        break
                    }
                }
                if (!anyVisible) continue
                canvas.saveLayer(null, darkStrokePaint)
                for (j in batchStart until i) {
                    val stroke = strokes[j]
                    val bounds = boundsIndex.getStrokeBounds(stroke)
                    if (bounds != null && !boundsIndex.intersects(bounds, visLeft, visTop, visRight, visBottom)) {
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
                    val bounds = boundsIndex.getStrokeBounds(stroke)
                    if (bounds != null && !boundsIndex.intersects(bounds, visLeft, visTop, visRight, visBottom)) {
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
    val currentScaleProvider by rememberUpdatedState(scaleProvider)
    val currentPanXProvider by rememberUpdatedState(panXProvider)
    val currentPanYProvider by rememberUpdatedState(panYProvider)

    val boundsIndex = remember { StrokeBoundsIndex() }
    val directMatrix = remember { Matrix() }
    val gpuChunkCache = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) GpuChunkCache(boundsIndex) else null
    }
    DisposableEffect(Unit) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                gpuChunkCache?.release()
            }
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
        // ─── Layer 1: Completed (dry) strokes via GPU Hardware Chunked RenderNodes (0 CPU bitmap uploads!) ───
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (strokes.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    gpuChunkCache?.syncStrokes(strokes, isDarkPaper)
                }
                return@Canvas
            }

            val s = currentScaleProvider().coerceAtLeast(0.05f)
            val px = currentPanXProvider()
            val py = currentPanYProvider()
            val visLeft = -px / s
            val visTop = -py / s
            val visRight = (size.width - px) / s
            val visBottom = (size.height - py) / s
            val nativeCanvas = drawContext.canvas.nativeCanvas

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                nativeCanvas.isHardwareAccelerated &&
                gpuChunkCache != null
            ) {
                gpuChunkCache.syncStrokes(strokes, isDarkPaper)
                val chunks = gpuChunkCache.chunksList

                nativeCanvas.withSave {
                    nativeCanvas.clipRect(0f, 0f, size.width, size.height)
                    nativeCanvas.translate(px, py)
                    nativeCanvas.scale(s, s)

                    for (i in 0 until chunks.size) {
                        val chunk = chunks[i]
                        if (!chunk.intersects(visLeft, visTop, visRight, visBottom)) continue
                        chunk.ensureRecorded(isDarkPaper, darkStrokePaint, canvasStrokeRenderer)

                        val ox = if (chunk.minX.isFinite()) chunk.minX else 0f
                        val oy = if (chunk.minY.isFinite()) chunk.minY else 0f
                        nativeCanvas.withSave {
                            nativeCanvas.translate(ox, oy)
                            nativeCanvas.drawRenderNode(chunk.renderNode)
                        }
                    }
                }
            } else {
                // Fallback for API 26–28 or non-hardware canvas: direct AABB-culled rendering
                directMatrix.reset()
                directMatrix.postScale(s, s)
                directMatrix.postTranslate(px, py)
                nativeCanvas.withSave {
                    nativeCanvas.clipRect(0f, 0f, size.width, size.height)
                    nativeCanvas.translate(px, py)
                    nativeCanvas.scale(s, s)
                    drawCulledStrokesDirect(
                        canvas = nativeCanvas,
                        strokes = strokes,
                        isDarkPaper = isDarkPaper,
                        darkStrokePaint = darkStrokePaint,
                        renderer = canvasStrokeRenderer,
                        matrix = directMatrix,
                        boundsIndex = boundsIndex,
                        visLeft = visLeft,
                        visTop = visTop,
                        visRight = visRight,
                        visBottom = visBottom
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
                                    val s0 = currentScaleProvider().coerceAtLeast(0.05f)
                                    val worldX = (down.position.x - currentPanXProvider()) / s0
                                    val worldY = (down.position.y - currentPanYProvider()) / s0
                                    currentOnErase(worldX, worldY)
                                    drag(down.id) { change ->
                                        val isChangeAllowed = change.type == PointerType.Stylus ||
                                            change.type == PointerType.Eraser ||
                                            (isFingerDrawingEnabled && change.type == PointerType.Touch)
                                        if (isChangeAllowed) {
                                            change.consume()
                                            eraserPointerPos = change.position
                                            val curS = currentScaleProvider().coerceAtLeast(0.05f)
                                            val curWorldX = (change.position.x - currentPanXProvider()) / curS
                                            val curWorldY = (change.position.y - currentPanYProvider()) / curS
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
    val currentScaleProvider by rememberUpdatedState(scaleProvider)
    val currentPanXProvider by rememberUpdatedState(panXProvider)
    val currentPanYProvider by rememberUpdatedState(panYProvider)

    val identityComposeMatrix = remember { androidx.compose.ui.graphics.Matrix() }
    var pointerEventToWorldMatrix by remember {
        mutableStateOf(
            computeInverseTransformMatrix(currentScaleProvider(), currentPanXProvider(), currentPanYProvider())
        )
    }

    // Debounce 120ms so InProgressStrokes NEVER updates/recomposes mid-pan on 60Hz or 90Hz displays!
    LaunchedEffect(Unit) {
        var first = true
        snapshotFlow {
            Triple(currentScaleProvider(), currentPanXProvider(), currentPanYProvider())
        }.collectLatest { (s, px, py) ->
            if (!first) delay(120L)
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
                Modifier.pointerInput(isFingerDrawingEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        if (down.type == PointerType.Touch && !isFingerDrawingEnabled) {
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                                if (event.changes.none { it.pressed }) break
                            }
                        } else if (down.type == PointerType.Stylus || down.type == PointerType.Eraser ||
                            (isFingerDrawingEnabled && down.type == PointerType.Touch)
                        ) {
                            // Immediately sync transform if user switches from pan to stroke within <120ms
                            val latest = computeInverseTransformMatrix(
                                currentScaleProvider(),
                                currentPanXProvider(),
                                currentPanYProvider()
                            )
                            if (latest != pointerEventToWorldMatrix) {
                                pointerEventToWorldMatrix = latest
                            }
                        }
                    }
                }
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
