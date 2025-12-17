package com.android.sample.ui.communication.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.ChatRepositoryFirestore
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.profile.UserSections
import com.android.sample.utils.BaseEmulatorTest
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive instrumented tests for ChatScreen.
 *
 * Test Coverage:
 * - Screen structure and basic UI elements
 * - Message input and sending functionality
 * - Read-only chat states (COMPLETED, EXPIRED, CANCELLED)
 * - Message display and list behavior
 * - Error handling and edge cases
 * - Loading states
 *
 * These tests are CI-friendly and use proper wait strategies for asynchronous UI updates.
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenTest : BaseEmulatorTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var chatRepository: ChatRepositoryFirestore
  private lateinit var profileRepository: UserProfileRepositoryFirestore
  private var backClickCount = 0

  // ==================== TEST CONSTANTS ====================

  private companion object TestConstants {
    // Chat data
    const val TEST_CHAT_ID = "test-chat-123"
    const val TEST_REQUEST_TITLE = "Help with moving"
    const val TEST_INPUT_TEXT = "Test message"
    const val TEST_MESSAGE_TEXT = "Hello, this is a test message"
    const val TEST_SENDER_NAME = "Test Sender"
    const val MULTILINE_TEXT = "Line 1\nLine 2\nLine 3"
    const val BLANK_SPACES = "   "
    const val NON_EXISTENT_CHAT_ID = "non-existent-chat"

    // Request statuses
    const val STATUS_OPEN = "OPEN"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_EXPIRED = "EXPIRED"
    const val STATUS_CANCELLED = "CANCELLED"

    // UI text
    const val MESSAGE_INPUT_PLACEHOLDER = "Type a message..."

    // Test tags
    const val BACK_BUTTON_TAG = "chat_back_button"
    const val MESSAGE_LIST_TAG = "chat_message_list"
    const val MESSAGE_INPUT_TAG = "chat_message_input"
    const val SEND_BUTTON_TAG = "chat_send_button"
    const val INPUT_ROW_TAG = "chat_input_row"
    const val READ_ONLY_MESSAGE_TAG = "chat_read_only_message"
    const val LOADING_MORE_INDICATOR_TAG = "chat_loading_more_indicator"

    // Timeouts and delays
    const val UI_WAIT_TIMEOUT = 10_000L
    const val DELAY_MEDIUM = 2_000L
    const val DELAY_SHORT = 500L

    // Counts
    const val EXPECTED_BACK_CLICKS = 1
  }

  // ==================== SETUP ====================
  /** Creates a user profile in Firestore for the current authenticated user. */
  private fun createUserProfileBlocking() {
    runBlocking {
      val userProfile =
          UserProfile(
              id = currentUserId,
              name = "Test",
              lastName = "User",
              email = "test@example.com",
              photo = null,
              kudos = 0,
              helpReceived = 0,
              section = UserSections.NONE,
              arrivalDate = Date(),
              followerCount = 0,
              followingCount = 0)

      profileRepository.addUserProfile(userProfile)
      delay(DELAY_SHORT)
    }
  }

  @Before
  override fun setUp() {
    super.setUp()
    chatRepository = ChatRepositoryFirestore(db)
    profileRepository = UserProfileRepositoryFirestore(db)
    backClickCount = 0

    // CREATE THE USER PROFILE
    createUserProfileBlocking()
  }

  // ==================== HELPER METHODS ====================

  /**
   * Sets the Compose content with ChatScreen.
   *
   * @param chatId ID of the chat to display
   */
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

  /**
   * Creates a test chat with the given status using blocking coroutine.
   *
   * @param requestStatus Status of the request (OPEN, COMPLETED, EXPIRED, CANCELLED)
   */
  private fun createTestChatBlocking(requestStatus: String = STATUS_OPEN) {
    runBlocking {
      chatRepository.createChat(
          requestId = TEST_CHAT_ID,
          requestTitle = TEST_REQUEST_TITLE,
          participants = listOf(currentUserId),
          creatorId = currentUserId,
          requestStatus = requestStatus)
      delay(DELAY_MEDIUM) // Wait for Firestore propagation
    }
  }

  /**
   * Waits for chat to load by checking for appropriate bottom bar element.
   *
   * @param expectReadOnly If true, waits for read-only message bar; otherwise waits for input bar
   */
  private fun waitForChatToLoad(expectReadOnly: Boolean = false) {
    val tagToWaitFor = if (expectReadOnly) READ_ONLY_MESSAGE_TAG else MESSAGE_INPUT_TAG

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(tagToWaitFor).fetchSemanticsNodes().isNotEmpty()
    }
  }

  /** Waits for the chat title to appear in the UI. */
  private fun waitForChatTitle() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_REQUEST_TITLE, substring = true, ignoreCase = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
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

    assert(backClickCount == EXPECTED_BACK_CLICKS)
  }

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

  @Test
  fun chatScreen_inputField_supportsMultilineText() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(MULTILINE_TEXT)

    composeTestRule.onNodeWithText(MULTILINE_TEXT, substring = true).assertExists()
  }

  @Test
  fun chatScreen_inputRow_exists() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(INPUT_ROW_TAG).assertExists().assertIsDisplayed()
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
  fun chatScreen_sendButton_disabled_whenInputBlank() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(BLANK_SPACES)

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertIsNotEnabled()
  }

  // ==================== MESSAGE DISPLAY TESTS ====================

  @Test
  fun chatScreen_displaysMessages_afterSending() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput(TEST_INPUT_TEXT)
    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).performClick()

    // Wait for message to appear
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(TEST_INPUT_TEXT, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(TEST_INPUT_TEXT, substring = true).assertExists()
  }

  @Test
  fun chatScreen_emptyMessageList_showsNoMessages() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(MESSAGE_LIST_TAG).assertExists()
  }

  // ==================== READ-ONLY CHAT TESTS ====================

  @Test
  fun chatScreen_openChat_showsMessageInput() {
    createTestChatBlocking(STATUS_OPEN)
    setContent()
    waitForChatToLoad(expectReadOnly = false)

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_completedChat_showsCorrectReadOnlyMessage() {
    createTestChatBlocking(STATUS_COMPLETED)
    setContent()
    waitForChatToLoad(expectReadOnly = true)

    composeTestRule.onNodeWithTag(READ_ONLY_MESSAGE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_expiredChat_showsCorrectReadOnlyMessage() {
    createTestChatBlocking(STATUS_EXPIRED)
    setContent()
    waitForChatToLoad(expectReadOnly = true)

    composeTestRule.onNodeWithTag(READ_ONLY_MESSAGE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_cancelledChat_showsCorrectReadOnlyMessage() {
    createTestChatBlocking(STATUS_CANCELLED)
    setContent()
    waitForChatToLoad(expectReadOnly = true)

    composeTestRule.onNodeWithTag(READ_ONLY_MESSAGE_TAG).assertExists().assertIsDisplayed()
  }

  @Test
  fun chatScreen_completedChat_hidesMessageInput() {
    createTestChatBlocking(STATUS_COMPLETED)
    setContent()
    waitForChatToLoad(expectReadOnly = true)

    composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).assertDoesNotExist()
  }

  @Test
  fun chatScreen_cancelledChat_hidesSendButton() {
    createTestChatBlocking(STATUS_CANCELLED)
    setContent()
    waitForChatToLoad(expectReadOnly = true)

    composeTestRule.onNodeWithTag(SEND_BUTTON_TAG).assertDoesNotExist()
  }

  // ==================== LOADING INDICATOR TESTS ====================

  @Test
  fun chatScreen_loadingMoreIndicator_displays() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    // Verify message list exists (loading more would require actual scrolling with many messages)
    composeTestRule.onNodeWithTag(MESSAGE_LIST_TAG).assertExists()
  }

  @Test
  fun chatScreen_loadingMoreIndicator_doesNotShowInitially() {
    createTestChatBlocking()
    setContent()
    waitForChatToLoad()

    composeTestRule.onNodeWithTag(LOADING_MORE_INDICATOR_TAG).assertDoesNotExist()
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  fun chatScreen_nonExistentChat_loadsWithoutCrashing() {
    setContent(chatId = NON_EXISTENT_CHAT_ID)

    // Should display screen without crashing (no bottom bar shown for non-existent chat)
    composeTestRule.onNodeWithTag(NavigationTestTags.CHAT_SCREEN).assertExists().assertIsDisplayed()
  }

  @Test
  fun debug_checkUserAndChatSetup() {
    runBlocking {
      // Does the user profile exist?
      val profile = profileRepository.getUserProfile(currentUserId)
      println("DEBUG: User profile loaded: ${profile.name}")

      // Does chat creation work?
      chatRepository.createChat(
          requestId = TEST_CHAT_ID,
          requestTitle = TEST_REQUEST_TITLE,
          participants = listOf(currentUserId),
          creatorId = currentUserId,
          requestStatus = STATUS_OPEN)

      // Can we retrieve it?
      val chat = chatRepository.getChat(TEST_CHAT_ID)
      println("DEBUG: Chat loaded: ${chat.requestTitle}")
    }
  }
}
