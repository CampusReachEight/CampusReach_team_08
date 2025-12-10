package com.android.sample.ui.request

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestCache
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.navigation.TopNavigationBar
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.request.ConstantRequestList.TypeChipBorderWidth
import com.android.sample.ui.request.ConstantRequestList.TypeChipColumnSpacing
import com.android.sample.ui.request.ConstantRequestList.TypeChipCornerRadius
import com.android.sample.ui.request.ConstantRequestList.TypeChipTextPadding
import com.android.sample.ui.request.ConstantRequestList.TypeChipTextSize
import com.android.sample.ui.request.ConstantRequestList.TypeChipTextSizeFactor
import com.android.sample.ui.theme.appPalette

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

  const val LOADING_INDICATOR = "loadingIndicator"

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
        viewModel(
            factory =
                RequestListViewModelFactory(
                    showOnlyMyRequests = showOnlyMyRequests,
                    requestCache = RequestCache(LocalContext.current))),
    navigationActions: NavigationActions? = null,
) {
  val searchFilterViewModel: RequestSearchFilterViewModel = viewModel()
  LaunchedEffect(Unit) { requestListViewModel.loadRequests() }

  val state by requestListViewModel.state.collectAsState()

  // Keep Lucene index in sync with loaded requests
  LaunchedEffect(state.requests) {
    if (state.requests.isNotEmpty()) {
      val base =
          state.requests.filter {
            it.status == com.android.sample.model.request.RequestStatus.OPEN ||
                it.status == com.android.sample.model.request.RequestStatus.IN_PROGRESS
          }
      searchFilterViewModel.initializeWithRequests(base)
    }
  }

  // Collect search/filter state
  val displayed by searchFilterViewModel.displayedRequests.collectAsState()
  val searchQuery by searchFilterViewModel.searchQuery.collectAsState()
  val isSearching by searchFilterViewModel.isSearching.collectAsState()

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(NavigationTestTags.REQUESTS_SCREEN),
      containerColor = appPalette().background,
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
              navigationActions = navigationActions)
        }
      },
      bottomBar = {
        if (!showOnlyMyRequests) {
          BottomNavigationMenu(
              selectedNavigationTab = NavigationTab.Requests, navigationActions = navigationActions)
        }
      },
      floatingActionButton = { if (!state.offlineMode) AddButton(navigationActions) }) {
          innerPadding ->

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
          val toShow =
              displayed.filter {
                it.status == com.android.sample.model.request.RequestStatus.OPEN ||
                    it.status == com.android.sample.model.request.RequestStatus.IN_PROGRESS
              }

          if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().testTag(RequestListTestTags.LOADING_INDICATOR),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator()
                }
          } else if (toShow.isEmpty()) {
            Text(
                text = if (showOnlyMyRequests) NO_REQUEST_YET else NO_REQUEST_NOW,
                modifier =
                    Modifier.fillMaxSize()
                        .wrapContentSize()
                        .testTag(RequestListTestTags.EMPTY_LIST_MESSAGE),
                textAlign = TextAlign.Center)
          } else {
            RequestList(
                viewModel = requestListViewModel,
                state = state.copy(requests = toShow),
                onRequestClick = {
                  // Always go to view-only accept screen; owner-specific edit is inside details
                  navigationActions?.navigateTo(Screen.RequestAccept(it.requestId))
                },
                navigationActions = navigationActions,
                modifier = Modifier.fillMaxSize())
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
    navigationActions: NavigationActions?,
    modifier: Modifier = Modifier
) {
  Column {
    if (state.offlineMode) {
      Text(
          text = "You are in offline mode. Displaying cached requests.",
          color = appPalette().error,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          textAlign = TextAlign.Center)
    }
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
                navigationActions = navigationActions,
                state = state)
          }
        }
  }
}

private const val WEIGHT_1f = 1f

private const val WEIGHT = WEIGHT_1f

private const val MAX_PARAM = 2
private const val ChipsDescriptionRatio = 0.4f

private const val THREE = 3

private const val ZERO = 0

