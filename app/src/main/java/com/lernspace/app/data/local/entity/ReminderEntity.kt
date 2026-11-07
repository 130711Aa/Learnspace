package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index("triggerAt"),
        Index("status"),
        Index("targetId"),
        Index("targetType")
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val type: String = "MANUAL", // MANUAL, AUTOMATIC, SCHEDULE
    val targetType: String? = "NONE", // TASK, EVENT, STUDY_SESSION, NONE
    val targetId: String? = null,
    val triggerAt: Long,
    val timeZoneId: String = "UTC",
    val status: String = "ACTIVE", // ACTIVE, FIRED, SNOOZED, DISMISSED, CANCELLED
    val snoozedUntil: Long? = null,
    val ruleKey: String? = null,
    val notificationId: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

