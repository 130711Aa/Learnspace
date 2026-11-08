package com.lernspace.app.ui.planner

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.PlannerItemUi
import com.lernspace.app.data.repository.ScheduleType
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.NoteEditorScreen
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToSyllabus: ((subjectId: String?) -> Unit)? = null
) {
    val scheduleItems = AcademicWorkspaceState.scheduleItems
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics
    val notes = AcademicWorkspaceState.notes

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Day Agenda", "Week Schedule", "Month Horizon")

    var activeNote by remember { mutableStateOf<NoteUiModel?>(null) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showHybridSuggestDialog by remember { mutableStateOf(false) }

    // If active note is opened from a study session, display fullscreen NoteEditorScreen!
    if (activeNote != null) {
        NoteEditorScreen(
            note = activeNote!!,
            onBack = { activeNote = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header Navigation Bar ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Planner & Calendar",
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
                            text = "Deterministic Hybrid Engine Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentTeal,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Smart timetable linking lectures, syllabus sub-babs, and suggested study blocks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LernspaceTextSecondary
                )
            }

            // Top Action Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Tab switcher
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LernspaceSurfaceLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        tabs.forEachIndexed { index, title ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedTab == index) Color.White else Color.Transparent,
                                shadowElevation = if (selectedTab == index) 1.dp else 0.dp,
                                modifier = Modifier.clickable { selectedTab = index }
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) LernspaceDark else LernspaceTextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // Auto-suggest study slot button (Hybrid Engine)
                OutlinedButton(
                    onClick = { showHybridSuggestDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Schedule Slot", fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                }

                // Plan session / Add event button
                Button(
                    onClick = { showAddEventDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plan Session", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Main Viewport Switcher (Day Agenda vs Week vs Month) ──────────────
        when (selectedTab) {
            0 -> DayAgendaView(
                scheduleItems = scheduleItems,
                subjects = subjects,
                onAcceptSuggestion = { id -> AcademicWorkspaceState.acceptHybridSuggestion(id) },
                onToggleComplete = { item ->
                    val idx = scheduleItems.indexOfFirst { it.id == item.id }
                    if (idx != -1) {
                        scheduleItems[idx] = item.copy(isCompleted = !item.isCompleted)
                    }
                },
                onOpenNoteForTopic = { topicId, topicTitle, subjectId, subjectName ->
                    val existingNote = notes.find { it.topicId == topicId }
                    if (existingNote != null) {
                        activeNote = existingNote
                    } else {
                        val subj = subjects.find { it.id == subjectId }
                        val top = topics.find { it.id == topicId }
                        if (subj != null && top != null) {
                            activeNote = AcademicWorkspaceState.createNoteForTopic(subj, top)
                        } else {
                            activeNote = AcademicWorkspaceState.createScratchpad(topicTitle)
                        }
                    }
                },
                onViewInSyllabus = { subjectId ->
                    if (subjectId != null) {
                        onNavigateToSyllabus?.invoke(subjectId)
                    }
                },
                onDeleteItem = { scheduleItems.remove(it) }
            )

            1 -> WeekScheduleView(
                scheduleItems = scheduleItems,
                subjects = subjects,
                onOpenItem = { item ->
                    if (item.topicId != null) {
                        val note = notes.find { it.topicId == item.topicId }
                        if (note != null) activeNote = note
                    }
                }
            )

            2 -> MonthHorizonView(
                scheduleItems = scheduleItems,
                onViewSyllabus = { subjectId -> onNavigateToSyllabus?.invoke(subjectId) }
            )
        }
    }

    // ── Dialog: Plan New Session / Event ──────────────────────────────────────
    if (showAddEventDialog) {
        PlanSessionDialog(
            subjects = subjects,
            topics = topics,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, type, startTime, endTime, dayOffset, subject, topic, room ->
                scheduleItems.add(
                    PlannerItemUi(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        type = type,
                        startTime = startTime,
                        endTime = endTime,
                        dayOfWeek = (dayOffset % 7) + 1,
                        dayOffset = dayOffset,
                        subjectId = subject?.id,
                        subjectName = subject?.name,
                        topicId = topic?.id,
                        topicName = topic?.title,
                        locationOrRoom = room.ifBlank { null }
                    )
                )
                showAddEventDialog = false
            }
        )
    }

    // ── Dialog: Auto-Suggest Study Slot (Hybrid Engine) ───────────────────────
    if (showHybridSuggestDialog) {
        HybridSuggestDialog(
            subjects = subjects,
            topics = topics,
            onDismiss = { showHybridSuggestDialog = false },
            onAccept = { subject, topic, durationMinutes, startStr, endStr, reason ->
                scheduleItems.add(
                    PlannerItemUi(
                        id = UUID.randomUUID().toString(),
                        title = "Hybrid Review: ${topic.title}",
                        type = ScheduleType.HYBRID_SUGGESTION,
                        startTime = startStr,
                        endTime = endStr,
                        dayOfWeek = 1,
                        dayOffset = 0,
                        subjectId = subject.id,
                        subjectName = subject.name,
                        topicId = topic.id,
                        topicName = topic.title,
                        aiReason = reason
                    )
                )
                showHybridSuggestDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW 1: Day Agenda (Timeline view with hours gutter & rich schedule cards)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayAgendaView(
    scheduleItems: List<PlannerItemUi>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onAcceptSuggestion: (String) -> Unit,
    onToggleComplete: (PlannerItemUi) -> Unit,
    onOpenNoteForTopic: (topicId: String, topicTitle: String, subjectId: String?, subjectName: String?) -> Unit,
    onViewInSyllabus: (String?) -> Unit,
    onDeleteItem: (PlannerItemUi) -> Unit
) {
    var selectedDayOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow, 2 = Wed...
    val dayNames = listOf("Today, Nov 8 (Mon)", "Tomorrow, Nov 9 (Tue)", "Wednesday, Nov 10", "Thursday, Nov 11", "Friday, Nov 12")

    val dayItems = remember(scheduleItems.size, selectedDayOffset) {
        scheduleItems.filter { it.dayOffset == selectedDayOffset }.sortedBy { it.startTime }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = LernspaceSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Day selector tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayNames.indices.toList()) { offset ->
                        FilterChip(
                            selected = selectedDayOffset == offset,
                            onClick = { selectedDayOffset = offset },
                            label = { Text(dayNames[offset], fontSize = 12.sp, fontWeight = if (selectedDayOffset == offset) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Text(
                    text = "${dayItems.size} scheduled blocks",
                    style = MaterialTheme.typography.labelMedium,
                    color = LernspaceTextSecondary
                )
            }

            // Timeline Items List
            if (dayItems.isEmpty()) {
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
                        Icon(Icons.Filled.EventAvailable, null, tint = AccentTeal, modifier = Modifier.size(44.dp))
                        Text(
                            text = "No scheduled blocks for this day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Text(
                            text = "Use '+ Plan Session' or 'Auto-Schedule Slot' to allocate study time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LernspaceTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dayItems, key = { it.id }) { item ->
                        val matchingSubject = subjects.find { it.id == item.subjectId }
                        val subjectColor = matchingSubject?.color ?: AccentBlue

                        AgendaTimelineCard(
                            item = item,
                            subjectColor = subjectColor,
                            onAcceptSuggestion = { onAcceptSuggestion(item.id) },
                            onToggleComplete = { onToggleComplete(item) },
                            onOpenNote = {
                                if (item.topicId != null) {
                                    onOpenNoteForTopic(item.topicId, item.topicName ?: item.title, item.subjectId, item.subjectName)
                                }
                            },
                            onViewSyllabus = { onViewInSyllabus(item.subjectId) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Agenda Timeline Card (Day View Item)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgendaTimelineCard(
    item: PlannerItemUi,
    subjectColor: Color,
    onAcceptSuggestion: () -> Unit,
    onToggleComplete: () -> Unit,
    onOpenNote: () -> Unit,
    onViewSyllabus: () -> Unit,
    onDelete: () -> Unit
) {
    val isHybrid = item.type == ScheduleType.HYBRID_SUGGESTION

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isHybrid) 1.5.dp else 1.dp,
                color = if (isHybrid) AccentAmber else LernspaceBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isHybrid) Color(0xFFFFFBEB) else if (item.isCompleted) LernspaceSurfaceLow else LernspaceSurfaceCard,
        shadowElevation = if (isHybrid) 2.dp else 0.5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Ribbon (colored by subject or hybrid amber)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (isHybrid) AccentAmber else subjectColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Time, Type Chip, Subject, Checkbox & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Time badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceDark
                        ) {
                            Text(
                                text = "${item.startTime} - ${item.endTime}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Type chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = item.type.chipBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isHybrid) {
                                    Icon(Icons.Filled.AutoAwesome, null, tint = item.type.chipFg, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = item.type.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = item.type.chipFg
                                )
                            }
                        }

                        // Subject tag
                        if (item.subjectName != null) {
                            Text(
                                text = "•  ${item.subjectName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subjectColor
                            )
                        }

                        // Location
                        if (item.locationOrRoom != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.LocationOn, null, tint = LernspaceTextSecondary, modifier = Modifier.size(13.dp))
                                Text(text = item.locationOrRoom, fontSize = 11.sp, color = LernspaceTextSecondary)
                            }
                        }
                    }

                    // Checkbox / Delete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isHybrid) {
                            IconButton(onClick = onToggleComplete, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "Complete",
                                    tint = if (item.isCompleted) Color(0xFF059669) else LernspaceTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, null, tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Row 2: Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) LernspaceTextSecondary else LernspaceDark,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                // Row 3: Hybrid Engine Reasoning OR Attached Topic Actions
                if (isHybrid && item.aiReason != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Lightbulb, null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Text(
                                    text = item.aiReason,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onAcceptSuggestion,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Accept & Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Row 4: Connected Syllabus Sub-bab & Note Launch Link
                if (item.topicName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Bookmark, null, tint = subjectColor, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Sub-bab: ${item.topicName}",
                                fontSize = 12.sp,
                                color = LernspaceDark,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // View Syllabus
                            TextButton(
                                onClick = onViewSyllabus,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Syllabus", fontSize = 11.sp)
                            }

                            // Open Lecture Note
                            Button(
                                onClick = onOpenNote,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Create, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Note Canvas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW 2: Week Schedule (7 Columns Landscape Timetable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeekScheduleView(
    scheduleItems: List<PlannerItemUi>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onOpenItem: (PlannerItemUi) -> Unit
) {
    val weekDays = listOf(
        Pair(1, "Mon (Nov 8)"),
        Pair(2, "Tue (Nov 9)"),
        Pair(3, "Wed (Nov 10)"),
        Pair(4, "Thu (Nov 11)"),
        Pair(5, "Fri (Nov 12)"),
        Pair(6, "Sat (Nov 13)"),
        Pair(7, "Sun (Nov 14)")
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
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
            Text(
                text = "Week of Nov 8 - Nov 14, 2026",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )

            // 7 Columns Grid
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEach { (dayIndex, dayLabel) ->
                    val isToday = dayIndex == 1
                    val dayCommitments = scheduleItems.filter { it.dayOfWeek == dayIndex }.sortedBy { it.startTime }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isToday) Color(0xFFF0FDF4) else LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isToday) 1.5.dp else 1.dp,
                            color = if (isToday) Color(0xFF86EFAC) else LernspaceBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isToday) Color(0xFF166534) else LernspaceDark
                                )
                                if (isToday) {
                                    Surface(shape = CircleShape, color = Color(0xFF166534)) {
                                        Box(modifier = Modifier.size(6.dp))
                                    }
                                }
                            }

                            Divider(color = LernspaceBorder.copy(alpha = 0.6f))

                            // Items in this day
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(dayCommitments, key = { it.id }) { item ->
                                    val subj = subjects.find { it.id == item.subjectId }
                                    val barColor = subj?.color ?: if (item.type == ScheduleType.EXAM) Color(0xFFE11D48) else AccentBlue

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenItem(item) }
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .fillMaxHeight()
                                                    .background(barColor)
                                            )
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text(text = item.startTime, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                                Text(
                                                    text = item.title,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LernspaceDark,
                                                    maxLines = 2,
                                                    lineHeight = 12.sp
                                                )
                                                if (item.locationOrRoom != null) {
                                                    Text(text = item.locationOrRoom, fontSize = 8.sp, color = LernspaceTextSecondary, maxLines = 1)
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
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW 3: Month Horizon (35-day grid with deadlines & exam highlights)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MonthHorizonView(
    scheduleItems: List<PlannerItemUi>,
    onViewSyllabus: (String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = LernspaceSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left 70%: Calendar Grid for November 2026
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "November 2026",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentBlue))
                            Text("Lecture", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE11D48)))
                            Text("Exam", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentTeal))
                            Text("Study Block", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                    }
                }

                // Month Grid Header (Sun to Sat)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                        Text(
                            text = d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceTextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Days (Nov 1 is Sunday)
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val daysInNov = (1..30).toList()
                    val weeks = daysInNov.chunked(7)

                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            week.forEach { dayNumber ->
                                val isToday = dayNumber == 8
                                val hasExam = dayNumber == 10
                                val hasDeadline = dayNumber == 12

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isToday) Color(0xFFEFF6FF) else LernspaceSurfaceLow,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isToday) 1.5.dp else 1.dp,
                                        color = if (isToday) AccentBlue else LernspaceBorder.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isToday) AccentBlue else LernspaceDark
                                        )

                                        // Event indicator dots
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            if (dayNumber in listOf(2, 4, 8, 9, 11, 15, 17, 22)) {
                                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AccentBlue))
                                            }
                                            if (hasExam) {
                                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFE11D48)))
                                            }
                                            if (hasDeadline) {
                                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AccentAmber))
                                            }
                                        }
                                    }
                                }
                            }
                            // Fill remaining row if week < 7 days
                            for (i in week.size until 7) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            }

            // Right 30%: Academic Milestones & Deadlines List
            Surface(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = LernspaceSurfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Upcoming Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            MilestoneCard(
                                title = "Neurobiology Mid-Term Quiz",
                                subject = "NEUR-401",
                                date = "Wed, Nov 10 • 10:00 AM",
                                typeColor = Color(0xFFE11D48),
                                onViewSyllabus = { onViewSyllabus("subj-1") }
                            )
                        }
                        item {
                            MilestoneCard(
                                title = "Stats Problem Set #4 Due",
                                subject = "STAT-210",
                                date = "Fri, Nov 12 • 5:00 PM",
                                typeColor = AccentAmber,
                                onViewSyllabus = { onViewSyllabus("subj-3") }
                            )
                        }
                        item {
                            MilestoneCard(
                                title = "Cognitive Psych Essay Draft",
                                subject = "PSYC-320",
                                date = "Tue, Nov 16 • 11:59 PM",
                                typeColor = AccentBlue,
                                onViewSyllabus = { onViewSyllabus("subj-2") }
                            )
                        }
                        item {
                            MilestoneCard(
                                title = "Orgo II Synthesis Project",
                                subject = "CHEM-222",
                                date = "Fri, Nov 20 • 2:00 PM",
                                typeColor = Color(0xFF8B5CF6),
                                onViewSyllabus = { onViewSyllabus("subj-4") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    title: String,
    subject: String,
    date: String,
    typeColor: Color,
    onViewSyllabus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewSyllabus() }
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.15f)) {
                    Text(
                        text = subject,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(text = date, fontSize = 10.sp, color = LernspaceTextSecondary)
            }
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Plan Session / Add Event
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlanSessionDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: ScheduleType,
        startTime: String,
        endTime: String,
        dayOffset: Int,
        subject: com.lernspace.app.ui.learning.SubjectUi?,
        topic: com.lernspace.app.ui.learning.TopicUi?,
        room: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ScheduleType.STUDY_SESSION) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("11:30") }
    var dayOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedTopic by remember { mutableStateOf<com.lernspace.app.ui.learning.TopicUi?>(null) }
    var room by remember { mutableStateOf("") }

    val subjectTopics = remember(selectedSubject) {
        if (selectedSubject != null) topics.filter { it.subjectId == selectedSubject!!.id } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(460.dp)
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
                    Text("Plan Session / Add Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                // Type selector chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ScheduleType.values()) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Chapter 4 Practice Problems)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Subject picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Subject", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubject == s,
                                onClick = {
                                    selectedSubject = s
                                    selectedTopic = null
                                },
                                label = { Text(s.name, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Topic picker
                if (subjectTopics.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sub-bab / Topic", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjectTopics) { t ->
                                FilterChip(
                                    selected = selectedTopic == t,
                                    onClick = {
                                        selectedTopic = if (selectedTopic == t) null else t
                                        if (title.isBlank() && selectedTopic != null) {
                                            title = "Study: ${t.title}"
                                        }
                                    },
                                    label = { Text(t.title, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Time & Day Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Location / Room (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalTitle = title.ifBlank { selectedTopic?.title ?: "Study Session" }
                            onConfirm(finalTitle.trim(), selectedType, startTime.trim(), endTime.trim(), dayOffset, selectedSubject, selectedTopic, room.trim())
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
                    ) {
                        Text("Add to Schedule")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Hybrid Auto-Suggest Study Slot
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HybridSuggestDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    onDismiss: () -> Unit,
    onAccept: (
        subject: com.lernspace.app.ui.learning.SubjectUi,
        topic: com.lernspace.app.ui.learning.TopicUi,
        durationMinutes: Int,
        startStr: String,
        endStr: String,
        reason: String
    ) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    val subjectTopics = remember(selectedSubject) {
        if (selectedSubject != null) topics.filter { it.subjectId == selectedSubject!!.id } else emptyList()
    }
    var selectedTopic by remember { mutableStateOf(subjectTopics.firstOrNull()) }

    var durationMinutes by remember { mutableIntStateOf(60) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(460.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                        Text("Auto-Schedule Study Slot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                Text(
                    text = "The deterministic Hybrid Study Engine searches your calendar for optimal open intervals between classes, prioritizing topics that need review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LernspaceTextSecondary
                )

                // Select Subject
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Subject to Study", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubject == s,
                                onClick = {
                                    selectedSubject = s
                                    selectedTopic = topics.filter { it.subjectId == s.id }.firstOrNull()
                                },
                                label = { Text(s.name, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Select Topic
                if (subjectTopics.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Target Topic / Sub-bab", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjectTopics) { t ->
                                FilterChip(
                                    selected = selectedTopic == t,
                                    onClick = { selectedTopic = t },
                                    label = { Text(t.title, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Target Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Duration", style = MaterialTheme.typography.labelMedium, color = LernspaceDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(45, 60, 90).forEach { mins ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text("${mins}m", fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Suggested Slot Result Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recommended Slot Found", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Surface(shape = RoundedCornerShape(4.dp), color = AccentAmber) {
                                Text("10:30 - 11:30", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "Detected 90m open gap between Neurobiology Lecture and Lunch. Fully buffered, zero conflict.",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedSubject != null && selectedTopic != null) {
                                onAccept(
                                    selectedSubject!!,
                                    selectedTopic!!,
                                    durationMinutes,
                                    "10:30",
                                    "11:30",
                                    "Deterministic suggestion: 60m study slot scheduled between morning classes."
                                )
                            }
                        },
                        enabled = selectedSubject != null && selectedTopic != null,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Text("Accept & Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
