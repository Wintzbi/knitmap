package com.knitMap.parametre

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.knitMap.*
import com.knitMap.MainActivity.Companion.isOnline
import com.knitMap.storage.cleanAndSortScratchedPoints
import com.knitMap.storage.fetchAndSyncDiscoveriesWithFirebase
import com.knitMap.storage.fetchAndSyncScratchedPointsWithFirebase
import com.knitMap.storage.flushPendingScratches
import com.knitMap.storage.getScratchedPointsLocally
import com.knitMap.storage.loadShaderState
import com.knitMap.storage.saveScratchedPointsLocally
import com.knitMap.storage.saveShaderState
import com.knitMap.utils.isConnectedToInternet
import com.knitMap.BaseActivity
import com.knitMap.MenuWithDropdown
import com.knitMap.storage.cleanAndSortScratchedPointsWithProgress
import com.knitMap.storage.fetchAndSyncDiscoveriesWithProgress
import com.knitMap.storage.fetchAndSyncScratchesWithProgress
import com.knitMap.storage.flushPendingActionsDiscoverie
import com.knitMap.storage.flushPendingActionsDiscoverieWithProgress
import com.knitMap.storage.flushPendingScratchesWithProgress
import com.knitMap.utils.ProgressDialogManager
import com.knitMap.utils.isConnectedToInternetFast

class ParametreActivity : BaseActivity() {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parametre)

        // Menu Compose
        val composeView = findViewById<ComposeView>(R.id.composeView)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MenuWithDropdown()
        }

        // ---- SWITCH SHADER ----
        val switchShader = findViewById<Switch>(R.id.Switch_shader)

        // Charger état initial
        switchShader.isChecked = loadShaderState(this)

        // Écouter les changements
        switchShader.setOnCheckedChangeListener { _, isChecked ->
            saveShaderState(this, isChecked)
            Toast.makeText(this, "Shaders ${if (isChecked) "activés" else "désactivés"}", Toast.LENGTH_SHORT).show()
        }

        // ---- BOUTON SYNC DISCOVERIES ----
        val syncDiscoveriesButton = findViewById<Button>(R.id.button4)
        syncDiscoveriesButton.setOnClickListener {
            if (!isOnline) {
                Toast.makeText(this, "❌ Vous êtes hors ligne", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ProgressDialogManager.show(this, "Envoi des découvertes en attente...") {
                Toast.makeText(this, "❌ Annulé", Toast.LENGTH_SHORT).show()
            }

            flushPendingActionsDiscoverieWithProgress(
                context = this,
                onProgress = { current, max ->
                    runOnUiThread { ProgressDialogManager.update(current, max) }
                },
                isCancelled = { ProgressDialogManager.isCancelled },
                onComplete = {
                    runOnUiThread {
                        ProgressDialogManager.dismiss()

                        ProgressDialogManager.show(this, "Synchronisation des découvertes...") {
                            Toast.makeText(this, "❌ Annulé", Toast.LENGTH_SHORT).show()
                        }

                        fetchAndSyncDiscoveriesWithProgress(
                            context = this,
                            onProgress = { current, max ->
                                runOnUiThread { ProgressDialogManager.update(current, max) }
                            },
                            isCancelled = { ProgressDialogManager.isCancelled },
                            onComplete = {
                                runOnUiThread {
                                    ProgressDialogManager.dismiss()
                                    Toast.makeText(this, "✅ Pings synchronisés avec succès", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFailure = {
                                runOnUiThread {
                                    ProgressDialogManager.dismiss()
                                    Toast.makeText(this, "❌ Échec de la synchronisation", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            )
        }


// ---- BOUTON SYNC FIREBASE (FOG) ----
        val syncFogButton = findViewById<Button>(R.id.button)
        syncFogButton.setOnClickListener {
            if (!isOnline) {
                Toast.makeText(this, "❌ Vous êtes hors ligne", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Étape 1 : flush des points en attente
            ProgressDialogManager.show(this, "Envoi des points grattés en attente...") {
                Toast.makeText(this, "❌ Annulé", Toast.LENGTH_SHORT).show()
            }

            flushPendingScratchesWithProgress(
                context = this,
                onProgress = { current, max ->
                    runOnUiThread { ProgressDialogManager.update(current, max) }
                },
                isCancelled = { ProgressDialogManager.isCancelled },
                onComplete = {
                    runOnUiThread {
                        ProgressDialogManager.dismiss()

                        // Étape 2 : nettoyage et tri
                        ProgressDialogManager.show(this, "Nettoyage et tri des points...") {
                            Toast.makeText(this, "❌ Nettoyage annulé", Toast.LENGTH_SHORT).show()
                        }

                        cleanAndSortScratchedPointsWithProgress(
                            context = this,
                            onProgress = { current, max ->
                                runOnUiThread { ProgressDialogManager.update(current, max) }
                            },
                            isCancelled = { ProgressDialogManager.isCancelled },
                            onComplete = {
                                runOnUiThread {
                                    ProgressDialogManager.dismiss()

                                    // Étape 3 : synchronisation avec Firebase
                                    ProgressDialogManager.show(this, "Synchronisation du Fog...") {
                                        Toast.makeText(this, "❌ Synchronisation annulée", Toast.LENGTH_SHORT).show()
                                    }

                                    fetchAndSyncScratchesWithProgress(
                                        context = this,
                                        onProgress = { current, max ->
                                            runOnUiThread { ProgressDialogManager.update(current, max) }
                                        },
                                        isCancelled = { ProgressDialogManager.isCancelled },
                                        onComplete = {
                                            runOnUiThread {
                                                ProgressDialogManager.dismiss()
                                                Toast.makeText(this, "✅ Fog synchronisé avec succès", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onFailure = {
                                            runOnUiThread {
                                                ProgressDialogManager.dismiss()
                                                Toast.makeText(this, "❌ Échec de la synchronisation du fog", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            )
        }

        // BOUTON LANCER / Télécharger → laissé tel quel
        val lancerButton = findViewById<Button>(R.id.button2)
        lancerButton.setOnClickListener {
            Toast.makeText(this, "❌ Fonction non implémentée", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.button_online_mode)?.setOnClickListener {
            isOnline = isConnectedToInternetFast(this)

            val message = if (isOnline) {
                "✅ Vous êtes en ligne"
            } else {
                "❌ Vous êtes hors ligne"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

    }
}
