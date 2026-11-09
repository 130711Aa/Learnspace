package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.lernspace.app.data.local.AcademicDatabaseManager
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.components.DrawingSurface
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.LernspaceBorder
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.theme.LernspaceSurfaceLow
import com.lernspace.app.ui.workspace.components.DraggableImage
import com.lernspace.app.ui.workspace.components.DraggableTextBox
import com.lernspace.app.ui.workspace.components.NoteFloatingToolbar
import com.lernspace.app.ui.workspace.components.PaperBackground
import com.lernspace.app.ui.workspace.lasso.LassoHelper
import com.lernspace.app.ui.workspace.lasso.LassoSelectionState
import com.lernspace.app.ui.workspace.model.*
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import com.lernspace.app.ui.workspace.shape.ShapeRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: Detect if stroke points were held stationary at the end (hold-to-refine)
// ─────────────────────────────────────────────────────────────────────────────
private fun isStrokeHoldAtEnd(stroke: Stroke, currentScale: Float): Boolean {
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
    return (endT - stationaryStartTime) >= 260L
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
    val paperStyle: PaperStyle get() = note.paperStyle
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

    fun eraseNear(touchX: Float, touchY: Float) {
        val effectiveRadius = 40f / scale
        val radiusSquared = effectiveRadius * effectiveRadius
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
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val l2 = dx * dx + dy * dy
                val d2 = if (l2 == 0f) {
                    val ex = touchX - p1.x
                    val ey = touchY - p1.y
                    ex * ex + ey * ey
                } else {
                    val t = (((touchX - p1.x) * dx + (touchY - p1.y) * dy) / l2).coerceIn(0f, 1f)
                    val projX = p1.x + t * dx
                    val projY = p1.y + t * dy
                    val ex = touchX - projX
                    val ey = touchY - projY
                    ex * ex + ey * ey
                }
                if (d2 <= radiusSquared) {
                    return@removeAll true
                }
            }
            false
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

    // Shared Tool State across the single unified toolbar system
    var activeTool by remember { mutableStateOf(NoteTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFF16A34A)) } // Green default
    var selectedThickness by remember { mutableFloatStateOf(7f) }
    var selectedShapeType by remember { mutableStateOf(GeometricShapeType.RECTANGLE) }
    var isExporting by remember { mutableStateOf(false) }

    // Brush calculation
    val currentBrush by remember(activeTool, selectedColor, selectedThickness) {
        derivedStateOf {
            when (activeTool) {
                NoteTool.HIGHLIGHTER -> {
                    Brush.createWithComposeColor(
                        family = StockBrushes.pressurePen(),
                        color = selectedColor.copy(alpha = 0.35f),
                        size = selectedThickness * 2.8f,
                        epsilon = 0.1f
                    )
                }
                else -> {
                    Brush.createWithComposeColor(
                        family = StockBrushes.pressurePen(),
                        color = selectedColor,
                        size = selectedThickness,
                        epsilon = 0.1f
                    )
                }
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

    // Image Picker Launcher (places image into whichever note is currently active)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        activeState.deselectAll()
                        val spawnX = (-activeState.panX + 160f) / activeState.scale
                        val spawnY = (-activeState.panY + 120f) / activeState.scale
                        val aspect = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.2f, 5f)
                        val baseWidth = 280f
                        val baseHeight = baseWidth / aspect
                        activeState.imageElements.add(
                            ImageElement(
                                x = maxOf(40f, spawnX),
                                y = maxOf(60f, spawnY),
                                width = baseWidth,
                                height = baseHeight,
                                bitmap = bitmap.asImageBitmap(),
                                isSelected = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    selectedColor = selectedColor,
                    selectedThickness = selectedThickness,
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
                                selectedColor = selectedColor,
                                selectedThickness = selectedThickness,
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
                                selectedColor = selectedColor,
                                selectedThickness = selectedThickness,
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
            selectedColor = selectedColor,
            onSelectColor = {
                selectedColor = it
                if (activeTool == NoteTool.ERASER) activeTool = NoteTool.PEN
            },
            selectedThickness = selectedThickness,
            onSelectThickness = {
                selectedThickness = it
                if (activeTool == NoteTool.ERASER) activeTool = NoteTool.PEN
            },
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
        // ── 1. Pan & Zoom Gesture Detector (Fingers for Pan/Zoom, Stylus for Inking) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(state.note.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
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
                    }
                }
        ) {
            // Background Layer: Either PDF Document Page or Paper Style (Strictly clipped)
            if (state.canvasType == NoteCanvasType.PDF_DOCUMENT) {
                Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    clipRect(0f, 0f, size.width, size.height) {
                        drawRect(Color(0xFFE2E8F0)) // desk canvas background
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
                },
                isEraserMode = activeTool == NoteTool.ERASER && isActive,
                isDrawingEnabled = activeTool == NoteTool.PEN || activeTool == NoteTool.HIGHLIGHTER || activeTool == NoteTool.ERASER,
                scale = state.scale,
                panX = state.panX,
                panY = state.panY,
                onErase = { worldX, worldY -> state.eraseNear(worldX, worldY) },
                onEraseEnd = { state.saveActivePageData() },
                modifier = Modifier.fillMaxSize().clipToBounds()
            )

            // Shape Tool Drawing & Live Preview Layer (Strictly clipped)
            if (activeTool == NoteTool.SHAPE && isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(state.note.id, selectedShapeType, currentBrush) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.type != PointerType.Stylus && down.type != PointerType.Eraser) {
                                    return@awaitEachGesture
                                }

                                val worldStart = Offset((down.position.x - state.panX) / state.scale, (down.position.y - state.panY) / state.scale)
                                state.shapeDrawStart = worldStart
                                state.shapeDrawCurrent = worldStart

                                drag(down.id) { change ->
                                    if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) {
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

                            when (selectedShapeType) {
                                GeometricShapeType.LINE -> {
                                    drawLine(color = selectedColor, start = Offset(sX, sY), end = Offset(eX, eY), strokeWidth = strokeW)
                                }
                                GeometricShapeType.RECTANGLE -> {
                                    drawRect(color = selectedColor, topLeft = Offset(minX, minY), size = Size(w, h), style = DrawStroke(width = strokeW))
                                }
                                GeometricShapeType.CIRCLE -> {
                                    drawOval(color = selectedColor, topLeft = Offset(minX, minY), size = Size(w, h), style = DrawStroke(width = strokeW))
                                }
                                GeometricShapeType.TRIANGLE -> {
                                    val path = Path().apply {
                                        moveTo((minX + maxX) / 2f, minY)
                                        lineTo(maxX, maxY)
                                        lineTo(minX, maxY)
                                        close()
                                    }
                                    drawPath(path = path, color = selectedColor, style = DrawStroke(width = strokeW))
                                }
                                GeometricShapeType.ARROW -> {
                                    drawLine(color = selectedColor, start = Offset(sX, sY), end = Offset(eX, eY), strokeWidth = strokeW)
                                    val dx = eX - sX
                                    val dy = eY - sY
                                    val angle = kotlin.math.atan2(dy, dx)
                                    val headLen = minOf(36f * state.scale, kotlin.math.hypot(dx, dy) * 0.35f)
                                    val headAngle = Math.toRadians(30.0).toFloat()
                                    drawLine(
                                        color = selectedColor,
                                        start = Offset(eX, eY),
                                        end = Offset(eX - headLen * kotlin.math.cos(angle - headAngle), eY - headLen * kotlin.math.sin(angle - headAngle)),
                                        strokeWidth = strokeW
                                    )
                                    drawLine(
                                        color = selectedColor,
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
                        .pointerInput(state.note.id, activeTool, state.lassoSelection == null) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.type != PointerType.Stylus && down.type != PointerType.Eraser) {
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
                                            if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) {
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
                                                    val newBounds = selBounds(curSel).translate(Offset(dx, dy))
                                                    curSel.bounds = newBounds
                                                    state.lassoSelection = curSel.copy(bounds = newBounds)
                                                }
                                            }
                                        }
                                        return@awaitEachGesture
                                    } else {
                                        state.lassoSelection = null
                                    }
                                }

                                if (activeTool == NoteTool.LASSO) {
                                    state.currentLassoLoop.clear()
                                    state.currentLassoLoop.add(worldPoint)

                                    drag(down.id) { change ->
                                        if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) {
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
                    val screenLeft = curSel.bounds.left * state.scale + state.panX
                    val screenTop = curSel.bounds.top * state.scale + state.panY
                    val screenRight = curSel.bounds.right * state.scale + state.panX
                    val screenBottom = curSel.bounds.bottom * state.scale + state.panY
                    val selWidth = maxOf(40f, screenRight - screenLeft)
                    val selHeight = maxOf(40f, screenBottom - screenTop)

                    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        clipRect(0f, 0f, size.width, size.height) {
                            drawRoundRect(
                                color = AccentBlue,
                                topLeft = Offset(screenLeft, screenTop),
                                size = Size(selWidth, selHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = DrawStroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            )
                        }
                    }

                    // Floating Action Menu
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (screenLeft + selWidth / 2f - 140.dp.toPx()).roundToInt().coerceAtLeast(16),
                                    y = (screenTop - 52.dp.toPx()).roundToInt().coerceAtLeast(70)
                                )
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
                                                    val newBounds = selBounds(curSel).translate(Offset(dx, dy))
                                                    curSel.bounds = newBounds
                                                    state.lassoSelection = curSel.copy(bounds = newBounds)
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
                                        val offsetDelta = 35f
                                        val newDuplicates = curSel.selectedStrokes.map { ShapeRecognizer.translateStroke(it, offsetDelta, offsetDelta) }
                                        state.strokes.addAll(newDuplicates)
                                        curSel.selectedStrokes.clear()
                                        curSel.selectedStrokes.addAll(newDuplicates)
                                        curSel.bounds = curSel.bounds.translate(Offset(offsetDelta, offsetDelta))
                                        state.lassoSelection = curSel.copy(bounds = curSel.bounds)
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
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < state.imageElements.size) {
                            state.imageElements[index] = state.imageElements[index].copy(width = newWorldW, height = newWorldH)
                        }
                    },
                    onSelect = {
                        state.deselectAll()
                        if (index < state.imageElements.size) {
                            state.imageElements[index] = state.imageElements[index].copy(isSelected = true)
                        }
                    },
                    onDelete = { state.imageElements.remove(img) }
                )
            }

            // Placed Draggable Text Boxes Layer (Rendered inside pane)
            state.textBoxes.forEachIndexed { index, boxElement ->
                DraggableTextBox(
                    box = boxElement,
                    scale = state.scale,
                    panX = state.panX,
                    panY = state.panY,
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

        // ── 3. Floating Zoom Percentage Indicator (Bottom-Start of this Pane) ──
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
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 16.dp)
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
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    selectedThickness: Float,
    onSelectThickness: (Float) -> Unit,
    onPickImage: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    onExportPdf: () -> Unit,
    isExporting: Boolean,
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
        // ── 1. LEFT CAPSULE: Back Button + Title or Clean Tabs ──
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.widthIn(min = 180.dp, max = 340.dp)
        ) {
            if (!isSplitMode) {
                // SINGLE NOTE HEADER
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
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
                        Text(
                            text = when (leftCanvasType) {
                                NoteCanvasType.PDF_DOCUMENT -> "$leftSubject · PDF Document (Hal $leftCurrentPage/$leftTotalPages)"
                                NoteCanvasType.PAGED_NOTE -> "$leftSubject · ${leftPaperStyle.displayName} (Hal $leftCurrentPage/$leftTotalPages)"
                                NoteCanvasType.INFINITE_SCRATCHPAD -> "$leftSubject · Scratchpad Bebas"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Split Screen Toggle Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
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
                // SPLIT SCREEN DUAL TABS (Clean and elegant note title tabs)
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
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
                        color = if (isLeftActive) AccentBlue.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isLeftActive) AccentBlue else Color(0xFFE2E8F0)),
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
                            color = if (isRightActive) AccentBlue.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isRightActive) AccentBlue else Color(0xFFE2E8F0)),
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

        // ── 2. CENTER: Single Unified NoteFloatingToolbar ──
        NoteFloatingToolbar(
            activeTool = activeTool,
            onSelectTool = onSelectTool,
            selectedShapeType = selectedShapeType,
            onSelectShapeType = onSelectShapeType,
            selectedColor = selectedColor,
            onSelectColor = onSelectColor,
            selectedThickness = selectedThickness,
            onSelectThickness = onSelectThickness,
            onPickImage = onPickImage
        )

        // ── 3. RIGHT CAPSULE: Close Split (if split) + Export PDF + Undo + Redo + Clear ──
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSplitMode) {
                    // Close Split Button (Minimal red circle)
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

                    VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFFE2E8F0))
                }

                // Export PDF Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
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

                // Undo
                Surface(
                    shape = CircleShape,
                    color = if (canUndo) Color(0xFFF1F5F9) else Color.Transparent,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(enabled = canUndo, onClick = onUndo)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) LernspaceDark else Color(0xFFCBD5E1),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Redo
                Surface(
                    shape = CircleShape,
                    color = if (canRedo) Color(0xFFF1F5F9) else Color.Transparent,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(enabled = canRedo, onClick = onRedo)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) LernspaceDark else Color(0xFFCBD5E1),
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
    modifier: Modifier = Modifier
) {
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

            // Page Indicator Label
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LernspaceSurfaceLow,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = "Halaman $currentPage / $totalPages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
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
