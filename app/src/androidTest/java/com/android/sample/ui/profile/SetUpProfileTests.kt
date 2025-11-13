package com.android.sample.ui.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.profile.setup.SetupProfileRepository
import com.android.sample.ui.profile.setup.SetupProfileEvent
import com.android.sample.ui.profile.setup.SetupProfileState
import com.android.sample.ui.profile.setup.SetupProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class FakeSetupProfileRepository(
    @Volatile var profileToReturn: SetupProfileState = SetupProfileState.default(),
    var failOnLoad: Boolean = false,
    var failOnSave: Boolean = false
) : SetupProfileRepository {

  @Volatile var lastSaved: SetupProfileState? = null

  override suspend fun getProfile(): SetupProfileState {
    if (failOnLoad) throw RuntimeException("load failed")
    return profileToReturn
  }

  override suspend fun saveProfile(state: SetupProfileState) {
    if (failOnSave) throw RuntimeException("save failed")
    lastSaved = state
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SetUpProfileTests {

  private val dispatcher = StandardTestDispatcher()
  private val scope = TestScope(dispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initLoadsProfileSuccessfully() =
      scope.runTest {
        val expected = SetupProfileState.default().copy(userName = "Alice")
        val repo = FakeSetupProfileRepository(profileToReturn = expected)
        val vm = SetupProfileViewModel(repository = repo)

        advanceUntilIdle()

        assertEquals(expected, vm.state.value)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.errorMessage)
      }

  @Test
  fun initLoadFailureSetsErrorMessage() =
      scope.runTest {
        val repo = FakeSetupProfileRepository(failOnLoad = true)
        val vm = SetupProfileViewModel(repository = repo)

        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.errorMessage)
      }

  @Test
  fun saveEventPersistsAndSetsSavedOnSuccess() =
      scope.runTest {
        val initial = SetupProfileState.default().copy(userName = "ToSave")
        val repo = FakeSetupProfileRepository(profileToReturn = initial)
        val vm = SetupProfileViewModel(repository = repo)

        advanceUntilIdle()

        vm.onEvent(SetupProfileEvent.Save)
        advanceUntilIdle()

        assertEquals(vm.state.value, repo.lastSaved)
        assertFalse(vm.state.value.isSaving)
        assertTrue(vm.state.value.saved)
        assertNull(vm.state.value.saveError)
      }

  @Test
  fun saveEventFailureSetsSaveErrorAndDoesNotMarkSaved() =
      scope.runTest {
        val initial = SetupProfileState.default().copy(userName = "WillFail")
        val repo = FakeSetupProfileRepository(profileToReturn = initial, failOnSave = true)
        val vm = SetupProfileViewModel(repository = repo)

        advanceUntilIdle()

        vm.onEvent(SetupProfileEvent.Save)
        advanceUntilIdle()

        assertFalse(vm.state.value.isSaving)
        assertFalse(vm.state.value.saved)
        assertNotNull(vm.state.value.saveError)
        assertNull(repo.lastSaved)
      }

  @Test
  fun onEventLoadRefetchesUpdatedProfileFromRepository() =
      scope.runTest {
        val initial = SetupProfileState.default().copy(userName = "Initial")
        val updated = initial.copy(userName = "LoadedAgain")
        val repo = FakeSetupProfileRepository(profileToReturn = initial)
        val vm = SetupProfileViewModel(repository = repo)

        advanceUntilIdle()

        // update repository backing data and request reload
        repo.profileToReturn = updated
        vm.onEvent(SetupProfileEvent.Load)
        advanceUntilIdle()

        assertEquals(updated.userName, vm.state.value.userName)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.errorMessage)
      }
}
