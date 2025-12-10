package com.android.sample.navigation

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.request.accepted.AcceptedRequestsTestTags
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FirebaseEmulator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTests : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var navigationActions: NavigationActions

  @Before
  override fun setUp() = runTest {
    super.setUp()
    // Sign in to bypass login screen
    FirebaseEmulator.signInTestUser()

    composeTestRule.setContent {
      val navController = rememberNavController()
      navigationActions = NavigationActions(navController)
      NavigationScreen(navController = navController, navigationActions = navigationActions)
    }

    // Wait for initial screen to load
    composeTestRule.waitForIdle()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun initialScreenIsRequestsScreen() {
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canNavigateToEventsScreen() {
    // Click on Events tab
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()

    // Verify Events screen is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canNavigateToMapScreen() {
    // Click on Map tab
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()

    // Verify Map screen is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canNavigateToRequestsScreenFromEventsTab() {
    // Navigate to Events
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate back to Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
    composeTestRule.waitForIdle()

    // Verify Requests screen is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canNavigateToRequestsScreenFromMapTab() {
    // Navigate to Map
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()

    // Navigate back to Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
    composeTestRule.waitForIdle()

    // Verify Requests screen is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canNavigateBetweenAllTabs() {
    // Start at Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Events
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Map
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()

    // Navigate back to Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Events again
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()
  }

  @Test
  fun navigateToAddRequestScreenFromRequestsTab() {
    // Start at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Request screen programmatically
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddRequest) }
    composeTestRule.waitForIdle()

    // Verify Add Request screen is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_REQUEST_SCREEN).assertIsDisplayed()
  }

  @Test
  fun navigateToAddEventScreenFromEventsTab() {
    // Navigate to Events tab
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Event screen programmatically
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddEvent) }
    composeTestRule.waitForIdle()

    // Verify Add Event screen is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_EVENT_SCREEN).assertIsDisplayed()
  }

  @Test
  fun goBackFromAddRequestScreenReturnsToRequestsTab() {
    // Start at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Request screen
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddRequest) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_REQUEST_SCREEN).assertIsDisplayed()

    // Go back using navigation action
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()

    // Verify we're back at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun goBackFromProfileScreenReturnsToPreviousTab() {
    // Start at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()

    // Go back using navigation action
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()

    // Verify we're back at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun goBackFromAddEventScreenReturnsToEventsTab() {
    // Navigate to Events tab
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Event screen
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddEvent) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_EVENT_SCREEN).assertIsDisplayed()

    // Go back using navigation action
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()

    // Verify we're back at Events screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun navigateToSubScreenFromDifferentTabAndReturn() {
    // Start at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Map tab
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()

    // Navigate to Add Request screen (from Map tab)
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddRequest) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_REQUEST_SCREEN).assertIsDisplayed()

    // Go back
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()

    // Should return to Map screen
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun multipleNavigationsAndGoBackMaintainCorrectStack() {
    // Start at Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Request
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddRequest) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_REQUEST_SCREEN).assertIsDisplayed()

    // Go back to Requests
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Events
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Event
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddEvent) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_EVENT_SCREEN).assertIsDisplayed()

    // Go back to Events
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Go back to Requests (root)
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationBarIsVisibleOnMainScreens() {
    // Check Requests screen has bottom bar
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()

    // Check Events screen has bottom bar
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()

    // Check Map screen has bottom bar
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationBarIsNotVisibleOnSubScreens() {
    // Navigate to Add Request screen
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddRequest) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_REQUEST_SCREEN).assertIsDisplayed()

    // Bottom bar should not exist
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertDoesNotExist()
  }

  @Test
  fun rapidTabSwitchingWorksCorrectly() {
    // Rapidly switch between tabs
    repeat(3) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
      composeTestRule.waitForIdle()
    }

    // Should end up at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun navigatingToSameTabDoesNothing() {
    // Start at Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Click Requests tab again
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
    composeTestRule.waitForIdle()

    // Should still be at Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun allTabButtonsAreClickableAndDisplayed() {
    // Verify all tab buttons are displayed and clickable
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()

    // Try clicking each one
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun complexNavigationScenario() {
    // Start at Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Go to Map
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()

    // Go to Events
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENT_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Event
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddEvent) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_EVENT_SCREEN).assertIsDisplayed()

    // Go back to Events
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EVENTS_SCREEN).assertIsDisplayed()

    // Navigate to Requests via tab
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Add Request
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AddRequest) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_REQUEST_SCREEN).assertIsDisplayed()

    // Go back to Requests
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }

  @Test
  fun navigateToAcceptedRequestsScreenFromProfile() {
    // Start at Requests screen
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Navigate to Profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()

    // Navigate to AcceptedRequests screen programmatically
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AcceptedRequests) }
    composeTestRule.waitForIdle()

    // Verify AcceptedRequests screen is displayed
    composeTestRule.onNodeWithTag(AcceptedRequestsTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun goBackFromAcceptedRequestsScreenReturnsToProfile() {
    // Navigate to Profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()

    // Navigate to AcceptedRequests screen
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AcceptedRequests) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AcceptedRequestsTestTags.SCREEN).assertIsDisplayed()

    // Go back using navigation action
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()

    // Verify we're back at Profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()
  }

  @Test
  fun complexNavigationWithAcceptedRequests() {
    // Start at Requests
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()

    // Go to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()

    // Go to AcceptedRequests
    composeTestRule.runOnUiThread { navigationActions.navigateTo(Screen.AcceptedRequests) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AcceptedRequestsTestTags.SCREEN).assertIsDisplayed()

    // Go back to Profile
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()

    // Go back to Requests
    composeTestRule.runOnUiThread { navigationActions.goBack() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN).assertIsDisplayed()
  }
}
