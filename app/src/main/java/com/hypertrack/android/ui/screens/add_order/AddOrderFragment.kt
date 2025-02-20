package com.hypertrack.android.ui.screens.add_order

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.hypertrack.android.ui.common.select_destination.SelectDestinationFragment
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_select_destination.*

open class AddOrderFragment : SelectDestinationFragment() {

    private val args: AddOrderFragmentArgs by navArgs()

    protected override val vm: AddOrderViewModel by viewModels {
        MyApplication.injector.provideParamVmFactory(args.tripId)
    }

}