package com.android.sample.ui.navigation

import com.android.sample.ui.theme.NavigationTab

object NavigationTestTags {
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val GO_BACK_BUTTON = "GoBackButton"
  const val TOP_BAR_TITLE = "TopBarTitle"

  const val LOGIN_SCREEN = "LoginScreen"
  const val REQUESTS_SCREEN = "RequestScreen"
  const val ADD_REQUEST_SCREEN = "AddRequestScreen"
  const val EDIT_REQUEST_SCREEN = "EditRequestScreen"
  const val ACCEPT_REQUEST_SCREEN = "RequestAcceptScreen"
  const val EVENTS_SCREEN = "EventScreen"
  const val ADD_EVENT_SCREEN = "AddEventScreen"
  const val EDIT_EVENT_SCREEN = "EditEventScreen"
  const val MAP_SCREEN = "MapScreen"
  const val PROFILE_SCREEN = "ProfileScreen"

  const val REQUEST_TAB = "RequestTab"
  const val EVENT_TAB = "EventTab"
  const val MAP_TAB = "MapTab"
  const val PROFILE_BUTTON = "ProfileButton"

  fun getTabTestTag(navigationTab: NavigationTab): String =
      when (navigationTab) {
        is NavigationTab.Map -> MAP_TAB
        is NavigationTab.Events -> EVENT_TAB
        is NavigationTab.Requests -> REQUEST_TAB
        is NavigationTab.Profile -> PROFILE_BUTTON
      }
}
