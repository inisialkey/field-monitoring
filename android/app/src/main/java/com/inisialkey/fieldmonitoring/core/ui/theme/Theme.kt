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
data class StatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val LightStatusColors = StatusColors(
    success = Color(0xFF016F33),
    successContainer = Color(0xFFD9F1E4),
    onSuccessContainer = Color(0xFF034721),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
)

private val DarkStatusColors = StatusColors(
    success = Color(0xFF6CB745),
    successContainer = Color(0xFF034721),
    onSuccessContainer = Color(0xFFD9F1E4),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFEF3C7),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

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
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF272727),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF272727),
    surfaceVariant = Color(0xFFE2E2E3),
    onSurfaceVariant = Color(0xFF5E5E60),
    surfaceTint = Color(0xFF016F33),
    inverseSurface = Color(0xFF2F3031),
    inverseOnSurface = Color(0xFFF1F1F1),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF767676),
    outlineVariant = Color(0xFFE2E2E2),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF8F9FA),
    surfaceDim = Color(0xFFDEDFE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F4),
    surfaceContainer = Color(0xFFEDEEEF),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE2E2E3),
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
    background = Color(0xFF111111),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB4B4B4),
    surfaceTint = Color(0xFF6CB745),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF2F3031),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF333333),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2E2E2E),
    surfaceDim = Color(0xFF111111),
    surfaceContainerLowest = Color(0xFF1C1C1C),
    surfaceContainerLow = Color(0xFF1C1C1C),
    surfaceContainer = Color(0xFF222222),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353535),
)

private val AppTypography = Typography().run { copy(labelSmall = labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp)) }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FieldMonitoringTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
