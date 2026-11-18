package com.lernspace.app.ui.workspace.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.components.MarkdownLatexContent

data class MathSymbolItem(
    val displayLabel: String,
    val insertSnippet: String,
    val description: String = ""
)

enum class MathKeyboardCategory(val title: String) {
    FRACTIONS_POWERS("Pecahan & Pangkat"),
    RELATIONS("Simbol & Relasi"),
    CALCULUS("Kalkulus & Aljabar"),
    GREEK("Huruf Yunani"),
    PRESETS("Rumus Populer")
}

@Composable
fun VisualMathEquationDialog(
    initialEquation: String = "",
    onDismiss: () -> Unit,
    onInsertEquation: (displayEquation: String) -> Unit
) {
    var equationText by remember { mutableStateOf(initialEquation) }
    var selectedCategory by remember { mutableStateOf(MathKeyboardCategory.FRACTIONS_POWERS) }

    val symbolsFractions = listOf(
        MathSymbolItem("a/b", "\\frac{a}{b}", "Pecahan"),
        MathSymbolItem("x²", "^2", "Kuadrat"),
        MathSymbolItem("xⁿ", "^{n}", "Pangkat n"),
        MathSymbolItem("√x", "\\sqrt{x}", "Akar Kuadrat"),
        MathSymbolItem("ⁿ√x", "\\sqrt[n]{x}", "Akar n"),
        MathSymbolItem("|x|", "|x|", "Nilai Mutlak"),
        MathSymbolItem("x₁", "_{1}", "Subskrip"),
        MathSymbolItem("( )", "(x)", "Kurung"),
        MathSymbolItem("[ ]", "[x]", "Kurung Siku"),
        MathSymbolItem("{ }", "\\{x\\}", "Kurung Kurawal")
    )

    val symbolsRelations = listOf(
        MathSymbolItem("±", "\\pm ", "Plus-Minus"),
        MathSymbolItem("×", "\\times ", "Kali"),
        MathSymbolItem("÷", "\\div ", "Bagi"),
        MathSymbolItem("·", "\\cdot ", "Titik Kali"),
        MathSymbolItem("≠", "\\neq ", "Tidak Sama"),
        MathSymbolItem("≈", "\\approx ", "Mendekati"),
        MathSymbolItem("≤", "\\le ", "Kurang Dari Sama Dengan"),
        MathSymbolItem("≥", "\\ge ", "Lebih Dari Sama Dengan"),
        MathSymbolItem("∞", "\\infty ", "Tak Hingga"),
        MathSymbolItem("%", "\\% ", "Persen")
    )

    val symbolsCalculus = listOf(
        MathSymbolItem("∫", "\\int ", "Integral"),
        MathSymbolItem("∫ₐᵇ", "\\int_{a}^{b} ", "Integral Tertentu"),
        MathSymbolItem("∑", "\\sum_{i=1}^{n} ", "Sigma / Penjumlahan"),
        MathSymbolItem("∏", "\\prod_{i=1}^{n} ", "Produk"),
        MathSymbolItem("lim", "\\lim_{x \\to 0} ", "Limit"),
        MathSymbolItem("d/dx", "\\frac{d}{dx}", "Turunan"),
        MathSymbolItem("∂", "\\partial ", "Turunan Parsial"),
        MathSymbolItem("∇", "\\nabla ", "Nabla / Del"),
        MathSymbolItem("sin", "\\sin(x)", "Sinus"),
        MathSymbolItem("cos", "\\cos(x)", "Cosinus"),
        MathSymbolItem("tan", "\\tan(x)", "Tangen"),
        MathSymbolItem("log", "\\log(x)", "Logaritma"),
        MathSymbolItem("ln", "\\ln(x)", "Logaritma Natural")
    )

    val symbolsGreek = listOf(
        MathSymbolItem("π", "\\pi ", "Pi"),
        MathSymbolItem("θ", "\\theta ", "Theta"),
        MathSymbolItem("α", "\\alpha ", "Alpha"),
        MathSymbolItem("β", "\\beta ", "Beta"),
        MathSymbolItem("γ", "\\gamma ", "Gamma"),
        MathSymbolItem("λ", "\\lambda ", "Lambda"),
        MathSymbolItem("Δ", "\\Delta ", "Delta"),
        MathSymbolItem("μ", "\\mu ", "Mu / Mikro"),
        MathSymbolItem("σ", "\\sigma ", "Sigma Kecil"),
        MathSymbolItem("ω", "\\omega ", "Omega"),
        MathSymbolItem("φ", "\\phi ", "Phi"),
        MathSymbolItem("ρ", "\\rho ", "Rho / Massa Jenis")
    )

    val presets = listOf(
        MathSymbolItem("Pythagoras", "a^2 + b^2 = c^2", "Teorema Pythagoras"),
        MathSymbolItem("Rumus ABC", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}", "Akar Persamaan Kuadrat"),
        MathSymbolItem("Energi Einstein", "E = mc^2", "Ekuivalensi Massa-Energi"),
        MathSymbolItem("Luas Lingkaran", "L = \\pi r^2", "Geometri Lingkaran"),
        MathSymbolItem("Hukum II Newton", "\\sum F = m \\cdot a", "Fisika Dinamika"),
        MathSymbolItem("Gaya Gravitasi", "F = G \\frac{m_1 m_2}{r^2}", "Gravitasi Universal")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 480.dp, max = 560.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            shape = CircleShape,
                            color = AccentBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Functions,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Papan Ketik Rumus Matematika",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = "Klik simbol untuk menyusun rumus tanpa perlu coding LaTeX",
                                fontSize = 11.5.sp,
                                color = LernspaceTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Clear, contentDescription = "Tutup", tint = LernspaceTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // ── LIVE FORMULA PREVIEW BOX ──
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 130.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (equationText.isBlank()) {
                            Text(
                                text = "Pratinjau rumus akan muncul di sini secara visual...",
                                fontSize = 13.sp,
                                color = LernspaceTextSecondary.copy(alpha = 0.6f)
                            )
                        } else {
                            MarkdownLatexContent(
                                markdown = "$$ ${equationText.trim()} $$",
                                baseFontSize = 18.sp,
                                textColor = LernspaceDark,
                                accentColor = AccentBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Raw Code Input & Clear/Backspace Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = equationText,
                        onValueChange = { equationText = it },
                        placeholder = { Text("Rumus...", fontSize = 12.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = LernspaceDark
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = LernspaceBorder,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (equationText.isNotEmpty()) {
                                equationText = equationText.dropLast(1)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Backspace, contentDescription = "Hapus 1 karakter", tint = LernspaceTextSecondary, modifier = Modifier.size(18.dp))
                    }

                    OutlinedButton(
                        onClick = { equationText = "" },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Reset", fontSize = 11.5.sp, color = LernspaceTextSecondary)
                    }
                }

                // Category Chips
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MathKeyboardCategory.entries.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = {
                                Text(
                                    text = cat.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == cat) AccentBlue else LernspaceTextSecondary
                                )
                            }
                        )
                    }
                }

                // Keypad Grid
                val currentSymbols = when (selectedCategory) {
                    MathKeyboardCategory.FRACTIONS_POWERS -> symbolsFractions
                    MathKeyboardCategory.RELATIONS -> symbolsRelations
                    MathKeyboardCategory.CALCULUS -> symbolsCalculus
                    MathKeyboardCategory.GREEK -> symbolsGreek
                    MathKeyboardCategory.PRESETS -> presets
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (selectedCategory == MathKeyboardCategory.PRESETS) 2 else 5),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(currentSymbols) { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (selectedCategory == MathKeyboardCategory.PRESETS) 46.dp else 40.dp)
                                .clickable {
                                    equationText = if (selectedCategory == MathKeyboardCategory.PRESETS) {
                                        item.insertSnippet
                                    } else {
                                        equationText + item.insertSnippet
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = item.displayLabel,
                                        fontSize = if (selectedCategory == MathKeyboardCategory.PRESETS) 12.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceDark
                                    )
                                    if (selectedCategory == MathKeyboardCategory.PRESETS && item.description.isNotEmpty()) {
                                        Text(
                                            text = item.description,
                                            fontSize = 9.sp,
                                            color = LernspaceTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = LernspaceTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (equationText.isNotBlank()) {
                                onInsertEquation("$$ ${equationText.trim()} $$")
                                onDismiss()
                            }
                        },
                        enabled = equationText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Sisipkan ke Dokumen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
