package com.hypertrack.android.ui.screens.select_trip_destination

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hypertrack.android.ui.common.select_destination.SelectDestinationFragment
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.add_order_info.AddOrderInfoViewModel
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_select_destination.*

class SelectTripDestinationFragment : SelectDestinationFragment() {

    override val vm: SelectTripDestinationViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

}