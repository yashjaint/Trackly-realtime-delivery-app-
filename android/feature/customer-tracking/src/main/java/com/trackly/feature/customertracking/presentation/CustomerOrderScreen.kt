package com.trackly.feature.customertracking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
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
    val pickupSuggestions by viewModel.pickupSuggestions.collectAsState()
    val deliverySuggestions by viewModel.deliverySuggestions.collectAsState()
    val isSubmittingOrder by viewModel.isSubmittingOrder.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchActiveOrder()
        viewModel.fetchOrderHistory()
    }

    CustomerOrderScreenContent(
        uiState = uiState,
        historyState = historyState,
        pickupSuggestions = pickupSuggestions,
        deliverySuggestions = deliverySuggestions,
        isSubmittingOrder = isSubmittingOrder,
        onSearchPickup = { viewModel.searchPickupAddress(it) },
        onSearchDelivery = { viewModel.searchDeliveryAddress(it) },
        onConfirmOrder = { pAddr, pLat, pLng, dAddr, dLat, dLng ->
            viewModel.createCustomOrder(pAddr, pLat, pLng, dAddr, dLat, dLng)
        },
        onSelectActiveOrder = { index -> viewModel.selectActiveOrder(index) },
        onSelectActiveOrderById = { orderId -> viewModel.selectActiveOrderById(orderId) },
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
    pickupSuggestions: List<com.trackly.core.model.AddressSearchResult> = emptyList(),
    deliverySuggestions: List<com.trackly.core.model.AddressSearchResult> = emptyList(),
    isSubmittingOrder: Boolean = false,
    onSearchPickup: (String) -> Unit = {},
    onSearchDelivery: (String) -> Unit = {},
    onConfirmOrder: (pAddr: String, pLat: Double, pLng: Double, dAddr: String, dLat: Double, dLng: Double) -> Unit = { _, _, _, _, _, _ -> },
    onSelectActiveOrder: (Int) -> Unit = {},
    onSelectActiveOrderById: (String) -> Unit = {},
    onRefreshClick: () -> Unit,
    onRefreshHistory: () -> Unit,
    onOpenAiChat: (orderId: String) -> Unit,
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAiBottomSheet by remember { mutableStateOf(false) }
    var showCreateOrderSheet by remember { mutableStateOf(false) }

    if (showCreateOrderSheet) {
        CreateOrderBottomSheet(
            isSubmitting = isSubmittingOrder,
            pickupSuggestions = pickupSuggestions,
            deliverySuggestions = deliverySuggestions,
            onSearchPickup = onSearchPickup,
            onSearchDelivery = onSearchDelivery,
            onConfirmOrder = { pAddr, pLat, pLng, dAddr, dLat, dLng ->
                showCreateOrderSheet = false
                onConfirmOrder(pAddr, pLat, pLng, dAddr, dLat, dLng)
            },
            onDismiss = { showCreateOrderSheet = false }
        )
    }


    if (showAiBottomSheet) {
        val selectedOrder = (uiState as? CustomerOrderUiState.ActiveOrders)?.selectedOrder
        val driverLat = (uiState as? CustomerOrderUiState.ActiveOrders)?.driverLat
        val driverLng = (uiState as? CustomerOrderUiState.ActiveOrders)?.driverLng
        com.trackly.feature.aiassistant.presentation.AiAssistantBottomSheet(
            order = selectedOrder,
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
                    title = { Text("Active Delivery", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showCreateOrderSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "New Order",
                                tint = SurfaceWhite
                            )
                        }
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
                    onRefresh = onRefreshHistory,
                    onOrderClick = { order ->
                        if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED) {
                            selectedTabIndex = 0
                            onSelectActiveOrderById(order.id)
                        }
                    }
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
                            text = "Create a custom order with live address search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryGrey
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCreateOrderSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealBluePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Place Delivery Order", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is CustomerOrderUiState.ActiveOrders -> {
                    val selectedOrder = uiState.selectedOrder
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Swipable Header Cards
                        ActiveOrderCardsPager(
                            orders = uiState.orders,
                            selectedIndex = uiState.selectedIndex,
                            onOrderSelected = onSelectActiveOrder
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Map View for Selected Order
                        com.trackly.core.common.ui.LiveTrackingMapView(
                            pickupLat = selectedOrder.pickupLat ?: 17.3850,
                            pickupLng = selectedOrder.pickupLng ?: 78.4867,
                            pickupName = selectedOrder.pickupAddress,
                            deliveryLat = selectedOrder.deliveryLat ?: 17.4401,
                            deliveryLng = selectedOrder.deliveryLng ?: 78.3489,
                            deliveryName = selectedOrder.deliveryAddress,
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
                                        text = "${selectedOrder.estimatedDurationMinutes ?: 25} mins",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealBluePrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        showAiBottomSheet = true
                                        onOpenAiChat(selectedOrder.id)
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

                                val currentOrdinal = selectedOrder.status.ordinal

                                milestones.forEachIndexed { index, status ->
                                    val isCompleted = status.ordinal <= currentOrdinal
                                    val isCurrent = status == selectedOrder.status

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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ActiveOrderCardsPager(
    orders: List<Order>,
    selectedIndex: Int,
    onOrderSelected: (Int) -> Unit
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, (orders.size - 1).coerceAtLeast(0)),
        pageCount = { orders.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onOrderSelected(pagerState.currentPage)
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != pagerState.currentPage && selectedIndex in orders.indices) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    Column {
        if (orders.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE DELIVERIES (${orders.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepOceanSecondary
                )
                Text(
                    text = "Order ${pagerState.currentPage + 1} of ${orders.size}  Swipe ➔",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealBluePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp
        ) { page ->
            val order = orders[page]
            OrderHeaderCard(
                order = order,
                isSelected = page == pagerState.currentPage
            )
        }

        if (orders.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(orders.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 20.dp else 6.dp)
                            .background(
                                color = if (isSelected) TealBluePrimary else DividerLight,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun OrderHeaderCard(
    order: Order,
    isSelected: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TealBluePrimary) else androidx.compose.foundation.BorderStroke(1.dp, DividerLight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
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
                    Text(
                        text = order.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = DeepOceanSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.pickupAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TealBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.deliveryAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Customer Active Order Preview")
@Composable
fun CustomerOrderScreenPreview() {
    TracklyTheme {
        CustomerOrderScreenContent(
            uiState = CustomerOrderUiState.ActiveOrders(
                orders = listOf(
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
                )
            ),
            historyState = CustomerHistoryUiState(),
            onRefreshClick = {},
            onRefreshHistory = {},
            onOpenAiChat = {}
        )
    }
}

