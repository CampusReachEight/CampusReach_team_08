package com.android.sample.ui.request.accepted

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.profile.accepted_requests.AcceptedRequestsViewModel
import com.android.sample.ui.profile.accepted_requests.AcceptedRequestsViewModelFactory
import com.android.sample.ui.profile.accepted_requests.KudosStatus
import com.android.sample.ui.profile.accepted_requests.RequestWithKudosStatus
import com.android.sample.ui.request.ConstantRequestList
import com.android.sample.ui.request.RequestListViewModel
import com.android.sample.ui.request.TypeChip
import com.android.sample.ui.theme.appPalette
import java.text.SimpleDateFormat
import java.util.*

// ============ Constants ============

private const val SCREEN_TITLE = "Accepted Requests"
private const val BACK_BUTTON_DESCRIPTION = "Back"
private const val EMPTY_MESSAGE = "You haven't accepted any requests yet"
private const val ERROR_DIALOG_TITLE = "Error"
private const val ERROR_DIALOG_BUTTON = "OK"
private const val DIALOG_CLOSE_BUTTON = "Close"

private val SHADOW_SIZE = 4.dp
private val BADGE_SIZE = 12.dp
private val BADGE_ICON_SIZE = 16.dp
private val DIALOG_PADDING = 24.dp
private val DIALOG_SPACING = 16.dp
private val DIALOG_SMALL_SPACING = 8.dp
private val DIALOG_MAX_WIDTH = 400.dp
private val BIG_MODIFIER = 40.dp
private val FONT_SIZE_14 = 14.sp
private val ALPHA_6 = 0.6f
private val ALPHA_7 = 0.7f
private val WEIGHT = 1f

private const val KUDOS_RECEIVED_TEXT = "✓ Kudos Received"
private const val KUDOS_NOT_RECEIVED_TEXT = "✗ No Kudos"
private const val KUDOS_PENDING_TEXT = "⏳ Pending"

private const val LABEL_LOCATION = "Location:"
private const val LABEL_START_TIME = "Start Time:"
private const val LABEL_EXPIRATION = "Expiration:"
private const val LABEL_HELPERS = "People Accepted:"

private const val LABEL_CREATOR = "Creator:"

private const val DATE_FORMAT_PATTERN = "MMM dd, yyyy HH:mm"

// ============ Test Tags ============

object AcceptedRequestsTestTags {
  const val SCREEN = "acceptedRequestsScreen"
  const val REQUEST_LIST = "acceptedRequestsList"
  const val REQUEST_ITEM = "acceptedRequestItem"
  const val EMPTY_MESSAGE = "acceptedRequestsEmptyMessage"
  const val ERROR_DIALOG = "acceptedRequestsErrorDialog"
  const val KUDOS_BADGE = "kudosBadge"
  const val REQUEST_DIALOG = "requestDetailsDialog"
  const val DIALOG_CLOSE_BUTTON = "dialogCloseButton"
}

// ============ Main Screen ============

