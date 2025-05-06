package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfilActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var pseudo by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        pseudo = document.getString("pseudo")
                    } else {
                        pseudo = "Pseudo introuvable"
                    }
                    setContent {
                        ProfilScreen(pseudo ?: "Chargement...")
                    }
                }
                .addOnFailureListener {
                    pseudo = "Erreur de chargement"
                    setContent {
                        ProfilScreen(pseudo ?: "Chargement...")
                    }
                }
        } else {
            pseudo = "Utilisateur non connecté"
            setContent {
                ProfilScreen(pseudo ?: "Chargement...")
            }
        }
    }
}

@Composable
fun ProfilScreen(pseudo: String) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image de fond (en arrière-plan)
        Image(
            painter = painterResource(id = R.drawable.up_profile),
            contentDescription = "Image de fond",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Image de profil en haut de l'écran
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .zIndex(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat03),
                contentDescription = "Image de profil",
                modifier = Modifier
                    .size(120.dp)
                    .zIndex(1f)
            )
        }

        // Contenu principal au milieu avec une image
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Bonjour",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                pseudo,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Image au milieu par-dessus toutes les autres
            Image(
                painter = painterResource(id = R.drawable.up_profile),
                contentDescription = "Image centrale",
                modifier = Modifier
                    .size(200.dp)
                    .zIndex(2f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfilScreen() {
    ProfilScreen(pseudo = "Mamie Ping")
}