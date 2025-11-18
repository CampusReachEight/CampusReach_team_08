package com.android.sample.ui.request_validation

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
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateRequestViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var requestRepository: RequestRepository
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var viewModel: ValidateRequestViewModel

  private val testRequestId = "request123"
  private val testCreatorId = "creator123"
  private val testHelper1 =
      UserProfile(
          id = "helper1",
          name = "Helper",
          lastName = "One",
          email = "helper1@test.com",
          photo = null,
          kudos = 100,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())
  private val testHelper2 =
      UserProfile(
          id = "helper2",
          name = "Helper",
          lastName = "Two",
          email = "helper2@test.com",
          photo = null,
          kudos = 50,
          section = UserSections.MATHEMATICS,
          arrivalDate = Date())
  private val testRequest =
      Request(
          requestId = testRequestId,
          creatorId = testCreatorId,
          title = "Test Request",
          description = "Test Description",
          requestType = listOf(RequestType.STUDYING),
          location = Location(0.0, 0.0, "Test Location"),
          locationName = "Test Location",
          status = RequestStatus.OPEN,
          startTimeStamp = Date(),
          expirationTime = Date(System.currentTimeMillis() + 86400000),
          people = listOf("helper1", "helper2"),
          tags = listOf(Tags.URGENT))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
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
  fun `init loads request data successfully`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
    coEvery { userProfileRepository.getUserProfile("helper1") } returns testHelper1
    coEvery { userProfileRepository.getUserProfile("helper2") } returns testHelper2

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Ready)
    val readyState = state as ValidationState.Ready
    assertEquals(testRequest, readyState.request)
    assertEquals(2, readyState.helpers.size)
    assertTrue(readyState.selectedHelperIds.isEmpty())
  }

  @Test
  fun `init shows error when user is not owner`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns false

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("not the owner"))
    assertFalse(errorState.canRetry)
  }

  @Test
  fun `init shows error when request status is COMPLETED`() = runTest {
    // Given
    val completedRequest = testRequest.copy(status = RequestStatus.COMPLETED)
    coEvery { requestRepository.getRequest(testRequestId) } returns completedRequest
    coEvery { requestRepository.isOwnerOfRequest(completedRequest) } returns true

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("cannot be closed"))
    assertFalse(errorState.canRetry)
  }

  @Test
  fun `init shows error when request status is CANCELLED`() = runTest {
    // Given
    val cancelledRequest = testRequest.copy(status = RequestStatus.CANCELLED)
    coEvery { requestRepository.getRequest(testRequestId) } returns cancelledRequest
    coEvery { requestRepository.isOwnerOfRequest(cancelledRequest) } returns true

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("cannot be closed"))
    assertFalse(errorState.canRetry)
  }

  @Test
  fun `init shows error when request status is ARCHIVED`() = runTest {
    // Given
    val archivedRequest = testRequest.copy(status = RequestStatus.ARCHIVED)
    coEvery { requestRepository.getRequest(testRequestId) } returns archivedRequest
    coEvery { requestRepository.isOwnerOfRequest(archivedRequest) } returns true

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("cannot be closed"))
    assertFalse(errorState.canRetry)
  }

  @Test
  fun `init succeeds with IN_PROGRESS status`() = runTest {
    // Given
    val inProgressRequest = testRequest.copy(status = RequestStatus.IN_PROGRESS)
    coEvery { requestRepository.getRequest(testRequestId) } returns inProgressRequest
    coEvery { requestRepository.isOwnerOfRequest(inProgressRequest) } returns true
    coEvery { userProfileRepository.getUserProfile("helper1") } returns testHelper1
    coEvery { userProfileRepository.getUserProfile("helper2") } returns testHelper2

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Ready)
  }

  @Test
  fun `init handles empty helpers list successfully`() = runTest {
    // Given
    val requestNoHelpers = testRequest.copy(people = emptyList())
    coEvery { requestRepository.getRequest(testRequestId) } returns requestNoHelpers
    coEvery { requestRepository.isOwnerOfRequest(requestNoHelpers) } returns true

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Ready)
    val readyState = state as ValidationState.Ready
    assertTrue(readyState.helpers.isEmpty())
  }

  @Test
  fun `init continues when some helper profiles fail to load`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
    coEvery { userProfileRepository.getUserProfile("helper1") } returns testHelper1
    coEvery { userProfileRepository.getUserProfile("helper2") } throws Exception("Network error")

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Ready)
    val readyState = state as ValidationState.Ready
    assertEquals(1, readyState.helpers.size)
    assertEquals(testHelper1, readyState.helpers[0])
  }

  @Test
  fun `init shows error when all helper profiles fail to load`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
    coEvery { userProfileRepository.getUserProfile("helper1") } throws Exception("Network error")
    coEvery { userProfileRepository.getUserProfile("helper2") } throws Exception("Network error")

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("Could not load helper profiles"))
    assertTrue(errorState.canRetry)
  }

  @Test
  fun `init shows error when request loading fails`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } throws Exception("Network error")

    // When
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("Failed to load request"))
    assertTrue(errorState.canRetry)
  }

  // ==================== Helper Selection Tests ====================

  @Test
  fun `toggleHelperSelection adds helper when not selected`() = runTest {
    // Given
    setupReadyState()

    // When
    viewModel.toggleHelperSelection("helper1")

    // Then
    val state = viewModel.state as ValidationState.Ready
    assertTrue(state.selectedHelperIds.contains("helper1"))
    assertEquals(1, state.selectedHelperIds.size)
  }

  @Test
  fun `toggleHelperSelection removes helper when already selected`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")

    // When
    viewModel.toggleHelperSelection("helper1")

    // Then
    val state = viewModel.state as ValidationState.Ready
    assertFalse(state.selectedHelperIds.contains("helper1"))
    assertTrue(state.selectedHelperIds.isEmpty())
  }

  @Test
  fun `toggleHelperSelection allows multiple selections`() = runTest {
    // Given
    setupReadyState()

    // When
    viewModel.toggleHelperSelection("helper1")
    viewModel.toggleHelperSelection("helper2")

    // Then
    val state = viewModel.state as ValidationState.Ready
    assertTrue(state.selectedHelperIds.contains("helper1"))
    assertTrue(state.selectedHelperIds.contains("helper2"))
    assertEquals(2, state.selectedHelperIds.size)
  }

  @Test
  fun `toggleHelperSelection does nothing when not in Ready state`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns false
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()
    // State is now Error

    // When
    viewModel.toggleHelperSelection("helper1")

    // Then
    assertTrue(viewModel.state is ValidationState.Error)
  }

  // ==================== Confirmation Tests ====================

  @Test
  fun `showConfirmation transitions to Confirming state with correct kudos`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.toggleHelperSelection("helper2")

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
  }

  @Test
  fun `showConfirmation with no helpers selected shows zero kudos`() = runTest {
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
  }

  @Test
  fun `showConfirmation does nothing when not in Ready state`() = runTest {
    // Given
    setupReadyState()
    viewModel.showConfirmation() // Now in Confirming state

    val previousState = viewModel.state

    // When
    viewModel.showConfirmation() // Try again

    // Then
    assertEquals(previousState, viewModel.state)
  }

  @Test
  fun `cancelConfirmation returns to Ready state with selections preserved`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.showConfirmation()

    // When
    viewModel.cancelConfirmation()
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Ready)
    val readyState = state as ValidationState.Ready
    assertTrue(readyState.selectedHelperIds.contains("helper1"))
  }

  @Test
  fun `cancelConfirmation does nothing when not in Confirming state`() = runTest {
    // Given
    setupReadyState()
    val previousState = viewModel.state

    // When
    viewModel.cancelConfirmation()

    // Then
    // State should remain Ready (after eventual reload)
    advanceUntilIdle()
    assertTrue(viewModel.state is ValidationState.Ready)
  }

  // ==================== Confirm and Close Tests ====================

  @Test
  fun `confirmAndClose succeeds with selected helpers and awards kudos`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(testRequestId, listOf("helper1")) } returns true
    coEvery { userProfileRepository.awardKudosBatch(any()) } returns Unit

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.state is ValidationState.Success)
    coVerify {
      requestRepository.closeRequest(testRequestId, listOf("helper1"))
      userProfileRepository.awardKudosBatch(
          match {
            it["helper1"] == KudosConstants.KUDOS_PER_HELPER &&
                it[testCreatorId] == KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION &&
                it.size == 2
          })
    }
  }

  @Test
  fun `confirmAndClose succeeds with multiple helpers`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.toggleHelperSelection("helper2")
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(testRequestId, listOf("helper1", "helper2")) } returns
        true
    coEvery { userProfileRepository.awardKudosBatch(any()) } returns Unit

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.state is ValidationState.Success)
    coVerify {
      userProfileRepository.awardKudosBatch(
          match {
            it["helper1"] == KudosConstants.KUDOS_PER_HELPER &&
                it["helper2"] == KudosConstants.KUDOS_PER_HELPER &&
                it[testCreatorId] == KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION &&
                it.size == 3
          })
    }
  }

  @Test
  fun `confirmAndClose without creator bonus when closeRequest returns false`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(testRequestId, listOf("helper1")) } returns false
    coEvery { userProfileRepository.awardKudosBatch(any()) } returns Unit

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.state is ValidationState.Success)
    coVerify {
      userProfileRepository.awardKudosBatch(
          match {
            it["helper1"] == KudosConstants.KUDOS_PER_HELPER &&
                !it.containsKey(testCreatorId) &&
                it.size == 1
          })
    }
  }

  @Test
  fun `confirmAndClose with no helpers selected closes request without kudos`() = runTest {
    // Given
    setupReadyState()
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(testRequestId, emptyList()) } returns false

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.state is ValidationState.Success)
    coVerify(exactly = 0) { userProfileRepository.awardKudosBatch(any()) }
  }

  @Test
  fun `confirmAndClose handles RequestClosureException`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(any(), any()) } throws
        RequestClosureException.InvalidStatus(RequestStatus.COMPLETED)

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("Failed to close request"))
    assertTrue(errorState.canRetry)
  }

  @Test
  fun `confirmAndClose logs error when kudos award fails but still succeeds`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(testRequestId, listOf("helper1")) } returns true
    coEvery { userProfileRepository.awardKudosBatch(any()) } throws
        KudosException("Kudos service unavailable")

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    // Should still show success since request was closed
    assertTrue(viewModel.state is ValidationState.Success)
  }

  @Test
  fun `confirmAndClose handles generic exception`() = runTest {
    // Given
    setupReadyState()
    viewModel.toggleHelperSelection("helper1")
    viewModel.showConfirmation()

    coEvery { requestRepository.closeRequest(any(), any()) } throws Exception("Unexpected error")

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    val state = viewModel.state
    assertTrue(state is ValidationState.Error)
    val errorState = state as ValidationState.Error
    assertTrue(errorState.message.contains("unexpected error occurred"))
    assertTrue(errorState.canRetry)
  }

  @Test
  fun `confirmAndClose does nothing when not in Confirming state`() = runTest {
    // Given
    setupReadyState()
    val previousState = viewModel.state

    // When
    viewModel.confirmAndClose()
    advanceUntilIdle()

    // Then
    // Should remain in Ready state
    assertTrue(viewModel.state is ValidationState.Ready)
    coVerify(exactly = 0) { requestRepository.closeRequest(any(), any()) }
  }

  // ==================== Retry and Reset Tests ====================

  @Test
  fun `retry reloads request data`() = runTest {
    // Given
    coEvery { requestRepository.getRequest(testRequestId) } throws Exception("Network error")
    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()
    assertTrue(viewModel.state is ValidationState.Error)

    // Now make it succeed
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
    coEvery { userProfileRepository.getUserProfile("helper1") } returns testHelper1
    coEvery { userProfileRepository.getUserProfile("helper2") } returns testHelper2

    // When
    viewModel.retry()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.state is ValidationState.Ready)
  }

  @Test
  fun `reset sets state to Loading`() = runTest {
    // Given
    setupReadyState()

    // When
    viewModel.reset()

    // Then
    assertTrue(viewModel.state is ValidationState.Loading)
  }

  // ==================== Factory Tests ====================

  @Test
  fun `factory creates ValidateRequestViewModel successfully`() {
    // Given
    val factory =
        ValidateRequestViewModelFactory(testRequestId, requestRepository, userProfileRepository)

    // When
    val vm = factory.create(ValidateRequestViewModel::class.java)

    // Then
    assertNotNull(vm)
    assertTrue(vm is ValidateRequestViewModel)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `factory throws exception for wrong ViewModel class`() {
    // Given
    val factory =
        ValidateRequestViewModelFactory(testRequestId, requestRepository, userProfileRepository)

    // When
    factory.create(DummyViewModel::class.java)

    // Then - expects exception
  }

  // ==================== Helper Methods ====================

  private suspend fun TestScope.setupReadyState() {
    coEvery { requestRepository.getRequest(testRequestId) } returns testRequest
    coEvery { requestRepository.isOwnerOfRequest(testRequest) } returns true
    coEvery { userProfileRepository.getUserProfile("helper1") } returns testHelper1
    coEvery { userProfileRepository.getUserProfile("helper2") } returns testHelper2

    viewModel = ValidateRequestViewModel(testRequestId, requestRepository, userProfileRepository)
    advanceUntilIdle()

    assertTrue(viewModel.state is ValidationState.Ready)
  }

  // Dummy ViewModel for factory test
  private class DummyViewModel : androidx.lifecycle.ViewModel()
}

// Mock exception class
class KudosException(message: String) : Exception(message)
