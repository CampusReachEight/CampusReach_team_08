package com.android.sample.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.android.sample.model.request.RequestType

interface AppPalette {
  val white: Color
  val black: Color
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

  fun getRequestTypeColor(type : RequestType): Color
}

object LightPalette : AppPalette {
  override val white: Color = AppColors.WhiteColor
  override val black: Color = AppColors.BlackColor
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
  override fun getRequestTypeColor(type: RequestType): Color {
    return when (type) {
        RequestType.STUDYING -> Color(0xFF4A90E2) // Bleu calme
        RequestType.STUDY_GROUP -> Color(0xFF5B9BD5) // Bleu moyen
        RequestType.HANGING_OUT -> Color(0xFFFF9800) // Orange chaleureux
        RequestType.EATING -> Color(0xFFE91E63) // Rose/Rouge
        RequestType.SPORT -> Color(0xFF4CAF50) // Vert actif
        RequestType.HARDWARE -> Color(0xFF9C27B0) // Violet
        RequestType.LOST_AND_FOUND -> Color(0xFFF44336) // Rouge attention
        RequestType.OTHER -> Color(0xFF757575) // Gris neutre
    }
  }
}

object DarkPalette : AppPalette {
  override val white: Color = AppColors.WhiteColor
  override val black: Color = AppColors.BlackColor
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

  override fun getRequestTypeColor(type: RequestType): Color {
    return when (type) {
        RequestType.STUDYING -> Color(0xFF64B5F6) // Bleu plus clair pour dark mode
        RequestType.STUDY_GROUP -> Color(0xFF81C4E8) // Bleu ciel
        RequestType.HANGING_OUT -> Color(0xFFFFB74D) // Orange plus doux
        RequestType.EATING -> Color(0xFFF48FB1) // Rose plus clair
        RequestType.SPORT -> Color(0xFF81C784) // Vert plus clair
        RequestType.HARDWARE -> Color(0xFFBA68C8) // Violet plus clair
        RequestType.LOST_AND_FOUND -> Color(0xFFEF5350) // Rouge plus doux
        RequestType.OTHER -> Color(0xFF9E9E9E) // Gris plus clair
    }
  }
}

val LocalAppPalette = staticCompositionLocalOf<AppPalette> { LightPalette }

@Composable fun appPalette(): AppPalette = LocalAppPalette.current
