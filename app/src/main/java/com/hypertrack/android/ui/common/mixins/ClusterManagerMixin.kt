package com.hypertrack.android.ui.common.mixins

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.screens.add_place.AddPlaceViewModel
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import net.sharewire.googlemapsclustering.Cluster
import net.sharewire.googlemapsclustering.ClusterItem
import net.sharewire.googlemapsclustering.ClusterManager
import net.sharewire.googlemapsclustering.IconGenerator

interface ClusterManagerMixin<T : ClusterItem> {

    fun createClusterManager(
        context: Context,
        googleMap: GoogleMap,
        icon: BitmapDescriptor,
        clusterIcon: BitmapDescriptor,
        onMarkerClickListener: (T) -> Unit
    ): ClusterManager<T> {
        val clusterManager = ClusterManager<T>(context, googleMap).apply {
            setMinClusterSize(10)
            setIconGenerator(object : IconGenerator<T> {
                override fun getClusterIcon(cluster: Cluster<T>): BitmapDescriptor {
                    return clusterIcon
                }

                override fun getClusterItemIcon(clusterItem: T): BitmapDescriptor {
                    return icon
                }
            })
        }

        clusterManager.setCallbacks(object : ClusterManager.Callbacks<T> {
            override fun onClusterClick(cluster: Cluster<T>): Boolean {
                //todo
                return true
            }

            override fun onClusterItemClick(clusterItem: T): Boolean {
                onMarkerClickListener.invoke(clusterItem)
                return true
            }
        })

        return clusterManager
    }

}

open class GeofenceClusterItem(
    private val geofence: LocalGeofence
) : ClusterItem {

    override fun getLatitude(): Double = geofence.latLng.latitude

    override fun getLongitude(): Double = geofence.latLng.longitude

    override fun getTitle() = geofence.name

    override fun getSnippet() = geofence.id

    companion object {
        fun createIcon(osUtilsProvider: OsUtilsProvider): BitmapDescriptor {
            return BitmapDescriptorFactory.fromBitmap(
                osUtilsProvider.bitmapFormResource(
                    R.drawable.ic_ht_departure_active
                )
            )
        }

        fun createClusterIcon(osUtilsProvider: OsUtilsProvider): BitmapDescriptor {
            return BitmapDescriptorFactory.fromBitmap(
                osUtilsProvider.bitmapFormResource(
                    R.drawable.ic_cluster
                )
            )
        }
    }
}