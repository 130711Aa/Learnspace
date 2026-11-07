package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("subjectId"),
        Index("topicId"),
        Index("status"),
        Index("dueDate"),
        Index("dueAt")
    ],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val subjectId: String? = null,
    val topicId: String? = null,
    val status: String = "TODO", // TODO, IN_PROGRESS, COMPLETED, CANCELLED
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val dueDate: String? = null, // ISO YYYY-MM-DD
    val dueAt: Long? = null, // epoch millis
    val estimatedMinutes: Int? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

