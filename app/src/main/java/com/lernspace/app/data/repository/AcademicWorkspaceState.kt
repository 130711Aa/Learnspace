package com.lernspace.app.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.lernspace.app.data.local.AppSettingsManager
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.AccentTeal
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle
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

    /** Global search overlay state */
    var isGlobalSearchOpen by mutableStateOf(false)

    /** Cross-module navigation targets */
    var selectedLearningSubjectId by mutableStateOf<String?>(null)
    var selectedPlannerTab by mutableIntStateOf(0)

    fun openNoteEditor(note: NoteUiModel) {
        activeEditingNote = note
    }

    fun closeNoteEditor() {
        activeEditingNote?.let { com.lernspace.app.data.local.AcademicDatabaseManager.persistNote(it) }
        activeEditingNote = null
    }

    val subjects = mutableStateListOf<SubjectUi>()

    val topics = mutableStateListOf<TopicUi>()

    val notes = mutableStateListOf<NoteUiModel>()

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

    fun createNoteForTopic(
        subject: SubjectUi,
        topic: TopicUi,
        paperStyle: PaperStyle = AppSettingsManager.defaultPaperStyle,
        canvasType: NoteCanvasType = NoteCanvasType.PAGED_NOTE,
        isDarkPaper: Boolean = false,
        customTitle: String? = null
    ): NoteUiModel {
        val finalTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() } ?: topic.title
        val newNote = NoteUiModel(
            id = UUID.randomUUID().toString(),
            title = finalTitle,
            subject = subject.name,
            date = "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            snippet = "Catatan ${paperStyle.displayName} untuk ${topic.title}...",
            subjectId = subject.id,
            topicId = topic.id,
            topicName = topic.title,
            canvasType = canvasType,
            paperStyle = paperStyle,
            isDarkPaper = isDarkPaper,
            isScratchpad = false
        )
        notes.add(0, newNote)
        com.lernspace.app.data.local.AcademicDatabaseManager.persistNote(newNote)
        return newNote
    }

    fun updateNoteLinkAndTitle(
        noteId: String,
        newTitle: String,
        subject: SubjectUi?,
        topic: TopicUi?
    ): NoteUiModel? {
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx == -1) return null
        val old = notes[idx]
        val isUnlinkedScratch = subject == null && old.canvasType == NoteCanvasType.INFINITE_SCRATCHPAD
        val updated = old.copy(
            title = newTitle.trim().ifBlank { old.title },
            subject = subject?.name ?: if (old.canvasType == NoteCanvasType.INFINITE_SCRATCHPAD) "Quick Scratchpad" else "General Study",
            subjectId = subject?.id,
            topicId = if (subject != null) topic?.id else null,
            topicName = if (subject != null) topic?.title else null,
            isScratchpad = isUnlinkedScratch
        )
        notes[idx] = updated
        if (activeEditingNote?.id == noteId) {
            activeEditingNote = updated
        }
        com.lernspace.app.data.local.AcademicDatabaseManager.persistNote(updated, immediate = true)
        return updated
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
        com.lernspace.app.data.local.AcademicDatabaseManager.persistNote(newNote)
        return newNote
    }

    fun updateTopicStatus(topicId: String, newStatus: TopicStatus) {
        val idx = topics.indexOfFirst { it.id == topicId }
        if (idx != -1) {
            val updated = topics[idx].copy(status = newStatus)
            topics[idx] = updated
            com.lernspace.app.data.local.AcademicDatabaseManager.persistTopic(updated)
        }
    }

    // ── Focus Session & Pomodoro State ────────────────────────────────────────
    var isFocusSessionActive by mutableStateOf(false)
    var isFocusSessionMinimized by mutableStateOf(false)
    var showFocusSetupDialog by mutableStateOf(false)
    var showFocusCompletionDialog by mutableStateOf(false)

    var focusSelectedSubject by mutableStateOf<SubjectUi?>(null)
    var focusSelectedTopic by mutableStateOf<TopicUi?>(null)
    var focusPreset by mutableStateOf(FocusTimerPreset.POMODORO_25)
    var focusPhase by mutableStateOf(FocusSessionPhase.FOCUS)
    var focusTimeRemainingSeconds by mutableIntStateOf(25 * 60)
    var focusTimeElapsedSeconds by mutableIntStateOf(0)
    var isFocusTimerRunning by mutableStateOf(false)
    var focusPillOffsetX by mutableStateOf(-1f)
    var focusPillOffsetY by mutableStateOf(-1f)
    val focusSessionHistory = mutableStateListOf<FocusSessionRecord>()
    var totalFocusStudyMinutes by mutableIntStateOf(0)

    fun startFocusSession(
        subject: SubjectUi?,
        topic: TopicUi?,
        preset: FocusTimerPreset
    ) {
        focusSelectedSubject = subject
        focusSelectedTopic = topic
        focusPreset = preset
        focusPhase = FocusSessionPhase.FOCUS
        focusTimeElapsedSeconds = 0
        focusTimeRemainingSeconds = if (preset == FocusTimerPreset.STOPWATCH) 0 else preset.focusMinutes * 60
        isFocusSessionActive = true
        isFocusSessionMinimized = false
        isFocusTimerRunning = true
        showFocusSetupDialog = false
    }

    fun pauseFocusTimer() {
        isFocusTimerRunning = false
    }

    fun resumeFocusTimer() {
        isFocusTimerRunning = true
    }

    fun tickFocusSecond() {
        if (!isFocusTimerRunning || !isFocusSessionActive) return

        if (focusPhase == FocusSessionPhase.FOCUS) {
            if (focusPreset == FocusTimerPreset.STOPWATCH) {
                focusTimeElapsedSeconds++
            } else {
                if (focusTimeRemainingSeconds > 0) {
                    focusTimeRemainingSeconds--
                    focusTimeElapsedSeconds++
                } else {
                    // Focus phase reached 0: transition to BREAK
                    val completedMins = focusPreset.focusMinutes
                    totalFocusStudyMinutes += completedMins
                    val record = FocusSessionRecord(
                        subjectId = focusSelectedSubject?.id,
                        subjectName = focusSelectedSubject?.name,
                        topicId = focusSelectedTopic?.id,
                        topicName = focusSelectedTopic?.title,
                        durationMinutes = completedMins,
                        completedAt = "Just now"
                    )
                    focusSessionHistory.add(0, record)
                    com.lernspace.app.data.local.AcademicDatabaseManager.persistFocusSession(record)
                    focusPhase = FocusSessionPhase.BREAK
                    focusTimeRemainingSeconds = focusPreset.breakMinutes * 60
                }
            }
        } else if (focusPhase == FocusSessionPhase.BREAK) {
            if (focusTimeRemainingSeconds > 0) {
                focusTimeRemainingSeconds--
            } else {
                focusPhase = FocusSessionPhase.COMPLETED
                isFocusTimerRunning = false
                showFocusCompletionDialog = true
            }
        }
    }

    fun finishFocusSessionEarly() {
        val completedMinutes = (focusTimeElapsedSeconds / 60).coerceAtLeast(1)
        totalFocusStudyMinutes += completedMinutes
        val record = FocusSessionRecord(
            subjectId = focusSelectedSubject?.id,
            subjectName = focusSelectedSubject?.name,
            topicId = focusSelectedTopic?.id,
            topicName = focusSelectedTopic?.title,
            durationMinutes = completedMinutes,
            completedAt = "Just now"
        )
        focusSessionHistory.add(0, record)
        com.lernspace.app.data.local.AcademicDatabaseManager.persistFocusSession(record)
        isFocusTimerRunning = false
        showFocusCompletionDialog = true
    }

    fun dismissFocusSession() {
        isFocusSessionActive = false
        isFocusSessionMinimized = false
        isFocusTimerRunning = false
        showFocusCompletionDialog = false
        focusPillOffsetX = -1f
        focusPillOffsetY = -1f
    }

    // ── Planner & Calendar Schedule State ─────────────────────────────────────
    val scheduleItems = mutableStateListOf<PlannerItemUi>()

    fun getItemsForDay(dayOffset: Int): List<PlannerItemUi> {
        return scheduleItems.filter { it.dayOffset == dayOffset }.sortedBy { it.startTime }
    }

    fun getItemsForWeekday(dayOfWeek: Int): List<PlannerItemUi> {
        return scheduleItems.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
    }

    // ── Fixed Weekly Class Timetable (Jadwal Mapel Mingguan) ─────────────────
    val weeklyClasses = mutableStateListOf<WeeklyClassUi>()

    // ── Student Tasks & Deadlines (Tugas Kuliah & Deadline) ───────────────────
    val tasks = mutableStateListOf<TaskUiModel>()

    fun getTasksForDayNumber(dayNumber: Int): List<TaskUiModel> {
        return tasks.filter { it.dueDayNumber == dayNumber }
    }

    fun getClassesForWeekday(dayOfWeek: Int): List<WeeklyClassUi> {
        return weeklyClasses.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
    }

    fun toggleTaskCompletion(taskId: String) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx != -1) {
            val updated = tasks[idx].copy(isCompleted = !tasks[idx].isCompleted)
            tasks[idx] = updated
            com.lernspace.app.data.local.AcademicDatabaseManager.updateTaskCompletion(taskId, updated.isCompleted)
        }
    }

    fun addTask(task: TaskUiModel) {
        tasks.add(0, task)
        com.lernspace.app.data.local.AcademicDatabaseManager.persistTask(task)
    }

    fun deleteTask(taskId: String) {
        tasks.removeAll { it.id == taskId }
        com.lernspace.app.data.local.AcademicDatabaseManager.deleteTask(taskId)
    }

    fun deleteNote(noteId: String) {
        notes.removeAll { it.id == noteId }
        com.lernspace.app.data.local.AcademicDatabaseManager.deleteNote(noteId)
    }

    fun acceptHybridSuggestion(suggestionId: String) {
        val idx = scheduleItems.indexOfFirst { it.id == suggestionId }
        if (idx != -1) {
            val item = scheduleItems[idx]
            val updated = item.copy(
                type = ScheduleType.STUDY_SESSION,
                title = item.title.replace("Hybrid Review:", "Study Session:")
            )
            scheduleItems[idx] = updated
            com.lernspace.app.data.local.AcademicDatabaseManager.persistPlannerItem(updated)
        }
    }

    // ── Flashcards & Spaced Repetition State ──────────────────────────────────
    val flashcardDecks = mutableStateListOf<FlashcardDeckUi>()
    val flashcards = mutableStateListOf<FlashcardUi>()

    fun getCardsForDeck(deckId: String): List<FlashcardUi> {
        return flashcards.filter { it.deckId == deckId }
    }

    fun getDueCardsForDeck(deckId: String, nowMs: Long = System.currentTimeMillis()): List<FlashcardUi> {
        return flashcards.filter { it.deckId == deckId && it.nextReviewAt <= nowMs }
    }

    fun addFlashcardDeck(deck: FlashcardDeckUi) {
        flashcardDecks.add(0, deck)
        com.lernspace.app.data.local.AcademicDatabaseManager.persistFlashcardDeck(deck)
    }

    fun deleteFlashcardDeck(deckId: String) {
        flashcardDecks.removeAll { it.id == deckId }
        flashcards.removeAll { it.deckId == deckId }
        com.lernspace.app.data.local.AcademicDatabaseManager.deleteFlashcardDeck(deckId)
    }

    fun addFlashcard(card: FlashcardUi) {
        flashcards.add(card)
        com.lernspace.app.data.local.AcademicDatabaseManager.persistFlashcard(card)
    }

    fun updateFlashcard(updated: FlashcardUi) {
        val idx = flashcards.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            flashcards[idx] = updated
            com.lernspace.app.data.local.AcademicDatabaseManager.persistFlashcard(updated)
        }
    }

    fun deleteFlashcard(cardId: String) {
        flashcards.removeAll { it.id == cardId }
        com.lernspace.app.data.local.AcademicDatabaseManager.deleteFlashcard(cardId)
    }

    fun reviewFlashcard(cardId: String, rating: FlashcardRating) {
        val idx = flashcards.indexOfFirst { it.id == cardId }
        if (idx == -1) return
        val card = flashcards[idx]
        val updated = computeSm2Review(card, rating)
        flashcards[idx] = updated
        com.lernspace.app.data.local.AcademicDatabaseManager.persistFlashcard(updated)
    }
}

