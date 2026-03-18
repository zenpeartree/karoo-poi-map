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
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class AddPoiActivity : Activity() {

    companion object {
        private const val TAG = "AddPoiActivity"
        private const val LOCATION_PERMISSION_REQUEST = 1
    }

    private lateinit var repository: PoiRepository
    private lateinit var coordsText: TextView
    private lateinit var selectedTypeText: TextView
    private lateinit var nameInput: EditText
    private lateinit var addButton: Button
    private lateinit var statusText: TextView

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var selectedType: PoiType? = null
    private var locationManager: LocationManager? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLat = location.latitude
            currentLng = location.longitude
            updateCoordsDisplay()
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
            text = "Add Point of Interest"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        // Location
        coordsText = TextView(this).apply {
            text = "Getting location..."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(coordsText)

        // Type selector label
        val typeLabel = TextView(this).apply {
            text = "Select type:"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(typeLabel)

        // Type grid
        val grid = GridLayout(this).apply {
            columnCount = 2
            setPadding(0, 0, 0, 16)
        }
        PoiType.entries.forEach { type ->
            val btn = Button(this).apply {
                text = type.label
                textSize = 13f
                isAllCaps = false
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                    setMargins(4, 4, 4, 4)
                }
                layoutParams = params
                setOnClickListener { selectType(type, this) }
            }
            grid.addView(btn)
        }
        layout.addView(grid)

        // Selected type display
        selectedTypeText = TextView(this).apply {
            text = "No type selected"
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(selectedTypeText)

        // Name input
        val nameLabel = TextView(this).apply {
            text = "Name (optional):"
            textSize = 14f
            setPadding(0, 0, 0, 4)
        }
        layout.addView(nameLabel)

        nameInput = EditText(this).apply {
            hint = "e.g. 'Fountain near park'"
            textSize = 14f
            setPadding(16, 12, 16, 12)
        }
        layout.addView(nameInput)

        // Add button
        addButton = Button(this).apply {
            text = "Add Point"
            textSize = 16f
            isEnabled = false
            setPadding(0, 16, 0, 0)
            setOnClickListener { addPoi() }
        }
        layout.addView(addButton)

        val voteButton = Button(this).apply {
            text = "Review Nearby POIs"
            textSize = 16f
            isAllCaps = false
            setPadding(0, 8, 0, 0)
            setOnClickListener { startActivity(Intent(this@AddPoiActivity, VotePoiActivity::class.java)) }
        }
        layout.addView(voteButton)

        // Status
        statusText = TextView(this).apply {
            text = ""
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        layout.addView(statusText)

        // POI count
        val countText = TextView(this).apply {
            val count = repository.getCachedPois().size
            text = "\n$count points loaded in area"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
        }
        layout.addView(countText)

        scroll.addView(layout)
        setContentView(scroll)

        requestLocationPermission()
    }

    private fun selectType(type: PoiType, button: Button) {
        selectedType = type
        selectedTypeText.text = "Selected: ${type.label}"
        updateAddButton()
    }

    private fun updateAddButton() {
        addButton.isEnabled = selectedType != null && currentLat != null && currentLng != null
    }

    private fun updateCoordsDisplay() {
        val lat = currentLat
        val lng = currentLng
        if (lat != null && lng != null) {
            coordsText.text = "Location: %.5f, %.5f".format(lat, lng)
            updateAddButton()
        }
    }

    private fun addPoi() {
        val type = selectedType ?: return
        val lat = currentLat ?: return
        val lng = currentLng ?: return
        val name = nameInput.text.toString().trim()

        addButton.isEnabled = false
        statusText.text = "Adding..."

        val poi = Poi(
            lat = lat,
            lng = lng,
            type = type,
            name = name,
        )

        repository.addPoi(poi) { success ->
            runOnUiThread {
                if (success) {
                    statusText.text = "Added ${type.label}!"
                    statusText.setTextColor(Color.parseColor("#4CAF50"))
                    Toast.makeText(this, "${type.label} added!", Toast.LENGTH_SHORT).show()

                    sendBroadcast(Intent(KarooPoiExtension.REFRESH_MAP_INTENT).setPackage(packageName))

                    // Reset form
                    selectedType = null
                    selectedTypeText.text = "No type selected"
                    nameInput.setText("")
                    addButton.isEnabled = false
                    finish()
                } else {
                    statusText.text = "Failed to add. Check connection."
                    statusText.setTextColor(Color.RED)
                    addButton.isEnabled = true
                }
            }
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
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Try GPS first, fall back to network
        val provider = when {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true -> LocationManager.GPS_PROVIDER
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            coordsText.text = "No location provider available"
            return
        }

        locationManager?.requestLocationUpdates(provider, 2000L, 5f, locationListener)

        // Use last known location immediately
        val last = locationManager?.getLastKnownLocation(provider)
        if (last != null) {
            currentLat = last.latitude
            currentLng = last.longitude
            updateCoordsDisplay()
        }
    }

    override fun onDestroy() {
        locationManager?.removeUpdates(locationListener)
        super.onDestroy()
    }
}
