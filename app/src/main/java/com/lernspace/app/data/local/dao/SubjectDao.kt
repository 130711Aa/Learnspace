package com.lernspace.app.data.local.dao

import androidx.room.*
import com.lernspace.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAllActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveSubject(id: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}
