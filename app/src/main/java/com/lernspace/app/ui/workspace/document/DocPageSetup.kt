package com.lernspace.app.ui.workspace.document

import android.print.PrintAttributes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.ui.theme.*
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

// ── PAPER SIZES (MM -> 96 DPI PX & OOXML TWIPS) ───────────────────────────────
enum class DocPaperSize(
    val label: String,
    val subtitle: String,
    val widthMm: Double,
    val heightMm: Double
) {
    A4("A4", "210 × 297 mm", 210.0, 297.0),
    F4("F4 / Folio", "215 × 330 mm", 215.0, 330.0),
    LETTER("Letter", "216 × 279 mm", 215.9, 279.4),
    LEGAL("Legal", "216 × 356 mm", 215.9, 355.6);

    fun toPrintMediaSize(isLandscape: Boolean): PrintAttributes.MediaSize {
        val base = when (this) {
            A4 -> PrintAttributes.MediaSize.ISO_A4
            LETTER -> PrintAttributes.MediaSize.NA_LETTER
            LEGAL -> PrintAttributes.MediaSize.NA_LEGAL
            F4 -> PrintAttributes.MediaSize("ISO_F4_FOLIO", "F4 / Folio", 8465, 12992)
        }
        return if (isLandscape) base.asLandscape() else base.asPortrait()
    }
}

enum class DocPageOrientation(val label: String) {
    PORTRAIT("Potret"),
    LANDSCAPE("Lanskap")
}

enum class DocMarginPreset(
    val label: String,
    val desc: String,
    val topCm: Float,
    val bottomCm: Float,
    val leftCm: Float,
    val rightCm: Float
) {
    NORMAL("Normal", "2.54 cm", 2.54f, 2.54f, 2.54f, 2.54f),
    SKRIPSI("Skripsi", "4-4-3-3 cm", 4.0f, 3.0f, 4.0f, 3.0f),
    NARROW("Sempit", "1.27 cm", 1.27f, 1.27f, 1.27f, 1.27f),
    MODERATE("Sedang", "2.5 × 1.9 cm", 2.54f, 2.54f, 1.91f, 1.91f),
    WIDE("Lebar", "2.5 × 3.8 cm", 2.54f, 2.54f, 3.81f, 3.81f),
    CUSTOM("Kustom", "Atur sendiri", 2.54f, 2.54f, 2.54f, 2.54f)
}

