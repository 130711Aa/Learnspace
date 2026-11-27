package com.lernspace.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.BuildConfig
import com.lernspace.app.core.ai.GeminiApiClient
import com.lernspace.app.core.updater.AppReleaseInfo
import com.lernspace.app.core.updater.UpdateManager
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.data.local.AppThemeMode
import com.lernspace.app.data.local.BackupRestoreManager
import com.lernspace.app.data.local.Handedness
import com.lernspace.app.data.local.ShapeSnapSensitivity
import com.lernspace.app.data.local.TouchInputMode
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlinx.coroutines.launch

private enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    STYLUS_ERGONOMICS("Stylus & Ergonomi", "Mode tangan, snap bentuk, scribble erase", Icons.Filled.Draw),
    APPEARANCE("Tampilan & Tema", "Mode gelap, terang, sistem", Icons.Filled.Palette),
    WORKSPACE_DEFAULTS("Format Catatan", "Template kertas bawaan", Icons.AutoMirrored.Filled.MenuBook),
    OFFLINE_AI("AI Belajar", "Konfigurasi Gemini API (BYOK)", Icons.Filled.AutoAwesome),
    BACKUP_RESTORE("Cadangan & Pemulihan", "Backup .lernspace ZIP & Restore", Icons.Filled.Backup),
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
                        SettingsCategory.OFFLINE_AI -> {
                            item {
                                OfflineSlmSection()
                            }
                        }
                        SettingsCategory.BACKUP_RESTORE -> {
                            item {
                                BackupRestoreSection()
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
    val currentTouchInput = AppSettingsManager.touchInputMode

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
                text = "Sesuaikan tata letak antarmuka dan respon sentuhan kanvas agar pergelangan tangan nyaman saat menulis.",
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
                            border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
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
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = LernspaceDark,
                                        unselectedColor = LernspaceTextSecondary
                                    )
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

        // 3. SETTING: MODE INPUT SENTUHAN KANVAS (STYLUS VS SENTUHAN TANGAN)
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
                        Icon(Icons.Filled.TouchApp, null, tint = LernspaceDark, modifier = Modifier.size(20.dp))
                        Text("Respon Sentuhan Kanvas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = LernspaceSurfaceCard, border = BorderStroke(1.dp, LernspaceBorder)) {
                        Text(
                            text = if (currentTouchInput == TouchInputMode.STYLUS_ONLY) "Stylus Only" else "Sentuhan Tangan Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Pilih apakah jari tangan dapat digunakan untuk mencoret catatan atau hanya difungsikan khusus navigasi.",
                    fontSize = 12.sp,
                    color = LernspaceTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TouchInputCard(
                        title = "Hanya Stylus (Rekomendasi)",
                        subtitle = "Goresan hanya direspon pena stylus. 1 & 2 jari untuk menggeser & zoom (Palm Rejection aktif 100%).",
                        icon = Icons.Filled.Edit,
                        isSelected = currentTouchInput == TouchInputMode.STYLUS_ONLY,
                        onClick = { AppSettingsManager.updateTouchInputMode(TouchInputMode.STYLUS_ONLY) },
                        modifier = Modifier.weight(1f)
                    )

                    TouchInputCard(
                        title = "Sentuhan Tangan & Stylus",
                        subtitle = "1 sentuhan jari atau stylus untuk coret-coret catatan. 2 jari untuk menggeser (pan) dan zoom.",
                        icon = Icons.Filled.TouchApp,
                        isSelected = currentTouchInput == TouchInputMode.FINGER_AND_STYLUS,
                        onClick = { AppSettingsManager.updateTouchInputMode(TouchInputMode.FINGER_AND_STYLUS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. SETTING: SCRIBBLE TO ERASE (CORET UNTUK MENGHAPUS)
        val currentScribbleEnabled = AppSettingsManager.isScribbleToEraseEnabled
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LernspaceSurfaceCard,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = null,
                                    tint = LernspaceDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Coret untuk Menghapus (Scribble to Erase)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = if (currentScribbleEnabled) "Aktif (Otomatis deteksi coretan)" else "Nonaktif",
                                fontSize = 12.sp,
                                color = if (currentScribbleEnabled) Color(0xFF16A34A) else LernspaceTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Switch(
                        checked = currentScribbleEnabled,
                        onCheckedChange = { AppSettingsManager.updateScribbleToErase(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LernspaceDark,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = LernspaceSurfaceCard
                        )
                    )
                }

                Text(
                    text = "Hapus tulisan tangan atau teks secara instan cukup dengan mencoret-coretnya bolak-balik (zigzag / scratch-out) menggunakan pena stylus, tanpa perlu berganti ke tool penghapus.",
                    fontSize = 12.sp,
                    color = LernspaceTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

    }
}

@Composable
private fun TouchInputCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) LernspaceDark else LernspaceBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            if (isDark) Color.Black else Color.White
                        } else LernspaceTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) LernspaceDark else LernspaceDark.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = LernspaceTextSecondary,
                lineHeight = 16.sp
            )
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) LernspaceDark else LernspaceBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        if (isDark) Color.Black else Color.White
                    } else LernspaceTextSecondary,
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
                        border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
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
                                    color = if (isSelected) LernspaceDark else LernspaceDark.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = when (style) {
                                        PaperStyle.RULED -> "Kertas bergaris horizontal standar untuk menulis teks panjang"
                                        PaperStyle.GRID -> "Kertas kotak-kotak matematika untuk rumus, tabel, dan diagram"
                                        PaperStyle.BLANK -> "Kertas putih polos bebas coret untuk sketsa dan gambar konsep"
                                        PaperStyle.DOTTED -> "Pola titik-titik rapi fleksibel untuk bullet journaling"
                                        PaperStyle.CORNELL -> "Layout Cornell (kolom kata kunci, catatan utama, dan ringkasan)"
                                        PaperStyle.WARM_CREAM -> "Kertas krem hangat bergaris yang nyaman di mata untuk baca lama"
                                    },
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = LernspaceDark,
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
// SECTION 4: TENTANG & SISTEM (Version Info & GitHub Auto-Updater)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AboutAndSystemSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateCheckStatus by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Tentang & Sistem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
            Text(
                text = "Informasi build aplikasi, status penyimpanan lokal, dan pemeriksaan pembaruan rilis",
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
                // Header Row with App Title & Check for Update Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Learnspace Tablet OS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        Text("Versi ${BuildConfig.VERSION_NAME} (Production Release)", fontSize = 12.sp, color = LernspaceTextSecondary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (availableUpdate?.hasUpdate == true) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.clickable { showUpdateDialog = true }
                            ) {
                                Text(
                                    text = "UPDATE: ${availableUpdate?.tagName}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LernspaceSurfaceCard,
                                border = BorderStroke(1.dp, LernspaceBorder)
                            ) {
                                Text(
                                    text = updateCheckStatus ?: "UP TO DATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (!isCheckingUpdate) {
                                    isCheckingUpdate = true
                                    scope.launch {
                                        val result = UpdateManager.checkForUpdates(BuildConfig.VERSION_NAME)
                                        isCheckingUpdate = false
                                        result.onSuccess { info ->
                                            if (info.hasUpdate) {
                                                availableUpdate = info
                                                showUpdateDialog = true
                                            } else {
                                                updateCheckStatus = "UP TO DATE"
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Learnspace Anda sudah versi terbaru (${info.tagName})",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }.onFailure { err ->
                                            android.widget.Toast.makeText(
                                                context,
                                                "Gagal memeriksa update: ${err.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(13.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Memeriksa...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Filled.Sync, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cek Pembaruan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = LernspaceBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Saluran Distribusi", fontSize = 12.sp, color = LernspaceTextSecondary)
                    Text("GitHub Releases (Direct APK)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                }

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
                    Text("Arsitektur UI", fontSize = 12.sp, color = LernspaceTextSecondary)
                    Text("100% Native Jetpack Compose", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                }
            }
        }
    }

    // ── Dialog: Pembaruan Tersedia ────────────────────────────────────────────
    if (showUpdateDialog && availableUpdate != null) {
        val info = availableUpdate!!
        Dialog(onDismissRequest = { showUpdateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.width(440.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AutoStories, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Pembaruan Tersedia!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                Text("Versi ${info.tagName}", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { showUpdateDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }

                    Text(
                        text = info.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LernspaceDark
                    )

                    // Changelog scroll box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Catatan Perubahan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Text(
                                text = info.changelog.ifBlank { "Peningkatan performa dan kestabilan sistem." },
                                fontSize = 11.sp,
                                color = LernspaceTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { UpdateManager.openInBrowser(context, info.releasePageUrl) }) {
                            Text("Buka di GitHub", fontSize = 12.sp, color = LernspaceDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showUpdateDialog = false
                                UpdateManager.startApkDownload(context, info.apkDownloadUrl, info.versionName)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh & Pasang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION: AI BELAJAR (Gemini Cloud API BYOK)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OfflineSlmSection() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var geminiKeyInput by remember { mutableStateOf(GeminiApiClient.apiKey) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingGemini by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "AI Belajar: Gemini Cloud API",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )
            Text(
                text = "Gunakan Gemini Flash & Flash-Lite (BYOK) untuk asisten belajar cerdas, pembuatan flashcard otomatis, dan analisis rumus tulisan tangan.",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        // Gemini Flash-Lite API Card (BYOK)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(
                1.dp,
                if (GeminiApiClient.isConfigured()) AccentBlue.copy(alpha = 0.45f) else LernspaceBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Gemini Flash-Lite API (Opsional / BYOK)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LernspaceDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (GeminiApiClient.isConfigured()) AccentTeal.copy(alpha = 0.14f) else LernspaceBorder.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = if (GeminiApiClient.isConfigured()) "Aktif" else "Belum Diisi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (GeminiApiClient.isConfigured()) AccentTeal else LernspaceTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Masukkan API key dari Google AI Studio (aistudio.google.com). Disimpan lokal di perangkat untuk asisten belajar, vision math, dan pembuatan flashcard.",
                            fontSize = 11.sp,
                            color = LernspaceTextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = {
                        geminiKeyInput = it
                        geminiTestResult = null
                    },
                    label = { Text("Gemini API Key (AIza...)", fontSize = 12.sp) },
                    placeholder = { Text("Tempel API key dari Google AI Studio", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Icon(
                                imageVector = if (showGeminiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showGeminiKey) "Sembunyikan" else "Tampilkan"
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Model Flash-Lite",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LernspaceDark
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GeminiApiClient.availableModels.forEach { (modelId, label) ->
                            FilterChip(
                                selected = GeminiApiClient.selectedModelId == modelId,
                                onClick = {
                                    GeminiApiClient.updateModelId(context, modelId)
                                    geminiTestResult = null
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            GeminiApiClient.updateApiKey(context, geminiKeyInput)
                            geminiTestResult = if (geminiKeyInput.isBlank()) {
                                "API key dihapus."
                            } else {
                                "API key tersimpan secara lokal."
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Simpan Key", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        enabled = !isTestingGemini && geminiKeyInput.trim().isNotEmpty(),
                        onClick = {
                            GeminiApiClient.updateApiKey(context, geminiKeyInput)
                            isTestingGemini = true
                            geminiTestResult = null
                            coroutineScope.launch {
                                val res = GeminiApiClient.testConnection()
                                geminiTestResult = res.fold(
                                    onSuccess = { reply -> "Terhubung (${GeminiApiClient.selectedModelId}): $reply" },
                                    onFailure = { err -> "Gagal: ${err.message}" }
                                )
                                isTestingGemini = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTestingGemini) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Tes Koneksi", fontSize = 12.sp)
                    }

                    if (GeminiApiClient.isConfigured()) {
                        TextButton(
                            onClick = {
                                geminiKeyInput = ""
                                GeminiApiClient.updateApiKey(context, "")
                                geminiTestResult = "API key dihapus."
                            }
                        ) {
                            Text("Hapus", fontSize = 12.sp, color = AccentRed)
                        }
                    }
                }

                if (!geminiTestResult.isNullOrBlank()) {
                    val isErr = geminiTestResult!!.startsWith("Gagal")
                    Text(
                        text = geminiTestResult!!,
                        fontSize = 11.sp,
                        color = if (isErr) AccentRed else AccentTeal
                    )
                }
            }
        }

        // ── Card: Pengenal Matematika & Tulisan Tangan AI (Gemini Vision AI) ──
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, tint = LernspaceDark)
                    Column {
                        Text("Pengenal Matematika & Tulisan Tangan AI", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LernspaceDark)
                        Text("Ditenagai langsung oleh Gemini Multimodal Vision AI untuk kalkulasi '=', rumus LaTeX murni, dan plot fungsi.", fontSize = 12.sp, color = LernspaceTextSecondary)
                    }
                }

                HorizontalDivider(color = LernspaceBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gemini Vision AI Engine", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = LernspaceDark)
                        Text("Kecerdasan multimodal cloud untuk membaca struktur 2D rumit (matriks, integral, sistem persamaan).", fontSize = 11.5.sp, color = LernspaceTextSecondary)
                    }
                    val isAiReady = GeminiApiClient.isConfigured()
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAiReady) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isAiReady) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (isAiReady) "AKTIF (AI)" else "API KEY BELUM DIATUR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAiReady) Color(0xFF059669) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION: CADANGAN & PEMULIHAN (.lernspace ZIP)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BackupRestoreSection() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreSuccessDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // Backup Launcher (Save to file)
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            isProcessing = true
            statusMessage = "Membuat arsip cadangan .lernspace..."
            isError = false
            coroutineScope.launch {
                val res = BackupRestoreManager.createBackup(context, targetUri)
                isProcessing = false
                isError = !res.success
                statusMessage = res.message
            }
        }
    }

    // Restore Launcher (Pick file)
    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            pendingRestoreUri = pickedUri
            showRestoreConfirmDialog = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Cadangan & Pemulihan Data",
                style = MaterialTheme.typography.titleLarge,
                color = LernspaceDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Simpan seluruh catatan tulisan tangan, jadwal, dokumen, dan pengaturan ke file arsip .lernspace (ZIP) agar data aman saat ganti perangkat.",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        // Status Card
        if (statusMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isError) AccentRed.copy(alpha = 0.1f) else AccentTeal.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, if (isError) AccentRed else AccentTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) AccentRed else AccentTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = statusMessage!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) AccentRed else AccentTeal
                    )
                }
            }
        }

        // Card 1: Ekspor Cadangan
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CloudUpload, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Buat Cadangan Lengkap (.lernspace)", fontWeight = FontWeight.Bold, color = LernspaceDark, fontSize = 15.sp)
                        Text("Mengompres database SQLite, semua stroke tulisan tangan, gambar, dan lampiran PDF.", fontSize = 12.sp, color = LernspaceTextSecondary)
                    }
                }

                Button(
                    enabled = !isProcessing,
                    onClick = {
                        createBackupLauncher.launch(BackupRestoreManager.generateDefaultBackupFileName())
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LernspaceDark,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (isProcessing && statusMessage?.contains("Membuat") == true) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Pilih Lokasi & Simpan Cadangan", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Card 2: Pulihkan Cadangan
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.SettingsBackupRestore, null, tint = AccentTeal, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Pulihkan dari File Cadangan", fontWeight = FontWeight.Bold, color = LernspaceDark, fontSize = 15.sp)
                        Text("Pilih file arsip .lernspace atau .zip untuk memulihkan seluruh data belajar.", fontSize = 12.sp, color = LernspaceTextSecondary)
                    }
                }

                OutlinedButton(
                    enabled = !isProcessing,
                    onClick = {
                        openRestoreLauncher.launch(arrayOf("*/*", "application/zip", "application/octet-stream"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LernspaceDark),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (isProcessing && statusMessage?.contains("Memulihkan") == true) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LernspaceDark)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.FileUpload, null, tint = LernspaceDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Pilih File Cadangan untuk Dipulihkan", fontWeight = FontWeight.SemiBold, color = LernspaceDark, fontSize = 13.sp)
                }
            }
        }
    }

    // Confirmation Dialog before restore (since restore overwrites active database)
    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            title = {
                Text("Konfirmasi Pemulihan Data", fontWeight = FontWeight.Bold, color = LernspaceDark)
            },
            text = {
                Text(
                    "Memulihkan cadangan akan menimpa data dan catatan saat ini dengan data dari file cadangan tersebut. Apakah kamu yakin ingin melanjutkan?",
                    fontSize = 13.sp,
                    color = LernspaceTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uriToRestore = pendingRestoreUri
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                        if (uriToRestore != null) {
                            isProcessing = true
                            statusMessage = "Memulihkan seluruh catatan dan basis data..."
                            isError = false
                            coroutineScope.launch {
                                val res = BackupRestoreManager.restoreBackup(context, uriToRestore)
                                isProcessing = false
                                isError = !res.success
                                statusMessage = res.message
                                if (res.success) {
                                    showRestoreSuccessDialog = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Ya, Pulihkan Data", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("Batal", color = LernspaceDark)
                }
            }
        )
    }

    // Success Dialog after restoring data
    if (showRestoreSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(22.dp))
                    Text("Data Berhasil Dipulihkan", fontWeight = FontWeight.Bold, color = LernspaceDark)
                }
            },
            text = {
                Text(
                    text = "${statusMessage ?: "Seluruh data catatan dan pengaturan telah selesai dipulihkan."}\n\nTekan 'Muat Ulang Tampilan' untuk menyegarkan tampilan sekarang.",
                    fontSize = 13.sp,
                    color = LernspaceTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreSuccessDialog = false
                        findActivity(context)?.recreate()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                ) {
                    Text("Muat Ulang Tampilan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreSuccessDialog = false }) {
                    Text("Tutup", color = LernspaceTextSecondary)
                }
            }
        )
    }
}

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}


