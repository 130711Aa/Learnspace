package com.lernspace.app.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.NoteEditorScreen
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.util.UUID

// Data models for Learning Syllabus UI
data class SubjectUi(
    val id: String,
    val name: String,
    val code: String,
    val description: String = "",
    val color: Color = AccentBlue
)

enum class TopicStatus(
    val label: String,
    val chipBg: Color,
    val chipFg: Color,
    val icon: @Composable () -> Unit
) {
    NOT_STARTED(
        "Not Started",
        Color(0xFFF1F5F9),
        Color(0xFF64748B),
        { Icon(Icons.Filled.RadioButtonUnchecked, null, modifier = Modifier.size(13.dp)) }
    ),
    LEARNING(
        "Learning",
        Color(0xFFDBEAFE),
        Color(0xFF1D4ED8),
        { Icon(Icons.Filled.AutoStories, null, modifier = Modifier.size(13.dp)) }
    ),
    NEEDS_REVIEW(
        "Needs Review",
        Color(0xFFFEF3C7),
        Color(0xFFD97706),
        { Icon(Icons.Filled.PriorityHigh, null, modifier = Modifier.size(13.dp)) }
    ),
    COMFORTABLE(
        "Comfortable",
        Color(0xFFD1FAE5),
        Color(0xFF059669),
        { Icon(Icons.Filled.Check, null, modifier = Modifier.size(13.dp)) }
    ),
    MASTERED(
        "Mastered",
        Color(0xFFEDE9FE),
        Color(0xFF7C3AED),
        { Icon(Icons.Filled.Star, null, modifier = Modifier.size(13.dp)) }
    )
}

data class TopicUi(
    val id: String,
    val subjectId: String,
    val title: String,
    val description: String = "",
    val status: TopicStatus = TopicStatus.NOT_STARTED,
    val targetDate: String? = null
)

