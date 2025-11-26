package com.android.sample.ui.request_validation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.profile.UserSections
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test suite for ValidateRequestScreen.
 *
 * Tests cover all UI states, user interactions, and edge cases. Uses fake implementations and
 * direct state/callback passing for reliable testing.
 */
@RunWith(AndroidJUnit4::class)
class ValidateRequestScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Fakes
  private lateinit var fakeUserProfileRepository: FakeUserProfileRepository

  // Callback tracking
  private lateinit var callbackTracker: CallbackTracker

  // Test data
  private lateinit var testRequest: Request
  private lateinit var testHelpers: List<UserProfile>

  @Before
  fun setUp() {
    // Setup callback tracker
    callbackTracker = CallbackTracker()

    // Setup test data
    testRequest = TestDataFactory.createTestRequest()
    testHelpers = TestDataFactory.createTestHelpers()

    // Setup fakes
    fakeUserProfileRepository = FakeUserProfileRepository(testHelpers)
  }

  @After
  fun tearDown() {
    // Clear caches to ensure test isolation
    fakeUserProfileRepository.clear()
  }

  // ==================== LOADING STATE TESTS ====================

  @Test
  fun loadingState_displaysLoadingIndicator() {
    // Given & When
    setUpScreen(state = ValidationState.Loading)

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_LOADING_INDICATOR)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun loadingState_backButtonNavigatesBack() {
    // Given
    setUpScreen(state = ValidationState.Loading)

    // When
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_BACK_BUTTON).performClick()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.navigateBackCount)
  }

  // ==================== READY STATE WITH HELPERS TESTS ====================

  @Test
  fun readyState_displaysHeaderAndDescription() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)

    // When
    setUpScreen(state = readyState)

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

    // When
    setUpScreen(state = readyState)

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

    // When
    setUpScreen(state = readyState)

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_VALIDATE_BUTTON)
        .assertExists()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun readyState_helperCardClick_callsToggleHelper() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    setUpScreen(state = readyState)

    // When
    val firstHelper = testHelpers.first()
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.getHelperCardTag(firstHelper.id))
        .performClick()

    // Then
    assertTrue(callbackTracker.toggledHelperIds.contains(firstHelper.id))
  }

  @Test
  fun readyState_validateButtonClick_callsShowConfirmation() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    setUpScreen(state = readyState)

    // When
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_VALIDATE_BUTTON).performClick()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.showConfirmationCount)
  }

  @Test
  fun readyState_withSelectedHelpers_displaysKudosSummary() {
    // Given
    val selectedIds = setOf(testHelpers[TestIndices.FIRST].id, testHelpers[TestIndices.SECOND].id)
    val readyState =
        TestDataFactory.createReadyState(
            request = testRequest, helpers = testHelpers, selectedHelperIds = selectedIds)

    // When
    setUpScreen(state = readyState)

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.SUMMARY_TOTAL_LABEL)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun readyState_helpersList_isScrollable() {
    // Given
    val manyHelpers = TestDataFactory.createTestHelpers(count = TestSizes.LARGE_LIST_SIZE)
    fakeUserProfileRepository.setHelpers(manyHelpers)
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = manyHelpers)

    // When
    setUpScreen(state = readyState)

    // Then - Scroll to bottom
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_HELPERS_LIST)
        .performScrollToIndex(manyHelpers.size - TestIndices.OFFSET_LAST)

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

    // When
    setUpScreen(state = readyState)

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

    // When
    setUpScreen(state = readyState)

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
        TestDataFactory.createConfirmingState(
            selectedHelpers = testHelpers.take(TestSizes.SELECTED_HELPERS_COUNT))

    // When
    setUpScreen(state = confirmingState)

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_CONFIRMATION_DIALOG)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun confirmingState_withHelpers_displaysSelectedHelpersList() {
    // Given
    val selectedHelpers = testHelpers.take(TestSizes.SELECTED_HELPERS_COUNT)
    val confirmingState = TestDataFactory.createConfirmingState(selectedHelpers = selectedHelpers)

    // When
    setUpScreen(state = confirmingState)

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

    // When
    setUpScreen(state = confirmingState)

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.CONFIRM_NO_HELPERS)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun confirmingState_confirmButtonClick_callsConfirmAndClose() {
    // Given
    val confirmingState = TestDataFactory.createConfirmingState()
    setUpScreen(state = confirmingState)

    // When
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_CONFIRM_BUTTON).performClick()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.confirmAndCloseCount)
  }

  @Test
  fun confirmingState_cancelButtonClick_callsCancelConfirmation() {
    // Given
    val confirmingState = TestDataFactory.createConfirmingState()
    setUpScreen(state = confirmingState)

    // When
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_CANCEL_BUTTON).performClick()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.cancelConfirmationCount)
  }

  // ==================== PROCESSING STATE TESTS ====================

  @Test
  fun processingState_displaysProcessingIndicator() {
    // Given & When
    setUpScreen(state = ValidationState.Processing)

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_PROCESSING_INDICATOR)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun processingState_backButtonIsNotVisible() {
    // Given & When
    setUpScreen(state = ValidationState.Processing)

    // Then
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_BACK_BUTTON).assertDoesNotExist()
  }

  // ==================== SUCCESS STATE TESTS ====================

  @Test
  fun successState_displaysSuccessMessages() {
    // Given & When
    setUpScreen(state = ValidationState.Success)

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.SUCCESS_TITLE)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun successState_triggersNavigationCallback() {
    // Given & When
    setUpScreen(state = ValidationState.Success)
    composeTestRule.waitForIdle()

    // Allow LaunchedEffect to execute
    composeTestRule.mainClock.advanceTimeBy(TestTimeouts.LAUNCHED_EFFECT_DELAY_MS)
    composeTestRule.waitForIdle()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.requestClosedCount)
  }

  // ==================== ERROR STATE TESTS ====================

  @Test
  fun errorState_displaysErrorMessage() {
    // Given
    val errorMessage = TestMessages.ERROR_MESSAGE
    val errorState = TestDataFactory.createErrorState(message = errorMessage)

    // When
    setUpScreen(state = errorState)

    // Then
    composeTestRule.onNodeWithText(errorMessage).assertExists().assertIsDisplayed()
  }

  @Test
  fun errorState_withRetry_displaysRetryButton() {
    // Given
    val errorState = TestDataFactory.createErrorState(canRetry = true)

    // When
    setUpScreen(state = errorState)

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.TAG_RETRY_BUTTON)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun errorState_withRetry_retryButtonCallsRetry() {
    // Given
    val errorState = TestDataFactory.createErrorState(canRetry = true)
    setUpScreen(state = errorState)

    // When
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_RETRY_BUTTON).performClick()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.retryCount)
  }

  @Test
  fun errorState_withoutRetry_doesNotDisplayRetryButton() {
    // Given
    val errorState = TestDataFactory.createErrorState(canRetry = false)

    // When
    setUpScreen(state = errorState)

    // Then
    composeTestRule.onNodeWithTag(ValidateRequestConstants.TAG_RETRY_BUTTON).assertDoesNotExist()
  }

  @Test
  fun errorState_backButtonNavigatesBack() {
    // Given
    val errorState = TestDataFactory.createErrorState()
    setUpScreen(state = errorState)

    // When
    composeTestRule.onNodeWithText(ValidateRequestConstants.BUTTON_GO_BACK).performClick()

    // Then
    assertEquals(TestAssertions.SINGLE_INVOCATION, callbackTracker.navigateBackCount)
  }

  // ==================== EDGE CASE TESTS ====================

  @Test
  fun edgeCase_allHelpersSelected_displaysCorrectKudos() {
    // Given
    val allIds = testHelpers.map { it.id }.toSet()
    val readyState =
        TestDataFactory.createReadyState(
            request = testRequest, helpers = testHelpers, selectedHelperIds = allIds)

    // When
    setUpScreen(state = readyState)

    // Then
    composeTestRule
        .onNodeWithText(ValidateRequestConstants.SUMMARY_TOTAL_LABEL)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun edgeCase_longRequestTitle_displaysCorrectly() {
    // Given
    val longTitle = TestDataFactory.generateLongString(TestSizes.LONG_TEXT_LENGTH)
    val longTitleRequest = testRequest.copy(title = longTitle)
    val readyState =
        TestDataFactory.createReadyState(request = longTitleRequest, helpers = testHelpers)

    // When
    setUpScreen(state = readyState)

    // Then
    composeTestRule
        .onNodeWithText(
            ValidateRequestConstants.getHeaderDescription(longTitleRequest.title), substring = true)
        .assertExists()
  }

  @Test
  fun edgeCase_singleHelper_displaysCorrectly() {
    // Given
    val singleHelper = listOf(testHelpers.first())
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = singleHelper)

    // When
    setUpScreen(state = readyState)

    // Then
    composeTestRule
        .onNodeWithTag(ValidateRequestConstants.getHelperCardTag(singleHelper.first().id))
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun edgeCase_multipleHelperClicks_tracksAllClicks() {
    // Given
    val readyState = TestDataFactory.createReadyState(request = testRequest, helpers = testHelpers)
    setUpScreen(state = readyState)

    // When - Click all helpers
    testHelpers.forEach { helper ->
      composeTestRule
          .onNodeWithTag(ValidateRequestConstants.getHelperCardTag(helper.id))
          .performClick()
    }

    // Then
    assertEquals(testHelpers.size, callbackTracker.toggledHelperIds.size)
    testHelpers.forEach { helper ->
      assertTrue(callbackTracker.toggledHelperIds.contains(helper.id))
    }
  }

  // ==================== HELPER METHODS ====================

  private fun setUpScreen(state: ValidationState) {
    composeTestRule.setContent {
      ValidateRequestScreen(
          state = state,
          userProfileRepository = fakeUserProfileRepository,
          callbacks =
              ValidateRequestCallbacks(
                  onToggleHelper = { callbackTracker.onToggleHelper(it) },
                  onShowConfirmation = { callbackTracker.onShowConfirmation() },
                  onCancelConfirmation = { callbackTracker.onCancelConfirmation() },
                  onConfirmAndClose = { callbackTracker.onConfirmAndClose() },
                  onRetry = { callbackTracker.onRetry() },
                  onRequestClosed = { callbackTracker.onRequestClosed() },
                  onNavigateBack = { callbackTracker.onNavigateBack() }))
    }
  }
}

