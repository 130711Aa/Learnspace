package com.lernspace.app.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.AccentTeal
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Shared in-memory single source of truth for Academic Syllabus and Workspace Notes.
 * Connects Subjects, Syllabus Topics, and Interactive Note Canvas seamlessly.
 */
object AcademicWorkspaceState {

    /** Global active note being edited fullscreen across the entire tablet display */
    var activeEditingNote by mutableStateOf<NoteUiModel?>(null)

    fun openNoteEditor(note: NoteUiModel) {
        activeEditingNote = note
    }

    fun closeNoteEditor() {
        activeEditingNote = null
    }

    val subjects = mutableStateListOf(
        SubjectUi(
            id = "subj-1",
            name = "Neurobiology",
            code = "NEUR-401",
            description = "Cellular physiology, ion channels, and synaptic plasticity",
            color = AccentTeal
        ),
        SubjectUi(
            id = "subj-2",
            name = "Cognitive Psychology",
            code = "PSYC-320",
            description = "Attention networks, working memory, and language processing",
            color = AccentBlue
        ),
        SubjectUi(
            id = "subj-3",
            name = "Advanced Statistics",
            code = "STAT-210",
            description = "Bayesian inference, probability distributions, and MCMC methods",
            color = Color(0xFF8B5CF6)
        ),
        SubjectUi(
            id = "subj-4",
            name = "Organic Chemistry II",
            code = "CHEM-222",
            description = "Reaction mechanisms, aromatic synthesis, and spectroscopy",
            color = Color(0xFFF59E0B)
        )
    )

    val topics = mutableStateListOf(
        // Neurobiology topics
        TopicUi(
            id = "top-1",
            subjectId = "subj-1",
            title = "Resting Membrane Potential & Nernst Equation",
            description = "Potassium leak channels and Gibbs-Donnan equilibrium",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 12"
        ),
        TopicUi(
            id = "top-2",
            subjectId = "subj-1",
            title = "Action Potential Propagation & Voltage Gating",
            description = "Hodgkin-Huxley model, absolute and relative refractory periods",
            status = TopicStatus.MASTERED,
            targetDate = "Nov 14"
        ),
        TopicUi(
            id = "top-3",
            subjectId = "subj-1",
            title = "Synaptic Transmission & LTP Plasticity",
            description = "NMDA receptor magnesium block release and CaMKII cascade",
            status = TopicStatus.NEEDS_REVIEW,
            targetDate = "Nov 16"
        ),
        TopicUi(
            id = "top-4",
            subjectId = "subj-1",
            title = "Hippocampal Circuitry & Spatial Mapping",
            description = "Trisynaptic circuit (DG -> CA3 -> CA1) and grid cell function",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 20"
        ),
        TopicUi(
            id = "top-5",
            subjectId = "subj-1",
            title = "Glial Cell Regulation & Myelination",
            description = "Oligodendrocytes, astrocyte glutamate uptake, and tripartite synapse",
            status = TopicStatus.NOT_STARTED,
            targetDate = "Nov 25"
        ),

        // Cognitive Psychology topics
        TopicUi(
            id = "top-6",
            subjectId = "subj-2",
            title = "Baddeley's Multicomponent Working Memory",
            description = "Phonological loop, visuospatial sketchpad, and central executive",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 15"
        ),
        TopicUi(
            id = "top-7",
            subjectId = "subj-2",
            title = "Feature Integration Theory & Visual Search",
            description = "Treisman's preattentive feature map vs serial attentive binding",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 18"
        ),
        TopicUi(
            id = "top-8",
            subjectId = "subj-2",
            title = "Dual Process Theory of Reasoning",
            description = "System 1 fast heuristic vs System 2 slow deliberate cognition",
            status = TopicStatus.NOT_STARTED,
            targetDate = "Nov 22"
        ),

        // Advanced Statistics topics
        TopicUi(
            id = "top-9",
            subjectId = "subj-3",
            title = "Bayesian Prior Distributions & Conjugacy",
            description = "Beta-Binomial, Normal-Normal conjugate priors for posterior estimation",
            status = TopicStatus.NEEDS_REVIEW,
            targetDate = "Nov 14"
        ),
        TopicUi(
            id = "top-10",
            subjectId = "subj-3",
            title = "Markov Chain Monte Carlo & Metropolis-Hastings",
            description = "Transition kernel, acceptance probability, and stationary distribution",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 19"
        ),
        TopicUi(
            id = "top-11",
            subjectId = "subj-3",
            title = "Maximum Likelihood vs MAP Estimators",
            description = "Log-likelihood maximization, Newton-Raphson, and regularized priors",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 23"
        ),

        // Organic Chemistry topics
        TopicUi(
            id = "top-12",
            subjectId = "subj-4",
            title = "Electrophilic Aromatic Substitution (EAS)",
            description = "Arenium ion intermediate, activating vs deactivating ortho/para directors",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 13"
        ),
        TopicUi(
            id = "top-13",
            subjectId = "subj-4",
            title = "Aldol Condensation & Claisen Reactions",
            description = "Enolate ion nucleophilic attack on carbonyl carbons",
            status = TopicStatus.NEEDS_REVIEW,
            targetDate = "Nov 17"
        ),
        TopicUi(
            id = "top-14",
            subjectId = "subj-4",
            title = "1H & 13C NMR Spectroscopy Interpretation",
            description = "Chemical shift, spin-spin splitting multiplicity, and integration",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 21"
        )
    )

