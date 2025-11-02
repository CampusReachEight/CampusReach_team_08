package com.android.sample.map

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.map.ConstantMap
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapTestTags
import com.android.sample.ui.map.MapViewModel
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.UI_WAIT_TIMEOUT
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapsTest : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepository

  private lateinit var viewModel: MapViewModel
  private lateinit var mapsUtil: MapsUtil
  private lateinit var request1: Request
  private lateinit var request2: Request

  @Before
  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)
    mapsUtil = MapsUtil(composeTestRule)
    viewModel = MapViewModel(repository)

    runTest {
      val calendar = Calendar.getInstance()
      calendar.set(2024, Calendar.MARCH, 15, 14, 30, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      val date = Date(calendar.timeInMillis)
      val datePlusOneHour = Date(calendar.timeInMillis + 3_600_000)

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
              listOf(),
              emptyList(),
              currentUserId)

      repository.addRequest(request2)

      signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
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
    composeTestRule.setContent { MapScreen(viewModel) }
    composeTestRule.waitForIdle()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun mapScreen_exist() {
    composeTestRule
        .onNodeWithTag(MapTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun zoomInButton_exists() {
    mapsUtil.assertZoomInButtonExists()
  }

  @Test
  fun zoomOutButton_exists() {
    mapsUtil.assertZoomOutButtonExists()
  }

  @Test
  fun zoomInButton_isClickable() {
    mapsUtil.clickZoomIn()
  }

  @Test
  fun zoomOutButton_isClickable() {
    mapsUtil.clickZoomOut()
  }

  @Test
  fun zoomButtons_multipleClicks() {
    mapsUtil.clickZoomIn()
    mapsUtil.clickZoomIn()
    mapsUtil.clickZoomOut()
    mapsUtil.clickZoomOut()
  }

  // check that all components displayed correctly
  @Test
  fun clickOnOwnRequest() {
    viewModel.updateCurrentRequest(request1)
    composeTestRule.waitForIdle()

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

    composeTestRule
        .onNodeWithTag(MapTestTags.testTagForTab(ConstantMap.PROFILE))
        .assertIsDisplayed()
        .performClick()

    composeTestRule.onNodeWithTag(MapTestTags.PROFILE_TEXT).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapTestTags.testTagForTab(ConstantMap.DETAILS))
        .assertIsDisplayed()
        .performClick()

    composeTestRule
        .onNodeWithTag(MapTestTags.BUTTON_DETAILS)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(ConstantMap.TEXT_EDIT)
  }

  // check if the name of the button has changed, and the button x works
  @Test
  fun clickOnAnotherRequestThenClose() {
    viewModel.updateCurrentRequest(request2)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MapTestTags.DRAG).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapTestTags.BUTTON_DETAILS)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(ConstantMap.TEXT_SEE_DETAILS)

    composeTestRule.onNodeWithTag(MapTestTags.BUTTON_X).assertIsDisplayed().performClick()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(MapTestTags.REQUEST_TITLE).fetchSemanticsNodes().isEmpty()
    }
  }
}

/** Utility class for performing map-related actions in tests */
class MapsUtil(private val composeTestRule: ComposeTestRule) {

  fun assertZoomInButtonExists() {
    composeTestRule.onNodeWithTag(MapTestTags.ZOOM_IN_BUTTON, useUnmergedTree = true).assertExists()
  }

  fun assertZoomOutButtonExists() {
    composeTestRule
        .onNodeWithTag(MapTestTags.ZOOM_OUT_BUTTON, useUnmergedTree = true)
        .assertExists()
  }

  fun clickZoomIn() {
    composeTestRule.onNodeWithTag(MapTestTags.ZOOM_IN_BUTTON, useUnmergedTree = true).performClick()
  }

  fun clickZoomOut() {
    composeTestRule
        .onNodeWithTag(MapTestTags.ZOOM_OUT_BUTTON, useUnmergedTree = true)
        .performClick()
  }

  fun getCurrentZoomLevel(): Float {
    composeTestRule.waitForIdle()

    // Get all nodes and manually filter for the zoom level tag
    val semanticsNodes = composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()

    fun findZoomTag(node: androidx.compose.ui.semantics.SemanticsNode): Float? {
      val tag = node.config.getOrNull(SemanticsProperties.TestTag)
      if (tag?.startsWith("${MapTestTags.ZOOM_LEVEL}:") == true) {
        return tag.substringAfter(":").toFloatOrNull()
      }

      // Recursively search children
      node.children.forEach { child ->
        val result = findZoomTag(child)
        if (result != null) return result
      }

      return null
    }

    return findZoomTag(semanticsNodes) ?: -1f
  }
}
