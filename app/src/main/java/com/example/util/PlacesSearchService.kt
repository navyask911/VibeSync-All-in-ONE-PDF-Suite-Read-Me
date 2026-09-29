package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class PlaceSearchResult(
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val address: String
)

object PlacesSearchService {
    private const val TAG = "PlacesSearchService"
    private val client = OkHttpClient()

    suspend fun searchPlaces(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=5"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VibeSyncVenueManager/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(bodyString)
                val results = mutableListOf<PlaceSearchResult>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val displayName = obj.optString("display_name", "")
                    val lat = obj.optString("lat", "0").toDoubleOrNull() ?: 0.0
                    val lon = obj.optString("lon", "0").toDoubleOrNull() ?: 0.0
                    results.add(PlaceSearchResult(displayName, lat, lon, displayName))
                }
                results
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to search places: ${e.message}")
            emptyList()
        }
    }
}
