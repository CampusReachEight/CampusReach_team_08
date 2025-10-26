package com.android.sample.ui.request

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.theme.BottomNavigationMenu
import com.android.sample.ui.theme.NavigationTab
import com.android.sample.ui.theme.TopNavigationBar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

// removed local magic number vals; use ConstantRequestList instead

object RequestListTestTags {
  const val REQUEST_ITEM = "requestItem"
  const val REQUEST_ITEM_TITLE = "requestItemTitle"
  const val REQUEST_ITEM_DESCRIPTION = "requestItemDescription"
  const val REQUEST_ITEM_ICON = "requestItemIcon"
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

private enum class FilterKind {
  Type,
  Status,
  Tags
}

@Composable
fun RequestListScreen(
    modifier: Modifier = Modifier,
    requestListViewModel: RequestListViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  LaunchedEffect(Unit) { requestListViewModel.loadRequests() }

  val icons by requestListViewModel.profileIcons.collectAsState()

  // Collect facet state and counts
  val selectedTypes by requestListViewModel.selectedTypes.collectAsState()
  val selectedStatuses by requestListViewModel.selectedStatuses.collectAsState()
  val selectedTags by requestListViewModel.selectedTags.collectAsState()
  val typeCounts by requestListViewModel.typeCounts.collectAsState()
  val statusCounts by requestListViewModel.statusCounts.collectAsState()
  val tagCounts by requestListViewModel.tagCounts.collectAsState()
  val filtered by requestListViewModel.filteredRequests.collectAsState()

  var openMenu by rememberSaveable { mutableStateOf<FilterKind?>(null) }

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
        val state by requestListViewModel.state.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          // Mock global search bar (UI only)
          RequestSearchBar(
              modifier =
                  Modifier.fillMaxWidth().padding(horizontal = ConstantRequestList.PaddingLarge))

          Spacer(modifier = Modifier.height(ConstantRequestList.PaddingMedium))

          // Horizontal filter bar
          Row(
              modifier =
                  Modifier.fillMaxWidth().padding(horizontal = ConstantRequestList.PaddingLarge),
              horizontalArrangement = Arrangement.spacedBy(ConstantRequestList.RowSpacing)) {
                FilterMenuButton(
                    title = RequestType.toString(),
                    selectedCount = selectedTypes.size,
                    testTag = RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON,
                    onClick = {
                      openMenu = if (openMenu == FilterKind.Type) null else FilterKind.Type
                    },
                    modifier = Modifier.weight(1f))
                FilterMenuButton(
                    title = RequestStatus.toString(),
                    selectedCount = selectedStatuses.size,
                    testTag = RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON,
                    onClick = {
                      openMenu = if (openMenu == FilterKind.Status) null else FilterKind.Status
                    },
                    modifier = Modifier.weight(1f))
                FilterMenuButton(
                    title = Tags.toString(),
                    selectedCount = selectedTags.size,
                    testTag = RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON,
                    onClick = {
                      openMenu = if (openMenu == FilterKind.Tags) null else FilterKind.Tags
                    },
                    modifier = Modifier.weight(1f))
              }

          // Single full-width expanded panel below buttons
          when (openMenu) {
            FilterKind.Type -> {
              FilterMenuPanel(
                  title = RequestType.toString(),
                  values = RequestType.entries.toTypedArray(),
                  selected = selectedTypes,
                  counts = typeCounts,
                  labelOf = { it.displayString() },
                  onToggle = { requestListViewModel.toggleType(it) },
                  dropdownSearchBarTestTag = RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR,
                  rowTestTagOf = { RequestListTestTags.getRequestTypeFilterTag(it) })
            }
            FilterKind.Status -> {
              FilterMenuPanel(
                  title = RequestStatus.toString(),
                  values = RequestStatus.entries.toTypedArray(),
                  selected = selectedStatuses,
                  counts = statusCounts,
                  labelOf = { it.displayString() },
                  onToggle = { requestListViewModel.toggleStatus(it) },
                  dropdownSearchBarTestTag = RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR,
                  rowTestTagOf = {
                    RequestListTestTags.getRequestStatusFilterTag(it.displayString())
                  })
            }
            FilterKind.Tags -> {
              FilterMenuPanel(
                  title = Tags.toString(),
                  values = Tags.entries.toTypedArray(),
                  selected = selectedTags,
                  counts = tagCounts,
                  labelOf = { it.displayString() },
                  onToggle = { requestListViewModel.toggleTag(it) },
                  dropdownSearchBarTestTag = RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR,
                  rowTestTagOf = { RequestListTestTags.getRequestTagFilterTag(it.displayString()) })
            }
            null -> {}
          }

          Spacer(modifier = Modifier.height(ConstantRequestList.PaddingLarge))

          // Content list (filtered)
          val toShow = if (filtered.isNotEmpty()) filtered else state.requests
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
                  Firebase.auth.currentUser?.uid?.let { id ->
                    if (it.creatorId == id) {
                      navigationActions?.navigateTo(Screen.EditRequest(it.requestId))
                      return@RequestList
                    }
                    navigationActions?.navigateTo(Screen.RequestAccept(it.requestId))
                  }
                },
                modifier = Modifier.fillMaxSize())
          }
        }
      }
}

