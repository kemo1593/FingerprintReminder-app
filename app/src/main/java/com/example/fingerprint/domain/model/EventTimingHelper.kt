package com.example.fingerprint.domain.model

object EventTimingHelper {

    fun getAlarmTriggerTime(eventType: EventType, scheduledTimeMillis: Long, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        return when (eventType) {
            EventType.IN -> {
                val pre15m = scheduledTimeMillis - (15 * 60 * 1000L)
                if (currentTimeMillis in pre15m..scheduledTimeMillis) {
                    currentTimeMillis + 500L
                } else if (currentTimeMillis < pre15m) {
                    pre15m
                } else {
                    scheduledTimeMillis
                }
            }
            EventType.OUT_IN, EventType.OUT -> scheduledTimeMillis // Exact time
        }
    }

    fun getLateDurationMillis(
        eventType: EventType,
        scheduledTimeMillis: Long,
        completedTimeMillis: Long,
        graceMinutesIn: Int = 0,
        graceMinutesOutIn: Int = 15,
        graceMinutesOut: Int = 30
    ): Long? {
        val graceMinutes = when (eventType) {
            EventType.IN -> graceMinutesIn
            EventType.OUT_IN -> graceMinutesOutIn
            EventType.OUT -> graceMinutesOut
        }
        val graceThresholdMillis = scheduledTimeMillis + (graceMinutes * 60 * 1000L) + (60 * 1000L) // Exceeds grace period

        if (completedTimeMillis < graceThresholdMillis) return null

        return completedTimeMillis - (scheduledTimeMillis + graceMinutes * 60 * 1000L)
    }

    fun formatLateLabel(lateMillis: Long, isArabic: Boolean): String {
        val totalMinutes = (lateMillis / (1000 * 60)).coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (isArabic) {
            if (hours > 0) {
                if (minutes > 0) "تأخير $hours ساعة و $minutes دقيقة"
                else "تأخير $hours ساعة"
            } else {
                "تأخير $minutes دقيقة"
            }
        } else {
            if (hours > 0) {
                if (minutes > 0) "Late $hours hr $minutes mins"
                else "Late $hours hr"
            } else {
                if (minutes == 1L) "Late 1 minute" else "Late $minutes minutes"
            }
        }
    }

    fun isEventActionable(
        eventType: EventType,
        scheduledTimeMillis: Long,
        status: EventStatus,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (status == EventStatus.DONE) return false
        if (status == EventStatus.SNOOZED) return true

        return when (eventType) {
            EventType.IN -> {
                // IN fingerprint becomes actionable 15 minutes before the scheduled time
                val windowStartTime = scheduledTimeMillis - (15 * 60 * 1000L)
                currentTimeMillis >= windowStartTime
            }
            EventType.OUT_IN, EventType.OUT -> {
                // OUT_IN and OUT fingerprints become actionable at the exact scheduled time
                currentTimeMillis >= scheduledTimeMillis
            }
        }
    }
}
