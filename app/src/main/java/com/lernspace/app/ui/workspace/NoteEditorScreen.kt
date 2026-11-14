package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.core.ai.HandwrittenNoteRecognizer
import com.lernspace.app.core.audio.NoteAudioController
import com.lernspace.app.ui.workspace.components.AudioLectureBar
import com.lernspace.app.data.local.AcademicDatabaseManager
import com.lernspace.app.data.local.NoteStorageManager
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.components.DrawingSurface
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.DraggableImage
import com.lernspace.app.ui.workspace.components.DraggableTextBox
import com.lernspace.app.ui.workspace.components.MarkdownLatexContent
import com.lernspace.app.ui.workspace.components.NoteFloatingToolbar
import com.lernspace.app.ui.workspace.components.PaperBackground
import com.lernspace.app.ui.workspace.lasso.LassoHelper
import com.lernspace.app.ui.workspace.lasso.LassoSelectionState
import com.lernspace.app.ui.workspace.model.*
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer
import com.lernspace.app.ui.workspace.gesture.ScribbleEraseRecognizer
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.EraserMode
import com.lernspace.app.data.local.Handedness
import com.lernspace.app.data.local.TouchInputMode
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: Detect if stroke points were held stationary at the end (hold-to-refine)
// ─────────────────────────────────────────────────────────────────────────────
private fun isStrokeHoldAtEnd(stroke: Stroke, currentScale: Float): Boolean {
    val threshold = AppSettingsManager.shapeSensitivity.thresholdMs
    if (threshold == Long.MAX_VALUE) return false

    val inputs = stroke.inputs
    val count = inputs.size
    if (count < 8) return false
    val temp = androidx.ink.strokes.StrokeInput()
    inputs.populate(count - 1, temp)
    val endX = temp.x
    val endY = temp.y
    val endT = temp.elapsedTimeMillis

    val maxJiggleDistance = maxOf(18f, 32f / currentScale)
    var stationaryStartTime = endT
    for (i in count - 2 downTo 0) {
        inputs.populate(i, temp)
        val d = kotlin.math.hypot(temp.x - endX, temp.y - endY)
        if (d > maxJiggleDistance) {
            stationaryStartTime = temp.elapsedTimeMillis
            break
        }
    }
    return (endT - stationaryStartTime) >= threshold
}

data class PdfRenderedTile(
    val pageIndex: Int,
    val bitmap: ImageBitmap,
    val worldLeft: Float,
    val worldTop: Float,
    val worldRight: Float,
    val worldBottom: Float,
    val renderedScale: Float
)

// ─────────────────────────────────────────────────────────────────────────────
// STATE HOLDER: Encapsulates all data and canvas state for one note pane
// ─────────────────────────────────────────────────────────────────────────────
class NotePaneState(
    var note: NoteUiModel,
    private val context: android.content.Context,
    private val coroutineScope: CoroutineScope
) {
    var noteTitle by mutableStateOf(note.title)
    var paperStyle by mutableStateOf(note.paperStyle)
    var isDarkPaper by mutableStateOf(note.isDarkPaper)
    var subject by mutableStateOf(note.subject)

    fun applyUpdatedMeta(updated: NoteUiModel) {
        note = updated
        noteTitle = updated.title
        subject = updated.subject
    }

    fun toggleDarkPaper() {
        isDarkPaper = !isDarkPaper
        note.isDarkPaper = isDarkPaper
        AcademicDatabaseManager.persistNote(note)
    }

    fun updatePaperStyle(newStyle: PaperStyle) {
        paperStyle = newStyle
        note.paperStyle = newStyle
        AcademicDatabaseManager.persistNote(note)
    }
    val canvasType: NoteCanvasType get() = note.canvasType

    var currentPageIndex by mutableIntStateOf(0)
    var totalPages by mutableIntStateOf(
        if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) maxOf(1, note.pdfTotalPages)
        else maxOf(1, note.pages.size)
    )

    val strokes = mutableStateListOf<Stroke>()
    val canvasStrokeRenderer = CanvasStrokeRenderer.create(true)

    val textBoxes = mutableStateListOf<TextBoxElement>()
    val imageElements = mutableStateListOf<ImageElement>()

    data class CanvasHistorySnapshot(
        val strokes: List<Stroke>,
        val textBoxes: List<TextBoxElement>,
        val imageElements: List<ImageElement>
    )

    val undoStack = mutableStateListOf<CanvasHistorySnapshot>()
    val redoStack = mutableStateListOf<CanvasHistorySnapshot>()
    private var pendingTransactionSnapshot: CanvasHistorySnapshot? = null

    fun captureSnapshot(): CanvasHistorySnapshot = CanvasHistorySnapshot(
        strokes = strokes.toList(),
        textBoxes = textBoxes.map { it.copy(isSelected = false) },
        imageElements = imageElements.map { it.copy(isSelected = false) }
    )

    fun pushUndoSnapshot(before: CanvasHistorySnapshot = captureSnapshot()) {
        if (undoStack.size >= 60) {
            undoStack.removeAt(0)
        }
        undoStack.add(before)
        redoStack.clear()
    }

    fun beginUndoTransaction() {
        if (pendingTransactionSnapshot == null) {
            pendingTransactionSnapshot = captureSnapshot()
        }
    }

    fun commitUndoTransactionIfChanged() {
        val before = pendingTransactionSnapshot ?: return
        pendingTransactionSnapshot = null
        val now = captureSnapshot()
        if (before.strokes != now.strokes ||
            before.textBoxes != now.textBoxes ||
            before.imageElements != now.imageElements
        ) {
            pushUndoSnapshot(before)
        }
    }

    private fun applySnapshot(snapshot: CanvasHistorySnapshot) {
        strokes.clear()
        strokes.addAll(snapshot.strokes)
        textBoxes.clear()
        textBoxes.addAll(snapshot.textBoxes.map { it.copy(isSelected = false) })
        imageElements.clear()
        imageElements.addAll(snapshot.imageElements.map { it.copy(isSelected = false) })
        lassoSelection = null
    }

    var pdfPageBitmap by mutableStateOf<ImageBitmap?>(null)
    var pdfPageWorldWidth by mutableFloatStateOf(CanvasConstants.PAGE_WIDTH)
    var pdfPageWorldHeight by mutableFloatStateOf(CanvasConstants.PAGE_HEIGHT)
    var pdfTile by mutableStateOf<PdfRenderedTile?>(null)
    var pdfLoadFailed by mutableStateOf(false)

    var isLoadingFromDisk by mutableStateOf(false)

    init {
        if (note.pages.isNotEmpty()) {
            val firstPage = note.pages[0]
            strokes.addAll(firstPage.strokes)
            textBoxes.addAll(firstPage.textBoxes)
            imageElements.addAll(firstPage.imageElements)
        }
    }

    // Canvas Viewport Pan & Zoom State
    var scale by mutableFloatStateOf(1f)
    var panX by mutableFloatStateOf(0f)
    var panY by mutableFloatStateOf(0f)

    // Lasso Selection State
    var lassoSelection by mutableStateOf<LassoSelectionState?>(null)
    val currentLassoLoop = mutableStateListOf<Offset>()

    // Shape Tool live preview
    var shapeDrawStart by mutableStateOf<Offset?>(null)
    var shapeDrawCurrent by mutableStateOf<Offset?>(null)

    fun saveActivePageData(immediate: Boolean = false) {
        if (isLoadingFromDisk) return
        while (note.pages.size <= currentPageIndex) {
            note.pages.add(NotePageData(pageIndex = note.pages.size))
        }
        val target = note.pages[currentPageIndex]
        target.strokes.clear()
        target.strokes.addAll(strokes)
        target.textBoxes.clear()
        target.textBoxes.addAll(textBoxes)
        target.imageElements.clear()
        target.imageElements.addAll(imageElements)
        note.isDarkPaper = isDarkPaper
        note.paperStyle = paperStyle
        AcademicDatabaseManager.persistNote(note, immediate = immediate)
    }

    fun loadActivePageData(index: Int) {
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
        pendingTransactionSnapshot = null
        textBoxes.clear()
        imageElements.clear()
        lassoSelection = null
        currentLassoLoop.clear()
        shapeDrawStart = null
        shapeDrawCurrent = null

        while (note.pages.size <= index) {
            note.pages.add(NotePageData(pageIndex = note.pages.size))
        }
        val source = note.pages[index]
        strokes.addAll(source.strokes)
        textBoxes.addAll(source.textBoxes)
        imageElements.addAll(source.imageElements)
    }

    suspend fun loadFromDiskAsync() {
        val alreadyInRam = note.pages.any {
            it.strokes.isNotEmpty() || it.textBoxes.isNotEmpty() || it.imageElements.isNotEmpty()
        }
        if (alreadyInRam || AcademicDatabaseManager.hasPendingSave(note.id)) {
            return
        }

        isLoadingFromDisk = true
        try {
            AcademicDatabaseManager.awaitPendingSave(note.id)
            val diskPages = withContext(Dispatchers.IO) {
                AcademicDatabaseManager.loadPagesForNote(note.id)
            }
            if (diskPages != null && diskPages.isNotEmpty()) {
                note.pages.clear()
                note.pages.addAll(diskPages)
                totalPages = if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) maxOf(1, note.pdfTotalPages) else maxOf(1, note.pages.size)
            }
            loadActivePageData(currentPageIndex.coerceIn(0, (note.pages.size - 1).coerceAtLeast(0)))
        } finally {
            isLoadingFromDisk = false
        }
    }

    fun loadPdfPage() {
        if (note.canvasType == NoteCanvasType.PDF_DOCUMENT && !note.pdfUri.isNullOrBlank()) {
            val pageToLoad = currentPageIndex
            pdfTile = null
            pdfLoadFailed = false

            // Fast synchronous cache hit (instant page flip!)
            val currentUri = Uri.parse(note.pdfUri)
            val cached = PdfNoteHelper.getCachedPage(currentUri, pageToLoad, 1400)
            if (cached != null) {
                pdfPageWorldWidth = cached.worldWidth
                pdfPageWorldHeight = cached.worldHeight
                pdfPageBitmap = cached.bitmap.asImageBitmap()
            }

            coroutineScope.launch(Dispatchers.IO) {
                val resolvedUriString = NoteStorageManager.ensurePdfInternal(context, note.id, note.pdfUri!!)
                if (resolvedUriString != null && resolvedUriString != note.pdfUri) {
                    note.pdfUri = resolvedUriString
                    AcademicDatabaseManager.persistNote(note, immediate = true)
                }
                val effectiveUri = Uri.parse(resolvedUriString ?: note.pdfUri)
                val result = PdfNoteHelper.renderPdfPageWithBounds(
                    context = context,
                    uri = effectiveUri,
                    pageIndex = pageToLoad,
                    targetWidth = 1400
                )
                withContext(Dispatchers.Main) {
                    if (currentPageIndex == pageToLoad) {
                        if (result != null) {
                            pdfPageWorldWidth = result.worldWidth
                            pdfPageWorldHeight = result.worldHeight
                            pdfPageBitmap = result.bitmap.asImageBitmap()
                            pdfLoadFailed = false
                        } else {
                            pdfPageBitmap = null
                            pdfLoadFailed = true
                        }
                    }
                }
                // Warm next page cleanly in background for instant Next flip
                if (result != null && pageToLoad + 1 < totalPages) {
                    PdfNoteHelper.preloadPage(context, effectiveUri, pageToLoad + 1, 1400)
                }
            }
        }
    }

    fun goToPage(targetIndex: Int) {
        if (targetIndex != currentPageIndex && targetIndex in 0 until totalPages) {
            saveActivePageData(immediate = true)
            currentPageIndex = targetIndex
            loadActivePageData(targetIndex)
            loadPdfPage()
        }
    }

    fun addNewPage() {
        saveActivePageData(immediate = true)
        val newPage = NotePageData(pageIndex = note.pages.size)
        note.pages.add(newPage)
        totalPages = note.pages.size
        currentPageIndex = note.pages.size - 1
        loadActivePageData(currentPageIndex)
    }

    fun deleteCurrentPage() {
        if (note.pages.size > 1) {
            note.pages.removeAt(currentPageIndex)
            note.pages.forEachIndexed { idx, page -> page.pageIndex = idx }
            val newIdx = if (currentPageIndex >= note.pages.size) note.pages.size - 1 else currentPageIndex
            currentPageIndex = newIdx
            totalPages = note.pages.size
            loadActivePageData(newIdx)
            loadPdfPage()
            saveActivePageData(immediate = true)
        }
    }

    fun duplicatePage(pageIndex: Int) {
        if (pageIndex in 0 until note.pages.size) {
            saveActivePageData(immediate = true)
            val src = note.pages[pageIndex]
            val newPage = NotePageData(
                pageIndex = note.pages.size,
                strokes = ArrayList(src.strokes),
                textBoxes = ArrayList(src.textBoxes.map { it.copy(id = UUID.randomUUID().toString(), isSelected = false) }),
                imageElements = ArrayList(src.imageElements.map { it.copy(id = UUID.randomUUID().toString(), isSelected = false) })
            )
            note.pages.add(newPage)
            totalPages = note.pages.size
            currentPageIndex = note.pages.size - 1
            loadActivePageData(currentPageIndex)
            saveActivePageData(immediate = true)
        }
    }

    fun deselectAll() {
        lassoSelection = null
        for (i in textBoxes.indices) {
            textBoxes[i] = textBoxes[i].copy(isSelected = false)
        }
        for (i in imageElements.indices) {
            imageElements[i] = imageElements[i].copy(isSelected = false)
        }
    }

    fun undo() {
        pendingTransactionSnapshot = null
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeLast()
            redoStack.add(captureSnapshot())
            applySnapshot(previous)
            saveActivePageData()
        }
    }

    fun redo() {
        pendingTransactionSnapshot = null
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            if (undoStack.size >= 60) {
                undoStack.removeAt(0)
            }
            undoStack.add(captureSnapshot())
            applySnapshot(next)
            saveActivePageData()
        }
    }

    fun clearAll() {
        if (strokes.isEmpty() && textBoxes.isEmpty() && imageElements.isEmpty()) return
        pushUndoSnapshot()
        strokes.clear()
        textBoxes.clear()
        imageElements.clear()
        lassoSelection = null
        saveActivePageData(immediate = true)
    }

    private fun distToSegmentSquared(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val l2 = dx * dx + dy * dy
        if (l2 == 0f) {
            val ex = px - x1
            val ey = py - y1
            return ex * ex + ey * ey
        }
        val t = (((px - x1) * dx + (py - y1) * dy) / l2).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        val ex = px - projX
        val ey = py - projY
        return ex * ex + ey * ey
    }

    // Cached per-stroke AABB [minX, minY, maxX, maxY] to make eraser hit-testing O(1) per non-intersecting stroke
    private val strokeBoundsCache = WeakHashMap<Stroke, FloatArray>()
    private val reusableInputA = StrokeInput()
    private val reusableInputB = StrokeInput()

    private fun getStrokeBounds(stroke: Stroke): FloatArray? {
        strokeBoundsCache[stroke]?.let { return it }
        val inputs = stroke.inputs
        val count = inputs.size
        if (count == 0) return null
        inputs.populate(0, reusableInputA)
        var minX = reusableInputA.x
        var maxX = reusableInputA.x
        var minY = reusableInputA.y
        var maxY = reusableInputA.y
        for (i in 1 until count) {
            inputs.populate(i, reusableInputA)
            val x = reusableInputA.x
            val y = reusableInputA.y
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        return floatArrayOf(minX, minY, maxX, maxY).also { strokeBoundsCache[stroke] = it }
    }

    private fun isPointOutsideBounds(x: Float, y: Float, radius: Float, bounds: FloatArray): Boolean {
        return x + radius < bounds[0] ||
            x - radius > bounds[2] ||
            y + radius < bounds[1] ||
            y - radius > bounds[3]
    }

    fun eraseNear(
        touchX: Float,
        touchY: Float,
        radiusPx: Float = AppSettingsManager.eraserRadius,
        mode: EraserMode = AppSettingsManager.eraserMode
    ) {
        val effectiveRadius = radiusPx / scale
        val radiusSquared = effectiveRadius * effectiveRadius

        if (mode == EraserMode.STROKE) {
            for (idx in strokes.indices.reversed()) {
                val stroke = strokes[idx]
                val bounds = getStrokeBounds(stroke) ?: continue
                if (isPointOutsideBounds(touchX, touchY, effectiveRadius, bounds)) continue

                val inputs = stroke.inputs
                val count = inputs.size
                var hit = false
                if (count == 1) {
                    inputs.populate(0, reusableInputA)
                    val dx = reusableInputA.x - touchX
                    val dy = reusableInputA.y - touchY
                    hit = (dx * dx + dy * dy) <= radiusSquared
                } else {
                    inputs.populate(0, reusableInputA)
                    var prevX = reusableInputA.x
                    var prevY = reusableInputA.y
                    for (i in 1 until count) {
                        inputs.populate(i, reusableInputB)
                        val curX = reusableInputB.x
                        val curY = reusableInputB.y
                        if (distToSegmentSquared(touchX, touchY, prevX, prevY, curX, curY) <= radiusSquared) {
                            hit = true
                            break
                        }
                        prevX = curX
                        prevY = curY
                    }
                }
                if (hit) {
                    strokes.removeAt(idx)
                }
            }
        } else {
            // ── Pixel / Segment Erase: cut out points inside radius in-place (zero whole-list clear!) ──
            for (idx in strokes.indices.reversed()) {
                val stroke = strokes[idx]
                val bounds = getStrokeBounds(stroke) ?: continue
                if (isPointOutsideBounds(touchX, touchY, effectiveRadius, bounds)) continue

                val inputs = stroke.inputs
                val count = inputs.size
                if (count == 1) {
                    inputs.populate(0, reusableInputA)
                    val dx = reusableInputA.x - touchX
                    val dy = reusableInputA.y - touchY
                    if ((dx * dx + dy * dy) <= radiusSquared) {
                        strokes.removeAt(idx)
                    }
                    continue
                }

                var hit = false
                inputs.populate(0, reusableInputA)
                var prevX = reusableInputA.x
                var prevY = reusableInputA.y
                if ((prevX - touchX) * (prevX - touchX) + (prevY - touchY) * (prevY - touchY) <= radiusSquared) {
                    hit = true
                } else {
                    for (i in 1 until count) {
                        inputs.populate(i, reusableInputB)
                        val curX = reusableInputB.x
                        val curY = reusableInputB.y
                        if (distToSegmentSquared(touchX, touchY, prevX, prevY, curX, curY) <= radiusSquared) {
                            hit = true
                            break
                        }
                        prevX = curX
                        prevY = curY
                    }
                }

                if (!hit) continue

                val splitParts = ArrayList<Stroke>(2)
                var currentBatch = MutableStrokeInputBatch()

                for (i in 0 until count) {
                    inputs.populate(i, reusableInputA)
                    val dx = reusableInputA.x - touchX
                    val dy = reusableInputA.y - touchY
                    val inside = (dx * dx + dy * dy) <= radiusSquared

                    if (!inside) {
                        currentBatch.add(reusableInputA)
                    } else {
                        if (currentBatch.size >= 2) {
                            splitParts.add(Stroke(stroke.brush, currentBatch))
                            currentBatch = MutableStrokeInputBatch()
                        } else {
                            currentBatch.clear()
                        }
                    }
                }
                if (currentBatch.size >= 2) {
                    splitParts.add(Stroke(stroke.brush, currentBatch))
                }

                when (splitParts.size) {
                    0 -> strokes.removeAt(idx)
                    1 -> strokes[idx] = splitParts[0]
                    else -> {
                        strokes.removeAt(idx)
                        strokes.addAll(idx, splitParts)
                    }
                }
            }
        }
    }

    /**
     * Erases strokes and text boxes that are scratched out by a scribble gesture.
     * Returns true if any content was erased and the gesture should consume the scribble stroke.
     */
    fun eraseWithScribble(scribbleStroke: Stroke): Boolean {
        val sBounds = getStrokeBounds(scribbleStroke) ?: return false
        val pad = maxOf(10f, 16f / scale)
        val sLeft = sBounds[0] - pad
        val sTop = sBounds[1] - pad
        val sRight = sBounds[2] + pad
        val sBottom = sBounds[3] + pad
        val scribbleW = sRight - sLeft
        val scribbleH = sBottom - sTop

        val strokesToRemove = mutableListOf<Stroke>()
        for (i in strokes.indices.reversed()) {
            val stroke = strokes[i]
            if (stroke === scribbleStroke) continue
            val b = getStrokeBounds(stroke) ?: continue

            // AABB fast reject
            if (b[2] < sLeft || b[0] > sRight || b[3] < sTop || b[1] > sBottom) {
                continue
            }

            val strokeW = b[2] - b[0]
            val strokeH = b[3] - b[1]
            val centerX = (b[0] + b[2]) / 2f
            val centerY = (b[1] + b[3]) / 2f

            val isContained = b[0] >= sLeft && b[2] <= sRight && b[1] >= sTop && b[3] <= sBottom
            val isCenterInside = centerX in sLeft..sRight && centerY in sTop..sBottom

            val inputs = stroke.inputs
            val count = inputs.size
            var insideCount = 0
            val step = maxOf(1, count / 16)
            var sampled = 0

            for (k in 0 until count step step) {
                inputs.populate(k, reusableInputA)
                sampled++
                if (reusableInputA.x in sLeft..sRight && reusableInputA.y in sTop..sBottom) {
                    insideCount++
                }
            }

            val insideRatio = if (sampled > 0) insideCount.toFloat() / sampled else 0f

            if (isContained || insideRatio >= 0.30f || (isCenterInside && strokeW <= scribbleW * 1.6f && strokeH <= scribbleH * 1.6f)) {
                strokesToRemove.add(stroke)
            }
        }

        val textBoxesToRemove = mutableListOf<TextBoxElement>()
        for (tb in textBoxes) {
            val tbLeft = tb.x
            val tbTop = tb.y
            val tbRight = tb.x + tb.width
            val tbBottom = tb.y + tb.height

            if (tbRight < sLeft || tbLeft > sRight || tbBottom < sTop || tbTop > sBottom) {
                continue
            }

            val tbCenterX = (tbLeft + tbRight) / 2f
            val tbCenterY = (tbTop + tbBottom) / 2f

            val interLeft = maxOf(sLeft, tbLeft)
            val interTop = maxOf(sTop, tbTop)
            val interRight = minOf(sRight, tbRight)
            val interBottom = minOf(sBottom, tbBottom)
            val interArea = maxOf(0f, interRight - interLeft) * maxOf(0f, interBottom - interTop)
            val tbArea = maxOf(1f, tb.width * tb.height)
            val scribbleArea = maxOf(1f, scribbleW * scribbleH)

            val overlapRatioTb = interArea / tbArea
            val overlapRatioScribble = interArea / scribbleArea
            val isScribbleInsideTb = sLeft >= tbLeft - 20f && sRight <= tbRight + 20f && sTop >= tbTop - 20f && sBottom <= tbBottom + 20f

            if (overlapRatioTb >= 0.25f || overlapRatioScribble >= 0.35f || isScribbleInsideTb || (tbCenterX in sLeft..sRight && tbCenterY in sTop..sBottom)) {
                textBoxesToRemove.add(tb)
            }
        }

        if (strokesToRemove.isEmpty() && textBoxesToRemove.isEmpty()) {
            return false
        }

        pushUndoSnapshot()
        if (strokesToRemove.isNotEmpty()) {
            strokes.removeAll(strokesToRemove)
        }
        if (textBoxesToRemove.isNotEmpty()) {
            textBoxes.removeAll(textBoxesToRemove)
        }
        saveActivePageData()
        return true
    }
}

