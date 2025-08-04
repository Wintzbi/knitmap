package com.knitMap.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.knitMap.BaseActivity
import com.knitMap.MainActivity.Companion.isOnline
import com.knitMap.MenuWithDropdown
import com.knitMap.R
import com.knitMap.discovery.Discovery
import com.knitMap.discovery.DiscoveryActivityXml
import com.knitMap.storage.flushPendingScratches
import com.knitMap.storage.getDiscoveriesLocally
import com.knitMap.storage.getLastKnownPoint
import com.knitMap.storage.removeLastKnownPoint
import com.knitMap.storage.saveDiscoveriesLocally
import com.knitMap.storage.saveLastKnownPoint
import com.knitMap.utils.getLocationFromCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapAdapter
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MapActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var discoveryLauncher: ActivityResultLauncher<Intent>
    private var photoUri: String? = null
    private var lastKnownPoint: GeoPoint? = null

    private var shouldCenterOnUser = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_map)

        setupLayout()
        requestPermissionsIfNecessary()

        discoveryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
                updated?.let {
                    val existing = getDiscoveriesLocally(this).toMutableList()
                    val index = existing.indexOfFirst { d -> d.uuid == updated.uuid }
                    if (index != -1) {
                        existing[index] = updated
                    } else {
                        existing.add(updated)
                    }
                    saveDiscoveriesLocally(this, existing)
                    updateDiscoveryMarker(mapView, this, updated, discoveryLauncher)
                    Toast.makeText(this, "Discovery enregistrée", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null && lastKnownPoint != null) {
                launchDiscoveryWithImage(this, photoUri, lastKnownPoint!!, discoveryLauncher)
                addDiscoveryMarkers(mapView, this, discoveryLauncher)
            } else if (lastKnownPoint != null) {
                launchDiscoveryWithImage(this, null, lastKnownPoint!!, discoveryLauncher)
                addDiscoveryMarkers(mapView, this, discoveryLauncher)
            } else {
                Toast.makeText(this, "Position inconnue, impossible de créer une découverte", Toast.LENGTH_SHORT).show()
            }
        }

        setupMap()
    }

    private fun setupLayout() {
        findViewById<ImageView>(R.id.Top).setImageResource(R.drawable.up_map_fond)
        findViewById<ImageView>(R.id.Bot).setImageResource(R.drawable.map_fond_bouton)

        findViewById<ImageView>(R.id.Ping_Boutton).apply {
            setImageResource(R.drawable.ping_resized)
            setOnClickListener {
                if (lastKnownPoint == null) {
                    Toast.makeText(this@MapActivity, "Veuillez activer la localisation pour pouvoir ajouter un point.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } else {
                    startCameraIntent(this@MapActivity, cameraLauncher) { path ->
                        photoUri = path
                    }
                }
            }
        }

        val composeView = findViewById<ComposeView>(R.id.composeView)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MenuWithDropdown()
        }

        mapView = findViewById(R.id.activity_map)
        mapView.minZoomLevel = 3.0
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val discoveries = getDiscoveriesLocally(this).map {
            if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
            if (it.date.isBlank()) it.copy(date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) else it
            if (it.locationName.isBlank()) it.copy(locationName = "Lieu inconnu") else it
        }

        val lastDiscovery = discoveries.lastOrNull()
        val lastDiscoveryPoint = lastDiscovery?.let { GeoPoint(it.latitude, it.longitude) }

        val scratchOverlay = ScratchOverlay(mapView)
        mapView.overlays.add(0, scratchOverlay)

        val provider = GpsMyLocationProvider(this)
        val locationOverlay = MyLocationNewOverlay(provider, mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)

        val followButton = findViewById<ImageView>(R.id.Follow_Button)
        followButton.setImageResource(R.drawable.target)
        val newColor = if (locationOverlay.isFollowLocationEnabled) "#E7191F".toColorInt() else "#154043".toColorInt()
        followButton.setColorFilter(newColor, PorterDuff.Mode.SRC_IN)
        followButton.setOnClickListener {
            if (!locationOverlay.isFollowLocationEnabled) {
                mapView.controller.setZoom(17.5)
                lastKnownPoint?.let { mapView.controller.animateTo(it) }
                locationOverlay.enableFollowLocation()
            } else {
                locationOverlay.disableFollowLocation()
            }
        }


        val ecoButton = findViewById<Button>(R.id.Eco_Button)
        val ecoOverlay = findViewById<FrameLayout>(R.id.eco_mode_overlay)
        val exitEcoButton = findViewById<Button>(R.id.exit_eco_mode_button)

        val lp = window.attributes

        ecoOverlay.setOnTouchListener { _, _ -> true }

        ecoButton.setOnClickListener {
            ecoOverlay.visibility = View.VISIBLE
            ecoOverlay.bringToFront()
            mapView.overlays.remove(scratchOverlay)
            mapView.visibility = View.GONE

            mapView.setMultiTouchControls(false)
            mapView.isEnabled = false

            lp.screenBrightness = 0.01f
            window.attributes = lp

        }

        exitEcoButton.setOnClickListener {
            ecoOverlay.visibility = View.GONE
            mapView.overlays.add(scratchOverlay)
            mapView.visibility = View.VISIBLE

            mapView.setMultiTouchControls(true)
            mapView.setOnTouchListener(null) // réactive l'interaction
            mapView.isEnabled = true

            lp.screenBrightness = -1f
            window.attributes = lp
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastLocation?.let {
                lastKnownPoint = GeoPoint(it.latitude, it.longitude)
                saveLastKnownPoint(this, lastKnownPoint!!)
            }

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lastKnownPoint = GeoPoint(location.latitude, location.longitude)
                    scratchOverlay.scratchAt(lastKnownPoint!!)
                    removeLastKnownPoint(this@MapActivity)
                    saveLastKnownPoint(this@MapActivity, lastKnownPoint!!)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
        }

        addDiscoveryMarkers(mapView, this, discoveryLauncher)

        intent.getDoubleExtra("center_lat", Double.NaN).takeIf { !it.isNaN() }?.let { lat ->
            val lon = intent.getDoubleExtra("center_lon", Double.NaN)
            if (!lon.isNaN()) {
                val point = GeoPoint(lat, lon)
                mapView.controller.setZoom(17.5)
                mapView.controller.animateTo(point)
                shouldCenterOnUser = false
            }
        }

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                if (shouldCenterOnUser) {
                    tryCenterOnUserLocation(
                        context = this@MapActivity,
                        mapView = mapView,
                        locationOverlay = locationOverlay,
                        fallback = lastDiscoveryPoint,
                        onScratch = { point -> scratchOverlay.scratchAt(point) }
                    )
                }
            }
        }

        mapView.addMapListener(object : MapAdapter() {
            override fun onScroll(event: ScrollEvent?): Boolean {
                addDiscoveryMarkers(mapView, this@MapActivity, discoveryLauncher)
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                addDiscoveryMarkers(mapView, this@MapActivity, discoveryLauncher)
                return false
            }
        })
    }

    override fun onPause() {
        super.onPause()
        getLastKnownPoint(this)?.let { removeLastKnownPoint(this) }
        lastKnownPoint?.let { saveLastKnownPoint(this, it) }
        flushPendingScratches(this)
    }

    private fun requestPermissionsIfNecessary() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
        ).filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        }
    }

}

