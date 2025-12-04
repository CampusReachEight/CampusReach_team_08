package com.android.sample.ui.acceptRequest

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestCache
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.overview.AcceptRequestScreen
import com.android.sample.ui.overview.AcceptRequestScreenTestTags
import com.android.sample.ui.overview.AcceptRequestViewModel
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FirebaseEmulator
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class AcceptRequestScreenTests : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepositoryFirestore
  private lateinit var previousUserId: String
  private val uiWaitTimeout = 5_000L

  private lateinit var request1: Request
  private lateinit var request2: Request
  private lateinit var request3: Request

  companion object {
    const val request1_id = "request1"
    const val request2_id = "request2"
    const val request3_id = "request3"
    const val request4_id = "request4"
    const val request5_id = "request5"
    const val request6_id = "request6"
    const val request7_id = "request7"
    const val request8_id = "request8"

    // Error messages
    const val ERROR_VALIDATE_CALLBACK_NOT_TRIGGERED = "Validate button callback was not triggered"
    const val ERROR_REQUEST_ID_MISMATCH = "Expected request ID %s but got %s"
  }

  private fun signIn(email: String = DEFAULT_USER_EMAIL, password: String = DEFAULT_USER_PASSWORD) {
    previousUserId = currentUserId

    runTest { signInUser(email, password) }
  }

  @Before
  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)
    runTest {
      FirebaseEmulator.clearAuthEmulator()
      // for having the previous User
      signIn(SECOND_USER_EMAIL)

      // actual user
      signIn(DEFAULT_USER_EMAIL)

      val calendar = Calendar.getInstance()
      calendar.set(2024, Calendar.MARCH, 15, 14, 30, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      val date = Date(calendar.timeInMillis)
      val datePlusOneHour = Date(calendar.timeInMillis + 3_600_000)

      request1 =
          Request(
              request1_id,
              "Here is a good title",
              "In here we will do a lot of things, like beeing good persons",
              listOf(RequestType.STUDYING, RequestType.STUDY_GROUP, RequestType.SPORT),
              Location(46.5191, 6.5668, "EPFL"),
              "EPFL",
              RequestStatus.OPEN,
              date,
              datePlusOneHour,
              emptyList(),
              listOf(
                  Tags.URGENT,
                  Tags.GROUP_WORK,
                  Tags.SOLO_WORK,
                  Tags.EASY,
                  Tags.INDOOR,
                  Tags.OUTDOOR),
              currentUserId)

      repository.addRequest(request1)

      request3 =
          Request(
              "request3",
              "Another one",
              "Very good decription",
              emptyList(),
              Location(26.5191, 6.5668, "IDK place"),
              "EPFL",
              RequestStatus.ARCHIVED,
              Date(),
              Date(System.currentTimeMillis() + 3_600_000),
              listOf(previousUserId),
              emptyList(),
              currentUserId)
      repository.addRequest(request3)

      // will try to accept his own request -> fail
      signIn(SECOND_USER_EMAIL)
      request2 =
          Request(
              "request2",
              "Another one",
              "Very good decription",
              emptyList(),
              Location(26.5191, 6.5668, "IDK place"),
              "EPFL",
              RequestStatus.ARCHIVED,
              Date(),
              Date(System.currentTimeMillis() + 3_600_000),
              listOf(previousUserId),
              emptyList(),
              currentUserId)
      repository.addRequest(request2)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun fakeRequestShouldFail() {
    composeTestRule.setContent { AcceptRequestScreen("fakeID") }
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_COLUMN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.NO_REQUEST)
        .assertIsDisplayed()
        .assertTextContains("An error occurred.", substring = true, ignoreCase = true)
  }

  @Test
  fun screenComponentsAreDisplayed() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Main column
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_COLUMN).assertIsDisplayed()

    // Top bar with title
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR).assertIsDisplayed()

    // Back button
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK).assertIsDisplayed()

    // Details card
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_DETAILS_CARD)
        .assertIsDisplayed()

    // Action button visible
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON).assertIsDisplayed()

    // As non-owner, Volunteers section header should not exist
    composeTestRule
        .onAllNodesWithTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_HEADER)
        .assertCountEquals(0)
  }

  @Ignore("Flaky test, needs investigation")
  @Test
  fun detailsCardDisplaysAllInformation() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify details card exists
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_DETAILS_CARD)
        .assertIsDisplayed()

    // Description
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextContains(
            "In here we will do a lot of things, like beeing good persons",
            substring = true,
            ignoreCase = true)

    // Tags
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TAG)
        .assertIsDisplayed()
        .assertTextContains("Urgent", substring = true, ignoreCase = true)

    // Request Type
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TYPE)
        .assertIsDisplayed()
        .assertTextContains("Studying", substring = true, ignoreCase = true)

    // Status
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_STATUS)
        .assertIsDisplayed()
        .assertTextContains("Completed", substring = true, ignoreCase = true)

    // Location
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_LOCATION_NAME)
        .assertIsDisplayed()
        .assertTextContains("EPFL", substring = true, ignoreCase = true)

    // Start time
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_START_TIME)
        .assertIsDisplayed()
        .assertTextContains("15/03/2024 14:30", substring = true, ignoreCase = true)

    // Expiration time
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_EXPIRATION_TIME)
        .assertIsDisplayed()
        .assertTextContains("15/03/2024 15:30", substring = true, ignoreCase = true)
  }

  @Test
  fun topBarDisplaysCorrectTitle() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Here is a good title", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR)
        .assertIsDisplayed()
        .assertTextContains("Here is a good title", substring = true, ignoreCase = true)
  }

  @Test
  fun acceptButtonDisplaysCorrectText() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertIsDisplayed()
        .assertTextContains("Accept Request", substring = true, ignoreCase = true)
  }

  @Test
  fun acceptAndCancelRequestFlow() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Initially should show "Accept Request"
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept Request", substring = true, ignoreCase = true)
        .performClick()

    // Wait for button text to change to "Cancel Acceptance"
    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Cancel", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel Acceptance", substring = true, ignoreCase = true)
        .performClick()

    // Wait for button text to change back to "Accept Request"
    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Accept Request", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept Request", substring = true, ignoreCase = true)
  }

  @Test
  fun haveAlreadyAcceptedInPast() {
    composeTestRule.setContent { AcceptRequestScreen("request3") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Cancel", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel Acceptance", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Accept Request", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept Request", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Cancel", ignoreCase = true)
          }
    }
  }

  @Test
  fun cantAcceptOwnRequest_showsEditButton() {
    // request2 is owned by the currently signed-in user (SECOND_USER_EMAIL)
    composeTestRule.setContent { AcceptRequestScreen("request2") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Owner sees "Edit Request" instead of Accept
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Edit Request", substring = true, ignoreCase = true)
        .performClick()

    // onEditClick is a no-op in this test; label remains "Edit Request"
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Edit Request", substring = true, ignoreCase = true)
  }

  @Test
  fun cantCancelRequestIfNotLoggedIn() {
    composeTestRule.setContent { AcceptRequestScreen("request3") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            text.contains("Cancel", ignoreCase = true)
          }
    }

    FirebaseEmulator.signOut()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel Acceptance", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Button should still say "Cancel Acceptance" because the action failed
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel Acceptance", substring = true, ignoreCase = true)
  }

  @Test
  fun backButtonNavigatesBack() {
    var backButtonClicked = false
    composeTestRule.setContent {
      AcceptRequestScreen(request1_id, onGoBack = { backButtonClicked = true })
    }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK).performClick()

    assert(backButtonClicked) { "Back button callback was not triggered" }
  }

  @Test
  fun buttonShowsLoadingState() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click accept button
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON).performClick()

    // Button should exist (may show loading indicator briefly)
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON).assertExists()
  }

  @Test
  fun emptyTagsDisplayCorrectly() {
    val requestWithNoTags =
        Request(
            "request4",
            "No tags request",
            "Description without tags",
            listOf(RequestType.OTHER),
            Location(46.5191, 6.5668, "EPFL"),
            "EPFL",
            RequestStatus.OPEN,
            Date(),
            Date(System.currentTimeMillis() + 3_600_000),
            emptyList(),
            emptyList(), // No tags
            currentUserId)

    runTest { repository.addRequest(requestWithNoTags) }

    composeTestRule.setContent { AcceptRequestScreen("request4") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TAG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Should still display the Tags row, but with empty content
    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TAG).assertIsDisplayed()
  }

  @Test
  fun multipleRequestTypesDisplayCorrectly() {
    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TYPE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Should display all three request types
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TYPE)
        .assertIsDisplayed()
        .assertTextContains("Studying", substring = true, ignoreCase = true)
  }

  @Test
  fun archivedStatusDisplaysCorrectly() {
    composeTestRule.setContent { AcceptRequestScreen("request3") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_STATUS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_STATUS)
        .assertIsDisplayed()
        .assertTextContains("Archived", substring = true, ignoreCase = true)
  }

  // New integration test: Volunteers visible and expandable only for owner
  @Test
  fun volunteersSection_visibleForOwner_and_expands() {
    // Sign in as the owner of request1
    runTest { signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD) }

    composeTestRule.setContent { AcceptRequestScreen(request1_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_HEADER)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Header visible for owner
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_HEADER)
        .assertExists()
        .performClick()

    // request1 has empty people list -> should show "No volunteers yet"
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_CONTAINER)
        .assertExists()
  }

  // Tests for getInitials function (via CreatorSection composable)
  @Test
  fun getInitials_withFullName_returnsFirstAndLastInitials() {
    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.overview.CreatorSection(
            creatorName = "John Doe",
            modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
      }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
        .assertIsDisplayed()
        .assertTextContains("JD", substring = true)
  }

  @Test
  fun getInitials_withSingleName_returnsFirstTwoCharacters() {
    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.overview.CreatorSection(
            creatorName = "Alice",
            modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
      }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
        .assertIsDisplayed()
        .assertTextContains("AL", substring = true, ignoreCase = true)
  }

  @Test
  fun getInitials_withEmptyName_returnsQuestionMark() {
    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.overview.CreatorSection(
            creatorName = "",
            modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
      }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
        .assertIsDisplayed()
        .assertTextContains("?", substring = true)
  }

  @Test
  fun getInitials_withMultipleSpaces_handlesCorrectly() {
    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.overview.CreatorSection(
            creatorName = "  John   Doe  ",
            modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
      }
    }
    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
        .assertIsDisplayed()
        .assertTextContains("JD", substring = true)
  }

  @Test
  fun getInitials_withThreeNames_returnsFirstAndLastInitials() {
    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.overview.CreatorSection(
            creatorName = "John Middle Doe",
            modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
      }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
        .assertIsDisplayed()
        .assertTextContains("JD", substring = true)
  }

  @Test
  fun creatorSection_displaysPostedByLabel() {
    composeTestRule.setContent {
      com.android.sample.ui.overview.CreatorSection(
          creatorName = "Jane Smith",
          modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR)
        .assertIsDisplayed()
        .assertTextContains("Posted by", substring = true, ignoreCase = true)
  }

  @Test
  fun creatorSection_displaysCreatorName() {
    composeTestRule.setContent {
      com.android.sample.ui.overview.CreatorSection(
          creatorName = "Jane Smith",
          modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR)
        .assertIsDisplayed()
        .assertTextContains("Jane Smith", substring = true)
  }

  @Test
  fun creatorSection_avatarIsDisplayed() {
    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.overview.CreatorSection(
            creatorName = "Test User",
            modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR))
      }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR)
        .assertIsDisplayed()
  }

  @Test
  fun validateButton_clickTriggersCallback() {
    // Create a request where current user is the owner
    runTest {
      val ownerRequest =
          Request(
              request4_id,
              "Owner Request",
              "Description",
              emptyList(),
              Location(46.5191, 6.5668, "EPFL"),
              "EPFL",
              RequestStatus.IN_PROGRESS,
              Date(),
              Date(System.currentTimeMillis() + 3_600_000),
              emptyList(),
              emptyList(),
              currentUserId // Current user is the owner
              )
      repository.addRequest(ownerRequest)
    }

    var validateClickedWithId: String? = null
    var onValidateClickCalled = false

    composeTestRule.setContent {
      AcceptRequestScreen(
          requestId = request4_id,
          onValidateClick = { requestId ->
            onValidateClickCalled = true
            validateClickedWithId = requestId
          })
    }

    // Wait for screen to load
    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_DETAILS_CARD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Wait specifically for validate button (only shows for owner)
    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.VALIDATE_REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to make the button visible and click it
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.VALIDATE_REQUEST_BUTTON)
        .performScrollTo() // This will scroll to make it visible
        .performClick()

    composeTestRule.waitForIdle()

    // Verify callback was triggered
    assert(onValidateClickCalled) { ERROR_VALIDATE_CALLBACK_NOT_TRIGGERED }
    assert(validateClickedWithId == request4_id) {
      String.format(ERROR_REQUEST_ID_MISMATCH, request4_id, validateClickedWithId)
    }
  }

  @Test
  fun offlineModeBannerDisplaysWhenInOfflineMode() = runTest {
    // Create a fake request ID that doesn't exist in Firestore
    val fakeRequestId = "nonexistent-request-id-12345"

    // Create cache with the request
    val cache = RequestCache(InstrumentationRegistry.getInstrumentation().targetContext)
    cache.saveRequests(listOf(request1.copy(requestId = fakeRequestId)))

    val viewModel =
        AcceptRequestViewModel(
            requestRepository = repository, userProfileRepository = null, requestCache = cache)

    composeTestRule.setContent {
      AcceptRequestScreen(requestId = fakeRequestId, acceptRequestViewModel = viewModel)
    }

    // Wait for the error to trigger cache fallback
    composeTestRule.waitForIdle()
    Thread.sleep(2000) // Give time for repository failure and cache fallback

    composeTestRule
        .onNodeWithText("You are currently in offline mode", substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun volunteersLazyRowWithMultipleVolunteers() = runTest {
    signIn(DEFAULT_USER_EMAIL)

    val requestWithVolunteers =
        Request(
            requestId = request6_id,
            title = "Request with volunteers",
            description = "Testing volunteers",
            requestType = listOf(RequestType.STUDYING),
            location = Location(46.5191, 6.5668, "EPFL"),
            locationName = "EPFL",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(),
            expirationTime = Date(System.currentTimeMillis() + 3_600_000),
            people = listOf("volunteer1", "volunteer2", "volunteer3"),
            tags = emptyList(),
            creatorId = currentUserId)
    repository.addRequest(requestWithVolunteers)

    composeTestRule.setContent { AcceptRequestScreen(requestId = request6_id) }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_HEADER)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_CONTAINER)
        .assertIsDisplayed()
  }
}
