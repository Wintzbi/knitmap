package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete



data class Travel(
    var title: String,
    var description: String,
    var imageResId: Int
) : java.io.Serializable

class TravelListActivity : ComponentActivity() {
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

    val travels = remember {
        mutableStateListOf(
            Travel("Paris", "Ville de lumière et d’amour.", R.drawable.paris),
            Travel("Tokyo", "Ville ultra-moderne.", R.drawable.tokyo),
            Travel("New York", "La ville qui ne dort jamais.", R.drawable.newyork)
        )
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedDiscovery") as? Travel
            if (updated != null && selectedIndex in travels.indices) {
                travels[selectedIndex] = updated
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

        // Menu en haut à gauche (il est automatiquement superposé à l'image)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp) // Assure-toi que le menu est à l'extérieur de l'image
        ) {
            MenuWithDropdown()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Text(
                "Découvertes",
                fontSize = 32.sp,
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                travels.forEachIndexed { index, item ->
                    Card(
                        onClick = {
                            selectedIndex = index
                            val intent = Intent(context, DiscoveryActivity::class.java)
                            intent.putExtra("discovery", item)
                            launcher.launch(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
                            Spacer(modifier = Modifier.weight(1f)) // Espacement pour que le bouton "supprimer" soit à droite
                            IconButton(onClick = {
                                travels.removeAt(index) // Supprime la découverte de la liste
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

            Button(
                onClick = {
                    val newTravel = Travel("Nouveaux voyage", "Description temporaire", R.drawable.cat03)
                    travels.add(newTravel) // Ajout de la nouvelle découverte à la liste
                    selectedIndex = travels.indexOf(newTravel) // Mettre à jour l'index sélectionné

                    val intent = Intent(context, TravelActivity::class.java)
                    intent.putExtra("discovery", newTravel) // Envoi de la nouvelle découverte à l'activité
                    launcher.launch(intent) // Lancer l'activité avec la nouvelle découverte
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Ajouter un voyage")
            }
        }
    }
}
