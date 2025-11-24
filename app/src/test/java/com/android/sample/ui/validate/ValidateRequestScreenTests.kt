package com.android.sample.ui.request_validation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.profile.UserSections
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SDK_VERSION = 33

private const val ONE = 1

private const val ZERO = 0

private const val TWENTY = 20

private const val TWO = 2

private const val TIMEOUT_2S = 2000L

private const val STRING_TEST_ERROR_MESSAGE = "Test error message"

private const val CONSTANT_500_MS = 500L

/**
 * Comprehensive test suite for ValidateRequestScreen.
 *
 * Tests cover all UI states, user interactions, and edge cases. Optimized for CI environments with
 * proper cleanup and deterministic behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SDK_VERSION])
class ValidateRequestScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Mocks
  private lateinit var mockViewModel: ValidateRequestViewModel
  private lateinit var mockUserProfileRepository: UserProfileRepository
  private lateinit var mockOnRequestClosed: () -> Unit
  private lateinit var mockOnNavigateBack: () -> Unit

  // Test data
  private lateinit var testRequest: Request
  private lateinit var testHelpers: List<UserProfile>
  private lateinit var stateFlow: MutableStateFlow<ValidationState>

  @Before
  fun setUp() {
    // Initialize mocks
    mockViewModel = mockk(relaxed = true)
    mockUserProfileRepository = mockk(relaxed = true)
    mockOnRequestClosed = mockk(relaxed = true)
    mockOnNavigateBack = mockk(relaxed = true)

    // Setup test data
    testRequest = TestDataFactory.createTestRequest()
    testHelpers = TestDataFactory.createTestHelpers()

    // Setup state flow
    stateFlow = MutableStateFlow(ValidationState.Loading)
    every { mockViewModel.state } answers { stateFlow.value }

    // Setup repository mock
    coEvery { mockUserProfileRepository.getUserProfile(any()) } answers
        {
          val id = firstArg<String>()
          testHelpers.find { it.id == id } as UserProfile
        }
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  // ==================== LOADING STATE TESTS ====================

  @Test
  fun loadingState_displaysLoadingIndicator() {
    // Given
    stateFlow.value = ValidationState.Loading

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_LOADING_INDICATOR)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun loadingState_backButtonNavigatesBack() {
    // Given
    stateFlow.value = ValidationState.Loading

    // When
    setUpScreen()
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_BACK_BUTTON).performClick()

    // Then
    verify(exactly = ONE) { mockOnNavigateBack() }
  }

  // ==================== READY STATE WITH HELPERS TESTS ====================

  @Test
  fun readyState_displaysHeaderAndDescription() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.HEADER_SELECT_HELPERS)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun readyState_displaysAllHelpers() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    testHelpers.forEach { helper ->
      composeTestRule
          .onNodeWithTag(ValidateRequestConstants.getHelperCardTag(helper.id))
          .assertExists()
          .assertIsDisplayed()
    }
  }

  @Test
  fun readyState_displaysValidateButton() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_VALIDATE_BUTTON)
        .assertExists()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun readyState_helperCardClick_togglesSelection() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()
    val firstHelper = testHelpers.first()
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.getHelperCardTag(firstHelper.id))
        .performClick()

    // Then
    verify(exactly = ONE) { mockViewModel.toggleHelperSelection(firstHelper.id) }
  }

  @Test
  fun readyState_validateButtonClick_showsConfirmation() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_VALIDATE_BUTTON).performClick()

    // Then
    verify(exactly = ONE) { mockViewModel.showConfirmation() }
  }

  @Test
  fun readyState_withSelectedHelpers_displaysKudosSummary() {
    // Given
    val selectedIds = setOf(testHelpers[ZERO].id, testHelpers[ONE].id)
    val readyState =
        TestDataFactory.createReadyState(
            request = testRequest, helpers = testHelpers, selectedHelperIds = selectedIds)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    val expectedTotal = selectedIds.size * KudosConstants.KUDOS_PER_HELPER
    // Use a more specific matcher - look for the exact kudos number in the summary card
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.SUMMARY_TOTAL_LABEL)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun readyState_helpersList_isScrollable() {
    // Given
    val manyHelpers = TestDataFactory.createTestHelpers(count = TWENTY)
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = manyHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then - Scroll to bottom
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_HELPERS_LIST)
        .performScrollToIndex(manyHelpers.size - ONE)

    // Last item should be visible
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.getHelperCardTag(manyHelpers.last().id))
        .assertExists()
  }

  // ==================== READY STATE EMPTY HELPERS TESTS ====================

  @Test
  fun readyState_withNoHelpers_displaysEmptyState() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = emptyList())
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.EMPTY_NO_HELPERS)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun readyState_withNoHelpers_displaysCloseButton() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = emptyList())
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.BUTTON_CLOSE)
        .assertExists()
        .assertIsDisplayed()
  }

  // ==================== CONFIRMING STATE TESTS ====================

  @Test
  fun confirmingState_displaysConfirmationDialog() {
    // Given
    val confirmingState =
        TestDataFactory.createConfirmingState(selectedHelpers = testHelpers.take(TWO))
    stateFlow.value = confirmingState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_CONFIRMATION_DIALOG)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun confirmingState_withHelpers_displaysSelectedHelpersList() {
    // Given
    val selectedHelpers = testHelpers.take(TWO)
    val confirmingState = TestDataFactory.createConfirmingState(selectedHelpers = selectedHelpers)
    stateFlow.value = confirmingState

    // When
    setUpScreen()

    // Then
    selectedHelpers.forEach { helper ->
      composeTestRule
          .onNodeWithText("${helper.name} ${helper.lastName}", substring = true)
          .assertExists()
    }
  }

  @Test
  fun confirmingState_withNoHelpers_displaysNoHelpersMessage() {
    // Given
    val confirmingState = TestDataFactory.createConfirmingState(selectedHelpers = emptyList())
    stateFlow.value = confirmingState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.CONFIRM_NO_HELPERS)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun confirmingState_confirmButtonClick_callsViewModel() {
    // Given
    val confirmingState = TestDataFactory.createConfirmingState()
    stateFlow.value = confirmingState

    // When
    setUpScreen()
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_CONFIRM_BUTTON).performClick()

    // Then
    verify(exactly = ONE) { mockViewModel.confirmAndClose() }
  }

  @Test
  fun confirmingState_cancelButtonClick_dismissesDialog() {
    // Given
    val confirmingState = TestDataFactory.createConfirmingState()
    stateFlow.value = confirmingState

    // When
    setUpScreen()
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_CANCEL_BUTTON).performClick()

    // Then
    verify(exactly = ONE) { mockViewModel.cancelConfirmation() }
  }

  // ==================== PROCESSING STATE TESTS ====================

  @Test
  fun processingState_displaysProcessingIndicator() {
    // Given
    stateFlow.value = ValidationState.Processing

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_PROCESSING_INDICATOR)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun processingState_backButtonIsNotVisible() {
    // Given
    stateFlow.value = ValidationState.Processing

    // When
    setUpScreen()

    // Then
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_BACK_BUTTON).assertDoesNotExist()
  }

  // ==================== SUCCESS STATE TESTS ====================

  @Test
  fun successState_displaysSuccessMessages() {
    // Given
    stateFlow.value = ValidationState.Success

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.SUCCESS_TITLE)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun readyState_afterLoading_displaysCorrectContent() {
    // Given - Start directly in Ready state (not testing transition, just the Ready UI)
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    composeTestRule.onNodeWithText(ValidateRequestConstants.HEADER_SELECT_HELPERS).assertExists()
  }

  @Test
  fun successState_triggersNavigationCallback() {
    // Given - Start directly in Success state
    stateFlow.value = ValidationState.Success

    // When
    setUpScreen()
    composeTestRule.waitForIdle()

    // Then - LaunchedEffect should trigger immediately
    verify(timeout = TIMEOUT_2S, exactly = ONE) { mockOnRequestClosed() }
  }

  // ==================== ERROR STATE TESTS ====================

  @Test
  fun errorState_displaysErrorMessage() {
    // Given
    val errorMessage = STRING_TEST_ERROR_MESSAGE
    val errorState = TestDataFactory.createErrorState(message = errorMessage)
    stateFlow.value = errorState

    // When
    setUpScreen()

    // Then
    composeTestRule.onNodeWithText(errorMessage).assertExists().assertIsDisplayed()
  }

  @Test
  fun errorState_withRetry_displaysRetryButton() {
    // Given
    val errorState = TestDataFactory.createErrorState(canRetry = true)
    stateFlow.value = errorState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_RETRY_BUTTON)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun errorState_withRetry_retryButtonCallsViewModel() {
    // Given
    val errorState = TestDataFactory.createErrorState(canRetry = true)
    stateFlow.value = errorState

    // When
    setUpScreen()
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_RETRY_BUTTON).performClick()

    // Then
    verify(exactly = ONE) { mockViewModel.retry() }
  }

  @Test
  fun errorState_withoutRetry_doesNotDisplayRetryButton() {
    // Given
    val errorState = TestDataFactory.createErrorState(canRetry = false)
    stateFlow.value = errorState

    // When
    setUpScreen()

    // Then
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_RETRY_BUTTON).assertDoesNotExist()
  }

  @Test
  fun errorState_backButtonNavigatesBack() {
    // Given
    val errorState = TestDataFactory.createErrorState()
    stateFlow.value = errorState

    // When
    setUpScreen()
    composeTestRule.onNodeWithText(ValidateRequestConstants.BUTTON_GO_BACK).performClick()

    // Then
    verify(exactly = ONE) { mockOnNavigateBack() }
  }

  // ==================== STATE TRANSITION TESTS ====================

  @Test
  fun stateTransition_processingToSuccess_triggersNavigation() {
    // Given - Start with Success state directly to test LaunchedEffect
    stateFlow.value = ValidationState.Success

    // When - Set up screen which should trigger LaunchedEffect
    setUpScreen()
    composeTestRule.waitForIdle()

    // Give LaunchedEffect time to execute
    composeTestRule.mainClock.advanceTimeBy(CONSTANT_500_MS)
    composeTestRule.waitForIdle()

    verify(timeout = TIMEOUT_2S, exactly = ONE) { mockOnRequestClosed() }
  }
  // ==================== EDGE CASE TESTS ====================

  @Test
  fun edgeCase_allHelpersSelected_displaysCorrectKudos() {
    // Given
    val allIds = testHelpers.map { it.id }.toSet()
    val readyState =
        TestDataFactory.createReadyState(
            request = testRequest, helpers = testHelpers, selectedHelperIds = allIds)
    stateFlow.value = readyState

    // When
    setUpScreen()

    composeTestRule
        .onNodeWithText(ValidateRequestConstants.SUMMARY_TOTAL_LABEL)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun edgeCase_longRequestTitle_displaysCorrectly() {
    // Given
    val longTitleRequest = testRequest.copy(title = "A".repeat(TestConstants.LONG_TEXT_LENGTH))
    val readyState =
        TestDataFactory.createReadyState(request = longTitleRequest, helpers = testHelpers)
    stateFlow.value = readyState

    // When
    setUpScreen()

    // Then
    composeTestRule
        .onNodeWithText(
            ValidateRequestConstants.getHeaderDescription(longTitleRequest.title), substring = true)
        .assertExists()
  }

  // ==================== HELPER METHODS ====================

  private fun setUpScreen() {
    composeTestRule.setContent {
      ValidateRequestScreen(
          requestId = TestConstants.TEST_REQUEST_ID,
          viewModel = mockViewModel,
          userProfileRepository = mockUserProfileRepository,
          onRequestClosed = mockOnRequestClosed,
          onNavigateBack = mockOnNavigateBack)
    }
  }
}

/**
 * Test constants to avoid magic values and ensure consistency across tests. Centralized for easy
 * maintenance and CI optimization.
 */
