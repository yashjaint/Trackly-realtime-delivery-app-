package com.trackly.features.driver.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateLocationRequest(
    val lat: Double,
    val lng: Double
)