// --- Fonctions utilitaires communes ---
fun launchDiscoveryWithImage(
    context: Context,
    uri: String?,
    point: GeoPoint,
    launcher: ActivityResultLauncher<Intent>
) {
    val discovery = Discovery(
        title = "",
        description = "",
        imageResId = R.drawable.cat03,
        latitude = point.latitude,
        longitude = point.longitude,
        imageUri = uri?.toString(),
        locationName = "Lieu inconnu"
    )

    if (isOnline) {
        CoroutineScope(Dispatchers.IO).launch {
            getLocationFromCoordinates(discovery.latitude, discovery.longitude) { resolvedLocation ->
                val discoveryWithLocation = discovery.copy(locationName = resolvedLocation)

                Handler(Looper.getMainLooper()).post {
                    val intent = Intent(context, DiscoveryActivityXml::class.java).apply {
                        putExtra("discovery", discoveryWithLocation)
                    }
                    launcher.launch(intent)
                }
            }
        }

    } else {
        val intent = Intent(context, DiscoveryActivityXml::class.java).apply {
            putExtra("discovery", discovery)
        }
        launcher.launch(intent)
    }
}

fun startCameraIntent(
    context: Context,
    launcher: ActivityResultLauncher<Uri>,
    onPathReady: (String) -> Unit
) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "IMG_$timestamp.jpg"

    // Utilise getExternalFilesDir pour que ce soit dans ton app, privé
    val picturesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "KnitMapPictures")
    if (!picturesDir.exists()) picturesDir.mkdirs()

    val imageFile = File(picturesDir, fileName)

    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",  // doit correspondre à AndroidManifest + file_paths.xml
        imageFile
    )

    onPathReady(imageFile.absolutePath)  // ✅ Ce fichier sera bien rempli après photo
    launcher.launch(uri)
}