data class DocPageSetup(
    val paperSize: DocPaperSize = DocPaperSize.A4,
    val orientation: DocPageOrientation = DocPageOrientation.PORTRAIT,
    val marginPreset: DocMarginPreset = DocMarginPreset.NORMAL,
    val marginTopCm: Float = 2.54f,
    val marginBottomCm: Float = 2.54f,
    val marginLeftCm: Float = 2.54f,
    val marginRightCm: Float = 2.54f,
    val isPageless: Boolean = false,
    val showPageNumbers: Boolean = true,
    val headerText: String = "",
    val footerText: String = ""
) {
    val effectiveWidthMm: Double
        get() = if (orientation == DocPageOrientation.PORTRAIT) paperSize.widthMm else paperSize.heightMm

    val effectiveHeightMm: Double
        get() = if (orientation == DocPageOrientation.PORTRAIT) paperSize.heightMm else paperSize.widthMm

    // 96 DPI CSS pixels: 1 mm = 96 / 25.4 px ≈ 3.7795 px
    val widthPx: Int get() = (effectiveWidthMm * 96.0 / 25.4).roundToInt()
    val heightPx: Int get() = (effectiveHeightMm * 96.0 / 25.4).roundToInt()

    // 1 cm = 96 / 2.54 px ≈ 37.795 px
    val marginTopPx: Int get() = (marginTopCm * 96.0 / 2.54).roundToInt().coerceIn(16, 260)
    val marginBottomPx: Int get() = (marginBottomCm * 96.0 / 2.54).roundToInt().coerceIn(16, 260)
    val marginLeftPx: Int get() = (marginLeftCm * 96.0 / 2.54).roundToInt().coerceIn(16, 260)
    val marginRightPx: Int get() = (marginRightCm * 96.0 / 2.54).roundToInt().coerceIn(16, 260)

    // OOXML twips (1 inch = 1440 twips, 1 mm = 56.6929 twips, 1 cm = 566.929 twips)
    val widthTwips: Int get() = (effectiveWidthMm * 1440.0 / 25.4).roundToInt()
    val heightTwips: Int get() = (effectiveHeightMm * 1440.0 / 25.4).roundToInt()
    val marginTopTwips: Int get() = (marginTopCm * 1440.0 / 2.54).roundToInt().coerceIn(280, 4000)
    val marginBottomTwips: Int get() = (marginBottomCm * 1440.0 / 2.54).roundToInt().coerceIn(280, 4000)
    val marginLeftTwips: Int get() = (marginLeftCm * 1440.0 / 2.54).roundToInt().coerceIn(280, 4000)
    val marginRightTwips: Int get() = (marginRightCm * 1440.0 / 2.54).roundToInt().coerceIn(280, 4000)

    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("size", paperSize.name)
        obj.put("orient", orientation.name)
        obj.put("preset", marginPreset.name)
        obj.put("mT", marginTopCm.toDouble())
        obj.put("mB", marginBottomCm.toDouble())
        obj.put("mL", marginLeftCm.toDouble())
        obj.put("mR", marginRightCm.toDouble())
        obj.put("pageless", isPageless)
        obj.put("pageNum", showPageNumbers)
        obj.put("hdr", headerText)
        obj.put("ftr", footerText)
        return obj.toString()
    }

    fun toJsConfigJson(): String {
        val obj = JSONObject()
        obj.put("widthPx", widthPx)
        obj.put("heightPx", heightPx)
        obj.put("widthMm", effectiveWidthMm)
        obj.put("heightMm", effectiveHeightMm)
        obj.put("mTopPx", marginTopPx)
        obj.put("mBottomPx", marginBottomPx)
        obj.put("mLeftPx", marginLeftPx)
        obj.put("mRightPx", marginRightPx)
        obj.put("mTopCm", marginTopCm.toDouble())
        obj.put("mBottomCm", marginBottomCm.toDouble())
        obj.put("mLeftCm", marginLeftCm.toDouble())
        obj.put("mRightCm", marginRightCm.toDouble())
        obj.put("pageless", isPageless)
        obj.put("showPageNumbers", showPageNumbers)
        obj.put("headerText", headerText)
        obj.put("footerText", footerText)
        obj.put("sizeLabel", paperSize.label)
        return obj.toString()
    }

    companion object {
        private val META_REGEX = Regex("^<!--LSPAGE:(\\{.*?\\})-->\\s*", RegexOption.DOT_MATCHES_ALL)

        fun extractFromContent(rawContent: String): Pair<DocPageSetup, String> {
            val match = META_REGEX.find(rawContent) ?: return DocPageSetup() to rawContent
            val jsonStr = match.groupValues[1]
            val body = rawContent.substring(match.range.last + 1)
            return fromJsonString(jsonStr) to body
        }

        fun packWithContent(setup: DocPageSetup, bodyHtml: String): String {
            val cleanBody = META_REGEX.replaceFirst(bodyHtml, "")
            return "<!--LSPAGE:${setup.toJsonString()}-->$cleanBody"
        }

        fun fromJsonString(jsonStr: String): DocPageSetup {
            return try {
                val obj = JSONObject(jsonStr)
                val size = DocPaperSize.entries.firstOrNull { it.name == obj.optString("size") } ?: DocPaperSize.A4
                val orient = DocPageOrientation.entries.firstOrNull { it.name == obj.optString("orient") } ?: DocPageOrientation.PORTRAIT
                val preset = DocMarginPreset.entries.firstOrNull { it.name == obj.optString("preset") } ?: DocMarginPreset.NORMAL
                DocPageSetup(
                    paperSize = size,
                    orientation = orient,
                    marginPreset = preset,
                    marginTopCm = obj.optDouble("mT", 2.54).toFloat().coerceIn(0.5f, 6.0f),
                    marginBottomCm = obj.optDouble("mB", 2.54).toFloat().coerceIn(0.5f, 6.0f),
                    marginLeftCm = obj.optDouble("mL", 2.54).toFloat().coerceIn(0.5f, 6.0f),
                    marginRightCm = obj.optDouble("mR", 2.54).toFloat().coerceIn(0.5f, 6.0f),
                    isPageless = obj.optBoolean("pageless", false),
                    showPageNumbers = obj.optBoolean("pageNum", true),
                    headerText = obj.optString("hdr", ""),
                    footerText = obj.optString("ftr", "")
                )
            } catch (_: Exception) {
                DocPageSetup()
            }
        }
    }
}

