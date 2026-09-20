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

    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    fun openAppSettings() {
        try {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("DriverDeliveryScreen", "Error opening app settings", e)
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = locationGranted

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[android.Manifest.permission.POST_NOTIFICATIONS] == true
        }

        if (locationGranted) {
            val intent = android.content.Intent(context, com.trackly.core.location.LocationService::class.java).apply {
                action = com.trackly.core.location.LocationService.ACTION_START
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    fun requestOrOpenSettings() {
        if (!hasLocationPermission || !hasNotificationPermission) {
            val perms = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
            // Also open App Settings so user can enable directly if OS suppresses dialog
            openAppSettings()
        }
    }

    val historyState by viewModel.historyState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchActiveDelivery()
        viewModel.fetchOrderHistory()

        if (hasLocationPermission) {
            val intent = android.content.Intent(context, com.trackly.core.location.LocationService::class.java).apply {
                action = com.trackly.core.location.LocationService.ACTION_START
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                android.util.Log.e("DriverDeliveryScreen", "Error starting LocationService", e)
            }
        } else {
            val perms = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    DriverDeliveryScreenContent(
        uiState = uiState,
        historyState = historyState,
        hasLocationPermission = hasLocationPermission,
        hasNotificationPermission = hasNotificationPermission,
        onRequestPermission = { requestOrOpenSettings() },
        onUpdateStatus = { orderId, status -> viewModel.updateStatus(orderId, status, "Driver updated state to ${status.name}") },
        onRefreshClick = { viewModel.fetchActiveDelivery() },
        onRefreshHistory = { viewModel.fetchOrderHistory() },
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
    historyState: DriverHistoryUiState = DriverHistoryUiState(),
    hasLocationPermission: Boolean = true,
    hasNotificationPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onUpdateStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onRefreshClick: () -> Unit,
    onRefreshHistory: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of Trackly?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondaryGrey)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (selectedTabIndex == 0) "Active Job" else "Delivery History", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showLogoutDialog = true }) {
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

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = DeepOceanSecondary,
                    contentColor = SurfaceWhite
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Active Job", fontWeight = FontWeight.Bold, color = SurfaceWhite) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            onRefreshHistory()
                        },
                        text = { Text("Delivery History", fontWeight = FontWeight.Bold, color = SurfaceWhite) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            if (selectedTabIndex == 1) {
                DriverHistoryScreen(
                    historyState = historyState,
                    onRefresh = onRefreshHistory
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                if (!hasLocationPermission || !hasNotificationPermission) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (!hasLocationPermission) Color(0xFFFFF3E0) else Color(0xFFE3F2FD)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (!hasLocationPermission) Color(0xFFE65100) else TealBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!hasLocationPermission) "Location Permission Required" else "Notification Permission Optional",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!hasLocationPermission) Color(0xFFE65100) else TealBluePrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (!hasLocationPermission)
                                        "Enable location in Settings so customers can track your live position."
                                    else
                                        "Enable notifications in Settings to see background status alerts.",
                                    color = TextSecondaryGrey,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = onRequestPermission) {
                                Text("Enable in Settings", fontWeight = FontWeight.Bold, color = TealBluePrimary)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
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
                            pickupLat = order.pickupLat ?: 17.3850,
                            pickupLng = order.pickupLng ?: 78.4867,
                            pickupName = order.pickupAddress,
                            deliveryLat = order.deliveryLat ?: 17.4401,
                            deliveryLng = order.deliveryLng ?: 78.3489,
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
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmeraldSuccess)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Delivery Completed Successfully!",
                                                color = StatusEmeraldSuccess,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = onRefreshClick,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Check for Next Delivery", fontWeight = FontWeight.Bold)
                                        }
                                    }
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

