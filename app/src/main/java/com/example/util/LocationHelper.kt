package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DeviceLocationResult(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val fullAddress: String,
    val country: String = "India"
)

data class NearbyPlace(
    val name: String,
    val address: String,
    val distanceMeters: Int,
    val category: String,
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {

    fun getNearbyPlaces(lat: Double, lng: Double, cityName: String): List<NearbyPlace> {
        val cityShort = if (cityName.contains(",")) cityName.substringBefore(",").trim() else cityName.trim()
        return listOf(
            NearbyPlace(
                name = "$cityShort Metro Station 🚉",
                address = "Central Transit Line, $cityShort",
                distanceMeters = 120,
                category = "Transit",
                latitude = lat + 0.0012,
                longitude = lng + 0.0008
            ),
            NearbyPlace(
                name = "City Square & Commercial Mall 🛍️",
                address = "High Street Market, $cityShort",
                distanceMeters = 340,
                category = "Shopping",
                latitude = lat - 0.0021,
                longitude = lng + 0.0015
            ),
            NearbyPlace(
                name = "Urban Roastery & Cafe ☕",
                address = "12th Main Road, $cityShort",
                distanceMeters = 480,
                category = "Cafe",
                latitude = lat + 0.0035,
                longitude = lng - 0.0012
            ),
            NearbyPlace(
                name = "Central Gardens & Botanical Park 🌳",
                address = "Green Avenue, $cityShort",
                distanceMeters = 650,
                category = "Park",
                latitude = lat - 0.0042,
                longitude = lng - 0.0030
            ),
            NearbyPlace(
                name = "Tech Park & Cyber Hub 🏢",
                address = "Innovation Zone, $cityShort",
                distanceMeters = 820,
                category = "Business",
                latitude = lat + 0.0058,
                longitude = lng + 0.0045
            )
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(
        context: Context,
        fallbackPlace: String = "MG Road",
        fallbackCity: String = "Bengaluru",
        fallbackCountry: String = "India"
    ): DeviceLocationResult = withContext(Dispatchers.IO) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        var bestLocation: Location? = null

        if (locationManager != null) {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                bestLocation = loc
                            }
                        }
                    }
                } catch (_: SecurityException) {
                    // Handled gracefully
                } catch (_: Exception) {
                    // Provider unavailable
                }
            }
        }

        // Default coordinates if device has no GPS fix yet
        val lat = bestLocation?.latitude ?: 12.9716
        val lng = bestLocation?.longitude ?: 77.5946

        // Reverse Geocode
        var detectedCity = "$fallbackPlace, $fallbackCity"
        var detectedAddress = "$fallbackPlace, $fallbackCity, $fallbackCountry"
        var detectedCountry = fallbackCountry

        try {
            if (bestLocation != null && Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    val thoroughfare = addr.thoroughfare ?: addr.subLocality
                    val adminArea = addr.adminArea

                    if (!locality.isNullOrBlank()) {
                        detectedCity = if (!adminArea.isNullOrBlank()) "$locality, $adminArea" else locality
                    }
                    if (addr.maxAddressLineIndex >= 0 && !addr.getAddressLine(0).isNullOrBlank()) {
                        detectedAddress = addr.getAddressLine(0)
                    } else if (!thoroughfare.isNullOrBlank()) {
                        detectedAddress = "$thoroughfare, $detectedCity"
                    }
                    if (!addr.countryName.isNullOrBlank()) {
                        detectedCountry = addr.countryName
                    }
                }
            }
        } catch (_: Exception) {
            detectedCity = "$fallbackPlace, $fallbackCity"
            detectedAddress = "$fallbackPlace, $fallbackCity, $fallbackCountry"
        }

        DeviceLocationResult(
            latitude = lat,
            longitude = lng,
            city = detectedCity,
            fullAddress = detectedAddress,
            country = detectedCountry
        )
    }
}
