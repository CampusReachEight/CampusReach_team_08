package com.android.sample.ui.communication.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

// Test Tags
private object ChatScreenTestTags {
  const val SCREEN = "chat_screen"
  const val TOP_BAR = "chat_top_bar"
  const val BACK_BUTTON = "chat_back_button"
  const val LOADING_INDICATOR = "chat_loading_indicator"
  const val ERROR_MESSAGE = "chat_error_message"
  const val MESSAGE_LIST = "chat_message_list"
  const val MESSAGE_INPUT = "chat_message_input"
  const val SEND_BUTTON = "chat_send_button"
  const val INPUT_ROW = "chat_input_row"
  const val READ_ONLY_MESSAGE = "chat_read_only_message"
  const val LOADING_MORE_INDICATOR = "chat_loading_more_indicator"
}

// Constants
private object ChatScreenConstants {
  const val BACK_BUTTON_DESCRIPTION = "Navigate back"
  const val SEND_BUTTON_DESCRIPTION = "Send message"
  const val MESSAGE_INPUT_PLACEHOLDER = "Type a message..."
  const val ERROR_DISMISS = "Dismiss"

  // Read-only messages
  const val CHAT_COMPLETED = "Request completed"
  const val CHAT_EXPIRED = "Request expired"
  const val CHAT_CANCELLED = "Request cancelled"
  const val CHAT_CLOSED = "Chat closed"
  const val READ_ONLY = "Read-only"

  // Status chip styling
  const val STATUS_CHIP_CORNER_RADIUS_DP = 20
  const val STATUS_ICON_SIZE_DP = 18
  const val STATUS_LOCK_ICON_SIZE_DP = 16
  const val STATUS_ICON_ALPHA = 0.6f
  const val STATUS_TEXT_ALPHA = 0.7f
  const val STATUS_LOCK_ALPHA = 0.4f
  const val STATUS_BACKGROUND_ALPHA = 0.1f
  const val LOADING_MORE_ICON_SIZE_DP = 24

  // Colors
  val COLOR_COMPLETED = Color(0xFF4CAF50)
  val COLOR_EXPIRED = Color(0xFFFF9800)
  val COLOR_CANCELLED = Color(0xFFF44336)
}

private const val WEIGHT = 1f
private const val DECREASE = 1
private const val MAX_LINES = 3

private const val ZERO = 0

/**
 * Chat conversation screen.
 *
 * Features:
 * - Chat header with request details
 * - Scrollable message list
 * - Message input field with send button (disabled for expired chats)
 * - Real-time message updates
 * - Auto-scroll to bottom on new messages
 * - Auto-load more messages when scrolling to top
 *
 * @param chatId The ID of the chat to display
 * @param onBackClick Callback when back button is clicked
 * @param onProfileClick Callback when profile picture is clicked
 * @param viewModel ViewModel managing the chat state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory())
) {
  val uiState by viewModel.uiState.collectAsState()
  val listState = rememberLazyListState()
  val snackbarHostState = remember { SnackbarHostState() }

  val isExpired = uiState.chat?.requestStatus in listOf("COMPLETED", "EXPIRED", "CANCELLED")

  // Initialize chat when screen is created
  LaunchedEffect(chatId) { viewModel.initializeChat(chatId) }

  // Auto-scroll to bottom when new messages arrive
  LaunchedEffect(uiState.messages.size) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.size - DECREASE)
    }
  }

  // Auto-load more messages when scrolled to top
  val shouldLoadMore by remember {
    derivedStateOf {
      val firstVisibleItemIndex = listState.firstVisibleItemIndex
      firstVisibleItemIndex == ZERO && !uiState.isLoadingMore && uiState.messages.isNotEmpty()
    }
  }

  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) {
      viewModel.loadMoreMessages()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().imePadding().testTag(NavigationTestTags.CHAT_SCREEN),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = uiState.chat?.requestTitle ?: "",
                  style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
              IconButton(
                  onClick = onBackClick,
                  modifier = Modifier.testTag(ChatScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ChatScreenConstants.BACK_BUTTON_DESCRIPTION,
                        tint = appPalette().text)
                  }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = appPalette().surface, titleContentColor = appPalette().text),
            modifier = Modifier.testTag(ChatScreenTestTags.TOP_BAR))
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (!isExpired) {
          MessageInputBar(
              messageText = uiState.messageInput,
              onMessageChange = viewModel::updateMessageInput,
              onSendClick = { viewModel.sendMessage(uiState.messageInput) },
              isSending = uiState.isSendingMessage)
        } else {
          ReadOnlyMessageBar(requestStatus = uiState.chat?.requestStatus)
        }
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          when {
            uiState.isLoading -> {
              CircularProgressIndicator(
                  modifier =
                      Modifier.align(Alignment.Center)
                          .testTag(ChatScreenTestTags.LOADING_INDICATOR),
                  color = appPalette().accent)
            }
            else -> {
              Column(modifier = Modifier.fillMaxSize().testTag(ChatScreenTestTags.MESSAGE_LIST)) {

                // Chat Header (Request Details)
                uiState.chat?.let { chat -> ChatHeader(chat = chat) }

                // Messages List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding =
                        PaddingValues(
                            vertical = MessageBubbleDimens.VerticalPadding.dp,
                            horizontal = MessageBubbleDimens.HorizontalPadding.dp)) {
                      // Loading indicator at top
                      if (uiState.isLoadingMore) {
                        item {
                          Box(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(MessageBubbleDimens.VerticalPadding.dp)
                                      .testTag(ChatScreenTestTags.LOADING_MORE_INDICATOR),
                              contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(
                                            ChatScreenConstants.LOADING_MORE_ICON_SIZE_DP.dp),
                                    color = appPalette().accent)
                              }
                        }
                      }

                      items(uiState.messages) { message ->
                        MessageBubble(
                            message = message,
                            isOwnMessage = message.senderId == uiState.currentUserProfile?.id,
                            onProfileClick = onProfileClick,
                            modifier =
                                Modifier.padding(vertical = MessageBubbleDimens.MessageSpacing.dp))
                      }
                    }
              }
            }
          }

          // Error handling
          uiState.errorMessage?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
              snackbarHostState.showSnackbar(
                  message = errorMessage,
                  actionLabel = ChatScreenConstants.ERROR_DISMISS,
                  duration = SnackbarDuration.Short)
              viewModel.clearError()
            }
          }
        }
      }
}

/**
 * Message input bar at the bottom of the screen.
 *
 * @param messageText Current message text
 * @param onMessageChange Callback when message text changes
 * @param onSendClick Callback when send button is clicked
 * @param isSending Whether a message is currently being sent
 */
