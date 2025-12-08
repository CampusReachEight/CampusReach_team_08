package com.android.sample.ui.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class RangeFilterUITest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun rangeFilterButton_displaysAndOpensPanel() {
    // Create a simple range facet for testing
    val testDef =
        RangeFilterDefinitions.RangeFilterDefinition<Int>(
            id = "testRange",
            title = "Test Range",
            minBound = 0,
            maxBound = 100,
            step = 1,
            extract = { it },
            buttonTestTag = "testRangeButton",
            panelTestTag = "testRangePanel",
            sliderTestTag = "testRangeSlider",
            minFieldTestTag = "testRangeMinField",
            maxFieldTestTag = "testRangeMaxField")
    val rangeFacet = RangeFacet(testDef)

    var panelOpened = false

    composeTestRule.setContent {
      RangeFilterButton(rangeFacet = rangeFacet, onClick = { panelOpened = true })
    }

    // Verify button is displayed with title
    composeTestRule.onNodeWithTag("testRangeButton").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Range").assertIsDisplayed()

    // Click button and verify callback
    composeTestRule.onNodeWithTag("testRangeButton").performClick()
    assert(panelOpened) { "Panel should have been opened" }
  }
}
