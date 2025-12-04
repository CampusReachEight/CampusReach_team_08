package com.android.sample.ui.map

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ConstantMap {
  const val TEXT_SEE_DETAILS = "See details"
  const val TEXT_EDIT = "Edit your request"
  const val PROBLEM_OCCUR = "A problem occur, click to reload"

  const val DETAILS = "Details"

  const val PROFILE = "Profile"
  const val LOCATION = "Location"

  const val CLOSE_BUTTON_DESCRIPTION = "Close"

  const val START_DATE = "Start Date"
  const val END_DATE = "End Date"

  const val KUDOS = "Kudos"
  const val SECTION = "Section"

  const val CURR_POS_NAME = "current location"
  const val ZOOM_OUT = "Zoom Out"
  const val ZOOM_IN = "Zoom In"
  const val YOUR_LOCATION = "Your Location"
  const val YOU_ARE_HERE = "You are here"

  const val ERROR_MESSAGE_CURRENT_LOCATION = "Error : this zoom needs your location"

  const val AUTO_ZOOM_SETTINGS = "Auto-Zoom Settings"

  // ======================
  // General / reusable
  // ======================
  const val ZERO = 0
  const val ONE = 1
  const val TWO_FLOAT = 2f
  const val THREE_FLOAT = 3f
  const val FIVE = 5
  val PADDING_STANDARD = 12.dp
  val PADDING_HORIZONTAL_STANDARD = 16.dp
  val SPACER_HEIGHT_SMALL = 4.dp
  val SPACER_HEIGHT_MID = 6.dp
  val SPACER_HEIGHT_MEDIUM = 12.dp
  val SPACER_HEIGHT_LARGE = 16.dp
  val SPACER_WIDTH_SMALL = 8.dp
  val CORNER_RADIUS_SMALL = 8.dp
  val CORNER_RADIUS_MEDIUM = 12.dp
  val CORNER_RADIUS_LARGE = 20.dp

  // Alphas reusable
  const val ALPHA_PRIMARY_SURFACE = 0.15f
  const val ALPHA_ON_CONTAINER_MEDIUM = 0.7f
  const val ALPHA_TEXT_UNSELECTED = 0.6f
  const val ALPHA_DIVIDER = 0.12f

  // ======================
  // Bottom Sheet
  // ======================
  val BOTTOM_SHEET_ELEVATION = 8.dp
  val BOTTOM_SHEET_SHAPE = 16.dp

  // ======================
  // Drag Handle
  // ======================
  val DRAG_HANDLE_WIDTH = 40.dp
  val DRAG_HANDLE_HEIGHT = 4.dp
  val DRAG_HANDLE_CORNER_RADIUS = 2.dp
  const val DRAG_HANDLE_ALPHA = 0.3f

  val MIN_HEIGHT = 120.dp
  const val DURATION_ANIMATION = 100
  const val PROPORTION_FOR_INITIALIZE_SHEET = 0.67f

  // ======================
  // Button X
  // ======================
  const val CLOSE_BUTTON_ALPHA = 0.6f

  // ======================
  // Tabs
  // ======================
  val TAB_ROW_EDGE_PADDING = 16.dp

  // ======================
  // Divider
  // ======================
  val DIVIDER_THICKNESS = 1.dp

  // ======================
  // Column / Surfaces / Buttons
  // ======================
  val ICON_SIZE_LOCATION = 20.dp
  const val WEIGHT_FILL = 1f

  const val MIN_OFFSET_Y = 0f

  // ======================
  // Profile
  // ======================

  val REQUEST_ITEM_ICON_SIZE = 40.dp
  const val TEXT_EDIT_PROFILE = "Edit your profile"
  val FONT_SIZE_BIG = 18.sp
  val FONT_SIZE_MID = 15.sp
  const val ALPHA_KUDOS_DIVIDER = 0.17f

  // ======================
  // Camera
  // ======================
  const val ZOOM_AFTER_CHOSEN = 17f
  const val LONG_DURATION_ANIMATION = 500
  const val VERY_LONG_DURATION_ANIMATION = 1000
  const val MAX_ZOOM_ONE = 5
  const val MAX_ZOOM_TWO = 7
  const val MAX_ZOOM_THREE = 9
  const val MAX_ZOOM_FOUR = 11
  const val MAX_ZOOM_FIVE = 14
  const val MAX_ZOOM_SIX = 17
  const val MAX_ZOOM_SEVEN = 20

  const val ZOOM_LEVEL_WORLD = 300000.0
  const val ZOOM_LEVEL_WL = 70000.0
  const val ZOOM_LEVEL_LAND = 40000.0
  const val ZOOM_LEVEL_REGION = 10000.0
  const val ZOOM_LEVEL_CITY = 3000.0
  const val ZOOM_LEVEL_MID = 400.0
  const val ZOOM_LEVEL_STREET_BIG = 50.0
  const val ZOOM_LEVEL_STREET_SMALL = 20.0
  const val ZOOM_DIVIDE = 5.0
  const val ZOOM_LEVEL_TWO = 2

  const val CURR_ZOOM_LEVEL_WORLD = 100000.0
  const val CURR_ZOOM_LEVEL_WL = 40000.0
  const val CURR_ZOOM_LEVEL_LAND = 20000.0
  const val CURR_ZOOM_LEVEL_REGION = 5000.0
  const val CURR_ZOOM_LEVEL_CITY = 1000.0
  const val CURR_ZOOM_LEVEL_MID = 200.0
  const val CURR_ZOOM_LEVEL_STREET_BIG = 35.0
  const val CURR_ZOOM_LEVEL_STREET_SMALL = 15.0
  const val CURR_ZOOM_DIVIDE = 5.0

  // ======================
  // Cluster image
  // ======================

  const val NUMBER_LENGTH_ONE = 10
  const val NUMBER_LENGTH_TWO = 100
  const val NUMBER_SIZE_ONE = 36f
  const val NUMBER_SIZE_TWO = 32f
  const val NUMBER_SIZE_THREE = 28f
  const val SIZE_OF_MARKER = 100

  // ======================
  // Distance request
  // ======================

  const val EARTH_RADIUS = 6371000.0
  const val TWO = 2

  // ======================
  // Map filter
  // ======================

  // Card elevation
  val CARD_DEFAULT_ELEVATION = 4.dp

  // Padding values
  val CARD_HORIZONTAL_PADDING = 16.dp
  val CARD_VERTICAL_PADDING = 8.dp
  val PANEL_HORIZONTAL_PADDING = 16.dp
  val PANEL_INTERNAL_PADDING = 16.dp
  val ROW_VERTICAL_PADDING = 8.dp

  // Spacing
  val BUTTON_SPACING = 8.dp
  val SPACER_HEIGHT = 8.dp
  val RADIO_BUTTON_SPACING = 8.dp

  // Component heights
  val FILTER_BUTTON_HEIGHT = 40.dp

  // Surface elevation
  val SURFACE_TONAL_ELEVATION = 2.dp
  val SURFACE_SHADOW_ELEVATION = 2.dp

  // Keys
  const val OWNERSHIP_KEY = "ownership"

  // ======================
  // Map settings
  // ======================

  const val DESCRIPTION_REQUEST = "Choose where to zoom when changing filters:"
  const val ALPHA_DIVIDER_SETTINGS = 0.7f
  const val DESCRIPTION_NEARBY_REQUEST = "Zoom to the closest request"
  const val TITLE_NEARBY_REQUEST = "Nearest Request"
  const val TITLE_CURR_LOCATION = "My Location"
  const val DESCRIPTION_CURRENT_LOCATION = "Zoom to your current position"
  const val TITLE_NO_ZOOM = "No Auto-Zoom"
  const val DESCRIPTION_NO_ZOOM = "Keep current map position"
  val ROUND_CORNER = 12.dp
  const val ALPHA_DIVIDER_SETTINGS_LITTLE = 0.1f
  const val ALPHA_DIVIDER_SETTINGS_MID = 0.2f
  const val ALPHA_DIVIDER_SETTINGS_BIG = 0.6f
  val TWO_DP = 2.dp
  val ONE_DP = 1.dp
  val SIXTEEN_DP = 16.dp
  val TWELVE_DP = 12.dp
  val TWENTY_FOUR_DP = 24.dp
  val TWENTY_DP = 20.dp
  const val SELECTED_ZOOM = "Selected"

  // ======================
  // Map view Model
  // ======================
  const val UNCHECKED_CAST = "UNCHECKED_CAST"
  const val ERROR_VIEW_MODEL_CLASS = "Unknown ViewModel class:"
  const val ERROR_MESSAGE_LOCATION_PERMISSION =
      "You will not have access to all the feature of the map"
  const val ERROR_FAILED_TO_GET_CURRENT_LOCATION = "Failed to get current location:"
  const val ZOOM_SETTING = "Zoom Settings"
}
