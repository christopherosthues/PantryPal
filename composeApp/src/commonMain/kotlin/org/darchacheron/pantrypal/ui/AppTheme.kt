package org.darchacheron.pantrypal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import org.darchacheron.pantrypal.settings.ThemeMode

@Immutable
data class PantryPalColorScheme(
    val overdueUseBy: Color,
    val overdueBestBefore: Color,
    val notOverdue: Color,
    val overdueUseByContainer: Color,
    val overdueBestBeforeContainer: Color,
    val notOverdueContainer: Color
)

val LocalPantryPalColorScheme = staticCompositionLocalOf {
    PantryPalColorScheme(
        overdueUseBy = Color.Unspecified,
        overdueBestBefore = Color.Unspecified,
        notOverdue = Color.Unspecified,
        overdueUseByContainer = Color.Unspecified,
        overdueBestBeforeContainer = Color.Unspecified,
        notOverdueContainer = Color.Unspecified
    )
}

object PantryPalTheme {
    val extraColors: PantryPalColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalPantryPalColorScheme.current
}

private val LightPantryPalColorScheme = PantryPalColorScheme(
    overdueUseBy = Color(0xFFD32F2F),
    overdueBestBefore = Color(0xFFF57C00),
    notOverdue = Color(0xFF7B1FA2),
    overdueUseByContainer = Color(0xFFFFEBEE),
    overdueBestBeforeContainer = Color(0xFFFFF3E0),
    notOverdueContainer = Color(0xFFF3E5F5)
)

private val DarkPantryPalColorScheme = PantryPalColorScheme(
    overdueUseBy = Color(0xFFEF9A9A),
    overdueBestBefore = Color(0xFFFFB74D),
    notOverdue = Color(0xFFCE93D8),
    overdueUseByContainer = Color(0xFF3B1F1F),
    overdueBestBeforeContainer = Color(0xFF3E2C1A),
    notOverdueContainer = Color(0xFF2D1D32)
)

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFFFFC107), // A vibrant, warm yellow (Amber 500)
        onPrimary = Color.Black, // Black text on yellow for good contrast
        primaryContainer = Color(0xFFFFD54F), // Lighter yellow for containers
        onPrimaryContainer = Color.Black,
        secondary = Color(0xFF4CAF50), // A complementary green
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFC8E6C9),
        onSecondaryContainer = Color.Black,
        tertiary = Color(0xFF2196F3), // A contrasting blue for accents
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFBBDEFB),
        onTertiaryContainer = Color.Black,
        error = Color(0xFFD32F2F), // Standard error red
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color.Black,
        background = Color(0xFFFFFFFF), // A very light, creamy yellow background
        onBackground = Color(0xFF3E2723), // Dark brown/black for text on light background
        surface = Color(0xFFFFFFFF), // White for card surfaces, dialogs etc.
        onSurface = Color(0xFF3E2723),
        surfaceVariant = Color(0xFFFBDB65), // Slightly off-white/pale yellow
        onSurfaceVariant = Color(0xFF3E2723),
        outline = Color(0xFFBCAAA4), // A muted brown/gray for outlines
        inverseOnSurface = Color(0xFFFFF8E1),
        inverseSurface = Color(0xFF3E2723),
        inversePrimary = Color(0xFFFFA000), // Darker yellow for inverse primary
        surfaceTint = Color(0xFFFFC107),
        outlineVariant = Color(0xFFD7CCC8),
        scrim = Color.Black
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFD54F), // Lighter, but still warm yellow for dark theme
        onPrimary = Color.Black,
        primaryContainer = Color(0xFFFFA000), // Deeper gold/amber for containers
        onPrimaryContainer = Color.Black, // Or a very dark gray
        secondary = Color(0xFF81C784), // Lighter, less saturated green
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF2E7D32),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFF64B5F6), // Lighter blue
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF1976D2),
        onTertiaryContainer = Color.White,
        error = Color(0xFFEF9A9A), // Softer red for errors
        onError = Color.Black,
        errorContainer = Color(0xFFD32F2F),
        onErrorContainer = Color.White,
        background = Color(0xFF212121), // Dark gray background (could go for a very dark brown too)
        onBackground = Color(0xFFFFF8E1), // Light creamy text
        surface = Color(0xFF333333), // Slightly lighter gray for surfaces
        onSurface = Color(0xFFFFF8E1),
        surfaceVariant = Color(0xFF424242), // Darker gray variant
        onSurfaceVariant = Color(0xFFFFF8E1),
        outline = Color(0xFF757575),
        inverseOnSurface = Color(0xFF212121),
        inverseSurface = Color(0xFFFFF8E1),
        inversePrimary = Color(0xFFFFC107),
        surfaceTint = Color(0xFFFFD54F),
        outlineVariant = Color(0xFF616161),
        scrim = Color.Black
    )

/**
 * Applies the selected theme mode.
 * System theme detection should be implemented per platform if needed.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extraColors = if (isDark) DarkPantryPalColorScheme else LightPantryPalColorScheme

    CompositionLocalProvider(
        LocalPantryPalColorScheme provides extraColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
