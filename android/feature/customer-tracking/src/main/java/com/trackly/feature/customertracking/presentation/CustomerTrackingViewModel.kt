package com.trackly.feature.customertracking.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackly.core.common.network.Resource
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus
import com.trackly.core.network.OrderRepository
import com.trackly.core.websocket.TrackingWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CustomerOrderUiState {
    object Idle : CustomerOrderUiState()
    object Loading : CustomerOrderUiState()
    data class ActiveOrder(
        val order: Order,
        val driverLat: Double? = null,
        val driverLng: Double? = null
    ) : CustomerOrderUiState()
    object NoActiveOrder : CustomerOrderUiState()
    data class Error(val message: String) : CustomerOrderUiState()
}

@HiltViewModel
class CustomerTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val webSocketClient: TrackingWebSocketClient
) : ViewModel() {

    companion object {
        private const val TAG = "TracklyCustomerVM"
    }

    private val _uiState = MutableStateFlow<CustomerOrderUiState>(CustomerOrderUiState.Idle)
    val uiState: StateFlow<CustomerOrderUiState> = _uiState.asStateFlow()

    private var webSocketJob: Job? = null

    fun fetchActiveOrder() {
        Log.d(TAG, "Fetching active order for Customer...")
        _uiState.value = CustomerOrderUiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.getActiveOrder()) {
                is Resource.Success -> {
                    Log.d(TAG, "Active order found: ${result.data.orderNumber}, status=${result.data.status}")
                    _uiState.value = CustomerOrderUiState.ActiveOrder(order = result.data)
                    startWebSocketObservation(result.data.id)
                }
                is Resource.Error -> {
                    Log.w(TAG, "No active order: ${result.message}")
                    _uiState.value = CustomerOrderUiState.NoActiveOrder
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun createSampleOrder() {
        Log.d(TAG, "Creating sample order for Customer...")
        _uiState.value = CustomerOrderUiState.Loading
        viewModelScope.launch {
            val result = orderRepository.createOrder(
                pickupAddress = "Italian Bistro, 100 Market St",
                pickupLat = 37.7749,
                pickupLng = -122.4194,
                deliveryAddress = "Customer Residence, 742 Evergreen Ter",
                deliveryLat = 37.7833,
                deliveryLng = -122.4167
            )
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Sample order created: ${result.data.orderNumber}")
                    _uiState.value = CustomerOrderUiState.ActiveOrder(order = result.data)
                    startWebSocketObservation(result.data.id)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Sample order creation failed: ${result.message}")
                    _uiState.value = CustomerOrderUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun startWebSocketObservation(orderId: String) {
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            Log.d(TAG, "Starting WebSocket observation for orderId=$orderId")
            webSocketClient.observeLocationUpdates(orderId).collect { frame ->
                Log.d(TAG, "Received driver location via WebSocket: lat=${frame.lat}, lng=${frame.lng}")
                val current = (_uiState.value as? CustomerOrderUiState.ActiveOrder)
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
}
