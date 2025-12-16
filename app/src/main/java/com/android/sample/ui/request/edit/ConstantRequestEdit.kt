package com.android.sample.ui.request.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object ConstantRequestEdit {
  // Delete Button //

  val deleteProgressIndicatorSize = 24.dp
  val fullWidthModifier = Modifier.fillMaxWidth()

  // Edit Request Screen //

  val SCREEN_CONTENT_PADDING = 16.dp
  val CARD_CONTENT_PADDING = 12.dp
  val SPACING = 8.dp
  val CIRCULAR_PROGRESS_INDICATOR = 20.dp
  val SAVE_BUTTON_PADDING = 24.dp

  // UI State //

  const val ONE_HOUR_MS = 3_600_000L
  const val TWO_HOURS_MS = 7_200_000L

  // View Model //

  const val LOCATION_PERMISSION_REQUIRED = "Location permission is required to use current location"
}
