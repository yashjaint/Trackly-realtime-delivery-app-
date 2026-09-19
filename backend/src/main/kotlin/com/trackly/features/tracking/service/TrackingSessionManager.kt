package com.trackly.features.tracking.service

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Serializable
data class LocationFrameDto(
    val orderId: String,
    val driverId: String?,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)

object TrackingSessionManager {
    private val logger = LoggerFactory.getLogger(TrackingSessionManager::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val activeSessions = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>>()

    fun addSession(orderId: String, session: WebSocketSession) {
        logger.info("Adding WebSocket tracking session for orderId={}", orderId)
        activeSessions.computeIfAbsent(orderId) { CopyOnWriteArraySet() }.add(session)
    }

    fun removeSession(orderId: String, session: WebSocketSession) {
        logger.info("Removing WebSocket tracking session for orderId={}", orderId)
        val set = activeSessions[orderId]
        if (set != null) {
            set.remove(session)
            if (set.isEmpty()) {
                activeSessions.remove(orderId)
            }
        }
    }

    suspend fun broadcastLocation(orderId: String, driverId: String?, lat: Double, lng: Double) {
        val sessions = activeSessions[orderId]
        if (sessions.isNullOrEmpty()) {
            logger.debug("No active WebSocket listeners for orderId={}", orderId)
            return
        }

        val frame = LocationFrameDto(
            orderId = orderId,
            driverId = driverId,
            lat = lat,
            lng = lng
        )
        val payload = json.encodeToString(frame)
        logger.info("Broadcasting GPS frame to {} session(s) for orderId={}: lat={}, lng={}", sessions.size, orderId, lat, lng)

        val deadSessions = mutableListOf<WebSocketSession>()
        sessions.forEach { session ->
            try {
                session.send(Frame.Text(payload))
            } catch (e: Exception) {
                logger.warn("Failed to send WebSocket frame to session, marking as dead", e)
                deadSessions.add(session)
            }
        }

        deadSessions.forEach { removeSession(orderId, it) }
    }
}
