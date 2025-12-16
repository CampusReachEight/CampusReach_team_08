package com.android.sample.ui.navigation

import androidx.navigation.NavHostController
import com.android.sample.ui.profile.follow.FollowListType

private const val REQUEST_ID = "requestId"

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

  object Requests : Screen(route = "requests/main", NavigationType.TAB)

  object Leaderboard : Screen(route = "leaderboard/main", NavigationType.TAB)

  data class Map(val requestId: String? = null) :
      Screen(
          route = if (requestId != null) "map/main?requestId=$requestId" else "map/main",
          NavigationType.TAB) {
    companion object {
      const val ARG_REQUEST_ID = REQUEST_ID
      const val ROUTE = "map/main?requestId={$ARG_REQUEST_ID}"
    }
  }

  object AddRequest : Screen(route = "requests/add")

  object MyRequest : Screen(route = "profile/myRequest", NavigationType.SUB_SCREEN)

  object AcceptedRequests : Screen(route = "profile/acceptedRequests", NavigationType.SUB_SCREEN)
  object Messages : Screen(route = "messages/main", NavigationType.TAB)


  data class RequestAccept(val requestId: String) : Screen(route = "requests/accept/${requestId}") {
    companion object {
      const val ARG_REQUEST_ID = REQUEST_ID
      const val route = "requests/accept/{$ARG_REQUEST_ID}"
    }
  }

  data class EditRequest(val requestId: String) : Screen(route = "requests/edit/${requestId}") {
    companion object {
      const val ARG_REQUEST_ID = REQUEST_ID
      const val route = "requests/edit/{$ARG_REQUEST_ID}"
    }
  }

  data class Profile(val userId: String) : Screen(route = "profile/main/${userId}") {
    companion object {
      const val ARG_USER_ID = "userId"
      const val route = "profile/main/{$ARG_USER_ID}"
    }
  }

  data class ValidateRequest(val requestId: String) : Screen(route = "validateRequest/$requestId") {
    companion object {
      const val ARG_REQUEST_ID = REQUEST_ID
      const val route = "validateRequest/{$ARG_REQUEST_ID}"
    }
  }

  data class PublicProfile(val userId: String) : Screen(route = "profile/public/${userId}") {
    companion object {
      const val ARG_USER_ID = "userId"
      const val route = "profile/public/{$ARG_USER_ID}"
    }
  }

  data class FollowList(val userId: String, val listType: FollowListType) :
      Screen(route = "follow_list/$userId/${listType.name}") {
    companion object {
      const val ARG_USER_ID = "userId"
      const val ARG_LIST_TYPE = "listType"
      const val route = "follow_list/{$ARG_USER_ID}/{$ARG_LIST_TYPE}"
    }
  }
  data class Chat(val chatId: String) : Screen(route = "messages/chat/${chatId}") {
    companion object {
      const val ARG_CHAT_ID = "chatId"
      const val route = "messages/chat/{$ARG_CHAT_ID}"
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
        // Navigate to the tab (clearing back to Requests if not going to Requests)
        if (screen.route != Screen.Requests.route) {
          navController.navigate(screen.route) {
            popUpTo(Screen.Requests.route) { inclusive = false }
            launchSingleTop = true
            restoreState = false
          }
        } else {
          // Going to Requests tab - just clear everything
          navController.navigate(Screen.Requests.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
            restoreState = false
          }
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
