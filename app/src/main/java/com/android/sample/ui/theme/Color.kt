package com.android.sample.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.firebase.annotations.concurrent.Background
import okhttp3.internal.concurrent.TaskRunner

object AppColors {
    // Helpful aliases from the Figma palette (kept minimal)
    val BlackColor = Color(0xFF1F242F)
    val WhiteColor = Color(0xFFFFFFFF)

    // Light theme palette
    val PrimaryColor = Color(0xFFF0F4FF)
    val SecondaryColor = Color(0xFFD8E4FF)
    val AccentColor = Color(0xFF1247F8)
    val BackgroundColor = PrimaryColor
    val SurfaceColor = WhiteColor
    val ErrorColor = Color(0xFFB22222)

    // Dark theme palette
    val PrimaryDark = Color(0xFF2B3650)
    val SecondaryDark = Color(0xFF4B4F58)
    val AccentDark = Color(0xFF2D6BFF)
    val BackgroundDark = Color(0xFF121216)
    val SurfaceDark = Color(0xFF1E1B1E)
    val ErrorDark = Color(0xFFFF6B6B)

}

// Legacy/compatibility aliases
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
