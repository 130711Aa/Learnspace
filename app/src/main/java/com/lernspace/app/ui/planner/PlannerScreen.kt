package com.lernspace.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.data.repository.*
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
    var showHybridSuggestDialog by remember { mutableStateOf(false) }
    var selectedCalendarDay by remember { mutableIntStateOf(8) } // Selected date in Nov 2026

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
                    onAcceptSuggestion = { id -> AcademicWorkspaceState.acceptHybridSuggestion(id) },
                    onToggleComplete = { item ->
                        val idx = scheduleItems.indexOfFirst { it.id == item.id }
                        if (idx != -1) {
                            val updated = item.copy(isCompleted = !item.isCompleted)
                            scheduleItems[idx] = updated
                            com.lernspace.app.data.local.AcademicDatabaseManager.persistPlannerItem(updated)
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
                com.lernspace.app.data.local.AcademicDatabaseManager.persistWeeklyClass(newClass)
                showAddWeeklyClassDialog = false
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                            Text("Deadline Tugas", fontSize = 11.sp, color = LernspaceTextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentBlue))
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

                                        // Badges
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            // Task deadline pill
                                            if (dayTasksCount > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (hasPendingTask) Color(0xFFEFF6FF) else Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = "$dayTasksCount Tugas",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (hasPendingTask) Color(0xFF1D4ED8) else Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Class indicator dot
                                            if (hasClass) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AccentBlue))
                                                    Text("Kuliah", fontSize = 8.sp, color = LernspaceTextSecondary)
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
                        // Section 1: Deadline Tugas Hari Ini
                        item {
                            Text(
                                text = "DEADLINE TUGAS ($selectedDay Nov)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
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

                        // Section 2: Jadwal Kuliah Hari Ini
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "JADWAL KULIAH RUTIN",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
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
                                            Text(text = "${c.subjectName} (${c.subjectCode})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
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
    val filteredTasks = remember(tasks.size, tasks.map { it.isCompleted }, searchQuery, selectedStatusFilter, selectedPriorityFilter, selectedSubjectId, sortBy) {
        tasks.filter { task ->
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
// VIEW 2: Jadwal Mapel Mingguan Builder (Weekly Class Schedule Builder)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeeklyTimetableBuilderView(
    weeklyClasses: List<WeeklyClassUi>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onAddClass: () -> Unit,
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Jadwal Mata Pelajaran / Kuliah Mingguan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "Jadwal rutin tetap tiap minggu. Menjadi acuan dasar kalender dan mesin pencari jam belajar kosong.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LernspaceTextSecondary
                    )
                }

                Button(
                    onClick = onAddClass,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Jadwal Mapel")
                }
            }

            // 7 Days Columns Timetable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                weekDays.forEach { (dayIndex, dayName) ->
                    val classes = weeklyClasses.filter { it.dayOfWeek == dayIndex }.sortedBy { it.startTime }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Day header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = "${classes.size}",
                                    fontSize = 10.sp,
                                    color = LernspaceTextSecondary
                                )
                            }

                            HorizontalDivider(color = LernspaceBorder)

                            if (classes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Kosong",
                                        fontSize = 10.sp,
                                        color = LernspaceTextSecondary.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(classes, key = { it.id }) { c ->
                                        val subj = subjects.find { it.id == c.subjectId }
                                        val subColor = subj?.color ?: AccentBlue

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .fillMaxHeight()
                                                        .background(subColor)
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${c.startTime} - ${c.endTime}",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = LernspaceDark
                                                        )
                                                        IconButton(
                                                            onClick = { onDeleteClass(c) },
                                                            modifier = Modifier.size(16.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Close, null, tint = LernspaceTextSecondary, modifier = Modifier.size(10.dp))
                                                        }
                                                    }
                                                    Text(
                                                        text = c.subjectName,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LernspaceDark,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = c.subjectCode,
                                                        fontSize = 8.sp,
                                                        color = subColor
                                                    )
                                                    if (c.room != null) {
                                                        Text(
                                                            text = c.room,
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
@Composable
private fun DayAgendaView(
    scheduleItems: List<PlannerItemUi>,
    weeklyClasses: List<WeeklyClassUi>,
    tasks: List<TaskUiModel>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onAcceptSuggestion: (String) -> Unit,
    onToggleComplete: (PlannerItemUi) -> Unit,
    onOpenNoteForTopic: (topicId: String, topicTitle: String, subjectId: String?, subjectName: String?) -> Unit,
    onViewInSyllabus: (String?) -> Unit,
    onAutoSchedule: () -> Unit,
    onDeleteItem: (PlannerItemUi) -> Unit
) {
    var selectedDayOffset by remember { mutableIntStateOf(0) } // 0 = Today
    val dayNames = listOf("Hari Ini (Senin, 8 Nov)", "Besok (Selasa, 9 Nov)", "Rabu, 10 Nov", "Kamis, 11 Nov", "Jumat, 12 Nov")

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
            // Header Bar
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

                Button(
                    onClick = onAutoSchedule,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Schedule Jam Belajar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Timeline Items List
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
                color = if (isHybrid) AccentBlue else LernspaceBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isHybrid) Color(0xFFEFF6FF) else if (item.isCompleted) LernspaceSurfaceLow else LernspaceSurfaceCard,
        shadowElevation = if (isHybrid) 2.dp else 0.5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Ribbon
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (isHybrid) AccentBlue else subjectColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
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

                        if (item.subjectName != null) {
                            Text(
                                text = "•  ${item.subjectName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subjectColor
                            )
                        }

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

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) LernspaceTextSecondary else LernspaceDark,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (isHybrid && item.aiReason != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
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
                                Icon(Icons.Filled.Lightbulb, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                Text(
                                    text = item.aiReason,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onAcceptSuggestion,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Accept & Jadwalkan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

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
                            TextButton(
                                onClick = onViewSyllabus,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cek Silabus", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onOpenNote,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Create, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buka Catatan Canvas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
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

    var room by remember { mutableStateOf("") }
    var lecturer by remember { mutableStateOf("") }
    var classType by remember { mutableStateOf("Kuliah (Teori)") }

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
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .width(520.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Set Jadwal Mapel Mingguan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                // ── 1. Input Nama & Kode Mata Kuliah ─────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Mata Kuliah / Pelajaran *",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick select chips from existing subjects
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
                                    label = { Text("${s.name} (${s.code})", fontSize = 11.sp) },
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
                            label = { Text("Nama Mata Kuliah / Mapel *") },
                            placeholder = { Text("misal: Kalkulus, Biologi Sel, Kimia") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = subjectCodeInput,
                            onValueChange = { subjectCodeInput = it },
                            label = { Text("Kode (Opsional)") },
                            placeholder = { Text("misal: KAL101") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // ── 2. Hari Perkuliahan Rutin ────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Hari Perkuliahan Rutin",
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

                // ── 3. Redesigned Practical Time Picker (Tanpa Perlu Ngetik) ────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Jam Perkuliahan",
                            style = MaterialTheme.typography.labelSmall,
                            color = LernspaceTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { showManualTimeInput = !showManualTimeInput },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (showManualTimeInput) Icons.Filled.Schedule else Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showManualTimeInput) "Mode Sentuh Praktis" else "Ketik Manual",
                                fontSize = 11.sp,
                                color = AccentBlue
                            )
                        }
                    }

                    // A. Slot Waktu Cepat (1-Tap Academic Slots)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Slot Waktu Cepat:", fontSize = 10.sp, color = LernspaceTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(commonClassSlots) { (slot, label) ->
                                val isSelected = startTime == slot.first && endTime == slot.second
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        startTime = slot.first
                                        endTime = slot.second
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
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
                        // B. Interactive Time Cards (Jam Mulai & Jam Selesai)
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
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (activeTimeTarget == "START") 1.5.dp else 1.dp,
                                    color = if (activeTimeTarget == "START") AccentBlue else LernspaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

                            Icon(Icons.Filled.ArrowForward, null, tint = LernspaceTextSecondary, modifier = Modifier.size(16.dp))

                            // Card Jam Selesai
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTimeTarget = "END" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTimeTarget == "END") Color(0xFFEFF6FF) else LernspaceSurfaceLow,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (activeTimeTarget == "END") 1.5.dp else 1.dp,
                                    color = if (activeTimeTarget == "END") AccentBlue else LernspaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

                        // C. Duration Quick Helper (Hitung Jam Selesai Otomatis dari Jam Mulai)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Durasi:", fontSize = 10.sp, color = LernspaceTextSecondary, fontWeight = FontWeight.Medium)
                            listOf(
                                Pair(50, "+50m (1 SKS)"),
                                Pair(100, "+100m (2 SKS)"),
                                Pair(150, "+150m (3 SKS)"),
                                Pair(120, "+2 Jam")
                            ).forEach { (m, label) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LernspaceSurfaceLow,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                    modifier = Modifier.clickable {
                                        endTime = addMinutes(startTime, m)
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

                        // D. Touch-Friendly Hour & Minute Grid
                        val currentTargetTime = if (activeTimeTarget == "START") startTime else endTime
                        val currentH = parseH(currentTargetTime)
                        val currentM = parseM(currentTargetTime)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pilih ${if (activeTimeTarget == "START") "Jam Mulai" else "Jam Selesai"} (Sentuh Jam & Menit):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceDark
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                            modifier = Modifier.clickable {
                                                val adjusted = addMinutes(currentTargetTime, -5)
                                                if (activeTimeTarget == "START") startTime = adjusted else endTime = adjusted
                                            }
                                        ) {
                                            Text("-5m", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = LernspaceDark)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                            modifier = Modifier.clickable {
                                                val adjusted = addMinutes(currentTargetTime, 5)
                                                if (activeTimeTarget == "START") startTime = adjusted else endTime = adjusted
                                            }
                                        ) {
                                            Text("+5m", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = LernspaceDark)
                                        }
                                    }
                                }

                                // Hour Row (07..21)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Jam:", fontSize = 10.sp, color = LernspaceTextSecondary, modifier = Modifier.width(34.dp))
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items((7..21).toList()) { h ->
                                            val isSelected = currentH == h
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) AccentBlue else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else LernspaceBorder),
                                                modifier = Modifier.clickable {
                                                    val newT = fmtTime(h, currentM)
                                                    if (activeTimeTarget == "START") {
                                                        startTime = newT
                                                        if (parseH(endTime) < h || (parseH(endTime) == h && parseM(endTime) <= currentM)) {
                                                            endTime = addMinutes(newT, 100)
                                                        }
                                                    } else {
                                                        endTime = newT
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = String.format(java.util.Locale.US, "%02d", h),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else LernspaceDark,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Minute Row (00, 10, 15, 30, 40, 45, 50)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Menit:", fontSize = 10.sp, color = LernspaceTextSecondary, modifier = Modifier.width(34.dp))
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(listOf(0, 10, 15, 30, 40, 45, 50)) { m ->
                                            val isSelected = currentM == m
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) AccentBlue else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else LernspaceBorder),
                                                modifier = Modifier.clickable {
                                                    val newT = fmtTime(currentH, m)
                                                    if (activeTimeTarget == "START") {
                                                        startTime = newT
                                                    } else {
                                                        endTime = newT
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = String.format(java.util.Locale.US, ":%02d", m),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else LernspaceDark,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 4. Tipe Kuliah ──────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tipe Pertemuan", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Kuliah (Teori)", "Praktikum / Lab", "Diskusi / Tutorial").forEach { t ->
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

                // ── 5. Ruangan & Dosen (Opsional) ───────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Ruangan / Lab (Opsional)") },
                        placeholder = { Text("misal: Gedung A 203") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = lecturer,
                        onValueChange = { lecturer = it },
                        label = { Text("Dosen / Pengajar (Opsional)") },
                        placeholder = { Text("misal: Dr. Ir. Budi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // ── 6. Tombol Aksi ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subjectNameInput.isNotBlank()) {
                                val finalSubject = selectedSubject ?: run {
                                    val existing = subjects.find { it.name.equals(subjectNameInput.trim(), ignoreCase = true) }
                                    if (existing != null) {
                                        existing
                                    } else {
                                        val code = if (subjectCodeInput.isNotBlank()) {
                                            subjectCodeInput.trim()
                                        } else {
                                            subjectNameInput.trim().split(" ")
                                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                                .joinToString("")
                                                .take(4)
                                                .ifBlank { "SUB" }
                                        }
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
                                onConfirm(
                                    finalSubject,
                                    dayOfWeek,
                                    startTime.trim(),
                                    endTime.trim(),
                                    room.trim(),
                                    lecturer.trim(),
                                    classType
                                )
                            }
                        },
                        enabled = subjectNameInput.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                    ) {
                        Text("Simpan Jadwal Mapel", fontWeight = FontWeight.Bold)
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
                        Icon(Icons.Filled.AutoAwesome, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Text("Auto-Schedule Study Slot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                Text(
                    text = "Mesin Hybrid menganalisis jadwal kuliah mingguan dan tenggat waktu tugas Anda untuk menemukan celah belajar bebas bentrok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LernspaceTextSecondary
                )

                // Select Subject
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pilih Mata Kuliah untuk Dipelajari", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
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
                        Text("Target Sub-bab / Materi", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
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
                    Text("Durasi Belajar", style = MaterialTheme.typography.labelMedium, color = LernspaceDark)
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
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slot Rekomendasi Bebas Bentrok", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                            Surface(shape = RoundedCornerShape(4.dp), color = AccentBlue) {
                                Text("10:30 - 11:30", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "Terdeteksi jeda waktu luang di jadwal Anda. Waktu ideal untuk sesi fokus mandiri dengan buffer istirahat aman.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
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
                                    "Saran deterministik: Slot belajar 60m dijadwalkan di antara jam kuliah pagi."
                                )
                            }
                        },
                        enabled = selectedSubject != null && selectedTopic != null,
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
