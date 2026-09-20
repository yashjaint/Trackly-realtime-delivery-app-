package com.trackly.core.common.ui

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
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
    var useStreetTileMap by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (useStreetTileMap) {
                OpenStreetMapTileView(
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    pickupName = pickupName,
                    deliveryLat = deliveryLat,
                    deliveryLng = deliveryLng,
                    deliveryName = deliveryName,
                    driverLat = driverLat,
                    driverLng = driverLng
                )
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

            // Header Pills Overlay
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
                        text = if (driverLat != null) "LIVE STREETS GPS" else "REAL STREET MAP",
                        color = SurfaceWhite,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    onClick = { useStreetTileMap = !useStreetTileMap },
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
                            text = if (useStreetTileMap) "Vector View" else "Street View",
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
fun OpenStreetMapTileView(
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
    val pLat = if (pickupLat == 0.0 || pickupLat == 37.7749) 17.3850 else pickupLat
    val pLng = if (pickupLng == 0.0 || pickupLng == -122.4194) 78.4867 else pickupLng
    val dLat = if (deliveryLat == 0.0 || deliveryLat == 37.7833) 17.4401 else deliveryLat
    val dLng = if (deliveryLng == 0.0 || deliveryLng == -122.4167) 78.3489 else deliveryLng

    val jsonPickup = org.json.JSONObject.quote(pickupName.ifBlank { "Pickup Point" })
    val jsonDelivery = org.json.JSONObject.quote(deliveryName.ifBlank { "Delivery Point" })

    val driverPointJs = if (driverLat != null && driverLng != null && driverLat != 0.0) {
        """
        var drvLat = $driverLat, drvLng = $driverLng;
        var driverIcon = L.divIcon({
            className: 'driver-marker',
            html: '<span style="font-size:18px;">🚗</span>',
            iconSize: [36, 36],
            iconAnchor: [18, 18]
        });
        L.marker([drvLat, drvLng], {icon: driverIcon}).addTo(map)
            .bindPopup("<b>Driver Live Location</b>");
        points.push([drvLat, drvLng]);
        """.trimIndent()
    } else ""

    val htmlContent = remember(pLat, pLng, dLat, dLng, driverLat, driverLng, jsonPickup, jsonDelivery) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
            <style>
                html, body { width: 100%; height: 100%; margin: 0; padding: 0; background: #eef2f5; overflow: hidden; }
                #map { width: 100%; height: 100%; background: #eef2f5; }
                .pickup-marker {
                    background: #0B2545;
                    color: white;
                    border-radius: 50%;
                    width: 32px;
                    height: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border: 2.5px solid white;
                    box-shadow: 0 3px 8px rgba(0,0,0,0.4);
                }
                .delivery-marker {
                    background: #E63946;
                    color: white;
                    border-radius: 50%;
                    width: 32px;
                    height: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border: 2.5px solid white;
                    box-shadow: 0 3px 8px rgba(0,0,0,0.4);
                }
                .driver-marker {
                    background: #1D9CC1;
                    color: white;
                    border-radius: 50%;
                    width: 36px;
                    height: 36px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border: 3px solid white;
                    box-shadow: 0 0 14px rgba(29, 156, 193, 0.9);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                function initMap() {
                    if (typeof L === 'undefined') {
                        setTimeout(initMap, 200);
                        return;
                    }
                    try {
                        var map = L.map('map', { zoomControl: false, attributionControl: false });
                        
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19
                        }).addTo(map);

                        var pLat = $pLat, pLng = $pLng;
                        var dLat = $dLat, dLng = $dLng;

                        var pickupIcon = L.divIcon({
                            className: 'pickup-marker',
                            html: '<span style="font-size:16px;">🏪</span>',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16]
                        });

                        var deliveryIcon = L.divIcon({
                            className: 'delivery-marker',
                            html: '<span style="font-size:16px;">📍</span>',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16]
                        });

                        L.marker([pLat, pLng], {icon: pickupIcon}).addTo(map)
                            .bindPopup("<b>Pickup:</b> " + $jsonPickup);

                        L.marker([dLat, dLng], {icon: deliveryIcon}).addTo(map)
                            .bindPopup("<b>Delivery:</b> " + $jsonDelivery);

                        var points = [[pLat, pLng]];
                        $driverPointJs
                        points.push([dLat, dLng]);

                        L.polyline(points, {
                            color: '#1D9CC1',
                            weight: 5,
                            dashArray: '8, 8',
                            lineCap: 'round'
                        }).addTo(map);

                        var bounds = L.latLngBounds(points);
                        map.fitBounds(bounds, { padding: [30, 30] });
                    } catch (err) {
                        console.error("Leaflet init error: " + err);
                    }
                }
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', initMap);
                } else {
                    initMap();
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(htmlContent) {
        android.util.Log.d("TracklyMapView", "================================================")
        android.util.Log.d("TracklyMapView", "📍 Map Coordinates Loaded:")
        android.util.Log.d("TracklyMapView", "  Pickup: ($pLat, $pLng) - $pickupName")
        android.util.Log.d("TracklyMapView", "  Delivery: ($dLat, $dLng) - $deliveryName")
        if (driverLat != null && driverLng != null) {
            android.util.Log.d("TracklyMapView", "  Driver: ($driverLat, $driverLng)")
        } else {
            android.util.Log.d("TracklyMapView", "  Driver: None")
        }
        android.util.Log.d("TracklyMapView", "================================================")
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("TracklyMapView", "🌐 JS Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        android.util.Log.e("TracklyMapView", "❌ WebView Error: $description (code=$errorCode, url=$failingUrl)")
                    }
                }
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
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

