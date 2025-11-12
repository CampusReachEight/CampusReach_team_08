package com.android.sample.endToEndTest

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.android.sample.AppNavigation
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.authentication.SignInScreenTestTags
import com.android.sample.ui.map.MapTestTags
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.overview.AcceptRequestScreenTestTags
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.request.LocationSearchFieldTestTags
import com.android.sample.ui.request.RequestListTestTags
import com.android.sample.ui.request.edit.DELETE_BUTTON_TEST_TAG
import com.android.sample.ui.request.edit.DELETE_CONFIRMATION_DIALOG_TEST_TAG
import com.android.sample.ui.request.edit.DELETE_CONFIRM_BUTTON_TEST_TAG
import com.android.sample.ui.request.edit.EditRequestScreenTestTags
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FakeCredentialManager
import com.android.sample.utils.FakeJwtGenerator
import com.android.sample.utils.FirebaseEmulator
import com.android.sample.utils.UI_WAIT_TIMEOUT
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EndToEndTests : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var titles: String
  private lateinit var anotherTitle: String
  private lateinit var descriptions: String

  private lateinit var locations: String

  private lateinit var firstName: String

  private lateinit var firstEmail: String

  private lateinit var secondName: String

  private lateinit var secondEmail: String

  private lateinit var thirdName: String

  private lateinit var thirdEmail: String

  private lateinit var fourthName: String

  private lateinit var fourthEmail: String

  private lateinit var repository: RequestRepositoryFirestore
  private lateinit var request1: Request

  @Before
  override fun setUp() {
    db = FirebaseEmulator.firestore
    auth = FirebaseEmulator.auth
    // Grant location permissions before tests
    InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand(
            "pm grant ${InstrumentationRegistry.getInstrumentation().targetContext.packageName} android.permission.ACCESS_FINE_LOCATION")
    InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand(
            "pm grant ${InstrumentationRegistry.getInstrumentation().targetContext.packageName} android.permission.ACCESS_COARSE_LOCATION")
    runTest {
      FirebaseEmulator.clearAuthEmulator()
      delay(500)
    }

    titles = "title"
    descriptions = "description"
    locations = "EPFL"
    anotherTitle = "anothertitle"

    firstName = "53849"
    firstEmail = "anothertest@example.com"

    secondName = "04829"
    secondEmail = "abcd@example.com"

    thirdName = "43789"
    thirdEmail = "pdtzf@example.com"

    fourthName = "61611"
    fourthEmail = "gdhsja@example.com"
    FirebaseEmulator.signOut()
  }

  @After
  override fun tearDown() {
    try {
      composeTestRule.waitForIdle()
      Thread.sleep(500)
    } catch (_: Exception) {}

    super.tearDown()
    Thread.sleep(1000)
  }

  // initialize all you want for an end to end test
  private fun initialize(name: String = firstName, email: String = firstEmail) {
    val fakeGoogleIdToken = FakeJwtGenerator.createFakeGoogleIdToken(name, email)

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    composeTestRule.waitForIdle()

    composeTestRule.setContent { AppNavigation(credentialManager = fakeCredentialManager) }

    composeTestRule.waitForIdle()
    Thread.sleep(1000)

    logIn()
  }

  private fun hadARequestWithOtherAccount() {
    repository = RequestRepositoryFirestore(db)

    runTest {

      // actual user
      signInUser()

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
    }
    FirebaseEmulator.signOut()
  }

  private fun hasTestTagThatStartsWith(prefix: String = "locationResult_"): SemanticsMatcher {
    return SemanticsMatcher("${SemanticsProperties.TestTag.name} starts with $prefix") { node ->
      val tag = node.config.getOrNull(SemanticsProperties.TestTag)
      tag != null && tag.startsWith(prefix)
    }
  }

  // go to the screen AddRequest
  private fun goAddRequest() {
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ADD_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.INPUT_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  // fill all the element of a request and save it
  private fun addElementOfRequest(
      title: String = titles,
      description: String = descriptions,
      type: RequestType = RequestType.SPORT,
      location: String = locations,
      tags: Tags = Tags.GROUP_WORK
  ) {
    // title
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput(title)

    // description
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput(description)

    // requestType
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.getTestTagForRequestType(type))
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // location input
    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput(location)

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodes(hasTestTagThatStartsWith(), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // click on the first Location that is proposed
    composeTestRule
        .onAllNodes(hasTestTagThatStartsWith(), useUnmergedTree = true)
        .onFirst()
        .performClick()

    // SELECT START DATE
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // Wait for date picker dialog to appear
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    // Click OK on date picker
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()

    // Wait for time picker dialog to appear
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    // Click OK on time picker
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()

    composeTestRule.waitForIdle()

    // SELECT EXPIRATION DATE
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // Wait for date picker dialog to appear
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    // Click OK on date picker
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()

    // Wait for time picker dialog to appear
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    // Click OK on time picker
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()

    composeTestRule.waitForIdle()

    // put a tag
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.getTestTagForRequestTags(tags))
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // save
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
  }

  // Go to editScreen (not for adding a request, but for editing it)
  // , and check the element is displayed
  private fun goToEditScreen() {
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      val nodes =
          composeTestRule
              .onAllNodesWithTag(EditRequestScreenTestTags.INPUT_TITLE)
              .fetchSemanticsNodes()
      val node = if (nodes.isNotEmpty()) nodes[0] else null

      val editable = node?.config?.getOrNull(SemanticsProperties.EditableText)?.text
      editable == titles
    }
  }

  // log in and check if you are in RequestList
  private fun logIn() {

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty() ||
          composeTestRule
              .onAllNodesWithTag(RequestListTestTags.EMPTY_LIST_MESSAGE)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }
  }

  private fun logOut() {
    // go to profile screen
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // click on disconnect
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.LOG_OUT_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // accept
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()

    // check you are on log in page

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  // can add a request, and then edit it
  @Test
  fun addRequestAndCanEdit() {

    initialize(firstName, firstEmail)

    goAddRequest()

    // test if save with empty value put error message
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.ERROR_MESSAGE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    addElementOfRequest()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    goToEditScreen()

    // check if back button works
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    goToEditScreen()

    // edit title
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performTextInput(anotherTitle)

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    // check that the title is the one modified in editScreen
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      val nodes =
          composeTestRule
              .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
              .fetchSemanticsNodes()

      val node = nodes.lastOrNull()
      val texts = node?.config?.getOrNull(SemanticsProperties.Text)?.map { it.text }
      val match = texts?.any { it == anotherTitle } == true

      match
    }
  }

  // can log in and go to Map
  @Test
  fun canAccessMap() {
    initialize(secondName, secondEmail)

    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  // check if you can log in, and then go to profile and disconnect
  @Test
  fun canLogInAndThenDisconnect() {

    initialize(thirdName, thirdEmail)

    logOut()
  }

  // check if you can accept a request and cancel it
  @Test
  fun canAcceptRequest() {
    hadARequestWithOtherAccount()
    initialize(fourthName, fourthEmail)

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Accept", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
        .performClick()
  }
  // Test 1: Sign in → Create Request → Edit Profile → View My Requests → Logout

  @Test
  fun canCreateAndDeleteRequest() {
    val sixthName = "92847"
    val sixthEmail = "sixth@example.com"

    initialize(sixthName, sixthEmail)

    // Create request
    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Open edit screen
    goToEditScreen()

    // Click delete
    composeTestRule
        .onNodeWithTag(DELETE_BUTTON_TEST_TAG)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(DELETE_CONFIRMATION_DIALOG_TEST_TAG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Confirm delete
    composeTestRule.onNodeWithTag(DELETE_CONFIRM_BUTTON_TEST_TAG).assertIsDisplayed().performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(2000)

    composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_ADD_BUTTON).assertExists()

    // Logout
    logOut()
  }

  @Test
  fun canCreateRequestAndViewOnMap() {
    val seventhName = "45612"
    val seventhEmail = "seventh@example.com"

    initialize(seventhName, seventhEmail)

    goAddRequest()

    addElementOfRequest()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to map
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Wait for the trigger button to exist (means requests are loaded!)
    composeTestRule.waitUntil(10000) { // 10 second timeout
      composeTestRule
          .onAllNodesWithTag(MapTestTags.TEST_TRIGGER_BOTTOM_SHEET)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Now click it
    composeTestRule.onNodeWithTag(MapTestTags.TEST_TRIGGER_BOTTOM_SHEET).performClick()

    // Wait for bottom sheet
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.DRAG_DOWN_MENU)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(titles).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapTestTags.BUTTON_DETAILS).performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.INPUT_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    logOut()
  }

  @Test
  fun canCreateRequestWithCurrentLocationEditOnMapAndLogout() {
    val TAG = "EndToEndTests"

    // 1. Sign in and create request (your existing working code)
    val fifthName = "78234"
    val fifthEmail = "fifth@example.com"
    initialize(fifthName, fifthEmail)

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ADD_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.INPUT_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performScrollTo()
        .performTextInput(titles)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION)
        .performScrollTo()
        .performTextInput(descriptions)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.getTestTagForRequestType(RequestType.SPORT))
        .performScrollTo()
        .performClick()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON)
        .performScrollTo()
        .performClick()

    Thread.sleep(1000)

    // Dates
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performScrollTo()
        .performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performScrollTo()
        .performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodesWithText("OK").onFirst().performClick()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.getTestTagForRequestTags(Tags.GROUP_WORK))
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(500)

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 2. Go to Map and verify
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Click map to open bottom sheet
    composeTestRule.onNodeWithTag(MapTestTags.GOOGLE_MAP_SCREEN).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(1000)

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.REQUEST_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify original title
    composeTestRule.onNodeWithText(titles).assertIsDisplayed()

    // 3. Go to Request List (FIXED: using REQUEST_TAB not REQUESTS_TAB)
    composeTestRule.onNodeWithTag(NavigationTestTags.REQUEST_TAB).performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 4. Click request item to edit
    composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_ITEM).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.INPUT_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 5. Edit title
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performScrollTo()
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performTextInput(anotherTitle)

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 6. Go back to Map
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // 7. Click map to verify edit
    composeTestRule.onNodeWithTag(MapTestTags.GOOGLE_MAP_SCREEN).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(1000)

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.REQUEST_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify edited title
    composeTestRule.onNodeWithText(anotherTitle).assertIsDisplayed()

    // 8. Logout
    logOut()
  }
}
