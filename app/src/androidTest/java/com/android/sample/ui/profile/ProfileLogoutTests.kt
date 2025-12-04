package com.android.sample.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.utils.BaseEmulatorTest
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class ProfileLogoutTests : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: UserProfileRepositoryFirestore
  private lateinit var viewModel: ProfileViewModel

  @Before
  override fun setUp() {
    viewModel =
        ProfileViewModel(
            initialState = ProfileState.default(),
        )
    composeTestRule.setContent {
      ProfileScreen(
          viewModel,
      )
    }
    composeTestRule.waitForIdle()
  }

  //    @Test
  //  fun profileScreen_showsLogoutButton() {
  //    // Simulate login
  //    composeTestRule.waitForIdle()
  //    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // Navigate to profile tab (replace with your actual test tag for the profile tab)
  //    composeTestRule.onNodeWithTag("profile_tab").performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // Now assert the logout button is displayed
  //    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).assertIsDisplayed()
  //  }

  @Ignore("Flaky test on CI")
  @Test
  fun clickingLogoutButton_showsLogoutDialog() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertIsDisplayed()
  }

  @Ignore("Flaky test on CI")
  @Test
  fun cancelLogoutDialog_hidesDialog() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL).performClick()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertDoesNotExist()
  }

  @Test
  fun logoutDialog_notShownByDefault() {
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertDoesNotExist()
  }

  @Test
  fun confirmLogout_signsOutUser() {
    composeTestRule.waitForIdle()

    // Scroll to the logout button to ensure it's visible
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Confirm logout in the dialog
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()

    // Wait for logout to complete (give it more time)
    Thread.sleep(500) // Allow time for Firebase to process logout
    composeTestRule.waitForIdle()

    // Assert Firebase user is signed out
    assertNull(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser)
  }
}
