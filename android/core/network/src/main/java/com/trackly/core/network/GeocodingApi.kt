package com.trackly.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class NominatimResultDto(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String
)

interface GeocodingApi {
    @GET("search")
    suspend fun searchAddress(
        @Header("User-Agent") userAgent: String = "TracklyApp/1.0 (android)",
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 8,
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int = 0
    ): List<NominatimResultDto>
}
