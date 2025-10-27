package com.android.sample.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

interface AppPalette {
    val primary: Color
    val secondary: Color
    val accent: Color
    val text: Color
    val background: Color
    val surface: Color
    val error: Color

    val onPrimary: Color
    val onBackground: Color
    val onSurface: Color
}

object LightPalette : AppPalette {
    override val primary: Color = AppColors.PrimaryColor
    override val secondary: Color = AppColors.SecondaryColor
    override val accent: Color = AppColors.AccentColor
    override val text: Color = AppColors.BlackColor
    override val background: Color = AppColors.BackgroundColor
    override val surface: Color = AppColors.SurfaceColor
    override val error: Color = AppColors.ErrorColor

    override val onPrimary: Color = AppColors.BlackColor
    override val onBackground: Color = AppColors.BlackColor
    override val onSurface: Color = AppColors.BlackColor
}

object DarkPalette : AppPalette {
    override val primary: Color = AppColors.PrimaryDark
    override val secondary: Color = AppColors.SecondaryDark
    override val accent: Color = AppColors.AccentDark
    override val text: Color = AppColors.WhiteColor
    override val background: Color = AppColors.BackgroundDark
    override val surface: Color = AppColors.SurfaceDark
    override val error: Color = AppColors.ErrorDark

    override val onPrimary: Color = AppColors.WhiteColor
    override val onBackground: Color = AppColors.WhiteColor
    override val onSurface: Color = AppColors.WhiteColor
}

val LocalAppPalette = staticCompositionLocalOf<AppPalette> { LightPalette }

@Composable
fun appPalette(): AppPalette = LocalAppPalette.current
