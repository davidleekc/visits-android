package com.hypertrack.android.utils

import android.content.Context
import androidx.fragment.app.FragmentFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.RetryParams
import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.*
import com.hypertrack.android.models.AbstractBackendProvider
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.common.ParamViewModelFactory
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.UserScopeViewModelFactory
import com.hypertrack.android.ui.common.ViewModelFactory
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.tabs.history.*
import com.hypertrack.android.utils.injection.CustomFragmentFactory
import com.hypertrack.android.view_models.VisitDetailsViewModel
import com.hypertrack.logistics.android.github.R
import com.hypertrack.sdk.HyperTrack
import com.hypertrack.sdk.ServiceNotificationConfig
import com.hypertrack.sdk.views.HyperTrackViews
import com.squareup.moshi.Moshi
import com.squareup.moshi.recipes.RuntimeJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Provider


class ServiceLocator(val crashReportsProvider: CrashReportsProvider) {

    fun getAccessTokenRepository(deviceId: String, userName: String) =
        BasicAuthAccessTokenRepository(AUTH_URL, deviceId, userName)

    fun getHyperTrackService(publishableKey: String): HyperTrackService {
        val listener = TrackingState(crashReportsProvider)
        val sdkInstance = HyperTrack
            .getInstance(publishableKey)
            .addTrackingListener(listener)
            .backgroundTrackingRequirement(false)
            .setTrackingNotificationConfig(
                ServiceNotificationConfig.Builder()
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .build()
            )

        return HyperTrackService(listener, sdkInstance, crashReportsProvider)
    }

}


object Injector {

    private var userScope: UserScope? = null
    var tripCreationScope: TripCreationScope? = null

    val crashReportsProvider: CrashReportsProvider by lazy { FirebaseCrashReportsProvider() }

    val deeplinkProcessor: DeeplinkProcessor = BranchIoDeepLinkProcessor(crashReportsProvider)

    private val serviceLocator = ServiceLocator(crashReportsProvider)

    val batteryLevelMonitor = BatteryLevelMonitor(crashReportsProvider)

    fun getMoshi(): Moshi = Moshi.Builder()
        .add(HistoryCoordinateJsonAdapter())
        .add(GeometryJsonAdapter())
        .add(
            RuntimeJsonAdapterFactory(HistoryMarker::class.java, "type")
                .registerSubtype(HistoryStatusMarker::class.java, "device_status")
                .registerSubtype(HistoryTripMarker::class.java, "trip_marker")
                .registerSubtype(HistoryGeofenceMarker::class.java, "geofence")
        )
        .build()

    fun provideViewModelFactory(context: Context): ViewModelFactory {
        return ViewModelFactory(
            getAccountRepo(context),
            getDriverRepo(),
            crashReportsProvider,
            getPermissionInteractor(),
            getLoginInteractor(),
            getOsUtilsProvider(MyApplication.context),
            getMoshi()
        )
    }

    //todo user scope
    fun <T> provideParamVmFactory(param: T): ParamViewModelFactory<T> {
        return ParamViewModelFactory(
            param,
            { getUserScope() },
            getOsUtilsProvider(MyApplication.context),
            getAccountRepo(MyApplication.context),
            getMoshi(),
            crashReportsProvider,
            placesClient,
            getDeviceLocationProvider()
        )
    }

    fun provideUserScopeViewModelFactory(): UserScopeViewModelFactory {
        return getUserScope().userScopeViewModelFactory
    }

    fun provideVisitStatusViewModel(context: Context, visitId: String): VisitDetailsViewModel {
        return VisitDetailsViewModel(
            getVisitsRepo(context),
            getVisitsInteractor(),
            visitId,
            getOsUtilsProvider(MyApplication.context)
        )
    }

    private fun isTwmoEnabled(): Boolean {
        return MyApplication.TWMO_ENABLED
    }

    fun provideTabs(): List<Tab> = mutableListOf<Tab>().apply {
        addAll(
            listOf(
                Tab.MAP,
                Tab.HISTORY,
            )
        )
        add(
            if (isTwmoEnabled()) {
                Tab.ORDERS
            } else {
                Tab.VISITS
            }
        )
        addAll(
            listOf(
                Tab.PLACES,
                Tab.SUMMARY,
                Tab.PROFILE,
            )
        )
    }

