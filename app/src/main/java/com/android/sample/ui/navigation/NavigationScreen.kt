package com.android.sample.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation

// TODO: implement login logic for starting destination
@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    navigationActions: NavigationActions = NavigationActions(navController),
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
) {
  NavHost(
      navController = navController,
      startDestination = Screen.Login.route,
      modifier = modifier,
  ) {
    navigation(startDestination = Screen.Login.route, route = Screen.Login.route) {
      composable(Screen.Login.route) {
        PlaceHolderScreen(text = "Login Screen", Modifier.testTag(NavigationTestTags.LOGIN_SCREEN))
      }
    }

    navigation(startDestination = Screen.Requests.route, route = Screen.Requests.route) {
      composable(Screen.Requests.route) {
        PlaceHolderScreen(
            text = "Requests Screen", Modifier.testTag(NavigationTestTags.REQUESTS_SCREEN))
      }
      composable(Screen.AddRequest.route) {
        PlaceHolderScreen(
            text = "Add Request Screen", Modifier.testTag(NavigationTestTags.ADD_EVENT_SCREEN))
      }
      composable(Screen.EditRequest.route) { navBackStackEntry ->
        val requestId = navBackStackEntry.arguments?.getString(Screen.EditRequest.ARG_REQUEST_ID)
        PlaceHolderScreen(
            text = "Edit Request Screen: $requestId",
            Modifier.testTag(NavigationTestTags.EDIT_REQUEST_SCREEN))
      }
    }

    navigation(startDestination = Screen.Events.route, route = Screen.Events.route) {
      composable(Screen.Events.route) {
        PlaceHolderScreen(
            text = "Events Screen", Modifier.testTag(NavigationTestTags.EVENTS_SCREEN))
      }
      composable(Screen.AddEvent.route) {
        PlaceHolderScreen(
            text = "Add Event Screen", Modifier.testTag(NavigationTestTags.ADD_EVENT_SCREEN))
      }
      composable(Screen.EditEvent.route) { navBackStackEntry ->
        val eventId = navBackStackEntry.arguments?.getString(Screen.EditEvent.ARG_EVENT_ID)
        PlaceHolderScreen(
            text = "Edit Event Screen : $eventId",
            Modifier.testTag(NavigationTestTags.EDIT_EVENT_SCREEN))
      }
    }

    navigation(startDestination = Screen.Profile.route, route = Screen.Profile.route) {
      composable(Screen.Profile.route) {
        PlaceHolderScreen(
            text = "Profile Screen", Modifier.testTag(NavigationTestTags.PROFILE_SCREEN))
      }
    }

    navigation(startDestination = Screen.Map.route, route = Screen.Map.route) {
      composable(Screen.Map.route) {
        PlaceHolderScreen(text = "Map Screen", Modifier.testTag(NavigationTestTags.MAP_SCREEN))
      }
    }
  }
}

@Composable
fun PlaceHolderScreen(text: String, modifier: Modifier = Modifier) {
  Text(text = text, textAlign = TextAlign.Center, modifier = modifier.fillMaxSize().padding(20.dp))
}
