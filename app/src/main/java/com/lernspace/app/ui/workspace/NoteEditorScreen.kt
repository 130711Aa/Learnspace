package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
import java.util.UUID
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
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
import com.lernspace.app.data.local.AcademicDatabaseManager
import com.lernspace.app.data.local.NoteStorageManager
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.components.DrawingSurface
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.DraggableImage
import com.lernspace.app.ui.workspace.components.DraggableTextBox
import com.lernspace.app.ui.workspace.components.NoteFloatingToolbar
import com.lernspace.app.ui.workspace.components.PaperBackground
import com.lernspace.app.ui.workspace.lasso.LassoHelper
import com.lernspace.app.ui.workspace.lasso.LassoSelectionState
import com.lernspace.app.ui.workspace.model.*
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.EraserMode
import com.lernspace.app.data.local.Handedness
import com.lernspace.app.data.local.TouchInputMode
import androidx.ink.strokes.MutableStrokeInputBatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val subject: String get() = note.subject

    var currentPageIndex by mutableIntStateOf(0)
    var totalPages by mutableIntStateOf(
        if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) maxOf(1, note.pdfTotalPages)
        else maxOf(1, note.pages.size)
    )

    val strokes = mutableStateListOf<Stroke>()
    val undoneStrokes = mutableStateListOf<Stroke>()
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()

    val textBoxes = mutableStateListOf<TextBoxElement>()
    val imageElements = mutableStateListOf<ImageElement>()

    var pdfPageBitmap by mutableStateOf<ImageBitmap?>(null)

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

    fun saveActivePageData() {
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
        AcademicDatabaseManager.persistNote(note)
    }

    fun loadActivePageData(index: Int) {
        strokes.clear()
        undoneStrokes.clear()
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

    fun loadFromDisk() {
        val diskPages = AcademicDatabaseManager.loadPagesForNote(note.id)
        if (diskPages != null && diskPages.isNotEmpty()) {
            note.pages.clear()
            note.pages.addAll(diskPages)
            totalPages = if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) maxOf(1, note.pdfTotalPages) else maxOf(1, note.pages.size)
        }
        loadActivePageData(0)
    }

    fun loadPdfPage() {
        if (note.canvasType == NoteCanvasType.PDF_DOCUMENT && note.pdfUri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val bmp = PdfNoteHelper.renderPdfPage(context, Uri.parse(note.pdfUri), currentPageIndex, targetWidth = 1800)
                if (bmp != null) {
                    withContext(Dispatchers.Main) {
                        pdfPageBitmap = bmp.asImageBitmap()
                    }
                }
            }
        }
    }

    fun goToPage(targetIndex: Int) {
        if (targetIndex != currentPageIndex && targetIndex in 0 until totalPages) {
            saveActivePageData()
            currentPageIndex = targetIndex
            loadActivePageData(targetIndex)
            loadPdfPage()
        }
    }

    fun addNewPage() {
        saveActivePageData()
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
        if (strokes.isNotEmpty()) {
            val removed = strokes.removeLast()
            undoneStrokes.add(removed)
            saveActivePageData()
        }
    }

    fun redo() {
        if (undoneStrokes.isNotEmpty()) {
            val restored = undoneStrokes.removeLast()
            strokes.add(restored)
            saveActivePageData()
        }
    }

    fun clearAll() {
        strokes.clear()
        undoneStrokes.clear()
        textBoxes.clear()
        imageElements.clear()
        saveActivePageData()
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

    fun eraseNear(
        touchX: Float,
        touchY: Float,
        radiusPx: Float = AppSettingsManager.eraserRadius,
        mode: EraserMode = AppSettingsManager.eraserMode
    ) {
        val effectiveRadius = radiusPx / scale
        val radiusSquared = effectiveRadius * effectiveRadius

        if (mode == EraserMode.STROKE) {
            strokes.removeAll { stroke ->
                val inputs = stroke.inputs
                val count = inputs.size
                if (count == 0) return@removeAll false
                if (count == 1) {
                    val pt = inputs.get(0)
                    val dx = pt.x - touchX
                    val dy = pt.y - touchY
                    return@removeAll (dx * dx + dy * dy) <= radiusSquared
                }
                for (i in 0 until count - 1) {
                    val p1 = inputs.get(i)
                    val p2 = inputs.get(i + 1)
                    val d2 = distToSegmentSquared(touchX, touchY, p1.x, p1.y, p2.x, p2.y)
                    if (d2 <= radiusSquared) {
                        return@removeAll true
                    }
                }
                false
            }
        } else {
            // ── Pixel / Segment Erase: cut out points inside radius ──
            var modifiedAny = false
            val newStrokesList = ArrayList<Stroke>(strokes.size)

            for (stroke in strokes) {
                val inputs = stroke.inputs
                val count = inputs.size
                if (count == 0) continue

                if (count == 1) {
                    val pt = inputs.get(0)
                    val dx = pt.x - touchX
                    val dy = pt.y - touchY
                    if ((dx * dx + dy * dy) <= radiusSquared) {
                        modifiedAny = true
                    } else {
                        newStrokesList.add(stroke)
                    }
                    continue
                }

                // Check if any point or segment is in radius
                var hit = false
                for (i in 0 until count) {
                    val pt = inputs.get(i)
                    val dx = pt.x - touchX
                    val dy = pt.y - touchY
                    if ((dx * dx + dy * dy) <= radiusSquared) {
                        hit = true
                        break
                    }
                }
                if (!hit) {
                    for (i in 0 until count - 1) {
                        val p1 = inputs.get(i)
                        val p2 = inputs.get(i + 1)
                        if (distToSegmentSquared(touchX, touchY, p1.x, p1.y, p2.x, p2.y) <= radiusSquared) {
                            hit = true
                            break
                        }
                    }
                }

                if (!hit) {
                    newStrokesList.add(stroke)
                    continue
                }

                // Split stroke into remaining sub-strokes outside radius
                modifiedAny = true
                var currentBatch = MutableStrokeInputBatch()

                for (i in 0 until count) {
                    val pt = inputs.get(i)
                    val dx = pt.x - touchX
                    val dy = pt.y - touchY
                    val inside = (dx * dx + dy * dy) <= radiusSquared

                    if (!inside) {
                        currentBatch.add(pt)
                    } else {
                        if (currentBatch.size >= 2) {
                            newStrokesList.add(Stroke(stroke.brush, currentBatch))
                            currentBatch = MutableStrokeInputBatch()
                        } else {
                            currentBatch.clear()
                        }
                    }
                }
                if (currentBatch.size >= 2) {
                    newStrokesList.add(Stroke(stroke.brush, currentBatch))
                }
            }

            if (modifiedAny) {
                strokes.clear()
                strokes.addAll(newStrokesList)
            }
        }
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
        state.loadFromDisk()
        state.loadPdfPage()
    }
    DisposableEffect(note.id) {
        onDispose {
            state.saveActivePageData()
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
        primaryState.saveActivePageData()
        secondaryState?.saveActivePageData()
        onBack()
    }

    fun handleCloseSplit() {
        secondaryState?.saveActivePageData()
        secondaryNote?.let { AcademicDatabaseManager.persistNote(it) }
        secondaryNote = null
        isSwapped = false
        activeNoteId = primaryState.note.id
    }

    // Hardware / gesture back button handling
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
                    PdfNoteHelper.sharePdf(context, file)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
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
                    val screenCenterX = (-activeState.panX + 260f) / activeState.scale
                    val screenCenterY = (-activeState.panY + 220f) / activeState.scale
                    activeState.textBoxes.add(
                        TextBoxElement(
                            x = maxOf(40f, screenCenterX),
                            y = maxOf(60f, screenCenterY),
                            width = 260f,
                            height = 110f,
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
            canUndo = activeState.strokes.isNotEmpty(),
            onUndo = { activeState.undo() },
            canRedo = activeState.undoneStrokes.isNotEmpty(),
            onRedo = { activeState.redo() },
            onClearAll = { activeState.clearAll() },
            onExportPdf = { handleExportPdf() },
            isExporting = isExporting,
            isDarkPaper = activeState.isDarkPaper,
            onToggleDarkPaper = { activeState.toggleDarkPaper() },
            onBack = { handleBack() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
        )

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
    val view = LocalView.current
    var showDeletePageDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clipToBounds()
            // Auto-activate pane on any touch or stylus down (keyed strictly to state.note.id)
            .pointerInput(state.note.id) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    onActivate()
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
                .pointerInput(state.note.id, isFingerDrawing) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)

                        if (!isFingerDrawing) {
                            // ── MODE HANYA STYLUS (Palm Rejection Maksimal) ──
                            // 1 Jari = Geser (Pan), 2 Jari = Zoom & Pan, Stylus = Tulis
                            if (down.type == PointerType.Touch) {
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
                                            val newScale = (state.scale * zoom).coerceIn(0.5f, 3.5f)
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
                                        val newScale = (state.scale * zoom).coerceIn(0.5f, 3.5f)
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
                            val dstW = bmp.width * state.scale
                            val dstH = bmp.height * state.scale

                            drawRoundRect(
                                color = Color(0x1F0F172A),
                                topLeft = Offset(state.panX + 4f, state.panY + 6f),
                                size = Size(dstW, dstH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )

                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(state.panX.toInt(), state.panY.toInt()),
                                dstSize = IntSize(dstW.toInt(), dstH.toInt())
                            )
                        }
                    }
                }
            } else {
                PaperBackground(
                    style = state.paperStyle,
                    scale = state.scale,
                    panX = state.panX,
                    panY = state.panY,
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
                        val processedStrokes = newStrokes.map { stroke ->
                            if (isStrokeHoldAtEnd(stroke, state.scale)) {
                                val refined = ShapeRecognizer.refineStroke(stroke)
                                if (refined != null) {
                                    try {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    } catch (e: Exception) {}
                                    refined
                                } else {
                                    stroke
                                }
                            } else {
                                stroke
                            }
                        }
                        state.strokes.addAll(processedStrokes)
                        state.undoneStrokes.clear()
                        state.saveActivePageData()
                    }
                },
                isEraserMode = activeTool == NoteTool.ERASER && isActive,
                isDarkPaper = state.isDarkPaper,
                isDrawingEnabled = activeTool == NoteTool.PEN || activeTool == NoteTool.HIGHLIGHTER || activeTool == NoteTool.ERASER,
                isFingerDrawingEnabled = isFingerDrawing,
                transformGeneration = transformGeneration,
                eraserRadiusPx = AppSettingsManager.eraserRadius,
                scale = state.scale,
                panX = state.panX,
                panY = state.panY,
                onErase = { worldX, worldY -> state.eraseNear(worldX, worldY, AppSettingsManager.eraserRadius, AppSettingsManager.eraserMode) },
                onEraseEnd = { state.saveActivePageData() },
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
                                    state.strokes.add(newStroke)
                                    state.undoneStrokes.clear()
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
                                        val matchedStrokes = state.strokes.filter { LassoHelper.isStrokeSelected(it, loopCopy) }.toMutableList()
                                        val matchedBoxes = state.textBoxes.filter { LassoHelper.isTextBoxSelected(it, loopCopy) }.toMutableList()
                                        val matchedImages = state.imageElements.filter { LassoHelper.isImageSelected(it, loopCopy) }.toMutableList()

                                        val bounds = LassoHelper.computeCombinedBounds(matchedStrokes, matchedBoxes, matchedImages)
                                        if (bounds != null && (matchedStrokes.isNotEmpty() || matchedBoxes.isNotEmpty() || matchedImages.isNotEmpty())) {
                                            state.lassoSelection = LassoSelectionState(
                                                selectedStrokes = matchedStrokes,
                                                selectedTextBoxes = matchedBoxes,
                                                selectedImages = matchedImages,
                                                bounds = bounds
                                            )
                                        } else {
                                            state.lassoSelection = null
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
                                        x = (sLeft + sWidth / 2f - 140.dp.toPx()).roundToInt().coerceAtLeast(16),
                                        y = (sTop - 52.dp.toPx()).roundToInt().coerceAtLeast(70)
                                    )
                                } else {
                                    IntOffset.Zero
                                }
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Geser
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .pointerInput(state.note.id, state.lassoSelection == null) {
                                            detectDragGestures { change, dragAmount ->
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
                                    Icon(Icons.Filled.OpenWith, contentDescription = "Geser", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                    Text("Geser", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                                }

                                VerticalDivider(modifier = Modifier.height(16.dp), color = Color(0xFFE2E8F0))

                                // Duplikat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        val sel = state.lassoSelection ?: return@clickable
                                        val offsetDelta = 35f
                                        val newDuplicates = sel.selectedStrokes.map { ShapeRecognizer.translateStroke(it, offsetDelta, offsetDelta) }
                                        state.strokes.addAll(newDuplicates)
                                        sel.selectedStrokes.clear()
                                        sel.selectedStrokes.addAll(newDuplicates)
                                        val newBounds = sel.bounds.translate(Offset(offsetDelta, offsetDelta))
                                        state.lassoSelection = sel.copy(bounds = newBounds)
                                        state.undoneStrokes.clear()
                                        state.saveActivePageData()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Duplikat", tint = LernspaceDark, modifier = Modifier.size(13.dp))
                                        Text("Duplikat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                                    }
                                }

                                // Hapus
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.clickable {
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
                                        Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                                        Text("Hapus", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                                    }
                                }

                                // Selesai
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(26.dp).clickable { state.lassoSelection = null }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Close, contentDescription = "Selesai", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
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
                    panX = state.panX,
                    panY = state.panY,
                    onCommitMove = { newWorldX, newWorldY ->
                        if (index < state.imageElements.size) {
                            state.imageElements[index] = state.imageElements[index].copy(x = newWorldX, y = newWorldY)
                            state.saveActivePageData()
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < state.imageElements.size) {
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
                    panX = state.panX,
                    panY = state.panY,
                    isDarkPaper = state.isDarkPaper,
                    onCommitMove = { newWorldX, newWorldY ->
                        if (index < state.textBoxes.size) {
                            state.textBoxes[index] = state.textBoxes[index].copy(x = newWorldX, y = newWorldY)
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < state.textBoxes.size) {
                            state.textBoxes[index] = state.textBoxes[index].copy(width = newWorldW, height = newWorldH)
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
                    onDelete = { state.textBoxes.remove(boxElement) }
                )
            }
        }

        // ── 2. Floating Page Navigator Bar (Bottom Center of this Pane) ──
        if (state.canvasType != NoteCanvasType.INFINITE_SCRATCHPAD) {
            PageNavigatorBar(
                currentPage = state.currentPageIndex + 1,
                totalPages = state.totalPages,
                canAddPage = state.canvasType == NoteCanvasType.PAGED_NOTE,
                canDeletePage = state.canvasType == NoteCanvasType.PAGED_NOTE && state.totalPages > 1,
                onPrevPage = { state.goToPage(state.currentPageIndex - 1) },
                onNextPage = { state.goToPage(state.currentPageIndex + 1) },
                onAddPage = { state.addNewPage() },
                onDeletePage = { showDeletePageDialog = true },
                onJumpToPage = { targetPage -> state.goToPage(targetPage - 1) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
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
            scale = state.scale,
            panX = state.panX,
            panY = state.panY,
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
                onClearAll = onClearAll
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
                onClearAll = onClearAll
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
                        Text(
                            text = when (leftCanvasType) {
                                NoteCanvasType.PDF_DOCUMENT -> "$leftSubject · PDF Document (Hal $leftCurrentPage/$leftTotalPages)"
                                NoteCanvasType.PAGED_NOTE -> "$leftSubject · ${leftPaperStyle.displayName} (Hal $leftCurrentPage/$leftTotalPages)"
                                NoteCanvasType.INFINITE_SCRATCHPAD -> "$leftSubject · Scratchpad Bebas"
                            },
                            fontSize = 11.sp,
                            color = LernspaceTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
    onClearAll: () -> Unit
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
// COMPONENT: Floating Page Navigator Bar (Bottom Center)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PageNavigatorBar(
    currentPage: Int,
    totalPages: Int,
    canAddPage: Boolean,
    canDeletePage: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: () -> Unit,
    onJumpToPage: (pageIndex: Int) -> Unit = {},
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
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.width(360.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ImportContacts,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Lompat ke Halaman",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Total $totalPages halaman tersedia",
                    fontSize = 12.sp,
                    color = LernspaceTextSecondary
                )

                // Large Number Input
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
// COMPONENT: Zoom Percentage Indicator (Bottom-Start)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ZoomIndicator(
    scale: Float,
    panX: Float,
    panY: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = (scale * 100).roundToInt()
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
