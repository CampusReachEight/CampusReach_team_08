package com.android.sample.ui.communication.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.Message
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageBubbleTest {

  @get:Rule val composeTestRule = createComposeRule()

  private companion object {
    const val BUBBLE_TAG = "message_bubble"
    const val SENDER_NAME_TAG = "message_sender_name"
    const val MESSAGE_TEXT_TAG = "message_text"
    const val TIMESTAMP_TAG = "message_timestamp"
  }

  private fun createTestMessage(
      text: String = "Test message",
      senderId: String = "user-123",
      senderName: String = "Test User"
  ): Message {
    return Message(
        messageId = "msg-123",
        chatId = "chat-123",
        senderId = senderId,
        senderName = senderName,
        text = text,
        timestamp = Date())
  }

  @Test
  fun messageBubble_ownMessage_displaysCorrectly() {
    val message = createTestMessage()

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = true,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule.onNodeWithTag(BUBBLE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messageBubble_ownMessage_doesNotShowSenderName() {
    val message = createTestMessage()

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = true,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule.onNodeWithTag(SENDER_NAME_TAG).assertDoesNotExist()
  }

  @Test
  fun messageBubble_ownMessage_showsMessageText() {
    val message = createTestMessage(text = "My test message")

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = true,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule
        .onNodeWithTag(MESSAGE_TEXT_TAG)
        .assertExists()
        .assertTextEquals("My test message")
  }

  @Test
  fun messageBubble_ownMessage_showsTimestamp() {
    val message = createTestMessage()

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = true,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule.onNodeWithTag(TIMESTAMP_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messageBubble_othersMessage_displaysCorrectly() {
    val message = createTestMessage()

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = false,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule.onNodeWithTag(BUBBLE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messageBubble_othersMessage_showsSenderName() {
    val message = createTestMessage(senderName = "Alice Smith")

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = false,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule.onNodeWithTag(SENDER_NAME_TAG).assertExists().assertTextEquals("Alice Smith")
  }

  @Test
  fun messageBubble_othersMessage_showsMessageText() {
    val message = createTestMessage(text = "Hello from Alice")

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = false,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule
        .onNodeWithTag(MESSAGE_TEXT_TAG)
        .assertExists()
        .assertTextEquals("Hello from Alice")
  }

  @Test
  fun messageBubble_longText_displaysCorrectly() {
    val longText =
        "This is a very long message that should wrap properly " +
            "and display all the content correctly in the bubble"
    val message = createTestMessage(text = longText)

    composeTestRule.setContent {
      MessageBubble(
          message = message,
          isOwnMessage = false,
          profileRepository = UserProfileRepositoryFirestore(Firebase.firestore))
    }

    composeTestRule.onNodeWithTag(MESSAGE_TEXT_TAG).assertExists().assertTextEquals(longText)
  }
}
