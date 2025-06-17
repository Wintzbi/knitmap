package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.view.ViewTreeObserver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import com.example.myapplication.storage.getScratchedPoints
import com.example.myapplication.storage.saveScratchedPoints
import com.example.myapplication.storage.saveScratchedPointsToFirebase
import com.example.myapplication.storage.getDiscoveries
import androidx.core.graphics.createBitmap
import kotlin.math.pow

/**
 * "Fog‑of‑War" overlay collé à la carte.
 *
 * ‑ Le brouillard est rendu comme une **tuile répétable** alignée sur le monde (ancrage 0°/0°), via BitmapShader.
 * ‑ Les zones découvertes sont effacées (PorterDuff.Mode.CLEAR) autour des points grattés & des découvertes.
 * ‑ Optimisations : flou adouci, projection partielle, clustering, shader unique.
 */
class ScratchOverlay(private val mapView: MapView) : Overlay() {

    /* ───────────────────────────────  Assets  ─────────────────────────────── */

    private val fogTile: Bitmap = BitmapFactory.decodeResource(
        mapView.context.resources,
        R.drawable.fog1nbvrai
    )
    private val fogShader = BitmapShader(fogTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

    /* ─────────────────────────────  Buffers  ─────────────────────────────── */

    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null

    /* ─────────────────────────────  Paints  ──────────────────────────────── */

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = fogShader
    }

    private val scratchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL) // 🌫️ Plus léger
    }

    /* ─────────────────────────────  Data  ────────────────────────────────── */

    private val scratchedPoints = getScratchedPoints(mapView.context).toMutableList()
    private val savedDiscoveries = getDiscoveries(mapView.context)

    companion object {
        private var dirty = false
        private var appContext: Context? = null

        fun markDirty(context: Context) {
            dirty = true
            appContext = context.applicationContext
        }

        fun flushScratchedPointsToFirebase() {
            if (!dirty || appContext == null) return
            val points = getScratchedPoints(appContext!!)
            saveScratchedPointsToFirebase(points)
            dirty = false
        }
    }

    init {
        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                overlayBitmap = createBitmap(mapView.width, mapView.height)
                overlayCanvas = Canvas(overlayBitmap!!)
            }
        })
    }

    /* ───────────────────────  API publique  ─────────────────────────────── */

    fun scratchAt(point: GeoPoint) {
        scratchedPoints.add(point)
        saveScratchedPoints(mapView.context, scratchedPoints)
        markDirty(mapView.context)
        mapView.invalidate()
    }


    /* ─────────────────────────────  draw()  ─────────────────────────────── */

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || overlayBitmap == null) return

        overlayBitmap!!.eraseColor(Color.TRANSPARENT)

        val projection = mapView.projection
        val screenBounds = Rect(0, 0, overlayBitmap!!.width, overlayBitmap!!.height)

        /*─────────────── 1) Rendu du brouillard (tiling) ───────────────*/
        val anchorGeo = GeoPoint(0.0, 0.0)                     // Ancrage : lat 0°, lon 0°
        val anchorScreen = projection.toPixels(anchorGeo, null) // Position écran de l’ancrage

        val tileW = fogTile.width
        val tileH = fogTile.height
        // Décalage pour que le motif suive le monde
        val offsetX = ((-anchorScreen.x) % tileW + tileW) % tileW
        val offsetY = ((-anchorScreen.y) % tileH + tileH) % tileH

        var y = -offsetY
        while (y < overlayBitmap!!.height) {
            var x = -offsetX
            while (x < overlayBitmap!!.width) {
                overlayCanvas?.drawBitmap(fogTile, x.toFloat(), y.toFloat(), tilePaint)
                x += tileW
            }
            y += tileH
        }

        /*─────────────── 2) Effacement autour des points grattés ─────────────*/
        clusterAndDrawPoints(scratchedPoints, projection, screenBounds, 150f)

        /*─────────────── 3) Effacement autour des découvertes ─────────────*/
        val discoveryPoints = savedDiscoveries.mapNotNull {
            if (it.latitude.isFinite() && it.longitude.isFinite()) GeoPoint(it.latitude, it.longitude) else null
        }
        clusterAndDrawPoints(discoveryPoints, projection, screenBounds, 300f)

        /*─────────────── 4) Dessin final ─────────────*/
        canvas.drawBitmap(overlayBitmap!!, 0f, 0f, null)
    }

    /* ───────────────────────  Clustering & Filtrage  ───────────────────── */

    private fun clusterAndDrawPoints(
        points: List<GeoPoint>,
        projection: org.osmdroid.views.Projection,
        bounds: Rect,
        radiusMeters: Float
    ) {
        val screenPoints = points.map { projection.toPixels(it, null) }
            .filter { bounds.contains(it.x, it.y) }

        val clustered = mutableListOf<Point>()
        val threshold = projection.metersToEquatorPixels(radiusMeters / 1.5f)

        for (point in screenPoints) {
            val nearby = clustered.find {
                ((it.x - point.x).toDouble().pow(2) + (it.y - point.y).toDouble().pow(2)) < threshold * threshold
            }
            if (nearby == null) clustered.add(point)
        }

        for (point in clustered) {
            val radius = projection.metersToEquatorPixels(radiusMeters)
            overlayCanvas?.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, scratchPaint)
        }
    }
}