// ==================== CALLBACK TRACKER ====================

/**
 * Tracks all callback invocations for test verification. Provides a clean way to verify UI
 * interactions without mocks.
 */
private class CallbackTracker {
  // Toggle helper tracking
  val toggledHelperIds = mutableListOf<String>()

  // Invocation counts
  var showConfirmationCount = 0
    private set

  var cancelConfirmationCount = 0
    private set

  var confirmAndCloseCount = 0
    private set

  var retryCount = 0
    private set

  var requestClosedCount = 0
    private set

  var navigateBackCount = 0
    private set

  fun onToggleHelper(userId: String) {
    toggledHelperIds.add(userId)
  }

  fun onShowConfirmation() {
    showConfirmationCount++
  }

  fun onCancelConfirmation() {
    cancelConfirmationCount++
  }

  fun onConfirmAndClose() {
    confirmAndCloseCount++
  }

  fun onRetry() {
    retryCount++
  }

  fun onRequestClosed() {
    requestClosedCount++
  }

  fun onNavigateBack() {
    navigateBackCount++
  }

  fun reset() {
    toggledHelperIds.clear()
    showConfirmationCount = 0
    cancelConfirmationCount = 0
    confirmAndCloseCount = 0
    retryCount = 0
    requestClosedCount = 0
    navigateBackCount = 0
  }
}

