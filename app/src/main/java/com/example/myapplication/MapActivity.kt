package com.example.myapplication

import android.Manifest
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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.myapplication.discovery.Discovery
import com.example.myapplication.discovery.DiscoveryActivity
import com.example.myapplication.storage.getDiscoveries
import com.example.myapplication.storage.getLastKnownPoint
import com.example.myapplication.storage.saveDiscoveries
import com.example.myapplication.storage.saveLastKnownPoint
import org.osmdroid.config.Configuration
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
    private var photoUri: Uri? = null
    private var lastKnownPoint = GeoPoint(48.8583, 2.2944)
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_map)

        setupLayout()

        requestPermissionsIfNecessary()

        discoveryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
                updated?.let { it ->
                    val discovery = it.copy(
                        latitude = lastKnownPoint.latitude,
                        longitude = lastKnownPoint.longitude,
                        imageUri = it.imageUri
                    )
                    val existing = getDiscoveries(this).map {
                        if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
                    }.toMutableList()
                    val index = existing.indexOfFirst { it.uuid == updated.uuid }

                    if (index != -1) {
                        existing[index] = updated
                    } else {
                        existing.add(updated)
                    }
                    saveDiscoveries(this, existing)

                    Toast.makeText(this, "Discovery ajoutée", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                launchDiscoveryWithImage(this, photoUri, lastKnownPoint, discoveryLauncher)
            } else {
                launchDiscoveryWithImage(this, null, lastKnownPoint, discoveryLauncher)
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
                startCameraIntent(this@MapActivity, cameraLauncher) { uri -> photoUri = uri }
            }
        }

        findViewById<ImageView>(R.id.Follow_Button).apply {
            setImageResource(R.drawable.target)
            setOnClickListener {
                isFollowing = !isFollowing
                val newColor = if (isFollowing) "#E7191F".toColorInt() else "#154043".toColorInt()
                setColorFilter(newColor, PorterDuff.Mode.SRC_IN)
                if (isFollowing) {
                    mapView.controller.setZoom(17.5)
                    mapView.controller.animateTo(lastKnownPoint)
                }
            }
        }

        val composeView = findViewById<ComposeView>(R.id.composeView)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MenuWithDropdown()
        }
        mapView = findViewById(R.id.activity_map)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val defaultParis = GeoPoint(48.8566, 2.3522)
        val discoveries = getDiscoveries(this).map {
            if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
        }

        val lastDiscovery = discoveries.lastOrNull()
        val lastDiscoveryPoint = lastDiscovery?.let { GeoPoint(it.latitude, it.longitude) }

        // --- AJOUT DU SCRATCH OVERLAY ---
        val scratchOverlay = ScratchOverlay(mapView)

        mapView.overlays.add(0, scratchOverlay)
        val provider = GpsMyLocationProvider(this)
        val locationOverlay = MyLocationNewOverlay(provider, mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)

        // Listener GPS pour mises à jour en temps réel
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastKnownPoint = GeoPoint(location.latitude, location.longitude)
                scratchOverlay.scratchAt(lastKnownPoint)
                saveLastKnownPoint(this@MapActivity, lastKnownPoint)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        }

        addDiscoveryMarkers(mapView, this, discoveryLauncher)

        locationOverlay.runOnFirstFix {
            val userLocation = locationOverlay.myLocation
            runOnUiThread {
                val savedPoint = getLastKnownPoint(this)
                val target = when {
                    userLocation != null -> GeoPoint(userLocation.latitude, userLocation.longitude)
                    savedPoint != null -> savedPoint
                    lastDiscoveryPoint != null -> lastDiscoveryPoint
                    else -> defaultParis
                }
                lastKnownPoint = target
                mapView.controller.setCenter(target)
                mapView.controller.setZoom(17.5)

                scratchOverlay.scratchAt(target)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveLastKnownPoint(this, lastKnownPoint)
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
fun launchDiscoveryWithImage(context: Context, uri: Uri?, point: GeoPoint, launcher: ActivityResultLauncher<Intent>) {
    val discovery = Discovery(
        title = "",
        description = "",
        imageResId = R.drawable.cat03,
        latitude = point.latitude,
        longitude = point.longitude,
        imageUri = uri?.toString()
    )
    val intent = Intent(context, DiscoveryActivity::class.java).apply {
        putExtra("discovery", discovery)
    }
    launcher.launch(intent)
}

fun startCameraIntent(context: Context, launcher: ActivityResultLauncher<Uri>, onUriReady: (Uri) -> Unit) {
    // Créer un fichier dans le dossier Pictures
    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val fileName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
    val file = File(picturesDir, fileName)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    onUriReady(uri)
    launcher.launch(uri)

    // Enregistrer dans la galerie après la prise de photo
    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
}


fun addDiscoveryMarkers(mapView: MapView, context: Context, launcher: ActivityResultLauncher<Intent>) {
    val discoveries = getDiscoveries(context).map {
        if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
    }
    val icon = ContextCompat.getDrawable(context, R.drawable.pinged)

    discoveries.forEach { discovery ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(discovery.latitude, discovery.longitude)
            title = discovery.title
            snippet = discovery.description
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.icon = icon  // <-- correction ici
            setOnMarkerClickListener { _, _ ->
                val intent = Intent(context, DiscoveryActivity::class.java).apply {
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