package com.example.myapplication

import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.example.myapplication.discovery.DiscoveryListActivity
import com.example.myapplication.friend.FriendListActivity
import com.example.myapplication.group.GroupListActivity
import com.example.myapplication.travel.TravelListActivity

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MenuWithDropdown() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }

    val enterTransition = slideInVertically(initialOffsetY = { -40 }) + fadeIn(initialAlpha = 0.3f)
    val exitTransition = slideOutHorizontally(targetOffsetX = { -200 }) + fadeOut(targetAlpha = 0.3f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu"
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = enterTransition,
            exit = exitTransition
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
                    .wrapContentSize()
            ) {
                // Cadre autour
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            menuSize = coordinates.size
                        }
                        .size(
                            width = with(LocalDensity.current) { menuSize.width.toDp() },
                            height = with(LocalDensity.current) { menuSize.height.toDp() }
                        )
                        .border(
                            width = 4.dp,
                            color = Color.Transparent,
                            shape = RoundedCornerShape(0.dp)
                        )
                ) {
                    // 4 lignes superposées via Box
                    Box(modifier = Modifier
                        .matchParentSize()
                        .zIndex(1f)) {
                        // Ligne en haut
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color(0xFFD1EAD3))
                                .align(Alignment.TopCenter)
                        )
                        // Ligne en bas
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color(0xFFFFE6A7))
                                .align(Alignment.BottomCenter)
                        )
                        // Ligne gauche
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(Color(0xFFEB6B63))
                                .align(Alignment.CenterStart)
                        )
                        // Ligne droite
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(Color(0xFFADD3F5))
                                .align(Alignment.CenterEnd)
                        )
                    }
                }

                // Menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(Color(0xFFFFFCED))
                        .clip(RoundedCornerShape(8.dp))
                        .zIndex(2f) // au-dessus des lignes
                ) {
                    DropdownMenuItem(text = { Text("Profil") }, onClick = {
                        expanded = false
                        context.startActivity(Intent(context, ProfilActivity::class.java))
                    })
                    DropdownMenuItem(text = { Text("Map") }, onClick = {
                        expanded = false
                        context.startActivity(Intent(context, MapActivity::class.java))
                    })
                    DropdownMenuItem(text = { Text("Voyages") }, onClick = {
                        expanded = false
                        context.startActivity(Intent(context, TravelListActivity::class.java))
                    })
                    DropdownMenuItem(text = { Text("Amis") }, onClick = {
                        expanded = false
                        context.startActivity(Intent(context, FriendListActivity::class.java))
                    })
                    DropdownMenuItem(text = { Text("Groupe") }, onClick = {
                        expanded = false
                        context.startActivity(Intent(context, GroupListActivity::class.java))
                    })
                    DropdownMenuItem(text = { Text("Découverte") }, onClick = {
                        expanded = false
                        context.startActivity(Intent(context, DiscoveryListActivity::class.java))
                    })
                    DropdownMenuItem(text = { Text("Déconnexion") }, onClick = {
                        expanded = false
                        FirebaseAuth.getInstance().signOut()
                        val logoutIntent = Intent(context, MainActivity::class.java)
                        logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(logoutIntent)
                    })
                }
            }
        }
    }
}
