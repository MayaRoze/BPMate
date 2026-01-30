package com.example.bpmate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PinkSecondaryDark, // Swapped: Pink is now Primary
    onPrimary = PinkOnSecondaryDark,
    primaryContainer = PinkContainerDark,
    onPrimaryContainer = PinkOnContainerDark,
    
    secondary = CyanTertiaryDark, // Swapped: Cyan is now Secondary
    onSecondary = CyanOnTertiaryDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanOnContainerDark,
    
    tertiary = PeachPrimaryDark, // Swapped: Peach is now Tertiary
    onTertiary = PeachOnPrimaryDark,
    tertiaryContainer = PeachContainerDark,
    onTertiaryContainer = PeachOnContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = PinkSecondary, // Swapped: Pink is now Primary
    onPrimary = PinkOnSecondary,
    primaryContainer = PinkContainer,
    onPrimaryContainer = PinkOnContainer,
    
    secondary = CyanTertiary, // Swapped: Cyan is now Secondary
    onSecondary = CyanOnTertiary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanOnContainer,
    
    tertiary = PeachPrimary, // Swapped: Peach is now Tertiary
    onTertiary = PeachOnPrimary,
    tertiaryContainer = PeachContainer,
    onTertiaryContainer = PeachOnContainer
)

@Composable
fun BPMateTheme(
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
