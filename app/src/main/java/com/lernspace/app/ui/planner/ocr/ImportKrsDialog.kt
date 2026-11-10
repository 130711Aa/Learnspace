package com.lernspace.app.ui.planner.ocr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.ui.theme.*

@Composable
fun ImportKrsDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (List<ParsedScheduleItem>) -> Unit
) {
    PasteScheduleDialog(onDismiss = onDismiss, onConfirmImport = onConfirmImport)
}

@Composable
fun PasteScheduleDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (List<ParsedScheduleItem>) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var pasteTextContent by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val parsedItems = remember { mutableStateListOf<ParsedScheduleItem>() }

    fun runExtraction(text: String) {
        if (text.isBlank()) return
        val items = ScheduleOcrParser.parseFromPlainText(text)
        parsedItems.clear()
        parsedItems.addAll(items)
        if (items.isEmpty()) {
            errorMessage = "Tidak menemukan pola jadwal (hari & jam) pada teks. Pastikan mencantumkan nama hari (Senin..Minggu) dan jam (contoh: 07:15 - 08:35)."
        } else {
            errorMessage = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(720.dp)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── 1. Header ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LernspaceDark,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Paste Teks Jadwal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = "Salin jadwal dari WhatsApp, SIAKAD / portal web, atau catatan, lalu tempel di sini",
                                style = MaterialTheme.typography.bodySmall,
                                color = LernspaceTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = LernspaceTextSecondary)
                    }
                }

                // ── 2. Text Input Area ────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Teks Jadwal Mentah",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Paste from clipboard button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.clickable {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            pasteTextContent = clip
                                            runExtraction(clip)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(13.dp), tint = AccentBlue)
                                        Text("Tempel dari Clipboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                                    }
                                }

                                // Example format button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.clickable {
                                        pasteTextContent = """
Senin
07:15 - 08:35 Matematika Ruang 4 Ismi Muzayimah, S.Pd
08:35 - 09:55 Bahasa Indonesia Ruang 4 Ari Nuraeni S.Pd.
10:10 - 11:30 Bahasa Inggris Ruang 04

Selasa
07:15 - 08:35 Matematika Lanjut Gedung A 203
08:35 - 09:55 Biologi Lab 1
10:10 - 11:30 SBK Ruang Seni
""".trimIndent()
                                        runExtraction(pasteTextContent)
                                    }
                                ) {
                                    Text(
                                        "Contoh Format",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = LernspaceTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (pasteTextContent.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.clickable {
                                            pasteTextContent = ""
                                            parsedItems.clear()
                                            errorMessage = null
                                        }
                                    ) {
                                        Text(
                                            "Hapus",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AccentRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = pasteTextContent,
                            onValueChange = { pasteTextContent = it },
                            placeholder = {
                                Text(
                                    text = "Tempel teks jadwal di sini...\nContoh:\nSenin\n07:15 - 08:35 Matematika Ruang 4 Ismi Muzayimah, S.Pd\n08:35 - 09:55 Bahasa Indonesia Ruang 4 Ari Nuraeni S.Pd.",
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { runExtraction(pasteTextContent) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                enabled = pasteTextContent.isNotBlank()
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekstrak Jadwal Otomatis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Error message banner if any
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentRed.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Text(errorMessage!!, fontSize = 11.sp, color = AccentRed, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── 3. Parsed Results Review Table / List ─────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Hasil Deteksi Jadwal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, LernspaceBorder)
                            ) {
                                Text(
                                    text = "${parsedItems.size} Kelas Terdeteksi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (parsedItems.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        parsedItems.add(
                                            ParsedScheduleItem(
                                                subjectName = "Mata Pelajaran Baru",
                                                dayOfWeek = 1,
                                                startTime = "07:15",
                                                endTime = "08:35",
                                                room = "Ruang 1"
                                            )
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = LernspaceDark)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah Baris", fontSize = 11.sp, color = LernspaceDark, fontWeight = FontWeight.SemiBold)
                                }

                                TextButton(
                                    onClick = {
                                        val allSelected = parsedItems.all { it.isSelected }
                                        parsedItems.forEachIndexed { idx, item ->
                                            parsedItems[idx] = item.copy(isSelected = !allSelected)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (parsedItems.all { it.isSelected }) "Batal Pilih Semua" else "Pilih Semua",
                                        fontSize = 11.sp,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (parsedItems.isEmpty()) {
                        // Empty state inside dialog
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LernspaceSurfaceLow.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.ContentPaste, null, tint = LernspaceTextSecondary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Belum ada jadwal yang diekstrak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LernspaceTextSecondary
                                )
                                Text(
                                    text = "Tempel teks jadwal di atas, lalu klik 'Ekstrak Jadwal Otomatis'",
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        // List of extracted items (Editable & Verifiable)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(parsedItems, key = { _, item -> item.id }) { index, item ->
                                ParsedItemRow(
                                    item = item,
                                    onUpdate = { updated ->
                                        parsedItems[index] = updated
                                    },
                                    onDelete = {
                                        parsedItems.removeAt(index)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── 4. Bottom Action Controls ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${parsedItems.count { it.isSelected }} dari ${parsedItems.size} jadwal siap disimpan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LernspaceTextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Batal", fontSize = 12.sp, color = LernspaceTextSecondary)
                        }

                        Button(
                            onClick = {
                                val selected = parsedItems.filter { it.isSelected }
                                onConfirmImport(selected)
                            },
                            enabled = parsedItems.any { it.isSelected },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simpan Semua Jadwal (${parsedItems.count { it.isSelected }})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedItemRow(
    item: ParsedScheduleItem,
    onUpdate: (ParsedScheduleItem) -> Unit,
    onDelete: () -> Unit
) {
    val days = listOf(
        Pair(1, "Senin"),
        Pair(2, "Selasa"),
        Pair(3, "Rabu"),
        Pair(4, "Kamis"),
        Pair(5, "Jumat"),
        Pair(6, "Sabtu"),
        Pair(7, "Minggu")
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LernspaceSurfaceCard,
        border = BorderStroke(1.dp, if (item.isSelected) LernspaceBorder else LernspaceBorder.copy(alpha = 0.4f)),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onUpdate(item.copy(isSelected = it)) },
                modifier = Modifier.size(24.dp)
            )

            // Day Selector Dropdown Chip
            var dayMenuExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LernspaceDark,
                    modifier = Modifier.clickable { dayMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                DropdownMenu(
                    expanded = dayMenuExpanded,
                    onDismissRequest = { dayMenuExpanded = false }
                ) {
                    days.forEach { (dNum, dLabel) ->
                        DropdownMenuItem(
                            text = { Text(dLabel, fontSize = 12.sp) },
                            onClick = {
                                onUpdate(item.copy(dayOfWeek = dNum))
                                dayMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Subject Name Input Field
            OutlinedTextField(
                value = item.subjectName,
                onValueChange = { onUpdate(item.copy(subjectName = it)) },
                placeholder = { Text("Nama Mata Kuliah / Mapel", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.5f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )

            // Start Time & End Time (Scroll Wheel Picker)
            var showStartTimePicker by remember { mutableStateOf(false) }
            var showEndTimePicker by remember { mutableStateOf(false) }

            if (showStartTimePicker) {
                com.lernspace.app.ui.components.WheelTimePickerDialog(
                    title = "Jam Mulai: ${item.subjectName}",
                    initialTime = item.startTime,
                    onDismiss = { showStartTimePicker = false },
                    onTimeSelected = { newT -> onUpdate(item.copy(startTime = newT)) }
                )
            }

            if (showEndTimePicker) {
                com.lernspace.app.ui.components.WheelTimePickerDialog(
                    title = "Jam Selesai: ${item.subjectName}",
                    initialTime = item.endTime,
                    onDismiss = { showEndTimePicker = false },
                    onTimeSelected = { newT -> onUpdate(item.copy(endTime = newT)) }
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LernspaceSurfaceLow,
                border = BorderStroke(1.dp, LernspaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.startTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        modifier = Modifier.clickable { showStartTimePicker = true }
                    )
                    Text("-", fontSize = 11.sp, color = LernspaceTextSecondary)
                    Text(
                        text = item.endTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        modifier = Modifier.clickable { showEndTimePicker = true }
                    )
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = AccentBlue
                    )
                }
            }

            // Room / Location
            OutlinedTextField(
                value = item.room,
                onValueChange = { onUpdate(item.copy(room = it)) },
                placeholder = { Text("Ruang", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(76.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )

            // Lecturer / Guru
            OutlinedTextField(
                value = item.lecturer,
                onValueChange = { onUpdate(item.copy(lecturer = it)) },
                placeholder = { Text("Guru / Dosen", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(115.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Hapus", tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
