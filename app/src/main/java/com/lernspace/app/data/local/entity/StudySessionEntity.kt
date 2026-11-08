package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
    indices = [
        Index("subjectId"),
        Index("topicId")
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
data class StudySessionEntity(
    @PrimaryKey
    val id: String,
    val title: String = "Focus Session",
    val subjectId: String? = null,
    val subjectName: String? = null,
    val topicId: String? = null,
    val topicName: String? = null,
    val durationMinutes: Int = 25,
    val completedAt: String = "Just now",
    val timestamp: Long = System.currentTimeMillis()
)