    val notes = mutableStateListOf(
        NoteUiModel(
            id = "note-1",
            title = "LTP Induction & NMDA Receptors",
            subject = "Neurobiology",
            date = "Today, 10:15 AM",
            snippet = "Long-term potentiation requires high-frequency stimulation of Schaffer collaterals to relieve magnesium block...",
            subjectId = "subj-1",
            topicId = "top-3",
            topicName = "Synaptic Transmission & LTP Plasticity"
        ),
        NoteUiModel(
            id = "note-2",
            title = "Working Memory & Executive Control",
            subject = "Cognitive Psychology",
            date = "Oct 19",
            snippet = "Baddeley's multicomponent model includes the phonological loop, visuospatial sketchpad, and central executive...",
            subjectId = "subj-2",
            topicId = "top-6",
            topicName = "Baddeley's Multicomponent Working Memory"
        ),
        NoteUiModel(
            id = "note-3",
            title = "Bayesian Prior Distributions Summary",
            subject = "Advanced Statistics",
            date = "Yesterday",
            snippet = "Conjugate priors simplify posterior density calculation when sampling from exponential family distributions...",
            subjectId = "subj-3",
            topicId = "top-9",
            topicName = "Bayesian Prior Distributions & Conjugacy"
        ),
        NoteUiModel(
            id = "note-4",
            title = "Thesis Brainstorming & Literature Gap",
            subject = "Quick Scratchpad",
            date = "Just now",
            snippet = "Compare EEG power spectral density against fMRI BOLD signals in working memory load tasks...",
            isScratchpad = true
        )
    )

    // Helper queries
    fun getNotesForTopic(topicId: String): List<NoteUiModel> {
        return notes.filter { it.topicId == topicId }
    }

    fun getNotesForSubject(subjectId: String): List<NoteUiModel> {
        return notes.filter { it.subjectId == subjectId }
    }

    fun getScratchpads(): List<NoteUiModel> {
        return notes.filter { it.isScratchpad }
    }

    fun createNoteForTopic(subject: SubjectUi, topic: TopicUi): NoteUiModel {
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val newNote = NoteUiModel(
            id = UUID.randomUUID().toString(),
            title = topic.title,
            subject = subject.name,
            date = "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            snippet = "Start handwriting or typing lecture notes for ${topic.title}...",
            subjectId = subject.id,
            topicId = topic.id,
            topicName = topic.title,
            isScratchpad = false
        )
        notes.add(0, newNote)
        return newNote
    }

