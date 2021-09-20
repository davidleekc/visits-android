package com.hypertrack.android.interactors

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.ui.common.select_destination.GooglePlaceModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface GooglePlacesInteractor {
    fun createSessionToken(): AutocompleteSessionToken

    suspend fun getPlaces(
        query: String,
        token: AutocompleteSessionToken,
        location: LatLng?
    ): List<GooglePlaceModel>

    suspend fun fetchPlace(
        googlePlaceModel: GooglePlaceModel,
        token: AutocompleteSessionToken
    ): Place
}

class GooglePlacesInteractorImpl(
    private val placesClient: PlacesClient,
) : GooglePlacesInteractor {

    override fun createSessionToken(): AutocompleteSessionToken {
        return AutocompleteSessionToken.newInstance()
//            .also {
//                Log.v("hypertrack-verbose", "token created ${it}")
//            }
    }

    override suspend fun getPlaces(
        query: String,
        token: AutocompleteSessionToken,
        location: LatLng?
    ): List<GooglePlaceModel> =
        suspendCoroutine {
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
//                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .setLocationBias(calculateBias(location))
                .setOrigin(location)

            placesClient.findAutocompletePredictions(requestBuilder.build())
                .addOnSuccessListener { response ->
                    it.resume(GooglePlaceModel.from(response.autocompletePredictions))
                }
                .addOnFailureListener { e ->
                    it.resumeWithException(e)
                }
        }

    override suspend fun fetchPlace(
        googlePlaceModel: GooglePlaceModel,
        token: AutocompleteSessionToken
    ): Place = suspendCoroutine {
        val placeFields: List<Place.Field> =
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.NAME,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.LAT_LNG
            )
        val request = FetchPlaceRequest.builder(googlePlaceModel.placeId, placeFields)
            .setSessionToken(token)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
//                Log.v("hypertrack-verbose", "place fetched fot token $token")
                it.resume(response.place)
            }
            .addOnFailureListener { e: java.lang.Exception ->
                it.resumeWithException(e)
            }
    }

    private fun calculateBias(location: LatLng?): RectangularBounds? {
        return location?.let {
            RectangularBounds.newInstance(
                LatLng(location.latitude - 0.3, location.longitude + 0.3),  // SW
                LatLng(location.latitude + 0.3, location.longitude - 0.3) // NE
            )
        }
    }
}