package com.trackly.feature.auth.domain

import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.network.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        if (email.isBlank() || !email.contains("@")) {
            return Resource.Error("Please enter a valid email address")
        }
        if (password.isBlank()) {
            return Resource.Error("Password cannot be empty")
        }
        return authRepository.login(email.trim(), password)
    }
}
