package com.android.sample.ui.request

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.theme.BottomNavigationMenu
import com.android.sample.ui.theme.NavigationTab

val LIST_PADDING = 16.dp
val LIST_ITEM_PADDING = 8.dp
val ICON_SIZE = 40.dp

object RequestListTestTags {
  const val REQUEST_ITEM = "requestItem"
  const val REQUEST_ITEM_TITLE = "requestItemTitle"
  const val REQUEST_ITEM_DESCRIPTION = "requestItemDescription"
  const val REQUEST_ITEM_ICON = "requestItemIcon"
  const val EMPTY_LIST_MESSAGE = "emptyListMessage"
}

@Composable
fun RequestListScreen(
    modifier: Modifier = Modifier,
    requestListViewModel: RequestListViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  LaunchedEffect(Unit) { requestListViewModel.loadRequests() }

  val icons by requestListViewModel.profileIcons.collectAsState()

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(NavigationTestTags.REQUESTS_SCREEN),
      topBar = {},
      bottomBar = {
        BottomNavigationMenu(
            selectedNavigationTab = NavigationTab.Requests, navigationActions = navigationActions)
      }) { innerPadding ->
        val state by requestListViewModel.state.collectAsState()
        if (state.requests.isEmpty()) {
          Text(
              text = "No requests at the moment",
              modifier =
                  Modifier.fillMaxSize()
                      .padding(innerPadding)
                      .wrapContentSize()
                      .testTag(RequestListTestTags.EMPTY_LIST_MESSAGE),
              textAlign = TextAlign.Center)
          return@Scaffold
        }
        RequestList(
            state = state,
            icons = icons,
            onRequestClick = { navigationActions?.navigateTo(Screen.RequestDetails(it)) },
            modifier = Modifier.fillMaxSize().padding(innerPadding))
      }
}

@Composable
fun RequestList(
    state: RequestListState,
    icons: Map<String, Bitmap?>,
    onRequestClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyColumn(modifier = modifier.padding(LIST_PADDING)) {
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
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier =
          modifier
              .padding(bottom = LIST_ITEM_PADDING)
              .fillMaxWidth()
              .clickable(onClick = { onClick(request.requestId) })
              .testTag(RequestListTestTags.REQUEST_ITEM),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
      if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = "Photo profile",
            modifier = Modifier.size(ICON_SIZE).testTag(RequestListTestTags.REQUEST_ITEM_ICON))
      } else {
        Box(Modifier.size(ICON_SIZE))
      }
      Spacer(Modifier.width(8.dp))
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
