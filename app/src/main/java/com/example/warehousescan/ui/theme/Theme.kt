package com.example.warehousescan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC7F2),
    onPrimary = Color(0xFF001A3D),
    secondary = Color(0xFFC1CAE0),
    tertiary = WarehouseSuccess,
    error = WarehouseError,
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF424750)
)

private val LightColorScheme = lightColorScheme(
    primary = WarehousePrimary,
    onPrimary = Color.White,
    secondary = WarehousePrimaryDark,
    tertiary = WarehouseSuccess,
    error = WarehouseError,
    surface = WarehouseSurface,
    surfaceVariant = Color(0xFFE5E7EB),
    background = WarehouseBackground,
    onBackground = WarehouseTextPrimary,
    onSurface = WarehouseTextPrimary,
    onSurfaceVariant = WarehouseTextSecondary
)

@Composable
fun WarehousescanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
