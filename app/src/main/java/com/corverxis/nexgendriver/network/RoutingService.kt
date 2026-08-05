package com.corverxis.nexgendriver.network

import com.corverxis.nexgendriver.data.Coordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class RouteInfo(
    val points: List<Coordinate>,
    val distanceKm: Double,
    val durationMin: Double
)

object RoutingService {
    private val client = OkHttpClient()

    suspend fun fetchRoute(from: Coordinate, to: Coordinate): RouteInfo? = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "${from.lng},${from.lat};${to.lng},${to.lat}?overview=full&geometries=geojson"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext straightLine(from, to)
                val body = response.body?.string() ?: return@withContext straightLine(from, to)
                val json = JSONObject(body)
                if (json.optString("code") != "Ok") return@withContext straightLine(from, to)

                val route = json.getJSONArray("routes").getJSONObject(0)
                val coordsArray = route.getJSONObject("geometry").getJSONArray("coordinates")
                val points = (0 until coordsArray.length()).map { i ->
                    val pair = coordsArray.getJSONArray(i)
                    Coordinate(lat = pair.getDouble(1), lng = pair.getDouble(0))
                }
                RouteInfo(
                    points = points,
                    distanceKm = route.getDouble("distance") / 1000,
                    durationMin = route.getDouble("duration") / 60
                )
            }
        } catch (e: Exception) {
            straightLine(from, to)
        }
    }

    private fun straightLine(from: Coordinate, to: Coordinate): RouteInfo {
        val distanceKm = haversineKm(from.lat, from.lng, to.lat, to.lng)
        return RouteInfo(points = listOf(from, to), distanceKm = distanceKm, durationMin = distanceKm * 2.2)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(a))
    }
}
