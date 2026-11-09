package com.lernspace.app.ui.workspace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.*
import com.lernspace.app.ui.workspace.pdf.PdfNoteHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    onNavigateToSyllabus: ((subjectId: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val notes = AcademicWorkspaceState.notes
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics

    var selectedFilterKey by remember { mutableStateOf("ALL") } // "ALL", "SCRATCHPAD", or subjectId
    var searchQuery by remember { mutableStateOf("") }
    var showNewNoteDialog by remember { mutableStateOf(false) }

    // Direct PDF Import Launcher
    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = PdfNoteHelper.getFileName(context, it).removeSuffix(".pdf")
            val pageCount = PdfNoteHelper.getPdfPageCount(context, it)
            val timeStr = "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val newNote = NoteUiModel(
                id = UUID.randomUUID().toString(),
                title = fileName,
                subject = "PDF Document",
                date = timeStr,
                snippet = "Anotasi PDF ($pageCount Halaman)...",
                canvasType = NoteCanvasType.PDF_DOCUMENT,
                paperStyle = PaperStyle.BLANK,
                pdfUri = it.toString(),
                pdfTotalPages = pageCount,
                pages = mutableListOf(NotePageData(pageIndex = 0))
            )
            notes.add(0, newNote)
            com.lernspace.app.data.local.AcademicDatabaseManager.persistNote(newNote)
            AcademicWorkspaceState.openNoteEditor(newNote)
        }
    }

    // Filtered notes
    val filteredNotes = remember(notes.size, selectedFilterKey, searchQuery) {
        notes.filter { note ->
            val matchesFilter = when (selectedFilterKey) {
                "ALL" -> true
                "SCRATCHPAD" -> note.isScratchpad
                else -> note.subjectId == selectedFilterKey
            }
            val matchesSearch = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.snippet.contains(searchQuery, ignoreCase = true) ||
                    note.subject.contains(searchQuery, ignoreCase = true) ||
                    (note.topicName != null && note.topicName.contains(searchQuery, ignoreCase = true))

            matchesFilter && matchesSearch
        }
    }

    val scratchpadCount = notes.count { it.isScratchpad }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header Bar ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Workspace",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LernspaceDark,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceSurfaceLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Text(
                        text = "${notes.size} Catatan",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Action Buttons (Impor PDF, Quick Scratchpad, New Note)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Import PDF Button
                OutlinedButton(
                    onClick = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Impor PDF", fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                }

                // Quick Scratchpad Button
                OutlinedButton(
                    onClick = {
                        val newScratchpad = AcademicWorkspaceState.createScratchpad()
                        AcademicWorkspaceState.openNoteEditor(newScratchpad)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Scratchpad", fontWeight = FontWeight.SemiBold)
                }

                // New Lecture / Subject Note Button
                Button(
                    onClick = { showNewNoteDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Note", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Search & Filter Tabs Row ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search all notes, topics, or subjects...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = LernspaceTextSecondary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LernspaceDark,
                    unfocusedBorderColor = LernspaceBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = LernspaceSurfaceLow
                ),
                modifier = Modifier
                    .width(320.dp)
                    .height(46.dp)
            )

            // Horizontal Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All Notes"
                item {
                    FilterChip(
                        selected = selectedFilterKey == "ALL",
                        onClick = { selectedFilterKey = "ALL" },
                        label = { Text("All Notes (${notes.size})", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // "Quick Scratchpads"
                item {
                    FilterChip(
                        selected = selectedFilterKey == "SCRATCHPAD",
                        onClick = { selectedFilterKey = "SCRATCHPAD" },
                        leadingIcon = { Icon(Icons.Filled.Draw, null, modifier = Modifier.size(13.dp)) },
                        label = { Text("Scratchpads ($scratchpadCount)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Each Enrolled Subject
                items(subjects, key = { it.id }) { subject ->
                    val count = notes.count { it.subjectId == subject.id }
                    FilterChip(
                        selected = selectedFilterKey == subject.id,
                        onClick = { selectedFilterKey = subject.id },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(subject.color)
                            )
                        },
                        label = { Text("${subject.name} ($count)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // ── Main Content Area: Notes Grid ─────────────────────────────────────
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LernspaceSurfaceCard)
                    .border(1.dp, LernspaceBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = LernspaceTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "No notes match your search '$searchQuery'"
                        else
                            "No notes found in this folder",
                        style = MaterialTheme.typography.titleMedium,
                        color = LernspaceDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a note for a syllabus chapter or start a freeform scratchpad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LernspaceTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showNewNoteDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create First Note")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    val matchingSubject = subjects.find { it.id == note.subjectId }
                    val subjectColor = matchingSubject?.color ?: if (note.isScratchpad) Color(0xFF64748B) else AccentBlue

                    WorkspaceNoteCard(
                        note = note,
                        subjectColor = subjectColor,
                        onOpen = { AcademicWorkspaceState.openNoteEditor(note) },
                        onViewInSyllabus = {
                            if (note.subjectId != null) {
                                onNavigateToSyllabus?.invoke(note.subjectId)
                            }
                        },
                        onDelete = {
                            AcademicWorkspaceState.deleteNote(note.id)
                        }
                    )
                }
            }
        }
    }

    // ── Dialog: Create New Note (linked or unlinked) ──────────────────────────
    if (showNewNoteDialog) {
        CreateNoteDialog(
            subjects = subjects,
            topics = topics,
            onDismiss = { showNewNoteDialog = false },
            onConfirm = { title, subject, topic, canvasType, paperStyle, pdfUri, pdfPages ->
                val timeStr = "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val newNote = NoteUiModel(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    subject = if (canvasType == NoteCanvasType.INFINITE_SCRATCHPAD) "Quick Scratchpad" else (subject?.name ?: "General Study"),
                    date = timeStr,
                    snippet = if (canvasType == NoteCanvasType.PDF_DOCUMENT) "Dokumen PDF ($pdfPages Halaman)..." else "Catatan ${paperStyle.displayName}...",
                    subjectId = if (canvasType == NoteCanvasType.INFINITE_SCRATCHPAD) null else subject?.id,
                    topicId = if (canvasType == NoteCanvasType.INFINITE_SCRATCHPAD) null else topic?.id,
                    topicName = if (canvasType == NoteCanvasType.INFINITE_SCRATCHPAD) null else topic?.title,
                    canvasType = canvasType,
                    paperStyle = paperStyle,
                    pdfUri = pdfUri,
                    pdfTotalPages = pdfPages,
                    pages = mutableListOf(NotePageData(pageIndex = 0)),
                    isScratchpad = (canvasType == NoteCanvasType.INFINITE_SCRATCHPAD)
                )
                notes.add(0, newNote)
                com.lernspace.app.data.local.AcademicDatabaseManager.persistNote(newNote)
                showNewNoteDialog = false
                AcademicWorkspaceState.openNoteEditor(newNote)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Workspace Note Card with Syllabus Sub-bab link
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WorkspaceNoteCard(
    note: NoteUiModel,
    subjectColor: Color,
    onOpen: () -> Unit,
    onViewInSyllabus: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Accent Bar (Colored by Subject)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(subjectColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Subject Tag + Date + Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Subject Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = subjectColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (note.isScratchpad) {
                                    Icon(Icons.Filled.Draw, null, tint = subjectColor, modifier = Modifier.size(11.dp))
                                } else {
                                    Icon(Icons.Filled.Folder, null, tint = subjectColor, modifier = Modifier.size(11.dp))
                                }
                                Text(
                                    text = note.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subjectColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Date & Dropdown Menu
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = note.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = LernspaceTextSecondary
                            )

                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.MoreVert, null, tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (note.topicId != null) {
                                        DropdownMenuItem(
                                            text = { Text("View in Syllabus") },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showMenu = false
                                                onViewInSyllabus()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete Note", color = Color(0xFFEF4444)) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Linked Sub-bab / Topic Badge (if linked to syllabus!)
                    if (note.topicName != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder.copy(alpha = 0.8f)),
                            modifier = Modifier.clickable { onViewInSyllabus() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Filled.Bookmark, null, tint = subjectColor, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Sub-bab: ${note.topicName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LernspaceDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Note Title
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark,
                        maxLines = 2
                    )

                    // Note Snippet preview
                    Text(
                        text = note.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = LernspaceTextSecondary,
                        maxLines = 2
                    )
                }

                // Bottom Action: Open Canvas Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stylus & Inking Ready",
                        fontSize = 11.sp,
                        color = LernspaceTextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Open Canvas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = subjectColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Create New Note (Linked to Subject/Topic or Freeform Scratchpad)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CreateNoteDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: com.lernspace.app.ui.learning.SubjectUi?,
        topic: com.lernspace.app.ui.learning.TopicUi?,
        canvasType: NoteCanvasType,
        paperStyle: PaperStyle,
        pdfUri: String?,
        pdfPages: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedCanvasType by remember { mutableStateOf(NoteCanvasType.PAGED_NOTE) }
    var selectedPaperStyle by remember { mutableStateOf(PaperStyle.RULED) }

    var selectedPdfUri by remember { mutableStateOf<String?>(null) }
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    var selectedPdfPages by remember { mutableIntStateOf(1) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = PdfNoteHelper.getFileName(context, it).removeSuffix(".pdf")
            val pages = PdfNoteHelper.getPdfPageCount(context, it)
            selectedPdfUri = it.toString()
            selectedPdfName = fileName
            selectedPdfPages = pages
            if (title.isBlank()) {
                title = fileName
            }
        }
    }

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedTopic by remember { mutableStateOf<com.lernspace.app.ui.learning.TopicUi?>(null) }

    val subjectTopics = remember(selectedSubject) {
        if (selectedSubject != null) {
            topics.filter { it.subjectId == selectedSubject!!.id }
        } else {
            emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(520.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Create New Note / Document", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        Text("Pilih format media di awal (terkunci permanen untuk konsistensi)", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                // 1. FORMAT MEDIA SELECTOR (3 OPTIONS)
                Text("1. Pilih Format Media Kanvas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LernspaceDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option A: Paged Note
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCanvasType == NoteCanvasType.PAGED_NOTE) Color(0xFFEFF6FF) else LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedCanvasType == NoteCanvasType.PAGED_NOTE) AccentBlue else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCanvasType = NoteCanvasType.PAGED_NOTE }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.AutoStories, null, tint = if (selectedCanvasType == NoteCanvasType.PAGED_NOTE) AccentBlue else LernspaceDark, modifier = Modifier.size(20.dp))
                            Text("Buku Berhalaman", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Text("Page 1 of N", fontSize = 9.sp, color = LernspaceTextSecondary)
                        }
                    }

                    // Option B: PDF Document
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCanvasType == NoteCanvasType.PDF_DOCUMENT) Color(0xFFFEE2E2) else LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedCanvasType == NoteCanvasType.PDF_DOCUMENT) Color(0xFFDC2626) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedCanvasType = NoteCanvasType.PDF_DOCUMENT
                                if (selectedPdfUri == null) {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, null, tint = if (selectedCanvasType == NoteCanvasType.PDF_DOCUMENT) Color(0xFFDC2626) else LernspaceDark, modifier = Modifier.size(20.dp))
                            Text("Anotasi PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Text("Impor PDF", fontSize = 9.sp, color = LernspaceTextSecondary)
                        }
                    }

                    // Option C: Infinite Scratchpad
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCanvasType == NoteCanvasType.INFINITE_SCRATCHPAD) Color(0xFFFEF3C7) else LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedCanvasType == NoteCanvasType.INFINITE_SCRATCHPAD) AccentAmber else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCanvasType = NoteCanvasType.INFINITE_SCRATCHPAD }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Draw, null, tint = if (selectedCanvasType == NoteCanvasType.INFINITE_SCRATCHPAD) AccentAmber else LernspaceDark, modifier = Modifier.size(20.dp))
                            Text("Scratchpad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Text("Kanvas Bebas", fontSize = 9.sp, color = LernspaceTextSecondary)
                        }
                    }
                }

                // 2. SUB-SELECTOR BASED ON TYPE
                if (selectedCanvasType == NoteCanvasType.PAGED_NOTE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Template Kertas (Terkunci Permanen):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PaperStyle.values().forEach { style ->
                                FilterChip(
                                    selected = selectedPaperStyle == style,
                                    onClick = { selectedPaperStyle = style },
                                    label = { Text(style.displayName, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                } else if (selectedCanvasType == NoteCanvasType.PDF_DOCUMENT) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPdfName ?: "Belum ada file PDF yang dipilih",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (selectedPdfUri != null) "$selectedPdfPages Halaman terdeteksi" else "Ketuk tombol untuk memilih dokumen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LernspaceTextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Pilih PDF", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 3. TITLE INPUT
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Catatan") },
                    placeholder = { Text("e.g. Action Potential & Ion Flux") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // 4. SUBJECT SELECTOR
                if (selectedCanvasType != NoteCanvasType.INFINITE_SCRATCHPAD) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hubungkan ke Mata Kuliah:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjects) { subj ->
                                FilterChip(
                                    selected = selectedSubject == subj,
                                    onClick = {
                                        selectedSubject = subj
                                        selectedTopic = null
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(subj.color)
                                        )
                                    },
                                    label = { Text(subj.name, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // 5. TOPIC SELECTOR
                    if (subjectTopics.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Hubungkan ke Sub-bab / Topik (Opsional):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(subjectTopics) { top ->
                                    FilterChip(
                                        selected = selectedTopic == top,
                                        onClick = {
                                            selectedTopic = if (selectedTopic == top) null else top
                                            if (title.isBlank() && selectedTopic != null) {
                                                title = top.title
                                            }
                                        },
                                        label = { Text(top.title, fontSize = 10.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalTitle = title.ifBlank {
                                when (selectedCanvasType) {
                                    NoteCanvasType.PDF_DOCUMENT -> selectedPdfName ?: "Dokumen PDF"
                                    NoteCanvasType.INFINITE_SCRATCHPAD -> "Untitled Scratchpad"
                                    NoteCanvasType.PAGED_NOTE -> selectedTopic?.title ?: "Buku Catatan"
                                }
                            }
                            onConfirm(
                                finalTitle.trim(),
                                selectedSubject,
                                selectedTopic,
                                selectedCanvasType,
                                selectedPaperStyle,
                                selectedPdfUri,
                                selectedPdfPages
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Buka Kanvas")
                    }
                }
            }
        }
    }
}
