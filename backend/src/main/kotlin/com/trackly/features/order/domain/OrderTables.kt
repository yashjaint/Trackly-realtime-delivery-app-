package com.trackly.features.order.domain

import com.trackly.features.auth.domain.DriversTable
import com.trackly.features.auth.domain.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object OrdersTable : Table("orders") {
    val id = uuid("id").defaultExpression(org.jetbrains.exposed.sql.CustomFunction("random_uuid", org.jetbrains.exposed.sql.UUIDColumnType()))
    val orderNumber = varchar("order_number", 64).uniqueIndex()
    val customerId = reference("customer_id", UsersTable.id)
    val driverId = reference("driver_id", DriversTable.id).nullable()
    val status = varchar("status", 32)
    val pickupAddress = varchar("pickup_address", 256)
    val pickupLat = double("pickup_lat")
    val pickupLng = double("pickup_lng")
    val deliveryAddress = varchar("delivery_address", 256)
    val deliveryLat = double("delivery_lat")
    val deliveryLng = double("delivery_lng")
    val estimatedDurationMinutes = integer("estimated_duration_minutes").nullable()
    val estimatedDeliveryTime = datetime("estimated_delivery_time").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object OrderStatusHistoryTable : Table("order_status_history") {
    val id = uuid("id").defaultExpression(org.jetbrains.exposed.sql.CustomFunction("random_uuid", org.jetbrains.exposed.sql.UUIDColumnType()))
    val orderId = reference("order_id", OrdersTable.id)
    val status = varchar("status", 32)
    val remark = varchar("remark", 256).nullable()
    val updatedBy = reference("updated_by", UsersTable.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