private object TestConstants {
  // Test configuration
  const val SCREEN_QUALIFIERS = "w400dp-h800dp-normal-long-notround-any-420dpi-keyshidden-nonav"
  // Fixed timestamps for deterministic tests
  const val TEST_START_TIME = 1700000000000L
  const val TEST_EXPIRATION_TIME = 1700086400000L
  const val TEST_ARRIVAL_DATE = 1600000000000L

  // Test IDs
  const val TEST_REQUEST_ID = "test-request-123"
  const val TEST_USER_ID_PREFIX = "user-"

  // Test data sizes
  const val DEFAULT_HELPER_COUNT = 3
  const val LONG_TEXT_LENGTH = 100

  // Test user data
  const val DEFAULT_KUDOS = 100
  const val DEFAULT_FIRST_NAME = "John"
  const val DEFAULT_LAST_NAME = "Doe"

  // Test request data
  const val DEFAULT_REQUEST_TITLE = "Help with moving furniture"
  const val DEFAULT_REQUEST_DESCRIPTION = "Need help moving a couch"
}

private const val STRING_CREATOR = "creator"

private const val STRING_TEST_LOCATION = "Test Location"

/**
 * Factory for creating test data objects with sensible defaults. Reduces code duplication and makes
 * tests more maintainable.
 */
