package com.android.sample.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.profile.ProfileActions
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileHeader_isDisplayed() {
        composeTestRule.setContent { ProfileScreen() }
        composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_HEADER).assertIsDisplayed()
    }

    @Test
    fun profileStats_areDisplayed() {
        composeTestRule.setContent { ProfileScreen() }
        composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_STATS).assertIsDisplayed()
    }

    @Test
    fun profileInformation_isDisplayed() {
        composeTestRule.setContent { ProfileScreen() }
        composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_INFORMATION).assertIsDisplayed()
    }

    @Test
    fun profileActions_areDisplayed() {
        composeTestRule.setContent { ProfileScreen() }
        composeTestRule.onNodeWithTag(ProfileTestTags.PROFILE_ACTIONS).assertIsDisplayed()
    }
}
