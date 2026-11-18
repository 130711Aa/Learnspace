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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    val color: Color = AccentBlue,
    val icon: ImageVector? = null,
    val associatedSubjectId: String? = null
)

enum class RadarMode(val title: String, val subtitle: String) {
    SUBJECTS("Peta Mata Kuliah", "Tingkat penguasaan per mata pelajaran"),
    COGNITIVE("6 Dimensi Kognitif", "Peta gaya & pilar kemampuan belajar")
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
            // Baseline academic subjects if user has fewer than 3 subjects
            val defaults = listOf(
                SubjectUi("matematika", "Matematika", "MTK", color = Color(0xFF2563EB)),
                SubjectUi("fisika", "Fisika", "FIS", color = Color(0xFF0284C7)),
                SubjectUi("biologi", "Biologi", "BIO", color = Color(0xFF059669)),
                SubjectUi("kimia", "Kimia", "KIM", color = Color(0xFFD97706)),
                SubjectUi("informatika", "Informatika", "INF", color = Color(0xFF8B5CF6)),
                SubjectUi("bahasa", "B. Inggris", "ENG", color = Color(0xFFEC4899))
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
                // Baseline score heuristic
                when (subject.name.lowercase()) {
                    "matematika" -> 0.88f
                    "fisika" -> 0.52f
                    "biologi" -> 0.78f
                    "kimia" -> 0.65f
                    "informatika" -> 0.92f
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
                color = subject.color,
                icon = Icons.Filled.School,
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
            RadarAxisItem(
                id = "concept",
                label = "Konsep",
                score = conceptScore,
                formattedScore = "${(conceptScore * 100).toInt()}%",
                subtitle = "$masteredTopics dari $totalTopics Topik Dikuasai",
                color = Color(0xFF2563EB),
                icon = Icons.Filled.MenuBook
            ),
            RadarAxisItem(
                id = "problem_solving",
                label = "Problem Solving",
                score = problemSolvingScore,
                formattedScore = "${(problemSolvingScore * 100).toInt()}%",
                subtitle = if (totalBankAttempts > 0) "$correctBankAttempts/$totalBankAttempts Soal Benar" else "Akurasi Kuis & Latihan Soal",
                color = Color(0xFF8B5CF6),
                icon = Icons.Filled.Psychology
            ),
            RadarAxisItem(
                id = "memory",
                label = "Daya Ingat",
                score = memoryScore,
                formattedScore = "${(memoryScore * 100).toInt()}%",
                subtitle = "Retensi Spaced Repetition",
                color = Color(0xFFEC4899),
                icon = Icons.Filled.Style
            ),
            RadarAxisItem(
                id = "discipline",
                label = "Disiplin Jam",
                score = disciplineScore,
                formattedScore = "${(disciplineScore * 100).toInt()}%",
                subtitle = String.format(Locale.US, "%.1f Jam / Minggu", totalWeeklyHours),
                color = Color(0xFF10B981),
                icon = Icons.Filled.Timer
            ),
            RadarAxisItem(
                id = "task_execution",
                label = "Eksekusi Tugas",
                score = taskExecutionScore,
                formattedScore = "${(taskExecutionScore * 100).toInt()}%",
                subtitle = "$completedTasks dari $totalTasks Tugas Tuntas",
                color = Color(0xFF06B6D4),
                icon = Icons.Filled.CheckCircle
            ),
            RadarAxisItem(
                id = "synthesis",
                label = "Sintesis Catatan",
                score = notesSynthesisScore,
                formattedScore = "${(notesSynthesisScore * 100).toInt()}%",
                subtitle = "${AcademicWorkspaceState.notes.size} Catatan & Modul",
                color = Color(0xFFF59E0B),
                icon = Icons.Filled.EditNote
            )
        )
    }

    val currentAxes = if (selectedMode == RadarMode.SUBJECTS) subjectAxes else cognitiveAxes
    var selectedAxisId by remember(selectedMode) { mutableStateOf(currentAxes.firstOrNull()?.id) }
    val activeSelectedItem = currentAxes.firstOrNull { it.id == selectedAxisId } ?: currentAxes.firstOrNull()

    // ── Diagnostic Calculations ──────────────────────────────────────────────
    val topStrength = remember(currentAxes) { currentAxes.maxByOrNull { it.score } ?: currentAxes[0] }
    val weakestArea = remember(currentAxes) { currentAxes.minByOrNull { it.score } ?: currentAxes[0] }

