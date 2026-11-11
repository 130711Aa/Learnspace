package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    indices = [
        Index("deckId"),
        Index("nextReviewAt")
    ],
    foreignKeys = [
        ForeignKey(
            entity = FlashcardDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FlashcardEntity(
    @PrimaryKey
    val id: String,
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
