package com.android.sample.ui.overview

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.android.sample.ui.theme.appPalette
import com.android.sample.utils.UrlUtils
import com.google.firebase.auth.FirebaseAuth
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AcceptRequestScreenTestTags {
  const val GO_TO_CHAT_BUTTON = "goToChatButton"

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
  const val NAVIGATE_TO_MAP = "navigateToMap"
  const val OWNER_CHAT_BUTTON = "ownerChatButton"
}

// Centralized user-visible strings for AcceptRequest screen
object AcceptRequestScreenLabels {
  const val BACK = "Back"
  const val VALIDATE_REQUEST = "Validate Request"
  const val GO_TO_CHAT = "Go to Chat"
  const val CHAT_WITH_VOLUNTEERS = "Chat with Volunteers"

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
  const val SEE_REQUEST_ON_MAP = "See request on map"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptRequestScreen(
    requestId: String,
    acceptRequestViewModel: AcceptRequestViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onValidateClick: (String) -> Unit = {},
    onMapClick: (String) -> Unit = {},
    onChatClick: (String) -> Unit = {}
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

  var isLoading by remember { mutableStateOf(false) }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.ACCEPT_REQUEST_SCREEN),
      topBar = {
        var backClicked by remember { mutableStateOf(false) }

        TopAppBar(
            title = {
              Text(
                  requestState.request?.title ?: "",
                  Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR))
            },
            navigationIcon = {
              IconButton(
                  onClick = {
                    onGoBack()
                    backClicked = true
                  },
                  enabled = !backClicked,
                  modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = AcceptRequestScreenLabels.BACK)
                  }
            })
      },
      content = { pd ->
        if (requestState.isLoadingDetails) {
          Box(modifier = Modifier.fillMaxWidth().padding(pd), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(AcceptRequestScreenConstants.CIRCULAR_PROGRESS_SIZE))
          }
          return@Scaffold
        }

        Column(modifier = Modifier.padding(pd)) {
          if (requestState.offlineMode) {
            Text(
                text = "You are currently in offline mode",
                modifier = Modifier.fillMaxWidth(),
                color = appPalette().error,
                textAlign = TextAlign.Center)
          }
          Column(
              modifier =
                  Modifier.fillMaxSize()
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
                      shape = RoundedCornerShape(AcceptRequestScreenConstants.CARD_CORNER_RADIUS),
                      colors =
                          CardDefaults.cardColors(
                              containerColor = appPalette().secondary,
                              contentColor = appPalette().onSurface)) {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(AcceptRequestScreenConstants.CARD_PADDING),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    AcceptRequestScreenConstants.SECTION_SPACING)) {
                              // Description
                              RequestDetailRow(
                                  icon = Icons.Outlined.ChatBubbleOutline,
                                  label = AcceptRequestScreenLabels.DESCRIPTION,
                                  content = request.description,
                                  testTag = AcceptRequestScreenTestTags.REQUEST_DESCRIPTION,
                                  isMarkdown = true,
                                  contentBelowHeader = true)

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

                  Spacer(
                      modifier = Modifier.height(AcceptRequestScreenConstants.BUTTON_TOP_SPACING))

                  // Action Button (Accept/Cancel for non-owners, Edit for owners)
                  if (requestState.offlineMode) {
                    return@Scaffold
                  }
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
                              .testTag(AcceptRequestScreenTestTags.REQUEST_BUTTON),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = appPalette().accent,
                              contentColor = appPalette().onAccent)) {
                        if (requestState.isLoading) {
                          CircularProgressIndicator(
                              modifier =
                                  Modifier.size(
                                      AcceptRequestScreenConstants.CIRCULAR_PROGRESS_SIZE),
                              color = appPalette().accent)
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
                  // Go to Chat button - only shown for non-owners who have accepted
                  if (!isOwner && requestState.accepted) {
                    FilledTonalButton(
                        onClick = { onChatClick(requestId) },
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(AcceptRequestScreenConstants.BUTTON_HEIGHT)
                                .testTag(AcceptRequestScreenTestTags.GO_TO_CHAT_BUTTON),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = appPalette().accent,
                                contentColor = appPalette().onAccent)) {
                          Text(
                              text = AcceptRequestScreenLabels.GO_TO_CHAT,
                              style = MaterialTheme.typography.labelLarge)
                        }
                  }
                  FilledTonalButton(
                      onClick = {
                        isLoading = true
                        onMapClick(requestId)
                      },
                      enabled = !isLoading,
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(AcceptRequestScreenConstants.BUTTON_HEIGHT)
                              .testTag(AcceptRequestScreenTestTags.NAVIGATE_TO_MAP),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = appPalette().accent,
                              contentColor = appPalette().onAccent)) {
                        if (isLoading) {
                          CircularProgressIndicator(
                              modifier =
                                  Modifier.size(
                                      AcceptRequestScreenConstants.CIRCULAR_PROGRESS_SIZE),
                              color = appPalette().onAccent)
                        } else {
                          Text(
                              text = AcceptRequestScreenLabels.SEE_REQUEST_ON_MAP,
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
                                .testTag(AcceptRequestScreenTestTags.VALIDATE_REQUEST_BUTTON),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = appPalette().accent,
                                contentColor = appPalette().onAccent)) {
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
                                .background(appPalette().secondary)
                                .padding(AcceptRequestScreenConstants.CREATOR_SECTION_PADDING)) {
                          Row(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .clickable { volunteersExpanded = !volunteersExpanded }
                                      .semantics(mergeDescendants = true) {}
                                      .testTag(
                                          AcceptRequestScreenTestTags.VOLUNTEERS_SECTION_HEADER),
                              verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = AcceptRequestScreenLabels.VOLUNTEERS,
                                tint = appPalette().onSurface)
                            Spacer(
                                modifier =
                                    Modifier.width(AcceptRequestScreenConstants.ICON_TEXT_SPACING))
                            Text(
                                text = AcceptRequestScreenLabels.VOLUNTEERS,
                                style = MaterialTheme.typography.titleMedium,
                                color = appPalette().onSurface,
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
                                tint = appPalette().onSurface)
                          }

                          if (volunteersExpanded) {
                            Spacer(
                                modifier =
                                    Modifier.height(AcceptRequestScreenConstants.SECTION_SPACING))

                            // Chat with Volunteers button - only show if there are volunteers
                            val volunteers = request.people.filterNot { it == request.creatorId }
                            if (volunteers.isNotEmpty()) {
                              FilledTonalButton(
                                  onClick = { onChatClick(requestId) },
                                  modifier =
                                      Modifier.fillMaxWidth()
                                          .height(AcceptRequestScreenConstants.BUTTON_HEIGHT)
                                          .testTag(AcceptRequestScreenTestTags.OWNER_CHAT_BUTTON),
                                  colors =
                                      ButtonDefaults.buttonColors(
                                          containerColor = appPalette().accent,
                                          contentColor = appPalette().onAccent)) {
                                    Text(
                                        text = AcceptRequestScreenLabels.CHAT_WITH_VOLUNTEERS,
                                        style = MaterialTheme.typography.labelLarge)
                                  }

                              Spacer(
                                  modifier =
                                      Modifier.height(AcceptRequestScreenConstants.SECTION_SPACING))
                            }

                            // Now show the volunteers list or empty message
                            if (volunteers.isEmpty()) {
                              Text(
                                  text = AcceptRequestScreenLabels.NO_VOLUNTEERS_YET,
                                  style = MaterialTheme.typography.bodyMedium,
                                  color = appPalette().onSurface)
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
                        color = appPalette().error,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(AcceptRequestScreenConstants.CARD_PADDING)
                                .testTag(AcceptRequestScreenTestTags.NO_REQUEST))
              }
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
    modifier: Modifier = Modifier,
    isMarkdown: Boolean = false,
    contentBelowHeader: Boolean = false
) {
  val context = LocalContext.current
  var showDialog by remember { mutableStateOf(false) }
  var pendingUrl by remember { mutableStateOf<String?>(null) }

  if (showDialog && pendingUrl != null) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("External Link") },
        text = { Text("Do you want to open this link in your browser?\n\n$pendingUrl") },
        confirmButton = {
          TextButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pendingUrl))
                context.startActivity(intent)
                showDialog = false
              }) {
                Text("Open")
              }
        },
        dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } })
  }

  val contentComposable: @Composable () -> Unit = {
    if (isMarkdown) {
      Material3RichText {
        Markdown(
            content = content,
            onLinkClicked = { url ->
              if (UrlUtils.isValidHttpsUrl(url)) {
                pendingUrl = url
                showDialog = true
              }
            })
      }
    } else {
      Text(
          text = content,
          style = MaterialTheme.typography.bodyMedium,
          color =
              MaterialTheme.colorScheme.onSurfaceVariant.copy(
                  alpha = AcceptRequestScreenConstants.SECONDARY_TEXT_ALPHA),
          fontSize = AcceptRequestScreenConstants.SECTION_CONTENT_FONT_SIZE)
    }
  }

  if (contentBelowHeader) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = AcceptRequestScreenConstants.ROW_VERTICAL_PADDING,
                    horizontal = AcceptRequestScreenConstants.ROW_HORIZONTAL_PADDING)
                .semantics(mergeDescendants = true) {}
                .testTag(testTag)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(AcceptRequestScreenConstants.ICON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(AcceptRequestScreenConstants.ICON_TEXT_SPACING))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = AcceptRequestScreenConstants.SECTION_TITLE_FONT_SIZE)
          }
          Spacer(
              modifier = Modifier.height(AcceptRequestScreenConstants.DESCRIPTION_BOTTOM_PADDING))
          contentComposable()
        }
  } else {
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

            contentComposable()
          }
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
