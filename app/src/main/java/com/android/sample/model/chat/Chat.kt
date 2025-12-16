package com.android.sample.model.chat

import com.android.sample.model.serializers.DateSerializer
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.serialization.Serializable

private const val CHAT_ID = "chatId"

private const val REQUEST_ID = "requestId"

private const val REQUEST_TITLE = "requestTitle"

private const val PARTICIPANTS = "participants"

private const val CREATOR_ID = "creatorId"

private const val LAST_MESSAGE = "lastMessage"

private const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"

private const val REQUEST_STATUS = "requestStatus"

private const val OPEN = "OPEN"

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
          chatId = data[CHAT_ID] as String,
          requestId = data[REQUEST_ID] as String,
          requestTitle = data[REQUEST_TITLE] as String,
          participants = (data[PARTICIPANTS] as List<*>).map { it as String },
          creatorId = data[CREATOR_ID] as String,
          lastMessage = data[LAST_MESSAGE] as? String ?: "",
          lastMessageTimestamp = (data[LAST_MESSAGE_TIMESTAMP] as? Timestamp)?.toDate() ?: Date(),
          requestStatus = data[REQUEST_STATUS] as? String ?: OPEN)
    }
  }

  /** Serializes the Chat to a Firestore-compatible map. */
  fun toMap(): Map<String, Any?> =
      mapOf(
          CHAT_ID to chatId,
          REQUEST_ID to requestId,
          REQUEST_TITLE to requestTitle,
          PARTICIPANTS to participants,
          CREATOR_ID to creatorId,
          LAST_MESSAGE to lastMessage,
          LAST_MESSAGE_TIMESTAMP to Timestamp(lastMessageTimestamp),
          REQUEST_STATUS to requestStatus)
}
