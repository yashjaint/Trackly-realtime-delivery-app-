package com.trackly.feature.aiassistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackly.core.model.Order
import com.trackly.feature.aiassistant.domain.AiMessage
import com.trackly.feature.aiassistant.domain.usecase.AskAiAssistantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val activeOrder: Order? = null,
    val driverLat: Double? = null,
    val driverLng: Double? = null
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val askAiAssistantUseCase: AskAiAssistantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    fun setOrderContext(order: Order?, driverLat: Double?, driverLng: Double?) {
        _uiState.update {
            it.copy(
                activeOrder = order,
                driverLat = driverLat,
                driverLng = driverLng
            )
        }

        if (_uiState.value.messages.isEmpty()) {
            val initialGreeting = if (order != null) {
                "Hello! I'm your Trackly AI assistant. I'm tracking your order #${order.orderNumber}. How can I help you today?"
            } else {
                "Hello! I'm your Trackly AI assistant. You don't have an active order right now, but I can answer questions about Trackly services."
            }
            _uiState.update {
                it.copy(messages = listOf(AiMessage(text = initialGreeting, isUser = false)))
            }
        }
    }

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return

        val userMessage = AiMessage(text = userPrompt, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true
            )
        }

        viewModelScope.launch {
            val order = _uiState.value.activeOrder
            val driverLat = _uiState.value.driverLat
            val driverLng = _uiState.value.driverLng

            val responseText = askAiAssistantUseCase(
                prompt = userPrompt,
                order = order,
                driverLat = driverLat,
                driverLng = driverLng
            )

            val assistantMessage = AiMessage(text = responseText, isUser = false)
            _uiState.update {
                it.copy(
                    messages = it.messages + assistantMessage,
                    isLoading = false
                )
            }
        }
    }
}
