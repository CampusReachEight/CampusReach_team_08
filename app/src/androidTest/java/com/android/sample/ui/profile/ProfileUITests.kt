package com.android.sample.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.profile.composables.ErrorBanner
import com.android.sample.ui.profile.composables.LoadingIndicator
import com.android.sample.ui.profile.composables.LogoutDialog
import com.android.sample.ui.profile.composables.ProfileActions
import com.android.sample.ui.profile.composables.ProfileContent
import com.android.sample.ui.profile.composables.ProfileHeader
import com.android.sample.ui.profile.composables.ProfileInformation
import com.android.sample.ui.profile.composables.ProfileStats
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileUiTests {

  @get:Rule val composeTestRule = createComposeRule()

  // ----- ProfileScreen high-level composition -----
  @Test
  fun profileScreen_displaysAllSections() {
    composeTestRule.setContent { ProfileScreen() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
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

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME)
        .assertTextEquals("Alice Smith")
    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals("Alice Smith")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals("Physics")
  }

  // ----- Header -----
  @Test
  fun profileHeader_name_and_email_and_picture_areDisplayed() {
    val state = ProfileState.default().copy(userName = "Bob", userEmail = "bob@ex.com")
    composeTestRule.setContent { ProfileHeader(state = state) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Profile Picture").assertIsDisplayed()
  }

  @Test
  fun profileHeader_editButton_invokesCallback() {
    var clicked = false
    val state = ProfileState.default()
    composeTestRule.setContent { ProfileHeader(state = state, onEditClick = { clicked = true }) }

    composeTestRule.onNodeWithContentDescription("Edit").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    composeTestRule.runOnIdle { assertTrue(clicked) }
  }

  // ----- Stats -----
  @Test
  fun profileStats_displaysValues_and_tags() {
    val s = ProfileState(kudosReceived = 12, helpReceived = 34, followers = 56, following = 78)
    composeTestRule.setContent { ProfileStats(state = s) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_KUDOS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING).assertIsDisplayed()

    composeTestRule.onNodeWithText("12").assertIsDisplayed()
    composeTestRule.onNodeWithText("34").assertIsDisplayed()
    composeTestRule.onNodeWithText("56").assertIsDisplayed()
    composeTestRule.onNodeWithText("78").assertIsDisplayed()
  }

  @Test
  fun statGroupCard_displaysCorrectValues_inProfileScreen() {
    val state = ProfileState(kudosReceived = 99, helpReceived = 88, followers = 77, following = 66)
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(state)) }
    composeTestRule.onNodeWithText("99").assertIsDisplayed()
    composeTestRule.onNodeWithText("88").assertIsDisplayed()
    composeTestRule.onNodeWithText("77").assertIsDisplayed()
    composeTestRule.onNodeWithText("66").assertIsDisplayed()
  }

  // ----- Information -----
  @Test
  fun infoRows_displayAllLabels_and_values() {
    val state =
        ProfileState(
            userName = "Alice",
            userEmail = "alice@ex.com",
            profileId = "ID123",
            arrivalDate = "01/01/2020",
            section = "Math")
    composeTestRule.setContent { ProfileInformation(state = state) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Information").assertIsDisplayed()

    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals("Alice")
    composeTestRule.onNodeWithTag("profile_info_profile_id").assertTextEquals("ID123")
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertTextEquals("01/01/2020")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals("Math")
    composeTestRule.onNodeWithTag("profile_info_email").assertTextEquals("alice@ex.com")
  }

  // ----- Actions -----
  @Test
  fun profileActions_displaysItems_and_titles() {
    composeTestRule.setContent { ProfileActions() }
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()
    composeTestRule.onNodeWithText("Log out").assertIsDisplayed()
    composeTestRule.onNodeWithText("About App").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_ABOUT_APP).assertIsDisplayed()
  }

  @Test
  fun logout_action_triggersCallback() {
    var triggered = false
    composeTestRule.setContent { ProfileActions(onLogoutClick = { triggered = true }) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).assertHasClickAction()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.runOnIdle { assertTrue(triggered) }
  }

  // ----- Dialogues -----
  @Test
  fun logoutDialog_showsTitle_message_and_buttons_whenVisible() {
    composeTestRule.setContent { LogoutDialog(visible = true, onConfirm = {}, onDismiss = {}) }

    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL).assertIsDisplayed()
    composeTestRule.onNodeWithText("Log out").assertIsDisplayed()
    composeTestRule.onNodeWithText("Are you sure you want to log out?").assertIsDisplayed()
  }

  @Test
  fun logoutDialog_confirmButton_callsOnConfirm() {
    var confirmed = false
    composeTestRule.setContent {
      LogoutDialog(visible = true, onConfirm = { confirmed = true }, onDismiss = {})
    }
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM).performClick()
    composeTestRule.runOnIdle { assertTrue(confirmed) }
  }

  @Test
  fun logoutDialog_cancelButton_callsOnDismiss_and_hiddenWhenNotVisible() {
    var dismissed = false
    composeTestRule.setContent {
      LogoutDialog(visible = true, onConfirm = {}, onDismiss = { dismissed = true })
    }
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL).performClick()
    composeTestRule.runOnIdle { assertTrue(dismissed) }

    composeTestRule.setContent { LogoutDialog(visible = false, onConfirm = {}, onDismiss = {}) }
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertIsNotDisplayed()
  }

  // ----- Loading & Error -----
  @Test
  fun loadingIndicator_displaysWhenComposed() {
    composeTestRule.setContent { LoadingIndicator() }
    composeTestRule.onNodeWithTag(ProfileTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun profileScreen_loadingState_showsLoadingIndicator_and_disappearsWhenNotLoading() {
    val loadingState = ProfileState.loading()
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(loadingState)) }
    composeTestRule.onNodeWithTag("profile_loading").assertIsDisplayed()

    val normal = ProfileState.default().copy(isLoading = false, errorMessage = null)
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(normal)) }
    composeTestRule.onNodeWithTag("profile_loading").assertIsNotDisplayed()
  }

  @Test
  fun errorBanner_and_profileScreen_errorState_showsMessage_and_disappears() {
    composeTestRule.setContent { ErrorBanner(message = "Network failed") }
    composeTestRule.onNodeWithTag("profile_error").assertIsDisplayed()
    composeTestRule.onNodeWithText("Network failed").assertIsDisplayed()

    val errorState = ProfileState.withError()
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(errorState)) }
    composeTestRule.onNodeWithTag("profile_error").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_error").assertTextEquals("Failed to load profile data")

    val noError = ProfileState.default().copy(errorMessage = null)
    composeTestRule.setContent { ProfileScreen(viewModel = ProfileViewModel(noError)) }
    composeTestRule.onNodeWithTag("profile_error").assertIsNotDisplayed()
  }

  // ----- ProfileContent integration -----
  @Test
  fun profileContent_composesAllSections_and_actionClick_propagates() {
    var logoutRequested = false
    val s = ProfileState.default()
    composeTestRule.setContent {
      ProfileContent(state = s, onLogoutRequested = { logoutRequested = true })
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.runOnIdle { assertTrue(logoutRequested) }
  }
}