@Composable
private fun FilterMenuButton(
    title: String,
    selectedCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  OutlinedButton(
      onClick = onClick,
      modifier =
          modifier.fillMaxWidth().height(ConstantRequestList.FilterButtonHeight).testTag(testTag)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
          Text("$title ($selectedCount)", modifier = Modifier.weight(1f))
          Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }
      }
}

@Composable
private fun <E : Enum<E>> FilterMenuPanel(
    title: String,
    values: Array<E>,
    selected: Set<E>,
    counts: Map<E, Int>,
    labelOf: (E) -> String,
    onToggle: (E) -> Unit,
    dropdownSearchBarTestTag: String,
    rowTestTagOf: (E) -> String
) {
  // Full-width panel below buttons
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = ConstantRequestList.PaddingLarge)) {
        Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))
        Surface(
            shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, shadowElevation = 2.dp) {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(ConstantRequestList.PaddingMedium)) {
                    var localQuery by rememberSaveable { mutableStateOf("") }
                    OutlinedTextField(
                        value = localQuery,
                        onValueChange = { localQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag(dropdownSearchBarTestTag),
                        singleLine = true,
                        placeholder = { Text("Search options") })

                    Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))

                    val filteredValues =
                        remember(localQuery, values, counts) {
                          values
                              .filter { labelOf(it).contains(localQuery, ignoreCase = true) }
                              .sortedByDescending { counts[it] ?: 0 }
                        }

                    Box(modifier = Modifier.heightIn(max = ConstantRequestList.DropdownMaxHeight)) {
                      Column(
                          modifier =
                              Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            filteredValues.forEach { v ->
                              val isChecked = selected.contains(v)
                              val count = counts[v] ?: 0

                              Row(
                                  modifier =
                                      Modifier.fillMaxWidth()
                                          .clickable { onToggle(v) }
                                          .padding(
                                              horizontal =
                                                  ConstantRequestList.FilterRowHorizontalPadding)
                                          .testTag(rowTestTagOf(v))
                                          .height(ConstantRequestList.FilterRowHeight),
                                  horizontalArrangement = Arrangement.Start) {
                                    Checkbox(checked = isChecked, onCheckedChange = null)
                                    Spacer(
                                        modifier = Modifier.width(ConstantRequestList.RowSpacing))
                                    Text(text = labelOf(v))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(text = count.toString())
                                  }
                            }
                          }
                    }
                  }
            }
      }
}

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
        Box(Modifier.size(ConstantRequestList.RequestItemIconSize))
      }
      Spacer(Modifier.width(ConstantRequestList.RowSpacing))
      Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
          Text(
              request.title,
              modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_TITLE).weight(1f))
          Text(request.requestType.toString(), textAlign = TextAlign.End)
        }
        Text(
            request.description,
            modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION))
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
private fun RequestSearchBar(modifier: Modifier = Modifier) {
  var query by rememberSaveable { mutableStateOf("") }
  OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      modifier = modifier.testTag(RequestListTestTags.REQUEST_SEARCH_BAR),
      singleLine = true,
      placeholder = { Text("Search") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <E : Enum<E>> FilterMenu(
    title: String,
    values: Array<E>,
    selected: Set<E>,
    counts: Map<E, Int>,
    labelOf: (E) -> String,
    onToggle: (E) -> Unit,
    dropdownButtonTestTag: String,
    dropdownSearchBarTestTag: String,
    rowTestTagOf: (E) -> String,
    modifier: Modifier = Modifier
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  var localQuery by rememberSaveable { mutableStateOf("") }

  Column(modifier = modifier) {
    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier =
            Modifier.fillMaxWidth()
                .height(ConstantRequestList.FilterButtonHeight)
                .testTag(dropdownButtonTestTag)) {
          val selCount = selected.size
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text("${'$'}title (${'$'}selCount)", modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
          }
        }

    if (expanded) {
      Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))
      Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(ConstantRequestList.PaddingMedium)) {
          OutlinedTextField(
              value = localQuery,
              onValueChange = { localQuery = it },
              modifier = Modifier.fillMaxWidth().testTag(dropdownSearchBarTestTag),
              singleLine = true,
              placeholder = { Text("Search options") })

          Spacer(modifier = Modifier.height(ConstantRequestList.PaddingSmall))

          val filteredValues =
              remember(localQuery, values) {
                values.filter { labelOf(it).contains(localQuery, ignoreCase = true) }
              }

          Box(modifier = Modifier.heightIn(max = ConstantRequestList.DropdownMaxHeight)) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
              filteredValues.forEach { v ->
                val isChecked = selected.contains(v)
                val count = counts[v] ?: 0

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { onToggle(v) }
                            .padding(horizontal = ConstantRequestList.FilterRowHorizontalPadding)
                            .testTag(rowTestTagOf(v))
                            .height(ConstantRequestList.FilterRowHeight),
                    horizontalArrangement = Arrangement.Start) {
                      Checkbox(checked = isChecked, onCheckedChange = null)
                      Spacer(modifier = Modifier.width(ConstantRequestList.RowSpacing))
                      Text(text = labelOf(v))
                      Spacer(modifier = Modifier.weight(1f))
                      Text(text = count.toString())
                    }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RequestTypeFilterMenu(
    selected: Set<RequestType>,
    counts: Map<RequestType, Int>,
    onToggle: (RequestType) -> Unit,
    modifier: Modifier = Modifier
) {
  FilterMenu(
      title = RequestType.toString(),
      values = RequestType.entries.toTypedArray(),
      selected = selected,
      counts = counts,
      labelOf = { it.displayString() },
      onToggle = onToggle,
      dropdownButtonTestTag = RequestListTestTags.REQUEST_TYPE_FILTER_DROPDOWN_BUTTON,
      dropdownSearchBarTestTag = RequestListTestTags.REQUEST_TYPE_FILTER_SEARCH_BAR,
      rowTestTagOf = { RequestListTestTags.getRequestTypeFilterTag(it) },
      modifier = modifier)
}

@Composable
private fun RequestStatusFilterMenu(
    selected: Set<RequestStatus>,
    counts: Map<RequestStatus, Int>,
    onToggle: (RequestStatus) -> Unit,
    modifier: Modifier = Modifier
) {
  FilterMenu(
      title = RequestStatus.toString(),
      values = RequestStatus.entries.toTypedArray(),
      selected = selected,
      counts = counts,
      labelOf = { it.displayString() },
      onToggle = onToggle,
      dropdownButtonTestTag = RequestListTestTags.REQUEST_STATUS_FILTER_DROPDOWN_BUTTON,
      dropdownSearchBarTestTag = RequestListTestTags.REQUEST_STATUS_FILTER_SEARCH_BAR,
      rowTestTagOf = { RequestListTestTags.getRequestStatusFilterTag(it.displayString()) },
      modifier = modifier)
}

@Composable
private fun RequestTagsFilterMenu(
    selected: Set<Tags>,
    counts: Map<Tags, Int>,
    onToggle: (Tags) -> Unit,
    modifier: Modifier = Modifier
) {
  FilterMenu(
      title = Tags.toString(),
      values = Tags.entries.toTypedArray(),
      selected = selected,
      counts = counts,
      labelOf = { it.displayString() },
      onToggle = onToggle,
      dropdownButtonTestTag = RequestListTestTags.REQUEST_TAG_FILTER_DROPDOWN_BUTTON,
      dropdownSearchBarTestTag = RequestListTestTags.REQUEST_TAG_FILTER_SEARCH_BAR,
      rowTestTagOf = { RequestListTestTags.getRequestTagFilterTag(it.displayString()) },
      modifier = modifier)
}

@Preview
@Composable
fun RequestListItemPreview() {
  val sampleRequest =
      Request(
          requestId = "1",
          title = "Sample Request",
          description = "This is a sample request description.",
          creatorId = "user1",
          location = Location(0.0, 0.0, "knowhere"),
          locationName = "No where",
          requestType = listOf(),
          status = com.android.sample.model.request.RequestStatus.OPEN,
          startTimeStamp = java.util.Date(),
          expirationTime = java.util.Date(),
          people = listOf(),
          tags = listOf())
  RequestListItem(request = sampleRequest, icon = null, onClick = {})
}
