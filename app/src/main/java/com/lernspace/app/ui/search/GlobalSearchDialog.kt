package com.lernspace.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.PlannerItemUi
import com.lernspace.app.data.repository.TaskUiModel
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.model.NoteUiModel
import kotlinx.coroutines.delay

/**
 * Top bar Search Pill strictly following the Stitch design system
 * (lernspace_home_dashboard_landscape), optimized for touch/tablet (no desktop keyboard shortcuts).
 */
@Composable
fun GlobalSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color(0xFF45474C),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Search concepts, citations, syllabi...",
                color = Color(0xFF75777D),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class SearchCategory(val label: String, val icon: ImageVector) {
    ALL("Semua", Icons.Filled.Search),
    NOTES("Catatan", Icons.Filled.EditNote),
    TOPICS("Silabus", Icons.AutoMirrored.Filled.MenuBook),
    TASKS("Tugas", Icons.Filled.CheckCircleOutline),
    SUBJECTS("Mata Kuliah", Icons.Filled.School),
    SCHEDULE("Jadwal", Icons.Filled.AccessTime)
}

sealed class SearchResultItem {
    data class NoteResult(val note: NoteUiModel) : SearchResultItem()
    data class TopicResult(val topic: TopicUi, val subject: SubjectUi?) : SearchResultItem()
    data class TaskResult(val task: TaskUiModel) : SearchResultItem()
    data class SubjectResult(val subject: SubjectUi) : SearchResultItem()
    data class ScheduleResult(val item: PlannerItemUi) : SearchResultItem()
}