@Composable
private fun MessageInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean
) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = appPalette().surface,
      tonalElevation = UiDimens.CardElevation) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(UiDimens.SpacingMd)
                    .testTag(ChatScreenTestTags.INPUT_ROW),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDimens.SpacingSm)) {
              // Message Input Field
              OutlinedTextField(
                  value = messageText,
                  onValueChange = onMessageChange,
                  modifier = Modifier.weight(WEIGHT).testTag(ChatScreenTestTags.MESSAGE_INPUT),
                  placeholder = {
                    Text(
                        text = ChatScreenConstants.MESSAGE_INPUT_PLACEHOLDER,
                        style = MaterialTheme.typography.bodyMedium)
                  },
                  textStyle = MaterialTheme.typography.bodyMedium,
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = appPalette().accent,
                          unfocusedBorderColor = appPalette().secondary,
                          focusedTextColor = appPalette().text,
                          unfocusedTextColor = appPalette().text),
                  enabled = !isSending,
                  maxLines = MAX_LINES)

              // Send Button
              IconButton(
                  onClick = onSendClick,
                  enabled = messageText.isNotBlank() && !isSending,
                  modifier = Modifier.testTag(ChatScreenTestTags.SEND_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = ChatScreenConstants.SEND_BUTTON_DESCRIPTION,
                        tint =
                            if (messageText.isNotBlank() && !isSending) appPalette().accent
                            else appPalette().secondary)
                  }
            }
      }
}

/**
 * Read-only message bar shown for expired/completed chats.
 *
 * @param requestStatus The status of the request
 */
@Composable
private fun ReadOnlyMessageBar(requestStatus: String?) {
  data class StatusInfo(val icon: ImageVector, val text: String, val backgroundColor: Color)

  val statusInfo: StatusInfo =
      when (requestStatus) {
        "COMPLETED" ->
            StatusInfo(
                icon = Icons.Filled.CheckCircle,
                text = ChatScreenConstants.CHAT_COMPLETED,
                backgroundColor =
                    ChatScreenConstants.COLOR_COMPLETED.copy(
                        alpha = ChatScreenConstants.STATUS_BACKGROUND_ALPHA))
        "EXPIRED" ->
            StatusInfo(
                icon = Icons.Filled.AccessTime,
                text = ChatScreenConstants.CHAT_EXPIRED,
                backgroundColor =
                    ChatScreenConstants.COLOR_EXPIRED.copy(
                        alpha = ChatScreenConstants.STATUS_BACKGROUND_ALPHA))
        "CANCELLED" ->
            StatusInfo(
                icon = Icons.Filled.Cancel,
                text = ChatScreenConstants.CHAT_CANCELLED,
                backgroundColor =
                    ChatScreenConstants.COLOR_CANCELLED.copy(
                        alpha = ChatScreenConstants.STATUS_BACKGROUND_ALPHA))
        else ->
            StatusInfo(
                icon = Icons.Filled.Lock,
                text = ChatScreenConstants.CHAT_CLOSED,
                backgroundColor =
                    appPalette()
                        .secondary
                        .copy(alpha = ChatScreenConstants.STATUS_BACKGROUND_ALPHA))
      }

  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = appPalette().surface,
      tonalElevation = UiDimens.CardElevation) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(UiDimens.SpacingMd),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Surface(
                  shape = RoundedCornerShape(ChatScreenConstants.STATUS_CHIP_CORNER_RADIUS_DP.dp),
                  color = statusInfo.backgroundColor,
                  modifier = Modifier.testTag(ChatScreenTestTags.READ_ONLY_MESSAGE)) {
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = UiDimens.SpacingMd, vertical = UiDimens.SpacingSm),
                        horizontalArrangement = Arrangement.spacedBy(UiDimens.SpacingXs),
                        verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                              imageVector = statusInfo.icon,
                              contentDescription = null,
                              modifier = Modifier.size(ChatScreenConstants.STATUS_ICON_SIZE_DP.dp),
                              tint =
                                  appPalette()
                                      .text
                                      .copy(alpha = ChatScreenConstants.STATUS_ICON_ALPHA))
                          Text(
                              text = statusInfo.text,
                              style = MaterialTheme.typography.bodyMedium,
                              color =
                                  appPalette()
                                      .text
                                      .copy(alpha = ChatScreenConstants.STATUS_TEXT_ALPHA))
                          Icon(
                              imageVector = Icons.Filled.Lock,
                              contentDescription = ChatScreenConstants.READ_ONLY,
                              modifier =
                                  Modifier.size(ChatScreenConstants.STATUS_LOCK_ICON_SIZE_DP.dp),
                              tint =
                                  appPalette()
                                      .text
                                      .copy(alpha = ChatScreenConstants.STATUS_LOCK_ALPHA))
                        }
                  }
            }
      }
}