    private fun createUserScope(
        publishableKey: String,
        accountRepository: AccountRepository,
        accessTokenRepository: BasicAuthAccessTokenRepository,
        driverRepository: DriverRepository,
        permissionsInteractor: PermissionsInteractor,
        deviceLocationProvider: DeviceLocationProvider,
        osUtilsProvider: OsUtilsProvider,
        crashReportsProvider: CrashReportsProvider,
        moshi: Moshi,
        myPreferences: MyPreferences,
        fileRepository: FileRepository,
        imageDecoder: ImageDecoder,
        timeDistanceFormatter: TimeDistanceFormatter
    ): UserScope {
        val apiClient = ApiClient(
            accessTokenRepository,
            BASE_URL,
            accessTokenRepository.deviceId,
            moshi,
            crashReportsProvider
        )
        val historyRepository = HistoryRepository(
            apiClient,
            crashReportsProvider,
            osUtilsProvider
        )
        val scope = CoroutineScope(Dispatchers.IO)
        val placesRepository = PlacesRepositoryImpl(
            apiClient,
            moshi,
            osUtilsProvider
        )
        val integrationsRepository = IntegrationsRepositoryImpl(apiClient)
        val placesInteractor = PlacesInteractorImpl(
            placesRepository,
            integrationsRepository,
            osUtilsProvider,
            GlobalScope
        )
        val hyperTrackService = serviceLocator.getHyperTrackService(publishableKey)

        val visitsRepo = VisitsRepository(
            permissionsInteractor,
            osUtilsProvider,
            apiClient,
            myPreferences,
            hyperTrackService,
            accountRepository,
            deviceLocationProvider
        )

        val photoUploadInteractor = PhotoUploadInteractorImpl(
            visitsRepo,
            fileRepository,
            crashReportsProvider,
            imageDecoder,
            apiClient,
            scope,
            RetryParams(
                retryTimes = 3,
                initialDelay = 1000,
                factor = 10.0,
                maxDelay = 30 * 1000
            )
        )

        val photoUploadQueueInteractor = PhotoUploadQueueInteractorImpl(
            myPreferences,
            fileRepository,
            crashReportsProvider,
            imageDecoder,
            apiClient,
            scope,
            RetryParams(
                retryTimes = 3,
                initialDelay = 1000,
                factor = 10.0,
                maxDelay = 30 * 1000
            )
        )

        val tripsRepository = TripsRepositoryImpl(
            apiClient,
            myPreferences,
            hyperTrackService,
            GlobalScope,
            accountRepository.isPickUpAllowed
        )

        val tripsInteractor = TripsInteractorImpl(
            tripsRepository,
            apiClient,
            hyperTrackService,
            photoUploadQueueInteractor,
            imageDecoder,
            osUtilsProvider,
            Dispatchers.IO,
            GlobalScope
        )

        val feedbackInteractor = FeedbackInteractor(
            accessTokenRepository.deviceId,
            tripsInteractor,
            integrationsRepository,
            moshi,
            osUtilsProvider,
            crashReportsProvider
        )

        val userScope = UserScope(
            visitsRepo,
            historyRepository,
            tripsInteractor,
            placesInteractor,
            feedbackInteractor,
            integrationsRepository,
            UserScopeViewModelFactory(
                visitsRepo,
                tripsInteractor,
                placesInteractor,
                feedbackInteractor,
                integrationsRepository,
                historyRepository,
                driverRepository,
                accountRepository,
                crashReportsProvider,
                hyperTrackService,
                permissionsInteractor,
                accessTokenRepository,
                timeDistanceFormatter,
                apiClient,
                osUtilsProvider,
                placesClient,
                deviceLocationProvider
            ),
            photoUploadInteractor,
            hyperTrackService,
            photoUploadQueueInteractor,
            apiClient
        )

        crashReportsProvider.setUserIdentifier(
            moshi.adapter(UserIdentifier::class.java).toJson(
                UserIdentifier(
                    deviceId = accessTokenRepository.deviceId,
                    driverId = driverRepository.username,
                    pubKey = accountRepository.publishableKey,
                )
            )
        )

        return userScope
    }

    private fun getUserScope(): UserScope {
        if (userScope == null) {
            val myPreferences = getMyPreferences(MyApplication.context)
            val publishableKey = myPreferences.getAccountData().publishableKey
                ?: throw IllegalStateException("No publishableKey saved")

            userScope = createUserScope(
                publishableKey,
                getAccountRepo(MyApplication.context),
                accessTokenRepository(MyApplication.context),
                getDriverRepo(),
                getPermissionInteractor(),
                getDeviceLocationProvider(),
                getOsUtilsProvider(MyApplication.context),
                crashReportsProvider,
                getMoshi(),
                getMyPreferences(MyApplication.context),
                getFileRepository(),
                getImageDecoder(),
                getTimeDistanceFormatter()
            )
        }
        return userScope!!
    }

    private val placesClient: PlacesClient by lazy {
        Places.createClient(MyApplication.context)
    }

    private fun getDriverRepo(): DriverRepository {
        return DriverRepository(
            getAccountRepo(MyApplication.context),
            serviceLocator,
            getMyPreferences(MyApplication.context),
            getOsUtilsProvider(MyApplication.context),
            crashReportsProvider,
        )
    }

    private fun getFileRepository(): FileRepository {
        return FileRepositoryImpl()
    }

    private fun getPermissionInteractor() = PermissionsInteractorImpl { getUserScope().hyperTrackService }

    private val tokenForPublishableKeyExchangeService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(LIVE_API_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .build()
        return@lazy retrofit.create(TokenForPublishableKeyExchangeService::class.java)
    }

