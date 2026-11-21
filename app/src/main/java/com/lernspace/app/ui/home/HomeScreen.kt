package com.lernspace.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.TaskUiModel
import com.lernspace.app.data.repository.WeeklyClassUi
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.planner.InputTaskDialog
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.NoteEditorScreen
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private fun parseTimeToMinutes(timeStr: String): Int {
    val parts = timeStr.trim().split(":")
    if (parts.size >= 2) {
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }
    return 0
}

enum class ClassTimingStatus {
    ONGOING,
    UPCOMING_TODAY,
    UPCOMING_NEXT_DAY,
    NONE
}

data class NextUpScheduleInfo(
    val weeklyClass: WeeklyClassUi?,
    val status: ClassTimingStatus,
    val badgeLabel: String,
    val badgeColor: Color,
    val dayLabel: String
)

@Composable
fun HomeScreen(
    onNavigateToScreen: (String) -> Unit = {},
    onQuickAddTask: () -> Unit = {}
) {
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics
    val notes = AcademicWorkspaceState.notes
    val tasks = AcademicWorkspaceState.tasks
    val weeklyClasses = AcademicWorkspaceState.weeklyClasses

    val isDark = LocalIsDarkTheme.current
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Dynamic Live Date & Time Engine
    val now = remember { Calendar.getInstance() }
    val formattedCurrentDate = remember {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID"))
        val res = sdf.format(cal.time)
        res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
    }

    val javaDow = now.get(Calendar.DAY_OF_WEEK)
    val currentDayOfWeek = if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1 // 1=Mon ... 7=Sun
    val currentDayName = when (currentDayOfWeek) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        else -> "Minggu"
    }

    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    // Today's classes sorted by start time
    val todayClasses = remember(weeklyClasses.toList(), currentDayOfWeek) {
        weeklyClasses.filter { it.dayOfWeek == currentDayOfWeek }.sortedBy { parseTimeToMinutes(it.startTime) }
    }

    // Smart Upcoming Schedule Item (ala live timetable)
    val nextUpSchedule = remember(weeklyClasses.toList(), todayClasses, currentDayOfWeek, currentMinutes) {
        // 1. Ongoing class right now
        val ongoingToday = todayClasses.firstOrNull { c ->
            val startM = parseTimeToMinutes(c.startTime)
            val endM = parseTimeToMinutes(c.endTime)
            currentMinutes in startM..endM
        }
        if (ongoingToday != null) {
            return@remember NextUpScheduleInfo(
                weeklyClass = ongoingToday,
                status = ClassTimingStatus.ONGOING,
                badgeLabel = "SEDANG BERLANGSUNG · HINGGA ${ongoingToday.endTime}",
                badgeColor = Color(0xFF10B981),
                dayLabel = "Hari Ini ($currentDayName)"
            )
        }

        // 2. Upcoming class today later
        val upcomingToday = todayClasses.firstOrNull { c ->
            parseTimeToMinutes(c.startTime) > currentMinutes
        }
        if (upcomingToday != null) {
            return@remember NextUpScheduleInfo(
                weeklyClass = upcomingToday,
                status = ClassTimingStatus.UPCOMING_TODAY,
                badgeLabel = "${upcomingToday.startTime} · UPCOMING CLASS HARI INI",
                badgeColor = AccentTeal,
                dayLabel = "Hari Ini ($currentDayName)"
            )
        }

        // 3. Next closest class in the upcoming days (tomorrow through next week)
        for (offset in 1..7) {
            val targetDay = ((currentDayOfWeek - 1 + offset) % 7) + 1
            val classesOnTargetDay = weeklyClasses
                .filter { it.dayOfWeek == targetDay }
                .sortedBy { parseTimeToMinutes(it.startTime) }

            if (classesOnTargetDay.isNotEmpty()) {
                val nextClass = classesOnTargetDay.first()
                val targetDayName = when (targetDay) {
                    1 -> "Senin"
                    2 -> "Selasa"
                    3 -> "Rabu"
                    4 -> "Kamis"
                    5 -> "Jumat"
                    6 -> "Sabtu"
                    else -> "Minggu"
                }
                val relativeLabel = if (offset == 1) "Besok ($targetDayName)" else targetDayName
                return@remember NextUpScheduleInfo(
                    weeklyClass = nextClass,
                    status = ClassTimingStatus.UPCOMING_NEXT_DAY,
                    badgeLabel = "$targetDayName, ${nextClass.startTime} · JADWAL TERDEKAT",
                    badgeColor = AccentBlue,
                    dayLabel = relativeLabel
                )
            }
        }

        NextUpScheduleInfo(
            weeklyClass = null,
            status = ClassTimingStatus.NONE,
            badgeLabel = "TIDAK ADA JADWAL KELAS",
            badgeColor = Color(0xFF64748B),
            dayLabel = ""
        )
    }

    val nextUpClass = nextUpSchedule.weeklyClass
    val nextUpTopic = remember(nextUpClass, topics.size) {
        topics.find { it.subjectId == nextUpClass?.subjectId } ?: topics.firstOrNull()
    }

    val pendingTasks = remember(tasks.size, tasks.map { it.isCompleted }) {
        tasks.filter { !it.isCompleted }.sortedBy { it.dueDayNumber }
    }
    val masteredTopicsCount = remember(topics.size, topics.map { it.status }) {
        topics.count { it.status == TopicStatus.COMFORTABLE || it.status == TopicStatus.MASTERED }
    }
    val recentNotes = remember(notes.size) {
        notes.take(4)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LernspaceDark,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LernspaceSurfaceLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Text(
                        text = formattedCurrentDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Actions Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Canvas Note button
                OutlinedButton(
                    onClick = {
                        val newScratchpad = AcademicWorkspaceState.createScratchpad()
                        AcademicWorkspaceState.openNoteEditor(newScratchpad)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Draw, null, modifier = Modifier.size(16.dp), tint = LernspaceDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catatan Cepat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                }

                // Input Tugas button
                Button(
                    onClick = { showAddTaskDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color.White else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.AddAlert, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Input Tugas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 2-Column Responsive Dashboard Viewport ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left Column (60%): Up Next Hero + Today's Schedule + Urgent Tasks ──
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Hero Card: Next Up Session (Smart Upcoming Schedule)
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = LernspaceSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            shadowElevation = 1.dp
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Left accent bar
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(nextUpSchedule.badgeColor)
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(18.dp)
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = nextUpSchedule.badgeColor.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = nextUpSchedule.badgeColor
                                                )
                                                Text(
                                                    text = nextUpSchedule.badgeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LernspaceDark
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = LernspaceSurfaceLow,
                                            border = BorderStroke(1.dp, LernspaceBorder)
                                        ) {
                                            Text(
                                                text = (nextUpClass?.subjectName ?: "LERNSPACE").uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = LernspaceDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        val heroTitle = nextUpTopic?.title ?: if (nextUpClass != null) {
                                            if (nextUpClass.subjectCode.isNotBlank()) "${nextUpClass.subjectName} (${nextUpClass.subjectCode})" else nextUpClass.subjectName
                                        } else "Mulai Sesi Belajar Anda"

                                        val heroSubtitle = nextUpTopic?.description ?: if (nextUpClass != null) {
                                            buildString {
                                                append("${nextUpSchedule.dayLabel} • ${nextUpClass.startTime} - ${nextUpClass.endTime}")
                                                if (!nextUpClass.room.isNullOrBlank()) append(" • Ruang: ${nextUpClass.room}")
                                                if (!nextUpClass.lecturer.isNullOrBlank()) append(" • Dosen: ${nextUpClass.lecturer}")
                                            }
                                        } else "Pilih materi di silabus atau buat catatan baru di Canvas Workspace."

                                        Text(
                                            text = heroTitle,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = heroSubtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = LernspaceTextSecondary
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Open Linked Note button
                                        Button(
                                            onClick = {
                                                if (nextUpTopic != null) {
                                                    val existing = notes.find { it.topicId == nextUpTopic.id }
                                                    if (existing != null) {
                                                        AcademicWorkspaceState.openNoteEditor(existing)
                                                    } else {
                                                        val subj = subjects.find { it.id == nextUpTopic.subjectId } ?: subjects.firstOrNull()
                                                        if (subj != null) {
                                                            val newNote = AcademicWorkspaceState.createNoteForTopic(subj, nextUpTopic)
                                                            AcademicWorkspaceState.openNoteEditor(newNote)
                                                        } else {
                                                            val newScratchpad = AcademicWorkspaceState.createScratchpad(title = nextUpTopic.title)
                                                            AcademicWorkspaceState.openNoteEditor(newScratchpad)
                                                        }
                                                    }
                                                } else if (nextUpClass != null) {
                                                    val targetClass = nextUpClass
                                                    val subj = subjects.find { it.id == targetClass.subjectId } ?: subjects.firstOrNull()
                                                    if (subj != null) {
                                                        val existing = notes.find { it.subjectId == subj.id }
                                                        if (existing != null) {
                                                            AcademicWorkspaceState.openNoteEditor(existing)
                                                        } else {
                                                            val newScratchpad = AcademicWorkspaceState.createScratchpad(title = "Catatan ${subj.name}")
                                                            AcademicWorkspaceState.openNoteEditor(newScratchpad)
                                                        }
                                                    } else {
                                                        val newScratchpad = AcademicWorkspaceState.createScratchpad(title = targetClass.subjectName)
                                                        AcademicWorkspaceState.openNoteEditor(newScratchpad)
                                                    }
                                                } else {
                                                    val newScratchpad = AcademicWorkspaceState.createScratchpad()
                                                    AcademicWorkspaceState.openNoteEditor(newScratchpad)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) Color.White else Color.Black,
                                                contentColor = if (isDark) Color.Black else Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Filled.Draw, null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buka Catatan Canvas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // View in Syllabus or Planner
                                        OutlinedButton(
                                            onClick = {
                                                if (nextUpTopic != null) {
                                                    onNavigateToScreen(Screen.Learning.route)
                                                } else {
                                                    onNavigateToScreen(Screen.Planner.route)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (nextUpTopic != null) Icons.Filled.AutoStories else Icons.Filled.CalendarMonth,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = LernspaceDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (nextUpTopic != null) "Lihat di Silabus" else "Lihat di Jadwal Kuliah",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LernspaceDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Section: Jadwal Kuliah Hari Ini (Real-time dynamic)
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = LernspaceSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            shadowElevation = 1.dp
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Jadwal Kuliah Hari Ini ($currentDayName)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LernspaceSurfaceLow,
                                            border = BorderStroke(1.dp, LernspaceBorder)
                                        ) {
                                            Text(
                                                text = "${todayClasses.size} Kelas",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LernspaceDark,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Planner.route) }
                                    ) {
                                        Text(
                                            text = "Buka Jadwal Penuh",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = LernspaceDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                if (todayClasses.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LernspaceSurfaceLow,
                                        border = BorderStroke(1.dp, LernspaceBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.EventAvailable,
                                                contentDescription = null,
                                                tint = AccentTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Tidak ada jadwal kuliah hari ini ($currentDayName)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LernspaceDark
                                                )
                                                if (nextUpClass != null) {
                                                    Text(
                                                        text = "Jadwal terdekat: ${nextUpClass.subjectName} (${nextUpSchedule.dayLabel}, ${nextUpClass.startTime})",
                                                        fontSize = 11.sp,
                                                        color = LernspaceTextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        todayClasses.forEach { c ->
                                            val subj = subjects.find { it.id == c.subjectId }
                                            val subColor = subj?.color ?: AccentBlue
                                            val startM = parseTimeToMinutes(c.startTime)
                                            val endM = parseTimeToMinutes(c.endTime)
                                            val isOngoing = currentMinutes in startM..endM
                                            val isFinished = currentMinutes > endM

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isOngoing) subColor.copy(alpha = 0.08f) else LernspaceSurfaceLow,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    if (isOngoing) 1.5.dp else 1.dp,
                                                    if (isOngoing) subColor else LernspaceBorder
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isFinished) LernspaceTextSecondary else subColor)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = if (c.subjectCode.isNotBlank()) "${c.subjectName} (${c.subjectCode})" else c.subjectName,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isFinished) LernspaceTextSecondary else LernspaceDark,
                                                                textDecoration = if (isFinished) TextDecoration.LineThrough else null
                                                            )
                                                            Text(
                                                                text = "${c.startTime} - ${c.endTime} • Ruang: ${c.room ?: "TBA"}",
                                                                fontSize = 11.sp,
                                                                color = LernspaceTextSecondary
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = when {
                                                            isOngoing -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                            isFinished -> LernspaceBorder.copy(alpha = 0.5f)
                                                            else -> subColor.copy(alpha = 0.12f)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = when {
                                                                isOngoing -> "SEDANG BERLANGSUNG"
                                                                isFinished -> "SELESAI ✓"
                                                                else -> c.type
                                                            },
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                isOngoing -> Color(0xFF047857)
                                                                isFinished -> LernspaceTextSecondary
                                                                else -> subColor
                                                            },
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                    // 3. Section: Deadline Tugas Mendesak
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = LernspaceSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            shadowElevation = 1.dp
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Deadline Tugas Mendesak",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LernspaceSurfaceLow,
                                            border = BorderStroke(1.dp, LernspaceBorder)
                                        ) {
                                            Text(
                                                text = "${pendingTasks.size} Pending",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LernspaceDark,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Planner.route) }
                                    ) {
                                        Text(
                                            text = "Buka Daftar Tugas",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = LernspaceDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                if (pendingTasks.isEmpty()) {
                                    Text(
                                        text = "Semua tugas beres! Tidak ada deadline mendesak.",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pendingTasks.take(4).forEach { task ->
                                            val subj = subjects.find { it.id == task.subjectId }
                                            val subColor = subj?.color ?: AccentBlue

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (task.isCompleted) Color(0xFFF8FAFC) else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (task.isCompleted) LernspaceBorder else subColor.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        IconButton(
                                                            onClick = { AcademicWorkspaceState.toggleTaskCompletion(task.id) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                                contentDescription = "Done",
                                                                tint = if (task.isCompleted) Color(0xFF16A34A) else LernspaceTextSecondary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        Column {
                                                            Text(
                                                                text = task.title,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (task.isCompleted) LernspaceTextSecondary else LernspaceDark,
                                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${task.subjectName ?: "Umum"} • Due ${task.dueDate} (${task.dueTime})",
                                                                fontSize = 10.sp,
                                                                color = LernspaceTextSecondary
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (task.priority.uppercase() == "HIGH") Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(
                                                            text = task.priority.uppercase(),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (task.priority.uppercase() == "HIGH") Color(0xFFDC2626) else Color(0xFFD97706),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

            // ── Right Column (40%): Sprint Metrics + Recent Notes + Subjects ─────
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Academic Sprint Metrics (4 Stat Cards)
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = LernspaceSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Ringkasan Belajar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Card 1: Subjects
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LernspaceSurfaceLow,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Mata Kuliah", fontSize = 10.sp, color = LernspaceTextSecondary)
                                            Text("${subjects.size} Mapel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                        }
                                    }

                                    // Card 2: Syllabus Mastery
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LernspaceSurfaceLow,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Silabus Dikuasai", fontSize = 10.sp, color = LernspaceTextSecondary)
                                            Text("$masteredTopicsCount/${topics.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Card 3: Tasks Pending
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LernspaceSurfaceLow,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Tugas Pending", fontSize = 10.sp, color = LernspaceTextSecondary)
                                            Text("${pendingTasks.size} Tugas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                        }
                                    }

                                    // Card 4: Total Notes
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LernspaceSurfaceLow,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Total Catatan", fontSize = 10.sp, color = LernspaceTextSecondary)
                                            Text("${notes.size} Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Recent Notes from Workspace Desk
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = LernspaceSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Catatan Terbaru (Desk)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceDark
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Workspace.route) }
                                    ) {
                                        Text(
                                            text = "Ke Workspace",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = LernspaceDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                if (recentNotes.isEmpty()) {
                                    Text(
                                        text = "Belum ada catatan dibuat. Ketuk 'Catatan Cepat' untuk mulai mencoret di kanvas.",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        recentNotes.forEach { note ->
                                            val subj = subjects.find { it.id == note.subjectId }
                                            val noteColor = subj?.color ?: if (note.isScratchpad) Color(0xFF64748B) else AccentBlue

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = LernspaceSurfaceLow,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { AcademicWorkspaceState.openNoteEditor(note) }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LernspaceDark))
                                                            Text(
                                                                text = note.subject,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = LernspaceDark
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = note.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = LernspaceDark,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = note.displaySnippet,
                                                            fontSize = 10.sp,
                                                            color = LernspaceTextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "Buka",
                                                        tint = LernspaceTextSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Enrolled Subjects Quick Jump to Syllabus
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = LernspaceSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mata Kuliah Terdaftar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceDark
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Learning.route) }
                                    ) {
                                        Text(
                                            text = "Ke Silabus",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = LernspaceDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                if (subjects.isEmpty()) {
                                    Text(
                                        text = "Belum ada mata kuliah terdaftar. Buka Silabus untuk menambahkan.",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        subjects.forEach { subject ->
                                            val count = topics.count { it.subjectId == subject.id }
                                            val mastered = topics.count { it.subjectId == subject.id && (it.status == TopicStatus.COMFORTABLE || it.status == TopicStatus.MASTERED) }
                                            val pct = if (count > 0) (mastered.toFloat() / count * 100).toInt() else 0

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = LernspaceSurfaceLow,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onNavigateToScreen(Screen.Learning.route) }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subject.color))
                                                        Text(
                                                            text = subject.name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = LernspaceDark
                                                        )
                                                    }

                                                    Text(
                                                        text = "$pct% ($mastered/$count bab)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (pct >= 50) Color(0xFF16A34A) else LernspaceTextSecondary
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

    // ── Dialog: Input Tugas & Set Deadline ────────────────────────────────────
    if (showAddTaskDialog) {
        InputTaskDialog(
            subjects = subjects,
            topics = topics,
            initialDayNumber = 8,
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
}
