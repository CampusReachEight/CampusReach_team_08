package com.android.sample.request

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import com.android.sample.model.profile.Section
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.model.request.displayString
import com.android.sample.ui.request.RequestListScreen
import com.android.sample.ui.request.RequestListTestTags
import com.android.sample.ui.request.RequestListViewModel
import com.android.sample.utils.BaseEmulatorTest
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestListTests : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  // Fake RequestRepository minimal
  private class FakeRequestRepository(private val requests: List<Request>) : RequestRepository {
    override fun getNewRequestId(): String = UUID.randomUUID().toString()

    override suspend fun getAllRequests(): List<Request> = requests

    override suspend fun getRequest(requestId: String): Request =
        requests.first { it.requestId == requestId }

    override suspend fun addRequest(request: Request) {}

    override suspend fun updateRequest(requestId: String, updatedRequest: Request) {}

    override suspend fun deleteRequest(requestId: String) {}

    override fun hasUserAcceptedRequest(request: Request): Boolean = false

    override suspend fun acceptRequest(requestId: String) {}

    override suspend fun cancelAcceptance(requestId: String) {}
  }

  // Fake UserProfileRepository avec comptage
  private class FakeUserProfileRepository(
      private val bitmap: Bitmap?,
      private val failing: Set<String> = emptySet()
  ) : UserProfileRepository {
    var calls = 0
      private set

    override fun getNewUid(): String = UUID.randomUUID().toString()

    override suspend fun getAllUserProfiles(): List<UserProfile> = emptyList()

    override suspend fun getUserProfile(userId: String): UserProfile {
      calls++
      if (userId in failing) throw IllegalStateException("Profile not found")
      return UserProfile(
          id = userId,
          name = "John",
          lastName = "Doe",
          email = null,
          photo = null,
          kudos = 0,
          section = Section.OTHER,
          arrivalDate = Date())
    }

    override suspend fun addUserProfile(userProfile: UserProfile) {}

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {}

    override suspend fun deleteUserProfile(userId: String) {}
  }

  private fun sampleRequests(creatorIds: List<String>): List<Request> {
    val now = Date()
    return creatorIds.mapIndexed { idx, creator ->
      Request(
          requestId = "req_${idx + 1}",
          title = "Title ${idx + 1}",
          description = "Description ${idx + 1}",
          requestType = listOf(RequestType.OTHER),
          location = Location(0.0, 0.0, "Loc"),
          locationName = "LocName",
          status = RequestStatus.OPEN,
          startTimeStamp = now,
          expirationTime = Date(now.time + 3_600_000),
          people = emptyList(),
          tags = listOf(Tags.INDOOR),
          creatorId = creator)
    }
  }

  private fun createBitmap(): Bitmap =
      Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

  @Test
  fun displaysTitlesAndDescriptions() {
    val requests = sampleRequests(listOf("u1", "u2", "u3"))
    val vm =
        RequestListViewModel(
            FakeRequestRepository(requests), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(5_000) { vm.state.value.requests.size == requests.size }

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .assertCountEquals(requests.size)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION, useUnmergedTree = true)
        .assertCountEquals(requests.size)

    requests.forEach { r ->
      composeTestRule.onNodeWithText(r.title).assertExists()
      composeTestRule.onNodeWithText(r.description).assertExists()
    }
  }

  @Test
  fun cacheDoesNotReloadTwice() = runBlocking {
    val user = "userX"
    val repoProfile = FakeUserProfileRepository(createBitmap())
    val vm = RequestListViewModel(FakeRequestRepository(emptyList()), repoProfile)

    vm.loadProfileImage(user)
    composeTestRule.waitUntil(5_000) { vm.profileIcons.value.containsKey(user) }
    val calls = repoProfile.calls
    vm.loadProfileImage(user)
    Thread.sleep(150)
    assert(repoProfile.calls == calls) { "Profile reloaded unexpectedly" }
  }

  @Test
  fun failureStoresNullIcon() {
    val bad = "badUser"
    val vm =
        RequestListViewModel(
            FakeRequestRepository(emptyList()),
            FakeUserProfileRepository(createBitmap(), failing = setOf(bad)))
    vm.loadProfileImage(bad)
    composeTestRule.waitUntil(5_000) { vm.profileIcons.value.containsKey(bad) }
    assert(vm.profileIcons.value[bad] == null) { "Expected null icon" }
  }

  @Test
  fun emptyListShowsMessage() {
    val vm =
        RequestListViewModel(
            FakeRequestRepository(emptyList()), FakeUserProfileRepository(createBitmap()))
    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()

    // Vérifier le message vide avec TestTag
    composeTestRule
        .onNodeWithTag(RequestListTestTags.EMPTY_LIST_MESSAGE)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun failedIconNoImageDisplayed() {
    val fail = "failUser"
    val requests = sampleRequests(listOf(fail))
    val profileRepo = FakeUserProfileRepository(createBitmap(), failing = setOf(fail))
    val vm = RequestListViewModel(FakeRequestRepository(requests), profileRepo)
    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitUntil(5_000) { vm.profileIcons.value.containsKey(fail) }

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_ICON, useUnmergedTree = true)
        .assertCountEquals(0)
  }

  @Test
  fun displaysCorrectNumberOfRequests() {
    val requests = sampleRequests(listOf("u1", "u2", "u3", "u4", "u5"))
    val vm =
        RequestListViewModel(
            FakeRequestRepository(requests), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(5_000) { vm.state.value.requests.size == requests.size }

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertCountEquals(requests.size)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .assertCountEquals(requests.size)
  }

  @Test
  fun requestItemsHaveCorrectContent() {
    val requests = sampleRequests(listOf("user1"))
    val vm =
        RequestListViewModel(
            FakeRequestRepository(requests), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(5_000) { vm.state.value.requests.size == 1 }

    val request = requests[0]
    composeTestRule.onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM).assertCountEquals(1)

    composeTestRule.onNodeWithText(request.title).assertExists()
    composeTestRule.onNodeWithText(request.description).assertExists()
  }

  @Test
  fun multipleRequestsDisplayAllContent() {
    val requests = sampleRequests(listOf("u1", "u2", "u3"))
    val vm =
        RequestListViewModel(
            FakeRequestRepository(requests), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(5_000) { vm.state.value.requests.size == requests.size }

    composeTestRule.onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM).assertCountEquals(3)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .assertCountEquals(3)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION, useUnmergedTree = true)
        .assertCountEquals(3)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_NO_ICON, useUnmergedTree = true)
        .assertCountEquals(3)
  }

  @Test
  fun filterButtons_showTitles_and_openMenus() {
    val vm =
        RequestListViewModel(
            FakeRequestRepository(emptyList()), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()

    val typeTitle = RequestType.toString()
    val statusTitle = RequestStatus.toString()
    val tagsTitle = Tags.toString()

    // Titles with (0) selected by default
    composeTestRule.onNodeWithText("$typeTitle (0)").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("$statusTitle (0)").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("$tagsTitle (0)").assertExists().assertIsDisplayed()

    // Open each menu and verify the per-menu search bar appears (no crash on click)
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()
    composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR).assertExists()

    // Close it by clicking again
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON)
        .performClick()

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR)
        .assertExists()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON)
        .performClick()

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()
    composeTestRule.onNodeWithTag(RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR).assertExists()
  }

  @Test
  fun selectingType_updatesSelectedCount_inHeader() {
    val request =
        Request(
            requestId = "req_1",
            title = "Title 1",
            description = "Description 1",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Loc"),
            locationName = "LocName",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(),
            expirationTime = Date(System.currentTimeMillis() + 3_600_000),
            people = emptyList(),
            tags = listOf(Tags.INDOOR),
            creatorId = currentUserId)
    val repository = FakeRequestRepository(listOf(request))
    val vm = RequestListViewModel(repository)

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()

    val typeTitle = RequestType.toString()

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()

    val otherTag = RequestListTestTags.getRequestTypeFilterTag(RequestType.OTHER)
    composeTestRule.onNodeWithTag(otherTag).assertExists().performClick()

    composeTestRule.onNodeWithText("$typeTitle (1)").assertExists().assertIsDisplayed()
  }

  @Test
  fun dropdownSearch_filtersOptions_locally() {
    val vm =
        RequestListViewModel(
            FakeRequestRepository(emptyList()), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()

    // Open Tags menu and search for "ind" (should match "Indoor")
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON)
        .performClick()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR)
        .performTextInput("ind")

    val indoorTag = RequestListTestTags.getRequestTagFilterTag(Tags.INDOOR.displayString())
    val outdoorTag = RequestListTestTags.getRequestTagFilterTag(Tags.OUTDOOR.displayString())

    composeTestRule.onNodeWithTag(indoorTag).assertExists()
    composeTestRule.onAllNodesWithTag(outdoorTag).assertCountEquals(0)
  }

  @Test
  fun filterButtons_openMenus_withFirestoreRepo_noCrash() {
    val vm =
        RequestListViewModel(RequestRepositoryFirestore(db), UserProfileRepositoryFirestore(db))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()

    // Open Type menu and verify search bar appears
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR)
        .assertExists()
        .assertIsDisplayed()

    // Open Status menu and verify search bar appears
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR)
        .assertExists()
        .assertIsDisplayed()

    // Open Tags menu and verify search bar appears
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON)
        .assertExists()
        .performClick()
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun errorDialog_shown_whenLoadFails() {
    // Failing repository triggers ViewModel to set errorMessage
    class FailingRequestRepository : RequestRepository {
      override fun getNewRequestId(): String = "n/a"

      override suspend fun getAllRequests(): List<Request> {
        throw RuntimeException("Simulated load failure")
      }

      override suspend fun getRequest(requestId: String): Request = throw NotImplementedError()

      override suspend fun addRequest(request: Request) = throw NotImplementedError()

      override suspend fun updateRequest(requestId: String, updatedRequest: Request) =
          throw NotImplementedError()

      override suspend fun deleteRequest(requestId: String) = throw NotImplementedError()

      override fun hasUserAcceptedRequest(request: Request): Boolean = false

      override suspend fun acceptRequest(requestId: String) = throw NotImplementedError()

      override suspend fun cancelAcceptance(requestId: String) = throw NotImplementedError()
    }

    val vm =
        RequestListViewModel(FailingRequestRepository(), FakeUserProfileRepository(createBitmap()))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    // Wait until error is set and ensure the dialog content is shown
    composeTestRule.waitUntil(5_000) { vm.state.value.errorMessage != null }
    composeTestRule.onNodeWithTag(RequestListTestTags.ERROR_MESSAGE_DIALOG).assertIsDisplayed()
  }
}
