package com.android.sample.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.sample.ui.authentication.SignInScreen
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.theme.BottomNavigationMenu
import com.android.sample.ui.theme.Tab
import com.google.firebase.auth.FirebaseAuth


@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    navigationActions: Screen.NavigationActions = Screen.NavigationActions(navController)
) {
  val startDestination = if (FirebaseAuth.getInstance().currentUser == null) "login" else "requests"

  // Track current route to determine selected tab
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  // Determine if we should show bottom nav (hide on login)
  val showBottomNav = currentRoute != Screen.Login.route

  Scaffold(
      modifier = modifier,
      bottomBar = {
        if (showBottomNav) {
          // Determine selected tab based on current route
          val selectedTab =
              when (currentRoute) {
                Screen.Events.route -> Tab.Events
                Screen.Map.route -> Tab.Map
                Screen.Profile.route -> Tab.Profile
                else -> Tab.Requests
              }

          BottomNavigationMenu(
              selectedTab = selectedTab,
              onTabSelected = { tab -> navigationActions.navigateTo(tab.destination) })
        }
      }) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
        ) {
          navigation(startDestination = Screen.Login.route, route = "login") {
            composable(Screen.Login.route) {
              SignInScreen(onSignInSuccess = { navigationActions.navigateTo(Screen.Requests) })
            }
          }

          navigation(startDestination = Screen.Requests.route, route = "requests") {
            composable(Screen.Requests.route) {
              PlaceHolderScreen(
                  text = "Requests Screen", Modifier.testTag(NavigationTestTags.REQUESTS_SCREEN))
            }
            composable(Screen.AddRequest.route) {
              PlaceHolderScreen(
                  text = "Add Request Screen",
                  Modifier.testTag(NavigationTestTags.ADD_REQUEST_SCREEN))
            }
            composable(Screen.RequestDetails.route) { navBackStackEntry ->
              val requestId =
                  navBackStackEntry.arguments?.getString(Screen.RequestDetails.ARG_REQUEST_ID)
              PlaceHolderScreen(
                  text = "Edit Request Screen: $requestId",
                  Modifier.testTag(NavigationTestTags.EDIT_REQUEST_SCREEN))
            }
          }

          navigation(startDestination = Screen.Events.route, route = "events") {
            composable(Screen.Events.route) {
              PlaceHolderScreen(
                  text = "Events Screen", Modifier.testTag(NavigationTestTags.EVENTS_SCREEN))
            }
            composable(Screen.AddEvent.route) {
              PlaceHolderScreen(
                  text = "Add Event Screen", Modifier.testTag(NavigationTestTags.ADD_EVENT_SCREEN))
            }
            composable(Screen.EventDetails.route) { navBackStackEntry ->
              val eventId = navBackStackEntry.arguments?.getString(Screen.EventDetails.ARG_EVENT_ID)
              PlaceHolderScreen(
                  text = "Edit Event Screen : $eventId",
                  Modifier.testTag(NavigationTestTags.EDIT_EVENT_SCREEN))
            }
          }

          navigation(startDestination = Screen.Map.route, route = "map") {
            composable(Screen.Map.route) { MapScreen() }
          }

          navigation(startDestination = Screen.Profile.route, route = "profile") {
            composable(Screen.Profile.route) {
              ProfileScreen(onBackClick = { navigationActions.goBack() })
            }
          }
        }
      }
}

@Composable
fun PlaceHolderScreen(text: String, modifier: Modifier) {
  Text(text = text, textAlign = TextAlign.Center, modifier = modifier.fillMaxSize().padding(20.dp))
}
