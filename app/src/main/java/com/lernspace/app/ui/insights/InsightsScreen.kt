package com.lernspace.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.theme.*

@Composable
fun InsightsScreen(
    onNavigateToSyllabus: ((subjectId: String?) -> Unit)? = null,
    onNavigateToPlanner: (() -> Unit)? = null,
    onNavigateToWorkspace: (() -> Unit)? = null
) {
    val subjects = AcademicWorkspaceState.subjects
    val topics = AcademicWorkspaceState.topics
    val tasks = AcademicWorkspaceState.tasks
    val notes = AcademicWorkspaceState.notes
    val weeklyClasses = AcademicWorkspaceState.weeklyClasses

    // Live metrics directly observed from SnapshotStateList
    val totalTopics = topics.size
    val masteredTopics = topics.count { it.status == TopicStatus.MASTERED || it.status == TopicStatus.COMFORTABLE }
    val masteryRate = if (totalTopics > 0) (masteredTopics.toFloat() / totalTopics) else 0f

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val taskRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks) else 0f

    val totalWeeklyHours = (weeklyClasses.size * 1.75f) + (AcademicWorkspaceState.totalFocusStudyMinutes.toFloat() / 60f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Top Header Bar ────────────────────────────────────────────────
        item {
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
                        Text(
                            text = "Insights & Analytics",
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
                                text = "Semester Ganjil 2026/2027",
                                style = MaterialTheme.typography.labelSmall,
                                color = LernspaceDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Live Sync Status Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LernspaceSurfaceLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LernspaceDark)
                        )
                        Text(
                            text = "Local-First Live Sync",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LernspaceDark
                        )
                    }
                }
            }
        }

        // ── 2. Top KPI Metric Cards (Row of 4) ──────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KpiCard(
                    title = "Beban Studi Mingguan",
                    value = String.format(java.util.Locale.US, "%.1f", totalWeeklyHours) + " Jam",
                    subtitle = "${weeklyClasses.size} Kelas + ${String.format(java.util.Locale.US, "%.1f", AcademicWorkspaceState.totalFocusStudyMinutes.toFloat() / 60f)} Jam Focus Session",
                    icon = Icons.Filled.School,
                    accentColor = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Penguasaan Silabus",
                    value = "${(masteryRate * 100).toInt()}%",
                    subtitle = "$masteredTopics dari $totalTopics Topik Dikuasai",
                    icon = Icons.Filled.TrendingUp,
                    accentColor = LernspaceDark,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Penyelesaian Tugas",
                    value = "$completedTasks / $totalTasks",
                    subtitle = "${totalTasks - completedTasks} Tugas aktif menunggu deadline",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = Color(0xFF475569),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Catatan & Canvas",
                    value = "${notes.size} Dokumen",
                    subtitle = "${notes.count { it.isScratchpad }} Scratchpad • ${notes.count { !it.isScratchpad }} Modul Kuliah",
                    icon = Icons.Filled.EditNote,
                    accentColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── 3. Main Landscape Two-Pane Analytics Cockpit ──────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── LEFT PANE (58%): Charts & Subject Mastery Progression ─────
                Column(
                    modifier = Modifier.weight(0.58f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Panel 1: Multi-Segment Syllabus Mastery Breakdown
                    SyllabusMasteryOverviewCard(
                        topics = topics,
                        subjects = subjects,
                        onNavigateToSyllabus = onNavigateToSyllabus
                    )

                    // Panel 2: Weekly Study Distribution by Subject
                    WeeklyStudyDistributionCard(
                        subjects = subjects,
                        weeklyClasses = weeklyClasses
                    )
                }

                // ── RIGHT PANE (42%): Smart Advisory, Deadlines & Notes ────────
                Column(
                    modifier = Modifier.weight(0.42f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Panel 3: Cognitive Health & Smart Advisory
                    SmartAdvisoryCard(
                        topics = topics,
                        tasks = tasks,
                        onNavigateToPlanner = onNavigateToPlanner
                    )

                    // Panel 4: Urgent Deadlines & Exam Readiness
                    UpcomingDeadlinesCard(
                        tasks = tasks,
                        onNavigateToPlanner = onNavigateToPlanner
                    )

                    // Panel 5: Quick Notes & Study Desk Link
                    RecentNotesActivityCard(
                        notes = notes,
                        onNavigateToWorkspace = onNavigateToWorkspace
                    )
                }
            }
        }
    }
}

