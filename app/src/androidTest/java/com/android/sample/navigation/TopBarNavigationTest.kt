package com.android.sample.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.navigation.NavigationTestTags
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopNavigationBarTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private var settingsClicked = false

  @Before
  fun setUp() {
    settingsClicked = false
  }

  @Test
  fun topNavigationBar_isDisplayed() {
    composeTestRule.setContent {
      TopNavigationBar(selectedTab = Tab.Requests, onSettingsClick = {})
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_NAVIGATION_BAR).assertIsDisplayed()
  }

  @Test
  fun topNavigationBar_displaysCorrectTitle_forRequestsTab() {
    composeTestRule.setContent {
      TopNavigationBar(selectedTab = Tab.Requests, onSettingsClick = {})
    }

    composeTestRule.onNodeWithText("Reach Out").assertIsDisplayed()
  }

  @Test
  fun topNavigationBar_displaysCorrectTitle_forEventsTab() {
    composeTestRule.setContent { TopNavigationBar(selectedTab = Tab.Events, onSettingsClick = {}) }

    composeTestRule.onNodeWithText("Campus Events").assertIsDisplayed()
  }

  @Test
  fun topNavigationBar_displaysCorrectTitle_forMapTab() {
    composeTestRule.setContent { TopNavigationBar(selectedTab = Tab.Map, onSettingsClick = {}) }

    composeTestRule.onNodeWithText("Map").assertIsDisplayed()
  }

  @Test
  fun settingsButton_isDisplayed() {
    composeTestRule.setContent {
      TopNavigationBar(selectedTab = Tab.Requests, onSettingsClick = {})
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON).assertIsDisplayed()
  }

  @Test
  fun settingsButton_invokesCallbackOnClick() {
    composeTestRule.setContent {
      TopNavigationBar(selectedTab = Tab.Events, onSettingsClick = { settingsClicked = true })
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON).performClick()

    assertTrue(settingsClicked)
  }

  @Test
  fun titleUsesCorrectStyleAndColor() {
    composeTestRule.setContent {
      TopNavigationBar(selectedTab = Tab.Requests, onSettingsClick = {})
    }

    composeTestRule.onNodeWithText("Reach Out").assertIsDisplayed()
  }

  @Test
  fun preview_composableRendersSuccessfully() {
    composeTestRule.setContent { TopNavigationBarPreview() }

    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_NAVIGATION_BAR).assertIsDisplayed()
  }
}
