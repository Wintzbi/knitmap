package com.knitMap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.knitMap.map.MapActivity
import com.knitMap.storage.flushPendingActionsDiscoverie
import com.knitMap.ui.theme.MyApplicationTheme
import com.knitMap.utils.isConnectedToInternet
import com.google.firebase.auth.FirebaseAuth
import com.knitMap.utils.isConnectedToInternetFast

class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private val cameraPermissionRequestCode = 100
    private val storagePermissionRequestCode = 101

    companion object {
        var isOnline: Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isOnline = isConnectedToInternetFast(this)
        if (!isOnline) {
            Toast.makeText(this, "Mode hors ligne activé ", Toast.LENGTH_LONG).show()
        }
        else if (isOnline) {
            Toast.makeText(this, "En ligne ", Toast.LENGTH_LONG).show()
            flushPendingActionsDiscoverie(this)
        }
        // Demande la permission d'accéder à la caméra et aux images au lancement
        requestPermissions()

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

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permission pour la caméra
        val cameraPermission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(cameraPermission)
        }

        // Permission pour les images et vidéos
        val readMediaImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val readMediaVideoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, readMediaImagesPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(readMediaImagesPermission)
        }

        if (ContextCompat.checkSelfPermission(this, readMediaVideoPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(readMediaVideoPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), cameraPermissionRequestCode)
        }
    }

    // Gestion des résultats des permissions demandées
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsDenied = permissions.filterIndexed { index, _ -> grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED }

        if (permissionsDenied.isNotEmpty()) {
            Toast.makeText(this, "Les permissions suivantes sont requises : ${permissionsDenied.joinToString(", ")}", Toast.LENGTH_LONG).show()
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