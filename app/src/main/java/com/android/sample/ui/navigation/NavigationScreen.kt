package com.android.sample.ui.navigation

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.android.sample.model.map.FusedLocationProvider
import com.android.sample.model.map.NominatimLocationRepository
import com.android.sample.model.profile.UserProfileCache
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.RequestCache
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.ui.authentication.SignInScreen
import com.android.sample.ui.authentication.SignInViewModel
import com.android.sample.ui.authentication.SignInViewModelFactory
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapViewModel
import com.android.sample.ui.map.MapViewModelFactory
import com.android.sample.ui.overview.AcceptRequestScreen
import com.android.sample.ui.overview.AcceptRequestViewModel
import com.android.sample.ui.overview.AcceptRequestViewModelFactory
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.profile.ProfileViewModel
import com.android.sample.ui.profile.publicProfile.PublicProfileScreen
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModel
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModelFactory
import com.android.sample.ui.request.RequestListScreen
import com.android.sample.ui.request.RequestListViewModel
import com.android.sample.ui.request.RequestListViewModelFactory
import com.android.sample.ui.request.accepted.AcceptedRequestsScreen
import com.android.sample.ui.request.edit.EditRequestScreen
import com.android.sample.ui.request.edit.EditRequestViewModel
import com.android.sample.ui.request.edit.EditRequestViewModelFactory
import com.android.sample.ui.request_validation.ValidateRequestCallbacks
import com.android.sample.ui.request_validation.ValidateRequestScreen
import com.android.sample.ui.request_validation.ValidateRequestViewModel
import com.android.sample.ui.request_validation.ValidateRequestViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import okhttp3.OkHttpClient

