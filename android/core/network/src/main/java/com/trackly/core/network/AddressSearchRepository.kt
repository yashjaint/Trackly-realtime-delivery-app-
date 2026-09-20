package com.trackly.core.network

import android.util.Log
import com.trackly.core.model.AddressSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AddressSearchRepository {
    suspend fun searchAddress(
        query: String,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<AddressSearchResult>
}

@Singleton
class AddressSearchRepositoryImpl @Inject constructor(
    private val geocodingApi: GeocodingApi
) : AddressSearchRepository {

    companion object {
        private const val TAG = "TracklyGeocoding"
    }

    override suspend fun searchAddress(
        query: String,
        userLat: Double?,
        userLng: Double?
    ): List<AddressSearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) return@withContext emptyList()

        // Proximity center (Defaulting to Hyderabad: 17.3850, 78.4867 if user location is unspecified)
        val lat = userLat ?: 17.3850
        val lng = userLng ?: 78.4867
        val delta = 0.5 // ~50km radius bounding box
        val viewboxStr = "${lng - delta},${lat - delta},${lng + delta},${lat + delta}"

        Log.d(TAG, "================================================")
        Log.d(TAG, "🔍 Search Query: '$trimmedQuery'")
        Log.d(TAG, "📍 Location Proximity: ($lat, $lng)")
        Log.d(TAG, "🌐 Viewbox: $viewboxStr")
        Log.d(TAG, "================================================")

        try {
            val dtos = geocodingApi.searchAddress(
                query = trimmedQuery,
                viewbox = viewboxStr,
                bounded = 0
            )

            val results = dtos.mapNotNull { dto ->
                val resultLat = dto.lat.toDoubleOrNull()
                val resultLon = dto.lon.toDoubleOrNull()
                if (resultLat != null && resultLon != null) {
                    AddressSearchResult(
                        displayName = dto.displayName,
                        latitude = resultLat,
                        longitude = resultLon
                    )
                } else null
            }

            if (results.isNotEmpty()) {
                Log.d(TAG, "✅ Nominatim API returned ${results.size} search results for '$trimmedQuery'")
                results.forEachIndexed { index, res ->
                    Log.d(TAG, "   [#${index + 1}] 📌 ${res.displayName} (${res.latitude}, ${res.longitude})")
                }
                return@withContext results
            } else {
                Log.w(TAG, "⚠️ Nominatim returned empty results, trying Photon Fallback API...")
                return@withContext searchPhotonApi(trimmedQuery, lat, lng)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Primary Nominatim API failed for query '$trimmedQuery' (${e.localizedMessage}). Triggering Photon Fallback API...")
            return@withContext searchPhotonApi(trimmedQuery, lat, lng)
        }
    }

    private fun searchPhotonApi(query: String, lat: Double, lng: Double): List<AddressSearchResult> {
        return try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://photon.komoot.io/api/?q=$encodedQuery&lat=$lat&lon=$lng&limit=8"
            Log.d(TAG, "⚡ Calling Photon Fallback API: $urlStr")
            val url = java.net.URL(urlStr)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TracklyApp/1.0 (android)")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(jsonString)
                val features = root.optJSONArray("features") ?: return emptyList()
                val list = mutableListOf<AddressSearchResult>()

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val geometry = feature.optJSONObject("geometry") ?: continue
                    val coords = geometry.optJSONArray("coordinates") ?: continue
                    val lon = coords.optDouble(0)
                    val latVal = coords.optDouble(1)

                    val props = feature.optJSONObject("properties") ?: continue
                    val name = props.optString("name", "")
                    val street = props.optString("street", "")
                    val district = props.optString("district", props.optString("suburb", ""))
                    val city = props.optString("city", props.optString("state", ""))
                    val country = props.optString("country", "")

                    val fullTitle = listOf(name, street, district, city, country)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(", ")

                    if (fullTitle.isNotBlank() && !latVal.isNaN() && !lon.isNaN()) {
                        list.add(AddressSearchResult(displayName = fullTitle, latitude = latVal, longitude = lon))
                    }
                }

                Log.d(TAG, "⚡ Photon API returned ${list.size} results for '$query'")
                list.forEachIndexed { index, res ->
                    Log.d(TAG, "   [#${index + 1}] 📍 ${res.displayName} (${res.latitude}, ${res.longitude})")
                }
                list
            } else {
                Log.w(TAG, "❌ Photon API failed with HTTP ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Photon API exception", e)
            emptyList()
        }
    }
}
