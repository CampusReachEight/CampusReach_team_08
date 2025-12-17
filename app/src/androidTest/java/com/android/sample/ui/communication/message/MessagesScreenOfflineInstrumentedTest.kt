package com.android.sample.ui.communication.message

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.Chat
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.chat.Message
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.ui.communication.messages.MessagesScreen
import com.android.sample.ui.communication.messages.MessagesViewModel
import com.android.sample.ui.theme.SampleAppTheme
import com.android.sample.utils.BaseEmulatorTest
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagesScreenOfflineInstrumentedTest : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var requestRepository: RequestRepositoryFirestore

  private companion object {
    const val OFFLINE_STATE_TAG = "messages_offline_state"
    const val CHAT_LIST_TAG = "messages_chat_list"
    const val EMPTY_STATE_TAG = "messages_empty_state"

    const val OFFLINE_TITLE = "You're Offline"
    const val OFFLINE_SUBTITLE = "Connect to the internet"

    const val UI_WAIT_TIMEOUT = 10_000L
  }

  @Before
  override fun setUp() {
    super.setUp()
    requestRepository = RequestRepositoryFirestore(db)
  }

  /** Fake ChatRepository that always throws a network unavailable error. */
  private class FakeOfflineChatRepository : ChatRepository {
    override suspend fun createChat(
        requestId: String,
        requestTitle: String,
        participants: List<String>,
        creatorId: String,
        requestStatus: String
    ) {
      throw IllegalStateException("Network unavailable: cannot create chat")
    }

    override suspend fun getChat(chatId: String): Chat {
      throw IllegalStateException("Network unavailable: cannot retrieve chat")
    }

    override suspend fun getUserChats(userId: String): List<Chat> {
      throw IllegalStateException("Network unavailable: cannot retrieve chats from server")
    }

    override suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
      throw IllegalStateException("Network unavailable: cannot send message")
    }

    override suspend fun getMessages(
        chatId: String,
        limit: Int,
        beforeTimestamp: Date?
    ): List<Message> {
      throw IllegalStateException("Network unavailable: cannot retrieve messages")
    }

    override fun listenToNewMessages(chatId: String, sinceTimestamp: Date): Flow<List<Message>> {
      throw IllegalStateException("Network unavailable: cannot listen to messages")
    }

    override suspend fun updateChatStatus(chatId: String, newStatus: String) {
      throw IllegalStateException("Network unavailable: cannot update status")
    }

    override suspend fun chatExists(requestId: String): Boolean {
      throw IllegalStateException("Network unavailable: cannot check chat existence")
    }

    override suspend fun updateChatParticipants(chatId: String, participants: List<String>) {
      throw IllegalStateException("Network unavailable: cannot update participants")
    }

    override suspend fun removeSelfFromChat(chatId: String) {
      throw IllegalStateException("Network unavailable: cannot remove participant")
    }

    override suspend fun deleteChat(chatId: String) {
      throw IllegalStateException("Network unavailable: cannot delete chat")
    }

    override suspend fun addSelfToChat(chatId: String) {
      throw IllegalStateException("Network unavailable: cannot add participant")
    }
  }

  /** Fake ChatRepository that returns empty list (simulates online with no chats). */
  private class FakeEmptyChatRepository : ChatRepository {
    override suspend fun createChat(
        requestId: String,
        requestTitle: String,
        participants: List<String>,
        creatorId: String,
        requestStatus: String
    ) {}

    override suspend fun getChat(chatId: String): Chat {
      throw NoSuchElementException("Chat not found")
    }

    override suspend fun getUserChats(userId: String): List<Chat> {
      return emptyList()
    }

    override suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {}

    override suspend fun getMessages(
        chatId: String,
        limit: Int,
        beforeTimestamp: Date?
    ): List<Message> {
      return emptyList()
    }

    override fun listenToNewMessages(chatId: String, sinceTimestamp: Date): Flow<List<Message>> {
      return flowOf(emptyList())
    }

    override suspend fun updateChatStatus(chatId: String, newStatus: String) {}

    override suspend fun chatExists(requestId: String): Boolean = false

    override suspend fun updateChatParticipants(chatId: String, participants: List<String>) {}

    override suspend fun removeSelfFromChat(chatId: String) {}

    override suspend fun deleteChat(chatId: String) {}

    override suspend fun addSelfToChat(chatId: String) {}
  }

  @Test
  fun messagesScreen_showsOfflineState_whenNetworkUnavailable() {
    // Given
    val offlineChatRepository = FakeOfflineChatRepository()
    val viewModel =
        MessagesViewModel(
            chatRepository = offlineChatRepository,
            requestRepository = requestRepository,
            firebaseAuth = auth)

    // When
    composeTestRule.setContent {
      SampleAppTheme { MessagesScreen(onChatClick = {}, viewModel = viewModel) }
    }

    // Then
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(OFFLINE_STATE_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(OFFLINE_STATE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messagesScreen_displaysOfflineTitle_whenNetworkUnavailable() {
    // Given
    val offlineChatRepository = FakeOfflineChatRepository()
    val viewModel =
        MessagesViewModel(
            chatRepository = offlineChatRepository,
            requestRepository = requestRepository,
            firebaseAuth = auth)

    // When
    composeTestRule.setContent {
      SampleAppTheme { MessagesScreen(onChatClick = {}, viewModel = viewModel) }
    }

    // Then
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(OFFLINE_TITLE, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText(OFFLINE_TITLE, substring = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun messagesScreen_displaysOfflineSubtitle_whenNetworkUnavailable() {
    // Given
    val offlineChatRepository = FakeOfflineChatRepository()
    val viewModel =
        MessagesViewModel(
            chatRepository = offlineChatRepository,
            requestRepository = requestRepository,
            firebaseAuth = auth)

    // When
    composeTestRule.setContent {
      SampleAppTheme { MessagesScreen(onChatClick = {}, viewModel = viewModel) }
    }

    // Then
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(OFFLINE_SUBTITLE, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText(OFFLINE_SUBTITLE, substring = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun messagesScreen_hidesEmptyState_whenOffline() {
    // Given
    val offlineChatRepository = FakeOfflineChatRepository()
    val viewModel =
        MessagesViewModel(
            chatRepository = offlineChatRepository,
            requestRepository = requestRepository,
            firebaseAuth = auth)

    // When
    composeTestRule.setContent {
      SampleAppTheme { MessagesScreen(onChatClick = {}, viewModel = viewModel) }
    }

    // Then
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(OFFLINE_STATE_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(OFFLINE_STATE_TAG).assertExists()
    composeTestRule.onNodeWithTag(EMPTY_STATE_TAG).assertDoesNotExist()
  }

  @Test
  fun messagesScreen_hidesChatList_whenOffline() {
    // Given
    val offlineChatRepository = FakeOfflineChatRepository()
    val viewModel =
        MessagesViewModel(
            chatRepository = offlineChatRepository,
            requestRepository = requestRepository,
            firebaseAuth = auth)

    // When
    composeTestRule.setContent {
      SampleAppTheme { MessagesScreen(onChatClick = {}, viewModel = viewModel) }
    }

    // Then
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(OFFLINE_STATE_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(OFFLINE_STATE_TAG).assertExists()
    composeTestRule.onNodeWithTag(CHAT_LIST_TAG).assertDoesNotExist()
  }

  @Test
  fun messagesScreen_showsEmptyState_whenOnlineWithNoChats() {
    // Given
    val emptyChatRepository = FakeEmptyChatRepository()
    val viewModel =
        MessagesViewModel(
            chatRepository = emptyChatRepository,
            requestRepository = requestRepository,
            firebaseAuth = auth)

    // When
    composeTestRule.setContent {
      SampleAppTheme { MessagesScreen(onChatClick = {}, viewModel = viewModel) }
    }

    // Then
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(EMPTY_STATE_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EMPTY_STATE_TAG).assertExists()
    composeTestRule.onNodeWithTag(OFFLINE_STATE_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CHAT_LIST_TAG).assertDoesNotExist()
  }
}