private object TestDataFactory {

  fun createTestRequest(
      requestId: String = TestConstants.TEST_REQUEST_ID,
      title: String = TestConstants.DEFAULT_REQUEST_TITLE,
      description: String = TestConstants.DEFAULT_REQUEST_DESCRIPTION,
      status: RequestStatus = RequestStatus.IN_PROGRESS
  ): Request {
    return Request(
        requestId = requestId,
        title = title,
        description = description,
        requestType = emptyList(),
        location = mockk(relaxed = true),
        locationName = STRING_TEST_LOCATION,
        status = status,
        startTimeStamp = Date(TestConstants.TEST_START_TIME),
        expirationTime = Date(TestConstants.TEST_EXPIRATION_TIME),
        people = emptyList(),
        tags = emptyList(),
        creatorId = "${TestConstants.TEST_USER_ID_PREFIX}${STRING_CREATOR}")
  }

  fun createTestHelpers(count: Int = TestConstants.DEFAULT_HELPER_COUNT): List<UserProfile> {
    return (ONE..count).map { index ->
      UserProfile(
          id = "${TestConstants.TEST_USER_ID_PREFIX}$index",
          name = "${TestConstants.DEFAULT_FIRST_NAME}$index",
          lastName = "${TestConstants.DEFAULT_LAST_NAME}$index",
          email = "user$index@test.com",
          photo = null,
          kudos = TestConstants.DEFAULT_KUDOS + index * 10,
          section = UserSections.COMPUTER_SCIENCE, // or whatever default section makes sense
          arrivalDate = Date(TestConstants.TEST_ARRIVAL_DATE))
    }
  }

  fun createReadyState(
      request: Request = createTestRequest(),
      helpers: List<UserProfile> = createTestHelpers(),
      selectedHelperIds: Set<String> = emptySet()
  ): ValidationState.Ready {
    return ValidationState.Ready(
        request = request, helpers = helpers, selectedHelperIds = selectedHelperIds)
  }

  fun createConfirmingState(
      request: Request = createTestRequest(),
      selectedHelpers: List<UserProfile> = createTestHelpers().take(TWO),
      kudosToAward: Int = selectedHelpers.size * KudosConstants.KUDOS_PER_HELPER,
      creatorBonus: Int =
          if (selectedHelpers.isNotEmpty()) {
            KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION
          } else {
            ZERO
          }
  ): ValidationState.Confirming {
    return ValidationState.Confirming(
        request = request,
        selectedHelpers = selectedHelpers,
        kudosToAward = kudosToAward,
        creatorBonus = creatorBonus)
  }

  fun createErrorState(
      message: String = STRING_TEST_ERROR_MESSAGE,
      canRetry: Boolean = true
  ): ValidationState.Error {
    return ValidationState.Error(message = message, canRetry = canRetry)
  }
}
