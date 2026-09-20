package com.trackly.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackly.core.common.R
import com.trackly.core.common.theme.*
import com.trackly.core.model.UserRole

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (role: UserRole) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            val user = (uiState as AuthUiState.Success).user
            onLoginSuccess(user.role)
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onLoginClick = { email, password -> viewModel.login(email, password) },
        onNavigateToRegister = onNavigateToRegister,
        onResetError = { viewModel.resetError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    onLoginClick: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onResetError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo & Title Header
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "Trackly Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Trackly",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimaryCharcoal,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Real-Time Intelligent Delivery Tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryGrey
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error Banner
            AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                if (uiState is AuthUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = StatusCrimsonError,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onResetError()
                },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TealBluePrimary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealBluePrimary,
                    unfocusedBorderColor = DividerLight
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onResetError()
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealBluePrimary) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealBluePrimary,
                    unfocusedBorderColor = DividerLight
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = { onLoginClick(email, password) },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealBluePrimary)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Log In",
                        color = SurfaceWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Don't have an account? ", color = TextSecondaryGrey)
                TextButton(onClick = onNavigateToRegister) {
                    Text(text = "Sign Up", color = TealBluePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun LoginScreenPreview() {
    TracklyTheme {
        LoginScreenContent(
            uiState = AuthUiState.Idle,
            onLoginClick = { _, _ -> },
            onNavigateToRegister = {},
            onResetError = {}
        )
    }
}
