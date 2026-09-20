package com.trackly.feature.aiassistant.domain

import java.util.UUID

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
