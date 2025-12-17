package com.android.sample.endToEndTest

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.rule.GrantPermissionRule
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
import com.android.sample.ui.request.edit.EditRequestScreenTestTags
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FakeCredentialManager
import com.android.sample.utils.FakeJwtGenerator
import com.android.sample.utils.FirebaseEmulator
import com.android.sample.utils.UI_WAIT_TIMEOUT
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EndToEndTests : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

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

    runTest { FirebaseEmulator.clearAuthEmulator() }

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
    } catch (_: Exception) {}

    super.tearDown()
  }

  // initialize all you want for an end to end test
  private fun initialize(name: String = firstName, email: String = firstEmail) {
    val fakeGoogleIdToken = FakeJwtGenerator.createFakeGoogleIdToken(name, email)

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    composeTestRule.waitForIdle()

    composeTestRule.setContent { AppNavigation(credentialManager = fakeCredentialManager) }

    composeTestRule.waitForIdle()
    logIn()
  }

  private fun hadARequestWithOtherAccount() {
    repository = RequestRepositoryFirestore(db)

    runTest {

      // actual user
      signInUser()

      // Use future dates so viewStatus calculates as OPEN (startTimeStamp > now)
      val now = System.currentTimeMillis()
      val oneHourFromNow = Date(now + 3_600_000)
      val twoHoursFromNow = Date(now + 7_200_000)

      request1 =
          Request(
              "request1",
              "Here is a good title",
              "In here we will do a lot of things, like beeing good persons",
              listOf(RequestType.STUDYING, RequestType.STUDY_GROUP, RequestType.SPORT),
              Location(46.5191, 6.5668, "EPFL"),
              "EPFL",
              RequestStatus.OPEN,
              oneHourFromNow,
              twoHoursFromNow,
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
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ADD_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ADD_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
        timeoutMillis = UI_WAIT_TIMEOUT)
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
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput(title)

    // description
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_DESCRIPTION),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput(description)

    // requestType
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.getTestTagForRequestType(type)),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(
            EditRequestScreenTestTags.getTestTagForRequestType(type), useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // location input
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(LocationSearchFieldTestTags.InputLocationName),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput(location)

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTagThatStartsWith(), timeoutMillis = UI_WAIT_TIMEOUT)

    // click on the first Location that is proposed
    composeTestRule
        .onAllNodes(hasTestTagThatStartsWith(), useUnmergedTree = true)
        .onFirst()
        .performClick()

    composeTestRule.waitForIdle()
    // put a tag
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.getTestTagForRequestTags(tags)),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(
            EditRequestScreenTestTags.getTestTagForRequestTags(tags), useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // save
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
  }

  // Go to editScreen (not for adding a request, but for editing it)
  // , and check the element is displayed
  private fun goToEditScreen() {
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    // Scroll, assert edit button is displayed and click on it
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
        timeoutMillis = UI_WAIT_TIMEOUT)
  }

  // Navigate to Profile > My Requests, open first item, then go to Edit screen
  private fun goToEditScreenFromMyRequests() {
    // Profile screen
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST),
        timeoutMillis = UI_WAIT_TIMEOUT)

    // My Requests
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    // Open first item
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    // Click Edit
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
        timeoutMillis = UI_WAIT_TIMEOUT)
  }

  // log in and check if you are in RequestList
  private fun logIn() {
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitForIdle()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.REQUESTS_SCREEN), timeoutMillis = UI_WAIT_TIMEOUT)
  }

  private fun logOut() {
    // go to profile screen
    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT),
        timeoutMillis = UI_WAIT_TIMEOUT)

    // Scroll first, then click
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT, useUnmergedTree = true)
        .performScrollTo()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

    // accept
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM, useUnmergedTree = true)
        .performClick()

    // check you are on log in page
    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  // can add a request, and then edit it
  @Test
  fun addRequestAndCanEdit() {

    initialize(firstName, firstEmail)

    goAddRequest()

    // test if save with empty value put error message
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.ERROR_MESSAGE),
        timeoutMillis = UI_WAIT_TIMEOUT)

    addElementOfRequest()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    // Go to edit through Profile > My Requests to ensure we edit our own item
    goToEditScreenFromMyRequests()

    // edit title
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
        .performScrollTo()
        .performTextClearance()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
        .performTextInput(anotherTitle)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
        .assertExists()
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // After save, we are on the view-only details screen; go back to My Requests
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK),
        timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK, useUnmergedTree = true)
        .assertExists()
        .performClick()

    // check that the title is the one modified in My Requests list
    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.onNodeWithText(anotherTitle).assertIsDisplayed()
  }

  // can log in and go to Map
  @Test
  fun canAccessMap() {
    initialize(secondName, secondEmail)
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.MAP_TAB), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MAP_TAB, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(MapTestTags.GOOGLE_MAP_SCREEN), timeoutMillis = UI_WAIT_TIMEOUT)
  }

  // check if you can log in, and then go to profile and disconnect
  @Test
  fun canLogInAndThenDisconnect() {

    initialize(thirdName, thirdEmail)
    composeTestRule.waitForIdle()

    logOut()
  }

  // check if you can accept a request and cancel it
  @Test
  fun canAcceptRequest() {
    hadARequestWithOtherAccount()
    initialize(fourthName, fourthEmail)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasText("Accept", substring = true, ignoreCase = true),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = false)
        .assertTextContains("Accept", substring = true, ignoreCase = true)
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasText("Cancel", substring = true, ignoreCase = true),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
        .performClick()
  }

  @Test
  fun canCreateRequestGoToProfileViewMyRequestsEditAndLogout() {
    // 1. Sign in
    val testName = "78901"
    val testEmail = "myrequest@example.com"
    initialize(testName, testEmail)

    // 2. Create request
    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    // 3. Navigate to Profile
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST),
        timeoutMillis = UI_WAIT_TIMEOUT)

    // 4. Click My Request button
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    // 5. Click request item to edit
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .assertExists()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
        timeoutMillis = UI_WAIT_TIMEOUT)

    // 6. Edit the title
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
        .performScrollTo()
        .assertExists()
        .performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
        .performTextInput(anotherTitle)

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .assertExists()
        .performClick()

    // We are now on a view-only request details screen

    composeTestRule.waitForIdle()

    Espresso.pressBack()

    composeTestRule.waitForIdle()

    // 7. Wait for return to My Requests screen
    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    // 8. Verify edited title is displayed
    composeTestRule.onNodeWithText(anotherTitle).assertIsDisplayed()

    // 9. Press back to return to Profile screen
    Espresso.pressBack()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT),
        timeoutMillis = UI_WAIT_TIMEOUT)

    // 10. Logout (now we're on profile screen with logout button)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun canLoginGoToProfileEditProfileAndLogout() {

    // 1. Sign in
    val testName = "12345"
    val testEmail = "editprofile@example.com"
    initialize(testName, testEmail)
    composeTestRule.waitForIdle()

    // 2. Navigate to Profile
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // 3. Click Edit Profile button - wait for it to be clickable
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON),
        timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.EDIT_PROFILE_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

    // 4. Edit name
    val newName = "John Smith"
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT)
        .assertExists()
        .performTextClearance()

    composeTestRule.onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT).performTextInput(newName)

    // 5. Click section dropdown
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.SECTION_DROPDOWN), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule.onNodeWithTag(ProfileTestTags.SECTION_DROPDOWN).assertExists().performClick()

    composeTestRule.waitForIdle()

    // 6. Select Computer Science
    val sectionTag = ProfileTestTags.SECTION_OPTION_PREFIX + "Computer_Science"
    composeTestRule.onNodeWithTag(sectionTag).assertExists().performClick()

    composeTestRule.waitForIdle()

    // 7. Save changes
    composeTestRule
        .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_SAVE_BUTTON)
        .assertExists()
        .performClick()

    // 8. Wait for dialog to close and profile to update
    composeTestRule.waitForIdle()

    // 9. Verify profile information is updated on screen
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_HEADER_NAME), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME)
        .assertExists()
        .assertTextContains(newName, ignoreCase = false)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_INFO_SECTION), timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_INFO_SECTION)
        .assertExists()
        .assertTextContains("Computer Science", ignoreCase = false)

    // 10. Logout
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
        .assertExists()
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
        .assertExists()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun userCanGoBack() {
    hadARequestWithOtherAccount()

    val testName = "12345"
    val testEmail = "editprofile@example.com"
    initialize(testName, testEmail)
    composeTestRule.waitForIdle()

    goAddRequest()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.GO_BACK_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_ITEM).assertExists().performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK),
        timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)
        .assertExists()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.REQUESTS_SCREEN), timeoutMillis = UI_WAIT_TIMEOUT)
  }

  @Test
  fun userCanSeeSpecificRequestOnMap() {
    hadARequestWithOtherAccount()

    val testName = "12345"
    val testEmail = "editprofile@example.com"
    initialize(testName, testEmail)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_ITEM).assertExists().performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(AcceptRequestScreenTestTags.NAVIGATE_TO_MAP),
        timeoutMillis = UI_WAIT_TIMEOUT)
    composeTestRule
        .onNodeWithTag(AcceptRequestScreenTestTags.NAVIGATE_TO_MAP)
        .assertExists()
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(MapTestTags.DRAG_DOWN_MENU), timeoutMillis = UI_WAIT_TIMEOUT)
  }

  @Test
  fun goAddRequestTest() {
    val testName = "12345"
    val testEmail = "editprofile@example.com"
    initialize(testName, testEmail)

    goAddRequest()
  }

  @Test
  fun addElementOfRequestTest() {
    val testName = "12346"
    val testEmail = "addelement@example.com"
    initialize(testName, testEmail)

    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)
  }

  @Test
  fun goToEditScreenTest() {
    val testName = "12347"
    val testEmail = "gotoedit@example.com"
    initialize(testName, testEmail)

    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    goToEditScreen()
  }

  @Test
  fun goToEditScreenFromMyRequestsTest() {
    val testName = "12348"
    val testEmail = "editmyrequests@example.com"
    initialize(testName, testEmail)

    goAddRequest()
    addElementOfRequest()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    goToEditScreenFromMyRequests()
  }

  @Test
  fun logInTest() {
    val testName = "12349"
    val testEmail = "logintest@example.com"
    initialize(testName, testEmail)

    // initialize appelle déjà logIn(), on vérifie qu'on est bien connecté
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ADD_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun logOutTest() {
    val testName = "12350"
    val testEmail = "logouttest@example.com"
    initialize(testName, testEmail)

    logOut()
  }

  @Test
  fun hadARequestWithOtherAccountTest() {
    hadARequestWithOtherAccount()

    val testName = "12351"
    val testEmail = "otheraccount@example.com"
    initialize(testName, testEmail)

    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
