package com.hypertrack.android.utils

import com.hypertrack.logistics.android.github.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.round

//todo separate and merge with DateTimeFormatter

interface TimeFormatter {
    fun formatTime(isoTimestamp: String): String
}

interface DistanceFormatter {
    fun formatDistance(meters: Int): String
}

//todo split usage
interface TimeDistanceFormatter : TimeFormatter, DistanceFormatter

open class SimpleTimeDistanceFormatter(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : TimeDistanceFormatter {

    private val format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    protected open val shouldUseImperial = Locale.getDefault().country in listOf("US", "LR", "MM")

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

class MetersDistanceFormatter(val osUtilsProvider: OsUtilsProvider) : DistanceFormatter {
    override fun formatDistance(meters: Int): String {
        return osUtilsProvider.stringFromResource(
            R.string.meters,
            meters
        )
    }
}