    fun createScratchpad(title: String = "Untitled Scratchpad"): NoteUiModel {
        val newNote = NoteUiModel(
            id = UUID.randomUUID().toString(),
            title = title,
            subject = "Quick Scratchpad",
            date = "Just now",
            snippet = "Freeform scratchpad for quick formulas, thoughts, or whiteboard sketch...",
            isScratchpad = true
        )
        notes.add(0, newNote)
        return newNote
    }

    fun updateTopicStatus(topicId: String, newStatus: TopicStatus) {
        val idx = topics.indexOfFirst { it.id == topicId }
        if (idx != -1) {
            topics[idx] = topics[idx].copy(status = newStatus)
        }
    }

    // ── Planner & Calendar Schedule State ─────────────────────────────────────
    val scheduleItems = mutableStateListOf(
        PlannerItemUi(
            id = "sched-1",
            title = "Neurobiology: Action Potentials & Ion Channels",
            type = ScheduleType.LECTURE,
            startTime = "08:30",
            endTime = "10:00",
            dayOfWeek = 1,
            dayOffset = 0,
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            locationOrRoom = "BioSci Hall 201"
        ),
        PlannerItemUi(
            id = "sched-2",
            title = "Hybrid Review: Synaptic Transmission & LTP",
            type = ScheduleType.HYBRID_SUGGESTION,
            startTime = "10:30",
            endTime = "11:45",
            dayOfWeek = 1,
            dayOffset = 0,
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            topicId = "top-3",
            topicName = "Synaptic Transmission & LTP Plasticity",
            aiReason = "Deterministic suggestion: 75m open gap detected. Topic marked NEEDS_REVIEW prior to Wednesday's quiz."
        ),
        PlannerItemUi(
            id = "sched-3",
            title = "Advanced Statistics: MCMC Simulation Lab",
            type = ScheduleType.LAB,
            startTime = "13:00",
            endTime = "15:00",
            dayOfWeek = 1,
            dayOffset = 0,
            subjectId = "subj-3",
            subjectName = "Advanced Statistics",
            locationOrRoom = "Math & Data Lab 3"
        ),
        PlannerItemUi(
            id = "sched-4",
            title = "Cognitive Psych: Working Memory Model Recap",
            type = ScheduleType.STUDY_SESSION,
            startTime = "15:30",
            endTime = "16:45",
            dayOfWeek = 1,
            dayOffset = 0,
            subjectId = "subj-2",
            subjectName = "Cognitive Psychology",
            topicId = "top-6",
            topicName = "Baddeley's Multicomponent Working Memory",
            locationOrRoom = "Library 2F Quiet Zone"
        ),
        PlannerItemUi(
            id = "sched-5",
            title = "Organic Chemistry II: Aromatic Synthesis & EAS",
            type = ScheduleType.LECTURE,
            startTime = "09:00",
            endTime = "10:45",
            dayOfWeek = 2,
            dayOffset = 1,
            subjectId = "subj-4",
            subjectName = "Organic Chemistry II",
            locationOrRoom = "Chem Annex 105"
        ),
        PlannerItemUi(
            id = "sched-6",
            title = "Neurobiology Mid-Term Quiz (Chapters 1-3)",
            type = ScheduleType.EXAM,
            startTime = "10:00",
            endTime = "11:30",
            dayOfWeek = 3,
            dayOffset = 2,
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            locationOrRoom = "Auditorium B"
        ),
        PlannerItemUi(
            id = "sched-7",
            title = "Cognitive Psychology: Attention & Feature Integration",
            type = ScheduleType.LECTURE,
            startTime = "11:00",
            endTime = "12:30",
            dayOfWeek = 4,
            dayOffset = 3,
            subjectId = "subj-2",
            subjectName = "Cognitive Psychology",
            locationOrRoom = "Psychology Hall 12"
        ),
        PlannerItemUi(
            id = "sched-8",
            title = "Bayesian Inference Problem Set #4 Due",
            type = ScheduleType.ASSIGNMENT_DEADLINE,
            startTime = "17:00",
            endTime = "17:00",
            dayOfWeek = 5,
            dayOffset = 4,
            subjectId = "subj-3",
            subjectName = "Advanced Statistics"
        )
    )

