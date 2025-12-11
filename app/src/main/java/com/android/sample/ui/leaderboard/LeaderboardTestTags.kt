package com.android.sample.ui.leaderboard

import com.android.sample.ui.profile.UserSections

/**
 * Test tags for Leaderboard UI components. Follows the pattern established by RequestListTestTags
 * for consistency.
 */
object LeaderboardTestTags {
  // Screen-level tags
  const val LEADERBOARD_SCREEN = "leaderboardScreen"
  const val LEADERBOARD_LIST = "leaderboardList"
  const val LEADERBOARD_CARD = "leaderboardCard"
  const val EMPTY_LIST_MESSAGE = "leaderboardEmptyMessage"
  const val LOADING_INDICATOR = "leaderboardLoadingIndicator"
  const val ERROR_MESSAGE_DIALOG = "leaderboardErrorDialog"
  const val OK_BUTTON_ERROR_DIALOG = "leaderboardOkButtonErrorDialog"

  // Search bar
  const val SEARCH_BAR = "leaderboardSearchBar"

  // Sort button and menu
  const val SORT_BUTTON = "leaderboardSortButton"
  const val SORT_MENU = "leaderboardSortMenu"

  // Section filter (enum facet)
  const val SECTION_FILTER_DROPDOWN_BUTTON = "leaderboardSectionFilterDropdown"
  const val SECTION_FILTER_SEARCH_BAR = "leaderboardSectionFilterSearchBar"

  // Kudos range filter
  const val KUDOS_RANGE_BUTTON = "leaderboardKudosRangeButton"
  const val KUDOS_RANGE_PANEL = "leaderboardKudosRangePanel"
  const val KUDOS_RANGE_SLIDER = "leaderboardKudosRangeSlider"
  const val KUDOS_RANGE_MIN_FIELD = "leaderboardKudosRangeMinField"
  const val KUDOS_RANGE_MAX_FIELD = "leaderboardKudosRangeMaxField"

  // Help received range filter
  const val HELP_RANGE_BUTTON = "leaderboardHelpRangeButton"
  const val HELP_RANGE_PANEL = "leaderboardHelpRangePanel"
  const val HELP_RANGE_SLIDER = "leaderboardHelpRangeSlider"
  const val HELP_RANGE_MIN_FIELD = "leaderboardHelpRangeMinField"
  const val HELP_RANGE_MAX_FIELD = "leaderboardHelpRangeMaxField"

  // Card content tags
  const val CARD_PROFILE_PICTURE = "leaderboardCardProfilePicture"
  const val CARD_NAME = "leaderboardCardName"
  const val CARD_SECTION = "leaderboardCardSection"
  const val CARD_KUDOS_VALUE = "leaderboardCardKudosValue"
  const val CARD_HELP_VALUE = "leaderboardCardHelpValue"

  // Medal/effect tags
  const val MEDAL_GOLD = "leaderboardMedalGold"
  const val MEDAL_SILVER = "leaderboardMedalSilver"
  const val MEDAL_BRONZE = "leaderboardMedalBronze"
  const val CUTIE_PATOOTIE_FILTER = "cutie_patootie_filter"

  /**
   * Generates a test tag for a section filter row.
   *
   * @param section The UserSections value
   * @return Test tag in format "sectionFilter_{sectionName}"
   */
  fun getSectionFilterTag(section: UserSections): String = "sectionFilter_${section.name}"

  /**
   * Generates a test tag for a specific leaderboard card by user ID.
   *
   * @param userId The user's ID
   * @return Test tag in format "leaderboardCard_{userId}"
   */
  fun getCardTag(userId: String): String = "${LEADERBOARD_CARD}_$userId"

  /**
   * Generates a test tag for a sort option.
   *
   * @param sort The LeaderboardSort option
   * @return Test tag in format "leaderboardSortOption_{sortName}"
   */
  fun getSortOptionTag(sort: LeaderboardSort): String = "leaderboardSortOption_${sort.name}"
}
