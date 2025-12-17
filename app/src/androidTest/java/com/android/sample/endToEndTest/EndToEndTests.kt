package com.android.sample.endToEndTest

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EndToEndTests : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createEmptyComposeRule()

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

  private var scenario: ActivityScenario<ComponentActivity>? = null

  companion object {
    private const val TAG = "EndToEndTests"
  }

  @Before
  override fun setUp() {
    println("TEST STARTED")
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
    // Attendre que l'UI soit stable avant de fermer
    composeTestRule.waitForIdle()

    scenario?.close()
    scenario = null

    // Forcer la déconnexion Firebase
    runBlocking {
      FirebaseEmulator.signOut()
      delay(500) // Petit délai pour laisser le temps au cleanup
    }

    super.tearDown()
    println("TEST ENDED")
  }

  // initialize all you want for an end to end test
  private fun initialize(name: String = firstName, email: String = firstEmail) {
    android.util.Log.d(TAG, "=== INITIALIZE START ===")
    android.util.Log.d(TAG, "Creating fake credentials for user: $name, email: $email")
    val fakeGoogleIdToken = FakeJwtGenerator.createFakeGoogleIdToken(name, email)

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    android.util.Log.d(TAG, "Launching ComponentActivity")
    scenario = ActivityScenario.launch(ComponentActivity::class.java)
    scenario!!.onActivity { activity ->
      android.util.Log.d(TAG, "Setting content with AppNavigation")
      activity.setContent { AppNavigation(credentialManager = fakeCredentialManager) }
    }

    android.util.Log.d(TAG, "Waiting for idle")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Calling logIn()")
    logIn()
    android.util.Log.d(TAG, "=== INITIALIZE END ===")
  }

  private fun hadARequestWithOtherAccount() {
    android.util.Log.d(TAG, "=== HAD A REQUEST WITH OTHER ACCOUNT START ===")
    repository = RequestRepositoryFirestore(db)

    runTest {
      android.util.Log.d(TAG, "Signing in user")
      // actual user
      signInUser()

      // Use future dates so viewStatus calculates as OPEN (startTimeStamp > now)
      val now = System.currentTimeMillis()
      val oneHourFromNow = Date(now + 3_600_000)
      val twoHoursFromNow = Date(now + 7_200_000)

      android.util.Log.d(TAG, "Creating request1 with future dates")
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

      android.util.Log.d(TAG, "Adding request to repository")
      repository.addRequest(request1)
    }
    android.util.Log.d(TAG, "Signing out")
    FirebaseEmulator.signOut()
    android.util.Log.d(TAG, "=== HAD A REQUEST WITH OTHER ACCOUNT END ===")
  }

  private fun hasTestTagThatStartsWith(prefix: String = "locationResult_"): SemanticsMatcher {
    return SemanticsMatcher("${SemanticsProperties.TestTag.name} starts with $prefix") { node ->
      val tag = node.config.getOrNull(SemanticsProperties.TestTag)
      tag != null && tag.startsWith(prefix)
    }
  }

  // go to the screen AddRequest
  private fun goAddRequest() {
    runBlocking {
      android.util.Log.d(TAG, "=== GO ADD REQUEST START ===")
      android.util.Log.d(TAG, "Waiting for REQUEST_ADD_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ADD_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking on REQUEST_ADD_BUTTON")
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_ADD_BUTTON, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle after click")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Waiting for INPUT_TITLE to appear")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "=== GO ADD REQUEST END ===")
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
    runBlocking {
      android.util.Log.d(TAG, "=== ADD ELEMENT OF REQUEST START ===")
      android.util.Log.d(
          TAG,
          "Parameters: title=$title, description=$description, type=$type, location=$location, tags=$tags")

      android.util.Log.d(TAG, "Waiting for INPUT_TITLE")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Entering title: $title")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performTextInput(title)

      // description
      android.util.Log.d(TAG, "Waiting for INPUT_DESCRIPTION")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_DESCRIPTION),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Entering description: $description")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.INPUT_DESCRIPTION, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performTextInput(description)

      // requestType
      android.util.Log.d(TAG, "Waiting for request type: $type")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.getTestTagForRequestType(type)),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on request type: $type")
      composeTestRule
          .onNodeWithTag(
              EditRequestScreenTestTags.getTestTagForRequestType(type), useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()

      // location input
      android.util.Log.d(TAG, "Waiting for location input")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(LocationSearchFieldTestTags.InputLocationName),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Entering location: $location")
      composeTestRule
          .onNodeWithTag(LocationSearchFieldTestTags.InputLocationName, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performTextInput(location)

      android.util.Log.d(TAG, "Waiting for idle after location input")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for location results")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTagThatStartsWith(), timeoutMillis = UI_WAIT_TIMEOUT)

      // click on the first Location that is proposed
      android.util.Log.d(TAG, "Clicking on first location result")
      composeTestRule
          .onAllNodes(hasTestTagThatStartsWith(), useUnmergedTree = true)
          .onFirst()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle after location selection")
      composeTestRule.waitForIdle()
      // put a tag
      android.util.Log.d(TAG, "Waiting for tags: $tags")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.getTestTagForRequestTags(tags)),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on tag: $tags")
      composeTestRule
          .onNodeWithTag(
              EditRequestScreenTestTags.getTestTagForRequestTags(tags), useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()

      // save
      android.util.Log.d(TAG, "Waiting for SAVE_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking SAVE_BUTTON")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()
      android.util.Log.d(TAG, "=== ADD ELEMENT OF REQUEST END ===")
    }
  }

  // Go to editScreen (not for adding a request, but for editing it)
  // , and check the element is displayed
  private fun goToEditScreen() {
    runBlocking {
      android.util.Log.d(TAG, "=== GO TO EDIT SCREEN START ===")
      android.util.Log.d(TAG, "Waiting for REQUEST_ITEM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on REQUEST_ITEM")
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      // Scroll, assert edit button is displayed and click on it
      android.util.Log.d(TAG, "Waiting for REQUEST_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on REQUEST_BUTTON (edit)")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for INPUT_TITLE (edit screen)")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "=== GO TO EDIT SCREEN END ===")
    }
  }

  // Navigate to Profile > My Requests, open first item, then go to Edit screen
  private fun goToEditScreenFromMyRequests() {
    runBlocking {
      android.util.Log.d(TAG, "=== GO TO EDIT SCREEN FROM MY REQUESTS START ===")
      // Profile screen
      android.util.Log.d(TAG, "Waiting for PROFILE_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on PROFILE_BUTTON")
      composeTestRule
          .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for PROFILE_ACTION_MY_REQUEST")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST),
          timeoutMillis = UI_WAIT_TIMEOUT)

      // My Requests
      android.util.Log.d(TAG, "Clicking on PROFILE_ACTION_MY_REQUEST")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for REQUEST_ITEM in My Requests")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      // Open first item
      android.util.Log.d(TAG, "Clicking on first REQUEST_ITEM")
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      // Click Edit
      android.util.Log.d(TAG, "Waiting for REQUEST_BUTTON (edit)")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on REQUEST_BUTTON (edit)")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for INPUT_TITLE (edit screen)")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "=== GO TO EDIT SCREEN FROM MY REQUESTS END ===")
    }
  }

  // log in and check if you are in RequestList
  private fun logIn() {
    runBlocking {
      android.util.Log.d(TAG, "=== LOG IN START ===")
      android.util.Log.d(TAG, "Waiting for LOGIN_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Delaying 5 seconds for login button to stabilize")
      delay(5_000)
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Clicking on LOGIN_BUTTON")
      composeTestRule
          .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Delaying 5 seconds for login process")
      delay(5_000)
      // Wait for the Requests screen to be displayed (more reliable than text matching on CI)
      android.util.Log.d(TAG, "Waiting for REQUESTS_SCREEN")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.REQUESTS_SCREEN), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Asserting REQUESTS_SCREEN is displayed")
      composeTestRule
          .onNodeWithTag(NavigationTestTags.REQUESTS_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()
      android.util.Log.d(TAG, "=== LOG IN END ===")
    }
  }

  private fun logOut() {
    android.util.Log.d(TAG, "=== LOG OUT START ===")
    // go to profile screen
    android.util.Log.d(TAG, "Waiting for idle")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Waiting for PROFILE_BUTTON")
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    android.util.Log.d(TAG, "Clicking on PROFILE_BUTTON")
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON, useUnmergedTree = true)
        .performClick()

    android.util.Log.d(TAG, "Waiting for idle")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Waiting for PROFILE_ACTION_LOG_OUT")
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT),
        timeoutMillis = UI_WAIT_TIMEOUT)

    // Scroll first, then click
    android.util.Log.d(TAG, "Waiting for idle before scroll")
    composeTestRule.waitForIdle()

    android.util.Log.d(TAG, "Scrolling to PROFILE_ACTION_LOG_OUT")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT, useUnmergedTree = true)
        .performScrollTo()

    android.util.Log.d(TAG, "Waiting for idle after scroll")
    composeTestRule.waitForIdle()

    android.util.Log.d(TAG, "Clicking on PROFILE_ACTION_LOG_OUT")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT, useUnmergedTree = true)
        .performClick()

    android.util.Log.d(TAG, "Waiting for idle")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Waiting for LOG_OUT_DIALOG")
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

    // accept
    android.util.Log.d(TAG, "Waiting for idle before confirm")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Clicking on LOG_OUT_DIALOG_CONFIRM")
    composeTestRule
        .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM, useUnmergedTree = true)
        .performClick()

    // check you are on log in page
    android.util.Log.d(TAG, "Waiting for idle after logout confirm")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Waiting for LOGIN_BUTTON (back to login screen)")
    composeTestRule.waitUntilAtLeastOneExists(
        matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

    android.util.Log.d(TAG, "Waiting for idle")
    composeTestRule.waitForIdle()
    android.util.Log.d(TAG, "Asserting LOGIN_BUTTON is displayed")
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    android.util.Log.d(TAG, "=== LOG OUT END ===")
  }

  // can add a request, and then edit it
  @Test
  fun addRequestAndCanEdit() {
    runBlocking {
      android.util.Log.d(TAG, "========== TEST: addRequestAndCanEdit START ==========")
      android.util.Log.d(
          TAG, "Step 1: Initializing with firstName=$firstName, firstEmail=$firstEmail")
      initialize(firstName, firstEmail)

      android.util.Log.d(TAG, "Step 2: Going to add request screen")
      goAddRequest()

      // test if save with empty value put error message
      android.util.Log.d(TAG, "Step 3: Testing save with empty values (should show error)")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking SAVE_BUTTON with empty fields")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for ERROR_MESSAGE")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.ERROR_MESSAGE),
          timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Step 4: Adding request elements")
      addElementOfRequest()

      android.util.Log.d(TAG, "Step 5: Waiting for request to appear in list")
      composeTestRule.waitForIdle()
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      // Go to edit through Profile > My Requests to ensure we edit our own item
      android.util.Log.d(TAG, "Step 6: Going to edit screen from My Requests")
      goToEditScreenFromMyRequests()

      // edit title
      android.util.Log.d(TAG, "Step 7: Editing title")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clearing title field")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
          .assertIsDisplayed()
          .performScrollTo()
          .performTextClearance()

      android.util.Log.d(TAG, "Entering new title: $anotherTitle")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
          .performTextInput(anotherTitle)

      android.util.Log.d(TAG, "Step 8: Saving edited request")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Clicking SAVE_BUTTON")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
          .assertExists()
          .performScrollTo()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle after save")
      composeTestRule.waitForIdle()

      // After save, we are on the view-only details screen; go back to My Requests
      android.util.Log.d(TAG, "Step 9: Going back to request list")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK),
          timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking REQUEST_GO_BACK")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK, useUnmergedTree = true)
          .assertExists()
          .performClick()

      // check that the title is the one modified in My Requests list
      android.util.Log.d(TAG, "Step 10: Verifying edited title in list")
      composeTestRule.waitForIdle()
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Asserting new title '$anotherTitle' is displayed")
      composeTestRule.onNodeWithText(anotherTitle).assertIsDisplayed()
      android.util.Log.d(TAG, "========== TEST: addRequestAndCanEdit END ==========")
    }
  }

  // can log in and go to Map
  @Test
  fun canAccessMap() {
    runBlocking {
      android.util.Log.d(TAG, "========== TEST: canAccessMap START ==========")
      android.util.Log.d(
          TAG, "Step 1: Initializing with secondName=$secondName, secondEmail=$secondEmail")
      initialize(secondName, secondEmail)

      android.util.Log.d(TAG, "Step 2: Waiting for MAP_TAB")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.MAP_TAB), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on MAP_TAB")
      composeTestRule
          .onNodeWithTag(NavigationTestTags.MAP_TAB, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Step 3: Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for GOOGLE_MAP_SCREEN")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(MapTestTags.GOOGLE_MAP_SCREEN), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "========== TEST: canAccessMap END ==========")
    }
  }

  // check if you can log in, and then go to profile and disconnect
  @Test
  fun canLogInAndThenDisconnect() {
    runBlocking {
      android.util.Log.d(TAG, "========== TEST: canLogInAndThenDisconnect START ==========")
      android.util.Log.d(
          TAG, "Step 1: Initializing with thirdName=$thirdName, thirdEmail=$thirdEmail")
      initialize(thirdName, thirdEmail)
      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 2: Logging out")
      logOut()
      android.util.Log.d(TAG, "========== TEST: canLogInAndThenDisconnect END ==========")
    }
  }

  // check if you can accept a request and cancel it
  @Test
  fun canAcceptRequest() {
    runBlocking {
      android.util.Log.d(TAG, "========== TEST: canAcceptRequest START ==========")
      android.util.Log.d(TAG, "Step 1: Creating request with other account")
      hadARequestWithOtherAccount()

      android.util.Log.d(
          TAG, "Step 2: Initializing with fourthName=$fourthName, fourthEmail=$fourthEmail")
      initialize(fourthName, fourthEmail)

      android.util.Log.d(TAG, "Step 3: Waiting for REQUEST_ITEM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking on REQUEST_ITEM")
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Step 4: Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for REQUEST_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Step 5: Waiting for 'Accept' text")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasText("Accept", substring = true, ignoreCase = true),
          timeoutMillis = UI_WAIT_TIMEOUT)
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Clicking Accept button")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = false)
          .assertTextContains("Accept", substring = true, ignoreCase = true)
          .performClick()

      android.util.Log.d(TAG, "Step 6: Waiting for idle after accept")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for REQUEST_GO_BACK")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking REQUEST_GO_BACK")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Step 7: Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for REQUEST_ITEM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Step 8: Clicking on REQUEST_ITEM again")
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Step 9: Waiting for 'Cancel' text")
      composeTestRule.waitForIdle()
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasText("Cancel", substring = true, ignoreCase = true),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking Cancel button")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)
          .assertTextContains("Cancel", substring = true, ignoreCase = true)
          .performClick()
      android.util.Log.d(TAG, "========== TEST: canAcceptRequest END ==========")
    }
  }

  @Test
  fun canCreateRequestGoToProfileViewMyRequestsEditAndLogout() {
    runBlocking {
      android.util.Log.d(
          TAG,
          "========== TEST: canCreateRequestGoToProfileViewMyRequestsEditAndLogout START ==========")
      // 1. Sign in
      val testName = "43546437"
      val testEmail = "myrequest@example.com"
      android.util.Log.d(TAG, "Step 1: Initializing with testName=$testName, testEmail=$testEmail")
      initialize(testName, testEmail)

      // 2. Create request
      android.util.Log.d(TAG, "Step 2: Going to add request screen")
      goAddRequest()
      android.util.Log.d(TAG, "Adding request elements")
      addElementOfRequest()

      android.util.Log.d(TAG, "Step 3: Waiting for request in list")
      composeTestRule.waitForIdle()
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      // 3. Navigate to Profile
      android.util.Log.d(TAG, "Step 4: Navigating to Profile")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking PROFILE_BUTTON")
      composeTestRule
          .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON, useUnmergedTree = true)
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for PROFILE_ACTION_MY_REQUEST")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST),
          timeoutMillis = UI_WAIT_TIMEOUT)

      // 4. Click My Request button
      android.util.Log.d(TAG, "Step 5: Clicking My Request button")
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_MY_REQUEST, useUnmergedTree = true)
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for REQUEST_ITEM in My Requests")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      // 5. Click request item to edit
      android.util.Log.d(TAG, "Step 6: Clicking request item")
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 7: Waiting for REQUEST_BUTTON (edit)")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking REQUEST_BUTTON")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_BUTTON, useUnmergedTree = true)
          .performScrollTo()
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for INPUT_TITLE")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.INPUT_TITLE),
          timeoutMillis = UI_WAIT_TIMEOUT)

      // 6. Edit the title
      android.util.Log.d(TAG, "Step 8: Editing title")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE, useUnmergedTree = true)
          .performScrollTo()
          .assertExists()
          .performTextClearance()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Entering new title: $anotherTitle")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.INPUT_TITLE)
          .performTextInput(anotherTitle)

      android.util.Log.d(TAG, "Step 9: Saving changes")
      composeTestRule.waitForIdle()
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(EditRequestScreenTestTags.SAVE_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking SAVE_BUTTON")
      composeTestRule
          .onNodeWithTag(EditRequestScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
          .performScrollTo()
          .assertExists()
          .performClick()

      // We are now on a view-only request details screen
      android.util.Log.d(TAG, "Step 10: Pressing back")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Pressing Espresso back")
      Espresso.pressBack()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      // 7. Wait for return to My Requests screen
      android.util.Log.d(TAG, "Step 11: Waiting for My Requests screen")
      composeTestRule.waitForIdle()
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      // 8. Verify edited title is displayed
      android.util.Log.d(TAG, "Step 12: Verifying edited title")
      composeTestRule.onNodeWithText(anotherTitle).assertIsDisplayed()

      // 9. Press back to return to Profile screen
      android.util.Log.d(TAG, "Step 13: Pressing back to Profile")
      Espresso.pressBack()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for PROFILE_ACTION_LOG_OUT")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT),
          timeoutMillis = UI_WAIT_TIMEOUT)

      // 10. Logout (now we're on profile screen with logout button)
      android.util.Log.d(TAG, "Step 14: Logging out")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
          .performScrollTo()
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for LOG_OUT_DIALOG")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Waiting for LOG_OUT_DIALOG_CONFIRM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking LOG_OUT_DIALOG_CONFIRM")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for LOGIN_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Asserting LOGIN_BUTTON is displayed")
      composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
      android.util.Log.d(
          TAG,
          "========== TEST: canCreateRequestGoToProfileViewMyRequestsEditAndLogout END ==========")
    }
  }

  @Test
  fun canLoginGoToProfileEditProfileAndLogout() {
    runBlocking {
      android.util.Log.d(
          TAG, "========== TEST: canLoginGoToProfileEditProfileAndLogout START ==========")
      // 1. Sign in
      val testName = "12368933"
      val testEmail = "editprofile@example.com"
      android.util.Log.d(TAG, "Step 1: Initializing with testName=$testName, testEmail=$testEmail")
      initialize(testName, testEmail)
      composeTestRule.waitForIdle()

      // 2. Navigate to Profile
      android.util.Log.d(TAG, "Step 2: Navigating to Profile")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.PROFILE_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking PROFILE_BUTTON")
      composeTestRule
          .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
          .assertIsDisplayed()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      // 3. Click Edit Profile button - wait for it to be clickable
      android.util.Log.d(TAG, "Step 3: Waiting for PROFILE_HEADER_EDIT_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON),
          timeoutMillis = UI_WAIT_TIMEOUT)

      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Clicking PROFILE_HEADER_EDIT_BUTTON")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_EDIT_BUTTON, useUnmergedTree = true)
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for EDIT_PROFILE_DIALOG")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.EDIT_PROFILE_DIALOG),
          timeoutMillis = UI_WAIT_TIMEOUT)

      // 4. Edit name
      val newName = "John Smith"
      android.util.Log.d(TAG, "Step 4: Editing name to '$newName'")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clearing name input")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT)
          .assertExists()
          .performTextClearance()

      android.util.Log.d(TAG, "Entering new name")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_NAME_INPUT)
          .performTextInput(newName)

      // 5. Click section dropdown
      android.util.Log.d(TAG, "Step 5: Clicking section dropdown")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.SECTION_DROPDOWN), timeoutMillis = UI_WAIT_TIMEOUT)
      composeTestRule.onNodeWithTag(ProfileTestTags.SECTION_DROPDOWN).assertExists().performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      // 6. Select Computer Science
      android.util.Log.d(TAG, "Step 6: Selecting Computer Science")
      val sectionTag = ProfileTestTags.SECTION_OPTION_PREFIX + "Computer_Science"
      android.util.Log.d(TAG, "Clicking on section: $sectionTag")
      composeTestRule.onNodeWithTag(sectionTag).assertExists().performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      // 7. Save changes
      android.util.Log.d(TAG, "Step 7: Saving changes")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.EDIT_PROFILE_DIALOG_SAVE_BUTTON)
          .assertExists()
          .performClick()

      // 8. Wait for dialog to close and profile to update
      android.util.Log.d(TAG, "Step 8: Waiting for dialog to close")
      composeTestRule.waitForIdle()

      // 9. Verify profile information is updated on screen
      android.util.Log.d(TAG, "Step 9: Verifying profile name update")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_HEADER_NAME),
          timeoutMillis = UI_WAIT_TIMEOUT)

      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_HEADER_NAME)
          .assertExists()
          .assertTextContains(newName, ignoreCase = false)

      android.util.Log.d(TAG, "Verifying profile section update")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_INFO_SECTION),
          timeoutMillis = UI_WAIT_TIMEOUT)
      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_INFO_SECTION)
          .assertExists()
          .assertTextContains("Computer Science", ignoreCase = false)

      // 10. Logout
      android.util.Log.d(TAG, "Step 10: Logging out")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking PROFILE_ACTION_LOG_OUT")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.PROFILE_ACTION_LOG_OUT)
          .assertExists()
          .performScrollTo()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for LOG_OUT_DIALOG")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Waiting for LOG_OUT_DIALOG_CONFIRM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking LOG_OUT_DIALOG_CONFIRM")
      composeTestRule
          .onNodeWithTag(ProfileTestTags.LOG_OUT_DIALOG_CONFIRM)
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()
      android.util.Log.d(TAG, "Waiting for LOGIN_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(SignInScreenTestTags.LOGIN_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Asserting LOGIN_BUTTON is displayed")
      composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
      android.util.Log.d(
          TAG, "========== TEST: canLoginGoToProfileEditProfileAndLogout END ==========")
    }
  }

  @Test
  fun userCanGoBack() {
    runBlocking {
      android.util.Log.d(TAG, "========== TEST: userCanGoBack START ==========")
      android.util.Log.d(TAG, "Step 1: Creating request with other account")
      hadARequestWithOtherAccount()

      val testName = "092890490823"
      val testEmail = "editprofile@example.com"
      android.util.Log.d(TAG, "Step 2: Initializing with testName=$testName, testEmail=$testEmail")
      initialize(testName, testEmail)
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 3: Going to add request screen")
      goAddRequest()

      android.util.Log.d(TAG, "Step 4: Waiting for GO_BACK_BUTTON")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.GO_BACK_BUTTON), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking GO_BACK_BUTTON")
      composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 5: Waiting for REQUEST_ITEM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking REQUEST_ITEM")
      composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_ITEM).assertExists().performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 6: Waiting for REQUEST_GO_BACK")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK),
          timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking REQUEST_GO_BACK")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)
          .assertExists()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 7: Waiting for REQUESTS_SCREEN")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(NavigationTestTags.REQUESTS_SCREEN), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "========== TEST: userCanGoBack END ==========")
    }
  }

  @Test
  fun userCanSeeSpecificRequestOnMap() {
    runBlocking {
      android.util.Log.d(TAG, "========== TEST: userCanSeeSpecificRequestOnMap START ==========")
      android.util.Log.d(TAG, "Step 1: Creating request with other account")
      hadARequestWithOtherAccount()

      val testName = "9012789890453777"
      val testEmail = "editprofile@example.com"
      android.util.Log.d(TAG, "Step 2: Initializing with testName=$testName, testEmail=$testEmail")
      initialize(testName, testEmail)
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 3: Waiting for REQUEST_ITEM")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(RequestListTestTags.REQUEST_ITEM), timeoutMillis = UI_WAIT_TIMEOUT)

      android.util.Log.d(TAG, "Clicking REQUEST_ITEM")
      composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_ITEM).assertExists().performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 4: Waiting for NAVIGATE_TO_MAP")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(AcceptRequestScreenTestTags.NAVIGATE_TO_MAP),
          timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "Clicking NAVIGATE_TO_MAP")
      composeTestRule
          .onNodeWithTag(AcceptRequestScreenTestTags.NAVIGATE_TO_MAP)
          .assertExists()
          .performScrollTo()
          .performClick()

      android.util.Log.d(TAG, "Waiting for idle")
      composeTestRule.waitForIdle()

      android.util.Log.d(TAG, "Step 5: Waiting for DRAG_DOWN_MENU")
      composeTestRule.waitUntilAtLeastOneExists(
          matcher = hasTestTag(MapTestTags.DRAG_DOWN_MENU), timeoutMillis = UI_WAIT_TIMEOUT)
      android.util.Log.d(TAG, "========== TEST: userCanSeeSpecificRequestOnMap END ==========")
    }
  }
}
