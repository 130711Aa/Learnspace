package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
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
data class CalendarEventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val eventType: String = "LECTURE", // LECTURE, LAB, STUDY_SESSION, EXAM, ASSIGNMENT_DEADLINE, HYBRID_SUGGESTION
    val startTime: String = "08:30",
    val endTime: String = "10:00",
    val dayOfWeek: Int = 1,
    val dayOffset: Int = 0,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val topicId: String? = null,
    val topicName: String? = null,
    val locationOrRoom: String? = null,
    val lecturer: String? = null,
    val isWeeklyClass: Boolean = false,
    val isCompleted: Boolean = false,
    val aiReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
