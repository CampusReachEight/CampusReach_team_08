package com.android.sample.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.publicProfile.FollowButton
import com.android.sample.ui.profile.publicProfile.PublicProfileScreen
import com.android.sample.ui.profile.publicProfile.PublicProfileTestTags
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModel
import com.android.sample.ui.profile.publicProfile.mapUserProfileToProfileState
import java.util.Date
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PublicProfileTests {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createMockViewModel(): PublicProfileViewModel {
    return PublicProfileViewModel(MockUserProfileRepository())
  }

  @Test
  fun publicProfileScreen_displaysAllSections() {
    val sampleProfile =
        UserProfile(
            id = "test123",
            name = "Test",
            lastName = "User",
            email = "test@example.com",
            photo = null,
            kudos = 0,
            helpReceived = 0,
            section = UserSections.NONE,
            arrivalDate = Date())

    composeTestRule.setContent {
      PublicProfileScreen(profile = sampleProfile, viewModel = createMockViewModel())
    }

    composeTestRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
  }

  @Test
  fun followButton_toggles_whenClicked_inPublicProfileScreen() {
    val sampleProfile =
        UserProfile(
            id = "test123",
            name = "Test",
            lastName = "User",
            email = "test@example.com",
            photo = null,
            kudos = 0,
            helpReceived = 0,
            section = UserSections.NONE,
            arrivalDate = Date())

    composeTestRule.setContent {
      PublicProfileScreen(profile = sampleProfile, viewModel = createMockViewModel())
    }

    val followTag = PublicProfileTestTags.FOLLOW_BUTTON
    val unfollowTag = PublicProfileTestTags.UNFOLLOW_BUTTON

    composeTestRule.onNodeWithTag(followTag).assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithTag(followTag).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(unfollowTag).assertIsDisplayed()
  }

  @Test
  fun followButton_direct_displaysLabel_and_invokesCallback() {
    var clicked = false
    composeTestRule.setContent { FollowButton(isFollowing = false, onToggle = { clicked = true }) }

    composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follow").performClick()

    composeTestRule.runOnIdle { assertTrue(clicked) }
  }

  @Test
  fun followButton_direct_showsUnfollow_whenStartingFollowing() {
    composeTestRule.setContent { FollowButton(isFollowing = true, onToggle = {}) }
    composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
  }

  @Test
  fun publicProfileHeader_displaysName_section_and_picture_inScreen() {
    val sampleProfile =
        UserProfile(
            id = "test123",
            name = "Test",
            lastName = "User",
            email = "test@example.com",
            photo = null,
            kudos = 0,
            helpReceived = 0,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    composeTestRule.setContent {
      PublicProfileScreen(profile = sampleProfile, viewModel = createMockViewModel())
    }

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
    composeTestRule.onNodeWithTag(ProfileTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun staticPublicProfileScreen_displaysProvidedProfile_valuesAndStats() {
    val sampleUserProfile =
        UserProfile(
            id = "u42",
            name = "Jane",
            lastName = "Doe",
            email = "jane.doe@fake.com",
            photo = null,
            kudos = 7,
            helpReceived = 8,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    composeTestRule.setContent {
      PublicProfileScreen(profile = sampleUserProfile, viewModel = createMockViewModel())
    }

    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME)
        .assertIsDisplayed()
        .assertTextContains("Jane Doe")

    composeTestRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL)
        .assertIsDisplayed()
        .assertTextContains("Computer Science")

    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STAT_TOP_KUDOS).assertTextContains("7")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        .assertTextContains("8")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_INFO_SECTION)
        .assertTextContains("Computer Science")
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFO_NAME).assertTextContains("Jane Doe")
    composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFO_EMAIL).assertDoesNotExist()
  }

  @Test
  fun mapUserProfileToProfileState_transfersAllFields() {
    val userProfile =
        UserProfile(
            id = "u1",
            name = "Jane",
            lastName = "Doe",
            email = "test@example.com",
            photo = null,
            kudos = 3,
            helpReceived = 4,
            section = UserSections.MATHEMATICS,
            arrivalDate = Date())

    val mapped = mapUserProfileToProfileState(userProfile)

    assertEquals("Jane Doe", mapped.userName)
    assertEquals("Mathematics", mapped.userSection)
    assertEquals(3, mapped.kudosReceived)
    assertEquals(4, mapped.helpReceived)
    assertEquals(false, mapped.isEditMode)
    assertEquals(false, mapped.isLoggingOut)
    assertTrue(mapped.arrivalDate.isNotBlank())
  }

  @Test
  fun mapUserProfileToProfileState_handlesNullProfile() {
    val mapped = mapUserProfileToProfileState(null)

    assertEquals("Unknown", mapped.userName)
    assertEquals("None", mapped.userSection)
    assertEquals(0, mapped.kudosReceived)
    assertEquals(0, mapped.helpReceived)
  }

  @Test
  fun mapUserProfileToProfileState_handlesBlankLastName() {
    val userProfile =
        UserProfile(
            id = "u1",
            name = "John",
            lastName = "",
            email = "john@example.com",
            photo = null,
            kudos = 5,
            helpReceived = 2,
            section = UserSections.ARCHITECTURE,
            arrivalDate = Date())

    val mapped = mapUserProfileToProfileState(userProfile)

    assertEquals("John", mapped.userName)
    assertEquals("Architecture", mapped.userSection)
  }

  @Test
  fun mapUserProfileToProfileState_handlesSectionLabel() {
    val userProfile =
        UserProfile(
            id = "u1",
            name = "Alice",
            lastName = "Smith",
            email = "alice@example.com",
            photo = null,
            kudos = 10,
            helpReceived = 5,
            section = UserSections.ELECTRICAL_ENGINEERING,
            arrivalDate = Date())

    val mapped = mapUserProfileToProfileState(userProfile)

    assertEquals("Electrical Engineering", mapped.userSection)
  }

  // Mock repository
  private class MockUserProfileRepository : UserProfileRepository {
    override fun getNewUid(): String = "mockUid"

    override fun getCurrentUserId(): String = "mockCurrentUser"

    override suspend fun getAllUserProfiles(): List<UserProfile> = emptyList()

    override suspend fun getUserProfile(userId: String): UserProfile {
      return UserProfile(
          id = userId,
          name = "Mock",
          lastName = "User",
          email = "mock@example.com",
          photo = null,
          kudos = 0,
          helpReceived = 0,
          section = UserSections.NONE,
          arrivalDate = Date())
    }

    override suspend fun addUserProfile(userProfile: UserProfile) {}

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {}

    override suspend fun deleteUserProfile(userId: String) {}

    override suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile> =
        emptyList()

    override suspend fun awardKudos(userId: String, amount: Int) {}

    override suspend fun awardKudosBatch(awards: Map<String, Int>) {}

    override suspend fun receiveHelp(userId: String, amount: Int) {}
  }
}
