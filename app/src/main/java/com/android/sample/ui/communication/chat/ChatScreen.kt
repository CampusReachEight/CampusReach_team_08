package com.android.sample.ui.communication.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

// Test Tags
object ChatScreenTestTags {
  const val SCREEN = "chat_screen"
  const val TOP_BAR = "chat_top_bar"
  const val BACK_BUTTON = "chat_back_button"
  const val LOADING_INDICATOR = "chat_loading_indicator"
  const val ERROR_MESSAGE = "chat_error_message"
  const val MESSAGE_LIST = "chat_message_list"
  const val MESSAGE_INPUT = "chat_message_input"
  const val SEND_BUTTON = "chat_send_button"
  const val INPUT_ROW = "chat_input_row"
}

// Constants
private object ChatScreenConstants {
  const val BACK_BUTTON_DESCRIPTION = "Navigate back"
  const val SEND_BUTTON_DESCRIPTION = "Send message"
  const val MESSAGE_INPUT_PLACEHOLDER = "Type a message..."
  const val ERROR_DISMISS = "Dismiss"
}

private const val WEIGHT = 1f

private const val DECREASE = 1

/**
 * Chat conversation screen.
 *
 * Features:
 * - Chat header with request details
 * - Scrollable message list
 * - Message input field with send button
 * - Real-time message updates
 * - Auto-scroll to bottom on new messages
 *
 * @param chatId The ID of the chat to display
 * @param onBackClick Callback when back button is clicked
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

  // Initialize chat when screen is created
  LaunchedEffect(chatId) { viewModel.initializeChat(chatId) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            firstVisibleItemIndex == 0 && !uiState.isLoadingMore && uiState.messages.isNotEmpty()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreMessages()
        }
    }
  // Auto-scroll to bottom when new messages arrive
  LaunchedEffect(uiState.messages.size) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.size - DECREASE)
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.CHAT_SCREEN),
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
        MessageInputBar(
            messageText = uiState.messageInput,
            onMessageChange = viewModel::updateMessageInput,
            onSendClick = { viewModel.sendMessage(uiState.messageInput) },
            isSending = uiState.isSendingMessage)
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
              Column(modifier = Modifier.fillMaxSize()) {
                // Chat Header (Request Details)
                uiState.chat?.let { chat -> ChatHeader(chat = chat) }

                // Messages List
                  LazyColumn(
                      modifier = Modifier
                          .fillMaxSize()
                          .testTag(ChatScreenTestTags.MESSAGE_LIST),
                      state = listState,
                      contentPadding = PaddingValues(
                          vertical = MessageBubbleDimens.VerticalPadding.dp,
                          horizontal = MessageBubbleDimens.HorizontalPadding.dp
                      )
                  ) {
                      // Loading indicator at top
                      if (uiState.isLoadingMore) {
                          item {
                              Box(
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .padding(MessageBubbleDimens.VerticalPadding.dp),
                                  contentAlignment = Alignment.Center
                              ) {
                                  CircularProgressIndicator(
                                      modifier = Modifier.size(24.dp),
                                      color = appPalette().accent
                                  )
                              }
                          }
                      }

                      items(uiState.messages) { message ->
                          MessageBubble(
                              message = message,
                              isOwnMessage = message.senderId == uiState.currentUserProfile?.id,
                              onProfileClick = onProfileClick,
                              modifier = Modifier.padding(vertical = MessageBubbleDimens.MessageSpacing.dp)
                          )
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

private const val MAX_LINES = 3

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
