package com.android.sample.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationNoUITests : TestCase() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  lateinit var navigationActions: NavigationActions

  @Before
  public override fun setUp() {
    super.setUp()
    composeTestRule.setContent {
      val navController = rememberNavController()
      navigationActions = NavigationActions(navController)

      NavigationScreen(
          navController = navController,
          navigationActions = navigationActions,
          isSignedInOverride = true)
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
  @Ignore("Doesn't work anymore with emulator fixes")
  fun loginScreenIsDisplayedAtBeginning() {
    assertScreen(NavigationTestTags.LOGIN_SCREEN)
  }

  @Test
  fun canGoToRequestsScreen() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun canGoToLeaderboardScreen() {
    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)
  }

  @Test
  fun canGoToMapScreen() {
    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)
  }

  @Test
  fun canGoToProfile() {
    navigateTo(Screen.Profile("user123"))
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
    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun canNavigateToLeaderboardFromAnyScreen() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)
  }

  @Test
  fun canNavigateToMapFromAnyScreen() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)
  }

  // ========== System back button behavior on tabs ==========
  @Test
  fun pressBackOnLeaderboardReturnsToRequests() {
    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

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
  fun pressBackTwiceFromLeaderboardClosesApp() {
    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

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
  fun goBackOnLeaderboardReturnsToRequests() {
    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

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

  @Ignore("test ignored for now as we can not navigate to request that does not exist")
  @Test
  fun navigateToEditRequestAndGoBack() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.RequestAccept("test"))
    assertScreen(NavigationTestTags.EDIT_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Ignore("test ignored for now as we can not navigate to request that does not exist")
  @Test
  fun pressBackOnEditRequestReturnsToRequests() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.RequestAccept("test"))
    assertScreen(NavigationTestTags.EDIT_REQUEST_SCREEN)

    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  // ========== Complex navigation scenarios ==========
  @Test
  fun complexNavigationBetweenTabs() {
    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

    navigateTo(Screen.Map)
    assertScreen(NavigationTestTags.MAP_SCREEN)

    navigateTo(Screen.Requests)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun navigateToSubScreenSwitchTabsAndBack() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.AddRequest)
    assertScreen(NavigationTestTags.ADD_REQUEST_SCREEN)

    // Switch to Leaderboard tab - should clear AddRequest from stack
    navigateTo(Screen.Leaderboard)
    assertScreen(NavigationTestTags.LEADERBOARD_SCREEN)

    // Go back should go to Requests, not AddRequest
    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Ignore("test ignored for now as we can not navigate to request that does not exist")
  @Test
  fun multipleSubScreenNavigationsAndBack() {
    navigateTo(Screen.Requests)
    navigateTo(Screen.AddRequest)
    assertScreen(NavigationTestTags.ADD_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.RequestAccept("test"))
    assertScreen(NavigationTestTags.EDIT_REQUEST_SCREEN)

    goBack()
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)
  }

  @Test
  fun systemBackMatchesGoBackInAllScenarios() {
    navigateTo(Screen.Leaderboard)
    pressSystemBack(shouldFinish = false)
    assertScreen(NavigationTestTags.REQUESTS_SCREEN)

    navigateTo(Screen.Leaderboard)
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
