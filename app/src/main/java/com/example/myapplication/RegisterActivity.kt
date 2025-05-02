package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            RegisterScreen(auth, db)
        }
    }
}

@Composable
fun RegisterScreen(auth: FirebaseAuth, db: FirebaseFirestore) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pseudo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Inscription", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = pseudo, onValueChange = { pseudo = it }, label = { Text("Pseudo") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                            db.collection("users").document(userId)
                                .set(mapOf("pseudo" to pseudo, "email" to email))  // Ajout de l'email à la base de données
                                .addOnSuccessListener {
                                    val intent = Intent(context, MapActivity::class.java)
                                    context.startActivity(intent)
                                }
                                .addOnFailureListener {
                                    errorMessage = "Erreur lors de l'ajout du pseudo et de l'email : ${it.localizedMessage}"
                                }
                        } else {
                            errorMessage = "Erreur lors de la création du compte : ${task.exception?.localizedMessage}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Créer un compte")
        }
    }
}

