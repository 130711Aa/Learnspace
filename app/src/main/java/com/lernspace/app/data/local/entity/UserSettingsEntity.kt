package com.lernspace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val weekStartsOn: Int = 1, // ISO weekday Monday = 1
    val defaultReminderLeadMinutes: Int = 60,
    val notificationsEnabled: Boolean = true,
    val quietHoursStartMinute: Int? = null,
    val quietHoursEndMinute: Int? = null,
    val timeZoneId: String = "UTC",
    val updatedAt: Long = System.currentTimeMillis()
)
