package com.trackly.core.network.domain.repository

import com.trackly.core.common.network.Resource
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus

interface OrderRepository {
    suspend fun createOrder(
        title: String? = null,
        description: String? = null,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        deliveryAddress: String,
        deliveryLat: Double,
        deliveryLng: Double
    ): Resource<Order>

    suspend fun getActiveOrder(): Resource<Order>
    suspend fun getActiveOrders(): Resource<List<Order>>
    suspend fun getOrderHistory(): Resource<List<Order>>
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, remark: String?): Resource<Order>
    suspend fun assignDriver(orderId: String): Resource<Order>
    suspend fun syncPendingActions()
}
