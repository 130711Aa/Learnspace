package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
    indices = [
        Index("subjectId"),
        Index("topicId"),
        Index("taskId"),
        Index("plannedStartAt"),
        Index("status")
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
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class StudySessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val taskId: String? = null,
    val plannedStartAt: Long? = null,
    val plannedEndAt: Long? = null,
    val actualStartAt: Long? = null,
    val actualEndAt: Long? = null,
    val status: String = "PLANNED", // PLANNED, IN_PROGRESS, COMPLETED, SKIPPED
    val origin: String = "MANUAL", // MANUAL, SCHEDULER
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

