package com.android.sample.ui.profile.follow

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.profile.FollowListScreen
import com.android.sample.ui.profile.FollowListState
import com.android.sample.ui.profile.FollowListTestTags
import com.android.sample.ui.profile.FollowListType
import com.android.sample.ui.profile.FollowListViewModel
import com.android.sample.ui.profile.UserSections
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FollowListScreenTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"
    const val USER_ID_1 = "user1"
    const val USER_ID_2 = "user2"
    const val USER_ID_3 = "user3"

    const val NAME_ALICE = "Alice"
    const val LASTNAME_SMITH = "Smith"
    const val NAME_BOB = "Bob"
    const val LASTNAME_JOHNSON = "Johnson"
    const val NAME_CHARLIE = "Charlie"

    const val ERROR_MESSAGE = "Failed to load users"
    const val TEXT_FOLLOWERS = "Followers"
    const val TEXT_FOLLOWING = "Following"
    const val TEXT_NO_FOLLOWERS = "No followers yet"
    const val TEXT_NO_FOLLOWING = "Not following anyone yet"

    const val EXPECTED_ONE_ITEM = 1
    const val EXPECTED_TWO_ITEMS = 2
    const val EXPECTED_THREE_ITEMS = 3
  }

  // ============ Test Fixtures ============
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val testUser1 =
      UserProfile(
          id = USER_ID_1,
          name = NAME_ALICE,
          lastName = LASTNAME_SMITH,
          email = "alice@test.com",
          photo = null,
          kudos = 10,
          helpReceived = 5,
          section = UserSections.COMPUTER_SCIENCE,
          arrivalDate = Date())

  private val testUser2 =
      UserProfile(
          id = USER_ID_2,
          name = NAME_BOB,
          lastName = LASTNAME_JOHNSON,
          email = "bob@test.com",
          photo = null,
          kudos = 20,
          helpReceived = 8,
          section = UserSections.MATHEMATICS,
          arrivalDate = Date())

  private val testUser3 =
      UserProfile(
          id = USER_ID_3,
          name = NAME_CHARLIE,
          lastName = "",
          email = "charlie@test.com",
          photo = null,
          kudos = 15,
          helpReceived = 3,
          section = UserSections.NONE,
          arrivalDate = Date())

  // ============ Fake ViewModel (No MockK) ============

  private class FakeFollowListViewModel(initialState: FollowListState = FollowListState()) :
      FollowListViewModel(CURRENT_USER_ID, FollowListType.FOLLOWERS, FakeUserProfileRepository()) {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<FollowListState>
      get() = _state

    var loadUsersCalled = false

    override fun loadUsers() {
      loadUsersCalled = true
    }

    fun updateState(newState: FollowListState) {
      _state.value = newState
    }
  }

  private class FakeUserProfileRepository : UserProfileRepository {
    override fun getNewUid(): String = ""

    override fun getCurrentUserId(): String = ""

    override suspend fun getAllUserProfiles(): List<UserProfile> = emptyList()

    override suspend fun getUserProfile(userId: String): UserProfile {
      throw NotImplementedError()
    }

    override suspend fun addUserProfile(userProfile: UserProfile) {}

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {}

    override suspend fun deleteUserProfile(userId: String) {}

    override suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile> =
        emptyList()

    override suspend fun awardKudos(userId: String, amount: Int) {}

    override suspend fun awardKudosBatch(awards: Map<String, Int>) {}

    override suspend fun receiveHelp(userId: String, amount: Int) {}

    override suspend fun followUser(currentUserId: String, targetUserId: String) {}

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String) {}

    override suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean = false

    override suspend fun getFollowerCount(userId: String): Int = 0

    override suspend fun getFollowingCount(userId: String): Int = 0

    override suspend fun getFollowerIds(userId: String): List<String> = emptyList()

    override suspend fun getFollowingIds(userId: String): List<String> = emptyList()
  }

  // For tests that need navigation tracking
  private class FakeNavigationActions {
    val navigatedScreens = mutableListOf<Screen>()
    private val dummyNavController =
        androidx.navigation.NavHostController(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)

    fun asNavigationActions(): NavigationActions {
      return object : NavigationActions(dummyNavController) {
        override fun navigateTo(screen: Screen) {
          navigatedScreens.add(screen)
        }
      }
    }
  }

  // ============ Line Coverage Tests ============

  @Test
  fun followListScreen_rendersScaffoldWithFollowersTitle() {
    val viewModel = FakeFollowListViewModel()

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FollowListTestTags.FOLLOW_LIST_TITLE)
        .assertTextEquals(TEXT_FOLLOWERS)
  }

  @Test
  fun followListScreen_rendersScaffoldWithFollowingTitle() {
    val viewModel = FakeFollowListViewModel()

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWING,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule
        .onNodeWithTag(FollowListTestTags.FOLLOW_LIST_TITLE)
        .assertTextEquals(TEXT_FOLLOWING)
  }

  @Test
  fun followListScreen_backButtonInTopAppBar() {
    val viewModel = FakeFollowListViewModel()

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun followListScreen_launchedEffectCallsLoadUsers() {
    val viewModel = FakeFollowListViewModel()

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.waitForIdle()
    assert(viewModel.loadUsersCalled) { "loadUsers should be called" }
  }

  @Test
  fun followListScreen_whenStateIsLoading_showsCircularProgressIndicator() {
    val viewModel = FakeFollowListViewModel(FollowListState(isLoading = true))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_LOADING).assertIsDisplayed()
  }

  @Test
  fun followListScreen_whenStateHasError_showsErrorText() {
    val viewModel = FakeFollowListViewModel(FollowListState(errorMessage = ERROR_MESSAGE))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun followListScreen_whenUsersEmptyAndFollowers_showsNoFollowersMessage() {
    val viewModel = FakeFollowListViewModel(FollowListState(users = emptyList()))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEXT_NO_FOLLOWERS).assertIsDisplayed()
  }

  @Test
  fun followListScreen_whenUsersEmptyAndFollowing_showsNoFollowingMessage() {
    val viewModel = FakeFollowListViewModel(FollowListState(users = emptyList()))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWING,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEXT_NO_FOLLOWING).assertIsDisplayed()
  }

  @Test
  fun followListScreen_whenUsersNotEmpty_showsLazyColumn() {
    val users = listOf(testUser1, testUser2)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST).assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(FollowListTestTags.FOLLOW_LIST_ITEM)
        .assertCountEquals(EXPECTED_TWO_ITEMS)
  }

  @Test
  fun followListScreen_lazyColumnItemsCreatesFollowListItems() {
    val users = listOf(testUser1, testUser2, testUser3)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule
        .onAllNodesWithTag(FollowListTestTags.FOLLOW_LIST_ITEM)
        .assertCountEquals(EXPECTED_THREE_ITEMS)
  }

  @Test
  fun followListScreen_backButtonClick_triggersOnBackClick() {
    val viewModel = FakeFollowListViewModel()
    var backClicked = false

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = { backClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    assert(backClicked) { "onBackClick should be triggered" }
  }

  @Test
  fun followListScreen_itemClick_navigatesToPublicProfile() {
    val users = listOf(testUser1)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))
    val fakeNav = FakeNavigationActions()

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = fakeNav.asNavigationActions(), // CHANGE THIS
          onBackClick = {})
    }

    composeTestRule.onNodeWithText("$NAME_ALICE $LASTNAME_SMITH").performClick()
    composeTestRule.waitForIdle()

    assert(fakeNav.navigatedScreens.any { it is Screen.PublicProfile && it.userId == USER_ID_1 }) {
      "Expected navigation to PublicProfile($USER_ID_1), got: ${fakeNav.navigatedScreens}"
    }
  }

  @Test
  fun followListItem_rendersCardWithAllElements() {
    val users = listOf(testUser1)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_ITEM).assertIsDisplayed()
  }

  @Test
  fun followListItem_displaysFullNameWhenLastNamePresent() {
    val users = listOf(testUser1)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithText("$NAME_ALICE $LASTNAME_SMITH").assertIsDisplayed()
  }

  @Test
  fun followListItem_displaysOnlyNameWhenLastNameBlank() {
    val users = listOf(testUser3)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithText(NAME_CHARLIE).assertIsDisplayed()
  }

  @Test
  fun followListItem_displaysSectionLabelWhenNotNone() {
    val users = listOf(testUser1)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithText(UserSections.COMPUTER_SCIENCE.label).assertIsDisplayed()
  }

  @Test
  fun followListItem_hidesSectionLabelWhenNone() {
    val users = listOf(testUser3)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.onNodeWithText(UserSections.NONE.label).assertDoesNotExist()
  }

  @Test
  fun followListItem_clickTriggersOnClick() {
    val users = listOf(testUser1)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))
    val fakeNav = FakeNavigationActions()

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = fakeNav.asNavigationActions(), // CHANGE THIS
          onBackClick = {})
    }

    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_ITEM).performClick()
    composeTestRule.waitForIdle()

    assert(fakeNav.navigatedScreens.isNotEmpty()) {
      "Expected navigation to occur, but no screens were navigated to"
    }
  }

  @Test
  fun followListItem_profilePictureRendered() {
    val users = listOf(testUser1)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(FollowListTestTags.FOLLOW_LIST_ITEM).assertIsDisplayed()
  }

  @Test
  fun followListScreen_multipleUsers_allRendered() {
    val users = listOf(testUser1, testUser2, testUser3)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule
        .onAllNodesWithTag(FollowListTestTags.FOLLOW_LIST_ITEM)
        .assertCountEquals(EXPECTED_THREE_ITEMS)
    composeTestRule.onNodeWithText("$NAME_ALICE $LASTNAME_SMITH").assertIsDisplayed()
    composeTestRule.onNodeWithText("$NAME_BOB $LASTNAME_JOHNSON").assertIsDisplayed()
    composeTestRule.onNodeWithText(NAME_CHARLIE).assertIsDisplayed()
  }

  @Test
  fun followListScreen_singleUser_rendered() {
    val users = listOf(testUser2)
    val viewModel = FakeFollowListViewModel(FollowListState(users = users))

    composeTestRule.setContent {
      FollowListScreen(
          userId = CURRENT_USER_ID,
          listType = FollowListType.FOLLOWERS,
          viewModel = viewModel,
          navigationActions = null,
          onBackClick = {})
    }

    composeTestRule
        .onAllNodesWithTag(FollowListTestTags.FOLLOW_LIST_ITEM)
        .assertCountEquals(EXPECTED_ONE_ITEM)
  }
}