private val SubjectColorPalette = listOf(
    AccentBlue,
    AccentTeal,
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF6366F1)  // Indigo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    onNavigateToWorkspace: ((subjectId: String?) -> Unit)? = null
) {
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics
    val notes = AcademicWorkspaceState.notes

    var selectedSubjectId by remember { mutableStateOf(AcademicWorkspaceState.selectedLearningSubjectId ?: subjects.firstOrNull()?.id ?: "") }
    LaunchedEffect(AcademicWorkspaceState.selectedLearningSubjectId) {
        AcademicWorkspaceState.selectedLearningSubjectId?.let { id ->
            selectedSubjectId = id
        }
    }

    // Dialog & filter states
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var topicToEdit by remember { mutableStateOf<TopicUi?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<TopicStatus?>(null) }

    val activeSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull()

    // Filtered topics for active subject (directly observed from SnapshotStateList)
    val filteredTopics = topics.filter { topic ->
        topic.subjectId == selectedSubjectId &&
                (selectedStatusFilter == null || topic.status == selectedStatusFilter) &&
                (searchQuery.isBlank() ||
                        topic.title.contains(searchQuery, ignoreCase = true) ||
                        topic.description.contains(searchQuery, ignoreCase = true))
    }

    // Progress metrics
    val subjectAllTopics = topics.filter { it.subjectId == selectedSubjectId }
    val totalCount = subjectAllTopics.size
    val comfortableCount = subjectAllTopics.count { it.status == TopicStatus.COMFORTABLE || it.status == TopicStatus.MASTERED }
    val needsReviewCount = subjectAllTopics.count { it.status == TopicStatus.NEEDS_REVIEW }
    val masteryPercent = if (totalCount > 0) (comfortableCount.toFloat() / totalCount * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header Navigation Bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Learning Syllabus & Topics",
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
                            text = "${subjects.size} Subjects Enrolled",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Structured course curriculum with attached lecture notes and mastery progression",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onNavigateToWorkspace != null) {
                    OutlinedButton(
                        onClick = { onNavigateToWorkspace(selectedSubjectId) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in Workspace Desk", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = { showAddSubjectDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Subject", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Landscape 2-Column Core Viewport ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left Column: Enrolled Subjects List (Width: ~340dp) ────────────
            Surface(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Text(
                            text = "${subjects.size} courses",
                            style = MaterialTheme.typography.labelMedium,
                            color = LernspaceTextSecondary
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subjects, key = { it.id }) { subject ->
                            val isSelected = subject.id == selectedSubjectId
                            val count = topics.count { it.subjectId == subject.id }
                            val mastered = topics.count { it.subjectId == subject.id && (it.status == TopicStatus.COMFORTABLE || it.status == TopicStatus.MASTERED) }
                            val pct = if (count > 0) (mastered.toFloat() / count * 100).toInt() else 0

                            SubjectCardItem(
                                subject = subject,
                                topicCount = count,
                                masteryPercent = pct,
                                isSelected = isSelected,
                                onClick = { selectedSubjectId = subject.id },
                                onDelete = {
                                    if (subjects.size > 1) {
                                        subjects.remove(subject)
                                        topics.removeAll { it.subjectId == subject.id }
                                        notes.removeAll { it.subjectId == subject.id }
                                        if (selectedSubjectId == subject.id) {
                                            selectedSubjectId = subjects.first().id
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── Right Column: Selected Subject Syllabus & Topics ──────────────
            Surface(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = LernspaceSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                shadowElevation = 1.dp
            ) {
                if (activeSubject == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a subject to view its syllabus topics", color = LernspaceTextSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Subject Header & Mastery Progress Card
                        SubjectSummaryBanner(
                            subject = activeSubject,
                            totalTopics = totalCount,
                            comfortableCount = comfortableCount,
                            needsReviewCount = needsReviewCount,
                            masteryPercent = masteryPercent,
                            onAddTopic = { showAddTopicDialog = true },
                            onOpenWorkspace = { onNavigateToWorkspace?.invoke(activeSubject.id) }
                        )

                        // 2. Search & Status Filter Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search topics in ${activeSubject.name}...", fontSize = 13.sp) },
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
                                    .weight(1f)
                                    .height(46.dp)
                            )

                            // Status Filter Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedStatusFilter == null,
                                        onClick = { selectedStatusFilter = null },
                                        label = { Text("All ($totalCount)", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                items(TopicStatus.values()) { status ->
                                    val count = subjectAllTopics.count { it.status == status }
                                    if (count > 0 || selectedStatusFilter == status) {
                                        FilterChip(
                                            selected = selectedStatusFilter == status,
                                            onClick = {
                                                selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                            },
                                            label = { Text("${status.label} ($count)", fontSize = 12.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Topics List with attached note cards!
                        if (filteredTopics.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LernspaceSurfaceLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = LernspaceTextSecondary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotEmpty() || selectedStatusFilter != null)
                                            "No topics match your current filter"
                                        else
                                            "No topics added yet for ${activeSubject.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LernspaceTextSecondary
                                    )
                                    Button(
                                        onClick = { showAddTopicDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = activeSubject.color)
                                    ) {
                                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Topic")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredTopics, key = { it.id }) { topic ->
                                    val index = subjectAllTopics.indexOfFirst { it.id == topic.id } + 1
                                    val topicNotes = AcademicWorkspaceState.getNotesForTopic(topic.id)

                                    TopicCardInteractive(
                                        index = index,
                                        topic = topic,
                                        subjectColor = activeSubject.color,
                                        attachedNote = topicNotes.firstOrNull(),
                                        onOpenNote = { note -> AcademicWorkspaceState.openNoteEditor(note) },
                                        onCreateNote = {
                                            val newNote = AcademicWorkspaceState.createNoteForTopic(activeSubject, topic)
                                            AcademicWorkspaceState.openNoteEditor(newNote)
                                        },
                                        onStatusChange = { newStatus ->
                                            AcademicWorkspaceState.updateTopicStatus(topic.id, newStatus)
                                        },
                                        onEdit = { topicToEdit = topic },
                                        onDelete = {
                                            topics.remove(topic)
                                            notes.removeAll { it.topicId == topic.id }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Add New Subject ───────────────────────────────────────────────
    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, code, desc, color ->
                val newId = UUID.randomUUID().toString()
                val newSubject = SubjectUi(
                    id = newId,
                    name = name,
                    code = code,
                    description = desc,
                    color = color
                )
                subjects.add(newSubject)
                com.lernspace.app.data.local.AcademicDatabaseManager.persistSubject(newSubject)
                selectedSubjectId = newId
                showAddSubjectDialog = false
            }
        )
    }

    // ── Dialog: Add New Topic ─────────────────────────────────────────────────
    if (showAddTopicDialog && activeSubject != null) {
        AddTopicDialog(
            subjectName = activeSubject.name,
            onDismiss = { showAddTopicDialog = false },
            onConfirm = { title, desc, status, targetDate ->
                val newTopic = TopicUi(
                    id = UUID.randomUUID().toString(),
                    subjectId = activeSubject.id,
                    title = title,
                    description = desc,
                    status = status,
                    targetDate = targetDate.ifBlank { null }
                )
                topics.add(newTopic)
                com.lernspace.app.data.local.AcademicDatabaseManager.persistTopic(newTopic)
                showAddTopicDialog = false
            }
        )
    }

    // ── Dialog: Edit Topic ────────────────────────────────────────────────────
    topicToEdit?.let { topic ->
        EditTopicDialog(
            topic = topic,
            onDismiss = { topicToEdit = null },
            onConfirm = { updatedTitle, updatedDesc, updatedStatus, updatedTargetDate ->
                val idx = topics.indexOfFirst { it.id == topic.id }
                if (idx != -1) {
                    val updated = topics[idx].copy(
                        title = updatedTitle,
                        description = updatedDesc,
                        status = updatedStatus,
                        targetDate = updatedTargetDate.ifBlank { null }
                    )
                    topics[idx] = updated
                    com.lernspace.app.data.local.AcademicDatabaseManager.persistTopic(updated)
                }
                topicToEdit = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Subject Card Item (Left Sidebar)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SubjectCardItem(
    subject: SubjectUi,
    topicCount: Int,
    masteryPercent: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) LernspaceSurfaceLow else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) subject.color.copy(alpha = 0.5f) else LernspaceBorder.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Colored Indicator Pill
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(subject.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = subject.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = LernspaceDark,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary
                        )
                        Text(text = "•", color = LernspaceTextSecondary, fontSize = 10.sp)
                        Text(
                            text = "$topicCount topics",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    // Linear Mastery Bar
                    LinearProgressIndicator(
                        progress = { masteryPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = subject.color,
                        trackColor = Color(0xFFE2E8F0),
                    )
                }
            }

            // Right Options / Percentage
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = LernspaceTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Subject", color = Color(0xFFEF4444)) },
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
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Subject Summary & Mastery Progress Banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SubjectSummaryBanner(
    subject: SubjectUi,
    totalTopics: Int,
    comfortableCount: Int,
    needsReviewCount: Int,
    masteryPercent: Int,
    onAddTopic: () -> Unit,
    onOpenWorkspace: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(subject.color)
                    )
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = subject.color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = subject.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = subject.color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenWorkspace,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Description, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Desk Notes", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onAddTopic,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = subject.color),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Topic", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (subject.description.isNotBlank()) {
                Text(
                    text = subject.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LernspaceTextSecondary
                )
            }

            // Stats row & progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "$totalTopics Total Topics",
                        style = MaterialTheme.typography.labelMedium,
                        color = LernspaceDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$comfortableCount Mastered / Comfortable",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (needsReviewCount > 0) {
                        Text(
                            text = "$needsReviewCount Need Review",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "$masteryPercent% Mastery",
                    style = MaterialTheme.typography.titleSmall,
                    color = subject.color,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { masteryPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = subject.color,
                trackColor = Color(0xFFCBD5E1)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Interactive Topic Card with Attached Note & Status Switcher
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopicCardInteractive(
    index: Int,
    topic: TopicUi,
    subjectColor: Color,
    attachedNote: NoteUiModel?,
    onOpenNote: (NoteUiModel) -> Unit,
    onCreateNote: () -> Unit,
    onStatusChange: (TopicStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Title, Index, Status & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Topic Index & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Topic Number Badge
                    Surface(
                        shape = CircleShape,
                        color = subjectColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$index",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = subjectColor
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = LernspaceDark
                        )
                        if (topic.description.isNotBlank()) {
                            Text(
                                text = topic.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = LernspaceTextSecondary,
                                maxLines = 2
                            )
                        }
                        if (topic.targetDate != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CalendarToday, null, tint = LernspaceTextSecondary, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "Target: ${topic.targetDate}",
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }
                    }
                }

                // Right: Interactive Status Pill & Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clickable Status Pill (Opens dropdown to update understanding)
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = topic.status.chipBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, topic.status.chipFg.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { showStatusMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                topic.status.icon()
                                Text(
                                    text = topic.status.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = topic.status.chipFg
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Change Status",
                                    tint = topic.status.chipFg,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            TopicStatus.values().forEach { st ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            st.icon()
                                            Text(st.label, fontWeight = if (st == topic.status) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        onStatusChange(st)
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Edit Topic Button
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Topic",
                            tint = LernspaceTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete Topic Button
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Topic",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Row 2: Attached Lecture Note Integration Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LernspaceSurfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (attachedNote != null) {
                    // Note already exists for this topic
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = subjectColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachedNote.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = attachedNote.snippet,
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { onOpenNote(attachedNote) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Create, null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Note", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // No note created yet for this topic
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NoteAdd,
                                contentDescription = null,
                                tint = LernspaceTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "No lecture notes created for this chapter yet",
                                fontSize = 12.sp,
                                color = LernspaceTextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = onCreateNote,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write Note", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Add Subject Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, desc: String, color: Color) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(SubjectColorPalette.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add New Subject", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Molecular Genetics)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. BIOL-301)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Scope (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

                // Color picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Subject Accent Color", style = MaterialTheme.typography.labelMedium, color = LernspaceTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SubjectColorPalette.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable { selectedColor = col }
                                    .then(
                                        if (selectedColor == col)
                                            Modifier.border(2.5.dp, LernspaceDark, CircleShape)
                                        else
                                            Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == col) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), code.trim(), desc.trim(), selectedColor)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Create Subject")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Add Topic Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddTopicDialog(
    subjectName: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, status: TopicStatus, targetDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TopicStatus.NOT_STARTED) }
    var targetDate by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Add Topic", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        Text("Under $subjectName", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Topic / Chapter Title (e.g. Chapter 4: Photosynthesis)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Key Concepts / Sub-topics (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Study Date (e.g. Nov 28)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Initial status selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Initial Mastery Level", style = MaterialTheme.typography.labelMedium, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TopicStatus.values()) { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label, fontSize = 12.sp) },
                                leadingIcon = if (status == st) { { st.icon() } } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), desc.trim(), status, targetDate.trim())
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Add Topic")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Edit Topic Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EditTopicDialog(
    topic: TopicUi,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, status: TopicStatus, targetDate: String) -> Unit
) {
    var title by remember { mutableStateOf(topic.title) }
    var desc by remember { mutableStateOf(topic.description) }
    var status by remember { mutableStateOf(topic.status) }
    var targetDate by remember { mutableStateOf(topic.targetDate ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Topic", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Topic Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Key Concepts / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Study Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Status selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mastery Level", style = MaterialTheme.typography.labelMedium, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TopicStatus.values()) { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label, fontSize = 12.sp) },
                                leadingIcon = if (status == st) { { st.icon() } } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), desc.trim(), status, targetDate.trim())
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
