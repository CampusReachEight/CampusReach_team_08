package com.android.sample.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = AppColors.PrimaryColor,
        onPrimary = AppColors.BlackColor,
        primaryContainer = AppColors.PrimaryColor,
        onPrimaryContainer = AppColors.BlackColor,
        secondary = AppColors.SecondaryColor,
        onSecondary = AppColors.BlackColor,
        tertiary = AppColors.AccentColor,
        onTertiary = AppColors.WhiteColor,
        background = AppColors.WhiteColor,
        onBackground = AppColors.BlackColor,
        surface = AppColors.WhiteColor,
        onSurface = AppColors.BlackColor,
        error = AppColors.ErrorColor,
        onError = AppColors.WhiteColor)

private val DarkColorScheme =
    darkColorScheme(
        primary = AppColors.PrimaryDark,
        onPrimary = AppColors.WhiteColor,
        primaryContainer = AppColors.PrimaryDark,
        onPrimaryContainer = AppColors.WhiteColor,
        secondary = AppColors.SecondaryDark,
        onSecondary = AppColors.WhiteColor,
        tertiary = AppColors.AccentDark,
        onTertiary = AppColors.WhiteColor,
        background = AppColors.BackgroundDark,
        onBackground = AppColors.WhiteColor,
        surface = AppColors.SurfaceDark,
        onSurface = AppColors.WhiteColor,
        error = AppColors.ErrorDark,
        onError = AppColors.BlackColor)

/**
 * App theme wrapper.
 * - Chooses dynamic color schemes on Android 12+ when enabled.
 * - Falls back to static Light/Dark schemes otherwise.
 * - Provides a typed `AppPalette` (LightPalette/DarkPalette) via [LocalAppPalette].
 * - Updates the status bar color to match the color scheme primary color.
 */
@Composable
fun SampleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }

  // pick palette object (lightweight project palette)
  val palette = if (darkTheme) DarkPalette else LightPalette

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.primary.toArgb()
      // light theme -> dark status bar icons, dark theme -> light icons
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  CompositionLocalProvider(LocalAppPalette provides palette) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
