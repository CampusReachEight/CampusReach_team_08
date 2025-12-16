package com.android.sample.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.android.sample.ui.theme.AppColors.SecondaryDark

/** Constants for shimmer animation configuration */
object ShimmerDefaults {
  const val DURATION_MS = 1000
  const val START_OFFSET = -1f
  const val END_OFFSET = 2f
  const val BACKGROUND_ALPHA = 0.3f
  const val HIGHLIGHT_ALPHA = 0.5f
  const val OFFSET_MULTIPLIER = 1000f
  const val GRADIENT_WIDTH = 0.7f
}

/**
 * CompositionLocal to provide a shared shimmer progress value across all shimmer composables. This
 * ensures that all shimmer effects are synchronized.
 */
val LocalShimmerProgress = compositionLocalOf { 0f }

/**
 * Provider for the synchronized shimmer effect. Wrap your content with this composable to
 * synchronize shimmer animations across all child composables that use [ShimmerBox] or
 * [ShimmerPlaceholder].
 *
 * @param content The composable content that will have access to synchronized shimmer animations
 */
@Composable
fun SynchronizedShimmerProvider(content: @Composable () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "shared_shimmer")
  val shimmerProgress by
      infiniteTransition.animateFloat(
          initialValue = ShimmerDefaults.START_OFFSET,
          targetValue = ShimmerDefaults.END_OFFSET,
          animationSpec =
              infiniteRepeatable(
                  animation =
                      tween(durationMillis = ShimmerDefaults.DURATION_MS, easing = LinearEasing),
                  repeatMode = RepeatMode.Restart),
          label = "shared_shimmer_progress")

  CompositionLocalProvider(LocalShimmerProgress provides shimmerProgress) { content() }
}

/**
 * Creates a shimmer brush based on the current shimmer progress from [LocalShimmerProgress].
 *
 * @return A [Brush] configured for shimmer effect
 */
@Composable
fun shimmerBrush(): Brush {
  val shimmerProgress = LocalShimmerProgress.current

  return Brush.linearGradient(
      colors =
          listOf(
              SecondaryDark.copy(alpha = ShimmerDefaults.BACKGROUND_ALPHA),
              Color.White.copy(alpha = ShimmerDefaults.HIGHLIGHT_ALPHA),
              SecondaryDark.copy(alpha = ShimmerDefaults.BACKGROUND_ALPHA)),
      start = Offset(shimmerProgress * ShimmerDefaults.OFFSET_MULTIPLIER, 0f),
      end =
          Offset(
              (shimmerProgress + ShimmerDefaults.GRADIENT_WIDTH) *
                  ShimmerDefaults.OFFSET_MULTIPLIER,
              0f))
}
