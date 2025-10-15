package com.android.sample.request

import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.request.EditRequestViewModel
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import org.mockito.kotlin.whenever

@Mock private lateinit var mockLocationRepo: LocationRepository

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class EditRequestViewModelTest {

  @Mock private lateinit var repository: RequestRepository
  @Mock private lateinit var locationRepository: LocationRepository

  private lateinit var viewModel: EditRequestViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val testRequest =
      Request(
          requestId = "test-id",
          title = "Test Request",
          description = "Test Description",
          requestType = listOf(RequestType.STUDYING),
          location = Location(0.0, 0.0, "Test Location"),
          locationName = "Test Building",
          status = RequestStatus.IN_PROGRESS,
          startTimeStamp = Date(),
          expirationTime = Date(System.currentTimeMillis() + 86400000),
          people = listOf("creator123"),
          tags = listOf(Tags.URGENT),
          creatorId = "creator123")

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    Dispatchers.setMain(testDispatcher)
    viewModel = EditRequestViewModel(repository, locationRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // Test 1: Initial state
  @Test
  fun viewModel_initialState_allFieldsEmpty() = runTest {
    assert(viewModel.title.first() == "")
    assert(viewModel.description.first() == "")
    assert(viewModel.requestTypes.first().isEmpty())
    assert(viewModel.location.first() == null)
    assert(viewModel.locationName.first() == "")
    assert(viewModel.tags.first().isEmpty())
    assert(!viewModel.isLoading.first())
    assert(viewModel.errorMessage.first() == null)
    assert(!viewModel.isEditMode.first())
  }

  // Test 2: Update title
  @Test
  fun updateTitle_updatesStateFlow() = runTest {
    viewModel.updateTitle("New Title")
    assert(viewModel.title.first() == "New Title")
  }

  // Test 3: Update description
  @Test
  fun updateDescription_updatesStateFlow() = runTest {
    viewModel.updateDescription("New Description")
    assert(viewModel.description.first() == "New Description")
  }

  // Test 4: Update request types
  @Test
  fun updateRequestTypes_updatesStateFlow() = runTest {
    val types = listOf(RequestType.STUDYING, RequestType.SPORT)
    viewModel.updateRequestTypes(types)
    assert(viewModel.requestTypes.first() == types)
  }

  // Test 5: Update location
  @Test
  fun updateLocation_updatesStateFlow() = runTest {
    val location = Location(46.5197, 6.5668, "EPFL")
    viewModel.updateLocation(location)
    assert(viewModel.location.first() == location)
  }

  // Test 6: Update location name
  @Test
  fun updateLocationName_updatesStateFlow() = runTest {
    viewModel.updateLocationName("BC Building")
    assert(viewModel.locationName.first() == "BC Building")
  }

  // Test 7: Update start timestamp
  @Test
  fun updateStartTimeStamp_updatesStateFlow() = runTest {
    val date = Date()
    viewModel.updateStartTimeStamp(date)
    assert(viewModel.startTimeStamp.first() == date)
  }

  // Test 8: Update expiration time
  @Test
  fun updateExpirationTime_updatesStateFlow() = runTest {
    val date = Date()
    viewModel.updateExpirationTime(date)
    assert(viewModel.expirationTime.first() == date)
  }

  // Test 9: Update tags
  @Test
  fun updateTags_updatesStateFlow() = runTest {
    val tags = listOf(Tags.URGENT, Tags.EASY)
    viewModel.updateTags(tags)
    assert(viewModel.tags.first() == tags)
  }

  // Test 10: Update people
  @Test
  fun updatePeople_updatesStateFlow() = runTest {
    val people = listOf("user1", "user2")
    viewModel.updatePeople(people)
    assert(viewModel.people.first() == people)
  }

  // Test 11: Initialize for create - sets edit mode to false
  @Test
  fun initializeForCreate_setsEditModeFalse() = runTest {
    viewModel.initializeForCreate("creator123")
    advanceUntilIdle()
    assert(!viewModel.isEditMode.first())
  }

  // Test 12: Initialize for create - adds creator to people
  @Test
  fun initializeForCreate_addsCreatorToPeople() = runTest {
    viewModel.initializeForCreate("creator123")
    advanceUntilIdle()
    assert(viewModel.people.first() == listOf("creator123"))
  }

  // Test 13: Initialize for create - clears all fields
  @Test
  fun initializeForCreate_clearsAllFields() = runTest {
    // Set some data first
    viewModel.updateTitle("Some title")
    viewModel.updateDescription("Some description")

    // Initialize for create
    viewModel.initializeForCreate("creator123")
    advanceUntilIdle()

    assert(viewModel.title.first() == "")
    assert(viewModel.description.first() == "")
    assert(viewModel.requestTypes.first().isEmpty())
  }

  // Test 14: Load request - success
  @Test
  fun loadRequest_success_populatesFields() = runTest {
    `when`(repository.getRequest("test-id")).thenReturn(testRequest)

    viewModel.loadRequest("test-id")
    advanceUntilIdle()

    assert(viewModel.title.first() == "Test Request")
    assert(viewModel.description.first() == "Test Description")
    assert(viewModel.requestTypes.first() == listOf(RequestType.STUDYING))
    assert(viewModel.locationName.first() == "Test Building")
    assert(viewModel.isEditMode.first())
    assert(!viewModel.isLoading.first())
  }

  // Test 15: Load request - sets loading state
  @Test
  fun loadRequest_setsLoadingState() = runTest {
    `when`(repository.getRequest("test-id")).thenReturn(testRequest)

    viewModel.loadRequest("test-id")
    // Check loading is true during execution
    // After advanceUntilIdle, loading should be false
    advanceUntilIdle()

    assert(!viewModel.isLoading.first())
  }

  // Test 16: Load request - error handling
  @Test
  fun loadRequest_error_setsErrorMessage() = runTest {
    `when`(repository.getRequest("test-id")).thenThrow(RuntimeException("Network error"))

    viewModel.loadRequest("test-id")
    advanceUntilIdle()

    assert(viewModel.errorMessage.first()?.contains("Failed to load request") == true)
    assert(!viewModel.isLoading.first())
  }

  // Test 17: Save request - validation error empty title
  @Test
  fun saveRequest_emptyTitle_setsValidationError() = runTest {
    viewModel.updateTitle("")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == "Title cannot be empty")
    assert(!successCalled)
  }

  // Test 18: Save request - validation error empty description
  @Test
  fun saveRequest_emptyDescription_setsValidationError() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == "Description cannot be empty")
    assert(!successCalled)
  }

  // Test 19: Save request - validation error no request types
  @Test
  fun saveRequest_noRequestTypes_setsValidationError() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(emptyList())
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == "Please select at least one request type")
    assert(!successCalled)
  }

  // Test 20: Save request - validation error no location
  @Test
  fun saveRequest_noLocation_setsValidationError() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(null)
    viewModel.updateLocationName("Building")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == "Please select a location")
    assert(!successCalled)
  }

  // Test 21: Save request - validation error empty location name
  @Test
  fun saveRequest_emptyLocationName_setsValidationError() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == "Location name cannot be empty")
    assert(!successCalled)
  }

  // Test 22: Save request - validation error expiration before start
  @Test
  fun saveRequest_expirationBeforeStart_setsValidationError() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")

    val now = Date()
    val yesterday = Date(now.time - 86400000)
    viewModel.updateStartTimeStamp(now)
    viewModel.updateExpirationTime(yesterday)

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == "Expiration time must be after start time")
    assert(!successCalled)
  }

  // Test 23: Save request - create mode success
  @Test
  fun saveRequest_createMode_success() = runTest {
    viewModel.initializeForCreate("creator123")
    viewModel.updateTitle("New Request")
    viewModel.updateDescription("New Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    verify(repository).addRequest(any()) // ← NOW WORKS!
    assert(successCalled)
  }

  // Test 24: Save request - edit mode success
  @Test
  fun saveRequest_editMode_success() = runTest {
    whenever(repository.getRequest("test-id")).thenReturn(testRequest) // ← USE whenever

    viewModel.loadRequest("test-id")
    advanceUntilIdle()

    viewModel.updateTitle("Updated Title")

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    verify(repository).updateRequest(eq("test-id"), any()) // ← NOW WORKS!
    assert(successCalled)
  }

  // Test 25: Save request - repository error
  @Test
  fun saveRequest_repositoryError_setsErrorMessage() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")

    whenever(repository.addRequest(any()))
        .thenThrow(RuntimeException("Database error")) // ← USE whenever

    var successCalled = false
    viewModel.saveRequest("creator123") { successCalled = true }
    advanceUntilIdle()

    assert(viewModel.errorMessage.first()?.contains("Failed to save request") == true)
    assert(!successCalled)
  }

  // Test 26: Clear error
  @Test
  fun clearError_clearsErrorMessage() = runTest {
    `when`(repository.getRequest("invalid")).thenThrow(RuntimeException("Error"))

    viewModel.loadRequest("invalid")
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() != null)

    viewModel.clearError()
    advanceUntilIdle()

    assert(viewModel.errorMessage.first() == null)
  }

  // Test 27: Reset - clears all state
  @Test
  fun reset_clearsAllState() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))

    viewModel.reset()
    advanceUntilIdle()

    assert(viewModel.title.first() == "")
    assert(viewModel.description.first() == "")
    assert(viewModel.requestTypes.first().isEmpty())
    assert(!viewModel.isLoading.first())
    assert(viewModel.errorMessage.first() == null)
    assert(!viewModel.isEditMode.first())
  }

  // Test 28: Save request with default creator when people list is empty
  @Test
  fun saveRequest_emptyPeopleList_addsCreator() = runTest {
    viewModel.updateTitle("Title")
    viewModel.updateDescription("Description")
    viewModel.updateRequestTypes(listOf(RequestType.STUDYING))
    viewModel.updateLocation(Location(0.0, 0.0, "Test"))
    viewModel.updateLocationName("Building")
    viewModel.updatePeople(emptyList())

    viewModel.saveRequest("creator123") {}
    advanceUntilIdle()

    val captor = argumentCaptor<Request>() // ← USE argumentCaptor
    verify(repository).addRequest(captor.capture())
    assert(captor.firstValue.people.contains("creator123"))
  }

  @Test
  fun searchLocations_validQuery_updatesSearchResults() = runTest {
    val mockResults =
        listOf(
            Location(46.5197, 6.5668, "EPFL, Lausanne"), Location(46.5191, 6.5668, "BC Building"))

    `when`(locationRepository.search("EPFL", 5)).thenReturn(mockResults)

    viewModel.searchLocations("EPFL")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify loading state
    assert(!viewModel.isSearchingLocation.first())

    // Verify search results
    assert(viewModel.locationSearchResults.first() == mockResults)
    assert(viewModel.locationSearchResults.first().size == 2)
  }

  // Test 6: Search locations with short query clears results
  @Test
  fun searchLocations_shortQuery_clearsResults() = runTest {
    // First add some results
    val mockResults = listOf(Location(46.5197, 6.5668, "EPFL"))
    `when`(locationRepository.search("EPFL", 5)).thenReturn(mockResults)

    viewModel.searchLocations("EPFL")
    testDispatcher.scheduler.advanceUntilIdle()
    assert(viewModel.locationSearchResults.first().isNotEmpty())

    // Now search with short query (< 3 chars)
    viewModel.searchLocations("EP")
    testDispatcher.scheduler.advanceUntilIdle()

    // Should clear results
    assert(viewModel.locationSearchResults.first().isEmpty())
  }

  // Test 7: Clear location search resets state
  @Test
  fun clearLocationSearch_resetsSearchResults() = runTest {
    val mockResults = listOf(Location(46.5197, 6.5668, "EPFL"))
    `when`(locationRepository.search("EPFL", 5)).thenReturn(mockResults)

    viewModel.searchLocations("EPFL")
    testDispatcher.scheduler.advanceUntilIdle()
    assert(viewModel.locationSearchResults.first().isNotEmpty())

    // Clear search
    viewModel.clearLocationSearch()

    assert(viewModel.locationSearchResults.first().isEmpty())
  }
}
