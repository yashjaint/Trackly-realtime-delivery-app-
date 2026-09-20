package com.trackly.features.order.routes

import com.trackly.features.auth.dto.ErrorResponse
import com.trackly.features.order.dto.CreateOrderRequest
import com.trackly.features.order.dto.UpdateOrderStatusRequest
import com.trackly.features.order.service.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes(orderService: OrderService) {
    authenticate("auth-jwt") {
        route("/api/v1/orders") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val customerId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid user token", 401))

                val request = call.receive<CreateOrderRequest>()
                try {
                    val order = orderService.createOrder(customerId, request)
                    call.respond(HttpStatusCode.Created, order)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Failed to create order", 400))
                }
            }

            get("/active") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid user token", 401))

                val activeOrder = orderService.getActiveOrderForUser(userId)
                if (activeOrder != null) {
                    call.respond(HttpStatusCode.OK, activeOrder)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("No active order found", 404))
                }
            }

            get("/{id}") {
                val orderId = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing order id", 400))
                try {
                    val order = orderService.getOrderById(orderId)
                    call.respond(HttpStatusCode.OK, order)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Order not found", 404))
                }
            }

            patch("/{id}/status") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid user token", 401))

                val orderId = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing order id", 400))

                val request = call.receive<UpdateOrderStatusRequest>()
                try {
                    val updatedOrder = orderService.updateOrderStatus(orderId, userId, request.newStatus, request.remark)
                    call.respond(HttpStatusCode.OK, updatedOrder)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Illegal transition", 400))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Update failed", 400))
                }
            }

            post("/{id}/assign") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid user token", 401))

                val orderId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing order id", 400))

                try {
                    val assignedOrder = orderService.assignDriverToOrder(orderId, userId)
                    call.respond(HttpStatusCode.OK, assignedOrder)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Assignment failed", 400))
                }
            }
        }
    }
}
