package com.android.sample.endToEndTest

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.request.LocationSearchFieldTestTags
import com.android.sample.ui.request.RequestListTestTags
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
      startDate: Date = Date(),
      expireDate: Date = Date(System.currentTimeMillis() + 3_600_000),
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

    // clear start date
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()

    // put start date
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_START_DATE)
        .performTextInput(startDate.toDisplayString())

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE)
        .performTextInput(expireDate.toDisplayString())

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

  // full end-to-end flow test covering sign in, search, accept existing request, create own
  // request, filter, edit, re-filter, map check, and logout.
  @Test
  fun fullFlow_search_accept_create_filter_edit_map_logout() {
    // Prepare an existing request under another account to accept & search for
    hadARequestWithOtherAccount()

    // Initialize (sign in) with a fresh user
    initialize(fourthName, fourthEmail)

    // 2) Search existing request using keyword from description + location name
    val searchQuery = "beeing EPFL" // combines word from description + location name
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_SEARCH_BAR)
        .assertIsDisplayed()
        .performTextInput(searchQuery)

    // Wait until only 1 request matches (the seeded one)
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .size == 1
    }

    // 3) Accept that request
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertIsDisplayed()
        .performClick() // Accept

    // Go back to list
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)
        .assertIsDisplayed()
        .performClick()

    // Clear search so creation list logic is simpler
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.CLEAR_SEARCH_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // 4) Create own request
    goAddRequest()
    val myTitle = "My Created Request"
    val myDescription = "Collaborate at EPFL campus"
    addElementOfRequest(
        title = myTitle,
        description = myDescription,
        type = RequestType.STUDY_GROUP,
        location = "EPFL",
        tags = Tags.GROUP_WORK)

    // Wait for it to appear in list
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, true)
          .fetchSemanticsNodes()
          .any { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.contains(myTitle) } ==
                true
          }
    }

    // Ensure filter bar itself is present
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.FILTER_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    Thread.sleep(250)

    // 5) Filters (scroll the FILTER_BAR LazyRow to reveal each button as needed)
    val typeBtnTag = RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON
    val tagBtnTag = RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON
    val statusBtnTag = RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON

    // Scroll to type button then click
    composeTestRule
        .onNodeWithTag(RequestListTestTags.FILTER_BAR)
        .performScrollToNode(hasTestTag(typeBtnTag))
    composeTestRule.onNodeWithTag(typeBtnTag).assertIsDisplayed().performClick()
    val studyGroupTag = RequestListTestTags.getRequestTypeFilterTag(RequestType.STUDY_GROUP)
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(studyGroupTag).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag(studyGroupTag).assertExists().performClick()
    composeTestRule.onNodeWithTag(typeBtnTag).performClick()
    Thread.sleep(150)

    // Scroll to tag button then click
    composeTestRule
        .onNodeWithTag(RequestListTestTags.FILTER_BAR)
        .performScrollToNode(hasTestTag(tagBtnTag))
    composeTestRule.onNodeWithTag(tagBtnTag).assertIsDisplayed().performClick()
    val groupWorkTag = RequestListTestTags.getRequestTagFilterTag(Tags.GROUP_WORK)
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(groupWorkTag).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag(groupWorkTag).assertExists().performClick()
    composeTestRule.onNodeWithTag(tagBtnTag).performClick()
    Thread.sleep(150)

    // Scroll to status button then click
    composeTestRule
        .onNodeWithTag(RequestListTestTags.FILTER_BAR)
        .performScrollToNode(hasTestTag(statusBtnTag))
    composeTestRule.onNodeWithTag(statusBtnTag).assertIsDisplayed().performClick()
    val openStatusTag = RequestListTestTags.getRequestStatusFilterTag(RequestStatus.IN_PROGRESS)
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(openStatusTag).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag(openStatusTag).assertExists().performClick()
    composeTestRule.onNodeWithTag(statusBtnTag).performClick()

    // Expect only our request remains
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      val nodes =
          composeTestRule
              .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
              .fetchSemanticsNodes()
      nodes.size == 1 &&
          nodes.first().config.getOrNull(SemanticsProperties.Text)?.any {
            it.text.contains(myTitle)
          } == true
    }

    // 6) Edit my request
    goToEditScreen()
    val editedTitle = "My Edited Request"
    composeTestRule.onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performTextInput(editedTitle)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON)
        .performScrollTo()
        .performClick()

    // Wait for updated title in list
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .any {
            it.config.getOrNull(SemanticsProperties.Text)?.any { s ->
              s.text.contains(editedTitle)
            } == true
          }
    }

    // 7) Update filters (re-apply search on edited title + location) and confirm visibility
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_SEARCH_BAR)
        .performTextInput("Edited EPFL")
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      val count =
          composeTestRule
              .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
              .fetchSemanticsNodes()
              .size
      count == 1
    }

    // 8) Open map and verify request title appears after selecting marker list (cannot click marker
    // directly -> ensure map screen loads)
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // (Best effort) Assert bottom sheet not crashing when opened later by some interaction - here
    // we just ensure map visible.

    // 9) Sign out
    logOut()
  }
}
