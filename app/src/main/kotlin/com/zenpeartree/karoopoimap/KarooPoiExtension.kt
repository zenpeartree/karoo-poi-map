package com.zenpeartree.karoopoimap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
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
        private const val FETCH_RADIUS_KM = 10.0
        private const val MIN_MOVE_METERS = 500.0
        private const val NOTIFICATION_REFRESH_INTERVAL_MS = 60_000L
        const val ADD_POI_INTENT = "com.zenpeartree.karoopoimap.ADD_POI"
        const val VOTE_POI_INTENT = "com.zenpeartree.karoopoimap.VOTE_POI"
        const val REFRESH_MAP_INTENT = "com.zenpeartree.karoopoimap.REFRESH_MAP"
        private const val ADD_NOTIFICATION_ID = "poi-add-notification"
        private const val VOTE_NOTIFICATION_ID = "poi-vote-notification"

        var repository: PoiRepository? = null
            private set
    }

    private lateinit var karooSystem: KarooSystemService
    private var locationConsumerId: String? = null
    private var rideStateConsumerId: String? = null
    private var lastFetchLat = 0.0
    private var lastFetchLng = 0.0
    private var mapEmitter: Emitter<MapEffect>? = null
    private var rideActive = false
    private var lastNotificationRefreshMs = 0L
    private var addNotificationShown = false
    private var voteNotificationShown = false
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
            } else {
                Log.w(TAG, "Failed to connect to Karoo System")
            }
        }
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
        if (!rideActive) return

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

    override fun onDestroy() {
        locationConsumerId?.let { karooSystem.removeConsumer(it) }
        rideStateConsumerId?.let { karooSystem.removeConsumer(it) }
        mapEmitter = null
        unregisterReceiver(refreshReceiver)
        karooSystem.disconnect()
        repository = null
        super.onDestroy()
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
