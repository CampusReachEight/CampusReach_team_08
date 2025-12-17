package com.android.sample.ui.communication.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
}

// Constants
private object MessagesScreenConstants {
  const val TITLE = "Messages"
  const val EMPTY_STATE_MESSAGE = "No messages yet"
  const val EMPTY_STATE_SUBTITLE = "Start helping others or create a request to begin chatting"
  const val ERROR_DISMISS = "Dismiss"
  const val EMPTY_STATE_ICON_SIZE_MULTIPLIER = 2f
}

/**
 * Messages screen showing all chats for the current user.
 *
 * Features:
 * - List of all chats sorted by most recent message
 * - Badge showing user role (Creator/Helper)
 * - Pull-to-refresh functionality
 * - Empty state when no chats exist
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

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(NavigationTestTags.MESSAGES_SCREEN),
      // REMOVED: topBar
      // REMOVED: bottomBar
      snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = UiDimens.SpacingMd)) {
              when {
                uiState.isLoading -> {
                  CircularProgressIndicator(
                      modifier =
                          Modifier.align(Alignment.Center)
                              .testTag(MessagesScreenTestTags.LOADING_INDICATOR),
                      color = appPalette().accent)
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
                              onClick = { onChatClick(chatItem.chat.chatId) })
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
}

private const val ALPHA = 0.6f

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
