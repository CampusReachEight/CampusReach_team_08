package com.android.sample.ui.communication.chat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.model.chat.Message
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
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

object MessageBubbleDimens {
  const val ProfilePictureSize = 40
  const val ProfilePicturePadding = 8
  const val BubbleCornerRadius = 16
  const val BubbleMaxWidthFraction = 0.75f
  const val BubbleOwnMessageWidthFraction = 0.7f

  const val NO_PADDING = 0
  const val BubblePadding = 12
  const val MessageSpacing = 4
  const val VerticalPadding = 8
  const val HorizontalPadding = 16
}

private const val F = 0.3f

private const val PROFILE_CLICKED = "PROFILE CLICKED"

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
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier,
    profileRepository: UserProfileRepository = UserProfileRepositoryFirestore(Firebase.firestore),
    onProfileClick: (String) -> Unit = {}
) {
  val bubbleColor = if (isOwnMessage) appPalette().accent else appPalette().surface
  val textColor = if (isOwnMessage) appPalette().white else appPalette().text
  val maxWidthFraction =
      if (isOwnMessage) MessageBubbleDimens.BubbleOwnMessageWidthFraction
      else MessageBubbleDimens.BubbleMaxWidthFraction

  Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start) {
        if (!isOwnMessage) {
          Box(
              modifier =
                  Modifier.padding(end = MessageBubbleDimens.ProfilePicturePadding.dp)
                      .size(MessageBubbleDimens.ProfilePictureSize.dp)
                      .clip(CircleShape)
                      .clickable(
                          onClick = {
                            println("$PROFILE_CLICKED: ${message.senderId}")
                            onProfileClick(message.senderId)
                          },
                          interactionSource = remember { MutableInteractionSource() },
                          indication = LocalIndication.current)) {
                ProfilePicture(
                    profileRepository = profileRepository,
                    profileId = message.senderId,
                    modifier = Modifier.fillMaxSize(),
                    withName = false)
              }
        }

        // Message bubble container
        Box(
            modifier =
                Modifier.fillMaxWidth(maxWidthFraction)
                    .wrapContentWidth(if (isOwnMessage) Alignment.End else Alignment.Start)) {
              Column(
                  modifier =
                      Modifier.clip(
                              RoundedCornerShape(
                                  topStart =
                                      if (isOwnMessage) MessageBubbleDimens.NO_PADDING.dp
                                      else MessageBubbleDimens.BubbleCornerRadius.dp,
                                  topEnd = MessageBubbleDimens.BubbleCornerRadius.dp,
                                  bottomStart = MessageBubbleDimens.BubbleCornerRadius.dp,
                                  bottomEnd =
                                      if (isOwnMessage) MessageBubbleDimens.NO_PADDING.dp
                                      else MessageBubbleDimens.BubbleCornerRadius.dp))
                          .background(bubbleColor)
                          .padding(MessageBubbleDimens.BubblePadding.dp)
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
}
