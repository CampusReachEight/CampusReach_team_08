package com.android.sample.ui.leaderboard

import androidx.compose.ui.unit.dp

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
  val CardBorderWidth = 1.dp
  val SurfaceTonalElevation = 2.dp
  val SurfaceShadowElevation = 2.dp

  // Profile picture section
  val ProfilePictureSize = 56.dp

  // Stats section
  val StatsColumnWidth = 70.dp
  val MedalIconSize = 24.dp
  val SecondaryTextAlpha = 0.7f

  // Filter buttons
  val FilterButtonHeight = 40.dp
  val FilterRowSpacingSmall = 4.dp
  val SortButtonPaddingHorizontal = 12.dp
  val SortButtonPaddingVertical = 6.dp

  // Dropdown menu
  val DropdownMaxHeight = 280.dp
  val MenuCornerRadius = 12.dp

  // Rows inside dropdown
  val FilterRowHeight = 48.dp
  val FilterRowHorizontalPadding = 12.dp
  val CheckboxSize = 20.dp

  // Loading indicator
  val SmallIndicatorSize = 18.dp
  val SmallIndicatorStroke = 2.dp

  // Utility
  val WeightFill = 1f
  val ZeroCountFallback = 0
  val ListIndexOffset = 1
  val SingleLineMax = 1
}
