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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.rotate

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.myapplication.storage.getDiscoveries
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
                    // Récupérer le nombre d'éléments dans la liste des découvertes
                    val nbDiscoveries = getDiscoveries(this).size

                    // Passer nbDiscoveries à ProfilScreen
                    setContent {
                        ProfilScreen(pseudo ?: "Chargement...", nbDiscoveries)
                    }
                }
                .addOnFailureListener {
                    pseudo = "Erreur de chargement"
                    val nbDiscoveries = getDiscoveries(this).size
                    setContent {
                        ProfilScreen(pseudo ?: "Chargement...", nbDiscoveries)
                    }
                }
        } else {
            pseudo = "Utilisateur non connecté"
            val nbDiscoveries = getDiscoveries(this).size
            setContent {
                ProfilScreen(pseudo ?: "Chargement...", nbDiscoveries)
            }
        }
    }
}

@Composable
fun ProfilScreen(pseudo: String, nbDiscoveries: Int) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image de fond (remplit la largeur, hauteur auto)
        Image(
            painter = painterResource(id = R.drawable.profil_fond),
            contentDescription = "Image de fond",
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )

        // Image de profil en haut (coupée à partir du quart de sa hauteur)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .zIndex(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat03),
                contentDescription = "Image de profil",
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-35).dp)
                    .clip(RectangleShape),
                contentScale = ContentScale.FillWidth
            )
        }

        // Placer le MenuWithDropdown tout en haut à gauche avec un zIndex élevé
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f) // Valeur élevée pour être au-dessus de tout
        ) {
            // Positionner le menu en haut à gauche
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                MenuWithDropdown()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 22.dp, horizontal = 0.dp)
                .zIndex(3f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image up_profile d'abord (au-dessus)
            Image(
                painter = painterResource(id = R.drawable.up_profile),
                contentDescription = "Image centrale",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .offset(y = (-85).dp)
                    .rotate(-8f)
                    .padding(vertical = 0.dp, horizontal = 0.dp),
                contentScale = ContentScale.FillWidth
            )

            // Removed the MenuWithDropdown from here as it's now placed in the Box above

            // Espace entre l'image et le texte
            Spacer(modifier = Modifier.height(24.dp))

            // Texte en dessous de l'image
            Text(
                "Bonjour",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                pseudo,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Afficher le nombre d'éléments dans la liste des découvertes
            Text(
                "$nbDiscoveries découvertes",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
    }
}

