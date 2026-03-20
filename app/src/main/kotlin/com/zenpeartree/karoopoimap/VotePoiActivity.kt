package com.zenpeartree.karoopoimap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class VotePoiActivity : Activity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 2
        private const val CARD_PADDING = 24
    }

    private lateinit var repository: PoiRepository
    private lateinit var coordsText: TextView
    private lateinit var statusText: TextView
    private lateinit var poiListLayout: LinearLayout

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var locationManager: LocationManager? = null
    private val votedPoiIds = mutableSetOf<String>()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLat = location.latitude
            currentLng = location.longitude
            updateCoordsDisplay()
            refreshPoiList()
        }

        @Deprecated("Required for older API levels")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = KarooPoiExtension.repository ?: PoiRepository(this)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Review Nearby POIs"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "Confirm useful points or downvote stale ones."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        layout.addView(subtitle)

        coordsText = TextView(this).apply {
            text = "Getting location..."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        layout.addView(coordsText)

        statusText = TextView(this).apply {
            text = ""
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        layout.addView(statusText)

        poiListLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(poiListLayout)

        scroll.addView(layout)
        setContentView(scroll)

        refreshPoiList()
        requestLocationPermission()
    }

    private fun refreshPoiList() {
        val lat = currentLat
        val lng = currentLng
        val pois = repository.getCachedPois()
        val sortedPois = if (lat != null && lng != null) {
            pois.sortedBy { haversineMeters(lat, lng, it.lat, it.lng) }
        } else {
            pois
        }

        poiListLayout.removeAllViews()

        if (sortedPois.isEmpty()) {
            poiListLayout.addView(TextView(this).apply {
                text = "No nearby POIs cached yet. Ride a bit to load points first."
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 0)
            })
            return
        }

        sortedPois.take(25).forEach { poi ->
            poiListLayout.addView(createPoiCard(poi, lat, lng))
        }
    }

    private fun createPoiCard(poi: Poi, lat: Double?, lng: Double?): LinearLayout {
        val isVoted = poi.id in votedPoiIds
        val distanceText = if (lat != null && lng != null) {
            formatDistance(haversineMeters(lat, lng, poi.lat, poi.lng))
        } else {
            "Distance unavailable"
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)
            setBackgroundColor(Color.parseColor("#F2F2F2"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 16
            }
            layoutParams = params

            addView(TextView(context).apply {
                text = poi.displayName
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(context).apply {
                text = "${poi.type.label} • $distanceText"
                textSize = 13f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 4)
            })

            addView(TextView(context).apply {
                text = "Votes: ${poi.upvotes} up / ${poi.downvotes} down"
                textSize = 13f
                setTextColor(Color.DKGRAY)
                setPadding(0, 0, 0, 16)
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL

                addView(Button(context).apply {
                    text = if (isVoted) "Voted" else "Upvote"
                    isAllCaps = false
                    isEnabled = !isVoted && poi.id.isNotBlank()
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 8
                    }
                    setOnClickListener { submitVote(poi, upvote = true) }
                })

                addView(Button(context).apply {
                    text = if (isVoted) "Voted" else "Downvote"
                    isAllCaps = false
                    isEnabled = !isVoted && poi.id.isNotBlank()
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { submitVote(poi, upvote = false) }
                })
            })
        }
    }

    private fun submitVote(poi: Poi, upvote: Boolean) {
        if (poi.id.isBlank() || poi.id in votedPoiIds) return

        statusText.text = if (upvote) "Submitting upvote..." else "Submitting downvote..."
        statusText.setTextColor(Color.DKGRAY)

        repository.vote(poi.id, upvote) { success ->
            runOnUiThread {
                if (success) {
                    votedPoiIds.add(poi.id)
                    sendBroadcast(Intent(KarooPoiExtension.REFRESH_MAP_INTENT).setPackage(packageName))
                    refreshPoiList()
                    statusText.text = if (upvote) {
                        "Upvoted ${poi.displayName}"
                    } else {
                        "Downvoted ${poi.displayName}"
                    }
                    statusText.setTextColor(Color.parseColor("#4CAF50"))
                    Toast.makeText(
                        this,
                        if (upvote) "Upvote saved" else "Downvote saved",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    statusText.text = "Vote failed or was already recorded."
                    statusText.setTextColor(Color.RED)
                }
            }
        }
    }

    private fun updateCoordsDisplay() {
        val lat = currentLat
        val lng = currentLng
        coordsText.text = if (lat != null && lng != null) {
            "Location: %.5f, %.5f".format(lat, lng)
        } else {
            "Getting location..."
        }
    }

    private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            coordsText.text = "Location permission denied"
            refreshPoiList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val provider = when {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true -> LocationManager.GPS_PROVIDER
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            coordsText.text = "No location provider available"
            refreshPoiList()
            return
        }

        locationManager?.requestLocationUpdates(provider, 2000L, 5f, locationListener)

        val last = locationManager?.getLastKnownLocation(provider)
        if (last != null) {
            currentLat = last.latitude
            currentLng = last.longitude
            updateCoordsDisplay()
            refreshPoiList()
        }
    }

    override fun onDestroy() {
        locationManager?.removeUpdates(locationListener)
        super.onDestroy()
    }

    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1000.0) {
            "%.1f km away".format(distanceMeters / 1000.0)
        } else {
            "${distanceMeters.roundToInt()} m away"
        }
    }
}
