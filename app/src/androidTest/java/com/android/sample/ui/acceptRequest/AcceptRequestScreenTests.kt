package com.android.sample.ui.acceptRequest

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.overview.AcceptRequestScreen
import com.android.sample.ui.overview.AcceptRequestScreenTestTags
import com.android.sample.utils.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AcceptRequestScreenTests {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepositoryFirestore
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth

  private lateinit var currentUserId: String
  private lateinit var previousUserId: String
  private var currentEmail: String = DEFAULT_USER_EMAIL
  private var currentPassword: String = DEFAULT_USER_PASSWORD

  private val uiWaitTimeout = 5_000L

  companion object {
    private const val DEFAULT_USER_EMAIL = "test@example.com"
    private const val OTHER_USER_MAIL = "tests@example.com"
    private const val DEFAULT_USER_PASSWORD = "test123456"
  }

  private suspend fun signInUser(email: String, password: String) {
    previousUserId = auth.currentUser?.uid ?: ""
    FirebaseEmulator.signOut()
    FirebaseEmulator.signInTestUser(email, password)
    currentEmail = email
    currentPassword = password
    currentUserId = auth.currentUser?.uid ?: error("Failed to sign in user $email")
  }

  private lateinit var request1: Request
  private lateinit var request2: Request
  private lateinit var request3: Request

  @Before
  fun setUp() {
    db = FirebaseEmulator.firestore
    auth = FirebaseEmulator.auth
    repository = RequestRepositoryFirestore(db)
    runTest {
      FirebaseEmulator.clearAuthEmulator()
      // for having the previous User
      signInUser(OTHER_USER_MAIL, DEFAULT_USER_PASSWORD)

      // actual user
      signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)

      val calendar = Calendar.getInstance()
      calendar.set(2024, Calendar.MARCH, 15, 14, 30, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      val date = Date(calendar.timeInMillis)
      val datePlusOneHour = Date(calendar.timeInMillis + 3_600_000)

      request1 =
          Request(
              "request1",
              "Here is a good title",
              "In here we will do a lot of things, like beeing good persons",
              listOf(
                  RequestType.STUDYING,
                  RequestType.STUDY_GROUP,
                  RequestType.SPORT,
                  RequestType.SPORT),
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
      signInUser(OTHER_USER_MAIL, DEFAULT_USER_PASSWORD)
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
  fun tearDown() {
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.signOut()
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
  fun acceptRequestWithValidArg() {
    composeTestRule.setContent { AcceptRequestScreen("request1") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_COLUMN).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Here is a good title", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextContains(
            "In here we will do a lot of things, like beeing good persons",
            substring = true,
            ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_LOCATION_NAME)
        .assertIsDisplayed()
        .assertTextContains("EPFL", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TYPE)
        .assertIsDisplayed()
        .assertTextContains("Studying, study group", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_TAG)
        .assertIsDisplayed()
        .assertTextContains("Urgent, group work, solo work", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_STATUS)
        .assertIsDisplayed()
        .assertTextContains("Open", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_START_TIME)
        .assertIsDisplayed()
        .assertTextContains("15/03/2024 14:30", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_EXPIRATION_TIME)
        .assertIsDisplayed()
        .assertTextContains("15/03/2024 15:30", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            // take the text of the node
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            // Check if there is the text "Cancel"
            text.contains("Cancel", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            // take the text of the node
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            // Check if there is the text "Accept"
            text.contains("Accept", ignoreCase = true)
          }
    }
  }

  @Test
  fun haveAlreadyAcceptedInPasted() {
    composeTestRule.setContent { AcceptRequestScreen("request3") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            // take the text of the node
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            // Check if there is the text "Accept"
            text.contains("Cancel", ignoreCase = true)
          }
    }
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            // take the text of the node
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            // Check if there is the text "Accept"
            text.contains("Accept", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept", substring = true, ignoreCase = true)
        .performClick()
  }

  @Test
  fun cantAcceptHisOwnRequest() {
    composeTestRule.setContent { AcceptRequestScreen("request2") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            // take the text of the node
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            // Check if there is the text "Accept"
            text.contains("Accept", ignoreCase = true)
          }
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept", substring = true, ignoreCase = true)
  }

  @Test
  fun cantCancelRequestIfNotLogIn() {
    composeTestRule.setContent { AcceptRequestScreen("request3") }

    composeTestRule.waitUntil(uiWaitTimeout) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .any { node ->
            // take the text of the node
            val text =
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
            // Check if there is the text "Accept"
            text.contains("Cancel", ignoreCase = true)
          }
    }

    FirebaseEmulator.signOut()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
  }
}