@SuppressLint("ViewModelConstructorInComposable")
@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    navigationActions: NavigationActions = NavigationActions(navController),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current)
) {
  val context = LocalContext.current

  // caches
  val requestCache = RequestCache(context)
  val profileCache = UserProfileCache(context)

  val user = FirebaseAuth.getInstance().currentUser
  var isSignedIn by rememberSaveable { mutableStateOf(user != null) }
  val startDestination = if (!isSignedIn) "login" else "requests"

  // repositories
  val requestRepository = RequestRepositoryFirestore(Firebase.firestore)
  val locationRepository = NominatimLocationRepository(client = OkHttpClient())
  val fusedLocationProvider = FusedLocationProvider(LocalContext.current)
  val userProfileRepository = UserProfileRepositoryFirestore(Firebase.firestore)

  // ViewModels
  val signInViewModel: SignInViewModel =
      viewModel(
          factory =
              SignInViewModelFactory(
                  profileRepository = userProfileRepository, profileCache = profileCache))
  val mapViewModel: MapViewModel =
      viewModel(
          factory =
              MapViewModelFactory(
                  requestRepository = requestRepository,
                  profileRepository = userProfileRepository,
                  locationProvider = fusedLocationProvider))
  val requestListViewModel: RequestListViewModel =
      viewModel(
          factory =
              RequestListViewModelFactory(showOnlyMyRequests = false, requestCache = requestCache))
  val editRequestViewModel: EditRequestViewModel =
      viewModel(
          factory =
              EditRequestViewModelFactory(
                  requestRepository = requestRepository,
                  locationRepository = locationRepository,
                  locationProvider = fusedLocationProvider))

  val acceptRequestViewModel: AcceptRequestViewModel =
      viewModel(
          factory =
              AcceptRequestViewModelFactory(
                  requestRepository = requestRepository,
                  userProfileRepository = UserProfileRepositoryFirestore(Firebase.firestore),
                  requestCache = requestCache))

  NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = modifier,
  ) {
    navigation(startDestination = Screen.Login.route, route = "login") {
      composable(Screen.Login.route) {
        SignInScreen(
            viewModel = signInViewModel,
            onSignInSuccess = { isSignedIn = true },
            credentialManager = credentialManager)
      }
    }

    navigation(startDestination = Screen.Requests.route, route = "requests") {
      composable(Screen.Requests.route) {
        RequestListScreen(
            requestListViewModel = requestListViewModel, navigationActions = navigationActions)
      }
      composable(Screen.AddRequest.route) {
        EditRequestScreen(
            requestId = null, // launch in add mode
            onNavigateBack = { navigationActions.goBack() },
            viewModel = editRequestViewModel)
      }
      composable(Screen.RequestAccept.route) { navBackStackEntry ->
        val requestId = navBackStackEntry.arguments?.getString(Screen.RequestAccept.ARG_REQUEST_ID)
        requestId?.let { id ->
          AcceptRequestScreen(
              requestId = id,
              onGoBack = { navigationActions.goBack() },
              onValidateClick = { requestIdToValidate ->
                navigationActions.navigateTo(Screen.ValidateRequest(requestIdToValidate))
              },
              onEditClick = { requestIdForEdit ->
                navigationActions.navigateTo(Screen.EditRequest(requestIdForEdit))
              },
              acceptRequestViewModel = acceptRequestViewModel,
              onMapClick = { requestIdForMap ->
                navigationActions.navigateTo(Screen.Map(requestIdForMap))
              })
        }
      }
      composable(Screen.ValidateRequest.route) { navBackStackEntry ->
        val requestId = navBackStackEntry.arguments?.getString(Screen.RequestAccept.ARG_REQUEST_ID)
        requestId?.let { id ->
          val validateRequestViewModel: ValidateRequestViewModel =
              viewModel(
                  factory =
                      ValidateRequestViewModelFactory(
                          requestId = id,
                          requestRepository = requestRepository,
                          userProfileRepository = userProfileRepository))
          ValidateRequestScreen(
              state = validateRequestViewModel.state,
              userProfileRepository = userProfileRepository,
              callbacks =
                  ValidateRequestCallbacks(
                      onToggleHelper = { userId ->
                        validateRequestViewModel.toggleHelperSelection(userId)
                      },
                      onShowConfirmation = { validateRequestViewModel.showConfirmation() },
                      onCancelConfirmation = { validateRequestViewModel.cancelConfirmation() },
                      onConfirmAndClose = { validateRequestViewModel.confirmAndClose() },
                      onRetry = { validateRequestViewModel.retry() },
                      onRequestClosed = { navigationActions.navigateTo(Screen.Requests) },
                      onNavigateBack = { navigationActions.goBack() }),
              modifier = Modifier.testTag(NavigationTestTags.VALIDATE_REQUEST_SCREEN))
        }
      }
      composable(Screen.EditRequest.route) { navBackStackEntry ->
        val requestId = navBackStackEntry.arguments?.getString(Screen.EditRequest.ARG_REQUEST_ID)
        EditRequestScreen(
            requestId = requestId,
            onNavigateBack = { navigationActions.goBack() },
            viewModel = editRequestViewModel)
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
        ProfileScreen(
            viewModel =
                ProfileViewModel(
                    profileCache = profileCache,
                    onLogout = {
                      isSignedIn = false
                      navController.navigate(Screen.Login.route) {
                        popUpTo(0) // Clears the back stack
                      }
                    }),
            onBackClick = { navigationActions.goBack() },
            navigationActions = navigationActions)
      }
      composable(Screen.MyRequest.route) {
        RequestListScreen(showOnlyMyRequests = true, navigationActions = navigationActions)
      }
      composable(Screen.PublicProfile.route) { navBackStackEntry ->
        val userId = navBackStackEntry.arguments?.getString(Screen.PublicProfile.ARG_USER_ID)
        userId?.let { id ->
          val publicProfileViewModel: PublicProfileViewModel =
              viewModel(factory = PublicProfileViewModelFactory(userProfileRepository))
          PublicProfileScreen(
              viewModel = publicProfileViewModel,
              defaultProfileId = id,
              onBackClick = { navigationActions.goBack() })
        }
      }
      composable(Screen.AcceptedRequests.route) {
        AcceptedRequestsScreen(navigationActions = navigationActions)
      }
    }

    navigation(startDestination = "map/main", route = "map") {
      composable(
          route = Screen.Map.ROUTE,
          arguments =
              listOf(
                  navArgument(Screen.Map.ARG_REQUEST_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                  })) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString(Screen.Map.ARG_REQUEST_ID)
            MapScreen(
                viewModel = mapViewModel,
                navigationActions = navigationActions,
                requestId = requestId)
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
      topBar = {
        defaultTab?.let {
          TopNavigationBar(
              selectedTab = defaultTab,
              onProfileClick = { navigationActions?.navigateTo(Screen.Profile("TODO")) },
              navigationActions = navigationActions)
        }
      },
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
