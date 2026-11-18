package com.lernspace.app.ui.insights.components

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.core.supabase.BankSoalAttemptManager
import com.lernspace.app.data.repository.AcademicWorkspaceState
import com.lernspace.app.data.repository.FocusTimerPreset
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class RadarAxisItem(
    val id: String,
    val label: String,
    val score: Float, // 0.0f .. 1.0f
    val formattedScore: String,
    val subtitle: String,
    val associatedSubjectId: String? = null
)

enum class RadarMode(val title: String, val subtitle: String) {
    SUBJECTS("Peta Mata Kuliah", "Tingkat penguasaan tiap mata pelajaran"),
    COGNITIVE("6 Dimensi Kognitif", "Peta pilar kemampuan belajar kognitif")
}

@Composable
fun CompetencyRadarCockpitCard(
    subjects: List<SubjectUi>,
    topics: List<TopicUi>,
    totalWeeklyHours: Float,
    taskRate: Float,
    completedTasks: Int,
    totalTasks: Int,
    onNavigateToSyllabus: ((subjectId: String?) -> Unit)? = null,
    onNavigateToPlanner: (() -> Unit)? = null,
    onNavigateToBankSoal: (() -> Unit)? = null,
    onNavigateToFlashcards: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(RadarMode.SUBJECTS) }

    // ── Build Data for Subject Mode ──────────────────────────────────────────
    val subjectAxes = remember(subjects, topics, AcademicWorkspaceState.notes.size) {
        val effectiveSubjects = if (subjects.size >= 3) {
            subjects
        } else {
            val defaults = listOf(
                SubjectUi("matematika", "Matematika", "MTK"),
                SubjectUi("fisika", "Fisika", "FIS"),
                SubjectUi("biologi", "Biologi", "BIO"),
                SubjectUi("kimia", "Kimia", "KIM"),
                SubjectUi("informatika", "Informatika", "INF"),
                SubjectUi("bahasa", "B. Inggris", "ENG")
            )
            (subjects + defaults).distinctBy { it.name }.take(6)
        }

        effectiveSubjects.map { subject ->
            val subTopics = topics.filter { it.subjectId == subject.id }
            val mastered = subTopics.count { it.status == TopicStatus.MASTERED }
            val comfortable = subTopics.count { it.status == TopicStatus.COMFORTABLE }
            val learning = subTopics.count { it.status == TopicStatus.LEARNING }

            val rawScore = if (subTopics.isNotEmpty()) {
                (mastered * 1.0f + comfortable * 0.75f + learning * 0.40f) / subTopics.size
            } else {
                when (subject.name.lowercase()) {
                    "matematika" -> 0.88f
                    "fisika" -> 0.52f
                    "biologi" -> 0.78f
                    "kimia" -> 0.65f
                    "informatika" -> 0.90f
                    else -> 0.70f
                }
            }

            val notesCount = AcademicWorkspaceState.notes.count { it.subjectId == subject.id }
            val noteBonus = (notesCount * 0.04f).coerceAtMost(0.12f)
            val finalScore = (rawScore * 0.88f + noteBonus).coerceIn(0.20f, 1.0f)
            val pct = (finalScore * 100).toInt()

            RadarAxisItem(
                id = subject.id,
                label = subject.name,
                score = finalScore,
                formattedScore = "$pct%",
                subtitle = if (subTopics.isNotEmpty()) "$mastered/${subTopics.size} Topik Dikuasai" else "Syllabus Aktif",
                associatedSubjectId = subject.id
            )
        }
    }

    // ── Build Data for Cognitive Mode ────────────────────────────────────────
    val totalTopics = topics.size
    val masteredTopics = topics.count { it.status == TopicStatus.MASTERED || it.status == TopicStatus.COMFORTABLE }
    val conceptScore = if (totalTopics > 0) (masteredTopics.toFloat() / totalTopics) else 0.74f

    val totalBankAttempts = BankSoalAttemptManager.attempts.size
    val correctBankAttempts = BankSoalAttemptManager.attempts.values.count { it.isCorrect }
    val problemSolvingScore = if (totalBankAttempts > 0) {
        (correctBankAttempts.toFloat() / totalBankAttempts).coerceIn(0.25f, 1.0f)
    } else {
        0.78f
    }

    val memoryScore = (AcademicWorkspaceState.notes.size * 0.06f + 0.68f).coerceIn(0.40f, 0.96f)
    val disciplineScore = (totalWeeklyHours / 16f).coerceIn(0.30f, 1.0f)
    val taskExecutionScore = taskRate.coerceIn(0.30f, 1.0f)
    val notesSynthesisScore = (AcademicWorkspaceState.notes.size / 8f).coerceIn(0.35f, 1.0f)

    val cognitiveAxes = remember(
        conceptScore, problemSolvingScore, memoryScore, disciplineScore, taskExecutionScore, notesSynthesisScore,
        totalTopics, masteredTopics, totalBankAttempts, correctBankAttempts, totalWeeklyHours, completedTasks, totalTasks
    ) {
        listOf(
            RadarAxisItem("concept", "Konsep", conceptScore, "${(conceptScore * 100).toInt()}%", "$masteredTopics/$totalTopics Topik"),
            RadarAxisItem("problem_solving", "Problem Solving", problemSolvingScore, "${(problemSolvingScore * 100).toInt()}%", if (totalBankAttempts > 0) "$correctBankAttempts/$totalBankAttempts Soal Benar" else "Akurasi Latihan"),
            RadarAxisItem("memory", "Daya Ingat", memoryScore, "${(memoryScore * 100).toInt()}%", "Retensi Spaced Repetition"),
            RadarAxisItem("discipline", "Disiplin", disciplineScore, "${(disciplineScore * 100).toInt()}%", String.format(Locale.US, "%.1f Jam/Minggu", totalWeeklyHours)),
            RadarAxisItem("task_execution", "Tugas", taskExecutionScore, "${(taskExecutionScore * 100).toInt()}%", "$completedTasks/$totalTasks Tuntas"),
            RadarAxisItem("synthesis", "Catatan", notesSynthesisScore, "${(notesSynthesisScore * 100).toInt()}%", "${AcademicWorkspaceState.notes.size} Dokumen")
        )
    }

    val currentAxes = if (selectedMode == RadarMode.SUBJECTS) subjectAxes else cognitiveAxes
    var selectedAxisId by remember(selectedMode) { mutableStateOf(currentAxes.firstOrNull()?.id) }
    val activeSelectedItem = currentAxes.firstOrNull { it.id == selectedAxisId } ?: currentAxes.firstOrNull()

    // ── Diagnostic Calculations ──────────────────────────────────────────────
    val topStrength = remember(currentAxes) { currentAxes.maxByOrNull { it.score } ?: currentAxes[0] }
    val weakestArea = remember(currentAxes) { currentAxes.minByOrNull { it.score } ?: currentAxes[0] }

    val personaTitle = remember(currentAxes, topStrength) {
        val scoreDiff = (currentAxes.maxOf { it.score } - currentAxes.minOf { it.score })
        when {
            scoreDiff < 0.16f -> "The Balanced Polymath" to "Profil belajar seimbang di seluruh dimensi materi."
            topStrength.label.contains("Matematika", ignoreCase = true) || topStrength.label.contains("Problem", ignoreCase = true) ->
                "The Analytical Strategist" to "Pola berpikir analitis dan penalaran logis sangat dominan."
            topStrength.label.contains("Disiplin", ignoreCase = true) || topStrength.label.contains("Jam", ignoreCase = true) ->
                "The Relentless Achiever" to "Konsistensi tinggi dan alokasi jam studi adalah kekuatan utamamu."
            topStrength.label.contains("Konsep", ignoreCase = true) || topStrength.label.contains("Sains", ignoreCase = true) ->
                "The Concept Architect" to "Pemahaman teori dan struktur konsep sangat kokoh."
            else -> "The Agile Scholar" to "Cepat beradaptasi dan mencerna konsep materi baru."
        }
    }

    Surface(
        modifier = modifier
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
            // ── Header & Minimal Mode Switcher ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Radar Kompetensi & Analisis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = selectedMode.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = LernspaceTextSecondary
                    )
                }

                // Minimalist Pill Tabs
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MinimalTabPill(
                            title = "Mata Kuliah",
                            isSelected = selectedMode == RadarMode.SUBJECTS,
                            onClick = { selectedMode = RadarMode.SUBJECTS }
                        )
                        MinimalTabPill(
                            title = "Dimensi Kognitif",
                            isSelected = selectedMode == RadarMode.COGNITIVE,
                            onClick = { selectedMode = RadarMode.COGNITIVE }
                        )
                    }
                }
            }

            // ── Cockpit Split Layout (Chart + Analysis) ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Left: Minimal Spider Radar Canvas ────────────────────────
                Box(
                    modifier = Modifier
                        .weight(0.48f)
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpiderRadarCanvas(
                        items = currentAxes,
                        selectedItem = activeSelectedItem,
                        onSelectItem = { selectedAxisId = it.id },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── Right: Minimalist Diagnostics & Action Panel ─────────────
                Column(
                    modifier = Modifier.weight(0.52f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Persona Row
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(LernspaceDark.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Insights,
                                    contentDescription = null,
                                    tint = LernspaceDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = personaTitle.first,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = personaTitle.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }
                    }

                    // Strength & Weakness Minimal Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Strength Tile
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = LernspaceDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Kekuatan Utama",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceTextSecondary
                                    )
                                }
                                Text(
                                    text = "${topStrength.label} · ${topStrength.formattedScore}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = topStrength.subtitle,
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }

                        // Growth Area Tile
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TrackChanges,
                                        contentDescription = null,
                                        tint = LernspaceDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Perlu Ditingkatkan",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LernspaceTextSecondary
                                    )
                                }
                                Text(
                                    text = "${weakestArea.label} · ${weakestArea.formattedScore}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceDark
                                )
                                Text(
                                    text = weakestArea.subtitle,
                                    fontSize = 11.sp,
                                    color = LernspaceTextSecondary
                                )
                            }
                        }
                    }

                    // Action Buttons (Clean Monochrome)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (onNavigateToBankSoal != null) {
                                    onNavigateToBankSoal()
                                } else {
                                    Toast.makeText(context, "Membuka Bank Soal...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LernspaceDark,
                                contentColor = LernspaceSurfaceCard
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FolderCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Latihan Soal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (onNavigateToSyllabus != null) {
                                    onNavigateToSyllabus(weakestArea.associatedSubjectId)
                                } else {
                                    Toast.makeText(context, "Membuka Silabus...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LernspaceBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LernspaceDark),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = LernspaceDark)
                            Spacer(Modifier.width(6.dp))
                            Text("Buka Silabus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalButton(
                            onClick = {
                                AcademicWorkspaceState.startFocusSession(
                                    subject = subjects.firstOrNull { it.id == weakestArea.associatedSubjectId } ?: subjects.firstOrNull(),
                                    topic = topics.firstOrNull { it.subjectId == weakestArea.associatedSubjectId },
                                    preset = FocusTimerPreset.POMODORO_25
                                )
                                Toast.makeText(context, "Sesi Fokus 25m Dimulai (${weakestArea.label})", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LernspaceSurfaceLow,
                                contentColor = LernspaceDark
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = LernspaceDark)
                            Spacer(Modifier.width(6.dp))
                            Text("Fokus 25m", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Minimalist Radar Canvas Implementation ───────────────────────────────────

@Composable
fun SpiderRadarCanvas(
    items: List<RadarAxisItem>,
    selectedItem: RadarAxisItem?,
    onSelectItem: (RadarAxisItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val defaultTextColor = LernspaceDark
    val defaultBorderColor = LernspaceBorder
    val defaultSurfaceCard = LernspaceSurfaceCard
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val numAxes = items.size.coerceAtLeast(3)
    val ringLevels = listOf(0.25f, 0.50f, 0.75f, 1.0f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(items) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = minOf(cx, cy) - 38.dp.toPx()
                    val curProgress = animProgress.value

                    var closestItem: RadarAxisItem? = null
                    var minDist = Float.MAX_VALUE

                    items.forEachIndexed { i, item ->
                        val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
                        val r = maxR * item.score * curProgress
                        val vx = cx + r * cos(angle)
                        val vy = cy + r * sin(angle)
                        val d = hypot(tapOffset.x - vx, tapOffset.y - vy)
                        if (d < minDist && d < 44.dp.toPx()) {
                            minDist = d
                            closestItem = item
                        }
                    }
                    closestItem?.let { onSelectItem(it) }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy) - 38.dp.toPx()
        val curProgress = animProgress.value

        // 1. Draw Clean Subtle Web Rings
        ringLevels.forEach { lvl ->
            val ringPath = Path()
            val ringR = maxR * lvl

            for (i in 0 until numAxes) {
                val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
                val x = cx + ringR * cos(angle)
                val y = cy + ringR * sin(angle)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()

            drawPath(
                path = ringPath,
                color = defaultBorderColor.copy(alpha = if (lvl == 1.0f) 0.8f else 0.4f),
                style = Stroke(width = if (lvl == 1.0f) 1.2.dp.toPx() else 0.8.dp.toPx())
            )
        }

        // 2. Draw Subtle Radial Spokes
        for (i in 0 until numAxes) {
            val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
            val endX = cx + maxR * cos(angle)
            val endY = cy + maxR * sin(angle)
            drawLine(
                color = defaultBorderColor.copy(alpha = 0.5f),
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = 0.8.dp.toPx()
            )
        }

        // 3. Build Minimal Data Polygon
        val dataPath = Path()
        val vertexPoints = mutableListOf<Offset>()

        items.forEachIndexed { i, item ->
            val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
            val r = maxR * (item.score * curProgress).coerceIn(0.08f, 1.0f)
            val vx = cx + r * cos(angle)
            val vy = cy + r * sin(angle)
            vertexPoints.add(Offset(vx, vy))

            if (i == 0) dataPath.moveTo(vx, vy) else dataPath.lineTo(vx, vy)
        }
        dataPath.close()

        // Clean subtle fill (Dark tone at 9% opacity, matches theme perfectly)
        drawPath(
            path = dataPath,
            color = defaultTextColor.copy(alpha = 0.09f)
        )

        // Outline (1.8dp Stroke)
        drawPath(
            path = dataPath,
            color = defaultTextColor.copy(alpha = 0.85f),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // 4. Draw Vertex Nodes & Clean Outer Labels
        items.forEachIndexed { i, item ->
            val pt = vertexPoints[i]
            val isSelected = item.id == selectedItem?.id

            // Selected vertex ring
            if (isSelected) {
                drawCircle(
                    color = defaultTextColor.copy(alpha = 0.15f),
                    radius = 9.dp.toPx(),
                    center = pt
                )
            }

            // Dot
            drawCircle(
                color = defaultTextColor,
                radius = if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = defaultSurfaceCard,
                radius = if (isSelected) 2.2.dp.toPx() else 1.6.dp.toPx(),
                center = pt
            )

            // Outer Axis Label (Clean single-line: "Mapel · 85%")
            val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
            val labelDistance = maxR + 20.dp.toPx()
            val lx = cx + labelDistance * cos(angle)
            val ly = cy + labelDistance * sin(angle)

            val textLayout = textMeasurer.measure(
                text = AnnotatedString("${item.label} · ${item.formattedScore}"),
                style = TextStyle(
                    fontSize = if (isSelected) 11.sp else 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) defaultTextColor else defaultTextColor.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    lx - textLayout.size.width / 2f,
                    ly - textLayout.size.height / 2f
                )
            )
        }
    }
}

// ── Minimalist Tab Pill ──────────────────────────────────────────────────────

@Composable
fun MinimalTabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) LernspaceDark else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) LernspaceSurfaceCard else LernspaceTextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
