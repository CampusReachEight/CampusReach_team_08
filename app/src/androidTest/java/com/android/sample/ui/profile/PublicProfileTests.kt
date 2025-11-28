package com.android.sample.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.profile.composables.ProfileLoadingBuffer
import com.android.sample.ui.profile.publicProfile.FollowButton
import com.android.sample.ui.profile.publicProfile.PublicProfileScreen
import com.android.sample.ui.profile.publicProfile.PublicProfileTestTags
import junit.framework.TestCase.assertTrue
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
}
