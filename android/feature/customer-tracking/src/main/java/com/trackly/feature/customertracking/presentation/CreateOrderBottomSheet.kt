package com.trackly.feature.customertracking.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
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
import com.trackly.core.model.AddressSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderBottomSheet(
    isSubmitting: Boolean,
    pickupSuggestions: List<AddressSearchResult>,
    deliverySuggestions: List<AddressSearchResult>,
    onSearchPickup: (String) -> Unit,
    onSearchDelivery: (String) -> Unit,
    onConfirmOrder: (
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        deliveryAddress: String,
        deliveryLat: Double,
        deliveryLng: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var pickupText by remember { mutableStateOf("") }
    var pickupSelectedLat by remember { mutableStateOf(37.7749) }
    var pickupSelectedLng by remember { mutableStateOf(-122.4194) }

    var deliveryText by remember { mutableStateOf("") }
    var deliverySelectedLat by remember { mutableStateOf(37.7833) }
    var deliverySelectedLng by remember { mutableStateOf(-122.4167) }

    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDeliveryDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Place New Delivery Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryCharcoal
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGrey)
                }
            }

            Text(
                text = "Enter pickup and delivery locations with live address search.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryGrey
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Pickup Address Input
            Text(
                text = "PICKUP LOCATION",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TealBluePrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = pickupText,
                onValueChange = {
                    pickupText = it
                    showPickupDropdown = true
                    onSearchPickup(it)
                },
                placeholder = { Text("Search restaurant, store or pickup address...") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = DeepOceanSecondary) },
                trailingIcon = {
                    if (pickupText.isNotEmpty()) {
                        IconButton(onClick = { pickupText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryGrey)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealBluePrimary,
                    unfocusedBorderColor = DividerLight
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Pickup Autocomplete Suggestions
            if (showPickupDropdown && pickupSuggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundLight,
                    shadowElevation = 4.dp
                ) {
                    Column {
                        pickupSuggestions.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pickupText = item.displayName
                                        pickupSelectedLat = item.latitude
                                        pickupSelectedLng = item.longitude
                                        showPickupDropdown = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TealBluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.displayName,
                                    fontSize = 13.sp,
                                    color = TextPrimaryCharcoal,
                                    maxLines = 2
                                )
                            }
                            HorizontalDivider(color = DividerLight)
                        }
                    }
                }
            }

            // Pickup Quick Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        pickupText = "AB Fitness Gym, Jubilee Hills, Hyderabad"
                        pickupSelectedLat = 17.4326
                        pickupSelectedLng = 78.4071
                        showPickupDropdown = false
                    },
                    label = { Text("AB Fitness (Hyderabad)", fontSize = 11.sp, color = TextPrimaryCharcoal, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Store, contentDescription = null, tint = TealBluePrimary, modifier = Modifier.size(14.dp)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFFE0F2F1),
                        labelColor = TextPrimaryCharcoal
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = TealBluePrimary
                    )
                )
                SuggestionChip(
                    onClick = {
                        pickupText = "Central Bakery, Banjara Hills, Hyderabad"
                        pickupSelectedLat = 17.4156
                        pickupSelectedLng = 78.4487
                        showPickupDropdown = false
                    },
                    label = { Text("Central Bakery", fontSize = 11.sp, color = TextPrimaryCharcoal, fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFFE0F2F1),
                        labelColor = TextPrimaryCharcoal
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = TealBluePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Delivery Address Input
            Text(
                text = "DELIVERY DESTINATION",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = StatusEmeraldSuccess
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = deliveryText,
                onValueChange = {
                    deliveryText = it
                    showDeliveryDropdown = true
                    onSearchDelivery(it)
                },
                placeholder = { Text("Search customer house, apartment or office...") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = StatusEmeraldSuccess) },
                trailingIcon = {
                    if (deliveryText.isNotEmpty()) {
                        IconButton(onClick = { deliveryText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryGrey)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StatusEmeraldSuccess,
                    unfocusedBorderColor = DividerLight
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Delivery Autocomplete Suggestions
            if (showDeliveryDropdown && deliverySuggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundLight,
                    shadowElevation = 4.dp
                ) {
                    Column {
                        deliverySuggestions.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        deliveryText = item.displayName
                                        deliverySelectedLat = item.latitude
                                        deliverySelectedLng = item.longitude
                                        showDeliveryDropdown = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = StatusEmeraldSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.displayName,
                                    fontSize = 13.sp,
                                    color = TextPrimaryCharcoal,
                                    maxLines = 2
                                )
                            }
                            HorizontalDivider(color = DividerLight)
                        }
                    }
                }
            }

            // Delivery Quick Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        deliveryText = "HITECH City, Madhapur, Hyderabad"
                        deliverySelectedLat = 17.4435
                        deliverySelectedLng = 78.3772
                        showDeliveryDropdown = false
                    },
                    label = { Text("HITECH City (Hyderabad)", fontSize = 11.sp, color = TextPrimaryCharcoal, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = StatusEmeraldSuccess, modifier = Modifier.size(14.dp)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFFE8F5E9),
                        labelColor = TextPrimaryCharcoal
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = StatusEmeraldSuccess
                    )
                )
                SuggestionChip(
                    onClick = {
                        deliveryText = "Gachibowli Financial District, Hyderabad"
                        deliverySelectedLat = 17.4401
                        deliverySelectedLng = 78.3489
                        showDeliveryDropdown = false
                    },
                    label = { Text("Gachibowli", fontSize = 11.sp, color = TextPrimaryCharcoal, fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFFE8F5E9),
                        labelColor = TextPrimaryCharcoal
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = StatusEmeraldSuccess
                    )
                )
            }


            Spacer(modifier = Modifier.height(20.dp))

            // Summary Card Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED ARRIVAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepOceanSecondary)
                        Text(
                            text = if (pickupText.isNotBlank() && deliveryText.isNotBlank()) "~15 - 25 mins" else "Select addresses above",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryCharcoal
                        )
                    }
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = TealBluePrimary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val finalPickup = pickupText.ifBlank { "Central Bakery, 10th Ave" }
                    val finalDelivery = deliveryText.ifBlank { "Downtown Office, Suite 400" }
                    onConfirmOrder(
                        finalPickup,
                        pickupSelectedLat,
                        pickupSelectedLng,
                        finalDelivery,
                        deliverySelectedLat,
                        deliverySelectedLng
                    )
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealBluePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceWhite)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Dispatch Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
