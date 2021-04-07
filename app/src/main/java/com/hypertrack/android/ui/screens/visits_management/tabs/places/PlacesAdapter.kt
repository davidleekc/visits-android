package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.View
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.common.toView
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place.view.*
import kotlinx.android.synthetic.main.item_spinner.view.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class PlacesAdapter(val osUtilsProvider: OsUtilsProvider) :
    BaseAdapter<PlaceItem, BaseAdapter.BaseVh<PlaceItem>>() {

    override val itemLayoutResource: Int = R.layout.item_place

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseAdapter.BaseVh<PlaceItem> {
        return object : BaseContainerVh<PlaceItem>(view, baseClickListener) {
            override fun bind(item: PlaceItem) {
                (item.geofence.visitsCount).let {
                    if (it > 0) {
                        val timesString =
                            MyApplication.context.resources.getQuantityString(R.plurals.time, it)

                        "${
                            MyApplication.context.getString(
                                R.string.places_visited,
                                it.toString()
                            )
                        } $timesString"
                            .toView(containerView.tvVisited)
                    } else {
                        containerView.tvVisited.setText(R.string.places_not_visited)
                    }
                }
                (item.geofence.name
                    ?: item.geofence.address?.street
                    ?: item.geofence.created_at.let {
                        ZonedDateTime.parse(it)
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                    }).toView(containerView.tvTitle)

                var address: String? = null
                item.geofence.metadata?.get("address").let {
                    if (it is String && it.isNotBlank()) {
                        address = it
                    }
                }
                if (address == null) {
                    address = item.geofence.address?.let {
                        "${it.city}, ${it.street}"
                    }
                }
                if (address == null) {
                    address = osUtilsProvider.getPlaceFromCoordinates(
                        item.geofence.geometry.latitude,
                        item.geofence.geometry.longitude,
                    )?.toAddressString()
                }
                (address
                    ?: "${item.geofence.geometry.latitude} ${item.geofence.geometry.longitude}").toView(
                    containerView.tvAddress
                )
            }
        }
    }
}

class PlaceItem(
    val geofence: Geofence
)