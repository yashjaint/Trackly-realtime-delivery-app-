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
        private const val PORT = 8080
    }

    private fun checkIsEmulator(): Boolean {
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val fingerprint = android.os.Build.FINGERPRINT
        val hardware = android.os.Build.HARDWARE
        val model = android.os.Build.MODEL
        val manufacturer = android.os.Build.MANUFACTURER
        val product = android.os.Build.PRODUCT

        return (brand.startsWith("generic") && device.startsWith("generic")) ||
                fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                manufacturer.contains("Genymotion") ||
                product.contains("sdk_gphone") ||
                product.contains("google_sdk") ||
                product.contains("sdk") ||
                product.contains("sdk_x86") ||
                product.contains("vbox86p") ||
                product.contains("emulator") ||
                product.contains("simulator")
    }

    private val host: String
        get() = if (checkIsEmulator()) "10.0.2.2" else "192.168.0.111"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    override fun observeLocationUpdates(orderId: String): Flow<LocationFrame> = flow {
        Log.d(TAG, "Opening WebSocket tracking connection for orderId=$orderId")
        try {
            client.webSocket(host = host, port = PORT, path = "/ws/tracking/$orderId") {
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
