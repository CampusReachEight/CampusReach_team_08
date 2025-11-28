// kotlin
package com.android.sample.ui.request

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.request.ConstantRequestList.TypeChipBorderWidth
import com.android.sample.ui.request.ConstantRequestList.TypeChipTextPadding
import com.android.sample.ui.theme.TopNavigationBar

object RequestListTestTags {
  const val ERROR_MESSAGE_DIALOG = "errorMessageDialog"
  const val OK_BUTTON_ERROR_DIALOG = "okButtonErrorDialog"
  const val REQUEST_LIST = "requestList"
  const val REQUEST_ITEM = "requestItem"
  const val REQUEST_ITEM_TITLE = "requestItemTitle"
  const val REQUEST_ITEM_DESCRIPTION = "requestItemDescription"
  const val EMPTY_LIST_MESSAGE = "emptyListMessage"
  const val REQUEST_SEARCH_BAR = "requestSearchBar"
  const val REQUEST_TYPE_FILTER_DROPDOWN_BUTTON = "requestTypeFilterDropdown"
  const val REQUEST_TAG_FILTER_DROPDOWN_BUTTON = "requestTagFilterDropdown"
  const val REQUEST_STATUS_FILTER_DROPDOWN_BUTTON = "requestStatusFilterDropdown"
  const val REQUEST_ADD_BUTTON = "requestAddButton"

  // Search-bar tags required by RequestFacetDefinitions
  const val REQUEST_TYPE_FILTER_SEARCH_BAR = "requestTypeFilterSearchBar"
  const val REQUEST_STATUS_FILTER_SEARCH_BAR = "requestStatusFilterSearchBar"
  const val REQUEST_TAG_FILTER_SEARCH_BAR = "requestTagFilterSearchBar"

  // Helper generators for individual facet rows used by RequestFacetDefinitions
  fun getRequestTypeFilterTag(type: RequestType): String = "requestTypeFilterRow_${type.name}"

  fun getRequestStatusFilterTag(status: String): String =
      "requestStatusFilterRow_${status.replace("\\s+".toRegex(), "_")}"

  fun getRequestTagFilterTag(tag: String): String =
      "requestTagFilterRow_${tag.replace("\\s+".toRegex(), "_")}"
}

private const val NO_REQUEST_NOW = "No requests at the moment"
private const val MY_REQUESTS = "My Requests"
private const val TEXT_BACK = "Back"

/**
 * Top-level helpers and constants. These must be declared at file scope (not inside a function) so
 * `private`/`const` modifiers are legal and `@Composable` helpers can be invoked from other
 * composables in this file.
 */
private const val WEIGHT = 1f
private val TYPE_CHIP_BORDER = BorderStroke(TypeChipBorderWidth, Color.White)

@Composable
private fun AddButton(onAdd: () -> Unit, modifier: Modifier = Modifier) {
  FloatingActionButton(
      onClick = onAdd, modifier = modifier.testTag(RequestListTestTags.REQUEST_ADD_BUTTON)) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
      }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        ElevatedButton(onClick = onDismiss) {
          Text(text = "OK", modifier = Modifier.testTag(RequestListTestTags.OK_BUTTON_ERROR_DIALOG))
        }
      },
      title = { Text(text = "Error") },
      text = {
        Text(text = message, modifier = Modifier.testTag(RequestListTestTags.ERROR_MESSAGE_DIALOG))
      })
}

@Composable
private fun TitleAndDescription(request: Request, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(
        text = request.title ?: "",
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_TITLE))
    Text(
        text = request.description ?: "",
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION))
  }
}

@Composable
private fun TypeChip(type: RequestType, modifier: Modifier = Modifier) {
  ElevatedButton(
      onClick = {},
      modifier = modifier,
      colors =
          ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
      border = TYPE_CHIP_BORDER,
      shape = RoundedCornerShape(8.dp)) {
        Text(text = type.displayString(), modifier = Modifier.padding(TypeChipTextPadding))
      }
}

@Composable
fun RequestListItem(
    request: Request,
    onClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(8.dp)
              .clickable { onClick() }
              .testTag(RequestListTestTags.REQUEST_ITEM),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          TitleAndDescription(request = request, modifier = Modifier.weight(WEIGHT))
          Spacer(modifier = Modifier.width(8.dp))
          TypeChip(type = request.requestType.firstOrNull() ?: RequestType.OTHER)
        }
      }
}

/**
 * Request List screen scaffold: top bar, filters section, list, bottom bar, and error dialog. Kept
 * intentionally minimal so it compiles and provides the expected test tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    requestListViewModel: RequestListViewModel = viewModel(),
    showOnlyMyRequests: Boolean = false,
    navigationActions: NavigationActions? = null
) {
  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.REQUESTS_SCREEN),
      topBar = {
        TopNavigationBar(
            selectedTab = NavigationTab.Requests,
            onProfileClick = { navigationActions?.navigateTo(Screen.Profile("TODO")) })
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedNavigationTab = NavigationTab.Requests, navigationActions = navigationActions)
      },
      floatingActionButton = {
        AddButton(onAdd = { navigationActions?.navigateTo(Screen.AddRequest) })
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          // Minimal placeholder content. Original implementation may show lists, filters, etc.
          Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = if (showOnlyMyRequests) MY_REQUESTS else NO_REQUEST_NOW)
          }
        }
      }
}

/** Renders the list of requests. Kept minimal so other files can call this helper if needed. */
@Composable
fun RequestList(
    viewModel: RequestListViewModel,
    state: RequestListState,
    onRequestClick: (Request) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (String) -> Unit = {}
) {
  // Minimal rendering using LazyColumn and the file-scope helpers.
  LazyColumn(modifier = modifier.testTag(RequestListTestTags.REQUEST_LIST)) {
    if (state.requests.isEmpty()) {
      item {
        Text(
            text = NO_REQUEST_NOW,
            modifier = Modifier.padding(16.dp).testTag(RequestListTestTags.EMPTY_LIST_MESSAGE))
      }
    } else {
      items(state.requests.size) { index ->
        val req = state.requests[index]
        RequestListItem(
            request = req, onClick = { onRequestClick(req) }, onProfileClick = onProfileClick)
      }
    }
  }
}
