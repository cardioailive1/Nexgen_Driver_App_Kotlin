package com.corverxis.nexgendriver.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.corverxis.nexgendriver.data.Coordinate
import com.corverxis.nexgendriver.network.RouteInfo
import com.corverxis.nexgendriver.ui.theme.NexgenAccent
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun TripMapView(
    modifier: Modifier = Modifier,
    driverCoordinate: Coordinate?,
    pickup: Coordinate?,
    drop: Coordinate?,
    route: RouteInfo?
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = "NexgenDriver"
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
            }
        },
        update = { map ->
            map.overlays.clear()

            val points = mutableListOf<GeoPoint>()

            driverCoordinate?.let {
                val gp = GeoPoint(it.lat, it.lng)
                points.add(gp)
                map.overlays.add(Marker(map).apply { position = gp; title = "You" })
            }
            pickup?.let {
                val gp = GeoPoint(it.lat, it.lng)
                points.add(gp)
                map.overlays.add(Marker(map).apply { position = gp; title = "Pickup" })
            }
            drop?.let {
                val gp = GeoPoint(it.lat, it.lng)
                points.add(gp)
                map.overlays.add(Marker(map).apply { position = gp; title = "Drop-off" })
            }
            route?.let { r ->
                val line = Polyline().apply {
                    setPoints(r.points.map { GeoPoint(it.lat, it.lng) })
                    outlinePaint.color = NexgenAccent.toArgb()
                    outlinePaint.strokeWidth = 8f
                }
                map.overlays.add(line)
                map.zoomToBoundingBox(line.bounds, false, 60)
            } ?: run {
                points.firstOrNull()?.let { map.controller.setCenter(it) }
            }

            map.invalidate()
        }
    )
}