fun addDiscoveryMarkers(mapView: MapView, context: Context, launcher: ActivityResultLauncher<Intent>) {
    val bounds = mapView.boundingBox

    val discoveries = getDiscoveriesLocally(context).filter {
        it.latitude in bounds.latSouth..bounds.latNorth &&
                it.longitude in bounds.lonWest..bounds.lonEast
    }

    val icon = ContextCompat.getDrawable(context, R.drawable.pinged)

    mapView.overlays.removeAll { it is Marker }

    discoveries.forEach { discovery ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(discovery.latitude, discovery.longitude)
            title = discovery.title
            snippet = discovery.description
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.icon = icon
            subDescription = discovery.uuid

            setOnMarkerClickListener { _, _ ->
                val intent = Intent(context, DiscoveryActivityXml::class.java).apply {
                    putExtra("discovery", discovery)
                }
                launcher.launch(intent)
                true
            }
        }
        mapView.overlays.add(marker)
    }
    mapView.invalidate()
}


fun tryCenterOnUserLocation(
    context: Context,
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
    fallback: GeoPoint? = null,          //  GeoPoint?  ← peut être nul
    onSuccess: ((GeoPoint) -> Unit)? = null,
    onFail: ((GeoPoint) -> Unit)? = null,
    onScratch: ((GeoPoint) -> Unit)? = null
) {
    val maxRetries = 10
    val intervalMs = 1_000L
    var attempts = 0

    val handler = Handler(Looper.getMainLooper())
    handler.post(object : Runnable {
        override fun run() {
            val loc = locationOverlay.myLocation
            if (loc != null) {
                val userPoint = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.setZoom(17.5)
                mapView.controller.animateTo(userPoint)
                onSuccess?.invoke(userPoint)
                onScratch?.invoke(userPoint)
                Toast.makeText(context, "Centré sur votre position", Toast.LENGTH_SHORT).show()
            } else if (attempts < maxRetries) {
                attempts++
                handler.postDelayed(this, intervalMs)
            } else {
                // échec définitif
                if (fallback != null) {
                    // on possède un dernier point connu (découverte)&nbsp;: on l’utilise
                    mapView.controller.setZoom(17.5)
                    mapView.controller.animateTo(fallback)
                    onFail?.invoke(fallback)
                    onScratch?.invoke(fallback)
                    Toast.makeText(context, "Position non trouvée, centrage sur votre dernière découverte", Toast.LENGTH_SHORT).show()
                } else {
                    // rien du tout: on demande à activer le GPS/permissions
                    Toast.makeText(context, "Impossible d’obtenir votre position. Veuillez activer la localisation puis réessayer.", Toast.LENGTH_LONG).show()
                    // Ouvre directement les réglages&nbsp;(optionnel mais pratique)
                    if (getLastKnownPoint(context) != null) {
                        mapView.controller.setZoom(17.5)
                        mapView.controller.animateTo(getLastKnownPoint(context) ?: fallback)
                    }
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
        }
    })
}

fun updateDiscoveryMarker(
    mapView: MapView,
    context: Context,
    updatedDiscovery: Discovery,
    launcher: ActivityResultLauncher<Intent>
) {
    val icon = ContextCompat.getDrawable(context, R.drawable.pinged)

    // Supprimer le marqueur existant
    val existingMarker = mapView.overlays.find {
        (it as? Marker)?.subDescription == updatedDiscovery.uuid
    }

    if (existingMarker != null) {
        mapView.overlays.remove(existingMarker)
    }

    // Ajouter le nouveau marqueur mis à jour
    val marker = Marker(mapView).apply {
        position = GeoPoint(updatedDiscovery.latitude, updatedDiscovery.longitude)
        title = updatedDiscovery.title
        snippet = updatedDiscovery.description
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.icon = icon
        subDescription = updatedDiscovery.uuid

        setOnMarkerClickListener { _, _ ->
            val intent = Intent(context, DiscoveryActivityXml::class.java).apply {
                putExtra("discovery", updatedDiscovery)
            }
            launcher.launch(intent)
            true
        }
    }

    mapView.overlays.add(marker)
    mapView.invalidate()
}
