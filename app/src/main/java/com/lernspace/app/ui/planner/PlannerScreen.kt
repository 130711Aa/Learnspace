package com.lernspace.app.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.lernspace.app.data.repository.*
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.components.WheelTimePicker
import com.lernspace.app.ui.planner.ocr.ImportKrsDialog
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.NoteEditorScreen
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToSyllabus: ((subjectId: String?) -> Unit)? = null
) {
    val scheduleItems = AcademicWorkspaceState.scheduleItems
    val weeklyClasses = AcademicWorkspaceState.weeklyClasses
    val tasks = AcademicWorkspaceState.tasks
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics
    val notes = AcademicWorkspaceState.notes

    var selectedTab by remember { mutableIntStateOf(AcademicWorkspaceState.selectedPlannerTab) }
    LaunchedEffect(AcademicWorkspaceState.selectedPlannerTab) {
        selectedTab = AcademicWorkspaceState.selectedPlannerTab
    }
    val tabs = listOf(
        Pair("Kalender & Deadline", Icons.Filled.CalendarMonth),
        Pair("Daftar Tugas", Icons.Filled.Checklist),
        Pair("Jadwal Mapel Mingguan", Icons.Filled.AccessTime),
        Pair("Day Agenda & Auto-Study", Icons.Filled.Today)
    )

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddWeeklyClassDialog by remember { mutableStateOf(false) }
    var showImportKrsDialog by remember { mutableStateOf(false) }
    var showHybridSuggestDialog by remember { mutableStateOf(false) }
    var showAddDayAgendaDialog by remember { mutableStateOf(false) }
    var selectedCalendarDay by remember { mutableIntStateOf(8) } // Selected date in Nov 2026
    var selectedDayAgendaOffset by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Top Header Navigation Bar ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Planner & Jadwal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LernspaceDark,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceSurfaceLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Text(
                        text = "${tasks.count { !it.isCompleted }} Tugas Aktif",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Top Action Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTab == 3) {
                    Button(
                        onClick = { showHybridSuggestDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Study", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAddDayAgendaDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Agenda", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                    }
                } else if (selectedTab == 2) {
                    OutlinedButton(
                        onClick = { showImportKrsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = LernspaceDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Teks Jadwal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }

                    Button(
                        onClick = { showAddWeeklyClassDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Jadwal Mapel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Add Task / Deadline button
                    Button(
                        onClick = { showAddTaskDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Input Tugas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Set Weekly Class button
                    OutlinedButton(
                        onClick = { showAddWeeklyClassDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Jadwal Mapel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                    }
                }
            }
        }

        // ── Navigation Tabs Switcher Bar ──────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = LernspaceSurfaceLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == index) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == index) 1.dp else 0.dp,
                        modifier = Modifier.clickable {
                            selectedTab = index
                            AcademicWorkspaceState.selectedPlannerTab = index
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selectedTab == index) AccentBlue else LernspaceTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) LernspaceDark else LernspaceTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── Main Viewport Switcher (Fills 100% of remaining vertical space!) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> CalendarDeadlineFullView(
                    selectedDay = selectedCalendarDay,
                    onSelectDay = { selectedCalendarDay = it },
                    tasks = tasks,
                    weeklyClasses = weeklyClasses,
                    subjects = subjects,
                    onToggleTask = { taskId -> AcademicWorkspaceState.toggleTaskCompletion(taskId) },
                    onAddNewTaskForDay = { showAddTaskDialog = true },
                    onViewSyllabus = { subjectId -> onNavigateToSyllabus?.invoke(subjectId) }
                )

                1 -> TaskListView(
                    tasks = tasks,
                    subjects = subjects,
                    topics = topics,
                    notes = notes,
                    onToggleTask = { taskId -> AcademicWorkspaceState.toggleTaskCompletion(taskId) },
                    onDeleteTask = { task -> AcademicWorkspaceState.deleteTask(task.id) },
                    onAddNewTask = { showAddTaskDialog = true },
                    onOpenNoteForTopic = { topicId, topicTitle, subjectId, subjectName ->
                        val existingNote = notes.find { it.topicId == topicId }
                        if (existingNote != null) {
                            AcademicWorkspaceState.openNoteEditor(existingNote)
                        } else {
                            val subj = subjects.find { it.id == subjectId }
                            val top = topics.find { it.id == topicId }
                            if (subj != null && top != null) {
                                val newNote = AcademicWorkspaceState.createNoteForTopic(subj, top)
                                AcademicWorkspaceState.openNoteEditor(newNote)
                            } else {
                                val newNote = AcademicWorkspaceState.createScratchpad(topicTitle)
                                AcademicWorkspaceState.openNoteEditor(newNote)
                            }
                        }
                    },
                    onViewSyllabus = { subjectId -> onNavigateToSyllabus?.invoke(subjectId) }
                )

                2 -> WeeklyTimetableBuilderView(
                    weeklyClasses = weeklyClasses,
                    subjects = subjects,
                    onAddClass = { showAddWeeklyClassDialog = true },
                    onOpenImportKrs = { showImportKrsDialog = true },
                    onDeleteClass = {
                        weeklyClasses.remove(it)
                        com.lernspace.app.data.local.AcademicDatabaseManager.deleteCalendarEvent(it.id)
                    }
                )

                3 -> DayAgendaView(
                    scheduleItems = scheduleItems,
                    weeklyClasses = weeklyClasses,
                    tasks = tasks,
                    subjects = subjects,
                    selectedDayOffset = selectedDayAgendaOffset,
                    onSelectDayOffset = { selectedDayAgendaOffset = it },
                    onAcceptSuggestion = { id -> AcademicWorkspaceState.acceptHybridSuggestion(id) },
                    onTogglePlannerComplete = { item ->
                        val idx = scheduleItems.indexOfFirst { it.id == item.id }
                        if (idx != -1) {
                            val updated = item.copy(isCompleted = !item.isCompleted)
                            scheduleItems[idx] = updated
                            com.lernspace.app.data.local.AcademicDatabaseManager.persistPlannerItem(updated)
                        }
                    },
                    onToggleTaskComplete = { taskId ->
                        AcademicWorkspaceState.toggleTaskCompletion(taskId)
                    },
                    onOpenNoteForSubject = { subjectId, subjectName ->
                        val existingNote = notes.find { it.subjectId == subjectId }
                        if (existingNote != null) {
                            AcademicWorkspaceState.openNoteEditor(existingNote)
                        } else {
                            val subj = subjects.find { it.id == subjectId }
                            if (subj != null) {
                                val newNote = AcademicWorkspaceState.createNoteForTopic(
                                    subj,
                                    TopicUi(
                                        id = UUID.randomUUID().toString(),
                                        subjectId = subj.id,
                                        title = "Catatan ${subj.name}",
                                        description = "",
                                        status = TopicStatus.LEARNING
                                    )
                                )
                                AcademicWorkspaceState.openNoteEditor(newNote)
                            } else {
                                val newNote = AcademicWorkspaceState.createScratchpad(subjectName ?: "Catatan Kuliah")
                                AcademicWorkspaceState.openNoteEditor(newNote)
                            }
                        }
                    },
                    onOpenNoteForTopic = { topicId, topicTitle, subjectId, subjectName ->
                        val existingNote = notes.find { it.topicId == topicId }
                        if (existingNote != null) {
                            AcademicWorkspaceState.openNoteEditor(existingNote)
                        } else {
                            val subj = subjects.find { it.id == subjectId }
                            val top = topics.find { it.id == topicId }
                            if (subj != null && top != null) {
                                val newNote = AcademicWorkspaceState.createNoteForTopic(subj, top)
                                AcademicWorkspaceState.openNoteEditor(newNote)
                            } else {
                                val newNote = AcademicWorkspaceState.createScratchpad(topicTitle)
                                AcademicWorkspaceState.openNoteEditor(newNote)
                            }
                        }
                    },
                    onViewInSyllabus = { subjectId ->
                        if (subjectId != null) {
                            onNavigateToSyllabus?.invoke(subjectId)
                        }
                    },
                    onAutoSchedule = { showHybridSuggestDialog = true },
                    onAddManualAgenda = { showAddDayAgendaDialog = true },
                    onDeleteItem = {
                        scheduleItems.remove(it)
                        com.lernspace.app.data.local.AcademicDatabaseManager.deleteCalendarEvent(it.id)
                    }
                )
            }
        }
    }

    // ── Dialog: Input Tugas & Set Deadline ────────────────────────────────────
    if (showAddTaskDialog) {
        InputTaskDialog(
            subjects = subjects,
            topics = topics,
            initialDayNumber = selectedCalendarDay,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, subject, topic, dueDate, dueDayNumber, dueTime, priority, estMins, notesStr ->
                val newTask = TaskUiModel(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    subjectId = subject?.id,
                    subjectName = subject?.name,
                    topicId = topic?.id,
                    topicName = topic?.title,
                    dueDate = dueDate,
                    dueDayNumber = dueDayNumber,
                    dueTime = dueTime,
                    priority = priority,
                    estimatedMinutes = estMins,
                    isCompleted = false,
                    notes = notesStr
                )
                AcademicWorkspaceState.addTask(newTask)
                showAddTaskDialog = false
            }
        )
    }

    // ── Dialog: Set Jadwal Mapel Mingguan ─────────────────────────────────────
    if (showAddWeeklyClassDialog) {
        SetWeeklyClassDialog(
            subjects = subjects,
            onDismiss = { showAddWeeklyClassDialog = false },
            onConfirm = { subject, dayOfWeek, startTime, endTime, room, lecturer, type ->
                val newClass = WeeklyClassUi(
                    id = UUID.randomUUID().toString(),
                    subjectId = subject.id,
                    subjectName = subject.name,
                    subjectCode = subject.code,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    room = room.ifBlank { null },
                    lecturer = lecturer.ifBlank { null },
                    type = type
                )
                weeklyClasses.add(newClass)
                com.lernspace.app.data.local.AcademicDatabaseManager.persistWeeklyClass(newClass, subject)
            }
        )
    }

    // ── Dialog: Smart Import Jadwal KRS (ML Kit OCR & Fast Paste) ────────────
    if (showImportKrsDialog) {
        ImportKrsDialog(
            onDismiss = { showImportKrsDialog = false },
            onConfirmImport = { items ->
                val colorPalette = listOf(
                    AccentBlue,
                    AccentTeal,
                    Color(0xFF8B5CF6),
                    Color(0xFFF59E0B),
                    Color(0xFFEC4899),
                    Color(0xFF10B981),
                    Color(0xFF6366F1)
                )

                val newSubjectsList = mutableListOf<com.lernspace.app.ui.learning.SubjectUi>()
                val newClassesList = mutableListOf<WeeklyClassUi>()

                items.forEachIndexed { index, parsed ->
                    // 1. Find or create Subject
                    var subj = subjects.find { it.name.equals(parsed.subjectName, ignoreCase = true) }
                    if (subj == null) {
                        val color = colorPalette[(subjects.size + newSubjectsList.size) % colorPalette.size]
                        subj = com.lernspace.app.ui.learning.SubjectUi(
                            id = UUID.randomUUID().toString(),
                            name = parsed.subjectName,
                            code = parsed.subjectCode.trim(),
                            description = "Jadwal dari paste teks",
                            color = color
                        )
                        subjects.add(subj)
                        newSubjectsList.add(subj)
                    }

                    // 2. Create WeeklyClassUi
                    val newClass = WeeklyClassUi(
                        id = UUID.randomUUID().toString(),
                        subjectId = subj.id,
                        subjectName = subj.name,
                        subjectCode = subj.code,
                        dayOfWeek = parsed.dayOfWeek,
                        startTime = parsed.startTime,
                        endTime = parsed.endTime,
                        room = parsed.room.ifBlank { null },
                        lecturer = parsed.lecturer.ifBlank { null },
                        type = "Lecture"
                    )
                    weeklyClasses.add(newClass)
                    newClassesList.add(newClass)
                }

                // Batch persist safely to SQLite
                com.lernspace.app.data.local.AcademicDatabaseManager.importWeeklyClassesBatch(
                    newSubjects = newSubjectsList,
                    newClasses = newClassesList
                )
                showImportKrsDialog = false
            }
        )
    }

    // ── Dialog: Auto-Suggest Study Slot (Hybrid Engine) ───────────────────────
    if (showHybridSuggestDialog) {
        HybridSuggestDialog(
            subjects = subjects,
            topics = topics,
            weeklyClasses = weeklyClasses,
            tasks = tasks,
            scheduleItems = scheduleItems,
            initialDayOffset = selectedDayAgendaOffset,
            onDismiss = { showHybridSuggestDialog = false },
            onAccept = { subject, customSubjectName, topic, customTopicName, durationMinutes, dayOffset, dayOfWeek, startStr, endStr, reason ->
                val finalSubName = subject?.name ?: customSubjectName.ifBlank { "Belajar Mandiri" }
                val finalTopicTitle = topic?.title ?: customTopicName.ifBlank { "Sesi Fokus Mandiri" }
                val newItem = PlannerItemUi(
                    id = UUID.randomUUID().toString(),
                    title = "Hybrid Review: $finalTopicTitle",
                    type = ScheduleType.HYBRID_SUGGESTION,
                    startTime = startStr,
                    endTime = endStr,
                    dayOfWeek = dayOfWeek,
                    dayOffset = dayOffset,
                    subjectId = subject?.id,
                    subjectName = finalSubName,
                    topicId = topic?.id,
                    topicName = finalTopicTitle,
                    aiReason = reason
                )
                scheduleItems.add(newItem)
                com.lernspace.app.data.local.AcademicDatabaseManager.persistPlannerItem(newItem)
                showHybridSuggestDialog = false
            }
        )
    }

    // ── Dialog: Input Day Agenda Manual ──────────────────────────────────────
    if (showAddDayAgendaDialog) {
        InputDayAgendaDialog(
            subjects = subjects,
            initialDayOffset = selectedDayAgendaOffset,
            onDismiss = { showAddDayAgendaDialog = false },
            onConfirm = { title, type, subject, customSubjectName, startTime, endTime, dayOffset, dayOfWeek, room ->
                val finalSubName = subject?.name ?: customSubjectName.ifBlank { null }
                val newItem = PlannerItemUi(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    type = type,
                    startTime = startTime,
                    endTime = endTime,
                    dayOfWeek = dayOfWeek,
                    dayOffset = dayOffset,
                    subjectId = subject?.id,
                    subjectName = finalSubName,
                    locationOrRoom = room.ifBlank { null },
                    isCompleted = false
                )
                scheduleItems.add(newItem)
                com.lernspace.app.data.local.AcademicDatabaseManager.persistPlannerItem(newItem)
                showAddDayAgendaDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW 1: Kalender Penuh & Deadline Tugas Interaktif (Interactive Full Calendar)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarDeadlineFullView(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    tasks: List<TaskUiModel>,
    weeklyClasses: List<WeeklyClassUi>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onToggleTask: (String) -> Unit,
    onAddNewTaskForDay: () -> Unit,
    onViewSyllabus: (String?) -> Unit
) {
    // Determine weekday for selected day (Nov 1, 2026 was Sunday = 7; Nov 2 was Monday = 1)
    val dayOfWeek = if (selectedDay % 7 == 1) 7 else (selectedDay % 7) - 1 // 1=Mon, 2=Tue.. 7=Sun
    val classesOnDay = weeklyClasses.filter { it.dayOfWeek == if (dayOfWeek == 0) 7 else dayOfWeek }.sortedBy { it.startTime }
    val tasksOnDay = tasks.filter { it.dueDayNumber == selectedDay }

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
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Left 65%: Full November Calendar Grid ─────────────────────────
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "November 2026",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Text(
                            text = "Klik tanggal untuk cek jadwal kuliah & deadline tugas hari itu",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary
                        )
                    }

                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Text("Deadline Tugas", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                            Text("Jadwal Kuliah", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                    }
                }

                // Days of week header (Sun, Mon, Tue...)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab").forEach { d ->
                        Text(
                            text = d,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceTextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // 30 Days Grid for November
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val daysInNov = (1..30).toList()
                    val weeks = daysInNov.chunked(7)

                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            week.forEach { dayNum ->
                                val isSelected = dayNum == selectedDay
                                val isToday = dayNum == 8
                                val dayTasks = tasks.filter { it.dueDayNumber == dayNum }
                                val dayTasksCount = dayTasks.size
                                val hasPendingTask = dayTasks.any { !it.isCompleted }

                                val dOfWeek = if (dayNum % 7 == 1) 7 else (dayNum % 7) - 1
                                val hasClass = weeklyClasses.any { it.dayOfWeek == (if (dOfWeek == 0) 7 else dOfWeek) }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onSelectDay(dayNum) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else if (isToday) Color(0xFFF0FDF4) else LernspaceSurfaceLow,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 1.dp,
                                        color = if (isSelected) AccentBlue else if (isToday) Color(0xFF16A34A) else LernspaceBorder.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) AccentBlue else if (isToday) Color(0xFF16A34A) else LernspaceDark
                                            )
                                            if (isToday) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF16A34A)) {
                                                    Text("HARI INI", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                }
                                            }
                                        }

                                        // Badges (Red for Deadline, Blue for Class)
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            // Task deadline pill (Vivid Red for pending)
                                            if (dayTasksCount > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (hasPendingTask) Color(0xFFFEF2F2) else Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = "$dayTasksCount Tugas",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (hasPendingTask) Color(0xFFDC2626) else Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Class indicator dot (Vivid Blue for classes)
                                            if (hasClass) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                                                    Text("Kuliah", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Filler boxes for incomplete row
                            for (i in week.size until 7) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            }

            // ── Right 35%: Selected Date Details Panel (Drilldown) ────────────
            Surface(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                color = LernspaceSurfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header for selected date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nov $selectedDay, 2026",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                            Text(
                                text = "${classesOnDay.size} Kuliah • ${tasksOnDay.size} Deadline Tugas",
                                style = MaterialTheme.typography.labelSmall,
                                color = LernspaceTextSecondary
                            )
                        }

                        Button(
                            onClick = onAddNewTaskForDay,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Tugas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = LernspaceBorder)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section 1: Deadline Tugas Hari Ini (Red marker)
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                Text(
                                    text = "DEADLINE TUGAS ($selectedDay Nov)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        if (tasksOnDay.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Tidak ada deadline tugas di tanggal ini. Waktu luang aman!",
                                        fontSize = 11.sp,
                                        color = LernspaceTextSecondary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            items(tasksOnDay, key = { it.id }) { task ->
                                val subj = subjects.find { it.id == task.subjectId }
                                val subColor = subj?.color ?: AccentBlue

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (task.isCompleted) Color(0xFFF8FAFC) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (task.isCompleted) LernspaceBorder else subColor.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                IconButton(onClick = { onToggleTask(task.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(
                                                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                        contentDescription = "Done",
                                                        tint = if (task.isCompleted) Color(0xFF059669) else LernspaceTextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Text(
                                                    text = task.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (task.isCompleted) LernspaceTextSecondary else LernspaceDark,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = subColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = task.dueTime,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = subColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (task.subjectName != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = task.subjectName,
                                                    fontSize = 11.sp,
                                                    color = subColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (task.topicName != null) {
                                                    Text(
                                                        text = "Bab: ${task.topicName}",
                                                        fontSize = 10.sp,
                                                        color = LernspaceTextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: Jadwal Kuliah Hari Ini (Blue marker)
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                                Text(
                                    text = "JADWAL KULIAH RUTIN",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }

                        if (classesOnDay.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Hari ini libur / tidak ada jadwal kuliah mingguan.",
                                        fontSize = 11.sp,
                                        color = LernspaceTextSecondary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            items(classesOnDay, key = { it.id }) { c ->
                                val subj = subjects.find { it.id == c.subjectId }
                                val subColor = subj?.color ?: AccentBlue

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(subColor)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${c.startTime} - ${c.endTime}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LernspaceDark
                                                )
                                                Surface(shape = RoundedCornerShape(4.dp), color = subColor.copy(alpha = 0.12f)) {
                                                    Text(text = c.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subColor, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(
                                                text = if (c.subjectCode.isNotBlank()) "${c.subjectName} (${c.subjectCode})" else c.subjectName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LernspaceDark
                                            )
                                            if (c.room != null) {
                                                Text(text = "Ruang: ${c.room}", fontSize = 10.sp, color = LernspaceTextSecondary)
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
// VIEW: Daftar Semua Tugas (Interactive Task & Assignment Manager)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskListView(
    tasks: List<TaskUiModel>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    notes: List<NoteUiModel>,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (TaskUiModel) -> Unit,
    onAddNewTask: () -> Unit,
    onOpenNoteForTopic: (topicId: String, topicTitle: String, subjectId: String?, subjectName: String?) -> Unit,
    onViewSyllabus: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "COMPLETED"
    var selectedPriorityFilter by remember { mutableStateOf("ALL") } // "ALL", "HIGH", "MEDIUM", "LOW"
    var selectedSubjectId by remember { mutableStateOf("ALL") } // "ALL" or subject.id
    var sortBy by remember { mutableStateOf("DUE_DATE") } // "DUE_DATE", "PRIORITY", "TITLE"

    val totalCount = tasks.size
    val pendingCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }
    val highPriorityPending = tasks.count { it.priority.uppercase() == "HIGH" && !it.isCompleted }

    // Filtered and sorted tasks
    val filteredTasks = tasks.filter { task ->
        val matchQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                (task.subjectName?.contains(searchQuery, ignoreCase = true) == true) ||
                (task.topicName?.contains(searchQuery, ignoreCase = true) == true) ||
                task.notes.contains(searchQuery, ignoreCase = true)

        val matchStatus = when (selectedStatusFilter) {
            "PENDING" -> !task.isCompleted
            "COMPLETED" -> task.isCompleted
            else -> true
        }

        val matchPriority = when (selectedPriorityFilter) {
            "ALL" -> true
            else -> task.priority.equals(selectedPriorityFilter, ignoreCase = true)
        }

        val matchSubject = when (selectedSubjectId) {
            "ALL" -> true
            else -> task.subjectId == selectedSubjectId
        }

        matchQuery && matchStatus && matchPriority && matchSubject
    }.sortedWith { a, b ->
        when (sortBy) {
            "PRIORITY" -> {
                val pA = if (a.priority.uppercase() == "HIGH") 0 else if (a.priority.uppercase() == "MEDIUM") 1 else 2
                val pB = if (b.priority.uppercase() == "HIGH") 0 else if (b.priority.uppercase() == "MEDIUM") 1 else 2
                if (pA != pB) pA.compareTo(pB) else a.dueDayNumber.compareTo(b.dueDayNumber)
            }
            "TITLE" -> a.title.compareTo(b.title, ignoreCase = true)
            else -> {
                // DUE_DATE: pending first, then by dueDayNumber
                if (a.isCompleted != b.isCompleted) {
                    a.isCompleted.compareTo(b.isCompleted)
                } else {
                    a.dueDayNumber.compareTo(b.dueDayNumber)
                }
            }
        }
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top Bar: Search + Stats Badges + Input Button ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari tugas, mata kuliah, catatan...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = LernspaceTextSecondary, modifier = Modifier.size(17.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(13.dp))
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
                            .height(44.dp)
                    )

                    // Quick Stat summary pills
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total: $totalCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Text("•", color = LernspaceBorder)
                            Text("Pending: $pendingCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pendingCount > 0) AccentBlue else LernspaceTextSecondary)
                            Text("•", color = LernspaceBorder)
                            Text("Selesai: $completedCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            if (highPriorityPending > 0) {
                                Text("•", color = LernspaceBorder)
                                Text("Urgent: $highPriorityPending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort switcher
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            listOf(
                                Pair("DUE_DATE", "Urut Deadline"),
                                Pair("PRIORITY", "Urut Prioritas")
                            ).forEach { (key, label) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (sortBy == key) Color.White else Color.Transparent,
                                    shadowElevation = if (sortBy == key) 1.dp else 0.dp,
                                    modifier = Modifier.clickable { sortBy = key }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (sortBy == key) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortBy == key) LernspaceDark else LernspaceTextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Button Input Tugas Baru
                    Button(
                        onClick = onAddNewTask,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tugas Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Filter Chips Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Filter
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Pair("ALL", "Semua ($totalCount)"),
                        Pair("PENDING", "Belum Selesai ($pendingCount)"),
                        Pair("COMPLETED", "Selesai ($completedCount)")
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedStatusFilter == key,
                            onClick = { selectedStatusFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedStatusFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Box(modifier = Modifier.height(20.dp).width(1.dp).background(LernspaceBorder))

                // Priority Filter
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Pair("ALL", "Semua Prio"),
                        Pair("HIGH", "High"),
                        Pair("MEDIUM", "Med"),
                        Pair("LOW", "Low")
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPriorityFilter == key,
                            onClick = { selectedPriorityFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedPriorityFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Box(modifier = Modifier.height(20.dp).width(1.dp).background(LernspaceBorder))

                // Subject filter row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubjectId == "ALL",
                            onClick = { selectedSubjectId = "ALL" },
                            label = { Text("Semua Mapel", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(subjects, key = { it.id }) { subject ->
                        val count = tasks.count { it.subjectId == subject.id }
                        FilterChip(
                            selected = selectedSubjectId == subject.id,
                            onClick = { selectedSubjectId = subject.id },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(subject.color)
                                )
                            },
                            label = { Text("${subject.name} ($count)", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f))

            // ── Tasks Scrollable List ─────────────────────────────────────────
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Checklist,
                            contentDescription = null,
                            tint = LernspaceTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "Tidak ada tugas yang cocok dengan '$searchQuery'"
                            else if (selectedStatusFilter == "COMPLETED")
                                "Belum ada tugas yang diselesaikan"
                            else if (selectedStatusFilter == "PENDING")
                                "Hebat! Semua tugas sudah selesai!"
                            else
                                "Belum ada tugas terdaftar",
                            style = MaterialTheme.typography.titleMedium,
                            color = LernspaceDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tekan tombol '+ Tugas Baru' di atas untuk mencatat tugas dan deadline baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LernspaceTextSecondary
                        )
                        Button(
                            onClick = onAddNewTask,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Input Tugas Sekarang")
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
                    items(filteredTasks, key = { it.id }) { task ->
                        val matchingSubject = subjects.find { it.id == task.subjectId }
                        val subjectColor = matchingSubject?.color ?: AccentBlue

                        TaskListItemCard(
                            task = task,
                            subjectColor = subjectColor,
                            onToggleComplete = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task) },
                            onOpenNote = {
                                if (task.topicId != null) {
                                    onOpenNoteForTopic(task.topicId, task.topicName ?: task.title, task.subjectId, task.subjectName)
                                } else {
                                    onOpenNoteForTopic(UUID.randomUUID().toString(), task.title, task.subjectId, task.subjectName)
                                }
                            },
                            onViewSyllabus = {
                                if (task.subjectId != null) {
                                    onViewSyllabus(task.subjectId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Task List Item Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskListItemCard(
    task: TaskUiModel,
    subjectColor: Color,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onOpenNote: () -> Unit,
    onViewSyllabus: () -> Unit
) {
    val isDone = task.isCompleted

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDone) Color(0xFFF8FAFC) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isDone) 1.dp else 1.5.dp,
            color = if (isDone) LernspaceBorder else subjectColor.copy(alpha = 0.4f)
        ),
        shadowElevation = if (isDone) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkbox button
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (isDone) "Selesai" else "Belum Selesai",
                    tint = if (isDone) Color(0xFF16A34A) else LernspaceTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Main Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top line: Title & Urgency badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) LernspaceTextSecondary else LernspaceDark,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Right Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority Badge
                        val priorityBg = when (task.priority.uppercase()) {
                            "HIGH" -> Color(0xFFFEE2E2)
                            "MEDIUM" -> Color(0xFFF1F5F9)
                            else -> Color(0xFFDBEAFE)
                        }
                        val priorityFg = when (task.priority.uppercase()) {
                            "HIGH" -> Color(0xFFDC2626)
                            "MEDIUM" -> Color(0xFF475569)
                            else -> Color(0xFF2563EB)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDone) LernspaceSurfaceLow else priorityBg
                        ) {
                            Text(
                                text = "Prio: ${task.priority.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) LernspaceTextSecondary else priorityFg,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        // Estimated Minutes Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, null, tint = LernspaceTextSecondary, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "${task.estimatedMinutes} Menit",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }

                        // Due Date & Time Pill
                        val isNearDue = task.dueDayNumber <= 9 && !isDone
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDone) LernspaceSurfaceLow else if (isNearDue) Color(0xFFFEF2F2) else Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDone) LernspaceBorder else if (isNearDue) Color(0xFFFCA5A5) else Color(0xFFBFDBFE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNearDue) Icons.Filled.AddAlert else Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isDone) LernspaceTextSecondary else if (isNearDue) Color(0xFFDC2626) else AccentBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${task.dueDate} • ${task.dueTime}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDone) LernspaceTextSecondary else if (isNearDue) Color(0xFFDC2626) else AccentBlue
                                )
                            }
                        }
                    }
                }

                // Middle line: Subject & Topic integration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (task.subjectName != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = subjectColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(subjectColor))
                                Text(
                                    text = task.subjectName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subjectColor
                                )
                            }
                        }
                    }

                    if (task.topicName != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.clickable { onViewSyllabus() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📌 Sub-bab: ${task.topicName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = "• Lihat Silabus",
                                    fontSize = 10.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action: Write/Open Note on Canvas for this Task
                    OutlinedButton(
                        onClick = onOpenNote,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Filled.Draw, null, modifier = Modifier.size(13.dp), tint = AccentBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Catatan Canvas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                    }

                    // Delete Task button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Hapus Tugas",
                            tint = LernspaceTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Notes / Instruction snippet if present
                if (task.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, LernspaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Catatan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary)
                            Text(
                                text = task.notes,
                                fontSize = 11.sp,
                                color = LernspaceDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW 2: Jadwal Mapel Mingguan Timetable Grid
// Lebar blok = kolom hari, Tinggi blok = durasi pelajaran (proporsional waktu)
// ─────────────────────────────────────────────────────────────────────────────

private fun parseTimeToMinutes(t: String): Int {
    val parts = t.split(":")
    val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    return h * 60 + m
}

private fun formatMinutesToTime(mins: Int): String {
    val h = (mins / 60) % 24
    val m = mins % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

@Composable
private fun WeeklyTimetableBuilderView(
    weeklyClasses: List<WeeklyClassUi>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onAddClass: () -> Unit,
    onOpenImportKrs: () -> Unit = {},
    onDeleteClass: (WeeklyClassUi) -> Unit
) {
    val weekDays = listOf(
        Pair(1, "Senin"),
        Pair(2, "Selasa"),
        Pair(3, "Rabu"),
        Pair(4, "Kamis"),
        Pair(5, "Jumat"),
        Pair(6, "Sabtu"),
        Pair(7, "Minggu")
    )

    // Dynamic hour range: at least 07:00 to 20:00, or expands if classes fall outside
    val minClassHour = weeklyClasses.mapNotNull { it.startTime.split(":").firstOrNull()?.trim()?.toIntOrNull() }.minOrNull() ?: 7
    val maxClassHour = weeklyClasses.mapNotNull { it.endTime.split(":").firstOrNull()?.trim()?.toIntOrNull() }.maxOrNull() ?: 18
    val startHour = minOf(7, minClassHour)
    val endHour = maxOf(20, maxClassHour + 1)
    val totalHours = endHour - startHour
    val hourSlots = (startHour until endHour).toList()

    val hourHeight = 60.dp // 1 hour = 60dp => 1 minute = 1dp!
    val totalGridHeight = (totalHours * 60).dp

    val timeColWidth = 64.dp
    val dayColWidth = 148.dp

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val todayDayOfWeek = remember { LocalDate.now().dayOfWeek.value } // 1=Mon .. 7=Sun
    val now = LocalTime.now()
    val currentMinsNow = now.hour * 60 + now.minute

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = LernspaceSurfaceCard,
        border = BorderStroke(1.dp, LernspaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Jadwal Mata Pelajaran / Kuliah Mingguan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder)
                        ) {
                            Text(
                                text = "${weeklyClasses.size} Kelas Terdaftar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Visualisasi jadwal akurat: lebar blok sesuai hari dan tinggi blok proporsional durasi kelas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LernspaceTextSecondary
                    )
                }

                Button(
                    onClick = onAddClass,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Jadwal Mapel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Continuous Schedule Grid Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = LernspaceSurfaceCard,
                border = BorderStroke(1.dp, LernspaceBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── 1. Top Pinned Row: [ JAM ] + Day Headers (scrolls horizontally in sync) ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LernspaceSurfaceLow)
                            .border(BorderStroke(0.5.dp, LernspaceBorder))
                    ) {
                        // Top-left corner box (Pinned above Time Column)
                        Box(
                            modifier = Modifier
                                .width(timeColWidth)
                                .height(44.dp)
                                .background(LernspaceSurfaceLow)
                                .border(BorderStroke(0.5.dp, LernspaceBorder)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, null, tint = LernspaceTextSecondary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "JAM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                            }
                        }

                        // Day Headers Row (horizontal scroll synchronized)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            weekDays.forEach { (dayIndex, dayName) ->
                                val dayClassesCount = weeklyClasses.count { it.dayOfWeek == dayIndex }
                                val isToday = dayIndex == todayDayOfWeek

                                Box(
                                    modifier = Modifier
                                        .width(dayColWidth)
                                        .height(44.dp)
                                        .background(if (isToday) LernspaceDark else LernspaceSurfaceLow)
                                        .border(BorderStroke(0.5.dp, LernspaceBorder)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = dayName.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isToday) Color.White else LernspaceDark
                                        )
                                        if (isToday) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.White.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "HARI INI",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (dayClassesCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = LernspaceDark.copy(alpha = 0.08f)
                                            ) {
                                                Text(
                                                    text = "$dayClassesCount",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LernspaceDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 2. Grid Body: Time Column + Days Matrix (scrolls vertically in sync) ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(verticalScrollState)
                    ) {
                        // Leftmost Column: Hourly Time Labels (Pinned horizontally, scrolls vertically)
                        Box(
                            modifier = Modifier
                                .width(timeColWidth)
                                .height(totalGridHeight)
                                .background(LernspaceSurfaceLow)
                                .border(BorderStroke(0.5.dp, LernspaceBorder))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                hourSlots.forEach { hour ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(hourHeight)
                                            .border(BorderStroke(0.5.dp, LernspaceBorder.copy(alpha = 0.35f)))
                                            .padding(top = 4.dp, end = 6.dp),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%02d:00", hour),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LernspaceTextSecondary
                                        )
                                    }
                                }
                            }

                            // Current time badge floating on the Time Column ruler
                            if (now.hour in startHour until endHour) {
                                val currentTopMins = currentMinsNow - (startHour * 60)
                                if (currentTopMins in 0..(totalHours * 60)) {
                                    Surface(
                                        modifier = Modifier
                                            .offset(y = (currentTopMins - 10).dp, x = 6.dp)
                                            .zIndex(30f),
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentRed,
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d", now.hour, now.minute),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Days Columns Container (scrolls horizontally with horizontalScrollState)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            weekDays.forEach { (dayIndex, _) ->
                                val isToday = dayIndex == todayDayOfWeek
                                val classesThisDay = weeklyClasses.filter { it.dayOfWeek == dayIndex }.sortedBy { it.startTime }

                                Box(
                                    modifier = Modifier
                                        .width(dayColWidth)
                                        .height(totalGridHeight)
                                        .background(if (isToday) LernspaceSurfaceLow.copy(alpha = 0.3f) else Color.Transparent)
                                        .border(BorderStroke(0.5.dp, LernspaceBorder.copy(alpha = 0.35f)))
                                ) {
                                    // 1. Horizontal hour slot guidelines
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        hourSlots.forEach { _ ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(hourHeight)
                                                    .border(BorderStroke(0.5.dp, LernspaceBorder.copy(alpha = 0.25f)))
                                            )
                                        }
                                    }

                                    // 2. Class Duration Blocks
                                    classesThisDay.forEach { c ->
                                        val sMins = parseTimeToMinutes(c.startTime)
                                        val eMins = parseTimeToMinutes(c.endTime)
                                        val duration = (eMins - sMins).coerceAtLeast(30)
                                        val topMins = (sMins - (startHour * 60)).coerceAtLeast(0)

                                        val topOffsetDp = topMins.dp
                                        val blockHeightDp = duration.dp

                                        // Overlap detection for side-by-side positioning
                                        val overlaps = classesThisDay.filter { other ->
                                            val os = parseTimeToMinutes(other.startTime)
                                            val oe = parseTimeToMinutes(other.endTime)
                                            maxOf(sMins, os) < minOf(eMins, oe)
                                        }
                                        val colIndex = overlaps.indexOf(c).coerceAtLeast(0)
                                        val colCount = overlaps.size.coerceAtLeast(1)

                                        val subj = subjects.find { it.id == c.subjectId }
                                        val subColor = subj?.color ?: Color(0xFF2563EB)

                                        WeeklyClassDurationCard(
                                            wClass = c,
                                            subjectColor = subColor,
                                            topOffsetDp = topOffsetDp,
                                            blockHeightDp = blockHeightDp,
                                            colIndex = colIndex,
                                            colCount = colCount,
                                            dayWidth = dayColWidth,
                                            onDelete = { onDeleteClass(c) }
                                        )
                                    }

                                    // 3. Current time red indicator line (Drawn ON TOP of all class blocks!)
                                    if (isToday && now.hour in startHour until endHour) {
                                        val currentTopMins = currentMinsNow - (startHour * 60)
                                        if (currentTopMins in 0..(totalHours * 60)) {
                                            val currentLineTop = (currentTopMins - 1).dp
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .offset(y = currentLineTop)
                                                    .zIndex(30f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(AccentRed)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(2.5.dp)
                                                        .background(AccentRed)
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Weekly Class Duration Block (Tinggi = Durasi, Lebar = Kolom Hari)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeeklyClassDurationCard(
    wClass: WeeklyClassUi,
    subjectColor: Color,
    topOffsetDp: androidx.compose.ui.unit.Dp,
    blockHeightDp: androidx.compose.ui.unit.Dp,
    colIndex: Int,
    colCount: Int,
    dayWidth: androidx.compose.ui.unit.Dp,
    onDelete: () -> Unit
) {
    val usableDayWidth = dayWidth - 4.dp
    val blockWidth = if (colCount > 1) usableDayWidth / colCount else usableDayWidth
    val xOffset = if (colCount > 1) (blockWidth * colIndex) + 2.dp else 2.dp

    Surface(
        modifier = Modifier
            .offset(x = xOffset, y = topOffsetDp)
            .width(blockWidth)
            .height(blockHeightDp)
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = subjectColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ),
        color = LernspaceSurfaceCard,
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left color ribbon
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(subjectColor.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Time Range & Delete Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = subjectColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${wClass.startTime} - ${wClass.endTime}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Hapus",
                            tint = LernspaceTextSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // Middle: Subject Name & Code
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = wClass.subjectName,
                        fontSize = if (blockHeightDp >= 60.dp) 11.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark,
                        maxLines = if (blockHeightDp >= 80.dp) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                    Text(
                        text = wClass.subjectCode,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subjectColor,
                        maxLines = 1
                    )
                }

                // Bottom: Room / Lecturer / Type (only if height allows >= 65dp)
                if (blockHeightDp >= 65.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!wClass.room.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Filled.LocationOn, null, tint = LernspaceTextSecondary, modifier = Modifier.size(9.dp))
                                Text(
                                    text = wClass.room,
                                    fontSize = 8.sp,
                                    color = LernspaceTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (blockHeightDp >= 90.dp && !wClass.type.isNullOrBlank()) {
                            Text(
                                text = wClass.type,
                                fontSize = 8.sp,
                                color = LernspaceTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW 3: Day Agenda & Auto-Schedule (Timeline Harian & Mesin Jadwal Otomatis)
// ─────────────────────────────────────────────────────────────────────────────

private sealed class AgendaEntry {
    abstract val sortTime: String
    abstract val entryId: String

    data class ClassEntry(val wClass: WeeklyClassUi) : AgendaEntry() {
        override val sortTime: String get() = wClass.startTime
        override val entryId: String get() = "class_${wClass.id}"
    }

    data class PlannerEntry(val item: PlannerItemUi) : AgendaEntry() {
        override val sortTime: String get() = item.startTime
        override val entryId: String get() = "planner_${item.id}"
    }

    data class TaskEntry(val task: TaskUiModel) : AgendaEntry() {
        override val sortTime: String get() = task.dueTime.ifBlank { "23:59" }
        override val entryId: String get() = "task_${task.id}"
    }
}

private data class RecommendedSlot(
    val startTime: String,
    val endTime: String,
    val reason: String
)


private fun calculateRecommendedSlot(
    dayClasses: List<WeeklyClassUi>,
    dayPlannerItems: List<PlannerItemUi>,
    durationMinutes: Int
): RecommendedSlot {
    val studyWindowStart = 8 * 60
    val studyWindowEnd = 22 * 60
    val buffer = 15

    val occupied = mutableListOf<Pair<Int, Int>>()
    dayClasses.forEach { c ->
        val s = parseTimeToMinutes(c.startTime)
        val e = parseTimeToMinutes(c.endTime)
        if (e > s) occupied.add(Pair(s, e))
    }
    dayPlannerItems.forEach { p ->
        val s = parseTimeToMinutes(p.startTime)
        val e = parseTimeToMinutes(p.endTime)
        if (e > s) occupied.add(Pair(s, e))
    }
    occupied.sortBy { it.first }

    if (occupied.isEmpty()) {
        val startMins = 9 * 60
        val endMins = startMins + durationMinutes
        return RecommendedSlot(
            startTime = formatMinutesToTime(startMins),
            endTime = formatMinutesToTime(endMins),
            reason = "Hari bebas kelas kuliah. Slot fokus optimal di pagi hari dengan buffer istirahat teratur."
        )
    }

    // Try before first block
    val first = occupied.first()
    if (first.first - studyWindowStart >= durationMinutes + buffer) {
        val startMins = studyWindowStart
        val endMins = startMins + durationMinutes
        return RecommendedSlot(
            startTime = formatMinutesToTime(startMins),
            endTime = formatMinutesToTime(endMins),
            reason = "Slot fokus pagi sebelum jadwal kelas pertama dimulai (buffer ${first.first - endMins}m)."
        )
    }

    // Between blocks
    for (i in 0 until occupied.size - 1) {
        val currentEnd = occupied[i].second
        val nextStart = occupied[i + 1].first
        val gap = nextStart - currentEnd
        if (gap >= durationMinutes + (2 * buffer)) {
            val startMins = currentEnd + buffer
            val endMins = startMins + durationMinutes
            val prevClass = dayClasses.find { parseTimeToMinutes(it.endTime) == currentEnd }
            val classNote = if (prevClass != null) "setelah kelas ${prevClass.subjectName}" else "di antara jadwal kuliah"
            return RecommendedSlot(
                startTime = formatMinutesToTime(startMins),
                endTime = formatMinutesToTime(endMins),
                reason = "Terdeteksi jeda luang ${gap}m $classNote. Waktu bebas bentrok ideal untuk belajar mandiri."
            )
        }
    }

    // After last block
    val last = occupied.last()
    if (studyWindowEnd - last.second >= durationMinutes + buffer) {
        val startMins = last.second + buffer
        val endMins = startMins + durationMinutes
        val prevClass = dayClasses.find { parseTimeToMinutes(it.endTime) == last.second }
        val classNote = if (prevClass != null) "setelah kelas ${prevClass.subjectName} usai" else "setelah seluruh kegiatan hari ini"
        return RecommendedSlot(
            startTime = formatMinutesToTime(startMins),
            endTime = formatMinutesToTime(endMins),
            reason = "Slot review sore/malam $classNote. Cocok untuk penguatan materi yang baru dipelajari."
        )
    }

    // Fallback slot
    return RecommendedSlot(
        startTime = "19:30",
        endTime = formatMinutesToTime(19 * 60 + 30 + durationMinutes),
        reason = "Slot sesi malam bebas bentrok setelah seluruh jadwal perkuliahan hari ini."
    )
}

@Composable
private fun DayAgendaView(
    scheduleItems: List<PlannerItemUi>,
    weeklyClasses: List<WeeklyClassUi>,
    tasks: List<TaskUiModel>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    selectedDayOffset: Int,
    onSelectDayOffset: (Int) -> Unit,
    onAcceptSuggestion: (String) -> Unit,
    onTogglePlannerComplete: (PlannerItemUi) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onOpenNoteForSubject: (subjectId: String?, subjectName: String?) -> Unit,
    onOpenNoteForTopic: (topicId: String, topicTitle: String, subjectId: String?, subjectName: String?) -> Unit,
    onViewInSyllabus: (String?) -> Unit,
    onAutoSchedule: () -> Unit,
    onAddManualAgenda: () -> Unit,
    onDeleteItem: (PlannerItemUi) -> Unit
) {
    val today = remember { LocalDate.now() }
    val dayOffsets = remember { (0..6).toList() }

    val selectedDate = remember(selectedDayOffset) { today.plusDays(selectedDayOffset.toLong()) }
    val selectedDayOfWeek = selectedDate.dayOfWeek.value // 1 = Monday, 7 = Sunday
    val selectedDayOfMonth = selectedDate.dayOfMonth

    // 1. Weekly classes for this weekday
    val dayClasses = weeklyClasses.filter { it.dayOfWeek == selectedDayOfWeek }.sortedBy { it.startTime }

    // 2. Specific planner items / auto-study sessions scheduled for this day
    val dayPlannerItems = scheduleItems.filter {
        it.dayOffset == selectedDayOffset || (it.dayOfWeek == selectedDayOfWeek && it.dayOffset == -1)
    }.sortedBy { it.startTime }

    // 3. Tasks due on this day
    val dayTasks = tasks.filter { task ->
        task.dueDayNumber == selectedDayOfMonth ||
        (task.dueDate.isNotBlank() && (task.dueDate.contains("$selectedDayOfMonth ") || task.dueDate.contains(" $selectedDayOfMonth,")))
    }.sortedBy { it.dueTime }

    // Combined unified chronological timeline
    val allDayEntries = (dayClasses.map { AgendaEntry.ClassEntry(it) } +
        dayPlannerItems.map { AgendaEntry.PlannerEntry(it) } +
        dayTasks.map { AgendaEntry.TaskEntry(it) }).sortedBy { it.sortTime }

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CLASSES", "STUDY", "TASKS"

    val filteredEntries = when (selectedFilter) {
        "CLASSES" -> allDayEntries.filterIsInstance<AgendaEntry.ClassEntry>()
        "STUDY" -> allDayEntries.filterIsInstance<AgendaEntry.PlannerEntry>()
        "TASKS" -> allDayEntries.filterIsInstance<AgendaEntry.TaskEntry>()
        else -> allDayEntries
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = LernspaceSurfaceCard,
        border = BorderStroke(1.dp, LernspaceBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top Bar: Day Selector & Quick Actions ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Selector Chips (Hari Ini, Besok, ...)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(dayOffsets) { offset ->
                        val targetDate = today.plusDays(offset.toLong())
                        val fullDayName = when (targetDate.dayOfWeek) {
                            DayOfWeek.MONDAY -> "Senin"
                            DayOfWeek.TUESDAY -> "Selasa"
                            DayOfWeek.WEDNESDAY -> "Rabu"
                            DayOfWeek.THURSDAY -> "Kamis"
                            DayOfWeek.FRIDAY -> "Jumat"
                            DayOfWeek.SATURDAY -> "Sabtu"
                            DayOfWeek.SUNDAY -> "Minggu"
                        }
                        val monthName = when (targetDate.monthValue) {
                            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "Mei"; 6 -> "Jun"
                            7 -> "Jul"; 8 -> "Agu"; 9 -> "Sep"; 10 -> "Okt"; 11 -> "Nov"; 12 -> "Des"
                            else -> ""
                        }
                        val chipText = when (offset) {
                            0 -> "Hari Ini ($fullDayName, ${targetDate.dayOfMonth} $monthName)"
                            1 -> "Besok ($fullDayName, ${targetDate.dayOfMonth} $monthName)"
                            else -> "$fullDayName, ${targetDate.dayOfMonth} $monthName"
                        }
                        val isSelected = selectedDayOffset == offset

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                            modifier = Modifier.clickable { onSelectDayOffset(offset) }
                        ) {
                            Text(
                                text = chipText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else LernspaceDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAutoSchedule,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Auto-Study", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAddManualAgenda,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LernspaceBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, tint = LernspaceDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Tambah Agenda", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                    }
                }
            }

            // ── Sub-bar: Category Filter Chips & Counter ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filterOptions = listOf(
                        Triple("ALL", "Semua", allDayEntries.size),
                        Triple("CLASSES", "Kuliah", dayClasses.size),
                        Triple("STUDY", "Auto-Study & Belajar", dayPlannerItems.size),
                        Triple("TASKS", "Tugas", dayTasks.size)
                    )
                    filterOptions.forEach { (key, label, count) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) LernspaceSurfaceLow else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) LernspaceBorder else Color.Transparent),
                            modifier = Modifier.clickable { selectedFilter = key }
                        ) {
                            Text(
                                text = "$label ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LernspaceDark else LernspaceTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${allDayEntries.size} Agenda Terjadwal",
                    fontSize = 11.sp,
                    color = LernspaceTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Main Timeline Viewport ────────────────────────────────────────
            if (filteredEntries.isEmpty()) {
                // Minimalist Clean Empty State
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = LernspaceTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Agenda untuk Hari Ini",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jadwal kuliah mingguan, tenggat waktu tugas, dan sesi belajar terjadwal akan muncul otomatis di sini.",
                            fontSize = 12.sp,
                            color = LernspaceTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 420.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAutoSchedule,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Auto-Schedule Jam Belajar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onAddManualAgenda,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LernspaceBorder),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Add, null, tint = LernspaceDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Agenda Manual", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEntries, key = { it.entryId }) { entry ->
                        when (entry) {
                            is AgendaEntry.ClassEntry -> {
                                val matchingSubject = subjects.find { it.id == entry.wClass.subjectId }
                                val subColor = matchingSubject?.color ?: AccentBlue
                                AgendaClassCard(
                                    wClass = entry.wClass,
                                    subjectColor = subColor,
                                    onOpenNote = { onOpenNoteForSubject(entry.wClass.subjectId, entry.wClass.subjectName) },
                                    onViewSyllabus = { onViewInSyllabus(entry.wClass.subjectId) }
                                )
                            }
                            is AgendaEntry.PlannerEntry -> {
                                val matchingSubject = subjects.find { it.id == entry.item.subjectId }
                                val subColor = matchingSubject?.color ?: AccentBlue
                                AgendaPlannerCard(
                                    item = entry.item,
                                    subjectColor = subColor,
                                    onAcceptSuggestion = { onAcceptSuggestion(entry.item.id) },
                                    onToggleComplete = { onTogglePlannerComplete(entry.item) },
                                    onOpenNote = {
                                        if (entry.item.topicId != null) {
                                            onOpenNoteForTopic(entry.item.topicId, entry.item.topicName ?: entry.item.title, entry.item.subjectId, entry.item.subjectName)
                                        } else {
                                            onOpenNoteForSubject(entry.item.subjectId, entry.item.subjectName)
                                        }
                                    },
                                    onViewSyllabus = { onViewInSyllabus(entry.item.subjectId) },
                                    onDelete = { onDeleteItem(entry.item) }
                                )
                            }
                            is AgendaEntry.TaskEntry -> {
                                val matchingSubject = subjects.find { it.id == entry.task.subjectId }
                                val subColor = matchingSubject?.color ?: AccentBlue
                                AgendaTaskCard(
                                    task = entry.task,
                                    subjectColor = subColor,
                                    onToggleComplete = { onToggleTaskComplete(entry.task.id) },
                                    onOpenNote = {
                                        if (entry.task.topicId != null) {
                                            onOpenNoteForTopic(entry.task.topicId, entry.task.topicName ?: entry.task.title, entry.task.subjectId, entry.task.subjectName)
                                        } else {
                                            onOpenNoteForSubject(entry.task.subjectId, entry.task.subjectName)
                                        }
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

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Agenda Class Card (Jadwal Kuliah Hari Ini)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgendaClassCard(
    wClass: WeeklyClassUi,
    subjectColor: Color,
    onOpenNote: () -> Unit,
    onViewSyllabus: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = LernspaceBorder, shape = RoundedCornerShape(12.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Surface(shape = RoundedCornerShape(6.dp), color = LernspaceDark) {
                            Text(
                                text = "${wClass.startTime} - ${wClass.endTime}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder)
                        ) {
                            Text(
                                text = wClass.type,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = if (wClass.subjectCode.isNotBlank()) "•  ${wClass.subjectCode} - ${wClass.subjectName}" else "•  ${wClass.subjectName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LernspaceDark
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = onViewSyllabus,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(13.dp), tint = LernspaceDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Silabus", fontSize = 11.sp, color = LernspaceDark)
                        }

                        Button(
                            onClick = onOpenNote,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Filled.Create, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Catatan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (!wClass.room.isNullOrBlank() || !wClass.lecturer.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!wClass.room.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.LocationOn, null, tint = LernspaceTextSecondary, modifier = Modifier.size(13.dp))
                                Text(wClass.room, fontSize = 11.sp, color = LernspaceTextSecondary)
                            }
                        }
                        if (!wClass.lecturer.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Person, null, tint = LernspaceTextSecondary, modifier = Modifier.size(13.dp))
                                Text(wClass.lecturer, fontSize = 11.sp, color = LernspaceTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Agenda Planner Card (Auto-Study & Sesi Belajar Mandiri)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgendaPlannerCard(
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
                width = 1.dp,
                color = if (isHybrid) LernspaceDark else LernspaceBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (item.isCompleted) LernspaceSurfaceLow else LernspaceSurfaceCard,
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isHybrid) LernspaceDark else subjectColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Surface(shape = RoundedCornerShape(6.dp), color = LernspaceDark) {
                            Text(
                                text = "${item.startTime} - ${item.endTime}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isHybrid) {
                                    Icon(Icons.Filled.AutoAwesome, null, tint = LernspaceDark, modifier = Modifier.size(11.dp))
                                    Text("Auto-Study AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                } else {
                                    Text(item.type.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                                }
                            }
                        }

                        if (!item.subjectName.isNullOrBlank()) {
                            Text(
                                text = "•  ${item.subjectName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isHybrid) {
                            IconButton(onClick = onToggleComplete, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "Complete",
                                    tint = if (item.isCompleted) AccentTeal else LernspaceTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, null, tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) LernspaceTextSecondary else LernspaceDark,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                // Minimalist AI Reason container
                if (isHybrid && !item.aiReason.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder)
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Lightbulb, null, tint = LernspaceDark, modifier = Modifier.size(15.dp))
                                Text(
                                    text = item.aiReason,
                                    fontSize = 11.sp,
                                    color = LernspaceDark,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = onAcceptSuggestion,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Accept & Jadwalkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.topicName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Bookmark, null, tint = LernspaceTextSecondary, modifier = Modifier.size(13.dp))
                            Text(text = "Topik: ${item.topicName}", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!item.subjectId.isNullOrBlank()) {
                            TextButton(
                                onClick = onViewSyllabus,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(13.dp), tint = LernspaceDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Silabus", fontSize = 11.sp, color = LernspaceDark)
                            }
                        }

                        Button(
                            onClick = onOpenNote,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Filled.Create, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Catatan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT: Agenda Task Card (Tenggat Tugas Hari Ini)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgendaTaskCard(
    task: TaskUiModel,
    subjectColor: Color,
    onToggleComplete: () -> Unit,
    onOpenNote: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = LernspaceBorder, shape = RoundedCornerShape(12.dp)),
        color = if (task.isCompleted) LernspaceSurfaceLow else LernspaceSurfaceCard,
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (task.priority == "HIGH") AccentRed else subjectColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
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
                        IconButton(onClick = onToggleComplete, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Tandai Selesai",
                                tint = if (task.isCompleted) AccentTeal else LernspaceTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(shape = RoundedCornerShape(6.dp), color = LernspaceDark) {
                            Text(
                                text = "Due ${task.dueTime}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder)
                        ) {
                            Text(
                                text = "Tenggat Tugas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (!task.subjectName.isNullOrBlank()) {
                            Text(
                                text = "•  ${task.subjectName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder)
                    ) {
                        Text(
                            text = when (task.priority) {
                                "HIGH" -> "Prioritas Tinggi"
                                "LOW" -> "Prioritas Rendah"
                                else -> "Prioritas Sedang"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.priority == "HIGH") AccentRed else LernspaceTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) LernspaceTextSecondary else LernspaceDark,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (!task.notes.isNullOrBlank()) {
                    Text(
                        text = task.notes,
                        fontSize = 11.sp,
                        color = LernspaceTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onOpenNote,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Create, null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Catatan Tugas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Input Tugas & Set Deadline
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InputTaskDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    initialDayNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: com.lernspace.app.ui.learning.SubjectUi?,
        topic: com.lernspace.app.ui.learning.TopicUi?,
        dueDate: String,
        dueDayNumber: Int,
        dueTime: String,
        priority: String,
        estMinutes: Int,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    val subjectTopics = remember(selectedSubject) {
        if (selectedSubject != null) topics.filter { it.subjectId == selectedSubject!!.id } else emptyList()
    }
    var selectedTopic by remember { mutableStateOf<com.lernspace.app.ui.learning.TopicUi?>(null) }

    // Date states (Default: initialDayNumber in November 2026)
    var selectedDayNumber by remember { mutableIntStateOf(initialDayNumber.coerceIn(1, 30)) }
    var selectedMonth by remember { mutableIntStateOf(10) } // 0-based: 10 = Nov
    var selectedYear by remember { mutableIntStateOf(2026) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var dueTime by remember { mutableStateOf("23:59") }
    var priority by remember { mutableStateOf("HIGH") } // HIGH, MEDIUM, LOW
    var estMinutes by remember { mutableIntStateOf(90) }
    var notesStr by remember { mutableStateOf("") }

    val monthNamesId = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val monthNamesEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val dayOfWeekName = remember(selectedYear, selectedMonth, selectedDayNumber) {
        val cal = java.util.Calendar.getInstance().apply {
            clear()
            set(selectedYear, selectedMonth, selectedDayNumber)
        }
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "Minggu"
            java.util.Calendar.MONDAY -> "Senin"
            java.util.Calendar.TUESDAY -> "Selasa"
            java.util.Calendar.WEDNESDAY -> "Rabu"
            java.util.Calendar.THURSDAY -> "Kamis"
            java.util.Calendar.FRIDAY -> "Jumat"
            java.util.Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
    }
    val currentMonthId = monthNamesId.getOrElse(selectedMonth) { "Nov" }
    val currentMonthEn = monthNamesEn.getOrElse(selectedMonth) { "Nov" }

    val displayDate = "$dayOfWeekName, $selectedDayNumber $currentMonthId $selectedYear"
    val modelDueDate = "$currentMonthEn $selectedDayNumber, $selectedYear"

    // Material 3 Calendar Date Picker Dialog
    if (showDatePickerDialog) {
        key(selectedYear, selectedMonth, selectedDayNumber) {
            val initialUtcMillis = remember {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(selectedYear, selectedMonth, selectedDayNumber.coerceIn(1, 31))
                }
                cal.timeInMillis
            }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialUtcMillis
            )

            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { utcMillis ->
                                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = utcMillis
                                }
                                selectedDayNumber = cal.get(java.util.Calendar.DAY_OF_MONTH)
                                selectedMonth = cal.get(java.util.Calendar.MONTH)
                                selectedYear = cal.get(java.util.Calendar.YEAR)
                            }
                            showDatePickerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                    ) {
                        Text("Pilih Tanggal", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Batal")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = "Pilih Tanggal Deadline",
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Input Tugas & Set Deadline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Tugas (misal: Laporan Praktikum Enzim)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Select Subject
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mata Kuliah / Pelajaran", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
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

                // Select Topic
                if (subjectTopics.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sub-bab / Topik Silabus (Opsional)", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjectTopics) { t ->
                                FilterChip(
                                    selected = selectedTopic == t,
                                    onClick = { selectedTopic = if (selectedTopic == t) null else t },
                                    label = { Text(t.title, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // ── Deadline Section: Tanggalan & Jam ───────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Batas Waktu (Deadline)", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary, fontWeight = FontWeight.SemiBold)

                    // Clickable Date Picker Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarMonth,
                                            contentDescription = null,
                                            tint = AccentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text("Tanggal Deadline (Klik untuk ganti)", fontSize = 11.sp, color = LernspaceTextSecondary)
                                    Text(
                                        text = displayDate,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceDark
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentBlue.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.EditCalendar, null, tint = AccentBlue, modifier = Modifier.size(13.dp))
                                    Text("Buka Kalender", fontSize = 11.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Quick Date Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for ((_, label, dNum) in listOf(
                            Triple(8, "Hari Ini (8 Nov)", 8),
                            Triple(9, "Besok (9 Nov)", 9),
                            Triple(10, "Lusa (10 Nov)", 10),
                            Triple(15, "+1 Minggu (15 Nov)", 15)
                        )) {
                            val isSelected = selectedDayNumber == dNum && selectedMonth == 10 && selectedYear == 2026
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDayNumber = dNum
                                    selectedMonth = 10
                                    selectedYear = 2026
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Jam Deadline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dueTime,
                            onValueChange = { dueTime = it },
                            label = { Text("Jam Deadline (HH:mm)") },
                            leadingIcon = { Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Quick Time Chips
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Preset Jam", fontSize = 10.sp, color = LernspaceTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (tPreset in listOf("23:59", "17:00", "12:00", "08:00")) {
                                    FilterChip(
                                        selected = dueTime == tPreset,
                                        onClick = { dueTime = tPreset },
                                        label = { Text(tPreset, fontSize = 10.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Priority chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Prioritas Tugas", style = MaterialTheme.typography.labelMedium, color = LernspaceDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((pKey, pLabel, pColor) in listOf(
                            Triple("HIGH", "Tinggi (Urgent)", Color(0xFFE11D48)),
                            Triple("MEDIUM", "Sedang", Color(0xFF475569)),
                            Triple("LOW", "Rendah", Color(0xFF64748B))
                        )) {
                            FilterChip(
                                selected = priority == pKey,
                                onClick = { priority = pKey },
                                label = { Text(pLabel, fontSize = 11.sp, color = if (priority == pKey) pColor else LernspaceDark) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Notes optional
                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title.trim(),
                                    selectedSubject,
                                    selectedTopic,
                                    modelDueDate,
                                    selectedDayNumber,
                                    dueTime.trim(),
                                    priority,
                                    estMinutes,
                                    notesStr.trim()
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                    ) {
                        Text("Simpan & Pasang di Kalender", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Set Jadwal Mapel Mingguan Baru
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SetWeeklyClassDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onDismiss: () -> Unit,
    onConfirm: (
        subject: com.lernspace.app.ui.learning.SubjectUi,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        lecturer: String,
        type: String
    ) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var subjectNameInput by remember { mutableStateOf(selectedSubject?.name ?: "") }
    var subjectCodeInput by remember { mutableStateOf(selectedSubject?.code ?: "") }

    var dayOfWeek by remember { mutableIntStateOf(1) } // 1=Senin, 2=Selasa...
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:40") }
    var activeTimeTarget by remember { mutableStateOf("START") } // "START" or "END"
    var showManualTimeInput by remember { mutableStateOf(false) }
    var minuteIntervalStep by remember { mutableIntStateOf(5) } // 5 or 1
    var savedFeedback by remember { mutableStateOf<String?>(null) }

    var room by remember { mutableStateOf("") }
    var lecturer by remember { mutableStateOf("") }
    var classType by remember { mutableStateOf("Reguler / Teori") }

    // Helper functions for time calculation
    fun parseH(t: String) = t.split(":").getOrNull(0)?.toIntOrNull() ?: 8
    fun parseM(t: String) = t.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    fun fmtTime(h: Int, m: Int) = String.format(java.util.Locale.US, "%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))

    fun addMinutes(base: String, mins: Int): String {
        val total = (parseH(base) * 60 + parseM(base) + mins).coerceIn(0, 23 * 60 + 59)
        return fmtTime(total / 60, total % 60)
    }

    val weekDays = listOf(
        Pair(1, "Senin"),
        Pair(2, "Selasa"),
        Pair(3, "Rabu"),
        Pair(4, "Kamis"),
        Pair(5, "Jumat"),
        Pair(6, "Sabtu"),
        Pair(7, "Minggu")
    )

    val commonClassSlots = listOf(
        Pair("08:00", "09:40") to "Pagi 2 SKS (08:00 - 09:40)",
        Pair("08:00", "10:30") to "Pagi 3 SKS (08:00 - 10:30)",
        Pair("10:00", "11:40") to "Siang 2 SKS (10:00 - 11:40)",
        Pair("10:15", "12:00") to "Siang (10:15 - 12:00)",
        Pair("13:00", "14:40") to "Siang 2 SKS (13:00 - 14:40)",
        Pair("13:30", "15:10") to "Siang (13:30 - 15:10)",
        Pair("15:30", "17:10") to "Sore (15:30 - 17:10)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(550.dp)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                // ── Fixed Top Header ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Set Jadwal Mapel / Kuliah",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                // Feedback Banner setelah "Simpan & Lanjut Tambah"
                if (savedFeedback != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text(savedFeedback ?: "", fontSize = 12.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Scrollable Form Body ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── 1. Input Nama & Kode Mata Pelajaran / Kuliah ─────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "Mata Pelajaran / Kuliah *",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Quick select chips from existing subjects (clean, no parenthesis)
                        if (subjects.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(subjects) { s ->
                                    val isSelected = selectedSubject == s || subjectNameInput.equals(s.name, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedSubject = s
                                            subjectNameInput = s.name
                                            subjectCodeInput = s.code
                                        },
                                        label = { Text(s.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Explicit text input fields for Subject Name & Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = subjectNameInput,
                                onValueChange = {
                                    subjectNameInput = it
                                    val match = subjects.find { s -> s.name.equals(it.trim(), ignoreCase = true) }
                                    selectedSubject = match
                                    if (match != null && subjectCodeInput.isBlank()) {
                                        subjectCodeInput = match.code
                                    }
                                },
                                label = { Text("Nama Mapel / Pelajaran *") },
                                placeholder = { Text("misal: Matematika, Biologi, Kimia") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = subjectCodeInput,
                                onValueChange = { subjectCodeInput = it },
                                label = { Text("Kode (Opsional)") },
                                placeholder = { Text("misal: MTK") },
                                singleLine = true,
                                modifier = Modifier.weight(0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // ── 2. Pilihan Hari ──────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Pilihan Hari",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(weekDays) { (dIdx, dName) ->
                            val isSelected = dayOfWeek == dIdx
                            FilterChip(
                                selected = isSelected,
                                onClick = { dayOfWeek = dIdx },
                                label = { Text(dName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // ── 3. Redesigned Universal Wheel Time Picker (Ultra Cepat & Tanpa Keyboard) ────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Waktu Pelajaran / Kuliah",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Step Interval Toggle (5 Menit vs 1 Menit)
                            FilterChip(
                                selected = minuteIntervalStep == 5,
                                onClick = { minuteIntervalStep = 5 },
                                label = { Text("5 Menit", fontSize = 10.sp, fontWeight = if (minuteIntervalStep == 5) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(6.dp)
                            )
                            FilterChip(
                                selected = minuteIntervalStep == 1,
                                onClick = { minuteIntervalStep = 1 },
                                label = { Text("1 Menit", fontSize = 10.sp, fontWeight = if (minuteIntervalStep == 1) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(6.dp)
                            )
                            TextButton(
                                onClick = { showManualTimeInput = !showManualTimeInput },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showManualTimeInput) "Scroll Wheel" else "Ketik",
                                    fontSize = 11.sp,
                                    color = AccentBlue
                                )
                            }
                        }
                    }

                    if (showManualTimeInput) {
                        // Mode Ketik Manual
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Jam Mulai (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("Jam Selesai (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    } else {
                        // Dual Interactive Cards (Jam Mulai & Jam Selesai)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Card Jam Mulai
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTimeTarget = "START" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTimeTarget == "START") Color(0xFFEFF6FF) else LernspaceSurfaceLow,
                                border = BorderStroke(
                                    width = if (activeTimeTarget == "START") 1.5.dp else 1.dp,
                                    color = if (activeTimeTarget == "START") AccentBlue else LernspaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        null,
                                        tint = if (activeTimeTarget == "START") AccentBlue else LernspaceTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text("Jam Mulai", fontSize = 10.sp, color = if (activeTimeTarget == "START") AccentBlue else LernspaceTextSecondary)
                                        Text(
                                            text = startTime,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeTimeTarget == "START") AccentBlue else LernspaceDark
                                        )
                                    }
                                }
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))

                            // Card Jam Selesai
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTimeTarget = "END" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTimeTarget == "END") Color(0xFFEFF6FF) else LernspaceSurfaceLow,
                                border = BorderStroke(
                                    width = if (activeTimeTarget == "END") 1.5.dp else 1.dp,
                                    color = if (activeTimeTarget == "END") AccentBlue else LernspaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        null,
                                        tint = if (activeTimeTarget == "END") AccentBlue else LernspaceTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text("Jam Selesai", fontSize = 10.sp, color = if (activeTimeTarget == "END") AccentBlue else LernspaceTextSecondary)
                                        Text(
                                            text = endTime,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeTimeTarget == "END") AccentBlue else LernspaceDark
                                        )
                                    }
                                }
                            }
                        }

                        // Duration Quick Helpers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Durasi:", fontSize = 10.sp, color = LernspaceTextSecondary, fontWeight = FontWeight.Medium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(listOf(
                                    Pair(40, "+40m"),
                                    Pair(45, "+45m"),
                                    Pair(50, "+50m"),
                                    Pair(80, "+80m"),
                                    Pair(90, "+90m"),
                                    Pair(100, "+100m"),
                                    Pair(120, "+120m")
                                )) { (dur, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.clickable {
                                            endTime = addMinutes(startTime, dur)
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LernspaceDark,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Wheel Time Picker Surface
                        val currentTargetTime = if (activeTimeTarget == "START") startTime else endTime
                        val currentH = parseH(currentTargetTime)
                        val currentM = parseM(currentTargetTime)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Putar Roda: ${if (activeTimeTarget == "START") "Jam Mulai" else "Jam Selesai"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceDark
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(-15, -5, 5, 15).forEach { delta ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, LernspaceBorder),
                                                modifier = Modifier.clickable {
                                                    val adjusted = addMinutes(currentTargetTime, delta)
                                                    if (activeTimeTarget == "START") startTime = adjusted else endTime = adjusted
                                                }
                                            ) {
                                                Text(
                                                    text = if (delta > 0) "+${delta}m" else "${delta}m",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    color = LernspaceDark
                                                )
                                            }
                                        }
                                    }
                                }

                                key(activeTimeTarget, minuteIntervalStep) {
                                    WheelTimePicker(
                                        initialHour = currentH,
                                        initialMinute = currentM,
                                        minuteInterval = minuteIntervalStep,
                                        onTimeChanged = { h, m ->
                                            val newT = fmtTime(h, m)
                                            if (activeTimeTarget == "START") {
                                                startTime = newT
                                            } else {
                                                endTime = newT
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                    // ── 4. Tipe Pertemuan ────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tipe Pertemuan", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Reguler / Teori", "Praktikum / Lab", "Tambahan / Les").forEach { t ->
                                val isSelected = classType == t
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { classType = t },
                                    label = { Text(t, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // ── 5. Ruangan & Pengajar (Opsional) ─────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Ruangan (Opsional)") },
                            placeholder = { Text("misal: Ruang 04, Lab 2") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = lecturer,
                            onValueChange = { lecturer = it },
                            label = { Text("Guru / Dosen (Opsional)") },
                            placeholder = { Text("misal: Bu Ismi, Dr. Budi") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                } // End of Scrollable Form Body

                // Helper to resolve or create subject
                fun resolveSubject(): com.lernspace.app.ui.learning.SubjectUi {
                    return selectedSubject ?: run {
                        val existing = subjects.find { it.name.equals(subjectNameInput.trim(), ignoreCase = true) }
                        if (existing != null) {
                            existing
                        } else {
                            val code = subjectCodeInput.trim()
                            val newSub = com.lernspace.app.ui.learning.SubjectUi(
                                id = java.util.UUID.randomUUID().toString(),
                                name = subjectNameInput.trim(),
                                code = code,
                                color = AccentBlue
                            )
                            AcademicWorkspaceState.subjects.add(newSub)
                            newSub
                        }
                    }
                }

                // ── 6. Sticky Bottom Action Bar (Selalu Tampil) ──────────────────
                HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = LernspaceTextSecondary) }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Rapid Entry: "Simpan & Lanjut Tambah"
                        OutlinedButton(
                            onClick = {
                                if (subjectNameInput.isNotBlank()) {
                                    val finalSubject = resolveSubject()
                                    val currentStart = startTime.trim()
                                    val currentEnd = endTime.trim()
                                    val dur = (parseH(currentEnd) * 60 + parseM(currentEnd)) - (parseH(currentStart) * 60 + parseM(currentStart))
                                    val safeDur = if (dur > 0) dur else 45

                                    onConfirm(
                                        finalSubject,
                                        dayOfWeek,
                                        currentStart,
                                        currentEnd,
                                        room.trim(),
                                        lecturer.trim(),
                                        classType
                                    )

                                    // Auto chain: next class start time = current class end time!
                                    startTime = currentEnd
                                    endTime = addMinutes(currentEnd, safeDur)
                                    subjectNameInput = ""
                                    subjectCodeInput = ""
                                    selectedSubject = null
                                    savedFeedback = "'${finalSubject.name}' tersimpan! Jam mulai berikutnya diatur ke $currentEnd."
                                }
                            },
                            enabled = subjectNameInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan & Lanjut Tambah", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        // Normal Save: "Simpan & Tutup"
                        Button(
                            onClick = {
                                if (subjectNameInput.isNotBlank()) {
                                    val finalSubject = resolveSubject()
                                    onConfirm(
                                        finalSubject,
                                        dayOfWeek,
                                        startTime.trim(),
                                        endTime.trim(),
                                        room.trim(),
                                        lecturer.trim(),
                                        classType
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = subjectNameInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                        ) {
                            Text("Simpan & Tutup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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
    weeklyClasses: List<WeeklyClassUi>,
    tasks: List<TaskUiModel>,
    scheduleItems: List<PlannerItemUi>,
    initialDayOffset: Int,
    onDismiss: () -> Unit,
    onAccept: (
        subject: com.lernspace.app.ui.learning.SubjectUi?,
        customSubjectName: String,
        topic: com.lernspace.app.ui.learning.TopicUi?,
        customTopicName: String,
        durationMinutes: Int,
        dayOffset: Int,
        dayOfWeek: Int,
        startStr: String,
        endStr: String,
        reason: String
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    var selectedDayOffset by remember { mutableIntStateOf(initialDayOffset.coerceIn(0, 6)) }
    val selectedDate = remember(selectedDayOffset) { today.plusDays(selectedDayOffset.toLong()) }
    val dayOfWeek = selectedDate.dayOfWeek.value // 1 = Monday .. 7 = Sunday

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var customSubjectName by remember { mutableStateOf("") }

    val subjectTopics = remember(selectedSubject) {
        if (selectedSubject != null) topics.filter { it.subjectId == selectedSubject!!.id } else emptyList()
    }
    var selectedTopic by remember(selectedSubject) { mutableStateOf(subjectTopics.firstOrNull()) }
    var customTopicName by remember { mutableStateOf("") }

    var durationMinutes by remember { mutableIntStateOf(60) }

    // Dynamic slot calculation based on selected day's classes & existing planner items
    val dayClasses = weeklyClasses.filter { it.dayOfWeek == dayOfWeek }
    val dayPlannerItems = scheduleItems.filter { it.dayOffset == selectedDayOffset || (it.dayOfWeek == dayOfWeek && it.dayOffset == -1) }

    val recommendedSlot = calculateRecommendedSlot(dayClasses, dayPlannerItems, durationMinutes)

    var manualStartTime by remember(recommendedSlot) { mutableStateOf(recommendedSlot.startTime) }
    var manualEndTime by remember(recommendedSlot) { mutableStateOf(recommendedSlot.endTime) }

    val dayOffsets = listOf(0, 1, 2, 3, 4, 5, 6)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = LernspaceDark, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Auto-Schedule Jam Belajar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, tint = LernspaceTextSecondary)
                    }
                }

                Text(
                    text = "Mesin Hybrid menganalisis jadwal kuliah mingguan dan celah luang Anda untuk menjadwalkan sesi belajar fokus bebas bentrok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LernspaceTextSecondary
                )

                // 1. Target Day Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pilih Hari Target", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dayOffsets) { offset ->
                            val targetDate = today.plusDays(offset.toLong())
                            val dayLabel = when (offset) {
                                0 -> "Hari Ini"
                                1 -> "Besok"
                                else -> when (targetDate.dayOfWeek) {
                                    DayOfWeek.MONDAY -> "Senin"
                                    DayOfWeek.TUESDAY -> "Selasa"
                                    DayOfWeek.WEDNESDAY -> "Rabu"
                                    DayOfWeek.THURSDAY -> "Kamis"
                                    DayOfWeek.FRIDAY -> "Jumat"
                                    DayOfWeek.SATURDAY -> "Sabtu"
                                    DayOfWeek.SUNDAY -> "Minggu"
                                }
                            }
                            val isSelected = selectedDayOffset == offset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                modifier = Modifier.clickable { selectedDayOffset = offset }
                            ) {
                                Text(
                                    text = "$dayLabel (${targetDate.dayOfMonth}/${targetDate.monthValue})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Select Subject
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pilih Mata Kuliah / Bidang Belajar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    if (subjects.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjects) { s ->
                                val isSelected = selectedSubject?.id == s.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                    modifier = Modifier.clickable {
                                        selectedSubject = s
                                        customSubjectName = s.name
                                        selectedTopic = topics.filter { it.subjectId == s.id }.firstOrNull()
                                    }
                                ) {
                                    Text(
                                        text = s.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else LernspaceDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customSubjectName,
                            onValueChange = { customSubjectName = it },
                            placeholder = { Text("Nama mata kuliah (misal: Matematika, Fisika, Belajar Mandiri)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 3. Select Topic / Focus Note
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Target Sub-bab / Fokus Belajar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    if (subjectTopics.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjectTopics) { t ->
                                val isSelected = selectedTopic?.id == t.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                    modifier = Modifier.clickable {
                                        selectedTopic = t
                                        customTopicName = t.title
                                    }
                                ) {
                                    Text(
                                        text = t.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else LernspaceDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customTopicName,
                        onValueChange = { customTopicName = it },
                        placeholder = { Text(if (selectedTopic != null) "Keterangan tambahan atau nama topik khusus..." else "Ketik topik belajar (misal: Review Bab 1, Latihan Soal)", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Target Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Durasi Belajar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 45, 60, 90).forEach { mins ->
                            val isSelected = durationMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                modifier = Modifier.clickable { durationMinutes = mins }
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Suggested Slot Result Box (Clean Minimalist Monochrome)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slot Rekomendasi Bebas Bentrok", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                            Surface(shape = RoundedCornerShape(6.dp), color = LernspaceDark) {
                                Text(
                                    text = "$manualStartTime - $manualEndTime",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = recommendedSlot.reason,
                            fontSize = 11.sp,
                            color = LernspaceTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                // 6. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = LernspaceTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalSub = selectedSubject
                            val finalSubName = if (customSubjectName.isNotBlank()) customSubjectName else (selectedSubject?.name ?: "Belajar Mandiri")
                            val finalTop = selectedTopic
                            val finalTopTitle = if (customTopicName.isNotBlank()) customTopicName else (selectedTopic?.title ?: "Review Materi & Latihan")

                            onAccept(
                                finalSub,
                                finalSubName,
                                finalTop,
                                finalTopTitle,
                                durationMinutes,
                                selectedDayOffset,
                                dayOfWeek,
                                manualStartTime,
                                manualEndTime,
                                recommendedSlot.reason
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                    ) {
                        Text("Jadwalkan Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: Input Day Agenda Manual
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InputDayAgendaDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    initialDayOffset: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: ScheduleType,
        subject: com.lernspace.app.ui.learning.SubjectUi?,
        customSubjectName: String,
        startTime: String,
        endTime: String,
        dayOffset: Int,
        dayOfWeek: Int,
        room: String
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    var selectedDayOffset by remember { mutableIntStateOf(initialDayOffset.coerceIn(0, 6)) }
    val selectedDate = remember(selectedDayOffset) { today.plusDays(selectedDayOffset.toLong()) }
    val dayOfWeek = selectedDate.dayOfWeek.value

    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ScheduleType.STUDY_SESSION) }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var customSubjectName by remember { mutableStateOf("") }

    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("11:30") }
    var room by remember { mutableStateOf("") }

    val dayOffsets = listOf(0, 1, 2, 3, 4, 5, 6)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.width(460.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah Agenda Harian",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, tint = LernspaceTextSecondary)
                    }
                }

                // 1. Title
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Judul Agenda / Sesi", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("misal: Belajar Mandiri Kalkulus, Diskusi Kelompok", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Type Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Kategori Agenda", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Pair(ScheduleType.STUDY_SESSION, "Belajar"),
                            Pair(ScheduleType.LECTURE, "Kuliah"),
                            Pair(ScheduleType.LAB, "Praktikum"),
                            Pair(ScheduleType.EXAM, "Ujian / Kuis")
                        ).forEach { (type, label) ->
                            val isSelected = selectedType == type
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                modifier = Modifier.clickable { selectedType = type }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Target Day
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pilih Hari", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dayOffsets) { offset ->
                            val targetDate = today.plusDays(offset.toLong())
                            val dayLabel = when (offset) {
                                0 -> "Hari Ini"
                                1 -> "Besok"
                                else -> when (targetDate.dayOfWeek) {
                                    DayOfWeek.MONDAY -> "Sen"
                                    DayOfWeek.TUESDAY -> "Sel"
                                    DayOfWeek.WEDNESDAY -> "Rab"
                                    DayOfWeek.THURSDAY -> "Kam"
                                    DayOfWeek.FRIDAY -> "Jum"
                                    DayOfWeek.SATURDAY -> "Sab"
                                    DayOfWeek.SUNDAY -> "Min"
                                }
                            }
                            val isSelected = selectedDayOffset == offset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                modifier = Modifier.clickable { selectedDayOffset = offset }
                            ) {
                                Text(
                                    text = "$dayLabel (${targetDate.dayOfMonth})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else LernspaceDark,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Subject Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mata Kuliah Terkait (Opsional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    if (subjects.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjects) { s ->
                                val isSelected = selectedSubject?.id == s.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LernspaceDark else LernspaceSurfaceLow,
                                    border = BorderStroke(1.dp, if (isSelected) LernspaceDark else LernspaceBorder),
                                    modifier = Modifier.clickable {
                                        selectedSubject = if (isSelected) null else s
                                    }
                                ) {
                                    Text(
                                        text = s.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else LernspaceDark,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customSubjectName,
                            onValueChange = { customSubjectName = it },
                            placeholder = { Text("Nama mata kuliah (opsional)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 5. Time range
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jam Mulai", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            placeholder = { Text("08:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jam Selesai", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            placeholder = { Text("09:30") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 6. Room
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ruangan / Lokasi (Opsional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        placeholder = { Text("misal: Lab Komputer, Perpus Lt 2", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 7. Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = LernspaceTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalTitle = title.ifBlank { "Sesi Belajar" }
                            val finalSub = selectedSubject
                            val finalSubName = customSubjectName.ifBlank { selectedSubject?.name ?: "" }
                            onConfirm(
                                finalTitle,
                                selectedType,
                                finalSub,
                                finalSubName,
                                startTime,
                                endTime,
                                selectedDayOffset,
                                dayOfWeek,
                                room
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                    ) {
                        Text("Simpan Agenda", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
