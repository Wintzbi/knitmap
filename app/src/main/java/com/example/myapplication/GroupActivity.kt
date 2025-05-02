package com.example.myapplication
import com.example.myapplication.MenuWithDropdown
import androidx.compose.ui.graphics.Color

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.myapplication.R

class GroupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                GroupScreen()
            }
        }
    }
}



@Composable
fun GroupScreen() {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Image de fond
        Image(
            painter = painterResource(id = R.drawable.groupe_fond), // ton image ici
            contentDescription = "Background",
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

        // Texte au centre de l'écran
        Box(
            modifier = Modifier
                .align(Alignment.Center) // Centrer le texte
        ) {
            Text("Groupe", fontSize = 40.sp, color = Color.Black)
        }
    }
}