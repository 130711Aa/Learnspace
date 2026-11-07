package com.lernspace.app.core.scheduling

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class TimeInterval(
    val startEpochMillis: Long,
    val endEpochMillis: Long
)

data class StudySlotSuggestion(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val durationMinutes: Int,
    val subjectId: String?,
    val topicId: String?,
    val reason: String,
    val isRecommended: Boolean = true
)

data class SchedulerRequest(
    val subjectId: String?,
    val topicId: String?,
    val targetDurationMinutes: Int = 60,
    val preferredSessionLengthMinutes: Int = 45,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(7),
    val startHourOfDay: Int = 9,
    val endHourOfDay: Int = 21,
    val bufferMinutes: Int = 15
)

/**
 * Deterministic Rule-Based Hybrid Study Scheduler (PRD Section 9)
 *
 * Rules:
 * 1. Never schedule in the past.
 * 2. Never overlap protected calendar events.
 * 3. Never overlap existing study sessions.
 * 4. Respect preferred study windows and buffers.
 * 5. Deterministic, transparent, and explainable suggestions.
 */
class HybridStudyScheduler(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun findAvailableSlots(
        request: SchedulerRequest,
        existingEvents: List<TimeInterval>,
        existingSessions: List<TimeInterval>,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<StudySlotSuggestion> {
        val occupiedBlocks = (existingEvents + existingSessions).sortedBy { it.startEpochMillis }
        val suggestions = mutableListOf<StudySlotSuggestion>()

        var currentDate = request.startDate
        while (!currentDate.isAfter(request.endDate)) {
            val windowStart = LocalDateTime.of(currentDate, LocalTime.of(request.startHourOfDay, 0))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
            val windowEnd = LocalDateTime.of(currentDate, LocalTime.of(request.endHourOfDay, 0))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

            var candidateStart = maxOf(windowStart, nowEpochMillis + (15 * 60 * 1000L))
            val targetSlotDurationMillis = request.preferredSessionLengthMinutes * 60 * 1000L

            while (candidateStart + targetSlotDurationMillis <= windowEnd) {
                val candidateEnd = candidateStart + targetSlotDurationMillis

                // Check conflict with existing occupied blocks
                val conflict = occupiedBlocks.any { occupied ->
                    val bufferedStart = occupied.startEpochMillis - (request.bufferMinutes * 60 * 1000L)
                    val bufferedEnd = occupied.endEpochMillis + (request.bufferMinutes * 60 * 1000L)
                    maxOf(candidateStart, bufferedStart) < minOf(candidateEnd, bufferedEnd)
                }

                if (!conflict) {
                    val isFirstSlot = suggestions.isEmpty()
                    suggestions.add(
                        StudySlotSuggestion(
                            startEpochMillis = candidateStart,
                            endEpochMillis = candidateEnd,
                            durationMinutes = request.preferredSessionLengthMinutes,
                            subjectId = request.subjectId,
                            topicId = request.topicId,
                            reason = if (isFirstSlot) "Earliest optimal slot matching availability" else "Balanced study interval with buffer",
                            isRecommended = isFirstSlot
                        )
                    )
                    // Move past this suggested slot + buffer
                    candidateStart = candidateEnd + (request.bufferMinutes * 60 * 1000L)
                } else {
                    // Advance candidate start by 30 mins to search next candidate window
                    candidateStart += 30 * 60 * 1000L
                }

                if (suggestions.size >= 5) break
            }

            if (suggestions.size >= 5) break
            currentDate = currentDate.plusDays(1)
        }

        return suggestions
    }
}

