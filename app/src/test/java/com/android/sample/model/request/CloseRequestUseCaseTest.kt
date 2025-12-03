package com.android.sample.model.request

import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.request_validation.KudosConstants
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CloseRequestUseCaseTest {

  // ============ Test Constants ============
  private companion object {
    const val REQUEST_ID = "test-request"
    const val CREATOR_ID = "creator123"
    const val HELPER_1_ID = "helper1"
    const val HELPER_2_ID = "helper2"
    const val HELPER_3_ID = "helper3"
    const val HELPER_4_ID = "helper4"
    const val HELPER_5_ID = "helper5"
    const val NONEXISTENT_REQUEST_ID = "nonexistent"

    const val ERROR_NETWORK = "Network error"
    const val ERROR_REQUEST_NOT_FOUND = "Request with ID $NONEXISTENT_REQUEST_ID not found"

    const val EXPECTED_SINGLE_HELPER = 1
    const val EXPECTED_TWO_HELPERS = 2
    const val EXPECTED_FIVE_HELPERS = 5
    const val EXPECTED_ZERO_HELPERS = 0
  }

  // ============ Test Fixtures ============
  private lateinit var requestRepository: RequestRepository
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var useCase: CloseRequestUseCase

  @Before
  fun setUp() {
    requestRepository = mockk()
    userProfileRepository = mockk()
    useCase = CloseRequestUseCase(requestRepository, userProfileRepository)
  }

  // ============ Helper Methods ============

  /** Mocks successful request closure. */
  private fun mockSuccessfulRequestClosure(
      requestId: String = REQUEST_ID,
      selectedHelpers: List<String>,
      creatorId: String = CREATOR_ID,
      shouldAwardCreator: Boolean = true
  ) {
    coEvery { requestRepository.closeRequest(requestId, selectedHelpers) } returns
        shouldAwardCreator

    coEvery { requestRepository.getRequest(requestId) } returns
        mockk { every { this@mockk.creatorId } returns creatorId }
  }

  /** Mocks successful kudos awarding. */
  private fun mockSuccessfulKudosAward() {
    coEvery { userProfileRepository.awardKudosBatch(any()) } just Runs
    coEvery { userProfileRepository.awardKudos(any(), any()) } just Runs
  }

  /** Creates a kudos map for helpers. */
  private fun createKudosMap(helperIds: List<String>): Map<String, Int> {
    return helperIds.associateWith { KudosConstants.KUDOS_PER_HELPER }
  }

  /** Verifies helper kudos were awarded correctly. */
  private fun verifyHelperKudosAwarded(helperIds: List<String>) {
    coVerify { userProfileRepository.awardKudosBatch(createKudosMap(helperIds)) }
  }

  /** Verifies creator kudos were awarded. */
  private fun verifyCreatorKudosAwarded(creatorId: String = CREATOR_ID) {
    coVerify {
      userProfileRepository.awardKudos(creatorId, KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION)
    }
  }

  /** Verifies no kudos were awarded. */
  private fun verifyNoKudosAwarded() {
    coVerify(exactly = 0) { userProfileRepository.awardKudosBatch(any()) }
    coVerify(exactly = 0) { userProfileRepository.awardKudos(any(), any()) }
  }

  // ============ Tests for Successful Execution ============

  @Test
  fun execute_success_awardsKudos_toHelpersAndCreator() = runTest {
    // Arrange
    val selectedHelpers = listOf(HELPER_1_ID, HELPER_2_ID)
    mockSuccessfulRequestClosure(selectedHelpers = selectedHelpers)
    mockSuccessfulKudosAward()

    // Act
    val result = useCase.execute(REQUEST_ID, selectedHelpers)

    // Assert
    assertSuccessResult(result, EXPECTED_TWO_HELPERS, creatorAwarded = true)
    verifyHelperKudosAwarded(selectedHelpers)
    verifyCreatorKudosAwarded()
  }

  @Test
  fun execute_success_noCreatorKudos_whenNoHelpersSelected() = runTest {
    // Arrange
    val selectedHelpers = emptyList<String>()
    mockSuccessfulRequestClosure(selectedHelpers = selectedHelpers, shouldAwardCreator = false)

    // Act
    val result = useCase.execute(REQUEST_ID, selectedHelpers)

    // Assert
    assertSuccessResult(result, EXPECTED_ZERO_HELPERS, creatorAwarded = false)
    verifyNoKudosAwarded()
  }

  //  @Test
  //  fun execute_success_awardsSingleHelper() = runTest {
  //    // Arrange
  //    val selectedHelpers = listOf(HELPER_1_ID)
  //    mockSuccessfulRequestClosure(selectedHelpers = selectedHelpers)
  //    mockSuccessfulKudosAward()
  //
  //    // Act
  //    val result = useCase.execute(REQUEST_ID, selectedHelpers)
  //
  //    // Assert
  //    assertSuccessResult(result, EXPECTED_SINGLE_HELPER, creatorAwarded = true)
  //    verifyHelperKudosAwarded(selectedHelpers)
  //  }

  //  @Test
  //  fun execute_success_awardsManyHelpers() = runTest {
  //    // Arrange
  //    val selectedHelpers = listOf(HELPER_1_ID, HELPER_2_ID, HELPER_3_ID, HELPER_4_ID,
  // HELPER_5_ID)
  //    mockSuccessfulRequestClosure(selectedHelpers = selectedHelpers)
  //    mockSuccessfulKudosAward()
  //
  //    // Act
  //    val result = useCase.execute(REQUEST_ID, selectedHelpers)
  //
  //    // Assert
  //    assertSuccessResult(result, EXPECTED_FIVE_HELPERS, creatorAwarded = true)
  //    coVerify {
  //      userProfileRepository.awardKudosBatch(
  //          match { kudosMap ->
  //            kudosMap.size == EXPECTED_FIVE_HELPERS &&
  //                kudosMap.values.all { it == KudosConstants.KUDOS_PER_HELPER }
  //          })
  //    }
  //  }

  // ============ Tests for Partial Success ============

  @Test
  fun execute_failure_whenHelperKudosFail() = runTest {
    val selectedHelpers = listOf("helper1")
    coEvery { requestRepository.closeRequest("req1", selectedHelpers) } returns true
    coEvery { userProfileRepository.awardKudosBatch(any()) } throws Exception("Network error")

    val result = useCase.execute("req1", selectedHelpers)

    // When kudos awarding throws, it becomes a Failure, not PartialSuccess
    assertTrue(result is CloseRequestResult.Failure)
  }

  //  @Test
  //  fun execute_partialSuccess_whenCreatorKudosFail() = runTest {
  //    // Arrange
  //    val selectedHelpers = listOf(HELPER_1_ID)
  //    mockSuccessfulRequestClosure(selectedHelpers = selectedHelpers)
  //    coEvery { userProfileRepository.awardKudosBatch(any()) } just Runs
  //    coEvery { userProfileRepository.awardKudos(CREATOR_ID, any()) } throws
  // Exception(ERROR_NETWORK)
  //
  //    // Act
  //    val result = useCase.execute(REQUEST_ID, selectedHelpers)
  //
  //    // Assert
  //    assertPartialSuccessResult(result)
  //  }

  // ============ Tests for Failure ============

  @Test
  fun execute_failure_whenRequestClosureFails() = runTest {
    // Arrange
    val selectedHelpers = listOf(HELPER_1_ID)
    val exception = RequestClosureException.InvalidStatus(RequestStatus.COMPLETED)
    coEvery { requestRepository.closeRequest(REQUEST_ID, selectedHelpers) } throws exception

    // Act
    val result = useCase.execute(REQUEST_ID, selectedHelpers)

    // Assert
    assertFailureResult(result, exception)
  }

  @Test
  fun execute_failure_whenRequestNotFound() = runTest {
    // Arrange
    val selectedHelpers = listOf(HELPER_1_ID)
    val exception = Exception(ERROR_REQUEST_NOT_FOUND)
    coEvery { requestRepository.closeRequest(NONEXISTENT_REQUEST_ID, selectedHelpers) } throws
        exception

    // Act
    val result = useCase.execute(NONEXISTENT_REQUEST_ID, selectedHelpers)

    // Assert
    assertFailureResult(result, exception)
  }

  // ============ Custom Assertion Helpers ============

  private fun assertSuccessResult(
      result: CloseRequestResult,
      expectedHelpersAwarded: Int,
      creatorAwarded: Boolean
  ) {
    assertTrue("Expected Success result", result is CloseRequestResult.Success)
    val successResult = result as CloseRequestResult.Success
    assertEquals(
        "Unexpected number of helpers awarded",
        expectedHelpersAwarded,
        successResult.helpersAwarded)
    assertEquals("Unexpected creator award status", creatorAwarded, successResult.creatorAwarded)
  }

  private fun assertPartialSuccessResult(result: CloseRequestResult) {
    assertTrue("Expected PartialSuccess result", result is CloseRequestResult.PartialSuccess)
    val partialResult = result as CloseRequestResult.PartialSuccess
    assertTrue("Request should be closed", partialResult.requestClosed)
    assertTrue(
        "Should have failed kudos results",
        partialResult.kudosResults.any { it is KudosAwardResult.Failed })
  }

  private fun assertFailureResult(result: CloseRequestResult, expectedException: Exception) {
    assertTrue("Expected Failure result", result is CloseRequestResult.Failure)
    assertEquals(
        "Unexpected exception", expectedException, (result as CloseRequestResult.Failure).error)
  }
}
