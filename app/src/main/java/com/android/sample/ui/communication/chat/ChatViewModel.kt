package com.android.sample.ui.communication.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.chat.Chat
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.chat.ChatRepositoryFirestore
import com.android.sample.model.chat.Message
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CANNOT_SEE_MESSAGES_INVALID_STATE = "Cannot send message: invalid state"

/**
 * ViewModel for the Chat conversation screen.
 *
 * Responsibilities:
 * - Loading chat metadata and messages
 * - Sending new messages
 * - Real-time message updates via Flow
 * - Managing message input state
 * - Error handling and loading state
 */
class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryFirestore(Firebase.firestore),
    private val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState

  private var currentChatId: String? = null

  /**
   * Initializes the chat screen with the given chat ID. Loads chat metadata, user profile, and
   * starts listening to messages.
   *
   * @param chatId The ID of the chat to display
   */
  fun initializeChat(chatId: String) {
    currentChatId = chatId
    _uiState.update { it.copy(isLoading = true) }

    viewModelScope.launch {
      try {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
          _uiState.update { it.copy(isLoading = false, errorMessage = "No authenticated user") }
          return@launch
        }

        // Load chat metadata
        val chat = chatRepository.getChat(chatId)

        // Load current user's profile for sender name
        val userProfile = profileRepository.getUserProfile(currentUserId)

        _uiState.update {
          it.copy(
              chat = chat, currentUserProfile = userProfile, isLoading = false, errorMessage = null)
        }

        // Start listening to messages in real-time
        listenToNewMessages(chatId, Date())
      } catch (e: Exception) {
        val friendly =
            e.message?.takeIf { it.isNotBlank() } ?: "Failed to load chat. Please try again."
        _uiState.update { it.copy(isLoading = false, errorMessage = friendly) }
      }
    }
  }

  /** Starts listening to real-time message updates for the given chat. */
  /**
   * Starts listening to real-time NEW messages only (not all messages).
   *
   * OPTIMIZATION: Only listens for messages after sinceTimestamp.
   * - Avoids re-reading existing messages on every new message
   * - Dramatically reduces Firebase reads (~99% reduction)
   */
  private fun listenToNewMessages(chatId: String, sinceTimestamp: Date) {
    viewModelScope.launch {
      chatRepository
          .listenToNewMessages(chatId, sinceTimestamp)
          .catch { e ->
            val friendly =
                e.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to load messages. Please try again."
            _uiState.update { it.copy(errorMessage = friendly) }
          }
          .collect { newMessages ->
            // Append new messages to existing list
            _uiState.update { state ->
              state.copy(
                  messages =
                      (state.messages + newMessages)
                          .distinctBy { it.messageId }
                          .sortedBy { it.timestamp })
            }
          }
    }
  }

  /**
   * Sends a new message to the current chat.
   *
   * @param text The message text to send
   */
  fun sendMessage(text: String) {
    val chatId = currentChatId
    val userId = firebaseAuth.currentUser?.uid
    val userProfile = _uiState.value.currentUserProfile

    if (chatId == null || userId == null || userProfile == null) {
      _uiState.update { it.copy(errorMessage = CANNOT_SEE_MESSAGES_INVALID_STATE) }
      return
    }

    if (text.isBlank()) {
      return // Don't send empty messages
    }

    _uiState.update { it.copy(isSendingMessage = true) }

    viewModelScope.launch {
      try {
        val senderName = "${userProfile.name} ${userProfile.lastName}"
        chatRepository.sendMessage(
            chatId = chatId, senderId = userId, senderName = senderName, text = text)

        // Clear message input after successful send
        _uiState.update {
          it.copy(messageInput = "", isSendingMessage = false, errorMessage = null)
        }
      } catch (e: Exception) {
        val friendly =
            e.message?.takeIf { it.isNotBlank() } ?: "Failed to send message. Please try again."
        _uiState.update { it.copy(isSendingMessage = false, errorMessage = friendly) }
      }
    }
  }

  /**
   * Updates the message input text.
   *
   * @param text The new input text
   */
  fun updateMessageInput(text: String) {
    _uiState.update { it.copy(messageInput = text) }
  }

  /** Clears the current error message, if any. */
  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  /** Refreshes the chat data (metadata and messages). */
  fun refresh() {
    currentChatId?.let { initializeChat(it) }
  }
}

private const val UNKNOWN_VIEW_MODEL_ERROR = "Unknown ViewModel class"

/**
 * Factory for creating [ChatViewModel] instances with custom dependencies.
 *
 * @param chatRepository Optional custom chat repository (mainly for testing)
 * @param profileRepository Optional custom profile repository (mainly for testing)
 * @param firebaseAuth Optional custom auth instance (mainly for testing)
 */
class ChatViewModelFactory(
    private val chatRepository: ChatRepository? = null,
    private val profileRepository: UserProfileRepository? = null,
    private val firebaseAuth: FirebaseAuth? = null
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return when {
        chatRepository != null && profileRepository != null && firebaseAuth != null -> {
          ChatViewModel(
              chatRepository = chatRepository,
              profileRepository = profileRepository,
              firebaseAuth = firebaseAuth)
              as T
        }
        chatRepository != null && profileRepository != null -> {
          ChatViewModel(chatRepository = chatRepository, profileRepository = profileRepository) as T
        }
        chatRepository != null -> {
          ChatViewModel(chatRepository = chatRepository) as T
        }
        else -> ChatViewModel() as T
      }
    }
    throw IllegalArgumentException(UNKNOWN_VIEW_MODEL_ERROR)
  }
}

/**
 * UI state for the chat conversation screen.
 *
 * @property chat The chat metadata (title, participants, status, etc.)
 * @property messages List of messages in the chat (ordered oldest to newest)
 * @property currentUserProfile The current user's profile (for sender name)
 * @property messageInput Current text in the message input field
 * @property isLoading Whether initial chat data is loading
 * @property isSendingMessage Whether a message is currently being sent
 * @property errorMessage User-facing error message, if any
 */
data class ChatUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val currentUserProfile: UserProfile? = null,
    val messageInput: String = "",
    val isLoading: Boolean = false,
    val isSendingMessage: Boolean = false,
    val errorMessage: String? = null
)
