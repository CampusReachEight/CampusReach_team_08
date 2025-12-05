package com.android.sample.ui.leaderboard

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Constants for Leaderboard UI dimensions and styling. Mirrors the approach used in
 * ConstantRequestList for consistency across the codebase.
 */
object ConstantLeaderboard {
  // Spacing
  val PaddingSmall = 8.dp
  val PaddingMedium = 12.dp
  val PaddingLarge = 16.dp
  val RowSpacing = 8.dp

  // List and items
  val ListPadding = 16.dp
  val ListItemSpacing = 8.dp
  val CardHeight = 80.dp
  val CardInnerPadding = 12.dp
  val CardCornerRadius = 12.dp

  // Profile picture section
  val ProfilePictureSize = 56.dp
  val ProfilePicturePadding = 4.dp

  // Stats section
  val StatsColumnWidth = 70.dp
  val StatsFontSize = 14.sp
  val StatsLabelFontSize = 11.sp
  val StatsSpacing = 4.dp

  // Name section
  val NameFontSize = 16.sp
  val SectionFontSize = 12.sp

  // Medal/badge effects
  val MedalIconSize = 24.dp
  val MedalBorderWidth = 2.dp

  // Search bar
  val SearchBarHeight = 56.dp
  val SearchBarCornerRadius = 12.dp

  // Filter buttons
  val FilterButtonHeight = 40.dp
  val FilterButtonMinWidth = 104.dp

  // Dropdown menu
  val DropdownMaxHeight = 280.dp
  val MenuCornerRadius = 12.dp

  // Rows inside dropdown
  val FilterRowHeight = 48.dp
  val FilterRowHorizontalPadding = 12.dp
  val CheckboxSize = 20.dp

  // Range filter specific
  val RangePanelMaxHeight = 200.dp
}
