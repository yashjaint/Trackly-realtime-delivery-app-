package com.trackly.feature.auth.domain

import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.network.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Please enter your email address")
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Resource.Error("Please enter a valid email address (e.g., user@example.com)")
        }
        if (password.isBlank()) {
            return Resource.Error("Please enter your password")
        }
        return authRepository.login(trimmedEmail, password)
    }
}
