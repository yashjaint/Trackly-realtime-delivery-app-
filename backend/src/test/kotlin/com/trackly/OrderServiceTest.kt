package com.trackly

import com.trackly.core.database.DatabaseFactory
import com.trackly.features.auth.dto.RegisterRequest
import com.trackly.features.auth.service.AuthService
import com.trackly.features.order.dto.CreateOrderRequest
import com.trackly.features.order.service.OrderService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class OrderServiceTest {

    private lateinit var authService: AuthService
    private lateinit var orderService: OrderService

    @BeforeTest
    fun setup() {
        DatabaseFactory.init()
        authService = AuthService()
        orderService = OrderService()
    }

    @Test
    fun `create order initializes state to CREATED with status history`() {
        val customerRes = authService.register(
            RegisterRequest("Order Customer", "ordcust_${System.currentTimeMillis()}@trackly.com", "Password123!", "CUSTOMER")
        )

        val createReq = CreateOrderRequest(
            pickupAddress = "123 Main St, San Francisco",
            pickupLat = 37.7749,
            pickupLng = -122.4194,
            deliveryAddress = "456 Market St, San Francisco",
            deliveryLat = 37.7833,
            deliveryLng = -122.4167
        )

        val order = orderService.createOrder(customerRes.user.id, createReq)

        assertNotNull(order.id)
        assertNotNull(order.orderNumber)
        assertEquals("CREATED", order.status)
        assertEquals(1, order.history.size)
        assertEquals("CREATED", order.history[0].status)
    }

    @Test
    fun `order status transition sequence CREATED to DELIVERED succeeds`() {
        val customerRes = authService.register(
            RegisterRequest("Seq Customer", "seqcust_${System.currentTimeMillis()}@trackly.com", "Password123!", "CUSTOMER")
        )
        val driverRes = authService.register(
            RegisterRequest("Seq Driver", "seqdriver_${System.currentTimeMillis()}@trackly.com", "Password123!", "DRIVER", "DL-999")
        )

        val order = orderService.createOrder(customerRes.user.id, CreateOrderRequest("A", 0.0, 0.0, "B", 1.0, 1.0))

        val assignedOrder = orderService.assignDriverToOrder(order.id, driverRes.user.id)
        assertNotNull(assignedOrder.driverId)

        val o1 = orderService.updateOrderStatus(order.id, customerRes.user.id, "CONFIRMED", "Payment received")
        assertEquals("CONFIRMED", o1.status)

        val o2 = orderService.updateOrderStatus(order.id, customerRes.user.id, "PREPARING", "Kitchen cooking")
        assertEquals("PREPARING", o2.status)

        val o3 = orderService.updateOrderStatus(order.id, customerRes.user.id, "READY_FOR_PICKUP", "Ready at counter")
        assertEquals("READY_FOR_PICKUP", o3.status)

        val o4 = orderService.updateOrderStatus(order.id, driverRes.user.id, "PICKED_UP", "Driver grabbed bag")
        assertEquals("PICKED_UP", o4.status)

        val o5 = orderService.updateOrderStatus(order.id, driverRes.user.id, "OUT_FOR_DELIVERY", "On the way")
        assertEquals("OUT_FOR_DELIVERY", o5.status)

        val o6 = orderService.updateOrderStatus(order.id, driverRes.user.id, "DELIVERED", "Handed to customer")
        assertEquals("DELIVERED", o6.status)
        assertEquals(7, o6.history.size)
    }

    @Test
    fun `illegal status transition from CREATED to DELIVERED throws error`() {
        val customerRes = authService.register(
            RegisterRequest("Err Customer", "errcust_${System.currentTimeMillis()}@trackly.com", "Password123!", "CUSTOMER")
        )
        val order = orderService.createOrder(customerRes.user.id, CreateOrderRequest("A", 0.0, 0.0, "B", 1.0, 1.0))

        assertFailsWith<IllegalStateException> {
            orderService.updateOrderStatus(order.id, customerRes.user.id, "DELIVERED", "Skip ahead")
        }
    }
}
