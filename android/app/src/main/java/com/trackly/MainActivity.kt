package com.trackly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackly.core.common.theme.TracklyTheme
import com.trackly.core.model.UserRole
import com.trackly.core.network.SessionManager
import com.trackly.feature.auth.presentation.AuthViewModel
import com.trackly.feature.auth.presentation.LoginScreen
import com.trackly.feature.auth.presentation.RegisterScreen
import com.trackly.feature.customertracking.presentation.CustomerOrderScreen
import com.trackly.feature.customertracking.presentation.CustomerTrackingViewModel
import com.trackly.feature.driverdelivery.presentation.DriverDeliveryScreen
import com.trackly.feature.driverdelivery.presentation.DriverDeliveryViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TracklyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember {
                        val currentUser = sessionManager.getUser()
                        mutableStateOf(
                            when (currentUser?.role) {
                                UserRole.CUSTOMER -> "customer_home"
                                UserRole.DRIVER -> "driver_home"
                                null -> "login"
                                else -> "login"
                            }
                        )
                    }

                    val authViewModel: AuthViewModel = hiltViewModel()

                    when (currentScreen) {
                        "login" -> {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = { currentScreen = "register" },
                                onLoginSuccess = { role ->
                                    currentScreen = if (role == UserRole.DRIVER) "driver_home" else "customer_home"
                                }
                            )
                        }
                        "register" -> {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { currentScreen = "login" },
                                onRegisterSuccess = { role ->
                                    currentScreen = if (role == UserRole.DRIVER) "driver_home" else "customer_home"
                                }
                            )
                        }
                        "customer_home" -> {
                            val customerVM: CustomerTrackingViewModel = hiltViewModel()
                            CustomerOrderScreen(
                                viewModel = customerVM,
                                onOpenAiChat = { orderId ->
                                    // Trackly AI sheet integration in Phase 8
                                },
                                onLogout = {
                                    sessionManager.clearSession()
                                    authViewModel.resetState()
                                    currentScreen = "login"
                                }
                            )
                        }
                        "driver_home" -> {
                            val driverVM: DriverDeliveryViewModel = hiltViewModel()
                            DriverDeliveryScreen(
                                viewModel = driverVM,
                                onLogout = {
                                    sessionManager.clearSession()
                                    authViewModel.resetState()
                                    currentScreen = "login"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
