package com.android.sample.ui.myrequests

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.request.*
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class MyRequestsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    // Sample data for tests
    private val sampleRequest1 = Request(
        requestId = "req1",
        title = "Title 1",
        description = "Desc 1",
        requestType = listOf(RequestType.STUDY_GROUP),
        location = Location(0.0, 0.0, "Loc1"),
        locationName = "Loc1",
        status = RequestStatus.OPEN,
        startTimeStamp = Date(),
        expirationTime = Date(),
        people = listOf("user1"),
        tags = listOf(Tags.GROUP_WORK),
        creatorId = "me"
    )

    private val sampleRequest2 = sampleRequest1.copy(
        requestId = "req2",
        title = "Title 2",
        description = "Desc 2",
        requestType = listOf(RequestType.EATING)
    )

    // --- ViewModel Tests ---
    class TestViewModel(
        private val loadRequests: suspend () -> List<Request>,
        private val dispatcher: TestDispatcher
    ) : MyRequestsViewModel(loadRequests) {
        override val state: StateFlow<MyRequestState> get() = super.state

        init {
            // Override default dispatcher for coroutine
            viewModelScope.launch(dispatcher) {}
        }
    }

    @Test
    fun viewModel_initialState_isLoading() = runTest {
        val vm = MyRequestsViewModel { emptyList() }
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun viewModel_loadsRequests_successfully() = runTest {
        val requests = listOf(sampleRequest1, sampleRequest2)
        val vm = MyRequestsViewModel { requests }
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.errorMessage)
        assertEquals(requests, vm.state.value.myRequests)
    }

    @Test
    fun viewModel_loadRequests_fails_andShowsError() = runTest {
        val vm = MyRequestsViewModel { throw RuntimeException("Failed") }
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertEquals("Failed", vm.state.value.errorMessage)
        assertTrue(vm.state.value.myRequests.isEmpty())
    }

    @Test
    fun viewModel_refresh_reloadsData() = runTest {
        var count = 0
        val vm = MyRequestsViewModel {
            count++
            listOf(sampleRequest1)
        }
        advanceUntilIdle()
        assertEquals(1, count)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, count)
    }

    // --- MyRequestState Tests ---
    @Test
    fun myRequestState_factoryFunctions() {
        val loading = MyRequestState.loading()
        assertTrue(loading.isLoading)
        assertNull(loading.errorMessage)
        assertTrue(loading.myRequests.isEmpty())

        val error = MyRequestState.withError("err")
        assertFalse(error.isLoading)
        assertEquals("err", error.errorMessage)
        assertTrue(error.myRequests.isEmpty())

        val empty = MyRequestState.empty()
        assertFalse(empty.isLoading)
        assertNull(empty.errorMessage)
        assertTrue(empty.myRequests.isEmpty())
    }

    // --- UI Tests ---


    @Test
    fun screen_showsLoadingIndicator_whenLoading() {
        val vm = object : MyRequestsViewModel({ emptyList() }) {
            override val state = MutableStateFlow(MyRequestState.loading())
        }
        composeTestRule.setContent {
            MyRequestsScreen(myRequestsViewModel = vm)
        }

        composeTestRule.onNode(hasTestTag(MyRequestsTestTags.SCREEN)).assertIsDisplayed()
        composeTestRule.onNode(isDialog().not().and(hasTestTag(MyRequestsTestTags.REQUEST_ITEM).not()))
        composeTestRule.onNode(hasTestTag(MyRequestsTestTags.REQUEST_ADD_BUTTON)).assertIsDisplayed()
            .assertIsDisplayed()
    }

    @Test
    fun screen_showsErrorMessage_whenError() {
        val errorMessage = "Oops, error!"
        val vm = object : MyRequestsViewModel({ emptyList() }) {
            override val state = MutableStateFlow(MyRequestState.withError(errorMessage))
        }
        composeTestRule.setContent {
            MyRequestsScreen(myRequestsViewModel = vm)
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun screen_showsEmptyMessage_whenNoRequests() {
        val vm = object : MyRequestsViewModel({ emptyList() }) {
            override val state = MutableStateFlow(MyRequestState.empty())
        }
        composeTestRule.setContent {
            MyRequestsScreen(myRequestsViewModel = vm)
        }

        composeTestRule.onNodeWithText("You don't have any requests yet.")
            .assertIsDisplayed()
            .assert(hasTestTag(MyRequestsTestTags.EMPTY_MESSAGE))
    }

    @Test
    fun screen_showsListOfRequests_andItemsClickable() {
        val requests = listOf(sampleRequest1, sampleRequest2)
        val vm = object : MyRequestsViewModel({ requests }) {
            override val state = MutableStateFlow(MyRequestState(myRequests = requests))
        }
        val nav = mock(NavigationActions::class.java)
        composeTestRule.setContent {
            MyRequestsScreen(
                navigationActions = nav,
                myRequestsViewModel = vm
            )
        }

        composeTestRule.onAllNodesWithTag(MyRequestsTestTags.REQUEST_ITEM).assertCountEquals(requests.size)
        composeTestRule.onNodeWithText(sampleRequest1.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleRequest2.title).assertIsDisplayed()

        // Click the first item
        composeTestRule.onAllNodesWithTag(MyRequestsTestTags.REQUEST_ITEM)[0].performClick()
        verify(nav).navigateTo(Screen.EditRequest(sampleRequest1.requestId))
    }

    @Test
    fun screen_backButton_navigatesBack_orToProfile() {
        val nav = mock(NavigationActions::class.java)

        // Case 1: with profileId navigates to profile
        composeTestRule.setContent {
            MyRequestsScreen(profileId = "123", navigationActions = nav)
        }
        composeTestRule.onNodeWithContentDescription("Back to profile").performClick()
        verify(nav).navigateTo(Screen.Profile("123"))

        // Case 2: without profileId navigates back
        reset(nav)
        composeTestRule.setContent {
            MyRequestsScreen(profileId = "", navigationActions = nav)
        }
        composeTestRule.onNodeWithContentDescription("Back to profile").performClick()
        verify(nav).goBack()
    }

    @Test
    fun screen_fabButton_navigatesToAddRequest() {
        val nav = mock(NavigationActions::class.java)

        composeTestRule.setContent {
            MyRequestsScreen(navigationActions = nav)
        }

        composeTestRule.onNodeWithTag(MyRequestsTestTags.REQUEST_ADD_BUTTON)
            .assertIsDisplayed()
            .performClick()

        verify(nav).navigateTo(Screen.AddRequest)
    }
}
