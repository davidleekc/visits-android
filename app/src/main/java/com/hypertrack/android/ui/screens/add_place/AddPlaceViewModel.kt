package com.hypertrack.android.ui.screens.add_place

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.screens.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class AddPlaceViewModel(
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val placesInteractor: PlacesInteractor
) : SelectDestinationViewModel(
    osUtilsProvider,
    placesClient,
    deviceLocationProvider
) {
    private lateinit var clusterManager: ClusterManager<GeofenceClusterItem>

    override val loadingStateBase = placesInteractor.isLoadingForLocation

    override val errorBase = MediatorLiveData<Consumable<String>>().apply {
        addSource(placesInteractor.errorFlow.asLiveData()) { e ->
            map.value?.let { onCameraMoved(it) }
            postValue(e.map {
                osUtilsProvider.getErrorMessage(it)
            })
        }
    }

    init {
        viewModelScope.launch {
            placesInteractor.geofencesDiff.collect {
                map.value?.let { map ->
                    addGeofencesToMap(map, it)
                }
            }
        }

//        (placesInteractor as PlacesInteractorImpl).debugCacheState.observeManaged {
//            map.value?.let {
//                showMapDebugData()
//            }
//        }
    }

    private val icon = BitmapDescriptorFactory.fromBitmap(
        osUtilsProvider.bitmapFormResource(
            R.drawable.ic_ht_departure_active
        )
    )

    private val clusterIcon = BitmapDescriptorFactory.fromBitmap(
        osUtilsProvider.bitmapFormResource(
            R.drawable.ic_cluster
        )
    )

    protected override fun proceed(latLng: LatLng, address: String?) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                latLng,
                address = address,
                name = currentPlace?.name
            )
        )
    }


    override fun onMapReady(context: Context, googleMap: GoogleMap) {
        super.onMapReady(context, googleMap)
        clusterManager = ClusterManager<GeofenceClusterItem>(context, googleMap)
        clusterManager.renderer = object :
            DefaultClusterRenderer<GeofenceClusterItem>(context, googleMap, clusterManager) {
            override fun onBeforeClusterItemRendered(
                item: GeofenceClusterItem,
                markerOptions: MarkerOptions
            ) {
                super.onBeforeClusterItemRendered(item, markerOptions)
                markerOptions.icon(icon).anchor(0.5f, 0.5f)
            }

            override fun onBeforeClusterRendered(
                cluster: Cluster<GeofenceClusterItem>,
                markerOptions: MarkerOptions
            ) {
                super.onBeforeClusterRendered(cluster, markerOptions)
                markerOptions.icon(clusterIcon)
            }
        }.apply {
            setAnimation(false)
            minClusterSize = 10
//            minClusterSize = Int.MAX_VALUE
        }

        clusterManager.setOnClusterItemClickListener {
            it.snippet.nullIfEmpty()?.let { snippet ->
                destination.postValue(
                    AddPlaceFragmentDirections.actionAddPlaceFragmentToPlaceDetailsFragment(
                        snippet
                    )
                )
                return@setOnClusterItemClickListener true
            }
            false
        }

        placesInteractor.geofences.value?.let {
            addGeofencesToMap(googleMap, it.values.toList())
        }
    }

    override fun onCameraMoved(map: GoogleMap) {
        val region = map.projection.visibleRegion
        placesInteractor.loadGeofencesForMap(map.cameraPosition.target)
        clusterManager.onCameraIdle()
    }

    private fun addGeofencesToMap(googleMap: GoogleMap, geofences: List<LocalGeofence>) {
        if (BuildConfig.DEBUG.not() || !SHOW_DEBUG_DATA) {
            clusterManager.addItems(geofences.map { GeofenceClusterItem(it) })
            clusterManager.cluster()
        } else {
            googleMap.clear()
            showMapDebugData(googleMap)
            placesInteractor.geofences.value!!.values.forEach {
                googleMap.addMarker(
                    MarkerOptions().icon(
                        icon
                    ).position(it.latLng)
                )
            }
        }
    }


    private fun showMapDebugData(googleMap: GoogleMap) {
        (placesInteractor as PlacesInteractorImpl).debugCacheState.value?.let { items ->
            items.forEach {
                val text = it.let { item ->
                    when (item.status) {
                        PlacesInteractorImpl.Status.COMPLETED -> "completed"
                        PlacesInteractorImpl.Status.LOADING -> item.pageToken
                            ?: "loading"
                        PlacesInteractorImpl.Status.ERROR -> "error"
                    }
                }
                googleMap.addMarker(
                    MarkerOptions().anchor(0.5f, 0.5f).position(
                        it.gh.toLocation().toLatLng()
                    )
                        .icon(createPureTextIcon(text))
                )

                it.gh.boundingBox
                    .let { bb ->
                        listOf(
                            bb.bottomLeft,
                            bb.bottomRight,
                            bb.topRight,
                            bb.topLeft,
                            bb.bottomLeft
                        ).map {
                            it.toLatLng()
                        }
                    }
                    .let {
                        googleMap.addPolygon(PolygonOptions().strokeWidth(1f).addAll(it))
                    }
            }
        }
    }

    private fun createPureTextIcon(text: String?): BitmapDescriptor? {
        val textPaint = Paint().apply {
            textSize = 50f
        } // Adapt to your needs
        val textWidth: Float = textPaint.measureText(text)
        val textHeight: Float = textPaint.getTextSize()
        val width = textWidth.toInt()
        val height = textHeight.toInt()
        val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.translate(0f, height.toFloat())
//        canvas.drawColor(Color.LTGRAY)
        canvas.drawText(text ?: "null", 0f, 0f, textPaint)
        return BitmapDescriptorFactory.fromBitmap(image)
    }

    fun Location.toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    class GeofenceClusterItem(
        private val geofence: LocalGeofence
    ) : ClusterItem {

        override fun getPosition() = geofence.latLng

        override fun getTitle() = geofence.name

        override fun getSnippet() = geofence.id
    }

    companion object {
        const val SHOW_DEBUG_DATA = false
    }

}