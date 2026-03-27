package com.statushub.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Extension functions and utility methods
 */

// Date formatting
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes min ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hours ago"
        }
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(this))
        }
    }
}

fun Long.toTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

// File size formatting
fun Long.toFileSizeString(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> String.format("%.1f KB", this / 1024.0)
        this < 1024 * 1024 * 1024 -> String.format("%.1f MB", this / (1024.0 * 1024))
        else -> String.format("%.1f GB", this / (1024.0 * 1024 * 1024))
    }
}

// Video duration formatting
fun Long.toDurationString(): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

// Boolean extensions
fun Boolean?.orFalse(): Boolean = this ?: false

// String extensions
fun String?.orEmpty(): String = this ?: ""

fun String.isNumeric(): Boolean = this.all { it.isDigit() }

// Collection extensions
fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

fun <T> MutableList<T>.clearAndAddAll(elements: Collection<T>) {
    clear()
    addAll(elements)
}