@Composable
fun rememberNotePaneState(
    note: NoteUiModel,
    context: android.content.Context = LocalContext.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): NotePaneState {
    val state = remember(note.id) {
        NotePaneState(note, context, coroutineScope)
    }
    LaunchedEffect(note.id) {
        state.loadFromDiskAsync()
        state.loadPdfPage()
    }
    DisposableEffect(note.id) {
        onDispose {
            state.saveActivePageData(immediate = true)
        }
    }
    return state
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN: NoteEditorScreen with Unified Toolbar & Optional Split-Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NoteEditorScreen(
    note: NoteUiModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Primary state (never reallocated, survives split and swap!)
    val primaryState = rememberNotePaneState(note)

    // Secondary note and state
    var secondaryNote by remember { mutableStateOf<NoteUiModel?>(null) }
    val secondaryState = secondaryNote?.let { rememberNotePaneState(it) }

    // Swap state: if true, left pane is secondary, right pane is primary
    var isSwapped by remember { mutableStateOf(false) }

    // Active Note Tracking by Note ID
    var activeNoteId by remember(note.id) { mutableStateOf(note.id) }

    // Current active state connected to the top toolbar
    val activeState = if (secondaryState != null && activeNoteId == secondaryState.note.id) secondaryState else primaryState

    // Split ratio: 0.20f to 0.80f (Default 50:50)
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var showSplitNoteDialog by remember { mutableStateOf(false) }

    // Resolved Left and Right Pane States
    val leftState = if (isSwapped && secondaryState != null) secondaryState else primaryState
    val rightState = if (isSwapped) primaryState else secondaryState

    // Tool State: Independent presets for Pen and Highlighter
    var activeTool by remember { mutableStateOf(NoteTool.PEN) }
    var penColor by remember { mutableStateOf(Color(0xFF0F172A)) } // Dark slate / Black default for clean writing
    var penThickness by remember { mutableFloatStateOf(4f) }       // 4 px default for Pen
    var highlighterColor by remember { mutableStateOf(Color(0xFFFDE047)) } // Soft Pastel Yellow default
    var highlighterThickness by remember { mutableFloatStateOf(24f) }      // 24 px default for Highlighter
    var selectedShapeType by remember { mutableStateOf(GeometricShapeType.RECTANGLE) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedPdfFile by remember { mutableStateOf<File?>(null) }

    // Audio Lecture Controller
    val audioController = remember(activeState.note.id) {
        NoteAudioController(context, activeState.note.id)
    }
    DisposableEffect(audioController) {
        onDispose {
            audioController.release()
        }
    }
    var showAudioBar by remember(activeState.note.id) {
        mutableStateOf(audioController.hasExistingRecording)
    }

    val activeColor = when (activeTool) {
        NoteTool.HIGHLIGHTER -> highlighterColor
        else -> penColor
    }
    val activeThickness = when (activeTool) {
        NoteTool.HIGHLIGHTER -> highlighterThickness
        else -> penThickness
    }

    // Brush calculation reacting immediately to independent tool presets
    val currentBrush = remember(activeTool, penColor, penThickness, highlighterColor, highlighterThickness) {
        when (activeTool) {
            NoteTool.HIGHLIGHTER -> {
                Brush.createWithComposeColor(
                    family = StockBrushes.highlighter(),
                    color = highlighterColor.copy(alpha = 0.40f),
                    size = highlighterThickness,
                    epsilon = 0.1f
                )
            }
            else -> {
                Brush.createWithComposeColor(
                    family = StockBrushes.pressurePen(),
                    color = penColor,
                    size = penThickness,
                    epsilon = 0.1f
                )
            }
        }
    }

    // Auto-save on exit or disposal
    fun handleBack() {
        primaryState.saveActivePageData(immediate = true)
        secondaryState?.saveActivePageData(immediate = true)
        onBack()
    }

    fun handleCloseSplit() {
        secondaryState?.saveActivePageData(immediate = true)
        secondaryNote?.let { AcademicDatabaseManager.persistNote(it, immediate = true) }
        secondaryNote = null
        isSwapped = false
        activeNoteId = primaryState.note.id
    }

    // Hardware / gesture back button handling
    var showEditNoteLinkDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (secondaryNote != null) {
            handleCloseSplit()
        } else {
            handleBack()
        }
    }

    // Image Picker Launcher with safe internal copy & auto-persistence
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val imageId = UUID.randomUUID().toString()
                    val bitmap = NoteStorageManager.copyImageToInternal(context, pickedUri, imageId)
                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            activeState.deselectAll()
                            // Center spawn point in the user's visible viewport
                            val spawnX = (-activeState.panX + 160f) / activeState.scale
                            val spawnY = (-activeState.panY + 140f) / activeState.scale
                            val aspect = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.2f, 5f)
                            // Clean natural initial size in world canvas pixels (450px wide)
                            val baseWidth = 450f
                            val baseHeight = baseWidth / aspect
                            activeState.pushUndoSnapshot()
                            activeState.imageElements.add(
                                ImageElement(
                                    id = imageId,
                                    x = maxOf(40f, spawnX),
                                    y = maxOf(60f, spawnY),
                                    width = baseWidth,
                                    height = baseHeight,
                                    bitmap = bitmap.asImageBitmap(),
                                    isSelected = true
                                )
                            )
                            activeState.saveActivePageData()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun handleExportPdf() {
        activeState.saveActivePageData()
        isExporting = true
        coroutineScope.launch(Dispatchers.IO) {
            val file = PdfNoteHelper.exportNoteToPdf(context, activeState.note, activeState.canvasStrokeRenderer)
            withContext(Dispatchers.Main) {
                isExporting = false
                if (file != null) {
                    exportedPdfFile = file
                } else {
                    android.widget.Toast.makeText(context, "Gagal membuat dokumen PDF", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        val currentExportedFile = exportedPdfFile
        if (currentExportedFile != null) {
            ExportPdfSuccessDialog(
                file = currentExportedFile,
                totalPages = if (activeState.canvasType == NoteCanvasType.PDF_DOCUMENT) activeState.note.pdfTotalPages else activeState.note.pages.size,
                onDismiss = { exportedPdfFile = null },
                onShare = {
                    PdfNoteHelper.sharePdf(context, currentExportedFile)
                },
                onSaveToDownloads = {
                    val success = PdfNoteHelper.savePdfToDownloads(context, currentExportedFile)
                    android.widget.Toast.makeText(
                        context,
                        if (success) "Disimpan ke folder Downloads: ${currentExportedFile.name}" else "Gagal menyimpan ke folder Downloads",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        // ── 1. CANVASES: Full Screen or Dual Split View ──
        if (secondaryNote == null || rightState == null) {
            // SINGLE NOTE MODE (Full Width)
            key(primaryState.note.id) {
                NotePaneViewport(
                    state = primaryState,
                    isActive = true,
                    activeTool = activeTool,
                    currentBrush = currentBrush,
                    selectedColor = activeColor,
                    selectedThickness = activeThickness,
                    selectedShapeType = selectedShapeType,
                    onActivate = { activeNoteId = primaryState.note.id },
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                )
            }
        } else {
            // DUAL-NOTE SPLIT SCREEN MODE (Clean canvas, double-tap divider to swap!)
            BoxWithConstraints(modifier = Modifier.fillMaxSize().clipToBounds()) {
                val totalWidthPx = constraints.maxWidth.toFloat()

                Row(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    // LEFT PANE (Strictly clipped & keyed by note ID)
                    Box(
                        modifier = Modifier
                            .weight(splitRatio)
                            .fillMaxHeight()
                            .clipToBounds()
                    ) {
                        key(leftState.note.id) {
                            NotePaneViewport(
                                state = leftState,
                                isActive = activeNoteId == leftState.note.id,
                                activeTool = activeTool,
                                currentBrush = currentBrush,
                                selectedColor = activeColor,
                                selectedThickness = activeThickness,
                                selectedShapeType = selectedShapeType,
                                onActivate = { activeNoteId = leftState.note.id },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            )
                        }
                    }

                    // MIDDLE DRAGGABLE DIVIDER (Double-tap to Swap notes!)
                    SplitScreenDivider(
                        onDrag = { dragDeltaPx ->
                            val ratioDelta = dragDeltaPx / totalWidthPx
                            splitRatio = (splitRatio + ratioDelta).coerceIn(0.20f, 0.80f)
                        },
                        onDoubleTap = {
                            isSwapped = !isSwapped
                        },
                        modifier = Modifier.zIndex(2f)
                    )

                    // RIGHT PANE (Strictly clipped & keyed by note ID)
                    Box(
                        modifier = Modifier
                            .weight(1f - splitRatio)
                            .fillMaxHeight()
                            .clipToBounds()
                    ) {
                        key(rightState.note.id) {
                            NotePaneViewport(
                                state = rightState,
                                isActive = activeNoteId == rightState.note.id,
                                activeTool = activeTool,
                                currentBrush = currentBrush,
                                selectedColor = activeColor,
                                selectedThickness = activeThickness,
                                selectedShapeType = selectedShapeType,
                                onActivate = { activeNoteId = rightState.note.id },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            )
                        }
                    }
                }
            }
        }

        // ── 2. SINGLE UNIFIED TOP APP BAR (Serves both notes seamlessly) ──
        UnifiedNoteTopBar(
            isSplitMode = secondaryNote != null && rightState != null,
            activeNoteId = activeNoteId,
            leftNoteId = leftState.note.id,
            leftTitle = leftState.noteTitle,
            onLeftTitleChange = {
                leftState.noteTitle = it
                leftState.note.title = it
            },
            leftSubject = leftState.subject,
            leftCanvasType = leftState.canvasType,
            leftPaperStyle = leftState.paperStyle,
            leftCurrentPage = leftState.currentPageIndex + 1,
            leftTotalPages = leftState.totalPages,
            onLeftChangePaperStyle = { leftState.updatePaperStyle(it) },
            onLeftEditLink = { showEditNoteLinkDialog = true },
            rightNoteId = rightState?.note?.id,
            rightTitle = rightState?.noteTitle,
            rightSubject = rightState?.subject,
            rightCurrentPage = (rightState?.currentPageIndex ?: 0) + 1,
            rightTotalPages = rightState?.totalPages ?: 1,
            onSelectActiveNote = { activeNoteId = it },
            onToggleSplitMode = { showSplitNoteDialog = true },
            onCloseSplit = { handleCloseSplit() },
            activeTool = activeTool,
            onSelectTool = { tool ->
                activeTool = tool
                if (tool != NoteTool.TEXT) {
                    activeState.deselectAll()
                } else {
                    activeState.deselectAll()
                    activeState.pushUndoSnapshot()
                    val screenCenterX = (-activeState.panX + 260f) / activeState.scale
                    val screenCenterY = (-activeState.panY + 220f) / activeState.scale
                    activeState.textBoxes.add(
                        TextBoxElement(
                            x = maxOf(40f, screenCenterX),
                            y = maxOf(80f, screenCenterY),
                            width = 420f,
                            height = 140f,
                            text = "",
                            isSelected = true
                        )
                    )
                }
            },
            selectedShapeType = selectedShapeType,
            onSelectShapeType = { selectedShapeType = it },
            penColor = penColor,
            onSelectPenColor = { penColor = it },
            penThickness = penThickness,
            onSelectPenThickness = { penThickness = it },
            highlighterColor = highlighterColor,
            onSelectHighlighterColor = { highlighterColor = it },
            highlighterThickness = highlighterThickness,
            onSelectHighlighterThickness = { highlighterThickness = it },
            onPickImage = {
                imagePickerLauncher.launch("image/*")
            },
            canUndo = activeState.undoStack.isNotEmpty(),
            onUndo = { activeState.undo() },
            canRedo = activeState.redoStack.isNotEmpty(),
            onRedo = { activeState.redo() },
            onClearAll = { activeState.clearAll() },
            onExportPdf = { handleExportPdf() },
            isExporting = isExporting,
            isDarkPaper = activeState.isDarkPaper,
            onToggleDarkPaper = { activeState.toggleDarkPaper() },
            hasAudioRecording = audioController.hasExistingRecording,
            isAudioBarOpen = showAudioBar,
            onToggleAudioBar = {
                showAudioBar = !showAudioBar
            },
            onBack = { handleBack() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
        )

        // Floating Audio Lecture Bar (Positioned gracefully below topbar)
        AnimatedVisibility(
            visible = showAudioBar,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 74.dp)
                .zIndex(12f)
        ) {
            AudioLectureBar(
                audioController = audioController,
                onClose = { showAudioBar = false }
            )
        }

        if (showEditNoteLinkDialog) {
            com.lernspace.app.ui.workspace.components.EditNoteLinkDialog(
                note = leftState.note,
                subjects = AcademicWorkspaceState.subjects,
                topics = AcademicWorkspaceState.topics,
                onDismiss = { showEditNoteLinkDialog = false },
                onConfirm = { newTitle, selectedSubject, selectedTopic ->
                    showEditNoteLinkDialog = false
                    leftState.saveActivePageData(immediate = true)
                    val updated = AcademicWorkspaceState.updateNoteLinkAndTitle(
                        noteId = leftState.note.id,
                        newTitle = newTitle,
                        subject = selectedSubject,
                        topic = selectedTopic
                    )
                    if (updated != null) {
                        leftState.applyUpdatedMeta(updated)
                    }
                }
            )
        }

        // ── 3. MODAL DIALOG: Choose or Create Secondary Note for Split Screen ──
        if (showSplitNoteDialog) {
            SplitNotePickerDialog(
                currentNoteId = primaryState.note.id,
                availableNotes = AcademicWorkspaceState.notes,
                onSelectNote = { selectedNote ->
                    primaryState.saveActivePageData()
                    val pages = AcademicDatabaseManager.loadPagesForNote(selectedNote.id)
                    if (pages != null && pages.isNotEmpty()) {
                        selectedNote.pages.clear()
                        selectedNote.pages.addAll(pages)
                    }
                    secondaryNote = selectedNote
                    activeNoteId = selectedNote.id
                    isSwapped = false
                    showSplitNoteDialog = false
                },
                onCreateScratchpad = {
                    primaryState.saveActivePageData()
                    val newScratch = AcademicWorkspaceState.createScratchpad("Scratchpad Pembanding")
                    secondaryNote = newScratch
                    activeNoteId = newScratch.id
                    isSwapped = false
                    showSplitNoteDialog = false
                },
                onDismiss = { showSplitNoteDialog = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: NotePaneViewport (Clean Full-Height Canvas Viewport)
// ─────────────────────────────────────────────────────────────────────────────
private enum class LassoAiMode(val label: String) {
    AI_TUTOR("AI Tutor"),
    FORMULA("Ubah ke Rumus"),
    CHECK("Cek Jawaban"),
    PRACTICE("Soal Serupa")
}

@Composable
private fun NotePaneViewport(
    state: NotePaneState,
    isActive: Boolean,
    activeTool: NoteTool,
    currentBrush: Brush,
    selectedColor: Color,
    selectedThickness: Float,
    selectedShapeType: GeometricShapeType,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var showDeletePageDialog by remember { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var showLassoAiDialog by remember { mutableStateOf(false) }
    var isAskingLassoAi by remember { mutableStateOf(false) }
    var lassoAiJob by remember { mutableStateOf<Job?>(null) }
    var lassoAiResponse by remember { mutableStateOf<String?>(null) }
    var lassoAiError by remember { mutableStateOf<String?>(null) }
    var lassoAiCustomPrompt by remember { mutableStateOf("") }
    var lassoAiMode by remember { mutableStateOf(LassoAiMode.AI_TUTOR) }

    val triggerLassoAi: (LassoSelectionState, String, LassoAiMode) -> Unit = { sel, customQuestion, mode ->
        lassoAiMode = mode
        showLassoAiDialog = true
        lassoAiResponse = null
        lassoAiError = null
        if (!GeminiApiClient.isConfigured()) {
            isAskingLassoAi = false
            lassoAiError = "Gemini API Key belum diatur. Buka Pengaturan -> AI & Integrasi untuk memasukkan API Key gratis kamu."
        } else {
            isAskingLassoAi = true
            val strokesSnapshot = sel.selectedStrokes.toList()
            val textSnippets = sel.selectedTextBoxes.mapNotNull { it.text.trim().takeIf { t -> t.isNotEmpty() } }
            val imageBitmapsSrc = sel.selectedImages.map { it.bitmap }
            val darkPaperBg = if (state.isDarkPaper) android.graphics.Color.rgb(30, 30, 38) else android.graphics.Color.WHITE
            val rendererSnapshot = state.canvasStrokeRenderer

            lassoAiJob?.cancel()
            lassoAiJob = coroutineScope.launch {
                val sendBitmaps = mutableListOf<android.graphics.Bitmap>()
                val bitmapsToRecycle = mutableListOf<android.graphics.Bitmap>()
                try {
                    withContext(Dispatchers.Default) {
                        // 1. Ekstraksi area PDF jika sedang di mode PDF atau isRegionCapture
                        if (state.canvasType == NoteCanvasType.PDF_DOCUMENT || sel.isRegionCapture) {
                            val pdfImageBitmap = state.pdfPageBitmap
                            if (pdfImageBitmap != null) {
                                runCatching {
                                    val pdfBmp = pdfImageBitmap.asAndroidBitmap()
                                    if (!pdfBmp.isRecycled) {
                                        val worldW = state.pdfPageWorldWidth.coerceAtLeast(1f)
                                        val worldH = state.pdfPageWorldHeight.coerceAtLeast(1f)
                                        val scaleX = pdfBmp.width.toFloat() / worldW
                                        val scaleY = pdfBmp.height.toFloat() / worldH

                                        val leftPx = (sel.bounds.left * scaleX).roundToInt().coerceIn(0, pdfBmp.width - 1)
                                        val topPx = (sel.bounds.top * scaleY).roundToInt().coerceIn(0, pdfBmp.height - 1)
                                        val rightPx = (sel.bounds.right * scaleX).roundToInt().coerceIn(leftPx + 1, pdfBmp.width)
                                        val bottomPx = (sel.bounds.bottom * scaleY).roundToInt().coerceIn(topPx + 1, pdfBmp.height)

                                        val cropW = rightPx - leftPx
                                        val cropH = bottomPx - topPx

                                        if (cropW > 4 && cropH > 4) {
                                            val croppedBmp = android.graphics.Bitmap.createBitmap(pdfBmp, leftPx, topPx, cropW, cropH)
                                            val strokesToDraw = if (strokesSnapshot.isNotEmpty()) strokesSnapshot else {
                                                state.strokes.filter { LassoHelper.isStrokeInBounds(it, sel.bounds) }
                                            }
                                            if (strokesToDraw.isNotEmpty() && rendererSnapshot != null) {
                                                val compositeBmp = croppedBmp.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                                                val cCanvas = android.graphics.Canvas(compositeBmp)
                                                val matrix = android.graphics.Matrix().apply {
                                                    postTranslate(-sel.bounds.left, -sel.bounds.top)
                                                    postScale(scaleX, scaleY)
                                                }
                                                for (st in strokesToDraw) {
                                                    rendererSnapshot.draw(cCanvas, st, matrix)
                                                }
                                                val maxSide = maxOf(compositeBmp.width, compositeBmp.height)
                                                if (maxSide > 1024) {
                                                    val downscale = 1024f / maxSide
                                                    val scaled = android.graphics.Bitmap.createScaledBitmap(
                                                        compositeBmp,
                                                        (compositeBmp.width * downscale).roundToInt().coerceAtLeast(1),
                                                        (compositeBmp.height * downscale).roundToInt().coerceAtLeast(1),
                                                        true
                                                    )
                                                    sendBitmaps.add(scaled)
                                                    bitmapsToRecycle.add(scaled)
                                                    compositeBmp.recycle()
                                                } else {
                                                    sendBitmaps.add(compositeBmp)
                                                    bitmapsToRecycle.add(compositeBmp)
                                                }
                                                if (croppedBmp !== compositeBmp) croppedBmp.recycle()
                                            } else {
                                                val maxSide = maxOf(croppedBmp.width, croppedBmp.height)
                                                if (maxSide > 1024) {
                                                    val downscale = 1024f / maxSide
                                                    val scaled = android.graphics.Bitmap.createScaledBitmap(
                                                        croppedBmp,
                                                        (croppedBmp.width * downscale).roundToInt().coerceAtLeast(1),
                                                        (croppedBmp.height * downscale).roundToInt().coerceAtLeast(1),
                                                        true
                                                    )
                                                    sendBitmaps.add(scaled)
                                                    bitmapsToRecycle.add(scaled)
                                                    croppedBmp.recycle()
                                                } else {
                                                    sendBitmaps.add(croppedBmp)
                                                    bitmapsToRecycle.add(croppedBmp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Jika bukan dari PDF atau belum ada bitmap, render strokes biasa jika ada
                        if (strokesSnapshot.isNotEmpty() && sendBitmaps.isEmpty()) {
                            HandwrittenNoteRecognizer.renderStrokesToBitmap(
                                strokes = strokesSnapshot,
                                renderer = rendererSnapshot,
                                maxDim = 720,
                                backgroundColor = darkPaperBg
                            )?.let {
                                sendBitmaps.add(it)
                                bitmapsToRecycle.add(it)
                            }
                        }

                        // 3. Ekstraksi gambar ImageElement yang terseleksi
                        for (img in sel.selectedImages) {
                            runCatching {
                                val src = img.bitmap.asAndroidBitmap()
                                val intersectLeft = maxOf(sel.bounds.left, img.x)
                                val intersectTop = maxOf(sel.bounds.top, img.y)
                                val intersectRight = minOf(sel.bounds.right, img.x + img.width)
                                val intersectBottom = minOf(sel.bounds.bottom, img.y + img.height)

                                if (intersectRight > intersectLeft && intersectBottom > intersectTop && sel.isRegionCapture) {
                                    val scaleX = src.width.toFloat() / img.width.coerceAtLeast(1f)
                                    val scaleY = src.height.toFloat() / img.height.coerceAtLeast(1f)
                                    val cropX = ((intersectLeft - img.x) * scaleX).roundToInt().coerceIn(0, src.width - 1)
                                    val cropY = ((intersectTop - img.y) * scaleY).roundToInt().coerceIn(0, src.height - 1)
                                    val cropW = ((intersectRight - intersectLeft) * scaleX).roundToInt().coerceIn(1, src.width - cropX)
                                    val cropH = ((intersectBottom - intersectTop) * scaleY).roundToInt().coerceIn(1, src.height - cropY)

                                    val croppedImg = android.graphics.Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
                                    val maxSide = maxOf(croppedImg.width, croppedImg.height)
                                    if (maxSide > 720) {
                                        val scale = 720f / maxSide
                                        val scaled = android.graphics.Bitmap.createScaledBitmap(
                                            croppedImg,
                                            (croppedImg.width * scale).roundToInt().coerceAtLeast(1),
                                            (croppedImg.height * scale).roundToInt().coerceAtLeast(1),
                                            true
                                        )
                                        sendBitmaps.add(scaled)
                                        bitmapsToRecycle.add(scaled)
                                        croppedImg.recycle()
                                    } else {
                                        sendBitmaps.add(croppedImg)
                                        bitmapsToRecycle.add(croppedImg)
                                    }
                                } else {
                                    val maxSide = maxOf(src.width, src.height).coerceAtLeast(1)
                                    if (maxSide > 720) {
                                        val scale = 720f / maxSide
                                        val w = (src.width * scale).roundToInt().coerceAtLeast(1)
                                        val h = (src.height * scale).roundToInt().coerceAtLeast(1)
                                        val scaled = android.graphics.Bitmap.createScaledBitmap(src, w, h, true)
                                        sendBitmaps.add(scaled)
                                        if (scaled !== src) bitmapsToRecycle.add(scaled)
                                    } else {
                                        sendBitmaps.add(src)
                                    }
                                }
                            }
                        }
                    }

                    if (sendBitmaps.isEmpty() && textSnippets.isEmpty()) {
                        isAskingLassoAi = false
                        lassoAiError = "Tidak ada konten tulisan, gambar, atau halaman yang terdeteksi di dalam area lasso."
                        return@launch
                    }

                    val sysPrompt = when (mode) {
                        LassoAiMode.AI_TUTOR -> """
                            Kamu adalah AI Tutor akademik yang sabar, cerdas, dan interaktif dalam Bahasa Indonesia.
                            - Jelaskan dan selesaikan soal atau konsep yang dilingkari secara terstruktur dan mendalam layaknya seorang tutor profesional.
                            - Untuk soal hitungan/sains: berikan penjelasan intuitif di setiap langkah pengerjaan, jelaskan konsep dasarnya sebelum rumus perhitungan, lalu akhiri dengan **Hasil Akhir:** yang jelas.
                            - Untuk pertanyaan konsep/teori: jelaskan secara bertahap dengan analogi atau contoh nyata jika relevan.
                            - Gunakan format LaTeX standar (${'$'}...${'$'} untuk sebaris, ${'$'}${'$'}...${'$'}${'$'} untuk persamaan utama) dan Markdown bersih tanpa basa-basi berlebihan.
                        """.trimIndent()
                        LassoAiMode.FORMULA -> """
                            Kamu adalah mesin konversi tulisan tangan ke rumus matematika dan sains (Math & Formula OCR) tingkat tinggi.
                            - Konversikan seluruh tulisan tangan, persamaan, pecahan, integral, matriks, atau notasi matematika/sains yang dilingkari menjadi rumus LaTeX murni yang presisi dan rapi.
                            - HANYA keluarkan rumus dalam blok LaTeX (${'$'}${'$'}...${'$'}${'$'} atau ${'$'}...${'$'}) tanpa sapaan, tanpa kata pengantar/penutup, tanpa langkah penyelesaian, dan tanpa komentar apapun.
                            - Jika terdiri dari beberapa persamaan atau baris, susun menggunakan format align atau baris baru yang rapi.
                        """.trimIndent()
                        LassoAiMode.CHECK -> """
                            Kamu adalah pemeriksa jawaban yang teliti dalam Bahasa Indonesia. Jawab langsung tanpa sapaan atau basa-basi.
                            - Periksa soal dan langkah pengerjaan yang ditulis pengguna pada gambar/catatan.
                            - Nyatakan langsung apakah **Benar** atau **Perlu Diperbaiki**.
                            - Jika ada kesalahan, tunjukkan tepat di langkah mana kelirunya, jelaskan singkat alasannya, lalu tulis perbaikan langkah & **Hasil:** yang benar.
                            - Gunakan LaTeX standar (${'$'}...${'$'} dan ${'$'}${'$'}...${'$'}${'$'}) dan Markdown bersih.
                        """.trimIndent()
                        LassoAiMode.PRACTICE -> """
                            Buat 2 soal latihan serupa dalam Bahasa Indonesia berdasarkan materi/soal yang dilingkari, tanpa sapaan atau basa-basi.
                            - Pertahankan topik dan tingkat kesulitan yang setara dengan variasi angka/kasus baru.
                            - Cantumkan bagian **Kunci Jawaban:** ringkas di bagian paling bawah.
                            - Gunakan LaTeX standar (${'$'}...${'$'} dan ${'$'}${'$'}...${'$'}${'$'}) dan Markdown bersih.
                        """.trimIndent()
                    }

                    val userPrompt = buildString {
                        if (customQuestion.isNotBlank()) {
                            append("Permintaan tambahan: ").append(customQuestion.trim()).append("\n\n")
                        } else {
                            when (mode) {
                                LassoAiMode.AI_TUTOR -> append("Bantu jelaskan dan selesaikan materi/soal yang dilingkari ini sebagai AI Tutor:\n\n")
                                LassoAiMode.FORMULA -> append("Ubah tulisan tangan/notasi matematika yang dilingkari ini menjadi rumus LaTeX murni tanpa teks pengantar:\n\n")
                                LassoAiMode.CHECK -> append("Periksa langkah dan jawaban yang ditulis pada area yang dilingkari ini:\n\n")
                                LassoAiMode.PRACTICE -> append("Buatkan 2 soal latihan serupa beserta kunci jawaban ringkas dari konten yang dilingkari ini:\n\n")
                            }
                        }
                        if (textSnippets.isNotEmpty()) {
                            append("Teks kotak catatan yang ikut terseleksi:\n")
                            textSnippets.forEachIndexed { idx, txt ->
                                append("${idx + 1}. ").append(txt).append('\n')
                            }
                        }
                    }

                    val thinkingBudget = if (mode == LassoAiMode.FORMULA) 0 else 256
                    val result = GeminiApiClient.streamGenerateTextFromImages(
                        systemPrompt = sysPrompt,
                        userPrompt = userPrompt,
                        imageBitmaps = sendBitmaps,
                        thinkingBudget = thinkingBudget
                    ) { partial ->
                        lassoAiResponse = partial
                    }

                    result.fold(
                        onSuccess = { reply ->
                            lassoAiResponse = reply.trim()
                        },
                        onFailure = { err ->
                            lassoAiError = err.message ?: "Gagal menghubungi Gemini AI."
                        }
                    )
                } finally {
                    bitmapsToRecycle.forEach { if (!it.isRecycled) it.recycle() }
                    isAskingLassoAi = false
                }
            }
        }
    }

    val relinkPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            coroutineScope.launch {
                val savedUri = withContext(Dispatchers.IO) {
                    NoteStorageManager.copyPdfToInternal(context, pickedUri, state.note.id) ?: pickedUri.toString()
                }
                val pageCount = withContext(Dispatchers.IO) {
                    PdfNoteHelper.getPdfPageCount(context, Uri.parse(savedUri))
                }
                state.note.pdfUri = savedUri
                state.note.pdfTotalPages = maxOf(1, pageCount)
                state.totalPages = state.note.pdfTotalPages
                PdfNoteHelper.invalidatePdfCache(savedUri)
                AcademicDatabaseManager.persistNote(state.note, immediate = true)
                state.loadPdfPage()
            }
        }
    }

    // Dynamic Viewport Vector Tile Renderer: keeps PDF text & diagrams razor-sharp at every zoom level
    LaunchedEffect(
        state.note.id,
        state.canvasType,
        state.note.pdfUri,
        state.currentPageIndex,
        state.pdfPageBitmap,
        viewportSize
    ) {
        if (state.canvasType != NoteCanvasType.PDF_DOCUMENT) return@LaunchedEffect
        val pdfUriStr = state.note.pdfUri ?: return@LaunchedEffect
        if (state.pdfPageBitmap == null || viewportSize.width <= 0 || viewportSize.height <= 0) return@LaunchedEffect

        snapshotFlow { Triple(state.scale, state.panX, state.panY) }
            .collectLatest { (currentScale, currentPanX, currentPanY) ->
                delay(90)

                val pageIdx = state.currentPageIndex
                val vpW = viewportSize.width.toFloat()
                val vpH = viewportSize.height.toFloat()
                val worldW = state.pdfPageWorldWidth
                val worldH = state.pdfPageWorldHeight

                val visLeft = -currentPanX / currentScale
                val visTop = -currentPanY / currentScale
                val visRight = (vpW - currentPanX) / currentScale
                val visBottom = (vpH - currentPanY) / currentScale

                val marginW = (visRight - visLeft) * 0.15f
                val marginH = (visBottom - visTop) * 0.15f

                val tileLeft = (visLeft - marginW).coerceIn(0f, worldW)
                val tileTop = (visTop - marginH).coerceIn(0f, worldH)
                val tileRight = (visRight + marginW).coerceIn(0f, worldW)
                val tileBottom = (visBottom + marginH).coerceIn(0f, worldH)

                val tileWorldW = tileRight - tileLeft
                val tileWorldH = tileBottom - tileTop
                if (tileWorldW <= 1f || tileWorldH <= 1f) return@collectLatest

                var bmpW = (tileWorldW * currentScale).roundToInt().coerceAtLeast(1)
                var bmpH = (tileWorldH * currentScale).roundToInt().coerceAtLeast(1)
                val maxTileDim = 2400
                if (bmpW > maxTileDim || bmpH > maxTileDim) {
                    val down = minOf(maxTileDim.toFloat() / bmpW, maxTileDim.toFloat() / bmpH)
                    bmpW = (bmpW * down).roundToInt().coerceAtLeast(1)
                    bmpH = (bmpH * down).roundToInt().coerceAtLeast(1)
                }

                val rendered = withContext(Dispatchers.IO) {
                    PdfNoteHelper.renderPdfRegion(
                        context = context,
                        uri = Uri.parse(pdfUriStr),
                        pageIndex = pageIdx,
                        worldLeft = tileLeft,
                        worldTop = tileTop,
                        worldRight = tileRight,
                        worldBottom = tileBottom,
                        targetWidth = bmpW,
                        targetHeight = bmpH
                    )
                }
                if (rendered != null && state.currentPageIndex == pageIdx) {
                    state.pdfTile = PdfRenderedTile(
                        pageIndex = pageIdx,
                        bitmap = rendered.asImageBitmap(),
                        worldLeft = tileLeft,
                        worldTop = tileTop,
                        worldRight = tileRight,
                        worldBottom = tileBottom,
                        renderedScale = currentScale
                    )
                }
            }
    }

    var lastStylusActivityMs by remember(state.note.id) { mutableLongStateOf(0L) }
    val shouldRejectFloatingTouch = remember(state.note.id) {
        { System.currentTimeMillis() - lastStylusActivityMs < 850L }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            // Track stylus activity (touch + hover) across the entire pane for palm rejection on floating bars,
            // and auto-activate pane on press.
            .pointerInput(state.note.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.changes.any { it.type == PointerType.Stylus || it.type == PointerType.Eraser }) {
                            lastStylusActivityMs = System.currentTimeMillis()
                        }
                        if (event.type == PointerEventType.Press) {
                            onActivate()
                        }
                    }
                }
            }
    ) {
        val isFingerDrawing = AppSettingsManager.touchInputMode == TouchInputMode.FINGER_AND_STYLUS
        var transformGeneration by remember { mutableIntStateOf(0) }
        var lastTransformTime by remember { mutableLongStateOf(0L) }

        // ── 1. Pan & Zoom Gesture Detector (Stylus-Only or Hand-Touch Mode) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { viewportSize = it }
                .pointerInput(state.note.id, isFingerDrawing) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)

                        if (!isFingerDrawing) {
                            // ── MODE HANYA STYLUS (Palm Rejection Maksimal) ──
                            // 1 Jari = Geser (Pan), 2 Jari = Zoom & Pan, Stylus = Tulis
                            // Pengecualian: sentuhan 1 jari di atas TextBox/Gambar tetap diteruskan agar bisa dipindah pakai tangan!
                            if (down.type == PointerType.Touch) {
                                val worldDownX = (down.position.x - state.panX) / state.scale
                                val worldDownY = (down.position.y - state.panY) / state.scale
                                val hitPlacedElement = state.textBoxes.any { tb ->
                                    val topPad = if (tb.isSelected) with(density) { 44.dp.toPx() } / state.scale else 12f / state.scale
                                    val sidePad = if (tb.isSelected) with(density) { 26.dp.toPx() } / state.scale else 12f / state.scale
                                    worldDownX in (tb.x - sidePad)..(tb.x + tb.width + sidePad) &&
                                        worldDownY in (tb.y - topPad)..(tb.y + tb.height + sidePad)
                                } || state.imageElements.any { img ->
                                    val pad = if (img.isSelected) with(density) { 26.dp.toPx() } / state.scale else 12f / state.scale
                                    worldDownX in (img.x - pad)..(img.x + img.width + pad) &&
                                        worldDownY in (img.y - pad)..(img.y + img.height + pad)
                                }

                                if (hitPlacedElement) {
                                    // Allow finger tap/drag on TextBox or Image, unless user puts a 2nd finger down to pinch-zoom
                                    while (true) {
                                        val ev = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val pressedTouches = ev.changes.filter { it.type == PointerType.Touch && it.pressed }
                                        if (pressedTouches.isEmpty()) break
                                        if (pressedTouches.size >= 2) {
                                            val zoom = ev.calculateZoom()
                                            val pan = ev.calculatePan()
                                            val centroid = ev.calculateCentroid(useCurrent = false)
                                            ev.changes.forEach { it.consume() }
                                            if (centroid != Offset.Unspecified && !zoom.isNaN() && zoom > 0f) {
                                                val minScale = if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) 0.4f else 0.5f
                                                val maxScale = if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) 5.0f else 3.5f
                                                val newScale = (state.scale * zoom).coerceIn(minScale, maxScale)
                                                val effectiveZoom = newScale / state.scale
                                                state.panX = centroid.x - (centroid.x - state.panX) * effectiveZoom + pan.x
                                                state.panY = centroid.y - (centroid.y - state.panY) * effectiveZoom + pan.y
                                                state.scale = newScale
                                            }
                                        }
                                    }
                                    return@awaitEachGesture
                                }

                                down.consume()

                                var isTransforming = false
                                var accumulatedPan = Offset.Zero
                                val touchSlop = viewConfiguration.touchSlop

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
                                            state.deselectAll()
                                        }
                                        break
                                    }

                                    if (pressedFingers.size >= 2) {
                                        // 2-finger pinch-to-zoom & pan
                                        isTransforming = true
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val centroid = event.calculateCentroid(useCurrent = false)
                                        event.changes.forEach { it.consume() }

                                        if (centroid != Offset.Unspecified && !zoom.isNaN() && zoom > 0f) {
                                            val minScale = if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) 0.4f else 0.5f
                                            val maxScale = if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) 5.0f else 3.5f
                                            val newScale = (state.scale * zoom).coerceIn(minScale, maxScale)
                                            val effectiveZoom = newScale / state.scale
                                            state.panX = centroid.x - (centroid.x - state.panX) * effectiveZoom + pan.x
                                            state.panY = centroid.y - (centroid.y - state.panY) * effectiveZoom + pan.y
                                            state.scale = newScale
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
                                                state.panX += accumulatedPan.x
                                                state.panY += accumulatedPan.y
                                            }
                                        } else {
                                            state.panX += panDelta.x
                                            state.panY += panDelta.y
                                        }
                                    }
                                }
                            }
                        } else {
                            // ── MODE SENTUHAN TANGAN & STYLUS ──
                            // 1 Jari / Stylus = Coret-coret (Drawing/Eraser/Shape/Lasso)
                            // 2 Jari = Geser (Pan) & Zoom
                            var isTransforming = false

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                                // Palm rejection: if stylus is active on screen, consume finger touches
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
                                    break
                                }

                                if (pressedFingers.size >= 2) {
                                    if (!isTransforming) {
                                        isTransforming = true
                                        transformGeneration++
                                    }
                                    lastTransformTime = System.currentTimeMillis()

                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    event.changes.forEach { it.consume() }

                                    if (centroid != Offset.Unspecified && !zoom.isNaN() && zoom > 0f) {
                                        val minScale = if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) 0.4f else 0.5f
                                        val maxScale = if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) 5.0f else 3.5f
                                        val newScale = (state.scale * zoom).coerceIn(minScale, maxScale)
                                        val effectiveZoom = newScale / state.scale
                                        state.panX = centroid.x - (centroid.x - state.panX) * effectiveZoom + pan.x
                                        state.panY = centroid.y - (centroid.y - state.panY) * effectiveZoom + pan.y
                                        state.scale = newScale
                                    }
                                } else if (isTransforming) {
                                    // Keep consuming remainder of 2-finger gesture until all lifted
                                    event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                                    lastTransformTime = System.currentTimeMillis()
                                }
                                // If 1 finger and !isTransforming:
                                // DO NOT CONSUME. Event passes down to DrawingSurface for 1-finger drawing!
                            }
                        }
                    }
                }
        ) {
            // Background Layer: Either PDF Document Page or Paper Style (Strictly clipped)
            if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) {
                Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    clipRect(0f, 0f, size.width, size.height) {
                        drawRect(if (state.isDarkPaper) Color(0xFF091426) else Color(0xFFE2E8F0)) // desk canvas background
                        if (state.pdfPageBitmap != null) {
                            val bmp = state.pdfPageBitmap!!
                            val dstW = state.pdfPageWorldWidth * state.scale
                            val dstH = state.pdfPageWorldHeight * state.scale

                            drawRoundRect(
                                color = Color(0x1F0F172A),
                                topLeft = Offset(state.panX + 4f, state.panY + 6f),
                                size = Size(dstW, dstH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )

                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(state.panX.roundToInt(), state.panY.roundToInt()),
                                dstSize = IntSize(dstW.roundToInt().coerceAtLeast(1), dstH.roundToInt().coerceAtLeast(1)),
                                filterQuality = FilterQuality.Medium
                            )

                            val tile = state.pdfTile
                            if (tile != null && tile.pageIndex == state.currentPageIndex) {
                                val tLeft = state.panX + tile.worldLeft * state.scale
                                val tTop = state.panY + tile.worldTop * state.scale
                                val tRight = state.panX + tile.worldRight * state.scale
                                val tBottom = state.panY + tile.worldBottom * state.scale
                                drawImage(
                                    image = tile.bitmap,
                                    dstOffset = IntOffset(tLeft.roundToInt(), tTop.roundToInt()),
                                    dstSize = IntSize(
                                        (tRight - tLeft).roundToInt().coerceAtLeast(1),
                                        (tBottom - tTop).roundToInt().coerceAtLeast(1)
                                    ),
                                    filterQuality = FilterQuality.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                PaperBackground(
                    style = state.paperStyle,
                    scaleProvider = { state.scale },
                    panXProvider = { state.panX },
                    panYProvider = { state.panY },
                    isPaged = state.canvasType == NoteCanvasType.PAGED_NOTE,
                    isDarkPaper = state.isDarkPaper,
                    modifier = Modifier.fillMaxSize().clipToBounds()
                )
            }

            // Inking Layer (AndroidX Ink Native Vector Tessellation - Strictly clipped)
            DrawingSurface(
                strokes = state.strokes,
                canvasStrokeRenderer = state.canvasStrokeRenderer,
                currentBrush = currentBrush,
                onGetNextBrush = { currentBrush },
                onStrokesFinished = { newStrokes ->
                    val isRecentTransform = System.currentTimeMillis() - lastTransformTime < 350L
                    if (!isRecentTransform) {
                        val strokesToAdd = mutableListOf<Stroke>()
                        for (stroke in newStrokes) {
                            // ── Scribble to Erase: Scratch-out gesture over handwriting or text ──
                            if (AppSettingsManager.isScribbleToEraseEnabled && (activeTool == NoteTool.PEN || activeTool == NoteTool.HIGHLIGHTER)) {
                                if (ScribbleEraseRecognizer.isScribble(stroke)) {
                                    val erased = state.eraseWithScribble(stroke)
                                    if (erased) {
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        } catch (e: Exception) {}
                                        continue // Gesture consumed! Do not add scribble stroke to canvas.
                                    }
                                }
                            }

                            if (isStrokeHoldAtEnd(stroke, state.scale)) {
                                val refined = ShapeRecognizer.refineStroke(stroke)
                                if (refined != null) {
                                    try {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    } catch (e: Exception) {}
                                    strokesToAdd.add(refined)
                                } else {
                                    strokesToAdd.add(stroke)
                                }
                            } else {
                                strokesToAdd.add(stroke)
                            }
                        }
                        if (strokesToAdd.isNotEmpty()) {
                            if (state.textBoxes.any { it.isSelected } || state.imageElements.any { it.isSelected }) {
                                state.deselectAll()
                            }
                            state.pushUndoSnapshot()
                            state.strokes.addAll(strokesToAdd)
                            state.saveActivePageData()
                        }
                    }
                },
                isEraserMode = activeTool == NoteTool.ERASER && isActive,
                isDarkPaper = state.isDarkPaper,
                isDrawingEnabled = activeTool == NoteTool.PEN || activeTool == NoteTool.HIGHLIGHTER || activeTool == NoteTool.ERASER,
                isFingerDrawingEnabled = isFingerDrawing,
                transformGeneration = transformGeneration,
                eraserRadiusPx = AppSettingsManager.eraserRadius,
                scaleProvider = { state.scale },
                panXProvider = { state.panX },
                panYProvider = { state.panY },
                onEraseStart = { state.beginUndoTransaction() },
                onErase = { worldX, worldY -> state.eraseNear(worldX, worldY, AppSettingsManager.eraserRadius, AppSettingsManager.eraserMode) },
                onEraseEnd = {
                    state.commitUndoTransactionIfChanged()
                    state.saveActivePageData()
                },
                modifier = Modifier.fillMaxSize().clipToBounds()
            )

            // Shape Tool Drawing & Live Preview Layer (Strictly clipped)
            if (activeTool == NoteTool.SHAPE && isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(state.note.id, selectedShapeType, currentBrush, isFingerDrawing) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val isAllowed = down.type == PointerType.Stylus || down.type == PointerType.Eraser || (isFingerDrawing && down.type == PointerType.Touch)
                                if (!isAllowed) {
                                    return@awaitEachGesture
                                }

                                val worldStart = Offset((down.position.x - state.panX) / state.scale, (down.position.y - state.panY) / state.scale)
                                state.shapeDrawStart = worldStart
                                state.shapeDrawCurrent = worldStart

                                drag(down.id) { change ->
                                    val isChangeAllowed = change.type == PointerType.Stylus || change.type == PointerType.Eraser || (isFingerDrawing && change.type == PointerType.Touch)
                                    if (isChangeAllowed) {
                                        change.consume()
                                        state.shapeDrawCurrent = Offset((change.position.x - state.panX) / state.scale, (change.position.y - state.panY) / state.scale)
                                    }
                                }

                                val start = state.shapeDrawStart
                                val end = state.shapeDrawCurrent
                                if (start != null && end != null && (end - start).getDistance() > 15f) {
                                    val newStroke = ShapeRecognizer.generateGeometricShape(
                                        type = selectedShapeType,
                                        start = start,
                                        end = end,
                                        brush = currentBrush
                                    )
                                    state.pushUndoSnapshot()
                                    state.strokes.add(newStroke)
                                    state.saveActivePageData()
                                }
                                state.shapeDrawStart = null
                                state.shapeDrawCurrent = null
                            }
                        }
                )

                // Live Preview Canvas
                Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    clipRect(0f, 0f, size.width, size.height) {
                        val start = state.shapeDrawStart
                        val end = state.shapeDrawCurrent
                        if (start != null && end != null) {
                            val sX = start.x * state.scale + state.panX
                            val sY = start.y * state.scale + state.panY
                            val eX = end.x * state.scale + state.panX
                            val eY = end.y * state.scale + state.panY

                            val minX = minOf(sX, eX)
                            val maxX = maxOf(sX, eX)
                            val minY = minOf(sY, eY)
                            val maxY = maxOf(sY, eY)
                            val w = maxOf(10f, maxX - minX)
                            val h = maxOf(10f, maxY - minY)
                            val strokeW = selectedThickness * state.scale
                            val shapePreviewColor = if (state.isDarkPaper && (selectedColor == Color.Black || selectedColor == Color(0xFF0F172A) || selectedColor == Color(0xFF191C1E))) Color(0xFFF8FAFC) else selectedColor

                            when (selectedShapeType) {
                                GeometricShapeType.LINE -> {
                                    drawLine(color = shapePreviewColor, start = Offset(sX, sY), end = Offset(eX, eY), strokeWidth = strokeW)
                                }
                                GeometricShapeType.RECTANGLE -> {
                                    drawRect(color = shapePreviewColor, topLeft = Offset(minX, minY), size = Size(w, h), style = DrawStroke(width = strokeW))
                                }
                                GeometricShapeType.CIRCLE -> {
                                    drawOval(color = shapePreviewColor, topLeft = Offset(minX, minY), size = Size(w, h), style = DrawStroke(width = strokeW))
                                }
                                GeometricShapeType.TRIANGLE -> {
                                    val path = Path().apply {
                                        moveTo((minX + maxX) / 2f, minY)
                                        lineTo(maxX, maxY)
                                        lineTo(minX, maxY)
                                        close()
                                    }
                                    drawPath(path = path, color = shapePreviewColor, style = DrawStroke(width = strokeW))
                                }
                                GeometricShapeType.ARROW -> {
                                    drawLine(color = shapePreviewColor, start = Offset(sX, sY), end = Offset(eX, eY), strokeWidth = strokeW)
                                    val dx = eX - sX
                                    val dy = eY - sY
                                    val angle = kotlin.math.atan2(dy, dx)
                                    val headLen = minOf(36f * state.scale, kotlin.math.hypot(dx, dy) * 0.35f)
                                    val headAngle = Math.toRadians(30.0).toFloat()
                                    drawLine(
                                        color = shapePreviewColor,
                                        start = Offset(eX, eY),
                                        end = Offset(eX - headLen * kotlin.math.cos(angle - headAngle), eY - headLen * kotlin.math.sin(angle - headAngle)),
                                        strokeWidth = strokeW
                                    )
                                    drawLine(
                                        color = shapePreviewColor,
                                        start = Offset(eX, eY),
                                        end = Offset(eX - headLen * kotlin.math.cos(angle + headAngle), eY - headLen * kotlin.math.sin(angle + headAngle)),
                                        strokeWidth = strokeW
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lasso Tool Selection & Manipulation Layer (Strictly clipped)
            if ((activeTool == NoteTool.LASSO || state.lassoSelection != null) && isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(state.note.id, activeTool, state.lassoSelection == null, isFingerDrawing) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val isAllowed = down.type == PointerType.Stylus || down.type == PointerType.Eraser || (isFingerDrawing && down.type == PointerType.Touch)
                                if (!isAllowed) {
                                    return@awaitEachGesture
                                }

                                val screenPoint = down.position
                                val worldPoint = Offset((screenPoint.x - state.panX) / state.scale, (screenPoint.y - state.panY) / state.scale)

                                val curSel = state.lassoSelection
                                if (curSel != null && !curSel.isEmpty) {
                                    val screenBounds = Rect(
                                        curSel.bounds.left * state.scale + state.panX,
                                        curSel.bounds.top * state.scale + state.panY,
                                        curSel.bounds.right * state.scale + state.panX,
                                        curSel.bounds.bottom * state.scale + state.panY
                                    )
                                    if (screenBounds.contains(screenPoint)) {
                                        val strokeIndices = curSel.selectedStrokes.map { state.strokes.indexOf(it) }
                                        state.beginUndoTransaction()

                                        drag(down.id) { change ->
                                            val isChangeAllowed = change.type == PointerType.Stylus || change.type == PointerType.Eraser || (isFingerDrawing && change.type == PointerType.Touch)
                                            if (isChangeAllowed) {
                                                val delta = change.positionChange()
                                                change.consume()
                                                val dx = delta.x / state.scale
                                                val dy = delta.y / state.scale

                                                if (dx != 0f || dy != 0f) {
                                                    for (i in curSel.selectedStrokes.indices) {
                                                        val oldStroke = curSel.selectedStrokes[i]
                                                        val newStroke = ShapeRecognizer.translateStroke(oldStroke, dx, dy)
                                                        val idxInMain = strokeIndices[i]
                                                        if (idxInMain != -1 && idxInMain < state.strokes.size) {
                                                            state.strokes[idxInMain] = newStroke
                                                        }
                                                        curSel.selectedStrokes[i] = newStroke
                                                    }
                                                    for (box in curSel.selectedTextBoxes) {
                                                        box.x += dx
                                                        box.y += dy
                                                    }
                                                    for (img in curSel.selectedImages) {
                                                        img.x += dx
                                                        img.y += dy
                                                    }
                                                    val sel = state.lassoSelection ?: curSel
                                                    val newBounds = sel.bounds.translate(Offset(dx, dy))
                                                    state.lassoSelection = sel.copy(bounds = newBounds)
                                                }
                                            }
                                        }
                                        state.commitUndoTransactionIfChanged()
                                        state.saveActivePageData()
                                        return@awaitEachGesture
                                    } else {
                                        state.lassoSelection = null
                                    }
                                }

                                if (activeTool == NoteTool.LASSO) {
                                    state.currentLassoLoop.clear()
                                    state.currentLassoLoop.add(worldPoint)

                                    drag(down.id) { change ->
                                        val isChangeAllowed = change.type == PointerType.Stylus || change.type == PointerType.Eraser || (isFingerDrawing && change.type == PointerType.Touch)
                                        if (isChangeAllowed) {
                                            change.consume()
                                            val curWorld = Offset((change.position.x - state.panX) / state.scale, (change.position.y - state.panY) / state.scale)
                                            state.currentLassoLoop.add(curWorld)
                                        }
                                    }

                                    if (state.currentLassoLoop.size >= 4) {
                                        val loopCopy = state.currentLassoLoop.toList()
                                        val polyBounds = LassoHelper.computePolygonBounds(loopCopy, padding = 8f)

                                        if (state.canvasType == NoteCanvasType.PDF_DOCUMENT && polyBounds != null && polyBounds.width >= 24f && polyBounds.height >= 24f) {
                                            // Di mode PDF: dukung deteksi konten teks/gambar/soal PDF secara langsung
                                            val matchedStrokes = state.strokes.filter {
                                                LassoHelper.isStrokeSelected(it, loopCopy) || LassoHelper.isStrokeInBounds(it, polyBounds)
                                            }.toMutableList()
                                            val matchedBoxes = state.textBoxes.filter { LassoHelper.isTextBoxSelected(it, loopCopy) }.toMutableList()
                                            val matchedImages = state.imageElements.filter { LassoHelper.isImageSelected(it, loopCopy) }.toMutableList()

                                            state.lassoSelection = LassoSelectionState(
                                                selectedStrokes = matchedStrokes,
                                                selectedTextBoxes = matchedBoxes,
                                                selectedImages = matchedImages,
                                                bounds = polyBounds,
                                                isRegionCapture = true,
                                                lassoPolygon = loopCopy
                                            )
                                        } else {
                                            val matchedStrokes = state.strokes.filter { LassoHelper.isStrokeSelected(it, loopCopy) }.toMutableList()
                                            val matchedBoxes = state.textBoxes.filter { LassoHelper.isTextBoxSelected(it, loopCopy) }.toMutableList()
                                            val matchedImages = state.imageElements.filter { LassoHelper.isImageSelected(it, loopCopy) }.toMutableList()

                                            val bounds = LassoHelper.computeCombinedBounds(matchedStrokes, matchedBoxes, matchedImages)
                                            if (bounds != null && (matchedStrokes.isNotEmpty() || matchedBoxes.isNotEmpty() || matchedImages.isNotEmpty())) {
                                                state.lassoSelection = LassoSelectionState(
                                                    selectedStrokes = matchedStrokes,
                                                    selectedTextBoxes = matchedBoxes,
                                                    selectedImages = matchedImages,
                                                    bounds = bounds,
                                                    isRegionCapture = false,
                                                    lassoPolygon = loopCopy
                                                )
                                            } else if (polyBounds != null && polyBounds.width >= 24f && polyBounds.height >= 24f) {
                                                // Lasso melingkari gambar atau ruang kanvas
                                                val overlappingImages = state.imageElements.filter { img ->
                                                    val imgRect = Rect(img.x, img.y, img.x + img.width, img.y + img.height)
                                                    imgRect.overlaps(polyBounds) || LassoHelper.isImageSelected(img, loopCopy)
                                                }.toMutableList()

                                                state.lassoSelection = LassoSelectionState(
                                                    selectedStrokes = mutableListOf(),
                                                    selectedTextBoxes = mutableListOf(),
                                                    selectedImages = overlappingImages,
                                                    bounds = polyBounds,
                                                    isRegionCapture = true,
                                                    lassoPolygon = loopCopy
                                                )
                                            } else {
                                                state.lassoSelection = null
                                            }
                                        }
                                    }
                                    state.currentLassoLoop.clear()
                                }
                            }
                        }
                )

                // Render in-progress Lasso loop (Strictly clipped)
                if (state.currentLassoLoop.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        clipRect(0f, 0f, size.width, size.height) {
                            val path = Path().apply {
                                val p0 = state.currentLassoLoop[0]
                                moveTo(p0.x * state.scale + state.panX, p0.y * state.scale + state.panY)
                                for (i in 1 until state.currentLassoLoop.size) {
                                    val p = state.currentLassoLoop[i]
                                    lineTo(p.x * state.scale + state.panX, p.y * state.scale + state.panY)
                                }
                            }
                            drawPath(path = path, color = AccentBlue.copy(alpha = 0.12f), style = Fill)
                            drawPath(
                                path = path,
                                color = AccentBlue,
                                style = DrawStroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                )
                            )
                        }
                    }
                }

                // Render Lasso Selection Marquee & Action Menu
                val curSel = state.lassoSelection
                if (curSel != null && !curSel.isEmpty) {
                    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        val sel = state.lassoSelection ?: return@Canvas
                        if (sel.isEmpty) return@Canvas
                        val sLeft = sel.bounds.left * state.scale + state.panX
                        val sTop = sel.bounds.top * state.scale + state.panY
                        val sRight = sel.bounds.right * state.scale + state.panX
                        val sBottom = sel.bounds.bottom * state.scale + state.panY
                        val sWidth = maxOf(40f, sRight - sLeft)
                        val sHeight = maxOf(40f, sBottom - sTop)

                        clipRect(0f, 0f, size.width, size.height) {
                            drawRoundRect(
                                color = AccentBlue,
                                topLeft = Offset(sLeft, sTop),
                                size = Size(sWidth, sHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = DrawStroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            )
                        }
                    }

                    // Floating Action Menu (Follows selection bounds dynamically)
                    Box(
                        modifier = Modifier
                            .offset {
                                val sel = state.lassoSelection
                                if (sel != null && !sel.isEmpty) {
                                    val sLeft = sel.bounds.left * state.scale + state.panX
                                    val sTop = sel.bounds.top * state.scale + state.panY
                                    val sRight = sel.bounds.right * state.scale + state.panX
                                    val sWidth = maxOf(40f, sRight - sLeft)
                                    IntOffset(
                                        x = (sLeft + sWidth / 2f - 190.dp.toPx()).roundToInt().coerceAtLeast(16),
                                        y = (sTop - 52.dp.toPx()).roundToInt().coerceAtLeast(70)
                                    )
                                } else {
                                    IntOffset.Zero
                                }
                            }
                    ) {
                        val hasAiContent = curSel.selectedStrokes.isNotEmpty() ||
                                curSel.selectedTextBoxes.isNotEmpty() ||
                                curSel.selectedImages.isNotEmpty() ||
                                curSel.isRegionCapture ||
                                state.canvasType == NoteCanvasType.PDF_DOCUMENT
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = LernspaceSurfaceCard,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.widthIn(max = 420.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Geser (pinned left, tidak ikut horizontalScroll agar drag mulus)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .pointerInput(state.note.id, state.lassoSelection == null) {
                                            detectDragGestures(
                                                onDragStart = { state.beginUndoTransaction() },
                                                onDragEnd = {
                                                    state.commitUndoTransactionIfChanged()
                                                    state.saveActivePageData()
                                                },
                                                onDragCancel = { state.commitUndoTransactionIfChanged() }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                val sel = state.lassoSelection ?: return@detectDragGestures
                                                val dx = dragAmount.x / state.scale
                                                val dy = dragAmount.y / state.scale
                                                if (dx != 0f || dy != 0f) {
                                                    for (i in sel.selectedStrokes.indices) {
                                                        val oldStroke = sel.selectedStrokes[i]
                                                        val newStroke = ShapeRecognizer.translateStroke(oldStroke, dx, dy)
                                                        val idxInMain = state.strokes.indexOf(oldStroke)
                                                        if (idxInMain != -1) {
                                                            state.strokes[idxInMain] = newStroke
                                                        }
                                                        sel.selectedStrokes[i] = newStroke
                                                    }
                                                    for (box in sel.selectedTextBoxes) {
                                                        box.x += dx
                                                        box.y += dy
                                                    }
                                                    for (img in sel.selectedImages) {
                                                        img.x += dx
                                                        img.y += dy
                                                    }
                                                    val newBounds = sel.bounds.translate(Offset(dx, dy))
                                                    state.lassoSelection = sel.copy(bounds = newBounds)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.OpenWith, contentDescription = "Geser", tint = LernspaceTextPrimary, modifier = Modifier.size(15.dp))
                                    Text("Geser", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LernspaceTextPrimary)
                                }

                                VerticalDivider(modifier = Modifier.height(16.dp), color = LernspaceBorder)

                                // Bagian tengah scrollable 1 baris
                                Row(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (hasAiContent) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LernspaceDark,
                                            modifier = Modifier.clickable {
                                                lassoAiCustomPrompt = ""
                                                triggerLassoAi(curSel, "", LassoAiMode.AI_TUTOR)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Tanya AI", tint = LernspaceSurfaceCard, modifier = Modifier.size(13.dp))
                                                Text("Tanya AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LernspaceSurfaceCard)
                                            }
                                        }

                                        LassoAiMode.entries.forEach { mode ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = LernspaceSurfaceLow,
                                                border = BorderStroke(1.dp, LernspaceBorder),
                                                modifier = Modifier.clickable {
                                                    lassoAiCustomPrompt = ""
                                                    triggerLassoAi(curSel, "", mode)
                                                }
                                            ) {
                                                Text(
                                                    text = mode.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = LernspaceTextPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (curSel.selectedStrokes.isNotEmpty()) {
                                        // Ganti Warna Stroke Lasso
                                        val lassoColors = listOf(
                                            Color(0xFF0F172A), // Dark Charcoal
                                            AccentBlue,        // Biru
                                            AccentRed,         // Merah
                                            Color(0xFF059669), // Hijau
                                            Color(0xFFD97706)  // Amber / Oranye
                                        )
                                        lassoColors.forEach { col ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                                    .clickable {
                                                        state.pushUndoSnapshot()
                                                        for (i in curSel.selectedStrokes.indices) {
                                                            val oldStroke = curSel.selectedStrokes[i]
                                                            val recolored = ShapeRecognizer.recolorStroke(oldStroke, col)
                                                            val idxInMain = state.strokes.indexOf(oldStroke)
                                                            if (idxInMain != -1) {
                                                                state.strokes[idxInMain] = recolored
                                                            }
                                                            curSel.selectedStrokes[i] = recolored
                                                        }
                                                        state.saveActivePageData()
                                                    }
                                            )
                                        }

                                        VerticalDivider(modifier = Modifier.height(16.dp), color = LernspaceBorder)
                                    }

                                    // Duplikat
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.clickable {
                                            val sel = state.lassoSelection ?: return@clickable
                                            val offsetDelta = 35f
                                            state.pushUndoSnapshot()

                                            // 1. Duplikasi strokes jika ada
                                            if (sel.selectedStrokes.isNotEmpty()) {
                                                val newDuplicates = sel.selectedStrokes.map { ShapeRecognizer.translateStroke(it, offsetDelta, offsetDelta) }
                                                state.strokes.addAll(newDuplicates)
                                                sel.selectedStrokes.clear()
                                                sel.selectedStrokes.addAll(newDuplicates)
                                            }

                                            // 2. Duplikasi images jika ada
                                            if (sel.selectedImages.isNotEmpty()) {
                                                val newImgDups = sel.selectedImages.map { img ->
                                                    img.copy(
                                                        id = UUID.randomUUID().toString(),
                                                        x = img.x + offsetDelta,
                                                        y = img.y + offsetDelta,
                                                        isSelected = false
                                                    )
                                                }
                                                state.imageElements.addAll(newImgDups)
                                            }

                                            // 3. Jika region capture pada PDF (crop potongan PDF ke catatan baru)
                                            if (sel.isRegionCapture && state.canvasType == NoteCanvasType.PDF_DOCUMENT) {
                                                state.pdfPageBitmap?.let { pdfImageBitmap ->
                                                    runCatching {
                                                        val pdfBmp = pdfImageBitmap.asAndroidBitmap()
                                                        if (!pdfBmp.isRecycled) {
                                                            val scaleX = pdfBmp.width.toFloat() / state.pdfPageWorldWidth.coerceAtLeast(1f)
                                                            val scaleY = pdfBmp.height.toFloat() / state.pdfPageWorldHeight.coerceAtLeast(1f)
                                                            val leftPx = (sel.bounds.left * scaleX).roundToInt().coerceIn(0, pdfBmp.width - 1)
                                                            val topPx = (sel.bounds.top * scaleY).roundToInt().coerceIn(0, pdfBmp.height - 1)
                                                            val rightPx = (sel.bounds.right * scaleX).roundToInt().coerceIn(leftPx + 1, pdfBmp.width)
                                                            val bottomPx = (sel.bounds.bottom * scaleY).roundToInt().coerceIn(topPx + 1, pdfBmp.height)
                                                            val cropW = rightPx - leftPx
                                                            val cropH = bottomPx - topPx
                                                            if (cropW > 4 && cropH > 4) {
                                                                val cropped = android.graphics.Bitmap.createBitmap(pdfBmp, leftPx, topPx, cropW, cropH)
                                                                val newElem = ImageElement(
                                                                    id = UUID.randomUUID().toString(),
                                                                    bitmap = cropped.asImageBitmap(),
                                                                    x = sel.bounds.left + offsetDelta,
                                                                    y = sel.bounds.top + offsetDelta,
                                                                    width = sel.bounds.width,
                                                                    height = sel.bounds.height
                                                                )
                                                                state.imageElements.add(newElem)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            val newBounds = sel.bounds.translate(Offset(offsetDelta, offsetDelta))
                                            state.lassoSelection = sel.copy(bounds = newBounds)
                                            state.saveActivePageData()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplikat", tint = LernspaceTextPrimary, modifier = Modifier.size(13.dp))
                                            Text("Duplikat", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                                        }
                                    }

                                    // Hapus
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.clickable {
                                            state.pushUndoSnapshot()
                                            state.strokes.removeAll(curSel.selectedStrokes)
                                            state.textBoxes.removeAll(curSel.selectedTextBoxes)
                                            state.imageElements.removeAll(curSel.selectedImages)
                                            state.lassoSelection = null
                                            state.saveActivePageData()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = LernspaceTextPrimary, modifier = Modifier.size(13.dp))
                                            Text("Hapus", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                                        }
                                    }
                                }

                                // Selesai (pinned right)
                                Surface(
                                    shape = CircleShape,
                                    color = LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.size(26.dp).clickable { state.lassoSelection = null }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Close, contentDescription = "Selesai", tint = LernspaceTextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Placed Images Layer (Rendered inside pane)
            state.imageElements.forEachIndexed { index, img ->
                DraggableImage(
                    image = img,
                    scale = state.scale,
                    panXProvider = { state.panX },
                    panYProvider = { state.panY },
                    onCommitMove = { newWorldX, newWorldY ->
                        if (index < state.imageElements.size) {
                            state.pushUndoSnapshot()
                            state.imageElements[index] = state.imageElements[index].copy(x = newWorldX, y = newWorldY)
                            state.saveActivePageData()
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < state.imageElements.size) {
                            state.pushUndoSnapshot()
                            state.imageElements[index] = state.imageElements[index].copy(width = newWorldW, height = newWorldH)
                            state.saveActivePageData()
                        }
                    },
                    onSelect = {
                        state.deselectAll()
                        if (index < state.imageElements.size) {
                            state.imageElements[index] = state.imageElements[index].copy(isSelected = true)
                        }
                    },
                    onDelete = {
                        state.pushUndoSnapshot()
                        state.imageElements.remove(img)
                        state.saveActivePageData()
                    }
                )
            }

            // Placed Draggable Text Boxes Layer (Rendered inside pane)
            state.textBoxes.forEachIndexed { index, boxElement ->
                DraggableTextBox(
                    box = boxElement,
                    scale = state.scale,
                    panXProvider = { state.panX },
                    panYProvider = { state.panY },
                    isDarkPaper = state.isDarkPaper,
                    onCommitMove = { newWorldX, newWorldY ->
                        if (index < state.textBoxes.size) {
                            state.pushUndoSnapshot()
                            state.textBoxes[index] = state.textBoxes[index].copy(x = newWorldX, y = newWorldY)
                            state.saveActivePageData()
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < state.textBoxes.size) {
                            state.pushUndoSnapshot()
                            state.textBoxes[index] = state.textBoxes[index].copy(width = newWorldW, height = newWorldH)
                            state.saveActivePageData()
                        }
                    },
                    onTextChange = { newText ->
                        if (index < state.textBoxes.size) {
                            state.textBoxes[index] = state.textBoxes[index].copy(text = newText)
                        }
                    },
                    onSelect = {
                        state.deselectAll()
                        if (index < state.textBoxes.size) {
                            state.textBoxes[index] = state.textBoxes[index].copy(isSelected = true)
                        }
                    },
                    onDelete = {
                        state.pushUndoSnapshot()
                        state.textBoxes.remove(boxElement)
                        state.saveActivePageData()
                    }
                )
            }
        }

        // Selected TextBox Rich Formatting Floating Bar
        val selectedTbIndex = state.textBoxes.indexOfFirst { it.isSelected }
        if (isActive && selectedTbIndex in state.textBoxes.indices) {
            val activeTb = state.textBoxes[selectedTbIndex]
            val colorPresets: List<Pair<String, Int?>> = listOf(
                "Auto" to null,
                "Biru" to Color(0xFF2563EB).toArgb(),
                "Merah" to Color(0xFFDC2626).toArgb(),
                "Hijau" to Color(0xFF059669).toArgb(),
                "Ungu" to Color(0xFF7C3AED).toArgb(),
                "Oranye" to Color(0xFFD97706).toArgb()
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.97f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 82.dp)
                    .zIndex(14f)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Font size stepper: A- / size / A+
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            state.pushUndoSnapshot()
                            val nextSize = (activeTb.fontSize - 4f).coerceIn(14f, 110f)
                            state.textBoxes[selectedTbIndex] = activeTb.copy(fontSize = nextSize)
                            state.saveActivePageData()
                        }
                    ) {
                        Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                    Text(
                        text = "${activeTb.fontSize.roundToInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            state.pushUndoSnapshot()
                            val nextSize = (activeTb.fontSize + 4f).coerceIn(14f, 110f)
                            state.textBoxes[selectedTbIndex] = activeTb.copy(fontSize = nextSize)
                            state.saveActivePageData()
                        }
                    ) {
                        Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }

                    VerticalDivider(modifier = Modifier.height(18.dp), color = Color(0xFFE2E8F0))

                    // Bold toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTb.isBold) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            state.pushUndoSnapshot()
                            state.textBoxes[selectedTbIndex] = activeTb.copy(isBold = !activeTb.isBold)
                            state.saveActivePageData()
                        }
                    ) {
                        Text(
                            "B",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeTb.isBold) AccentBlue else LernspaceDark,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    // Italic toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTb.isItalic) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            state.pushUndoSnapshot()
                            state.textBoxes[selectedTbIndex] = activeTb.copy(isItalic = !activeTb.isItalic)
                            state.saveActivePageData()
                        }
                    ) {
                        Text(
                            "I",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = if (activeTb.isItalic) AccentBlue else LernspaceDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Alignment cycle button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            state.pushUndoSnapshot()
                            val nextAlign = when (activeTb.textAlign) {
                                TextBoxAlign.LEFT -> TextBoxAlign.CENTER
                                TextBoxAlign.CENTER -> TextBoxAlign.RIGHT
                                TextBoxAlign.RIGHT -> TextBoxAlign.LEFT
                            }
                            state.textBoxes[selectedTbIndex] = activeTb.copy(textAlign = nextAlign)
                            state.saveActivePageData()
                        }
                    ) {
                        val alignIcon = when (activeTb.textAlign) {
                            TextBoxAlign.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                            TextBoxAlign.CENTER -> Icons.Filled.FormatAlignCenter
                            TextBoxAlign.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                        }
                        Icon(
                            imageVector = alignIcon,
                            contentDescription = "Rata Teks",
                            tint = LernspaceDark,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp).size(15.dp)
                        )
                    }

                    // Card background toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTb.hasBackground) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            state.pushUndoSnapshot()
                            state.textBoxes[selectedTbIndex] = activeTb.copy(hasBackground = !activeTb.hasBackground)
                            state.saveActivePageData()
                        }
                    ) {
                        Text(
                            "Kartu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeTb.hasBackground) Color(0xFF15803D) else LernspaceDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(18.dp), color = Color(0xFFE2E8F0))

                    // Color Swatches
                    colorPresets.forEach { (_, argb) ->
                        val isPicked = activeTb.colorArgb == argb
                        val swatchColor = argb?.let { Color(it) } ?: if (state.isDarkPaper) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .then(
                                    if (isPicked) Modifier.padding(2.dp) else Modifier
                                )
                                .clickable {
                                    state.pushUndoSnapshot()
                                    state.textBoxes[selectedTbIndex] = activeTb.copy(colorArgb = argb)
                                    state.saveActivePageData()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (swatchColor == Color(0xFFF8FAFC)) LernspaceDark else Color.White)
                                )
                            }
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(18.dp), color = Color(0xFFE2E8F0))

                    // Done button to preview Markdown + LaTeX immediately
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentBlue,
                        modifier = Modifier.clickable {
                            state.textBoxes[selectedTbIndex] = activeTb.copy(isSelected = false)
                            state.saveActivePageData()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selesai",
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp).size(15.dp)
                        )
                    }
                }
            }
        }

        // Lasso Tanya AI (Gemini Vision) Result Dialog
        if (showLassoAiDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isAskingLassoAi) showLassoAiDialog = false
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LernspaceDark)
                        Text("${lassoAiMode.label} (${GeminiApiClient.selectedModelId})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LernspaceDark)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val curSel = state.lassoSelection
                        if (curSel != null && GeminiApiClient.isConfigured()) {
                            // Quick Action Chips inside dialog (monokrom sesuai tema)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LassoAiMode.entries.forEach { mode ->
                                    val isSelectedMode = mode == lassoAiMode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelectedMode) LernspaceDark else LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, if (isSelectedMode) LernspaceDark else LernspaceBorder),
                                        modifier = Modifier.clickable(enabled = !isAskingLassoAi) {
                                            triggerLassoAi(curSel, lassoAiCustomPrompt, mode)
                                        }
                                    ) {
                                        Text(
                                            text = mode.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelectedMode) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelectedMode) LernspaceSurfaceCard else LernspaceTextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = lassoAiCustomPrompt,
                                    onValueChange = { lassoAiCustomPrompt = it },
                                    placeholder = { Text("Tanya spesifik (opsional)...", fontSize = 12.sp) },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 13.sp, color = LernspaceTextPrimary),
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = {
                                        if (!isAskingLassoAi) {
                                            triggerLassoAi(curSel, lassoAiCustomPrompt, lassoAiMode)
                                        }
                                    },
                                    enabled = !isAskingLassoAi,
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = LernspaceDark)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Kirim", tint = LernspaceSurfaceCard, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        when {
                            isAskingLassoAi && lassoAiResponse.isNullOrBlank() -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = LernspaceDark)
                                    Text("Gemini sedang membaca area yang dilingkari...", fontSize = 13.sp, color = LernspaceTextSecondary)
                                }
                            }
                            lassoAiError != null && lassoAiResponse.isNullOrBlank() -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, LernspaceBorder)
                                ) {
                                    Text(
                                        text = lassoAiError ?: "",
                                        fontSize = 13.sp,
                                        color = LernspaceTextPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            !lassoAiResponse.isNullOrBlank() -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 360.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    ) {
                                        MarkdownLatexContent(
                                            markdown = lassoAiResponse ?: "",
                                            baseFontSize = 13.5.sp,
                                            textColor = LernspaceTextPrimary,
                                            accentColor = LernspaceDark,
                                            isDarkSurface = LocalIsDarkTheme.current,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    val reply = lassoAiResponse
                    val sel = state.lassoSelection
                    if (!reply.isNullOrBlank() && sel != null && !isAskingLassoAi) {
                        Button(
                            onClick = {
                                val boxW = 760f
                                val approxLines = reply.lines().sumOf { ((it.length / 44) + 1) }.coerceIn(4, 14)
                                val boxH = (approxLines * 42f + 56f).coerceIn(220f, 480f)
                                val insertX = sel.bounds.left.coerceAtLeast(32f)
                                val insertY = (sel.bounds.bottom + 24f).coerceAtLeast(32f)
                                state.pushUndoSnapshot()
                                state.textBoxes.add(
                                    TextBoxElement(
                                        x = insertX,
                                        y = insertY,
                                        width = boxW,
                                        height = boxH,
                                        text = reply,
                                        isSelected = false,
                                        fontSize = 30f,
                                        hasBackground = true
                                    )
                                )
                                state.saveActivePageData()
                                showLassoAiDialog = false
                                state.lassoSelection = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LernspaceDark,
                                contentColor = LernspaceSurfaceCard
                            )
                        ) {
                            Text("Sisipkan ke Kanvas")
                        }
                    }
                },
                dismissButton = {
                    if (isAskingLassoAi) {
                        TextButton(
                            onClick = {
                                lassoAiJob?.cancel()
                                lassoAiJob = null
                                isAskingLassoAi = false
                                lassoAiError = "Permintaan AI dibatalkan."
                            }
                        ) {
                            Text("Batal", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = { showLassoAiDialog = false }
                        ) {
                            Text("Tutup", color = LernspaceTextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = LernspaceSurfaceCard
            )
        }

        // Recovery Banner if a legacy PDF lost external URI permission
        if (state.canvasType == NoteCanvasType.PDF_DOCUMENT && state.pdfLoadFailed) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "File PDF tidak dapat diakses",
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Izin akses file PDF lama sudah kedaluwarsa atau file dipindahkan. Pilih ulang file PDF agar tersimpan permanen.",
                        color = LernspaceTextSecondary,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { relinkPdfLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Pilih Ulang File PDF", color = Color.White)
                    }
                }
            }
        }

        var isPageSidebarOpen by remember { mutableStateOf(false) }

        // ── 2. Nebo-Style Collapsible Page Sidebar & Floating Handle ──
        if (state.canvasType != NoteCanvasType.INFINITE_SCRATCHPAD) {
            // Floating pill handle on the left edge when sidebar is collapsed
            if (!isPageSidebarOpen) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { isPageSidebarOpen = true }
                        .rejectPalmAfterStylus(shouldRejectFloatingTouch)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                            contentDescription = "Buka Panel Preview Halaman",
                            tint = LernspaceDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${state.currentPageIndex + 1}/${state.totalPages}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }
            }

            // Nebo-Style Page Sidebar Drawer
            NeboPageSidebar(
                isOpen = isPageSidebarOpen,
                noteId = state.note.id,
                currentPageIndex = state.currentPageIndex,
                totalPages = state.totalPages,
                pages = state.note.pages,
                currentStrokes = state.strokes,
                canvasType = state.canvasType,
                paperStyle = state.paperStyle,
                isDarkPaper = state.isDarkPaper,
                pdfUri = state.note.pdfUri,
                canAddPage = state.canvasType == NoteCanvasType.PAGED_NOTE,
                canDeletePage = state.canvasType == NoteCanvasType.PAGED_NOTE && state.totalPages > 1,
                onClose = { isPageSidebarOpen = false },
                onSelectPage = { targetIdx -> state.goToPage(targetIdx) },
                onAddPage = { state.addNewPage() },
                onDuplicatePage = { targetIdx -> state.duplicatePage(targetIdx) },
                onDeletePage = { targetIdx ->
                    state.goToPage(targetIdx)
                    showDeletePageDialog = true
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .padding(top = 114.dp, bottom = 84.dp, start = 12.dp)
                    .zIndex(20f)
                    .rejectPalmAfterStylus(shouldRejectFloatingTouch)
            )
        }

        // ── 3. Floating Page Navigator Bar (Bottom Center of this Pane) ──
        if (state.canvasType != NoteCanvasType.INFINITE_SCRATCHPAD) {
            PageNavigatorBar(
                currentPage = state.currentPageIndex + 1,
                totalPages = state.totalPages,
                pages = state.note.pages,
                canAddPage = state.canvasType == NoteCanvasType.PAGED_NOTE,
                canDeletePage = state.canvasType == NoteCanvasType.PAGED_NOTE && state.totalPages > 1,
                isSidebarOpen = isPageSidebarOpen,
                onToggleSidebar = { isPageSidebarOpen = !isPageSidebarOpen },
                onPrevPage = { state.goToPage(state.currentPageIndex - 1) },
                onNextPage = { state.goToPage(state.currentPageIndex + 1) },
                onAddPage = { state.addNewPage() },
                onDeletePage = { showDeletePageDialog = true },
                onJumpToPage = { targetPage -> state.goToPage(targetPage - 1) },
                onDuplicatePage = { pageIdx -> state.duplicatePage(pageIdx) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .rejectPalmAfterStylus(shouldRejectFloatingTouch)
            )
        }

        // Dialog: Confirm Delete Current Page
        if (showDeletePageDialog) {
            AlertDialog(
                onDismissRequest = { showDeletePageDialog = false },
                title = {
                    Text(
                        text = "Hapus Halaman ${state.currentPageIndex + 1}?",
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                },
                text = {
                    Text(
                        text = "Seluruh coretan dan konten pada halaman ${state.currentPageIndex + 1} akan dihapus secara permanen. Tindakan ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            state.deleteCurrentPage()
                            showDeletePageDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hapus Halaman", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePageDialog = false }) {
                        Text("Batal")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }

        // ── 3. Floating Zoom Percentage Indicator (Bottom-Start or Bottom-End for Left-Handed) ──
        val isLeftHanded = AppSettingsManager.handedness == Handedness.LEFT_HANDED
        ZoomIndicator(
            scaleProvider = { state.scale },
            onReset = {
                state.scale = 1f
                state.panX = 0f
                state.panY = 0f
            },
            modifier = Modifier
                .align(if (isLeftHanded) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(
                    start = if (isLeftHanded) 0.dp else 14.dp,
                    end = if (isLeftHanded) 14.dp else 0.dp,
                    bottom = 16.dp
                )
                .rejectPalmAfterStylus(shouldRejectFloatingTouch)
        )
    }
}

private fun selBounds(sel: LassoSelectionState): Rect = sel.bounds

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Draggable Middle Divider (Double-Tap to Swap Notes!)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SplitScreenDivider(
    onDrag: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        } catch (e: Exception) {}
                        onDoubleTap()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Vertical Divider Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (isDragging) 3.dp else 2.dp)
                .background(if (isDragging) AccentBlue else Color(0xFFCBD5E1))
        )

        // Center Tactile Handle
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDragging) AccentBlue else Color.White,
            shadowElevation = if (isDragging) 6.dp else 3.dp,
            border = BorderStroke(1.dp, if (isDragging) AccentBlue else Color(0xFFCBD5E1)),
            modifier = Modifier.size(width = 16.dp, height = 54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = "Geser Pembagi (Ketuk 2x untuk Menukar Posisi Catatan)",
                    tint = if (isDragging) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Single Unified Floating Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UnifiedNoteTopBar(
    isSplitMode: Boolean,
    activeNoteId: String,
    leftNoteId: String,
    leftTitle: String,
    onLeftTitleChange: (String) -> Unit,
    leftSubject: String,
    leftCanvasType: NoteCanvasType,
    leftPaperStyle: PaperStyle,
    leftCurrentPage: Int,
    leftTotalPages: Int,
    onLeftChangePaperStyle: (PaperStyle) -> Unit = {},
    onLeftEditLink: () -> Unit = {},
    rightNoteId: String?,
    rightTitle: String?,
    rightSubject: String?,
    rightCurrentPage: Int,
    rightTotalPages: Int,
    onSelectActiveNote: (String) -> Unit,
    onToggleSplitMode: () -> Unit,
    onCloseSplit: () -> Unit,
    activeTool: NoteTool,
    onSelectTool: (NoteTool) -> Unit,
    selectedShapeType: GeometricShapeType = GeometricShapeType.RECTANGLE,
    onSelectShapeType: (GeometricShapeType) -> Unit = {},
    penColor: Color,
    onSelectPenColor: (Color) -> Unit,
    penThickness: Float,
    onSelectPenThickness: (Float) -> Unit,
    highlighterColor: Color,
    onSelectHighlighterColor: (Color) -> Unit,
    highlighterThickness: Float,
    onSelectHighlighterThickness: (Float) -> Unit,
    onPickImage: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    onExportPdf: () -> Unit,
    isExporting: Boolean,
    isDarkPaper: Boolean = false,
    onToggleDarkPaper: () -> Unit = {},
    hasAudioRecording: Boolean = false,
    isAudioBarOpen: Boolean = false,
    onToggleAudioBar: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        val isLeftHanded = AppSettingsManager.handedness == Handedness.LEFT_HANDED

        if (!isLeftHanded) {
            NoteTitleCapsule(
                isSplitMode = isSplitMode,
                activeNoteId = activeNoteId,
                leftNoteId = leftNoteId,
                leftTitle = leftTitle,
                onLeftTitleChange = onLeftTitleChange,
                leftSubject = leftSubject,
                leftCanvasType = leftCanvasType,
                leftPaperStyle = leftPaperStyle,
                leftCurrentPage = leftCurrentPage,
                leftTotalPages = leftTotalPages,
                onLeftChangePaperStyle = onLeftChangePaperStyle,
                onLeftEditLink = onLeftEditLink,
                rightNoteId = rightNoteId,
                rightTitle = rightTitle,
                onSelectActiveNote = onSelectActiveNote,
                onToggleSplitMode = onToggleSplitMode,
                onBack = onBack
            )
        } else {
            NoteActionsCapsule(
                isSplitMode = isSplitMode,
                onCloseSplit = onCloseSplit,
                isExporting = isExporting,
                onExportPdf = onExportPdf,
                isDarkPaper = isDarkPaper,
                onToggleDarkPaper = onToggleDarkPaper,
                canUndo = canUndo,
                onUndo = onUndo,
                canRedo = canRedo,
                onRedo = onRedo,
                onClearAll = onClearAll,
                hasAudioRecording = hasAudioRecording,
                isAudioBarOpen = isAudioBarOpen,
                onToggleAudioBar = onToggleAudioBar
            )
        }

        // ── 2. CENTER: Single Unified NoteFloatingToolbar ──
        NoteFloatingToolbar(
            activeTool = activeTool,
            onSelectTool = onSelectTool,
            selectedShapeType = selectedShapeType,
            onSelectShapeType = onSelectShapeType,
            penColor = penColor,
            onSelectPenColor = onSelectPenColor,
            penThickness = penThickness,
            onSelectPenThickness = onSelectPenThickness,
            highlighterColor = highlighterColor,
            onSelectHighlighterColor = onSelectHighlighterColor,
            highlighterThickness = highlighterThickness,
            onSelectHighlighterThickness = onSelectHighlighterThickness,
            eraserRadius = AppSettingsManager.eraserRadius,
            onSelectEraserRadius = { AppSettingsManager.updateEraserRadius(it) },
            eraserMode = AppSettingsManager.eraserMode,
            onSelectEraserMode = { AppSettingsManager.updateEraserMode(it) },
            onPickImage = onPickImage
        )

        if (!isLeftHanded) {
            NoteActionsCapsule(
                isSplitMode = isSplitMode,
                onCloseSplit = onCloseSplit,
                isExporting = isExporting,
                onExportPdf = onExportPdf,
                isDarkPaper = isDarkPaper,
                onToggleDarkPaper = onToggleDarkPaper,
                canUndo = canUndo,
                onUndo = onUndo,
                canRedo = canRedo,
                onRedo = onRedo,
                onClearAll = onClearAll,
                hasAudioRecording = hasAudioRecording,
                isAudioBarOpen = isAudioBarOpen,
                onToggleAudioBar = onToggleAudioBar
            )
        } else {
            NoteTitleCapsule(
                isSplitMode = isSplitMode,
                activeNoteId = activeNoteId,
                leftNoteId = leftNoteId,
                leftTitle = leftTitle,
                onLeftTitleChange = onLeftTitleChange,
                leftSubject = leftSubject,
                leftCanvasType = leftCanvasType,
                leftPaperStyle = leftPaperStyle,
                leftCurrentPage = leftCurrentPage,
                leftTotalPages = leftTotalPages,
                onLeftChangePaperStyle = onLeftChangePaperStyle,
                onLeftEditLink = onLeftEditLink,
                rightNoteId = rightNoteId,
                rightTitle = rightTitle,
                onSelectActiveNote = onSelectActiveNote,
                onToggleSplitMode = onToggleSplitMode,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun NoteTitleCapsule(
    isSplitMode: Boolean,
    activeNoteId: String,
    leftNoteId: String,
    leftTitle: String,
    onLeftTitleChange: (String) -> Unit,
    leftSubject: String,
    leftCanvasType: NoteCanvasType,
    leftPaperStyle: PaperStyle,
    leftCurrentPage: Int,
    leftTotalPages: Int,
    onLeftChangePaperStyle: (PaperStyle) -> Unit,
    onLeftEditLink: () -> Unit,
    rightNoteId: String?,
    rightTitle: String?,
    onSelectActiveNote: (String) -> Unit,
    onToggleSplitMode: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = LernspaceSurfaceCard.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, LernspaceBorder),
        modifier = Modifier.widthIn(min = 180.dp, max = 340.dp)
    ) {
        if (!isSplitMode) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = LernspaceSurfaceLow,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onBack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LernspaceDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 4.dp)
                ) {
                    BasicTextField(
                        value = leftTitle,
                        onValueChange = onLeftTitleChange,
                        textStyle = TextStyle(
                            color = LernspaceDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(LernspaceDark),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (leftCanvasType == NoteCanvasType.INFINITE_SCRATCHPAD) {
                        var showPaperMenu by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.clickable { showPaperMenu = true }
                            ) {
                                Text(
                                    text = "$leftSubject · ${leftPaperStyle.displayName}",
                                    fontSize = 11.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Ganti Kertas",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showPaperMenu,
                                onDismissRequest = { showPaperMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tautkan ke Silabus / Edit Info", fontWeight = FontWeight.SemiBold, color = AccentBlue) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, null, tint = AccentBlue, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showPaperMenu = false
                                        onLeftEditLink()
                                    }
                                )
                                HorizontalDivider()
                                PaperStyle.values().forEach { style ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = style.displayName,
                                                fontWeight = if (leftPaperStyle == style) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onLeftChangePaperStyle(style)
                                            showPaperMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onLeftEditLink() }
                        ) {
                            Text(
                                text = when (leftCanvasType) {
                                    NoteCanvasType.PDF_DOCUMENT -> "$leftSubject · PDF Document (Hal $leftCurrentPage/$leftTotalPages)"
                                    NoteCanvasType.PAGED_NOTE -> "$leftSubject · ${leftPaperStyle.displayName} (Hal $leftCurrentPage/$leftTotalPages)"
                                    NoteCanvasType.INFINITE_SCRATCHPAD -> "$leftSubject · Scratchpad Bebas"
                                },
                                fontSize = 11.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Tautkan ke Silabus",
                                tint = AccentBlue,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Split Screen Toggle Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentBlueContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable(onClick = onToggleSplitMode)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerticalSplit,
                            contentDescription = "Buka Catatan Samping",
                            tint = AccentBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Split",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }
            }
        } else {
            // SPLIT SCREEN DUAL TABS
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = LernspaceSurfaceLow,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onBack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LernspaceDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Tab Left Note
                val isLeftActive = activeNoteId == leftNoteId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLeftActive) AccentBlue.copy(alpha = 0.12f) else LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, if (isLeftActive) AccentBlue else LernspaceBorder),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onSelectActiveNote(leftNoteId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (isLeftActive) AccentBlue else Color(0xFF94A3B8), CircleShape)
                        )
                        Text(
                            text = leftTitle,
                            fontSize = 11.sp,
                            fontWeight = if (isLeftActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLeftActive) AccentBlue else LernspaceDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tab Right Note
                if (rightNoteId != null) {
                    val isRightActive = activeNoteId == rightNoteId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRightActive) AccentBlue.copy(alpha = 0.12f) else LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, if (isRightActive) AccentBlue else LernspaceBorder),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onSelectActiveNote(rightNoteId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (isRightActive) AccentBlue else Color(0xFF94A3B8), CircleShape)
                            )
                            Text(
                                text = rightTitle ?: "Catatan 2",
                                fontSize = 11.sp,
                                fontWeight = if (isRightActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRightActive) AccentBlue else LernspaceDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteActionsCapsule(
    isSplitMode: Boolean,
    onCloseSplit: () -> Unit,
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    isDarkPaper: Boolean,
    onToggleDarkPaper: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    hasAudioRecording: Boolean = false,
    isAudioBarOpen: Boolean = false,
    onToggleAudioBar: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = LernspaceSurfaceCard.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, LernspaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSplitMode) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onCloseSplit)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup Split Screen",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                VerticalDivider(modifier = Modifier.height(20.dp), color = LernspaceBorder)
            }

            // Audio Lecture Recording Button
            Surface(
                shape = CircleShape,
                color = when {
                    isAudioBarOpen -> AccentRed.copy(alpha = 0.18f)
                    hasAudioRecording -> AccentBlueContainer.copy(alpha = 0.7f)
                    else -> LernspaceSurfaceLow
                },
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onToggleAudioBar)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (hasAudioRecording) Icons.Filled.Headphones else Icons.Filled.Mic,
                        contentDescription = "Audio Kuliah / Rekaman Suara",
                        tint = when {
                            isAudioBarOpen -> AccentRed
                            hasAudioRecording -> AccentBlue
                            else -> LernspaceDark
                        },
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Export PDF Button
            Surface(
                shape = CircleShape,
                color = AccentBlueContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = !isExporting, onClick = onExportPdf)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentBlue)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Export PDF",
                            tint = AccentBlue,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Dark Paper Toggle Button
            Surface(
                shape = CircleShape,
                color = if (isDarkPaper) Color(0xFF1E293B) else LernspaceSurfaceLow,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onToggleDarkPaper)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDarkPaper) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = if (isDarkPaper) "Mode Kertas Gelap (Aktif)" else "Mode Kertas Terang",
                        tint = if (isDarkPaper) Color(0xFFFBBF24) else LernspaceTextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Undo
            Surface(
                shape = CircleShape,
                color = if (canUndo) LernspaceSurfaceLow else Color.Transparent,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = canUndo, onClick = onUndo)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) LernspaceDark else LernspaceBorder,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Redo
            Surface(
                shape = CircleShape,
                color = if (canRedo) LernspaceSurfaceLow else Color.Transparent,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = canRedo, onClick = onRedo)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) LernspaceDark else LernspaceBorder,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Clear Canvas
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onClearAll)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/**
 * Blocks accidental finger/palm touches on floating canvas HUD bars while the stylus
 * is hovering, active, or within the post-lift palm-rejection cooldown window.
 * Explicit Stylus/Eraser taps are never blocked.
 */
