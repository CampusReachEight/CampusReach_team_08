package com.android.sample.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.LightThemePreview
import com.android.sample.ui.theme.DarkThemePreview
import com.android.sample.ui.theme.SampleAppTheme
import com.android.sample.ui.theme.ThemePreviewContent

@RunWith(AndroidJUnit4::class)
class ThemePreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lightThemePreview_showsExpectedSectionsAndSwatches() {
        composeTestRule.setContent {
            LightThemePreview()
        }

        // core headings
        composeTestRule.onNodeWithTag("heading_utility_colors").assertIsDisplayed()
        composeTestRule.onNodeWithTag("heading_app_palette_light").assertIsDisplayed()
        composeTestRule.onNodeWithTag("heading_app_palette_dark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("heading_typography_sample").assertIsDisplayed()

        // some swatch labels
        composeTestRule.onNodeWithTag("swatch_BlackColor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("swatch_PrimaryColor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("swatch_PrimaryDark").assertIsDisplayed()
    }

    @Test
    fun darkThemePreview_showsExpectedSectionsAndSwatches() {
        composeTestRule.setContent {
            DarkThemePreview()
        }

        // core headings
        composeTestRule.onNodeWithTag("heading_utility_colors").assertIsDisplayed()
        composeTestRule.onNodeWithTag("heading_app_palette_light").assertIsDisplayed()
        composeTestRule.onNodeWithTag("heading_app_palette_dark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("heading_typography_sample").assertIsDisplayed()

        // some swatch labels
        composeTestRule.onNodeWithTag("swatch_WhiteColor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("swatch_SecondaryDark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("swatch_ErrorDark").assertIsDisplayed()
    }

    @Test
    fun sampleAppTheme_staticColors_and_customTextColor_pathExecuted() {
        composeTestRule.setContent {
            // force static (non-dynamic) branch and supply an explicit textColor
            SampleAppTheme(darkTheme = false, dynamicColor = false) {
                ThemePreviewContent(textColor = Color.Magenta)
            }
        }

        // ensure the UI built with the explicit textColor is displayed
        composeTestRule.onNodeWithTag("heading_app_palette_light").assertIsDisplayed()
        composeTestRule.onNodeWithTag("swatch_PrimaryColor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("swatch_BackgroundColor").assertIsDisplayed()
    }
}