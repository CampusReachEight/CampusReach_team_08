package com.android.sample.ui.request

import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.overview.AcceptRequestViewModel
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
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AcceptRequestViewModelTests {

  private lateinit var viewModel: AcceptRequestViewModel
  private lateinit var mockRequestRepository: RequestRepository
  private lateinit var mockUserProfileRepository: UserProfileRepository
  private val testDispatcher = StandardTestDispatcher()

  private val testRequest =
      Request(
          requestId = "test123",
          title = "Test Request",
          description = "Test Description",
          requestType = emptyList(),
          location = Location(0.0, 0.0, "Test Location"),
          locationName = "Test Location",
          status = RequestStatus.OPEN,
          startTimeStamp = Date(),
          expirationTime = Date(),
          people = emptyList(),
          tags = emptyList(),
          creatorId = "creator123")

  private val testUserProfile =
      UserProfile(
          id = "creator123",
          name = "John",
          lastName = "Doe",
          email = "john@example.com",
          photo = null,
          kudos = 0,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRequestRepository = mock(RequestRepository::class.java)
    mockUserProfileRepository = mock(UserProfileRepository::class.java)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `loadRequest with valid user profile fetches creator name`() = runTest {
    // Given
    whenever(mockRequestRepository.getRequest("test123")).thenReturn(testRequest)
    whenever(mockRequestRepository.hasUserAcceptedRequest(testRequest)).thenReturn(false)
    whenever(mockUserProfileRepository.getUserProfile("creator123")).thenReturn(testUserProfile)

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository,
            userProfileRepository = mockUserProfileRepository)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertEquals("John Doe", state.creatorName)
    assertEquals(testRequest, state.request)
    assertFalse(state.accepted)
    assertNull(state.errorMsg)
  }

  @Test
  fun `loadRequest with empty lastName shows only first name`() = runTest {
    // Given
    val profileWithNoLastName = testUserProfile.copy(lastName = "")
    whenever(mockRequestRepository.getRequest("test123")).thenReturn(testRequest)
    whenever(mockRequestRepository.hasUserAcceptedRequest(testRequest)).thenReturn(false)
    whenever(mockUserProfileRepository.getUserProfile("creator123"))
        .thenReturn(profileWithNoLastName)

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository,
            userProfileRepository = mockUserProfileRepository)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertEquals("John", state.creatorName)
  }

  @Test
  fun `loadRequest with blank lastName shows only first name`() = runTest {
    // Given
    val profileWithBlankLastName = testUserProfile.copy(lastName = "   ")
    whenever(mockRequestRepository.getRequest("test123")).thenReturn(testRequest)
    whenever(mockRequestRepository.hasUserAcceptedRequest(testRequest)).thenReturn(false)
    whenever(mockUserProfileRepository.getUserProfile("creator123"))
        .thenReturn(profileWithBlankLastName)

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository,
            userProfileRepository = mockUserProfileRepository)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertEquals("John", state.creatorName)
  }

  @Test
  fun `loadRequest when user profile not found falls back to creatorId`() = runTest {
    // Given
    whenever(mockRequestRepository.getRequest("test123")).thenReturn(testRequest)
    whenever(mockRequestRepository.hasUserAcceptedRequest(testRequest)).thenReturn(false)
    whenever(mockUserProfileRepository.getUserProfile("creator123"))
        .thenThrow(NoSuchElementException("User not found"))

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository,
            userProfileRepository = mockUserProfileRepository)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertEquals("creator123", state.creatorName)
  }

  @Test
  fun `loadRequest with null userProfileRepository falls back to creatorId`() = runTest {
    // Given
    whenever(mockRequestRepository.getRequest("test123")).thenReturn(testRequest)
    whenever(mockRequestRepository.hasUserAcceptedRequest(testRequest)).thenReturn(false)

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository, userProfileRepository = null)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertEquals("creator123", state.creatorName)
  }

  @Test
  fun `loadRequest when already accepted shows accepted state`() = runTest {
    // Given
    whenever(mockRequestRepository.getRequest("test123")).thenReturn(testRequest)
    whenever(mockRequestRepository.hasUserAcceptedRequest(testRequest)).thenReturn(true)
    whenever(mockUserProfileRepository.getUserProfile("creator123")).thenReturn(testUserProfile)

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository,
            userProfileRepository = mockUserProfileRepository)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertTrue(state.accepted)
    assertEquals("John Doe", state.creatorName)
  }

  @Test
  fun `loadRequest with request repository failure sets error message`() = runTest {
    // Given
    whenever(mockRequestRepository.getRequest("test123"))
        .thenThrow(RuntimeException("Network error"))

    viewModel =
        AcceptRequestViewModel(
            requestRepository = mockRequestRepository,
            userProfileRepository = mockUserProfileRepository)

    // When
    viewModel.loadRequest("test123")
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg?.contains("Network error") == true)
  }
}