private fun Modifier.rejectPalmAfterStylus(
    shouldRejectTouch: () -> Boolean
): Modifier = this.pointerInput(shouldRejectTouch) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val hasStylus = event.changes.any {
                it.type == PointerType.Stylus || it.type == PointerType.Eraser
            }
            if (hasStylus) continue

            val multiTouch = event.changes.count { it.type == PointerType.Touch && it.pressed } > 1
            if (shouldRejectTouch() || multiTouch) {
                var swallowed = false
                for (change in event.changes) {
                    if (change.type == PointerType.Touch) {
                        change.consume()
                        swallowed = true
                    }
                }
                if (swallowed && event.changes.any { it.type == PointerType.Touch && it.pressed }) {
                    while (true) {
                        val next = awaitPointerEvent(pass = PointerEventPass.Initial)
                        next.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                        if (next.changes.none { it.type == PointerType.Touch && it.pressed }) break
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Nebo-Style Collapsible Page Sidebar & Live Thumbnails (Pure 2D Hairline Engine)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * High-performance in-memory cache for page thumbnail bitmaps.
 * Keeps rendered miniature pages in memory so drawers scroll at full 60/120 FPS.
 */
private object NeboThumbnailCache {
    private val cache = android.util.LruCache<String, ImageBitmap>(32)

    private fun buildKey(
        noteId: String,
        pageIndex: Int,
        strokeCount: Int,
        isDarkPaper: Boolean,
        paperStyle: PaperStyle,
        canvasType: NoteCanvasType,
        pdfUri: String?
    ): String = "$noteId#$pageIndex#$strokeCount#$isDarkPaper#${paperStyle.name}#${canvasType.name}#$pdfUri"

    fun get(
        noteId: String,
        pageIndex: Int,
        strokeCount: Int,
        isDarkPaper: Boolean,
        paperStyle: PaperStyle,
        canvasType: NoteCanvasType,
        pdfUri: String?
    ): ImageBitmap? = cache.get(buildKey(noteId, pageIndex, strokeCount, isDarkPaper, paperStyle, canvasType, pdfUri))

    fun put(
        noteId: String,
        pageIndex: Int,
        strokeCount: Int,
        isDarkPaper: Boolean,
        paperStyle: PaperStyle,
        canvasType: NoteCanvasType,
        pdfUri: String?,
        bitmap: ImageBitmap
    ) {
        cache.put(buildKey(noteId, pageIndex, strokeCount, isDarkPaper, paperStyle, canvasType, pdfUri), bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
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
 * Renders a crisp, proportional miniature bitmap of a note page in 2-3ms.
 * Uses pure Android 2D Canvas + Path (zero C++ JNI overhead, thread-safe, no crash).
 */
private fun renderPageThumbnailBitmap(
    context: android.content.Context,
    noteId: String,
    pageIndex: Int,
    pageData: NotePageData?,
    canvasType: NoteCanvasType,
    paperStyle: PaperStyle,
    isDarkPaper: Boolean,
    pdfUri: String?,
    targetWidth: Int = 164,
    targetHeight: Int = 232
): ImageBitmap {
    val strokeCount = pageData?.strokes?.size ?: 0
    val cached = NeboThumbnailCache.get(
        noteId, pageIndex, strokeCount, isDarkPaper, paperStyle, canvasType, pdfUri
    )
    if (cached != null) return cached

    val bitmap = android.graphics.Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val scale = targetWidth.toFloat() / CanvasConstants.PAGE_WIDTH
    val bgColor = if (isDarkPaper) android.graphics.Color.rgb(15, 23, 42) else android.graphics.Color.WHITE
    canvas.drawColor(bgColor)

    // 1. PDF Page Background
    if (canvasType == NoteCanvasType.PDF_DOCUMENT && !pdfUri.isNullOrBlank()) {
        try {
            val pdfBmp = PdfNoteHelper.renderPdfPage(context, Uri.parse(pdfUri), pageIndex, targetWidth = targetWidth)
            if (pdfBmp != null) {
                val destRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
                canvas.drawBitmap(pdfBmp, null, destRect, null)
            }
        } catch (_: Exception) {}
    } else {
        // 2. Paper Template Guidelines
        val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkPaper) android.graphics.Color.argb(0x33, 0x47, 0x55, 0x69) else android.graphics.Color.argb(0xFF, 0xE2, 0xE8, 0xF0)
            strokeWidth = 0.8f
        }
        val marginPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkPaper) android.graphics.Color.argb(0x66, 0xF8, 0x71, 0x71) else android.graphics.Color.argb(0x80, 0xFC, 0xA5, 0xA5)
            strokeWidth = 1f
        }

        when (paperStyle) {
            PaperStyle.RULED -> {
                val marginX = CanvasConstants.RULED_MARGIN_X * scale
                canvas.drawLine(marginX, 0f, marginX, targetHeight.toFloat(), marginPaint)
                val spacing = CanvasConstants.RULED_LINE_SPACING * scale
                var y = CanvasConstants.RULED_FIRST_LINE_Y * scale
                while (y < targetHeight - 2f) {
                    canvas.drawLine(0f, y, targetWidth.toFloat(), y, linePaint)
                    y += spacing
                }
            }
            PaperStyle.GRID -> {
                val gridSpacing = CanvasConstants.GRID_SPACING * scale
                var x = 0f
                while (x < targetWidth) {
                    canvas.drawLine(x, 0f, x, targetHeight.toFloat(), linePaint)
                    x += gridSpacing
                }
                var y = 0f
                while (y < targetHeight) {
                    canvas.drawLine(0f, y, targetWidth.toFloat(), y, linePaint)
                    y += gridSpacing
                }
            }
            PaperStyle.DOTTED -> {
                val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = linePaint.color
                    style = android.graphics.Paint.Style.FILL
                }
                val dotSpacing = CanvasConstants.DOT_SPACING * scale
                var x = dotSpacing
                while (x < targetWidth) {
                    var y = dotSpacing
                    while (y < targetHeight) {
                        canvas.drawCircle(x, y, 0.6f, dotPaint)
                        y += dotSpacing
                    }
                    x += dotSpacing
                }
            }
            PaperStyle.BLANK -> {}
        }
    }

    // 3. Hairline Vector Strokes: Perfectly scaled, crystal clear, non-bloated
    val strokes = pageData?.strokes
    if (!strokes.isNullOrEmpty()) {
        val reusableInput = StrokeInput()
        val path = android.graphics.Path()

        val penPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = 0.9f // Sharp, clean hairline
        }

        val highPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = 2.2f // Subtle highlighter stroke
        }

        // Pass 1: Highlighters under ink
        for (stroke in strokes) {
            if (stroke.brush.family == StockBrushes.highlighter()) {
                val count = stroke.inputs.size
                if (count > 0) {
                    path.reset()
                    stroke.inputs.populate(0, reusableInput)
                    path.moveTo(reusableInput.x * scale, reusableInput.y * scale)
                    for (i in 1 until count) {
                        stroke.inputs.populate(i, reusableInput)
                        path.lineTo(reusableInput.x * scale, reusableInput.y * scale)
                    }
                    val originalColor = stroke.brush.colorIntArgb
                    val alpha = (android.graphics.Color.alpha(originalColor) * 0.40f).toInt().coerceIn(30, 140)
                    highPaint.color = originalColor
                    highPaint.alpha = alpha
                    canvas.drawPath(path, highPaint)
                }
            }
        }

        // Pass 2: Regular ink pens on top
        for (stroke in strokes) {
            if (stroke.brush.family != StockBrushes.highlighter()) {
                val count = stroke.inputs.size
                if (count > 0) {
                    path.reset()
                    stroke.inputs.populate(0, reusableInput)
                    path.moveTo(reusableInput.x * scale, reusableInput.y * scale)
                    for (i in 1 until count) {
                        stroke.inputs.populate(i, reusableInput)
                        path.lineTo(reusableInput.x * scale, reusableInput.y * scale)
                    }
                    val color = stroke.brush.colorIntArgb
                    val finalColor = if (isDarkPaper && isDarkInkColor(color)) {
                        android.graphics.Color.rgb(248, 250, 252) // off-white
                    } else {
                        color
                    }
                    penPaint.color = finalColor
                    canvas.drawPath(path, penPaint)
                }
            }
        }
    }

    val imageBitmap = bitmap.asImageBitmap()
    NeboThumbnailCache.put(
        noteId, pageIndex, strokeCount, isDarkPaper, paperStyle, canvasType, pdfUri, imageBitmap
    )
    return imageBitmap
}

