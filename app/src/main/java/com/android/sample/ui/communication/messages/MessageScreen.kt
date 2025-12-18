package com.android.sample.ui.communication.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.communication.chat.ChatListItem
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

// Test Tags
private object MessagesScreenTestTags {
  const val SCREEN = "messages_screen"
  const val TOP_BAR = "messages_top_bar"
  const val LOADING_INDICATOR = "messages_loading_indicator"
  const val ERROR_MESSAGE = "messages_error_message"
  const val CHAT_LIST = "messages_chat_list"
  const val EMPTY_STATE = "messages_empty_state"
  const val EMPTY_STATE_ICON = "messages_empty_state_icon"
  const val EMPTY_STATE_TEXT = "messages_empty_state_text"
  const val EMPTY_STATE_SUBTITLE = "messages_empty_state_subtitle"
  const val MESSAGES_OFFLINE_STATE = "messages_offline_state"
}

private const val WEIGHT_2 = 2f
private const val WEIGHT_6 = 0.6f
private const val ALPHA = WEIGHT_6
private const val AUTO_REFRESH_INTERVAL_MS = 3_000L // 3 seconds

// Constants
private object MessagesScreenConstants {
  const val TITLE = "Messages"
  const val EMPTY_STATE_MESSAGE = "No messages yet"
  const val EMPTY_STATE_SUBTITLE = "Start helping others or create a request to begin chatting"
  const val ERROR_DISMISS = "Dismiss"
  const val EMPTY_STATE_ICON_SIZE_MULTIPLIER = WEIGHT_2
}

/**
 * Messages screen showing all chats for the current user.
 *
 * Features:
 * - List of all chats sorted by most recent message
 * - Badge showing user role (Creator/Helper)
 * - Auto-refresh every 30 seconds
 * - Empty state when no chats exist
 * - Offline state when network unavailable
 * - Error handling with Snackbar
 *
 * @param onChatClick Callback when a chat item is clicked, receives chatId
 * @param viewModel ViewModel managing the messages state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onChatClick: (String) -> Unit,
    viewModel: MessagesViewModel = viewModel(factory = MessagesViewModelFactory()),
    modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = androidx.compose.material3.SnackbarHostState()

  // Auto-refresh every 30 seconds when online
  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(AUTO_REFRESH_INTERVAL_MS)
      if (!uiState.isOffline && !uiState.isLoading) {
        viewModel.refresh()
      }
    }
  }

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(NavigationTestTags.MESSAGES_SCREEN),
      snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = UiDimens.SpacingMd)) {
              when {
                uiState.isFirstLoad -> {
                  CircularProgressIndicator(
                      modifier =
                          Modifier.align(Alignment.Center)
                              .testTag(MessagesScreenTestTags.LOADING_INDICATOR),
                      color = appPalette().accent)
                }
                uiState.isOffline -> {
                  OfflineState(modifier = Modifier.align(Alignment.Center))
                }
                uiState.chatItems.isEmpty() -> {
                  EmptyMessagesState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                  LazyColumn(
                      modifier = Modifier.fillMaxSize().testTag(MessagesScreenTestTags.CHAT_LIST),
                      verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingSm),
                      contentPadding = PaddingValues(vertical = UiDimens.SpacingMd)) {
                        items(items = uiState.chatItems, key = { it.chat.chatId }) { chatItem ->
                          ChatListItem(
                              chat = chatItem.chat,
                              isCreator = chatItem.isCreator,
                              onClick = {
                                if (!uiState.isOffline) {
                                  onChatClick(chatItem.chat.chatId)
                                }
                              })
                        }
                      }
                }
              }
            }

        // Error handling
        uiState.errorMessage?.let { errorMessage ->
          androidx.compose.runtime.LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = MessagesScreenConstants.ERROR_DISMISS,
                duration = SnackbarDuration.Short)
            viewModel.clearError()
          }
        }
      }
}

/**
 * Empty state displayed when user has no chats.
 *
 * @param modifier Modifier for the composable
 */
@Composable
private fun EmptyMessagesState(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().testTag(MessagesScreenTestTags.EMPTY_STATE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingMd)) {
        Icon(
            imageVector = Icons.Filled.Message,
            contentDescription = null,
            modifier =
                Modifier.size(
                        UiDimens.IconLarge *
                            MessagesScreenConstants.EMPTY_STATE_ICON_SIZE_MULTIPLIER)
                    .testTag(MessagesScreenTestTags.EMPTY_STATE_ICON),
            tint = appPalette().secondary)

        Text(
            text = MessagesScreenConstants.EMPTY_STATE_MESSAGE,
            style = MaterialTheme.typography.titleLarge,
            color = appPalette().text,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(MessagesScreenTestTags.EMPTY_STATE_TEXT))

        Text(
            text = MessagesScreenConstants.EMPTY_STATE_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = appPalette().text.copy(alpha = ALPHA),
            textAlign = TextAlign.Center,
            modifier =
                Modifier.padding(horizontal = UiDimens.SpacingXl)
                    .testTag(MessagesScreenTestTags.EMPTY_STATE_SUBTITLE))
      }
}

private const val YOU_RE_OFFLINE = "You're Offline"

private const val CONNECT_TO_THE_INTERNET_MESSAGE =
    "Connect to the internet to view and access your messages"

/**
 * Offline state displayed when network is unavailable.
 *
 * @param modifier Modifier for the composable
 */
@Composable
private fun OfflineState(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().testTag(MessagesScreenTestTags.MESSAGES_OFFLINE_STATE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(UiDimens.SpacingMd)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Message,
            contentDescription = null,
            modifier = Modifier.size(UiDimens.IconLarge * WEIGHT_2),
            tint = appPalette().secondary.copy(alpha = WEIGHT_6))

        Text(
            text = YOU_RE_OFFLINE,
            style = MaterialTheme.typography.titleLarge,
            color = appPalette().text,
            textAlign = TextAlign.Center)

        Text(
            text = CONNECT_TO_THE_INTERNET_MESSAGE,
            style = MaterialTheme.typography.bodyMedium,
            color = appPalette().text.copy(alpha = WEIGHT_6),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = UiDimens.SpacingXl))
      }
}
