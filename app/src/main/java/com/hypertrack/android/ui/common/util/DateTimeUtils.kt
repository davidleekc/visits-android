package com.hypertrack.android.ui.common.util

import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.datetimeFromString
import com.hypertrack.logistics.android.github.R
import java.lang.Math.abs
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

object DateTimeUtils {
    fun secondsToLocalizedString(_totalSeconds: Int): String {
        abs(_totalSeconds).let { totalSeconds ->
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) {
                return MyApplication.context.getString(R.string.duration, hours, minutes)
            } else {
                return MyApplication.context.getString(R.string.duration_minutes, minutes)
            }
        }
    }
}

fun String.formatDateTime(): String {
    return datetimeFromString(this)
        .formatDateTime()
}

fun ZonedDateTime.formatDateTime(): String {
    return format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault())) + ", " +
            format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}

//todo to time distance formatter
fun ZonedDateTime.formatDate(): String {
    return format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault()))
}

fun LocalDate.format(): String {
    return format(createFormatterWithoutYear(FormatStyle.MEDIUM, Locale.getDefault()))
}

private fun createFormatterWithoutYear(
    style: FormatStyle,
    locale: Locale
): DateTimeFormatter {
    try {
        var pattern: String = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            style, null, Chronology.ofLocale(locale), locale
        )
        pattern = pattern.replaceFirst("\\P{IsLetter}+[Yy]+".toRegex(), "")
        pattern = pattern.replaceFirst("^[Yy]+\\P{IsLetter}+".toRegex(), "")
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return formatter
    } catch (e: Exception) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}