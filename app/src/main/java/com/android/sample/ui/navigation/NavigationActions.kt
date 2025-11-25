package com.android.sample.ui.navigation

import androidx.navigation.NavHostController

/**
 * Class defining the different screens in the app
 *
 * @param route Navigation route for the screen
 * @param navigationType Type of navigation for the screen, see [NavigationType]
 */
sealed class Screen(
    val route: String,
    val navigationType: NavigationType = NavigationType.SUB_SCREEN
) {
  object Login : Screen(route = "login/main", NavigationType.APP_ENTRY_POINT)

  object Requests : Screen(route = "requests/main", NavigationType.APP_ENTRY_POINT)

  object Events : Screen(route = "events/main", NavigationType.TAB)

  object Map : Screen(route = "map/main", NavigationType.TAB)

  object AddRequest : Screen(route = "requests/add")

  object AddEvent : Screen(route = "events/add")

  object MyRequest : Screen(route = "profile/myRequest", NavigationType.SUB_SCREEN)

  data class RequestAccept(val requestId: String) : Screen(route = "requests/accept/${requestId}") {
    companion object {
      const val ARG_REQUEST_ID = "requestId"
      const val route = "requests/accept/{$ARG_REQUEST_ID}"
    }
  }

  data class EditRequest(val requestId: String) : Screen(route = "requests/edit/${requestId}") {
    companion object {
      const val ARG_REQUEST_ID = "requestId"
      const val route = "requests/edit/{$ARG_REQUEST_ID}"
    }
  }

  data class EventDetails(val eventId: String) : Screen(route = "events/details/${eventId}") {
    companion object {
      const val ARG_EVENT_ID = "eventId"
      const val route = "events/details/{$ARG_EVENT_ID}"
    }
  }

  data class Profile(val userId: String) : Screen(route = "profile/main/${userId}") {
    companion object {
      const val ARG_USER_ID = "userId"
      const val route = "profile/main/{$ARG_USER_ID}"
    }
  }

  data class PublicProfile(val userId: String) : Screen(route = "profile/public/${userId}") {
    companion object {
      const val ARG_USER_ID = "userId"
      const val route = "profile/public/{$ARG_USER_ID}"
    }
  }

  data class ValidateRequest(val requestId: String) : Screen("validateRequest/{requestId}") {
    companion object {
      const val ARG_REQUEST_ID = "requestId"
      const val route = "validateRequest/{$ARG_REQUEST_ID}"
    }
  }
}

/**
 * Defines how the screen is treated in the navigation stack
 * - TAB: Bottom navigation tab, navigating to a tab clears the back stack, sets Requests as the
 *   root, then adds the tab on top
 * - APP_ENTRY_POINT: App entry point, navigating to an entry point clears the back stack and sets
 *   the screen as the root
 * - SUB_SCREEN: Regular screen, navigating to a sub screen adds it to the back stack (navigating to
 *   a sub screen from a tab not associated with it will still put it on top of the stack without
 *   clearing it)
 */
enum class NavigationType {
  TAB,
  APP_ENTRY_POINT,
  SUB_SCREEN,
}

/**
 * Class holding navigation actions this the main way to navigate between screens in the app
 *
 * @param navController NavHostController to be used for navigation actions
 */
open class NavigationActions(private val navController: NavHostController) {

  open fun navigateTo(screen: Screen) {
    if (currentRoute() == screen.route) {
      return
    }

    when (screen.navigationType) {
      NavigationType.TAB -> {
        // Clear stack and set Requests as the root
        navController.navigate(Screen.Requests.route) {
          popUpTo(0) { inclusive = true }
          restoreState = false
          launchSingleTop = true
        }

        // Navigate to the tab
        navController.navigate(screen.route) {
          launchSingleTop = true
          restoreState = false
        }
      }
      NavigationType.APP_ENTRY_POINT -> {
        navController.navigate(screen.route) {
          popUpTo(0) { inclusive = true }
          restoreState = false
          launchSingleTop = true
        }
      }
      NavigationType.SUB_SCREEN -> {
        navController.navigate(screen.route) {
          launchSingleTop = true
          restoreState = false
        }
      }
    }
  }

  /**
   * Navigate back to previous screen
   *
   * contrary to the system back button, this does not close the app
   */
  open fun goBack() {
    navController.popBackStack()
  }

  open fun currentRoute(): String = navController.currentBackStackEntry?.destination?.route ?: ""
}
