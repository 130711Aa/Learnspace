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

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Pair("Kalender & Deadline", Icons.Filled.CalendarMonth),
        Pair("Jadwal Mapel Mingguan", Icons.Filled.AccessTime),
        Pair("Day Agenda & Auto-Study", Icons.Filled.Today)
    )

    var activeNote by remember { mutableStateOf<NoteUiModel?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddWeeklyClassDialog by remember { mutableStateOf(false) }
    var showHybridSuggestDialog by remember { mutableStateOf(false) }
    var selectedCalendarDay by remember { mutableIntStateOf(8) } // Selected date in Nov 2026

    // If active note is opened, display fullscreen NoteEditorScreen!
    if (activeNote != null) {
        NoteEditorScreen(
            note = activeNote!!,
            onBack = { activeNote = null }
        )
        return
    }

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
                    text = "Planner & Academic Schedule",
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
                        text = "${tasks.count { !it.isCompleted }} Active Tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentAmber,
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
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
                        modifier = Modifier.clickable { selectedTab = index }
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

                1 -> WeeklyTimetableBuilderView(
                    weeklyClasses = weeklyClasses,
                    subjects = subjects,
                    onAddClass = { showAddWeeklyClassDialog = true },
                    onDeleteClass = { weeklyClasses.remove(it) }
                )

                2 -> DayAgendaView(
                    scheduleItems = scheduleItems,
                    weeklyClasses = weeklyClasses,
                    tasks = tasks,
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
                    onAutoSchedule = { showHybridSuggestDialog = true },
                    onDeleteItem = { scheduleItems.remove(it) }
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
            onConfirm = { title, subject, topic, dueDayNumber, dueTime, priority, estMins, notesStr ->
                tasks.add(
                    TaskUiModel(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        subjectId = subject?.id,
                        subjectName = subject?.name,
                        topicId = topic?.id,
                        topicName = topic?.title,
                        dueDate = "Nov $dueDayNumber, 2026",
                        dueDayNumber = dueDayNumber,
                        dueTime = dueTime,
                        priority = priority,
                        estimatedMinutes = estMins,
                        isCompleted = false,
                        notes = notesStr
                    )
                )
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
                weeklyClasses.add(
                    WeeklyClassUi(
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
                )
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentAmber))
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
                                                    color = if (hasPendingTask) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = "$dayTasksCount Tugas",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (hasPendingTask) Color(0xFFB45309) else Color(0xFF047857),
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
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
                                color = if (tasksOnDay.any { !it.isCompleted }) Color(0xFFB45309) else LernspaceDark
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
                                val subColor = subj?.color ?: AccentAmber

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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
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
                color = if (isHybrid) AccentAmber else LernspaceBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isHybrid) Color(0xFFFFFBEB) else if (item.isCompleted) LernspaceSurfaceLow else LernspaceSurfaceCard,
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
                    .background(if (isHybrid) AccentAmber else subjectColor)
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
@Composable
private fun InputTaskDialog(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    initialDayNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: com.lernspace.app.ui.learning.SubjectUi?,
        topic: com.lernspace.app.ui.learning.TopicUi?,
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
    var dueDayNumber by remember { mutableIntStateOf(initialDayNumber) }
    var dueTime by remember { mutableStateOf("23:59") }
    var priority by remember { mutableStateOf("HIGH") } // HIGH, MEDIUM, LOW
    var estMinutes by remember { mutableIntStateOf(90) }
    var notesStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(480.dp)
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

                // Deadline inputs (Tanggal di November & Jam)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = "$dueDayNumber",
                        onValueChange = { str -> str.toIntOrNull()?.let { dueDayNumber = it.coerceIn(1, 30) } },
                        label = { Text("Tgl Nov (1-30)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Jam Deadline") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Priority chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Prioritas Tugas", style = MaterialTheme.typography.labelMedium, color = LernspaceDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple("HIGH", "Tinggi (Urgent)", Color(0xFFE11D48)),
                            Triple("MEDIUM", "Sedang", AccentAmber),
                            Triple("LOW", "Rendah", Color(0xFF64748B))
                        ).forEach { (pKey, pLabel, pColor) ->
                            FilterChip(
                                selected = priority == pKey,
                                onClick = { priority = pKey },
                                label = { Text(pLabel, fontSize = 11.sp, color = if (priority == pKey) pColor else LernspaceDark) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
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
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), selectedSubject, selectedTopic, dueDayNumber, dueTime.trim(), priority, estMinutes, notesStr.trim())
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
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
    var dayOfWeek by remember { mutableIntStateOf(1) } // 1=Senin, 2=Selasa...
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("") }
    var lecturer by remember { mutableStateOf("") }
    var classType by remember { mutableStateOf("Lecture") } // Lecture, Lab, Practicum, Discussion

    val weekDays = listOf(
        Pair(1, "Senin"),
        Pair(2, "Selasa"),
        Pair(3, "Rabu"),
        Pair(4, "Kamis"),
        Pair(5, "Jumat"),
        Pair(6, "Sabtu"),
        Pair(7, "Minggu")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.width(480.dp)
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
                    Text("Set Jadwal Mapel Mingguan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LernspaceDark)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null)
                    }
                }

                // Subject picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pilih Mata Kuliah / Pelajaran", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubject == s,
                                onClick = { selectedSubject = s },
                                label = { Text("${s.name} (${s.code})", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Hari picker (Senin - Minggu)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hari Perkuliahan Rutin", style = MaterialTheme.typography.labelSmall, color = LernspaceTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(weekDays) { (dIdx, dName) ->
                            FilterChip(
                                selected = dayOfWeek == dIdx,
                                onClick = { dayOfWeek = dIdx },
                                label = { Text(dName, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Jam mulai & selesai
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Jam Mulai (08:00)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Jam Selesai (10:00)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Tipe Kuliah
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Kuliah (Teori)", "Praktikum / Lab", "Diskusi / Tutorial").forEach { t ->
                        FilterChip(
                            selected = classType == t,
                            onClick = { classType = t },
                            label = { Text(t, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Ruangan / Lab (misal: BioSci Hall 201)") },
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
                            if (selectedSubject != null) {
                                onConfirm(selectedSubject!!, dayOfWeek, startTime.trim(), endTime.trim(), room.trim(), lecturer.trim(), classType)
                            }
                        },
                        enabled = selectedSubject != null,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
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
                        Icon(Icons.Filled.AutoAwesome, null, tint = AccentAmber, modifier = Modifier.size(22.dp))
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
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slot Rekomendasi Bebas Bentrok", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Surface(shape = RoundedCornerShape(4.dp), color = AccentAmber) {
                                Text("10:30 - 11:30", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "Terdeteksi jeda 90 menit setelah Kuliah Neurobiology sebelum istirahat siang. Buffer 15 menit aman.",
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
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Text("Jadwalkan Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
