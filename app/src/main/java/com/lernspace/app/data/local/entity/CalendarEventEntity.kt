package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
    indices = [
        Index("startAt"),
        Index("endAt"),
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
    val description: String? = null,
    val startAt: Long,
    val endAt: Long,
    val timeZoneId: String = "UTC",
    val isAllDay: Boolean = false,
    val subjectId: String? = null,
    val topicId: String? = null,
    val location: String? = null,
    val recurrenceRuleId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

