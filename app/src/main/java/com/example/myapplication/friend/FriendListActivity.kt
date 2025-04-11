package com.example.myapplication.friend

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.example.myapplication.MenuWithDropdown
import com.example.myapplication.R
import com.example.myapplication.components.GenericListWithControls

data class Friend(
    var title: String,
    var description: String,
    var imageResId: Int
) : java.io.Serializable

class FriendListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                FriendListScreen()
            }
        }
    }
}

@Composable
fun FriendListScreen() {
    val context = LocalContext.current

    // Liste des amis
    val friends = remember {
        mutableStateListOf(
            Friend("Leynaïck", "Ley", R.drawable.paris),
            Friend("Mathys", "Déku", R.drawable.tokyo),
            Friend("Clément", "Oni-chan", R.drawable.newyork)
        )
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    // Launcher pour récupérer un résultat de l'activité d'édition
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedFriend") as? Friend
            if (updated != null && selectedIndex in friends.indices) {
                friends[selectedIndex] = updated
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Image de fond
        Image(
            painter = painterResource(id = R.drawable.amis_fond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Menu en haut à gauche
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            MenuWithDropdown()
        }

        // Contenu de la liste d'amis
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Text(
                "Amis",
                fontSize = 32.sp,
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Liste des amis avec leur contenu et actions
            GenericListWithControls(
                items = friends,
                onAdd = {
                    val newFriend = Friend("Nouvel ami", "Description temporaire", R.drawable.cat03)
                    friends.add(newFriend) // Ajouter un nouvel ami
                    selectedIndex = friends.indexOf(newFriend)

                    val intent = Intent(context, FriendsActivity::class.java)
                    intent.putExtra("Friend", newFriend) // Passer le nouvel ami à l'activité
                    launcher.launch(intent)
                },
                onDelete = { index ->
                    friends.removeAt(index) // Supprimer un ami de la liste
                },
                itemContent = { item, index, onDeleteClick ->
                    Card(
                        onClick = {
                            selectedIndex = index
                            val intent = Intent(context, FriendsActivity::class.java)
                            intent.putExtra("Friend", item)
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
                            Spacer(modifier = Modifier.weight(1f)) // Pour placer le bouton "supprimer" à droite
                            IconButton(onClick = onDeleteClick) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}
