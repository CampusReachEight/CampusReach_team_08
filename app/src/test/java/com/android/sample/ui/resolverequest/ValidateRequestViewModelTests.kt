package com.android.sample.ui.resolverequest

import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestClosureException
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.request_validation.KudosConstants
import com.android.sample.ui.request_validation.ValidateRequestViewModel
import com.android.sample.ui.request_validation.ValidateRequestViewModelFactory
import com.android.sample.ui.request_validation.ValidationState
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

private const val HELPER_1 = "helper1"
private const val NAME_HELPER = "Helper"
private const val ID_HELPER2 = "helper2"
private const val HELPER1_EMAIL = "helper1@test.com"
private const val LASTNAME_1 = "One"
private const val LASTNAME_2 = "Two"
private const val HELPER2_EMAIL = "helper2@test.com"
private const val TITLE_TEST_REQUEST = "Test Request"
private const val DESCRIPTION_TEST = "Test Description"
private const val TEST_LOCATION = "Test Location"
private const val NOT_OWNER = "not the owner"
private const val CANNOT_BE_CLOSED = "cannot be closed"
private const val NETWORK_ERROR = "Network error"
private const val COULD_NOT_LOAD_PROFILE = "Could not load helper profiles"
private const val FAILED_TO_LOAD_REQUEST = "Failed to load request"
private const val UNEXPECTED_ERROR_OCCURED = "unexpected error occurred"

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateRequestViewModelTest {

  private lateinit var requestRepository: RequestRepository
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var viewModel: ValidateRequestViewModel

  // Single test dispatcher shared across all tests
  private val testDispatcher = TestCoroutineDispatcher()

  private val testRequestId = "request123"
  private val testCreatorId = "creator123"
  private val testHelper1 =
      UserProfile(
          id = HELPER_1,
          name = NAME_HELPER,
          lastName = LASTNAME_1,
          email = HELPER1_EMAIL,
          photo = null,
          kudos = 100,
          helpReceived = 3,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())
  private val testHelper2 =
      UserProfile(
          id = ID_HELPER2,
          name = NAME_HELPER,
          lastName = LASTNAME_2,
          email = HELPER2_EMAIL,
          photo = null,
          kudos = 50,
          helpReceived = 1,
          section = UserSections.MATHEMATICS,
          arrivalDate = Date())
  private val testRequest =
      Request(
          requestId = testRequestId,
          creatorId = testCreatorId,
          title = TITLE_TEST_REQUEST,
          description = DESCRIPTION_TEST,
          requestType = listOf(RequestType.STUDYING),
          location = Location(0.0, 0.0, TEST_LOCATION),
          locationName = TEST_LOCATION,
          status = RequestStatus.OPEN,
          startTimeStamp = Date(),
          expirationTime = Date(System.currentTimeMillis() + 86400000),
          people = listOf(HELPER_1, ID_HELPER2),
          tags = listOf(Tags.URGENT))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    requestRepository = mockk(relaxed = true)
    userProfileRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    testDispatcher.cleanupTestCoroutines()
    clearAllMocks()
  }

  // ==================== Initialization Tests ====================

  @Test
  fun initLoadsRequestDataSuccessfully() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
        coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
        coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
        coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } returns testHelper2

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Ready)
        val readyState = state as ValidationState.Ready
        assertEquals(testRequest, readyState.request)
        assertEquals(2, readyState.helpers.size)
        assertTrue(readyState.selectedHelperIds.isEmpty())
      }

  @Test
  fun initShowsErrorWhenUserIsNotOwner() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
        coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns false

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(NOT_OWNER))
        assertFalse(errorState.canRetry)
      }

  @Test
  fun initShowsErrorWhenRequestStatusCompleted() =
      testDispatcher.runBlockingTest {
        // Given
        val completedRequest = testRequest.copy(status = RequestStatus.COMPLETED)
        coEvery { requestRepository.getRequest(testRequestId) } returns completedRequest
        coEvery { requestRepository.isOwnerOfRequest(completedRequest) } returns true

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(CANNOT_BE_CLOSED))
        assertFalse(errorState.canRetry)
      }

  @Test
  fun initShowsErrorWhenRequestStatusCancelled() =
      testDispatcher.runBlockingTest {
        // Given
        val cancelledRequest = testRequest.copy(status = RequestStatus.CANCELLED)
        coEvery { requestRepository.getRequest(testRequestId) } returns cancelledRequest
        coEvery { requestRepository.isOwnerOfRequest(cancelledRequest) } returns true

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(CANNOT_BE_CLOSED))
        assertFalse(errorState.canRetry)
      }

  @Test
  fun initShowsErrorWhenRequestStatusArchived() =
      testDispatcher.runBlockingTest {
        // Given
        val archivedRequest = testRequest.copy(status = RequestStatus.ARCHIVED)
        coEvery { requestRepository.getRequest(testRequestId) } returns archivedRequest
        coEvery { requestRepository.isOwnerOfRequest(archivedRequest) } returns true

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(CANNOT_BE_CLOSED))
        assertFalse(errorState.canRetry)
      }

  @Test
  fun initSucceedsWithInProgressStatus() =
      testDispatcher.runBlockingTest {
        // Given
        val inProgressRequest = testRequest.copy(status = RequestStatus.IN_PROGRESS)
        coEvery { requestRepository.getRequest(testRequestId) } returns inProgressRequest
        coEvery { requestRepository.isOwnerOfRequest(inProgressRequest) } returns true
        coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
        coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } returns testHelper2

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Ready)
      }

  @Test
  fun initHandlesEmptyHelpersListSuccessfully() =
      testDispatcher.runBlockingTest {
        // Given
        val requestNoHelpers = testRequest.copy(people = emptyList())
        coEvery { requestRepository.getRequest(testRequestId) } returns requestNoHelpers
        coEvery { requestRepository.isOwnerOfRequest(requestNoHelpers) } returns true

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Ready)
        val readyState = state as ValidationState.Ready
        assertTrue(readyState.helpers.isEmpty())
      }

  @Test
  fun initContinuesWhenSomeHelperProfilesFailToLoad() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
        coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
        coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
        coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } throws Exception(NETWORK_ERROR)

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Ready)
        val readyState = state as ValidationState.Ready
        assertEquals(1, readyState.helpers.size)
        assertEquals(testHelper1, readyState.helpers[0])
      }

  @Test
  fun initShowsErrorWhenAllHelperProfilesFailToLoad() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
        coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
        coEvery { userProfileRepository.getUserProfile(HELPER_1) } throws Exception(NETWORK_ERROR)
        coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } throws Exception(NETWORK_ERROR)

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(COULD_NOT_LOAD_PROFILE))
        assertTrue(errorState.canRetry)
      }

  @Test
  fun initShowsErrorWhenRequestLoadingFails() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } throws Exception(NETWORK_ERROR)

        // When
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(FAILED_TO_LOAD_REQUEST))
        assertTrue(errorState.canRetry)
      }

  // ==================== Helper Selection Tests ====================

  @Test
  fun toggleHelperSelectionAddsHelperWhenNotSelected() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()

        // When
        viewModel.toggleHelperSelection(HELPER_1)

        // Then
        val state = viewModel.state as ValidationState.Ready
        assertTrue(state.selectedHelperIds.contains(HELPER_1))
        assertEquals(1, state.selectedHelperIds.size)
      }

  @Test
  fun toggleHelperSelectionRemovesHelperWhenAlreadySelected() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)

        // When
        viewModel.toggleHelperSelection(HELPER_1)

        // Then
        val state = viewModel.state as ValidationState.Ready
        assertFalse(state.selectedHelperIds.contains(HELPER_1))
        assertTrue(state.selectedHelperIds.isEmpty())
      }

  @Test
  fun toggleHelperSelectionAllowsMultipleSelections() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()

        // When
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.toggleHelperSelection(ID_HELPER2)

        // Then
        val state = viewModel.state as ValidationState.Ready
        assertTrue(state.selectedHelperIds.contains(HELPER_1))
        assertTrue(state.selectedHelperIds.contains(ID_HELPER2))
        assertEquals(2, state.selectedHelperIds.size)
      }

  @Test
  fun toggleHelperSelectionDoesNothingWhenNotInReadyState() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
        coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns false
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
        // State is now Error

        // When
        viewModel.toggleHelperSelection(HELPER_1)

        // Then
        assertTrue(viewModel.state is ValidationState.Error)
      }

  // ==================== Confirmation Tests ====================

  @Test
  fun showConfirmationTransitionsToConfirmingStateWithCorrectKudos() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.toggleHelperSelection(ID_HELPER2)

        // When
        viewModel.showConfirmation()

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Confirming)
        val confirmingState = state as ValidationState.Confirming
        assertEquals(testRequest, confirmingState.request)
        assertEquals(2, confirmingState.selectedHelpers.size)
        assertEquals(2 * KudosConstants.KUDOS_PER_HELPER, confirmingState.kudosToAward)
        // Removed: assertEquals(KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION,
        // confirmingState.creatorBonus)
      }

  @Test
  fun showConfirmationWithNoHelpersSelectedShowsZeroKudos() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()

        // When
        viewModel.showConfirmation()

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Confirming)
        val confirmingState = state as ValidationState.Confirming
        assertEquals(0, confirmingState.kudosToAward)
        // Removed: assertEquals(0, confirmingState.creatorBonus)
        assertTrue(confirmingState.selectedHelpers.isEmpty())
      }

  @Test
  fun showConfirmationDoesNothingWhenNotInReadyState() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.showConfirmation()
        // Now in Confirming state

        // When
        viewModel.showConfirmation() // Try again

        // Then
        assertTrue(viewModel.state is ValidationState.Confirming)
      }

  @Test
  fun cancelConfirmationDoesNothingWhenNotInConfirmingState() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()

        // When
        viewModel.cancelConfirmation()

        // Then
        assertTrue(viewModel.state is ValidationState.Ready)
      }

  // ==================== Confirm and Close Tests ====================

  @Test
  fun confirmAndCloseSucceedsWithSelectedHelpersAndAwardsKudos() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.showConfirmation()

        coEvery { requestRepository.closeRequest(testRequestId, listOf(HELPER_1)) } returns true
        coEvery { userProfileRepository.awardKudosBatch(any()) } returns Unit

        // When
        viewModel.confirmAndClose()

        // Then
        assertTrue(viewModel.state is ValidationState.Success)
        coVerify {
          requestRepository.closeRequest(testRequestId, listOf(HELPER_1))
          userProfileRepository.awardKudosBatch(
              match {
                it[HELPER_1] == KudosConstants.KUDOS_PER_HELPER &&
                    !it.containsKey(testCreatorId) &&
                    it.size == 1
              })
        }
      }

  @Test
  fun confirmAndCloseWithoutCreatorBonusWhenCloseRequestReturnsFalse() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.showConfirmation()

        coEvery { requestRepository.closeRequest(testRequestId, listOf(HELPER_1)) } returns false
        coEvery { userProfileRepository.awardKudosBatch(any()) } returns Unit

        // When
        viewModel.confirmAndClose()

        // Then
        assertTrue(viewModel.state is ValidationState.Success)
        coVerify {
          userProfileRepository.awardKudosBatch(
              match {
                it[HELPER_1] == KudosConstants.KUDOS_PER_HELPER &&
                    !it.containsKey(testCreatorId) &&
                    it.size == 1
              })
        }
      }

  @Test
  fun confirmAndCloseSucceedsWithMultipleHelpers() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.toggleHelperSelection(ID_HELPER2)
        viewModel.showConfirmation()

        coEvery {
          requestRepository.closeRequest(testRequestId, listOf(HELPER_1, ID_HELPER2))
        } returns true
        coEvery { userProfileRepository.awardKudosBatch(any()) } returns Unit

        // When
        viewModel.confirmAndClose()

        // Then
        assertTrue(viewModel.state is ValidationState.Success)
        coVerify {
          userProfileRepository.awardKudosBatch(
              match {
                it[HELPER_1] == KudosConstants.KUDOS_PER_HELPER &&
                    it[ID_HELPER2] == KudosConstants.KUDOS_PER_HELPER &&
                    !it.containsKey(testCreatorId) &&
                    it.size == 2
              })
        }
      }

  @Test
  fun confirmAndCloseWithNoHelpersSelectedClosesRequestWithoutKudos() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.showConfirmation()

        coEvery { requestRepository.closeRequest(testRequestId, emptyList()) } returns false

        // When
        viewModel.confirmAndClose()

        // Then
        assertTrue(viewModel.state is ValidationState.Success)
        coVerify(exactly = 0) { userProfileRepository.awardKudosBatch(any()) }
      }

  @Test
  fun confirmAndCloseHandlesRequestClosureException() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.showConfirmation()

        coEvery { requestRepository.closeRequest(any(), any()) } throws
            RequestClosureException.InvalidStatus(RequestStatus.COMPLETED)

        // When
        viewModel.confirmAndClose()

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains("Failed to close request"))
        assertTrue(errorState.canRetry)
      }

  @Test
  fun confirmAndCloseHandlesGenericException() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()
        viewModel.toggleHelperSelection(HELPER_1)
        viewModel.showConfirmation()

        coEvery { requestRepository.closeRequest(any(), any()) } throws
            Exception("Unexpected error")

        // When
        viewModel.confirmAndClose()

        // Then
        val state = viewModel.state
        assertTrue(state is ValidationState.Error)
        val errorState = state as ValidationState.Error
        assertTrue(errorState.message.contains(UNEXPECTED_ERROR_OCCURED))
        assertTrue(errorState.canRetry)
      }

  @Test
  fun confirmAndCloseDoesNothingWhenNotInConfirmingState() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()

        // When
        viewModel.confirmAndClose()

        // Then
        assertTrue(viewModel.state is ValidationState.Ready)
        coVerify(exactly = 0) { requestRepository.closeRequest(any(), any()) }
      }

  // ==================== Retry and Reset Tests ====================

  @Test
  fun retryReloadsRequestData() =
      testDispatcher.runBlockingTest {
        // Given
        coEvery { requestRepository.getRequest(testRequestId) } throws Exception(NETWORK_ERROR)
        viewModel =
            ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
        assertTrue(viewModel.state is ValidationState.Error)

        // Now make it succeed
        coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
        coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
        coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
        coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } returns testHelper2

        // When
        viewModel.retry()

        // Then
        assertTrue(viewModel.state is ValidationState.Ready)
      }

  @Test
  fun resetSetsStateToLoading() =
      testDispatcher.runBlockingTest {
        // Given
        setupReadyState()

        // When
        viewModel.reset()

        // Then
        assertTrue(viewModel.state is ValidationState.Loading)
      }

  // ==================== Factory Tests ====================

  @Test
  fun factoryCreatesValidateRequestViewModelSuccessfully() =
      testDispatcher.runBlockingTest {
        // Given
        val factory =
            ValidateRequestViewModelFactory(testRequestId, requestRepository, userProfileRepository)

        // When
        val vm = factory.create(ValidateRequestViewModel::class.java)

        // Then
        assertNotNull(vm)
      }

  @Test(expected = IllegalArgumentException::class)
  fun factoryThrowsExceptionForWrongViewModelClass() {
    // Given
    val factory =
        ValidateRequestViewModelFactory(testRequestId, requestRepository, userProfileRepository)

    // When
    factory.create(DummyViewModel::class.java)

    // Then - expects exception
  }

  // ==================== Helper Methods ====================

  private fun setupReadyState() {
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
    coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
    coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } returns testHelper2

    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

    assertTrue(viewModel.state is ValidationState.Ready)
  }

  // Dummy ViewModel for factory test
  private class DummyViewModel : androidx.lifecycle.ViewModel()
}
