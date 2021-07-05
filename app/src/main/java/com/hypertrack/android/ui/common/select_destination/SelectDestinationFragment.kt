package com.hypertrack.android.ui.common.select_destination

import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.Utils.isDoneAction
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripFragment
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_select_destination.*
import kotlinx.android.parcel.Parcelize

//todo merge with add place
open class SelectDestinationFragment :
    ProgressDialogFragment(R.layout.fragment_select_destination) {

    protected open val vm: SelectDestinationViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    private val adapter =
        GooglePlacesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?)?.getMapAsync {
            vm.onMapReady(requireContext(), it)
        }

        toolbar.title = getString(R.string.select_destination)
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mainActivity().supportActionBar!!.setHomeButtonEnabled(true)

        locations.setLinearLayoutManager(requireContext())
        locations.adapter = adapter
        adapter.setOnItemClickListener { adapter, view, position ->
            vm.onPlaceItemClick(this.adapter.getItem(position))
        }

        val watcher = object : SimpleTextWatcher() {
            override fun afterChanged(text: String) {
                vm.onSearchQueryChanged(text)
            }
        }
        search.addTextChangedListener(watcher)
        search.setOnClickListener {
            vm.onSearchQueryChanged(search.textString())
        }
        search.setOnEditorActionListener { v, actionId, event ->
            if (isDoneAction(actionId, event)) {
                Utils.hideKeyboard(requireActivity())
                true
            } else false
        }

        vm.placesResults.observe(viewLifecycleOwner, {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        })

        vm.error.observe(viewLifecycleOwner, {
            Utils.hideKeyboard(mainActivity())
            SnackbarUtil.showErrorSnackbar(view, it)
        })

        vm.address.observe(viewLifecycleOwner, {
            Utils.hideKeyboard(mainActivity())
            search.silentUpdate(watcher, it)
            search.setSelection(search.textString().length)
        })

        vm.destination.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        vm.closeKeyboard.observe(viewLifecycleOwner, {
            Utils.hideKeyboard(requireActivity())
        })

        vm.goBackEvent.observe(viewLifecycleOwner, {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                CurrentTripFragment.KEY_DESTINATION,
                it
            )
            findNavController().popBackStack()
            Utils.hideKeyboard(requireActivity())
        })

        set_on_map.hide()
        destination_on_map.show()
        confirm.show()

        confirm.setOnClickListener {
            vm.onConfirmClicked()
        }
    }

}

@Parcelize
data class DestinationData(
    val latLng: LatLng,
    val address: String? = null,
    val name: String? = null
) : Parcelable