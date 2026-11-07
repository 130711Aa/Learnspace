package com.lernspace.app.data.repository

import com.lernspace.app.data.local.dao.TaskDao
import com.lernspace.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllActiveTasks()

    fun getTasksDueToday(): Flow<List<TaskEntity>> {
        val today = LocalDate.now().format(dateFormatter)
        return taskDao.getTasksDueToday(today)
    }

    fun getOverdueTasks(): Flow<List<TaskEntity>> {
        val today = LocalDate.now().format(dateFormatter)
        val nowMillis = System.currentTimeMillis()
        return taskDao.getOverdueTasks(today, nowMillis)
    }

    suspend fun createTask(
        title: String,
        subjectId: String? = null,
        topicId: String? = null,
        priority: String = "MEDIUM",
        dueDate: String? = LocalDate.now().format(dateFormatter)
    ): String {
        val id = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = id,
            title = title,
            subjectId = subjectId,
            topicId = topicId,
            priority = priority,
            dueDate = dueDate
        )
        taskDao.insertTask(task)
        return id
    }

    suspend fun completeTask(taskId: String) {
        taskDao.updateStatus(
            id = taskId,
            status = "COMPLETED",
            completedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun seedInitialTasksIfEmpty(subjectId: String? = null) {
        val today = LocalDate.now().format(dateFormatter)
        createTask("Review NMDA receptor cascades & LTP paper", subjectId = subjectId, priority = "HIGH", dueDate = today)
        createTask("Complete problem set #4: Biostatistics", subjectId = subjectId, priority = "MEDIUM", dueDate = today)
    }
}
