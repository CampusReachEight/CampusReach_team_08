package com.android.sample.map

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapTestTags
import com.android.sample.ui.map.MapViewModel
import com.android.sample.utils.BaseEmulatorTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapsTest : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepository

  private lateinit var viewModel: MapViewModel
  private lateinit var mapsUtil: MapsUtil

  @Before
  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)
    mapsUtil = MapsUtil(composeTestRule)
    viewModel = MapViewModel(repository)
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
