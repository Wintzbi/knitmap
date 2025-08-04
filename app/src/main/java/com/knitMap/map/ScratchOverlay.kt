package com.knitMap.map

import android.content.Context
import android.graphics.*
import android.view.ViewTreeObserver
import androidx.core.graphics.createBitmap
import com.knitMap.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import com.knitMap.storage.getScratchedPointsLocally
import com.knitMap.storage.saveScratchedPointsLocally
import com.knitMap.storage.saveScratchedPointsToFirebase
import com.knitMap.storage.getDiscoveriesLocally
import com.knitMap.storage.loadShaderState
import com.knitMap.storage.queuePendingScratch
import kotlin.math.pow

class ScratchOverlay(private val mapView: MapView) : Overlay() {

    private val fogTile: Bitmap = BitmapFactory.decodeResource(
        mapView.context.resources,
        R.drawable.fog1nbvrai
    )
    private val fogShader = BitmapShader(fogTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = fogShader }
    private val scratchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
        if (loadShaderState(mapView.context)) {
            maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    private val scratchedPoints = getScratchedPointsLocally(mapView.context).toMutableList()
    private val savedDiscoveries = getDiscoveriesLocally(mapView.context)

    private val scratchGrid = mutableMapOf<Pair<Int, Int>, MutableList<GeoPoint>>()
    private val discoveryGrid = mutableMapOf<Pair<Int, Int>, MutableList<GeoPoint>>()
    private val cellSizeMeters = 100.0

    // --- Cache bitmap ---
    private var lastZoom = -1.0
    private var lastCenter: GeoPoint? = null
    private var dirtyCache = true

    companion object {
        private var dirty = false
        private var appContext: Context? = null

        fun markDirty(context: Context) {
            dirty = true
            appContext = context.applicationContext
        }

        fun flushScratchedPointsToFirebase() {
            if (!dirty || appContext == null) return
            val points = getScratchedPointsLocally(appContext!!)
            saveScratchedPointsToFirebase(points)
            dirty = false
        }
    }

    init {
        scratchedPoints.forEach { addToGrid(it, scratchGrid) }
        savedDiscoveries.mapNotNull {
            if (it.latitude.isFinite() && it.longitude.isFinite()) GeoPoint(it.latitude, it.longitude) else null
        }.forEach { addToGrid(it, discoveryGrid) }

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                overlayBitmap = createBitmap(mapView.width, mapView.height)
                overlayCanvas = Canvas(overlayBitmap!!)
                dirtyCache = true
            }
        })
    }

    fun scratchAt(point: GeoPoint) {
        if (!alreadyScratched(point)) {
            scratchedPoints.add(point)
            addToGrid(point, scratchGrid)
            saveScratchedPointsLocally(mapView.context, scratchedPoints)
            markDirty(mapView.context)
            queuePendingScratch(mapView.context, point)
            dirtyCache = true
        }
        mapView.invalidate()
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || overlayBitmap == null) return

        val center = mapView.mapCenter as GeoPoint
        val zoom = mapView.zoomLevelDouble

        if (!dirtyCache && zoom == lastZoom && (lastCenter?.distanceToAsDouble(center)
                ?: 1000.0) < 50.0
        ) {
            canvas.drawBitmap(overlayBitmap!!, 0f, 0f, null)
            return
        }

        // Mise à jour du cache
        overlayBitmap!!.eraseColor(Color.TRANSPARENT)

        val projection = mapView.projection
        val scratchRadius = 150f
        val discoveryRadius = 300f
        val margin = maxOf(scratchRadius, discoveryRadius)
        val screenBounds = getExpandedScreenRect(projection, margin)
        val screenGeoBounds = projection.boundingBox

        drawFog(projection)
        clusterAndDrawPoints(getNearbyPoints(screenGeoBounds, scratchGrid), projection, screenBounds, scratchRadius)
        clusterAndDrawPoints(getNearbyPoints(screenGeoBounds, discoveryGrid), projection, screenBounds, discoveryRadius)

        canvas.drawBitmap(overlayBitmap!!, 0f, 0f, null)

        lastZoom = zoom
        lastCenter = center
        dirtyCache = false
    }


    private fun drawFog(projection: Projection) {
        val anchorGeo = GeoPoint(0.0, 0.0)
        val anchorScreen = projection.toPixels(anchorGeo, null)
        val tileW = fogTile.width
        val tileH = fogTile.height
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
    }

    private fun clusterAndDrawPoints(
        points: List<GeoPoint>,
        projection: Projection,
        bounds: Rect,
        radiusMeters: Float
    ) {
        val screenPoints = points.map { projection.toPixels(it, null) }
            .filter { bounds.contains(it.x, it.y) }

        val clustered = clusterScreenPoints(screenPoints, projection.metersToEquatorPixels(radiusMeters / 1.5f).toInt())

        for (point in clustered) {
            val radius = projection.metersToEquatorPixels(radiusMeters)
            overlayCanvas?.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, scratchPaint)
        }
    }

    private fun clusterScreenPoints(points: List<Point>, threshold: Int): List<Point> {
        val clustered = mutableListOf<Point>()
        val visited = mutableSetOf<Point>()

        for (point in points) {
            if (visited.contains(point)) continue
            val cluster = points.filter {
                !visited.contains(it) &&
                        ((it.x - point.x).toDouble().pow(2) + (it.y - point.y).toDouble().pow(2)) < threshold * threshold
            }
            val avgX = cluster.map { it.x }.average().toInt()
            val avgY = cluster.map { it.y }.average().toInt()
            clustered.add(Point(avgX, avgY))
            visited.addAll(cluster)
        }

        return clustered
    }

    private fun getExpandedScreenRect(projection: Projection, expansionMeters: Float): Rect {
        val viewWidth = mapView.width
        val viewHeight = mapView.height
        val expansionPixels = projection.metersToEquatorPixels(expansionMeters)
        return Rect(
            (-expansionPixels).toInt(),
            (-expansionPixels).toInt(),
            (viewWidth + expansionPixels).toInt(),
            (viewHeight + expansionPixels).toInt()
        )
    }

    private fun addToGrid(point: GeoPoint, grid: MutableMap<Pair<Int, Int>, MutableList<GeoPoint>>) {
        val key = spatialKey(point)
        grid.getOrPut(key) { mutableListOf() }.add(point)
    }

    private fun spatialKey(point: GeoPoint): Pair<Int, Int> {
        val cellSizeDeg = cellSizeMeters / 111_000.0
        val latKey = (point.latitude / cellSizeDeg).toInt()
        val lonKey = (point.longitude / cellSizeDeg).toInt()
        return latKey to lonKey
    }

    private fun getNearbyPoints(bounds: BoundingBox, grid: Map<Pair<Int, Int>, MutableList<GeoPoint>>): List<GeoPoint> {
        val nearby = mutableListOf<GeoPoint>()
        val cellSizeDeg = cellSizeMeters / 111_000.0

        val minLat = (bounds.latSouth / cellSizeDeg).toInt()
        val maxLat = (bounds.latNorth / cellSizeDeg).toInt()
        val minLon = (bounds.lonWest / cellSizeDeg).toInt()
        val maxLon = (bounds.lonEast / cellSizeDeg).toInt()

        for (lat in minLat..maxLat) {
            for (lon in minLon..maxLon) {
                grid[lat to lon]?.let { nearby.addAll(it) }
            }
        }

        return nearby
    }

    private fun alreadyScratched(p: GeoPoint): Boolean {
        return scratchedPoints.any {
            it.latitude == p.latitude && it.longitude == p.longitude
        }
    }
}
