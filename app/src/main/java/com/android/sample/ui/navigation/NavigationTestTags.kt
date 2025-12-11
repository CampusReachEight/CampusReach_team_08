package com.android.sample.ui.navigation

import com.android.sample.ui.leaderboard.LeaderboardTestTags

object NavigationTestTags {
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val GO_BACK_BUTTON = "GoBackButton"
  const val TOP_BAR_TITLE = "TopBarTitle"

  const val TOP_NAVIGATION_BAR = "TopNavigationBar"
  const val VALIDATE_REQUEST_SCREEN = "ValidateRequestScreen"

  const val LOGIN_SCREEN = "LoginScreen"
  const val REQUESTS_SCREEN = "RequestScreen"
  const val ADD_REQUEST_SCREEN = "AddRequestScreen"
  const val EDIT_REQUEST_SCREEN = "EditRequestScreen"
  const val ACCEPT_REQUEST_SCREEN = "RequestAcceptScreen"
  const val LEADERBOARD_SCREEN = LeaderboardTestTags.LEADERBOARD_SCREEN
  const val MAP_SCREEN = "MapScreen"
  const val PROFILE_SCREEN = "ProfileScreen"

  const val REQUEST_TAB = "RequestTab"
  const val LEADERBOARD_TAB = "LeaderboardTab"
  const val MAP_TAB = "MapTab"
  const val PROFILE_BUTTON = "ProfileButton"
  const val SETTINGS_BUTTON = "settingsButton"

  fun getTabTestTag(navigationTab: NavigationTab): String =
      when (navigationTab) {
        is NavigationTab.Map -> MAP_TAB
        is NavigationTab.Leaderboard -> LEADERBOARD_TAB
        is NavigationTab.Requests -> REQUEST_TAB
      }
}
