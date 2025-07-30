package com.knitMap.travel

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
import com.knitMap.BaseActivity
import com.knitMap.MenuWithDropdown
import com.knitMap.R
import com.knitMap.GenericListWithControls
import com.knitMap.discovery.Discovery
import com.knitMap.ui.theme.MyApplicationTheme
import com.knitMap.storage.getTravels
import com.knitMap.storage.saveTravels

data class Travel(
    var title: String,
    var description: String,
    var imageResId: Int,
    val pings: List<Discovery> = emptyList()
) : java.io.Serializable

class TravelListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                TravelListScreen()
            }
        }
    }
}

@Composable
fun TravelListScreen() {
    val context = LocalContext.current

    // Chargement des voyages depuis les préférences
    val travels = remember {
        mutableStateListOf<Travel>().apply {
            val savedTravels = getTravels(context)
            addAll(savedTravels)
        }
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedTravel") as? Travel
            if (updated != null && selectedIndex in travels.indices) {
                travels[selectedIndex] = updated
                saveTravels(context, travels)  // Sauvegarde les modifications
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

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            MenuWithDropdown()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Text(
                "Playlists de vos voyages",
                fontSize = 32.sp,
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GenericListWithControls(
                items = travels,
                onAdd = {
                    val newTravel = Travel(
                        "Nouveau voyage",
                        "Description temporaire",
                        R.drawable.cat03
                    )
                    travels.add(newTravel)
                    selectedIndex = travels.indexOf(newTravel)

                    // Sauvegarder le nouveau voyage dans la liste
                    saveTravels(context, travels)

                    val intent = Intent(context, TravelActivity::class.java)
                    intent.putExtra("Travel", newTravel)
                    launcher.launch(intent)
                },
                onDelete = { index ->
                    travels.removeAt(index)
                    // Sauvegarder après suppression
                    saveTravels(context, travels)
                },
                itemContent = { item, index, onDeleteClick ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                selectedIndex = index
                                val intent = Intent(context, TravelActivity::class.java)
                                intent.putExtra("Travel", item)
                                launcher.launch(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = item.title,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = item.title, fontSize = 20.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = Color.Red
                            )
                        }
                    }
                }
            )
        }
    }
}
