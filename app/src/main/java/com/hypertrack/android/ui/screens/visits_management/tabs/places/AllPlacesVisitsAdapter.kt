package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.ui.base.BaseAdapter
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.screens.place_details.PlaceVisitsAdapter
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TimeDistanceFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.item_place.view.tvTitle
import java.time.LocalDate
import java.time.ZonedDateTime


class AllPlacesVisitsAdapter(
    private val osUtilsProvider: OsUtilsProvider,
    private val timeDistanceFormatter: TimeDistanceFormatter,
    private val onCopyClickListener: ((String) -> Unit)
) : BaseAdapter<VisitItem, BaseAdapter.BaseVh<VisitItem>>() {

    override val itemLayoutResource: Int = R.layout.item_place_visit_all_places

    fun addVisits(visits: List<GeofenceVisit>) {
        updateVisits(
            items.filterIsInstance<Visit>().map { it.visit }.toMutableList()
                .apply { addAll(visits) })
    }

    private fun updateVisits(visits: List<GeofenceVisit>) {
        val res = mutableListOf<VisitItem>()
        if (visits.isNotEmpty()) {
            var currentDay: LocalDate? = null
            for (visit in visits) {
                if (visit.getDay() != currentDay) {
                    currentDay = visit.getDay()
                    res.add(Day(currentDay))
                }
                res.add(Visit(visit))
            }
        }
        updateItems(res)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Day -> Day::class.java.hashCode()
            is Visit -> Visit::class.java.hashCode()
            else -> throw IllegalStateException("position ${position} ${items[position]}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVh<VisitItem> {
        return createViewHolder(
            LayoutInflater.from(parent.context).inflate(
                when (viewType) {
                    Day::class.java.hashCode() -> R.layout.item_day
                    Visit::class.java.hashCode() -> itemLayoutResource
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
                    }
                    is Visit -> {
                        PlaceVisitsAdapter.bindVisit(
                            containerView,
                            item.visit,
                            timeDistanceFormatter,
                            osUtilsProvider,
                            onCopyClickListener
                        )
                    }
                }
            }
        }
    }
}

fun GeofenceVisit.getDay(): LocalDate {
    return if (exit != null) {
        ZonedDateTime.parse(exit.recordedAt).toLocalDate()
    } else {
        ZonedDateTime.parse(arrival!!.recordedAt).toLocalDate()
    }
}

sealed class VisitItem
class Visit(val visit: GeofenceVisit) : VisitItem()
class Day(val date: LocalDate) : VisitItem()

