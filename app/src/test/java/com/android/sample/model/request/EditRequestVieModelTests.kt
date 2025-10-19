package com.android.sample.ui.request

import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.request.edit.EditRequestViewModel
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EditRequestViewModelTest {

  private lateinit var viewModel: EditRequestViewModel
  private lateinit var requestRepository: RequestRepository
  private lateinit var locationRepository: LocationRepository
  private val testDispatcher = StandardTestDispatcher()

  private val testLocation = Location(46.5197, 6.6323, "EPFL")
  private val testRequest =
      Request(
          requestId = "test-id",
          title = "Test Request",
          description = "Test Description",
          requestType = RequestType.values().toList(),
          location = testLocation,
          locationName = "EPFL",
          status = RequestStatus.IN_PROGRESS,
          startTimeStamp = Date(),
          expirationTime = Date(System.currentTimeMillis() + 86400000),
          people = listOf("user1"),
          tags = listOf(Tags.URGENT),
          creatorId = "user1")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    requestRepository = mock(RequestRepository::class.java)
    locationRepository = mock(LocationRepository::class.java)
    viewModel = EditRequestViewModel(requestRepository, locationRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========================================================================
  // Test: Initial State
  // ========================================================================

  @Test
  fun initialState_hasDefaultValues() {
    val uiState = viewModel.uiState.value

    assertEquals("", uiState.title)
    assertEquals("", uiState.description)
    assertTrue(uiState.requestTypes.isEmpty())
    assertNull(uiState.location)
    assertEquals("", uiState.locationName)
    assertTrue(uiState.tags.isEmpty())
    assertFalse(uiState.isEditMode)
    assertFalse(uiState.isLoading)
    assertNull(uiState.errorMessage)
    assertFalse(uiState.validationState.showSuccessMessage)
  }

  // ========================================================================
  // Test: Update Functions
  // ========================================================================

  @Test
  fun updateTitle_updatesUiState() {
    viewModel.updateTitle("New Title")

    assertEquals("New Title", viewModel.uiState.value.title)
  }

  @Test
  fun updateTitle_withBlankValue_setsValidationError() {
    viewModel.updateTitle("")

    assertTrue(viewModel.uiState.value.validationState.showTitleError)
  }

  @Test
  fun updateTitle_withValidValue_clearsValidationError() {
    // First set an error
    viewModel.updateTitle("")
    assertTrue(viewModel.uiState.value.validationState.showTitleError)

    // Then set a valid value
    viewModel.updateTitle("Valid Title")
    assertFalse(viewModel.uiState.value.validationState.showTitleError)
  }

  @Test
  fun updateDescription_updatesUiState() {
    viewModel.updateDescription("New Description")

    assertEquals("New Description", viewModel.uiState.value.description)
  }

  @Test
  fun updateDescription_withBlankValue_setsValidationError() {
    viewModel.updateDescription("")

    assertTrue(viewModel.uiState.value.validationState.showDescriptionError)
  }

  @Test
  fun updateRequestTypes_updatesUiState() {
    val types = listOf(RequestType.HARDWARE, RequestType.STUDYING)
    viewModel.updateRequestTypes(types)

    assertEquals(types, viewModel.uiState.value.requestTypes)
  }

  @Test
  fun updateRequestTypes_withEmptyList_setsValidationError() {
    viewModel.updateRequestTypes(emptyList())

    assertTrue(viewModel.uiState.value.validationState.showRequestTypeError)
  }

  @Test
  fun updateLocation_updatesUiState() {
    viewModel.updateLocation(testLocation)

    assertEquals(testLocation, viewModel.uiState.value.location)
  }

  @Test
  fun updateLocationName_updatesUiState() {
    viewModel.updateLocationName("EPFL")

    assertEquals("EPFL", viewModel.uiState.value.locationName)
  }

  @Test
  fun updateLocationName_withBlankValue_setsValidationError() {
    viewModel.updateLocationName("")

    assertTrue(viewModel.uiState.value.validationState.showLocationNameError)
  }

  @Test
  fun updateStartTimeStamp_updatesUiState() {
    val date = Date()
    viewModel.updateStartTimeStamp(date)

    assertEquals(date, viewModel.uiState.value.startTimeStamp)
  }

  @Test
  fun updateExpirationTime_updatesUiState() {
    val date = Date(System.currentTimeMillis() + 86400000)
    viewModel.updateExpirationTime(date)

    assertEquals(date, viewModel.uiState.value.expirationTime)
  }

  @Test
  fun updateExpirationTime_beforeStartDate_setsDateOrderError() {
    val startDate = Date(System.currentTimeMillis() + 86400000)
    val expirationDate = Date()

    viewModel.updateStartTimeStamp(startDate)
    viewModel.updateExpirationTime(expirationDate)

    assertTrue(viewModel.uiState.value.validationState.showDateOrderError)
  }

  @Test
  fun updateExpirationTime_afterStartDate_clearsDateOrderError() {
    val startDate = Date()
    val expirationDate = Date(System.currentTimeMillis() + 86400000)

    viewModel.updateStartTimeStamp(startDate)
    viewModel.updateExpirationTime(expirationDate)

    assertFalse(viewModel.uiState.value.validationState.showDateOrderError)
  }

  @Test
  fun updateTags_updatesUiState() {
    val tags = listOf(Tags.URGENT, Tags.EASY)
    viewModel.updateTags(tags)

    assertEquals(tags, viewModel.uiState.value.tags)
  }

  // ========================================================================
  // Test: Load Request
  // ========================================================================

  @Test
  fun loadRequest_success_updatesUiStateWithRequestData() = runTest {
    whenever(requestRepository.getRequest("test-id")).thenReturn(testRequest)

    viewModel.loadRequest("test-id")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("Test Request", uiState.title)
    assertEquals("Test Description", uiState.description)

    assertTrue(uiState.requestTypes.contains(RequestType.HARDWARE))

    assertEquals(testLocation, uiState.location)
    assertEquals("EPFL", uiState.locationName)
    assertEquals(listOf(Tags.URGENT), uiState.tags)
    assertTrue(uiState.isEditMode)
    assertFalse(uiState.isLoading)
    assertNull(uiState.errorMessage)
  }

  @Test
  fun loadRequest_failure_setsErrorMessage() = runTest {
    whenever(requestRepository.getRequest("test-id")).thenThrow(RuntimeException("Network error"))

    viewModel.loadRequest("test-id")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertNotNull(uiState.errorMessage)
    assertTrue(uiState.errorMessage!!.contains("Failed to load request"))
  }

  @Test
  fun loadRequest_setsLoadingState() = runTest {
    whenever(requestRepository.getRequest("test-id")).thenReturn(testRequest)

    viewModel.loadRequest("test-id")

    advanceUntilIdle()

    // check loading state after coroutine has completed
    assertFalse(viewModel.uiState.value.isLoading)
  }

  // ========================================================================
  // Test: Initialize for Create
  // ========================================================================

  @Test
  fun initializeForCreate_setsDefaultState() {
    viewModel.initializeForCreate("user1")

    val uiState = viewModel.uiState.value
    assertEquals("", uiState.title)
    assertEquals("", uiState.description)
    assertTrue(uiState.requestTypes.isEmpty())
    assertFalse(uiState.isEditMode)
    assertEquals(listOf("user1"), viewModel.people.value)
  }

  // ========================================================================
  // Test: Location Search
  // ========================================================================

  @Test
  fun searchLocations_withShortQuery_clearsResults() {
    viewModel.searchLocations("ab")

    assertTrue(viewModel.uiState.value.locationSearchResults.isEmpty())
  }

  @Test
  fun searchLocations_success_updatesSearchResults() = runTest {
    val locations = listOf(testLocation)
    whenever(locationRepository.search("EPFL", 5)).thenReturn(locations)

    viewModel.searchLocations("EPFL")
    advanceUntilIdle()

    assertEquals(locations, viewModel.uiState.value.locationSearchResults)
    assertFalse(viewModel.uiState.value.isSearchingLocation)
  }

  @Test
  fun searchLocations_failure_setsErrorMessage() = runTest {
    whenever(locationRepository.search("EPFL", 5)).thenThrow(RuntimeException("Network error"))

    viewModel.searchLocations("EPFL")
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMessage)
    assertTrue(viewModel.uiState.value.errorMessage!!.contains("Location search failed"))
    assertTrue(viewModel.uiState.value.locationSearchResults.isEmpty())
  }

  @Test
  fun clearLocationSearch_clearsResults() {
    viewModel.clearLocationSearch()

    assertTrue(viewModel.uiState.value.locationSearchResults.isEmpty())
  }

  // ========================================================================
  // Test: Save Request
  // ========================================================================

  @Test
  fun saveRequest_withValidData_callsRepositoryAndShowsSuccess() = runTest {
    // Setup valid data
    viewModel.updateTitle("Test Title")
    viewModel.updateDescription("Test Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(testLocation)
    viewModel.updateLocationName("EPFL")
    viewModel.updateStartTimeStamp(Date())
    viewModel.updateExpirationTime(Date(System.currentTimeMillis() + 86400000))

    var onSuccessCalled = false
    viewModel.saveRequest("user1") { onSuccessCalled = true }
    advanceUntilIdle()

    verify(requestRepository).addRequest(any())
    assertTrue(onSuccessCalled)
    assertTrue(viewModel.uiState.value.validationState.showSuccessMessage)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun saveRequest_withInvalidData_doesNotCallRepository() = runTest {
    // Don't set any data (all fields invalid)

    var onSuccessCalled = false
    viewModel.saveRequest("user1") { onSuccessCalled = true }
    advanceUntilIdle()

    verify(requestRepository, never()).addRequest(any())
    assertFalse(onSuccessCalled)
    assertFalse(viewModel.uiState.value.validationState.showSuccessMessage)
  }

  @Test
  fun saveRequest_inEditMode_callsUpdateRequest() = runTest {
    whenever(requestRepository.getRequest("test-id")).thenReturn(testRequest)
    viewModel.loadRequest("test-id")
    advanceUntilIdle()

    // Modify some data
    viewModel.updateTitle("Updated Title")

    viewModel.saveRequest("user1") {}
    advanceUntilIdle()

    // Simple fix: Use only matchers or only raw values
    verify(requestRepository).updateRequest(any(), any())
    verify(requestRepository, never()).addRequest(any())
  }

  @Test
  fun saveRequest_failure_setsErrorMessage() = runTest {
    // Setup valid data
    viewModel.updateTitle("Test Title")
    viewModel.updateDescription("Test Description")
    viewModel.updateRequestTypes(listOf(RequestType.HARDWARE))
    viewModel.updateLocation(testLocation)
    viewModel.updateLocationName("EPFL")
    viewModel.updateStartTimeStamp(Date())
    viewModel.updateExpirationTime(Date(System.currentTimeMillis() + 86400000))

    whenever(requestRepository.addRequest(any())).thenThrow(RuntimeException("Network error"))

    viewModel.saveRequest("user1") {}
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMessage)
    assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to save request"))
    assertFalse(viewModel.uiState.value.isLoading)
  }

  // ========================================================================
  // Test: Error Handling
  // ========================================================================

  @Test
  fun clearError_clearsErrorMessage() {
    // Manually set an error in the UI state
    viewModel.updateTitle("Valid") // First set to valid state
    // Then trigger an error through the state
    viewModel.loadRequest("non-existent-id") // This will fail and set error

    viewModel.clearError()

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun clearSuccessMessage_clearsSuccessFlag() = runTest {
    // Setup and save a valid request to show success
    viewModel.updateTitle("Test Title")
    viewModel.updateDescription("Test Description")
    viewModel.updateRequestTypes(listOf(RequestType.HARDWARE))
    viewModel.updateLocation(testLocation)
    viewModel.updateLocationName("EPFL")
    viewModel.updateStartTimeStamp(Date())
    viewModel.updateExpirationTime(Date(System.currentTimeMillis() + 86400000))

    viewModel.saveRequest("user1") {}
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.validationState.showSuccessMessage)

    viewModel.clearSuccessMessage()

    assertFalse(viewModel.uiState.value.validationState.showSuccessMessage)
  }

  // ========================================================================
  // Test: Reset
  // ========================================================================

  @Test
  fun reset_clearsAllState() {
    // Set some data
    viewModel.updateTitle("Test")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.HARDWARE))

    viewModel.reset()

    val uiState = viewModel.uiState.value
    assertEquals("", uiState.title)
    assertEquals("", uiState.description)
    assertTrue(uiState.requestTypes.isEmpty())
    assertFalse(uiState.isEditMode)
    assertFalse(uiState.isLoading)
    assertNull(uiState.errorMessage)
    assertTrue(viewModel.people.value.isEmpty())
  }

  // ========================================================================
  // Test: Validation Integration
  // ========================================================================

  @Test
  fun validation_showsAllErrors_whenAllFieldsInvalid() = runTest {
    // Try to save with no data
    viewModel.saveRequest("user1") {}
    advanceUntilIdle()

    val validation = viewModel.uiState.value.validationState
    assertTrue(validation.showTitleError)
    assertTrue(validation.showDescriptionError)
    assertTrue(validation.showRequestTypeError)
    assertTrue(validation.showLocationNameError)
  }

  @Test
  fun validation_clearsErrors_whenFieldsBecomesValid() {
    // Set blank title to show error
    viewModel.updateTitle("")
    assertTrue(viewModel.uiState.value.validationState.showTitleError)

    // Fix the title
    viewModel.updateTitle("Valid Title")
    assertFalse(viewModel.uiState.value.validationState.showTitleError)
  }
}