    // Cognitive Persona Title
    val cognitivePersona = remember(currentAxes, topStrength) {
        val scoreDiff = (currentAxes.maxOf { it.score } - currentAxes.minOf { it.score })
        when {
            scoreDiff < 0.16f -> PersonaInfo(
                title = "The Balanced Polymath",
                badge = "🌌 Multitalenta Seimbang",
                desc = "Profil kompetensimu merata di setiap sudut radar. Kamu memiliki keseimbangan prima antara teori, logika, dan konsistensi belajar."
            )
            topStrength.label.contains("Matematika", ignoreCase = true) || topStrength.label.contains("Problem", ignoreCase = true) -> PersonaInfo(
                title = "The Analytical Strategist",
                badge = "⚡ Pemikir Analitis",
                desc = "Kekuatan terbesarmu terletak pada penalaran logis, akurasi pemecahan masalah, dan kecepatan mencerna pola hitungan."
            )
            topStrength.label.contains("Disiplin", ignoreCase = true) || topStrength.label.contains("Jam", ignoreCase = true) -> PersonaInfo(
                title = "The Relentless Achiever",
                badge = "🛡️ Pejuang Konsisten",
                desc = "Ketahanan mental, streak belajar, dan alokasi waktu fokus mingguanmu adalah motor utama yang membawa kemajuan pesat."
            )
            topStrength.label.contains("Konsep", ignoreCase = true) || topStrength.label.contains("Sains", ignoreCase = true) -> PersonaInfo(
                title = "The Concept Architect",
                badge = "🧠 Penguasa Konsep",
                desc = "Kamu tidak hanya menghafal, tetapi memahami esensi dan struktur ilmu secara komprehensif hingga ke akar teorinya."
            )
            topStrength.label.contains("Daya Ingat", ignoreCase = true) || topStrength.label.contains("Flashcard", ignoreCase = true) -> PersonaInfo(
                title = "The Memory Prodigy",
                badge = "📚 Master Retensi",
                desc = "Daya simpan memori jangka panjangmu sangat tajam berkat pengulangan terjadwal (spaced repetition) yang teratur."
            )
            else -> PersonaInfo(
                title = "The Agile Explorer",
                badge = "🚀 Pembelajar Proaktif",
                desc = "Kamu adaptif dan cepat menangkap berbagai materi baru dengan dorongan eksplorasi yang tinggi."
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(16.dp)),
        color = LernspaceSurfaceCard,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Cockpit Header & Mode Switcher ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Radar Kompetensi & Analisis Bakat",
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
                    }
                }

