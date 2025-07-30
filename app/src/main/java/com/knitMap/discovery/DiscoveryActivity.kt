package com.knitMap.discovery

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import coil.load
import com.knitMap.BaseActivity
import com.knitMap.map.MapActivity
import com.knitMap.R
import com.knitMap.map.startCameraIntent
import com.knitMap.storage.removeDiscoveryByUuidLocally
import com.knitMap.storage.updateDiscoveryLocally
import com.knitMap.utils.saveGalleryImageToAppFolder
import java.io.File
import androidx.core.net.toUri

class DiscoveryActivityXml : BaseActivity() {

    private var updatedImageUri: String? = null
    private lateinit var discovery: Discovery

    private lateinit var imageView: ImageView
    private lateinit var editTitle: EditText
    private lateinit var editDescription: EditText
    private lateinit var textViewDate: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    @SuppressLint("UseKtx")
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            updatedImageUri?.let {
                        imageView.load(File(it))
                updateButtonsVisibility()
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val savedUri = saveGalleryImageToAppFolder(this, it)
            savedUri?.let { path ->
                updatedImageUri = path
                imageView.load(path.toUri())
                updateButtonsVisibility()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discoverie)

        discovery = intent.getSerializableExtra("discovery") as Discovery
        updatedImageUri = discovery.imageUri

        imageView = findViewById(R.id.image_discovery)
        editTitle = findViewById(R.id.edit_title)
        editDescription = findViewById(R.id.edit_description)
        textViewDate = findViewById(R.id.text_view_date)
        textViewLocation = findViewById(R.id.text_view_location)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)

        val btnDelete = findViewById<Button>(R.id.btn_delete)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        editTitle.setText(discovery.title)
        editDescription.setText(discovery.description)

        // Si image manquante, affiche une image par défaut
        if (discovery.imageUri.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.cat03)
        } else {
            imageView.load(discovery.imageUri!!.toUri())
        }

        // Affichage de la date (lecture seule)
        textViewDate.text = "Ajouté le : ${discovery.date}"

        // Affichage de la localisation (lecture seule, via API)
        val locationToDisplay = discovery.locationName.takeIf { !it.isNullOrBlank() } ?: "Lieu inconnu"
        textViewLocation.text = "Lieu : $locationToDisplay"

        val btnOpenMap = findViewById<ImageButton>(R.id.btn_open_map)

        btnOpenMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java).apply {
                putExtra("center_lat", discovery.latitude)
                putExtra("center_lon", discovery.longitude)
            }
            startActivity(intent)
        }



        btnSave.visibility = View.GONE
        btnCancel.visibility = View.GONE

        btnBack.setOnClickListener { finish() }

        imageView.setOnClickListener { showImageChoiceDialog() }

        btnSave.setOnClickListener {
            val updated = discovery.copy(
                title = editTitle.text.toString(),
                description = editDescription.text.toString(),
                imageUri = updatedImageUri
            )
            updateDiscoveryLocally(this, updated)
            setResult(RESULT_OK, Intent().putExtra("updatedDiscovery", updated))
            finish()
        }

        btnCancel.setOnClickListener {
            editTitle.setText(discovery.title)
            editDescription.setText(discovery.description)
            updatedImageUri = discovery.imageUri
            reloadImage()
            updateButtonsVisibility()
        }

        btnDelete.setOnClickListener {
            removeDiscoveryByUuidLocally(this, discovery.uuid)
            finish()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonsVisibility()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editTitle.addTextChangedListener(watcher)
        editDescription.addTextChangedListener(watcher)
    }

    private fun reloadImage() {
        if (updatedImageUri.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.cat03)
        } else {
            imageView.load(updatedImageUri!!.toUri())
        }
    }

    private fun updateButtonsVisibility() {
        val currentTitle = editTitle.text.toString().trim()
        val currentDescription = editDescription.text.toString().trim()
        val initialImage = discovery.imageUri ?: ""
        val currentImage = updatedImageUri ?: ""

        val hasChanged = currentTitle != discovery.title ||
                currentDescription != discovery.description ||
                currentImage != initialImage

        btnSave.visibility = if (hasChanged) View.VISIBLE else View.INVISIBLE
        btnCancel.visibility = if (hasChanged) View.VISIBLE else View.INVISIBLE

    }

    private fun showImageChoiceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Choisir une image")
            .setItems(arrayOf("Prendre une photo", "Choisir dans la galerie")) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun launchCamera() {
        startCameraIntent(this, cameraLauncher) { path ->
            updatedImageUri = path
        }
    }
}
