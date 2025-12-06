package com.android.sample.ui.leaderboard

import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.UserSections
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private fun profile(id: String, name: String): UserProfile =
      UserProfile(
          id = id,
          name = name,
          lastName = "Doe",
          email = "$name.doe@example.com",
          photo = null,
          kudos = 10,
          helpReceived = 1,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadProfiles_success_updates_state() =
      runTest(testDispatcher) {
        val repo =
            object : UserProfileRepository by ThrowingUserProfileRepository() {
              override suspend fun getAllUserProfiles(): List<UserProfile> =
                  listOf(profile("1", "Alice"))
            }

        val vm = LeaderboardViewModel(profileRepository = repo, profileCache = null)

        vm.loadProfiles()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.offlineMode)
        assertEquals(1, state.profiles.size)
        assertEquals("Alice", state.profiles.first().name)
      }

  @Test
  fun loadProfiles_failure_sets_error() =
      runTest(testDispatcher) {
        val repo =
            object : UserProfileRepository by ThrowingUserProfileRepository() {
              override suspend fun getAllUserProfiles(): List<UserProfile> {
                throw IllegalStateException("network")
              }
            }

        val vm = LeaderboardViewModel(profileRepository = repo, profileCache = null)

        vm.loadProfiles()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage != null)
        assertTrue(state.profiles.isEmpty())
        assertFalse(state.offlineMode)
      }

  @Test
  fun clearError_resets_errorMessage() =
      runTest(testDispatcher) {
        val repo =
            object : UserProfileRepository by ThrowingUserProfileRepository() {
              override suspend fun getAllUserProfiles(): List<UserProfile> {
                throw IllegalStateException("network")
              }
            }

        val vm = LeaderboardViewModel(profileRepository = repo, profileCache = null)

        vm.loadProfiles()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errorMessage)

        vm.clearError()
        assertNull(vm.state.value.errorMessage)
      }

  private open class ThrowingUserProfileRepository : UserProfileRepository {
    override fun getNewUid(): String = error("Not implemented")

    override fun getCurrentUserId(): String = error("Not implemented")

    override suspend fun getAllUserProfiles(): List<UserProfile> = error("Not implemented")

    override suspend fun getUserProfile(userId: String): UserProfile = error("Not implemented")

    override suspend fun addUserProfile(userProfile: UserProfile): Unit = error("Not implemented")

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile): Unit =
        error("Not implemented")

    override suspend fun deleteUserProfile(userId: String): Unit = error("Not implemented")

    override suspend fun awardKudos(userId: String, amount: Int): Unit = error("Not implemented")

    override suspend fun awardKudosBatch(awards: Map<String, Int>): Unit = error("Not implemented")

    override suspend fun receiveHelp(userId: String, amount: Int): Unit = error("Not implemented")
  }
}
