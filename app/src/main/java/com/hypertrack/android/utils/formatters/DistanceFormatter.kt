package com.hypertrack.android.utils.formatters

import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import java.time.ZoneId
import java.util.*

interface DistanceFormatter {
    fun formatDistance(meters: Int): String
}

class LocalizedDistanceFormatter(
    private val osUtilsProvider: OsUtilsProvider,
) : DistanceFormatter {
    private val shouldUseImperial = Locale.getDefault().country in listOf("US", "LR", "MM")

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
}

class MetersDistanceFormatter(val osUtilsProvider: OsUtilsProvider) : DistanceFormatter {
    override fun formatDistance(meters: Int): String {
        return osUtilsProvider.stringFromResource(
            R.string.meters,
            meters
        )
    }
}