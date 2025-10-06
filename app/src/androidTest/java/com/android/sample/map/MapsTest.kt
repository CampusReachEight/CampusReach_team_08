package com.android.sample.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.setContent { MapScreen() }
    }

    @Test
    fun mapScreen_exist() {
        composeTestRule
            .onNodeWithTag(MapTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
            .assertExists()
    }
}