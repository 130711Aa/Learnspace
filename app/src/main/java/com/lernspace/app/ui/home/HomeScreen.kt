package com.lernspace.app.ui.home

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
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.planner.InputTaskDialog
import com.lernspace.app.ui.theme.*
import com.lernspace.app.ui.workspace.NoteEditorScreen
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.util.UUID

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

    var activeNote by remember { mutableStateOf<NoteUiModel?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // If active note is opened, display fullscreen NoteEditorScreen!
    if (activeNote != null) {
        NoteEditorScreen(
            note = activeNote!!,
            onBack = { activeNote = null }
        )
        return
    }

    // Computed values
    val mondayClasses = weeklyClasses.filter { it.dayOfWeek == 1 }.sortedBy { it.startTime }
    val pendingTasks = tasks.filter { !it.isCompleted }.sortedBy { it.dueDayNumber }
    val masteredTopicsCount = topics.count { it.status == TopicStatus.COMFORTABLE || it.status == TopicStatus.MASTERED }
    val recentNotes = notes.take(4)

    // Next Up hero schedule item
    val nextUpClass = mondayClasses.firstOrNull()
    val nextUpTopic = topics.find { it.subjectId == nextUpClass?.subjectId } ?: topics.firstOrNull()

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
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentTeal)
                    )
                    Text(
                        text = "ACADEMIC HORIZON · DAILY COMMAND CENTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Today's Command Center",
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
                            text = "Senin, 8 Nov 2026",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
                        activeNote = newScratchpad
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Draw, null, modifier = Modifier.size(16.dp), tint = AccentBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catatan Cepat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                }

                // Input Tugas button
                Button(
                    onClick = { showAddTaskDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
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
                    // 1. Hero Card: Next Up Session
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
                                        .background(AccentTeal)
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
                                            color = AccentBlueContainer
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
                                                    tint = AccentBlue
                                                )
                                                Text(
                                                    text = "${nextUpClass?.startTime ?: "08:30"} · UPCOMING CLASS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentBlue
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = AccentTealContainer
                                        ) {
                                            Text(
                                                text = (nextUpClass?.subjectName ?: "NEUROBIOLOGY").uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentTeal,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = nextUpTopic?.title ?: "Synaptic Plasticity & LTP Review",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = nextUpTopic?.description ?: "Persiapan review materi kuliah dan catatan sub-bab silabus.",
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
                                                        activeNote = existing
                                                    } else {
                                                        val subj = subjects.find { it.id == nextUpTopic.subjectId } ?: subjects.first()
                                                        activeNote = AcademicWorkspaceState.createNoteForTopic(subj, nextUpTopic)
                                                    }
                                                } else {
                                                    activeNote = AcademicWorkspaceState.createScratchpad()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Filled.Draw, null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buka Catatan Canvas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // View in Syllabus
                                        OutlinedButton(
                                            onClick = { onNavigateToScreen(Screen.Learning.route) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Filled.AutoStories, null, modifier = Modifier.size(15.dp), tint = LernspaceDark)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Lihat di Silabus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LernspaceDark)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Section: Jadwal Kuliah Hari Ini (Monday)
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
                                            text = "Jadwal Kuliah Hari Ini (Senin)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LernspaceDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = "${mondayClasses.size} Kelas",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Buka Jadwal Penuh ->",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Planner.route) }
                                    )
                                }

                                if (mondayClasses.isEmpty()) {
                                    Text(
                                        text = "Tidak ada jadwal kuliah hari ini.",
                                        fontSize = 12.sp,
                                        color = LernspaceTextSecondary
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        mondayClasses.forEach { c ->
                                            val subj = subjects.find { it.id == c.subjectId }
                                            val subColor = subj?.color ?: AccentBlue

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = LernspaceSurfaceLow,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder),
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
                                                                .background(subColor)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = "${c.subjectName} (${c.subjectCode})",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = LernspaceDark
                                                            )
                                                            Text(
                                                                text = "${c.startTime} - ${c.endTime} • Ruang: ${c.room ?: "TBA"}",
                                                                fontSize = 11.sp,
                                                                color = LernspaceTextSecondary
                                                            )
                                                        }
                                                    }

                                                    Surface(shape = RoundedCornerShape(4.dp), color = subColor.copy(alpha = 0.12f)) {
                                                        Text(
                                                            text = c.type,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = subColor,
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
                                            color = if (pendingTasks.isNotEmpty()) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = "${pendingTasks.size} Pending",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pendingTasks.isNotEmpty()) Color(0xFFB45309) else Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Buka Daftar Tugas ->",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Planner.route) }
                                    )
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
                                            val subColor = subj?.color ?: AccentAmber

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
                                    text = "Academic Sprint Snapshot",
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
                                            Text("$masteredTopicsCount/${topics.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
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
                                            Text("${pendingTasks.size} Tugas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (pendingTasks.isNotEmpty()) Color(0xFFD97706) else Color(0xFF16A34A))
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
                                            Text("${notes.size} Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
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
                                    Text(
                                        text = "Ke Workspace ->",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Workspace.route) }
                                    )
                                }

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
                                                .clickable { activeNote = note }
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
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(noteColor))
                                                        Text(
                                                            text = note.subject,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = noteColor
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
                                                        text = note.snippet,
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
                                    Text(
                                        text = "Ke Silabus ->",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        modifier = Modifier.clickable { onNavigateToScreen(Screen.Learning.route) }
                                    )
                                }

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

    // ── Dialog: Input Tugas & Set Deadline ────────────────────────────────────
    if (showAddTaskDialog) {
        InputTaskDialog(
            subjects = subjects,
            topics = topics,
            initialDayNumber = 8,
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
}
