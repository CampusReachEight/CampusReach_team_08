package com.android.sample.ui.profile

/**
 * Centralized test tags for the Profile UI. Organized by screen areas to make UI tests more robust
 * and discoverable.
 */
object ProfileTestTags {

  // Screen
  const val PROFILE_SCREEN = "profile_screen"

  // Top bar
  const val PROFILE_TOP_BAR = "profile_top_bar"
  const val PROFILE_TOP_BAR_BACK_BUTTON = "profile_top_bar_back_button"
  const val PROFILE_TOP_BAR_TITLE = "profile_top_bar_title"

  // Header
  const val PROFILE_HEADER = "profile_header"
  const val PROFILE_HEADER_PROFILE_PICTURE = "profile_header_profile_picture"
  const val PROFILE_HEADER_PICTURE = "profile_header_picture" // alternate/common name
  const val PROFILE_HEADER_NAME = "profile_header_name"
  const val PROFILE_HEADER_EMAIL = "profile_header_email"
  const val PROFILE_HEADER_EDIT_BUTTON = "profile_header_edit_button"

  // Loading & Error
  const val LOADING_INDICATOR = "loading_indicator"
  const val PROFILE_LOADING = "profile_loading" // used in ProfileScreen
  const val PROFILE_ERROR = "profile_error"

  // Stats
  const val PROFILE_STATS = "profile_stats"
  const val PROFILE_STAT_TOP_KUDOS = "profile_stat_top_kudos"
  const val PROFILE_STAT_BOTTOM_HELP_RECEIVED = "profile_stat_bottom_help_received"
  const val PROFILE_STAT_TOP_FOLLOWERS = "profile_stat_top_followers"
  const val PROFILE_STAT_BOTTOM_FOLLOWING = "profile_stat_bottom_following"

  // Information section and individual info rows (match InfoRow generated tags)
  const val PROFILE_INFORMATION = "profile_information"
  const val PROFILE_INFO_NAME = "profile_info_name"
  const val PROFILE_INFO_PROFILE_ID = "profile_info_profile_id"
  const val PROFILE_INFO_ARRIVAL_DATE = "profile_info_arrival_date"
  const val PROFILE_INFO_SECTION = "profile_info_section"
  const val PROFILE_INFO_EMAIL = "profile_info_email"

  // Actions
  const val PROFILE_ACTIONS = "profile_actions"
  const val PROFILE_ACTION_LOG_OUT = "profile_action_log_out"
  const val PROFILE_ACTION_ABOUT_APP = "profile_action_about_app"
  const val PROFILE_ACTION_ITEM_CARD_PREFIX = "profile_action_" // for discoverability

  // Logout dialog
  const val LOG_OUT_DIALOG = "log_out_dialog"
  const val LOG_OUT_DIALOG_TITLE = "log_out_dialog_title"
  const val LOG_OUT_DIALOG_MESSAGE = "log_out_dialog_message"
  const val LOG_OUT_DIALOG_CONFIRM = "log_out_dialog_confirm"
  const val LOG_OUT_DIALOG_CANCEL = "log_out_dialog_cancel"

  // Reusable / convenience aliases for older tests or variants
  const val PROFILE_STAT_KUDOS = PROFILE_STAT_TOP_KUDOS
  const val PROFILE_STAT_HELP = PROFILE_STAT_BOTTOM_HELP_RECEIVED
  const val PROFILE_STAT_FOLLOWERS = PROFILE_STAT_TOP_FOLLOWERS
  const val PROFILE_STAT_FOLLOWING = PROFILE_STAT_BOTTOM_FOLLOWING

  // Edit profile test tags (existing)
  const val EDIT_PROFILE_DIALOG = "edit_profile_dialog"
  const val EDIT_PROFILE_DIALOG_TITLE = "edit_profile_dialog_title"
  const val EDIT_PROFILE_NAME_INPUT = "edit_profile_name_input"
  const val EDIT_PROFILE_SECTION_DROPDOWN = "edit_profile_section_dropdown"
  const val EDIT_PROFILE_DIALOG_SAVE_BUTTON = "edit_profile_dialog_save_button"
  const val EDIT_PROFILE_DIALOG_CANCEL_BUTTON = "edit_profile_dialog_cancel_button"

  // Dropdown-specific tags / aliases used in some implementations
  const val SECTION_DROPDOWN = "section_dropdown" // used by SectionDropDown.testTag
  const val SECTION_OPTION_PREFIX = "section_option_" // optional per-option tag prefix
  const val EDIT_PROFILE_SECTION_DROPDOWN_ITEM_PREFIX = "edit_profile_section_dropdown_item_"
}
