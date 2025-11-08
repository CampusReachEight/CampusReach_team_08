package com.android.sample.ui.request

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
import com.android.sample.ui.theme.TopNavigationBar

// removed local magic number vals; use ConstantRequestList instead

object RequestListTestTags {
  const val ERROR_MESSAGE_DIALOG = "errorMessageDialog"

  const val OK_BUTTON_ERROR_DIALOG = "okButtonErrorDialog"

  const val REQUEST_ITEM = "requestItem"
  const val REQUEST_ITEM_TITLE = "requestItemTitle"
  const val REQUEST_ITEM_DESCRIPTION = "requestItemDescription"
  const val REQUEST_ITEM_ICON = "requestItemIcon"
  const val REQUEST_ITEM_NO_ICON = "requestItemNoIcon"
  const val EMPTY_LIST_MESSAGE = "emptyListMessage"

  const val REQUEST_SEARCH_BAR = "requestSearchBar"

  /** Tags for the filter dropdown buttons When clicked, they open the respective filter menus */
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

/** Request List screen scaffold: top bar, filters section, list, bottom bar, and error dialog. */
@Composable
fun RequestListScreen(
    modifier: Modifier = Modifier,
    requestListViewModel: RequestListViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val searchFilterViewModel: RequestSearchFilterViewModel = viewModel()

  LaunchedEffect(Unit) { requestListViewModel.loadRequests() }

  val icons by requestListViewModel.profileIcons.collectAsState()
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

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(NavigationTestTags.REQUESTS_SCREEN),
      topBar = {
        TopNavigationBar(
            selectedTab = NavigationTab.Requests,
            onProfileClick = { navigationActions?.navigateTo(Screen.Profile("TODO")) },
        )
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedNavigationTab = NavigationTab.Requests, navigationActions = navigationActions)
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

          // 2) Content list (filtered or base) - unchanged display
          val toShow = displayed.ifEmpty { state.requests }
          if (!state.isLoading && toShow.isEmpty()) {
            Text(
                text = "No requests at the moment",
                modifier =
                    Modifier.fillMaxSize()
                        .wrapContentSize()
                        .testTag(RequestListTestTags.EMPTY_LIST_MESSAGE),
                textAlign = TextAlign.Center)
          } else {
            RequestList(
                state = state.copy(requests = toShow),
                icons = icons,
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
                modifier = Modifier.fillMaxSize())
          }
        }
      }
}

// --- List rendering remains here to keep the screen self-contained visually ---
@Composable
fun RequestList(
    state: RequestListState,
    icons: Map<String, Bitmap?>,
    onRequestClick: (Request) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyColumn(modifier = modifier.padding(ConstantRequestList.ListPadding)) {
    items(state.requests.size) { index ->
      val request = state.requests[index]
      RequestListItem(request = request, icon = icons[request.creatorId], onClick = onRequestClick)
    }
  }
}

@Composable
fun RequestListItem(
    request: Request,
    icon: Bitmap?,
    onClick: (Request) -> Unit,
    modifier: Modifier = Modifier
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
      if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = "Photo profile",
            modifier =
                Modifier.size(ConstantRequestList.RequestItemIconSize)
                    .testTag(RequestListTestTags.REQUEST_ITEM_ICON))
      } else {
        Box(
            Modifier.size(ConstantRequestList.RequestItemIconSize)
                .testTag(RequestListTestTags.REQUEST_ITEM_NO_ICON))
      }
      Spacer(Modifier.width(ConstantRequestList.RowSpacing))
      Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
          Text(
              request.title,
              modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_TITLE).weight(1f))
          Text(
              request.requestType.toCompactLabel(max = 2),
              textAlign = TextAlign.End,
              maxLines = 1,
              overflow = TextOverflow.Clip)
        }
        Text(
            request.description,
            modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
fun AddButton(navigationActions: NavigationActions?) {
  FloatingActionButton(
      onClick = { navigationActions?.navigateTo(Screen.AddRequest) },
      modifier = Modifier.testTag(RequestListTestTags.REQUEST_ADD_BUTTON)) {
        Text("+")
      }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("An error occurred") },
      text = {
        Text(message, modifier = Modifier.testTag(RequestListTestTags.ERROR_MESSAGE_DIALOG))
      },
      confirmButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(RequestListTestTags.OK_BUTTON_ERROR_DIALOG)) {
              Text("OK")
            }
      })
}

private fun List<RequestType>.toCompactLabel(max: Int = 2): String {
  if (isEmpty()) return ""
  val head = take(max).joinToString(" | ") { it.displayString() }
  return if (size > max) "$head | ..." else head
}
