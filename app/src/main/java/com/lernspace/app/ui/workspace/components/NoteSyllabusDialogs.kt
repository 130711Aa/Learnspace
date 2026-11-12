package com.lernspace.app.ui.workspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle

/**
 * Dialog shown when creating a new note directly from a Syllabus Topic (or Planner task),
 * letting the user choose the paper style (Ruled, Math Grid, Dot Grid, Blank),
 * canvas mode, and light/dark paper before opening the editor.
 */
@Composable
fun CreateTopicNoteDialog(
    subjectName: String,
    topicTitle: String,
    subjectColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        paperStyle: PaperStyle,
        canvasType: NoteCanvasType,
        isDarkPaper: Boolean
    ) -> Unit
) {
    var title by remember(topicTitle) { mutableStateOf(topicTitle) }
    var selectedPaperStyle by remember { mutableStateOf(AppSettingsManager.defaultPaperStyle) }
    var selectedCanvasType by remember { mutableStateOf(NoteCanvasType.PAGED_NOTE) }
    var isDarkPaper by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(480.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subjectColor))
                            Text(subjectName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subjectColor)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Buat Catatan Topik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Pilih Jenis Kertas:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PaperStyle.values().toList()) { style ->
                            val isSelected = selectedPaperStyle == style
                            val icon = when (style) {
                                PaperStyle.RULED -> Icons.Filled.Subject
                                PaperStyle.GRID -> Icons.Filled.GridOn
                                PaperStyle.DOTTED -> Icons.Filled.Apps
                                PaperStyle.BLANK -> Icons.Filled.CropSquare
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEFF6FF) else LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, if (isSelected) AccentBlue else LernspaceBorder),
                                modifier = Modifier.clickable { selectedPaperStyle = style }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(icon, null, tint = if (isSelected) AccentBlue else LernspaceDark, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = style.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) AccentBlue else LernspaceDark
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Canvas type toggle
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mode Kanvas:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedCanvasType == NoteCanvasType.PAGED_NOTE,
                                onClick = { selectedCanvasType = NoteCanvasType.PAGED_NOTE },
                                label = { Text("Berhalaman", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilterChip(
                                selected = selectedCanvasType == NoteCanvasType.INFINITE_SCRATCHPAD,
                                onClick = { selectedCanvasType = NoteCanvasType.INFINITE_SCRATCHPAD },
                                label = { Text("Bebas", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Paper light/dark toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tema Kertas:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !isDarkPaper,
                                onClick = { isDarkPaper = false },
                                label = { Text("Terang", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilterChip(
                                selected = isDarkPaper,
                                onClick = { isDarkPaper = true },
                                label = { Text("Gelap", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                title.trim().ifBlank { topicTitle },
                                selectedPaperStyle,
                                selectedCanvasType,
                                isDarkPaper
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Buat & Buka")
                    }
                }
            }
        }
    }
}

/**
 * Dialog to edit a note's title and link/re-link it to a specific Subject & Syllabus Topic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditNoteLinkDialog(
    note: NoteUiModel,
    subjects: List<SubjectUi>,
    topics: List<TopicUi>,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, selectedSubject: SubjectUi?, selectedTopic: TopicUi?) -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var selectedSubject by remember(note.id) {
        mutableStateOf(subjects.find { it.id == note.subjectId })
    }
    var selectedTopic by remember(note.id) {
        mutableStateOf(topics.find { it.id == note.topicId })
    }

    val subjectTopics = remember(selectedSubject, topics.size) {
        if (selectedSubject != null) topics.filter { it.subjectId == selectedSubject!!.id } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(540.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Text(
                            "Edit & Tautkan ke Silabus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Catatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Mata Kuliah:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSubject == null,
                                onClick = {
                                    selectedSubject = null
                                    selectedTopic = null
                                },
                                label = { Text("Umum / Tidak Tertaut", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            subjects.forEach { subj ->
                                FilterChip(
                                    selected = selectedSubject?.id == subj.id,
                                    onClick = {
                                        selectedSubject = subj
                                        if (selectedTopic?.subjectId != subj.id) {
                                            selectedTopic = null
                                        }
                                    },
                                    leadingIcon = {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subj.color))
                                    },
                                    label = { Text(subj.name, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    if (selectedSubject != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Sub-bab / Topik Silabus:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            if (subjectTopics.isEmpty()) {
                                Text(
                                    "Belum ada topik pada mata kuliah ini (tetap akan tertaut ke mata kuliah).",
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedTopic == null,
                                        onClick = { selectedTopic = null },
                                        label = { Text("Umum Matkul", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    subjectTopics.forEach { top ->
                                        FilterChip(
                                            selected = selectedTopic?.id == top.id,
                                            onClick = { selectedTopic = top },
                                            label = { Text(top.title, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                title.trim().ifBlank { note.title },
                                selectedSubject,
                                selectedTopic
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Simpan Perubahan")
                    }
                }
            }
        }
    }
}

/**
 * Dialog opened from a Syllabus Topic card so the user can attach any existing Workspace note/PDF
 * directly to that topic without leaving the Syllabus screen.
 */
@Composable
fun LinkExistingNoteToTopicDialog(
    subject: SubjectUi,
    topic: TopicUi,
    allNotes: List<NoteUiModel>,
    onDismiss: () -> Unit,
    onSelectNote: (NoteUiModel) -> Unit
) {
    val availableNotes = remember(allNotes.size, topic.id) {
        allNotes.filter { it.topicId != topic.id }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tautkan Note / PDF ke Topik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Text(
                            "${subject.name} • ${topic.title}",
                            fontSize = 12.sp,
                            color = subject.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                if (availableNotes.isEmpty()) {
                    Text(
                        "Semua catatan yang ada sudah tertaut ke topik ini, atau belum ada catatan di Workspace.",
                        fontSize = 13.sp,
                        color = LernspaceTextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableNotes.forEach { note ->
                            val isPdf = note.canvasType == NoteCanvasType.PDF_DOCUMENT
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, LernspaceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectNote(note) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = if (isPdf) Color(0xFFDC2626) else AccentBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LernspaceDark,
                                            maxLines = 1
                                        )
                                        val subText = buildString {
                                            append(note.subject)
                                            if (!note.topicName.isNullOrBlank()) {
                                                append(" • ").append(note.topicName)
                                            }
                                        }
                                        Text(
                                            text = subText,
                                            fontSize = 11.sp,
                                            color = LernspaceTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        "Tautkan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
            }
        }
    }
}
