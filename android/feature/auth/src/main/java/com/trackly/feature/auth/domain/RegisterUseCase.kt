package com.trackly.feature.auth.domain

import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.model.UserRole
import com.trackly.core.network.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        vehicleNumber: String?
    ): Resource<User> {
        if (name.isBlank()) {
            return Resource.Error("Name cannot be empty")
        }
        if (email.isBlank() || !email.contains("@")) {
            return Resource.Error("Please enter a valid email address")
        }
        if (password.length < 6) {
            return Resource.Error("Password must be at least 6 characters long")
        }
        if (role == UserRole.DRIVER && vehicleNumber.isNullOrBlank()) {
            return Resource.Error("Vehicle number is required for Driver registration")
        }
        return authRepository.register(name.trim(), email.trim(), password, role, vehicleNumber?.trim())
    }
}
