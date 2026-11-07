package com.lernspace.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lernspace.app.LernspaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Reschedules all pending active reminders after device reboot (PRD Section 10 / REM-09)
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? LernspaceApplication ?: return
            val reminderScheduler = ReminderScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val activeReminders = app.database.reminderDao().getActiveReminders().firstOrNull() ?: emptyList()
                val now = System.currentTimeMillis()

                activeReminders.forEach { reminder ->
                    if (reminder.triggerAt > now) {
                        reminderScheduler.scheduleExact(
                            reminderId = reminder.id,
                            triggerAtMillis = reminder.triggerAt,
                            title = reminder.title,
                            targetType = reminder.targetType,
                            targetId = reminder.targetId
                        )
                    } else {
                        // Mark as fired if missed during reboot
                        app.database.reminderDao().updateStatus(reminder.id, "FIRED")
                    }
                }
            }
        }
    }
}
