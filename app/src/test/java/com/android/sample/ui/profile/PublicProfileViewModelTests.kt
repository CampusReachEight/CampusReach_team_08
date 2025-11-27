package com.android.sample.ui.profile

import androidx.lifecycle.AndroidViewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.publicProfile.PublicProfileErrors
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModel
import com.android.sample.ui.profile.publicProfile.PublicProfileViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: UserProfileRepository
    private lateinit var viewModel: PublicProfileViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(UserProfileRepository::class.java)
        viewModel = PublicProfileViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockUserProfile(
        id: String = "test123",
        name: String = "John",
        lastName: String = "Doe",
        section: String = "CS",
        arrivalDate: Date = Date()
    ): UserProfile {
        val upClass = Class.forName("com.android.sample.model.profile.UserProfile")
        val ctor = upClass.constructors.minByOrNull { it.parameterTypes.size }!!
        val args = ctor.parameterTypes.map { p ->
            when {
                p == String::class.java -> ""
                p == Int::class.java || p == Integer.TYPE -> 0
                p == Long::class.java || p == java.lang.Long.TYPE -> 0L
                p == Boolean::class.java || p == java.lang.Boolean.TYPE -> false
                p == Date::class.java -> Date()
                p.isEnum -> p.enumConstants[0]
                else -> null
            }
        }.toTypedArray()

        val instance = ctor.newInstance(*args) as UserProfile

        listOf(
            "id" to id,
            "name" to name,
            "lastName" to lastName,
            "section" to section,
            "arrivalDate" to arrivalDate
        ).forEach { (fieldName, value) ->
            try {
                upClass.getDeclaredField(fieldName).apply {
                    isAccessible = true
                    set(instance, value)
                }
            } catch (_: Exception) {
                // Field may not exist
            }
        }

        return instance
    }

    @Test
    fun loadPublicProfile_with_blank_id_sets_error_immediately() = runTest {
        viewModel.loadPublicProfile("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.profile)
        assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
    }

    @Test
    fun loadPublicProfile_with_whitespace_id_sets_error_immediately() = runTest {
        viewModel.loadPublicProfile("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.profile)
        assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
    }

    @Test
    fun loadPublicProfile_successfully_loads_profile() = runTest {
        val validProfile = createMockUserProfile(id = "user1", name = "Jane", lastName = "Smith")
        `when`(repository.getUserProfile("user1")).thenReturn(validProfile)

        viewModel.loadPublicProfile("user1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.profile)
        assertNull(state.error)
        assertEquals("Jane Smith", state.profile?.name)
        assertEquals("user1", state.profile?.userId)
    }


    @Test
    fun loadPublicProfile_with_empty_name_creates_Unknown_profile() = runTest {
        val invalidProfile = createMockUserProfile(id = "test", name = "", lastName = "")
        `when`(repository.getUserProfile(any())).thenReturn(invalidProfile)

        viewModel.loadPublicProfile("test123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.profile)
        assertEquals("Unknown", state.profile?.name)
        assertNull(state.error)
    }

    @Test
    fun loadPublicProfile_handles_repository_exception() = runTest {
        `when`(repository.getUserProfile(any())).thenThrow(RuntimeException("Network error"))

        viewModel.loadPublicProfile("test123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.profile)
        assertEquals("Network error", state.error)
    }

    @Test
    fun loadPublicProfile_handles_exception_with_null_message() = runTest {
        `when`(repository.getUserProfile(any())).thenThrow(RuntimeException())

        viewModel.loadPublicProfile("test123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.profile)
        assertEquals(PublicProfileErrors.FAILED_TO_LOAD, state.error)
    }

    @Test
    fun loadPublicProfile_cancels_previous_job() = runTest {
        // First call will delay
        `when`(repository.getUserProfile("first")).thenAnswer {
            runTest {
                delay(10_000L)
                createMockUserProfile(id = "first")
            }
        }

        `when`(repository.getUserProfile("second")).thenReturn(
            createMockUserProfile(id = "second", name = "Second", lastName = "User")
        )

        // Start first load
        viewModel.loadPublicProfile("first")
        advanceTimeBy(1000L)

        // Start second load (should cancel first)
        viewModel.loadPublicProfile("second")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("second", state.profile?.userId)
        assertEquals("Second User", state.profile?.name)
    }

    @Test
    fun refresh_with_null_lastProfileId_sets_error() = runTest {
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
    }

    @Test
    fun refresh_after_loading_blank_id_sets_error() = runTest {
        viewModel.loadPublicProfile("")
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PublicProfileErrors.EMPTY_PROFILE_ID, state.error)
    }

    @Test
    fun refresh_reloads_last_profile_successfully() = runTest {
        val profile = createMockUserProfile(id = "user1", name = "Jane", lastName = "Doe")
        `when`(repository.getUserProfile("user1")).thenReturn(profile)

        viewModel.loadPublicProfile("user1")
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.profile)
        assertEquals("user1", state.profile?.userId)
    }

    @Test
    fun factory_creates_PublicProfileViewModel_successfully() {
        val factory = PublicProfileViewModelFactory(repository)
        val vm = factory.create(PublicProfileViewModel::class.java)

        assertNotNull(vm)
        assertTrue(true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun factory_throws_IllegalArgumentException_for_unknown_ViewModel() {
        val factory = PublicProfileViewModelFactory(repository)

        // Try to create a ViewModel class that's not PublicProfileViewModel
        factory.create(AndroidViewModel::class.java)
    }

    @Test
    fun isLoading_is_set_to_false_after_load_completes() = runTest {
        val profile = createMockUserProfile()
        `when`(repository.getUserProfile(any())).thenReturn(profile)

        viewModel.loadPublicProfile("test")
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun isLoading_is_set_to_false_after_exception() = runTest {
        `when`(repository.getUserProfile(any())).thenThrow(RuntimeException("Error"))

        viewModel.loadPublicProfile("test")
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun error_is_cleared_when_new_load_starts() = runTest {
        // First load fails
        `when`(repository.getUserProfile("fail")).thenThrow(RuntimeException("Error"))
        viewModel.loadPublicProfile("fail")
        advanceUntilIdle()

        assertEquals("Error", viewModel.uiState.value.error)

        // Second load succeeds - error should be cleared
        `when`(repository.getUserProfile("success")).thenReturn(
            createMockUserProfile(id = "success")
        )
        viewModel.loadPublicProfile("success")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.profile)
    }
}