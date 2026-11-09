package com.lernspace.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.AppThemeMode
import com.lernspace.app.data.local.Handedness
import com.lernspace.app.data.local.ShapeSnapSensitivity
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.PaperStyle

private enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    STYLUS_ERGONOMICS("Stylus & Ergonomi", "Mode tangan, snap bentuk geometri", Icons.Filled.Draw),
    APPEARANCE("Tampilan & Tema", "Mode gelap, terang, sistem", Icons.Filled.Palette),
    WORKSPACE_DEFAULTS("Format Catatan", "Template kertas bawaan", Icons.AutoMirrored.Filled.MenuBook),
    ABOUT("Tentang & Sistem", "Info versi dan status memori", Icons.Filled.Info)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.STYLUS_ERGONOMICS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pengaturan & Preferensi",
                        style = MaterialTheme.typography.headlineLarge,
                        color = LernspaceDark,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder)
                    ) {
                        Text(
                            text = "Tablet Workspace",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Kustomisasi ergonomi menulis, responsivitas stylus, dan preferensi tampilan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }
        }

        // ── Landscape 2-Column Responsive Body ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left Column: Category Navigator (~320dp) ──────────────────────
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SettingsCategory.entries.size) { idx ->
                        val cat = SettingsCategory.entries[idx]
                        val isSelected = cat == selectedCategory
                        val isDark = LocalIsDarkTheme.current
                        val activeBg = if (isDark) Color.White else Color.Black
                        val activeFg = if (isDark) Color.Black else Color.White

                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) activeBg else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) activeFg.copy(alpha = 0.15f) else LernspaceSurfaceLow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) activeFg else LernspaceDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) activeFg else LernspaceDark
                                    )
                                    Text(
                                        text = cat.subtitle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) activeFg.copy(alpha = 0.75f) else LernspaceTextSecondary,
                                        maxLines = 1
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = activeFg,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Right Column: Detail Content (~Expanded) ──────────────────────
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (selectedCategory) {
                        SettingsCategory.STYLUS_ERGONOMICS -> {
                            item {
                                StylusAndErgonomicsSection()
                            }
                        }
                        SettingsCategory.APPEARANCE -> {
                            item {
                                AppearanceSection()
                            }
                        }
                        SettingsCategory.WORKSPACE_DEFAULTS -> {
                            item {
                                WorkspaceDefaultsSection()
                            }
                        }
                        SettingsCategory.ABOUT -> {
                            item {
                                AboutAndSystemSection()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 1: STYLUS & ERGONOMI
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StylusAndErgonomicsSection() {
    val currentHandedness = AppSettingsManager.handedness
    val currentSensitivity = AppSettingsManager.shapeSensitivity

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Section Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Stylus & Ergonomi Menulis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
            Text(
                text = "Sesuaikan tata letak antarmuka dengan tangan dominan Anda agar pergelangan tangan tidak menyenggol tombol saat menulis dengan stylus.",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        // 1. SETTING: MODE TANGAN (HANDEDNESS)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PanTool, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Text("Orientasi Tangan (Handedness)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = AccentBlueContainer) {
                        Text(
                            text = if (currentHandedness == Handedness.RIGHT_HANDED) "Kanan" else "Kidal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Mengatur posisi bilah navigasi utama tablet. Mode kidal memindahkan bilah ke sisi kanan agar telapak tangan kiri Anda bebas bersandar di kanvas.",
                    fontSize = 12.sp,
                    color = LernspaceTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HandednessCard(
                        title = "Tangan Kanan",
                        description = "Navigasi rail di kiri. Ideal untuk pengguna bertangan kanan.",
                        isSelected = currentHandedness == Handedness.RIGHT_HANDED,
                        railOnRight = false,
                        onClick = { AppSettingsManager.updateHandedness(Handedness.RIGHT_HANDED) },
                        modifier = Modifier.weight(1f)
                    )

                    HandednessCard(
                        title = "Kidal (Tangan Kiri)",
                        description = "Navigasi rail di kanan. Bebas bentrok pergelangan tangan kiri.",
                        isSelected = currentHandedness == Handedness.LEFT_HANDED,
                        railOnRight = true,
                        onClick = { AppSettingsManager.updateHandedness(Handedness.LEFT_HANDED) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. SETTING: SENSITIVITAS HOLD-TO-SNAP SHAPE
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoFixHigh, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Text("Sensitivitas Hold-to-Snap Bentuk", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = AccentTealContainer) {
                        Text(
                            text = currentSensitivity.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Saat Anda menggambar lingkaran, garis lurus, atau persegi lalu menahan ujung stylus tanpa diangkat, sistem akan merapikan goresan secara otomatis.",
                    fontSize = 12.sp,
                    color = LernspaceTextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShapeSnapSensitivity.entries.forEach { option ->
                        val isSelected = option == currentSensitivity
                        Surface(
                            onClick = { AppSettingsManager.updateShapeSensitivity(option) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) LernspaceSurfaceCard else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) AccentTeal else LernspaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { AppSettingsManager.updateShapeSensitivity(option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentTeal)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LernspaceDark else LernspaceDark.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = option.description,
                                        fontSize = 11.sp,
                                        color = LernspaceTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandednessCard(
    title: String,
    description: String,
    isSelected: Boolean,
    railOnRight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) LernspaceSurfaceCard else LernspaceSurfaceLow,
        border = BorderStroke(1.5.dp, if (isSelected) LernspaceDark else LernspaceBorder),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Visual Diagram of Tablet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LernspaceBorder)
                    .padding(3.dp)
            ) {
                // Tablet Screen Box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color.White)
                ) {
                    // Navigation Rail Preview
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight()
                            .align(if (railOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                            .background(if (isSelected) LernspaceDark else Color(0xFF64748B))
                    )
                    // Canvas writing area indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(if (railOnRight) Alignment.CenterStart else Alignment.CenterEnd)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) LernspaceDark else LernspaceDark.copy(alpha = 0.7f))
            Text(text = description, fontSize = 11.sp, color = LernspaceTextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 2: TAMPILAN & TEMA APLIKASI
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppearanceSection() {
    val currentTheme = AppSettingsManager.themeMode

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Tampilan & Tema Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
            Text(
                text = "Pilih tema warna antarmuka Learnspace sesuai preferensi kenyamanan mata Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Mode Tema Utama", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeOptionCard(
                        title = "Mengikuti Sistem",
                        subtitle = "Otomatis",
                        icon = Icons.Filled.BrightnessAuto,
                        isSelected = currentTheme == AppThemeMode.SYSTEM,
                        onClick = { AppSettingsManager.updateThemeMode(AppThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeOptionCard(
                        title = "Mode Terang",
                        subtitle = "Cerah & Bersih",
                        icon = Icons.Filled.LightMode,
                        isSelected = currentTheme == AppThemeMode.LIGHT,
                        onClick = { AppSettingsManager.updateThemeMode(AppThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeOptionCard(
                        title = "Mode Gelap",
                        subtitle = "Redup & Nyaman",
                        icon = Icons.Filled.DarkMode,
                        isSelected = currentTheme == AppThemeMode.DARK,
                        onClick = { AppSettingsManager.updateThemeMode(AppThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) LernspaceSurfaceCard else LernspaceSurfaceLow,
        border = BorderStroke(1.5.dp, if (isSelected) LernspaceDark else LernspaceBorder),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) LernspaceDarkContainer else LernspaceBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) LernspaceDark else LernspaceTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) LernspaceDark else LernspaceDark.copy(alpha = 0.7f))
            Text(text = subtitle, fontSize = 11.sp, color = LernspaceTextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 3: FORMAT CATATAN BAWAAN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WorkspaceDefaultsSection() {
    val currentPaper = AppSettingsManager.defaultPaperStyle

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Format Catatan Bawaan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
            Text(
                text = "Pilih gaya pola kertas yang otomatis digunakan saat Anda membuat catatan baru dari Catatan Cepat atau Silabus.",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Template Kertas Standar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)

                PaperStyle.entries.forEach { style ->
                    val isSelected = style == currentPaper
                    Surface(
                        onClick = { AppSettingsManager.updateDefaultPaperStyle(style) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) LernspaceSurfaceCard else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) AccentBlue else LernspaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = style.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AccentBlue else LernspaceDark
                                )
                                Text(
                                    text = when (style) {
                                        PaperStyle.RULED -> "Kertas bergaris horizontal standar untuk menulis teks panjang"
                                        PaperStyle.GRID -> "Kertas kotak-kotak matematika untuk rumus, tabel, dan diagram"
                                        PaperStyle.BLANK -> "Kertas putih polos bebas coret untuk sketsa dan gambar konsep"
                                        PaperStyle.DOTTED -> "Pola titik-titik rapi fleksibel untuk bullet journaling"
                                    },
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 4: TENTANG & SISTEM
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AboutAndSystemSection() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Tentang & Sistem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
            Text(
                text = "Informasi build aplikasi dan status penyimpanan lokal tablet",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Learnspace Tablet OS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        Text("Versi 1.0.0 (Production Release)", fontSize = 12.sp, color = LernspaceTextSecondary)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = AccentTealContainer) {
                        Text("UP TO DATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentTeal, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                HorizontalDivider(color = LernspaceBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Penyimpanan Database", fontSize = 12.sp, color = LernspaceTextSecondary)
                    Text("SQLite Room (Offline-First)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mesin Goresan Stylus", fontSize = 12.sp, color = LernspaceTextSecondary)
                    Text("AndroidX Ink Native Hardware Accelerated", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Arsitektur", fontSize = 12.sp, color = LernspaceTextSecondary)
                    Text("100% Native Jetpack Compose", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                }
            }
        }
    }
}

