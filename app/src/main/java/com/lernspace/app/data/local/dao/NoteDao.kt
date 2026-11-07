package com.lernspace.app.data.local.dao

import androidx.room.*
import com.lernspace.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE archivedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId AND archivedAt IS NULL ORDER BY updatedAt DESC")
    fun getNotesForSubject(subjectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE topicId = :topicId AND archivedAt IS NULL ORDER BY updatedAt DESC")
    fun getNotesForTopic(topicId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveNote(id: String, archivedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
