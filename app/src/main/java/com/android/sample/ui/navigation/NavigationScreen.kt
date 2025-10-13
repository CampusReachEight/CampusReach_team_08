package com.android.sample.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.sample.ui.authentication.SignInScreen
import com.android.sample.ui.authentication.SignInViewModel
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapViewModel
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.profile.ProfileViewModel
import com.android.sample.ui.theme.BottomNavigationMenu
import com.android.sample.ui.theme.NavigationTab
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    navigationActions: NavigationActions = NavigationActions(navController)
) {

  val user = FirebaseAuth.getInstance().currentUser
  var isSignedIn by rememberSaveable { mutableStateOf(user != null) }
  val startDestination = if (!isSignedIn) "login" else "requests"

  // ViewModels
  val signInViewModel: SignInViewModel = SignInViewModel()
  val profileViewModel: ProfileViewModel = ProfileViewModel()
  val mapViewModel: MapViewModel = MapViewModel()

  NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = modifier,
  ) {
    navigation(startDestination = Screen.Login.route, route = "login") {
      composable(Screen.Login.route) {
        SignInScreen(viewModel = signInViewModel, onSignInSuccess = { isSignedIn = true })
      }
    }

    navigation(startDestination = Screen.Requests.route, route = "requests") {
      composable(Screen.Requests.route) {
        PlaceHolderScreen(
            text = "Requests Screen",
            modifier = Modifier.testTag(NavigationTestTags.REQUESTS_SCREEN),
            withBottomBar = true,
            navigationActions = navigationActions,
            defaultTab = NavigationTab.Requests)
      }
      composable(Screen.AddRequest.route) {
        PlaceHolderScreen(
            text = "Add Request Screen",
            modifier = Modifier.testTag(NavigationTestTags.ADD_REQUEST_SCREEN),
            withBottomBar = true)
      }
      composable(Screen.RequestDetails.route) { navBackStackEntry ->
        val requestId = navBackStackEntry.arguments?.getString(Screen.RequestDetails.ARG_REQUEST_ID)
        PlaceHolderScreen(
            text = "Edit Request Screen: $requestId",
            modifier = Modifier.testTag(NavigationTestTags.EDIT_REQUEST_SCREEN),
            withBottomBar = false,
        )
      }
    }

    navigation(startDestination = Screen.Events.route, route = "events") {
      composable(Screen.Events.route) {
        PlaceHolderScreen(
            text = "Events Screen",
            modifier = Modifier.testTag(NavigationTestTags.EVENTS_SCREEN),
            withBottomBar = true,
            navigationActions = navigationActions,
            defaultTab = NavigationTab.Events)
      }
      composable(Screen.AddEvent.route) {
        PlaceHolderScreen(
            text = "Add Event Screen",
            modifier = Modifier.testTag(NavigationTestTags.ADD_EVENT_SCREEN),
            withBottomBar = false)
      }
      composable(Screen.EventDetails.route) { navBackStackEntry ->
        val eventId = navBackStackEntry.arguments?.getString(Screen.EventDetails.ARG_EVENT_ID)
        PlaceHolderScreen(
            text = "Edit Event Screen : $eventId",
            modifier = Modifier.testTag(NavigationTestTags.EDIT_EVENT_SCREEN),
            withBottomBar = false)
      }
    }

    navigation(startDestination = Screen.Profile.route, route = "profile") {
      composable(Screen.Profile.route) { navBackStackEntry ->
        val userId = navBackStackEntry.arguments?.getString(Screen.Profile.ARG_USER_ID)
        ProfileScreen(viewModel = profileViewModel, onBackClick = { navigationActions.goBack() })
      }
    }

    navigation(startDestination = Screen.Map.route, route = "map") {
      composable(Screen.Map.route) {
        MapScreen(viewModel = mapViewModel, navigationActions = navigationActions)
      }
    }
  }
}

@Composable
fun PlaceHolderScreen(
    text: String,
    withBottomBar: Boolean,
    modifier: Modifier = Modifier,
    defaultTab: NavigationTab? = null,
    navigationActions: NavigationActions? = null,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      bottomBar = {
        if (!withBottomBar) {
          return@Scaffold
        }
        defaultTab?.let {
          BottomNavigationMenu(selectedNavigationTab = it, navigationActions = navigationActions)
        }
      }) { padding ->
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize().padding(20.dp))
      }
}
