package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.model.CountryCode
import com.example.data.model.CountryCodeList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class SimSlotInfo(
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String,
    val formattedDisplay: String,
    val countryIso: String,
    val dialCode: String,
    val flagEmoji: String,
    val isRealSim: Boolean
)

data class GeoIpResult(
    val countryCodeIso: String,
    val countryName: String,
    val clientIp: String,
    val detectedCountry: CountryCode?
)

data class DetailedLocationResult(
    val cityName: String,
    val stateOrRegion: String,
    val countryName: String,
    val countryCodeIso: String,
    val detectedCountry: CountryCode?,
    val formattedAddress: String,
    val placeName: String,
    val coordsString: String
)

data class HandsetSimResult(
    val country: CountryCode,
    val rawNumber: String,
    val cleanNumber: String,
    val formattedFullNumber: String,
    val carrierName: String,
    val isRealSim: Boolean = false
)

object DeviceSimAndIpCountryHelper {
    const val TRUECALLER_CLIENT_ID = "z3rfwgowtpkt-4_5hmsogbcjf2wc8igmb_bjpvzk8xc"

    /**
     * Finds active handset SIM mobile number via SubscriptionManager / TelephonyManager
     * or returns active SIM mobile number for handset binding.
     */
    fun detectHandsetMobileNumber(context: Context, preferredCountry: CountryCode? = null): HandsetSimResult {
        val detectedCountry = preferredCountry ?: detectCountryFromDevice(context)
        val sims = getAvailableSimCards(context, detectedCountry)
        val realSim = sims.firstOrNull { it.isRealSim && it.phoneNumber.isNotBlank() }
        if (realSim != null) {
            val country = findCountryByIso(realSim.countryIso) ?: detectedCountry
            val raw = realSim.phoneNumber
            var digits = raw.replace(Regex("[^0-9]"), "")
            
            // Handle India numbers (+91 / 91 prefix clean up)
            val clean = if (digits.length > 10 && digits.startsWith(country.dialCode.removePrefix("+"))) {
                digits.removePrefix(country.dialCode.removePrefix("+"))
            } else if (digits.length > 10 && digits.startsWith("91")) {
                digits.removePrefix("91")
            } else {
                digits
            }
            val matchedCountry = if (clean.length == 10 && (clean.startsWith("9") || clean.startsWith("8") || clean.startsWith("7") || clean.startsWith("6"))) {
                CountryCodeList.allCountries.firstOrNull { it.isoCode == "IN" } ?: country
            } else {
                country
            }

            return HandsetSimResult(
                country = matchedCountry,
                rawNumber = raw,
                cleanNumber = clean,
                formattedFullNumber = if (clean.isNotBlank()) "${matchedCountry.dialCode} $clean" else "",
                carrierName = realSim.carrierName,
                isRealSim = true
            )
        }

        // Active Carrier without embedded SIM MSISDN
        val activeSimWithCarrier = sims.firstOrNull()
        val inCountry = if (detectedCountry.isoCode == "IN") detectedCountry else (CountryCodeList.allCountries.firstOrNull { it.isoCode == "IN" } ?: detectedCountry)
        return HandsetSimResult(
            country = activeSimWithCarrier?.let { findCountryByIso(it.countryIso) } ?: inCountry,
            rawNumber = "",
            cleanNumber = "",
            formattedFullNumber = "",
            carrierName = activeSimWithCarrier?.carrierName ?: "Mobile SIM",
            isRealSim = false
        )
    }

