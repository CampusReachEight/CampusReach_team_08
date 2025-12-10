package com.android.sample.ui.accepted_requests

import androidx.lifecycle.AndroidViewModel
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.profile.accepted_requests.AcceptedRequestsViewModel
import com.android.sample.ui.profile.accepted_requests.AcceptedRequestsViewModelFactory
import com.android.sample.ui.profile.accepted_requests.KudosStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import java.util.Date
import kotlin.collections.get
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
class AcceptedRequestsViewModelTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"
    const val CREATOR_ID = "creator456"
    const val OTHER_HELPER_ID = "otherHelper789"

    const val REQUEST_ID_1 = "request1"
    const val REQUEST_ID_2 = "request2"
    const val REQUEST_ID_3 = "request3"
    const val REQUEST_ID_4 = "request4"

    const val TITLE_COMPLETED_WITH_KUDOS = "Completed with Kudos"
    const val TITLE_COMPLETED_NO_KUDOS = "Completed without Kudos"
    const val TITLE_IN_PROGRESS = "In Progress Request"
    const val TITLE_OPEN_REQUEST = "Open Request"

    const val DESC_COMPLETED_KUDOS = "User got kudos"
    const val DESC_COMPLETED_NO_KUDOS = "User didn't get kudos"
    const val DESC_IN_PROGRESS = "Not yet completed"
    const val DESC_OPEN = "Still open"

    const val LOCATION_NAME = "Test Location"
    const val LATITUDE = 46.5191
    const val LONGITUDE = 6.5668

    const val ERROR_FAILED_TO_LOAD = "Failed to load accepted requests"
    const val ERROR_NO_AUTH = "No authenticated user"

    const val TIME_OFFSET_1_HOUR = 3_600_000L
    const val TIME_OFFSET_2_HOURS = 7_200_000L
    const val TIME_OFFSET_3_HOURS = 10_800_000L

    const val EXPECTED_ZERO_REQUESTS = 0
    const val EXPECTED_ONE_REQUEST = 1
    const val EXPECTED_TWO_REQUESTS = 2
    const val EXPECTED_FOUR_REQUESTS = 4
  }

  // ============ Test Fixtures ============
  private lateinit var requestRepository: RequestRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: AcceptedRequestsViewModel

  private val testDispatcher = StandardTestDispatcher()

  // ============ Test Lifecycle ============

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    requestRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    // Mock Firebase Auth
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns firebaseAuth
    every { firebaseAuth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns CURRENT_USER_ID
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
    unmockkAll()
  }

  // ============ Helper Methods ============

  /** Creates a test request with default values. */
  private fun createTestRequest(
      requestId: String,
      title: String,
      description: String,
      status: RequestStatus,
      selectedHelpers: List<String> = emptyList(),
      timeOffsetMs: Long = TIME_OFFSET_1_HOUR
  ): Request {
    val now = System.currentTimeMillis()
    return Request(
        requestId = requestId,
        title = title,
        description = description,
        requestType = listOf(RequestType.STUDYING),
        location = Location(LATITUDE, LONGITUDE, LOCATION_NAME),
        locationName = LOCATION_NAME,
        status = status,
        startTimeStamp = Date(now - timeOffsetMs),
        expirationTime = Date(now + TIME_OFFSET_2_HOURS),
        people = listOf(CURRENT_USER_ID, OTHER_HELPER_ID),
        tags = listOf(Tags.URGENT),
        creatorId = CREATOR_ID,
        selectedHelpers = selectedHelpers)
  }

  /** Creates a completed request where current user received kudos. */
  private fun createCompletedRequestWithKudos(
      requestId: String = REQUEST_ID_1,
      title: String = TITLE_COMPLETED_WITH_KUDOS,
      timeOffsetMs: Long = TIME_OFFSET_1_HOUR
  ): Request {
    return createTestRequest(
        requestId = requestId,
        title = title,
        description = DESC_COMPLETED_KUDOS,
        status = RequestStatus.COMPLETED,
        selectedHelpers = listOf(CURRENT_USER_ID),
        timeOffsetMs = timeOffsetMs)
  }

  /** Creates a completed request where current user did NOT receive kudos. */
  private fun createCompletedRequestWithoutKudos(
      requestId: String = REQUEST_ID_2,
      title: String = TITLE_COMPLETED_NO_KUDOS,
      timeOffsetMs: Long = TIME_OFFSET_2_HOURS
  ): Request {
    return createTestRequest(
        requestId = requestId,
        title = title,
        description = DESC_COMPLETED_NO_KUDOS,
        status = RequestStatus.COMPLETED,
        selectedHelpers = listOf(OTHER_HELPER_ID), // Other helper got kudos, not current user
        timeOffsetMs = timeOffsetMs)
  }

  /** Creates a request that is still in progress. */
  private fun createInProgressRequest(
      requestId: String = REQUEST_ID_3,
      title: String = TITLE_IN_PROGRESS,
      timeOffsetMs: Long = TIME_OFFSET_3_HOURS
  ): Request {
    return createTestRequest(
        requestId = requestId,
        title = title,
        description = DESC_IN_PROGRESS,
        status = RequestStatus.IN_PROGRESS,
        timeOffsetMs = timeOffsetMs)
  }

  /** Creates a request that is still open. */
  private fun createOpenRequest(
      requestId: String = REQUEST_ID_4,
      title: String = TITLE_OPEN_REQUEST,
      timeOffsetMs: Long = TIME_OFFSET_1_HOUR
  ): Request {
    return createTestRequest(
        requestId = requestId,
        title = title,
        description = DESC_OPEN,
        status = RequestStatus.OPEN,
        timeOffsetMs = timeOffsetMs)
  }

  /** Initializes ViewModel and waits for initial load to complete. */
  private suspend fun initializeViewModel() {
    viewModel = AcceptedRequestsViewModel(requestRepository)
      viewModel.loadAcceptedRequests()
    testDispatcher.scheduler.advanceUntilIdle()
  }

  /** Asserts that the UI state matches expected values. */
  private fun assertUiState(
      expectedRequestCount: Int,
      expectedIsLoading: Boolean,
      expectedErrorMessage: String? = null
  ) {
    val state = viewModel.uiState.value
    assertEquals("Unexpected request count", expectedRequestCount, state.requests.size)
    assertEquals("Unexpected loading state", expectedIsLoading, state.isLoading)
    assertEquals("Unexpected error message", expectedErrorMessage, state.errorMessage)
  }

  /** Asserts kudos status for a request at given index. */
  private fun assertKudosStatus(index: Int, expectedStatus: KudosStatus) {
    val actualStatus = viewModel.uiState.value.requests[index].kudosStatus
    assertEquals("Unexpected kudos status at index $index", expectedStatus, actualStatus)
  }

  /** Asserts requests are sorted by most recent first. */
  private fun assertSortedByMostRecent() {
    val requests = viewModel.uiState.value.requests.map { it.request }
    for (i in 0 until requests.size - 1) {
      val current = requests[i].startTimeStamp.time
      val next = requests[i + 1].startTimeStamp.time
      assertTrue(
          "Requests should be sorted by most recent first (index $i and ${i + 1})", current >= next)
    }
  }

  // ============ Tests for Initialization ============

  @Test
  fun init_loadsAcceptedRequests_successfully() = runTest {
    // Given
    val requests = listOf(createCompletedRequestWithKudos())
    coEvery { requestRepository.getAcceptedRequests() } returns requests

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedRequestCount = EXPECTED_ONE_REQUEST, expectedIsLoading = false)
    coVerify(exactly = 1) { requestRepository.getAcceptedRequests() }
  }

  @Test
  fun init_setsLoadingState_thenCompletesSuccessfully() = runTest {
    // Given
    val requests = listOf(createCompletedRequestWithKudos())
    coEvery { requestRepository.getAcceptedRequests() } returns requests

    // When
    viewModel = AcceptedRequestsViewModel(requestRepository)

    // Then - Initially loading
    assertTrue("Should be loading initially", viewModel.uiState.value.isLoading)

      viewModel.loadAcceptedRequests()

    // Complete loading
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Loading complete
    assertUiState(expectedRequestCount = EXPECTED_ONE_REQUEST, expectedIsLoading = false)
  }

  @Test
  fun init_handlesEmptyList_successfully() = runTest {
    // Given
    coEvery { requestRepository.getAcceptedRequests() } returns emptyList()

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedRequestCount = EXPECTED_ZERO_REQUESTS, expectedIsLoading = false)
  }

  @Test
  fun init_handlesRepositoryError_setsErrorMessage() = runTest {
    // Given
    val errorMessage = "Network error"
    coEvery { requestRepository.getAcceptedRequests() } throws Exception(errorMessage)

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedRequestCount = EXPECTED_ZERO_REQUESTS,
        expectedIsLoading = false,
        expectedErrorMessage = "$ERROR_FAILED_TO_LOAD: $errorMessage")
  }

  @Test
  fun init_handlesNoAuthenticatedUser_setsErrorMessage() = runTest {
    // Given
    every { firebaseAuth.currentUser } returns null
    coEvery { requestRepository.getAcceptedRequests() } returns emptyList()

    // When
    initializeViewModel()

    // Then
    val state = viewModel.uiState.value
    assertFalse("Should not be loading", state.isLoading)
    assertNotNull("Error message should be set", state.errorMessage)
    assertTrue(
        "Error should mention authentication", state.errorMessage?.contains(ERROR_NO_AUTH) == true)
  }

  // ============ Tests for Kudos Status Determination ============

  @Test
  fun determineKudosStatus_returnsReceived_whenUserInSelectedHelpers() = runTest {
    // Given
    val request = createCompletedRequestWithKudos()
    coEvery { requestRepository.getAcceptedRequests() } returns listOf(request)

    // When
    initializeViewModel()

    // Then
    assertKudosStatus(0, KudosStatus.RECEIVED)
  }

  @Test
  fun determineKudosStatus_returnsNotReceived_whenUserNotInSelectedHelpers() = runTest {
    // Given
    val request = createCompletedRequestWithoutKudos()
    coEvery { requestRepository.getAcceptedRequests() } returns listOf(request)

    // When
    initializeViewModel()

    // Then
    assertKudosStatus(0, KudosStatus.NOT_RECEIVED)
  }

  @Test
  fun determineKudosStatus_returnsPending_whenRequestInProgress() = runTest {
    // Given
    val request = createInProgressRequest()
    coEvery { requestRepository.getAcceptedRequests() } returns listOf(request)

    // When
    initializeViewModel()

    // Then
    assertKudosStatus(0, KudosStatus.PENDING)
  }

  @Test
  fun determineKudosStatus_returnsPending_whenRequestOpen() = runTest {
    // Given
    val request = createOpenRequest()
    coEvery { requestRepository.getAcceptedRequests() } returns listOf(request)

    // When
    initializeViewModel()

    // Then
    assertKudosStatus(0, KudosStatus.PENDING)
  }

  @Test
  fun determineKudosStatus_returnsNotReceived_whenCompletedWithEmptySelectedHelpers() = runTest {
    // Given
    val request =
        createTestRequest(
            requestId = REQUEST_ID_1,
            title = "Completed No Selection",
            description = "No helpers selected",
            status = RequestStatus.COMPLETED,
            selectedHelpers = emptyList())
    coEvery { requestRepository.getAcceptedRequests() } returns listOf(request)

    // When
    initializeViewModel()

    // Then
    assertKudosStatus(0, KudosStatus.NOT_RECEIVED)
  }

  @Test
  fun determineKudosStatus_handlesMultipleRequestsWithMixedStatuses() = runTest {
    // Given
    val requestWithKudos =
        createCompletedRequestWithKudos(
            requestId = REQUEST_ID_1, timeOffsetMs = TIME_OFFSET_3_HOURS)
    val requestWithoutKudos =
        createCompletedRequestWithoutKudos(
            requestId = REQUEST_ID_2, timeOffsetMs = TIME_OFFSET_2_HOURS)
    val pendingRequest =
        createInProgressRequest(requestId = REQUEST_ID_3, timeOffsetMs = TIME_OFFSET_1_HOUR)

    coEvery { requestRepository.getAcceptedRequests() } returns
        listOf(requestWithKudos, requestWithoutKudos, pendingRequest)

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedRequestCount = 3, expectedIsLoading = false)
    // Note: After sorting, order will be: pendingRequest (most recent), requestWithoutKudos,
    // requestWithKudos
    assertKudosStatus(
        0, KudosStatus.PENDING) // pendingRequest (TIME_OFFSET_1_HOUR ago - most recent)
    assertKudosStatus(1, KudosStatus.NOT_RECEIVED) // requestWithoutKudos (TIME_OFFSET_2_HOURS ago)
    assertKudosStatus(
        2, KudosStatus.RECEIVED) // requestWithKudos (TIME_OFFSET_3_HOURS ago - oldest)
  }

  // ============ Tests for Sorting ============

  @Test
  fun loadAcceptedRequests_sortsByMostRecentFirst() = runTest {
    // Given - Create requests with different timestamps (oldest to newest)
    val oldestRequest =
        createCompletedRequestWithKudos(
            requestId = REQUEST_ID_1,
            title = "Oldest",
            timeOffsetMs = TIME_OFFSET_3_HOURS // 3 hours ago
            )
    val middleRequest =
        createCompletedRequestWithoutKudos(
            requestId = REQUEST_ID_2,
            title = "Middle",
            timeOffsetMs = TIME_OFFSET_2_HOURS // 2 hours ago
            )
    val newestRequest =
        createInProgressRequest(
            requestId = REQUEST_ID_3,
            title = "Newest",
            timeOffsetMs = TIME_OFFSET_1_HOUR // 1 hour ago
            )

    coEvery { requestRepository.getAcceptedRequests() } returns
        listOf(oldestRequest, middleRequest, newestRequest)

    // When
    initializeViewModel()

    // Then
    assertSortedByMostRecent()

    // Verify order explicitly
    val requests = viewModel.uiState.value.requests
    assertEquals("Newest should be first", "Newest", requests[0].request.title)
    assertEquals("Middle should be second", "Middle", requests[1].request.title)
    assertEquals("Oldest should be last", "Oldest", requests[2].request.title)
  }

  @Test
  fun loadAcceptedRequests_maintainsSortOrder_withIdenticalTimestamps() = runTest {
    // Given - Multiple requests with same timestamp
    val timestamp = System.currentTimeMillis() - TIME_OFFSET_1_HOUR
    val request1 =
        createTestRequest(
            requestId = REQUEST_ID_1,
            title = "Request A",
            description = "First",
            status = RequestStatus.COMPLETED,
            selectedHelpers = listOf(CURRENT_USER_ID),
            timeOffsetMs = TIME_OFFSET_1_HOUR)
    val request2 =
        createTestRequest(
            requestId = REQUEST_ID_2,
            title = "Request B",
            description = "Second",
            status = RequestStatus.COMPLETED,
            selectedHelpers = emptyList(),
            timeOffsetMs = TIME_OFFSET_1_HOUR)

    coEvery { requestRepository.getAcceptedRequests() } returns listOf(request1, request2)

    // When
    initializeViewModel()

    // Then - Should maintain stable sort
    val requests = viewModel.uiState.value.requests
    assertEquals(EXPECTED_TWO_REQUESTS, requests.size)
    assertSortedByMostRecent()
  }

  // ============ Tests for Refresh ============

  @Test
  fun refresh_reloadsAcceptedRequests() = runTest {
    // Given
    val initialRequests = listOf(createCompletedRequestWithKudos())
    coEvery { requestRepository.getAcceptedRequests() } returns initialRequests

    initializeViewModel()

    // Change repository response
    val updatedRequests =
        listOf(createCompletedRequestWithKudos(), createCompletedRequestWithoutKudos())
    coEvery { requestRepository.getAcceptedRequests() } returns updatedRequests

    // When
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedRequestCount = EXPECTED_TWO_REQUESTS, expectedIsLoading = false)
    coVerify(exactly = 2) { requestRepository.getAcceptedRequests() }
  }

  @Test
  fun refresh_setsLoadingState_duringRefresh() = runTest {
    // Given
    val requests = listOf(createCompletedRequestWithKudos())
    var loadingStateObserved = false

    // Suspend the repository call so we can observe loading state
    coEvery { requestRepository.getAcceptedRequests() } coAnswers
        {
          loadingStateObserved = viewModel.uiState.value.isLoading
          kotlinx.coroutines.delay(50) // Small delay to simulate network
          requests
        }

    initializeViewModel()

    // Reset for refresh test
    loadingStateObserved = false

    // When
    viewModel.refresh()
    testDispatcher.scheduler.runCurrent() // Execute until first suspension point

    // Then - Should be loading during the suspended repository call
    assertTrue("Should be loading during refresh", loadingStateObserved)

    // Complete refresh
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Should finish loading
    assertFalse("Should finish loading", viewModel.uiState.value.isLoading)
  }


  // ============ Tests for Error Handling ============

  @Test
  fun clearError_removesErrorMessage() = runTest {
    // Given
    coEvery { requestRepository.getAcceptedRequests() } throws Exception("Test error")
    initializeViewModel()

    // Verify error is set
    assertNotNull("Error should be set", viewModel.uiState.value.errorMessage)

    // When
    viewModel.clearError()

    // Then
    assertNull("Error should be cleared", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun clearError_doesNotAffectOtherState() = runTest {
    // Given
    coEvery { requestRepository.getAcceptedRequests() } throws Exception("Test error")
    initializeViewModel()

    // When
    viewModel.clearError()

    // Then
    val state = viewModel.uiState.value
    assertNull("Error should be cleared", state.errorMessage)
    assertFalse("Loading should remain false", state.isLoading)
    assertTrue("Requests should remain empty", state.requests.isEmpty())
  }

  // ============ Tests for ViewModelFactory ============

  @Test
  fun factory_createsViewModel_successfully() {
    // Given
    val factory = AcceptedRequestsViewModelFactory(requestRepository)

    // When
    val createdViewModel = factory.create(AcceptedRequestsViewModel::class.java)

    // Then
    assertNotNull("ViewModel should be created", createdViewModel)
    assertTrue(
        "Should be instance of AcceptedRequestsViewModel",
        createdViewModel is AcceptedRequestsViewModel)
  }

  @Test
  fun factory_throwsException_forInvalidViewModelClass() {
    // Given
    val factory = AcceptedRequestsViewModelFactory(requestRepository)

    // When/Then
    assertThrows(IllegalArgumentException::class.java) {
      factory.create(AndroidViewModel::class.java)
    }
  }
}
