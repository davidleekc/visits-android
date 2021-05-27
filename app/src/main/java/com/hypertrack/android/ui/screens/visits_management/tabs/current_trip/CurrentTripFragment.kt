package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.SnackbarUtil
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoFragment
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_current_trip.*

class CurrentTripFragment : ProgressDialogFragment(R.layout.fragment_current_trip) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>(KEY_DESTINATION)
            ?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    SnackbarUtil.showErrorSnackbar(view, result)
                }
            }

        whereAreYouGoing.setOnClickListener {
            findNavController().navigate(
                VisitsManagementFragmentDirections
                    .actionVisitManagementFragmentToSelectDestinationFragment()
            )
        }
    }

    companion object {
        const val KEY_DESTINATION = "destination"
    }
}