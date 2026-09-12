package com.shots.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ShotsTextStyles = object {
    val displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Bold)
    val displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Bold)
    val displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold)
    val headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    val headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    val titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    val titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
}

val ShotsTypography = Typography(
    displayLarge = ShotsTextStyles.displayLarge,
    displayMedium = ShotsTextStyles.displayMedium,
    displaySmall = ShotsTextStyles.displaySmall,
    headlineLarge = ShotsTextStyles.headlineLarge,
    headlineMedium = ShotsTextStyles.headlineMedium,
    headlineSmall = ShotsTextStyles.headlineSmall,
    titleLarge = ShotsTextStyles.titleLarge,
    titleMedium = ShotsTextStyles.titleMedium,
    titleSmall = ShotsTextStyles.titleSmall,
    bodyLarge = ShotsTextStyles.bodyLarge,
    bodyMedium = ShotsTextStyles.bodyMedium,
    bodySmall = ShotsTextStyles.bodySmall,
    labelLarge = ShotsTextStyles.labelLarge,
    labelMedium = ShotsTextStyles.labelMedium,
    labelSmall = ShotsTextStyles.labelSmall
)
