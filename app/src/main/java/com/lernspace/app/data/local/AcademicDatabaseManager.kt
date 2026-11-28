package com.lernspace.app.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.lernspace.app.data.local.entity.*
import com.lernspace.app.data.repository.*
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.AccentTeal
import com.lernspace.app.ui.workspace.document.DocxDocumentCodec
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Central Offline-First Synchronization Engine.
 * Seamlessly bridges the Room Database (SQLite) and local disk storage
 * with the reactive Compose state (AcademicWorkspaceState).
 */
object AcademicDatabaseManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var db: LernspaceDatabase? = null
    private var storageManager: NoteStorageManager? = null
    private val pendingNoteSaves = ConcurrentHashMap<String, Job>()
    private val noteWriteMutexes = ConcurrentHashMap<String, Mutex>()
    private val inFlightNoteLoads = ConcurrentHashMap<String, Deferred<LoadedNoteContent?>>()
    private var preloadJob: Job? = null

    fun init(database: LernspaceDatabase, context: Context) {
        db = database
        storageManager = NoteStorageManager(context)

        scope.launch {
            loadAllFromDatabase()
        }
    }

    /**
     * Fully suspends and reloads all domains into AcademicWorkspaceState.
     * Essential after restoring a backup so the in-memory state is refreshed immediately.
     */
    suspend fun reloadAllFromDatabase() {
        loadAllFromDatabase()
    }

    private suspend fun loadAllFromDatabase() {
        val database = db ?: return
        val storage = storageManager ?: return

        try {

        // 1. Subjects
        val existingSubjects = database.subjectDao().getAllActiveSubjects().firstOrNull() ?: emptyList()
        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.subjects.clear()
            AcademicWorkspaceState.subjects.addAll(existingSubjects.map { entity ->
                SubjectUi(
                    id = entity.id,
                    name = entity.name,
                    code = entity.code,
                    description = entity.description ?: "",
                    color = if (entity.colorArgb != null) Color(entity.colorArgb.toULong()) else AccentBlue
                )
            })
        }

        // 2. Topics
        val existingTopics = database.topicDao().getAllActiveTopics().firstOrNull() ?: emptyList()
        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.topics.clear()
            AcademicWorkspaceState.topics.addAll(existingTopics.map { entity ->
                val status = try {
                    TopicStatus.valueOf(entity.status)
                } catch (e: Exception) {
                    TopicStatus.NOT_STARTED
                }
                TopicUi(
                    id = entity.id,
                    subjectId = entity.subjectId,
                    title = entity.name,
                    description = entity.description ?: "",
                    status = status,
                    targetDate = entity.targetDate
                )
            })
        }

        // 3. Tasks
        val existingTasks = database.taskDao().getAllActiveTasks().firstOrNull() ?: emptyList()
        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.tasks.clear()
            AcademicWorkspaceState.tasks.addAll(existingTasks.map { entity ->
                TaskUiModel(
                    id = entity.id,
                    title = entity.title,
                    subjectId = entity.subjectId,
                    subjectName = entity.subjectName,
                    topicId = entity.topicId,
                    topicName = entity.topicName,
                    dueDate = entity.dueDate ?: "",
                    dueDayNumber = entity.dueDayNumber,
                    dueTime = entity.dueTime,
                    priority = entity.priority,
                    estimatedMinutes = entity.estimatedMinutes ?: 60,
                    isCompleted = entity.status == "COMPLETED",
                    notes = entity.notes
                )
            })
        }

        // 4. Notes (Instant metadata-only load from Room; heavy strokes/images load lazily when opened!)
        val existingNotes = database.noteDao().getAllActiveNotes().firstOrNull() ?: emptyList()
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val loadedNoteModels = existingNotes.map { entity ->
            val paperStyle = try {
                PaperStyle.valueOf(entity.paperStyle)
            } catch (e: Exception) {
                PaperStyle.RULED
            }
            val dateFormatted = dateFormat.format(Date(entity.updatedAt))
            val isDoc = entity.paperStyle == "DOC_DOCUMENT"
            val cleanSnippet = if (entity.snippet.contains('<') || entity.snippet.contains("<!--LSPAGE")) {
                val text = DocxDocumentCodec.exportToPlainText(entity.snippet).replace("\n", " ").trim()
                if (text.isNotBlank()) text.take(120) + (if (text.length > 120) "..." else "") else if (isDoc) "Dokumen Teks Kosong..." else "Catatan kosong..."
            } else {
                entity.snippet
            }

            NoteUiModel(
                id = entity.id,
                title = entity.title,
                subject = entity.subject,
                date = dateFormatted,
                snippet = cleanSnippet,
                subjectId = entity.subjectId,
                topicId = entity.topicId,
                topicName = entity.topicName,
                isScratchpad = entity.isScratchpad,
                canvasType = when {
                    isDoc -> NoteCanvasType.DOC_DOCUMENT
                    !entity.pdfUri.isNullOrBlank() -> NoteCanvasType.PDF_DOCUMENT
                    entity.isScratchpad -> NoteCanvasType.INFINITE_SCRATCHPAD
                    else -> NoteCanvasType.PAGED_NOTE
                },
                paperStyle = paperStyle,
                isDarkPaper = false,
                pdfUri = entity.pdfUri,
                pdfTotalPages = entity.pdfTotalPages,
                pages = mutableListOf(NotePageData(pageIndex = 0)),
                documentContent = "",
                isContentLoaded = false
            )
        }
        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.notes.clear()
            AcademicWorkspaceState.notes.addAll(loadedNoteModels)
        }

        // Background Preload: diam-diam muat 6 catatan terbaru setelah Workspace tampil agar buka catatan langsung instan
        preloadJob?.cancel()
        preloadJob = scope.launch(Dispatchers.IO) {
            for (noteModel in loadedNoteModels.take(6)) {
                if (!isActive) break
                if (!noteModel.isContentLoaded) {
                    try {
                        ensureNoteLoaded(noteModel)
                    } catch (_: Exception) {}
                }
            }
        }

        // 5. Calendar Events & Weekly Classes
        val existingPlanner = database.calendarEventDao().getPlannerItems().firstOrNull() ?: emptyList()
        val existingWeekly = database.calendarEventDao().getWeeklyClasses().firstOrNull() ?: emptyList()

        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.scheduleItems.clear()
            AcademicWorkspaceState.scheduleItems.addAll(existingPlanner.map { entity ->
                val schedType = try {
                    ScheduleType.valueOf(entity.eventType)
                } catch (e: Exception) {
                    ScheduleType.LECTURE
                }
                PlannerItemUi(
                    id = entity.id,
                    title = entity.title,
                    type = schedType,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    dayOfWeek = entity.dayOfWeek,
                    dayOffset = entity.dayOffset,
                    subjectId = entity.subjectId,
                    subjectName = entity.subjectName,
                    topicId = entity.topicId,
                    topicName = entity.topicName,
                    locationOrRoom = entity.locationOrRoom,
                    isCompleted = entity.isCompleted,
                    aiReason = entity.aiReason
                )
            })

            AcademicWorkspaceState.weeklyClasses.clear()
            AcademicWorkspaceState.weeklyClasses.addAll(existingWeekly.map { entity ->
                WeeklyClassUi(
                    id = entity.id,
                    subjectId = entity.subjectId ?: "",
                    subjectName = entity.subjectName ?: "",
                    subjectCode = entity.subjectCode ?: "",
                    dayOfWeek = entity.dayOfWeek,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    room = entity.locationOrRoom ?: "",
                    lecturer = entity.lecturer ?: "",
                    type = entity.eventType
                )
            })
        }

        // 6. Focus Sessions History
        val existingSessions = database.studySessionDao().getAllSessions().firstOrNull() ?: emptyList()
        withContext(Dispatchers.Main) {
            if (existingSessions.isNotEmpty()) {
                AcademicWorkspaceState.focusSessionHistory.clear()
                var totalMins = 0
                existingSessions.forEach { entity ->
                    totalMins += entity.durationMinutes
                    AcademicWorkspaceState.focusSessionHistory.add(
                        FocusSessionRecord(
                            id = entity.id,
                            subjectId = entity.subjectId,
                            subjectName = entity.subjectName,
                            topicId = entity.topicId,
                            topicName = entity.topicName,
                            durationMinutes = entity.durationMinutes,
                            completedAt = entity.completedAt
                        )
                    )
                }
                AcademicWorkspaceState.totalFocusStudyMinutes = totalMins
            }
        }

        // 7. Flashcards & Decks
        val existingDecks = database.flashcardDao().getAllDecks()
        val existingCards = database.flashcardDao().getAllCards()
        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.flashcardDecks.clear()
            AcademicWorkspaceState.flashcardDecks.addAll(existingDecks.map { entity ->
                FlashcardDeckUi(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    subjectId = entity.subjectId,
                    subjectName = entity.subjectName,
                    topicId = entity.topicId,
                    topicName = entity.topicName,
                    sourceNoteId = entity.sourceNoteId,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            })
            AcademicWorkspaceState.flashcards.clear()
            AcademicWorkspaceState.flashcards.addAll(existingCards.map { entity ->
                FlashcardUi(
                    id = entity.id,
                    deckId = entity.deckId,
                    front = entity.front,
                    back = entity.back,
                    hint = entity.hint,
                    repetitions = entity.repetitions,
                    intervalDays = entity.intervalDays,
                    easeFactor = entity.easeFactor,
                    nextReviewAt = entity.nextReviewAt,
                    lastReviewedAt = entity.lastReviewedAt,
                    createdAt = entity.createdAt
                )
            })
        }
        } catch (e: Exception) {
            android.util.Log.e("AcademicDatabaseManager", "Error loading data from database", e)
        }
    }

    // ── Persistent Operations (Async background writes) ───────────────────────

    fun persistNote(note: NoteUiModel, immediate: Boolean = false) {
        val database = db ?: return
        val storage = storageManager ?: return

        // Take a thread-safe snapshot of mutable page lists on the calling thread before hopping to IO
        val pagesSnapshot = note.pages.map { page ->
            NotePageData(
                pageIndex = page.pageIndex,
                textBoxes = page.textBoxes.toMutableList(),
                strokes = page.strokes.toMutableList(),
                imageElements = page.imageElements.toMutableList(),
                interactiveModules = page.interactiveModules.toMutableList()
            )
        }
        val isDark = note.isDarkPaper
        val isDoc = note.canvasType == NoteCanvasType.DOC_DOCUMENT
        val cleanSnippet = if (isDoc) {
            val text = DocxDocumentCodec.exportToPlainText(note.documentContent).replace("\n", " ").trim()
            if (text.isNotBlank()) text.take(120) + (if (text.length > 120) "..." else "") else "Dokumen Teks Kosong..."
        } else {
            val s = note.snippet
            if (s.contains('<') || s.contains("<!--LSPAGE")) {
                val text = DocxDocumentCodec.exportToPlainText(s).replace("\n", " ").trim()
                if (text.isNotBlank()) text.take(120) + (if (text.length > 120) "..." else "") else "Catatan kosong..."
            } else {
                s
            }
        }
        note.snippet = cleanSnippet

        val entity = NoteEntity(
            id = note.id,
            title = note.title,
            snippet = cleanSnippet,
            subject = note.subject,
            subjectId = note.subjectId,
            topicId = note.topicId,
            topicName = note.topicName,
            isScratchpad = note.isScratchpad,
            paperStyle = if (note.canvasType == NoteCanvasType.DOC_DOCUMENT) "DOC_DOCUMENT" else note.paperStyle.name,
            pdfUri = note.pdfUri,
            pdfTotalPages = note.pdfTotalPages,
            updatedAt = System.currentTimeMillis()
        )

        val shouldSaveContent = note.isContentLoaded
        pendingNoteSaves.remove(note.id)?.cancel()
        val mutex = noteWriteMutexes.getOrPut(note.id) { Mutex() }
        lateinit var launchedJob: Job
        launchedJob = scope.launch {
            try {
                if (!immediate) {
                    delay(900L)
                }
                mutex.withLock {
                    database.noteDao().insertNote(entity)
                    if (shouldSaveContent) {
                        storage.saveNotePages(
                            note.id,
                            pagesSnapshot,
                            isDark,
                            note.lastOpenedPageIndex,
                            canvasType = note.canvasType.name,
                            documentContent = note.documentContent
                        )
                    }
                }
            } finally {
                pendingNoteSaves.remove(note.id, launchedJob)
            }
        }
        pendingNoteSaves[note.id] = launchedJob
    }

    fun hasPendingSave(noteId: String): Boolean {
        return pendingNoteSaves[noteId]?.isActive == true
    }

    suspend fun awaitPendingSave(noteId: String) {
        pendingNoteSaves[noteId]?.join()
    }

    fun loadPagesForNote(noteId: String): List<NotePageData>? {
        return storageManager?.loadNotePages(noteId)
    }

    fun loadNoteContent(noteId: String): com.lernspace.app.data.local.LoadedNoteContent? {
        return storageManager?.loadNoteWithMeta(noteId)
    }

    suspend fun loadNoteContentAsync(noteId: String): LoadedNoteContent? {
        val storage = storageManager ?: return null
        val existing = inFlightNoteLoads[noteId]
        if (existing != null && existing.isActive) {
            return existing.await()
        }
        val deferred = scope.async(Dispatchers.IO) {
            try {
                storage.loadNoteWithMeta(noteId)
            } finally {
                inFlightNoteLoads.remove(noteId)
            }
        }
        val prev = inFlightNoteLoads.putIfAbsent(noteId, deferred)
        return if (prev != null && prev.isActive) {
            deferred.cancel()
            prev.await()
        } else {
            deferred.await()
        }
    }

    suspend fun ensureNoteLoaded(note: NoteUiModel) {
        if (note.isContentLoaded || hasPendingSave(note.id)) {
            note.isContentLoaded = true
            return
        }
        awaitPendingSave(note.id)
        val diskContent = loadNoteContentAsync(note.id)
        withContext(Dispatchers.Main) {
            if (!note.isContentLoaded && diskContent != null) {
                note.isDarkPaper = diskContent.isDarkPaper
                if (diskContent.documentContent != null) {
                    note.documentContent = diskContent.documentContent
                }
                if (diskContent.pages.isNotEmpty()) {
                    note.pages.clear()
                    note.pages.addAll(diskContent.pages)
                    if (diskContent.lastOpenedPageIndex in 0 until note.pages.size) {
                        note.lastOpenedPageIndex = diskContent.lastOpenedPageIndex
                    }
                }
            }
            note.isContentLoaded = true
        }
    }

    fun deleteNote(noteId: String) {
        pendingNoteSaves.remove(noteId)?.cancel()
        val database = db ?: return
        val storage = storageManager ?: return

        scope.launch {
            database.noteDao().deleteNoteById(noteId)
            storage.deleteNoteStorage(noteId)
        }
    }

    fun persistTask(task: TaskUiModel) {
        val database = db ?: return
        scope.launch {
            val entity = TaskEntity(
                id = task.id,
                title = task.title,
                subjectId = task.subjectId,
                topicId = task.topicId,
                subjectName = task.subjectName,
                topicName = task.topicName,
                status = if (task.isCompleted) "COMPLETED" else "TODO",
                priority = task.priority,
                dueDate = task.dueDate,
                dueTime = task.dueTime,
                dueDayNumber = task.dueDayNumber,
                estimatedMinutes = task.estimatedMinutes,
                notes = task.notes,
                completedAt = if (task.isCompleted) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
            database.taskDao().insertTask(entity)
        }
    }

    fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        val database = db ?: return
        scope.launch {
            database.taskDao().updateStatus(
                id = taskId,
                status = if (isCompleted) "COMPLETED" else "TODO",
                completedAt = if (isCompleted) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun deleteTask(taskId: String) {
        val database = db ?: return
        scope.launch {
            database.taskDao().deleteTaskById(taskId)
        }
    }

    fun deleteSubject(subjectId: String) {
        val database = db ?: return
        scope.launch {
            database.subjectDao().deleteSubjectById(subjectId)
            database.topicDao().deleteTopicsForSubject(subjectId)
        }
    }

    fun deleteTopic(topicId: String) {
        val database = db ?: return
        scope.launch {
            database.topicDao().deleteTopicById(topicId)
        }
    }

    fun persistTopic(topic: TopicUi) {
        val database = db ?: return
        scope.launch {
            val entity = TopicEntity(
                id = topic.id,
                subjectId = topic.subjectId,
                name = topic.title,
                description = topic.description,
                status = topic.status.name,
                targetDate = topic.targetDate,
                updatedAt = System.currentTimeMillis()
            )
            database.topicDao().insertTopic(entity)
        }
    }

    fun persistSubject(subject: SubjectUi) {
        val database = db ?: return
        scope.launch {
            val entity = SubjectEntity(
                id = subject.id,
                name = subject.name,
                code = subject.code,
                description = subject.description,
                colorArgb = subject.color.value.toLong(),
                updatedAt = System.currentTimeMillis()
            )
            database.subjectDao().insertSubject(entity)
        }
    }

    fun persistPlannerItem(item: PlannerItemUi) {
        val database = db ?: return
        scope.launch {
            val entity = CalendarEventEntity(
                id = item.id,
                title = item.title,
                eventType = item.type.name,
                startTime = item.startTime,
                endTime = item.endTime,
                dayOfWeek = item.dayOfWeek,
                dayOffset = item.dayOffset,
                subjectId = item.subjectId,
                subjectName = item.subjectName,
                topicId = item.topicId,
                topicName = item.topicName,
                locationOrRoom = item.locationOrRoom,
                isWeeklyClass = false,
                isCompleted = item.isCompleted,
                aiReason = item.aiReason
            )
            database.calendarEventDao().insertEvent(entity)
        }
    }

    fun persistWeeklyClass(wClass: WeeklyClassUi, subject: SubjectUi? = null) {
        val database = db ?: return
        scope.launch {
            try {
                if (subject != null) {
                    val existing = database.subjectDao().getSubjectById(subject.id)
                    if (existing == null) {
                        val subEntity = SubjectEntity(
                            id = subject.id,
                            name = subject.name,
                            code = subject.code,
                            description = subject.description,
                            colorArgb = subject.color.value.toLong(),
                            updatedAt = System.currentTimeMillis()
                        )
                        database.subjectDao().insertSubject(subEntity)
                    }
                }
                val entity = CalendarEventEntity(
                    id = wClass.id,
                    title = "${wClass.subjectName}: ${wClass.type}",
                    eventType = wClass.type,
                    startTime = wClass.startTime,
                    endTime = wClass.endTime,
                    dayOfWeek = wClass.dayOfWeek,
                    subjectId = wClass.subjectId,
                    subjectName = wClass.subjectName,
                    subjectCode = wClass.subjectCode,
                    locationOrRoom = wClass.room,
                    lecturer = wClass.lecturer,
                    isWeeklyClass = true
                )
                database.calendarEventDao().insertEvent(entity)
            } catch (e: Exception) {
                android.util.Log.e("AcademicDbManager", "Error persisting weekly class: ${e.message}", e)
            }
        }
    }

    fun importWeeklyClassesBatch(
        newSubjects: List<SubjectUi>,
        newClasses: List<WeeklyClassUi>
    ) {
        val database = db ?: return
        scope.launch {
            try {
                // 1. Insert all new subjects FIRST in database
                if (newSubjects.isNotEmpty()) {
                    val subjectEntities = newSubjects.map { subject ->
                        SubjectEntity(
                            id = subject.id,
                            name = subject.name,
                            code = subject.code,
                            description = subject.description,
                            colorArgb = subject.color.value.toLong(),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    database.subjectDao().insertSubjects(subjectEntities)
                }

                // 2. Insert all weekly classes SECOND
                if (newClasses.isNotEmpty()) {
                    val classEntities = newClasses.map { wClass ->
                        CalendarEventEntity(
                            id = wClass.id,
                            title = "${wClass.subjectName}: ${wClass.type}",
                            eventType = wClass.type,
                            startTime = wClass.startTime,
                            endTime = wClass.endTime,
                            dayOfWeek = wClass.dayOfWeek,
                            dayOffset = -1,
                            subjectId = wClass.subjectId,
                            subjectName = wClass.subjectName,
                            subjectCode = wClass.subjectCode,
                            locationOrRoom = wClass.room,
                            lecturer = wClass.lecturer,
                            isWeeklyClass = true,
                            isCompleted = false
                        )
                    }
                    database.calendarEventDao().insertEvents(classEntities)
                }
            } catch (e: Exception) {
                android.util.Log.e("AcademicDbManager", "Failed to import weekly classes batch: ${e.message}", e)
            }
        }
    }

    fun deleteCalendarEvent(id: String) {
        val database = db ?: return
        scope.launch {
            database.calendarEventDao().deleteEventById(id)
        }
    }

    fun persistFocusSession(record: FocusSessionRecord) {
        val database = db ?: return
        scope.launch {
            val entity = StudySessionEntity(
                id = record.id,
                title = record.topicName ?: "Focus Session",
                subjectId = record.subjectId,
                subjectName = record.subjectName,
                topicId = record.topicId,
                topicName = record.topicName,
                durationMinutes = record.durationMinutes,
                completedAt = record.completedAt,
                timestamp = System.currentTimeMillis()
            )
            database.studySessionDao().insertSession(entity)
        }
    }

    fun persistFlashcardDeck(deck: FlashcardDeckUi) {
        val database = db ?: return
        scope.launch {
            database.flashcardDao().insertDeck(
                FlashcardDeckEntity(
                    id = deck.id,
                    title = deck.title,
                    description = deck.description,
                    subjectId = deck.subjectId,
                    subjectName = deck.subjectName,
                    topicId = deck.topicId,
                    topicName = deck.topicName,
                    sourceNoteId = deck.sourceNoteId,
                    createdAt = deck.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteFlashcardDeck(deckId: String) {
        val database = db ?: return
        scope.launch {
            database.flashcardDao().deleteDeckById(deckId)
        }
    }

    fun persistFlashcard(card: FlashcardUi) {
        val database = db ?: return
        scope.launch {
            database.flashcardDao().insertCard(
                FlashcardEntity(
                    id = card.id,
                    deckId = card.deckId,
                    front = card.front,
                    back = card.back,
                    hint = card.hint,
                    repetitions = card.repetitions,
                    intervalDays = card.intervalDays,
                    easeFactor = card.easeFactor,
                    nextReviewAt = card.nextReviewAt,
                    lastReviewedAt = card.lastReviewedAt,
                    createdAt = card.createdAt
                )
            )
        }
    }

    fun deleteFlashcard(cardId: String) {
        val database = db ?: return
        scope.launch {
            database.flashcardDao().deleteCardById(cardId)
        }
    }

    /**
     * Wipes all data from Room tables and local disk files, resetting state to clean empty.
     */
    fun clearAllData(context: Context) {
        val database = db ?: return
        scope.launch {
            database.clearAllTables()
            storageManager?.clearAllFiles()
            withContext(Dispatchers.Main) {
                AcademicWorkspaceState.subjects.clear()
                AcademicWorkspaceState.topics.clear()
                AcademicWorkspaceState.tasks.clear()
                AcademicWorkspaceState.notes.clear()
                AcademicWorkspaceState.scheduleItems.clear()
                AcademicWorkspaceState.weeklyClasses.clear()
                AcademicWorkspaceState.focusSessionHistory.clear()
                AcademicWorkspaceState.totalFocusStudyMinutes = 0
                AcademicWorkspaceState.flashcardDecks.clear()
                AcademicWorkspaceState.flashcards.clear()
            }
        }
    }
}
