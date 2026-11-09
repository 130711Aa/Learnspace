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
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NotePageData
import com.lernspace.app.ui.workspace.model.NoteUiModel
import com.lernspace.app.ui.workspace.model.PaperStyle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

/**
 * Central Offline-First Synchronization Engine.
 * Seamlessly bridges the Room Database (SQLite) and local disk storage
 * with the reactive Compose state (AcademicWorkspaceState).
 */
object AcademicDatabaseManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var db: LernspaceDatabase? = null
    private var storageManager: NoteStorageManager? = null

    fun init(database: LernspaceDatabase, context: Context) {
        db = database
        storageManager = NoteStorageManager(context)

        scope.launch {
            loadAllFromDatabase()
        }
    }

    private suspend fun loadAllFromDatabase() {
        val database = db ?: return
        val storage = storageManager ?: return

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

        // 4. Notes & Canvas Strokes
        val existingNotes = database.noteDao().getAllActiveNotes().firstOrNull() ?: emptyList()
        withContext(Dispatchers.Main) {
            AcademicWorkspaceState.notes.clear()
            AcademicWorkspaceState.notes.addAll(existingNotes.map { entity ->
                val loadedPages = storage.loadNotePages(entity.id) ?: listOf(NotePageData(pageIndex = 0))
                val paperStyle = try {
                    PaperStyle.valueOf(entity.paperStyle)
                } catch (e: Exception) {
                    PaperStyle.RULED
                }
                val dateFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(entity.updatedAt))

                NoteUiModel(
                    id = entity.id,
                    title = entity.title,
                    subject = entity.subject,
                    date = dateFormatted,
                    snippet = entity.snippet,
                    subjectId = entity.subjectId,
                    topicId = entity.topicId,
                    topicName = entity.topicName,
                    isScratchpad = entity.isScratchpad,
                    canvasType = if (entity.isScratchpad) NoteCanvasType.INFINITE_SCRATCHPAD else NoteCanvasType.PAGED_NOTE,
                    paperStyle = paperStyle,
                    isDarkPaper = storageManager?.isDarkPaper(entity.id) ?: false,
                    pdfUri = entity.pdfUri,
                    pdfTotalPages = entity.pdfTotalPages,
                    pages = loadedPages.toMutableList()
                )
            })
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
    }

    // ── Persistent Operations (Async background writes) ───────────────────────

    fun persistNote(note: NoteUiModel) {
        val database = db ?: return
        val storage = storageManager ?: return

        scope.launch {
            val entity = NoteEntity(
                id = note.id,
                title = note.title,
                snippet = note.snippet,
                subject = note.subject,
                subjectId = note.subjectId,
                topicId = note.topicId,
                topicName = note.topicName,
                isScratchpad = note.isScratchpad,
                paperStyle = note.paperStyle.name,
                pdfUri = note.pdfUri,
                pdfTotalPages = note.pdfTotalPages,
                updatedAt = System.currentTimeMillis()
            )
            database.noteDao().insertNote(entity)
            storage.saveNotePages(note.id, note.pages, note.isDarkPaper)
        }
    }

    fun loadPagesForNote(noteId: String): List<NotePageData>? {
        return storageManager?.loadNotePages(noteId)
    }

    fun deleteNote(noteId: String) {
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

    fun persistWeeklyClass(wClass: WeeklyClassUi) {
        val database = db ?: return
        scope.launch {
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
            }
        }
    }
}
