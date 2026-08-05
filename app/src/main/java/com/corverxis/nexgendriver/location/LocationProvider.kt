package com.corverxis.nexgendriver.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.*
import com.corverxis.nexgendriver.data.Coordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationProvider(context: Context) {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _coordinate = MutableStateFlow<Coordinate?>(null)
    val coordinate: StateFlow<Coordinate?> = _coordinate

    var onUpdate: ((Coordinate) -> Unit)? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val coord = Coordinate(lat = loc.latitude, lng = loc.longitude)
            _coordinate.value = coord
            onUpdate?.invoke(coord)
        }
    }

    @SuppressLint("MissingPermission") // caller must have requested ACCESS_FINE_LOCATION first
    fun startUpdating() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        client.requestLocationUpdates(request, callback, null)
    }

    fun stopUpdating() {
        client.removeLocationUpdates(callback)
    }
}
