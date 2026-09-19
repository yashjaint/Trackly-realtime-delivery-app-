package com.trackly.feature.driverdelivery.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackly.core.common.network.Resource
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus
import com.trackly.core.network.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DriverDeliveryUiState {
    object Idle : DriverDeliveryUiState()
    object Loading : DriverDeliveryUiState()
    data class ActiveDelivery(
        val order: Order,
        val isUpdating: Boolean = false,
        val errorMessage: String? = null,
        val driverLat: Double? = null,
        val driverLng: Double? = null
    ) : DriverDeliveryUiState()
    object NoActiveDelivery : DriverDeliveryUiState()
    data class Error(val message: String) : DriverDeliveryUiState()
}

@HiltViewModel
class DriverDeliveryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val webSocketClient: com.trackly.core.websocket.TrackingWebSocketClient
) : ViewModel() {

    companion object {
        private const val TAG = "TracklyDriverVM"
    }

    private val _uiState = MutableStateFlow<DriverDeliveryUiState>(DriverDeliveryUiState.Idle)
    val uiState: StateFlow<DriverDeliveryUiState> = _uiState.asStateFlow()

    private var webSocketJob: kotlinx.coroutines.Job? = null

    fun fetchActiveDelivery() {
        Log.d(TAG, "Fetching active delivery for Driver...")
        _uiState.value = DriverDeliveryUiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.getActiveOrder()) {
                is Resource.Success -> {
                    Log.d(TAG, "Driver active delivery found: orderNumber=${result.data.orderNumber}, status=${result.data.status}")
                    _uiState.value = DriverDeliveryUiState.ActiveDelivery(order = result.data)
                    startWebSocketObservation(result.data.id)
                }
                is Resource.Error -> {
                    Log.w(TAG, "No active delivery for driver: ${result.message}")
                    _uiState.value = DriverDeliveryUiState.NoActiveDelivery
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun startWebSocketObservation(orderId: String) {
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            Log.d(TAG, "Driver observing WebSocket location for orderId=$orderId")
            webSocketClient.observeLocationUpdates(orderId).collect { frame ->
                val current = (_uiState.value as? DriverDeliveryUiState.ActiveDelivery)
                if (current != null && current.order.id == frame.orderId) {
                    _uiState.value = current.copy(driverLat = frame.lat, driverLng = frame.lng)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketJob?.cancel()
    }

    fun updateStatus(orderId: String, newStatus: OrderStatus, remark: String?) {
        Log.d(TAG, "Driver updating order status: orderId=$orderId, targetStatus=${newStatus.name}")
        val currentOrder = (_uiState.value as? DriverDeliveryUiState.ActiveDelivery)?.order
        if (currentOrder != null) {
            _uiState.value = DriverDeliveryUiState.ActiveDelivery(order = currentOrder, isUpdating = true)
        } else {
            _uiState.value = DriverDeliveryUiState.Loading
        }
        viewModelScope.launch {
            when (val result = orderRepository.updateOrderStatus(orderId, newStatus, remark)) {
                is Resource.Success -> {
                    Log.d(TAG, "Order status updated to ${result.data.status}")
                    _uiState.value = DriverDeliveryUiState.ActiveDelivery(order = result.data, isUpdating = false)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Driver status update failed: ${result.message}")
                    if (currentOrder != null) {
                        _uiState.value = DriverDeliveryUiState.ActiveDelivery(
                            order = currentOrder,
                            isUpdating = false,
                            errorMessage = result.message
                        )
                    } else {
                        _uiState.value = DriverDeliveryUiState.Error(result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun assignCurrentOrder(orderId: String) {
        Log.d(TAG, "Driver accepting orderId=$orderId")
        _uiState.value = DriverDeliveryUiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.assignDriver(orderId)) {
                is Resource.Success -> {
                    Log.d(TAG, "Driver accepted order: ${result.data.orderNumber}")
                    _uiState.value = DriverDeliveryUiState.ActiveDelivery(order = result.data)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Accept order failed: ${result.message}")
                    _uiState.value = DriverDeliveryUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }
}
