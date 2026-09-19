package com.trackly.core.websocket

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface TrackingWebSocketClient {
    fun observeLocationUpdates(orderId: String): Flow<LocationFrame>
}

@Singleton
class TrackingWebSocketClientImpl @Inject constructor() : TrackingWebSocketClient {

    companion object {
        private const val TAG = "TracklyWebSocket"
        private const val HOST = "10.0.2.2" // Suitable for Android emulator & reverse port forwarding on device
        private const val PORT = 8080
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    override fun observeLocationUpdates(orderId: String): Flow<LocationFrame> = flow {
        Log.d(TAG, "Opening WebSocket tracking connection for orderId=$orderId")
        try {
            client.webSocket(host = HOST, port = PORT, path = "/ws/tracking/$orderId") {
                Log.d(TAG, "WebSocket connected successfully for orderId=$orderId")
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        Log.d(TAG, "WebSocket frame received for orderId=$orderId: $text")
                        try {
                            val locationFrame = json.decodeFromString<LocationFrame>(text)
                            emit(locationFrame)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding LocationFrame JSON", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket connection error for orderId=$orderId", e)
        } finally {
            Log.d(TAG, "WebSocket closed for orderId=$orderId")
        }
    }
}
