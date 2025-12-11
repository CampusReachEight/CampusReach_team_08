package com.android.sample.ui.profile.follow

import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.UserSections
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FollowListViewModelTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"
    const val USER_ID_1 = "user1"
    const val USER_ID_2 = "user2"
    const val USER_ID_3 = "user3"

    const val NAME_ALICE = "Alice"
    const val LASTNAME_SMITH = "Smith"
    const val EMAIL_ALICE = "alice@test.com"

    const val NAME_BOB = "Bob"
    const val LASTNAME_JOHNSON = "Johnson"
    const val EMAIL_BOB = "bob@test.com"

    const val NAME_CHARLIE = "Charlie"
    const val EMAIL_CHARLIE = "charlie@test.com"

    const val KUDOS_10 = 10
    const val KUDOS_20 = 20
    const val KUDOS_15 = 15

    const val HELP_5 = 5
    const val HELP_8 = 8
    const val HELP_3 = 3

    const val ERROR_NETWORK = "Network error"
    const val ERROR_DATABASE = "Database error"
    const val ERROR_NOT_FOUND = "Profile not found"

    const val EXPECTED_ZERO_USERS = 0
    const val EXPECTED_ONE_USER = 1
    const val EXPECTED_TWO_USERS = 2
    const val EXPECTED_THREE_USERS = 3
  }

  // ============ Test Fixtures ============
  private lateinit var mockRepository: UserProfileRepository
  private lateinit var viewModel: FollowListViewModel

  private val testDispatcher = StandardTestDispatcher()

  private val testUser1 =
      UserProfile(
          id = USER_ID_1,
          name = NAME_ALICE,
          lastName = LASTNAME_SMITH,
          email = EMAIL_ALICE,
          photo = null,
          kudos = KUDOS_10,
          helpReceived = HELP_5,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())

  private val testUser2 =
      UserProfile(
          id = USER_ID_2,
          name = NAME_BOB,
          lastName = LASTNAME_JOHNSON,
          email = EMAIL_BOB,
          photo = null,
          kudos = KUDOS_20,
          helpReceived = HELP_8,
          section = UserSections.MATHEMATICS,
          arrivalDate = Date())

  private val testUser3 =
      UserProfile(
          id = USER_ID_3,
          name = NAME_CHARLIE,
          lastName = "",
          email = EMAIL_CHARLIE,
          photo = null,
          kudos = KUDOS_15,
          helpReceived = HELP_3,
          section = UserSections.NONE,
          arrivalDate = Date())

  // ============ Test Lifecycle ============

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ============ Helper Methods ============

  /** Initializes ViewModel and waits for load to complete. */
  private suspend fun initializeViewModel(listType: FollowListType) {
    viewModel = FollowListViewModel(CURRENT_USER_ID, listType, mockRepository)
    viewModel.loadUsers()
    testDispatcher.scheduler.advanceUntilIdle()
  }

  /** Asserts that the UI state matches expected values. */
  private fun assertUiState(
      expectedUserCount: Int,
      expectedIsLoading: Boolean,
      expectedErrorMessage: String? = null
  ) {
    val state = viewModel.state.value
    assertEquals("Unexpected user count", expectedUserCount, state.users.size)
    assertEquals("Unexpected loading state", expectedIsLoading, state.isLoading)
    assertEquals("Unexpected error message", expectedErrorMessage, state.errorMessage)
  }

  /** Asserts user at given index matches expected profile. */
  private fun assertUserAtIndex(index: Int, expectedName: String, expectedLastName: String) {
    val user = viewModel.state.value.users[index]
    assertEquals("Unexpected name at index $index", expectedName, user.name)
    assertEquals("Unexpected last name at index $index", expectedLastName, user.lastName)
  }

  // ============ Tests for Initial State ============

  @Test
  fun initialState_isEmpty() {
    viewModel = FollowListViewModel(CURRENT_USER_ID, FollowListType.FOLLOWERS, mockRepository)

    val state = viewModel.state.value
    assertTrue("Initial users should be empty", state.users.isEmpty())
    assertFalse("Initial loading should be false", state.isLoading)
    assertNull("Initial error should be null", state.errorMessage)
  }

  // ============ Tests for Followers Loading ============

  @Test
  fun loadUsers_withFollowers_updatesStateCorrectly() = runTest {
    // Given
    val followerIds = listOf(USER_ID_1, USER_ID_2)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } returns testUser1
    coEvery { mockRepository.getUserProfile(USER_ID_2) } returns testUser2

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_TWO_USERS, expectedIsLoading = false)
    assertUserAtIndex(0, NAME_ALICE, LASTNAME_SMITH)
    assertUserAtIndex(1, NAME_BOB, LASTNAME_JOHNSON)
    coVerify(exactly = 1) { mockRepository.getFollowerIds(CURRENT_USER_ID) }
  }

  @Test
  fun loadUsers_withEmptyFollowers_updatesStateCorrectly() = runTest {
    // Given
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns emptyList()

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_ZERO_USERS, expectedIsLoading = false)
  }

  @Test
  fun loadUsers_withSingleFollower_updatesStateCorrectly() = runTest {
    // Given
    val followerIds = listOf(USER_ID_1)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } returns testUser1

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_ONE_USER, expectedIsLoading = false)
    assertUserAtIndex(0, NAME_ALICE, LASTNAME_SMITH)
  }

  // ============ Tests for Following Loading ============

  @Test
  fun loadUsers_withFollowing_updatesStateCorrectly() = runTest {
    // Given
    val followingIds = listOf(USER_ID_2, USER_ID_3)
    coEvery { mockRepository.getFollowingIds(CURRENT_USER_ID) } returns followingIds
    coEvery { mockRepository.getUserProfile(USER_ID_2) } returns testUser2
    coEvery { mockRepository.getUserProfile(USER_ID_3) } returns testUser3

    // When
    initializeViewModel(FollowListType.FOLLOWING)

    // Then
    assertUiState(expectedUserCount = EXPECTED_TWO_USERS, expectedIsLoading = false)
    assertUserAtIndex(0, NAME_BOB, LASTNAME_JOHNSON)
    assertUserAtIndex(1, NAME_CHARLIE, "")
    coVerify(exactly = 1) { mockRepository.getFollowingIds(CURRENT_USER_ID) }
  }

  @Test
  fun loadUsers_withEmptyFollowing_updatesStateCorrectly() = runTest {
    // Given
    coEvery { mockRepository.getFollowingIds(CURRENT_USER_ID) } returns emptyList()

    // When
    initializeViewModel(FollowListType.FOLLOWING)

    // Then
    assertUiState(expectedUserCount = EXPECTED_ZERO_USERS, expectedIsLoading = false)
  }

  // ============ Tests for Loading State Management ============

  @Test
  fun loadUsers_setsLoadingState_thenCompletesSuccessfully() = runTest {
    // Given
    val followerIds = listOf(USER_ID_1)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } returns testUser1

    // When
    viewModel = FollowListViewModel(CURRENT_USER_ID, FollowListType.FOLLOWERS, mockRepository)

    // Then - Initially not loading
    assertFalse("Should not be loading initially", viewModel.state.value.isLoading)

    viewModel.loadUsers()

    // Complete loading
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Loading complete
    assertUiState(expectedUserCount = EXPECTED_ONE_USER, expectedIsLoading = false)
  }

  // ============ Tests for Error Handling ============

  @Test
  fun loadUsers_whenGetFollowerIdsFails_setsErrorMessage() = runTest {
    // Given
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } throws Exception(ERROR_NETWORK)

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    val state = viewModel.state.value
    assertEquals("Unexpected user count", EXPECTED_ZERO_USERS, state.users.size)
    assertFalse("Should not be loading", state.isLoading)
    assertTrue(
        "Error should contain 'Failed to load users'",
        state.errorMessage?.contains("Failed to load users") == true)
    assertTrue(
        "Error should contain network error", state.errorMessage?.contains(ERROR_NETWORK) == true)
  }

  @Test
  fun loadUsers_whenGetFollowingIdsFails_setsErrorMessage() = runTest {
    // Given
    coEvery { mockRepository.getFollowingIds(CURRENT_USER_ID) } throws Exception(ERROR_DATABASE)

    // When
    initializeViewModel(FollowListType.FOLLOWING)

    // Then
    val state = viewModel.state.value
    assertEquals("Unexpected user count", EXPECTED_ZERO_USERS, state.users.size)
    assertFalse("Should not be loading", state.isLoading)
    assertTrue(
        "Error should contain 'Failed to load users'",
        state.errorMessage?.contains("Failed to load users") == true)
    assertTrue(
        "Error should contain database error", state.errorMessage?.contains(ERROR_DATABASE) == true)
  }

  @Test
  fun loadUsers_whenSomeProfilesFail_skipsFailedProfiles() = runTest {
    // Given
    val followerIds = listOf(USER_ID_1, USER_ID_2, USER_ID_3)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } returns testUser1
    coEvery { mockRepository.getUserProfile(USER_ID_2) } throws Exception(ERROR_NOT_FOUND)
    coEvery { mockRepository.getUserProfile(USER_ID_3) } returns testUser3

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_TWO_USERS, expectedIsLoading = false)
    assertUserAtIndex(0, NAME_ALICE, LASTNAME_SMITH)
    assertUserAtIndex(1, NAME_CHARLIE, "")
    assertNull("No error message when some profiles succeed", viewModel.state.value.errorMessage)
  }

  @Test
  fun loadUsers_whenAllProfilesFail_returnsEmptyList() = runTest {
    // Given
    val followerIds = listOf(USER_ID_1, USER_ID_2)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } throws Exception(ERROR_NOT_FOUND)
    coEvery { mockRepository.getUserProfile(USER_ID_2) } throws Exception(ERROR_NOT_FOUND)

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_ZERO_USERS, expectedIsLoading = false)
    assertNull("No error when IDs load but profiles fail", viewModel.state.value.errorMessage)
  }

  // ============ Tests for Multiple Load Calls ============

  @Test
  fun loadUsers_calledMultipleTimes_updatesStateEachTime() = runTest {
    // Given - First load
    val firstFollowerIds = listOf(USER_ID_1)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns firstFollowerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } returns testUser1

    // When - First load
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then - First load
    assertUiState(expectedUserCount = EXPECTED_ONE_USER, expectedIsLoading = false)

    // Given - Second load with different data
    val secondFollowerIds = listOf(USER_ID_2, USER_ID_3)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns secondFollowerIds
    coEvery { mockRepository.getUserProfile(USER_ID_2) } returns testUser2
    coEvery { mockRepository.getUserProfile(USER_ID_3) } returns testUser3

    // When - Second load
    viewModel.loadUsers()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Second load
    assertUiState(expectedUserCount = EXPECTED_TWO_USERS, expectedIsLoading = false)
    assertUserAtIndex(0, NAME_BOB, LASTNAME_JOHNSON)
    assertUserAtIndex(1, NAME_CHARLIE, "")
  }

  // ============ Tests for State Data Class ============

  @Test
  fun followListState_defaultValues_areCorrect() {
    val state = FollowListState()

    assertTrue("Default users should be empty", state.users.isEmpty())
    assertFalse("Default loading should be false", state.isLoading)
    assertNull("Default error should be null", state.errorMessage)
  }

  @Test
  fun followListState_withCustomValues_storesCorrectly() {
    val users = listOf(testUser1, testUser2)
    val state = FollowListState(users = users, isLoading = true, errorMessage = "Test error")

    assertEquals("Custom user count should be 2", EXPECTED_TWO_USERS, state.users.size)
    assertTrue("Custom loading should be true", state.isLoading)
    assertEquals("Custom error should match", "Test error", state.errorMessage)
  }

  // ============ Tests for User Profile Handling ============

  @Test
  fun loadUsers_withUserHavingNoLastName_loadsCorrectly() = runTest {
    // Given
    val followerIds = listOf(USER_ID_3)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_3) } returns testUser3

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_ONE_USER, expectedIsLoading = false)
    assertUserAtIndex(0, NAME_CHARLIE, "")
  }

  @Test
  fun loadUsers_withMixedSections_loadsCorrectly() = runTest {
    // Given
    val followerIds = listOf(USER_ID_1, USER_ID_2, USER_ID_3)
    coEvery { mockRepository.getFollowerIds(CURRENT_USER_ID) } returns followerIds
    coEvery { mockRepository.getUserProfile(USER_ID_1) } returns testUser1
    coEvery { mockRepository.getUserProfile(USER_ID_2) } returns testUser2
    coEvery { mockRepository.getUserProfile(USER_ID_3) } returns testUser3

    // When
    initializeViewModel(FollowListType.FOLLOWERS)

    // Then
    assertUiState(expectedUserCount = EXPECTED_THREE_USERS, expectedIsLoading = false)
    assertEquals(
        "User1 should have COMPUTER_SCIENCE section",
        UserSections.COMPUTER_SCIENCE,
        viewModel.state.value.users[0].section)
    assertEquals(
        "User2 should have MATHEMATICS section",
        UserSections.MATHEMATICS,
        viewModel.state.value.users[1].section)
    assertEquals(
        "User3 should have NONE section", UserSections.NONE, viewModel.state.value.users[2].section)
  }
}
