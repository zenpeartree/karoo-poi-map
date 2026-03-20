package com.zenpeartree.karoopoimap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.MapEffect
import io.hammerhead.karooext.models.OnLocationChanged
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.ShowSymbols
import io.hammerhead.karooext.models.SystemNotification

class KarooPoiExtension : KarooExtension("karoo-poi-map", "1") {

    companion object {
        private const val TAG = "KarooPoiExt"
        private const val FIELD_ADD_POI = "add-poi-field"
        private const val FIELD_REVIEW_POIS = "review-pois-field"
        private const val FETCH_RADIUS_KM = 10.0
        private const val MIN_MOVE_METERS = 500.0
        private const val NOTIFICATION_REFRESH_INTERVAL_MS = 15_000L
        private const val BONUS_ACTION_ADD_POI = "add-poi"
        private const val BONUS_ACTION_REVIEW_POIS = "review-pois"
        const val ADD_POI_INTENT = "com.zenpeartree.karoopoimap.ADD_POI"
        const val VOTE_POI_INTENT = "com.zenpeartree.karoopoimap.VOTE_POI"
        const val REFRESH_MAP_INTENT = "com.zenpeartree.karoopoimap.REFRESH_MAP"
        private const val ADD_NOTIFICATION_ID = "poi-add-notification"
        private const val VOTE_NOTIFICATION_ID = "poi-vote-notification"

        var repository: PoiRepository? = null
            private set
    }

    override val types: List<DataTypeImpl> by lazy {
        listOf(
            RideActionField(
                typeId = FIELD_ADD_POI,
                titleResId = R.string.ride_field_add_title,
                subtitleResId = R.string.ride_field_add_subtitle,
                activityClass = AddPoiActivity::class.java,
            ),
            RideActionField(
                typeId = FIELD_REVIEW_POIS,
                titleResId = R.string.ride_field_review_title,
                subtitleResId = R.string.ride_field_review_subtitle,
                activityClass = VotePoiActivity::class.java,
            ),
        )
    }

