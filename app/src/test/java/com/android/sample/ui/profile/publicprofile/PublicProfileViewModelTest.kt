package com.android.sample.ui.profile.publicprofile

import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModel
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"
    const val TARGET_USER_ID = "targetUser456"
    const val PROFILE_ID = "profile789"

    const val USER_NAME = "John"
    const val USER_LAST_NAME = "Doe"
    const val USER_EMAIL = "john.doe@example.com"

    const val INITIAL_KUDOS = 50
    const val INITIAL_HELP_RECEIVED = 10
    const val INITIAL_FOLLOWER_COUNT = 5
    const val INITIAL_FOLLOWING_COUNT = 3

    const val ERROR_NOT_LOGGED_IN = "You must be logged in to follow users"
    const val ERROR_FOLLOW_FAILED = "Failed to update follow status"
    const val ERROR_CANNOT_FOLLOW_SELF = "Cannot follow yourself"
  }

  // ============ Test Fixtures ============
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var viewModel: PublicProfileViewModel

  private val testDispatcher = StandardTestDispatcher()

  // ============ Test Lifecycle ============

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    userProfileRepository = mockk(relaxed = true)
    viewModel = PublicProfileViewModel(userProfileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ============ Helper Methods ============

  /** Creates a test user profile with default values. */
  private fun createTestUserProfile(
      id: String = PROFILE_ID,
      name: String = USER_NAME,
      lastName: String = USER_LAST_NAME,
      email: String = USER_EMAIL,
      kudos: Int = INITIAL_KUDOS,
      helpReceived: Int = INITIAL_HELP_RECEIVED,
      followerCount: Int = INITIAL_FOLLOWER_COUNT,
      followingCount: Int = INITIAL_FOLLOWING_COUNT
  ): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        lastName = lastName,
        email = email,
        photo = null,
        kudos = kudos,
        helpReceived = helpReceived,
        section = UserSections.COMPUTER_SCIENCE,
        arrivalDate = Date(),
        followerCount = followerCount,
        followingCount = followingCount)
  }

  /** Asserts that the UI state matches expected values. */
  private fun assertUiState(
      expectedIsLoading: Boolean,
      expectedProfile: UserProfile? = null,
      expectedError: String? = null,
      expectedIsFollowing: Boolean = false
  ) {
    val state = viewModel.uiState.value
    assertEquals("Unexpected loading state", expectedIsLoading, state.isLoading)
    assertEquals("Unexpected profile", expectedProfile, state.profile)
    assertEquals("Unexpected error message", expectedError, state.error)
    assertEquals("Unexpected following state", expectedIsFollowing, state.isFollowing)
  }

  // ============ Tests for toggleFollow() ============

  @Test
  fun toggleFollow_whenNotFollowing_callsFollowUser() = runTest {
    // Given
    val profile = createTestUserProfile(id = TARGET_USER_ID, followerCount = 5)
    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } returns false
    coEvery { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) } just Runs
    coEvery { userProfileRepository.getUserProfile(TARGET_USER_ID) } returns profile

    // Set initial state with isFollowing = false
    viewModel.loadPublicProfile(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // When
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 1) { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) }
    coVerify(exactly = 0) { userProfileRepository.unfollowUser(any(), any()) }
    coVerify(exactly = 1) {
      userProfileRepository.getUserProfile(TARGET_USER_ID)
    } // CHANGED from 2 to 1
    assertEquals(
        6, viewModel.uiState.value.profile?.followerCount) // ADD THIS - verify optimistic increment
    assertEquals(true, viewModel.uiState.value.isFollowing) // ADD THIS
  }

  @Test
  fun toggleFollow_whenFollowing_callsUnfollowUser() = runTest {
    // Given
    val profile = createTestUserProfile(id = TARGET_USER_ID, followerCount = 10)
    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } returns true
    coEvery { userProfileRepository.unfollowUser(CURRENT_USER_ID, TARGET_USER_ID) } just Runs
    coEvery { userProfileRepository.getUserProfile(TARGET_USER_ID) } returns profile

    // Load profile and check following status to set isFollowing = true
    viewModel.loadPublicProfile(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.checkFollowingStatus(CURRENT_USER_ID, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // When
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 1) { userProfileRepository.unfollowUser(CURRENT_USER_ID, TARGET_USER_ID) }
    coVerify(exactly = 0) { userProfileRepository.followUser(any(), any()) }
    coVerify(exactly = 1) {
      userProfileRepository.getUserProfile(TARGET_USER_ID)
    } // CHANGED from 2 to 1
    assertEquals(
        9, viewModel.uiState.value.profile?.followerCount) // ADD THIS - verify optimistic decrement
    assertEquals(false, viewModel.uiState.value.isFollowing) // ADD THIS
  }

  @Test
  fun toggleFollow_whenNotLoggedIn_setsError() = runTest {
    // Given
    coEvery { userProfileRepository.getCurrentUserId() } returns ""

    // When
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedIsLoading = false, expectedError = ERROR_NOT_LOGGED_IN)
    coVerify(exactly = 0) { userProfileRepository.followUser(any(), any()) }
    coVerify(exactly = 0) { userProfileRepository.unfollowUser(any(), any()) }
  }

  @Test
  fun toggleFollow_whenIllegalArgumentException_setsError() = runTest {
    // Given
    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) } throws
        IllegalArgumentException(ERROR_CANNOT_FOLLOW_SELF)

    // When
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedIsLoading = false, expectedError = ERROR_CANNOT_FOLLOW_SELF)
  }

  @Test
  fun toggleFollow_whenIllegalStateException_setsError() = runTest {
    // Given
    val errorMessage = "Already following user"
    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) } throws
        IllegalStateException(errorMessage)

    // When
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedIsLoading = false, expectedError = errorMessage)
  }

  @Test
  fun toggleFollow_whenGenericException_setsError() = runTest {
    // Given
    val errorMessage = "Network error"
    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) } throws
        Exception(errorMessage)

    // When
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertTrue(viewModel.uiState.value.error?.contains(ERROR_FOLLOW_FAILED) == true)
    assertTrue(viewModel.uiState.value.error?.contains(errorMessage) == true)
  }

  @Test
  fun toggleFollow_updatesFollowerCountOptimistically() = runTest {
    // Given
    val initialProfile = createTestUserProfile(id = TARGET_USER_ID, followerCount = 5)

    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } returns false
    coEvery { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) } just Runs
    coEvery { userProfileRepository.getUserProfile(TARGET_USER_ID) } returns initialProfile

    // When - Load profile (this happens in the ViewModel test, not through the screen)
    viewModel.loadPublicProfile(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Manually check following status (simulating what the screen does)
    viewModel.checkFollowingStatus(CURRENT_USER_ID, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    val profileBeforeFollow = viewModel.uiState.value.profile
    assertEquals(5, profileBeforeFollow?.followerCount)
    assertEquals(false, viewModel.uiState.value.isFollowing)

    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Follower count incremented optimistically
    val profileAfterFollow = viewModel.uiState.value.profile
    assertEquals(6, profileAfterFollow?.followerCount)
    assertEquals(true, viewModel.uiState.value.isFollowing)

    // Verify profile only loaded once (not reloaded after toggle)
    coVerify(exactly = 1) { userProfileRepository.getUserProfile(TARGET_USER_ID) }
    // Verify follow was called
    coVerify(exactly = 1) { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) }
  }

  // ============ Tests for checkFollowingStatus() ============

  @Test
  fun checkFollowingStatus_whenFollowing_setsIsFollowingTrue() = runTest {
    // Given
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } returns true

    // When
    viewModel.checkFollowingStatus(CURRENT_USER_ID, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedIsLoading = false, expectedIsFollowing = true)
    coVerify(exactly = 1) { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) }
  }

  @Test
  fun checkFollowingStatus_whenNotFollowing_setsIsFollowingFalse() = runTest {
    // Given
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } returns false

    // When
    viewModel.checkFollowingStatus(CURRENT_USER_ID, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedIsLoading = false, expectedIsFollowing = false)
    coVerify(exactly = 1) { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) }
  }

  @Test
  fun checkFollowingStatus_whenCurrentUserIdBlank_doesNothing() = runTest {
    // Given
    val blankUserId = ""

    // When
    viewModel.checkFollowingStatus(blankUserId, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { userProfileRepository.isFollowing(any(), any()) }
  }

  @Test
  fun checkFollowingStatus_whenTargetUserIdBlank_doesNothing() = runTest {
    // Given
    val blankUserId = ""

    // When
    viewModel.checkFollowingStatus(CURRENT_USER_ID, blankUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { userProfileRepository.isFollowing(any(), any()) }
  }

  @Test
  fun checkFollowingStatus_whenBothIdsBlank_doesNothing() = runTest {
    // Given
    val blankUserId = ""

    // When
    viewModel.checkFollowingStatus(blankUserId, blankUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { userProfileRepository.isFollowing(any(), any()) }
  }

  @Test
  fun checkFollowingStatus_whenExceptionThrown_doesNotSetError() = runTest {
    // Given
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } throws
        Exception("Network error")

    // When
    viewModel.checkFollowingStatus(CURRENT_USER_ID, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(
        expectedIsLoading = false,
        expectedError = null, // Should silently fail
        expectedIsFollowing = false)
  }

  // ============ Integration Tests ============

  @Test
  fun fullFollowFlow_loadProfile_checkStatus_follow_success() = runTest {
    // Given
    val profile = createTestUserProfile(id = TARGET_USER_ID, followerCount = 5)
    val updatedProfile = createTestUserProfile(id = TARGET_USER_ID, followerCount = 6)

    coEvery { userProfileRepository.getCurrentUserId() } returns CURRENT_USER_ID
    coEvery { userProfileRepository.getUserProfile(TARGET_USER_ID) } returnsMany
        listOf(profile, updatedProfile)
    coEvery { userProfileRepository.isFollowing(CURRENT_USER_ID, TARGET_USER_ID) } returns false
    coEvery { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) } just Runs

    // When - Load profile
    viewModel.loadPublicProfile(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Profile loaded
    assertEquals(profile, viewModel.uiState.value.profile)
    assertEquals(false, viewModel.uiState.value.isFollowing)

    // When - Check following status
    viewModel.checkFollowingStatus(CURRENT_USER_ID, TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Not following
    assertEquals(false, viewModel.uiState.value.isFollowing)

    // When - Toggle follow
    viewModel.toggleFollow(TARGET_USER_ID)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Profile reloaded with updated follower count
    assertEquals(6, viewModel.uiState.value.profile?.followerCount)
    coVerify(exactly = 1) { userProfileRepository.followUser(CURRENT_USER_ID, TARGET_USER_ID) }
  }
}
