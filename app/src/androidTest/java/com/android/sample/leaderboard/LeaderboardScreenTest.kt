package com.android.sample.leaderboard

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.leaderboard.LeaderboardScreen
import com.android.sample.ui.leaderboard.LeaderboardSearchFilterViewModel
import com.android.sample.ui.leaderboard.LeaderboardTestTags
import com.android.sample.ui.leaderboard.LeaderboardViewModel
import com.android.sample.ui.profile.UserSections
import java.util.Date
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeaderboardScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val profiles =
      listOf(
          UserProfile(
              id = "u1",
              name = "Alice",
              lastName = "Anderson",
              email = null,
              photo = null,
              kudos = 120,
              helpReceived = 5,
              section = UserSections.COMPUTER_SCIENCE,
              arrivalDate = Date(0)),
          UserProfile(
              id = "u2",
              name = "Bob",
              lastName = "Baker",
              email = null,
              photo = null,
              kudos = 60,
              helpReceived = 2,
              section = UserSections.PHYSICS,
              arrivalDate = Date(0)),
          UserProfile(
              id = "u3",
              name = "Charlie",
              lastName = "Clark",
              email = null,
              photo = null,
              kudos = 15,
              helpReceived = 20,
              section = UserSections.COMPUTER_SCIENCE,
              arrivalDate = Date(0)))

  private class FakeUserProfileRepository(private val data: List<UserProfile>) :
      UserProfileRepository {
    override fun getNewUid(): String = UUID.randomUUID().toString()

    override fun getCurrentUserId(): String = "current"

    override suspend fun getAllUserProfiles(): List<UserProfile> = data

    override suspend fun getUserProfile(userId: String): UserProfile =
        data.first { it.id == userId }

    override suspend fun addUserProfile(userProfile: UserProfile) {}

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {}

    override suspend fun deleteUserProfile(userId: String) {}

    override suspend fun awardKudos(userId: String, amount: Int) {}

    override suspend fun awardKudosBatch(awards: Map<String, Int>) {}

    override suspend fun receiveHelp(userId: String, amount: Int) {}
  }

  private fun launchScreen(repo: UserProfileRepository = FakeUserProfileRepository(profiles)) {
    val vm = LeaderboardViewModel(profileRepository = repo, verboseLogging = true)
    composeTestRule.setContent {
      LeaderboardScreen(
          leaderboardViewModel = vm, searchFilterViewModel = LeaderboardSearchFilterViewModel())
    }
  }

  private fun waitForCards(expected: Int) {
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(LeaderboardTestTags.LEADERBOARD_CARD, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .size == expected
    }
  }

  @Test
  fun showsCardsWithDynamicTags() {
    launchScreen()
    waitForCards(expected = profiles.size)

    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.getCardTag("u1"), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(LeaderboardTestTags.LEADERBOARD_CARD, useUnmergedTree = true)
        .assertCountEquals(profiles.size)
  }

  @Test
  fun searchFiltersByName() {
    launchScreen()
    waitForCards(expected = profiles.size)

    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.SEARCH_BAR, useUnmergedTree = true)
        .performTextInput("Bob")

    waitForCards(expected = 1)
    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.getCardTag("u2"), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun sectionFilterNarrowsResults() {
    launchScreen()
    waitForCards(expected = profiles.size)

    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.SECTION_FILTER_DROPDOWN_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule
        .onNodeWithTag(
            LeaderboardTestTags.getSectionFilterTag(UserSections.COMPUTER_SCIENCE),
            useUnmergedTree = true)
        .performClick()

    waitForCards(expected = 2)
    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.getCardTag("u1"), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun rangeFilterAppliesKudosBounds() {
    launchScreen()
    waitForCards(expected = profiles.size)

    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.KUDOS_RANGE_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.KUDOS_RANGE_MIN_FIELD, useUnmergedTree = true)
        .performTextReplacement("80")

    waitForCards(expected = 1)
    composeTestRule
        .onNodeWithTag(LeaderboardTestTags.getCardTag("u1"), useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
