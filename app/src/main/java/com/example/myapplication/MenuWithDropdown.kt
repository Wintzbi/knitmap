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

@OptIn(ExperimentalAnimationApi::class)
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
            exit = exitTransition
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 80.dp)
                    .wrapContentSize()
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(color = backgroundColor,
                            shape = RoundedCornerShape(22.dp),

                        )
                        .border(
                            width = 1.5.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(30.dp))
                )
                {
                    // Premier élément
                    DropdownMenuItem(
                        text = { Text("Profil", color = textColor) },
                        onClick = {
                            expanded = false
                            context.startActivity(Intent(context, ProfilActivity::class.java))
                        }
                    )

                    // Séparateur
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(textColor)
                    )

                    // Deuxième élément
                    DropdownMenuItem(
                        text = { Text("Map", color = textColor) },
                        onClick = {
                            expanded = false
                            context.startActivity(Intent(context, MapActivity::class.java))
                        }
                    )

                    // Séparateur
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(textColor)
                    )

                    // Troisième élément
                    DropdownMenuItem(
                        text = { Text("Déconnexion", color = textColor) },
                        onClick = {
                            expanded = false
                            FirebaseAuth.getInstance().signOut()
                            val logoutIntent = Intent(context, MainActivity::class.java)
                            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(logoutIntent)
                        }
                    )
                }
            }
        }
    }
}