package com.trackly.core.model

enum class UserRole {
    CUSTOMER,
    DRIVER,
    ADMIN
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val fcmToken: String? = null
)

enum class OrderStatus {
    CREATED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

data class Order(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val driverId: String?,
    val status: OrderStatus,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double,
    val estimatedDurationMinutes: Int? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class LocationEvent(
    val eventId: String,
    val orderId: String,
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val bearing: Double,
    val sequenceNumber: Long,
    val timestamp: Long
)

data class DriverLocation(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val bearing: Double
)
