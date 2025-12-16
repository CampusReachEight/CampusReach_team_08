package com.android.sample.model.chat

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
   * Retrieves all messages for a specific chat, ordered by timestamp (oldest first).
   *
   * @param chatId The ID of the chat
   * @return List of messages
   */
  suspend fun getMessages(chatId: String): List<Message>

  /**
   * Returns a Flow that emits real-time updates for messages in a chat. This enables live message
   * synchronization.
   *
   * @param chatId The ID of the chat
   * @return Flow of message lists (updates in real-time)
   */
  fun listenToMessages(chatId: String): Flow<List<Message>>

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
}