@Composable
private fun NeboPageThumbnailCard(
    noteId: String,
    pageIndex: Int,
    pageData: NotePageData?,
    isCurrent: Boolean,
    canvasType: NoteCanvasType,
    paperStyle: PaperStyle,
    isDarkPaper: Boolean,
    pdfUri: String?,
    onSelect: () -> Unit,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOptionsMenu by remember { mutableStateOf(false) }

    val strokeCount = pageData?.strokes?.size ?: 0
    val thumbnailBitmap = remember(
        noteId, pageIndex, strokeCount, isDarkPaper, paperStyle, canvasType, pdfUri
    ) {
        renderPageThumbnailBitmap(
            context = context,
            noteId = noteId,
            pageIndex = pageIndex,
            pageData = pageData,
            canvasType = canvasType,
            paperStyle = paperStyle,
            isDarkPaper = isDarkPaper,
            pdfUri = pdfUri
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Thumbnail Sheet Container (Proportional Mini A4 Paper)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDarkPaper) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) AccentBlue else Color(0xFFE2E8F0)
            ),
            shadowElevation = if (isCurrent) 4.dp else 1.dp,
            modifier = Modifier
                .width(82.dp)
                .height(116.dp)
                .clickable(onClick = onSelect)
        ) {
            Image(
                bitmap = thumbnailBitmap,
                contentDescription = "Halaman ${pageIndex + 1}",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Page Indicator & Action Menu
        Row(
            modifier = Modifier
                .width(82.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pageIndex + 1}",
                fontSize = 11.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) AccentBlue else LernspaceDark
            )

            if (onDuplicate != null || onDelete != null) {
                Box {
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Opsi Halaman",
                            tint = LernspaceTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        if (onDuplicate != null) {
                            DropdownMenuItem(
                                text = { Text("Duplikasi", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onDuplicate()
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Hapus Halaman", fontSize = 12.sp, color = Color(0xFFEF4444)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeboPageSidebar(
    isOpen: Boolean,
    noteId: String,
    currentPageIndex: Int,
    totalPages: Int,
    pages: List<NotePageData>,
    currentStrokes: List<Stroke>,
    canvasType: NoteCanvasType,
    paperStyle: PaperStyle,
    isDarkPaper: Boolean,
    pdfUri: String?,
    canAddPage: Boolean,
    canDeletePage: Boolean,
    onClose: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDuplicatePage: ((Int) -> Unit)?,
    onDeletePage: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.98f),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier
                .width(114.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Tutup & Tambah)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup Sidebar",
                            tint = LernspaceDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$totalPages Hal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceTextSecondary
                    )

                    if (canAddPage) {
                        IconButton(
                            onClick = onAddPage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Tambah Halaman",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(28.dp))
                    }
                }

                HorizontalDivider(
                    color = LernspaceBorder.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                // Scrollable List of Mini A4 Thumbnails
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(totalPages) { idx ->
                        val pageData = if (idx == currentPageIndex) {
                            NotePageData(
                                pageIndex = idx,
                                strokes = ArrayList(currentStrokes)
                            )
                        } else if (idx < pages.size) {
                            pages[idx]
                        } else null

                        NeboPageThumbnailCard(
                            noteId = noteId,
                            pageIndex = idx,
                            pageData = pageData,
                            isCurrent = idx == currentPageIndex,
                            canvasType = canvasType,
                            paperStyle = paperStyle,
                            isDarkPaper = isDarkPaper,
                            pdfUri = pdfUri,
                            onSelect = { onSelectPage(idx) },
                            onDuplicate = if (canAddPage && onDuplicatePage != null) {
                                { onDuplicatePage(idx) }
                            } else null,
                            onDelete = if (canDeletePage && onDeletePage != null) {
                                { onDeletePage(idx) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Floating Page Navigator Bar (Bottom Center)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PageNavigatorBar(
    currentPage: Int,
    totalPages: Int,
    pages: List<NotePageData> = emptyList(),
    canAddPage: Boolean,
    canDeletePage: Boolean,
    isSidebarOpen: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: () -> Unit,
    onJumpToPage: (pageIndex: Int) -> Unit = {},
    onDuplicatePage: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showJumpDialog by remember { mutableStateOf(false) }

    if (showJumpDialog) {
        JumpToPageDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            onDismiss = { showJumpDialog = false },
            onJump = { targetPage ->
                onJumpToPage(targetPage)
                showJumpDialog = false
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, LernspaceBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Toggle Nebo Page Preview Sidebar Button
            Surface(
                shape = CircleShape,
                color = if (isSidebarOpen) AccentBlue else LernspaceSurfaceLow,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onToggleSidebar)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                        contentDescription = "Buka/Tutup Preview Halaman (Nebo)",
                        tint = if (isSidebarOpen) Color.White else LernspaceDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = LernspaceBorder
            )

            // Previous Page Button
            Surface(
                shape = CircleShape,
                color = if (currentPage > 1) LernspaceSurfaceLow else Color.Transparent,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(enabled = currentPage > 1, onClick = onPrevPage)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Halaman Sebelumnya",
                        tint = if (currentPage > 1) LernspaceDark else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Clickable Page Indicator Label (Jump to Page Dialog trigger)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LernspaceSurfaceLow,
                border = BorderStroke(1.dp, LernspaceBorder.copy(alpha = 0.6f)),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable { showJumpDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Halaman $currentPage / $totalPages",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Icon(
                        imageVector = Icons.Filled.UnfoldMore,
                        contentDescription = "Lompat Halaman",
                        tint = LernspaceTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Next Page Button
            Surface(
                shape = CircleShape,
                color = if (currentPage < totalPages) LernspaceSurfaceLow else Color.Transparent,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(enabled = currentPage < totalPages, onClick = onNextPage)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Halaman Berikutnya",
                        tint = if (currentPage < totalPages) LernspaceDark else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Add Page Button (for Paged Notes)
            if (canAddPage) {
                VerticalDivider(
                    modifier = Modifier.height(18.dp),
                    color = LernspaceBorder
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceDark,
                    modifier = Modifier.clickable(onClick = onAddPage)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Tambah Halaman",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Tambah Hal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Delete Page Button (for Paged Notes when totalPages > 1)
            if (canDeletePage) {
                VerticalDivider(
                    modifier = Modifier.height(18.dp),
                    color = LernspaceBorder
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.clickable(onClick = onDeletePage)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Hapus Halaman",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Hapus Hal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Jump To Page (Lompat ke Halaman Cepat)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun JumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJump: (targetPage: Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentPage.toString()) }
    var sliderValue by remember { mutableFloatStateOf(currentPage.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier
                .width(380.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ImportContacts,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Lompat ke Halaman",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = "Halaman saat ini: $currentPage dari $totalPages",
                                fontSize = 11.sp,
                                color = LernspaceTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp), tint = LernspaceTextSecondary)
                    }
                }

                // Number Input and Slider Jump
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            pageInput = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null && parsed in 1..totalPages) {
                                sliderValue = parsed.toFloat()
                            }
                        },
                        label = { Text("Nomor Halaman") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = LernspaceDark
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                val p = pageInput.toIntOrNull()
                                if (p != null && p in 1..totalPages) {
                                    onJump(p)
                                }
                            }
                        ),
                        modifier = Modifier.width(160.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Slider if totalPages > 3
                    if (totalPages > 3) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Slider(
                                value = sliderValue,
                                onValueChange = {
                                    sliderValue = it
                                    pageInput = kotlin.math.round(it).toInt().toString()
                                },
                                valueRange = 1f..totalPages.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentBlue,
                                    activeTrackColor = AccentBlue,
                                    inactiveTrackColor = LernspaceBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hal 1", fontSize = 10.sp, color = LernspaceTextSecondary)
                                Text("Hal $totalPages", fontSize = 10.sp, color = LernspaceTextSecondary)
                            }
                        }
                    }

                    // Quick Target Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    pageInput = "1"
                                    sliderValue = 1f
                                }
                        ) {
                            Text(
                                text = "Awal (1)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        if (totalPages > 4) {
                            val mid = totalPages / 2
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, LernspaceBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        pageInput = mid.toString()
                                        sliderValue = mid.toFloat()
                                    }
                            ) {
                                Text(
                                    text = "Tengah ($mid)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    pageInput = totalPages.toString()
                                    sliderValue = totalPages.toFloat()
                                }
                        ) {
                            Text(
                                text = "Akhir ($totalPages)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        val targetP = pageInput.toIntOrNull()
                        val isValid = targetP != null && targetP in 1..totalPages
                        Button(
                            onClick = {
                                if (isValid) {
                                    onJump(targetP!!)
                                }
                            },
                            enabled = isValid,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                        ) {
                            Text("Lompat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Export PDF Success (Share & Save to Downloads)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExportPdfSuccessDialog(
    file: File,
    totalPages: Int,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSaveToDownloads: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Dokumen PDF Berhasil Dibuat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "${file.name} • $totalPages Halaman",
                        style = MaterialTheme.typography.bodySmall,
                        color = LernspaceTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    val fileSizeKb = file.length() / 1024f
                    val sizeFormatted = if (fileSizeKb >= 1024f) {
                        String.format(java.util.Locale.getDefault(), "%.2f MB", fileSizeKb / 1024f)
                    } else {
                        String.format(java.util.Locale.getDefault(), "%.0f KB", fileSizeKb)
                    }
                    Text(
                        text = "Ukuran File: $sizeFormatted (Vektor & Gambar HD)",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Share button
                    Button(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LernspaceDark,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Bagikan Dokumen (Share)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // 2. Save to Downloads button
                    OutlinedButton(
                        onClick = onSaveToDownloads,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, AccentBlue),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentBlue
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Simpan ke Folder Downloads", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // 3. Close button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup", color = LernspaceTextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Zoom Percentage Indicator (Bottom-Start)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ZoomIndicator(
    scaleProvider: () -> Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent by remember(scaleProvider) {
        derivedStateOf { (scaleProvider() * 100).roundToInt() }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, LernspaceBorder),
        modifier = modifier.clickable(onClick = onReset)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ZoomIn,
                contentDescription = "Zoom",
                tint = LernspaceDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$percent%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Pick an existing note or create scratchpad for Split Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SplitNotePickerDialog(
    currentNoteId: String,
    availableNotes: List<NoteUiModel>,
    onSelectNote: (NoteUiModel) -> Unit,
    onCreateScratchpad: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(searchQuery, availableNotes, currentNoteId) {
        availableNotes.filter { note ->
            note.id != currentNoteId && (
                searchQuery.isEmpty() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.subject.contains(searchQuery, ignoreCase = true)
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerticalSplit,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Buka Catatan Samping",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = LernspaceDark
                    )
                }
                Text(
                    text = "Pilih catatan lain untuk dipelajari berdampingan (Split Screen)",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari catatan atau mata kuliah...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick action: New Scratchpad
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateScratchpad)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentBlue,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Buka Scratchpad Kosong Baru",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                            Text(
                                text = "Coret-coret rumus bebas atau konsep cepat",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Notes list
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Tidak ada catatan yang cocok dengan \"$searchQuery\""
                            else "Belum ada catatan lain. Buat scratchpad baru di atas!",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectNote(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (item.canvasType) {
                                                    NoteCanvasType.PDF_DOCUMENT -> Icons.Filled.PictureAsPdf
                                                    NoteCanvasType.INFINITE_SCRATCHPAD -> Icons.Filled.Draw
                                                    NoteCanvasType.PAGED_NOTE -> Icons.Filled.Description
                                                },
                                                contentDescription = null,
                                                tint = LernspaceDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.subject} · ${when (item.canvasType) {
                                                NoteCanvasType.PDF_DOCUMENT -> "PDF"
                                                NoteCanvasType.INFINITE_SCRATCHPAD -> "Scratchpad"
                                                NoteCanvasType.PAGED_NOTE -> item.paperStyle.displayName
                                            }}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