enum class FlashcardRating(val label: String) {
    AGAIN("Lupa"),
    HARD("Sulit"),
    GOOD("Bagus"),
    EASY("Mudah")
}

fun previewIntervalLabel(card: FlashcardUi, rating: FlashcardRating): String {
    if (rating == FlashcardRating.AGAIN) return "< 10 mnt"
    val next = computeSm2Review(card, rating)
    val days = next.intervalDays
    return when {
        days <= 1 -> "1 hari"
        days < 30 -> "$days hari"
        else -> "${days / 30} bln"
    }
}

fun computeSm2Review(
    card: FlashcardUi,
    rating: FlashcardRating,
    nowMs: Long = System.currentTimeMillis()
): FlashcardUi {
    val quality = when (rating) {
        FlashcardRating.AGAIN -> 1
        FlashcardRating.HARD -> 3
        FlashcardRating.GOOD -> 4
        FlashcardRating.EASY -> 5
    }
    var reps = card.repetitions
    var interval = card.intervalDays
    var ef = card.easeFactor

    if (quality < 3) {
        reps = 0
        interval = 0
    } else {
        interval = when (reps) {
            0 -> if (rating == FlashcardRating.EASY) 4 else 1
            1 -> if (rating == FlashcardRating.HARD) 3 else if (rating == FlashcardRating.EASY) 8 else 6
            else -> {
                val mult = when (rating) {
                    FlashcardRating.HARD -> 1.2f
                    FlashcardRating.GOOD -> ef
                    FlashcardRating.EASY -> ef * 1.3f
                    FlashcardRating.AGAIN -> 0f
                }
                kotlin.math.max(interval + 1, (interval * mult).toInt())
            }
        }
        reps += 1
    }

    ef = (ef + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))).coerceAtLeast(1.3f)
    val nextReviewMs = if (interval == 0) {
        nowMs + 10 * 60 * 1000L // 10 minutes for Again
    } else {
        nowMs + interval * 24L * 60L * 60L * 1000L
    }

    return card.copy(
        repetitions = reps,
        intervalDays = interval,
        easeFactor = ef,
        nextReviewAt = nextReviewMs,
        lastReviewedAt = nowMs
    )
}

data class FlashcardDeckUi(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val subjectId: String? = null,
    val subjectName: String = "General Study",
    val topicId: String? = null,
    val topicName: String? = null,
    val sourceNoteId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FlashcardUi(
    val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,
    val hint: String = "",
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

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

enum class FocusTimerPreset(val title: String, val subtitle: String, val focusMinutes: Int, val breakMinutes: Int) {
    POMODORO_25("Pomodoro 25/5", "25m Fokus • 5m Istirahat", 25, 5),
    DEEP_50("Deep Study 50/10", "50m Fokus Intensif • 10m Istirahat", 50, 10),
    QUICK_15("Sprint 15/3", "15m Review Kilat • 3m Istirahat", 15, 3),
    STOPWATCH("Stopwatch Bebas", "Hitung maju tanpa batas waktu", 0, 0)
}

enum class FocusSessionPhase {
    FOCUS,
    BREAK,
    COMPLETED
}

data class FocusSessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String?,
    val subjectName: String?,
    val topicId: String?,
    val topicName: String?,
    val durationMinutes: Int,
    val completedAt: String
)

