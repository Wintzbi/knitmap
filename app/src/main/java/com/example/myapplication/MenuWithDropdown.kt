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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.myapplication.discovery.DiscoveryListActivity
import androidx.compose.ui.zIndex

@Composable
fun MenuWithDropdown() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // Couleurs personnalisées
    val textColor = Color(0xFF154043)     // Couleur du texte et des séparateurs
    val backgroundColor = Color(0xFFFFFBED) // Couleur de fond du menu (beige très clair)
    val borderColor = Color(0xFFE7191F)    // Couleur de la bordure (rouge)

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
            exit = exitTransition,
            modifier = Modifier.zIndex(1f) // 👈 ici
        ) {
            Box(
                modifier = Modifier
                    .zIndex(1f) // 👈 aussi ici pour être sûr
                    .padding(start = 16.dp, top = 80.dp)
                    .wrapContentSize()
                    .widthIn(max = 150.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(backgroundColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
            ) {
                Column {
                    MenuItem("Profil", textColor) {
                        expanded = false
                        context.startActivity(Intent(context, ProfilActivity::class.java))
                    }

                    Separator(textColor)

                    MenuItem("Map", textColor) {
                        expanded = false
                        context.startActivity(Intent(context, MapActivity::class.java))
                    }

                    Separator(textColor)

                    MenuItem("Découvertes", textColor) {
                        expanded = false
                        context.startActivity(Intent(context, DiscoveryListActivity::class.java))
                    }

                    Separator(textColor)

                    MenuItem("Déconnexion", textColor) {
                        expanded = false
                        FirebaseAuth.getInstance().signOut()
                        val logoutIntent = Intent(context, MainActivity::class.java)
                        logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(logoutIntent)
                    }
                }
            }
        }

    }
}
@Composable
fun MenuItem(label: String, textColor: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = textColor)
    }
}

@Composable
fun Separator(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(color)
    )
}
