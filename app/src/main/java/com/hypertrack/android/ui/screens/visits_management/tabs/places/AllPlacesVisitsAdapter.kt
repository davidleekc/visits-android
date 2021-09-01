package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hypertrack.android.delegates.GeofenceNameDelegate
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.delegates.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.util.format
import com.hypertrack.android.ui.screens.place_details.PlaceVisitsAdapter
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TimeDistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_day.view.*
import kotlinx.android.synthetic.main.item_place.view.tvTitle
import kotlinx.android.synthetic.main.item_place_visit_all_places.view.*


class AllPlacesVisitsAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val timeDistanceFormatter: TimeDistanceFormatter,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<VisitItem, BaseAdapter.BaseVh<VisitItem>>() {

    private val nameDelegate = GeofenceNameDelegate(osUtilsProvider)
    private val addressDelegate = GeofenceAddressDelegate(osUtilsProvider)

    override val itemLayoutResource: Int = R.layout.item_place_visit_all_places

    private var visitsData: VisitsData = VisitsData(listOf(), mapOf())

    fun updateData(_visitsData: VisitsData) {
        visitsData = _visitsData
        updateItems(visitsData.adapterData)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Day -> Day::class.java.hashCode()
            is Visit -> Visit::class.java.hashCode()
//            is MonthItem -> MonthItem::class.java.hashCode()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVh<VisitItem> {
        return createViewHolder(
            LayoutInflater.from(parent.context).inflate(
                when (viewType) {
                    Day::class.java.hashCode() -> R.layout.item_day
                    Visit::class.java.hashCode() -> itemLayoutResource
//                    MonthItem::class.java.hashCode() -> R.layout.item_month
                    else -> throw IllegalStateException("viewType ${viewType}")
                },
                parent,
                false
            )
        ) { position ->
            if (items[position] is Visit) {
                onItemClickListener?.invoke(items[position])
            }
        }
    }

    override fun createViewHolder(
        view: View,
        baseClickListener: (Int) -> Unit
    ): BaseAdapter.BaseVh<VisitItem> {
        return object : BaseContainerVh<VisitItem>(view, baseClickListener) {
            override fun bind(item: VisitItem) {
                when (item) {
                    is Day -> {
                        containerView.tvTitle.text = item.date.format()
                        containerView.tvTotal.text = visitsData.dayStats[item.date]?.let {
                            timeDistanceFormatter.formatDistance(it)
                        } ?: osUtilsProvider.stringFromResource(R.string.places_visits_loading)
                    }
                    is Visit -> {
                        PlaceVisitsAdapter.bindVisit(
                            containerView,
                            item.visit,
                            timeDistanceFormatter,
                            osUtilsProvider,
                            onCopyClickListener
                        )
                        containerView.tvPlaceName.text = nameDelegate.getName(item.visit)
                        containerView.tvPlaceAddress.text = addressDelegate.shortAddress(item.visit)
                    }
//                    is MonthItem -> {
////                        Log.v(
////                            "hypertrack-verbose",
////                            "adapter $item ${visitsData.monthStats} ${visitsData.monthStats}"
////                        )
//                        val monthTotal = visitsData.monthStats[item.month]?.let {
//                            timeDistanceFormatter.formatDistance(it)
//                        }
//                        containerView.tvTitle.text =
//                            item.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
//                        containerView.tvTotal.text = monthTotal
//                            ?: osUtilsProvider.stringFromResource(R.string.places_visits_loading)
//                    }
                }
            }
        }
    }
}



