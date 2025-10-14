package com.android.sample.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.MainActivity
import com.android.sample.ui.authentication.SignInScreenTestTags
import org.junit.Rule
import org.junit.Test

class ProfileLogoutTests {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun profileScreen_showsLogoutButton() {
    // Simulate login
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Navigate to profile tab (replace with your actual test tag for the profile tab)
    composeTestRule.onNodeWithTag("profile_tab").performClick()
    composeTestRule.waitForIdle()

    // Now assert the logout button is displayed
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).assertIsDisplayed()
  }

  @Test
  fun clickingLogoutButton_showsLogoutDialog() {
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.onNodeWithText("Are you sure you want to log out?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Log out").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun cancelLogoutDialog_hidesDialog() {
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithText("Are you sure you want to log out?").assertDoesNotExist()
  }

  @Test
  fun confirmLogout_showsLoadingIndicator_andNavigatesToLogin() {
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.onNodeWithText("Log out").performClick()
    composeTestRule.onNodeWithTag("profile_loading").assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun loadingIndicator_disappearsAfterLogout() {
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.onNodeWithText("Log out").performClick()
    // Wait for navigation to login screen
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    // Loading indicator should not be visible anymore
    composeTestRule.onNodeWithTag("profile_loading").assertIsNotDisplayed()
  }

  @Test
  fun logoutDialog_notShownByDefault() {
    composeTestRule.onNodeWithText("Are you sure you want to log out?").assertIsNotDisplayed()
  }
}
