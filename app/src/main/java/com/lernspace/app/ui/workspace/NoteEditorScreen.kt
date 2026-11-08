package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.lernspace.app.ui.workspace.model.*
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
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
                    strokes.addAll(newStrokes)
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
        verticalAlignment = Alignment.CenterVertically
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
