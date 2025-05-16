package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.myapplication.discovery.Discovery
import com.example.myapplication.discovery.DiscoveryActivity
import com.example.myapplication.storage.getDiscoveries
import com.example.myapplication.storage.saveDiscoveries
import com.example.myapplication.storage.saveLastKnownPoint
import com.example.myapplication.storage.getLastKnownPoint
import com.example.myapplication.storage.removeLastKnownPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MapActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var discoveryLauncher: ActivityResultLauncher<Intent>
    private var photoUri: Uri? = null
    private var lastKnownPoint: GeoPoint? = null
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid",
            MODE_PRIVATE
        ))
        setContentView(R.layout.activity_map)

        setupLayout()

        requestPermissionsIfNecessary()

        discoveryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
                updated?.let { it ->
                    if (lastKnownPoint != null) {
                        val discovery = it.copy(
                            latitude = lastKnownPoint!!.latitude,
                            longitude = lastKnownPoint!!.longitude,
                            imageUri = it.imageUri
                        )
                        val existing = getDiscoveries(this).map {
                            if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
                            if (it.date.isBlank()) it.copy(date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) else it
                        }.toMutableList()
                        val index = existing.indexOfFirst { it.uuid == updated.uuid }

                        if (index != -1) {
                            existing[index] = updated
                        } else {
                            existing.add(updated)
                        }
                        saveDiscoveries(this, existing)
                        addDiscoveryMarkers(mapView, this, discoveryLauncher)
                        Toast.makeText(this, "Discovery ajoutée", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Position inconnue, impossible d'enregistrer la découverte.", Toast.LENGTH_LONG).show()
                    }

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
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } else {
                    startCameraIntent(this@MapActivity, cameraLauncher) { uri -> photoUri = uri }
                }
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
        mapView.minZoomLevel = 3.0
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val discoveries = getDiscoveries(this).map {
            if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
            if (it.date.isBlank()) it.copy(date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) else it
        }

        val lastDiscovery = discoveries.lastOrNull()
        val lastDiscoveryPoint = lastDiscovery?.let { GeoPoint(it.latitude, it.longitude) }

        val scratchOverlay = ScratchOverlay(mapView)
        mapView.overlays.add(0, scratchOverlay)

        val provider = GpsMyLocationProvider(this)
        val locationOverlay = MyLocationNewOverlay(provider, mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // ✅ Récupération immédiate de la dernière position connue
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastLocation?.let {
                lastKnownPoint = GeoPoint(it.latitude, it.longitude)
                saveLastKnownPoint(this, lastKnownPoint!!)
            }

            // ✅ Ecoute des mises à jour en temps réel
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
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener) // facultatif, mais améliore la vitesse
        }

        addDiscoveryMarkers(mapView, this, discoveryLauncher)

        locationOverlay.runOnFirstFix {
            runOnUiThread {
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


    override fun onPause() {
        super.onPause()
        getLastKnownPoint(this@MapActivity)?.let { removeLastKnownPoint(this@MapActivity) }
        lastKnownPoint?.let { saveLastKnownPoint(this@MapActivity, it) }
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
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "IMG_$timestamp.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KnitMapPictures")
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (imageUri != null) {
        onUriReady(imageUri)
        Handler(Looper.getMainLooper()).post {
            launcher.launch(imageUri)
        }
    } else {
        Toast.makeText(context, "Erreur : impossible de créer l'image", Toast.LENGTH_SHORT).show()
    }
}


fun addDiscoveryMarkers(mapView: MapView, context: Context, launcher: ActivityResultLauncher<Intent>) {
    val discoveries = getDiscoveries(context).map {
        if (it.uuid.isBlank()) it.copy(uuid = UUID.randomUUID().toString()) else it
        if (it.date.isBlank()) it.copy(date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) else it
    }

    val icon = ContextCompat.getDrawable(context, R.drawable.pinged)

    discoveries.forEach { discovery ->
        val alreadyExists = mapView.overlays.any { overlay ->
            (overlay as? Marker)?.let { marker ->
                marker.subDescription == discovery.uuid
            } == true
        }

        if (!alreadyExists) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(discovery.latitude, discovery.longitude)
                title = discovery.title
                snippet = discovery.description
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                this.icon = icon
                subDescription = discovery.uuid  // On stocke l'uuid ici

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
                    context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
        }
    })
}