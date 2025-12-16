package com.android.sample.model.chat

import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

const val CHATS_COLLECTION_PATH = "chats"
const val MESSAGES_SUBCOLLECTION_PATH = "messages"
const val DEFAULT_MESSAGE_LIMIT = 50

/**
 * Firestore implementation of the ChatRepository interface.
 *
 * This class handles all chat and message operations with Firebase Firestore.
 *
 * Firestore structure:
 * - chats/{chatId} - Chat metadata documents
 * - chats/{chatId}/messages/{messageId} - Message subcollection
 *
 * OPTIMIZATION NOTES:
 * - Uses pagination for message loading (default 50 messages)
 * - Real-time listener only listens for NEW messages, not all messages
 * - Uses Source.DEFAULT to leverage Firestore cache when possible
 */
class ChatRepositoryFirestore(private val db: FirebaseFirestore) : ChatRepository {

  private val chatsCollectionRef = db.collection(CHATS_COLLECTION_PATH)

  private fun notAuthenticated(): Nothing = throw IllegalStateException("No authenticated user")

  private fun notAuthorized(): Nothing =
      throw IllegalArgumentException("User is not a participant in this chat")

  override suspend fun createChat(
      requestId: String,
      requestTitle: String,
      participants: List<String>,
      creatorId: String,
      requestStatus: String
  ) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify current user is either creator or participant
    require(currentUserId == creatorId || participants.contains(currentUserId)) {
      "Current user must be the creator or a participant"
    }

