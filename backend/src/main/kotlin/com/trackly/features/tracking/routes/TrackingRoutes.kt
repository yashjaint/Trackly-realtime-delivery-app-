package com.trackly.features.tracking.routes

import com.trackly.features.tracking.service.TrackingSessionManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory

fun Route.trackingRoutes() {
    val logger = LoggerFactory.getLogger("TrackingRoutes")

    webSocket("/ws/tracking/{orderId}") {
        val orderId = call.parameters["orderId"]
        if (orderId.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Order ID required"))
            return@webSocket
        }

        logger.info("WebSocket client connected for orderId={}", orderId)
        TrackingSessionManager.addSession(orderId, this)

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    logger.debug("Received frame from client for orderId={}: {}", orderId, text)
                }
            }
        } catch (e: Exception) {
            logger.warn("WebSocket session error for orderId={}", orderId, e)
        } finally {
            TrackingSessionManager.removeSession(orderId, this)
            logger.info("WebSocket client disconnected for orderId={}", orderId)
        }
    }
}
