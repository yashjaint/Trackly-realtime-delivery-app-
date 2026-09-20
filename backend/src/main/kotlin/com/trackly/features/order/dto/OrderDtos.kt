package com.trackly.features.order.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val title: String? = null,
    val description: String? = null,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double
)

@Serializable
data class UpdateOrderStatusRequest(
    val newStatus: String,
    val remark: String? = null
)

@Serializable
data class AssignDriverRequest(
    val driverId: String
)

@Serializable
data class OrderStatusHistoryDto(
    val id: String,
    val orderId: String,
    val status: String,
    val remark: String? = null,
    val updatedBy: String,
    val createdAt: String
)

@Serializable
data class OrderDto(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val driverId: String? = null,
    val driverName: String? = null,
    val title: String? = null,
    val description: String? = null,
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
    val history: List<OrderStatusHistoryDto> = emptyList()
)
