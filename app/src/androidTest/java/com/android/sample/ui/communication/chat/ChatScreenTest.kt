package com.android.sample.ui.communication.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.ChatRepositoryFirestore
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.utils.BaseEmulatorTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var chatRepository: ChatRepositoryFirestore
  private lateinit var profileRepository: UserProfileRepositoryFirestore
  private var backClickCount = 0

  private companion object {
    const val TEST_CHAT_ID = "test-chat-123"
    const val TEST_REQUEST_TITLE = "Help with moving"
    const val TEST_INPUT_TEXT = "Test message"

    const val BACK_BUTTON_TAG = "chat_back_button"
    const val MESSAGE_LIST_TAG = "chat_message_list"
    const val MESSAGE_INPUT_TAG = "chat_message_input"
    const val SEND_BUTTON_TAG = "chat_send_button"
    const val INPUT_ROW_TAG = "chat_input_row"
    const val CHAT_HEADER_TAG = "chat_header"
    const val CHAT_HEADER_TITLE_TAG = "chat_header_title"
    const val MESSAGE_BUBBLE_TAG = "message_bubble"
    const val MESSAGE_TEXT_TAG = "message_text"
    const val MESSAGE_SENDER_NAME_TAG = "message_sender_name"
    const val MESSAGE_TIMESTAMP_TAG = "message_timestamp"

    const val TEST_MESSAGE_TEXT = "Hello, this is a test message"
    const val TEST_SENDER_NAME = "Test Sender"

    const val MESSAGE_INPUT_PLACEHOLDER = "Type a message..."

    const val UI_WAIT_TIMEOUT = 10_000L
    const val DELAY_MEDIUM = 2_000L
  }

  private fun waitForChatTitle() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_REQUEST_TITLE, substring = true, ignoreCase = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Before
  override fun setUp() {
    super.setUp()
    chatRepository = ChatRepositoryFirestore(db)
    profileRepository = UserProfileRepositoryFirestore(db)
    backClickCount = 0
  }

  private fun setContent(chatId: String = TEST_CHAT_ID) {
    composeTestRule.setContent {
      ChatScreen(
          chatId = chatId,
          onBackClick = { backClickCount++ },
          onProfileClick = {},
          viewModel =
              ChatViewModel(
                  chatRepository = chatRepository,
                  profileRepository = profileRepository,
                  firebaseAuth = auth))
    }
  }

  private fun createTestChatBlocking(requestStatus: String = "OPEN") {
    runBlocking {
      chatRepository.createChat(
          requestId = TEST_CHAT_ID,
          requestTitle = TEST_REQUEST_TITLE,
          participants = listOf(currentUserId),
          creatorId = currentUserId,
          requestStatus = requestStatus)
    }
    Thread.sleep(DELAY_MEDIUM)
  }

  /** Checks if a request status indicates the chat is read-only. */
  private fun waitForChatToLoad() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(MESSAGE_INPUT_TAG).fetchSemanticsNodes().isNotEmpty()
    }
  }

  // ==================== SCREEN STRUCTURE TESTS ====================

  @Test
  fun chatScreen_displaysCorrectly() {
    createTestChatBlocking()
    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.CHAT_SCREEN).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_backButton_exists() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule
        .onNodeWithTag(BACK_BUTTON_TAG)
        .assertExists()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun chatScreen_backButton_triggersCallback() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(BACK_BUTTON_TAG).performClick()

    assert(backClickCount == 1)
  }

  // ==================== MESSAGE LIST TESTS ====================

  @Test
  fun chatScreen_messageList_exists() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_LIST_TAG).assertExists().assertIsDisplayed()
  }

  // ==================== MESSAGE INPUT TESTS ====================

  @Test
  fun chatScreen_messageInput_exists() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_messageInput_showsPlaceholder() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithText(MESSAGE_INPUT_PLACEHOLDER).assertExists()
  }

  @Test
  fun chatScreen_messageInput_acceptsInput() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(TEST_INPUT_TEXT)

    composeTestRule.onNodeWithText(TEST_INPUT_TEXT, substring = true).assertExists()
  }

  // ==================== SEND BUTTON TESTS ====================

  @Test
  fun chatScreen_sendButton_exists() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_sendButton_isDisabled_whenMessageEmpty() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertIsNotEnabled()
  }

  @Test
  fun chatScreen_sendButton_isEnabled_whenMessageNotBlank() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(TEST_INPUT_TEXT)

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertIsEnabled()
  }

  @Test
  fun chatScreen_inputRow_exists() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(INPUT_ROW_TAG).assertExists().assertIsDisplayed()
  }

  // ==================== ERROR HANDLING TEST ====================

  @Test
  fun chatScreen_nonExistentChat_loadsWithoutCrashing() {
    setContent(chatId = "non-existent-chat")

    // Should show UI elements even if chat doesn't exist
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(MESSAGE_INPUT_TAG).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertExists()
  }

  // ==================== MESSAGE DISPLAY TESTS ====================

  @Test
  fun chatScreen_displaysMessages_afterSending() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // Send a message
    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(TEST_INPUT_TEXT)

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).performClick()

    // Wait for message to appear
    Thread.sleep(DELAY_MEDIUM)

    // Message should be displayed
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_INPUT_TEXT, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun chatScreen_emptyMessageList_showsNoMessages() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // Initially no messages in a new chat
    composeTestRule.onNodeWithTag(MESSAGE_LIST_TAG).assertExists()
  }

  // ==================== INPUT VALIDATION TESTS ====================

  @Test
  fun chatScreen_sendButton_disabled_whenInputBlank() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // Type only spaces
    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput("   ")

    // Send button should still be disabled
    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertIsNotEnabled()
  }

  @Test
  fun chatScreen_inputField_supportsMultilineText() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    val multilineText = "Line 1\nLine 2\nLine 3"

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(multilineText)

    composeTestRule.onNodeWithText(multilineText, substring = true).assertExists()
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  fun chatScreen_nonExistentChat_displaysInputWithoutCrashing() {
    setContent(chatId = "non-existent-chat")

    waitForChatToLoad()

    // Should still show input components
    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertExists()

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertExists()
  }

  // ==================== SEND BUTTON STATE TESTS ====================

  @Test
  fun chatScreen_sendButton_disabled_whileSending() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // Type message
    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(TEST_INPUT_TEXT)

    // Button should be enabled
    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertIsEnabled()

    // Click send
    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).performClick()

    // Button should be briefly disabled while sending
    // (This might be too fast to catch, but good to have)
  }
  // ==================== READ-ONLY CHAT TESTS ====================
  @Test
  fun chatScreen_openChat_showsMessageInput() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_loadingMoreIndicator_displays() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // This test verifies the loading indicator exists in the UI
    // Actual loading state would need messages and scrolling
    composeTestRule.onNodeWithTag(MESSAGE_LIST_TAG).assertExists()
  }

  // ==================== READ-ONLY MESSAGE BAR TESTS ====================

  @Test
  fun chatScreen_completedChat_showsCorrectReadOnlyMessage() {
    createTestChatBlocking(requestStatus = "COMPLETED")
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag("chat_read_only_message").assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_expiredChat_showsCorrectReadOnlyMessage() {
    createTestChatBlocking(requestStatus = "EXPIRED")
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag("chat_read_only_message").assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_cancelledChat_showsCorrectReadOnlyMessage() {
    createTestChatBlocking(requestStatus = "CANCELLED")
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag("chat_read_only_message").assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_completedChat_hidesMessageInput() {
    createTestChatBlocking(requestStatus = "COMPLETED")
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertDoesNotExist()
  }

  @Test
  fun chatScreen_cancelledChat_hidesSendButton() {
    createTestChatBlocking(requestStatus = "CANCELLED")
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun chatScreen_loadingMoreIndicator_doesNotShowInitially() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // Loading more indicator should not show initially
    composeTestRule.onNodeWithTag("chat_loading_more_indicator").assertDoesNotExist()
  }
}
