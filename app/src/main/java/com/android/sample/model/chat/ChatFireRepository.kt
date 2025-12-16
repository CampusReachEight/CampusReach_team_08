package com.android.sample.model.chat

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

/**
 * Firestore implementation of the ChatRepository interface.
 *
 * This class handles all chat and message operations with Firebase Firestore.
 *
 * Firestore structure:
 * - chats/{chatId} - Chat metadata documents
 * - chats/{chatId}/messages/{messageId} - Message subcollection
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
      if (!createdChat.exists()) {
        throw Exception("Failed to verify chat creation for ID $requestId")
      }
      if (createdChat.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot verify chat creation: data from cache (network unavailable)")
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
      val snapshot = chatsCollectionRef.document(chatId).get(Source.SERVER).await()

      if (snapshot.metadata.isFromCache) {
        throw IllegalStateException("Cannot retrieve chat: data from cache (network unavailable)")
      }

      snapshot.data?.let { Chat.fromMap(it) }
          ?: throw NoSuchElementException("Chat with ID $chatId not found")
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve chat from server", e)
      }
      throw Exception("Failed to retrieve chat with ID $chatId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to retrieve chat with ID $chatId", e)
    }
  }

  override suspend fun getUserChats(userId: String): List<Chat> {
    Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      val snapshot =
          chatsCollectionRef.whereArrayContains("participants", userId).get(Source.SERVER).await()

      if (snapshot.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot retrieve user chats: data from cache (network unavailable)")
      }

      snapshot.documents
          .mapNotNull { doc -> doc.data?.let { Chat.fromMap(it) } }
          .sortedByDescending { it.lastMessageTimestamp } // Most recent first
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve chats from server", e)
      }
      throw Exception("Failed to retrieve chats for user $userId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
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
      if (!chat.participants.contains(currentUserId)) {
        notAuthorized()
      }

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
              mapOf(
                  "lastMessage" to text.trim(),
                  "lastMessageTimestamp" to com.google.firebase.Timestamp(timestamp)))
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

  override suspend fun getMessages(chatId: String): List<Message> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      // Verify user is a participant
      val chat = getChat(chatId)
      if (!chat.participants.contains(currentUserId)) {
        notAuthorized()
      }

      val snapshot =
          chatsCollectionRef
              .document(chatId)
              .collection(MESSAGES_SUBCOLLECTION_PATH)
              .orderBy("timestamp", Query.Direction.ASCENDING) // Oldest first
              .get(Source.SERVER)
              .await()

      if (snapshot.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot retrieve messages: data from cache (network unavailable)")
      }

      snapshot.documents.mapNotNull { doc -> doc.data?.let { Message.fromMap(it) } }
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

  override fun listenToMessages(chatId: String): Flow<List<Message>> = callbackFlow {
    val currentUserId = Firebase.auth.currentUser?.uid
    if (currentUserId == null) {
      close(IllegalStateException("No authenticated user"))
      return@callbackFlow
    }

    // Note: We're not verifying participant status here for performance
    // The security rules on Firestore should handle access control
    val listenerRegistration =
        chatsCollectionRef
            .document(chatId)
            .collection(MESSAGES_SUBCOLLECTION_PATH)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }

              if (snapshot != null) {
                val messages =
                    snapshot.documents.mapNotNull { doc -> doc.data?.let { Message.fromMap(it) } }
                trySend(messages)
              }
            }

    awaitClose { listenerRegistration.remove() }
  }

  override suspend fun updateChatStatus(chatId: String, newStatus: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    try {
      // Verify chat exists and user is a participant
      val chat = getChat(chatId)
      if (!chat.participants.contains(currentUserId)) {
        notAuthorized()
      }

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
      val snapshot = chatsCollectionRef.document(requestId).get(Source.SERVER).await()
      snapshot.exists()
    } catch (e: Exception) {
      false
    }
  }
}
