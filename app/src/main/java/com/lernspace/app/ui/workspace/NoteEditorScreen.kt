package com.lernspace.app.ui.workspace

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showPaperMenu by remember { mutableStateOf(false) }

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
                        imageElements.add(
                            ImageElement(
                                x = 120f,
                                y = 140f,
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

    // Erase helper function
    fun eraseNear(touchX: Float, touchY: Float, radius: Float = 36f) {
        val radiusSquared = radius * radius
        strokes.removeAll { stroke ->
            val inputs = stroke.inputs
            var hit = false
            val count = inputs.size
            for (i in 0 until count) {
                val pt = inputs.get(i)
                val dx = pt.x - touchX
                val dy = pt.y - touchY
                if ((dx * dx + dy * dy) <= radiusSquared) {
                    hit = true
                    break
                }
            }
            hit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Paper Background (Ruled, Grid, Dotted, Blank)
        PaperBackground(style = paperStyle)

        // 2. Gesture Detector for Canvas (Deselect or Place Text Box on Tap)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool) {
                    detectTapGestures { offset ->
                        if (activeTool == NoteTool.TEXT) {
                            deselectAll()
                            textBoxes.add(
                                TextBoxElement(
                                    x = offset.x - 80f,
                                    y = offset.y - 30f,
                                    width = 240f,
                                    height = 100f,
                                    text = "",
                                    isSelected = true
                                )
                            )
                        } else {
                            // Tapping blank canvas deselects any active text boxes/images
                            deselectAll()
                        }
                    }
                }
        )

        // 3. Placed Images Layer
        imageElements.forEachIndexed { index, img ->
            DraggableImage(
                image = img,
                onMove = { newX, newY ->
                    img.x = newX
                    img.y = newY
                },
                onResize = { newW, newH ->
                    img.width = newW
                    img.height = newH
                },
                onSelect = {
                    deselectAll()
                    imageElements[index] = img.copy(isSelected = true)
                },
                onDelete = { imageElements.remove(img) }
            )
        }

        // 4. Inking Layer (AndroidX Ink)
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
            onErase = { x, y -> eraseNear(x, y) },
            modifier = Modifier.fillMaxSize()
        )

        // 5. Placed Draggable Text Boxes Layer (MS Word Style)
        textBoxes.forEachIndexed { index, boxElement ->
            DraggableTextBox(
                box = boxElement,
                onMove = { newX, newY ->
                    boxElement.x = newX
                    boxElement.y = newY
                },
                onResize = { newW, newH ->
                    boxElement.width = newW
                    boxElement.height = newH
                },
                onTextChange = { newText ->
                    boxElement.text = newText
                },
                onSelect = {
                    deselectAll()
                    textBoxes[index] = boxElement.copy(isSelected = true)
                },
                onDelete = { textBoxes.remove(boxElement) }
            )
        }

        // 6. Top Header Navigation Bar
        Row(
            modifier = Modifier
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
                        value = noteTitle,
                        onValueChange = {
                            noteTitle = it
                            note.title = it
                        },
                        textStyle = TextStyle(
                            color = LernspaceDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(LernspaceDark),
                        singleLine = true
                    )
                    Text(
                        text = "${note.subject} · ${paperStyle.displayName}",
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
                                    paperStyle = style
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
                        .clickable(enabled = strokes.isNotEmpty()) {
                            if (strokes.isNotEmpty()) {
                                val removed = strokes.removeLast()
                                undoneStrokes.add(removed)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (strokes.isNotEmpty()) LernspaceDark else Color(0xFFCBD5E1),
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
                        .clickable(enabled = undoneStrokes.isNotEmpty()) {
                            if (undoneStrokes.isNotEmpty()) {
                                val restored = undoneStrokes.removeLast()
                                strokes.add(restored)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (undoneStrokes.isNotEmpty()) LernspaceDark else Color(0xFFCBD5E1),
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
                        .clickable {
                            strokes.clear()
                            undoneStrokes.clear()
                            textBoxes.clear()
                            imageElements.clear()
                        }
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

        // 7. Floating Pill Toolbar (Top Center)
        NoteFloatingToolbar(
            activeTool = activeTool,
            onSelectTool = { tool ->
                activeTool = tool
                if (tool == NoteTool.TEXT) {
                    // If no box is selected, create a new Word-style box in the visible workspace
                    if (textBoxes.none { it.isSelected }) {
                        deselectAll()
                        textBoxes.add(
                            TextBoxElement(
                                x = 200f,
                                y = 200f,
                                width = 260f,
                                height = 110f,
                                text = "",
                                isSelected = true
                            )
                        )
                    }
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
    }
}
