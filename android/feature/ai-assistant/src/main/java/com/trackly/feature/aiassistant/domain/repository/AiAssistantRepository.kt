package com.trackly.feature.aiassistant.domain.repository

import com.trackly.core.model.Order

interface AiAssistantRepository {
    suspend fun getAiResponse(
        prompt: String,
        order: Order?,
        driverLat: Double?,
        driverLng: Double?
    ): String
}
