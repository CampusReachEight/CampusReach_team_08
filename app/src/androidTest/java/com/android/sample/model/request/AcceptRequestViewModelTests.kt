package com.android.sample.model.request

import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
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
    mockRequestRepository = Mockito.mock(RequestRepository::class.java)
    mockUserProfileRepository = Mockito.mock(UserProfileRepository::class.java)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadRequest_withValidUserProfile_fetchesCreatorName() = runTest {
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
    Assert.assertEquals("John Doe", state.creatorName)
    Assert.assertEquals(testRequest, state.request)
    Assert.assertFalse(state.accepted)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun loadRequest_withEmptyLastName_showsOnlyFirstName() = runTest {
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
    Assert.assertEquals("John", state.creatorName)
  }

  @Test
  fun loadRequest_withBlankLastName_showsOnlyFirstName() = runTest {
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
    Assert.assertEquals("John", state.creatorName)
  }

  @Test
  fun loadRequest_whenUserProfileNotFound_fallsBackToCreatorId() = runTest {
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
    Assert.assertEquals("creator123", state.creatorName)
  }

  @Test
  fun loadRequest_withNullUserProfileRepository_fallsBackToCreatorId() = runTest {
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
    Assert.assertEquals("creator123", state.creatorName)
  }

  @Test
  fun loadRequest_whenAlreadyAccepted_showsAcceptedState() = runTest {
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
    Assert.assertTrue(state.accepted)
    Assert.assertEquals("John Doe", state.creatorName)
  }

  @Test
  fun loadRequest_withRequestRepositoryFailure_setsErrorMessage() = runTest {
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
    Assert.assertNotNull(state.errorMsg)
    Assert.assertTrue(state.errorMsg?.contains("Network error") == true)
  }
}
