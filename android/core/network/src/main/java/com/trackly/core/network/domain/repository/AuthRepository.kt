package com.trackly.core.network.domain.repository

import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.model.UserRole

interface AuthRepository {
    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        vehicleNumber: String?
    ): Resource<User>

    suspend fun login(
        email: String,
        password: String
    ): Resource<User>

    suspend fun updateProfile(
        name: String,
        vehicleNumber: String?
    ): Resource<User>

    suspend fun deleteAccount(): Resource<Unit>
}
