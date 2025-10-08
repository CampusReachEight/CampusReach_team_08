package com.android.sample.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.profile.ProfileViewModel
import org.junit.Rule
import org.junit.Test

class ProfileUITests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun profileHeader_isDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
  }

  @Test
  fun profileStats_areDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
  }

  @Test
  fun profileInformation_isDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
  }

  @Test
  fun profileActions_areDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()
  }

  @Test
  fun customProfile_isDisplayedCorrectly() {
    val customState =
        ProfileState(
            userName = "Alice Smith",
            userEmail = "alice.smith@epfl.ch",
            profileId = "999999",
            kudosReceived = 42,
            helpReceived = 17,
            followers = 8,
            following = 15,
            arrivalDate = "15/02/2024",
            section = "Physics")
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(customState)) }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_header_name").assertTextEquals("Alice Smith")
    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals("Alice Smith")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals("Physics")
  }
}
