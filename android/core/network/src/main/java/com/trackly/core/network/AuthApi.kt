package com.trackly.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class ApiRegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val vehicleNumber: String? = null
)

data class ApiLoginRequest(
    val email: String,
    val password: String
)

data class ApiUserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val fcmToken: String? = null
)

data class ApiAuthResponse(
    val token: String,
    val user: ApiUserDto
)

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: ApiRegisterRequest): Response<ApiAuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: ApiLoginRequest): Response<ApiAuthResponse>

    @GET("api/v1/auth/me")
    suspend fun getProfile(@Header("Authorization") authHeader: String): Response<ApiUserDto>
}
