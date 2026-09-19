package com.trackly.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ApiUpdateLocationRequest(
    val lat: Double,
    val lng: Double
)

data class ApiGenericResponse(
    val status: String,
    val message: String
)

interface DriverApi {
    @POST("api/v1/driver/location")
    suspend fun updateLocation(
        @Header("Authorization") authHeader: String,
        @Body request: ApiUpdateLocationRequest
    ): Response<ApiGenericResponse>
}