    fun getItemsForDay(dayOffset: Int): List<PlannerItemUi> {
        return scheduleItems.filter { it.dayOffset == dayOffset }.sortedBy { it.startTime }
    }

    fun getItemsForWeekday(dayOfWeek: Int): List<PlannerItemUi> {
        return scheduleItems.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
    }

    // ── Fixed Weekly Class Timetable (Jadwal Mapel Mingguan) ─────────────────
    val weeklyClasses = mutableStateListOf(
        WeeklyClassUi(
            id = "wclass-1",
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            subjectCode = "NEUR-401",
            dayOfWeek = 1, // Monday
            startTime = "08:30",
            endTime = "10:00",
            room = "BioSci Hall 201",
            lecturer = "Dr. Aris Thorne",
            type = "Lecture"
        ),
        WeeklyClassUi(
            id = "wclass-2",
            subjectId = "subj-3",
            subjectName = "Advanced Statistics",
            subjectCode = "STAT-210",
            dayOfWeek = 1, // Monday
            startTime = "13:00",
            endTime = "15:00",
            room = "Math & Data Lab 3",
            lecturer = "Prof. Elena Rostova",
            type = "Lab"
        ),
        WeeklyClassUi(
            id = "wclass-3",
            subjectId = "subj-4",
            subjectName = "Organic Chemistry II",
            subjectCode = "CHEM-222",
            dayOfWeek = 2, // Tuesday
            startTime = "09:00",
            endTime = "10:45",
            room = "Chem Annex 105",
            lecturer = "Dr. Marcus Vance",
            type = "Lecture"
        ),
        WeeklyClassUi(
            id = "wclass-4",
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            subjectCode = "NEUR-401",
            dayOfWeek = 3, // Wednesday
            startTime = "10:00",
            endTime = "12:00",
            room = "Neurophysiology Lab 101",
            lecturer = "Dr. Aris Thorne",
            type = "Practicum"
        ),
        WeeklyClassUi(
            id = "wclass-5",
            subjectId = "subj-2",
            subjectName = "Cognitive Psychology",
            subjectCode = "PSYC-320",
            dayOfWeek = 4, // Thursday
            startTime = "11:00",
            endTime = "12:30",
            room = "Psychology Hall 12",
            lecturer = "Prof. Sarah Jenkins",
            type = "Lecture"
        ),
        WeeklyClassUi(
            id = "wclass-6",
            subjectId = "subj-3",
            subjectName = "Advanced Statistics",
            subjectCode = "STAT-210",
            dayOfWeek = 5, // Friday
            startTime = "13:30",
            endTime = "15:00",
            room = "Science Bldg 204",
            lecturer = "Prof. Elena Rostova",
            type = "Discussion"
        )
    )

    // ── Student Tasks & Deadlines (Tugas Kuliah & Deadline) ───────────────────
    val tasks = mutableStateListOf(
        TaskUiModel(
            id = "task-1",
            title = "Problem Set #4: MCMC Sampling & Posterior Derivation",
            subjectId = "subj-3",
            subjectName = "Advanced Statistics",
            dueDate = "Nov 12, 2026",
            dueDayNumber = 12,
            dueTime = "17:00",
            priority = "HIGH",
            estimatedMinutes = 120,
            isCompleted = false,
            notes = "Include trace plots and convergence diagnostics."
        ),
        TaskUiModel(
            id = "task-2",
            title = "Neurobiology Mid-Term Quiz (Chapters 1-3)",
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            topicId = "top-3",
            topicName = "Synaptic Transmission & LTP Plasticity",
            dueDate = "Nov 10, 2026",
            dueDayNumber = 10,
            dueTime = "10:00",
            priority = "HIGH",
            estimatedMinutes = 90,
            isCompleted = false,
            notes = "Focus on Nernst equation calculations and magnesium block release."
        ),
        TaskUiModel(
            id = "task-3",
            title = "Literature Review: Working Memory Capacity vs Load",
            subjectId = "subj-2",
            subjectName = "Cognitive Psychology",
            topicId = "top-6",
            topicName = "Baddeley's Multicomponent Working Memory",
            dueDate = "Nov 16, 2026",
            dueDayNumber = 16,
            dueTime = "23:59",
            priority = "MEDIUM",
            estimatedMinutes = 180,
            isCompleted = false,
            notes = "Minimum 4 peer-reviewed citations from 2020-present."
        ),
        TaskUiModel(
            id = "task-4",
            title = "EAS Reaction Mechanisms & Synthetic Pathway Sheet",
            subjectId = "subj-4",
            subjectName = "Organic Chemistry II",
            dueDate = "Nov 20, 2026",
            dueDayNumber = 20,
            dueTime = "14:00",
            priority = "LOW",
            estimatedMinutes = 60,
            isCompleted = false,
            notes = "Draw all resonance contributor structures for meta-director attacks."
        ),
        TaskUiModel(
            id = "task-5",
            title = "Daily Review: Action Potential Nernst Calculations",
            subjectId = "subj-1",
            subjectName = "Neurobiology",
            dueDate = "Nov 8, 2026",
            dueDayNumber = 8,
            dueTime = "21:00",
            priority = "MEDIUM",
            estimatedMinutes = 45,
            isCompleted = false,
            notes = "Finish practice problem set 2 on paper canvas."
        )
    )

