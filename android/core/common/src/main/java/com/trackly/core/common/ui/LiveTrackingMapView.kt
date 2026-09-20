package com.trackly.core.common.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
    var useGoogleMaps by remember { mutableStateOf(false) }

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
            .height(270.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (useGoogleMaps) {
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
            } else {
                VectorRouteMapView(
                    pickupName = pickupName,
                    deliveryName = deliveryName,
                    driverLat = driverLat,
                    driverLng = driverLng,
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    deliveryLat = deliveryLat,
                    deliveryLng = deliveryLng
                )
            }

            // Header Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
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

                Surface(
                    onClick = { useGoogleMaps = !useGoogleMaps },
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceWhite.copy(alpha = 0.95f),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Toggle Map Mode",
                            tint = TealBluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (useGoogleMaps) "Vector Map" else "Google Map",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimaryCharcoal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VectorRouteMapView(
    pickupName: String,
    deliveryName: String,
    pickupLat: Double,
    pickupLng: Double,
    deliveryLat: Double,
    deliveryLng: Double,
    driverLat: Double?,
    driverLng: Double?
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF4F1))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Canvas Map background & route line
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw subtle city grid lines
            val gridStep = 40.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color(0xFFD6E3DD),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += gridStep
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0xFFD6E3DD),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += gridStep
            }

            // Route points (Pickup leftish, Dropoff rightish)
            val pX = size.width * 0.22f
            val pY = size.height * 0.72f
            val dX = size.width * 0.78f
            val dY = size.height * 0.28f

            // Mid curve point for realistic road feel
            val cX = size.width * 0.40f
            val cY = size.height * 0.35f

            val path = Path().apply {
                moveTo(pX, pY)
                quadraticTo(cX, cY, dX, dY)
            }

            // Draw route shadow
            drawPath(
                path = path,
                color = Color(0x331D9CC1),
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw primary route polyline
            drawPath(
                path = path,
                color = TealBluePrimary,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                )
            )
        }

        // Relative positions for overlay items
        val pX = widthPx * 0.22f
        val pY = heightPx * 0.72f
        val dX = widthPx * 0.78f
        val dY = heightPx * 0.28f

        // Driver interpolation along curve
        val driverProgress = remember(driverLat, driverLng) {
            if (driverLat == null || driverLng == null) 0.45f
            else {
                val totalDist = Math.hypot(deliveryLat - pickupLat, deliveryLng - pickupLng)
                val currentDist = Math.hypot(driverLat - pickupLat, driverLng - pickupLng)
                if (totalDist > 0) (currentDist / totalDist).toFloat().coerceIn(0.1f, 0.9f) else 0.45f
            }
        }

        val cX = widthPx * 0.40f
        val cY = heightPx * 0.35f

        // Quadratic Bezier point at driverProgress t
        val t = driverProgress
        val drvX = (1 - t) * (1 - t) * pX + 2 * (1 - t) * t * cX + t * t * dX
        val drvY = (1 - t) * (1 - t) * pY + 2 * (1 - t) * t * cY + t * t * dY

        // 1. Pickup Badge Marker
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { (pX - 40.dp.toPx()).toDp() },
                    y = with(density) { (pY - 55.dp.toPx()).toDp() }
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceWhite,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = DeepOceanSecondary, modifier = Modifier.size(14.dp))
                        Text(pickupName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimaryCharcoal)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(DeepOceanSecondary, CircleShape)
                        .border(2.dp, SurfaceWhite, CircleShape)
                )
            }
        }

        // 2. Dropoff Badge Marker
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { (dX - 40.dp.toPx()).toDp() },
                    y = with(density) { (dY - 55.dp.toPx()).toDp() }
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceWhite,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                        Text(deliveryName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimaryCharcoal)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFFD32F2F), CircleShape)
                        .border(2.dp, SurfaceWhite, CircleShape)
                )
            }
        }

        // 3. Driver Vehicle Marker (with pulse)
        if (driverLat != null || driverProgress > 0f) {
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (drvX - 20.dp.toPx()).toDp() },
                        y = with(density) { (drvY - 20.dp.toPx()).toDp() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Pulse Ring
                Box(
                    modifier = Modifier
                        .size((36 * pulseScale).dp)
                        .background(TealBluePrimary.copy(alpha = pulseAlpha), CircleShape)
                )

                // Vehicle Circle
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = TealBluePrimary,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, SurfaceWhite)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Driver Vehicle",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

