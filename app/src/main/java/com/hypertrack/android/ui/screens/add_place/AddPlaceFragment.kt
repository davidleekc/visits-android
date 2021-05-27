package com.hypertrack.android.ui.screens.add_place

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.Utils.isDoneAction
import com.hypertrack.android.ui.screens.select_destination.SelectDestinationFragment
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_add_place.*

open class AddPlaceFragment : SelectDestinationFragment() {

    protected override val vm: AddPlaceViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.add_place)
    }

}