/**
 * Screen displaying all requests the current user has accepted as a helper. Shows kudos status for
 * each request with color-coded badges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptedRequestsScreen(
    navigationActions: NavigationActions,
    acceptedRequestsViewModel: AcceptedRequestsViewModel =
        viewModel(factory = AcceptedRequestsViewModelFactory()),
    requestListViewModel: RequestListViewModel = viewModel()
) {
  LaunchedEffect(Unit) { acceptedRequestsViewModel.loadAcceptedRequests() }

  val uiState by acceptedRequestsViewModel.uiState.collectAsState()
  var selectedRequest by remember { mutableStateOf<RequestWithKudosStatus?>(null) }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(AcceptedRequestsTestTags.SCREEN),
      topBar = {
        TopAppBar(
            title = { Text(SCREEN_TITLE) },
            navigationIcon = {
              IconButton(onClick = { navigationActions.goBack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = BACK_BUTTON_DESCRIPTION)
              }
            })
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Content
          when {
            uiState.errorMessage != null -> {
              ErrorDialog(
                  message = uiState.errorMessage!!,
                  onDismiss = { acceptedRequestsViewModel.clearError() })
            }
            uiState.isLoading -> {
              // Show loading indicator
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
            }
            uiState.requests.isEmpty() -> {
              EmptyState()
            }
            else -> {
              AcceptedRequestsList(
                  requests = uiState.requests,
                  requestListViewModel = requestListViewModel,
                  onRequestClick = { selectedRequest = it })
            }
          }
        }

        // Request details dialog
        selectedRequest?.let { requestWithStatus ->
          RequestDetailsDialog(
              requestWithStatus = requestWithStatus,
              requestListViewModel = requestListViewModel,
              onDismiss = { selectedRequest = null })
        }
      }
}

// ============ Request List ============

@Composable
private fun AcceptedRequestsList(
    requests: List<RequestWithKudosStatus>,
    requestListViewModel: RequestListViewModel,
    onRequestClick: (RequestWithKudosStatus) -> Unit
) {
  LazyColumn(
      modifier =
          Modifier.fillMaxSize()
              .padding(ConstantRequestList.ListPadding)
              .testTag(AcceptedRequestsTestTags.REQUEST_LIST)) {
        items(requests.size) { index ->
          AcceptedRequestItem(
              requestWithStatus = requests[index],
              requestListViewModel = requestListViewModel,
              onClick = onRequestClick)
        }
      }
}

// ============ Request Item Card ============

private const val ONE = 1

private const val WEIGHT_04 = 0.4f

private const val WEIGHT_08 = 0.8f

@Composable
private fun AcceptedRequestItem(
    requestWithStatus: RequestWithKudosStatus,
    requestListViewModel: RequestListViewModel,
    onClick: (RequestWithKudosStatus) -> Unit
) {
  val request = requestWithStatus.request

  Card(
      modifier =
          Modifier.padding(bottom = ConstantRequestList.ListItemSpacing)
              .fillMaxWidth()
              .height(ConstantRequestList.RequestItemHeight)
              .testTag(AcceptedRequestsTestTags.REQUEST_ITEM),
      onClick = { onClick(requestWithStatus) }) {
        Box(modifier = Modifier.fillMaxSize()) {
          // Main content
          Row(
              modifier =
                  Modifier.fillMaxSize().padding(ConstantRequestList.RequestItemInnerPadding)) {
                // Creator profile picture
                ProfilePicture(
                    profileRepository = requestListViewModel.profileRepository,
                    profileId = request.creatorId,
                    onClick = {},
                    modifier =
                        Modifier.width(ConstantRequestList.RequestItemCreatorSectionSize)
                            .fillMaxHeight()
                            .align(Alignment.CenterVertically)
                            .padding(
                                vertical = ConstantRequestList.RequestItemProfileHeightPadding),
                    withName = true)

                Spacer(Modifier.width(ConstantRequestList.RowSpacing))

                // Title and description
                Column(modifier = Modifier.weight(WEIGHT)) {
                  Text(
                      text = request.title,
                      maxLines = ONE,
                      overflow = TextOverflow.Ellipsis,
                      fontSize = ConstantRequestList.RequestItemTitleFontSize,
                      fontWeight = FontWeight.SemiBold)
                  Spacer(
                      modifier = Modifier.height(ConstantRequestList.RequestItemDescriptionSpacing))
                  Text(
                      text = request.description,
                      color = appPalette().text.copy(alpha = WEIGHT_08),
                      fontSize = ConstantRequestList.RequestItemDescriptionFontSize,
                      modifier = Modifier.fillMaxSize(),
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.width(ConstantRequestList.RowSpacing))

                // Request type chips
                LazyColumn(
                    modifier = Modifier.weight(WEIGHT_04),
                    verticalArrangement =
                        Arrangement.spacedBy(ConstantRequestList.TypeChipColumnSpacing)) {
                      val sortedRequestTypes = request.requestType.sortedBy { it.ordinal }
                      items(sortedRequestTypes.size) { index ->
                        TypeChip(requestType = sortedRequestTypes[index])
                      }
                    }
              }

          // Kudos status badge (top-right corner)
          KudosStatusBadge(
              kudosStatus = requestWithStatus.kudosStatus,
              modifier = Modifier.align(Alignment.TopEnd).padding(DIALOG_SMALL_SPACING))
        }
      }
}

// ============ Kudos Status Badge ============

private const val LIGHT_GREEN = 0xFF4CAF50

private const val BRIGHT_RED = 0xFFF44336

private const val GREY = 0xFF9E9E9E

@Composable
private fun KudosStatusBadge(kudosStatus: KudosStatus, modifier: Modifier = Modifier) {
  val (color, icon) =
      when (kudosStatus) {
        KudosStatus.RECEIVED -> Color(LIGHT_GREEN) to Icons.Default.CheckCircle
        KudosStatus.NOT_RECEIVED -> Color(BRIGHT_RED) to Icons.Default.Close
        KudosStatus.PENDING -> Color(GREY) to Icons.Default.Schedule
      }

  Surface(
      modifier = modifier.size(BADGE_SIZE),
      shape = CircleShape,
      color = color,
      shadowElevation = SHADOW_SIZE,
      tonalElevation = SHADOW_SIZE) {
        Box(
            modifier = Modifier.fillMaxSize().testTag(AcceptedRequestsTestTags.KUDOS_BADGE),
            contentAlignment = Alignment.Center) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(BADGE_ICON_SIZE))
            }
      }
}

// ============ Request Details Dialog ============

@Composable
private fun RequestDetailsDialog(
    requestWithStatus: RequestWithKudosStatus,
    requestListViewModel: RequestListViewModel,
    onDismiss: () -> Unit
) {
  val request = requestWithStatus.request
  val dateFormat = remember { SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault()) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
        modifier =
            Modifier.widthIn(max = DIALOG_MAX_WIDTH)
                .testTag(AcceptedRequestsTestTags.REQUEST_DIALOG),
        shape = MaterialTheme.shapes.large,
        tonalElevation = DIALOG_SPACING) {
          Column(modifier = Modifier.fillMaxWidth().padding(DIALOG_PADDING)) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = request.title,
                      style = MaterialTheme.typography.headlineSmall,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.weight(WEIGHT))
                  IconButton(
                      onClick = onDismiss,
                      modifier = Modifier.testTag(AcceptedRequestsTestTags.DIALOG_CLOSE_BUTTON)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = DIALOG_CLOSE_BUTTON)
                      }
                }

            Spacer(modifier = Modifier.height(DIALOG_SPACING))

            // Kudos status
            KudosStatusChip(kudosStatus = requestWithStatus.kudosStatus)

            Spacer(modifier = Modifier.height(DIALOG_SPACING))

            // Creator info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                  Text(
                      text = LABEL_CREATOR,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = FONT_SIZE_14)
                  Spacer(modifier = Modifier.width(DIALOG_SMALL_SPACING))
                  ProfilePicture(
                      profileRepository = requestListViewModel.profileRepository,
                      profileId = request.creatorId,
                      onClick = {},
                      modifier = Modifier.size(BIG_MODIFIER),
                      withName = true)
                }

            Spacer(modifier = Modifier.height(DIALOG_SPACING))

            // Description
            Text(
                text = request.description,
                style = MaterialTheme.typography.bodyMedium,
                color = appPalette().text.copy(alpha = WEIGHT_08))

            Spacer(modifier = Modifier.height(DIALOG_SPACING))

            // Request types
            LazyRow(horizontalArrangement = Arrangement.spacedBy(DIALOG_SMALL_SPACING)) {
              items(request.requestType.size) { index ->
                TypeChip(requestType = request.requestType[index])
              }
            }

            Spacer(modifier = Modifier.height(DIALOG_SPACING))

            // Location
            DialogInfoRow(label = LABEL_LOCATION, value = request.locationName)

            Spacer(modifier = Modifier.height(DIALOG_SMALL_SPACING))

            // Start time
            DialogInfoRow(
                label = LABEL_START_TIME, value = dateFormat.format(request.startTimeStamp))

            Spacer(modifier = Modifier.height(DIALOG_SMALL_SPACING))

            // Expiration time
            DialogInfoRow(
                label = LABEL_EXPIRATION, value = dateFormat.format(request.expirationTime))

            Spacer(modifier = Modifier.height(DIALOG_SMALL_SPACING))

            // Number of people who accepted
            DialogInfoRow(label = LABEL_HELPERS, value = request.people.size.toString())

            Spacer(modifier = Modifier.height(DIALOG_SPACING))

            // Close button
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
              Text(DIALOG_CLOSE_BUTTON)
            }
          }
        }
  }
}

// ============ Helper Composables ============

private const val GREEN = 0xFF1B5E20

private const val WHITE = 0xFFE8F5E9

private const val RED = 0xFFB71C1C

private const val GREY_WHITE = 0xFFFFEBEE

private const val BLACK = 0xFF424242

private const val LIGHT_WHITE = 0xFFF5F5F5

@Composable
private fun KudosStatusChip(kudosStatus: KudosStatus) {
  val (text, color, backgroundColor) =
      when (kudosStatus) {
        KudosStatus.RECEIVED -> Triple(KUDOS_RECEIVED_TEXT, Color(GREEN), Color(WHITE))
        KudosStatus.NOT_RECEIVED -> Triple(KUDOS_NOT_RECEIVED_TEXT, Color(RED), Color(GREY_WHITE))
        KudosStatus.PENDING -> Triple(KUDOS_PENDING_TEXT, Color(BLACK), Color(LIGHT_WHITE))
      }

  Surface(
      color = backgroundColor,
      shape = MaterialTheme.shapes.small,
      modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = FONT_SIZE_14,
            modifier = Modifier.padding(horizontal = BADGE_SIZE, vertical = DIALOG_SMALL_SPACING))
      }
}

@Composable
private fun DialogInfoRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(
        text = label,
        fontWeight = FontWeight.SemiBold,
        fontSize = FONT_SIZE_14,
        color = appPalette().text.copy(alpha = ALPHA_7))
    Text(text = value, fontSize = FONT_SIZE_14)
  }
}

@Composable
private fun EmptyState() {
  Box(
      modifier = Modifier.fillMaxSize().testTag(AcceptedRequestsTestTags.EMPTY_MESSAGE),
      contentAlignment = Alignment.Center) {
        Text(
            text = EMPTY_MESSAGE,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = appPalette().text.copy(alpha = ALPHA_6))
      }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(ERROR_DIALOG_TITLE) },
      text = {
        Text(text = message, modifier = Modifier.testTag(AcceptedRequestsTestTags.ERROR_DIALOG))
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text(ERROR_DIALOG_BUTTON) } })
}
