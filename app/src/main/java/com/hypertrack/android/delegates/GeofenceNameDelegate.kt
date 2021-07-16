package com.hypertrack.android.delegates

import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.common.formatDateTime
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class GeofenceNameDelegate(private val osUtilsProvider: OsUtilsProvider) {

    fun getName(localGeofence: LocalGeofence): String {
        return localGeofence.name
            ?: localGeofence.address
            ?: osUtilsProvider.stringFromResource(
                R.string.places_created,
                localGeofence.createdAt.formatDateTime()
            )
    }

}