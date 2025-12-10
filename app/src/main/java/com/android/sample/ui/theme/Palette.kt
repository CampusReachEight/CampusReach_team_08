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
  val errorContainer: Color
  val onErrorContainer: Color

  val onPrimary: Color
  val onBackground: Color
  val onSurface: Color
  val onAccent: Color

  fun getRequestTypeColor(type: RequestType): Color

  fun getRequestTypeBackgroundColor(type: RequestType): Color
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
  override val errorContainer: Color = AppColors.ErrorContainer
  override val onErrorContainer: Color = AppColors.OnErrorContainer

  override val onPrimary: Color = AppColors.BlackColor
  override val onBackground: Color = AppColors.BlackColor
  override val onSurface: Color = AppColors.BlackColor
  override val onAccent: Color = AppColors.WhiteColor

  override fun getRequestTypeColor(type: RequestType): Color {
    return when (type) {
      RequestType.STUDYING -> Color(0xFF1247F8) // Bleu accent principal (matching AccentColor)
      RequestType.STUDY_GROUP -> Color(0xFF4A7FE8) // Bleu plus doux
      RequestType.HANGING_OUT -> Color(0xFFFF8C42) // Orange harmonieux
      RequestType.EATING -> Color(0xFFE63946) // Rouge/Rose plus doux
      RequestType.SPORT -> Color(0xFF2ECC71) // Vert vif mais pas trop
      RequestType.HARDWARE -> Color(0xFF9B59B6) // Violet équilibré
      RequestType.LOST_AND_FOUND -> Color(0xFFE74C3C) // Rouge attention mais doux
      RequestType.OTHER -> Color(0xFF6C757D) // Gris neutre
    }
  }

  override fun getRequestTypeBackgroundColor(type: RequestType): Color {
    return when (type) {
      RequestType.STUDYING -> Color(0xFFE8EFFE) // Bleu très clair (proche de PrimaryColor)
      RequestType.STUDY_GROUP -> Color(0xFFD8E4FF) // Bleu clair (matching SecondaryColor)
      RequestType.HANGING_OUT -> Color(0xFFFFECE0) // Orange très clair
      RequestType.EATING -> Color(0xFFFFE5E8) // Rose très clair
      RequestType.SPORT -> Color(0xFFE8F8F0) // Vert très clair
      RequestType.HARDWARE -> Color(0xFFF3EAF8) // Violet très clair
      RequestType.LOST_AND_FOUND -> Color(0xFFFFE9E8) // Rouge très clair
      RequestType.OTHER -> Color(0xFFF5F6F7) // Gris très clair
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
  override val errorContainer: Color = AppColors.ErrorContainerDark
  override val onErrorContainer: Color = AppColors.ErrorOnContainerDark

  override val onPrimary: Color = AppColors.WhiteColor
  override val onBackground: Color = AppColors.WhiteColor
  override val onSurface: Color = AppColors.WhiteColor
  override val onAccent: Color = AppColors.WhiteColor

  override fun getRequestTypeColor(type: RequestType): Color {
    return when (type) {
      RequestType.STUDYING -> Color(0xFF5A8FFF) // Bleu clair (proche de AccentDark)
      RequestType.STUDY_GROUP -> Color(0xFF7BA5FF) // Bleu ciel plus doux
      RequestType.HANGING_OUT -> Color(0xFFFFAA6B) // Orange clair
      RequestType.EATING -> Color(0xFFFF7B8A) // Rose clair
      RequestType.SPORT -> Color(0xFF6FDA9A) // Vert clair
      RequestType.HARDWARE -> Color(0xFFC48FD9) // Violet clair
      RequestType.LOST_AND_FOUND -> Color(0xFFFF6B6B) // Rouge clair (matching ErrorDark)
      RequestType.OTHER -> Color(0xFFADB5BD) // Gris clair
    }
  }

  override fun getRequestTypeBackgroundColor(type: RequestType): Color {
    return when (type) {
      RequestType.STUDYING -> Color(0xFF1A2744) // Bleu foncé (proche de PrimaryDark)
      RequestType.STUDY_GROUP -> Color(0xFF2B3650) // Bleu foncé (matching PrimaryDark)
      RequestType.HANGING_OUT -> Color(0xFF3D2A1A) // Orange foncé
      RequestType.EATING -> Color(0xFF3D1A22) // Rose/Rouge foncé
      RequestType.SPORT -> Color(0xFF1A3D28) // Vert foncé
      RequestType.HARDWARE -> Color(0xFF3A1A44) // Violet foncé
      RequestType.LOST_AND_FOUND -> Color(0xFF3D1A1A) // Rouge foncé
      RequestType.OTHER -> Color(0xFF2E3238) // Gris foncé (proche de SecondaryDark)
    }
  }
}

val LocalAppPalette = staticCompositionLocalOf<AppPalette> { LightPalette }

@Composable fun appPalette(): AppPalette = LocalAppPalette.current
