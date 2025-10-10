package com.android.sample.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapTestTags
import com.android.sample.utils.BaseEmulatorTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapsTest : BaseEmulatorTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepositoryFirestore

  @Before
  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)
    composeTestRule.setContent { MapScreen() }
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
}
