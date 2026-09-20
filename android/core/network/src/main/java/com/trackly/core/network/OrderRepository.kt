package com.trackly.core.network

import android.util.Log
import com.trackly.core.common.network.Resource
import com.trackly.core.database.dao.OrderDao
import com.trackly.core.database.dao.PendingActionDao
import com.trackly.core.database.entity.PendingActionEntity
import com.trackly.core.database.entity.toDomain
import com.trackly.core.database.entity.toEntity
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface OrderRepository {
    suspend fun createOrder(pickupAddress: String, pickupLat: Double, pickupLng: Double, deliveryAddress: String, deliveryLat: Double, deliveryLng: Double): Resource<Order>
    suspend fun getActiveOrder(): Resource<Order>
    suspend fun getOrderHistory(): Resource<List<Order>>
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, remark: String?): Resource<Order>
    suspend fun assignDriver(orderId: String): Resource<Order>
    suspend fun syncPendingActions()
}

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderApi: OrderApi,
    private val sessionManager: SessionManager,
    private val orderDao: OrderDao,
    private val pendingActionDao: PendingActionDao
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
    ): Resource<Order> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Creating new order: pickup=$pickupAddress, delivery=$deliveryAddress")
        try {
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

            val body = response.body()
            if (response.isSuccessful && body != null) {
                val dto = body
                val order = mapDtoToOrder(dto)
                orderDao.insertOrder(order.toEntity(isSynced = true))
                Log.d(TAG, "Order created & cached in Room DB: orderId=${dto.id}")
                syncPendingActions()
                Resource.Success(order)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create order"
                Log.w(TAG, "Create order error: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception creating order. Reading from local cache.", e)
            val cached = orderDao.getActiveOrder()
            if (cached != null) {
                Resource.Success(cached.toDomain())
            } else {
                Resource.Error(e.localizedMessage ?: "Offline - Network connection error")
            }
        }
    }

    override suspend fun getActiveOrder(): Resource<Order> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching active order (Remote + Room Cache)...")
        try {
            val response = orderApi.getActiveOrder(authHeader = getAuthHeader())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val dto = body
                val order = mapDtoToOrder(dto)
                orderDao.insertOrder(order.toEntity(isSynced = true))
                Log.d(TAG, "Active order updated in Room DB: orderId=${dto.id}")
                syncPendingActions()
                Resource.Success(order)
            } else {
                val cached = orderDao.getActiveOrder()
                if (cached != null) {
                    Log.d(TAG, "Server returned no active order, using cached order: ${cached.orderNumber}")
                    Resource.Success(cached.toDomain())
                } else {
                    Resource.Error("No active order found")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Offline mode - fetching active order from Room DB cache", e)
            val cached = orderDao.getActiveOrder()
            if (cached != null) {
                Resource.Success(cached.toDomain())
            } else {
                Resource.Error("Offline mode: No cached order available")
            }
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        remark: String?
    ): Resource<Order> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating order status: orderId=$orderId, newStatus=${newStatus.name}")
        
        // Optimistic Room Cache update
        orderDao.updateOrderStatus(orderId, newStatus.name, System.currentTimeMillis(), isSynced = false)

        try {
            val response = orderApi.updateOrderStatus(
                authHeader = getAuthHeader(),
                orderId = orderId,
                request = ApiUpdateOrderStatusRequest(newStatus = newStatus.name, remark = remark)
            )

            val body = response.body()
            if (response.isSuccessful && body != null) {
                val dto = body
                val order = mapDtoToOrder(dto)
                orderDao.insertOrder(order.toEntity(isSynced = true))
                Log.d(TAG, "Order status synced with server: ${dto.status}")
                Resource.Success(order)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to update status on server"
                queueOfflineAction(orderId, "UPDATE_STATUS", "{\"newStatus\":\"${newStatus.name}\",\"remark\":\"$remark\"}")
                val cached = orderDao.getOrderById(orderId)
                if (cached != null) Resource.Success(cached.toDomain()) else Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Offline network exception during status update. Queueing pending action.", e)
            queueOfflineAction(orderId, "UPDATE_STATUS", "{\"newStatus\":\"${newStatus.name}\",\"remark\":\"$remark\"}")
            val cached = orderDao.getOrderById(orderId)
            if (cached != null) {
                Resource.Success(cached.toDomain())
            } else {
                Resource.Error("Status updated locally (Offline)")
            }
        }
    }

    override suspend fun assignDriver(orderId: String): Resource<Order> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Assigning driver to orderId=$orderId")
        try {
            val response = orderApi.assignDriver(
                authHeader = getAuthHeader(),
                orderId = orderId
            )

            val body = response.body()
            if (response.isSuccessful && body != null) {
                val dto = body
                val order = mapDtoToOrder(dto)
                orderDao.insertOrder(order.toEntity(isSynced = true))
                Resource.Success(order)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to assign driver"
                val cached = orderDao.getOrderById(orderId)
                if (cached != null) Resource.Success(cached.toDomain()) else Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Offline exception assigning driver", e)
            val cached = orderDao.getOrderById(orderId)
            if (cached != null) Resource.Success(cached.toDomain()) else Resource.Error("Offline: Assign driver failed")
        }
    }

    override suspend fun syncPendingActions() {
        withContext(Dispatchers.IO) {
            try {
                val pendingActions = pendingActionDao.getAllPendingActions()
                if (pendingActions.isEmpty()) return@withContext

                Log.d(TAG, "Reconciling ${pendingActions.size} pending offline actions...")
                for (action in pendingActions) {
                    try {
                        if (action.actionType == "UPDATE_STATUS") {
                            val json = org.json.JSONObject(action.payloadJson)
                            val statusStr = json.optString("newStatus")
                            val remark = if (json.has("remark") && !json.isNull("remark")) json.getString("remark") else null
                            val status = OrderStatus.valueOf(statusStr)

                            val response = orderApi.updateOrderStatus(
                                authHeader = getAuthHeader(),
                                orderId = action.orderId,
                                request = ApiUpdateOrderStatusRequest(newStatus = status.name, remark = remark)
                            )
                            val body = response.body()
                            if (response.isSuccessful && body != null) {
                                val order = mapDtoToOrder(body)
                                orderDao.insertOrder(order.toEntity(isSynced = true))
                                pendingActionDao.deletePendingAction(action.id)
                                Log.d(TAG, "Successfully synced pending action ${action.id}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync action ${action.id}, will retry on next connection", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during pending actions sync", e)
            }
        }
    }

    private suspend fun queueOfflineAction(orderId: String, actionType: String, payloadJson: String) {
        try {
            val action = PendingActionEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                actionType = actionType,
                payloadJson = payloadJson,
                createdAt = System.currentTimeMillis()
            )
            pendingActionDao.insertPendingAction(action)
            Log.d(TAG, "Queued offline pending action: $actionType for orderId=$orderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error queueing offline action", e)
        }
    }

    override suspend fun getOrderHistory(): Resource<List<Order>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching order history from server...")
        try {
            val response = orderApi.getOrderHistory(authHeader = getAuthHeader())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val orders = body.map { mapDtoToOrder(it) }
                orders.forEach { orderDao.insertOrder(it.toEntity(isSynced = true)) }
                Log.d(TAG, "Fetched & cached ${orders.size} history orders")
                Resource.Success(orders)
            } else {
                val cached = orderDao.getAllOrders().map { it.toDomain() }
                Resource.Success(cached)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Offline mode - fetching order history from Room DB", e)
            val cached = orderDao.getAllOrders().map { it.toDomain() }
            Resource.Success(cached)
        }
    }

    private fun parseTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            java.time.LocalDateTime.parse(isoString).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.Instant.parse(isoString).toEpochMilli()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
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
            createdAt = parseTimestamp(dto.createdAt),
            updatedAt = parseTimestamp(dto.updatedAt)
        )
    }
}
