package com.android.sample.ui.profile

import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.publicProfile.PublicProfileErrors
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTests {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadPublicProfile_blankId_setsEmptyProfileIdError() = runTest {
    val mockRepo = Mockito.mock(UserProfileRepository::class.java)
    val vm = PublicProfileViewModel(mockRepo)

    vm.loadPublicProfile("")

    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(false, state.isLoading)
    assertNull(state.profile)
    assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
  }

  @Test
  fun loadPublicProfile_repoReturnsNull_setsFailedToLoad() = runTest {
    val mockRepo = Mockito.mock(UserProfileRepository::class.java)
    whenever(mockRepo.getUserProfile("u1")).thenReturn(null)

    val vm = PublicProfileViewModel(mockRepo)
    vm.loadPublicProfile("u1")
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(false, state.isLoading)
    assertNull(state.profile)
    assertEquals(PublicProfileErrors.FAILED_TO_LOAD, state.error)
  }

  @Test
  fun loadPublicProfile_repoThrows_setsExceptionMessage() = runTest {
    val mockRepo = Mockito.mock(UserProfileRepository::class.java)
    whenever(mockRepo.getUserProfile("u1")).thenThrow(RuntimeException("boom"))

    val vm = PublicProfileViewModel(mockRepo)
    vm.loadPublicProfile("u1")
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(false, state.isLoading)
    assertNull(state.profile)
    // VM uses exception.message when available
    assertEquals("boom", state.error)
  }

  @Test
  fun refresh_withoutLastId_setsEmptyProfileIdError() = runTest {
    val mockRepo = Mockito.mock(UserProfileRepository::class.java)
    val vm = PublicProfileViewModel(mockRepo)

    vm.refresh()
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
  }

  @Test
  fun refresh_withLastId_callsLoadAgain() = runTest {
    val mockRepo = Mockito.mock(UserProfileRepository::class.java)
    val calls = AtomicInteger(0)

    // count invocations and return null
    whenever(mockRepo.getUserProfile("xyz")).thenAnswer {
      calls.incrementAndGet()
      null
    }

    val vm = PublicProfileViewModel(mockRepo)

    vm.loadPublicProfile("xyz")
    advanceUntilIdle()
    assertEquals(1, calls.get())

    vm.refresh()
    advanceUntilIdle()
    assertEquals(2, calls.get())
  }
}
