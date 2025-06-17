package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private val cameraPermissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande la permission d'accéder à la caméra au lancement
        requestCameraPermission()

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                LoginScreen(auth)
            }
        }
    }

    private fun requestCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), cameraPermissionRequestCode)
        }
    }

    // Optionnel : gestion du résultat de la permission
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == cameraPermissionRequestCode) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Vous devez acceptez la permission", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


@Composable
fun LoginScreen(auth: FirebaseAuth) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Page de Connexion", fontSize = 32.sp)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Email utilisateur") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                auth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(context, MapActivity::class.java)
                            context.startActivity(intent)
                        } else {
                            errorMessage = "Échec de la connexion : ${task.exception?.localizedMessage}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se connecter")
        }

        Spacer(modifier = Modifier.height(8.dp)) // <-- Ajoute un petit espace entre les deux boutons

        TextButton(
            onClick = {
                val intent = Intent(context, RegisterActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                "Créer un compte",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

    }


}
@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    MyApplicationTheme {
        // Attention : pour le preview, on ne peut pas utiliser FirebaseAuth réellement,
        // donc on passe une version factice ou on ignore l'aperçu pour cette fonction.
        Text("Preview désactivé car FirebaseAuth requis")
    }
}
