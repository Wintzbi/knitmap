package com.example.myapplication.discovery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.BaseActivity
import com.example.myapplication.MenuWithDropdown
import com.example.myapplication.R
import com.example.myapplication.components.GenericListWithControls
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.storage.getDiscoveries
import com.example.myapplication.storage.saveDiscoveries
import java.io.Serializable

data class Discovery(
    var title: String,
    var description: String,
    var imageResId: Int = R.drawable.cat03,
    val latitude: Double,
    val longitude: Double,
    var imageUri: String? = null
) : Serializable

class DiscoveryListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                DiscoveryListScreen()
            }
        }
    }
}

@Composable
fun DiscoveryListScreen() {
    val context = LocalContext.current
    val discoveries = remember {
        mutableStateListOf<Discovery>().apply {
            addAll(getDiscoveries(context))
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
                    it.latitude == updated.latitude && it.longitude == updated.longitude
                }

                if (index != -1) {
                    discoveries[index] = updated
                } else {
                    discoveries.add(updated) // au cas où elle n'était pas dans la liste
                }

                saveDiscoveries(context, discoveries)
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

        Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            MenuWithDropdown()
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Text("Découvertes", fontSize = 32.sp, color = Color(0xFF4E7072).copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 16.dp))

            GenericListWithControls(
                items = discoveries,
                onAdd = {
                    val newDiscovery = Discovery("", "", R.drawable.cat03, 0.0, 0.0,null)
                    discoveries.add(newDiscovery)
                    selectedIndex = discoveries.indexOf(newDiscovery)
                    saveDiscoveries(context, discoveries)
                    val intent = Intent(context, DiscoveryActivity::class.java)
                    intent.putExtra("discovery", newDiscovery)
                    launcher.launch(intent)
                },
                onDelete = { index ->
                    discoveries.removeAt(index)
                    saveDiscoveries(context, discoveries)
                },
                itemContent = { item, index, onDeleteClick ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                selectedIndex = index
                                val intent = Intent(context, DiscoveryActivity::class.java)
                                intent.putExtra("discovery", item)
                                launcher.launch(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val painter = if (item.imageUri != null)
                            rememberAsyncImagePainter(model = item.imageUri)
                        else
                            painterResource(id = item.imageResId)

                        Image(
                            painter = painter,
                            contentDescription = item.title,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = item.title, fontSize = 20.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onDeleteClick) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red)
                        }
                    }
                }
            )
        }
    }
}
