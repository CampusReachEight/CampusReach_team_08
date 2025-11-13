package com.android.sample.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.NavigationTestTags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AcceptRequestScreenTestTags {
  const val REQUEST_BUTTON = "requestButton"
  const val REQUEST_TITLE = "requestTitle"
  const val REQUEST_DESCRIPTION = "requestDescription"
  const val REQUEST_TAG = "requestTag"
  const val REQUEST_TYPE = "requestType"
  const val REQUEST_STATUS = "requestStatus"
  const val REQUEST_LOCATION_NAME = "requestLocationName"
  const val REQUEST_START_TIME = "requestStartTime"
  const val REQUEST_EXPIRATION_TIME = "requestExpirationTime"
  const val NO_REQUEST = "noRequest"
  const val REQUEST_COLUMN = "requestColumn"
  const val REQUEST_TOP_BAR = "requestTopBar"
  const val REQUEST_GO_BACK = "requestGoBack"
  const val REQUEST_CREATOR = "requestCreator"
  const val REQUEST_CREATOR_AVATAR = "requestCreatorAvatar"
  const val REQUEST_DETAILS_CARD = "requestDetailsCard"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptRequestScreen(
    requestId: String,
    acceptRequestViewModel: AcceptRequestViewModel = viewModel(),
    onGoBack: () -> Unit = {}
) {
  LaunchedEffect(requestId) { acceptRequestViewModel.loadRequest(requestId) }

  val requestState by acceptRequestViewModel.uiState.collectAsState()
  val errorMessage = requestState.errorMsg

  val context = LocalContext.current

  LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
      Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
      acceptRequestViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.ACCEPT_REQUEST_SCREEN),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  requestState.request?.title ?: "",
                  Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR))
            },
            navigationIcon = {
              IconButton(
                  onClick = { onGoBack() },
                  Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back")
                  }
            })
      },
      content = { pd ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(pd)
                    .padding(
                        horizontal = AcceptRequestScreenConstants.SCREEN_HORIZONTAL_PADDING,
                        vertical = AcceptRequestScreenConstants.SCREEN_VERTICAL_PADDING)
                    .verticalScroll(rememberScrollState())
                    .testTag(AcceptRequestScreenTestTags.REQUEST_COLUMN),
            verticalArrangement =
                Arrangement.spacedBy(AcceptRequestScreenConstants.SECTION_SPACING)) {
              requestState.request?.let { request ->
                // Main Details Card
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(AcceptRequestScreenTestTags.REQUEST_DETAILS_CARD),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = AcceptRequestScreenConstants.CARD_ELEVATION),
                    shape = RoundedCornerShape(AcceptRequestScreenConstants.CARD_CORNER_RADIUS)) {
                      Column(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(AcceptRequestScreenConstants.CARD_PADDING),
                          verticalArrangement =
                              Arrangement.spacedBy(AcceptRequestScreenConstants.SECTION_SPACING)) {
                            // Description
                            RequestDetailRow(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                label = "Description",
                                content = request.description,
                                testTag = AcceptRequestScreenTestTags.REQUEST_DESCRIPTION)

                            // Tags
                            RequestDetailRow(
                                icon = Icons.Outlined.LocalOffer,
                                label = "Tags",
                                content = request.tags.joinToString(", ") { it.displayString() },
                                testTag = AcceptRequestScreenTestTags.REQUEST_TAG)

                            // Request type
                            RequestDetailRow(
                                icon = Icons.Outlined.BookmarkBorder,
                                label = "Request type",
                                content =
                                    request.requestType.joinToString(", ") { it.displayString() },
                                testTag = AcceptRequestScreenTestTags.REQUEST_TYPE)

                            // Status
                            RequestDetailRow(
                                icon = Icons.Outlined.Notifications,
                                label = "Status",
                                content = request.status.displayString(),
                                testTag = AcceptRequestScreenTestTags.REQUEST_STATUS)

                            // Location
                            RequestDetailRow(
                                icon = Icons.Outlined.LocationOn,
                                label = "Location",
                                content = request.locationName,
                                testTag = AcceptRequestScreenTestTags.REQUEST_LOCATION_NAME)

                            // Start time
                            RequestDetailRow(
                                icon = Icons.Outlined.AccessTime,
                                label = "Start time",
                                content = request.startTimeStamp.toDisplayString(),
                                testTag = AcceptRequestScreenTestTags.REQUEST_START_TIME)

                            // Expiration time
                            RequestDetailRow(
                                icon = Icons.Outlined.WatchLater,
                                label = "Expiration time",
                                content = request.expirationTime.toDisplayString(),
                                testTag = AcceptRequestScreenTestTags.REQUEST_EXPIRATION_TIME)
                          }
                    }

                Spacer(modifier = Modifier.height(AcceptRequestScreenConstants.BUTTON_TOP_SPACING))

                // Accept/Cancel Button
                FilledTonalButton(
                    onClick = {
                      if (requestState.accepted) {
                        acceptRequestViewModel.cancelAcceptanceToRequest(requestId)
                      } else {
                        acceptRequestViewModel.acceptRequest(requestId)
                      }
                    },
                    enabled = !requestState.isLoading,
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(AcceptRequestScreenConstants.BUTTON_HEIGHT)
                            .testTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)) {
                      if (requestState.isLoading) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(AcceptRequestScreenConstants.CIRCULAR_PROGRESS_SIZE),
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                      } else {
                        Text(
                            text =
                                if (requestState.accepted) "Cancel Acceptance"
                                else "Accept Request",
                            style = MaterialTheme.typography.labelLarge)
                      }
                    }
              }
                  ?: Text(
                      text = "An error occurred. Please reload or go back",
                      fontSize = AcceptRequestScreenConstants.ERROR_TEXT_FONT_SIZE,
                      color = MaterialTheme.colorScheme.error,
                      textAlign = TextAlign.Center,
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(AcceptRequestScreenConstants.CARD_PADDING)
                              .testTag(AcceptRequestScreenTestTags.NO_REQUEST))
            }
      })
}

