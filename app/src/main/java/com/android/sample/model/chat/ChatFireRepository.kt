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

const val DEFAULT_MESSAGE_LIMIT = 50

private const val ONLY_CREATOR_CAN_UPDATE = "Only creator can update participants"

private const val NETWORK_UNAVAILABLE_CANNOT_UPDATE_PARTICIPANTS =
    "Network unavailable: cannot update participants"

private const val FAILED_TO_UPDATE_PARTICIPANTS = "Failed to update participants for chat"

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
  companion object {
    const val CHATS_COLLECTION_PATH = "chats"
    const val MESSAGES_SUBCOLLECTION_PATH = "messages"

    private const val FAILED_TO_UPDATE_STATUS_FOR_CHAT = "Failed to update status for chat"
    private const val FAILED_TO_RETRIEVE_CHATS_FOR_USER = "Failed to retrieve chats for user"
    private const val NO_AUTHENTICATED_USER = "No authenticated user"
    private const val USER_IS_NOT_A_PARTICIPANT_IN_THIS_CHAT =
        "User is not a participant in this chat"
    private const val CURRENT_USER_PARTICIPANT_OR_CREATOR =
        "Current user must be the creator or a participant"
    private const val CHAT_CREATED = "Chat created"
    private const val FAILED_TO_VERIFY_CHAT = "Failed to verify chat creation for ID"
    private const val CANNOT_VERIFY_CHAT_CREATION =
        "Cannot verify chat creation: data from cache (network unavailable)"
    private const val NETWORK_UNAVAILABLE_CANNOT_CREATE_CHAT =
        "Network unavailable: cannot create chat on server"
    private const val FAILED_TO_CREATE_CHAT_FOR_REQUEST = "Failed to create chat for request"
    private const val CHAT_WITH_ID = "Chat with ID"
    private const val NOT_FOUND = "not found"
    private const val NETWORK_UNAVAILABLE = "Network unavailable: cannot retrieve chat from server"
    private const val FAILED_TO_RETRIEVE_CHAT_WITH_ID = "Failed to retrieve chat with ID"
    private const val CANNOT_RETRIEVE_USER_CHATS =
        "Cannot retrieve user chats: data from cache (network unavailable)"
    private const val PARTICIPANTS = "participants"
    private const val NETWORK_UNAVAILABLE_CANNOT_RETRIEVE_CHATS_FROM_SERVER =
        "Network unavailable: cannot retrieve chats from server"
    private const val CAN_ONLY_SEND_MESSAGES_AS_CURRENT_USER =
        "Can only send messages as the current user"
    private const val MESSAGE_TEXT_CANNOT_BE_EMPTY = "Message text cannot be empty"
    private const val LAST_MESSAGE = "lastMessage"
    private const val LASTMESSAGE_TIMESTAMP = "lastMessageTimestamp"
    private const val NETWORK_UNAVAILABLE_CANNOT_SEND_MESSAGE =
        "Network unavailable: cannot send message to server"
    private const val FAILED_TO_SEND_MESSAGE_TO_CHAT = "Failed to send message to chat"
    private const val TIMESTAMP = "timestamp"
    private const val REQUESTSTATUS = "requestStatus"
    private const val NETWORK_UNAVAILABLE_CANNOT_UPDATE_CHAT_STATUS =
        "Network unavailable: cannot update chat status on server"
    private const val FAILED_TO_RETRIEVE_MESSAGES_FOR_CHAT = "Failed to retrieve messages for chat"
    private const val CANNOT_RETRIEVE_CHAT_FROM_SERVER =
        "Network unavailable: cannot retrieve messages from server"
    private const val CANNOT_RETRIEVE_MESSAGES =
        "Cannot retrieve messages: data from cache (network unavailable)"
  }

  private val chatsCollectionRef = db.collection(CHATS_COLLECTION_PATH)

  private fun notAuthenticated(): Nothing = throw IllegalStateException(NO_AUTHENTICATED_USER)

  override suspend fun createChat(
      requestId: String,
      requestTitle: String,
      participants: List<String>,
      creatorId: String,
      requestStatus: String
  ) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    require(currentUserId == creatorId || participants.contains(currentUserId)) {
      CURRENT_USER_PARTICIPANT_OR_CREATOR
    }

    try {
      val chat =
          Chat(
              chatId = requestId,
              requestId = requestId,
              requestTitle = requestTitle,
              participants = participants,
              creatorId = creatorId,
              lastMessage = CHAT_CREATED,
              lastMessageTimestamp = Date(),
              requestStatus = requestStatus)

      chatsCollectionRef.document(requestId).set(chat.toMap()).await()

      val createdChat = chatsCollectionRef.document(requestId).get(Source.SERVER).await()
      check(createdChat.exists()) { "$FAILED_TO_VERIFY_CHAT $requestId" }
      check(!createdChat.metadata.isFromCache) { CANNOT_VERIFY_CHAT_CREATION }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE_CANNOT_CREATE_CHAT, e)
      }
      throw Exception("$FAILED_TO_CREATE_CHAT_FOR_REQUEST $requestId: ${e.message}", e)
    }
  }

  override suspend fun getChat(chatId: String): Chat {
    return try {
      val snapshot = chatsCollectionRef.document(chatId).get(Source.DEFAULT).await()
      snapshot.data?.let { Chat.fromMap(it) }
          ?: throw NoSuchElementException("$CHAT_WITH_ID $chatId $NOT_FOUND")
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE, e)
      }
      throw Exception("$FAILED_TO_RETRIEVE_CHAT_WITH_ID $chatId: ${e.message}", e)
    }
  }

  override suspend fun getUserChats(userId: String): List<Chat> {
    Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      val snapshot =
          chatsCollectionRef.whereArrayContains(PARTICIPANTS, userId).get(Source.DEFAULT).await()

      check(!snapshot.metadata.isFromCache) { CANNOT_RETRIEVE_USER_CHATS }

      snapshot.documents
          .mapNotNull { doc -> doc.data?.let { Chat.fromMap(it) } }
          .sortedByDescending { it.lastMessageTimestamp }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE_CANNOT_RETRIEVE_CHATS_FROM_SERVER, e)
      }
      throw Exception("$FAILED_TO_RETRIEVE_CHATS_FOR_USER $userId: ${e.message}", e)
    }
  }

  override suspend fun sendMessage(
      chatId: String,
      senderId: String,
      senderName: String,
      text: String
  ) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    require(senderId == currentUserId) { CAN_ONLY_SEND_MESSAGES_AS_CURRENT_USER }
    require(text.isNotBlank()) { MESSAGE_TEXT_CANNOT_BE_EMPTY }

    try {
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { USER_IS_NOT_A_PARTICIPANT_IN_THIS_CHAT }

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

      chatsCollectionRef
          .document(chatId)
          .collection(MESSAGES_SUBCOLLECTION_PATH)
          .document(messageId)
          .set(message.toMap())
          .await()

      chatsCollectionRef
          .document(chatId)
          .update(mapOf(LAST_MESSAGE to text.trim(), LASTMESSAGE_TIMESTAMP to Timestamp(timestamp)))
          .await()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE_CANNOT_SEND_MESSAGE, e)
      }
      throw Exception("$FAILED_TO_SEND_MESSAGE_TO_CHAT $chatId: ${e.message}", e)
    }
  }

  override suspend fun getMessages(
      chatId: String,
      limit: Int,
      beforeTimestamp: Date?
  ): List<Message> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { USER_IS_NOT_A_PARTICIPANT_IN_THIS_CHAT }

      var query =
          chatsCollectionRef
              .document(chatId)
              .collection(MESSAGES_SUBCOLLECTION_PATH)
              .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
              .limit(limit.toLong())

      if (beforeTimestamp != null) {
        query = query.whereLessThan(TIMESTAMP, Timestamp(beforeTimestamp))
      }

      val snapshot = query.get(Source.DEFAULT).await()

      check(!snapshot.metadata.isFromCache) { CANNOT_RETRIEVE_MESSAGES }

      snapshot.documents.mapNotNull { doc -> doc.data?.let { Message.fromMap(it) } }.reversed()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(CANNOT_RETRIEVE_CHAT_FROM_SERVER, e)
      }
      throw Exception("$FAILED_TO_RETRIEVE_MESSAGES_FOR_CHAT $chatId: ${e.message}", e)
    }
  }

  override fun listenToNewMessages(chatId: String, sinceTimestamp: Date): Flow<List<Message>> =
      callbackFlow {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId == null) {
          close(IllegalStateException(NO_AUTHENTICATED_USER))
          return@callbackFlow
        }

        val listenerRegistration =
            chatsCollectionRef
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION_PATH)
                .whereGreaterThan(TIMESTAMP, Timestamp(sinceTimestamp))
                .orderBy(TIMESTAMP, Query.Direction.ASCENDING)
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
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { USER_IS_NOT_A_PARTICIPANT_IN_THIS_CHAT }

      chatsCollectionRef.document(chatId).update(REQUESTSTATUS, newStatus).await()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE_CANNOT_UPDATE_CHAT_STATUS, e)
      }
      throw Exception("$FAILED_TO_UPDATE_STATUS_FOR_CHAT $chatId: ${e.message}", e)
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

  /**
   * Updates the participants of a chat. Only the creator of the chat is allowed to update the
   * participants.
   */
  override suspend fun updateChatParticipants(chatId: String, participants: List<String>) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    try {
      val chat = getChat(chatId)
      check(chat.creatorId == currentUserId) { ONLY_CREATOR_CAN_UPDATE }

      chatsCollectionRef.document(chatId).update(PARTICIPANTS, participants).await()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE_CANNOT_UPDATE_PARTICIPANTS, e)
      }
      throw Exception("$FAILED_TO_UPDATE_PARTICIPANTS $chatId: ${e.message}", e)
    }
  }
  /**
   * Removes the current user from chat participants. Any participant can remove themselves (used
   * when canceling acceptance).
   */
  override suspend fun removeSelfFromChat(chatId: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    try {
      val chat = getChat(chatId)
      check(chat.participants.contains(currentUserId)) { USER_IS_NOT_A_PARTICIPANT_IN_THIS_CHAT }

      // Remove current user from participants
      val updatedParticipants = chat.participants.filter { it != currentUserId }

      chatsCollectionRef.document(chatId).update(PARTICIPANTS, updatedParticipants).await()
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(NETWORK_UNAVAILABLE_CANNOT_UPDATE_PARTICIPANTS, e)
      }
      throw Exception("$FAILED_TO_UPDATE_PARTICIPANTS $chatId: ${e.message}", e)
    }
  }
}
