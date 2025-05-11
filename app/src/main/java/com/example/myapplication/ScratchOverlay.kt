package com.example.myapplication

import android.graphics.*
import android.view.ViewTreeObserver
import android.widget.Toast
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import com.example.myapplication.storage.getScratchedPoints
import com.example.myapplication.storage.saveScratchedPoints
import com.example.myapplication.discovery.Discovery
import com.example.myapplication.storage.getDiscoveries
import androidx.core.graphics.createBitmap

class ScratchOverlay(private val mapView: MapView) : Overlay() {

    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null
    private val overlayPaint = Paint()
    val savedDiscoveries = getDiscoveries(mapView.context)
    private val scratchPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // Mode effacement
        strokeWidth = 50f
        style = Paint.Style.FILL
    }

    private val scratchedPoints = getScratchedPoints(mapView.context).toMutableList() // Charge les points grattés enregistrés
    private var imageBitmap: Bitmap? = null // Bitmap pour l'image à ajouter
    private var isImageLoaded = false

    init {
        // Charger l'image (ici une image depuis les ressources)
        imageBitmap = BitmapFactory.decodeResource(mapView.context.resources, R.drawable.fog)

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                overlayBitmap = createBitmap(mapView.width, mapView.height).apply {
                    eraseColor(Color.GRAY) // Fond gris par défaut
                }
                overlayCanvas = overlayBitmap?.let { Canvas(it) }
            }
        })
    }

    fun scratchAt(point: GeoPoint) {
        scratchedPoints.add(point)
        saveScratchedPoints(mapView.context, scratchedPoints)  // Sauvegarde des points à chaque rayure
        mapView.invalidate()
    }

    private fun scaleImage(image: Bitmap, screenWidth: Int, screenHeight: Int): Bitmap {
        val scaleFactor = maxOf(screenWidth.toFloat() / image.width, screenHeight.toFloat() / image.height)
        val scaledWidth = (image.width * scaleFactor).toInt()
        val scaledHeight = (image.height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(image, scaledWidth, scaledHeight, false)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || overlayBitmap == null) return

        val projection = mapView.projection

        // Redessiner le bitmap de zéro à chaque frame
        overlayBitmap?.eraseColor(Color.GRAY)

        // Si l'image existe, la redimensionner et la dessiner pour couvrir la carte seulement
        imageBitmap?.let {
            if (!isImageLoaded) {
                val screenWidth = mapView.width
                val screenHeight = mapView.height
                overlayCanvas?.drawBitmap(scaleImage(it, screenWidth, screenHeight), 0f, 0f, overlayPaint)
                isImageLoaded = true
            }
        }

        // Dessiner tous les points découverts
        for (geoPoint in scratchedPoints) {
            val screenPoint = projection.toPixels(geoPoint, null)
            val radius = projection.metersToEquatorPixels(150.0f) // Ajuster le rayon en fonction du zoom
            overlayCanvas?.drawCircle(
                screenPoint.x.toFloat(),
                screenPoint.y.toFloat(),
                radius,
                scratchPaint
            )
        }

        // Dessiner tous les marqueurs sur la carte à partir des Discovery
        for (discovery in savedDiscoveries) {
            if (!discovery.latitude.isFinite() || !discovery.longitude.isFinite()) continue
            val geoPoint = GeoPoint(discovery.latitude, discovery.longitude)
            val screenPoint = projection.toPixels(geoPoint, null)
            val radius = projection.metersToEquatorPixels(300.0f) // Ajuster le rayon selon le zoom
            overlayCanvas?.drawCircle(
                screenPoint.x.toFloat(),
                screenPoint.y.toFloat(),
                radius,
                scratchPaint
            )
        }

        canvas.drawBitmap(overlayBitmap!!, 0f, 0f, overlayPaint)
    }
}