package com.trackly.feature.driverdelivery.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackly.core.common.theme.*
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus

import androidx.compose.material.icons.automirrored.filled.ExitToApp

@Composable
fun DriverDeliveryScreen(
    viewModel: DriverDeliveryViewModel,
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            val intent = android.content.Intent(context, com.trackly.core.location.LocationService::class.java).apply {
                action = com.trackly.core.location.LocationService.ACTION_START
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchActiveDelivery()

        val perms = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())

        val intent = android.content.Intent(context, com.trackly.core.location.LocationService::class.java).apply {
            action = com.trackly.core.location.LocationService.ACTION_START
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            android.util.Log.e("DriverDeliveryScreen", "Error starting LocationService", e)
        }
    }

    DriverDeliveryScreenContent(
        uiState = uiState,
        onUpdateStatus = { orderId, status -> viewModel.updateStatus(orderId, status, "Driver updated state to ${status.name}") },
        onRefreshClick = { viewModel.fetchActiveDelivery() },
        onLogout = {
            val stopIntent = android.content.Intent(context, com.trackly.core.location.LocationService::class.java).apply {
                action = com.trackly.core.location.LocationService.ACTION_STOP
            }
            context.startService(stopIntent)
            onLogout()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDeliveryScreenContent(
    uiState: DriverDeliveryUiState,
    onUpdateStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onRefreshClick: () -> Unit,
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Delivery Portal", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = SurfaceWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepOceanSecondary,
                    titleContentColor = SurfaceWhite
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            when (uiState) {
                is DriverDeliveryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DeepOceanSecondary)
                    }
                }
                is DriverDeliveryUiState.NoActiveDelivery -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = DeepOceanSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Delivery Assigned",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryCharcoal
                        )
                        Text(
                            text = "You are currently online and available for new deliveries.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryGrey
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onRefreshClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary)
                        ) {
                            Text("Check for Deliveries", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is DriverDeliveryUiState.ActiveDelivery -> {
                    val order = uiState.order
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Order Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = order.orderNumber,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryCharcoal
                                    )
                                    Badge(
                                        containerColor = DeepOceanSecondary,
                                        contentColor = SurfaceWhite
                                    ) {
                                        Text(order.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = DeepOceanSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Pickup: ${order.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = StatusEmeraldSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Dropoff: ${order.deliveryAddress}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Map View for Driver
                        com.trackly.core.common.ui.LiveTrackingMapView(
                            pickupLat = order.pickupLat ?: 37.7749,
                            pickupLng = order.pickupLng ?: -122.4194,
                            pickupName = order.pickupAddress,
                            deliveryLat = order.deliveryLat ?: 37.7833,
                            deliveryLng = order.deliveryLng ?: -122.4167,
                            deliveryName = order.deliveryAddress,
                            driverLat = uiState.driverLat,
                            driverLng = uiState.driverLng
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Error Banner if update failed
                        if (uiState.errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "Error: ${uiState.errorMessage}",
                                    color = StatusCrimsonError,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // State Transition Driver Action Buttons
                        Text(
                            text = "Driver Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryCharcoal
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val isUpdating = uiState.isUpdating

                        when (order.status) {
                            OrderStatus.CREATED -> {
                                Button(
                                    onClick = { onUpdateStatus(order.id, OrderStatus.CONFIRMED) },
                                    enabled = !isUpdating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanTealHighlight)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirm Order", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            OrderStatus.CONFIRMED -> {
                                Button(
                                    onClick = { onUpdateStatus(order.id, OrderStatus.PREPARING) },
                                    enabled = !isUpdating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanTealHighlight)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                                    } else {
                                        Icon(Icons.Default.Store, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Preparing", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            OrderStatus.PREPARING -> {
                                Button(
                                    onClick = { onUpdateStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                                    enabled = !isUpdating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanTealHighlight)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Mark Ready for Pickup", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            OrderStatus.READY_FOR_PICKUP -> {
                                Button(
                                    onClick = { onUpdateStatus(order.id, OrderStatus.PICKED_UP) },
                                    enabled = !isUpdating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealBluePrimary)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                                    } else {
                                        Icon(Icons.Default.Store, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirm Package Pickup", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            OrderStatus.PICKED_UP -> {
                                Button(
                                    onClick = { onUpdateStatus(order.id, OrderStatus.OUT_FOR_DELIVERY) },
                                    enabled = !isUpdating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                                    } else {
                                        Icon(Icons.Default.Navigation, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Route (Out for Delivery)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            OrderStatus.OUT_FOR_DELIVERY -> {
                                Button(
                                    onClick = { onUpdateStatus(order.id, OrderStatus.DELIVERED) },
                                    enabled = !isUpdating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusEmeraldSuccess)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Complete Delivery", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            OrderStatus.DELIVERED -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Delivery Completed Successfully!",
                                        color = StatusEmeraldSuccess,
                                        modifier = Modifier.padding(16.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
                is DriverDeliveryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = (uiState as DriverDeliveryUiState.Error).message,
                            color = StatusCrimsonError,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefreshClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary)
                        ) {
                            Text("Retry Loading Delivery", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Preview(showBackground = true, name = "Driver Active Delivery Preview")
@Composable
fun DriverDeliveryScreenPreview() {
    TracklyTheme {
        DriverDeliveryScreenContent(
            uiState = DriverDeliveryUiState.ActiveDelivery(
                Order(
                    id = "order-456",
                    orderNumber = "ORD-991238",
                    customerId = "cust-2",
                    driverId = "driver-1",
                    status = OrderStatus.OUT_FOR_DELIVERY,
                    pickupAddress = "Central Bakery, 10th Ave",
                    pickupLat = 37.7749,
                    pickupLng = -122.4194,
                    deliveryAddress = "Downtown Office, Suite 400",
                    deliveryLat = 37.7833,
                    deliveryLng = -122.4167,
                    estimatedDurationMinutes = 15,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ),
            onUpdateStatus = { _, _ -> },
            onRefreshClick = {}
        )
    }
}
