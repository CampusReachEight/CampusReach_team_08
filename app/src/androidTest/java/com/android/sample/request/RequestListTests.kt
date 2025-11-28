package com.android.sample.request

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
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
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.ProfilePictureTestTags
import com.android.sample.ui.profile.UserSections
import com.android.sample.ui.request.RequestListScreen
import com.android.sample.ui.request.RequestListTestTags
import com.android.sample.ui.request.RequestListViewModel
import com.android.sample.ui.request.RequestSearchFilterTestTags
import com.android.sample.ui.theme.DarkPalette
import com.android.sample.ui.theme.LocalAppPalette
import com.android.sample.utils.BaseEmulatorTest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestListTests : BaseEmulatorTest() {

  companion object {
    const val ONE_HOUR_MS = 3_600_000L
    const val WAIT_TIMEOUT_MS = 5_000L
    const val LONG_SLEEP_MS = 10_000L
    const val COUNT_ZERO = 0
    const val COUNT_ONE = 1

    // This is the kicker aka the magic sauce
    const val COUNT_TWO = 2
    const val COUNT_THREE = 3
    const val DEFAULT_USER_ID = "test_current_user_id"

    const val OFFSET_1_S_MS = 1_000L
    const val OFFSET_2_S_MS = 2_000L
    const val OFFSET_5_S_MS = 5_000L
    const val OFFSET_10_S_MS = 10_000L
    const val OFFSET_11_S_MS = 11_000L
    const val OFFSET_12_S_MS = 12_000L
    const val OFFSET_20_S_MS = 20_000L
    const val OFFSET_30_S_MS = 30_000L
    const val OFFSET_110_S_MS = 110_000L
    const val OFFSET_120_S_MS = 120_000L
    const val OFFSET_130_S_MS = 130_000L
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  // Fake RequestRepository minimal
  private class FakeRequestRepository(private val requests: List<Request>) : RequestRepository {
    override fun getNewRequestId(): String = UUID.randomUUID().toString()

    override suspend fun getMyRequests(): List<Request> {
      val currentUid = Firebase.auth.currentUser?.uid ?: DEFAULT_USER_ID
      return requests.filter { it.creatorId == currentUid }
    }

    override suspend fun getAcceptedRequests(): List<Request> {
      return emptyList()
    }

    override suspend fun closeRequest(requestId: String, selectedHelperIds: List<String>): Boolean {
      return false
    }

    override suspend fun getAllRequests(): List<Request> = requests

    override suspend fun getAllCurrentRequests(): List<Request> =
        requests.filter { request ->
          request.viewStatus != RequestStatus.COMPLETED &&
              request.viewStatus != RequestStatus.CANCELLED
        }

    override suspend fun getRequest(requestId: String): Request =
        requests.first { it.requestId == requestId }

    override suspend fun addRequest(request: Request) {}

    override suspend fun updateRequest(requestId: String, updatedRequest: Request) {}

    override suspend fun deleteRequest(requestId: String) {}

    override fun hasUserAcceptedRequest(request: Request): Boolean = false

    override suspend fun acceptRequest(requestId: String) {}

    override suspend fun cancelAcceptance(requestId: String) {}

    override suspend fun isOwnerOfRequest(request: Request): Boolean = false
  }

  // Fake UserProfileRepository avec comptage
  private class FakeUserProfileRepository(
      private val withImage: Set<String> = emptySet(),
      private val failing: Set<String> = emptySet()
  ) : UserProfileRepository {
    var calls = 0
      private set

    override fun getNewUid(): String = UUID.randomUUID().toString()

    override fun getCurrentUserId(): String {
      return ""
    }

    override suspend fun getAllUserProfiles(): List<UserProfile> = emptyList()

    override suspend fun getUserProfile(userId: String): UserProfile {
      calls++
      if (userId in failing) throw IllegalStateException("Profile not found")
      val user =
          UserProfile(
              id = userId,
              name = "John",
              lastName = "Doe",
              email = null,
              photo = null,
              kudos = 0,
              section = UserSections.NONE,
              arrivalDate = Date())
      if (userId in withImage) {
        val uri =
            "https://lh3.googleusercontent.com/a/ACg8ocIb9J_JIRcgy6IyLyn13VDWBzB5GJ_FLrIjCQ7Nj_pcUoy2qK3H=s96-c"
        return user.copy(photo = Uri.parse(uri))
      }
      return user
    }

    override suspend fun addUserProfile(userProfile: UserProfile) {}

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {}

    override suspend fun deleteUserProfile(userId: String) {}

    override suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile> = listOf()

    override suspend fun awardKudos(userId: String, amount: Int) {
      return Unit
    }

    override suspend fun awardKudosBatch(awards: Map<String, Int>) {
      return Unit
    }
  }

  private fun sampleRequests(creatorIds: List<String>): List<Request> {
    val now = System.currentTimeMillis()
    // Use future start time so viewStatus computes OPEN (startTimeStamp > now)
    return creatorIds.mapIndexed { idx, creator ->
      Request(
          requestId = "req_${idx + COUNT_ONE}",
          title = "Title ${idx + COUNT_ONE}",
          description = "Description ${idx + COUNT_ONE}",
          requestType =
              listOf(
                  RequestType.OTHER,
                  RequestType.STUDYING,
                  RequestType.EATING,
                  RequestType.SPORT,
                  RequestType.HARDWARE,
                  RequestType.LOST_AND_FOUND,
                  RequestType.HANGING_OUT,
                  RequestType.STUDY_GROUP),
          location = Location(0.0, 0.0, "Loc"),
          locationName = "LocName",
          status = RequestStatus.OPEN,
          startTimeStamp = Date(now + ONE_HOUR_MS), // 1 hour from now
          // 2 hours from now BUT with a constants made from TWO constants ;)
          expirationTime = Date(now + COUNT_TWO * ONE_HOUR_MS),
          people = emptyList(),
          tags = listOf(Tags.INDOOR),
          creatorId = creator)
    }
  }

  private fun createBitmap(): Bitmap =
      Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

  /** Helper to extract the visible sequence of request titles (in list order). */
  private fun extractVisibleTitles(): List<String> {
    return composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") }
  }

  /** Helper to instantiate fake ViewModel for tests. */
  fun getFakeVm(requests: List<Request>) =
      RequestListViewModel(
          requestRepository =
              object : RequestRepository {
                override fun getNewRequestId(): String = "n/a"

                override suspend fun getAllRequests(): List<Request> = requests

                override suspend fun getAllCurrentRequests(): List<Request> =
                    requests.filter { request ->
                      request.viewStatus != RequestStatus.COMPLETED &&
                          request.viewStatus != RequestStatus.CANCELLED
                    }

                override suspend fun getRequest(requestId: String): Request =
                    requests.first { it.requestId == requestId }

                override suspend fun addRequest(request: Request) {}

                override suspend fun updateRequest(requestId: String, updatedRequest: Request) {}

                override suspend fun deleteRequest(requestId: String) {}

                override fun hasUserAcceptedRequest(request: Request): Boolean = false

                override suspend fun acceptRequest(requestId: String) {}

                override suspend fun cancelAcceptance(requestId: String) {}

                override suspend fun isOwnerOfRequest(request: Request): Boolean = false

                override suspend fun getMyRequests(): List<Request> {
                  return emptyList()
                }

                override suspend fun getAcceptedRequests(): List<Request> {
                  return emptyList()
                }

                override suspend fun closeRequest(
                    requestId: String,
                    selectedHelperIds: List<String>
                ): Boolean {
                  return false
                }
              },
          profileRepository = FakeUserProfileRepository())

  @Test
  fun displaysTitlesAndDescriptions() {
    val requests = sampleRequests(listOf("u1", "u2", "u3"))
    val vm = RequestListViewModel(FakeRequestRepository(requests), FakeUserProfileRepository())

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
    val repoProfile = FakeUserProfileRepository()
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
            FakeRequestRepository(emptyList()), FakeUserProfileRepository(failing = setOf(bad)))
    vm.loadProfileImage(bad)
    composeTestRule.waitUntil(5_000) { vm.profileIcons.value.containsKey(bad) }
    assert(vm.profileIcons.value[bad] == null) { "Expected null icon" }
  }

  @Test
  fun emptyListShowsMessage() {
    val vm = RequestListViewModel(FakeRequestRepository(emptyList()), FakeUserProfileRepository())
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
    val profileRepo = FakeUserProfileRepository(failing = setOf(fail))
    val vm = RequestListViewModel(FakeRequestRepository(requests), profileRepo)
    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.profileIcons.value.containsKey(fail) }

    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_LIST, useUnmergedTree = true)
        .onChildren()
        .filter(hasAnyDescendant(hasTestTag(ProfilePictureTestTags.PROFILE_PICTURE_DEFAULT)))
        .assertCountEquals(COUNT_ONE)
  }

  @Test
  fun displaysCorrectNumberOfRequests() {
    val requests = sampleRequests(listOf("u1", "u2", "u3", "u4", "u5"))
    val vm = RequestListViewModel(FakeRequestRepository(requests), FakeUserProfileRepository())

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == requests.size }

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
    val vm = RequestListViewModel(FakeRequestRepository(requests), FakeUserProfileRepository())

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == COUNT_ONE }

    val request = requests[0]
    composeTestRule.onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM).assertCountEquals(COUNT_ONE)

    composeTestRule.onNodeWithText(request.title).assertExists()
    composeTestRule.onNodeWithText(request.description).assertExists()
  }

  @Test
  fun multipleRequestsDisplayAllContent() {
    val requests = sampleRequests(listOf("u1", "u2", "u3"))
    val vm = RequestListViewModel(FakeRequestRepository(requests), FakeUserProfileRepository())

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == requests.size }

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertCountEquals(COUNT_THREE)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .assertCountEquals(COUNT_THREE)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION, useUnmergedTree = true)
        .assertCountEquals(COUNT_THREE)
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_LIST, useUnmergedTree = true)
        .onChildren()
        .filter(hasAnyDescendant(hasTestTag(ProfilePictureTestTags.PROFILE_PICTURE_DEFAULT)))
        .assertCountEquals(COUNT_THREE)
  }

  @Test
  fun multipleRequestsDisplayAllContentDarkMode() {
    val requests = sampleRequests(listOf("u1", "u2", "u3"))
    val vm = RequestListViewModel(FakeRequestRepository(requests), FakeUserProfileRepository())

    composeTestRule.setContent {
      CompositionLocalProvider(LocalAppPalette provides DarkPalette) {
        RequestListScreen(requestListViewModel = vm)
      }
    }

    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == requests.size }

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM)
        .assertCountEquals(COUNT_THREE)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .assertCountEquals(COUNT_THREE)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION, useUnmergedTree = true)
        .assertCountEquals(COUNT_THREE)
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_LIST, useUnmergedTree = true)
        .onChildren()
        .filter(hasAnyDescendant(hasTestTag(ProfilePictureTestTags.PROFILE_PICTURE_DEFAULT)))
        .assertCountEquals(COUNT_THREE)
  }

  @Test
  fun filterButtons_showTitles_and_openMenus() {
    val vm = RequestListViewModel(FakeRequestRepository(emptyList()), FakeUserProfileRepository())

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()

    val typeTitle = RequestType.toString()
    val statusTitle = RequestStatus.toString()
    val tagsTitle = Tags.toString()

    Thread.sleep(LONG_SLEEP_MS)

    // Titles selected by default
    composeTestRule.onNodeWithText(typeTitle).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText(statusTitle).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText(tagsTitle).assertExists().assertIsDisplayed()

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
    val now = System.currentTimeMillis()
    val request =
        Request(
            requestId = "req_1",
            title = "Title 1",
            description = "Description 1",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Loc"),
            locationName = "LocName",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
            expirationTime = Date(now + 2 * ONE_HOUR_MS),
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

    composeTestRule.onNodeWithText("$typeTitle ($COUNT_ONE)").assertExists().assertIsDisplayed()
  }

  @Test
  fun dropdownSearch_filtersOptions_locally() {
    val vm = RequestListViewModel(FakeRequestRepository(emptyList()), FakeUserProfileRepository())

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
    composeTestRule.onAllNodesWithTag(outdoorTag).assertCountEquals(COUNT_ZERO)
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

      override suspend fun getAllCurrentRequests(): List<Request> {
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

      override suspend fun isOwnerOfRequest(request: Request): Boolean = false

      override suspend fun getMyRequests(): List<Request> {
        return emptyList()
      }

      override suspend fun getAcceptedRequests(): List<Request> {
        return emptyList()
      }

      override suspend fun closeRequest(
          requestId: String,
          selectedHelperIds: List<String>
      ): Boolean {
        return false
      }
    }

    val vm = RequestListViewModel(FailingRequestRepository(), FakeUserProfileRepository())

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    // Wait until error is set and ensure the dialog content is shown
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.errorMessage != null }
    composeTestRule.onNodeWithTag(RequestListTestTags.ERROR_MESSAGE_DIALOG).assertIsDisplayed()
  }

  @Test
  fun search_bar_filters_results_and_clear_restores_all() {
    val requests =
        listOf(
            Request(
                requestId = "1",
                title = "Study group calculus",
                description = "desc",
                requestType = listOf(RequestType.STUDY_GROUP),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "Loc",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(System.currentTimeMillis()),
                expirationTime = Date(System.currentTimeMillis() + OFFSET_10_S_MS),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "creator-1"),
            Request(
                requestId = "2",
                title = "Pizza night",
                description = "desc",
                requestType = listOf(RequestType.EATING),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "Loc",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(System.currentTimeMillis() + OFFSET_1_S_MS),
                expirationTime = Date(System.currentTimeMillis() + OFFSET_11_S_MS),
                people = listOf("u1", "u2"),
                tags = listOf(Tags.INDOOR),
                creatorId = "creator-2"),
            Request(
                requestId = "3",
                title = "Football practice",
                description = "desc",
                requestType = listOf(RequestType.SPORT),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "Loc",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(System.currentTimeMillis() + OFFSET_2_S_MS),
                expirationTime = Date(System.currentTimeMillis() + OFFSET_12_S_MS),
                people = listOf("u1", "u2", "u3"),
                tags = listOf(Tags.INDOOR),
                creatorId = "creator-3"),
        )

    val vm = getFakeVm(requests)

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    // Wait until base requests loaded (Lucene index builds asynchronously afterwards)
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == requests.size }

    // Enter a query that should narrow to a single item ("pizza")
    composeTestRule
        .onNodeWithTag(RequestListTestTags.REQUEST_SEARCH_BAR)
        .assertExists()
        .assertIsDisplayed()
        .performTextInput("pizza")

    // Wait for debounce + indexing (~300ms). Poll until only 1 title remains
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .size == COUNT_ONE
    }

    val afterSearch = extractVisibleTitles()
    assert(
        afterSearch.size == COUNT_ONE && afterSearch.first().contains("Pizza", ignoreCase = true)) {
          "Expected only 'Pizza night', got $afterSearch"
        }

    // Clear button becomes visible when query not empty
    composeTestRule.onNodeWithText("Clear").assertIsDisplayed().performClick()

    // All items should reappear (3)
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
      composeTestRule
          .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .size == COUNT_THREE
    }
  }

  @Test
  fun sorting_changes_order_based_on_selected_criteria() {
    val now = System.currentTimeMillis()
    val requests =
        listOf(
            Request(
                requestId = "a",
                title = "C Charlie",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "Loc",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + OFFSET_30_S_MS),
                expirationTime = Date(now + OFFSET_130_S_MS),
                people = listOf("u1", "u2"),
                tags = listOf(Tags.INDOOR),
                creatorId = "creator-a"),
            Request(
                requestId = "b",
                title = "A Alpha",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "Loc",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + OFFSET_10_S_MS),
                expirationTime = Date(now + OFFSET_110_S_MS),
                people = listOf("u1", "u2", "u3", "u4", "u5"),
                tags = listOf(Tags.INDOOR),
                creatorId = "creator-b"),
            Request(
                requestId = "c",
                title = "B Bravo",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "Loc",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + OFFSET_20_S_MS),
                expirationTime = Date(now + OFFSET_120_S_MS),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "creator-c"),
        )

    val vm = getFakeVm(requests)

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == requests.size }

    // Default sort: NEWEST_START (descending startTime) => C (30s), B (20s), A (10s)
    val defaultOrder = extractVisibleTitles()
    assert(defaultOrder.first().startsWith("C")) {
      "Expected newest start first (C), got $defaultOrder"
    }

    composeTestRule
        .onNodeWithTag(RequestSearchFilterTestTags.SORT_BUTTON)
        .assertExists()
        .assertIsDisplayed()
        .performClick()

    // Select Title Asc
    composeTestRule
        .onNodeWithText("Title ascending", ignoreCase = true)
        .assertIsDisplayed()
        .performClick()

    // Verify alphabetical order: A, B, C
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
      val titles = extractVisibleTitles()
      titles == titles.sortedBy { it.lowercase() }
    }

    // Switch to Most participants
    composeTestRule.onNodeWithTag(RequestSearchFilterTestTags.SORT_BUTTON).performClick()
    composeTestRule.onNodeWithText("Most participants", ignoreCase = true).performClick()

    // Expect request with 5 participants (A Alpha) first.
    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { extractVisibleTitles().first().startsWith("A") }
  }

  @Test
  fun loadsProfileImagesSuccessfully() {
    val requests =
        sampleRequests(listOf("special_profile1", "special_profile2", "special_profile3"))
    val vm =
        RequestListViewModel(
            FakeRequestRepository(requests),
            FakeUserProfileRepository(
                withImage = setOf("special_profile1", "special_profile2", "special_profile3")))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(OFFSET_5_S_MS) {
      composeTestRule
          .onNodeWithTag(RequestListTestTags.REQUEST_LIST, useUnmergedTree = true)
          .onChildren()
          .filter(hasAnyDescendant(hasTestTag(ProfilePictureTestTags.PROFILE_PICTURE)))
          .fetchSemanticsNodes()
          .size == COUNT_THREE
    }
  }

  @Test
  fun loadsCachedProfileImagesSuccessfully() {
    val requests = sampleRequests(listOf("cached_profile1", "cached_profile2", "cached_profile3"))
    val vm =
        RequestListViewModel(
            FakeRequestRepository(requests),
            FakeUserProfileRepository(
                withImage = setOf("cached_profile1", "cached_profile2", "cached_profile3")))

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(OFFSET_5_S_MS) {
      composeTestRule
          .onAllNodesWithTag(ProfilePictureTestTags.PROFILE_PICTURE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .size == 3
    }
  }

  // Add these tests to your RequestListTests class

  @Test
  fun showOnlyMyRequests_displaysOnlyCurrentUserRequests() {
    val now = System.currentTimeMillis()
    // Create requests with different creators
    val myRequest =
        Request(
            requestId = "my_req_1",
            title = "My Request",
            description = "Created by me",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Loc"),
            locationName = "LocName",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
            expirationTime = Date(now + 2 * ONE_HOUR_MS),
            people = emptyList(),
            tags = listOf(Tags.INDOOR),
            creatorId = currentUserId // Current user's request
            )

    val otherRequest =
        Request(
            requestId = "other_req_1",
            title = "Other Request",
            description = "Created by someone else",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Loc"),
            locationName = "LocName",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
            expirationTime = Date(now + 2 * ONE_HOUR_MS),
            people = emptyList(),
            tags = listOf(Tags.INDOOR),
            creatorId = "other_user" // Different user
            )

    val allRequests = listOf(myRequest, otherRequest)

    // Create FakeRepository that returns all requests
    val repository = FakeRequestRepository(allRequests)

    // Create ViewModel with showOnlyMyRequests = true
    val vm =
        RequestListViewModel(
            requestRepository = repository,
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = true)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = true)
    }

    // Wait for requests to load
    composeTestRule.waitUntil(5_000) { vm.state.value.requests.isNotEmpty() }

    // Verify only current user's request is displayed
    composeTestRule.onNodeWithText("My Request").assertExists()
    composeTestRule.onNodeWithText("Other Request").assertDoesNotExist()

    // Verify count is 1
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertCountEquals(1)
  }

  @Test
  fun showOnlyMyRequests_displaysBackButton() {
    val vm =
        RequestListViewModel(
            requestRepository = FakeRequestRepository(emptyList()),
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = true)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = true)
    }

    composeTestRule.waitForIdle()

    // Verify "My Requests" title is displayed
    composeTestRule.onNodeWithText("My Requests").assertExists().assertIsDisplayed()

    // Verify back button exists (by content description)
    composeTestRule.onNodeWithContentDescription("Back").assertExists()
  }

  @Test
  fun showOnlyMyRequests_hidesBottomNavigation() {
    val vm =
        RequestListViewModel(
            requestRepository = FakeRequestRepository(emptyList()),
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = true)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = true)
    }

    composeTestRule.waitForIdle()

    // Bottom navigation should not be present when showOnlyMyRequests is true
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertDoesNotExist()
  }

  @Test
  fun showOnlyMyRequests_false_displaysAllRequests() {
    val now = System.currentTimeMillis()
    val myRequest =
        Request(
            requestId = "my_req_1",
            title = "My Request",
            description = "Created by me",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Loc"),
            locationName = "LocName",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
            expirationTime = Date(now + 2 * ONE_HOUR_MS),
            people = emptyList(),
            tags = listOf(Tags.INDOOR),
            creatorId = currentUserId)

    val otherRequest =
        Request(
            requestId = "other_req_1",
            title = "Other Request",
            description = "Created by someone else",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Loc"),
            locationName = "LocName",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
            expirationTime = Date(now + 2 * ONE_HOUR_MS),
            people = emptyList(),
            tags = listOf(Tags.INDOOR),
            creatorId = "other_user")

    val allRequests = listOf(myRequest, otherRequest)
    val repository = FakeRequestRepository(allRequests)

    // Create ViewModel with showOnlyMyRequests = false
    val vm =
        RequestListViewModel(
            requestRepository = repository,
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = false)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = false)
    }

    composeTestRule.waitUntil(5_000) { vm.state.value.requests.size == 2 }

    // Verify both requests are displayed
    composeTestRule.onNodeWithText("My Request").assertExists()
    composeTestRule.onNodeWithText("Other Request").assertExists()

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertCountEquals(2)
  }

  @Test
  fun showOnlyMyRequests_emptyList_displaysCustomMessage() {
    val vm =
        RequestListViewModel(
            requestRepository = FakeRequestRepository(emptyList()),
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = true)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = true)
    }

    composeTestRule.waitForIdle()

    // Verify the custom empty message for My Requests
    composeTestRule
        .onNodeWithText("You don't have any requests yet")
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun showOnlyMyRequests_false_emptyList_displaysGenericMessage() {
    val vm =
        RequestListViewModel(
            requestRepository = FakeRequestRepository(emptyList()),
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = false)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = false)
    }

    composeTestRule.waitForIdle()

    // Verify the generic empty message for All Requests
    composeTestRule.onNodeWithText("No requests at the moment").assertExists().assertIsDisplayed()
  }

  @Test
  fun showOnlyMyRequests_multipleUserRequests_displaysAll() {
    val now = System.currentTimeMillis()
    // Create multiple requests from the current user
    val requests =
        listOf(
            Request(
                requestId = "my_req_1",
                title = "My First Request",
                description = "First",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
                expirationTime = Date(now + 2 * ONE_HOUR_MS),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = currentUserId),
            Request(
                requestId = "my_req_2",
                title = "My Second Request",
                description = "Second",
                requestType = listOf(RequestType.SPORT),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
                expirationTime = Date(now + 2 * ONE_HOUR_MS),
                people = emptyList(),
                tags = listOf(Tags.OUTDOOR),
                creatorId = currentUserId),
            Request(
                requestId = "other_req",
                title = "Other Request",
                description = "Someone else",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + ONE_HOUR_MS), // Future start for OPEN viewStatus
                expirationTime = Date(now + 2 * ONE_HOUR_MS),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "other_user"))

    val vm =
        RequestListViewModel(
            requestRepository = FakeRequestRepository(requests),
            profileRepository = FakeUserProfileRepository(),
            showOnlyMyRequests = true)

    composeTestRule.setContent {
      RequestListScreen(requestListViewModel = vm, showOnlyMyRequests = true)
    }

    composeTestRule.waitUntil(OFFSET_5_S_MS) { vm.state.value.requests.size == 2 }

    composeTestRule.onNodeWithText("My First Request").assertExists()
    composeTestRule.onNodeWithText("My Second Request").assertExists()
    composeTestRule.onNodeWithText("Other Request").assertDoesNotExist()

    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM, useUnmergedTree = true)
        .assertCountEquals(2)
    fun loadsProfileNameSuccessfully() {
      val requests = sampleRequests(listOf("special_profile4"))
      val vm =
          RequestListViewModel(
              FakeRequestRepository(requests),
              FakeUserProfileRepository(withImage = setOf("special_profile4")))

      composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }
      composeTestRule.waitForIdle()
      composeTestRule.waitUntil(OFFSET_5_S_MS) {
        composeTestRule
            .onAllNodesWithText("John", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size == 1
      }
    }
  }

  @Test
  fun filtering_excludes_nonOpenAndInProgressStatuses() {
    val now = System.currentTimeMillis()
    val hour = ONE_HOUR_MS
    val requests =
        listOf(
            // OPEN: startTimeStamp > now (future start)
            Request(
                requestId = "r_open",
                title = "Open Req",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(now + hour), // Future start for OPEN viewStatus
                expirationTime = Date(now + 2 * hour),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "c1"),
            // IN_PROGRESS: startTimeStamp <= now, expirationTime > now
            Request(
                requestId = "r_inprog",
                title = "In Progress Req",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.IN_PROGRESS,
                startTimeStamp = Date(now - 1000), // Past start for IN_PROGRESS viewStatus
                expirationTime = Date(now + hour),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "c2"),
            // COMPLETED: expirationTime <= now
            Request(
                requestId = "r_completed",
                title = "Completed Req",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.COMPLETED,
                startTimeStamp = Date(now - 2 * hour), // Past dates for COMPLETED viewStatus
                expirationTime = Date(now - hour), // Expired
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "c3"),
            // CANCELLED: preserved regardless of dates
            Request(
                requestId = "r_cancelled",
                title = "Cancelled Req",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.CANCELLED,
                startTimeStamp = Date(now + hour),
                expirationTime = Date(now + 2 * hour),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "c4"),
            // ARCHIVED: preserved regardless of dates
            Request(
                requestId = "r_archived",
                title = "Archived Req",
                description = "desc",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Loc"),
                locationName = "LocName",
                status = RequestStatus.ARCHIVED,
                startTimeStamp = Date(now + hour),
                expirationTime = Date(now + 2 * hour),
                people = emptyList(),
                tags = listOf(Tags.INDOOR),
                creatorId = "c5"))
    val vm = RequestListViewModel(FakeRequestRepository(requests), FakeUserProfileRepository())

    composeTestRule.setContent { RequestListScreen(requestListViewModel = vm) }

    composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { vm.state.value.requests.size == requests.size }

    // Only 2 requests should be displayed (OPEN + IN_PROGRESS)
    composeTestRule
        .onAllNodesWithTag(RequestListTestTags.REQUEST_ITEM_TITLE, useUnmergedTree = true)
        .assertCountEquals(2)
    composeTestRule.onNodeWithText("Open Req").assertExists()
    composeTestRule.onNodeWithText("In Progress Req").assertExists()
    composeTestRule.onNodeWithText("Completed Req").assertDoesNotExist()
    composeTestRule.onNodeWithText("Cancelled Req").assertDoesNotExist()
    composeTestRule.onNodeWithText("Archived Req").assertDoesNotExist()
  }
}
