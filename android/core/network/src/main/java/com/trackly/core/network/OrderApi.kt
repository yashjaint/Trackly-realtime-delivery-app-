package com.trackly.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class ApiCreateOrderRequest(
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double
)

data class ApiUpdateOrderStatusRequest(
    val newStatus: String,
    val remark: String? = null
)

data class ApiOrderStatusHistoryDto(
    val id: String,
    val orderId: String,
    val status: String,
    val remark: String? = null,
    val updatedBy: String,
    val createdAt: String
)

data class ApiOrderDto(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val driverId: String? = null,
    val status: String,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double,
    val estimatedDurationMinutes: Int? = null,
    val estimatedDeliveryTime: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val history: List<ApiOrderStatusHistoryDto> = emptyList()
)

interface OrderApi {
    @POST("api/v1/orders")
    suspend fun createOrder(
        @Header("Authorization") authHeader: String,
        @Body request: ApiCreateOrderRequest
    ): Response<ApiOrderDto>

    @GET("api/v1/orders/active")
    suspend fun getActiveOrder(
        @Header("Authorization") authHeader: String
    ): Response<ApiOrderDto>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderById(
        @Header("Authorization") authHeader: String,
        @Path("id") orderId: String
    ): Response<ApiOrderDto>

    @PATCH("api/v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Header("Authorization") authHeader: String,
        @Path("id") orderId: String,
        @Body request: ApiUpdateOrderStatusRequest
    ): Response<ApiOrderDto>

    @POST("api/v1/orders/{id}/assign")
    suspend fun assignDriver(
        @Header("Authorization") authHeader: String,
        @Path("id") orderId: String
    ): Response<ApiOrderDto>
}
