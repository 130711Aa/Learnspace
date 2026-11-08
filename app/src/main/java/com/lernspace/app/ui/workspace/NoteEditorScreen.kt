package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun NoteEditorScreen(
    note: NoteUiModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Note Meta
    var noteTitle by remember { mutableStateOf(note.title) }
    val paperStyle = note.paperStyle // Locked/Immutable format set from creation!

    // Multi-page state
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember {
        mutableIntStateOf(
            if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) maxOf(1, note.pdfTotalPages)
            else maxOf(1, note.pages.size)
        )
    }

    // Tool State
    var activeTool by remember { mutableStateOf(NoteTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFF16A34A)) } // Green default
    var selectedThickness by remember { mutableFloatStateOf(7f) }
    var selectedShapeType by remember { mutableStateOf(GeometricShapeType.RECTANGLE) }
    // Helper: Detect if stroke points were held stationary at the end (hold-to-refine)
    fun isStrokeHoldAtEnd(stroke: Stroke, currentScale: Float): Boolean {
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

    // Live preview coordinates for Shape Tool
    var shapeDrawStart by remember { mutableStateOf<Offset?>(null) }
    var shapeDrawCurrent by remember { mutableStateOf<Offset?>(null) }

    // Lasso Selection State
    var lassoSelection by remember { mutableStateOf<LassoSelectionState?>(null) }
    val currentLassoLoop = remember { mutableStateListOf<Offset>() }

    // Inking State for the active page
    val strokes = remember { mutableStateListOf<Stroke>() }
    val undoneStrokes = remember { mutableStateListOf<Stroke>() }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }

    // Text Boxes & Images for the active page
    val textBoxes = remember { mutableStateListOf<TextBoxElement>() }
    val imageElements = remember { mutableStateListOf<ImageElement>() }

    // PDF Page background bitmap state
    var pdfPageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // Canvas Viewport Pan & Zoom State (Fingers only for Pan & Zoom, Stylus dedicated to Inking!)
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Page Synchronization Functions
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

    // Load initial page
    LaunchedEffect(Unit) {
        loadActivePageData(0)
    }

    // Load PDF Page Bitmap when currentPageIndex changes
    LaunchedEffect(currentPageIndex, note.pdfUri) {
        if (note.canvasType == NoteCanvasType.PDF_DOCUMENT && note.pdfUri != null) {
            withContext(Dispatchers.IO) {
                val bmp = PdfNoteHelper.renderPdfPage(context, Uri.parse(note.pdfUri), currentPageIndex, targetWidth = 1800)
                if (bmp != null) {
                    pdfPageBitmap = bmp.asImageBitmap()
                }
            }
        }
    }

    // Multi-page navigation helpers
    fun goToPage(targetIndex: Int) {
        if (targetIndex != currentPageIndex && targetIndex in 0 until totalPages) {
            saveActivePageData()
            currentPageIndex = targetIndex
            loadActivePageData(targetIndex)
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

    var showDeletePageDialog by remember { mutableStateOf(false) }

    fun deleteCurrentPage() {
        if (note.pages.size > 1) {
            note.pages.removeAt(currentPageIndex)
            note.pages.forEachIndexed { idx, page -> page.pageIndex = idx }
            val newIdx = if (currentPageIndex >= note.pages.size) note.pages.size - 1 else currentPageIndex
            currentPageIndex = newIdx
            totalPages = note.pages.size
            loadActivePageData(newIdx)
        }
    }

    fun handleBack() {
        saveActivePageData()
        onBack()
    }

    fun handleExportPdf() {
        saveActivePageData()
        isExporting = true
        coroutineScope.launch(Dispatchers.IO) {
            val file = PdfNoteHelper.exportNoteToPdf(context, note, canvasStrokeRenderer)
            withContext(Dispatchers.Main) {
                isExporting = false
                if (file != null) {
                    PdfNoteHelper.sharePdf(context, file)
                }
            }
        }
    }

    // Helper: Deselect all objects
    fun deselectAll() {
        lassoSelection = null
        for (i in textBoxes.indices) {
            textBoxes[i] = textBoxes[i].copy(isSelected = false)
        }
        for (i in imageElements.indices) {
            imageElements[i] = imageElements[i].copy(isSelected = false)
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        deselectAll()
                        val spawnX = (-panX + 160f) / scale
                        val spawnY = (-panY + 120f) / scale
                        val aspect = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.2f, 5f)
                        val baseWidth = 280f
                        val baseHeight = baseWidth / aspect
                        imageElements.add(
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

    // Erase helper function with segment-distance checking & zoom compensation
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

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Vector Note Canvas Viewport
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Layer: Either PDF Document Page or Paper Style
            if (note.canvasType == NoteCanvasType.PDF_DOCUMENT) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFE2E8F0)) // desk canvas background
                    if (pdfPageBitmap != null) {
                        val bmp = pdfPageBitmap!!
                        val dstW = bmp.width * scale
                        val dstH = bmp.height * scale

                        // Drop shadow behind PDF page sheet
                        drawRoundRect(
                            color = Color(0x1F0F172A),
                            topLeft = Offset(panX + 4f, panY + 6f),
                            size = Size(dstW, dstH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(panX.toInt(), panY.toInt()),
                            dstSize = IntSize(dstW.toInt(), dstH.toInt())
                        )
                    }
                }
            } else {
                PaperBackground(
                    style = paperStyle,
                    scale = scale,
                    panX = panX,
                    panY = panY,
                    isPaged = note.canvasType == NoteCanvasType.PAGED_NOTE,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Inking Layer (AndroidX Ink Native Vector Tessellation)
            DrawingSurface(
                strokes = strokes,
                canvasStrokeRenderer = canvasStrokeRenderer,
                currentBrush = currentBrush,
                onGetNextBrush = { currentBrush },
                onStrokesFinished = { newStrokes ->
                    val processedStrokes = newStrokes.map { stroke ->
                        if (isStrokeHoldAtEnd(stroke, scale)) {
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
                    strokes.addAll(processedStrokes)
                    undoneStrokes.clear()
                },
                isEraserMode = activeTool == NoteTool.ERASER,
                isDrawingEnabled = activeTool == NoteTool.PEN || activeTool == NoteTool.HIGHLIGHTER || activeTool == NoteTool.ERASER,
                scale = scale,
                panX = panX,
                panY = panY,
                onErase = { worldX, worldY -> eraseNear(worldX, worldY) },
                onTransform = { centroid, pan, zoom ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 3.5f)
                    val effectiveZoom = newScale / scale
                    panX = centroid.x - (centroid.x - panX) * effectiveZoom + pan.x
                    panY = centroid.y - (centroid.y - panY) * effectiveZoom + pan.y
                    scale = newScale
                },
                onCanvasTap = {
                    deselectAll()
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── SHAPE TOOL DRAWING & LIVE PREVIEW LAYER ──
            if (activeTool == NoteTool.SHAPE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedShapeType, currentBrush) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                // STYLUS ONLY: Fingers are for canvas pan & zoom!
                                if (down.type != androidx.compose.ui.input.pointer.PointerType.Stylus && down.type != androidx.compose.ui.input.pointer.PointerType.Eraser) {
                                    return@awaitEachGesture
                                }

                                val worldStart = Offset((down.position.x - panX) / scale, (down.position.y - panY) / scale)
                                shapeDrawStart = worldStart
                                shapeDrawCurrent = worldStart

                                drag(down.id) { change ->
                                    if (change.type == androidx.compose.ui.input.pointer.PointerType.Stylus || change.type == androidx.compose.ui.input.pointer.PointerType.Eraser) {
                                        change.consume()
                                        shapeDrawCurrent = Offset((change.position.x - panX) / scale, (change.position.y - panY) / scale)
                                    }
                                }

                                val start = shapeDrawStart
                                val end = shapeDrawCurrent
                                if (start != null && end != null && (end - start).getDistance() > 15f) {
                                    val newStroke = ShapeRecognizer.generateGeometricShape(
                                        type = selectedShapeType,
                                        start = start,
                                        end = end,
                                        brush = currentBrush
                                    )
                                    strokes.add(newStroke)
                                    undoneStrokes.clear()
                                }
                                shapeDrawStart = null
                                shapeDrawCurrent = null
                            }
                        }
                )

                // Live Preview Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val start = shapeDrawStart
                    val end = shapeDrawCurrent
                    if (start != null && end != null) {
                        val sX = start.x * scale + panX
                        val sY = start.y * scale + panY
                        val eX = end.x * scale + panX
                        val eY = end.y * scale + panY

                        val minX = minOf(sX, eX)
                        val maxX = maxOf(sX, eX)
                        val minY = minOf(sY, eY)
                        val maxY = maxOf(sY, eY)
                        val w = maxOf(10f, maxX - minX)
                        val h = maxOf(10f, maxY - minY)
                        val strokeW = selectedThickness * scale

                        when (selectedShapeType) {
                            GeometricShapeType.LINE -> {
                                drawLine(
                                    color = selectedColor,
                                    start = Offset(sX, sY),
                                    end = Offset(eX, eY),
                                    strokeWidth = strokeW
                                )
                            }
                            GeometricShapeType.RECTANGLE -> {
                                drawRect(
                                    color = selectedColor,
                                    topLeft = Offset(minX, minY),
                                    size = Size(w, h),
                                    style = DrawStroke(width = strokeW)
                                )
                            }
                            GeometricShapeType.CIRCLE -> {
                                drawOval(
                                    color = selectedColor,
                                    topLeft = Offset(minX, minY),
                                    size = Size(w, h),
                                    style = DrawStroke(width = strokeW)
                                )
                            }
                            GeometricShapeType.TRIANGLE -> {
                                val path = Path().apply {
                                    moveTo((minX + maxX) / 2f, minY)
                                    lineTo(maxX, maxY)
                                    lineTo(minX, maxY)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = selectedColor,
                                    style = DrawStroke(width = strokeW)
                                )
                            }
                            GeometricShapeType.ARROW -> {
                                drawLine(
                                    color = selectedColor,
                                    start = Offset(sX, sY),
                                    end = Offset(eX, eY),
                                    strokeWidth = strokeW
                                )
                                val dx = eX - sX
                                val dy = eY - sY
                                val angle = kotlin.math.atan2(dy, dx)
                                val headLen = minOf(36f * scale, kotlin.math.hypot(dx, dy) * 0.35f)
                                val headAngle = Math.toRadians(30.0).toFloat()
                                drawLine(
                                    color = selectedColor,
                                    start = Offset(eX, eY),
                                    end = Offset(
                                        eX - headLen * kotlin.math.cos(angle - headAngle),
                                        eY - headLen * kotlin.math.sin(angle - headAngle)
                                    ),
                                    strokeWidth = strokeW
                                )
                                drawLine(
                                    color = selectedColor,
                                    start = Offset(eX, eY),
                                    end = Offset(
                                        eX - headLen * kotlin.math.cos(angle + headAngle),
                                        eY - headLen * kotlin.math.sin(angle + headAngle)
                                    ),
                                    strokeWidth = strokeW
                                )
                            }
                        }
                    }
                }
            }

            // ── LASSO TOOL SELECTION & MANIPULATION LAYER ──
            if (activeTool == NoteTool.LASSO || lassoSelection != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeTool, lassoSelection == null) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                // STYLUS ONLY: Fingers are for canvas pan & zoom!
                                if (down.type != androidx.compose.ui.input.pointer.PointerType.Stylus && down.type != androidx.compose.ui.input.pointer.PointerType.Eraser) {
                                    return@awaitEachGesture
                                }

                                val screenPoint = down.position
                                val worldPoint = Offset((screenPoint.x - panX) / scale, (screenPoint.y - panY) / scale)

                                val curSel = lassoSelection
                                if (curSel != null && !curSel.isEmpty) {
                                    val screenBounds = Rect(
                                        curSel.bounds.left * scale + panX,
                                        curSel.bounds.top * scale + panY,
                                        curSel.bounds.right * scale + panX,
                                        curSel.bounds.bottom * scale + panY
                                    )
                                    if (screenBounds.contains(screenPoint)) {
                                        val strokeIndices = curSel.selectedStrokes.map { strokes.indexOf(it) }

                                        // Drag inside selection bounding box with stylus to MOVE selected items!
                                        drag(down.id) { change ->
                                            if (change.type == androidx.compose.ui.input.pointer.PointerType.Stylus || change.type == androidx.compose.ui.input.pointer.PointerType.Eraser) {
                                                val delta = change.positionChange()
                                                change.consume()
                                                val dx = delta.x / scale
                                                val dy = delta.y / scale

                                                if (dx != 0f || dy != 0f) {
                                                    // 1. Move selected strokes
                                                    for (i in curSel.selectedStrokes.indices) {
                                                        val oldStroke = curSel.selectedStrokes[i]
                                                        val newStroke = ShapeRecognizer.translateStroke(oldStroke, dx, dy)
                                                        val idxInMain = strokeIndices[i]
                                                        if (idxInMain != -1 && idxInMain < strokes.size) {
                                                            strokes[idxInMain] = newStroke
                                                        }
                                                        curSel.selectedStrokes[i] = newStroke
                                                    }

                                                    // 2. Move selected text boxes
                                                    for (box in curSel.selectedTextBoxes) {
                                                        box.x += dx
                                                        box.y += dy
                                                    }

                                                    // 3. Move selected images
                                                    for (img in curSel.selectedImages) {
                                                        img.x += dx
                                                        img.y += dy
                                                    }

                                                    curSel.bounds = curSel.bounds.translate(Offset(dx, dy))
                                                    lassoSelection = curSel.copy(bounds = curSel.bounds)
                                                }
                                            }
                                        }
                                        return@awaitEachGesture
                                    } else {
                                        // Stylus tapped outside bounding box -> deselect!
                                        lassoSelection = null
                                    }
                                }

                                if (activeTool == NoteTool.LASSO) {
                                    // Draw new lasso loop with stylus
                                    currentLassoLoop.clear()
                                    currentLassoLoop.add(worldPoint)

                                    drag(down.id) { change ->
                                        if (change.type == androidx.compose.ui.input.pointer.PointerType.Stylus || change.type == androidx.compose.ui.input.pointer.PointerType.Eraser) {
                                            change.consume()
                                            val curWorld = Offset((change.position.x - panX) / scale, (change.position.y - panY) / scale)
                                            currentLassoLoop.add(curWorld)
                                        }
                                    }

                                    // Finished drawing lasso loop: calculate matches!
                                    if (currentLassoLoop.size >= 4) {
                                        val loopCopy = currentLassoLoop.toList()
                                        val matchedStrokes = strokes.filter { LassoHelper.isStrokeSelected(it, loopCopy) }.toMutableList()
                                        val matchedBoxes = textBoxes.filter { LassoHelper.isTextBoxSelected(it, loopCopy) }.toMutableList()
                                        val matchedImages = imageElements.filter { LassoHelper.isImageSelected(it, loopCopy) }.toMutableList()

                                        val bounds = LassoHelper.computeCombinedBounds(matchedStrokes, matchedBoxes, matchedImages)
                                        if (bounds != null && (matchedStrokes.isNotEmpty() || matchedBoxes.isNotEmpty() || matchedImages.isNotEmpty())) {
                                            lassoSelection = LassoSelectionState(
                                                selectedStrokes = matchedStrokes,
                                                selectedTextBoxes = matchedBoxes,
                                                selectedImages = matchedImages,
                                                bounds = bounds
                                            )
                                        } else {
                                            lassoSelection = null
                                        }
                                    }
                                    currentLassoLoop.clear()
                                }
                            }
                        }
                )

                // Render in-progress Lasso loop (dashed path + semi-transparent fill)
                if (currentLassoLoop.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            val p0 = currentLassoLoop[0]
                            moveTo(p0.x * scale + panX, p0.y * scale + panY)
                            for (i in 1 until currentLassoLoop.size) {
                                val p = currentLassoLoop[i]
                                lineTo(p.x * scale + panX, p.y * scale + panY)
                            }
                        }
                        drawPath(
                            path = path,
                            color = AccentBlue.copy(alpha = 0.12f),
                            style = Fill
                        )
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

                // Render Lasso Selection Bounding Box & Floating Action Menu
                val curSel = lassoSelection
                if (curSel != null && !curSel.isEmpty) {
                    val screenLeft = curSel.bounds.left * scale + panX
                    val screenTop = curSel.bounds.top * scale + panY
                    val screenRight = curSel.bounds.right * scale + panX
                    val screenBottom = curSel.bounds.bottom * scale + panY
                    val selWidth = maxOf(40f, screenRight - screenLeft)
                    val selHeight = maxOf(40f, screenBottom - screenTop)

                    // Dashed selection marquee
                    Canvas(modifier = Modifier.fillMaxSize()) {
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

                    // Floating Action Menu (Geser / Duplikat / Hapus / Selesai)
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
                                // Drag Move pill (both draggable and indicates drag area)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .pointerInput(lassoSelection == null) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val sel = lassoSelection ?: return@detectDragGestures
                                                val dx = dragAmount.x / scale
                                                val dy = dragAmount.y / scale
                                                if (dx != 0f || dy != 0f) {
                                                    for (i in sel.selectedStrokes.indices) {
                                                        val oldStroke = sel.selectedStrokes[i]
                                                        val newStroke = ShapeRecognizer.translateStroke(oldStroke, dx, dy)
                                                        val idxInMain = strokes.indexOf(oldStroke)
                                                        if (idxInMain != -1) {
                                                            strokes[idxInMain] = newStroke
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
                                                    lassoSelection = sel.copy(bounds = newBounds)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenWith,
                                        contentDescription = "Geser",
                                        tint = AccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Geser",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                }

                                VerticalDivider(modifier = Modifier.height(16.dp), color = Color(0xFFE2E8F0))

                                // Duplicate button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        val offsetDelta = 35f
                                        val newDuplicates = curSel.selectedStrokes.map {
                                            ShapeRecognizer.translateStroke(it, offsetDelta, offsetDelta)
                                        }
                                        strokes.addAll(newDuplicates)
                                        curSel.selectedStrokes.clear()
                                        curSel.selectedStrokes.addAll(newDuplicates)
                                        curSel.bounds = curSel.bounds.translate(Offset(offsetDelta, offsetDelta))
                                        lassoSelection = curSel.copy(bounds = curSel.bounds)
                                        undoneStrokes.clear()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Duplikat",
                                            tint = LernspaceDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Duplikat",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LernspaceDark
                                        )
                                    }
                                }

                                // Delete button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.clickable {
                                        strokes.removeAll(curSel.selectedStrokes)
                                        textBoxes.removeAll(curSel.selectedTextBoxes)
                                        imageElements.removeAll(curSel.selectedImages)
                                        lassoSelection = null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Hapus",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Hapus",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }

                                // Done / Close button
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable { lassoSelection = null }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Selesai",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Placed Images Layer
            imageElements.forEachIndexed { index, img ->
                DraggableImage(
                    image = img,
                    scale = scale,
                    panX = panX,
                    panY = panY,
                    onCommitMove = { newWorldX, newWorldY ->
                        if (index < imageElements.size) {
                            imageElements[index] = imageElements[index].copy(x = newWorldX, y = newWorldY)
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < imageElements.size) {
                            imageElements[index] = imageElements[index].copy(width = newWorldW, height = newWorldH)
                        }
                    },
                    onSelect = {
                        deselectAll()
                        if (index < imageElements.size) {
                            imageElements[index] = imageElements[index].copy(isSelected = true)
                        }
                    },
                    onDelete = { imageElements.remove(img) }
                )
            }

            // Placed Draggable Text Boxes Layer
            textBoxes.forEachIndexed { index, boxElement ->
                DraggableTextBox(
                    box = boxElement,
                    scale = scale,
                    panX = panX,
                    panY = panY,
                    onCommitMove = { newWorldX, newWorldY ->
                        if (index < textBoxes.size) {
                            textBoxes[index] = textBoxes[index].copy(x = newWorldX, y = newWorldY)
                        }
                    },
                    onCommitResize = { newWorldW, newWorldH ->
                        if (index < textBoxes.size) {
                            textBoxes[index] = textBoxes[index].copy(width = newWorldW, height = newWorldH)
                        }
                    },
                    onTextChange = { newText ->
                        if (index < textBoxes.size) {
                            textBoxes[index] = textBoxes[index].copy(text = newText)
                        }
                    },
                    onSelect = {
                        deselectAll()
                        if (index < textBoxes.size) {
                            textBoxes[index] = textBoxes[index].copy(isSelected = true)
                        }
                        activeTool = NoteTool.TEXT
                    },
                    onDelete = {
                        textBoxes.remove(boxElement)
                    }
                )
            }
        }

        // 2. Unified Top App Bar (Title Capsule + Center Tools Toolbar + Action Capsule)
        UnifiedNoteTopBar(
            title = noteTitle,
            onTitleChange = {
                noteTitle = it
                note.title = it
            },
            subject = note.subject,
            canvasType = note.canvasType,
            paperStyle = paperStyle,
            currentPage = currentPageIndex + 1,
            totalPages = totalPages,
            activeTool = activeTool,
            onSelectTool = { tool ->
                activeTool = tool
                if (tool != NoteTool.TEXT) {
                    deselectAll()
                } else {
                    deselectAll()
                    val screenCenterX = (-panX + 260f) / scale
                    val screenCenterY = (-panY + 220f) / scale
                    textBoxes.add(
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
            canUndo = strokes.isNotEmpty(),
            onUndo = {
                if (strokes.isNotEmpty()) {
                    val removed = strokes.removeLast()
                    undoneStrokes.add(removed)
                }
            },
            canRedo = undoneStrokes.isNotEmpty(),
            onRedo = {
                if (undoneStrokes.isNotEmpty()) {
                    val restored = undoneStrokes.removeLast()
                    strokes.add(restored)
                }
            },
            onClearAll = {
                strokes.clear()
                undoneStrokes.clear()
                textBoxes.clear()
                imageElements.clear()
            },
            onExportPdf = { handleExportPdf() },
            isExporting = isExporting,
            onBack = { handleBack() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        // 3. Floating Page Navigator Bar (Bottom Center) - Multi-Page and PDF Document
        if (note.canvasType != NoteCanvasType.INFINITE_SCRATCHPAD) {
            PageNavigatorBar(
                currentPage = currentPageIndex + 1,
                totalPages = totalPages,
                canAddPage = note.canvasType == NoteCanvasType.PAGED_NOTE,
                canDeletePage = note.canvasType == NoteCanvasType.PAGED_NOTE && totalPages > 1,
                onPrevPage = { goToPage(currentPageIndex - 1) },
                onNextPage = { goToPage(currentPageIndex + 1) },
                onAddPage = { addNewPage() },
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
                        text = "Hapus Halaman ${currentPageIndex + 1}?",
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                },
                text = {
                    Text(
                        text = "Seluruh coretan dan konten pada halaman ${currentPageIndex + 1} akan dihapus secara permanen. Tindakan ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            deleteCurrentPage()
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

        // 4. Floating Zoom Percentage Indicator (Bottom-Left Corner of Canvas)
        ZoomIndicator(
            scale = scale,
            panX = panX,
            panY = panY,
            onReset = {
                scale = 1f
                panX = 0f
                panY = 0f
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Unified Floating Note Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UnifiedNoteTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    subject: String,
    canvasType: NoteCanvasType,
    paperStyle: PaperStyle,
    currentPage: Int,
    totalPages: Int,
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
        // ── 1. LEFT CAPSULE: Back Button + Title & Immutable Format Label (Max 300dp) ──
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.widthIn(min = 180.dp, max = 300.dp)
        ) {
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
                        value = title,
                        onValueChange = onTitleChange,
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
                        text = when (canvasType) {
                            NoteCanvasType.PDF_DOCUMENT -> "$subject · PDF Document (Hal $currentPage/$totalPages)"
                            NoteCanvasType.PAGED_NOTE -> "$subject · ${paperStyle.displayName} (Hal $currentPage/$totalPages)"
                            NoteCanvasType.INFINITE_SCRATCHPAD -> "$subject · Scratchpad Bebas"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ── 2. CENTER: NoteFloatingToolbar ──────────────────────────────────────
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

        // ── 3. RIGHT CAPSULE: Export PDF, Undo, Redo, Clear ────────────────────
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
// COMPONENT: Zoom Percentage Indicator (Bottom-Left)
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
        modifier = modifier
            .clickable(onClick = onReset)
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
