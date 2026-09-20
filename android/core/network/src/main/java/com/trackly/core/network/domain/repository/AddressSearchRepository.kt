package com.trackly.core.network.domain.repository

import com.trackly.core.model.AddressSearchResult

interface AddressSearchRepository {
    suspend fun searchAddress(
        query: String,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<AddressSearchResult>
}
