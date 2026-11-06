package com.example.camp_serve.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * DateUtils.kt holds small date helpers shared by the lottery, reservation and alert screens.
 * Lottery periods are whole days, so most helpers work with the start of a day.
 * Outstanding issues: dates use the phone's time zone, not the campsite's.
 */

private const val INPUT_PATTERN = "yyyy-MM-dd"

/** Returns [date] moved back to 00:00 of the same day. */
fun startOfDay(date: Date): Date {
    val cal = Calendar.getInstance()
    cal.time = date
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

/** Returns 00:00 of the day after [date]. */
fun dayAfter(date: Date): Date = addDays(startOfDay(date), 1)

/** Returns [date] moved by [days] days (negative goes back). */
fun addDays(date: Date, days: Int): Date {
    val cal = Calendar.getInstance()
    cal.time = date
    cal.add(Calendar.DAY_OF_MONTH, days)
    return cal.time
}

/** Formats a date for the text boxes, like 2026-06-09. */
fun formatInputDate(date: Date): String =
    SimpleDateFormat(INPUT_PATTERN, Locale.US).format(date)

/** Reads a date typed as yyyy-MM-dd. Returns null if it isn't a real date. */
fun parseInputDate(text: String): Date? {
    val trimmed = text.trim()
    if (trimmed.length != INPUT_PATTERN.length) return null
    val format = SimpleDateFormat(INPUT_PATTERN, Locale.US)
    format.isLenient = false
    return try {
        format.parse(trimmed)?.let { startOfDay(it) }
    } catch (e: ParseException) {
        null
    }
}

/** Formats a date for display, like Jun 9, 2026. */
fun formatDisplayDate(date: Date): String =
    SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)

/** Formats a date and time for display, like Jun 9, 2026 3:05 PM. */
fun formatDisplayDateTime(date: Date): String =
    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(date)

/** Short "time ago" text, like "5m ago", "2h ago" or "3d ago". */
fun timeAgo(date: Date, now: Date = Date()): String {
    val minutes = (now.time - date.time) / 60000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

/** Number of nights between two dates (at least 1 for a real stay). */
fun nightsBetween(start: Date, end: Date): Int {
    val days = (startOfDay(end).time - startOfDay(start).time) / (24L * 60 * 60 * 1000)
    return days.toInt()
}
