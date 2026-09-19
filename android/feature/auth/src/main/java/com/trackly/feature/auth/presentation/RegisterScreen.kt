package com.trackly.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackly.core.common.theme.*
import com.trackly.core.model.UserRole

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (role: UserRole) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                val user = (uiState as AuthUiState.Success).user
                onRegisterSuccess(user.role)
            }
            is AuthUiState.Error -> {
                val msg = (uiState as AuthUiState.Error).message
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    RegisterScreenContent(
        uiState = uiState,
        onRegisterClick = { name, email, password, role, vehicleNumber ->
            viewModel.register(name, email, password, role, vehicleNumber)
        },
        onNavigateToLogin = onNavigateToLogin,
        onResetError = { viewModel.resetError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreenContent(
    uiState: AuthUiState,
    onRegisterClick: (name: String, email: String, password: String, role: UserRole, vehicleNumber: String?) -> Unit,
    onNavigateToLogin: () -> Unit,
    onResetError: () -> Unit,
    initialRole: UserRole = UserRole.CUSTOMER
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var vehicleNumber by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimaryCharcoal,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Join Trackly as a Customer or Driver",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryGrey
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(SurfaceBorderLight, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { selectedRole = UserRole.CUSTOMER },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == UserRole.CUSTOMER) TealBluePrimary else Color.Transparent,
                        contentColor = if (selectedRole == UserRole.CUSTOMER) SurfaceWhite else TextSecondaryGrey
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Customer", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedRole = UserRole.DRIVER },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == UserRole.DRIVER) DeepOceanSecondary else Color.Transparent,
                        contentColor = if (selectedRole == UserRole.DRIVER) SurfaceWhite else TextSecondaryGrey
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Driver", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Banner
            AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                if (uiState is AuthUiState.Error) {
                    val errorMessage = (uiState as AuthUiState.Error).message
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = errorMessage,
                                color = StatusCrimsonError,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (errorMessage.contains("already registered", ignoreCase = true) || errorMessage.contains("already exists", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = onNavigateToLogin,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "Log In with existing account →",
                                        color = TealBluePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onResetError()
                },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealBluePrimary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealBluePrimary,
                    unfocusedBorderColor = DividerLight
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                label = { Text("Password (6+ characters)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealBluePrimary) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (selectedRole == UserRole.DRIVER) ImeAction.Next else ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealBluePrimary,
                    unfocusedBorderColor = DividerLight
                )
            )

            if (selectedRole == UserRole.DRIVER) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = {
                        vehicleNumber = it
                        onResetError()
                    },
                    label = { Text("Vehicle Plate / License Number") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DeepOceanSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepOceanSecondary,
                        unfocusedBorderColor = DividerLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { onRegisterClick(name, email, password, selectedRole, vehicleNumber) },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedRole == UserRole.DRIVER) DeepOceanSecondary else TealBluePrimary)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (selectedRole == UserRole.DRIVER) "Register as Driver" else "Register as Customer",
                        color = SurfaceWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Already have an account? ", color = TextSecondaryGrey)
                TextButton(onClick = onNavigateToLogin) {
                    Text(text = "Log In", color = TealBluePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Register Customer Preview")
@Composable
fun RegisterScreenCustomerPreview() {
    TracklyTheme {
        RegisterScreenContent(
            uiState = AuthUiState.Idle,
            onRegisterClick = { _, _, _, _, _ -> },
            onNavigateToLogin = {},
            onResetError = {},
            initialRole = UserRole.CUSTOMER
        )
    }
}

@Preview(showBackground = true, name = "Register Driver Preview")
@Composable
fun RegisterScreenDriverPreview() {
    TracklyTheme {
        RegisterScreenContent(
            uiState = AuthUiState.Idle,
            onRegisterClick = { _, _, _, _, _ -> },
            onNavigateToLogin = {},
            onResetError = {},
            initialRole = UserRole.DRIVER
        )
    }
}
