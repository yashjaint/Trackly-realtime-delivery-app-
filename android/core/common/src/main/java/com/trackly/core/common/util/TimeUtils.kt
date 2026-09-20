package com.trackly.core.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats duration between start and end timestamps into days, hours, and minutes.
     * Example outputs:
     * - "2 mins total"
     * - "1 hr 15 mins total"
     * - "2 days 4 hrs 30 mins total"
     */
    fun formatDurationInDaysHoursMins(startMs: Long, endMs: Long): String {
        if (endMs <= startMs) return "0 mins total"
        val totalMinutes = (endMs - startMs) / (1000 * 60)
        return "${formatMinutesToDaysHoursMins(totalMinutes)} total"
    }

    /**
     * Formats a raw minute count into days, hours, and minutes.
     * Example outputs:
     * - "25 mins"
     * - "1 hr 15 mins"
     * - "1 day 2 hrs 10 mins"
     */
    fun formatMinutesToDaysHoursMins(totalMinutes: Long): String {
        if (totalMinutes <= 0) return "0 mins"

        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val mins = totalMinutes % 60

        val parts = mutableListOf<String>()
        if (days > 0) {
            parts.add("$days ${if (days == 1L) "day" else "days"}")
        }
        if (hours > 0) {
            parts.add("$hours ${if (hours == 1L) "hr" else "hrs"}")
        }
        if (mins > 0 || parts.isEmpty()) {
            parts.add("$mins ${if (mins == 1L) "min" else "mins"}")
        }

        return parts.joinToString(" ")
    }
}
