package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.location.Address
import android.util.Log
import android.view.View
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.Location
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.delegates.GeofenceAddressDelegate
import com.hypertrack.android.ui.screens.place_details.PlaceVisitsAdapter
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TimeDistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place.view.*
import kotlinx.android.synthetic.main.item_place.view.tvTitle
import kotlinx.android.synthetic.main.item_place_visit.view.*
import java.time.ZonedDateTime


class AllPlacesVisitsAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val timeDistanceFormatter: TimeDistanceFormatter,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<GeofenceMarker, BaseAdapter.BaseVh<GeofenceMarker>>() {

    override val itemLayoutResource: Int = R.layout.item_place_visit

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseAdapter.BaseVh<GeofenceMarker> {
        return object : BaseContainerVh<GeofenceMarker>(view, baseClickListener) {
            override fun bind(item: GeofenceMarker) {
                PlaceVisitsAdapter.bindVisit(
                    containerView,
                    item,
                    timeDistanceFormatter,
                    osUtilsProvider,
                    onCopyClickListener
                )
            }
        }
    }
}

