package com.android.sample.map

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.rule.GrantPermissionRule
import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestOwnership
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.ui.map.ConstantMap
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapSettingsTestTags
import com.android.sample.ui.map.MapTestTags
import com.android.sample.ui.map.MapViewModel
import com.android.sample.ui.map.toDisplayStringWithoutHours
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.request.FakeLocationProvider
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.UI_WAIT_TIMEOUT
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapWithCurrentLocationTest : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var repository: RequestRepository
  private lateinit var profileRepository: UserProfileRepository
  private lateinit var fakeFusedLocationProvider: FakeLocationProvider

  private lateinit var viewModel: MapViewModel
  private lateinit var mapsUtil: MapsUtil
  private lateinit var request1: Request
  private lateinit var request2: Request
  private lateinit var request3: Request
  private lateinit var profile2: UserProfile
  private lateinit var date: Date
  private lateinit var datePlusOneHour: Date
  private val location1: Location = Location(26.5191, 6.5668, "IDK place")
  private val location2: Location = Location(46.5191, 6.5668, "EPFL")
  private val location3: Location = Location(46.5191, 6.5667, "denhub")
  private val currentLocation: Location = Location(46.5191, 6.5668, "Current position")

  private val requestId1 = "request2"
  private val requestId2 = "request2"
  private val requestId3 = "request3"

  private val title2 = "Another one"
  private val title3 = "A big title"
  private val description2 = "Very good description"

  private val name2 = "Other"
  private val firstName2 = "Mama"
  private val kudos2 = 11
  private val helpReceived = 0
  private val section2 = UserSections.MATHEMATICS
  private val randomPerson = "randomPerson"
  private val messageError = "No location"

  @Before
  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)
    profileRepository = UserProfileRepositoryFirestore(db)
    mapsUtil = MapsUtil(composeTestRule)
    fakeFusedLocationProvider = FakeLocationProvider()
    viewModel = MapViewModel(repository, profileRepository, fakeFusedLocationProvider)

    runTest {
      val calendar = Calendar.getInstance()
      calendar.set(
          MapsTestConst.RANDOM_YEAR,
          MapsTestConst.RANDOM_MONTH,
          MapsTestConst.RANDOM_DATE,
          MapsTestConst.RANDOM_HOUR,
          MapsTestConst.RANDOM_MINUTE,
          MapsTestConst.RANDOM_SECOND)
      calendar.set(Calendar.MILLISECOND, MapsTestConst.ZERO_MILLISECONDS)
      date = Date(calendar.timeInMillis)
      datePlusOneHour = Date(calendar.timeInMillis + MapsTestConst.ONE_HOUR)

      val creatorIdForRequest2 = currentUserId

      request2 =
          Request(
              requestId2,
              title2,
              description2,
              listOf(RequestType.STUDYING),
              location1,
              location1.name,
              RequestStatus.ARCHIVED,
              date,
              datePlusOneHour,
              listOf(),
              emptyList(),
              creatorIdForRequest2)
      request3 =
          request2.copy(
              requestId = requestId3,
              title = title3,
              location = location2,
              locationName = location2.name,
              requestType = listOf(RequestType.STUDYING, RequestType.EATING),
              people = listOf(creatorIdForRequest2))
      request1 = request2.copy(requestId1, location = location3, people = listOf(randomPerson))

      repository.addRequest(request2)
      repository.addRequest(request3)
      repository.addRequest(request1)

      profile2 =
          UserProfile(
              creatorIdForRequest2,
              firstName2,
              name2,
              DEFAULT_USER_EMAIL,
              null,
              kudos2,
              helpReceived,
              section2,
              datePlusOneHour)

      profileRepository.addUserProfile(profile2)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun haveAValidCurrentLocation() {
    fakeFusedLocationProvider.locationToReturn = currentLocation
    fakeFusedLocationProvider.exceptionToThrow = null

    composeTestRule.setContent { MapScreen(viewModel) }
    composeTestRule.waitForIdle()

    checkSettingsWithFilter()
  }

  @Test
  fun haveNotAValidCurrentLocation() {
    fakeFusedLocationProvider.locationToReturn = null
    fakeFusedLocationProvider.exceptionToThrow = Exception(messageError)

    composeTestRule.setContent { MapScreen(viewModel) }
    composeTestRule.waitForIdle()

    checkSettingsWithFilter()
  }

  @Test
  fun beginWithACurrentRequestAndValidLocation() {
    fakeFusedLocationProvider.locationToReturn = currentLocation
    fakeFusedLocationProvider.exceptionToThrow = null

    composeTestRule.setContent { MapScreen(viewModel, requestId = requestId1) }
    composeTestRule.waitForIdle()

    checkDetailsRequest()
    checkDetailsProfile()
  }

  @Test
  fun beginWithACurrentRequestAndNoLocation() {
    fakeFusedLocationProvider.locationToReturn = null
    fakeFusedLocationProvider.exceptionToThrow = Exception(messageError)

    composeTestRule.setContent { MapScreen(viewModel, requestId = requestId2) }
    composeTestRule.waitForIdle()

    checkDetailsRequest()
    checkDetailsProfile()
  }

  fun checkDetailsProfile() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.testTagForTab(ConstantMap.PROFILE))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(MapTestTags.testTagForTab(ConstantMap.PROFILE))
        .assertIsDisplayed()
        .performClick()

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_NAME)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains("$name2 $firstName2")

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_SECTION_TEXT)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(ConstantMap.SECTION)

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_SECTION)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(section2.label)

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_KUDOS_TEXT)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(ConstantMap.KUDOS)

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_KUDOS)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(kudos2.toString())

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_CREATION_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(date.toDisplayStringWithoutHours())

    composeTestRule
        .onNodeWithTag(MapTestTags.PROFILE_EDIT_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
  }

  fun checkDetailsRequest() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(MapTestTags.DRAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(MapTestTags.DRAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapTestTags.DRAG_DOWN_MENU).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapTestTags.REQUEST_TITLE)
        .assertIsDisplayed()
        .assertTextContains(request1.title)

    composeTestRule
        .onNodeWithTag(MapTestTags.REQUEST_DESCRIPTION)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(request1.description)

    composeTestRule
        .onNodeWithTag(MapTestTags.START_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(request1.startTimeStamp.toDisplayString())

    composeTestRule
        .onNodeWithTag(MapTestTags.END_DATE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(request1.expirationTime.toDisplayString())

    composeTestRule.onNodeWithTag(MapTestTags.BUTTON_X).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapTestTags.REQUEST_STATUS)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(request1.status.name)

    composeTestRule
        .onNodeWithTag(MapTestTags.REQUEST_LOCATION_NAME)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(request1.locationName)
  }

  fun clickOnARequestOwnership(filter: RequestOwnership) {
    composeTestRule.onNodeWithTag(MapTestTags.MAP_FILTER_OWNER).assertIsDisplayed().performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.testTagForRequestOwnership(filter))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(MapTestTags.testTagForRequestOwnership(filter))
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.testTagForRequestOwnership(filter))
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  fun checkSettingsWithFilter() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapTestTags.MAP_LIST_FILTER)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    clickOnARequestOwnership(RequestOwnership.OWN)
    clickOnARequestOwnership(RequestOwnership.NOT_ACCEPTED)
    clickOnARequestOwnership(RequestOwnership.NOT_ACCEPTED_BY_ME)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
        .assertIsDisplayed()
        .assertTextContains(ConstantMap.AUTO_ZOOM_SETTINGS)

    composeTestRule
        .onNodeWithTag(MapSettingsTestTags.DESCRIPTION_MAP_SETTINGS)
        .assertIsDisplayed()
        .assertTextContains(ConstantMap.DESCRIPTION_REQUEST)

    composeTestRule
        .onNodeWithTag(MapSettingsTestTags.CURRENT_LOCATION_SETTINGS)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    clickOnARequestOwnership(RequestOwnership.OWN)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(MapSettingsTestTags.NO_ZOOM_SETTINGS)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    clickOnARequestOwnership(RequestOwnership.ALL)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(MapSettingsTestTags.NEAREST_REQUEST_SETTINGS)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    clickOnARequestOwnership(RequestOwnership.OWN)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(MapSettingsTestTags.CLOSE_BUTTON_SETTINGS)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(MapSettingsTestTags.TITLE_MAP_SETTINGS)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    clickOnARequestOwnership(RequestOwnership.ALL)
  }
}
