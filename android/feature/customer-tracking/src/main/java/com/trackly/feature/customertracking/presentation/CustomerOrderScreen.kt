package com.trackly.feature.customertracking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun CustomerOrderScreen(
    viewModel: CustomerTrackingViewModel,
    onOpenAiChat: (orderId: String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchActiveOrder()
        viewModel.fetchOrderHistory()
    }

    CustomerOrderScreenContent(
        uiState = uiState,
        historyState = historyState,
        onCreateOrderClick = { viewModel.createSampleOrder() },
        onRefreshClick = { viewModel.fetchActiveOrder() },
        onRefreshHistory = { viewModel.fetchOrderHistory() },
        onOpenAiChat = onOpenAiChat,
        onLogout = onLogout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrderScreenContent(
    uiState: CustomerOrderUiState,
    historyState: CustomerHistoryUiState,
    onCreateOrderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onRefreshHistory: () -> Unit,
    onOpenAiChat: (orderId: String) -> Unit,
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAiBottomSheet by remember { mutableStateOf(false) }

    if (showAiBottomSheet) {
        val activeOrder = (uiState as? CustomerOrderUiState.ActiveOrder)?.order
        val driverLat = (uiState as? CustomerOrderUiState.ActiveOrder)?.driverLat
        val driverLng = (uiState as? CustomerOrderUiState.ActiveOrder)?.driverLng
        com.trackly.feature.aiassistant.presentation.AiAssistantBottomSheet(
            order = activeOrder,
            driverLat = driverLat,
            driverLng = driverLng,
            onDismiss = { showAiBottomSheet = false }
        )
    }

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
                    title = { Text(if (selectedTabIndex == 0) "Active Delivery" else "Order History", fontWeight = FontWeight.Bold) },
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
                        containerColor = TealBluePrimary,
                        titleContentColor = SurfaceWhite
                    )
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = TealBluePrimary,
                    contentColor = SurfaceWhite
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Active Delivery", fontWeight = FontWeight.Bold, color = SurfaceWhite) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            onRefreshHistory()
                        },
                        text = { Text("Order History", fontWeight = FontWeight.Bold, color = SurfaceWhite) }
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
                CustomerHistoryScreen(
                    historyState = historyState,
                    onRefresh = onRefreshHistory
                )
            } else {
                when (uiState) {
                is CustomerOrderUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TealBluePrimary)
                    }
                }
                is CustomerOrderUiState.NoActiveOrder -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TealBluePrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Delivery",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryCharcoal
                        )
                        Text(
                            text = "You don't have any ongoing orders right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryGrey
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onCreateOrderClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealBluePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Place Sample Order", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is CustomerOrderUiState.ActiveOrder -> {
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
                                        containerColor = OceanTealHighlight,
                                        contentColor = SurfaceWhite
                                    ) {
                                        Text(order.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = DeepOceanSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = order.pickupAddress, style = MaterialTheme.typography.bodyMedium)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealBluePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = order.deliveryAddress, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Google Map View
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

                        // ETA & AI Button Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Estimated Arrival", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryGrey)
                                    Text(
                                        text = "${order.estimatedDurationMinutes ?: 25} mins",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealBluePrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        showAiBottomSheet = true
                                        onOpenAiChat(order.id)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepOceanSecondary)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SurfaceWhite)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Trackly AI", color = SurfaceWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Progression Timeline
                        Text(
                            text = "Delivery Status Timeline",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryCharcoal,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val milestones = listOf(
                                    OrderStatus.CREATED,
                                    OrderStatus.CONFIRMED,
                                    OrderStatus.PREPARING,
                                    OrderStatus.READY_FOR_PICKUP,
                                    OrderStatus.PICKED_UP,
                                    OrderStatus.OUT_FOR_DELIVERY,
                                    OrderStatus.DELIVERED
                                )

                                val currentOrdinal = order.status.ordinal

                                milestones.forEachIndexed { index, status ->
                                    val isCompleted = status.ordinal <= currentOrdinal
                                    val isCurrent = status == order.status

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrent) TealBluePrimary
                                                    else if (isCompleted) StatusEmeraldSuccess
                                                    else DividerLight
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCompleted) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = status.name.replace("_", " "),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCompleted) TextPrimaryCharcoal else TextSecondaryGrey
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is CustomerOrderUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = (uiState as CustomerOrderUiState.Error).message, color = StatusCrimsonError)
                    }
                }
                else -> {}
            }
        }
    }
}
}

@Preview(showBackground = true, name = "Customer Active Order Preview")
@Composable
fun CustomerOrderScreenPreview() {
    TracklyTheme {
        CustomerOrderScreenContent(
            uiState = CustomerOrderUiState.ActiveOrder(
                Order(
                    id = "order-123",
                    orderNumber = "ORD-849201",
                    customerId = "cust-1",
                    driverId = "driver-1",
                    status = OrderStatus.OUT_FOR_DELIVERY,
                    pickupAddress = "Bistro Restaurant, Market St",
                    pickupLat = 37.7749,
                    pickupLng = -122.4194,
                    deliveryAddress = "Customer Flat, 5th Ave",
                    deliveryLat = 37.7833,
                    deliveryLng = -122.4167,
                    estimatedDurationMinutes = 12,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ),
            historyState = CustomerHistoryUiState(),
            onCreateOrderClick = {},
            onRefreshClick = {},
            onRefreshHistory = {},
            onOpenAiChat = {}
        )
    }
}