    try {
      val chat =
          Chat(
              chatId = requestId,
              requestId = requestId,
              requestTitle = requestTitle,
              participants = participants,
              creatorId = creatorId,
              lastMessage = "Chat created",
              lastMessageTimestamp = Date(),
              requestStatus = requestStatus)

      chatsCollectionRef.document(requestId).set(chat.toMap()).await()

      // Verify the chat was created
      val createdChat = chatsCollectionRef.document(requestId).get(Source.SERVER).await()
      check(createdChat.exists()) { "Failed to verify chat creation for ID $requestId" }
      check(!createdChat.metadata.isFromCache) {
        "Cannot verify chat creation: data from cache (network unavailable)"
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot create chat on server", e)
      }
      throw Exception("Failed to create chat for request $requestId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to create chat for request $requestId", e)
    }
  }

  override suspend fun getChat(chatId: String): Chat {
    return try {
      // OPTIMIZATION: Use Source.DEFAULT to leverage cache
      val snapshot = chatsCollectionRef.document(chatId).get(Source.DEFAULT).await()

      snapshot.data?.let { Chat.fromMap(it) }
          ?: throw NoSuchElementException("Chat with ID $chatId not found")
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve chat from server", e)
      }
      throw Exception("Failed to retrieve chat with ID $chatId: ${e.message}", e)
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to retrieve chat with ID $chatId", e)
    }
  }

  override suspend fun getUserChats(userId: String): List<Chat> {
    Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      // OPTIMIZATION: Use Source.DEFAULT to leverage cache
      val snapshot =
          chatsCollectionRef.whereArrayContains("participants", userId).get(Source.DEFAULT).await()

      check(!snapshot.metadata.isFromCache) {
        "Cannot retrieve user chats: data from cache (network unavailable)"
      }

      snapshot.documents
          .mapNotNull { doc -> doc.data?.let { Chat.fromMap(it) } }
          .sortedByDescending { it.lastMessageTimestamp } // Most recent first
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve chats from server", e)
      }
      throw Exception("Failed to retrieve chats for user $userId: ${e.message}", e)
    } catch (e: Exception) {
      throw Exception("Failed to retrieve chats for user $userId", e)
    }
  }

  override suspend fun sendMessage(
      chatId: String,
      senderId: String,
      senderName: String,
      text: String
  ) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify sender is current user
    require(senderId == currentUserId) { "Can only send messages as the current user" }

    // Verify text is not empty
    require(text.isNotBlank()) { "Message text cannot be empty" }

    try {
      // Verify chat exists and user is a participant
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { "User is not a participant in this chat" }

      val messageId = Message.generateMessageId()
      val timestamp = Date()

      val message =
          Message(
              messageId = messageId,
              chatId = chatId,
              senderId = senderId,
              senderName = senderName,
              text = text.trim(),
              timestamp = timestamp)

      // Add message to subcollection
      chatsCollectionRef
          .document(chatId)
          .collection(MESSAGES_SUBCOLLECTION_PATH)
          .document(messageId)
          .set(message.toMap())
          .await()

      // Update chat metadata with last message info
      chatsCollectionRef
          .document(chatId)
          .update(
              mapOf("lastMessage" to text.trim(), "lastMessageTimestamp" to Timestamp(timestamp)))
          .await()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot send message to server", e)
      }
      throw Exception("Failed to send message to chat $chatId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to send message to chat $chatId", e)
    }
  }

  /**
   * Retrieves messages with pagination support.
   *
   * OPTIMIZATION: Limits number of messages loaded to reduce Firebase reads.
   * - Query orders by timestamp descending to get most recent first
   * - Results are reversed to return oldest-to-newest for UI display
   * - Use beforeTimestamp for pagination ("Load More" functionality)
   */
  override suspend fun getMessages(
      chatId: String,
      limit: Int,
      beforeTimestamp: Date?
  ): List<Message> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      // Verify user is a participant
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { "User is not a participant in this chat" }

      // Build query with pagination
      var query =
          chatsCollectionRef
              .document(chatId)
              .collection(MESSAGES_SUBCOLLECTION_PATH)
              .orderBy("timestamp", Query.Direction.DESCENDING)
              .limit(limit.toLong())

      // Add pagination cursor if provided
      if (beforeTimestamp != null) {
        query = query.whereLessThan("timestamp", Timestamp(beforeTimestamp))
      }

      // OPTIMIZATION: Use Source.DEFAULT to leverage cache
      val snapshot = query.get(Source.DEFAULT).await()

      check(!snapshot.metadata.isFromCache) {
        "Cannot retrieve messages: data from cache (network unavailable)"
      }

      // Reverse to get oldest-to-newest order
      snapshot.documents.mapNotNull { doc -> doc.data?.let { Message.fromMap(it) } }.reversed()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve messages from server", e)
      }
      throw Exception("Failed to retrieve messages for chat $chatId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to retrieve messages for chat $chatId", e)
    }
  }

  /**
   * Listens only to NEW messages after the specified timestamp.
   *
   * OPTIMIZATION: This is dramatically more efficient than listening to all messages.
   * - Only queries messages with timestamp > sinceTimestamp
   * - Avoids re-reading existing messages on every new message
   * - Reduces reads by ~99% compared to listening to all messages
   */
  override fun listenToNewMessages(chatId: String, sinceTimestamp: Date): Flow<List<Message>> =
      callbackFlow {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId == null) {
          close(IllegalStateException("No authenticated user"))
          return@callbackFlow
        }

        // Listen only for messages AFTER sinceTimestamp
        val listenerRegistration =
            chatsCollectionRef
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION_PATH)
                .whereGreaterThan("timestamp", Timestamp(sinceTimestamp))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                  if (error != null) {
                    close(error)
                    return@addSnapshotListener
                  }

                  if (snapshot != null && !snapshot.isEmpty) {
                    val newMessages =
                        snapshot.documents.mapNotNull { doc ->
                          doc.data?.let { Message.fromMap(it) }
                        }
                    trySend(newMessages)
                  }
                }

        awaitClose { listenerRegistration.remove() }
      }

  override suspend fun updateChatStatus(chatId: String, newStatus: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    try {
      // Verify chat exists and user is a participant
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { "User is not a participant in this chat" }

      chatsCollectionRef.document(chatId).update("requestStatus", newStatus).await()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot update chat status on server", e)
      }
      throw Exception("Failed to update status for chat $chatId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to update status for chat $chatId", e)
    }
  }

  override suspend fun chatExists(requestId: String): Boolean {
    return try {
      val snapshot = chatsCollectionRef.document(requestId).get(Source.DEFAULT).await()
      snapshot.exists()
    } catch (e: Exception) {
      false
    }
  }
}