// ── PAGE SETUP MODAL DIALOG (GOOGLE DOCS / WORD STYLE) ────────────────────────
@Composable
fun PageSetupDialog(
    initialSetup: DocPageSetup,
    onDismiss: () -> Unit,
    onApply: (DocPageSetup) -> Unit
) {
    var isPageless by remember { mutableStateOf(initialSetup.isPageless) }
    var paperSize by remember { mutableStateOf(initialSetup.paperSize) }
    var orientation by remember { mutableStateOf(initialSetup.orientation) }
    var marginPreset by remember { mutableStateOf(initialSetup.marginPreset) }
    var mTopText by remember { mutableStateOf(formatCm(initialSetup.marginTopCm)) }
    var mBottomText by remember { mutableStateOf(formatCm(initialSetup.marginBottomCm)) }
    var mLeftText by remember { mutableStateOf(formatCm(initialSetup.marginLeftCm)) }
    var mRightText by remember { mutableStateOf(formatCm(initialSetup.marginRightCm)) }
    var showPageNumbers by remember { mutableStateOf(initialSetup.showPageNumbers) }
    var headerText by remember { mutableStateOf(initialSetup.headerText) }
    var footerText by remember { mutableStateOf(initialSetup.footerText) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 10.dp,
            modifier = Modifier
                .widthIn(min = 340.dp, max = 480.dp)
                .heightIn(max = 640.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Description, null, tint = AccentBlue)
                    Text("Penyiapan Halaman", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LernspaceDark)
                }

                Spacer(Modifier.height(14.dp))

                // Mode Switch: Halaman vs Tanpa Halaman (Pageless)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LernspaceSurfaceLow, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeTabButton(
                        label = "Halaman (Cetak)",
                        selected = !isPageless,
                        onClick = { isPageless = false },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabButton(
                        label = "Tanpa Halaman",
                        selected = isPageless,
                        onClick = { isPageless = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isPageless) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "Mode Tanpa Halaman (Pageless) menampilkan dokumen secara leluasa tanpa batas halaman — cocok untuk tabel lebar dan catatan layar penuh di tablet.",
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = LernspaceDark,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        // 1. Ukuran Kertas
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Ukuran Kertas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceTextSecondary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DocPaperSize.entries.forEach { size ->
                                    val selected = paperSize == size
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (selected) AccentBlue else LernspaceBorder),
                                        modifier = Modifier.clickable { paperSize = size }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                size.label,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) AccentBlue else LernspaceDark
                                            )
                                            Text(size.subtitle, fontSize = 10.sp, color = LernspaceTextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Orientasi
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Orientasi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DocPageOrientation.entries.forEach { orient ->
                                    val selected = orientation == orient
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (selected) AccentBlue else LernspaceBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { orientation = orient }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                if (orient == DocPageOrientation.PORTRAIT) Icons.Filled.CropPortrait else Icons.Filled.CropLandscape,
                                                null,
                                                Modifier.size(16.dp),
                                                tint = if (selected) AccentBlue else LernspaceTextSecondary
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                orient.label,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) AccentBlue else LernspaceDark
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Preset Margin
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Margin (cm)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceTextSecondary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DocMarginPreset.entries.filter { it != DocMarginPreset.CUSTOM }.forEach { preset ->
                                    val selected = marginPreset == preset
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (selected) AccentBlue else LernspaceBorder),
                                        modifier = Modifier.clickable {
                                            marginPreset = preset
                                            mTopText = formatCm(preset.topCm)
                                            mBottomText = formatCm(preset.bottomCm)
                                            mLeftText = formatCm(preset.leftCm)
                                            mRightText = formatCm(preset.rightCm)
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(preset.label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (selected) AccentBlue else LernspaceDark)
                                            Text(preset.desc, fontSize = 9.5.sp, color = LernspaceTextSecondary)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            // Custom cm fields
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MarginField("Atas", mTopText, { mTopText = it; marginPreset = DocMarginPreset.CUSTOM }, Modifier.weight(1f))
                                MarginField("Bawah", mBottomText, { mBottomText = it; marginPreset = DocMarginPreset.CUSTOM }, Modifier.weight(1f))
                                MarginField("Kiri", mLeftText, { mLeftText = it; marginPreset = DocMarginPreset.CUSTOM }, Modifier.weight(1f))
                                MarginField("Kanan", mRightText, { mRightText = it; marginPreset = DocMarginPreset.CUSTOM }, Modifier.weight(1f))
                            }
                        }

                        HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f))

                        // 4. Header, Footer & Nomor Halaman
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor Halaman", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                                    Text("Tampilkan penomoran otomatis di tiap halaman", fontSize = 11.sp, color = LernspaceTextSecondary)
                                }
                                Switch(
                                    checked = showPageNumbers,
                                    onCheckedChange = { showPageNumbers = it }
                                )
                            }

                            OutlinedTextField(
                                value = headerText,
                                onValueChange = { headerText = it },
                                label = { Text("Header / Kop Halaman (opsional)", fontSize = 12.sp) },
                                placeholder = { Text("Contoh: Makalah Kalkulus — Semester 3", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = footerText,
                                onValueChange = { footerText = it },
                                label = { Text("Footer / Catatan Kaki (opsional)", fontSize = 12.sp) },
                                placeholder = { Text("Contoh: Universitas Indonesia · 2026", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Actions
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
                            val top = mTopText.replace(',', '.').toFloatOrNull()?.coerceIn(0.5f, 6.0f) ?: 2.54f
                            val bottom = mBottomText.replace(',', '.').toFloatOrNull()?.coerceIn(0.5f, 6.0f) ?: 2.54f
                            val left = mLeftText.replace(',', '.').toFloatOrNull()?.coerceIn(0.5f, 6.0f) ?: 2.54f
                            val right = mRightText.replace(',', '.').toFloatOrNull()?.coerceIn(0.5f, 6.0f) ?: 2.54f
                            onApply(
                                DocPageSetup(
                                    paperSize = paperSize,
                                    orientation = orientation,
                                    marginPreset = marginPreset,
                                    marginTopCm = top,
                                    marginBottomCm = bottom,
                                    marginLeftCm = left,
                                    marginRightCm = right,
                                    isPageless = isPageless,
                                    showPageNumbers = showPageNumbers,
                                    headerText = headerText.trim(),
                                    footerText = footerText.trim()
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Terapkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) AccentBlue else LernspaceTextSecondary
            )
        }
    }
}

@Composable
private fun MarginField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 5) onValueChange(it) },
        label = { Text(label, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    )
}

private fun formatCm(v: Float): String {
    return if (v % 1.0f == 0f) String.format(Locale.US, "%.1f", v)
    else String.format(Locale.US, "%.2f", v)
}