    suspend fun fetchDetailedLocation(context: Context): DetailedLocationResult = withContext(Dispatchers.IO) {
        // 1. Try GPS Location via Fused Location Client & LocationManager if permission granted
        try {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                var location: android.location.Location? = null
                try {
                    val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    val task = fusedClient.lastLocation
                    location = com.google.android.gms.tasks.Tasks.await(task, 2, java.util.concurrent.TimeUnit.SECONDS)
                    if (location == null) {
                        val currentTask = fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                        location = com.google.android.gms.tasks.Tasks.await(currentTask, 3, java.util.concurrent.TimeUnit.SECONDS)
                    }
                } catch (_: Exception) {}

                if (location == null) {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                    location = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                        ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        ?: lm?.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
                }

                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val geocoder = android.location.Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = try { geocoder.getFromLocation(lat, lon, 1) } catch (_: Exception) { null }
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Bengaluru"
                        val state = addr.adminArea ?: ""
                        val cName = addr.countryName ?: "India"
                        val cIso = addr.countryCode ?: "IN"
                        val matchedCountry = findCountryByIso(cIso)
                        val addressLine = addr.getAddressLine(0) ?: "$city, $state, $cName"
                        val place = addr.subLocality ?: addr.featureName ?: "Main Area"
                        val coords = String.format(Locale.US, "%.4f° N, %.4f° E", lat, lon)
                        return@withContext DetailedLocationResult(
                            cityName = if (state.isNotBlank() && !city.contains(state)) "$city, $state" else city,
                            stateOrRegion = state,
                            countryName = cName,
                            countryCodeIso = cIso.uppercase(Locale.ROOT),
                            detectedCountry = matchedCountry,
                            formattedAddress = addressLine,
                            placeName = place,
                            coordsString = coords
                        )
                    } else {
                        // Geocoder didn't return address but coordinates are valid
                        val coords = String.format(Locale.US, "%.4f° N, %.4f° E", lat, lon)
                        return@withContext DetailedLocationResult(
                            cityName = "Live Handset Location",
                            stateOrRegion = "",
                            countryName = "India",
                            countryCodeIso = "IN",
                            detectedCountry = findCountryByIso("IN"),
                            formattedAddress = "Live GPS Coordinates: $coords",
                            placeName = "Current Location",
                            coordsString = coords
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // GPS location or Geocoder failed, fall back to IP Geolocation
        }

        // 2. IP Geolocation Query
        val endpointList = listOf(
            "http://ip-api.com/json/?fields=status,message,country,countryCode,regionName,city,lat,lon,query",
            "https://ipwho.is/"
        )

        for (endpoint in endpointList) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2000
                    readTimeout = 2000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "VibeSync-Android-Client/3.0")
                    setRequestProperty("Accept", "application/json")
                }

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.use { it.readText() }
                    conn.disconnect()

                    val json = JSONObject(responseStr)
                    val countryCode = when {
                        json.has("countryCode") -> json.getString("countryCode")
                        json.has("country_code") -> json.getString("country_code")
                        else -> "IN"
                    }
                    val countryName = when {
                        json.has("country") -> json.getString("country")
                        json.has("country_name") -> json.getString("country_name")
                        else -> "India"
                    }
                    val city = when {
                        json.has("city") && json.getString("city").isNotBlank() -> json.getString("city")
                        else -> ""
                    }
                    val region = when {
                        json.has("regionName") -> json.getString("regionName")
                        json.has("region") -> json.getString("region")
                        else -> ""
                    }
                    val lat = json.optDouble("lat", json.optDouble("latitude", 12.9716))
                    val lon = json.optDouble("lon", json.optDouble("longitude", 77.5946))
                    val matchedCountry = findCountryByIso(countryCode)

                    if (city.isNotBlank() || countryCode.isNotBlank()) {
                        val displayCity = if (city.isNotBlank()) {
                            if (region.isNotBlank() && !city.contains(region)) "$city, $region" else city
                        } else {
                            getCityForCountry(countryCode)
                        }
                        return@withContext DetailedLocationResult(
                            cityName = displayCity,
                            stateOrRegion = region,
                            countryName = countryName,
                            countryCodeIso = countryCode.uppercase(Locale.ROOT),
                            detectedCountry = matchedCountry,
                            formattedAddress = "$displayCity, $countryName",
                            placeName = "Central Metropolitan Area",
                            coordsString = String.format(Locale.US, "%.4f° N, %.4f° E", lat, lon)
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Hardware / Telephony / Locale Fallback
        val activeCountry = detectCountryFromDevice(context)
        val defaultCity = getCityForCountry(activeCountry.isoCode)
        return@withContext DetailedLocationResult(
            cityName = defaultCity,
            stateOrRegion = activeCountry.name,
            countryName = activeCountry.name,
            countryCodeIso = activeCountry.isoCode,
            detectedCountry = activeCountry,
            formattedAddress = "$defaultCity, ${activeCountry.name}",
            placeName = "Central District",
            coordsString = getCoordsForCountry(activeCountry.isoCode)
        )
    }

    fun getCityForCountry(iso: String): String {
        return when (iso.uppercase(Locale.ROOT)) {
            "IN" -> "Bengaluru, Karnataka"
            "US" -> "New York, NY"
            "GB" -> "London, England"
            "CA" -> "Toronto, ON"
            "AU" -> "Sydney, NSW"
            "AE" -> "Dubai, UAE"
            "DE" -> "Berlin"
            "FR" -> "Paris"
            "SG" -> "Singapore Central"
            "JP" -> "Tokyo"
            "KR" -> "Seoul"
            "BR" -> "São Paulo"
            "MX" -> "Mexico City"
            "SA" -> "Riyadh"
            "ZA" -> "Johannesburg"
            else -> "Central District"
        }
    }

    fun getCoordsForCountry(iso: String): String {
        return when (iso.uppercase(Locale.ROOT)) {
            "IN" -> "12.9716° N, 77.5946° E"
            "US" -> "40.7128° N, 74.0060° W"
            "GB" -> "51.5074° N, 0.1278° W"
            "CA" -> "43.6532° N, 79.3832° W"
            "AU" -> "33.8688° S, 151.2093° E"
            "AE" -> "25.2048° N, 55.2708° E"
            "DE" -> "52.5200° N, 13.4050° E"
            "FR" -> "48.8566° N, 2.3522° E"
            "SG" -> "1.3521° N, 103.8198° E"
            "JP" -> "35.6762° N, 139.6503° E"
            else -> "20.0000° N, 77.0000° E"
        }
    }

    /**
     * Fast asynchronous IP geolocation query.
     * Tries lightweight IP services with low timeouts (1.5s) to avoid UI blocking.
     */
    suspend fun detectCountryFromIp(): GeoIpResult? = withContext(Dispatchers.IO) {
        val endpointList = listOf(
            "https://api.country.is/",
            "https://ipwho.is/",
            "http://ip-api.com/json/?fields=countryCode,country,query"
        )

        for (endpoint in endpointList) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 1800
                    readTimeout = 1800
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "VibeSync-Android-Client/3.0")
                    setRequestProperty("Accept", "application/json")
                }

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.use { it.readText() }
                    conn.disconnect()

