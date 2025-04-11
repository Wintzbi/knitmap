package com.example.myapplication.group

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

data class Group(
    var title: String,
    var description: String,
    var imageResId: Int
) : java.io.Serializable

class GroupListActivity : ComponentActivity() {
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

    val groups = remember {
        mutableStateListOf(
            Group("Groupe A", "Description du groupe A", R.drawable.paris),
            Group("Groupe B", "Description du groupe B", R.drawable.paris),
            Group("Groupe C", "Description du groupe C", R.drawable.paris)
        )
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getSerializableExtra("updatedGroup") as? Group
            if (updated != null && selectedIndex in groups.indices) {
                groups[selectedIndex] = updated
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
                .padding(16.dp)
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
                    val newGroup = Group("Nouveau Groupe", "Description temporaire", R.drawable.paris)
                    groups.add(newGroup)
                    selectedIndex = groups.indexOf(newGroup)

                    val intent = Intent(context, GroupActivity::class.java)
                    intent.putExtra("group", newGroup)
                    launcher.launch(intent)
                },
                onDelete = { index ->
                    groups.removeAt(index)
                },
                itemContent = { item, index, onDeleteClick ->
                    Card(
                        onClick = {
                            selectedIndex = index
                            val intent = Intent(context, GroupActivity::class.java)
                            intent.putExtra("group", item)
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
                }
            )
        }
    }
}
