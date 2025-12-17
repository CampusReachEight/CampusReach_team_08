package com.android.sample.model.chat

import com.android.sample.model.serializers.DateSerializer
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Data class representing a single chat message.
 *
 * Fields:
 * - messageId: Unique identifier for the message
 * - chatId: ID of the chat this message belongs to (same as requestId)
 * - senderId: User ID of the message sender
 * - senderName: Display name of the sender (for convenience, avoids extra lookups)
 * - text: The message content
 * - timestamp: When the message was sent
 *
 * The class includes methods for serialization to/from Firestore document format.
 */
@Serializable
data class Message(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    @Serializable(with = DateSerializer::class) val timestamp: Date
) {

  companion object {
    /** Generates a new unique message ID. */
    fun generateMessageId(): String = UUID.randomUUID().toString()

    /** Deserializes a Message from Firestore document data. */
    fun fromMap(data: Map<String, Any?>): Message {
      return Message(
          messageId = data["messageId"] as String,
          chatId = data["chatId"] as String,
          senderId = data["senderId"] as String,
          senderName = data["senderName"] as String,
          text = data["text"] as String,
          timestamp = (data["timestamp"] as Timestamp).toDate())
    }
  }

  /** Serializes the Message to a Firestore-compatible map. */
  fun toMap(): Map<String, Any?> =
      mapOf(
          "messageId" to messageId,
          "chatId" to chatId,
          "senderId" to senderId,
          "senderName" to senderName,
          "text" to text,
          "timestamp" to Timestamp(timestamp))
}
