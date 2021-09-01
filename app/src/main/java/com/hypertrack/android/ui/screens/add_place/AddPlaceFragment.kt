package com.hypertrack.android.ui.screens.add_place

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationFragment
import com.hypertrack.android.ui.common.util.SnackbarUtil
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_select_destination.*

open class AddPlaceFragment : SelectDestinationFragment() {

    protected override val vm: AddPlaceViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.add_place)

        vm.loadingStateBase.observe(viewLifecycleOwner, {
            progressbar.setGoneState(!it)
        })

        vm.errorBase.observe(viewLifecycleOwner, {
            it.consume {
                SnackbarUtil.showErrorSnackbar(view, it)
            }
        })

        vm.adjacentGeofenceDialog.observe(viewLifecycleOwner, {
            it.consume {
                createConfirmationDialog(it).show()
            }
        })
    }

    private fun createConfirmationDialog(destinationData: DestinationData): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(
                R.string.add_place_confirm_adjacent.stringFromResource()
            )
            .setPositiveButton(R.string.yes) { dialog, which ->
                vm.proceedCreation(destinationData, true)
            }
            .setNegativeButton(R.string.no, null)
            .create()
    }

}