package com.example.myapplication

import android.graphics.*
import android.view.ViewTreeObserver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import com.example.myapplication.storage.getScratchedPoints
import com.example.myapplication.storage.saveScratchedPoints
import com.example.myapplication.discovery.Discovery
import com.example.myapplication.storage.getDiscoveries

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

    private val scratchedPoints = getScratchedPoints(mapView.context).toMutableList() // Charge les poinrs découvert enregistrés
    private var imageBitmap: Bitmap? = null // Bitmap pour l'image à ajouter

    init {
        // Charger l'image (ici une image depuis les ressources)
        imageBitmap = BitmapFactory.decodeResource(mapView.context.resources, R.drawable.fog)

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                overlayBitmap = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888).apply {
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


    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || overlayBitmap == null) return

        val projection = mapView.projection

        // Redessiner le bitmap de zéro à chaque frame
        overlayBitmap?.eraseColor(Color.GRAY)

        // Si l'image existe, la redimensionner et la dessiner pour couvrir la carte seulement
        imageBitmap?.let {
            val imageWidth = it.width
            val imageHeight = it.height
            val screenWidth = mapView.width
            val screenHeight = mapView.height

            // Calculer le facteur de mise à l'échelle pour garder l'aspect ratio
            val scaleFactor = maxOf(screenWidth.toFloat() / imageWidth, screenHeight.toFloat() / imageHeight)

            // Calculer les nouvelles dimensions de l'image
            val scaledWidth = (imageWidth * scaleFactor).toInt()
            val scaledHeight = (imageHeight * scaleFactor).toInt()

            // Créer le Bitmap redimensionné
            val resizedBitmap = Bitmap.createScaledBitmap(it, scaledWidth, scaledHeight, false)

            // Dessiner l'image redimensionnée pour qu'elle couvre la carte
            val left = (screenWidth - scaledWidth) / 2f
            val top = (screenHeight - scaledHeight) / 2f

            overlayCanvas?.drawBitmap(resizedBitmap, left, top, overlayPaint)
        }

        // Dessiner tous les points découverts
        for (geoPoint in scratchedPoints) {
            val screenPoint = projection.toPixels(geoPoint, null)
            val radius = projection.metersToEquatorPixels(50.0f) // Ajuster le rayon en fonction du zoom
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
            val geoPoint = GeoPoint(discovery.latitude, discovery.longitude)  // Création du GeoPoint correctement
            val screenPoint = projection.toPixels(geoPoint, null)
            val radius = projection.metersToEquatorPixels(100.0f) // Ajuster le rayon selon le zoom
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


