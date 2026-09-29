package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. MIDNIGHT DARK PRESET (OLED Black Dark Canvas)
private val DarkColorScheme = darkColorScheme(
    primary = VibeSyncTeal,
    onPrimary = Color(0xFF111B21),
    primaryContainer = Color(0xFF005C4B),
    onPrimaryContainer = Color(0xFFD9FDD3),
    secondary = VibeSyncEmerald,
    onSecondary = Color(0xFF111B21),
    secondaryContainer = Color(0xFF1F3528),
    onSecondaryContainer = Color(0xFF8696A0),
    tertiary = VibeSyncBlueTick,
    onTertiary = PureWhite,
    background = VibeSyncDarkCanvas,
    onBackground = Color(0xFFE9EDEF),
    surface = VibeSyncDarkSurface,
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = VibeSyncDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF8696A0)
)

// 2. PURE LIGHT PRESET (Crisp High-Contrast Daytime Canvas)
private val LightColorScheme = lightColorScheme(
    primary = VibeSyncDarkTeal,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFD9FDD3),
    onPrimaryContainer = Color(0xFF075E54),
    secondary = VibeSyncTeal,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE8F8F5),
    onSecondaryContainer = Color(0xFF075E54),
    tertiary = VibeSyncBlueTick,
    onTertiary = PureWhite,
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF111B21),
    surface = PureWhite,
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFE9EDEF),
    onSurfaceVariant = Color(0xFF54656F)
)

// 3. SUNLIGHT GOLD PRESET (Outdoor Daylight High-Visibility Golden Canvas)
private val SunlightGoldColorScheme = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFFD97706),
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFFEF08A),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFFEA580C),
    onTertiary = PureWhite,
    background = Color(0xFFFFFDF5),
    onBackground = Color(0xFF1C1917),
    surface = PureWhite,
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFFEF9C3),
    onSurfaceVariant = Color(0xFF451A03)
)

// 4. AFTERNOON AZURE PRESET (Refreshing Sky-Blue Breeze Canvas)
private val AfternoonAzureColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0EA5E9),
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFBAE6FD),
    onSecondaryContainer = Color(0xFF0284C7),
    tertiary = Color(0xFF0284C7),
    onTertiary = PureWhite,
    background = Color(0xFFF0F9FF),
    onBackground = Color(0xFF0C4A6E),
    surface = PureWhite,
    onSurface = Color(0xFF0C4A6E),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF0369A1)
)

// 5. EVENING SUNSET PRESET (Warm Sunset Rose & Deep Twilight Canvas)
private val EveningSunsetColorScheme = darkColorScheme(
    primary = Color(0xFFF43F5E),
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF18020C),
    secondaryContainer = Color(0xFF4C0519),
    onSecondaryContainer = Color(0xFFFDA4AF),
    tertiary = Color(0xFFE11D48),
    onTertiary = PureWhite,
    background = Color(0xFF18020C),
    onBackground = Color(0xFFFFE4E6),
    surface = Color(0xFF270718),
    onSurface = Color(0xFFFFE4E6),
    surfaceVariant = Color(0xFF3F0E2A),
    onSurfaceVariant = Color(0xFFFDA4AF)
)

data class AppThemeOption(
    val id: String,
    val name: String,
    val tagLine: String,
    val description: String,
    val icon: String,
    val isDark: Boolean,
    val previewPrimary: Color,
    val previewBackground: Color,
    val previewSurface: Color
)

val AppThemeOptions = listOf(
    AppThemeOption(
        id = "PURE_LIGHT",
        name = "Pure Light",
        tagLine = "Crisp Daytime Light ☀️",
        description = "Clean white layout with high contrast for ultra-sharp readability.",
        icon = "☀️",
        isDark = false,
        previewPrimary = Color(0xFF075E54),
        previewBackground = Color(0xFFF0F2F5),
        previewSurface = Color.White
    ),
    AppThemeOption(
        id = "MIDNIGHT_DARK",
        name = "Midnight Dark",
        tagLine = "OLED Pitch-Black 🌙",
        description = "Deep OLED black canvas optimized for low-light night browsing.",
        icon = "🌙",
        isDark = true,
        previewPrimary = Color(0xFF00A884),
        previewBackground = Color(0xFF0B141A),
        previewSurface = Color(0xFF111B21)
    ),
    AppThemeOption(
        id = "SUNLIGHT_GOLD",
        name = "Sunlight High-Noon",
        tagLine = "Outdoor Daylight Gold 🌅",
        description = "High-visibility amber & golden scheme engineered for bright outdoor sunlight.",
        icon = "🌅",
        isDark = false,
        previewPrimary = Color(0xFFB45309),
        previewBackground = Color(0xFFFFFDF5),
        previewSurface = Color.White
    ),
    AppThemeOption(
        id = "AFTERNOON_AZURE",
        name = "Afternoon Azure",
        tagLine = "Sky-Blue Afternoon Breeze 🌤️",
        description = "Cool sky blue and fresh mint accents for relaxing afternoon reading.",
        icon = "🌤️",
        isDark = false,
        previewPrimary = Color(0xFF0284C7),
        previewBackground = Color(0xFFF0F9FF),
        previewSurface = Color.White
    ),
    AppThemeOption(
        id = "EVENING_SUNSET",
        name = "Evening Dusk Glow",
        tagLine = "Warm Twilight Sunset 🌆",
        description = "Cozy sunset rose and deep plum twilight canvas for calm evening chats.",
        icon = "🌆",
        isDark = true,
        previewPrimary = Color(0xFFF43F5E),
        previewBackground = Color(0xFF18020C),
        previewSurface = Color(0xFF270718)
    )
)

@Composable
fun MyApplicationTheme(
    presetId: String = "PURE_LIGHT",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (presetId) {
        "PURE_LIGHT" -> LightColorScheme
        "SUNLIGHT_GOLD" -> SunlightGoldColorScheme
        "AFTERNOON_AZURE" -> AfternoonAzureColorScheme
        "EVENING_SUNSET" -> EveningSunsetColorScheme
        "MIDNIGHT_DARK" -> DarkColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


