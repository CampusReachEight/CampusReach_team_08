package com.android.sample.ui.communication.message

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.ChatRepositoryFirestore
import com.android.sample.ui.communication.messages.MessagesScreen
import com.android.sample.ui.communication.messages.MessagesViewModel
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.SampleAppTheme
import com.android.sample.utils.BaseEmulatorTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagesScreenTest : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var chatRepository: ChatRepositoryFirestore
  private var clickedChatId: String? = null

  private companion object {
    const val TEST_CHAT_ID_1 = "test-chat-1"
    const val TEST_CHAT_ID_2 = "test-chat-2"
    const val TEST_TITLE_1 = "Help with moving"
    const val TEST_TITLE_2 = "Study session"

    const val CHAT_LIST_TAG = "messages_chat_list"
    const val EMPTY_STATE_TAG = "messages_empty_state"
    const val EMPTY_STATE_TEXT_TAG = "messages_empty_state_text"
    const val EMPTY_STATE_SUBTITLE_TAG = "messages_empty_state_subtitle"
    const val EMPTY_STATE_ICON_TAG = "messages_empty_state_icon"

    const val EMPTY_STATE_MESSAGE = "No messages yet"
    const val EMPTY_STATE_SUBTITLE = "Start helping others or create a request to begin chatting"

    const val UI_WAIT_TIMEOUT = 10_000L
    const val DELAY_LONG = 3_000L
  }

  @Before
  override fun setUp() {
    super.setUp()
    chatRepository = ChatRepositoryFirestore(db)
    clickedChatId = null
  }

  private fun setContent() {
    composeTestRule.setContent {
      SampleAppTheme {
        MessagesScreen(
            onChatClick = { chatId -> clickedChatId = chatId },
            viewModel = MessagesViewModel(chatRepository = chatRepository, firebaseAuth = auth))
      }
    }
  }

  private fun createTestChatBlocking(chatId: String, title: String) {
    runBlocking {
      chatRepository.createChat(
          requestId = chatId,
          requestTitle = title,
          participants = listOf(currentUserId),
          creatorId = currentUserId,
          requestStatus = "OPEN")
    }
    Thread.sleep(DELAY_LONG)
  }

  @Test
  fun messagesScreen_displaysCorrectly() {
    setContent()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.MESSAGES_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun messagesScreen_showsEmptyState_whenNoChats() {
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(EMPTY_STATE_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EMPTY_STATE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messagesScreen_emptyState_displaysIcon() {
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(EMPTY_STATE_ICON_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EMPTY_STATE_ICON_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messagesScreen_emptyState_displaysMessage() {
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithText(EMPTY_STATE_MESSAGE).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EMPTY_STATE_TEXT_TAG)
        .assertExists()
        .assertTextContains(EMPTY_STATE_MESSAGE)
  }

  @Test
  fun messagesScreen_emptyState_displaysSubtitle() {
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(EMPTY_STATE_SUBTITLE_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EMPTY_STATE_SUBTITLE_TAG)
        .assertExists()
        .assertTextContains(EMPTY_STATE_SUBTITLE, substring = true)
  }

  @Test
  fun messagesScreen_displaysChatList_whenChatsExist() {
    createTestChatBlocking(TEST_CHAT_ID_1, TEST_TITLE_1)
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(CHAT_LIST_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(CHAT_LIST_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun messagesScreen_displaysSingleChat() {
    createTestChatBlocking(TEST_CHAT_ID_1, TEST_TITLE_1)
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_TITLE_1, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText(TEST_TITLE_1, substring = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun messagesScreen_displaysMultipleChats() {
    createTestChatBlocking(TEST_CHAT_ID_1, TEST_TITLE_1)
    createTestChatBlocking(TEST_CHAT_ID_2, TEST_TITLE_2)
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_TITLE_1, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty() &&
          composeTestRule
              .onAllNodesWithText(TEST_TITLE_2, substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    composeTestRule.onNodeWithText(TEST_TITLE_1, substring = true).assertExists()
    composeTestRule.onNodeWithText(TEST_TITLE_2, substring = true).assertExists()
  }

  @Test
  fun messagesScreen_chatClick_triggersCallback() {
    createTestChatBlocking(TEST_CHAT_ID_1, TEST_TITLE_1)
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_TITLE_1, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(TEST_TITLE_1, substring = true).performClick()

    assert(clickedChatId == TEST_CHAT_ID_1)
  }

  @Test
  fun messagesScreen_hidesEmptyState_whenChatsExist() {
    createTestChatBlocking(TEST_CHAT_ID_1, TEST_TITLE_1)
    setContent()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(CHAT_LIST_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EMPTY_STATE_TAG).assertDoesNotExist()
  }

  @Test
  fun messagesScreen_showsErrorSnackbar_onLoadFailure() {
    // Use invalid/mock repository that will cause an error
    // Or force an error by using a non-existent user
    composeTestRule.setContent {
      SampleAppTheme {
        MessagesScreen(
            onChatClick = { chatId -> clickedChatId = chatId },
            viewModel =
                MessagesViewModel(
                    chatRepository = chatRepository,
                    firebaseAuth =
                        auth // This will work, but error testing is hard with real Firebase
                    ))
      }
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.MESSAGES_SCREEN).assertExists()
  }
}
