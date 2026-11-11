package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcard_decks",
    indices = [
        Index("subjectId"),
        Index("topicId"),
        Index("sourceNoteId")
    ]
)
data class FlashcardDeckEntity(
    @PrimaryKey
    val id: String,
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
