package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.lernspace.app.ui.components.DrawingSurface
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.workspace.components.DraggableImage
import com.lernspace.app.ui.workspace.components.DraggableTextBox
import com.lernspace.app.ui.workspace.components.NoteFloatingToolbar
import com.lernspace.app.ui.workspace.components.PaperBackground
import com.lernspace.app.ui.workspace.model.*

@Composable
fun NoteEditorScreen(
    note: NoteUiModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Note Meta
    var noteTitle by remember { mutableStateOf(note.title) }
    var paperStyle by remember { mutableStateOf(PaperStyle.RULED) }

    // Tool State
    var activeTool by remember { mutableStateOf(NoteTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFF16A34A)) } // Green default
    var selectedThickness by remember { mutableFloatStateOf(7f) }

    // Inking State
    val strokes = remember { mutableStateListOf<Stroke>() }
    val undoneStrokes = remember { mutableStateListOf<Stroke>() }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }

    // Text Boxes & Images
    val textBoxes = remember { mutableStateListOf<TextBoxElement>() }
    val imageElements = remember { mutableStateListOf<ImageElement>() }

    // Canvas Viewport Pan & Zoom State (Fingers only for Pan & Zoom, Stylus dedicated to Inking!)
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

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
        // 1. Vector Note Canvas Viewport (100% Vector Rendering - Zero Blur at Any Zoom Level!)
        Box(modifier = Modifier.fillMaxSize()) {
            // Paper Background (Rendered at full native tablet display resolution)
            PaperBackground(
                style = paperStyle,
                scale = scale,
                panX = panX,
                panY = panY,
                modifier = Modifier.fillMaxSize()
            )

            // Inking Layer (AndroidX Ink Native Vector Tessellation - STYLUS ONLY for Inking, FINGER for Pan & Zoom)
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

            // Placed Images Layer (Rendered on top of DrawingSurface so gestures work cleanly!)
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

            // Placed Draggable Text Boxes Layer (MS Word Style - Crisp Vector Fonts)
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

        // 6. Top Header Navigation Bar
        NoteTopHeader(
            title = noteTitle,
            onTitleChange = {
                noteTitle = it
                note.title = it
            },
            subject = note.subject,
            paperStyle = paperStyle,
            onSelectPaperStyle = { paperStyle = it },
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
            onBack = onBack
        )

        // 7. Floating Pill Toolbar (Top Center)
        NoteFloatingToolbar(
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        // 8. Floating Zoom Percentage Indicator (Bottom-Left Corner of Canvas)
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

@Composable
private fun ZoomIndicator(
    scale: Float,
    panX: Float,
    panY: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.clickable { onReset() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ZoomIn,
                contentDescription = "Zoom",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${(scale * 100).roundToInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            if (scale != 1f || panX != 0f || panY != 0f) {
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NoteTopHeader(
    title: String,
    onTitleChange: (String) -> Unit,
    subject: String,
    paperStyle: PaperStyle,
    onSelectPaperStyle: (PaperStyle) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPaperMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back button & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LernspaceDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        color = LernspaceDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(LernspaceDark),
                    singleLine = true
                )
                Text(
                    text = "$subject · ${paperStyle.displayName}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Right: Actions (Paper style, Undo, Redo, Clear)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Paper Style Selector
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { showPaperMenu = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = "Paper style",
                            tint = LernspaceDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showPaperMenu,
                    onDismissRequest = { showPaperMenu = false }
                ) {
                    PaperStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.displayName) },
                            onClick = {
                                onSelectPaperStyle(style)
                                showPaperMenu = false
                            }
                        )
                    }
                }
            }

            // Undo
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clickable(enabled = canUndo, onClick = onUndo)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) LernspaceDark else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Redo
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clickable(enabled = canRedo, onClick = onRedo)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) LernspaceDark else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Clear Canvas
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onClearAll)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
