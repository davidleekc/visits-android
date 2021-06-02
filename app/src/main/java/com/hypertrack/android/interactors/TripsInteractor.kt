package com.hypertrack.android.interactors

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.models.*
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.models.local.TripStatus
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.toHotTransformation
import com.hypertrack.android.ui.common.toMap
import com.hypertrack.android.ui.screens.visits_management.tabs.livemap.SearchPlacePresenter
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.ImageDecoder
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException
import java.util.*

interface TripsInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val currentTrip: LiveData<LocalTrip?>
    val completedTrips: LiveData<List<LocalTrip>>
    fun getOrderLiveData(orderId: String): LiveData<LocalOrder>
    suspend fun refreshTrips()
    suspend fun cancelOrder(orderId: String): OrderCompletionResponse
    suspend fun completeOrder(orderId: String): OrderCompletionResponse
    fun getOrder(orderId: String): LocalOrder?
    suspend fun updateOrderNote(orderId: String, orderNote: String)
    suspend fun setOrderPickedUp(orderId: String)
    suspend fun addPhotoToOrder(orderId: String, path: String)
    fun retryPhotoUpload(orderId: String, photoId: String)
    suspend fun createTrip(latLng: LatLng): TripCreationResult
}

class TripsInteractorImpl(
    private val tripsRepository: TripsRepository,
    private val apiClient: ApiClient,
    private val hyperTrackService: HyperTrackService,
    private val photoUploadInteractor: PhotoUploadQueueInteractor,
    private val imageDecoder: ImageDecoder,
    private val osUtilsProvider: OsUtilsProvider,
    private val ioDispatcher: CoroutineDispatcher
) : TripsInteractor {

    override val completedTrips = Transformations.map(tripsRepository.trips) {
        it.filter { it.status != TripStatus.ACTIVE }
    }

    override val currentTrip = Transformations.map(tripsRepository.trips) {
        getCurrentTrip(it)
    }.toHotTransformation().liveData


    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun getOrderLiveData(orderId: String): LiveData<LocalOrder> {
        return Transformations.switchMap(tripsRepository.trips) {
            MutableLiveData<LocalOrder>().apply {
                getOrder(orderId).let { if (it != null) postValue(it) }
            }
        }
    }

    override suspend fun refreshTrips() {
        tripsRepository.refreshTrips()
    }

    override fun getOrder(orderId: String): LocalOrder? {
        return tripsRepository.trips.value?.map { it.orders }?.flatten()
            ?.firstOrNull() { it.id == orderId }
    }

    override suspend fun completeOrder(orderId: String): OrderCompletionResponse {
        return setOrderCompletionStatus(orderId, canceled = false)
    }

    override suspend fun cancelOrder(orderId: String): OrderCompletionResponse {
        return setOrderCompletionStatus(orderId, canceled = true)
    }

    override suspend fun updateOrderNote(orderId: String, orderNote: String) {
        try {
            tripsRepository.updateLocalOrder(orderId) {
                it.note = orderNote
            }
        } catch (e: Exception) {
            errorFlow.emit(Consumable(e))
        }
    }

    override suspend fun setOrderPickedUp(orderId: String) {
        //used only for legacy orders, so orderId is tripId
        hyperTrackService.sendPickedUp(orderId, "trip_id")
        tripsRepository.updateLocalOrder(orderId) {
            it.isPickedUp = true
        }
    }

    override suspend fun addPhotoToOrder(orderId: String, path: String) {
        try {
            val generatedImageId = UUID.randomUUID().toString()

            val previewMaxSideLength: Int = (200 * osUtilsProvider.screenDensity).toInt()
            withContext(ioDispatcher) {
                val bitmap = imageDecoder.readBitmap(path, previewMaxSideLength)
                val photo = PhotoForUpload(
                    photoId = generatedImageId,
                    filePath = path,
                    base64thumbnail = osUtilsProvider.bitmapToBase64(bitmap),
                    state = PhotoUploadingState.NOT_UPLOADED
                )
                tripsRepository.updateLocalOrder(orderId) {
//                it.photos.add(photo.photoId)
                    it.photos.add(photo)
                }
                photoUploadInteractor.addToQueue(photo)
            }
        } catch (e: Exception) {
            errorFlow.emit(Consumable(e))
        }
    }

    override fun retryPhotoUpload(orderId: String, photoId: String) {
        photoUploadInteractor.retry(photoId)
    }

    override suspend fun createTrip(latLng: LatLng): TripCreationResult {
        return tripsRepository.createTrip(latLng)
    }

    private suspend fun setOrderCompletionStatus(
        orderId: String,
        canceled: Boolean
    ): OrderCompletionResponse {
        try {
            currentTrip.value!!.let { trip ->
                trip.getOrder(orderId)!!.let { order ->
                    if (order.legacy) {
                        //legacy v1 trip, destination is order, order.id is trip.id
                        hyperTrackService.sendCompletionEvent(order, canceled)
                        tripsRepository.updateLocalOrder(orderId) {
                            it.status = if (!canceled) {
                                OrderStatus.COMPLETED
                            } else {
                                OrderStatus.CANCELED
                            }
                        }
                        return OrderCompletionSuccess
                        //todo completion is disabled regarding to Indiabulls use-case
//                        val res = apiClient.completeTrip(order.id)
//                        when (res) {
//                            TripCompletionSuccess -> {
//                                updateCurrentTripOrderStatus(
//                                    orderId, if (!canceled) {
//                                        OrderStatus.COMPLETED
//                                    } else {
//                                        OrderStatus.CANCELED
//                                    }
//                                )
//                                return OrderCompletionSuccess
//                            }
//                            is TripCompletionError -> {
//                                return OrderCompletionFailure(res.error as Exception)
//                            }
//                        }
                    } else {
                        val mdRes = apiClient.updateOrderMetadata(
                            orderId = orderId,
                            tripId = trip.id,
                            metadata = (order._metadata ?: Metadata.empty()).apply {
                                visitsAppMetadata.note = order.note
                                visitsAppMetadata.photos = order.photos.map { it.photoId }
                            }
                        )
                        if (!mdRes.isSuccessful) {
                            throw HttpException(mdRes)
                        }

                        val res = if (!canceled) {
                            apiClient.completeOrder(orderId = orderId, tripId = trip.id)
                        } else {
                            apiClient.cancelOrder(orderId = orderId, tripId = trip.id)
                        }
                        if (res is OrderCompletionSuccess) {
                            tripsRepository.updateLocalOrder(orderId) {
                                it.status = if (!canceled) {
                                    OrderStatus.COMPLETED
                                } else {
                                    OrderStatus.CANCELED
                                }
                            }
                        }
                        return res
                    }
                }
            }
        } catch (e: Exception) {
            return OrderCompletionFailure(e)
        }
    }

    private fun getCurrentTrip(trips: List<LocalTrip>): LocalTrip? {
        return trips.firstOrNull {
            it.status == TripStatus.ACTIVE
        }
    }

}


