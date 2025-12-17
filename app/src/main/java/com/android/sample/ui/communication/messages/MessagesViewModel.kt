package com.android.sample.ui.communication.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.chat.Chat
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.chat.ChatRepositoryFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val NO_AUTHENTICATED_USER_ERROR = "No authenticated user"

private const val FAILED_TO_LOAD_CHATS_PLEASE_TRY_AGAIN_ = "Failed to load chats. Please try again."

/**
 * ViewModel for the Messages screen (list of all chats).
 *
 * Responsibilities:
 * - Loading all chats for the current user
 * - Sorting chats by most recent message
 * - Determining user's role in each chat (creator or helper)
 * - Error handling and loading state
 */
class MessagesViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryFirestore(Firebase.firestore),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(MessagesUiState())
  val uiState: StateFlow<MessagesUiState> = _uiState

  init {
    loadChats()
  }

  /**
   * Loads all chats where the current user is a participant. Chats are automatically sorted by most
   * recent message (handled by repository).
   */
  fun loadChats() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      try {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
          _uiState.update { it.copy(isLoading = false, errorMessage = NO_AUTHENTICATED_USER_ERROR) }
          return@launch
        }

        val chats = chatRepository.getUserChats(currentUserId)
        val chatItems =
            chats.map { chat -> ChatItem(chat = chat, isCreator = chat.creatorId == currentUserId) }

        _uiState.update { it.copy(chatItems = chatItems, isLoading = false, errorMessage = null) }
      } catch (e: Exception) {
        val friendly =
            e.message?.takeIf { it.isNotBlank() } ?: FAILED_TO_LOAD_CHATS_PLEASE_TRY_AGAIN_
        _uiState.update { it.copy(isLoading = false, errorMessage = friendly) }
      }
    }
  }

  /** Refreshes the chat list from the server. */
  fun refresh() {
    loadChats()
  }

  /** Clears the current error message, if any. */
  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }
}

private const val UNKNOWN_VIEW_MODEL_CLASS = "Unknown ViewModel class"

/**
 * Factory for creating [MessagesViewModel] instances with custom dependencies.
 *
 * @param chatRepository Optional custom repository (mainly for testing)
 * @param firebaseAuth Optional custom auth instance (mainly for testing)
 */
class MessagesViewModelFactory(
    private val chatRepository: ChatRepository? = null,
    private val firebaseAuth: FirebaseAuth? = null
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MessagesViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return if (chatRepository != null && firebaseAuth != null) {
        MessagesViewModel(chatRepository = chatRepository, firebaseAuth = firebaseAuth) as T
      } else if (chatRepository != null) {
        MessagesViewModel(chatRepository = chatRepository) as T
      } else {
        MessagesViewModel() as T
      }
    }
    throw IllegalArgumentException(UNKNOWN_VIEW_MODEL_CLASS)
  }
}

/**
 * UI state for the messages screen.
 *
 * @property chatItems List of chats with user role information
 * @property isLoading Whether chats are currently being loaded
 * @property errorMessage User-facing error message, if any
 */
data class MessagesUiState(
    val chatItems: List<ChatItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Represents a chat with additional UI information.
 *
 * @property chat The chat data
 * @property isCreator Whether the current user is the creator of the associated request
 */
data class ChatItem(val chat: Chat, val isCreator: Boolean)
