package com.trackly.core.network

import android.util.Log
import com.trackly.core.common.network.Resource
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus
import javax.inject.Inject
import javax.inject.Singleton

interface OrderRepository {
    suspend fun createOrder(pickupAddress: String, pickupLat: Double, pickupLng: Double, deliveryAddress: String, deliveryLat: Double, deliveryLng: Double): Resource<Order>
    suspend fun getActiveOrder(): Resource<Order>
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, remark: String?): Resource<Order>
    suspend fun assignDriver(orderId: String): Resource<Order>
}

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderApi: OrderApi,
    private val sessionManager: SessionManager
) : OrderRepository {

    companion object {
        private const val TAG = "TracklyOrderRepo"
    }

    private fun getAuthHeader(): String {
        val token = sessionManager.getJwtToken() ?: ""
        return "Bearer $token"
    }

    override suspend fun createOrder(
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        deliveryAddress: String,
        deliveryLat: Double,
        deliveryLng: Double
    ): Resource<Order> {
        Log.d(TAG, "Creating new order: pickup=$pickupAddress, delivery=$deliveryAddress")
        return try {
            val response = orderApi.createOrder(
                authHeader = getAuthHeader(),
                request = ApiCreateOrderRequest(
                    pickupAddress = pickupAddress,
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    deliveryAddress = deliveryAddress,
                    deliveryLat = deliveryLat,
                    deliveryLng = deliveryLng
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Log.d(TAG, "Order created successfully: orderId=${dto.id}, orderNumber=${dto.orderNumber}")
                Resource.Success(mapDtoToOrder(dto))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create order"
                Log.w(TAG, "Create order error: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception creating order", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun getActiveOrder(): Resource<Order> {
        Log.d(TAG, "Fetching active order from server...")
        return try {
            val response = orderApi.getActiveOrder(authHeader = getAuthHeader())
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Log.d(TAG, "Active order fetched: orderId=${dto.id}, status=${dto.status}")
                Resource.Success(mapDtoToOrder(dto))
            } else {
                Log.w(TAG, "No active order found or response code=${response.code()}")
                Resource.Error("No active order found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception fetching active order", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        remark: String?
    ): Resource<Order> {
        Log.d(TAG, "Updating order status: orderId=$orderId, newStatus=${newStatus.name}")
        return try {
            val response = orderApi.updateOrderStatus(
                authHeader = getAuthHeader(),
                orderId = orderId,
                request = ApiUpdateOrderStatusRequest(newStatus = newStatus.name, remark = remark)
            )

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Log.d(TAG, "Order status updated successfully to ${dto.status}")
                Resource.Success(mapDtoToOrder(dto))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to update status"
                Log.w(TAG, "Update order status error: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception updating order status", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun assignDriver(orderId: String): Resource<Order> {
        Log.d(TAG, "Assigning driver to orderId=$orderId")
        return try {
            val response = orderApi.assignDriver(
                authHeader = getAuthHeader(),
                orderId = orderId
            )

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Log.d(TAG, "Driver assigned successfully to orderId=${dto.id}")
                Resource.Success(mapDtoToOrder(dto))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to assign driver"
                Log.w(TAG, "Assign driver error: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception assigning driver", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    private fun mapDtoToOrder(dto: ApiOrderDto): Order {
        val status = try { OrderStatus.valueOf(dto.status) } catch (e: Exception) { OrderStatus.CREATED }
        return Order(
            id = dto.id,
            orderNumber = dto.orderNumber,
            customerId = dto.customerId,
            driverId = dto.driverId,
            status = status,
            pickupAddress = dto.pickupAddress,
            pickupLat = dto.pickupLat,
            pickupLng = dto.pickupLng,
            deliveryAddress = dto.deliveryAddress,
            deliveryLat = dto.deliveryLat,
            deliveryLng = dto.deliveryLng,
            estimatedDurationMinutes = dto.estimatedDurationMinutes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
