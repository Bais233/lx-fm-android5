package com.dragon.fm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily

@Composable
fun DragonFMTheme(
    preset: ThemePreset = ThemePreset.SODA_OBSIDIAN,
    customPrimaryColor: Color? = null,
    customFontFamily: FontFamily? = null,
    content: @Composable () -> Unit
) {
    val appliedPreset = preset.withPrimary(customPrimaryColor ?: preset.primary)
    val contentColor = if (appliedPreset.primary.luminance() > 0.4f) Color.Black else Color.White
    val colorScheme = if (appliedPreset.isDark) {
        darkColorScheme(
            primary = appliedPreset.primary,
            secondary = appliedPreset.secondary,
            tertiary = appliedPreset.accent,
            background = appliedPreset.background,
            surface = appliedPreset.surface,
            surfaceVariant = appliedPreset.surfaceVariant,
            onPrimary = contentColor,
            onSecondary = Color.Black,
            onBackground = appliedPreset.textPrimary,
            onSurface = appliedPreset.textPrimary
        )
    } else {
        lightColorScheme(
            primary = appliedPreset.primary,
            secondary = appliedPreset.secondary,
            tertiary = appliedPreset.accent,
            background = appliedPreset.background,
            surface = appliedPreset.surface,
            surfaceVariant = appliedPreset.surfaceVariant,
            onPrimary = contentColor,
            onSecondary = contentColor,
            onBackground = appliedPreset.textPrimary,
            onSurface = appliedPreset.textPrimary
        )
    }

    val baseTypography = Typography()
    val typography = if (customFontFamily == null) baseTypography else baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = customFontFamily),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = customFontFamily),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = customFontFamily),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = customFontFamily),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = customFontFamily),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = customFontFamily),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = customFontFamily),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = customFontFamily),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = customFontFamily),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = customFontFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = customFontFamily),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = customFontFamily),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = customFontFamily),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = customFontFamily),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = customFontFamily)
    )
    CompositionLocalProvider(
        LocalDragonTheme provides appliedPreset
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
