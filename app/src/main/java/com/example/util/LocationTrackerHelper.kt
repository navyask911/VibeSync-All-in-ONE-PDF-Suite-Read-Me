package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class GpsCoordinate(
    val latitude: Double,
    val longitude: Double,
    val label: String = "Current Location",
    val isRealTimeGps: Boolean = false
)

object LocationTrackerHelper {

    // Default reference location (Bangalore Central)
    val DEFAULT_BANGALORE_COORDINATES = GpsCoordinate(
        latitude = 12.9716,
        longitude = 77.5946,
        label = "Bangalore Central (Default)",
        isRealTimeGps = false
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    suspend fun getDeviceLocation(context: Context): GpsCoordinate = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext DEFAULT_BANGALORE_COORDINATES
        }

        var loc: Location? = null

        // Try FusedLocationProviderClient first
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val lastLocTask = fusedClient.lastLocation
            loc = Tasks.await(lastLocTask, 1500, TimeUnit.MILLISECONDS)
            if (loc == null) {
                val currentTask = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                loc = Tasks.await(currentTask, 2000, TimeUnit.MILLISECONDS)
            }
        } catch (_: Exception) {
            // Google Play Services location might timeout in emulator or mock setup
        }

        // Fallback to LocationManager (GPS or Network)
        if (loc == null) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                }
            } catch (_: Exception) {}
        }

        if (loc != null) {
            val friendlyName = try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addrs = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                if (!addrs.isNullOrEmpty()) {
                    val addr = addrs[0]
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Nearby"
                } else {
                    "GPS (${String.format("%.3f", loc.latitude)}, ${String.format("%.3f", loc.longitude)})"
                }
            } catch (_: Exception) {
                "GPS (${String.format("%.3f", loc.latitude)}, ${String.format("%.3f", loc.longitude)})"
            }

            GpsCoordinate(
                latitude = loc.latitude,
                longitude = loc.longitude,
                label = friendlyName,
                isRealTimeGps = true
            )
        } else {
            DEFAULT_BANGALORE_COORDINATES
        }
    }

    /**
     * Calculates distance between device coordinates and venue coordinates in kilometers
     */
    fun calculateDistanceKm(
        deviceLat: Double,
        deviceLon: Double,
        venueLat: Double,
        venueLon: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(deviceLat, deviceLon, venueLat, venueLon, results)
        val km = results[0] / 1000.0
        val rounded = (km * 10.0).roundToInt() / 10.0
        return if (rounded < 0.1) 0.1 else rounded
    }

    fun formatDistance(km: Double): String {
        return if (km < 1.0) {
            "${(km * 1000).toInt()} m"
        } else {
            "$km km"
        }
    }
}