    private val liveAccountUrlService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(LIVE_ACCOUNT_URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .build()
        return@lazy retrofit.create(LiveAccountApi::class.java)
    }

    private fun getLoginInteractor(): LoginInteractor {
        return LoginInteractorImpl(
            getDriverRepo(),
            getCognitoLoginProvider(MyApplication.context),
            getAccountRepo(MyApplication.context),
            tokenForPublishableKeyExchangeService,
            liveAccountUrlService,
            MyApplication.SERVICES_API_KEY
        )
    }

    private fun getMyPreferences(context: Context): MyPreferences =
        MyPreferences(context, getMoshi())

    private fun getVisitsInteractor(): VisitsInteractor {
        return VisitsInteractorImpl(
            getVisitsRepo(MyApplication.context),
            getImageDecoder(),
            getUserScope().photoUploadInteractor
        )
    }

    fun accessTokenRepository(context: Context): BasicAuthAccessTokenRepository =
        (getMyPreferences(context).restoreRepository()
            ?: throw IllegalStateException("No access token repository was saved"))

    private fun getAccountRepo(context: Context) =
        AccountRepository(serviceLocator, getAccountData(context), getMyPreferences(context))
        { userScope = null }

    private fun getAccountData(context: Context): AccountData =
        getMyPreferences(context).getAccountData()

    fun getOsUtilsProvider(context: Context): OsUtilsProvider {
        return OsUtilsProvider(context, crashReportsProvider)
    }

    fun getVisitsRepo(context: Context): VisitsRepository {
        return getUserScope().visitsRepository
    }

    private fun getImageDecoder(): ImageDecoder = SimpleImageDecoder()

    private fun getCognitoLoginProvider(context: Context): CognitoAccountLoginProvider =
        CognitoAccountLoginProviderImpl(context)

    private fun getHistoryMapRenderer(supportMapFragment: SupportMapFragment): HistoryMapRenderer =
        GoogleMapHistoryRenderer(
            supportMapFragment,
            BaseHistoryStyle(MyApplication.context),
            getDeviceLocationProvider(),
            crashReportsProvider
        )

    private fun getDeviceLocationProvider(): DeviceLocationProvider {
        return FusedDeviceLocationProvider(MyApplication.context)
    }

    fun getHistoryRendererFactory(): Factory<SupportMapFragment, HistoryMapRenderer> =
        Factory { a -> getHistoryMapRenderer(a) }

    fun getBackendProvider(ctx: Context): Provider<AbstractBackendProvider> =
        Provider { getUserScope().apiClient }

    fun getRealTimeUpdatesService(ctx: Context): Provider<HyperTrackViews> =
        Provider { HyperTrackViews.getInstance(ctx, getAccountRepo(ctx).publishableKey) }

    val hyperTrackServiceProvider = Provider { getUserScope().hyperTrackService }

    fun getTimeDistanceFormatter() =
        LocalizedTimeDistanceFormatter(getOsUtilsProvider(MyApplication.context))

    fun getCustomFragmentFactory(applicationContext: Context): FragmentFactory {
        val publishableKeyProvider: Provider<String> =
            Provider<String> { getAccountRepo(applicationContext).publishableKey }
        val hyperTrackServiceProvider = Provider { getUserScope().hyperTrackService }
        val apiClientProvider: Provider<AbstractBackendProvider> =
            Provider { getUserScope().apiClient }

        return CustomFragmentFactory(
            MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.style_map),
            MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.style_map_silver),
            hyperTrackServiceProvider,
            { HyperTrackViews.getInstance(applicationContext, publishableKeyProvider.get()) },
            apiClientProvider
        )
    }

}

class TripCreationScope(
    val destinationData: DestinationData
)

class UserScope(
    val visitsRepository: VisitsRepository,
    val historyRepository: HistoryRepository,
    val tripsInteractor: TripsInteractor,
    val placesInteractor: PlacesInteractor,
    val feedbackInteractor: FeedbackInteractor,
    val integrationsRepository: IntegrationsRepository,
    val userScopeViewModelFactory: UserScopeViewModelFactory,
    val photoUploadInteractor: PhotoUploadInteractor,
    val hyperTrackService: HyperTrackService,
    val photoUploadQueueInteractor: PhotoUploadQueueInteractor,
    val apiClient: ApiClient
)

fun interface Factory<A, T> {
    fun create(a: A): T
}

interface AccountPreferencesProvider {
    var wasWhitelisted: Boolean
    val isManualCheckInAllowed: Boolean
    val isPickUpAllowed: Boolean
    var shouldStartTracking: Boolean
}

const val BASE_URL = "https://live-app-backend.htprod.hypertrack.com/"
const val LIVE_API_URL_BASE = "https://live-api.htprod.hypertrack.com/"
const val AUTH_URL = LIVE_API_URL_BASE + "authenticate"
const val MAX_IMAGE_SIDE_LENGTH_PX = 1024

const val LIVE_ACCOUNT_URL_BASE = "https://live-account.htprod.hypertrack.com"
