package com.lernspace.app.data.local.dao

import androidx.room.*
import com.lernspace.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveTopic(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE topics SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)
}

