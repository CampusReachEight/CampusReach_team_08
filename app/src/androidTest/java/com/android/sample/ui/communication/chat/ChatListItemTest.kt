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
class ChatListItemTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestChat(
      requestTitle: String = "Help with moving",
      lastMessage: String = "Sure, I can help!",
      lastMessageTimestamp: Date = Date(),
      requestStatus: String = "OPEN"
  ): Chat {
    return Chat(
        chatId = "chat-123",
        requestId = "request-123",
        requestTitle = requestTitle,
        participants = listOf("user-1", "user-2"),
        creatorId = "user-1",
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        requestStatus = requestStatus)
  }

  @Test
  fun chatListItem_displaysCorrectly() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule.onNodeWithTag(ChatListItemTestTags.ITEM).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatListItem_isClickable() {
    val chat = createTestChat()
    var clickCount = 0

    composeTestRule.setContent {
      ChatListItem(chat = chat, isCreator = true, onClick = { clickCount++ })
    }

    composeTestRule.onNodeWithTag(ChatListItemTestTags.ITEM).performClick()

    assert(clickCount == 1)
  }

  @Test
  fun chatListItem_displaysTitle() {
    val chat = createTestChat(requestTitle = "Custom Title")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.TITLE, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("Custom Title")
  }

  @Test
  fun chatListItem_displaysLastMessage() {
    val chat = createTestChat(lastMessage = "Hello there!")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.LAST_MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("Hello there!")
  }

  @Test
  fun chatListItem_emptyLastMessage_showsPlaceholder() {
    val chat = createTestChat(lastMessage = "")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.LAST_MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("No messages yet")
  }

  @Test
  fun chatListItem_displaysTimestamp() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.TIMESTAMP, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_creatorBadge_displays() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.ROLE_BADGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_helperBadge_displays() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = false, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.ROLE_BADGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_roleIcon_displays() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.ROLE_ICON, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_helperRoleIcon_displays() {
    val chat = createTestChat()

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = false, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.ROLE_ICON, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_longTitle_truncates() {
    val longTitle =
        "This is an extremely long request title that should be truncated with an ellipsis because it exceeds the maximum line limit"
    val chat = createTestChat(requestTitle = longTitle)

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.TITLE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_longLastMessage_truncates() {
    val longMessage =
        "This is a very long last message that should wrap to multiple lines but will eventually be truncated with an ellipsis after the maximum number of lines"
    val chat = createTestChat(lastMessage = longMessage)

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.LAST_MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  // ==================== EXPIRED CHAT TESTS ====================

  @Test
  fun chatListItem_completedChat_showsStatusBadge() {
    val chat = createTestChat(requestStatus = "COMPLETED")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.STATUS_BADGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_expiredChat_showsStatusBadge() {
    val chat = createTestChat(requestStatus = "EXPIRED")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.STATUS_BADGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_cancelledChat_showsStatusBadge() {
    val chat = createTestChat(requestStatus = "CANCELLED")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.STATUS_BADGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun chatListItem_openChat_doesNotShowStatusBadge() {
    val chat = createTestChat(requestStatus = "OPEN")

    composeTestRule.setContent { ChatListItem(chat = chat, isCreator = true, onClick = {}) }

    composeTestRule
        .onNodeWithTag(ChatListItemTestTags.STATUS_BADGE, useUnmergedTree = true)
        .assertDoesNotExist()
  }
}
