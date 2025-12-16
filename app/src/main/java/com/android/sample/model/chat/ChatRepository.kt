package com.android.sample.model.chat

import java.util.Date
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing chat and message operations.
 *
 * This interface defines the contract for chat-related data operations, including creating chats,
 * sending/retrieving messages, and managing chat metadata.
 */
interface ChatRepository {

  /**
   * Creates a new chat for a request. The chatId will be the same as the requestId.
   *
   * @param requestId The ID of the request
   * @param requestTitle The title of the request
   * @param participants List of user IDs participating in the chat
   * @param creatorId The ID of the request creator
   * @param requestStatus Current status of the request
   */
  suspend fun createChat(
      requestId: String,
      requestTitle: String,
      participants: List<String>,
      creatorId: String,
      requestStatus: String
  )

  /**
   * Retrieves chat metadata by chatId (same as requestId).
   *
   * @param chatId The ID of the chat to retrieve
   * @return Chat metadata
   * @throws NoSuchElementException if chat doesn't exist
   */
  suspend fun getChat(chatId: String): Chat

  /**
   * Retrieves all chats where the current user is a participant. Results are sorted by
   * lastMessageTimestamp (most recent first).
   *
   * @param userId The current user's ID
   * @return List of chats the user participates in
   */
  suspend fun getUserChats(userId: String): List<Chat>

  /**
   * Sends a message to a chat.
   *
   * @param chatId The ID of the chat
   * @param senderId The ID of the message sender
   * @param senderName The display name of the sender
   * @param text The message content
   * @throws IllegalStateException if user is not a participant
   */
  suspend fun sendMessage(chatId: String, senderId: String, senderName: String, text: String)

  /**
   * Retrieves messages for a specific chat with pagination support. Messages are ordered by
   * timestamp (newest first for pagination, but returned oldest first).
   *
   * OPTIMIZATION: Use pagination to reduce Firebase reads.
   * - Initial load: getMessages(chatId, limit = 50) → 50 reads instead of 1000+
   * - Load more: getMessages(chatId, limit = 50, beforeTimestamp = oldestMessageDate)
   *
   * @param chatId The ID of the chat
   * @param limit Maximum number of messages to retrieve (default: 50)
   * @param beforeTimestamp Load messages older than this timestamp (for pagination)
   * @return List of messages (ordered oldest to newest)
   */
  suspend fun getMessages(
      chatId: String,
      limit: Int = DEFAULT_MESSAGE_LIMIT,
      beforeTimestamp: Date? = null
  ): List<Message>

  /**
   * Returns a Flow that emits only NEW messages after the specified timestamp. This is more
   * efficient than listening to all messages.
   *
   * OPTIMIZATION: Listen only to new messages to reduce Firebase reads.
   * - Old way: 100 existing messages + 1 new = 101 reads per user
   * - New way: 1 new message = 1 read per user
   *
   * Usage:
   * 1. Load initial messages with getMessages(chatId, limit = 50)
   * 2. Get timestamp of last message
   * 3. Listen for new messages: listenToNewMessages(chatId, lastTimestamp)
   *
   * @param chatId The ID of the chat
   * @param sinceTimestamp Only listen for messages after this timestamp
   * @return Flow of new messages (emits only new messages as they arrive)
   */
  fun listenToNewMessages(chatId: String, sinceTimestamp: Date): Flow<List<Message>>

  /**
   * Updates the chat metadata when the request status changes.
   *
   * @param chatId The ID of the chat
   * @param newStatus The new request status
   */
  suspend fun updateChatStatus(chatId: String, newStatus: String)

  /**
   * Checks if a chat exists for a given request.
   *
   * @param requestId The request ID
   * @return true if chat exists, false otherwise
   */
  suspend fun chatExists(requestId: String): Boolean

  /** Updates the list of participants in a chat. */
  suspend fun updateChatParticipants(chatId: String, participants: List<String>)
}
