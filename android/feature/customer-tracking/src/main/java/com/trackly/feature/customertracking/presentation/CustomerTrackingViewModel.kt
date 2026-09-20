package com.trackly.feature.customertracking.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackly.core.common.network.Resource
import com.trackly.core.model.AddressSearchResult
import com.trackly.core.model.Order
import com.trackly.core.network.AddressSearchRepository
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
    data class ActiveOrders(
        val orders: List<Order>,
        val selectedIndex: Int = 0,
        val driverLat: Double? = null,
        val driverLng: Double? = null
    ) : CustomerOrderUiState() {
        val selectedOrder: Order get() = orders.getOrElse(selectedIndex.coerceIn(0, (orders.size - 1).coerceAtLeast(0))) { orders.first() }
    }
    object NoActiveOrder : CustomerOrderUiState()
    data class Error(val message: String) : CustomerOrderUiState()
}

data class CustomerHistoryUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CustomerTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val webSocketClient: TrackingWebSocketClient,
    private val addressSearchRepository: AddressSearchRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TracklyCustomerVM"
    }

    private val _uiState = MutableStateFlow<CustomerOrderUiState>(CustomerOrderUiState.Idle)
    val uiState: StateFlow<CustomerOrderUiState> = _uiState.asStateFlow()

    private val _historyState = MutableStateFlow(CustomerHistoryUiState())
    val historyState: StateFlow<CustomerHistoryUiState> = _historyState.asStateFlow()

    private val _pickupSuggestions = MutableStateFlow<List<AddressSearchResult>>(emptyList())
    val pickupSuggestions: StateFlow<List<AddressSearchResult>> = _pickupSuggestions.asStateFlow()

    private val _deliverySuggestions = MutableStateFlow<List<AddressSearchResult>>(emptyList())
    val deliverySuggestions: StateFlow<List<AddressSearchResult>> = _deliverySuggestions.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    private var webSocketJob: Job? = null
    private var searchPickupJob: Job? = null
    private var searchDeliveryJob: Job? = null

    fun fetchActiveOrder() {
        Log.d(TAG, "Fetching active orders for Customer...")
        _uiState.value = CustomerOrderUiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.getActiveOrders()) {
                is Resource.Success -> {
                    if (result.data.isEmpty()) {
                        Log.d(TAG, "No active orders found")
                        _uiState.value = CustomerOrderUiState.NoActiveOrder
                    } else {
                        Log.d(TAG, "Found ${result.data.size} active orders")
                        _uiState.value = CustomerOrderUiState.ActiveOrders(orders = result.data, selectedIndex = 0)
                        startWebSocketObservation(result.data.first().id)
                    }
                }
                is Resource.Error -> {
                    Log.w(TAG, "Error fetching active orders: ${result.message}")
                    _uiState.value = CustomerOrderUiState.NoActiveOrder
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun selectActiveOrder(index: Int) {
        val current = _uiState.value as? CustomerOrderUiState.ActiveOrders ?: return
        val safeIndex = index.coerceIn(0, current.orders.size - 1)
        if (safeIndex != current.selectedIndex) {
            val selected = current.orders[safeIndex]
            Log.d(TAG, "Selected active order #${selected.orderNumber} (index $safeIndex)")
            _uiState.value = current.copy(selectedIndex = safeIndex, driverLat = null, driverLng = null)
            startWebSocketObservation(selected.id)
        }
    }

    fun selectActiveOrderById(orderId: String) {
        val current = _uiState.value as? CustomerOrderUiState.ActiveOrders
        if (current != null) {
            val foundIndex = current.orders.indexOfFirst { it.id == orderId }
            if (foundIndex != -1) {
                selectActiveOrder(foundIndex)
            }
        } else {
            fetchActiveOrder()
        }
    }

    fun fetchOrderHistory() {
        Log.d(TAG, "Fetching order history for Customer...")
        _historyState.value = CustomerHistoryUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = orderRepository.getOrderHistory()) {
                is Resource.Success -> {
                    Log.d(TAG, "Fetched ${result.data.size} orders for history")
                    _historyState.value = CustomerHistoryUiState(orders = result.data, isLoading = false)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error fetching order history: ${result.message}")
                    _historyState.value = CustomerHistoryUiState(error = result.message, isLoading = false)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun searchPickupAddress(query: String) {
        searchPickupJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _pickupSuggestions.value = emptyList()
            return
        }
        searchPickupJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            val results = addressSearchRepository.searchAddress(trimmed)
            _pickupSuggestions.value = results
        }
    }

    fun searchDeliveryAddress(query: String) {
        searchDeliveryJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _deliverySuggestions.value = emptyList()
            return
        }
        searchDeliveryJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            val results = addressSearchRepository.searchAddress(trimmed)
            _deliverySuggestions.value = results
        }
    }

    fun createCustomOrder(
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        deliveryAddress: String,
        deliveryLat: Double,
        deliveryLng: Double
    ) {
        Log.d(TAG, "Creating custom order: pickup=$pickupAddress, delivery=$deliveryAddress")
        _isSubmittingOrder.value = true
        _uiState.value = CustomerOrderUiState.Loading
        viewModelScope.launch {
            val result = orderRepository.createOrder(
                pickupAddress = pickupAddress,
                pickupLat = pickupLat,
                pickupLng = pickupLng,
                deliveryAddress = deliveryAddress,
                deliveryLat = deliveryLat,
                deliveryLng = deliveryLng
            )
            _isSubmittingOrder.value = false
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Custom order created successfully: ${result.data.orderNumber}")
                    fetchActiveOrder()
                    fetchOrderHistory()
                }
                is Resource.Error -> {
                    Log.e(TAG, "Custom order creation failed: ${result.message}")
                    _uiState.value = CustomerOrderUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun createSampleOrder() {
        createCustomOrder(
            pickupAddress = "Central Bakery, 10th Ave",
            pickupLat = 37.7749,
            pickupLng = -122.4194,
            deliveryAddress = "Downtown Office, Suite 400",
            deliveryLat = 37.7833,
            deliveryLng = -122.4167
        )
    }

    private fun startWebSocketObservation(orderId: String) {
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            Log.d(TAG, "Starting WebSocket observation for orderId=$orderId")
            webSocketClient.observeLocationUpdates(orderId).collect { frame ->
                Log.d(TAG, "Received driver location via WebSocket: lat=${frame.lat}, lng=${frame.lng}")
                val current = (_uiState.value as? CustomerOrderUiState.ActiveOrders)
                if (current != null && current.selectedOrder.id == frame.orderId) {
                    _uiState.value = current.copy(driverLat = frame.lat, driverLng = frame.lng)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketJob?.cancel()
        searchPickupJob?.cancel()
        searchDeliveryJob?.cancel()
    }
}