@Composable
internal fun CreatorSection(creatorName: String, modifier: Modifier = Modifier) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(AcceptRequestScreenConstants.CARD_CORNER_RADIUS))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(AcceptRequestScreenConstants.CREATOR_SECTION_PADDING)
              .semantics(mergeDescendants = true) {},
      verticalAlignment = Alignment.CenterVertically) {
        // Avatar with initials
        Box(
            modifier =
                Modifier.size(AcceptRequestScreenConstants.CREATOR_AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = AcceptRequestScreenConstants.AVATAR_BACKGROUND_ALPHA))
                    .semantics(mergeDescendants = true) {}
                    .testTag(AcceptRequestScreenTestTags.REQUEST_CREATOR_AVATAR),
            contentAlignment = Alignment.Center) {
              Text(
                  text = getInitials(creatorName),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary)
            }

        Spacer(modifier = Modifier.width(AcceptRequestScreenConstants.CREATOR_AVATAR_TEXT_SPACING))

        Column {
          Text(
              text = "Posted by",
              style = MaterialTheme.typography.labelSmall,
              color =
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(
                      alpha = AcceptRequestScreenConstants.SECONDARY_TEXT_ALPHA),
              fontSize = AcceptRequestScreenConstants.CREATOR_LABEL_FONT_SIZE)
          Text(
              text = creatorName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = AcceptRequestScreenConstants.CREATOR_NAME_FONT_SIZE)
        }
      }
}

@Composable
private fun RequestDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(
                  vertical = AcceptRequestScreenConstants.ROW_VERTICAL_PADDING,
                  horizontal = AcceptRequestScreenConstants.ROW_HORIZONTAL_PADDING)
              .semantics(mergeDescendants = true) {}
              .testTag(testTag),
      verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier =
                Modifier.size(AcceptRequestScreenConstants.ICON_SIZE)
                    .padding(top = AcceptRequestScreenConstants.ICON_TOP_PADDING),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.width(AcceptRequestScreenConstants.ICON_TEXT_SPACING))

        Column(modifier = Modifier.weight(AcceptRequestScreenConstants.TEXT_COLUMN_WEIGHT)) {
          Text(
              text = label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = AcceptRequestScreenConstants.SECTION_TITLE_FONT_SIZE)

          Spacer(modifier = Modifier.height(AcceptRequestScreenConstants.CONTENT_TOP_SPACING))

          Text(
              text = content,
              style = MaterialTheme.typography.bodyMedium,
              color =
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(
                      alpha = AcceptRequestScreenConstants.SECONDARY_TEXT_ALPHA),
              fontSize = AcceptRequestScreenConstants.SECTION_CONTENT_FONT_SIZE)
        }
      }
}

/**
 * Extracts initials from a full name for display in the avatar.
 *
 * @param name The full name of the user
 * @return A string containing up to 2 uppercase initials
 */
private fun getInitials(name: String): String {
  val parts = name.trim().split(" ").filter { it.isNotEmpty() }
  return when {
    parts.isEmpty() -> "?"
    parts.size == 1 -> parts[0].take(2).uppercase(Locale.ROOT)
    else -> {
      val firstInitial = parts[0].firstOrNull()?.toString() ?: ""
      val lastInitial = parts.lastOrNull()?.firstOrNull()?.toString() ?: ""
      (firstInitial + lastInitial).uppercase(Locale.ROOT)
    }
  }
}

fun Date.toDisplayString(): String {
  return this.let { timestamp ->
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp)
  }
}
