package com.trackly.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val driverId: String?,
    val status: String,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double,
    val estimatedDurationMinutes: Int?,
    val estimatedDeliveryTime: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

fun OrderEntity.toDomain(): Order {
    return Order(
        id = id,
        orderNumber = orderNumber,
        customerId = customerId,
        driverId = driverId,
        status = try { OrderStatus.valueOf(status) } catch (e: Exception) { OrderStatus.CREATED },
        pickupAddress = pickupAddress,
        pickupLat = pickupLat,
        pickupLng = pickupLng,
        deliveryAddress = deliveryAddress,
        deliveryLat = deliveryLat,
        deliveryLng = deliveryLng,
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Order.toEntity(isSynced: Boolean = true): OrderEntity {
    return OrderEntity(
        id = id,
        orderNumber = orderNumber,
        customerId = customerId,
        driverId = driverId,
        status = status.name,
        pickupAddress = pickupAddress,
        pickupLat = pickupLat,
        pickupLng = pickupLng,
        deliveryAddress = deliveryAddress,
        deliveryLat = deliveryLat,
        deliveryLng = deliveryLng,
        estimatedDurationMinutes = estimatedDurationMinutes,
        estimatedDeliveryTime = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = isSynced
    )
}
