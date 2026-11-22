package com.lernspace.app.ui.math3d

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.math3d.GeoElementModel
import com.lernspace.app.core.math3d.GeoElementType
import com.lernspace.app.ui.math3d.components.GeoGebraAlgebraView
import com.lernspace.app.ui.math3d.components.Math3DWebView
import com.lernspace.app.ui.math3d.components.Math3DWebViewController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Math3DLabScreen(
    onNavigateBack: () -> Unit = {},
    onInsertSnapshotToNote: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewController by remember { mutableStateOf<Math3DWebViewController?>(null) }

    // GeoGebra Multi-Object Construction List
    val elements = remember {
        mutableStateListOf(
            GeoElementModel(
                type = GeoElementType.FUNCTION_2D,
                name = "f(x)",
                definition = "y = sqrt(x)",
                expr = "sqrt(x)",
                color = "#00E5FF",
                visible = true
            ),
            GeoElementModel(
                type = GeoElementType.SOLID_OF_REVOLUTION,
                name = "BendaPutar",
                definition = "Putar f(x) [0, 4] 360° Sumbu-X",
                expr = "sqrt(x)",
                a = 0f,
                b = 4f,
                axis = "x",
                angle = 360f,
                color = "#7C3AED",
                visible = true
            ),
            GeoElementModel(
                type = GeoElementType.VECTOR_3D,
                name = "u",
                definition = "u = (3, 2, 4)",
                x = 3f,
                y = 2f,
                z = 4f,
                color = "#FFD600",
                visible = true
            ),
            GeoElementModel(
                type = GeoElementType.POINT_3D,
                name = "A",
                definition = "A(2, 3, 2)",
                x = 2f,
                y = 3f,
                z = 2f,
                color = "#FF5722",
                visible = true
            )
        )
    }

    // Sync elements list with WebGL Engine whenever items change
    fun syncWithEngine() {
        webViewController?.setElements(elements.toList())
    }

    LaunchedEffect(elements.size) {
        syncWithEngine()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GeoGebra 3D Math Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("Multi-Object Construction Engine", fontSize = 10.sp) }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { webViewController?.setCameraView("reset") }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Kamera")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: GeoGebra Dynamic Algebra View
                GeoGebraAlgebraView(
                    elements = elements,
                    onElementToggleVisibility = { id ->
                        val index = elements.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val current = elements[index]
                            elements[index] = current.copy(visible = !current.visible)
                            syncWithEngine()
                        }
                    },
                    onElementDelete = { id ->
                        elements.removeAll { it.id == id }
                        syncWithEngine()
                    },
                    onElementVariableChange = { varName, newVal ->
                        webViewController?.setVariable(varName, newVal)
                    },
                    onAddElement = { newElem ->
                        elements.add(newElem)
                        syncWithEngine()
                    },
                    onExportSnapshot = {
                        webViewController?.captureSnapshot()
                    },
                    onCameraPreset = { preset ->
                        webViewController?.setCameraView(preset)
                    }
                )

                // Right Area: Interactive 3D WebGL Graphics Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Math3DWebView(
                        onControllerReady = { controller ->
                            webViewController = controller
                            controller.setElements(elements.toList())
                        },
                        onMetricsUpdated = { _ -> },
                        onSnapshotReady = { bitmap ->
                            if (onInsertSnapshotToNote != null) {
                                onInsertSnapshotToNote(bitmap)
                                Toast.makeText(context, "Snapshot 3D berhasil disisipkan ke Catatan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Snapshot 3D berhasil diambil (${bitmap.width}x${bitmap.height} px)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}
