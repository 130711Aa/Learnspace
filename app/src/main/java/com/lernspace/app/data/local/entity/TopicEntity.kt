package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("subjectId")
    ]
)
data class TopicEntity(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val name: String,
    val description: String? = null,
    val status: String = "NOT_STARTED", // NOT_STARTED, LEARNING, NEEDS_REVIEW, COMFORTABLE
    val targetDate: String? = null, // YYYY-MM-DD
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

