package com.trackly.feature.aiassistant.data.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.trackly.core.model.Order
import com.trackly.feature.aiassistant.domain.repository.AiAssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAssistantRepositoryImpl @Inject constructor() : AiAssistantRepository {

    private companion object {
        private const val TAG = "TracklyAI"
        private val MODEL_CANDIDATES = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-3.7-flash"
        )
    }

    private var apiKey: String = ""

    init {
        apiKey = com.trackly.feature.aiassistant.BuildConfig.GEMINI_API_KEY.ifBlank {
            System.getenv("GEMINI_API_KEY") ?: System.getProperty("GEMINI_API_KEY") ?: ""
        }

        Log.i(TAG, "================================================")
        Log.i(TAG, "Initializing Trackly AI Assistant Repository")
        Log.i(TAG, "Gemini API Key detected: ${if (apiKey.isNotBlank()) "YES (Length: ${apiKey.length})" else "NO"}")

        if (apiKey.isNotBlank()) {
            Log.i(TAG, "STATUS: LIVE AGENT ACTIVE ✨ (Configured for model candidates: $MODEL_CANDIDATES)")
        } else {
            Log.w(TAG, "STATUS: OFFLINE / FALLBACK MODE ⚠️ (Set GEMINI_API_KEY to enable live Gemini AI)")
        }
        Log.i(TAG, "================================================")
    }

    override suspend fun getAiResponse(
        prompt: String,
        order: Order?,
        driverLat: Double?,
        driverLng: Double?
    ): String {
        Log.d(TAG, "Incoming Prompt: '$prompt' | Order #: ${order?.orderNumber ?: "NONE"} | Driver Coordinates: ($driverLat, $driverLng)")

        if (apiKey.isBlank() || order == null) {
            Log.w(TAG, "Executing Fallback Engine (No API Key or missing order context)")
            return generateSmartFallbackResponse(prompt, order, driverLat, driverLng)
        }

        val promptWithContext = """
            You are Trackly AI Assistant, an order tracking and delivery assistant.
            Here is the live order context:
            - Order Number: ${order.orderNumber}
            - Status: ${order.status.name}
            - Pickup Address: ${order.pickupAddress}
            - Delivery Address: ${order.deliveryAddress}
            - Estimated Duration: ${order.estimatedDurationMinutes ?: 20} minutes
            - Driver Live Location: ${if (driverLat != null && driverLng != null) "$driverLat, $driverLng" else "En route"}
            
            User question: $prompt
            
            Provide a friendly, concise, and helpful response.
        """.trimIndent()

        // Try model candidates sequentially until one succeeds
        for (modelName in MODEL_CANDIDATES) {
            try {
                Log.i(TAG, "Attempting Gemini API call with model: '$modelName'...")
                val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
                val response = model.generateContent(promptWithContext)
                val responseText = response.text

                if (!responseText.isNullOrBlank()) {
                    Log.i(TAG, "Gemini API ($modelName) Response SUCCESS! (Length: ${responseText.length} chars)")
                    Log.d(TAG, "Gemini Output: $responseText")
                    return responseText
                }
            } catch (e: Exception) {
                Log.w(TAG, "Model '$modelName' failed: ${e.message}. Trying next candidate...")
            }
        }

        Log.e(TAG, "All Gemini model candidates failed. Executing Smart Fallback Engine.")
        return generateSmartFallbackResponse(prompt, order, driverLat, driverLng)
    }

    private fun generateSmartFallbackResponse(
        prompt: String,
        order: Order?,
        driverLat: Double?,
        driverLng: Double?
    ): String {
        val result = if (order == null) {
            "You currently have no active order. Once you place an order, I can give you real-time updates on driver location and arrival times."
        } else {
            val lower = prompt.lowercase()
            when {
                lower.contains("eta") || lower.contains("time") || lower.contains("arrive") || lower.contains("when") -> {
                    "Your order #${order.orderNumber} is estimated to arrive in approximately ${order.estimatedDurationMinutes ?: 25} minutes."
                }
                lower.contains("driver") || lower.contains("where") || lower.contains("location") -> {
                    val locStr = if (driverLat != null && driverLng != null) {
                        "at coordinates (${String.format("%.4f", driverLat)}, ${String.format("%.4f", driverLng)})"
                    } else {
                        "en route to pickup/delivery"
                    }
                    "Your driver is currently ${order.status.name.replace("_", " ").lowercase()} and is $locStr."
                }
                lower.contains("pickup") || lower.contains("restaurant") || lower.contains("store") -> {
                    "The pickup location for your order is ${order.pickupAddress}."
                }
                lower.contains("delivery") || lower.contains("address") || lower.contains("drop") -> {
                    "Your order will be delivered to ${order.deliveryAddress}."
                }
                lower.contains("status") -> {
                    "Current Status: ${order.status.name.replace("_", " ")}."
                }
                else -> {
                    "Order #${order.orderNumber} is currently ${order.status.name.replace("_", " ")}. Target delivery: ${order.deliveryAddress}. Estimated arrival: ${order.estimatedDurationMinutes ?: 25} mins."
                }
            }
        }
        Log.d(TAG, "Fallback Engine Response: '$result'")
        return result
    }
}
