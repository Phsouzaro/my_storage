package com.mystorage.ui.theme

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
import com.mystorage.domain.model.ExpiryStatus

private val Green = Color(0xFF2E7D32)
private val GreenLight = Color(0xFF81C784)

private val LightColors = lightColorScheme(primary = Green, secondary = Color(0xFF558B2F))
private val DarkColors = darkColorScheme(primary = GreenLight, secondary = Color(0xFFAED581))

@Composable
fun MyStorageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Cores fixas de status (não seguem o tema dinâmico, para manter o significado). */
fun ExpiryStatus.color(): Color = when (this) {
    ExpiryStatus.OK -> Color(0xFF43A047)
    ExpiryStatus.WARNING -> Color(0xFFFBC02D)
    ExpiryStatus.CRITICAL -> Color(0xFFFB8C00)
    ExpiryStatus.EXPIRED -> Color(0xFFE53935)
}
