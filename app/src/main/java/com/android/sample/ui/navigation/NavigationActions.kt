package com.android.sample.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(val route: String, val doesResetStack: Boolean = false) {
  object Login : Screen(route = "login", doesResetStack = true)

  object Requests : Screen(route = "requests", doesResetStack = true)

  object Events : Screen(route = "events", doesResetStack = true)

  object Map : Screen(route = "map", doesResetStack = true)

  object Profile : Screen(route = "profile")

  object AddRequest : Screen(route = "add_request")

  object AddEvent : Screen(route = "add_event")

  data class EditRequest(val requestId: String) : Screen(route = "edit_request/${requestId}") {
    companion object {
      const val ARG_REQUEST_ID = "requestId"
      const val route = "edit_request/{$ARG_REQUEST_ID}"
    }
  }

  data class EditEvent(val eventId: String) : Screen(route = "edit_event/${eventId}") {
    companion object {
      const val ARG_EVENT_ID = "eventId"
      const val route = "edit_event/{$ARG_EVENT_ID}"
    }
  }
}

open class NavigationActions(private val navController: NavHostController) {
  open fun navigateTo(screen: Screen) {
    if (screen.route == currentRoute()) {
      return
    }

    if (screen.doesResetStack) {
      navController.navigate(screen.route) {
        popUpTo(Screen.Requests.route) { saveState = false }
        launchSingleTop = true
      }
    } else {
      navController.navigate(screen.route) { launchSingleTop = true }
    }
  }

  open fun goBack() {
    navController.popBackStack()
  }

  open fun currentRoute(): String {
    return navController.currentBackStackEntry?.destination?.route ?: ""
  }
}
