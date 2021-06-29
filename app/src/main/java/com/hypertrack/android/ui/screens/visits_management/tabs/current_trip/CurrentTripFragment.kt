package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.screens.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersAdapter
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_current_trip.*
import kotlinx.android.synthetic.main.inflate_current_trip.*
import kotlinx.android.synthetic.main.progress_bar.*
import java.time.format.DateTimeFormatter
import java.util.*

class CurrentTripFragment : ProgressDialogFragment(R.layout.fragment_current_trip) {

    private lateinit var bottomHolderSheetBehavior: BottomSheetBehavior<*>

    private val vm: CurrentTripViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }
    private val timeDistanceFormatter = Injector.getTimeDistanceFormatter()

    private val ordersAdapter = OrdersAdapter(
        timeDistanceFormatter,
        showStatus = false
    )

    private lateinit var map: GoogleMap

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<DestinationData>(KEY_DESTINATION)
            ?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    vm.onDestinationResult(it)
                    findNavController().currentBackStackEntry?.savedStateHandle
                        ?.set(KEY_DESTINATION, null)
                }
            }

        (childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment?)?.getMapAsync {
            map = it
            vm.onMapReady(requireContext(), it)
        }

        bottomHolderSheetBehavior = BottomSheetBehavior.from(bottom_holder)
        bottom_holder.show()
        val bottomHolder = bottom_holder
        recycler_view.setLinearLayoutManager(requireContext())
        recycler_view.adapter = ordersAdapter.apply {
            onItemClickListener = {
                vm.onOrderClick(it.id)
            }
        }

        bottomHolder.setOnClickListener {
            if (bottomHolderSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomHolderSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            } else {
                bottomHolderSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        vm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        vm.trip.observe(viewLifecycleOwner, {
            whereAreYouGoing.setGoneState(it != null)
            lTrip.setGoneState(it == null)
            it?.let { displayTrip(it) }
        })

        vm.errorText.observe(viewLifecycleOwner, {
            it.consume {
                SnackbarUtil.showErrorSnackbar(view, it)
            }
        })

        vm.loadingStateBase.observe(viewLifecycleOwner, {
            Log.v("hypertrack-verbose", "fragment $it")
            whereAreYouGoing.setGoneState(it)
            progress.setGoneState(!it)
            if (it) {
                loader.playAnimation()
            } else {
                loader.cancelAnimation()
            }
        })

        whereAreYouGoing.setOnClickListener {
            vm.onWhereAreYouGoingClick()
        }

        shareButton.setOnClickListener {
            vm.onShareTripClick()
        }

        bAddOrder.setOnClickListener {
            vm.onAddOrderClick()
        }

        endTripButton.setGoneState(BuildConfig.DEBUG.not())
        endTripButton.setOnClickListener {
            vm.onCompleteClick()
        }
    }

    //todo to vm
    private fun displayTrip(trip: LocalTrip) {
        trip.nextOrder?.let { order ->
            order.shortAddress.toView(destination_address)
            (order.eta?.let {
                timeDistanceFormatter.formatTime(it.format(DateTimeFormatter.ISO_INSTANT))
            } ?: R.string.orders_list_eta_unavailable.stringFromResource()).toView(
                destination_arrival
            )
            order.awaySeconds.let { seconds ->
                listOf(destination_away, destination_away_title).forEach {
                    it.setGoneState(seconds == null)
                }
                seconds?.let {
                    DateTimeUtils.secondsToLocalizedString(seconds.toInt()).toView(destination_away)
                }
            }
        }

        trip.ongoingOrgers.let { orders ->
            trips_count.setGoneState(orders.isEmpty())
            recycler_view.setGoneState(orders.isEmpty())
            if (orders.size > 0) {
                val text = getString(R.string.you_have_ongoing_orders)
                val plural = resources.getQuantityString(R.plurals.order, orders.size)
                trips_count!!.text = String.format(text, orders.size, plural)
            }
            ordersAdapter.updateItems(orders)
        }
    }



    companion object {
        const val KEY_DESTINATION = "destination"
    }

}