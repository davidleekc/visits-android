package com.hypertrack.android.utils.injection

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.hypertrack.android.ui.screens.visits_management.tabs.livemap.LiveMapFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.livemap.SearchPlaceFragment
import com.hypertrack.android.ui.screens.visits_management.tabs.livemap.TrackingFragment
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.backend.AbstractBackendProvider
import com.hypertrack.sdk.views.HyperTrackViews
import javax.inject.Provider


class CustomFragmentFactory(
    private val mapStyleOptions: MapStyleOptions,
    private val mapStyleOptionsSilver: MapStyleOptions,
    private val hyperTrackServiceProvider: Provider<HyperTrackService>,
    private val hyperTrackViewsProvider: Provider<HyperTrackViews>,
    private val backendProvider: Provider<AbstractBackendProvider>,
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            LiveMapFragment::class.java.name -> LiveMapFragment(
                mapStyleOptions,
                mapStyleOptionsSilver,
                hyperTrackServiceProvider.get()
            )
            TrackingFragment::class.java.name ->
                TrackingFragment(backendProvider.get(), hyperTrackServiceProvider.get(), hyperTrackViewsProvider.get())
            SearchPlaceFragment::class.java.name ->
                SearchPlaceFragment(
                    backendProvider.get(),
                    hyperTrackServiceProvider.get().deviceId,
                    hyperTrackViewsProvider.get()
                )
            else -> super.instantiate(classLoader, className)
        }
    }
}