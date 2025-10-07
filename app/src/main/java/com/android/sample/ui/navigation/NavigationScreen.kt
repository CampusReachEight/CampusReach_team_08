package com.android.sample.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    // navigationActions: NavigationActions = NavigationActions(navController), // commented bcs of
    // sonarcloud
) {

  val startDestination = if (FirebaseAuth.getInstance().currentUser == null) "login" else "requests"

  NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = modifier,
  ) {
    navigation(startDestination = Screen.Login.route, route = "login") {
      composable(Screen.Login.route) {
        PlaceHolderScreen(text = "Login Screen", Modifier.testTag(NavigationTestTags.LOGIN_SCREEN))
      }
    }

    navigation(startDestination = Screen.Requests.route, route = "requests") {
      composable(Screen.Requests.route) {
        PlaceHolderScreen(
            text = "Requests Screen", Modifier.testTag(NavigationTestTags.REQUESTS_SCREEN))
      }
      composable(Screen.AddRequest.route) {
        PlaceHolderScreen(
            text = "Add Request Screen", Modifier.testTag(NavigationTestTags.ADD_REQUEST_SCREEN))
      }
      composable(Screen.RequestDetails.route) { navBackStackEntry ->
        val requestId = navBackStackEntry.arguments?.getString(Screen.RequestDetails.ARG_REQUEST_ID)
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

    navigation(startDestination = Screen.Profile.route, route = "profile") {
      composable(Screen.Profile.route) { navBackStackEntry ->
        val userId = navBackStackEntry.arguments?.getString(Screen.Profile.ARG_USER_ID)
        PlaceHolderScreen(
            text = "Profile Screen: $userId", Modifier.testTag(NavigationTestTags.PROFILE_SCREEN))
      }
    }

    navigation(startDestination = Screen.Map.route, route = "map") {
      composable(Screen.Map.route) {
        PlaceHolderScreen(text = "Map Screen", Modifier.testTag(NavigationTestTags.MAP_SCREEN))
      }
    }
  }
}

@Composable
fun PlaceHolderScreen(text: String, modifier: Modifier) {
  Text(text = text, textAlign = TextAlign.Center, modifier = modifier.fillMaxSize().padding(20.dp))
}
