package com.knitMap.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.knitMap.BaseActivity
import com.knitMap.MenuWithDropdown
import com.knitMap.R
import com.knitMap.GenericListWithControls
import com.knitMap.friend.Friend
import com.knitMap.ui.theme.MyApplicationTheme
import com.knitMap.storage.getGroups
import com.knitMap.storage.saveGroups

data class Group(
    var title: String,
    var description: String,
    var imageResId: Int,
    val amis: List<Friend> = emptyList()
) : java.io.Serializable

class GroupListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                GroupListScreen()
            }
        }
    }
}

@Composable
fun GroupListScreen() {
    val context = LocalContext.current

    // Chargement des groupes depuis les préférences
    val groups = remember {
        mutableStateListOf<Group>().apply {
            val savedGroups = getGroups(context)
            addAll(savedGroups)
        }
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedGroup") as? Group
            if (updated != null && selectedIndex in groups.indices) {
                groups[selectedIndex] = updated
                saveGroups(context, groups)  // Sauvegarde les modifications
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.groupe_fond),
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
                "Groupes",
                fontSize = 32.sp,
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GenericListWithControls(
                items = groups,
                onAdd = {
                    val newGroup = Group("Nouveau Groupe", "Description temporaire", R.drawable.cat03)
                    groups.add(newGroup)
                    selectedIndex = groups.indexOf(newGroup)

                    // Sauvegarder le nouveau groupe dans la liste
                    saveGroups(context, groups)

                    val intent = Intent(context, GroupActivity::class.java)
                    intent.putExtra("group", newGroup)
                    launcher.launch(intent)
                },
                onDelete = { index ->
                    groups.removeAt(index)
                    // Sauvegarder après suppression
                    saveGroups(context, groups)
                },
                itemContent = { item, index, onDeleteClick ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                selectedIndex = index
                                val intent = Intent(context, GroupActivity::class.java)
                                intent.putExtra("group", item)
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
