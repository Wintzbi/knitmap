package com.knitMap.old

import com.knitMap.utils.saveGalleryImageToAppFolder
import com.knitMap.map.startCameraIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.knitMap.BaseActivity
import com.knitMap.R
import com.knitMap.discovery.Discovery
import com.knitMap.ui.theme.MyApplicationTheme
import java.io.File

class DiscoveryActivityold : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val discovery = intent.getSerializableExtra("discovery") as? Discovery

        setContent {
            MyApplicationTheme {
                if (discovery != null) {
                    DiscoveryScreen(discovery) { updated ->
                        val resultIntent = Intent().apply {
                            putExtra("updatedDiscovery", updated)
                        }
                        setResult(RESULT_OK, resultIntent)
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
    var imageUri by remember { mutableStateOf(discovery.imageUri) }
    var showImageOptions by remember { mutableStateOf(false) }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher pour prendre la photo
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                imageUri = uri.toString()
                showEditor = true
            }
        }
    }

    // Launcher pour choisir une image depuis la galerie
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Copie l'image dans un fichier dans le dossier Pictures/KnitMapPictures
            val savedUri = saveGalleryImageToAppFolder(context, it)
            savedUri?.let { finalUri ->
                imageUri = finalUri
                showEditor = true
            }
        }
    }

    // Lance la caméra via ta fonction startCameraIntent
    fun launchCamera() {
        startCameraIntent(context, cameraLauncher) { path ->
            cameraImageUri = Uri.fromFile(File(path))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.decouv_fond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(onClick = {
                (context as? ComponentActivity)?.finish()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF4E7072).copy(alpha = 0.8f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                val painter = when {
                    imageUri?.startsWith("content://") == true -> rememberAsyncImagePainter(model = Uri.parse(imageUri))
                    imageUri?.startsWith("/") == true -> rememberAsyncImagePainter(model = File(imageUri))
                    else -> rememberAsyncImagePainter(model = R.drawable.cat03)  // image par défaut si aucune image
                }

                Image(
                    painter = painter,
                    contentDescription = "Illustration découverte",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp)
                        .clickable {
                            showImageOptions = true
                            showEditor = true
                        },
                    contentScale = ContentScale.Crop
                )
            }

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
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            showEditor = true
                        },
                        placeholder = { Text("Titre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            showEditor = true
                        },
                        placeholder = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    )
                }
            }
        }

        if (showImageOptions) {
            AlertDialog(
                onDismissRequest = { showImageOptions = false },
                title = { Text("Choisir une image") },
                text = {
                    Column {
                        Text(
                            "Prendre une photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showImageOptions = false
                                    launchCamera()
                                }
                                .padding(8.dp)
                        )
                        Text(
                            "Choisir dans la galerie",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showImageOptions = false
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(8.dp)
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (showEditor) {
                    Button(onClick = {
                        val updated = discovery.copy(
                            title = title,
                            description = description,
                            imageUri = imageUri
                        )
                        updateDiscovery(context, updated)
                        onSave(updated)
                        showEditor = false
                    }) {
                        Text("Sauvegarder")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        showEditor = false
                        title = discovery.title
                        description = discovery.description
                        imageUri = discovery.imageUri
                    }) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        removeDiscoveryByUuid(context, discovery.uuid)
                        (context as? ComponentActivity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            }
        }
    }
}