                // Toggle Mode Switcher (Mapel vs Dimensi Kognitif)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RadarModeTabPill(
                            title = "Peta Mapel",
                            icon = Icons.Filled.School,
                            isSelected = selectedMode == RadarMode.SUBJECTS,
                            onClick = { selectedMode = RadarMode.SUBJECTS }
                        )
                        RadarModeTabPill(
                            title = "Dimensi Kognitif",
                            icon = Icons.Filled.Psychology,
                            isSelected = selectedMode == RadarMode.COGNITIVE,
                            onClick = { selectedMode = RadarMode.COGNITIVE }
                        )
                    }
                }
            }

            // ── Main Two-Column Cockpit Layout ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Left: Spider Radar Canvas (50% Width) ───────────────────
                Box(
                    modifier = Modifier
                        .weight(0.48f)
                        .height(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpiderRadarCanvas(
                        items = currentAxes,
                        selectedItem = activeSelectedItem,
                        onSelectItem = { selectedAxisId = it.id },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── Right: Mimo's Talent Diagnostic & Action Panel (50% Width)
                Column(
                    modifier = Modifier
                        .weight(0.52f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Persona Card Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = cognitivePersona.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF334155)
                                    ) {
                                        Text(
                                            text = cognitivePersona.badge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF7DD3FC),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = cognitivePersona.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Superpower & Blindspot Split Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Superpower Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.50f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
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
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Kekuatan Utama",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Text(
                                    text = "${topStrength.label} (${topStrength.formattedScore})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = "Unggul paling tinggi. ${topStrength.subtitle}. Jadikan bidang ini fondasi untuk materi lanjutan.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Growth Opportunity / Blindspot Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2).copy(alpha = 0.50f),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
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
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Area Pertumbuhan",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                                Text(
                                    text = "${weakestArea.label} (${weakestArea.formattedScore})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7F1D1D)
                                )
                                Text(
                                    text = "Paling perlu ditingkatkan. ${weakestArea.subtitle}. Perbanyak latihan soal & sesi review.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Actionable Quick Launch Buttons (Bukan cuma baca laporan!)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Rekomendasi Tindakan Nyata dari Mimo:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (onNavigateToBankSoal != null) {
                                        onNavigateToBankSoal()
                                    } else {
                                        Toast.makeText(context, "Membuka Bank Soal latihan...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FolderCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Latihan Soal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (onNavigateToSyllabus != null) {
                                        onNavigateToSyllabus(weakestArea.associatedSubjectId)
                                    } else {
                                        Toast.makeText(context, "Membuka Silabus materi...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LernspaceBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LernspaceDark),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp), tint = LernspaceDark)
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
                                    Toast.makeText(context, "Sesi Pomodoro 25 Menit dimulai! Fokus pada ${weakestArea.label}.", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF047857)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF047857))
                                Spacer(Modifier.width(6.dp))
                                Text("Fokus 25m", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Radar Canvas Implementation ──────────────────────────────────────────────

@Composable
fun SpiderRadarCanvas(
    items: List<RadarAxisItem>,
    selectedItem: RadarAxisItem?,
    onSelectItem: (RadarAxisItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val defaultTextColor = LernspaceDark
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    val numAxes = items.size.coerceAtLeast(3)
    val ringLevels = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(items) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = minOf(cx, cy) - 46.dp.toPx()
                    val curProgress = animProgress.value

                    var closestItem: RadarAxisItem? = null
                    var minDist = Float.MAX_VALUE

                    items.forEachIndexed { i, item ->
                        val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
                        val r = maxR * item.score * curProgress
                        val vx = cx + r * cos(angle)
                        val vy = cy + r * sin(angle)
                        val d = hypot(tapOffset.x - vx, tapOffset.y - vy)
                        if (d < minDist && d < 50.dp.toPx()) {
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
        val maxR = minOf(cx, cy) - 46.dp.toPx()
        val curProgress = animProgress.value

        // 1. Draw Concentric Web Polygon Rings (20%, 40%, 60%, 80%, 100%)
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

            val ringColor = if (lvl == 1.0f) Color(0xFF94A3B8).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.35f)
            val strokeW = if (lvl == 1.0f) 1.5.dp.toPx() else 1.0.dp.toPx()
            drawPath(path = ringPath, color = ringColor, style = Stroke(width = strokeW))

            // Percentage guideline label on the vertical top axis
            if (lvl in listOf(0.4f, 0.8f, 1.0f)) {
                val pctText = "${(lvl * 100).toInt()}%"
                val measure = textMeasurer.measure(
                    text = AnnotatedString(pctText),
                    style = TextStyle(fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                )
                drawText(
                    textLayoutResult = measure,
                    topLeft = Offset(cx + 4.dp.toPx(), cy - ringR - measure.size.height / 2f)
                )
            }
        }

        // 2. Draw Radial Spokes radiating from center to outer boundary
        for (i in 0 until numAxes) {
            val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
            val endX = cx + maxR * cos(angle)
            val endY = cy + maxR * sin(angle)
            drawLine(
                color = Color(0xFFCBD5E1).copy(alpha = 0.45f),
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 3. Build Filled Data Polygon
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

        // Draw Translucent Gradient Fill
        val fillBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2563EB).copy(alpha = 0.42f),
                Color(0xFF6366F1).copy(alpha = 0.28f),
                Color(0xFF8B5CF6).copy(alpha = 0.12f)
            ),
            center = Offset(cx, cy),
            radius = maxR
        )
        drawPath(path = dataPath, brush = fillBrush)

        // Draw Polygon Crisp Outline
        drawPath(
            path = dataPath,
            color = Color(0xFF2563EB),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 4. Draw Vertex Nodes & Outer Axis Labels
        items.forEachIndexed { i, item ->
            val pt = vertexPoints[i]
            val isSelected = item.id == selectedItem?.id

            // Glow / Pulse circle if selected
            if (isSelected) {
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = 0.25f),
                    radius = 12.dp.toPx(),
                    center = pt
                )
            }

            // Outer Vertex Dot
            drawCircle(
                color = if (isSelected) Color(0xFF2563EB) else item.color,
                radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                center = pt
            )
            // Inner White Center
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 3.dp.toPx() else 2.2.dp.toPx(),
                center = pt
            )

            // Outer Axis Label
            val angle = -PI.toFloat() / 2f + (i * 2f * PI.toFloat() / numAxes)
            val labelDistance = maxR + 24.dp.toPx()
            val lx = cx + labelDistance * cos(angle)
            val ly = cy + labelDistance * sin(angle)

            val labelText = AnnotatedString("${item.label}\n${item.formattedScore}")
            val textLayout = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    fontSize = if (isSelected) 11.sp else 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color(0xFF1D4ED8) else defaultTextColor,
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

// ── Helper Pills & Data Models ──────────────────────────────────────────────

@Composable
fun RadarModeTabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else LernspaceTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else LernspaceTextSecondary
            )
        }
    }
}

data class PersonaInfo(
    val title: String,
    val badge: String,
    val desc: String
)
