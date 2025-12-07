package com.android.sample.ui.leaderboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Describes how to render a badge/medal: icon, colors, and optional test tag. */
data class BadgeTheme(
    val id: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val haloColor: Color,
    val borderColor: Color,
    val cardBorderWidth: Dp = BadgeThemeDefaults.CardBorderWidth,
    val testTag: String? = null,
)

/** Central place to declare reusable badge themes (gold, silver, bronze, roles, etc.). */
object LeaderboardBadgeThemes {

  private val GoldColor = Color(0xFFF4C542)
  private val SilverColor = Color(0xFFC0C0C0)
  private val BronzeColor = Color(0xFFCD7F32)

  val Gold =
      BadgeTheme(
          id = "gold",
          icon = Icons.Filled.MilitaryTech,
          primaryColor = GoldColor,
          haloColor = GoldColor.copy(alpha = BadgeThemeDefaults.HaloAlphaStrong),
          borderColor = GoldColor,
          testTag = LeaderboardTestTags.MEDAL_GOLD)

  val Silver =
      BadgeTheme(
          id = "silver",
          icon = Icons.Filled.Star,
          primaryColor = SilverColor,
          haloColor = SilverColor.copy(alpha = BadgeThemeDefaults.HaloAlphaStrong),
          borderColor = SilverColor,
          testTag = LeaderboardTestTags.MEDAL_SILVER)

  val Bronze =
      BadgeTheme(
          id = "bronze",
          icon = Icons.Filled.Star,
          primaryColor = BronzeColor,
          haloColor = BronzeColor.copy(alpha = BadgeThemeDefaults.HaloAlphaSoft),
          borderColor = BronzeColor,
          testTag = LeaderboardTestTags.MEDAL_BRONZE)

  /** Map any rank to a badge theme; extend here for custom roles (admin, beta, etc.). */
  fun forRank(rank: Int): BadgeTheme? =
      when (rank) {
        BadgeThemeDefaults.RankGold -> Gold
        BadgeThemeDefaults.RankSilver -> Silver
        BadgeThemeDefaults.RankBronze -> Bronze
        else -> null
      }
}

/** Shared defaults for badge themes to avoid scattered literals. */
object BadgeThemeDefaults {
  const val RankGold = 1
  const val RankSilver = 2
  const val RankBronze = 3
  val CardBorderWidth = 1.5.dp
  const val HaloAlphaStrong = 0.16f
  const val HaloAlphaSoft = 0.18f
}
