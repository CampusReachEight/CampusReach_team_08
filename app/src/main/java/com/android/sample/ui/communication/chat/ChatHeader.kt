package com.android.sample.ui.communication.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.sample.model.chat.Chat
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

// Test Tags
private object ChatHeaderTestTags {
    const val HEADER = "chat_header"
    const val TITLE = "chat_header_title"
}

// Constants
private object ChatHeaderConstants {
    const val HEADER_CORNER_RADIUS_DP = 12
}

private const val TWO = 2

/**
 * Header card displaying request details at the top of the chat.
 *
 * Shows:
 * - Request title
 *
 * @param chat The chat containing request details
 * @param modifier Modifier for the composable
 */
@Composable
fun ChatHeader(chat: Chat, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(UiDimens.SpacingMd)
                .clip(RoundedCornerShape(ChatHeaderConstants.HEADER_CORNER_RADIUS_DP.dp))
                .background(appPalette().secondary)
                .testTag(ChatHeaderTestTags.HEADER)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(UiDimens.SpacingMd),
            verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingXs)) {
            // Request Title
            Text(
                text = chat.requestTitle,
                style = MaterialTheme.typography.titleMedium,
                color = appPalette().text,
                maxLines = TWO,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(ChatHeaderTestTags.TITLE))
        }
    }
}