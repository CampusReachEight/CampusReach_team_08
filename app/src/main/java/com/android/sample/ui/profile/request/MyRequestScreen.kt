package com.android.sample.ui.myrequests

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette
import androidx.compose.ui.tooling.preview.Preview
import com.android.sample.model.map.Location
import com.android.sample.model.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

object MyRequestsTestTags {
    const val SCREEN = "myRequestsScreen"
    const val REQUEST_ITEM = "myRequestItem"
    const val REQUEST_ADD_BUTTON = "myRequestAddButton"
    const val EMPTY_MESSAGE = "myRequestsEmptyMessage"
}

/**
 * Composable screen that shows the user's requests using MyRequestsViewModel which exposes
 * a StateFlow<MyRequestState>.
 *
 * @param profileId Optional — used when navigating back to profile (Screen.Profile(profileId))
 * @param navigationActions Navigation helper (nullable for previews/tests)
 * @param myRequestsViewModel ViewModel that provides the MyRequestState (default via viewModel())
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    profileId: String = "",
    navigationActions: NavigationActions? = null,
    modifier: Modifier = Modifier,
    myRequestsViewModel: MyRequestsViewModel = viewModel(),
    palette: AppPalette = appPalette()
) {
    // Collect state from viewmodel
    val state by myRequestsViewModel.state.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(MyRequestsTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text("My Requests") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate back to profile if profileId provided
                        if (profileId.isNotBlank()) {
                            navigationActions?.navigateTo(Screen.Profile(profileId))
                        } else {
                            navigationActions?.goBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigationActions?.navigateTo(Screen.AddRequest) },
                modifier = Modifier.testTag(MyRequestsTestTags.REQUEST_ADD_BUTTON)
            ) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null -> {
                    Text(
                        text = state.errorMessage ?: "Unknown error",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.myRequests.isEmpty() -> {
                    Text(
                        text = "You don't have any requests yet.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(MyRequestsTestTags.EMPTY_MESSAGE)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(state.myRequests) { request ->
                            MyRequestItem(
                                request = request,
                                onClick = {
                                    navigationActions?.navigateTo(Screen.EditRequest(request.requestId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyRequestItem(
    request: Request,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .testTag(MyRequestsTestTags.REQUEST_ITEM),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = request.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = request.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = request.requestType.joinToString(" | ") { it.displayString() },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private class FakeMyRequestsViewModelPreview : MyRequestsViewModel(
    loadMyRequests = { emptyList() }
) {
    private val previewState = MutableStateFlow(
        MyRequestState(
            isLoading = false,
            errorMessage = null,
            myRequests = listOf(
                Request(
                    requestId = "1",
                    title = "Group Study for Calculus",
                    description = "Looking for someone to study calc 2 chapters 4 and 5.",
                    requestType = listOf(RequestType.STUDY_GROUP),
                    location = Location(46.5191, 6.5668, "EPFL Library"),
                    locationName = "Rolex Learning Center",
                    status = RequestStatus.OPEN,
                    startTimeStamp = Date(),
                    expirationTime = Date(),
                    people = listOf("userA", "userB"),
                    tags = listOf(Tags.GROUP_WORK),
                    creatorId = "me"
                ),
                Request(
                    requestId = "2",
                    title = "Lunch at SV Cafeteria",
                    description = "Anyone hungry?",
                    requestType = listOf(RequestType.EATING),
                    location = Location(46.518, 6.567, "SV Cafeteria"),
                    locationName = "SV Cafeteria",
                    status = RequestStatus.OPEN,
                    startTimeStamp = Date(),
                    expirationTime = Date(),
                    people = emptyList(),
                    tags = listOf(Tags.EASY),
                    creatorId = "me"
                )
            )
        )
    )

    override val state: StateFlow<MyRequestState> = previewState
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun MyRequestsScreenPreview() {
    MyRequestsScreen(
        profileId = "1234",
        navigationActions = null, // not used in preview
        myRequestsViewModel = FakeMyRequestsViewModelPreview()
    )
}