// ==================== TEST CONSTANTS ====================

/** Test identifiers used across tests. */
private object TestIds {
  const val REQUEST_ID = "test-request-123"
  const val USER_ID_PREFIX = "user-"
  const val CREATOR_SUFFIX = "creator"
}

/** Test indices for accessing list elements. */
private object TestIndices {
  const val FIRST = 0
  const val SECOND = 1
  const val OFFSET_LAST = 1
}

/** Test size constants. */
private object TestSizes {
  const val DEFAULT_HELPER_COUNT = 3
  const val SELECTED_HELPERS_COUNT = 2
  const val LARGE_LIST_SIZE = 20
  const val LONG_TEXT_LENGTH = 100
}

/** Test timeout values in milliseconds. */
private object TestTimeouts {
  const val LAUNCHED_EFFECT_DELAY_MS = 500L
}

/** Test assertion constants. */
private object TestAssertions {
  const val SINGLE_INVOCATION = 1
  const val NO_INVOCATIONS = 0
}

/** Test message strings. */
private object TestMessages {
  const val ERROR_MESSAGE = "Test error message"
}

/** Test timestamps for deterministic test data. */
private object TestTimestamps {
  const val START_TIME = 1700000000000L
  const val EXPIRATION_TIME = 1700086400000L
  const val ARRIVAL_DATE = 1600000000000L
}

/** Default test values for user profiles. */
private object TestUserDefaults {
  const val KUDOS = 100
  const val KUDOS_INCREMENT = 10
  const val FIRST_NAME = "John"
  const val LAST_NAME = "Doe"
  const val EMAIL_DOMAIN = "@test.com"
}

/** Default test values for requests. */
private object TestRequestDefaults {
  const val TITLE = "Help with moving furniture"
  const val DESCRIPTION = "Need help moving a couch"
  const val LOCATION_NAME = "Test Location"
}

// ==================== FAKE IMPLEMENTATIONS ====================

/**
 * Fake UserProfileRepository for testing. Returns predefined helper profiles without Firebase
 * dependencies.
 */
