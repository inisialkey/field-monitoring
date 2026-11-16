package com.inisialkey.fieldmonitoring.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inisialkey.fieldmonitoring.R

private val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

private fun TextStyle.atkinson(weight: FontWeight, size: Int? = null, lineHeight: Int? = null) = copy(
    fontFamily = AtkinsonHyperlegible,
    fontWeight = weight,
    fontSize = size?.sp ?: fontSize,
    lineHeight = lineHeight?.sp ?: this.lineHeight,
)

private val Base = Typography()

val AppTypography = Typography(
    displayLarge = Base.displayLarge.atkinson(FontWeight.Normal),
    displayMedium = Base.displayMedium.atkinson(FontWeight.Normal),
    displaySmall = Base.displaySmall.atkinson(FontWeight.Normal),
    headlineLarge = Base.headlineLarge.atkinson(FontWeight.Bold),
    headlineMedium = Base.headlineMedium.atkinson(FontWeight.Bold),
    headlineSmall = Base.headlineSmall.atkinson(FontWeight.Bold, 24, 32),
    titleLarge = Base.titleLarge.atkinson(FontWeight.Bold, 22, 28),
    titleMedium = Base.titleMedium.atkinson(FontWeight.Bold, 16, 24),
    titleSmall = Base.titleSmall.atkinson(FontWeight.Bold, 14, 20),
    bodyLarge = Base.bodyLarge.atkinson(FontWeight.Normal, 16, 24),
    bodyMedium = Base.bodyMedium.atkinson(FontWeight.Normal, 14, 20),
    bodySmall = Base.bodySmall.atkinson(FontWeight.Normal, 12, 16),
    labelLarge = Base.labelLarge.atkinson(FontWeight.Bold, 14, 20),
    labelMedium = Base.labelMedium.atkinson(FontWeight.Bold, 12, 16),
    labelSmall = Base.labelSmall.atkinson(FontWeight.Bold, 12, 16),
)
