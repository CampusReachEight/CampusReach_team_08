package com.android.sample.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.android.sample.ui.profile.ProfilePicture
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AcceptRequestScreenTestTags {
  const val REQUEST_BUTTON = "requestButton"
  const val REQUEST_TITLE = "requestTitle"
  const val VALIDATE_REQUEST_BUTTON = "validateRequestButton"
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
  const val VOLUNTEERS_SECTION_HEADER = "volunteersHeader"
  const val VOLUNTEERS_SECTION_CONTAINER = "volunteersContainer"
}

// Centralized user-visible strings for AcceptRequest screen
object AcceptRequestScreenLabels {
  const val BACK = "Back"
  const val VALIDATE_REQUEST = "Validate Request"

  const val DESCRIPTION = "Description"
  const val TAGS = "Tags"
  const val REQUEST_TYPE = "Request type"
  const val STATUS = "Status"
  const val LOCATION = "Location"
  const val START_TIME = "Start time"
  const val EXPIRATION_TIME = "Expiration time"

  const val EDIT_REQUEST = "Edit Request"
  const val CANCEL_ACCEPTANCE = "Cancel Acceptance"
  const val ACCEPT_REQUEST = "Accept Request"

  const val VOLUNTEERS = "Volunteers"
  const val COLLAPSE = "Collapse"
  const val EXPAND = "Expand"
  const val NO_VOLUNTEERS_YET = "No volunteers yet"

  const val GENERIC_ERROR = "An error occurred. Please reload or go back"
  const val POSTED_BY = "Posted by"

  const val INITIALS_PLACEHOLDER = "?"

