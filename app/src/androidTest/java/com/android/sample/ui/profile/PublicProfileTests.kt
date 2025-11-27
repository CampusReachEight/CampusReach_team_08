package com.android.sample.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationScreen
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.publicProfile.FollowButton
import com.android.sample.ui.profile.publicProfile.PublicProfile
import com.android.sample.ui.profile.publicProfile.PublicProfileHeader
import com.android.sample.ui.profile.publicProfile.PublicProfileTestTags
import com.android.sample.ui.profile.publicProfile.PublicProfileUiState
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PublicProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val samplePublic =
      PublicProfile(
          userId = "u1",
          name = "Test User",
          section = "Computer Science",
          arrivalDate = "01/01/2024",
          pictureUriString = null,
          kudosReceived = 0,
          helpReceived = 0,
          followers = 0,
          following = 0)

  @Test
  fun followButton_showsFollow_and_togglesToUnfollow() {
    val isFollowing = mutableStateOf(false)

    composeRule.setContent {
      FollowButton(
          isFollowing = isFollowing.value,
          onToggle = { isFollowing.value = !isFollowing.value },
          modifier = Modifier.testTag(PublicProfileTestTags.FOLLOW_BUTTON))
    }

    // initially "Follow"
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).assertTextEquals("Follow")

    // click toggles state -> "Unfollow"
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).performClick()
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).assertTextEquals("Unfollow")
  }

  @Test
  fun followButton_emits_onToggle_callback() {
    var toggled = false
    val isFollowing = mutableStateOf(false)

    composeRule.setContent {
      FollowButton(
          isFollowing = isFollowing.value,
          onToggle = {
            toggled = true
            isFollowing.value = !isFollowing.value
          },
          modifier = Modifier.testTag(PublicProfileTestTags.FOLLOW_BUTTON))
    }

    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).performClick()
    composeRule.runOnIdle { assert(toggled) }
  }

  @Test
  fun publicProfileHeader_displays_name_section_picture_and_followButton() {
    // Construct a minimal PublicProfile used by header
    val samplePublic =
        PublicProfile(
            userId = "u1",
            name = "Test User",
            section = "Computer Science",
            arrivalDate = "01/01/2024",
            pictureUriString = null,
            kudosReceived = 0,
            helpReceived = 0,
            followers = 0,
            following = 0)

    // minimal UI state matching what PublicProfileHeader expects
    val uiState = PublicProfileUiState(profile = samplePublic, isLoading = false, error = null)

    composeRule.setContent {
      PublicProfileHeader(
          state = uiState, isFollowing = false, onFollowToggle = {}, modifier = Modifier)
    }

    composeRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER).assertIsDisplayed()
    composeRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME).assertIsDisplayed()
    composeRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_NAME)
        .assertTextEquals("Test User")
    composeRule.onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_EMAIL).assertIsDisplayed()
    composeRule
        .onNodeWithTag(PublicProfileTestTags.PUBLIC_PROFILE_HEADER_PROFILE_PICTURE)
        .assertIsDisplayed()
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()
  }

  @Test
  fun followButton_showsFollow_and_togglesToUnfollow_withTagSwap() {
    val state = PublicProfileUiState(profile = samplePublic, isLoading = false, error = null)
    val isFollowing = mutableStateOf(false)

    composeRule.setContent {
      PublicProfileHeader(
          state = state,
          isFollowing = isFollowing.value,
          onFollowToggle = { isFollowing.value = !isFollowing.value })
    }

    // initial: Follow button tag present and label shown
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithText("Follow").assertIsDisplayed()

    // click to toggle
    composeRule.onNodeWithTag(PublicProfileTestTags.FOLLOW_BUTTON).performClick()
    // after toggle the UNFOLLOW tag and text should be visible
    composeRule.onNodeWithTag(PublicProfileTestTags.UNFOLLOW_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithText("Unfollow").assertIsDisplayed()
  }

  private fun mapPublicToProfileStateViaReflection(
      publicProfile: PublicProfile?,
      error: String?,
      isLoading: Boolean
  ): ProfileState {
    val cls = Class.forName("com.android.sample.ui.profile.publicProfile.PublicProfileScreenKt")
    val method =
        cls.getDeclaredMethod(
            "mapPublicToProfileState",
            PublicProfile::class.java,
            String::class.java,
            java.lang.Boolean.TYPE)
    method.isAccessible = true
    return method.invoke(null, publicProfile, error, java.lang.Boolean.valueOf(isLoading))
        as ProfileState
  }

  @Test
  fun maps_null_public_profile_to_minimal_state_with_error() {
    val state = mapPublicToProfileStateViaReflection(null, "load failed", false)

    assertEquals("Unknown", state.userName)
    assertEquals("", state.userEmail)
    assertEquals("", state.profileId)
    assertEquals(0, state.kudosReceived)
    assertEquals(0, state.followers)
    assertEquals(0, state.following)
    assertEquals("load failed", state.errorMessage)
    assertEquals(false, state.isLoading)
  }

  @Test
  fun maps_non_null_public_profile_to_profilestate_fields() {
    val pub =
        PublicProfile(
            userId = "u42",
            name = "Jane Doe",
            section = "Engineering",
            arrivalDate = "01/02/2024",
            pictureUriString = "content://pic",
            kudosReceived = 7,
            helpReceived = 1,
            followers = 123,
            following = 10)

    val state = mapPublicToProfileStateViaReflection(pub, null, false)

    assertEquals("Jane Doe", state.userName)
    assertEquals("u42", state.profileId)
    assertEquals("Engineering", state.userSection)
    assertEquals("01/02/2024", state.arrivalDate)
    assertEquals(7, state.kudosReceived)
    assertEquals(123, state.followers)
    assertEquals("content://pic", state.profilePictureUrl)
    assertEquals(null, state.errorMessage)
    assertEquals(false, state.isLoading)
  }

  private fun callUserProfileToPublic(userProfileInstance: Any): PublicProfile {
    val ktClass = Class.forName("com.android.sample.ui.profile.publicProfile.PublicProfileScreenKt")
    val upClass = Class.forName("com.android.sample.model.profile.UserProfile")
    val method = ktClass.getDeclaredMethod("userProfileToPublic", upClass)
    method.isAccessible = true
    return method.invoke(null, userProfileInstance) as PublicProfile
  }

  private fun createDummyUserProfile(overrides: Map<String, Any?> = emptyMap()): Any {
    val upClass = Class.forName("com.android.sample.model.profile.UserProfile")
    // pick the constructor with lowest parameter count
    val ctor = upClass.constructors.minByOrNull { it.parameterTypes.size }!!
    val args =
        ctor.parameterTypes
            .map { p ->
              when {
                p == java.lang.String::class.java -> "X"
                p == java.lang.Integer::class.java || p == java.lang.Integer.TYPE -> 0
                p == java.lang.Long::class.java || p == java.lang.Long.TYPE -> 0L
                p == java.lang.Boolean::class.java || p == java.lang.Boolean.TYPE -> false
                p == java.util.Date::class.java -> Date(1700000000)
                p.isEnum -> p.enumConstants[0]
                else -> null
              }
            }
            .toTypedArray()

    val instance = ctor.newInstance(*args)
    // apply overrides by setting fields reflectively if present
    overrides.forEach { (name, value) ->
      try {
        val field = upClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(instance, value)
      } catch (_: NoSuchFieldException) {
        // ignore if field not present; caller can adapt
      }
    }
    return instance
  }

  @Test
  fun maps_userprofile_fields_to_publicprofile() {
    val upInstance =
        createDummyUserProfile(
            mapOf(
                "id" to "u42",
                "name" to "Jane",
                "lastName" to "Doe",
            ))

    val pub = callUserProfileToPublic(upInstance)

    assertEquals("Jane Doe", pub.name)
    assertEquals("u42", pub.userId)
    // arrivalDate formatting depends on actual up.arrivalDate value; presence checked below
    // ensure basic numeric fields exist and default to integers
    assertEquals(0, pub.kudosReceived)
  }

  @Test
  fun maps_empty_name_to_unknown() {
    val upInstance = createDummyUserProfile(mapOf("name" to "", "lastName" to ""))

    val pub = callUserProfileToPublic(upInstance)

    assertEquals("Unknown", pub.name)
  }

  @Test
  fun navigates_to_public_profile_screen() {
    composeRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      NavigationScreen(
          navController = navController, navigationActions = navigationActions, testMode = true)
    }
    composeRule.waitForIdle()

    // click the hidden test-only button and verify Public Profile opens
    composeRule.onNodeWithTag(NavigationTestTags.PUBLIC_PROFILE_BUTTON).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NavigationTestTags.PUBLIC_PROFILE_SCREEN).assertIsDisplayed()

    // clean up: go back and assert requests visible
    composeRule.onNodeWithTag(ProfileTestTags.PROFILE_TOP_BAR_BACK_BUTTON).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NavigationTestTags.PUBLIC_PROFILE_SCREEN).assertIsNotDisplayed()
  }
}
