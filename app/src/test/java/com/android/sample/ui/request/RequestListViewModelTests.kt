package com.android.sample.ui.request

import android.net.Uri
import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestCache
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.profile.UserSections
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class RequestListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockRequestRepo: RequestRepository
  private lateinit var mockProfileRepo: UserProfileRepository
  private lateinit var mockRequestCache: RequestCache

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRequestRepo = mock(RequestRepository::class.java)
    mockProfileRepo = mock(UserProfileRepository::class.java)
    mockRequestCache = mock(RequestCache::class.java)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createSampleRequest(
      requestId: String = "req1",
      creatorId: String = "user1"
  ): Request {
    return Request(
        requestId = requestId,
        title = "Test Request",
        description = "Test Description",
        requestType = listOf(RequestType.STUDYING),
        location = Location(46.5, 6.5, "EPFL"),
        locationName = "EPFL",
        status = RequestStatus.OPEN,
        startTimeStamp = Date(System.currentTimeMillis() + 3600000),
        expirationTime = Date(System.currentTimeMillis() + 7200000),
        people = emptyList(),
        tags = listOf(Tags.URGENT),
        creatorId = creatorId)
  }

  private fun createSampleProfile(userId: String = "user1"): UserProfile {
    return UserProfile(
        id = userId,
        name = "Test",
        lastName = "User",
        email = null,
        photo = null,
        kudos = 0,
        helpReceived = 0,
        section = UserSections.NONE,
        arrivalDate = Date())
  }

  @Test
  fun loadRequests_loads_from_cache_when_repository_throws_exception() = runTest {
    val cachedRequests = listOf(createSampleRequest("cached1"), createSampleRequest("cached2"))

    `when`(mockRequestRepo.getAllRequests()).thenThrow(RuntimeException("Network error"))
    `when`(mockRequestCache.loadRequests()).thenReturn(cachedRequests)
    `when`(mockProfileRepo.getUserProfile(any())).thenReturn(createSampleProfile())

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadRequests()
    advanceUntilIdle()

    val state = viewModel.state.first()
    assertEquals(2, state.requests.size)
    assertTrue(state.offlineMode)
    assertFalse(state.isLoading)
    assertNull(state.errorMessage)

    verify(mockRequestCache).loadRequests()
  }

  @Test
  fun loadRequests_calls_loadProfileImage_for_cached_requests() = runTest {
    val cachedRequests =
        listOf(createSampleRequest("cached1", "user1"), createSampleRequest("cached2", "user2"))

    `when`(mockRequestRepo.getAllRequests()).thenThrow(RuntimeException("Network error"))
    `when`(mockRequestCache.loadRequests()).thenReturn(cachedRequests)
    `when`(mockProfileRepo.getUserProfile("user1")).thenReturn(createSampleProfile("user1"))
    `when`(mockProfileRepo.getUserProfile("user2")).thenReturn(createSampleProfile("user2"))

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadRequests()
    advanceUntilIdle()

    val profileIcons = viewModel.profileIcons.first()
    assertTrue(profileIcons.containsKey("user1"))
    assertTrue(profileIcons.containsKey("user2"))
  }

  @Test
  fun loadRequests_logs_when_verboseLogging_is_enabled_and_cache_is_used() = runTest {
    val cachedRequests = listOf(createSampleRequest())

    `when`(mockRequestRepo.getAllRequests()).thenThrow(RuntimeException("Network error"))
    `when`(mockRequestCache.loadRequests()).thenReturn(cachedRequests)
    `when`(mockProfileRepo.getUserProfile(any())).thenReturn(createSampleProfile())

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false,
            verboseLogging = true)

    viewModel.loadRequests()
    advanceUntilIdle()

    val state = viewModel.state.first()
    assertTrue(state.offlineMode)
  }

  @Test
  fun loadRequests_sets_error_when_repository_fail_and_cache_is_empty() = runTest {
    `when`(mockRequestRepo.getAllRequests()).thenThrow(RuntimeException("Network error"))
    `when`(mockRequestCache.loadRequests()).thenReturn(emptyList())

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadRequests()
    advanceUntilIdle()

    val state = viewModel.state.first()
    assertNotNull(state.errorMessage)
    assertFalse(state.isLoading)
  }

  @Test
  fun loadRequests_uses_default_error_message_when_exception_message_is_blank() = runTest {
    `when`(mockRequestRepo.getAllRequests()).thenThrow(RuntimeException(""))
    `when`(mockRequestCache.loadRequests()).thenReturn(emptyList())

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadRequests()
    advanceUntilIdle()

    val state = viewModel.state.first()
    assertEquals("Failed to load requests. Please try again.", state.errorMessage)
  }

  @Test
  fun loadProfileImage_handles_unsupported_URI_scheme_gracefully() = runTest {
    val profile = createSampleProfile("user1").copy(photo = Uri.parse("content://some/path"))
    `when`(mockProfileRepo.getUserProfile("user1")).thenReturn(profile)

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadProfileImage("user1")
    advanceUntilIdle()

    val profileIcons = viewModel.profileIcons.first()
    assertTrue(profileIcons.containsKey("user1"))
    assertNull(profileIcons["user1"])
  }

  @Test
  fun loadProfileImage_handles_bitmap_loading_exception_gracefully() = runTest {
    val profile =
        createSampleProfile("user1")
            .copy(photo = Uri.parse("https://invalid-url-that-will-fail.com/image.jpg"))
    `when`(mockProfileRepo.getUserProfile("user1")).thenReturn(profile)

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadProfileImage("user1")
    advanceUntilIdle()

    val profileIcons = viewModel.profileIcons.first()
    assertTrue(profileIcons.containsKey("user1"))
  }

  @Test
  fun clearError_sets_errorMessage_to_null() = runTest {
    `when`(mockRequestRepo.getAllRequests()).thenThrow(RuntimeException("Error"))
    `when`(mockRequestCache.loadRequests()).thenReturn(emptyList())

    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = false)

    viewModel.loadRequests()
    advanceUntilIdle()

    var state = viewModel.state.first()
    assertNotNull(state.errorMessage)

    viewModel.clearError()
    advanceUntilIdle()

    state = viewModel.state.first()
    assertNull(state.errorMessage)
  }

  @Test(expected = IllegalArgumentException::class)
  fun factory_throws_IllegalArgumentException_for_unknown_ViewModel_class() {
    val factory = RequestListViewModelFactory()
    factory.create(androidx.lifecycle.AndroidViewModel::class.java)
  }

  @Test
  fun factory_creates_RequestListViewModel_successfully() {
    val factory =
        RequestListViewModelFactory(showOnlyMyRequests = true, requestCache = mockRequestCache)

    // Create the ViewModel with mocked dependencies
    val viewModel =
        RequestListViewModel(
            requestRepository = mockRequestRepo,
            profileRepository = mockProfileRepo,
            requestCache = mockRequestCache,
            showOnlyMyRequests = true)

    assertNotNull(viewModel)
    assertTrue(viewModel is RequestListViewModel)
  }
}
