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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
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
    // Click a request item (now opens AcceptRequestScreen, not Edit directly)
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertIsDisplayed()
        .performClick()

    // Wait for Accept screen button to appear
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll the action button into view if needed, then click it
    val buttonNode = composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
    buttonNode.assertExists()
    try {
      buttonNode.performScrollTo()
    } catch (_: Throwable) {
      // Ignore if not scrollable; best-effort bring into view
    }
    buttonNode.performClick()

    // Wait for edit screen title field
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.INPUT_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
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

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(EditRequestScreenTestTags.ERROR_MESSAGE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    addElementOfRequest()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate via Accept screen to edit
    goToEditScreen()

    // Back from edit (EditRequestScreen) to Accept screen first then again to list
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // After back, we might be on Accept screen (if a nested stack). Press back again if still
    // there.
    composeTestRule.waitForIdle()
    val acceptButtonNodes =
        composeTestRule
            .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
            .fetchSemanticsNodes()
    if (acceptButtonNodes.isNotEmpty()) {
      // Back to list
      composeTestRule.onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK).performClick()
    }

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Enter edit again
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

    // Wait for return to list (Accept + back path); ensure item titles loaded
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify edited title appears
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .any { node ->
            val texts =
                node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()
            texts.any { it == anotherTitle }
          }
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

  @Test
  fun canCreateAndDeleteRequest() {
    val sixthName = "92847"
    val sixthEmail = "sixth@example.com"

    initialize(sixthName, sixthEmail)

    // Create request
    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Open edit screen via Accept
    goToEditScreen()

    // Click delete
    composeTestRule
        .onNodeWithTag(DELETE_BUTTON_TEST_TAG)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(DELETE_CONFIRMATION_DIALOG_TEST_TAG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Confirm delete
    composeTestRule.onNodeWithTag(DELETE_CONFIRM_BUTTON_TEST_TAG).assertIsDisplayed().performClick()

    // Wait for list (request removed) or empty list message
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

    // Logout
    logOut()
  }

  @Test
  fun canCreateRequestGoToProfileViewMyRequestsEditAndLogout() {
    val testName = "78901"
    val testEmail = "myrequest@example.com"
    initialize(testName, testEmail)

    // Create request
    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to Profile
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // My Requests
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Open via Accept then edit
    goToEditScreen()

    // Edit title
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

    // Wait for My Requests list again
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify edited title present
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .any { node ->
            val texts =
                node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()
            texts.any { it == anotherTitle }
          }
    }

    // Back to Profile
    Espresso.pressBack()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Logout
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.LOG_OUT_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun canLoginGoToProfileEditProfileAndLogout() {
    // 1. Sign in
    val testName = "12345"
    val testEmail = "editprofile@example.com"
    initialize(testName, testEmail)

    // 2. Navigate to Profile
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 3. Click Edit Profile button
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 4. Edit name
    val newName = "John Smith"
    composeTestRule
        .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT)
        .assertIsDisplayed()
        .performTextClearance()

    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT).performTextInput(newName)

    // 5. Click section dropdown
    composeTestRule
        .onNodeWithTag(ProfileTestTags.SECTION_DROPDOWN)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(500) // Wait for bottom sheet to appear

    // 6. Select Computer Science
    val sectionTag = ProfileTestTags.SECTION_OPTION_PREFIX + "Computer_Science"
    composeTestRule.onNodeWithTag(sectionTag).assertIsDisplayed().performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(500)

    // 7. Save changes
    composeTestRule
        .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_SAVE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // 8. Wait for dialog to close and profile to update
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    Thread.sleep(500) // Let UI settle
    composeTestRule.waitForIdle()

    // 9. Verify profile information is updated on screen
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME)
        .assertIsDisplayed()
        .assertTextContains(newName, ignoreCase = false)

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_INFO_SECTION)
        .assertIsDisplayed()
        .assertTextContains("Computer Science", ignoreCase = false)

    // 10. Logout
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

    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }
}
