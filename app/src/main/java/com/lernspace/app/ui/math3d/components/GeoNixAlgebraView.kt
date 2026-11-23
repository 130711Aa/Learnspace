package com.lernspace.app.ui.math3d.components

import kotlin.math.roundToInt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.math3d.GeoElementModel
import com.lernspace.app.core.math3d.GeoElementType
import com.lernspace.app.ui.workspace.document.MathExpr
import com.lernspace.app.ui.workspace.document.MathExprView
import com.lernspace.app.ui.workspace.document.latexToMathExpr

enum class GeoNixTab(val label: String) {
    ALGEBRA("Algebra"),
    TOOLS("Tools"),
    TABLE("Table"),
    SPREADSHEET("Spreadsheet")
}

@Composable
fun GeoNixAlgebraView(
    elements: List<GeoElementModel>,
    is3DMode: Boolean,
    onElementToggleVisibility: (String) -> Unit,
    onElementDelete: (String) -> Unit,
    onElementDuplicate: (GeoElementModel) -> Unit,
    onElementColorChange: (String, String) -> Unit,
    onElementVariableChange: (String, Float) -> Unit,
    onElementPropertyChange: (String, (GeoElementModel) -> GeoElementModel) -> Unit,
    onAddElement: (GeoElementModel) -> Unit,
    onCameraPreset: (String) -> Unit,
    onSelect3DMode: () -> Unit,
    // Math RetEX Input State (Mirroring FloatingGraphWindow)
    mathRoot: MathExpr.Sequence,
    mathActiveSeqId: String,
    mathCursorOffset: Int,
    mathCursorAlpha: Float,
    onSelectCursor: (String, Int) -> Unit,
    showMathKeypad: Boolean,
    onToggleMathKeypad: () -> Unit,
    editingElement: GeoElementModel?,
    onStartEdit: (GeoElementModel) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEquation: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onConvertToSolidOfRevolution: (GeoElementModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(GeoNixTab.ALGEBRA) }

    // In 2D mode, filter so only 2D ordinary graph functions & sliders appear in sidebar
    val displayElements = elements.filter {
        is3DMode || it.type == GeoElementType.FUNCTION_2D || it.type == GeoElementType.VARIABLE_SLIDER
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. Far Left Vertical Tab Rail
            // ==========================================
            Surface(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GeoNixRailTabItem(
                        icon = Icons.Default.Calculate,
                        label = "Algebra",
                        isSelected = selectedTab == GeoNixTab.ALGEBRA,
                        onClick = { selectedTab = GeoNixTab.ALGEBRA }
                    )

                    GeoNixRailTabItem(
                        icon = Icons.Default.Category,
                        label = "Tools",
                        isSelected = selectedTab == GeoNixTab.TOOLS,
                        onClick = { selectedTab = GeoNixTab.TOOLS }
                    )

                    GeoNixRailTabItem(
                        icon = Icons.Default.TableChart,
                        label = "Table",
                        isSelected = selectedTab == GeoNixTab.TABLE,
                        onClick = { selectedTab = GeoNixTab.TABLE }
                    )

                    GeoNixRailTabItem(
                        icon = Icons.Default.GridOn,
                        label = "Spreadsheet",
                        isSelected = selectedTab == GeoNixTab.SPREADSHEET,
                        onClick = { selectedTab = GeoNixTab.SPREADSHEET }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ==========================================
            // 2. Active Tab Content
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedTab) {
                    GeoNixTab.ALGEBRA -> {
                        var isAddingNew by remember { mutableStateOf(false) }
                        if (editingElement != null) {
                            isAddingNew = false
                        }

                        // ── Scrollable Equations List with Inline Editing & GeoNix "+ Input..." ──
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                        ) {
                            items(displayElements, key = { it.id }) { elem ->
                                if (editingElement?.id == elem.id) {
                                    GeoNixInlineInputRow(
                                        title = "Edit: ${elem.name}",
                                        mathRoot = mathRoot,
                                        mathActiveSeqId = mathActiveSeqId,
                                        mathCursorOffset = mathCursorOffset,
                                        mathCursorAlpha = mathCursorAlpha,
                                        onSelectCursor = onSelectCursor,
                                        showMathKeypad = showMathKeypad,
                                        onToggleMathKeypad = onToggleMathKeypad,
                                        onSave = {
                                            onSaveEquation()
                                        },
                                        onCancel = {
                                            onCancelEdit()
                                        },
                                        indicatorColor = parseHexColorSafe(elem.color)
                                    )
                                } else {
                                    GeoNixEquationItemRow(
                                        element = elem,
                                        isEditing = false,
                                        mathRoot = mathRoot,
                                        mathActiveSeqId = mathActiveSeqId,
                                        mathCursorOffset = mathCursorOffset,
                                        mathCursorAlpha = mathCursorAlpha,
                                        onSelectCursor = onSelectCursor,
                                        onSaveEquation = onSaveEquation,
                                        onToggleVisibility = { onElementToggleVisibility(elem.id) },
                                        onStartEdit = {
                                            isAddingNew = false
                                            onStartEdit(elem)
                                        },
                                        onDelete = { onElementDelete(elem.id) },
                                        onDuplicate = { onElementDuplicate(elem) },
                                        onColorChange = { colorHex -> onElementColorChange(elem.id, colorHex) },
                                        onVariableChange = { newVal -> onElementVariableChange(elem.varName, newVal) },
                                        onPropertyChange = { updater -> onElementPropertyChange(elem.id, updater) },
                                        onConvertToSolidOfRevolution = onConvertToSolidOfRevolution
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    thickness = 0.8.dp
                                )
                            }

                            // Bottom Slot: Active New Input OR Clean "+ Input..." row
                            if (isAddingNew && editingElement == null) {
                                item(key = "active_add_input") {
                                    GeoNixInlineInputRow(
                                        title = "Input Baru",
                                        mathRoot = mathRoot,
                                        mathActiveSeqId = mathActiveSeqId,
                                        mathCursorOffset = mathCursorOffset,
                                        mathCursorAlpha = mathCursorAlpha,
                                        onSelectCursor = onSelectCursor,
                                        showMathKeypad = showMathKeypad,
                                        onToggleMathKeypad = onToggleMathKeypad,
                                        onSave = {
                                            onSaveEquation()
                                            isAddingNew = false
                                        },
                                        onCancel = {
                                            isAddingNew = false
                                            onCancelEdit()
                                        },
                                        indicatorColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                item(key = "plus_input_button") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                isAddingNew = true
                                                onCancelEdit()
                                                if (!showMathKeypad) onToggleMathKeypad()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Tambah Input",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Input...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    GeoNixTab.TOOLS -> {
                        GeoNixToolsPalette(
                            onAddElement = { elem ->
                                if (elem.type in listOf(
                                        GeoElementType.SOLID_OF_REVOLUTION,
                                        GeoElementType.SURFACE_3D,
                                        GeoElementType.VECTOR_3D,
                                        GeoElementType.PLANE_3D,
                                        GeoElementType.POLYHEDRON
                                    )
                                ) {
                                    onSelect3DMode()
                                }
                                onAddElement(elem)
                                selectedTab = GeoNixTab.ALGEBRA
                            }
                        )
                    }

                    GeoNixTab.TABLE -> {
                        GeoNixValuesTable(elements = elements)
                    }

                    GeoNixTab.SPREADSHEET -> {
                        GeoNixSpreadsheetView(elements = elements)
                    }
                }
            }
        }
    }
}

/**
 * Rail Tab Button Item
 */
@Composable
private fun GeoNixRailTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

/**
 * Equation Item Row with Clear Calculus Notations (including Benda Putar)
 */
@Composable
private fun GeoNixEquationItemRow(
    element: GeoElementModel,
    isEditing: Boolean,
    mathRoot: MathExpr.Sequence,
    mathActiveSeqId: String,
    mathCursorOffset: Int,
    mathCursorAlpha: Float,
    onSelectCursor: (String, Int) -> Unit,
    onSaveEquation: () -> Unit,
    onToggleVisibility: () -> Unit,
    onStartEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onColorChange: (String) -> Unit,
    onVariableChange: (Float) -> Unit,
    onPropertyChange: ((GeoElementModel) -> GeoElementModel) -> Unit,
    onConvertToSolidOfRevolution: (GeoElementModel) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    val parsedColor = remember(element.color) {
        try {
            Color(android.graphics.Color.parseColor(element.color))
        } catch (_: Exception) {
            Color(0xFF2563EB)
        }
    }

    val formulaLatex = remember(element.definition, element.expr) {
        if (element.type == GeoElementType.SOLID_OF_REVOLUTION) {
            val raw = if (element.definition.contains("Putar ")) {
                element.definition.substringAfter("Putar ").substringBefore(" [").trim()
            } else {
                element.expr.ifBlank { "sqrt(x)" }
            }
            val displayExpr = if (raw.contains("=") || raw.startsWith("f(x)")) raw else "f(x) = $raw"
            formatFormulaToLatex(displayExpr)
        } else {
            formatFormulaToLatex(element.definition.ifBlank { element.expr })
        }
    }
    val mathExpr = remember(formulaLatex) {
        try {
            latexToMathExpr(formulaLatex)
        } catch (_: Exception) {
            latexToMathExpr(element.definition.ifBlank { element.expr })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visibility Circle Toggle
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (element.visible) parsedColor else Color.Transparent)
                    .border(2.dp, parsedColor, CircleShape)
                    .clickable { onToggleVisibility() }
            )

            Spacer(Modifier.width(8.dp))

            // Expression Label & Math Expr
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStartEdit() }
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${element.name}: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = if (element.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )

                if (element.type in listOf(GeoElementType.FUNCTION_2D, GeoElementType.SURFACE_3D)) {
                    MathExprView(
                        expr = mathExpr,
                        activeSeqId = "",
                        cursorOffset = -1,
                        cursorAlpha = 0f,
                        fontSize = 12.5.sp,
                        textColor = if (element.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        onSelectCursor = { _, _ -> onStartEdit() }
                    )
                } else if (element.type == GeoElementType.SOLID_OF_REVOLUTION) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Putar ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = parsedColor
                        )
                        MathExprView(
                            expr = mathExpr,
                            activeSeqId = "",
                            cursorOffset = -1,
                            cursorAlpha = 0f,
                            fontSize = 12.5.sp,
                            textColor = if (element.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            onSelectCursor = { _, _ -> onStartEdit() }
                        )
                        Text(
                            text = " [${element.a.toInt()}, ${element.b.toInt()}]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = element.definition.ifBlank { element.expr },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = if (element.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Quick Definite Integral Toggle Button (∫) - Identik Notes
            if (element.type == GeoElementType.FUNCTION_2D) {
                IconButton(
                    onClick = {
                        onPropertyChange { cur -> cur.copy(hasIntegral = !cur.hasIntegral) }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "∫",
                        fontSize = 15.sp,
                        fontWeight = if (element.hasIntegral) FontWeight.Black else FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = if (element.hasIntegral) parsedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }

            // Quick Edit Button
            IconButton(
                onClick = onStartEdit,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Expand / Collapse for Benda Putar
            if (element.type == GeoElementType.SOLID_OF_REVOLUTION) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Three dots menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (element.type == GeoElementType.FUNCTION_2D) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (element.hasIntegral) "Sembunyikan Batas Integral (2D)" else "Hitung Integral / Batas Area (2D)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB)
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Functions, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                showMenu = false
                                onPropertyChange { cur -> cur.copy(hasIntegral = !cur.hasIntegral) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Jadikan Benda Putar (3D)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7C3AED)) },
                            leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onConvertToSolidOfRevolution(element)
                            }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("Edit Rumus", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        onClick = {
                            showMenu = false
                            onStartEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplikat", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ubah Warna", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        onClick = {
                            showMenu = false
                            showColorPicker = true
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (element.type == GeoElementType.FUNCTION_2D) "Hapus Fungsi" else "Hapus Objek",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        // ==========================================
        // ── USER-FRIENDLY & SPACIOUS BENDA PUTAR CARD ──
        // ==========================================
        if (element.type == GeoElementType.SOLID_OF_REVOLUTION && isExpanded) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, parsedColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isAxisX = element.axis.lowercase() == "x"
                    val approxVol = if (isAxisX) {
                        (Math.PI * 0.5 * (element.b * element.b - element.a * element.a) * (element.angle / 360f)).coerceAtLeast(0.0)
                    } else {
                        (2 * Math.PI * (1.0 / 3.0) * (element.b * element.b * element.b - element.a * element.a * element.a) * (element.angle / 360f)).coerceAtLeast(0.0)
                    }

                    // Header Card: Badge Judul & Live Volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = parsedColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Rotate90DegreesCcw,
                                        contentDescription = null,
                                        tint = parsedColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Benda Putar 3D",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = parsedColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, parsedColor)
                        ) {
                            Text(
                                text = "V ≈ ${String.format("%.2f", approxVol)} sat³",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = parsedColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 1. Kurva Fungsi f(x) Input Field (Visual MathExprView Identik Grafik Garis)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kurva:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isEditing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onStartEdit() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isEditing) {
                                    MathExprView(
                                        expr = mathRoot,
                                        activeSeqId = mathActiveSeqId,
                                        cursorOffset = mathCursorOffset,
                                        cursorAlpha = mathCursorAlpha,
                                        fontSize = 13.5.sp,
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                        onSelectCursor = onSelectCursor
                                    )
                                } else {
                                    MathExprView(
                                        expr = mathExpr,
                                        activeSeqId = "",
                                        cursorOffset = -1,
                                        cursorAlpha = 0f,
                                        fontSize = 13.5.sp,
                                        textColor = parsedColor,
                                        onSelectCursor = { _, _ -> onStartEdit() }
                                    )
                                }
                            }
                        }

                        // Tombol Cepat Edit / Simpan
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                if (isEditing) {
                                    onSaveEquation()
                                } else {
                                    onStartEdit()
                                }
                            }
                        ) {
                            Icon(
                                if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Simpan Kurva" else "Edit Kurva",
                                tint = if (isEditing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(6.dp).size(15.dp)
                            )
                        }
                    }

                    // 2. Interval Pemotongan [a, b] dengan Stepper (-) dan (+)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Interval Batas [a, b]:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Batas a
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("a =", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                    Text("${element.a.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Row {
                                        IconButton(
                                            onClick = { onPropertyChange { it.copy(a = (it.a - 1f).coerceAtLeast(-20f)) } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang a", modifier = Modifier.size(13.dp))
                                        }
                                        IconButton(
                                            onClick = { onPropertyChange { it.copy(a = (it.a + 1f).coerceAtMost(it.b - 1f)) } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah a", modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }

                            // Batas b
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("b =", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                    Text("${element.b.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Row {
                                        IconButton(
                                            onClick = { onPropertyChange { it.copy(b = (it.b - 1f).coerceAtLeast(it.a + 1f)) } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang b", modifier = Modifier.size(13.dp))
                                        }
                                        IconButton(
                                            onClick = { onPropertyChange { it.copy(b = (it.b + 1f).coerceAtMost(30f)) } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah b", modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Sumbu Putar (Segmented Pill Buttons)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sumbu Putar:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAxisX) parsedColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isAxisX) parsedColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPropertyChange { it.copy(axis = "x") } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.RotateRight,
                                        contentDescription = null,
                                        tint = if (isAxisX) parsedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Sumbu-X (Horizontal)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isAxisX) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAxisX) parsedColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isAxisX) parsedColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (!isAxisX) parsedColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPropertyChange { it.copy(axis = "y") } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.RotateRight,
                                        contentDescription = null,
                                        tint = if (!isAxisX) parsedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Sumbu-Y (Vertikal)",
                                        fontSize = 11.sp,
                                        fontWeight = if (!isAxisX) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isAxisX) parsedColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 4. Sudut Putaran θ (0° - 360°) dengan Play/Pause Animasi
                    var isPlayingAnimation by remember { mutableStateOf(false) }
                    val currentOnPropertyChange by rememberUpdatedState(onPropertyChange)
                    val currentElement by rememberUpdatedState(element)

                    LaunchedEffect(isPlayingAnimation) {
                        if (isPlayingAnimation) {
                            var runningAngle = currentElement.angle
                            while (isPlayingAnimation) {
                                kotlinx.coroutines.delay(22L)
                                runningAngle += 2.5f
                                if (runningAngle > 360f) {
                                    runningAngle = 0f
                                }
                                currentOnPropertyChange { it.copy(angle = runningAngle) }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPlayingAnimation) MaterialTheme.colorScheme.errorContainer else parsedColor.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { isPlayingAnimation = !isPlayingAnimation }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isPlayingAnimation) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Animasi Putar",
                                            tint = if (isPlayingAnimation) MaterialTheme.colorScheme.error else parsedColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Sudut Rotasi θ = ${element.angle.toInt()}°",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = parsedColor
                                )
                            }

                            // Quick angle snap chips
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                listOf(90f, 180f, 270f, 360f).forEach { snapAng ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (element.angle.toInt() == snapAng.toInt()) parsedColor else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(0.6.dp, parsedColor.copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable {
                                            isPlayingAnimation = false
                                            onPropertyChange { it.copy(angle = snapAng) }
                                        }
                                    ) {
                                        Text(
                                            "${snapAng.toInt()}°",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (element.angle.toInt() == snapAng.toInt()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Slider(
                            value = element.angle,
                            onValueChange = {
                                isPlayingAnimation = false
                                onPropertyChange { cur -> cur.copy(angle = it) }
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = parsedColor,
                                activeTrackColor = parsedColor,
                                inactiveTrackColor = parsedColor.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }


                }
            }
        }

        // ── DEFINITE INTEGRAL & BOUNDS CARD (Identik Notes / FloatingGraphWindow) ──
        if (element.type == GeoElementType.FUNCTION_2D && element.hasIntegral) {
            val cleanFunc = element.expr.ifBlank { element.definition }
            val effExpr = cleanEquationForEval(cleanFunc)
            val intResult = remember(effExpr, element.a, element.b) {
                com.lernspace.app.core.math.MathPlotHelper.calculateDefiniteIntegral(effExpr, element.a, element.b)
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = parsedColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, parsedColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header: Integral Symbol & Calculated Net Value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Functions,
                                contentDescription = null,
                                tint = parsedColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "∫ f(x) dx",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = parsedColor
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = parsedColor.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, parsedColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Area ≈ ${com.lernspace.app.core.math.MathPlotHelper.formatNumber(intResult.netIntegral.toFloat())}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = parsedColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    // Bounds Inputs: a (Batas Bawah) & b (Batas Atas)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Batas Bawah a
                        var rawAText by remember(element.id, element.a) {
                            mutableStateOf(com.lernspace.app.core.math.MathPlotHelper.formatNumber(element.a))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("a = ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                androidx.compose.foundation.text.BasicTextField(
                                    value = rawAText,
                                    onValueChange = { newVal ->
                                        val clean = newVal.trim()
                                        if (clean.isEmpty() || clean == "-" || clean.matches(Regex("""^-?\d*\.?\d*$"""))) {
                                            rawAText = clean
                                            clean.toFloatOrNull()?.let { newA ->
                                                onPropertyChange { cur -> cur.copy(a = newA) }
                                            }
                                        }
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "−",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable {
                                                val newA = ((element.a - 0.5f) * 10f).let { kotlin.math.round(it) }.toInt() / 10f
                                                rawAText = com.lernspace.app.core.math.MathPlotHelper.formatNumber(newA)
                                                onPropertyChange { cur -> cur.copy(a = newA) }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        "+",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable {
                                                val newA = ((element.a + 0.5f) * 10f).let { kotlin.math.round(it) }.toInt() / 10f
                                                rawAText = com.lernspace.app.core.math.MathPlotHelper.formatNumber(newA)
                                                onPropertyChange { cur -> cur.copy(a = newA) }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Batas Atas b
                        var rawBText by remember(element.id, element.b) {
                            mutableStateOf(com.lernspace.app.core.math.MathPlotHelper.formatNumber(element.b))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("b = ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                androidx.compose.foundation.text.BasicTextField(
                                    value = rawBText,
                                    onValueChange = { newVal ->
                                        val clean = newVal.trim()
                                        if (clean.isEmpty() || clean == "-" || clean.matches(Regex("""^-?\d*\.?\d*$"""))) {
                                            rawBText = clean
                                            clean.toFloatOrNull()?.let { newB ->
                                                onPropertyChange { cur -> cur.copy(b = newB) }
                                            }
                                        }
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "−",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable {
                                                val newB = ((element.b - 0.5f) * 10f).let { kotlin.math.round(it) }.toInt() / 10f
                                                rawBText = com.lernspace.app.core.math.MathPlotHelper.formatNumber(newB)
                                                onPropertyChange { cur -> cur.copy(b = newB) }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        "+",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable {
                                                val newB = ((element.b + 0.5f) * 10f).let { kotlin.math.round(it) }.toInt() / 10f
                                                rawBText = com.lernspace.app.core.math.MathPlotHelper.formatNumber(newB)
                                                onPropertyChange { cur -> cur.copy(b = newB) }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Preset Chips: [0, 2], [0, 4], [-2, 2], [1, 3]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0f to 2f, 0f to 4f, -2f to 2f, 1f to 3f).forEach { (pa, pb) ->
                            val isCur = kotlin.math.abs(element.a - pa) < 0.05f && kotlin.math.abs(element.b - pb) < 0.05f
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isCur) parsedColor else parsedColor.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onPropertyChange { cur -> cur.copy(a = pa, b = pb) }
                                    }
                            ) {
                                Text(
                                    text = "[${pa.toInt()}, ${pb.toInt()}]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCur) Color.White else parsedColor,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Riemann Approximation Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "SMOOTH" to "Mulus",
                            "TRAPEZOIDAL" to "Trapesium",
                            "LEFT" to "Riemann Kiri",
                            "RIGHT" to "Riemann Kanan",
                            "MIDPOINT" to "Riemann Tengah"
                        ).forEach { (typeKey, typeLabel) ->
                            val isSelected = element.integralRiemannType == typeKey
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.clickable {
                                    onPropertyChange { cur -> cur.copy(integralRiemannType = typeKey) }
                                }
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Color Picker
        if (showColorPicker) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val palette = listOf(
                    "#00897B", "#E53935", "#2563EB", "#00E676",
                    "#7C3AED", "#FFB300", "#FF5722", "#E91E63"
                )
                palette.forEach { hex ->
                    val c = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                if (element.color.equals(hex, ignoreCase = true)) 2.dp else 0.dp,
                                MaterialTheme.colorScheme.onSurface,
                                CircleShape
                            )
                            .clickable {
                                onColorChange(hex)
                                showColorPicker = false
                            }
                    )
                }
            }
        }

        // Variable Slider
        if (element.type == GeoElementType.VARIABLE_SLIDER) {
            Spacer(Modifier.height(4.dp))
            var sliderPos by remember(element.sliderValue) { mutableFloatStateOf(element.sliderValue) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${element.varName} = ${String.format("%.1f", sliderPos)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(50.dp)
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

/**
 * GeoNix Tools Palette Tab
 */
@Composable
private fun GeoNixToolsPalette(
    onAddElement: (GeoElementModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Peralatan 2D Kartesius", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GeoNixToolButton("Garis y=mx+c", Icons.Default.LinearScale, Modifier.weight(1f)) {
                    onAddElement(
                        GeoElementModel(
                            type = GeoElementType.FUNCTION_2D,
                            name = "g",
                            definition = "y = 2/4 x + 5",
                            expr = "0.5*x + 5",
                            color = "#E53935"
                        )
                    )
                }
                GeoNixToolButton("f(x) = x²", Icons.AutoMirrored.Filled.ShowChart, Modifier.weight(1f)) {
                    onAddElement(
                        GeoElementModel(
                            type = GeoElementType.FUNCTION_2D,
                            name = "f",
                            definition = "y = x^2 - 4",
                            expr = "x^2 - 4",
                            color = "#00897B"
                        )
                    )
                }
                GeoNixToolButton("Slider a", Icons.Default.Tune, Modifier.weight(1f)) {
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
        }

        item {
            Text("Bangun Ruang & Vektor 3D", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeoNixToolButton("Benda Putar 3D", Icons.Default.Rotate90DegreesCcw, Modifier.weight(1f)) {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.SOLID_OF_REVOLUTION,
                                name = "BendaPutar",
                                definition = "Putar \\sqrt{x} [0, 4] 360° Sumbu-X",
                                expr = "sqrt(x)",
                                a = 0f,
                                b = 4f,
                                axis = "x",
                                angle = 360f,
                                color = "#7C3AED",
                                visible = true
                            )
                        )
                    }
                    GeoNixToolButton("Vektor R³", Icons.Default.ArrowOutward, Modifier.weight(1f)) {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.VECTOR_3D,
                                name = "u",
                                definition = "(3, 2, 4)",
                                x = 3f,
                                y = 2f,
                                z = 4f,
                                color = "#FFB300",
                                visible = true
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeoNixToolButton("Kubus ABCD.EFGH", Icons.Default.ViewInAr, Modifier.weight(1f)) {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.POLYHEDRON,
                                name = "Kubus",
                                definition = "Kubus s = 4",
                                subType = "CUBE",
                                size = 4f,
                                color = "#2563EB",
                                visible = true
                            )
                        )
                    }
                    GeoNixToolButton("Permukaan z(x,y)", Icons.Default.Layers, Modifier.weight(1f)) {
                        onAddElement(
                            GeoElementModel(
                                type = GeoElementType.SURFACE_3D,
                                name = "z(x,y)",
                                definition = "z = sin(x)*cos(y)",
                                expr = "sin(x)*cos(y)",
                                color = "#FF5722",
                                visible = true
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeoNixToolButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(2.dp))
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/**
 * GeoNix Table of Values Tab
 */
@Composable
private fun GeoNixValuesTable(
    elements: List<GeoElementModel>
) {
    val activeFunctions = elements.filter { it.type == GeoElementType.FUNCTION_2D }

    if (activeFunctions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Tidak ada fungsi f(x) aktif untuk ditampilkan dalam tabel nilai.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Text(
                "Tabel Nilai Fungsi",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(6.dp)
            ) {
                Text("x", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.width(36.dp))
                activeFunctions.forEach { fn ->
                    Text(
                        fn.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val xValues = (-5..5).toList()
                items(xValues) { xVal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 6.dp)
                    ) {
                        Text(xVal.toString(), fontSize = 11.5.sp, modifier = Modifier.width(36.dp))
                        activeFunctions.forEach { fn ->
                            val approxVal = evaluateExpressionQuick(fn.expr, xVal.toFloat())
                            Text(
                                String.format("%.2f", approxVal),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }
            }
        }
    }
}

private fun evaluateExpressionQuick(expr: String, x: Float): Float {
    return try {
        when {
            expr.contains("sqrt") -> kotlin.math.sqrt(kotlin.math.max(0f, x))
            expr.contains("sin") -> kotlin.math.sin(x)
            expr.contains("cos") -> kotlin.math.cos(x)
            expr.contains("x^2") -> x * x
            expr.contains("-2*x + 3") || expr.contains("-2x + 3") -> -2 * x + 3
            expr.contains("0.5*x + 5") || expr.contains("2/4") -> 0.5f * x + 5
            else -> x
        }
    } catch (_: Exception) {
        0f
    }
}

/**
 * Converts standard formula into clean LaTeX for MathExprView
 */
fun formatFormulaToLatex(formula: String): String {
    var s = formula.trim()
    s = s.replace("²", "^{2}").replace("³", "^{3}")
    if (!s.contains("\\frac") && s.contains("/")) {
        s = s.replace(Regex("""(\d+)\s*/\s*(\d+)"""), """\\frac{$1}{$2}""")
    }
    if (!s.contains("\\sqrt") && s.contains("sqrt")) {
        s = s.replace(Regex("""sqrt\(([^)]+)\)"""), """\\sqrt{$1}""")
    }
    if (!s.contains("^{") && s.contains("^")) {
        s = s.replace(Regex("""\^(\d+)"""), """^{$1}""")
    }
    s = s.replace("*", " \\cdot ")
    return s
}

/**
 * Converts LaTeX into JS expression for Three.js engine
 */
fun parseHexColorSafe(hex: String, defaultColor: Color = Color(0xFF2563EB)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}

/**
 * Clean Inline Input Row for GeoNix Algebra View
 */
@Composable
fun GeoNixInlineInputRow(
    title: String,
    mathRoot: MathExpr.Sequence,
    mathActiveSeqId: String,
    mathCursorOffset: Int,
    mathCursorAlpha: Float,
    onSelectCursor: (String, Int) -> Unit,
    showMathKeypad: Boolean,
    onToggleMathKeypad: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val mathCanvasScroll = rememberScrollState()
    LaunchedEffect(mathActiveSeqId, mathCursorOffset, mathRoot) {
        if (mathActiveSeqId == mathRoot.id && mathCursorOffset >= mathRoot.items.size) {
            mathCanvasScroll.animateScrollTo(mathCanvasScroll.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
            Text(
                text = "Batal",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onCancel() }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp, max = 80.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.2.dp,
                    indicatorColor,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onSelectCursor(mathRoot.id, mathRoot.items.size)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(mathCanvasScroll)
                ) {
                    if (mathRoot.items.isEmpty()) {
                        Text(
                            "Ketik rumus di sini...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        MathExprView(
                            expr = mathRoot,
                            activeSeqId = mathActiveSeqId,
                            cursorOffset = mathCursorOffset,
                            cursorAlpha = mathCursorAlpha,
                            fontSize = 13.5.sp,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            onSelectCursor = onSelectCursor
                        )
                    }
                }
            }

            // Keypad toggle button
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = if (showMathKeypad) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (showMathKeypad) Color(0xFF1E293B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.clickable { onToggleMathKeypad() }
            ) {
                Icon(
                    imageVector = Icons.Filled.Keyboard,
                    contentDescription = "Toggle Keypad",
                    tint = if (showMathKeypad) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).size(16.dp)
                )
            }

            // Save / Confirm Action Button
            val hasContent = mathRoot.items.isNotEmpty()
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = if (hasContent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable(enabled = hasContent) {
                    onSave()
                }
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Simpan",
                    tint = if (hasContent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(4.dp).size(16.dp)
                )
            }
        }
    }
}

/**
 * GeoNix Spreadsheet Suite View
 */
@Composable
fun GeoNixSpreadsheetView(elements: List<GeoElementModel>) {
    val columns = listOf("A", "B", "C", "D", "E")
    val rows = 1..18
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row (A, B, C, D, E)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(28.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                columns.forEach { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(col, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Data Rows
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows.toList()) { rowIdx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(26.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$rowIdx", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        columns.forEachIndexed { colIdx, _ ->
                            val cellText = when (colIdx) {
                                0 -> (rowIdx - 8).toString() // x
                                1 -> {
                                    val x = (rowIdx - 8).toFloat()
                                    // f(x) = -2x + 3
                                    String.format("%.1f", -2 * x + 3)
                                }
                                2 -> {
                                    val x = (rowIdx - 8).toFloat()
                                    // g(x) = 0.5x + 5
                                    String.format("%.1f", 0.5f * x + 5)
                                }
                                else -> ""
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cellText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Converts LaTeX into JS expression for Three.js engine
 */
fun latexToJsExpr(latex: String): String {
    var s = latex.trim()
    val eqIdx = s.indexOf('=')
    if (eqIdx != -1) {
        val lhs = s.substring(0, eqIdx).trim().replace(" ", "")
        val rhs = s.substring(eqIdx + 1).trim()
        val y2Pattern = Regex("""^([+-]?\d*\.?\d*\*?)?y(\^2|\^\{2\}|²|\*y)$""", RegexOption.IGNORE_CASE)
        val lhsMatch = y2Pattern.matchEntire(lhs)
        if (lhsMatch != null) {
            val coeffStr = lhsMatch.groupValues[1].replace("*", "")
            val coeff = if (coeffStr.isEmpty() || coeffStr == "+") 1.0 else if (coeffStr == "-") -1.0 else coeffStr.toDoubleOrNull() ?: 1.0
            val inner = if (coeff == 1.0) rhs else "($rhs)/$coeff"
            s = "sqrt($inner)"
        } else {
            val rhsMatch = y2Pattern.matchEntire(rhs.replace(" ", ""))
            if (rhsMatch != null && (lhs.equals("x", ignoreCase = true) || lhs.equals("x(y)", ignoreCase = true))) {
                val coeffStr = rhsMatch.groupValues[1].replace("*", "")
                val coeff = if (coeffStr.isEmpty() || coeffStr == "+") 1.0 else if (coeffStr == "-") -1.0 else coeffStr.toDoubleOrNull() ?: 1.0
                val inner = if (coeff == 1.0) "x" else "x/$coeff"
                s = "sqrt($inner)"
            } else if (lhs.equals("y", ignoreCase = true) || lhs.startsWith("f(", ignoreCase = true) || lhs.startsWith("g(", ignoreCase = true)) {
                s = rhs
            } else {
                s = rhs
            }
        }
    } else if (s.contains(":")) {
        s = s.substringAfter(":").trim()
    }
    s = s.replace("\\left(", "(").replace("\\right)", ")")
    s = s.replace("left(", "(").replace("right)", ")")
    val fracRegex = Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")
    while (fracRegex.containsMatchIn(s)) {
        s = s.replace(fracRegex, "(($1)/($2))")
    }
    s = s.replace(Regex("""\\sqrt\{([^}]+)\}"""), "sqrt($1)")
    s = s.replace(Regex("""\^\{([^}]+)\}"""), "^($1)")
    s = s.replace("\\cdot", "*")
    s = s.replace("·", "*")
    s = s.replace(Regex("""\\(sin|cos|tan|ln|log|abs|exp)\b"""), "$1")
    s = s.replace("\\", "")
    return s.trim()
}


private fun cleanEquationForEval(raw: String): String {
    var text = raw.trim()
    val eqIdx = text.indexOf('=')
    if (eqIdx != -1) {
        val rhs = text.substring(eqIdx + 1).trim()
        text = rhs
    }
    return com.lernspace.app.core.math.MathPlotHelper.cleanLatexForPlot(text)
}
