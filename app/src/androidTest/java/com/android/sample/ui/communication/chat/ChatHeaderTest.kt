package com.android.sample.ui.communication.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.Chat
import java.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatHeaderTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestChat(requestTitle: String = "Help moving"): Chat {
    return Chat(
        chatId = "chat-123",
        requestId = "request-123",
        requestTitle = requestTitle,
        participants = listOf("user-1", "user-2"),
        creatorId = "user-1",
        lastMessage = "",
        lastMessageTimestamp = Date(),
        requestStatus = "OPEN")
  }

  @Test
  fun chatHeader_displaysCorrectly() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatHeader(chat = chat) }

    composeTestRule.onNodeWithTag("chat_header").assertExists().assertIsDisplayed()
  }

  @Test
  fun chatHeader_displaysTitle() {
    val chat = createTestChat(requestTitle = "Custom Request")

    composeTestRule.setContent { ChatHeader(chat = chat) }

    composeTestRule
        .onNodeWithTag("chat_header_title", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("Custom Request")
  }

  @Test
  fun chatHeader_longTitle_displays() {
    val longTitle = "This is a very long title that might be truncated"
    val chat = createTestChat(requestTitle = longTitle)

    composeTestRule.setContent { ChatHeader(chat = chat) }

    composeTestRule
        .onNodeWithTag("chat_header_title", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }
}