@Composable
fun RequestListItem(
    viewModel: RequestListViewModel,
    request: Request,
    onClick: (Request) -> Unit,
    navigationActions: NavigationActions?,
    state: RequestListState,
    modifier: Modifier = Modifier
) {
  val accentColor =
      when {
        request.creatorId in state.followingIds -> appPalette().accent
        request.creatorId in state.followerIds -> appPalette().secondary
        else -> null
      }

  Card(
      modifier =
          modifier
              .padding(bottom = ConstantRequestList.ListItemSpacing)
              .fillMaxWidth()
              .height(ConstantRequestList.RequestItemHeight)
              .clickable(onClick = { onClick(request) })
              .testTag(RequestListTestTags.REQUEST_ITEM),
      colors =
          CardDefaults.cardColors(
              containerColor = appPalette().surface, contentColor = appPalette().onSurface)) {
        Row(modifier = Modifier.fillMaxSize()) {
          // Left edge accent indicator
          if (accentColor != null) {
            val width = 4.dp
            Box(modifier = Modifier.width(width).fillMaxHeight().background(accentColor))
          }

          // Original card content
          Row(
              modifier =
                  Modifier.fillMaxSize().padding(ConstantRequestList.RequestItemInnerPadding)) {
                ProfilePicture(
                    profileRepository = viewModel.profileRepository,
                    profileId = request.creatorId,
                    navigationActions = navigationActions,
                    modifier =
                        Modifier.width(ConstantRequestList.RequestItemCreatorSectionSize)
                            .fillMaxHeight()
                            .align(Alignment.CenterVertically)
                            .padding(
                                vertical = ConstantRequestList.RequestItemProfileHeightPadding),
                    withName = true,
                )

                Spacer(Modifier.width(ConstantRequestList.RowSpacing))

                TitleAndDescription(request, modifier = Modifier.weight(1f))

                Spacer(Modifier.width(ConstantRequestList.RowSpacing))
                LazyColumn(
                    modifier = Modifier.weight(ChipsDescriptionRatio),
                    verticalArrangement = Arrangement.spacedBy(TypeChipColumnSpacing)) {
                      val sortedRequestTypes = request.requestType.sortedBy { it.ordinal }
                      items(sortedRequestTypes.size) { index ->
                        val requestType = sortedRequestTypes[index]
                        TypeChip(
                            requestType = requestType,
                        )
                      }
                    }
              }
        }
      }
}

@Composable
fun TitleAndDescription(request: Request, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
      Text(
          request.title,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          fontSize = ConstantRequestList.RequestItemTitleFontSize,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.testTag(RequestListTestTags.REQUEST_ITEM_TITLE).weight(WEIGHT))
    }
    Spacer(modifier = Modifier.height(ConstantRequestList.RequestItemDescriptionSpacing))
    Text(
        request.description,
        color = appPalette().text.copy(alpha = 0.8f),
        fontSize = ConstantRequestList.RequestItemDescriptionFontSize,
        modifier = Modifier.fillMaxSize().testTag(RequestListTestTags.REQUEST_ITEM_DESCRIPTION),
        maxLines = MAX_PARAM,
        overflow = TextOverflow.Ellipsis)
  }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TypeChip(
    requestType: RequestType,
    modifier: Modifier = Modifier,
) {
  Surface(
      color = appPalette().getRequestTypeBackgroundColor(requestType),
      shape = RoundedCornerShape(TypeChipCornerRadius),
      modifier = modifier.fillMaxWidth(), // Fill the available width
      border = BorderStroke(TypeChipBorderWidth, appPalette().getRequestTypeColor(requestType))) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = TypeChipTextPadding)) {
              val baseFontSize = TypeChipTextSize
              val textLength = requestType.displayString().length

              val density = LocalDensity.current
              val availableWidthPx = with(density) { maxWidth.toPx() }

              val baseFontSizePx = with(density) { baseFontSize.toPx() }

              val estimatedTextWidth = textLength * baseFontSizePx * TypeChipTextSizeFactor
              val scaleFactor = (availableWidthPx / estimatedTextWidth).coerceAtMost(WEIGHT_1f)

              Text(
                  text = requestType.displayString(),
                  color = appPalette().getRequestTypeColor(requestType),
                  fontSize = baseFontSize * scaleFactor,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Clip // Prevent ellipsis from showing up unnecessarily
                  )
            }
      }
}

/** Floating add button to navigate to the add-request screen. */
@Composable
fun AddButton(navigationActions: NavigationActions?) {
  FloatingActionButton(
      onClick = { navigationActions?.navigateTo(Screen.AddRequest) },
      containerColor = appPalette().accent,
      modifier = Modifier.testTag(RequestListTestTags.REQUEST_ADD_BUTTON)) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Request",
            tint = appPalette().white)
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

// Preview for rendering improvements during development
/*
@Preview
@Composable
fun RequestListItemPreview() {
  val sampleRequest =
      Request(
          requestId = "1",
          title = "Sample Request",
          description = "This is a sample request description.",
          creatorId = "user1",
          location = com.android.sample.model.map.Location(0.0, 0.0, "nowhere"),
          locationName = "No where",
          requestType =
              RequestType.entries, // 2 values should appear and the rest should be truncated
          status = RequestStatus.OPEN,
          startTimeStamp = java.util.Date(),
          expirationTime = java.util.Date(),
          people = listOf(),
          tags = listOf())
  RequestListItem(request = sampleRequest, onClick = {}, viewModel = RequestListViewModel())
}
 */
