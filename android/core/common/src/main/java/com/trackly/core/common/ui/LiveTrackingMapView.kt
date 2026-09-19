package com.trackly.core.common.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trackly.core.common.theme.*

@Composable
fun LiveTrackingMapView(
    pickupLat: Double,
    pickupLng: Double,
    pickupName: String,
    deliveryLat: Double,
    deliveryLng: Double,
    deliveryName: String,
    driverLat: Double? = null,
    driverLng: Double? = null,
    modifier: Modifier = Modifier
) {
    val pickupLatLng = remember(pickupLat, pickupLng) { LatLng(pickupLat, pickupLng) }
    val deliveryLatLng = remember(deliveryLat, deliveryLng) { LatLng(deliveryLat, deliveryLng) }

    val animatedDriverLat by animateFloatAsState(
        targetValue = (driverLat ?: pickupLat).toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "DriverLatAnim"
    )
    val animatedDriverLng by animateFloatAsState(
        targetValue = (driverLng ?: pickupLng).toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "DriverLngAnim"
    )

    val currentDriverLatLng = remember(animatedDriverLat, animatedDriverLng) {
        LatLng(animatedDriverLat.toDouble(), animatedDriverLng.toDouble())
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickupLatLng, 13f)
    }

    LaunchedEffect(currentDriverLatLng) {
        if (driverLat != null && driverLng != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentDriverLatLng, 14f)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                Marker(
                    state = MarkerState(position = pickupLatLng),
                    title = "Pickup: $pickupName",
                    snippet = "Restaurant"
                )

                Marker(
                    state = MarkerState(position = deliveryLatLng),
                    title = "Dropoff: $deliveryName",
                    snippet = "Customer Address"
                )

                if (driverLat != null && driverLng != null) {
                    Marker(
                        state = MarkerState(position = currentDriverLatLng),
                        title = "Driver Vehicle",
                        snippet = "Real-time Position"
                    )
                }

                Polyline(
                    points = listOf(pickupLatLng, deliveryLatLng),
                    color = TealBluePrimary,
                    width = 10f
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(DeepOceanSecondary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (driverLat != null) "LIVE GPS MAP" else "ROUTE MAP",
                    color = SurfaceWhite,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
