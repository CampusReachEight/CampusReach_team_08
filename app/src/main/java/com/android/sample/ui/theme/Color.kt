package com.android.sample.ui.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    // Semantic, app-wide colors (use these across the app)
    val Primary = Color(0xFF6650A4)
    val PrimaryVariant = Color(0xFFD0BCFF)
    val Secondary = Color(0xFF625B71)
    val Accent = Color(0xFFEFB8C8)
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)

    // On-* colors for contrast
    val OnPrimary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF000000)
    val OnAccent = Color(0xFF000000) // dark text on the light pink Accent

    // Error token (moved out of Theme.kt magic number)
    val Error = Color(0xFFB00020)
}

// Legacy/compatibility aliases
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
