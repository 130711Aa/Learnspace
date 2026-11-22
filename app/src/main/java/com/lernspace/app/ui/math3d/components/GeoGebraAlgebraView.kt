package com.lernspace.app.ui.math3d.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.math3d.GeoElementModel
import com.lernspace.app.core.math3d.GeoElementType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoGebraAlgebraView(
    elements: List<GeoElementModel>,
    onElementToggleVisibility: (String) -> Unit,
    onElementDelete: (String) -> Unit,
    onElementVariableChange: (String, Float) -> Unit,
    onAddElement: (GeoElementModel) -> Unit,
    onExportSnapshot: () -> Unit,
    onCameraPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawInputText by remember { mutableStateOf("") }
    var showMathKeyboard by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Title & Preset View Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Functions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tampilan Aljabar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text("${elements.size} Objek Aktif", fontSize = 11.sp) }
                    )
                }

                // Camera Views Bar (GeoGebra 3D Orientation Shortcuts)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val views = listOf(
                        "iso" to "Isometrik",
                        "top" to "Atas (XY)",
                        "front" to "Depan",
                        "side" to "Samping"
                    )
                    views.forEach { (preset, label) ->
                        OutlinedButton(
                            onClick = { onCameraPreset(preset) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Dynamic GeoGebra Construction Element List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                if (elements.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Belum ada objek matematika",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Gunakan tombol '+ Tambah Objek' atau ketik rumus fungsi pada kolom di bawah.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(elements, key = { it.id }) { elem ->
                    GeoGebraElementRow(
                        element = elem,
                        onToggleVisibility = { onElementToggleVisibility(elem.id) },
                        onDelete = { onElementDelete(elem.id) },
                        onVariableChange = { newVal -> onElementVariableChange(elem.varName, newVal) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Input Bar & Quick Add Menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick Add Templates Buttons (Chips row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { showAddMenu = !showAddMenu },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Objek", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    QuickTemplateChip("f(x) Fungsi 2D") {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.FUNCTION_2D,
                                name = "f(x)",
                                definition = "x^2 - 4",
                                expr = "x^2 - 4",
                                color = "#00E5FF"
                            )
                        )
                    }
                    QuickTemplateChip("Benda Putar 3D") {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.SOLID_OF_REVOLUTION,
                                name = "BendaPutar",
                                definition = "Putar f(x) [0, 4] 360°",
                                expr = "sqrt(x)",
                                a = 0f,
                                b = 4f,
                                axis = "x",
                                angle = 360f,
                                color = "#7C3AED"
                            )
                        )
                    }
                    QuickTemplateChip("z=f(x,y) Permukaan") {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.SURFACE_3D,
                                name = "z(x,y)",
                                definition = "sin(x)*cos(y)",
                                expr = "sin(x)*cos(y)",
                                color = "#FF5722"
                            )
                        )
                    }
                    QuickTemplateChip("Vektor u") {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.VECTOR_3D,
                                name = "u",
                                definition = "(3, 2, 4)",
                                x = 3f,
                                y = 2f,
                                z = 4f,
                                color = "#FFD600"
                            )
                        )
                    }
                    QuickTemplateChip("Kubus") {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.POLYHEDRON,
                                name = "Kubus",
                                definition = "Kubus ABCD.EFGH s = 4",
                                subType = "CUBE",
                                size = 4f,
                                color = "#3F51B5"
                            )
                        )
                    }
                    QuickTemplateChip("Slider a") {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.VARIABLE_SLIDER,
                                name = "a",
                                definition = "a = 2.0",
                                varName = "a",
                                sliderValue = 2f,
                                sliderMin = -5f,
                                sliderMax = 5f
                            )
                        )
                    }
                }

                // Dropdown template sheet if clicked "Tambah Objek"
                if (showAddMenu) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Pilih Template Geometri:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        onAddElement(GeoElementModel(type = GeoElementType.FUNCTION_2D, name = "f(x)", definition = "sin(x)", expr = "sin(x)", color = "#00E5FF"))
                                        showAddMenu = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("y = sin(x)", fontSize = 11.sp) }
                                TextButton(
                                    onClick = {
                                        onAddElement(GeoElementModel(type = GeoElementType.PLANE_3D, name = "p1", definition = "x + y + z = 4", coeffA = 1f, coeffB = 1f, coeffC = 1f, coeffD = 4f, color = "#00E676"))
                                        showAddMenu = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Bidang 3D", fontSize = 11.sp) }
                                TextButton(
                                    onClick = {
                                        onAddElement(GeoElementModel(type = GeoElementType.POINT_3D, name = "A", definition = "(2, 3, 4)", x = 2f, y = 3f, z = 4f, color = "#FFEB3B"))
                                        showAddMenu = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Titik (2,3,4)", fontSize = 11.sp) }
                            }
                        }
                    }
                }

                // Expression Text Field with Keyboard Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextField(
                        value = rawInputText,
                        onValueChange = { rawInputText = it },
                        placeholder = { Text("Ketik rumus: misal x^2 - 4 atau u=(1,2,3)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (rawInputText.isNotBlank()) {
                                    parseAndAddInput(rawInputText, onAddElement)
                                    rawInputText = ""
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    IconButton(
                        onClick = { showMathKeyboard = !showMathKeyboard },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (showMathKeyboard) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Math Keypad", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            if (rawInputText.isNotBlank()) {
                                parseAndAddInput(rawInputText, onAddElement)
                                rawInputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Math Virtual Keypad (GeoGebra Layout)
                AnimatedVisibility(visible = showMathKeyboard) {
                    GeoGebraMathKeypad(
                        onInsertChar = { char ->
                            rawInputText += char
                        },
                        onBackspace = {
                            if (rawInputText.isNotEmpty()) {
                                rawInputText = rawInputText.dropLast(1)
                            }
                        },
                        onClear = { rawInputText = "" }
                    )
                }

                // Tempel Snapshot Button
                Button(
                    onClick = onExportSnapshot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tempel Snapshot ke Catatan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickTemplateChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        shape = RoundedCornerShape(8.dp)
    )
}

/**
 * GeoGebra Algebra View Row Item
 */
@Composable
private fun GeoGebraElementRow(
    element: GeoElementModel,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
    onVariableChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (element.visible) Color(android.graphics.Color.parseColor(element.color)).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // GeoGebra Colored Radio Button (Toggle Visibility)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val colorObj = Color(android.graphics.Color.parseColor(element.color))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (element.visible) colorObj else Color.Transparent)
                            .border(2.dp, colorObj, CircleShape)
                            .clickable { onToggleVisibility() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = element.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (element.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = element.definition,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            // Real-time Slider for Variables
            if (element.type == GeoElementType.VARIABLE_SLIDER) {
                Spacer(Modifier.height(4.dp))
                var sliderPos by remember(element.sliderValue) { mutableFloatStateOf(element.sliderValue) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${element.varName} = ${String.format("%.1f", sliderPos)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(60.dp)
                    )
                    Slider(
                        value = sliderPos,
                        onValueChange = {
                            sliderPos = it
                            onVariableChange(it)
                        },
                        valueRange = element.sliderMin..element.sliderMax,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * GeoGebra Virtual Math Keyboard
 */
@Composable
private fun GeoGebraMathKeypad(
    onInsertChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val row1 = listOf("x", "y", "z", "+", "-", "×", "÷", "^", "√")
            val row2 = listOf("7", "8", "9", "(", ")", "=", "π", "sin", "cos")
            val row3 = listOf("4", "5", "6", "tan", "abs", "ln", "a", "b", "t")
            val row4 = listOf("1", "2", "3", "0", ".", ",")

            KeypadRow(row1, onInsertChar)
            KeypadRow(row2, onInsertChar)
            KeypadRow(row3, onInsertChar)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row4.forEach { key ->
                    KeypadButton(key, modifier = Modifier.weight(1f)) { onInsertChar(key) }
                }
                KeypadButton("⌫", modifier = Modifier.weight(1f), isAction = true) { onBackspace() }
                KeypadButton("C", modifier = Modifier.weight(1f), isAction = true) { onClear() }
            }
        }
    }
}

@Composable
private fun KeypadRow(keys: List<String>, onInsert: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        keys.forEach { key ->
            KeypadButton(key, modifier = Modifier.weight(1f)) {
                val mapped = when (key) {
                    "×" -> "*"
                    "÷" -> "/"
                    "√" -> "sqrt("
                    "sin" -> "sin("
                    "cos" -> "cos("
                    "tan" -> "tan("
                    "abs" -> "abs("
                    "ln" -> "ln("
                    else -> key
                }
                onInsert(mapped)
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isAction) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAction) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Intelligent Smart Parser for Input Bar
 */
private fun parseAndAddInput(input: String, onAdd: (GeoElementModel) -> Unit) {
    val clean = input.trim()

    // 1. Vector: u = (1, 2, 3) or (1, 2, 3)
    val vecMatch = Regex("""(?:([a-zA-Z]+)\s*=\s*)?\((-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\)""").find(clean)
    if (vecMatch != null) {
        val name = vecMatch.groupValues[1].ifEmpty { "u" }
        val vx = vecMatch.groupValues[2].toFloatOrNull() ?: 0f
        val vy = vecMatch.groupValues[3].toFloatOrNull() ?: 0f
        val vz = vecMatch.groupValues[4].toFloatOrNull() ?: 0f
        onAdd(
            GeoElementModel(
                type = GeoElementType.VECTOR_3D,
                name = name,
                definition = "($vx, $vy, $vz)",
                x = vx,
                y = vy,
                z = vz,
                color = "#00E5FF"
            )
        )
        return
    }

    // 2. Variable slider: a = 3
    val varMatch = Regex("""^([a-zA-Z])\s*=\s*(-?\d+(?:\.\d+)?)$""").find(clean)
    if (varMatch != null) {
        val vName = varMatch.groupValues[1]
        val vVal = varMatch.groupValues[2].toFloatOrNull() ?: 1f
        onAdd(
            GeoElementModel(
                type = GeoElementType.VARIABLE_SLIDER,
                name = vName,
                definition = "$vName = $vVal",
                varName = vName,
                sliderValue = vVal,
                sliderMin = vVal - 5f,
                sliderMax = vVal + 5f
            )
        )
        return
    }

    // 3. 3D Plane: ax + by + cz = d
    if (clean.contains("z") && clean.contains("=") && !clean.startsWith("z=")) {
        onAdd(
            GeoElementModel(
                type = GeoElementType.PLANE_3D,
                name = "p",
                definition = clean,
                coeffA = 1f,
                coeffB = 1f,
                coeffC = 1f,
                coeffD = 3f,
                color = "#00E676"
            )
        )
        return
    }

    // 4. 3D Surface: z = f(x, y)
    if (clean.startsWith("z=") || clean.startsWith("z =") || clean.contains("y")) {
        val expr = clean.replace(Regex("""^z\s*=\s*"""), "")
        onAdd(
            GeoElementModel(
                type = GeoElementType.SURFACE_3D,
                name = "z(x,y)",
                definition = "z = $expr",
                expr = expr,
                color = "#FF5722"
            )
        )
        return
    }

    // 5. Default: 2D Curve f(x)
    val expr = clean.replace(Regex("""^[yYfF]\s*(?:\(x\))?\s*=\s*"""), "")
    onAdd(
        GeoElementModel(
            type = GeoElementType.FUNCTION_2D,
            name = "f(x)",
            definition = "y = $expr",
            expr = expr,
            color = "#00E5FF"
        )
    )
}
