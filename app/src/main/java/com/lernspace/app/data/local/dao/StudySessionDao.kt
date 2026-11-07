package com.lernspace.app.data.local.dao

import androidx.room.*
import com.lernspace.app.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE plannedStartAt >= :rangeStart AND plannedStartAt <= :rangeEnd ORDER BY plannedStartAt ASC")
    fun getSessionsBetween(rangeStart: Long, rangeEnd: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE status = 'PLANNED' AND plannedStartAt >= :currentMillis ORDER BY plannedStartAt ASC LIMIT 1")
    fun getNextUpcomingSession(currentMillis: Long): Flow<StudySessionEntity?>

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Delete
    suspend fun deleteSession(session: StudySessionEntity)
}

