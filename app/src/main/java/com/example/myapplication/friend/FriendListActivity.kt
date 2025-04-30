package com.example.myapplication.friend

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.myapplication.BaseActivity
import com.example.myapplication.MenuWithDropdown
import com.example.myapplication.R
import com.example.myapplication.components.GenericListWithControls
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.storage.getFriends
import com.example.myapplication.storage.saveFriends
import com.example.myapplication.storage.removeFriendByName

data class Friend(
    var title: String,
    var description: String,
    var imageResId: Int
) : java.io.Serializable

class FriendListActivity : BaseActivity() {
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

    // Chargement des amis depuis les préférences
    val friends = remember {
        mutableStateListOf<Friend>().apply {
            val savedFriends = getFriends(context)
            addAll(savedFriends)
        }
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedFriend") as? Friend
            if (updated != null && selectedIndex in friends.indices) {
                friends[selectedIndex] = updated
                saveFriends(context, friends)  // Sauvegarde les modifications
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.amis_fond),
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
                "Amis",
                fontSize = 32.sp,
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GenericListWithControls(
                items = friends,
                onAdd = {
                    val newFriend = Friend("Nouvel ami", "Description temporaire", R.drawable.cat03)
                    friends.add(newFriend)
                    selectedIndex = friends.indexOf(newFriend)

                    // Sauvegarder le nouvel ami dans la liste
                    saveFriends(context, friends)

                    val intent = Intent(context, FriendsActivity::class.java)
                    intent.putExtra("Friend", newFriend)
                    launcher.launch(intent)
                },
                onDelete = { index ->
                    friends.removeAt(index)
                    // Sauvegarder après suppression
                    saveFriends(context, friends)
                },
                itemContent = { item, index, onDeleteClick ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                selectedIndex = index
                                val intent = Intent(context, FriendsActivity::class.java)
                                intent.putExtra("Friend", item)
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
