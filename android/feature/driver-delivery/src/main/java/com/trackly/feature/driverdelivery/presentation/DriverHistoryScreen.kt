package com.trackly.feature.driverdelivery.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackly.core.common.theme.*
import com.trackly.core.common.util.TimeUtils
import com.trackly.core.model.Order
import com.trackly.core.model.OrderStatus

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DriverHistoryScreen(
    historyState: DriverHistoryUiState,
    onRefresh: () -> Unit
) {
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        if (historyState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealBluePrimary)
            }
        } else if (historyState.orders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextSecondaryGrey,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Delivery History Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryCharcoal
                )
                Text(
                    text = "Your completed delivery jobs will be listed here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryGrey
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyState.orders) { order ->
                    DriverJobHistoryCard(order = order, dateFormat = dateTimeFormat)
                }
            }
        }
    }
}

@Composable
fun DriverJobHistoryCard(
    order: Order,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Order Number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryCharcoal
                )

                val (badgeBg, badgeText) = when (order.status) {
                    OrderStatus.DELIVERED -> StatusEmeraldSuccess to SurfaceWhite
                    OrderStatus.CANCELLED -> StatusCrimsonError to SurfaceWhite
                    OrderStatus.OUT_FOR_DELIVERY -> OceanTealHighlight to SurfaceWhite
                    else -> TealBluePrimary to SurfaceWhite
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = order.status.name.replace("_", " "),
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timestamps: Order Created & Delivery Completed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextSecondaryGrey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Created: ${dateFormat.format(Date(order.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryGrey
                )
            }

            if (order.status == OrderStatus.DELIVERED) {
                Spacer(modifier = Modifier.height(4.dp))
                val durationText = if (order.updatedAt > order.createdAt) {
                    TimeUtils.formatDurationInDaysHoursMins(order.createdAt, order.updatedAt)
                } else {
                    "${TimeUtils.formatMinutesToDaysHoursMins((order.estimatedDurationMinutes ?: 25).toLong())} estimated"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusEmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Completed: ${dateFormat.format(Date(order.updatedAt))} ($durationText)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusEmeraldSuccess
                    )
                }
            }


            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = DividerLight
            )

            // Address Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Store, contentDescription = null, tint = DeepOceanSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = order.pickupAddress, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryCharcoal)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealBluePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = order.deliveryAddress, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryCharcoal)
            }
        }
    }
}