    fun getTasksForDayNumber(dayNumber: Int): List<TaskUiModel> {
        return tasks.filter { it.dueDayNumber == dayNumber }
    }

    fun getClassesForWeekday(dayOfWeek: Int): List<WeeklyClassUi> {
        return weeklyClasses.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
    }

    fun toggleTaskCompletion(taskId: String) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx != -1) {
            tasks[idx] = tasks[idx].copy(isCompleted = !tasks[idx].isCompleted)
        }
    }

    fun acceptHybridSuggestion(suggestionId: String) {
        val idx = scheduleItems.indexOfFirst { it.id == suggestionId }
        if (idx != -1) {
            val item = scheduleItems[idx]
            scheduleItems[idx] = item.copy(
                type = ScheduleType.STUDY_SESSION,
                title = item.title.replace("Hybrid Review:", "Study Session:")
            )
        }
    }
}

enum class ScheduleType(val label: String, val chipBg: Color, val chipFg: Color) {
    LECTURE("Lecture", Color(0xFFDBEAFE), Color(0xFF1D4ED8)),
    LAB("Lab Session", Color(0xFFEDE9FE), Color(0xFF7C3AED)),
    STUDY_SESSION("Self Study", Color(0xFFD1FAE5), Color(0xFF059669)),
    HYBRID_SUGGESTION("Hybrid Suggestion", Color(0xFFFEF3C7), Color(0xFFD97706)),
    EXAM("Exam / Quiz", Color(0xFFFFE4E6), Color(0xFFE11D48)),
    ASSIGNMENT_DEADLINE("Deadline", Color(0xFFF1F5F9), Color(0xFF475569))
}

data class PlannerItemUi(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ScheduleType,
    val startTime: String, // e.g. "08:30"
    val endTime: String,   // e.g. "10:00"
    val dayOfWeek: Int = 1, // 1 = Mon ... 7 = Sun
    val dayOffset: Int = 0, // 0 = Today, 1 = Tomorrow, etc.
    val subjectId: String? = null,
    val subjectName: String? = null,
    val topicId: String? = null,
    val topicName: String? = null,
    val locationOrRoom: String? = null,
    val isCompleted: Boolean = false,
    val aiReason: String? = null
)

data class WeeklyClassUi(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val dayOfWeek: Int, // 1 = Mon ... 7 = Sun
    val startTime: String, // "08:30"
    val endTime: String,   // "10:00"
    val room: String? = null,
    val lecturer: String? = null,
    val type: String = "Lecture" // Lecture, Lab, Practicum, Discussion
)

data class TaskUiModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val topicId: String? = null,
    val topicName: String? = null,
    val dueDate: String, // "Nov 12, 2026"
    val dueDayNumber: Int, // 12
    val dueTime: String = "23:59",
    val priority: String = "HIGH", // HIGH, MEDIUM, LOW
    val estimatedMinutes: Int = 90,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

