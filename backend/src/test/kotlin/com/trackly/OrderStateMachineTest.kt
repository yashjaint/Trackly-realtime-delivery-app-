package com.trackly

import com.trackly.features.order.domain.OrderStateMachine
import com.trackly.features.order.domain.OrderStatusEnum
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderStateMachineTest {

    @Test
    fun `valid transition path CREATED to DELIVERED succeeds`() {
        OrderStateMachine.validateTransition(OrderStatusEnum.CREATED, OrderStatusEnum.CONFIRMED)
        OrderStateMachine.validateTransition(OrderStatusEnum.CONFIRMED, OrderStatusEnum.PREPARING)
        OrderStateMachine.validateTransition(OrderStatusEnum.PREPARING, OrderStatusEnum.READY_FOR_PICKUP)
        OrderStateMachine.validateTransition(OrderStatusEnum.READY_FOR_PICKUP, OrderStatusEnum.PICKED_UP)
        OrderStateMachine.validateTransition(OrderStatusEnum.PICKED_UP, OrderStatusEnum.OUT_FOR_DELIVERY)
        OrderStateMachine.validateTransition(OrderStatusEnum.OUT_FOR_DELIVERY, OrderStatusEnum.DELIVERED)

        assertTrue(OrderStateMachine.canTransition(OrderStatusEnum.OUT_FOR_DELIVERY, OrderStatusEnum.DELIVERED))
    }

    @Test
    fun `illegal transition from PICKED_UP to CREATED fails`() {
        assertFailsWith<IllegalStateException> {
            OrderStateMachine.validateTransition(OrderStatusEnum.PICKED_UP, OrderStatusEnum.CREATED)
        }
    }

    @Test
    fun `illegal transition from DELIVERED to OUT_FOR_DELIVERY fails`() {
        assertFailsWith<IllegalStateException> {
            OrderStateMachine.validateTransition(OrderStatusEnum.DELIVERED, OrderStatusEnum.OUT_FOR_DELIVERY)
        }
    }

    @Test
    fun `cancellation is allowed from CREATED, CONFIRMED, or PREPARING`() {
        OrderStateMachine.validateTransition(OrderStatusEnum.CREATED, OrderStatusEnum.CANCELLED)
        OrderStateMachine.validateTransition(OrderStatusEnum.CONFIRMED, OrderStatusEnum.CANCELLED)
        OrderStateMachine.validateTransition(OrderStatusEnum.PREPARING, OrderStatusEnum.CANCELLED)
    }

    @Test
    fun `cancellation is rejected after PICKED_UP`() {
        assertFailsWith<IllegalStateException> {
            OrderStateMachine.validateTransition(OrderStatusEnum.PICKED_UP, OrderStatusEnum.CANCELLED)
        }
    }
}
