package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


class MapActivity : ComponentActivity() {
    private val RequestPermissionsRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), RequestPermissionsRequestCode)
        }
    }
}
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var isFollowingLocation by remember { mutableStateOf(false) }
    var lastKnownPoint by remember { mutableStateOf(GeoPoint(48.8583, 2.2944)) }

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
            if (updated != null) {
                val location = lastKnownPoint
                val newDiscovery = updated.copy(latitude = location.latitude, longitude = location.longitude)
                val existing = getDiscoveries(context).toMutableList()
                existing.add(newDiscovery)
                saveDiscoveries(context, existing)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val configuration = Configuration.getInstance()
        configuration.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Map(
                modifier = Modifier.padding(innerPadding),
                isFollowingLocation = isFollowingLocation,
                onMapInteraction = { isFollowingLocation = false },
                onLocationChanged = { lastKnownPoint = it }
            )
        }

        // Menu en haut à gauche
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            MenuWithDropdown()
        }

        // Bouton suivre en bas à droite
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FollowButton(isFollowing = isFollowingLocation) {
                isFollowingLocation = !isFollowingLocation
            }
        }

        // Nouveau bouton Ping en bas au centre
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // remonte le bouton
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        val intent = Intent(context, DiscoveryActivity::class.java).apply {
                            putExtra(
                                "discovery",
                                Discovery(
                                    "Nouveau ping",
                                    "Description ici",
                                    R.drawable.cat03,
                                    lastKnownPoint.latitude,
                                    lastKnownPoint.longitude
                                )
                            )
                        }
                        launcher.launch(intent)
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ping),
                        contentDescription = "Ping",
                        tint = Color.Unspecified, // pas de recoloration, conserve ton image
                        modifier = Modifier.size(48.dp)
                            .offset(x = 1.dp, y = (-3).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(if (isFollowing) "Arrêter" else "Suivre")
    }
}

@Composable
fun Map(
    modifier: Modifier = Modifier,
    isFollowingLocation: Boolean,
    onMapInteraction: () -> Unit,
    onLocationChanged: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { }

    AndroidView(
        modifier = modifier,
        factory = { mapContext ->
            MapView(mapContext).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                myLocationOverlay.enableMyLocation()
                overlays.add(myLocationOverlay)

                controller.setZoom(15.0)
                myLocationOverlay.runOnFirstFix {
                    val location = myLocationOverlay.myLocation ?: GeoPoint(48.8583, 2.2944)
                    onLocationChanged(location)
                    post {
                        controller.setCenter(location)
                    }
                }

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
                        overlays.removeIf { it is Marker && it.title != null }
                        addDiscoveryMarkers(this@apply, context, launcher)
                        return true
                    }
                })

                addDiscoveryMarkers(this, context, launcher)
            }
        },
        update = { mapView ->
            val myLocationOverlay = mapView.overlays.firstOrNull { it is MyLocationNewOverlay } as? MyLocationNewOverlay
            myLocationOverlay?.myLocation?.let { onLocationChanged(it) }

            if (isFollowingLocation) {
                myLocationOverlay?.enableFollowLocation()
                myLocationOverlay?.myLocation?.let { mapView.controller.animateTo(it) }
            } else {
                myLocationOverlay?.disableFollowLocation()
            }
        }
    )
}

fun addDiscoveryMarkers(
    mapView: MapView,
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val savedDiscoveries = getDiscoveries(context)
    val zoomLevel = mapView.zoomLevelDouble
    val scaleFactor = (zoomLevel / 15.0).toFloat().coerceIn(0.5f, 2.0f)

    val originalDrawable = ContextCompat.getDrawable(context, R.drawable.pinged)
    val bitmap = originalDrawable?.let { drawable ->
        val width = (drawable.intrinsicWidth * scaleFactor).toInt()
        val height = (drawable.intrinsicHeight * scaleFactor).toInt()
        val bitmap = createBitmap(width, height)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap
    }

    val scaledDrawable = bitmap?.toDrawable(context.resources)

    savedDiscoveries.forEach { discovery ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(discovery.latitude, discovery.longitude)
            title = discovery.title
            subDescription = discovery.description
            icon = scaledDrawable
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
