package com.android.sample.ui.request_validation

/**
 * UI constants and strings for the request validation screen. Centralized for easy maintenance and
 * potential i18n support.
 *
 * ALL magic values are defined here - no hardcoded values in the UI code.
 */
object ValidateRequestConstants {

  // ==================== STRINGS ====================

  // Screen titles and headers
  const val SCREEN_TITLE = "Resolve Request"
  const val HEADER_SELECT_HELPERS = "Select helpers to reward"

  // Instructions and descriptions
  fun getHeaderDescription(requestTitle: String) =
      "Choose the people who helped you with \"$requestTitle\""

  // Loading states
  const val LOADING_REQUEST = "Loading request details..."
  const val PROCESSING_REQUEST = "Closing request and awarding kudos..."
  const val PROCESSING_WAIT = "Please wait"

  // Success messages
  const val SUCCESS_TITLE = "Request closed successfully!"
  const val SUCCESS_SUBTITLE = "Kudos have been awarded"

  // Error messages
  const val ERROR_TITLE = "Error"
  const val ERROR_NOT_OWNER = "You are not the owner of this request."
  const val ERROR_INVALID_STATUS = "This request cannot be closed. Status: "
  const val ERROR_LOAD_PROFILES = "Could not load helper profiles. Please try again."
  const val ERROR_LOAD_REQUEST_PREFIX = "Failed to load request: "
  const val ERROR_CLOSE_REQUEST_PREFIX = "Failed to close request: "
  const val ERROR_AWARD_KUDOS_PREFIX = "Failed to award kudos: "
  const val ERROR_UNEXPECTED_PREFIX = "An unexpected error occurred: "

  // Empty state
  const val EMPTY_NO_HELPERS = "No one has accepted this request yet"
  const val EMPTY_CAN_CLOSE = "You can still close the request without awarding kudos"

  // Buttons
  const val BUTTON_VALIDATE = "Close Request & Award Kudos"
  const val BUTTON_CLOSE = "Close Request"
  const val BUTTON_CONFIRM = "Confirm & Close"
  const val BUTTON_CANCEL = "Cancel"
  const val BUTTON_RETRY = "Retry"
  const val BUTTON_GO_BACK = "Go Back"

  // Confirmation dialog
  const val CONFIRM_TITLE = "Confirm & Close Request"
  const val CONFIRM_NO_HELPERS =
      "You are about to close this request without selecting any helpers."
  const val CONFIRM_NO_KUDOS = "No kudos will be awarded."
  const val CONFIRM_AWARD_TO = "You are about to award kudos to:"
  const val CONFIRM_TOTAL_KUDOS = "Total kudos:"
  const val CONFIRM_CANNOT_UNDO = "This action cannot be undone."

  // Symbols and formatting
  const val SYMBOL_MULTIPLY = " × "
  const val SYMBOL_PLUS = "+"

  fun getConfirmHelperBonus(kudos: Int) =
      "The people you selected will receive ${SYMBOL_PLUS}$kudos kudos for resolving this request!"

  // Kudos summary
  const val SUMMARY_TOTAL_LABEL = "Total kudos to award"

  fun getSummaryHelperCount(count: Int) =
      "$count helper${if (count != 1) "s" else ""}$SYMBOL_MULTIPLY${KudosConstants.KUDOS_PER_HELPER} kudos"

  // fun getSummaryCreatorBonus(kudos: Int) = "You will receive ${SYMBOL_PLUS}$kudos kudos"

  // Profile display
  const val KUDOS_SUFFIX = " kudos"

  fun getUserKudos(kudos: Int) = "$kudos$KUDOS_SUFFIX"

  fun getKudosBadge(kudos: Int) = "$SYMBOL_PLUS$kudos"

  // Content descriptions (accessibility)
  const val CD_GO_BACK = "Go back"
  const val CD_PROFILE_PICTURE = "Profile picture of "
  const val CD_SELECTED = "Selected"
  const val CD_KUDOS = "Kudos"
  const val CD_SUCCESS = "Success"
  const val CD_ERROR = "Error"

  // ==================== TEST TAGS ====================

  const val TAG_BACK_BUTTON = "backButton"
  const val TAG_LOADING_INDICATOR = "loadingIndicator"
  const val TAG_HELPERS_LIST = "helpersList"
  const val TAG_VALIDATE_BUTTON = "validateButton"
  const val TAG_PROCESSING_INDICATOR = "processingIndicator"
  const val TAG_CONFIRMATION_DIALOG = "confirmationDialog"
  const val TAG_CONFIRM_BUTTON = "confirmButton"
  const val TAG_CANCEL_BUTTON = "cancelButton"
  const val TAG_RETRY_BUTTON = "retryButton"

  fun getHelperCardTag(userId: String) = "helperCard_$userId"

  // ==================== DIMENSIONS (DP) ====================

  // Icon sizes
  const val PROFILE_PICTURE_SIZE_DP = 56
  const val VALUE_ZERO = 0
  const val SELECTION_INDICATOR_SIZE_DP = 20
  const val SELECTION_INDICATOR_ICON_SIZE_DP = 12
  const val KUDOS_ICON_SIZE_DP = 16
  const val KUDOS_ICON_LARGE_SIZE_DP = 32
  const val LARGE_ICON_SIZE_DP = 64
  const val BUTTON_ICON_SIZE_DP = 20
  const val BUTTON_ICON_SIZE_LARGE_DP = 24
  const val DIALOG_ICON_SIZE_DP = 16

  // Component sizes
  const val BUTTON_HEIGHT_DP = 56
  const val SELECTION_INDICATOR_BORDER_DP = 2
  const val CARD_BORDER_WIDTH_UNSELECTED_DP = 1
  const val CARD_BORDER_WIDTH_SELECTED_DP = 2
  const val PROFILE_BORDER_WIDTH_DP = 2

  // Padding and spacing
  const val PADDING_SCREEN_DP = 16
  const val PADDING_CARD_DP = 16
  const val PADDING_BADGE_HORIZONTAL_DP = 12
  const val PADDING_BADGE_VERTICAL_DP = 6
  const val PADDING_BADGE_START_DP = 8
  const val PADDING_HORIZONTAL_LARGE_DP = 32
  const val PADDING_VERTICAL_DIALOG_DP = 8

  const val SPACING_SMALL_DP = 4
  const val SPACING_MEDIUM_DP = 8
  const val SPACING_LARGE_DP = 12
  const val SPACING_XLARGE_DP = 16
  const val SPACING_XXLARGE_DP = 24
  const val SPACING_XXXLARGE_DP = 32

  const val SPACING_HEADER_BOTTOM_DP = 8
  const val SPACING_DESCRIPTION_BOTTOM_DP = 24
  const val SPACING_SUMMARY_VERTICAL_DP = 8
  const val SPACING_ERROR_TOP_DP = 16

  // Corner radius
  const val CORNER_RADIUS_SMALL_DP = 8
  const val CORNER_RADIUS_MEDIUM_DP = 12
  const val CORNER_RADIUS_LARGE_DP = 16

  // ==================== FRACTIONS & RATIOS ====================

  const val ALPHA_SELECTED_BACKGROUND = 0.3f
  const val ALPHA_SECONDARY_TEXT = 0.7f
  const val WIDTH_FRACTION_EMPTY_BUTTON = 0.7f

  // ==================== ANIMATION ====================

  const val COLOR_ANIMATION_DURATION_MS = 200
}
