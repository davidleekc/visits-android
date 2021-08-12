package com.hypertrack.android.utils

import com.hypertrack.logistics.android.github.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.round

//todo separate and merge with DateTimeFormatter
interface TimeDistanceFormatter {
    /** 2020-02-02T20:02:02.000Z -> 20:02 or 8:02pm adjusted to device's timezone */
    fun formatTime(isoTimestamp: String): String

    /** 2400 meters -> 2.4 km or 1.5 mi */
    fun formatDistance(meters: Int): String
}

open class SimpleTimeDistanceFormatter(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : TimeDistanceFormatter {

    private val format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    protected val shouldUseImperial = Locale.getDefault().country in listOf("US", "LR", "MM")

    override fun formatTime(isoTimestamp: String): String {
        return try {
            Instant.parse(isoTimestamp).atZone(zoneId).toLocalTime().format(format)
                .replace(" PM", "pm").replace(" AM", "am")
        } catch (ignored: Exception) {
            isoTimestamp
        }
    }

    override fun formatDistance(meters: Int): String {
        if (shouldUseImperial) {
            val miles = meters / 1609.0
            return "${"%.1f".format(miles)} mi"
        }
        val kms = meters / 1000.0
        return "${"%.1f".format(kms)} km"
    }

    companion object {
        const val TAG = "TimeDistanceFormatter"
    }
}

class LocalizedTimeDistanceFormatter(
    private val osUtilsProvider: OsUtilsProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : SimpleTimeDistanceFormatter(zoneId) {

    override fun formatDistance(meters: Int): String {
        return if (shouldUseImperial && false) {
            val miles = meters / 1609.0
            when {
                miles >= 100 -> {
                    osUtilsProvider.stringFromResource(
                        R.string.miles,
                        "%.0f".format(miles.toFloat())
                    )
                }
                miles >= 1 -> {
                    osUtilsProvider.stringFromResource(
                        R.string.miles,
                        "%.1f".format(miles.toFloat())
                    )
                }
                else -> {
                    val feet = 0.3048 * meters
                    osUtilsProvider.stringFromResource(R.string.feet, "%.0f".format(feet.toFloat()))
                }
            }
        } else {
            val kms = meters / 1000.0
            when {
                kms >= 100 -> {
                    osUtilsProvider.stringFromResource(R.string.kms, "%.0f".format(kms.toFloat()))
                }
                kms >= 1 -> {
                    osUtilsProvider.stringFromResource(R.string.kms, "%.1f".format(kms.toFloat()))
                }
                else -> {
                    osUtilsProvider.stringFromResource(
                        R.string.meters,
                        "%.0f".format(meters.toFloat())
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "TimeDistanceFormatter"
    }
}