package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.discovery.Discovery
import com.example.myapplication.discovery.DiscoveryActivity
import com.example.myapplication.storage.getDiscoveries
import com.example.myapplication.storage.saveDiscoveries
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri

import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image



class MapActivity : BaseActivity() {
    private val requestPermissionsRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cacher les barres système : plein écran immersif
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
            )
        )
        enableEdgeToEdge()
        setContent {
            MapScreen()
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                requestPermissionsRequestCode
            )
        }
    }
}
// Bouton Ping
@Composable
fun PingImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Ajoute d'un paramètre pour modifier pos
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .background(
                color = Color(0xFFFBED),
                shape = CircleShape

            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ping),
            contentDescription = "Ajouter un ping",
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )
    }
}


@Composable
fun MapScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var isFollowingLocation by remember { mutableStateOf(false) }
    var lastKnownPoint by remember { mutableStateOf(GeoPoint(48.8583, 2.2944)) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
            if (updated != null) {
                val location = lastKnownPoint
                val newDiscovery = updated.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    imageUri = updated.imageUri
                )
                val existing = getDiscoveries(context).toMutableList()
                existing.add(newDiscovery)
                saveDiscoveries(context, existing)
                Toast.makeText(context, "Discovery ajoutée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Lancer DiscoveryActivity avec ou sans image selon que l'utilisateur a pris une photo
    fun launchDiscoveryWithImage(uri: Uri?) {
        val discovery = Discovery(
            title = "Nouveau ping",
            description = "Description ici",
            imageResId = R.drawable.cat03,
            imageUri = uri?.toString(),
            latitude = lastKnownPoint.latitude,
            longitude = lastKnownPoint.longitude
        )
        val intent = Intent(context, DiscoveryActivity::class.java).apply {
            putExtra("discovery", discovery)
        }
        launcher.launch(intent)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            launchDiscoveryWithImage(photoUri)
        } else {
            // Si annulation => image par défaut
            launchDiscoveryWithImage(null)
        }
    }

    fun startCameraIntent() {
        val photoFile = File.createTempFile(
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}",
            ".jpg",
            context.cacheDir
        )
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
        photoUri = uri
        cameraLauncher.launch(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val configuration = Configuration.getInstance()
        configuration.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Map(
                modifier = Modifier.padding(innerPadding),
                isFollowingLocation = isFollowingLocation,
                onMapInteraction = {
                    if (!isFollowingLocation) {
                        isFollowingLocation = false
                    }
                },
                onLocationChanged = { lastKnownPoint = it },
                launcher = launcher,
                mapViewRef = mapViewRef
            )
        }

        // Image en haut de l'écran avec menu à gauche
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            // Image de fond de la barre supérieure
            Image(
                painter = painterResource(id = R.drawable.up_map_fond),
                contentDescription = "Top Bar Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            // Menu à gauche de la barre
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                MenuWithDropdown()
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                // FOLLOW BUTTON - premier plan et sans fond
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            isFollowingLocation = !isFollowingLocation
                            if (isFollowingLocation) {
                                mapViewRef.value?.controller?.setZoom(17.5)
                                mapViewRef.value?.controller?.animateTo(lastKnownPoint)
                            }
                        }
                ) {
                    Icon(
                        painter = painterResource(id = if (isFollowingLocation) R.drawable.ping else R.drawable.ping),  // Même icône pour les deux états
                        contentDescription = if (isFollowingLocation) "Stop following" else "Start following",
                        tint = if (isFollowingLocation) Color.Blue else Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // Image en bas de l'écran avec bouton ping centré qui lance la caméra
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Image de fond du footer
            Image(
                painter = painterResource(id = R.drawable.map_fond_bouton),
                contentDescription = "Footer Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            // Bouton ping qui lance la caméra
            PingImageButton(
                onClick = { startCameraIntent() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-10).dp, x = (10).dp) // ajuste la valeur ici pour bien centrer
            )
        }
    }
}
@Composable
fun Map(
    modifier: Modifier = Modifier,
    isFollowingLocation: Boolean,
    onMapInteraction: () -> Unit,
    onLocationChanged: (GeoPoint) -> Unit,
    launcher: ActivityResultLauncher<Intent>,
    mapViewRef: MutableState<MapView?>
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { mapContext ->
            MapView(mapContext).apply {
                mapViewRef.value = this

                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                val gpsLocationProvider = GpsMyLocationProvider(context)
                val myLocationOverlay = MyLocationNewOverlay(gpsLocationProvider, this)
                myLocationOverlay.enableMyLocation()
                overlays.add(myLocationOverlay)

                controller.setZoom(15.0)

                // Ajout du ScratchOverlay avec une priorité inférieure pour qu'il soit sous l'UI
                val scratchOverlay = ScratchOverlay(this)
                // Assurez-vous d'ajouter le scratch overlay en premier (il sera dessiné en premier, donc en dessous)
                overlays.add(0, scratchOverlay)

                myLocationOverlay.runOnFirstFix {
                    val location = myLocationOverlay.myLocation ?: GeoPoint(48.8583, 2.2944)
                    onLocationChanged(location)
                    post {
                        controller.setCenter(location)
                        controller.setZoom(17.5)
                    }
                    scratchOverlay.scratchAt(location)
                }

                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        scratchOverlay.scratchAt(geoPoint)
                        onLocationChanged(geoPoint)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@apply
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)

                if (isFollowingLocation) {
                    myLocationOverlay.enableFollowLocation()
                } else {
                    myLocationOverlay.disableFollowLocation()
                }

                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        onMapInteraction()
                        return true
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        onMapInteraction()
                        overlays.removeIf { it is Marker && it.title != null }
                        addDiscoveryMarkers(this@apply, context, launcher)
                        return true
                    }
                })

                addDiscoveryMarkers(this, context, launcher)
            }
        },
        update = { mapView ->
            val myLocationOverlay =
                mapView.overlays.firstOrNull { it is MyLocationNewOverlay } as? MyLocationNewOverlay
            myLocationOverlay?.myLocation?.let { location ->
                onLocationChanged(location)
            }

            if (isFollowingLocation) {
                myLocationOverlay?.enableFollowLocation()
                myLocationOverlay?.myLocation?.let {
                    mapView.controller.setZoom(17.5)
                    mapView.controller.animateTo(it)
                }
            } else {
                myLocationOverlay?.disableFollowLocation()
            }
        }
    )
}

private fun addDiscoveryMarkers(
    mapView: MapView,
    context: Context,
    launcher: ActivityResultLauncher<Intent>
) {
    val discoveries = getDiscoveries(context)
    val iconDrawable = ContextCompat.getDrawable(context, R.drawable.pinged)

    discoveries.forEach { discovery ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(discovery.latitude, discovery.longitude)
            title = discovery.title
            snippet = discovery.description
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = iconDrawable
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
