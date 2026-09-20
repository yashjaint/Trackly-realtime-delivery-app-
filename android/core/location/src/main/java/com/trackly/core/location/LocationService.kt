package com.trackly.core.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.trackly.core.network.ApiUpdateLocationRequest
import com.trackly.core.network.DriverApi
import com.trackly.core.network.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var locationClient: LocationClient

    @Inject
    lateinit var driverApi: DriverApi

    @Inject
    lateinit var sessionManager: SessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "TracklyLocationService"
        const val ACTION_START = "ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_LOCATION_SERVICE"
        private const val CHANNEL_ID = "trackly_location_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        Log.d(TAG, "Starting location tracking service...")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trackly Driver Active")
            .setContentText("Broadcasting real-time GPS location...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            val hasLocationPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPerm) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException starting location foreground service: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed fallback startForeground", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting foreground service", e)
        }

        locationClient.getLocationUpdates(5000L)
            .catch { e -> Log.e(TAG, "Error in location updates stream", e) }
            .onEach { location ->
                val lat = location.latitude
                val lng = location.longitude
                Log.d(TAG, "GPS location captured: lat=$lat, lng=$lng")

                val token = sessionManager.getJwtToken()
                if (!token.isNull_orEmpty()) {
                    try {
                        val response = driverApi.updateLocation("Bearer $token", ApiUpdateLocationRequest(lat, lng))
                        if (response.isSuccessful) {
                            Log.d(TAG, "Driver location sent to backend successfully.")
                        } else {
                            Log.w(TAG, "Failed to send driver location: code=${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception posting location to backend", e)
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun stopTracking() {
        Log.d(TAG, "Stopping location tracking service...")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Location service destroyed.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Driver Location Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification channel for live driver location tracking"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
private fun String?.isNull_orEmpty(): Boolean = this == null || this.isEmpty()
