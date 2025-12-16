package com.android.sample.model.chat

import com.android.sample.model.serializers.DateSerializer
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.serialization.Serializable

/**
 * Data class representing chat metadata (not individual messages).
 *
 * This stores high-level information about a chat associated with a request. The actual messages
 * are stored in a subcollection: chats/{chatId}/messages/
 *
 * Fields:
 * - chatId: Unique identifier for the chat (same as requestId)
 * - requestId: The request this chat is associated with
 * - requestTitle: Title of the request (for display in chat list)
 * - participants: List of user IDs participating in this chat
 * - creatorId: ID of the request creator
 * - lastMessage: Preview of the most recent message
 * - lastMessageTimestamp: When the last message was sent
 * - requestStatus: Current status of the request (to show visual indicators)
 *
 * The class includes methods for serialization to/from Firestore document format.
 */
@Serializable
data class Chat(
    val chatId: String, // Same as requestId
    val requestId: String, // Redundant but explicit for clarity
    val requestTitle: String,
    val participants: List<String>, // List of user IDs
    val creatorId: String, // To determine if current user is creator or helper
    val lastMessage: String,
    @Serializable(with = DateSerializer::class) val lastMessageTimestamp: Date,
    val requestStatus: String // "OPEN", "IN_PROGRESS", "COMPLETED", etc.
) {

  companion object {
    /** Deserializes a Chat from Firestore document data. */
    fun fromMap(data: Map<String, Any?>): Chat {
      return Chat(
          chatId = data["chatId"] as String,
          requestId = data["requestId"] as String,
          requestTitle = data["requestTitle"] as String,
          participants = (data["participants"] as List<*>).map { it as String },
          creatorId = data["creatorId"] as String,
          lastMessage = data["lastMessage"] as? String ?: "",
          lastMessageTimestamp = (data["lastMessageTimestamp"] as? Timestamp)?.toDate() ?: Date(),
          requestStatus = data["requestStatus"] as? String ?: "OPEN")
    }
  }

  /** Serializes the Chat to a Firestore-compatible map. */
  fun toMap(): Map<String, Any?> =
      mapOf(
          "chatId" to chatId,
          "requestId" to requestId,
          "requestTitle" to requestTitle,
          "participants" to participants,
          "creatorId" to creatorId,
          "lastMessage" to lastMessage,
          "lastMessageTimestamp" to Timestamp(lastMessageTimestamp),
          "requestStatus" to requestStatus)
}
