package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.zIndex


class DiscoveryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val discovery = intent.getSerializableExtra("discovery") as? Discovery

        setContent {
            MyApplicationTheme {
                if (discovery != null) {
                    DiscoveryScreen(discovery) { updated ->
                        val resultIntent = Intent().apply {
                            putExtra("updatedDiscovery", updated)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } else {
                    Text("Aucune découverte à afficher.")
                }
            }
        }
    }
}
@Composable
fun DiscoveryScreen(discovery: Discovery, onSave: (Discovery) -> Unit) {
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(discovery.title) }
    var description by remember { mutableStateOf(discovery.description) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Image de fond
        Image(
            painter = painterResource(id = R.drawable.decouv_fond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Bouton retour dans une Surface
        Surface(
            shape = RoundedCornerShape(50), // Tu peux ajuster la forme ici
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(onClick = {
                (context as? ComponentActivity)?.finish()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }
        }

        // Contenu de la découverte
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = discovery.imageResId),
                    contentDescription = "Illustration découverte",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Texte
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = title, fontSize = 32.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = description, fontSize = 20.sp, color = Color.DarkGray)
                }
            }

            if (showEditor) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Texte") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val updated = discovery.copy(title = title, description = description)
                    onSave(updated)
                }) {
                    Text("Sauvegarder")
                }
            }
        }

        // Bouton Modifier en bas
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Button(onClick = { showEditor = !showEditor }) {
                Text(if (showEditor) "Annuler" else "Modifier le texte")
            }
        }
    }
}