@Composable
fun GlobalSearchDialog(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    val allNotes = AcademicWorkspaceState.notes
    val allTopics = AcademicWorkspaceState.topics
    val allTasks = AcademicWorkspaceState.tasks
    val allSubjects = AcademicWorkspaceState.subjects
    val allSchedule = AcademicWorkspaceState.scheduleItems

    // Compute search matches
    val trimmedQuery = searchQuery.trim().lowercase()

    val matchedNotes = remember(trimmedQuery, allNotes.size, allNotes.toList()) {
        if (trimmedQuery.isBlank()) allNotes.toList()
        else allNotes.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.snippet.lowercase().contains(trimmedQuery) ||
                    it.subject.lowercase().contains(trimmedQuery) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true)
        }
    }

    val matchedTopics = remember(trimmedQuery, allTopics.size, allTopics.toList()) {
        if (trimmedQuery.isBlank()) allTopics.toList()
        else allTopics.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.description.lowercase().contains(trimmedQuery)
        }
    }

    val matchedTasks = remember(trimmedQuery, allTasks.size, allTasks.toList()) {
        if (trimmedQuery.isBlank()) allTasks.toList()
        else allTasks.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    it.notes.lowercase().contains(trimmedQuery) ||
                    (it.subjectName?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true)
        }
    }

    val matchedSubjects = remember(trimmedQuery, allSubjects.size, allSubjects.toList()) {
        if (trimmedQuery.isBlank()) allSubjects.toList()
        else allSubjects.filter {
            it.name.lowercase().contains(trimmedQuery) ||
                    it.code.lowercase().contains(trimmedQuery) ||
                    it.description.lowercase().contains(trimmedQuery)
        }
    }

    val matchedSchedule = remember(trimmedQuery, allSchedule.size, allSchedule.toList()) {
        if (trimmedQuery.isBlank()) allSchedule.toList()
        else allSchedule.filter {
            it.title.lowercase().contains(trimmedQuery) ||
                    (it.subjectName?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.locationOrRoom?.lowercase()?.contains(trimmedQuery) == true) ||
                    (it.topicName?.lowercase()?.contains(trimmedQuery) == true)
        }
    }

    val totalCount = matchedNotes.size + matchedTopics.size + matchedTasks.size + matchedSubjects.size + matchedSchedule.size

    val results = remember(selectedCategory, matchedNotes, matchedTopics, matchedTasks, matchedSubjects, matchedSchedule) {
        val list = mutableListOf<SearchResultItem>()
        when (selectedCategory) {
            SearchCategory.ALL -> {
                matchedNotes.forEach { list.add(SearchResultItem.NoteResult(it)) }
                matchedTopics.forEach { topic ->
                    val subj = allSubjects.find { it.id == topic.subjectId }
                    list.add(SearchResultItem.TopicResult(topic, subj))
                }
                matchedTasks.forEach { list.add(SearchResultItem.TaskResult(it)) }
                matchedSubjects.forEach { list.add(SearchResultItem.SubjectResult(it)) }
                matchedSchedule.forEach { list.add(SearchResultItem.ScheduleResult(it)) }
            }
            SearchCategory.NOTES -> matchedNotes.forEach { list.add(SearchResultItem.NoteResult(it)) }
            SearchCategory.TOPICS -> matchedTopics.forEach { topic ->
                val subj = allSubjects.find { it.id == topic.subjectId }
                list.add(SearchResultItem.TopicResult(topic, subj))
            }
            SearchCategory.TASKS -> matchedTasks.forEach { list.add(SearchResultItem.TaskResult(it)) }
            SearchCategory.SUBJECTS -> matchedSubjects.forEach { list.add(SearchResultItem.SubjectResult(it)) }
            SearchCategory.SCHEDULE -> matchedSchedule.forEach { list.add(SearchResultItem.ScheduleResult(it)) }
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(760.dp)
                .heightIn(min = 400.dp, max = 580.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── Search Input Row ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(LernspaceSurfaceLow)
                        .border(1.dp, Color(0xFFE2E4E8), CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF45474C),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = LernspaceTextPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(LernspaceDark),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search concepts, citations, syllabi, notes, tasks...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF888B92)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Touch-friendly Close Icon Button (No ESC shortcut label)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF45474C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Category Filter Pills (Fully responsive to taps) ──────────
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(SearchCategory.values()) { category ->
                        val isSelected = selectedCategory == category
                        val count = when (category) {
                            SearchCategory.ALL -> totalCount
                            SearchCategory.NOTES -> matchedNotes.size
                            SearchCategory.TOPICS -> matchedTopics.size
                            SearchCategory.TASKS -> matchedTasks.size
                            SearchCategory.SUBJECTS -> matchedSubjects.size
                            SearchCategory.SCHEDULE -> matchedSchedule.size
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF45474C)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFE0E3E5)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF1E293B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFF1F5F9)
                )

                // ── Results Area ──────────────────────────────────────────────
                if (selectedCategory == SearchCategory.ALL && searchQuery.isBlank()) {
                    // "Semua" + Empty query: Show Popular Academic Concepts & Quick Access Overview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "PENCARIAN CEPAT & TOPIK POPULER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val suggestedQueries = listOf(
                            "Resting Membrane Potential",
                            "Action Potential",
                            "Synaptic Transmission & LTP",
                            "Working Memory",
                            "Bayesian Prior Distributions",
                            "MCMC Sampling",
                            "Aldol Condensation",
                            "NEUR-401",
                            "STAT-210"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestedQueries.forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LernspaceSurfaceLow,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { searchQuery = suggestion }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = AccentTeal,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = LernspaceDark
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "CATATAN & TUGAS TERBARU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(allNotes.take(2)) { note ->
                                NoteResultCard(
                                    note = note,
                                    onClick = {
                                        AcademicWorkspaceState.openNoteEditor(note)
                                        onDismiss()
                                    }
                                )
                            }
                            items(allTasks.take(2)) { task ->
                                TaskResultCard(
                                    task = task,
                                    onClick = {
                                        AcademicWorkspaceState.selectedPlannerTab = 1
                                        onNavigate(Screen.Planner.route)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    // No matches state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada hasil untuk \"$searchQuery\"" else "Tidak ada item di kategori ${selectedCategory.label}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba kata kunci lain atau pilih kategori yang berbeda.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    // Direct category listing & filtered search results
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "DAFTAR ${selectedCategory.label.uppercase()} (${results.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results) { item ->
                                when (item) {
                                    is SearchResultItem.NoteResult -> {
                                        NoteResultCard(
                                            note = item.note,
                                            onClick = {
                                                AcademicWorkspaceState.openNoteEditor(item.note)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.TopicResult -> {
                                        TopicResultCard(
                                            topic = item.topic,
                                            subject = item.subject,
                                            onClick = {
                                                AcademicWorkspaceState.selectedLearningSubjectId = item.topic.subjectId
                                                onNavigate(Screen.Learning.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.TaskResult -> {
                                        TaskResultCard(
                                            task = item.task,
                                            onClick = {
                                                AcademicWorkspaceState.selectedPlannerTab = 1
                                                onNavigate(Screen.Planner.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.SubjectResult -> {
                                        SubjectResultCard(
                                            subject = item.subject,
                                            onClick = {
                                                AcademicWorkspaceState.selectedLearningSubjectId = item.subject.id
                                                onNavigate(Screen.Learning.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    is SearchResultItem.ScheduleResult -> {
                                        ScheduleResultCard(
                                            item = item.item,
                                            onClick = {
                                                AcademicWorkspaceState.selectedPlannerTab = 0
                                                onNavigate(Screen.Planner.route)
                                                onDismiss()
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
    }
}

@Composable
private fun NoteResultCard(
    note: NoteUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note.snippet,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Catatan • ${note.subject}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TopicResultCard(
    topic: TopicUi,
    subject: SubjectUi?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentTealContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topic.description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = topic.status.chipBg
            ) {
                Text(
                    text = "${subject?.code ?: "Silabus"} • ${topic.status.label}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = topic.status.chipFg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TaskResultCard(
    task: TaskUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (task.priority == "HIGH") AccentRedContainer else Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Checklist,
                    contentDescription = null,
                    tint = if (task.priority == "HIGH") AccentRed else AccentAmber,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deadline: ${task.dueDate} • ${task.subjectName ?: ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = if (task.priority == "HIGH") AccentRedContainer else Color(0xFFE2E8F0)
            ) {
                Text(
                    text = "Tugas • ${task.priority}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.priority == "HIGH") AccentRed else Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SubjectResultCard(
    subject: SubjectUi,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(subject.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = subject.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${subject.code}: ${subject.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subject.description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Mata Kuliah",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ScheduleResultCard(
    item: PlannerItemUi,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LernspaceSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LernspaceDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.startTime} - ${item.endTime} • ${item.locationOrRoom ?: (item.subjectName ?: "")}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = "Jadwal • ${item.type.name}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
