package com.android.sample.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.DarkPalette
import com.android.sample.ui.theme.LightPalette
import com.android.sample.ui.theme.PaletteDebugView
import com.android.sample.ui.theme.PaletteTestTags
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaletteDebugViewTests {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsLightPaletteHexes() {
        composeTestRule.setContent {
            SampleAppTheme(darkTheme = false) {
                PaletteDebugView()
            }
        }
        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_PRIMARY)
            .assertIsDisplayed()
            .assertTextEquals(String.format("#%08X", LightPalette.primary.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_SECONDARY)
            .assertTextEquals(String.format("#%08X", LightPalette.secondary.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ACCENT)
            .assertTextEquals(String.format("#%08X", LightPalette.accent.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_TEXT)
            .assertTextEquals(String.format("#%08X", LightPalette.text.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_BACKGROUND)
            .assertTextEquals(String.format("#%08X", LightPalette.background.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_SURFACE)
            .assertTextEquals(String.format("#%08X", LightPalette.surface.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ERROR)
            .assertTextEquals(String.format("#%08X", LightPalette.error.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ON_PRIMARY)
            .assertTextEquals(String.format("#%08X", LightPalette.onPrimary.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ON_BACKGROUND)
            .assertTextEquals(String.format("#%08X", LightPalette.onBackground.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ON_SURFACE)
            .assertTextEquals(String.format("#%08X", LightPalette.onSurface.toArgb()))
    }

    @Test
    fun showsDarkPaletteHexes() {
        composeTestRule.setContent {
            SampleAppTheme(darkTheme = true) {
                PaletteDebugView()
            }
        }

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_PRIMARY)
            .assertTextEquals(String.format("#%08X", DarkPalette.primary.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_SECONDARY)
            .assertTextEquals(String.format("#%08X", DarkPalette.secondary.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ACCENT)
            .assertTextEquals(String.format("#%08X", DarkPalette.accent.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_TEXT)
            .assertTextEquals(String.format("#%08X", DarkPalette.text.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_BACKGROUND)
            .assertTextEquals(String.format("#%08X", DarkPalette.background.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_SURFACE)
            .assertTextEquals(String.format("#%08X", DarkPalette.surface.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ERROR)
            .assertTextEquals(String.format("#%08X", DarkPalette.error.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ON_PRIMARY)
            .assertTextEquals(String.format("#%08X", DarkPalette.onPrimary.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ON_BACKGROUND)
            .assertTextEquals(String.format("#%08X", DarkPalette.onBackground.toArgb()))

        composeTestRule.onNodeWithTag(PaletteTestTags.PALETTE_ON_SURFACE)
            .assertTextEquals(String.format("#%08X", DarkPalette.onSurface.toArgb()))
    }
}