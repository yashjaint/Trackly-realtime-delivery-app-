package com.trackly.features.order.service

import com.trackly.features.auth.domain.DriversTable
import com.trackly.features.auth.domain.UsersTable
import com.trackly.features.order.domain.OrderStateMachine
import com.trackly.features.order.domain.OrderStatusEnum
import com.trackly.features.order.domain.OrderStatusHistoryTable
import com.trackly.features.order.domain.OrdersTable
import com.trackly.features.order.dto.CreateOrderRequest
import com.trackly.features.order.dto.OrderDto
import com.trackly.features.order.dto.OrderStatusHistoryDto
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class OrderService {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    fun createOrder(customerIdStr: String, request: CreateOrderRequest): OrderDto {
        val customerId = UUID.fromString(customerIdStr)
        val newOrderId = UUID.randomUUID()
        val orderNum = "ORD-${Random.nextInt(100000, 999999)}"

        logger.info("Creating new order for customerId={}, orderNumber={}", customerId, orderNum)

        val estimatedMins = calculateEstimatedDurationMinutes(
            request.pickupLat,
            request.pickupLng,
            request.deliveryLat,
            request.deliveryLng
        )

        transaction {
            OrdersTable.insert {
                it[id] = newOrderId
                it[orderNumber] = orderNum
                it[title] = request.title
                it[description] = request.description
                it[OrdersTable.customerId] = customerId
                it[driverId] = null
                it[status] = OrderStatusEnum.CREATED.name
                it[pickupAddress] = request.pickupAddress
                it[pickupLat] = request.pickupLat
                it[pickupLng] = request.pickupLng
                it[deliveryAddress] = request.deliveryAddress
                it[deliveryLat] = request.deliveryLat
                it[deliveryLng] = request.deliveryLng
                it[estimatedDurationMinutes] = estimatedMins
                it[estimatedDeliveryTime] = LocalDateTime.now().plusMinutes(estimatedMins.toLong())
                it[createdAt] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }

            OrderStatusHistoryTable.insert {
                it[id] = UUID.randomUUID()
                it[orderId] = newOrderId
                it[status] = OrderStatusEnum.CREATED.name
                it[remark] = "Order placed by customer"
                it[updatedBy] = customerId
                it[createdAt] = LocalDateTime.now()
            }
        }

        logger.info("Order created successfully: orderId={}, orderNumber={}", newOrderId, orderNum)
        return getOrderById(newOrderId.toString())
    }

    fun getOrderById(orderIdStr: String): OrderDto {
        val orderId = UUID.fromString(orderIdStr)
        return transaction {
            val orderRow = OrdersTable.selectAll().where { OrdersTable.id eq orderId }.singleOrNull()
                ?: throw IllegalArgumentException("Order with ID '$orderIdStr' not found")

            val driverIdVal = orderRow[OrdersTable.driverId]
            val driverNameVal = if (driverIdVal != null) {
                (DriversTable innerJoin UsersTable)
                    .selectAll()
                    .where { DriversTable.id eq driverIdVal }
                    .singleOrNull()
                    ?.get(UsersTable.name)
            } else null

            val historyRows = OrderStatusHistoryTable.selectAll()
                .where { OrderStatusHistoryTable.orderId eq orderId }
                .orderBy(OrderStatusHistoryTable.createdAt to SortOrder.ASC)
                .map {
                    OrderStatusHistoryDto(
                        id = it[OrderStatusHistoryTable.id].toString(),
                        orderId = it[OrderStatusHistoryTable.orderId].toString(),
                        status = it[OrderStatusHistoryTable.status],
                        remark = it[OrderStatusHistoryTable.remark],
                        updatedBy = it[OrderStatusHistoryTable.updatedBy].toString(),
                        createdAt = it[OrderStatusHistoryTable.createdAt].toString()
                    )
                }

            OrderDto(
                id = orderRow[OrdersTable.id].toString(),
                orderNumber = orderRow[OrdersTable.orderNumber],
                title = orderRow[OrdersTable.title],
                description = orderRow[OrdersTable.description],
                customerId = orderRow[OrdersTable.customerId].toString(),
                driverId = driverIdVal?.toString(),
                driverName = driverNameVal,
                status = orderRow[OrdersTable.status],
                pickupAddress = orderRow[OrdersTable.pickupAddress],
                pickupLat = orderRow[OrdersTable.pickupLat],
                pickupLng = orderRow[OrdersTable.pickupLng],
                deliveryAddress = orderRow[OrdersTable.deliveryAddress],
                deliveryLat = orderRow[OrdersTable.deliveryLat],
                deliveryLng = orderRow[OrdersTable.deliveryLng],
                estimatedDurationMinutes = orderRow[OrdersTable.estimatedDurationMinutes],
                estimatedDeliveryTime = orderRow[OrdersTable.estimatedDeliveryTime]?.toString(),
                createdAt = orderRow[OrdersTable.createdAt].toString(),
                updatedAt = orderRow[OrdersTable.updatedAt].toString(),
                history = historyRows
            )
        }
    }

    fun getActiveOrderForUser(userIdStr: String): OrderDto? {
        val userId = UUID.fromString(userIdStr)
        logger.info("Fetching active order for userId={}", userIdStr)

        val driverRow = transaction {
            DriversTable.selectAll().where { DriversTable.userId eq userId }.singleOrNull()
        }
        val driverId = driverRow?.get(DriversTable.id)

        var orderRow = transaction {
            OrdersTable.selectAll()
                .where {
                    val isCustomer = OrdersTable.customerId eq userId
                    val isDriver = if (driverId != null) OrdersTable.driverId eq driverId else null
                    val roleCondition = if (isDriver != null) (isCustomer or isDriver) else isCustomer

                    roleCondition and
                    (OrdersTable.status neq OrderStatusEnum.DELIVERED.name) and
                    (OrdersTable.status neq OrderStatusEnum.CANCELLED.name)
                }
                .orderBy(OrdersTable.createdAt to SortOrder.DESC)
                .firstOrNull()
        }

        // If user is a driver and currently has no active delivery assigned, auto-assign any unassigned order!
        if (orderRow == null && driverId != null) {
            orderRow = transaction {
                val unassignedOrder = OrdersTable.selectAll()
                    .where {
                        OrdersTable.driverId.isNull() and
                        (OrdersTable.status neq OrderStatusEnum.DELIVERED.name) and
                        (OrdersTable.status neq OrderStatusEnum.CANCELLED.name)
                    }
                    .orderBy(OrdersTable.createdAt to SortOrder.DESC)
                    .firstOrNull()

                if (unassignedOrder != null) {
                    val targetOrderId = unassignedOrder[OrdersTable.id]
                    OrdersTable.update({ OrdersTable.id eq targetOrderId }) {
                        it[OrdersTable.driverId] = driverId
                        it[updatedAt] = LocalDateTime.now()
                    }
                    logger.info("Auto-assigned unassigned orderId={} to driverId={}", targetOrderId, driverId)
                    OrdersTable.selectAll().where { OrdersTable.id eq targetOrderId }.singleOrNull()
                } else {
                    null
                }
            }
        }

        return orderRow?.get(OrdersTable.id)?.let { getOrderById(it.toString()) }
    }

    fun getOrderHistoryForUser(userIdStr: String): List<OrderDto> {
        val userId = UUID.fromString(userIdStr)
        logger.info("Fetching order history for userId={}", userIdStr)

        val driverRow = transaction {
            DriversTable.selectAll().where { DriversTable.userId eq userId }.singleOrNull()
        }
        val driverId = driverRow?.get(DriversTable.id)

        val orderIds = transaction {
            OrdersTable.selectAll()
                .where {
                    val isCustomer = OrdersTable.customerId eq userId
                    val isDriver = if (driverId != null) OrdersTable.driverId eq driverId else null
                    if (isDriver != null) (isCustomer or isDriver) else isCustomer
                }
                .orderBy(OrdersTable.createdAt to SortOrder.DESC)
                .map { it[OrdersTable.id].toString() }
        }

        return orderIds.map { getOrderById(it) }
    }

    fun updateOrderStatus(orderIdStr: String, updatedByUserIdStr: String, newStatusStr: String, remark: String?): OrderDto {
        val orderId = UUID.fromString(orderIdStr)
        val updatedByUserId = UUID.fromString(updatedByUserIdStr)

        val targetStatus = try {
            OrderStatusEnum.valueOf(newStatusStr.uppercase().trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid status: '$newStatusStr'")
        }

        transaction {
            val orderRow = OrdersTable.selectAll().where { OrdersTable.id eq orderId }.singleOrNull()
                ?: throw IllegalArgumentException("Order not found")

            val currentStatusStr = orderRow[OrdersTable.status]
            val currentStatus = OrderStatusEnum.valueOf(currentStatusStr)

            // Validate against Order State Machine
            OrderStateMachine.validateTransition(currentStatus, targetStatus)

            OrdersTable.update({ OrdersTable.id eq orderId }) {
                it[status] = targetStatus.name
                it[updatedAt] = LocalDateTime.now()
            }

            OrderStatusHistoryTable.insert {
                it[id] = UUID.randomUUID()
                it[OrderStatusHistoryTable.orderId] = orderId
                it[status] = targetStatus.name
                it[OrderStatusHistoryTable.remark] = remark ?: "Status updated to ${targetStatus.name}"
                it[updatedBy] = updatedByUserId
                it[createdAt] = LocalDateTime.now()
            }
        }

        logger.info("Order status successfully updated for orderId={}, newStatus={}", orderId, targetStatus.name)
        return getOrderById(orderIdStr)
    }

    fun assignDriverToOrder(orderIdStr: String, driverUserIdStr: String): OrderDto {
        val orderId = UUID.fromString(orderIdStr)
        val driverUserId = UUID.fromString(driverUserIdStr)

        val driverRow = transaction {
            DriversTable.selectAll().where { DriversTable.userId eq driverUserId }.singleOrNull()
                ?: throw IllegalArgumentException("Driver profile not found for user '$driverUserIdStr'")
        }
        val driverId = driverRow[DriversTable.id]

        transaction {
            OrdersTable.update({ OrdersTable.id eq orderId }) {
                it[OrdersTable.driverId] = driverId
                it[updatedAt] = LocalDateTime.now()
            }
        }

        logger.info("Assigned driverId={} (userId={}) to orderId={}", driverId, driverUserId, orderId)
        return getOrderById(orderIdStr)
    }

    private fun calculateEstimatedDurationMinutes(
        pickupLat: Double,
        pickupLng: Double,
        deliveryLat: Double,
        deliveryLng: Double
    ): Int {
        if (pickupLat == 0.0 || pickupLng == 0.0 || deliveryLat == 0.0 || deliveryLng == 0.0) {
            return 25
        }

        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(deliveryLat - pickupLat)
        val dLng = Math.toRadians(deliveryLng - pickupLng)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(pickupLat)) * Math.cos(Math.toRadians(deliveryLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val straightLineKm = r * c

        // Road factor: roads are ~30% longer than straight-line distance
        val roadKm = straightLineKm * 1.3

        // Determine average speed & buffer based on distance tier
        val (avgSpeedKmH, handlingBufferMins) = when {
            roadKm <= 10.0 -> 25.0 to 10 // Local city: 25 km/h + 10 mins buffer
            roadKm <= 100.0 -> 40.0 to 15 // Regional: 40 km/h + 15 mins buffer
            else -> 60.0 to 30 // Inter-state highway: 60 km/h + 30 mins buffer
        }

        val travelTimeMins = (roadKm / avgSpeedKmH) * 60.0
        return (travelTimeMins + handlingBufferMins).toInt().coerceAtLeast(10)
    }
}
