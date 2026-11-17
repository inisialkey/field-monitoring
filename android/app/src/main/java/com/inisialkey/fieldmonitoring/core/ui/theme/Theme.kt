package com.inisialkey.fieldmonitoring.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppColors(
    val isDark: Boolean,
    val heroStart: Color,
    val heroEnd: Color,
    val accent: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val LightAppColors = AppColors(
    isDark = false,
    heroStart = Color(0xFF016F33),
    heroEnd = Color(0xFF034721),
    accent = Color(0xFFB0C836),
    success = Color(0xFF016F33),
    successContainer = Color(0xFFD9F1E4),
    onSuccessContainer = Color(0xFF034721),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
)

private val DarkAppColors = AppColors(
    isDark = true,
    heroStart = Color(0xFF0D4D2A),
    heroEnd = Color(0xFF06301A),
    accent = Color(0xFFB0C836),
    success = Color(0xFF6CB745),
    successContainer = Color(0xFF034721),
    onSuccessContainer = Color(0xFFD9F1E4),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFEF3C7),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightColors = lightColorScheme(
    primary = Color(0xFF016F33),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9F1E4),
    onPrimaryContainer = Color(0xFF034721),
    inversePrimary = Color(0xFF6CB745),
    secondary = Color(0xFF5A6A13),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFF4D7),
    onSecondaryContainer = Color(0xFF3B4410),
    tertiary = Color(0xFF017180),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9EFF2),
    onTertiaryContainer = Color(0xFF004650),
    background = Color(0xFFF3F6F2),
    onBackground = Color(0xFF1E231F),
    surface = Color(0xFFF3F6F2),
    onSurface = Color(0xFF1E231F),
    surfaceVariant = Color(0xFFDCE2DA),
    onSurfaceVariant = Color(0xFF545B55),
    surfaceTint = Color(0xFF016F33),
    inverseSurface = Color(0xFF2F3031),
    inverseOnSurface = Color(0xFFF1F1F1),
    error = Color(0xFFC81E1E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF727973),
    outlineVariant = Color(0xFFD5DBD3),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF3F6F2),
    surfaceDim = Color(0xFFD8DDD6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF2EC),
    surfaceContainer = Color(0xFFE9EEE7),
    surfaceContainerHigh = Color(0xFFE3E8E1),
    surfaceContainerHighest = Color(0xFFDCE2DA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6CB745),
    onPrimary = Color(0xFF0B2E14),
    primaryContainer = Color(0xFF034721),
    onPrimaryContainer = Color(0xFFD9F1E4),
    inversePrimary = Color(0xFF016F33),
    secondary = Color(0xFFB0C836),
    onSecondary = Color(0xFF2A3000),
    secondaryContainer = Color(0xFF3B4410),
    onSecondaryContainer = Color(0xFFEFF4D7),
    tertiary = Color(0xFF5CC6D3),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004650),
    onTertiaryContainer = Color(0xFFD9EFF2),
    background = Color(0xFF0E1310),
    onBackground = Color(0xFFE6ECE7),
    surface = Color(0xFF0E1310),
    onSurface = Color(0xFFE6ECE7),
    surfaceVariant = Color(0xFF2D3630),
    onSurfaceVariant = Color(0xFFB3BCB5),
    surfaceTint = Color(0xFF6CB745),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF2F3031),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF87918A),
    outlineVariant = Color(0xFF2F3832),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A322C),
    surfaceDim = Color(0xFF0E1310),
    surfaceContainerLowest = Color(0xFF171D19),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1C231E),
    surfaceContainerHigh = Color(0xFF232B25),
    surfaceContainerHighest = Color(0xFF2D3630),
)

private val AppTypography = Typography().run { copy(labelSmall = labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp)) }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FieldMonitoringTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
