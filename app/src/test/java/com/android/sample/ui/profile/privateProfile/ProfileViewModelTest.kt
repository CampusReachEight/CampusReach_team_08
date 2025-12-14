package com.android.sample.ui.profile.privateProfile

import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.ProfileViewModel
import com.android.sample.ui.profile.UserSections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Date
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"

    const val NAME_ALICE = "Alice"
    const val LASTNAME_SMITH = "Smith"
    const val EMAIL_ALICE = "alice@test.com"

    const val NAME_JANE = "Jane"
    const val LASTNAME_DOE = "Doe"

    const val KUDOS_10 = 10
    const val HELP_5 = 5

    const val FOLLOWER_COUNT_42 = 42
    const val FOLLOWING_COUNT_15 = 15

    const val SECTION_LABEL_SC = "SC"

    const val EXPECTED_FOLLOWER_COUNT = FOLLOWER_COUNT_42
    const val EXPECTED_FOLLOWING_COUNT = FOLLOWING_COUNT_15

    const val ONE = 1
  }

  // ============ Test Fixtures ============
  private lateinit var mockRepository: UserProfileRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: ProfileViewModel

  private val testDispatcher = StandardTestDispatcher()

  private val testProfile =
      UserProfile(
          id = CURRENT_USER_ID,
          name = NAME_ALICE,
          lastName = LASTNAME_SMITH,
          email = EMAIL_ALICE,
          photo = null,
          kudos = KUDOS_10,
          helpReceived = HELP_5,
          followerCount = FOLLOWER_COUNT_42,
          followingCount = FOLLOWING_COUNT_15,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())

  // ============ Test Lifecycle ============

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { mockFirebaseUser.uid } returns CURRENT_USER_ID
    every { mockAuth.currentUser } returns mockFirebaseUser
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ============ Helper Methods ============

  /** Captures the auth listener and triggers it manually. */
  private fun simulateAuthStateChange() {
    val authListenerSlot = slot<FirebaseAuth.AuthStateListener>()
    every { mockAuth.addAuthStateListener(capture(authListenerSlot)) } answers
        {
          // Trigger the listener immediately after capture
          authListenerSlot.captured.onAuthStateChanged(mockAuth)
        }

    testDispatcher.scheduler.advanceUntilIdle()
  }

  // ============ Tests for Save Profile Changes ============

  @Test
  fun saveProfileChanges_preservesFollowerAndFollowingCounts() = runTest {
    // Given
    val capturedProfiles = mutableListOf<UserProfile>()

    coEvery { mockRepository.getUserProfile(CURRENT_USER_ID) } returns testProfile
    coEvery { mockRepository.updateUserProfile(any(), any()) } answers
        {
          capturedProfiles.add(arg(ONE))
        }

    viewModel =
        ProfileViewModel(
            repository = mockRepository, fireBaseAuth = mockAuth, attachAuthListener = true)

    // Simulate auth state change to load profile
    simulateAuthStateChange()
    testDispatcher.scheduler.advanceUntilIdle()

    // When
    viewModel.saveProfileChanges("$NAME_JANE $LASTNAME_DOE", SECTION_LABEL_SC)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertTrue("Profile should have been saved", capturedProfiles.isNotEmpty())
    val savedProfile = capturedProfiles.first()
    assertEquals(
        "Follower count should be preserved", EXPECTED_FOLLOWER_COUNT, savedProfile.followerCount)
    assertEquals(
        "Following count should be preserved",
        EXPECTED_FOLLOWING_COUNT,
        savedProfile.followingCount)
  }
}
