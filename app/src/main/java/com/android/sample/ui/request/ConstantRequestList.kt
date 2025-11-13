package com.android.sample.ui.request

import androidx.compose.ui.unit.dp

/**
 * Constants for RequestList UI. Avoid magic numbers: use these values for sizing/spacing. Mirrors
 * the approach used in ConstantAcceptRequest.
 */
object ConstantRequestList {
  // Spacing
  val PaddingSmall = 8.dp
  val PaddingMedium = 12.dp
  val PaddingLarge = 16.dp
  val RowSpacing = 8.dp

  // List and items
  val ListPadding = 16.dp
  val ListItemSpacing = 8.dp
  val RequestItemIconSize = 40.dp
  val RequestItemInnerPadding = 8.dp

  // Search bar
  val SearchBarSize = 18.dp
  val SearchBarStrokeWidth = 2.dp

  // Filter buttons (collapsed headers)
  val FilterButtonHeight = 40.dp
  val FilterButtonMinWidth = 104.dp

  // Dropdown menu
  val DropdownMaxHeight = 280.dp // ~5-6 items depending on row height
  val MenuCornerRadius = 12.dp

  // Rows inside dropdown
  val FilterRowHeight = 48.dp
  val FilterRowHorizontalPadding = 12.dp
  val CheckboxSize = 20.dp
}
