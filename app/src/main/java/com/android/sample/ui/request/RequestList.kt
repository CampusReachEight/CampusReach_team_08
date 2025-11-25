package com.android.sample.ui.request

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.theme.TopNavigationBar
import com.google.firebase.auth.FirebaseAuth

// removed local magic number vals; use ConstantRequestList instead

object RequestListTestTags {
  const val ERROR_MESSAGE_DIALOG = "errorMessageDialog"

  const val OK_BUTTON_ERROR_DIALOG = "okButtonErrorDialog"

  const val REQUEST_LIST = "requestList"

  const val REQUEST_ITEM = "requestItem"
  const val REQUEST_ITEM_TITLE = "requestItemTitle"
  const val REQUEST_ITEM_DESCRIPTION = "requestItemDescription"
  const val EMPTY_LIST_MESSAGE = "emptyListMessage"

  const val REQUEST_SEARCH_BAR = "requestSearchBar"

  /**
   * Tags for the filter dropdown buttons When clicked, they open the respective filter dropdown
   * menus
   */
  const val REQUEST_TYPE_FILTER_DROPDOWN_BUTTON = "requestTypeFilterDropdown"

  const val REQUEST_TAG_FILTER_DROPDOWN_BUTTON = "requestTagFilterDropdown"

  const val REQUEST_STATUS_FILTER_DROPDOWN_BUTTON = "requestStatusFilterDropdown"

  const val REQUEST_ADD_BUTTON = "requestAddButton"

  /**
   * Tags for the search bar of each dropdown menu These allow users to search within the filter
   * options
   */
  const val REQUEST_TYPE_FILTER_SEARCH_BAR = "requestTypeFilterSearchBar"
  const val REQUEST_TAG_FILTER_SEARCH_BAR = "requestTagFilterSearchBar"
  const val REQUEST_STATUS_FILTER_SEARCH_BAR = "requestStatusFilterSearchBar"

  /**
   * Generates a tag for a given filter type and value within dropdown menus. These tags are
   * dynamically created based on filter values.
   */
  private fun getFilterTag(type: String, value: String): String = "${type}Filter_$value"

  fun getRequestTypeFilterTag(requestType: RequestType): String =
      getFilterTag("requestType", requestType.displayString())

  fun getRequestTagFilterTag(tag: String): String = getFilterTag("requestTag", tag)

  fun getRequestStatusFilterTag(status: String): String = getFilterTag("requestStatus", status)
}

private const val NO_REQUEST_NOW = "No requests at the moment"

private const val NO_REQUEST_YET = "You don't have any requests yet"

private const val MY_REQUESTS = "My Requests"

private const val TEXT_BACK = "Back"

private const val TEXT_TODO = "TODO"

/** Request List screen scaffold: top bar, filters section, list, bottom bar, and error dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    modifier: Modifier = Modifier,
    showOnlyMyRequests: Boolean = false,
    requestListViewModel: RequestListViewModel =
        viewModel(factory = RequestListViewModelFactory(showOnlyMyRequests = showOnlyMyRequests)),
    navigationActions: NavigationActions? = null,
) {
  val searchFilterViewModel: RequestSearchFilterViewModel = viewModel()
  LaunchedEffect(Unit) { requestListViewModel.loadRequests() }

  val state by requestListViewModel.state.collectAsState()

  // Keep Lucene index in sync with loaded requests
  LaunchedEffect(state.requests) {
    if (state.requests.isNotEmpty()) {
      searchFilterViewModel.initializeWithRequests(state.requests)
    }
  }

  // Collect search/filter state
  val displayed by searchFilterViewModel.displayedRequests.collectAsState()
  val searchQuery by searchFilterViewModel.searchQuery.collectAsState()
  val isSearching by searchFilterViewModel.isSearching.collectAsState()
  val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(NavigationTestTags.REQUESTS_SCREEN),
      topBar = {
        if (showOnlyMyRequests) {
          // Simple back button for My Requests
          TopAppBar(
              title = { Text(MY_REQUESTS) },
              navigationIcon = {
                IconButton(onClick = { navigationActions?.goBack() }) {
                  Icon(Icons.Default.ArrowBack, contentDescription = TEXT_BACK)
                }
              })
        } else {
          // Full navigation bar for All Requests
          TopNavigationBar(
              selectedTab = NavigationTab.Requests,
              onProfileClick = { navigationActions?.navigateTo(Screen.Profile(TEXT_TODO)) },
          )
        }
      },
      bottomBar = {
        if (!showOnlyMyRequests) {
          BottomNavigationMenu(
              selectedNavigationTab = NavigationTab.Requests, navigationActions = navigationActions)
        }
      },
      floatingActionButton = { AddButton(navigationActions) }) { innerPadding ->

        // Error dialog when present
        state.errorMessage?.let { msg ->
          ErrorDialog(message = msg, onDismiss = { requestListViewModel.clearError() })
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          // 1) Filters block (moved to RequestSearchFilter.kt)
          FiltersSection(
              searchFilterViewModel = searchFilterViewModel,
              query = searchQuery,
              isSearching = isSearching,
              onQueryChange = { searchFilterViewModel.updateSearchQuery(it) },
              onClearQuery = { searchFilterViewModel.clearSearch() })

          Spacer(modifier = Modifier.height(ConstantRequestList.PaddingLarge))

          // Always show the sorted/filtered list from the filter ViewModel
          val toShow = displayed

          if (!state.isLoading && toShow.isEmpty()) {
            Text(
                text = if (showOnlyMyRequests) NO_REQUEST_YET else NO_REQUEST_NOW,
                modifier =
                    Modifier.fillMaxSize()
                        .wrapContentSize()
                        .testTag(RequestListTestTags.EMPTY_LIST_MESSAGE),
                textAlign = TextAlign.Center)
          } else {
            RequestList(
                state = state.copy(requests = toShow),
                onRequestClick = {
                  requestListViewModel.handleRequestClick(
                      request = it,
                      onNavigateEdit = { id ->
                        navigationActions?.navigateTo(Screen.EditRequest(id))
                      },
                      onNavigateAccept = { id ->
                        navigationActions?.navigateTo(Screen.RequestAccept(id))
                      })
                },
                modifier = Modifier.fillMaxSize(),
                viewModel = requestListViewModel,
                onProfileClick = { id ->
                  if (id != currentUserId) {
                    navigationActions?.navigateTo(Screen.PublicProfile(id))
                  } else {
                    navigationActions?.navigateTo(Screen.Profile(id))
                  }
                })
          }
        }
      }
}

/** Renders the list of requests. */
@Composable
fun RequestList(
    viewModel: RequestListViewModel,
    state: RequestListState,
    onRequestClick: (Request) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (String) -> Unit = {}
) {
  LazyColumn(
      modifier =
          modifier
              .padding(ConstantRequestList.ListPadding)
              .testTag(RequestListTestTags.REQUEST_LIST)) {
        items(state.requests.size) { index ->
          val request = state.requests[index]
          RequestListItem(
              viewModel = viewModel,
              request = request,
              onClick = onRequestClick,
              onProfileClick = onProfileClick)
        }
      }
}

