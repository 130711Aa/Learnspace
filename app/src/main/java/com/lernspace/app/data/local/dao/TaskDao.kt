package com.lernspace.app.data.local.dao

import androidx.room.*
import com.lernspace.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archivedAt IS NULL ORDER BY status ASC, dueDate ASC, dueAt ASC, priority DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' AND status != 'CANCELLED' AND archivedAt IS NULL AND dueDate = :todayDate ORDER BY priority DESC, dueAt ASC")
    fun getTasksDueToday(todayDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' AND status != 'CANCELLED' AND archivedAt IS NULL AND ((dueDate < :todayDate) OR (dueAt IS NOT NULL AND dueAt < :currentMillis)) ORDER BY dueDate ASC, dueAt ASC")
    fun getOverdueTasks(todayDate: String, currentMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId AND archivedAt IS NULL ORDER BY dueDate ASC")
    fun getTasksBySubject(subjectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveTask(id: String, archivedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}
