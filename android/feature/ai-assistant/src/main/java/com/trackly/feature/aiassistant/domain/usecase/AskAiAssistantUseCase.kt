package com.trackly.feature.aiassistant.domain.usecase

import com.trackly.core.model.Order
import com.trackly.feature.aiassistant.domain.repository.AiAssistantRepository
import javax.inject.Inject

class AskAiAssistantUseCase @Inject constructor(
    private val repository: AiAssistantRepository
) {
    suspend operator fun invoke(
        prompt: String,
        order: Order?,
        driverLat: Double?,
        driverLng: Double?
    ): String {
        if (prompt.isBlank()) return ""
        return repository.getAiResponse(prompt, order, driverLat, driverLng)
    }
}
