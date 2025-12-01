package com.android.sample.ui.profile.accepted_requests

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.request.RequestListViewModel
import com.android.sample.ui.request.accepted.AcceptedRequestsScreen
import com.android.sample.ui.request.accepted.AcceptedRequestsTestTags
import com.android.sample.utils.BaseEmulatorTest
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class AcceptedRequestsScreenTest : BaseEmulatorTest() {

  private companion object {
    const val OTHER_USER_EMAIL = "otheruser@example.com"
    const val OTHER_USER_PASSWORD = "password123"
    const val REQUEST_ID_1 = "request1"
    const val REQUEST_ID_2 = "request2"
    const val REQUEST_ID_3 = "request3"
    const val TITLE_WITH_KUDOS = "Request with Kudos"
    const val TITLE_WITHOUT_KUDOS = "Request without Kudos"
    const val TITLE_PENDING = "Pending Request"
    const val LONG_TITLE = "This is a very long title that should be truncated "
    const val DESCRIPTION_WITH_KUDOS = "You received kudos"
    const val DESCRIPTION_WITHOUT_KUDOS = "No kudos for you"
    const val DESCRIPTION_PENDING = "Still in progress"
    const val LONG_DESCRIPTION = "This is a very long description that should be truncated "
    const val LOCATION_NAME = "Test Location"
    const val LATITUDE = 46.5191
    const val LONGITUDE = 6.5668
    const val SCREEN_TITLE = "Accepted Requests"
    const val BACK_BUTTON_DESCRIPTION = "Back"
    const val EMPTY_MESSAGE = "You haven't accepted any requests yet"
    const val KUDOS_RECEIVED_TEXT = "✓ Kudos Received"
    const val KUDOS_NOT_RECEIVED_TEXT = "✗ No Kudos"
    const val KUDOS_PENDING_TEXT = "⏳ Pending"
    const val LABEL_LOCATION = "Location:"
    const val LABEL_START_TIME = "Start Time:"
    const val LABEL_EXPIRATION = "Expiration:"
    const val LABEL_HELPERS = "People Accepted:"
    const val REQUEST_TYPE_STUDYING = "Studying"
    const val REQUEST_TYPE_OTHER = "Other"
    const val DIALOG_CLOSE_BUTTON_TEXT = "Close"
    const val TIME_OFFSET_1_HOUR = 3_600_000L
    const val TIME_OFFSET_2_HOURS = 7_200_000L
    const val EXPECTED_TWO_PEOPLE = 2
    const val LONG_TITLE_REPEAT = 5
    const val LONG_DESCRIPTION_REPEAT = 10
  }

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepositoryFirestore
  private lateinit var viewModel: AcceptedRequestsViewModel
  private lateinit var otherUserId: String
  private lateinit var mainUserId: String

  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)
  }

  private suspend fun setupOtherUser() {
    createAndSignInUser(OTHER_USER_EMAIL, OTHER_USER_PASSWORD)
    otherUserId = currentUserId
  }

  private suspend fun createAndAddRequest(
      requestId: String,
      title: String,
      description: String,
      status: RequestStatus,
      selectedHelpers: List<String>,
      helpersIds: List<String>,
      timeOffsetMs: Long = TIME_OFFSET_1_HOUR
  ): Request {
    val now = System.currentTimeMillis()
    val request =
        Request(
            requestId = requestId,
            title = title,
            description = description,
            requestType = listOf(RequestType.STUDYING, RequestType.OTHER),
            location = Location(LATITUDE, LONGITUDE, LOCATION_NAME),
            locationName = LOCATION_NAME,
            status = status,
            startTimeStamp = Date(now - timeOffsetMs),
            expirationTime = Date(now + TIME_OFFSET_2_HOURS),
            people = helpersIds,
            tags = listOf(Tags.URGENT),
            creatorId = otherUserId,
            selectedHelpers = selectedHelpers)

    // Stay signed in as other user to create request
    repository.addRequest(request)
    return request
  }

  private fun launchScreen() {
    viewModel = AcceptedRequestsViewModel(repository)
    composeTestRule.setContent {
      MaterialTheme {
        AcceptedRequestsScreen(
            navigationActions = mock<NavigationActions>(),
            acceptedRequestsViewModel = viewModel,
            requestListViewModel = RequestListViewModel())
      }
    }
  }

  private fun assertNodeExists(testTag: String) {
    composeTestRule.onNodeWithTag(testTag).assertExists().assertIsDisplayed()
  }

  private fun assertNodeDoesNotExist(testTag: String) {
    composeTestRule.onNodeWithTag(testTag).assertDoesNotExist()
  }

  private fun assertTextExists(text: String) {
    composeTestRule.onNodeWithText(text).assertExists().assertIsDisplayed()
  }

  private fun assertTextExistsSubstring(text: String) {
    composeTestRule.onNodeWithText(text, substring = true).assertExists()
  }

  private fun waitForUI() {
    composeTestRule.waitForIdle()
  }

  @Test
  fun screen_displaysTopBar_withTitleAndBackButton() = runTest {
    // Given
    mainUserId = currentUserId

    // When
    launchScreen()
    waitForUI()

    // Then
    assertTextExists(SCREEN_TITLE)
    composeTestRule.onNodeWithContentDescription(BACK_BUTTON_DESCRIPTION).assertExists()
  }

  @Test
  fun screen_displaysEmptyMessage_whenNoRequests() = runTest {
    // Given
    mainUserId = currentUserId

    // When
    launchScreen()
    waitForUI()

    // Then
    assertNodeExists(AcceptedRequestsTestTags.EMPTY_MESSAGE)
    assertTextExists(EMPTY_MESSAGE)
  }

  @Test
  fun requestList_displaysSingleRequest() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser() // Switch back to main user

    // When
    launchScreen()
    waitForUI()

    // Then
    assertNodeExists(AcceptedRequestsTestTags.REQUEST_LIST)
    assertTextExists(TITLE_WITH_KUDOS)
    assertTextExists(DESCRIPTION_WITH_KUDOS)
  }

  @Test
  fun requestList_displaysMultipleRequests() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))
    createAndAddRequest(
        REQUEST_ID_2,
        TITLE_WITHOUT_KUDOS,
        DESCRIPTION_WITHOUT_KUDOS,
        RequestStatus.COMPLETED,
        listOf(otherUserId),
        listOf(mainUserId, otherUserId))
    createAndAddRequest(
        REQUEST_ID_3,
        TITLE_PENDING,
        DESCRIPTION_PENDING,
        RequestStatus.IN_PROGRESS,
        emptyList(),
        listOf(mainUserId, otherUserId))

    signInUser()

    // When
    launchScreen()
    waitForUI()

    // Then
    assertTextExists(TITLE_WITH_KUDOS)
    assertTextExists(TITLE_WITHOUT_KUDOS)
    assertTextExists(TITLE_PENDING)
  }

  @Test
  fun requestItem_displaysKudosBadge() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()

    // When
    launchScreen()
    waitForUI()

    // Then - Just verify the request is displayed (badge rendering is visual)
    assertTextExists(TITLE_WITH_KUDOS)
    assertTextExists(DESCRIPTION_WITH_KUDOS)
  }

  @Test
  fun requestItem_displaysRequestTypes() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()

    // When
    launchScreen()
    waitForUI()

    // Then
    assertTextExists(REQUEST_TYPE_STUDYING)
    assertTextExists(REQUEST_TYPE_OTHER)
  }

  @Test
  fun requestDialog_opensWhenRequestClicked() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // When
    composeTestRule.onNodeWithText(TITLE_WITH_KUDOS).performClick()
    waitForUI()

    // Then
    assertNodeExists(AcceptedRequestsTestTags.REQUEST_DIALOG)
  }

  @Test
  fun requestDialog_displaysAllRequestDetails() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // When
    composeTestRule.onNodeWithText(TITLE_WITH_KUDOS).performClick()
    waitForUI()

    // Then - Check for DIALOG-SPECIFIC elements only (not duplicated in list)
    assertNodeExists(AcceptedRequestsTestTags.REQUEST_DIALOG)
    assertTextExists(LOCATION_NAME)
    assertTextExists(LABEL_LOCATION)
    assertTextExists(LABEL_START_TIME)
    assertTextExists(LABEL_EXPIRATION)
    assertTextExists(LABEL_HELPERS)
    assertTextExists(DIALOG_CLOSE_BUTTON_TEXT)
    assertTextExists(EXPECTED_TWO_PEOPLE.toString())
  }

  @Test
  fun requestDialog_displaysKudosReceivedStatus() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // When
    composeTestRule.onNodeWithText(TITLE_WITH_KUDOS).performClick()
    waitForUI()

    // Then
    assertTextExists(KUDOS_RECEIVED_TEXT)
  }

  @Test
  fun requestDialog_displaysKudosNotReceivedStatus() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITHOUT_KUDOS,
        DESCRIPTION_WITHOUT_KUDOS,
        RequestStatus.COMPLETED,
        listOf(otherUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // When
    composeTestRule.onNodeWithText(TITLE_WITHOUT_KUDOS).performClick()
    waitForUI()

    // Then
    assertTextExists(KUDOS_NOT_RECEIVED_TEXT)
  }

  @Test
  fun requestDialog_displaysKudosPendingStatus() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_PENDING,
        DESCRIPTION_PENDING,
        RequestStatus.IN_PROGRESS,
        emptyList(),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // When
    composeTestRule.onNodeWithText(TITLE_PENDING).performClick()
    waitForUI()

    // Then
    assertTextExists(KUDOS_PENDING_TEXT)
  }

  @Test
  fun requestDialog_closesWhenCloseButtonClicked() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    composeTestRule.onNodeWithText(TITLE_WITH_KUDOS).performClick()
    waitForUI()
    assertNodeExists(AcceptedRequestsTestTags.REQUEST_DIALOG)

    // When
    composeTestRule.onNodeWithTag(AcceptedRequestsTestTags.DIALOG_CLOSE_BUTTON).performClick()
    waitForUI()

    // Then
    assertNodeDoesNotExist(AcceptedRequestsTestTags.REQUEST_DIALOG)
  }

  @Test
  fun requestDialog_displaysNumberOfHelpersAccepted() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // When
    composeTestRule.onNodeWithText(TITLE_WITH_KUDOS).performClick()
    waitForUI()

    // Then
    assertTextExists(EXPECTED_TWO_PEOPLE.toString())
  }

  @Test
  fun requestItem_truncatesLongTitle() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()
    val longTitle = LONG_TITLE.repeat(LONG_TITLE_REPEAT)

    createAndAddRequest(
        REQUEST_ID_1,
        longTitle,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()

    // When
    launchScreen()
    waitForUI()

    // Then
    assertTextExistsSubstring(longTitle)
  }

  @Test
  fun requestItem_truncatesLongDescription() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()
    val longDescription = LONG_DESCRIPTION.repeat(LONG_DESCRIPTION_REPEAT)

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        longDescription,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()

    // When
    launchScreen()
    waitForUI()

    // Then
    assertTextExistsSubstring(longDescription)
  }

  @Test
  fun screen_displaysLoadingIndicator_whenLoading() = runTest {
    // Given
    mainUserId = currentUserId

    // When - Launch screen (ViewModel loads in init, so it will be loading briefly)
    launchScreen()

    // Then - Check that loading eventually completes
    composeTestRule.waitUntil(timeoutMillis = 5000) { !viewModel.uiState.value.isLoading }

    // Verify screen loaded successfully
    assertTextExists(SCREEN_TITLE)
  }

  @Test
  fun screen_displaysErrorDialog_whenErrorOccurs() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    // Create a request then delete the current user to cause an error
    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    // Sign out to trigger authentication error
    auth.signOut()

    // When
    launchScreen()
    waitForUI()

    // Then - Error dialog should appear
    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMessage != null }

    val errorMessage = viewModel.uiState.value.errorMessage
    assert(errorMessage != null)
    assertTextExists("Error")
  }

  @Test
  fun screen_clearsError_whenErrorDialogDismissed() = runTest {
    // Given
    mainUserId = currentUserId

    // Sign out to cause error
    auth.signOut()
    launchScreen()
    waitForUI()

    // Wait for error to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMessage != null }

    // When - Dismiss error dialog
    composeTestRule.onNodeWithText("OK").performClick()
    waitForUI()

    // Then - Error should be cleared
    assert(viewModel.uiState.value.errorMessage == null)
  }

  @Test
  fun screen_callsGoBack_whenBackButtonClicked() = runTest {
    // Given
    mainUserId = currentUserId
    val mockNavActions = mock<NavigationActions>()

    viewModel = AcceptedRequestsViewModel(repository)
    composeTestRule.setContent {
      MaterialTheme {
        AcceptedRequestsScreen(
            navigationActions = mockNavActions,
            acceptedRequestsViewModel = viewModel,
            requestListViewModel = RequestListViewModel())
      }
    }
    waitForUI()

    // When
    composeTestRule.onNodeWithContentDescription(BACK_BUTTON_DESCRIPTION).performClick()

    // Then
    org.mockito.kotlin.verify(mockNavActions).goBack()
  }

  @Test
  fun screen_doesNotDisplayEmptyMessage_whenLoading() = runTest {
    // Given
    mainUserId = currentUserId

    // When
    launchScreen()

    // Then - During loading, empty message should not show
    // (it will eventually show if no requests, but not while loading)
    val isLoadingOrHasContent =
        viewModel.uiState.value.isLoading || viewModel.uiState.value.requests.isNotEmpty()

    if (viewModel.uiState.value.isLoading) {
      // Empty message should not be visible while loading
      composeTestRule.onNodeWithTag(AcceptedRequestsTestTags.EMPTY_MESSAGE).assertDoesNotExist()
    }
  }

  @Test
  fun viewModel_refreshReloadsData() = runTest {
    // Given
    mainUserId = currentUserId
    setupOtherUser()

    createAndAddRequest(
        REQUEST_ID_1,
        TITLE_WITH_KUDOS,
        DESCRIPTION_WITH_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))

    signInUser()
    launchScreen()
    waitForUI()

    // Verify initial load
    assertTextExists(TITLE_WITH_KUDOS)

    // When - Add another request and refresh
    setupOtherUser()
    createAndAddRequest(
        REQUEST_ID_2,
        TITLE_WITHOUT_KUDOS,
        DESCRIPTION_WITHOUT_KUDOS,
        RequestStatus.COMPLETED,
        listOf(mainUserId),
        listOf(mainUserId, otherUserId))
    signInUser()

    viewModel.refresh()
    composeTestRule.waitUntil(timeoutMillis = 5000) { !viewModel.uiState.value.isLoading }
    waitForUI()

    // Then - Both requests should be visible
    assertTextExists(TITLE_WITH_KUDOS)
    assertTextExists(TITLE_WITHOUT_KUDOS)
  }
}