// ── Components ───────────────────────────────────────────────────────────────

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = LernspaceTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = LernspaceDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = LernspaceTextSecondary
            )
        }
    }
}

@Composable
fun SyllabusMasteryOverviewCard(
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    onNavigateToSyllabus: ((String?) -> Unit)?
) {
    val totalTopics = topics.size
    val mastered = topics.count { it.status == TopicStatus.MASTERED }
    val comfortable = topics.count { it.status == TopicStatus.COMFORTABLE }
    val learning = topics.count { it.status == TopicStatus.LEARNING }
    val needsReview = topics.count { it.status == TopicStatus.NEEDS_REVIEW }
    val notStarted = topics.count { it.status == TopicStatus.NOT_STARTED }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pemetaan Penguasaan Silabus (Topic Mastery)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "Distribusi tingkat pemahaman materi di seluruh mata kuliah",
                        style = MaterialTheme.typography.labelMedium,
                        color = LernspaceTextSecondary
                    )
                }
                TextButton(onClick = { onNavigateToSyllabus?.invoke(null) }) {
                    Text("Buka Silabus Lengkap", fontSize = 12.sp, color = LernspaceDark, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = LernspaceDark)
                }
            }

            // Stacked Multi-Color Segment Bar
            if (totalTopics > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(LernspaceSurfaceLow)
                ) {
                    if (mastered > 0) {
                        Box(
                            modifier = Modifier
                                .weight(mastered.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF7C3AED))
                        )
                    }
                    if (comfortable > 0) {
                        Box(
                            modifier = Modifier
                                .weight(comfortable.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF059669))
                        )
                    }
                    if (learning > 0) {
                        Box(
                            modifier = Modifier
                                .weight(learning.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF1D4ED8))
                        )
                    }
                    if (needsReview > 0) {
                        Box(
                            modifier = Modifier
                                .weight(needsReview.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF64748B))
                        )
                    }
                    if (notStarted > 0) {
                        Box(
                            modifier = Modifier
                                .weight(notStarted.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFCBD5E1))
                        )
                    }
                }
            }

            // Legend Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusLegendChip("Mastered", mastered, Color(0xFF7C3AED), Modifier.weight(1f))
                StatusLegendChip("Comfortable", comfortable, Color(0xFF059669), Modifier.weight(1f))
                StatusLegendChip("Learning", learning, Color(0xFF1D4ED8), Modifier.weight(1f))
                StatusLegendChip("Review", needsReview, Color(0xFF64748B), Modifier.weight(1f))
                StatusLegendChip("Pending", notStarted, Color(0xFF94A3B8), Modifier.weight(1f))
            }

            HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f))

            // Subject Breakdown Rows
            Text(
                text = "Penguasaan per Mata Kuliah:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subjects.forEach { subject ->
                    val subjectTopics = topics.filter { it.subjectId == subject.id }
                    val masteredSubTopics = subjectTopics.count { it.status == TopicStatus.MASTERED || it.status == TopicStatus.COMFORTABLE }
                    val progress = if (subjectTopics.isNotEmpty()) masteredSubTopics.toFloat() / subjectTopics.size else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToSyllabus?.invoke(subject.id) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(subject.color)
                        )
                        Column(modifier = Modifier.weight(0.35f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LernspaceDark
                            )
                            Text(
                                text = subject.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = LernspaceTextSecondary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(0.45f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = subject.color,
                            trackColor = LernspaceSurfaceLow
                        )
                        Text(
                            text = "$masteredSubTopics/${subjectTopics.size} Topik",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark,
                            modifier = Modifier.weight(0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusLegendChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LernspaceSurfaceLow,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall,
                color = LernspaceDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WeeklyStudyDistributionCard(
    subjects: List<com.lernspace.app.ui.learning.SubjectUi>,
    weeklyClasses: List<com.lernspace.app.data.repository.WeeklyClassUi>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Distribusi Alokasi Jam Studi (Workload Distribution)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "Total jam tatap muka kuliah & estimasi porsi belajar per minggu",
                        style = MaterialTheme.typography.labelMedium,
                        color = LernspaceTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LernspaceSurfaceLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Text(
                        text = "Beban Seimbang",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceDark,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Bar visualizer
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                subjects.forEach { subject ->
                    val classCount = weeklyClasses.count { it.subjectId == subject.id }
                    val estimatedHours = if (classCount > 0) classCount * 1.75f + 1.5f else 2.0f
                    val progress = (estimatedHours / 8.0f).coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(subject.color)
                                )
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = LernspaceDark
                                )
                            }
                            Text(
                                text = String.format("%.1f Jam / Minggu", estimatedHours),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = subject.color,
                            trackColor = LernspaceSurfaceLow
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartAdvisoryCard(
    topics: List<com.lernspace.app.ui.learning.TopicUi>,
    tasks: List<com.lernspace.app.data.repository.TaskUiModel>,
    onNavigateToPlanner: (() -> Unit)?
) {
    val needsReviewTopic = topics.firstOrNull { it.status == TopicStatus.NEEDS_REVIEW }
    val urgentTask = tasks.firstOrNull { it.priority == "HIGH" && !it.isCompleted }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(
                        text = "Smart Study Advisory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "Rekomendasi fokus berbasis deadline & silabus",
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LernspaceSurfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TipsAndUpdates,
                            contentDescription = null,
                            tint = LernspaceDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Rekomendasi Fokus Belajar:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark
                        )
                    }
                    Text(
                        text = if (needsReviewTopic != null) {
                            "Materi '${needsReviewTopic.title}' masih berstatus Needs Review. Sisihkan 45 menit sebelum ujian/kuis terdekat."
                        } else {
                            "Semua topik berada dalam status penguasaan baik. Fokus pada penyelesaian problem set minggu ini."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LernspaceDark,
                        lineHeight = 18.sp
                    )
                }
            }

            Button(
                onClick = { onNavigateToPlanner?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Planner & Jadwalkan Sesi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun UpcomingDeadlinesCard(
    tasks: List<com.lernspace.app.data.repository.TaskUiModel>,
    onNavigateToPlanner: (() -> Unit)?
) {
    val pendingTasks = tasks.filter { !it.isCompleted }.take(3)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deadline Kritis Mendatang",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark
                )
                TextButton(onClick = { onNavigateToPlanner?.invoke() }, contentPadding = PaddingValues(0.dp)) {
                    Text("Semua (${tasks.count { !it.isCompleted }})", fontSize = 11.sp, color = LernspaceDark)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingTasks.forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LernspaceBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPlanner?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (task.priority) {
                                            "HIGH" -> AccentRed
                                            "MEDIUM" -> Color(0xFF475569)
                                            else -> LernspaceTextSecondary
                                        }
                                    )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${task.subjectName} • ${task.dueDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LernspaceTextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (task.priority == "HIGH") AccentRed.copy(alpha = 0.12f) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = task.priority,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.priority == "HIGH") AccentRed else Color(0xFF475569),
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

@Composable
fun RecentNotesActivityCard(
    notes: List<com.lernspace.app.ui.workspace.model.NoteUiModel>,
    onNavigateToWorkspace: (() -> Unit)?
) {
    val recentNotes = notes.take(2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(14.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktivitas Catatan Terkini",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LernspaceDark
                )
                TextButton(onClick = { onNavigateToWorkspace?.invoke() }, contentPadding = PaddingValues(0.dp)) {
                    Text("Ke Workspace", fontSize = 11.sp, color = LernspaceDark)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentNotes.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWorkspace?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.EditNote, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LernspaceDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${note.subject} • ${note.date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
