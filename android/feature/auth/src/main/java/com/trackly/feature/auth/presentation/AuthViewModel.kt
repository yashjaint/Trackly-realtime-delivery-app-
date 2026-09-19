package com.trackly.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.model.UserRole
import com.trackly.feature.auth.domain.LoginUseCase
import com.trackly.feature.auth.domain.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "TracklyAuthVM"
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        Log.d(TAG, "ViewModel login triggered for email: $email")
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is Resource.Success -> {
                    Log.d(TAG, "Login ViewModel success: user=${result.data.email}, role=${result.data.role}")
                    _uiState.value = AuthUiState.Success(result.data)
                }
                is Resource.Error -> {
                    Log.w(TAG, "Login ViewModel error: ${result.message}")
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        vehicleNumber: String?
    ) {
        Log.d(TAG, "ViewModel register triggered for email: $email, role: $role")
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = registerUseCase(name, email, password, role, vehicleNumber)) {
                is Resource.Success -> {
                    Log.d(TAG, "Registration ViewModel success: user=${result.data.email}")
                    _uiState.value = AuthUiState.Success(result.data)
                }
                is Resource.Error -> {
                    Log.w(TAG, "Registration ViewModel error: ${result.message}")
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
