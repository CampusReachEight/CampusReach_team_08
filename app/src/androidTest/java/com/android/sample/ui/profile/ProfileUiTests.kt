package com.android.sample.ui.profile

import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.profile.composables.ErrorBanner
import com.android.sample.ui.profile.composables.InfoRow
import com.android.sample.ui.profile.composables.LoadingIndicator
import com.android.sample.ui.profile.composables.LogoutDialog
import com.android.sample.ui.profile.composables.ProfileActions
import com.android.sample.ui.profile.composables.ProfileContent
import com.android.sample.ui.profile.composables.ProfileHeader
import com.android.sample.ui.profile.composables.ProfileInformation
import com.android.sample.ui.profile.composables.ProfileStats
import com.android.sample.ui.profile.composables.ProfileTopBar
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            userSection = "Physics")
    composeTestRule.setContent {
      ProfileScreen(viewModel = ProfileViewModel(customState, attachAuthListener = false))
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME)
        .assertTextContains("Alice Smith")
    composeTestRule.onNodeWithTag("profile_info_name").assertTextContains("Alice Smith")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextContains("Physics")
  }

  // ----- Header -----
  @Test
  fun profileHeader_name_and_email_and_picture_areDisplayed() {
    val state = ProfileState.default().copy(userName = "Bob", userEmail = "bob@ex.com")
    composeTestRule.setContent { ProfileHeader(state = state) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EMAIL).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_PROFILE_PICTURE)
        .assertIsDisplayed()
  }

  @Test
  fun topBar_displaysTitle_and_backButton() {
    composeTestRule.setContent { ProfileTopBar(onBackClick = {}) }

    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun topBar_backButton_invokesCallback() {
    var clicked = false
    composeTestRule.setContent { ProfileTopBar(onBackClick = { clicked = true }) }

    composeTestRule.onNodeWithContentDescription("Back").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.runOnIdle { assertTrue(clicked) }
  }

  @Test
  fun profileHeader_editButton_invokesCallback() {
    var clicked = false
    val state = ProfileState.default()
    composeTestRule.setContent {
      ProfileHeader(state = state, onEditRequested = { clicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Edit").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    composeTestRule.runOnIdle { assertTrue(clicked) }
  }

  @Test
  fun profileHeader_displaysEmptyValues_whenNoNameOrEmail() {
    val emptyState = ProfileState.default().copy(userName = "", userEmail = "")
    composeTestRule.setContent { ProfileHeader(state = emptyState) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME).assertTextEquals("")
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EMAIL).assertTextEquals("")
  }

  @Test
  fun profileHeader_updates_whenStateChanges() {
    val state =
        mutableStateOf(ProfileState.default().copy(userName = "Initial", userEmail = "init@ex.com"))
    composeTestRule.setContent { ProfileHeader(state = state.value) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME).assertTextEquals("Initial")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EMAIL)
        .assertTextEquals("init@ex.com")

    composeTestRule.runOnIdle {
      state.value = state.value.copy(userName = "Updated Name", userEmail = "updated@ex.com")
    }

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME)
        .assertTextEquals("Updated Name")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EMAIL)
        .assertTextEquals("updated@ex.com")
  }

  @Test
  fun profileHeader_preservesLongName_andEmail_withoutTruncation() {
    val longText = "L".repeat(256)
    val longState = ProfileState.default().copy(userName = longText, userEmail = longText)
    composeTestRule.setContent { ProfileHeader(state = longState) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME).assertTextEquals(longText)
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EMAIL).assertTextEquals(longText)
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
    composeTestRule.setContent {
      ProfileScreen(viewModel = ProfileViewModel(state, attachAuthListener = false))
    }
    composeTestRule.onNodeWithText("99").assertIsDisplayed()
    composeTestRule.onNodeWithText("88").assertIsDisplayed()
    composeTestRule.onNodeWithText("77").assertIsDisplayed()
    composeTestRule.onNodeWithText("66").assertIsDisplayed()
  }

  @Test
  fun statGroupCard_displaysTopAndBottom_values_and_tags_directly() {
    composeTestRule.setContent {
      com.android.sample.ui.profile.composables.StatGroupCard(
          labelTop = "Top",
          topValue = 7,
          labelBottom = "Bottom",
          bottomValue = 3,
          topTag = "top_test_tag",
          bottomTag = "bottom_test_tag")
    }

    composeTestRule.onNodeWithTag("top_test_tag").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_test_tag").assertIsDisplayed()
    composeTestRule.onNodeWithTag("top_test_tag").assertTextEquals("7")
    composeTestRule.onNodeWithTag("bottom_test_tag").assertTextEquals("3")
  }

  @Test
  fun profileStats_providesUnique_statTags_forEachCard() {
    val s = ProfileState(kudosReceived = 1, helpReceived = 2, followers = 3, following = 4)
    composeTestRule.setContent { ProfileStats(state = s) }

    // each tag should appear exactly once
    composeTestRule.onAllNodesWithTag(ProfileTestTags.PROFILE_STAT_TOP_KUDOS).assertCountEquals(1)
    composeTestRule
        .onAllNodesWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        .assertCountEquals(1)
    composeTestRule
        .onAllNodesWithTag(ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS)
        .assertCountEquals(1)
    composeTestRule
        .onAllNodesWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING)
        .assertCountEquals(1)

    // values visible
    composeTestRule.onNodeWithText("1").assertIsDisplayed()
    composeTestRule.onNodeWithText("2").assertIsDisplayed()
    composeTestRule.onNodeWithText("3").assertIsDisplayed()
    composeTestRule.onNodeWithText("4").assertIsDisplayed()
  }

  @Test
  fun profileStats_updatesValues_whenStateChanges() {
    val state =
        mutableStateOf(
            ProfileState.default()
                .copy(kudosReceived = 5, helpReceived = 6, followers = 7, following = 8))
    composeTestRule.setContent { ProfileStats(state = state.value) }

    // initial values
    composeTestRule.onNodeWithText("5").assertIsDisplayed()
    composeTestRule.onNodeWithText("6").assertIsDisplayed()
    composeTestRule.onNodeWithText("7").assertIsDisplayed()
    composeTestRule.onNodeWithText("8").assertIsDisplayed()

    // update the backing state and verify the UI updates
    composeTestRule.runOnIdle {
      state.value =
          state.value.copy(kudosReceived = 50, helpReceived = 60, followers = 70, following = 80)
    }

    composeTestRule.onNodeWithText("50").assertIsDisplayed()
    composeTestRule.onNodeWithText("60").assertIsDisplayed()
    composeTestRule.onNodeWithText("70").assertIsDisplayed()
    composeTestRule.onNodeWithText("80").assertIsDisplayed()
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
            userSection = "Math")
    composeTestRule.setContent { ProfileInformation(state = state) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Information").assertIsDisplayed()

    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals("Alice")
    composeTestRule.onNodeWithTag("profile_info_profile_id").assertTextEquals("ID123")
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertTextEquals("01/01/2020")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals("Math")
    composeTestRule.onNodeWithTag("profile_info_email").assertTextEquals("alice@ex.com")
  }

  @Test
  fun infoRow_generatesTag_fromLabel_withSpacesAndCase() {
    composeTestRule.setContent { InfoRow(label = "Arrival date", value = "01/01/2020") }

    // InfoRow creates tag: profile_info_arrival_date
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertTextEquals("01/01/2020")
  }

  @Test
  fun profileInformation_showsLabels_and_handlesEmptyValues() {
    val emptyState =
        ProfileState.default()
            .copy(userName = "", userEmail = "", profileId = "", arrivalDate = "", userSection = "")

    composeTestRule.setContent { ProfileInformation(state = emptyState) }

    // Header label remains visible
    composeTestRule.onNodeWithText("Information").assertIsDisplayed()

    // Each info value exists and is an empty string
    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals("")
    composeTestRule.onNodeWithTag("profile_info_profile_id").assertTextEquals("")
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertTextEquals("")
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals("")
    composeTestRule.onNodeWithTag("profile_info_email").assertTextEquals("")
  }

  @Test
  fun profileInformation_preservesLongValues_withoutTruncation() {
    val longText = "L".repeat(256)
    val stateWithLongs =
        ProfileState.default()
            .copy(
                userName = longText,
                userEmail = longText,
                profileId = longText,
                arrivalDate = longText,
                userSection = longText)

    composeTestRule.setContent { ProfileInformation(state = stateWithLongs) }

    // Verify the full long text is set on each value node
    composeTestRule.onNodeWithTag("profile_info_name").assertTextEquals(longText)
    composeTestRule.onNodeWithTag("profile_info_profile_id").assertTextEquals(longText)
    composeTestRule.onNodeWithTag("profile_info_arrival_date").assertTextEquals(longText)
    composeTestRule.onNodeWithTag("profile_info_section").assertTextEquals(longText)
    composeTestRule.onNodeWithTag("profile_info_email").assertTextEquals(longText)
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

  @Test
  fun actionItem_direct_displaysTitleSubtitle_and_invokesOnClick() {
    var clicked = false
    composeTestRule.setContent {
      com.android.sample.ui.profile.composables.ActionItem(
          icon = androidx.compose.material.icons.Icons.Default.Info,
          title = "Test Action",
          subtitle = "Do something",
          tag = "test_action_item",
          onClick = { clicked = true },
          palette = com.android.sample.ui.theme.appPalette())
    }

    composeTestRule.onNodeWithTag("test_action_item").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Action").assertIsDisplayed()
    composeTestRule.onNodeWithText("Do something").assertIsDisplayed()

    composeTestRule.onNodeWithTag("test_action_item").performClick()
    composeTestRule.runOnIdle { assertTrue(clicked) }
  }

  @Test
  fun profileActions_containsBothActions_uniqueTags_and_areClickable() {
    composeTestRule.setContent { ProfileActions() }

    // container visible
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()

    // ensure each action appears exactly once and is clickable
    composeTestRule.onAllNodesWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).assertCountEquals(1)
    composeTestRule.onAllNodesWithTag(ProfileTestTags.PROFILE_ACTION_ABOUT_APP).assertCountEquals(1)

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).assertHasClickAction()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_ABOUT_APP).assertHasClickAction()

    // titles still visible
    composeTestRule.onNodeWithText("Log out").assertIsDisplayed()
    composeTestRule.onNodeWithText("About App").assertIsDisplayed()
  }

  @Test
  fun profileActions_aboutApp_item_isClickable_whenComposed() {
    composeTestRule.setContent { ProfileActions() }
    // About App should be present and have a click action
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_ABOUT_APP).assertHasClickAction()
  }

  @Test
  fun backButton_invokesOnBackClick() {
    var backClicked = false

    val viewModel = ProfileViewModel(initialState = ProfileState.default())

    composeTestRule.setContent {
      ProfileScreen(viewModel = viewModel, onBackClick = { backClicked = true })
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_TOP_BAR_BACK_BUTTON)
        .assertExists()
        .performClick()

    // ensure callback executed
    TestCase.assertTrue("Expected onBackClick to be invoked", backClicked)
  }

  // ----- Dialogues -----
  @Test
  fun logoutDialog_showsTitle_message_and_buttons_whenVisible() {
    // Compose once and assert the dialog and its key parts (avoid ambiguous onNodeWithText("Log
    // out"))
    composeTestRule.setContent { LogoutDialog(visible = true, onConfirm = {}, onDismiss = {}) }

    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL).assertIsDisplayed()
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
    val visibleState = mutableStateOf(true)
    composeTestRule.setContent {
      // pass mutable state so the dialog can be removed without re-calling setContent from test
      LogoutDialog(
          visible = visibleState.value,
          onConfirm = {},
          onDismiss = {
            dismissed = true
            visibleState.value = false
          })
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL).performClick()
    composeTestRule.runOnIdle { assertTrue(dismissed) }

    // dialog should have been removed from the tree
    composeTestRule.onAllNodesWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertCountEquals(0)
  }

  @Test
  fun logoutDialog_notVisible_whenVisibleFalse() {
    composeTestRule.setContent { LogoutDialog(visible = false, onConfirm = {}, onDismiss = {}) }
    composeTestRule.onAllNodesWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertCountEquals(0)
  }

  @Test
  fun logoutDialog_confirmButton_callsOnConfirm_and_hidesWhenStateCleared() {
    var confirmed = false
    val visibleState = mutableStateOf(true)

    composeTestRule.setContent {
      LogoutDialog(
          visible = visibleState.value,
          onConfirm = {
            confirmed = true
            visibleState.value = false
          },
          onDismiss = {})
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM).assertHasClickAction()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM).performClick()

    composeTestRule.runOnIdle { assertTrue(confirmed) }
    composeTestRule.onAllNodesWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertCountEquals(0)
  }

  @Test
  fun logoutDialog_buttons_and_title_present_and_singleInstance() {
    composeTestRule.setContent { LogoutDialog(visible = true, onConfirm = {}, onDismiss = {}) }

    // exactly one dialog instance
    composeTestRule.onAllNodesWithTag(ProfileTestTags.LOG_OUT_DIALOG).assertCountEquals(1)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Log out")
    // buttons present (use test tags to avoid ambiguous text matches)
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CANCEL).assertIsDisplayed()
  }

  // ----- Loading & Error -----
  @Test
  fun loadingIndicator_displaysWhenComposed() {
    composeTestRule.setContent { LoadingIndicator() }
    composeTestRule.onNodeWithTag(ProfileTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun loadingIndicator_notPresent_whenNotComposed() {
    // Compose an empty UI and assert no loading indicator nodes exist
    composeTestRule.setContent { /* intentionally empty */}
    composeTestRule.onAllNodesWithTag(ProfileTestTags.LOADING_INDICATOR).assertCountEquals(0)
  }

  @Test
  fun profileScreen_loadingState_showsLoadingIndicator_and_disappearsWhenNotLoading() {
    val loadingState = ProfileState.loading()
    val vm = ProfileViewModel(loadingState)
    composeTestRule.setContent {
      ProfileScreen(
          viewModel = vm,
      )
    }

    // initially loading
    composeTestRule.onNodeWithTag(ProfileTestTags.LOADING_INDICATOR).assertIsDisplayed()

    // flip loading off via the viewModel (no second setContent)
    composeTestRule.runOnIdle { vm.setLoading(false) }
    composeTestRule.onNodeWithTag(ProfileTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }

  @Test
  fun errorBanner_and_profileScreen_errorState_showsMessage_and_disappears() {
    // Start with a ProfileViewModel that has an error and compose once
    val errorState = ProfileState.withError()
    val vm = ProfileViewModel(errorState, attachAuthListener = false)
    composeTestRule.setContent {
      ProfileScreen(
          viewModel = vm,
      )
    }

    composeTestRule.onNodeWithTag("profile_error").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_error").assertTextEquals("Failed to load profile data")

    // clear the error via the viewModel (no second setContent)
    composeTestRule.runOnIdle { vm.setError(null) }
    composeTestRule.onAllNodesWithTag("profile_error").assertCountEquals(0)
  }

  @Test
  fun errorBanner_displaysProvidedMessage_whenComposedDirectly() {
    composeTestRule.setContent { ErrorBanner("Test error") }
    composeTestRule.onNodeWithTag("profile_error").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_error").assertTextEquals("Test error")
  }

  @Test
  fun profileContent_showsErrorBanner_and_disappears_whenStateCleared() {
    val state = mutableStateOf(ProfileState.withError())
    composeTestRule.setContent {
      ProfileContent(state = state.value, onLogoutRequested = {}, onMyRequestAction = {})
    }

    // initially present
    composeTestRule.onAllNodesWithTag("profile_error").assertCountEquals(1)
    composeTestRule.onNodeWithTag("profile_error").assertTextEquals("Failed to load profile data")

    // clear the error via state and ensure banner is removed
    composeTestRule.runOnIdle { state.value = state.value.copy(errorMessage = null) }
    composeTestRule.onAllNodesWithTag("profile_error").assertCountEquals(0)
  }

  @Test
  fun errorBanner_singleInstance_inProfileContent() {
    val state = ProfileState.withError()
    composeTestRule.setContent {
      ProfileContent(state = state, onLogoutRequested = {}, onMyRequestAction = {})
    }

    // ensure exactly one error banner node is present
    composeTestRule.onAllNodesWithTag("profile_error").assertCountEquals(1)
  }

  // ----- ProfileContent integration -----
  @Test
  fun profileContent_composesAllSections_and_actionClick_propagates() {
    var logoutRequested = false
    val s = ProfileState.default()
    composeTestRule.setContent {
      ProfileContent(
          state = s, onLogoutRequested = { logoutRequested = true }, onMyRequestAction = {})
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT).performClick()
    composeTestRule.runOnIdle { assertTrue(logoutRequested) }
  }

  @Test
  fun setEditMode_updates_state() = runTest {
    val viewModel = ProfileViewModel() // adjust if constructor requires params
    // enable edit mode
    viewModel.setEditMode(true)
    assertEquals(true, viewModel.state.value.isEditMode)

    // disable edit mode
    viewModel.setEditMode(false)
    assertEquals(false, viewModel.state.value.isEditMode)
  }

  @Test
  fun updateSection_updates_userSection() = runTest {
    val viewModel = ProfileViewModel() // adjust if constructor requires params
    val newSection = "Architecture"
    viewModel.updateSection(newSection)
    assertEquals(newSection, viewModel.state.value.userSection)
  }

  @Test
  fun updateUserName_updates_userName() = runTest {
    val viewModel = ProfileViewModel() // adjust if constructor requires params
    val newName = "Charlie"
    viewModel.updateUserName(newName)
    assertEquals(newName, viewModel.state.value.userName)
  }

  @Test
  fun fromLabel_mapsEachEntry() {
    for (entry in UserSections.entries) {
      assertEquals(entry, UserSections.fromLabel(entry.label))
    }
  }

  @Test
  fun fromLabel_unknownReturnsNone() {
    assertEquals(UserSections.NONE, UserSections.fromLabel("non_existent_label_123"))
  }

  // ----- My Request Action -----
  @Test
  fun myRequest_action_isDisplayed_and_hasClickAction() {
    composeTestRule.setContent { ProfileActions() }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST).assertIsDisplayed()
    composeTestRule.onNodeWithText("My Request").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST).assertHasClickAction()
  }

  @Test
  fun myRequest_action_triggersCallback() {
    var triggered = false
    composeTestRule.setContent { ProfileActions(onMyRequestClick = { triggered = true }) }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST).performClick()
    composeTestRule.runOnIdle { assertTrue(triggered) }
  }

  @Test
  fun myRequest_button_callsViewModel_onMyRequestsClick() = runTest {
    var navigationCalled = false
    var correctScreen = false

    composeTestRule.setContent {
      val navController = rememberNavController()
      val mockNavigationActions =
          object : NavigationActions(navController) {
            override fun navigateTo(screen: Screen) {
              navigationCalled = true
              if (screen is Screen.MyRequest) {
                correctScreen = true
              }
            }
          }

      val viewModel = ProfileViewModel(attachAuthListener = false)

      ProfileActions(onMyRequestClick = { viewModel.onMyRequestsClick(mockNavigationActions) })
    }

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST).performClick()

    composeTestRule.runOnIdle {
      assertTrue("Expected navigation to be called", navigationCalled)
      assertTrue("Expected navigation to MyRequest screen", correctScreen)
    }
  }
}
