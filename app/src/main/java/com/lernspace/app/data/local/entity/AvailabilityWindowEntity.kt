package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "availability_windows")
data class AvailabilityWindowEntity(
    @PrimaryKey
    val id: String,
    val dayOfWeek: Int, // 1-7 ISO weekday
    val startMinute: Int, // Minutes after midnight
    val endMinute: Int, // Minutes after midnight
    val timeZoneId: String = "UTC",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

