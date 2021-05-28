package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.screens.select_destination.DestinationData
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_current_trip.*

class CurrentTripFragment : ProgressDialogFragment(R.layout.fragment_current_trip) {

    private lateinit var bottomHolderSheetBehavior: BottomSheetBehavior<*>

    private val vm: CurrentTripViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<DestinationData>(KEY_DESTINATION)
            ?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    vm.onDestinationResult(it)
                }
            }

        bottomHolderSheetBehavior = BottomSheetBehavior.from(bottom_holder)
        bottom_holder.show()
        val bottomHolder = bottom_holder
        recycler_view.setLinearLayoutManager(requireContext())
        recycler_view.adapter = KeyValueAdapter().apply {
            updateItems(
                listOf(
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                    KeyValueItem("a", "b"),
                )
            )
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

        vm.showWhereAreYouGoing.observe(viewLifecycleOwner, {
            whereAreYouGoing.setGoneState(!it)
        })

        vm.trip.observe(viewLifecycleOwner, {
            if (it != null) {
                lTrip.show()
                displayTrip(it)
            } else {
                lTrip.hide()
            }
        })

        whereAreYouGoing.setOnClickListener {
            vm.onWhereAreYouGoingClick()
        }
    }

    private fun displayTrip(trip: LocalTrip) {

    }

    companion object {
        const val KEY_DESTINATION = "destination"
    }

}