package com.example.myapplication

import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import android.widget.ImageView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView


class MapActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Charger la vue de la carte depuis le layout XML
        setContentView(R.layout.activity_map)
        val topImage = findViewById<ImageView>(R.id.Top)
        topImage.setImageResource(R.drawable.up_map_fond)
        val botImage = findViewById<ImageView>(R.id.Bot)
        botImage.setImageResource(R.drawable.map_fond_bouton)
        val pingButton = findViewById<ImageView>(R.id.Ping_Boutton)
        pingButton.setImageResource(R.drawable.ping_resized)
        val followButton = findViewById<ImageView>(R.id.Follow_Button)
        followButton.setImageResource(R.drawable.target)
        val mapView = findViewById<MapView>(R.id.activity_map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        // Injection du composant Compose dans ComposeView défini dans XML
        val composeView = findViewById<ComposeView>(R.id.composeView)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MenuWithDropdown()
        }
    }
}
