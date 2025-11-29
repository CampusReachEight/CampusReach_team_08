package com.android.sample.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.publicProfile.FollowButton
import com.android.sample.ui.profile.publicProfile.PublicProfile
import com.android.sample.ui.profile.publicProfile.PublicProfileErrors
import com.android.sample.ui.profile.publicProfile.PublicProfileScreen
import com.android.sample.ui.profile.publicProfile.PublicProfileTestTags
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModel
import com.android.sample.ui.profile.publicProfile.mapPublicToProfile
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PublicProfileTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun publicProfileScreen_displaysAllSections() {
    composeTestRule.setContent { PublicProfileScreen() }

    // top-level screen
    composeTestRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_SCREEN).assertIsDisplayed()

    // header, stats and information sections should be present
    composeTestRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
  }

  @Test
  fun followButton_toggles_whenClicked_inPublicProfileScreen() {
    composeTestRule.setContent { PublicProfileScreen() }

    // initial follow button should be visible and clickable
    val followTag = PublicProfileTestTags.FOLLOW_BUTTON
    val unfollowTag = PublicProfileTestTags.UNFOLLOW_BUTTON

    composeTestRule.onNodeWithTag(followTag).assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithTag(followTag).performClick()

    // wait for recomposition/state change and ensure the UNFOLLOW button is now visible
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(unfollowTag).assertIsDisplayed()
  }

  @Test
  fun followButton_direct_displaysLabel_and_invokesCallback() {
    var clicked = false
    composeTestRule.setContent { FollowButton(isFollowing = false, onToggle = { clicked = true }) }

    // "Follow" text present and clickable
    composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follow").performClick()

    composeTestRule.runOnIdle { assertTrue(clicked) }
  }

  @Test
  fun followButton_direct_showsUnfollow_whenStartingFollowing() {
    composeTestRule.setContent { FollowButton(isFollowing = true, onToggle = {}) }

    // When isFollowing == true, label should be Unfollow
    composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
  }

  @Test
  fun publicProfileHeader_displaysName_section_and_picture_inScreen() {
    composeTestRule.setContent { PublicProfileScreen() }

    // Header name and section/email text nodes and profile picture should be present
    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_PROFILE_PICTURE)
        .assertIsDisplayed()
  }

  @Test
  fun profileLoadingBuffer_displaysWhenComposed() {
    composeTestRule.setContent { ProfileLoadingBuffer(modifier = androidx.compose.ui.Modifier) }

    // Loading indicator used by profile components should be visible when the buffer is composed
    composeTestRule.onNodeWithTag(ProfileTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun staticPublicProfileScreen_displaysProvidedProfile_valuesAndStats() {
    val sample =
        PublicProfile(
            userId = "u42",
            email = "jane.doe@fake.com",
            name = "Jane Doe",
            section = "Engineering",
            arrivalDate = "12/12/2022",
            pictureUriString = null,
            kudosReceived = 7,
            helpReceived = 8,
            followers = 9,
            following = 10)

    composeTestRule.setContent { PublicProfileScreen(profile = sample) }

    // Header values
    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME)
        .assertIsDisplayed()
        .assertTextContains("Jane Doe")

    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL)
        .assertIsDisplayed()
        .assertTextContains("jane.doe@fake.com")

    // Stats values (verify visible numbers)
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_KUDOS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING).assertIsDisplayed()

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_KUDOS).assertTextContains("7")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        .assertTextContains("8")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS)
        .assertTextContains("9")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING)
        .assertTextContains("10")

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_INFO_SECTION)
        .assertTextContains("Engineering")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        .assertTextContains("8")
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFO_NAME).assertTextContains("Jane Doe")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_INFO_EMAIL)
        .assertTextContains("jane.doe@fake.com")
  }

  @Test
  fun mapPublicToProfile_transfersAllFields() {
    val pub =
        PublicProfile(
            userId = "u1",
            name = "Name",
            email = "email",
            section = "Section",
            arrivalDate = "01/01/2020",
            pictureUriString = "uri://img",
            kudosReceived = 3,
            helpReceived = 4,
            followers = 5,
            following = 6)

    val mapped = mapPublicToProfile(pub)
    assertEquals("Name", mapped.userName)
    assertEquals("Section", mapped.userSection)
    assertEquals("uri://img", mapped.profilePictureUrl)
    assertEquals(3, mapped.kudosReceived)
    assertEquals(4, mapped.helpReceived)
    assertEquals(5, mapped.followers)
    assertEquals(6, mapped.following)
    assertEquals(false, mapped.isEditMode)
    assertEquals(false, mapped.isLoggingOut)
  }

  @Test
  fun loadBlank_setsErrorAndClearsProfile() {
    val vm = PublicProfileViewModel()
    vm.loadPublicProfile("")

    val state = vm.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.profile)
    assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
  }

  @Test
  fun loadNonBlank_returnsDeterministicProfile() {
    val vm = PublicProfileViewModel()
    vm.loadPublicProfile("alice123")

    val profile = vm.uiState.value.profile
    assertNotNull(profile)
    assertEquals("alice123", profile!!.userId)
    Assert.assertTrue(profile.name.isNotBlank())
  }

  @Test
  fun helpers_setLoading_setError_and_clear_work() {
    val vm = PublicProfileViewModel()
    vm.setLoading(true)
    Assert.assertTrue(vm.uiState.value.isLoading)

    vm.setError("boom")
    assertEquals("boom", vm.uiState.value.error)

    vm.clear()
    val cleared = vm.uiState.value
    assertFalse(cleared.isLoading)
    assertNull(cleared.profile)
    assertNull(cleared.error)
  }
}
