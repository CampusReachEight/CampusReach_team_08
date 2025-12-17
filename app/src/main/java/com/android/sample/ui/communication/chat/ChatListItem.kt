package com.android.sample.ui.communication.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.sample.model.chat.Chat
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import java.text.SimpleDateFormat
import java.util.*

// Test Tags
object ChatListItemTestTags {
  const val ITEM = "chat_list_item"
  const val ROLE_BADGE = "chat_list_item_role_badge"
  const val ROLE_ICON = "chat_list_item_role_icon"
  const val TITLE = "chat_list_item_title"
  const val LAST_MESSAGE = "chat_list_item_last_message"
  const val TIMESTAMP = "chat_list_item_timestamp"
  const val STATUS_BADGE = "chat_list_item_status_badge"
}

// Constants
private object ChatListItemConstants {
  const val CREATOR_BADGE = "Creator"
  const val HELPER_BADGE = "Helper"
  const val NO_MESSAGES_YET = "No messages yet"
  const val TIMESTAMP_PATTERN = "MMM d, HH:mm"
  const val BADGE_CORNER_RADIUS_DP = 12
  const val TEXT_ALPHA_SECONDARY = 0.7f
  const val EXPIRED_ALPHA = 0.6f
  const val EXPIRED_SURFACE_ALPHA = 0.5f
  const val STATUS_ICON_SIZE_DP = 20
  const val STATUS_ICON_ALPHA = 0.7f

  // Status colors
  // val COLOR_COMPLETED = Color(0xFF4CAF50)
  // val COLOR_EXPIRED = Color(0xFFFF9800)
  // val COLOR_CANCELLED = Color(0xFFF44336)
  val COMPLETED = "COMPLETED"

  val EXPIRED = "EXPIRED"

  val CANCELLED = "CANCELLED"
  val WEIGHT = 1f
}

private const val MAX_LINES = 1
private const val LAST_MESSAGE_MAX_LINES = 2

/**
 * List item displaying a chat preview.
 *
 * Shows:
 * - Request title
 * - Last message preview
 * - Timestamp of last message
 * - User role badge (Creator/Helper)
 * - Status badge for expired/completed chats
 *
 * @param chat The chat to display
 * @param isCreator Whether the current user is the creator
 * @param onClick Callback when item is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun ChatListItem(
    chat: Chat,
    isCreator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val isExpired =
      chat.requestStatus in
          listOf(
              ChatListItemConstants.COMPLETED,
              ChatListItemConstants.EXPIRED,
              ChatListItemConstants.CANCELLED)

  Surface(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(UiDimens.CornerRadiusSm))
              .clickable(onClick = onClick)
              .testTag(ChatListItemTestTags.ITEM),
      color =
          if (isExpired)
              appPalette().surface.copy(alpha = ChatListItemConstants.EXPIRED_SURFACE_ALPHA)
          else appPalette().surface,
      tonalElevation = UiDimens.CardElevation) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(UiDimens.SpacingMd)
                    .alpha(
                        if (isExpired) ChatListItemConstants.EXPIRED_ALPHA
                        else ChatListItemConstants.WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDimens.SpacingMd)) {
              // Role Badge
              RoleBadge(isCreator = isCreator)

              // Chat Details
              Column(
                  modifier = Modifier.weight(ChatListItemConstants.WEIGHT),
                  verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingXs)) {
                    // Title
                    Text(
                        text = chat.requestTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = appPalette().text,
                        maxLines = MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(ChatListItemTestTags.TITLE))

                    // Last Message
                    Text(
                        text = chat.lastMessage.ifBlank { ChatListItemConstants.NO_MESSAGES_YET },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            appPalette()
                                .text
                                .copy(alpha = ChatListItemConstants.TEXT_ALPHA_SECONDARY),
                        maxLines = LAST_MESSAGE_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(ChatListItemTestTags.LAST_MESSAGE))
                  }

              // Timestamp and Status
              Column(
                  horizontalAlignment = Alignment.End,
                  verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingXs)) {
                    chat.lastMessageTimestamp?.let { timestamp ->
                      Text(
                          text = formatTimestamp(timestamp),
                          style = MaterialTheme.typography.bodySmall,
                          color =
                              appPalette()
                                  .text
                                  .copy(alpha = ChatListItemConstants.TEXT_ALPHA_SECONDARY),
                          modifier = Modifier.testTag(ChatListItemTestTags.TIMESTAMP))
                    }
                  }
            }
      }
}

/**
 * Badge showing user's role in the chat (Creator or Helper).
 *
 * @param isCreator Whether the user is the creator
 * @param modifier Modifier for the composable
 */
@Composable
private fun RoleBadge(isCreator: Boolean, modifier: Modifier = Modifier) {
  val backgroundColor = if (isCreator) appPalette().accent else appPalette().secondary
  val icon = if (isCreator) Icons.Filled.Person else Icons.Filled.Handshake
  val label =
      if (isCreator) ChatListItemConstants.CREATOR_BADGE else ChatListItemConstants.HELPER_BADGE

  Box(
      modifier =
          modifier
              .size(UiDimens.IconMedium)
              .clip(RoundedCornerShape(ChatListItemConstants.BADGE_CORNER_RADIUS_DP.dp))
              .background(backgroundColor)
              .testTag(ChatListItemTestTags.ROLE_BADGE),
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = appPalette().white,
            modifier = Modifier.size(UiDimens.IconSmall).testTag(ChatListItemTestTags.ROLE_ICON))
      }
}

/**
 * Formats a timestamp to a readable string.
 *
 * @param date The date to format
 * @return Formatted string (e.g., "Dec 16, 14:30")
 */
fun formatTimestamp(date: Date): String {
  val formatter = SimpleDateFormat(ChatListItemConstants.TIMESTAMP_PATTERN, Locale.getDefault())
  return formatter.format(date)
}
