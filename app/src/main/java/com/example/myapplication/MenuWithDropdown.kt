package com.example.myapplication

import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import com.example.myapplication.discovery.DiscoveryListActivity
import com.example.myapplication.friend.FriendListActivity
import com.example.myapplication.group.GroupListActivity
import com.example.myapplication.travel.TravelListActivity


@Composable
fun MenuWithDropdown() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Map") },
                onClick = {
                    expanded = false
                    context.startActivity(Intent(context, MapActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Voyages") },
                onClick = {
                    expanded = false
                    context.startActivity(Intent(context, TravelListActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Amis") },
                onClick = {
                    expanded = false
                    context.startActivity(Intent(context, FriendListActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Groupe") },
                onClick = {
                    expanded = false
                    context.startActivity(Intent(context, GroupListActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Découverte") },
                onClick = {
                    expanded = false
                    context.startActivity(Intent(context, DiscoveryListActivity::class.java))
                }
            )
            DropdownMenuItem(
                text = { Text("Déconnexion") },
                onClick = {
                    expanded = false
                    FirebaseAuth.getInstance().signOut() // <- Déconnecte l'utilisateur
                    val logoutIntent = Intent(context, MainActivity::class.java)
                    logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(logoutIntent)
                }
            )


        }
    }
}
