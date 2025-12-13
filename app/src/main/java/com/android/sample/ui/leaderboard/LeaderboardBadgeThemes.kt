package com.android.sample.ui.leaderboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.sample.ui.theme.LeaderboardColors

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

  val CutieColor = LeaderboardColors.Cutie

  val Gold =
      BadgeTheme(
          id = "gold",
          icon = Icons.Filled.Stars,
          primaryColor = LeaderboardColors.Gold,
          haloColor = LeaderboardColors.Gold.copy(alpha = BadgeThemeDefaults.HaloAlphaStrong),
          borderColor = LeaderboardColors.Gold,
          testTag = LeaderboardTestTags.MEDAL_GOLD)

  val Silver =
      BadgeTheme(
          id = "silver",
          icon = Icons.Filled.Stars,
          primaryColor = LeaderboardColors.Silver,
          haloColor = LeaderboardColors.Silver.copy(alpha = BadgeThemeDefaults.HaloAlphaStrong),
          borderColor = LeaderboardColors.Silver,
          testTag = LeaderboardTestTags.MEDAL_SILVER)

  val Bronze =
      BadgeTheme(
          id = "bronze",
          icon = Icons.Filled.Stars,
          primaryColor = LeaderboardColors.Bronze,
          haloColor = LeaderboardColors.Bronze.copy(alpha = BadgeThemeDefaults.HaloAlphaSoft),
          borderColor = LeaderboardColors.Bronze,
          testTag = LeaderboardTestTags.MEDAL_BRONZE)

  /** Map any rank to a badge theme; extend here for custom roles (admin, beta, etc.). */
  fun forRank(rank: Int): BadgeTheme? =
      when (rank) {
        BadgeThemeDefaults.RankGold -> Gold
        BadgeThemeDefaults.RankSilver -> Silver
        BadgeThemeDefaults.RankBronze -> Bronze
        else -> null
      }

  fun isPodiumTheme(theme: BadgeTheme): Boolean {
    return theme == Gold || theme == Silver || theme == Bronze
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
