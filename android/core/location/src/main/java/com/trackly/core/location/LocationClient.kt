package com.trackly.core.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(intervalMs: Long): Flow<Location>
    class LocationException(message: String) : Exception(message)
}
