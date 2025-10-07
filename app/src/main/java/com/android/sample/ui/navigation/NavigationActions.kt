package com.android.sample.ui.navigation

import android.annotation.SuppressLint
import androidx.navigation.NavHostController

sealed class Screen(val route: String, val isTopLevel: Boolean = false) {
  object Login : Screen(route = "login/main", isTopLevel = true)

  object Requests : Screen(route = "requests/main", isTopLevel = true)

  object Events : Screen(route = "events/main", isTopLevel = true)

  object Map : Screen(route = "map/main", isTopLevel = true)

  object Profile : Screen(route = "profile/main")

  object AddRequest : Screen(route = "requests/add")

  object AddEvent : Screen(route = "events/add")

  data class EditRequest(val requestId: String) : Screen(route = "requests/edit/${requestId}") {
    companion object {
      const val ARG_REQUEST_ID = "requestId"
      const val route = "requests/edit/{$ARG_REQUEST_ID}"
    }
  }

  data class EditEvent(val eventId: String) : Screen(route = "events/edit/${eventId}") {
    companion object {
      const val ARG_EVENT_ID = "eventId"
      const val route = "events/edit/{$ARG_EVENT_ID}"
    }
  }
}

open class NavigationActions(private val navController: NavHostController) {

  open fun navigateTo(screen: Screen) {
    // Don't navigate if already on this screen
    if (currentRoute() == screen.route) {
      return
    }

    if (screen is Screen.Requests || screen is Screen.Login) {
      navController.navigate(screen.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
      }
    } else if (screen.isTopLevel) {
      navController.navigate(screen.route) {
        popUpTo(Screen.Requests.route) { inclusive = false }
        launchSingleTop = true
      }
    } else {
      navController.navigate(screen.route)
    }
  }

  @SuppressLint("RestrictedApi")
  open fun goBack() {
    navController.popBackStack()
  }

  open fun currentRoute(): String = navController.currentBackStackEntry?.destination?.route ?: ""
}
