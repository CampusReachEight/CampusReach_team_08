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
import kotlinx.coroutines.test.*
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
    Dispatchers.setMain(UnconfinedTestDispatcher())
    requestRepository = mockk()
    userProfileRepository = mockk()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ==================== Initialization Tests ====================

  @Test
  fun initLoadsRequestDataSuccessfully() = runTest {
    // Set up Main dispatcher for this test
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
      coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
      coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
      coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } returns testHelper2

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Ready)
      val readyState = state as ValidationState.Ready
      assertEquals(testRequest, readyState.request)
      assertEquals(2, readyState.helpers.size)
      assertTrue(readyState.selectedHelperIds.isEmpty())
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initShowsErrorWhenUserIsNotOwner() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
      coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns false

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(NOT_OWNER))
      assertFalse(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initShowsErrorWhenRequestStatusCompleted() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      val completedRequest = testRequest.copy(status = RequestStatus.COMPLETED)
      coEvery { requestRepository.getRequest(testRequestId) } returns completedRequest
      coEvery { requestRepository.isOwnerOfRequest(completedRequest) } returns true

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(CANNOT_BE_CLOSED))
      assertFalse(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initShowsErrorWhenRequestStatusCancelled() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      val cancelledRequest = testRequest.copy(status = RequestStatus.CANCELLED)
      coEvery { requestRepository.getRequest(testRequestId) } returns cancelledRequest
      coEvery { requestRepository.isOwnerOfRequest(cancelledRequest) } returns true

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(CANNOT_BE_CLOSED))
      assertFalse(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initShowsErrorWhenRequestStatusArchived() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      val archivedRequest = testRequest.copy(status = RequestStatus.ARCHIVED)
      coEvery { requestRepository.getRequest(testRequestId) } returns archivedRequest
      coEvery { requestRepository.isOwnerOfRequest(archivedRequest) } returns true

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(CANNOT_BE_CLOSED))
      assertFalse(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }
  /**
   * @Test fun initSucceedsWithInProgressStatus() = runTest {
   *   Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
   *
   * try { // Given val inProgressRequest = testRequest.copy(status = RequestStatus.IN_PROGRESS)
   * coEvery { requestRepository.getRequest(testRequestId) } returns inProgressRequest coEvery {
   * requestRepository.isOwnerOfRequest(inProgressRequest) } returns true coEvery {
   * userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1 coEvery {
   * userProfileRepository.getUserProfile(ID_HELPER2) } returns testHelper2
   *
   * // When viewModel = ValidateRequestViewModel(testRequestId, requestRepository,
   * userProfileRepository)
   *
   * // Then val state = viewModel.state assertTrue(state is ValidationState.Ready) } finally {
   * Dispatchers.resetMain() } }
   */
  @Test
  fun initHandlesEmptyHelpersListSuccessfully() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      val requestNoHelpers = testRequest.copy(people = emptyList())
      coEvery { requestRepository.getRequest(testRequestId) } returns requestNoHelpers
      coEvery { requestRepository.isOwnerOfRequest(requestNoHelpers) } returns true

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Ready)
      val readyState = state as ValidationState.Ready
      assertTrue(readyState.helpers.isEmpty())
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initContinuesWhenSomeHelperProfilesFailToLoad() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
      coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
      coEvery { userProfileRepository.getUserProfile(HELPER_1) } returns testHelper1
      coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } throws Exception(NETWORK_ERROR)

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Ready)
      val readyState = state as ValidationState.Ready
      assertEquals(1, readyState.helpers.size)
      assertEquals(testHelper1, readyState.helpers[0])
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initShowsErrorWhenAllHelperProfilesFailToLoad() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
      coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
      coEvery { userProfileRepository.getUserProfile(HELPER_1) } throws Exception(NETWORK_ERROR)
      coEvery { userProfileRepository.getUserProfile(ID_HELPER2) } throws Exception(NETWORK_ERROR)

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(COULD_NOT_LOAD_PROFILE))
      assertTrue(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun initShowsErrorWhenRequestLoadingFails() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } throws Exception(NETWORK_ERROR)

      // When
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(FAILED_TO_LOAD_REQUEST))
      assertTrue(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }

  // ==================== Helper Selection Tests ====================

  @Test
  fun toggleHelperSelectionAddsHelperWhenNotSelected() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()

      // When
      viewModel.toggleHelperSelection(HELPER_1)

      // Then
      val state = viewModel.state as ValidationState.Ready
      assertTrue(state.selectedHelperIds.contains(HELPER_1))
      assertEquals(1, state.selectedHelperIds.size)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun toggleHelperSelectionRemovesHelperWhenAlreadySelected() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()
      viewModel.toggleHelperSelection(HELPER_1)

      // When
      viewModel.toggleHelperSelection(HELPER_1)

      // Then
      val state = viewModel.state as ValidationState.Ready
      assertFalse(state.selectedHelperIds.contains(HELPER_1))
      assertTrue(state.selectedHelperIds.isEmpty())
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun toggleHelperSelectionAllowsMultipleSelections() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun toggleHelperSelectionDoesNothingWhenNotInReadyState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
      coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns false
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
      // State is now Error

      // When
      viewModel.toggleHelperSelection(HELPER_1)

      // Then
      assertTrue(viewModel.state is ValidationState.Error)
    } finally {
      Dispatchers.resetMain()
    }
  }

  // ==================== Confirmation Tests ====================

  @Test
  fun showConfirmationTransitionsToConfirmingStateWithCorrectKudos() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
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
      assertEquals(KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION, confirmingState.creatorBonus)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun showConfirmationWithNoHelpersSelectedShowsZeroKudos() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()

      // When
      viewModel.showConfirmation()

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Confirming)
      val confirmingState = state as ValidationState.Confirming
      assertEquals(0, confirmingState.kudosToAward)
      assertEquals(0, confirmingState.creatorBonus)
      assertTrue(confirmingState.selectedHelpers.isEmpty())
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun showConfirmationDoesNothingWhenNotInReadyState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()
      viewModel.showConfirmation() // Now in Confirming state

      // When
      viewModel.showConfirmation() // Try again

      // Then
      assertTrue(viewModel.state is ValidationState.Confirming)
    } finally {
      Dispatchers.resetMain()
    }
  }
  /**
   * @Test fun cancelConfirmationReturnsToReadyStateWithSelectionsPreserved() = runTest {
   *   Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
   *
   * try { // Given setupReadyState() viewModel.toggleHelperSelection(HELPER_1)
   * viewModel.showConfirmation()
   *
   * // When viewModel.cancelConfirmation()
   *
   * // Then val state = viewModel.state assertTrue(state is ValidationState.Ready) val readyState =
   * state as ValidationState.Ready assertTrue(readyState.selectedHelperIds.contains(HELPER_1)) }
   * finally { Dispatchers.resetMain() } }
   */
  @Test
  fun cancelConfirmationDoesNothingWhenNotInConfirmingState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()

      // When
      viewModel.cancelConfirmation()

      // Then
      assertTrue(viewModel.state is ValidationState.Ready)
    } finally {
      Dispatchers.resetMain()
    }
  }

  // ==================== Confirm and Close Tests ====================

  @Test
  fun confirmAndCloseSucceedsWithSelectedHelpersAndAwardsKudos() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun confirmAndCloseWithoutCreatorBonusWhenCloseRequestReturnsFalse() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun confirmAndCloseSucceedsWithMultipleHelpers() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun confirmAndCloseWithNoHelpersSelectedClosesRequestWithoutKudos() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()
      viewModel.showConfirmation()

      coEvery { requestRepository.closeRequest(testRequestId, emptyList()) } returns false

      // When
      viewModel.confirmAndClose()

      // Then
      assertTrue(viewModel.state is ValidationState.Success)
      coVerify(exactly = 0) { userProfileRepository.awardKudosBatch(any()) }
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun confirmAndCloseHandlesRequestClosureException() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun confirmAndCloseHandlesGenericException() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()
      viewModel.toggleHelperSelection(HELPER_1)
      viewModel.showConfirmation()

      coEvery { requestRepository.closeRequest(any(), any()) } throws Exception("Unexpected error")

      // When
      viewModel.confirmAndClose()

      // Then
      val state = viewModel.state
      assertTrue(state is ValidationState.Error)
      val errorState = state as ValidationState.Error
      assertTrue(errorState.message.contains(UNEXPECTED_ERROR_OCCURED))
      assertTrue(errorState.canRetry)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun confirmAndCloseDoesNothingWhenNotInConfirmingState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()

      // When
      viewModel.confirmAndClose()

      // Then
      assertTrue(viewModel.state is ValidationState.Ready)
      coVerify(exactly = 0) { requestRepository.closeRequest(any(), any()) }
    } finally {
      Dispatchers.resetMain()
    }
  }

  // ==================== Retry and Reset Tests ====================

  @Test
  fun retryReloadsRequestData() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      coEvery { requestRepository.getRequest(testRequestId) } throws Exception(NETWORK_ERROR)
      viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
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
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun resetSetsStateToLoading() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      setupReadyState()

      // When
      viewModel.reset()

      // Then
      assertTrue(viewModel.state is ValidationState.Loading)
    } finally {
      Dispatchers.resetMain()
    }
  }

  // ==================== Factory Tests ====================

  @Test
  fun factory_creates_ValidateRequestViewModel_successfully() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

    try {
      // Given
      val factory =
          ValidateRequestViewModelFactory(testRequestId, requestRepository, userProfileRepository)

      // When
      @Suppress("USELESS_IS_CHECK") val vm = factory.create(ValidateRequestViewModel::class.java)

      // Then
      assertNotNull(vm)
    } finally {
      Dispatchers.resetMain()
    }
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
