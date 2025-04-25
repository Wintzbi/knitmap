package com.example.myapplication

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myapplication.storage.getPings
import com.example.myapplication.storage.savePings
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.Serializable

import com.example.myapplication.discovery.Discovery
import com.example.myapplication.discovery.DiscoveryActivity
import com.example.myapplication.discovery.DiscoveryListActivity
import com.example.myapplication.discovery.DiscoveryScreen
import com.example.myapplication.discovery.DiscoveryListScreen

import com.example.myapplication.storage.saveAmis
import com.example.myapplication.storage.saveGroupes
import com.example.myapplication.storage.savePings
import com.example.myapplication.storage.saveVoyages

data class Ping(
    val latitude: Double,
    val longitude: Double,
    val titre: String,
    val description: String,
    val imageUri: String
) : Serializable

class MapActivity : ComponentActivity() {
    private val RequestPermissionsRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle permissions
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
            )
        )
        enableEdgeToEdge()
        setContent {
            MapScreen()
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = ArrayList<String>()
        permissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                RequestPermissionsRequestCode
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        for (i in grantResults.indices) {
            val result = grantResults[i]
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permissions[i])
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                RequestPermissionsRequestCode
            )
        }
    }
}

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var isFollowingLocation by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
            if (updated != null) {
                val location = GeoPoint(48.8583, 2.2944) // Remplace par position réelle si possible
                val newPing = Ping(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    titre = updated.title,
                    description = updated.description,
                    imageUri = "" // À remplir si image dispo
                )
                val existing = getPings(context).toMutableList()
                existing.add(newPing)
                savePings(context, existing)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val configuration = Configuration.getInstance()
        configuration.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Map(Modifier.padding(innerPadding), isFollowingLocation) {
                isFollowingLocation = false
            }
        }

        // Menu en haut à gauche
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            MenuWithDropdown()
        }

        // Bouton Suivre en bas à droite
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FollowButton(isFollowing = isFollowingLocation) {
                isFollowingLocation = !isFollowingLocation
            }
        }

        // 🚀 Bouton Ping en bas au centre
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Button(onClick = {
                val intent = Intent(context, DiscoveryActivity::class.java).apply {
                    putExtra(
                        "discovery",
                        Discovery("Nouveau ping", "Description ici", R.drawable.cat03, 0.0, 0.0) // coordonnées par défaut
                    )
                }
                launcher.launch(intent)
            }) {
                Text("Ping")
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
fun Map(modifier: Modifier = Modifier, isFollowingLocation: Boolean, onMapInteraction: () -> Unit) {
    val context = LocalContext.current
    // Use remember and mutableStateOf to preserve the state across recompositions.
    var lastKnownPoint by remember { mutableStateOf(GeoPoint(48.8583, 2.2944)) } // Default: Eiffel Tower

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // My Location Overlay
                val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                myLocationOverlay.enableMyLocation()
                overlays.add(myLocationOverlay)

                // Set initial center and zoom
                controller.setCenter(lastKnownPoint)
                controller.setZoom(15.0)

                // Center on the user's location if available
                myLocationOverlay.runOnFirstFix {
                    lastKnownPoint = myLocationOverlay.myLocation ?: lastKnownPoint // Update the state with the current position
                    post {
                        controller.setCenter(lastKnownPoint)
                    }
                }
                if(isFollowingLocation){
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
                        return true
                    }
                })
            }
        },
        update = { mapView ->
            // Get a reference to the MyLocationNewOverlay
            val myLocationOverlay =
                mapView.overlays.firstOrNull { it is MyLocationNewOverlay } as? MyLocationNewOverlay

            // Update lastKnownPoint if the overlay has a new location
            if (myLocationOverlay != null && myLocationOverlay.myLocation != null) {
                lastKnownPoint = myLocationOverlay.myLocation
            }
            if(isFollowingLocation){
                myLocationOverlay?.enableFollowLocation()
                myLocationOverlay?.myLocation?.let {mapView.controller.animateTo(it)}
            }else{
                myLocationOverlay?.disableFollowLocation()
            }

            // Set the map center and zoom
            mapView.controller.setCenter(lastKnownPoint)
            mapView.controller.setZoom(15.0)
        }
    )
}