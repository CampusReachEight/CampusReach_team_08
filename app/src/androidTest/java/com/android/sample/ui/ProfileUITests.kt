// Kotlin
package com.android.sample.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.profile.*
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

  @Test
  fun profileScreen_loadingState_showsLoadingIndicator() {
    val loadingState = ProfileState.loading()
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(loadingState)) }
    composeTestRule.onNodeWithTag("profile_loading").assertIsDisplayed()
  }

  @Test
  fun profileScreen_errorState_showsErrorMessage() {
    val errorState = ProfileState.withError()
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(errorState)) }
    composeTestRule.onNodeWithTag("profile_error").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_error").assertTextEquals("Failed to load profile data")
  }

  @Test
  fun emptyProfile_displaysEmptyFields() {
    val emptyState = ProfileState.empty()
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(emptyState)) }
    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals("")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals("None")
  }

  @Test
  fun profileHeader_editButton_isClickable() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithContentDescription("Edit").assertHasClickAction()
  }

  @Test
  fun actionItems_areDisplayedWithCorrectTitles() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithText("Log out").assertIsDisplayed()
    composeTestRule.onNodeWithText("About App").assertIsDisplayed()
  }

  @Test
  fun statGroupCard_displaysCorrectValues() {
    val state = ProfileState(kudosReceived = 99, helpReceived = 88, followers = 77, following = 66)
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(state)) }
    composeTestRule.onNodeWithText("99").assertIsDisplayed()
    composeTestRule.onNodeWithText("88").assertIsDisplayed()
    composeTestRule.onNodeWithText("77").assertIsDisplayed()
    composeTestRule.onNodeWithText("66").assertIsDisplayed()
  }

  @Test
  fun infoRows_displayAllLabels() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithText("Name").assertIsDisplayed()
    composeTestRule.onNodeWithText("Profile Id").assertIsDisplayed()
    composeTestRule.onNodeWithText("Arrival date").assertIsDisplayed()
    composeTestRule.onNodeWithText("Section").assertIsDisplayed()
    composeTestRule.onNodeWithText("Email").assertIsDisplayed()
  }

  @Test
  fun profileScreen_displaysAllSections() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()
  }

  @Test
  fun errorMessage_disappearsWhenNoError() {
    val state = ProfileState.default().copy(errorMessage = null)
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(state)) }
    composeTestRule.onNodeWithTag("profile_error").assertDoesNotExist()
  }

  @Test
  fun loadingIndicator_disappearsWhenNotLoading() {
    val state = ProfileState.default().copy(isLoading = false)
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(state)) }
    composeTestRule.onNodeWithTag("profile_loading").assertDoesNotExist()
  }

  @Test
  fun profileHeader_name_and_email_areDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag("profile_header_name").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_header_email").assertIsDisplayed()
  }

  @Test
  fun profileStats_individualValues_areDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag("profile_stat_top_kudos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_stat_bottom_help_received").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_stat_top_followers").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_stat_bottom_following").assertIsDisplayed()
  }

  @Test
  fun infoRows_individualValues_areDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag("profile_info_name").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_info_profile_id").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_info_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_info_email").assertIsDisplayed()
  }

  @Test
  fun actionItems_individualCards_areDisplayed() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag("profile_action_log_out").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_action_about_app").assertIsDisplayed()
  }
}