private const val WEIGHT = 1f

private const val MAX_PARAM = 2

/** One request list item: title, compact type labels, truncated description, and optional icon. */
@Composable
fun RequestListItem(
    viewModel: RequestListViewModel,
    request: Request,
    onClick: (Request) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (String) -> Unit = {}
) {
  Card(
      modifier =
          modifier
              .padding(bottom = ConstantRequestList.ListItemSpacing)
              .fillMaxWidth()
              .clickable(onClick = { onClick(request) })
              .testTag(RequestListTestTags.REQUEST_ITEM),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(ConstantRequestList.RequestItemInnerPadding)) {
      ProfilePicture(
          profileRepository = viewModel.profileRepository,
          profileId = request.creatorId,
          onClick = { onProfileClick(request.creatorId) },
          modifier =
              Modifier.size(ConstantRequestList.RequestItemCreatorSectionSize)
                  .align(Alignment.CenterVertically),
          withName = true)
      Spacer(Modifier.width(ConstantRequestList.RowSpacing))
      Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
          Text(
              request.title,
              modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_TITLE).weight(WEIGHT))
          Text(
              request.requestType.toCompactLabel(max = MAX_PARAM),
              textAlign = TextAlign.End,
              maxLines = 1,
              overflow = TextOverflow.Clip)
        }
        Text(
            request.description,
            modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION),
            maxLines = MAX_PARAM,
            overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

/** Floating add button to navigate to the add-request screen. */
@Composable
fun AddButton(navigationActions: NavigationActions?) {
  FloatingActionButton(
      onClick = { navigationActions?.navigateTo(Screen.AddRequest) },
      modifier = Modifier.testTag(RequestListTestTags.REQUEST_ADD_BUTTON)) {
        Text("+")
      }
}

private const val DIALOG_ERROR_OCCURED = "An error occurred"

private const val DIALOG_OK = "OK"

/** Simple alert dialog to surface user-facing errors. */
@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(DIALOG_ERROR_OCCURED) },
      text = {
        Text(message, modifier = Modifier.testTag(RequestListTestTags.ERROR_MESSAGE_DIALOG))
      },
      confirmButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(RequestListTestTags.OK_BUTTON_ERROR_DIALOG)) {
              Text(DIALOG_OK)
            }
      })
}

/** Formats a list of request types into a compact label like `SPORT | EATING | ...`. */
private fun List<RequestType>.toCompactLabel(max: Int = MAX_PARAM): String {
  if (isEmpty()) return ""
  val head = take(max).joinToString(" | ") { it.displayString() }
  return if (size > max) "$head | ..." else head
}

// Preview for rendering improvements during development
// @Preview
// @Composable
// fun RequestListItemPreview() {
//  val sampleRequest =
//      Request(
//          requestId = "1",
//          title = "Sample Request",
//          description = "This is a sample request description.",
//          creatorId = "user1",
//          location = com.android.sample.model.map.Location(0.0, 0.0, "knowhere"),
//          locationName = "No where",
//          requestType = RequestType.entries, // 2 values should appear and the rest should be
// truncated
//          status = RequestStatus.OPEN,
//          startTimeStamp = java.util.Date(),
//          expirationTime = java.util.Date(),
//          people = listOf(),
//          tags = listOf())
//  RequestListItem(request = sampleRequest, icon = null, onClick = {})
// }
