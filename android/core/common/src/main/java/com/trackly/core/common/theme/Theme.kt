package com.trackly.core.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TealBluePrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = OceanTealHighlight,
    onPrimaryContainer = SurfaceWhite,
    secondary = DeepOceanSecondary,
    onSecondary = SurfaceWhite,
    background = BackgroundLight,
    onBackground = TextPrimaryCharcoal,
    surface = SurfaceWhite,
    onSurface = TextPrimaryCharcoal,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryGrey,
    outline = DividerLight,
    error = StatusCrimsonError,
    onError = SurfaceWhite
)

@Composable
fun TracklyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = TracklyTypography,
        content = content
    )
}