private class FakeUserProfileRepository(private var helpers: List<UserProfile>) :
    UserProfileRepository {

  private val profileCache = mutableMapOf<String, UserProfile>()
  private var uidCounter = 0

  init {
    helpers.forEach { profileCache[it.id] = it }
  }

  fun setHelpers(newHelpers: List<UserProfile>) {
    helpers = newHelpers
    profileCache.clear()
    helpers.forEach { profileCache[it.id] = it }
  }

  fun clear() {
    profileCache.clear()
  }

  override fun getNewUid(): String {
    return "fake-uid-${uidCounter++}"
  }

  override fun getCurrentUserId(): String {
    return "fake-current-user"
  }

  override suspend fun getAllUserProfiles(): List<UserProfile> {
    return profileCache.values.toList()
  }

  override suspend fun getUserProfile(userId: String): UserProfile {
    return profileCache[userId] ?: throw NoSuchElementException("Profile not found: $userId")
  }

  override suspend fun addUserProfile(userProfile: UserProfile) {
    profileCache[userProfile.id] = userProfile
  }

  override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {
    if (!profileCache.containsKey(userId)) {
      throw NoSuchElementException("Profile not found: $userId")
    }
    profileCache[userId] = updatedProfile
  }

  override suspend fun deleteUserProfile(userId: String) {
    profileCache.remove(userId)
  }

  override suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile> {
    return helpers
        .filter {
          it.name.contains(query, ignoreCase = true) ||
              it.lastName.contains(query, ignoreCase = true)
        }
        .take(limit)
  }

  override suspend fun awardKudos(userId: String, amount: Int) {
    require(amount > 0) { "Amount must be positive" }
    val profile = profileCache[userId] ?: throw NoSuchElementException("Profile not found: $userId")
    profileCache[userId] = profile.copy(kudos = profile.kudos + amount)
  }

  override suspend fun awardKudosBatch(awards: Map<String, Int>) {
    awards.forEach { (userId, kudos) -> awardKudos(userId, kudos) }
  }
}

// ==================== TEST DATA FACTORY ====================

/**
 * Factory for creating test data objects with sensible defaults. Centralized for maintainability
 * and consistency.
 */
private object TestDataFactory {

  fun createTestRequest(
      requestId: String = TestIds.REQUEST_ID,
      title: String = TestRequestDefaults.TITLE,
      description: String = TestRequestDefaults.DESCRIPTION,
      status: RequestStatus = RequestStatus.IN_PROGRESS
  ): Request {
    return Request(
        requestId = requestId,
        title = title,
        description = description,
        requestType = emptyList(),
        location =
            Location(latitude = 0.0, longitude = 0.0, name = TestRequestDefaults.LOCATION_NAME),
        locationName = TestRequestDefaults.LOCATION_NAME,
        status = status,
        startTimeStamp = Date(TestTimestamps.START_TIME),
        expirationTime = Date(TestTimestamps.EXPIRATION_TIME),
        people = emptyList(),
        tags = emptyList(),
        creatorId = "${TestIds.USER_ID_PREFIX}${TestIds.CREATOR_SUFFIX}")
  }

  fun createTestHelpers(count: Int = TestSizes.DEFAULT_HELPER_COUNT): List<UserProfile> {
    return (1..count).map { index -> createTestHelper(index) }
  }

  fun createTestHelper(index: Int): UserProfile {
    return UserProfile(
        id = "${TestIds.USER_ID_PREFIX}$index",
        name = "${TestUserDefaults.FIRST_NAME}$index",
        lastName = "${TestUserDefaults.LAST_NAME}$index",
        email = "user$index${TestUserDefaults.EMAIL_DOMAIN}",
        photo = null,
        kudos = TestUserDefaults.KUDOS + index * TestUserDefaults.KUDOS_INCREMENT,
        section = UserSections.COMPUTER_SCIENCE,
        arrivalDate = Date(TestTimestamps.ARRIVAL_DATE))
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
      selectedHelpers: List<UserProfile> =
          createTestHelpers().take(TestSizes.SELECTED_HELPERS_COUNT),
      kudosToAward: Int = selectedHelpers.size * KudosConstants.KUDOS_PER_HELPER
  ): ValidationState.Confirming {
    return ValidationState.Confirming(
        request = request, selectedHelpers = selectedHelpers, kudosToAward = kudosToAward)
  }

  fun createErrorState(
      message: String = TestMessages.ERROR_MESSAGE,
      canRetry: Boolean = true
  ): ValidationState.Error {
    return ValidationState.Error(message = message, canRetry = canRetry)
  }

  fun generateLongString(length: Int): String {
    return "A".repeat(length)
  }
}
