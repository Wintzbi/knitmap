package com.knitMap.discovery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import coil.compose.rememberAsyncImagePainter
import com.knitMap.BaseActivity
import com.knitMap.MainActivity.Companion.isOnline
import com.knitMap.MenuWithDropdown
import com.knitMap.R
import com.knitMap.storage.getDiscoveriesLocally
import com.knitMap.storage.removeDiscoveryByUuidLocally
import com.knitMap.storage.saveDiscoveriesLocally
import com.knitMap.ui.theme.MyApplicationTheme
import com.knitMap.utils.getLocationFromCoordinates
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


data class Discovery(
    var title: String,
    var description: String,
    var imageResId: Int = R.drawable.cat03,
    val latitude: Double,
    val longitude: Double,
    var imageUri: String? = null,
    var locationName: String = "Lieu inconnu", // <-- Ajouté ici
    val uuid: String = UUID.randomUUID().toString(),
    val date: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
) : Serializable


class DiscoveryListActivity : BaseActivity() {
    private lateinit var locationManager: LocationManager

    private val _currentLocation = mutableStateOf(GeoPoint(48.8583, 2.2944))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                DiscoveryListScreen(currentLocation = _currentLocation)
            }
        }
        // Initialiser le LocationManager ici, dans onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _currentLocation.value = GeoPoint(location.latitude, location.longitude)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Vérifier si la permission est accordée avant de demander la localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
        } else {
            // Demander la permission si elle n'est pas encore accordée
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Si la permission est accordée après la demande, on peut démarrer la localisation
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    _currentLocation.value = GeoPoint(location.latitude, location.longitude)
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        }
    }
}

@Composable
fun DiscoveryListScreen(currentLocation: State<GeoPoint>) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val location = currentLocation.value
    val discoveries = remember {
        mutableStateListOf<Discovery>().apply {
            addAll(getDiscoveriesLocally(context).map { old ->
                var updated = old

                if (old.uuid.isBlank()) {
                    updated = updated.copy(uuid = UUID.randomUUID().toString())
                }

                if (old.date.isBlank()) {
                    updated = updated.copy(date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                }

                if (old.locationName.isNullOrBlank()) {
                    updated = updated.copy(locationName = "Lieu inconnu")
                }

                updated
            })

        }
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Discovery
            if (updated != null) {
                val index = discoveries.indexOfFirst {
                    it.uuid == updated.uuid
                }

                if (index != -1) {
                    discoveries[index] = updated
                } else {
                    discoveries.add(updated)
                }

                saveDiscoveriesLocally(context, discoveries)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.voyage_liste_fond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Text(
                "Découvertes",
                fontSize = 32.sp,
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Bouton Ajouter
            androidx.compose.material3.Button(
                onClick = {
                    val lat = location.latitude
                    val lon = location.longitude

                    var newDiscovery = Discovery(
                        title = "",
                        description = "",
                        imageResId = R.drawable.cat03,
                        latitude = lat,
                        longitude = lon,
                        locationName = "Lieu inconnu"
                    )

                    if (isOnline) {
                        coroutineScope.launch {
                            getLocationFromCoordinates(lat, lon) { resolvedLocation ->
                                newDiscovery = newDiscovery.copy(locationName = resolvedLocation)

                                discoveries.add(newDiscovery)
                                saveDiscoveriesLocally(context, discoveries)

                                val intent = Intent(context, DiscoveryActivityXml::class.java)
                                intent.putExtra("discovery", newDiscovery)
                                launcher.launch(intent)
                            }
                        }
                    } else {
                        discoveries.add(newDiscovery)
                        saveDiscoveriesLocally(context, discoveries)

                        val intent = Intent(context, DiscoveryActivityXml::class.java)
                        intent.putExtra("discovery", newDiscovery)
                        launcher.launch(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("+ Ajouter")
            }

            // Liste scrollable
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                itemsIndexed(
                    discoveries.sortedByDescending {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.date)
                    }
                ) { index, item ->
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = Color(0xFF4E7072).copy(alpha = 0.8f),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                selectedIndex = index
                                val intent = Intent(context, DiscoveryActivityXml::class.java)
                                intent.putExtra("discovery", item)
                                launcher.launch(intent)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val painter = when {
                                item.imageUri?.startsWith("content://") == true -> rememberAsyncImagePainter(model = Uri.parse(item.imageUri))
                                item.imageUri?.startsWith("/") == true -> rememberAsyncImagePainter(model = File(item.imageUri))
                                else -> rememberAsyncImagePainter(model = R.drawable.cat03)  // image par défaut si aucune image
                            }

                            Image(
                                painter = painter,
                                contentDescription = item.title,
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = item.title, fontSize = 20.sp, color = Color.White)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val sortedList = discoveries.sortedByDescending {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.date)
                                }
                                val toDelete = sortedList[index]
                                removeDiscoveryByUuidLocally(context,toDelete.uuid)
                                discoveries.removeIf { it.uuid == toDelete.uuid }
                                saveDiscoveriesLocally(context, discoveries)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }

        }

        Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            MenuWithDropdown()
        }
    }
}