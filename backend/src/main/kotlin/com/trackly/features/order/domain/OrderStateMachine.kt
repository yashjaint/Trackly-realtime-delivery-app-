package com.trackly.features.order.domain

import org.slf4j.LoggerFactory

enum class OrderStatusEnum {
    CREATED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

object OrderStateMachine {
    private val logger = LoggerFactory.getLogger(OrderStateMachine::class.java)

    private val validTransitions: Map<OrderStatusEnum, Set<OrderStatusEnum>> = mapOf(
        OrderStatusEnum.CREATED to setOf(OrderStatusEnum.CONFIRMED, OrderStatusEnum.CANCELLED),
        OrderStatusEnum.CONFIRMED to setOf(OrderStatusEnum.PREPARING, OrderStatusEnum.CANCELLED),
        OrderStatusEnum.PREPARING to setOf(OrderStatusEnum.READY_FOR_PICKUP, OrderStatusEnum.CANCELLED),
        OrderStatusEnum.READY_FOR_PICKUP to setOf(OrderStatusEnum.PICKED_UP),
        OrderStatusEnum.PICKED_UP to setOf(OrderStatusEnum.OUT_FOR_DELIVERY),
        OrderStatusEnum.OUT_FOR_DELIVERY to setOf(OrderStatusEnum.DELIVERED),
        OrderStatusEnum.DELIVERED to emptySet(),
        OrderStatusEnum.CANCELLED to emptySet()
    )

    fun validateTransition(currentStatus: OrderStatusEnum, targetStatus: OrderStatusEnum) {
        val allowedTargets = validTransitions[currentStatus] ?: emptySet()
        if (!allowedTargets.contains(targetStatus)) {
            logger.warn("Illegal state transition attempted: {} -> {}", currentStatus, targetStatus)
            throw IllegalStateException("Cannot transition order state from $currentStatus to $targetStatus. Allowed transitions: $allowedTargets")
        }
        logger.info("Order state transition validated: {} -> {}", currentStatus, targetStatus)
    }

    fun canTransition(currentStatus: OrderStatusEnum, targetStatus: OrderStatusEnum): Boolean {
        return validTransitions[currentStatus]?.contains(targetStatus) == true
    }
}
