package com.android.sample.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTestNoUI : TestCase() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  lateinit var navigationActions: Screen.NavigationActions

  @Before
  public override fun setUp() {
    super.setUp()
    composeTestRule.setContent {
      val navController = rememberNavController()
      navigationActions = Screen.NavigationActions(navController)

      NavigationScreen(navController = navController, navigationActions = navigationActions)
    }
  }

  // Helper functions
  private fun navigateTo(screen: Screen) {
    composeTestRule.runOnIdle { navigationActions.navigateTo(screen) }
    composeTestRule.waitForIdle()
  }

  private fun goBack() {
    composeTestRule.runOnIdle { navigationActions.goBack() }
    composeTestRule.waitForIdle()
  }

  private fun pressSystemBack(shouldFinish: Boolean = false) {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    if (shouldFinish) {
      composeTestRule.waitUntil(timeoutMillis = 2000) { composeTestRule.activity.isFinishing }
    }
    Assert.assertEquals(shouldFinish, composeTestRule.activity.isFinishing)
  }

  private fun assertScreen(tag: String) {
    composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
  }

  // ========== Basic Navigation Tests ==========
  @Test
  fun loginScreenIsDisplayedAtBeginning() {
    assertScreen(NavigationTestTags.LOGIN_SCREEN)
  }

  @Test
  fun canGoToRequestsScreen() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun canGoToEventsScreen() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)
  }

  @Test
  fun canGoToMapScreen() {
    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)
  }

  @Test
  fun canGoToProfile() {
    navigateTo(Screen.Profile)
    assertScreen(NavigationTestTags.PROFILE_SCREEN)
  }

  @Test
  fun navigateToCurrentRouteDoesNothing() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  // ========== Navigate to tabs at any time ==========
  @Test
  fun canNavigateToRequestsFromAnyScreen() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun canNavigateToEventsFromAnyScreen() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)
  }

  @Test
  fun canNavigateToMapFromAnyScreen() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)
  }

  // ========== System back button behavior on tabs ==========
  @Test
  fun pressBackOnEventsReturnsToRequests() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun pressBackOnMapReturnsToRequests() {
    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun pressBackOnRequestsClosesApp() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    pressSystemBack(shouldFinish = true)
  }

  @Test
  fun pressBackTwiceFromEventsClosesApp() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    pressSystemBack(shouldFinish = true)
  }

  @Test
  fun pressBackTwiceFromMapClosesApp() {
    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    pressSystemBack(shouldFinish = true)
  }

  // ========== goBack() matches system back on tabs ==========
  @Test
  fun goBackOnEventsReturnsToRequests() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun goBackOnMapReturnsToRequests() {
    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun navigateToAddRequestAndGoBack() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.AddRequest)
    assertScreen(NavigationTestTags.ADD_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun pressBackOnAddRequestReturnsToRequests() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.AddRequest)
    assertScreen(NavigationTestTags.ADD_REQUEST_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun navigateToEditRequestAndGoBack() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.RequestDetails("123"))
    assertScreen(NavigationTestTags.EDIT_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun pressBackOnEditRequestReturnsToRequests() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.RequestDetails("456"))
    assertScreen(NavigationTestTags.EDIT_REQUEST_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  // ========== Sub-screens from Events ==========
  @Test
  fun navigateToAddEventAndGoBack() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    navigateTo(Screen.AddEvent)
    assertScreen(NavigationTestTags.ADD_EVENT_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.EVENTS_SCREEN)
  }

  @Test
  fun pressBackOnAddEventReturnsToEvents() {
    navigateTo(Screen.Events)
    navigateTo(Screen.AddEvent)
    assertScreen(NavigationTestTags.ADD_EVENT_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)
  }

  @Test
  fun navigateFromAddEventBackToEventsThenRequests() {
    navigateTo(Screen.Events)
    navigateTo(Screen.AddEvent)
    assertScreen(NavigationTestTags.ADD_EVENT_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun pressBackFromAddEventToEventsThenRequestsThenClosesApp() {
    navigateTo(Screen.Events)
    navigateTo(Screen.AddEvent)
    assertScreen(NavigationTestTags.ADD_EVENT_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    pressSystemBack(shouldFinish = true)
  }

  @Test
  fun navigateToEditEventAndGoBack() {
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    navigateTo(Screen.EventDetails("event123"))
    assertScreen(NavigationTestTags.EDIT_EVENT_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.EVENTS_SCREEN)
  }

  // ========== Complex navigation scenarios ==========
  @Test
  fun complexNavigationBetweenTabs() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun navigateToSubScreenSwitchTabsAndBack() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.AddRequest)
    assertScreen(NavigationTestTags.ADD_REQUEST_SCREEN)

    // Switch to Events tab - should clear AddRequest from stack
    navigateTo(Screen.Events)
    assertScreen(NavigationTestTags.EVENTS_SCREEN)

    // Go back should go to Requests, not AddRequest
    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun multipleSubScreenNavigationsAndBack() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.AddRequest)
    assertScreen(NavigationTestTags.ADD_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.RequestDetails("test123"))
    assertScreen(NavigationTestTags.EDIT_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun navigateFromEventsSubScreenToMapTab() {
    navigateTo(Screen.Events)
    navigateTo(Screen.AddEvent)
    assertScreen(NavigationTestTags.ADD_EVENT_SCREEN)

    // Navigate to Map from sub-screen
    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    // Back should go to Requests (not to AddEvent)
    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun systemBackMatchesGoBackInAllScenarios() {
    navigateTo(Screen.Events)
    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Events)
    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Map)
    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Map)
    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.AddRequest)
    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.AddRequest)
    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }
}