    private lateinit var karooSystem: KarooSystemService
    private val mainHandler = Handler(Looper.getMainLooper())
    private var locationConsumerId: String? = null
    private var rideStateConsumerId: String? = null
    private var lastFetchLat = 0.0
    private var lastFetchLng = 0.0
    private var mapEmitter: Emitter<MapEffect>? = null
    private var rideActive = false
    private var hasRideState = false
    private var lastNotificationRefreshMs = 0L
    private var addNotificationShown = false
    private var voteNotificationShown = false
    private val notificationRefreshRunnable = object : Runnable {
        override fun run() {
            refreshRideNotifications(force = true)
            mainHandler.postDelayed(this, NOTIFICATION_REFRESH_INTERVAL_MS)
        }
    }
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == REFRESH_MAP_INTENT) {
                refreshMap()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(this)
        repository = PoiRepository(this)
        registerReceiver(refreshReceiver, IntentFilter(REFRESH_MAP_INTENT))

        karooSystem.connect { connected ->
            if (connected) {
                Log.i(TAG, "Connected to Karoo System")
                subscribeToRideState()
                startNotificationRefreshLoop()
            } else {
                Log.w(TAG, "Failed to connect to Karoo System")
            }
        }
    }

    private fun startNotificationRefreshLoop() {
        mainHandler.removeCallbacks(notificationRefreshRunnable)
        notificationRefreshRunnable.run()
    }

    private fun subscribeToRideState() {
        if (rideStateConsumerId != null) return

        rideStateConsumerId = karooSystem.addConsumer<RideState>(
            onError = { Log.w(TAG, "Ride state error: $it") },
        ) { state ->
            onRideStateChanged(state)
        }
    }

    private fun onRideStateChanged(state: RideState) {
        hasRideState = true
        val wasRideActive = rideActive
        rideActive = state !is RideState.Idle
        Log.i(TAG, "Ride state changed: ${state.javaClass.simpleName}")

        if (!rideActive) {
            addNotificationShown = false
            voteNotificationShown = false
            lastNotificationRefreshMs = 0L
            return
        }

        val enteringRide = !wasRideActive
        if (enteringRide) {
            addNotificationShown = false
            voteNotificationShown = false
            lastNotificationRefreshMs = 0L
        }

        refreshRideNotifications(force = enteringRide)
    }

    private fun refreshRideNotifications(force: Boolean = false) {
        if (hasRideState && !rideActive) return

        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationRefreshMs < NOTIFICATION_REFRESH_INTERVAL_MS) return

        showAddPoiNotification(force)
        showVotePoiNotification(force)
        lastNotificationRefreshMs = now
    }

    private fun showAddPoiNotification(force: Boolean = false) {
        if (addNotificationShown && !force) return
        val dispatched = karooSystem.dispatch(
            SystemNotification(
                id = ADD_NOTIFICATION_ID,
                header = "POI Map",
                message = "Add a point of interest",
                action = "Add POI",
                actionIntent = ADD_POI_INTENT,
                style = SystemNotification.Style.EVENT,
            )
        )
        addNotificationShown = dispatched
        Log.i(TAG, "Add POI notification dispatched: $dispatched")
    }

    private fun showVotePoiNotification(force: Boolean = false) {
        if (voteNotificationShown && !force) return
        val dispatched = karooSystem.dispatch(
            SystemNotification(
                id = VOTE_NOTIFICATION_ID,
                header = "POI Map",
                message = "Review nearby points",
                action = "Vote POIs",
                actionIntent = VOTE_POI_INTENT,
                style = SystemNotification.Style.EVENT,
            )
        )
        voteNotificationShown = dispatched
        Log.i(TAG, "Vote POI notification dispatched: $dispatched")
    }

    override fun startMap(emitter: Emitter<MapEffect>) {
        mapEmitter = emitter
        Log.i(TAG, "Map layer started")
        refreshRideNotifications(force = true)

        // Show cached POIs immediately
        val cached = repository?.getCachedPois() ?: emptyList()
        if (cached.isNotEmpty()) {
            emitter.onNext(ShowSymbols(cached.map { it.toSymbol() }))
            Log.i(TAG, "Showing ${cached.size} cached POIs")
        }

        // Subscribe to location to refresh POIs as rider moves
        locationConsumerId?.let { karooSystem.removeConsumer(it) }
        locationConsumerId = karooSystem.addConsumer<OnLocationChanged>(
            onError = { Log.w(TAG, "Location error: $it") },
        ) { event ->
            onLocationUpdate(event.lat, event.lng)
        }
    }

    private fun onLocationUpdate(lat: Double, lng: Double) {
        refreshRideNotifications()

        val distance = haversineMeters(lastFetchLat, lastFetchLng, lat, lng)
        if (distance < MIN_MOVE_METERS && lastFetchLat != 0.0) return

        lastFetchLat = lat
        lastFetchLng = lng

        repository?.fetchNearby(lat, lng, FETCH_RADIUS_KM) { pois ->
            val emitter = mapEmitter ?: return@fetchNearby
            emitter.onNext(ShowSymbols(pois.map { it.toSymbol() }))
            Log.i(TAG, "Updated map with ${pois.size} POIs near ($lat, $lng)")
        }
    }

    fun refreshMap() {
        val pois = repository?.getCachedPois() ?: return
        val emitter = mapEmitter ?: return
        emitter.onNext(ShowSymbols(pois.map { it.toSymbol() }))
    }

    override fun onBonusAction(actionId: String) {
        when (actionId) {
            BONUS_ACTION_ADD_POI -> {
                Log.i(TAG, "Launching Add POI from bonus action")
                startActivity(
                    Intent(this, AddPoiActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            BONUS_ACTION_REVIEW_POIS -> {
                Log.i(TAG, "Launching Review POIs from bonus action")
                startActivity(
                    Intent(this, VotePoiActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            else -> Log.w(TAG, "Unknown bonus action: $actionId")
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(notificationRefreshRunnable)
        locationConsumerId?.let { karooSystem.removeConsumer(it) }
        rideStateConsumerId?.let { karooSystem.removeConsumer(it) }
        mapEmitter = null
        unregisterReceiver(refreshReceiver)
        karooSystem.disconnect()
        repository = null
        super.onDestroy()
    }

}