  const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptRequestScreen(
    requestId: String,
    acceptRequestViewModel: AcceptRequestViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onValidateClick: (String) -> Unit = {}
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

  // UI local state
  var volunteersExpanded by rememberSaveable { mutableStateOf(false) }

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
                        contentDescription = AcceptRequestScreenLabels.BACK)
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
                val isOwner = FirebaseAuth.getInstance().currentUser?.uid == request.creatorId
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
                                label = AcceptRequestScreenLabels.DESCRIPTION,
                                content = request.description,
                                testTag = AcceptRequestScreenTestTags.REQUEST_DESCRIPTION)

                            // Tags
                            RequestDetailRow(
                                icon = Icons.Outlined.LocalOffer,
                                label = AcceptRequestScreenLabels.TAGS,
                                content = request.tags.joinToString(", ") { it.displayString() },
                                testTag = AcceptRequestScreenTestTags.REQUEST_TAG)

                            // Request type
                            RequestDetailRow(
                                icon = Icons.Outlined.BookmarkBorder,
                                label = AcceptRequestScreenLabels.REQUEST_TYPE,
                                content =
                                    request.requestType.joinToString(", ") { it.displayString() },
                                testTag = AcceptRequestScreenTestTags.REQUEST_TYPE)

                            // Status
                            RequestDetailRow(
                                icon = Icons.Outlined.Notifications,
                                label = "Status",
                                content = request.viewStatus.displayString(),
                                testTag = AcceptRequestScreenTestTags.REQUEST_STATUS)

                            // Location
                            RequestDetailRow(
                                icon = Icons.Outlined.LocationOn,
                                label = AcceptRequestScreenLabels.LOCATION,
                                content = request.locationName,
                                testTag = AcceptRequestScreenTestTags.REQUEST_LOCATION_NAME)

                            // Start time
                            RequestDetailRow(
                                icon = Icons.Outlined.AccessTime,
                                label = AcceptRequestScreenLabels.START_TIME,
                                content = request.startTimeStamp.toDisplayString(),
                                testTag = AcceptRequestScreenTestTags.REQUEST_START_TIME)

                            // Expiration time
                            RequestDetailRow(
                                icon = Icons.Outlined.WatchLater,
                                label = AcceptRequestScreenLabels.EXPIRATION_TIME,
                                content = request.expirationTime.toDisplayString(),
                                testTag = AcceptRequestScreenTestTags.REQUEST_EXPIRATION_TIME)
                          }
                    }

                Spacer(modifier = Modifier.height(AcceptRequestScreenConstants.BUTTON_TOP_SPACING))

                // Action Button (Accept/Cancel for non-owners, Edit for owners)
                FilledTonalButton(
                    onClick = {
                      if (isOwner) {
                        onEditClick(requestId)
                      } else if (requestState.accepted) {
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
                                if (isOwner) AcceptRequestScreenLabels.EDIT_REQUEST
                                else if (requestState.accepted)
                                    AcceptRequestScreenLabels.CANCEL_ACCEPTANCE
                                else AcceptRequestScreenLabels.ACCEPT_REQUEST,
                            style = MaterialTheme.typography.labelLarge)
                      }
                    }

                if (isOwner) {
                  Spacer(modifier = Modifier.height(AcceptRequestScreenConstants.SECTION_SPACING))

                  FilledTonalButton(
                      onClick = { onValidateClick(requestId) },
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(AcceptRequestScreenConstants.BUTTON_HEIGHT)
                              .testTag(AcceptRequestScreenTestTags.VALIDATE_REQUEST_BUTTON)) {
                        Text(
                            text = AcceptRequestScreenLabels.VALIDATE_REQUEST,
                            style = MaterialTheme.typography.labelLarge)
                      }
                }

                // Volunteers expandable section (owners only)
                if (isOwner) {
                  Column(
                      modifier =
                          Modifier.fillMaxWidth()
                              .testTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_CONTAINER)
                              .clip(
                                  RoundedCornerShape(
                                      AcceptRequestScreenConstants.CARD_CORNER_RADIUS))
                              .background(MaterialTheme.colorScheme.surfaceVariant)
                              .padding(AcceptRequestScreenConstants.CREATOR_SECTION_PADDING)) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { volunteersExpanded = !volunteersExpanded }
                                    .semantics(mergeDescendants = true) {}
                                    .testTag(AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_HEADER),
                            verticalAlignment = Alignment.CenterVertically) {
                              Icon(
                                  imageVector = Icons.Outlined.Group,
                                  contentDescription = AcceptRequestScreenLabels.VOLUNTEERS,
                                  tint = MaterialTheme.colorScheme.onSurfaceVariant)
                              Spacer(
                                  modifier =
                                      Modifier.width(
                                          AcceptRequestScreenConstants.ICON_TEXT_SPACING))
                              Text(
                                  text = AcceptRequestScreenLabels.VOLUNTEERS,
                                  style = MaterialTheme.typography.titleMedium,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  modifier =
                                      Modifier.weight(
                                          AcceptRequestScreenConstants.TEXT_COLUMN_WEIGHT))
                              Icon(
                                  imageVector =
                                      if (volunteersExpanded) Icons.Outlined.KeyboardArrowUp
                                      else Icons.Outlined.KeyboardArrowDown,
                                  contentDescription =
                                      if (volunteersExpanded) AcceptRequestScreenLabels.COLLAPSE
                                      else AcceptRequestScreenLabels.EXPAND,
                                  tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                        if (volunteersExpanded) {
                          Spacer(
                              modifier =
                                  Modifier.height(AcceptRequestScreenConstants.SECTION_SPACING))

                          // Safeguard: exclude creatorId from volunteers list if present
                          val volunteers = request.people.filterNot { it == request.creatorId }
                          if (volunteers.isEmpty()) {
                            Text(
                                text = AcceptRequestScreenLabels.NO_VOLUNTEERS_YET,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                          } else {
                            LazyRow(
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        AcceptRequestScreenConstants.VOLUNTEER_ROW_SPACING),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(
                                            AcceptRequestScreenConstants.VOLUNTEER_ROW_HEIGHT)) {
                                  items(volunteers.size) { index ->
                                    val userId = volunteers[index]
                                    ProfilePicture(profileId = userId, withName = true)
                                  }
                                }
                          }
                        }
                      }
                }
              }
                  ?: Text(
                      text = AcceptRequestScreenLabels.GENERIC_ERROR,
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
              text = AcceptRequestScreenLabels.POSTED_BY,
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
    parts.isEmpty() -> AcceptRequestScreenLabels.INITIALS_PLACEHOLDER
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
    SimpleDateFormat(AcceptRequestScreenLabels.DATE_TIME_FORMAT, Locale.getDefault())
        .format(timestamp)
  }
}
