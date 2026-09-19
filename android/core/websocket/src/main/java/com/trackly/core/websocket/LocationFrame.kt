package com.trackly.core.websocket

import kotlinx.serialization.Serializable

@Serializable
data class LocationFrame(
    val orderId: String,
    val driverId: String? = null,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)