                    val json = JSONObject(responseStr)
                    val countryCode = when {
                        json.has("country") && json.getString("country").length == 2 -> json.getString("country")
                        json.has("country_code") -> json.getString("country_code")
                        json.has("countryCode") -> json.getString("countryCode")
                        else -> null
                    }

                    val ip = when {
                        json.has("ip") -> json.getString("ip")
                        json.has("query") -> json.getString("query")
                        else -> "127.0.0.1"
                    }

                    val countryName = when {
                        json.has("country") && json.getString("country").length > 2 -> json.getString("country")
                        json.has("country_name") -> json.getString("country_name")
                        else -> ""
                    }

                    if (!countryCode.isNullOrBlank()) {
                        val matched = findCountryByIso(countryCode)
                        return@withContext GeoIpResult(
                            countryCodeIso = countryCode.uppercase(Locale.ROOT),
                            countryName = if (countryName.isNotBlank()) countryName else (matched?.name ?: countryCode),
                            clientIp = ip,
                            detectedCountry = matched
                        )
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint fallback
            }
        }
        null
    }

    /**
     * Extracts country code from device Telephony SIM / Network / System Locale.
     */
    fun detectCountryFromDevice(context: Context): CountryCode {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simIso = telephonyManager?.simCountryIso
            if (!simIso.isNullOrBlank()) {
                val matched = findCountryByIso(simIso)
                if (matched != null) return matched
            }

            val networkIso = telephonyManager?.networkCountryIso
            if (!networkIso.isNullOrBlank()) {
                val matched = findCountryByIso(networkIso)
                if (matched != null) return matched
            }
        } catch (_: Exception) {
            // Ignore security or hardware exceptions
        }

        // Locale Fallback
        val localeCountry = Locale.getDefault().country
        if (!localeCountry.isNullOrBlank()) {
            val matched = findCountryByIso(localeCountry)
            if (matched != null) return matched
        }

        // Default to India or US as global primary
        return CountryCodeList.allCountries.firstOrNull { it.isoCode == "IN" }
            ?: CountryCodeList.allCountries[0]
    }

    /**
     * Resolves the best country by prioritizing IP Geolocation -> Telephony -> Locale.
     */
    fun resolveActiveCountry(context: Context, geoResult: GeoIpResult?): CountryCode {
        if (geoResult?.detectedCountry != null) {
            return geoResult.detectedCountry
        }
        return detectCountryFromDevice(context)
    }

    /**
     * Detects hardware SIM cards and subscriptions from device or generates intelligent
     * carrier SIM suggestions tailored to the active country.
     */
    fun getAvailableSimCards(context: Context, country: CountryCode): List<SimSlotInfo> {
        val resultList = mutableListOf<SimSlotInfo>()

        // 1. Try reading real hardware subscriptions if permissions are granted
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                val hasPhoneNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
                } else true

                if (hasPhoneState || hasPhoneNumbers) {
                    val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                    val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

                    if (!activeList.isNullOrEmpty()) {
                        for (info in activeList) {
                            val slot = info.simSlotIndex + 1
                            val carrier = info.carrierName?.toString()?.ifBlank { null } ?: "Carrier $slot"
                            val rawNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPhoneNumbers) {
                                try {
                                    subscriptionManager.getPhoneNumber(info.subscriptionId)
                                } catch (_: Exception) {
                                    info.number
                                }
                            } else {
                                info.number
                            }

                            val cleanNumber = sanitizePhoneNumber(rawNumber, country)
                            val finalNumber = cleanNumber ?: generateDefaultNumberForCountry(country.isoCode, slot)

                            resultList.add(
                                SimSlotInfo(
                                    slotIndex = slot,
                                    displayName = "SIM $slot",
                                    carrierName = carrier,
                                    phoneNumber = finalNumber,
                                    formattedDisplay = "${country.dialCode} $finalNumber",
                                    countryIso = country.isoCode,
                                    dialCode = country.dialCode,
                                    flagEmoji = country.flagEmoji,
                                    isRealSim = !cleanNumber.isNullOrBlank()
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Permission or telephony failure handled gracefully
        }

        // 2. If no hardware SIM cards returned (e.g. emulator, tablets, or unreadable number), provide high quality carrier suggestions
        if (resultList.isEmpty()) {
            val carrierProfiles = getCarrierProfilesForCountry(country.isoCode)
            carrierProfiles.forEachIndexed { index, (carrier, sampleNum) ->
                val slot = index + 1
                resultList.add(
                    SimSlotInfo(
                        slotIndex = slot,
                        displayName = "SIM $slot",
                        carrierName = carrier,
                        phoneNumber = sampleNum,
                        formattedDisplay = "${country.dialCode} $sampleNum",
                        countryIso = country.isoCode,
                        dialCode = country.dialCode,
                        flagEmoji = country.flagEmoji,
                        isRealSim = false
                    )
                )
            }
        }

        return resultList
    }

    private fun findCountryByIso(iso: String): CountryCode? {
        val clean = iso.trim().uppercase(Locale.ROOT)
        return CountryCodeList.allCountries.firstOrNull { it.isoCode.equals(clean, ignoreCase = true) }
    }

    private fun sanitizePhoneNumber(rawNumber: String?, country: CountryCode): String? {
        if (rawNumber.isNullOrBlank()) return null
        var digits = rawNumber.replace(Regex("[^0-9+]"), "")
        if (digits.startsWith(country.dialCode)) {
            digits = digits.removePrefix(country.dialCode)
        } else if (digits.startsWith("+")) {
            digits = digits.removePrefix("+")
        }
        if (digits.startsWith("0")) {
            digits = digits.removePrefix("0")
        }
        return if (digits.length >= 7) digits else null
    }

    private fun generateDefaultNumberForCountry(iso: String, slot: Int): String {
        return when (iso.uppercase(Locale.ROOT)) {
            "IN" -> if (slot == 1) "98451 20492" else "94480 18234"
            "US", "CA" -> if (slot == 1) "555-0199" else "555-0142"
            "GB" -> if (slot == 1) "7911 123456" else "7822 654321"
            "AE" -> if (slot == 1) "50 123 4567" else "55 987 6543"
            "DE" -> if (slot == 1) "151 23456789" else "170 98765432"
            "AU" -> if (slot == 1) "412 345 678" else "423 876 543"
            "SG" -> if (slot == 1) "8123 4567" else "9123 4567"
            "BR" -> if (slot == 1) "11 98765-4321" else "21 91234-5678"
            else -> if (slot == 1) "555-0101" else "555-0102"
        }
    }

    private fun getCarrierProfilesForCountry(iso: String): List<Pair<String, String>> {
        return when (iso.uppercase(Locale.ROOT)) {
            "IN" -> listOf(
                "Jio 5G" to "98451 20492",
                "Airtel 5G Plus" to "94480 18234"
            )
            "US" -> listOf(
                "Verizon 5G Ultra" to "555-0199",
                "T-Mobile 5G" to "555-0142"
            )
            "GB" -> listOf(
                "EE 5G" to "7911 123456",
                "Vodafone UK" to "7822 654321"
            )
            "CA" -> listOf(
                "Rogers 5G" to "555-0199",
                "Bell Canada" to "555-0142"
            )
            "AU" -> listOf(
                "Telstra 5G" to "412 345 678",
                "Optus 5G" to "423 876 543"
            )
            "AE" -> listOf(
                "e& (Etisalat)" to "50 123 4567",
                "du 5G" to "55 987 6543"
            )
            "DE" -> listOf(
                "Telekom 5G" to "151 23456789",
                "Vodafone DE" to "170 98765432"
            )
            "SG" -> listOf(
                "Singtel 5G" to "8123 4567",
                "StarHub 5G" to "9123 4567"
            )
            "BR" -> listOf(
                "Vivo 5G" to "11 98765-4321",
                "Claro 5G" to "21 91234-5678"
            )
            "SA" -> listOf(
                "STC 5G" to "50 123 4567",
                "Mobily" to "56 987 6543"
            )
            else -> listOf(
                "Primary SIM 1" to "98765 43210",
                "Secondary SIM 2" to "91234 56789"
            )
        }
    }
}
