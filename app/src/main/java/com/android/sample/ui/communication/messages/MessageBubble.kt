package com.android.sample.ui.communication.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.model.chat.Message
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import java.text.SimpleDateFormat
import java.util.*

// Test Tags
private object MessageBubbleTestTags {
  const val BUBBLE = "message_bubble"
  const val SENDER_NAME = "message_sender_name"
  const val MESSAGE_TEXT = "message_text"
  const val TIMESTAMP = "message_timestamp"
}

// Constants
private object MessageBubbleConstants {
  const val TIMESTAMP_PATTERN = "HH:mm"
  const val MAX_BUBBLE_WIDTH_FRACTION = 0.75f
  const val BUBBLE_CORNER_RADIUS_DP = 16
  const val SENDER_NAME_ALPHA = 0.8f
  const val TIMESTAMP_ALPHA = 0.6f
}

/**
 * Message bubble component.
 *
 * Displays:
 * - Sender name (if not own message)
 * - Message text
 * - Timestamp
 *
 * Styling:
 * - Own messages: Right-aligned, accent color background
 * - Other messages: Left-aligned, surface color background
 *
 * @param message The message to display
 * @param isOwnMessage Whether this message was sent by the current user
 * @param modifier Modifier for the composable
 */
@Composable
fun MessageBubble(message: Message, isOwnMessage: Boolean, modifier: Modifier = Modifier) {
  val bubbleColor = if (isOwnMessage) appPalette().accent else appPalette().surface
  val textColor = if (isOwnMessage) appPalette().white else appPalette().text
  val alignment = if (isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart

  Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
    Column(
        modifier =
            Modifier.fillMaxWidth(MessageBubbleConstants.MAX_BUBBLE_WIDTH_FRACTION)
                .clip(RoundedCornerShape(MessageBubbleConstants.BUBBLE_CORNER_RADIUS_DP.dp))
                .background(bubbleColor)
                .padding(UiDimens.SpacingMd)
                .testTag(MessageBubbleTestTags.BUBBLE),
        verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingXs)) {
          // Sender Name (only for other's messages)
          if (!isOwnMessage) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = MessageBubbleConstants.SENDER_NAME_ALPHA),
                modifier = Modifier.testTag(MessageBubbleTestTags.SENDER_NAME))
          }

          // Message Text
          Text(
              text = message.text,
              style = MaterialTheme.typography.bodyMedium,
              color = textColor,
              modifier = Modifier.testTag(MessageBubbleTestTags.MESSAGE_TEXT))

          // Timestamp
          Text(
              text = formatTimestamp(message.timestamp),
              style = MaterialTheme.typography.labelSmall,
              color = textColor.copy(alpha = MessageBubbleConstants.TIMESTAMP_ALPHA),
              modifier =
                  Modifier.align(if (isOwnMessage) Alignment.End else Alignment.Start)
                      .testTag(MessageBubbleTestTags.TIMESTAMP))
        }
  }
}

/**
 * Formats a timestamp to a time string.
 *
 * @param date The date to format
 * @return Formatted string (e.g., "14:30")
 */
private fun formatTimestamp(date: Date): String {
  val formatter = SimpleDateFormat(MessageBubbleConstants.TIMESTAMP_PATTERN, Locale.getDefault())
  return formatter.format(date)
}
