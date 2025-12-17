package com.android.sample.ui.communication.messages

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.chat.Chat
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.theme.SampleAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import java.util.Date
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for MessagesScreen offline functionality using Robolectric.
 *
 * Tests verify that the screen correctly displays offline state and prevents user interactions when
 * network is unavailable.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class MessagesScreenOfflineTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var chatRepository: ChatRepository
  private lateinit var requestRepository: RequestRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser

  private var clickedChatId: String? = null

  // ============ Test Constants ============
  private companion object {
    // User IDs
    const val CURRENT_USER_ID = "testUser123"
    const val OTHER_USER_ID = "otherUser456"

    // Chat IDs
    const val TEST_CHAT_ID = "chat123"
    const val TEST_REQUEST_ID = "request123"

    // UI Content
    const val TEST_CHAT_TITLE = "Test Chat"
    const val TEST_LAST_MESSAGE = "Hello"
    const val TEST_REQUEST_STATUS = "OPEN"

    // Test Tags
    const val OFFLINE_STATE_TAG = "messages_offline_state"
    const val EMPTY_STATE_TAG = "messages_empty_state"
    const val CHAT_LIST_TAG = "messages_chat_list"

    // Offline State Text
    const val OFFLINE_STATE_TITLE = "You're Offline"
    const val OFFLINE_STATE_SUBTITLE = "Connect to the internet to view and access your messages"

    // Error Messages
    const val NETWORK_UNAVAILABLE_ERROR = "Network unavailable: cannot retrieve chats from server"
  }

  // ============ Test Lifecycle ============

  @Before
  fun setUp() {
    chatRepository = mockk(relaxed = true)
    requestRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    every { firebaseAuth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns CURRENT_USER_ID

    clickedChatId = null
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  // ============ Helper Methods ============

  /** Creates a test chat with default values. */
  private fun createTestChat(
      chatId: String = TEST_CHAT_ID,
      requestId: String = TEST_REQUEST_ID,
      title: String = TEST_CHAT_TITLE,
      creatorId: String = CURRENT_USER_ID,
      participants: List<String> = listOf(CURRENT_USER_ID, OTHER_USER_ID),
      lastMessage: String = TEST_LAST_MESSAGE,
      status: String = TEST_REQUEST_STATUS
  ): Chat {
    return Chat(
        chatId = chatId,
        requestId = requestId,
        requestTitle = title,
        participants = participants,
        creatorId = creatorId,
        lastMessage = lastMessage,
        lastMessageTimestamp = Date(),
        requestStatus = status)
  }

  /** Creates a mock request with the given status. */
  private fun createMockRequest(
      requestId: String = TEST_REQUEST_ID,
      status: RequestStatus = RequestStatus.OPEN
  ): Request {
    return mockk {
      every { this@mockk.requestId } returns requestId
      every { this@mockk.status } returns status
      every { viewStatus } returns status
      every { title } returns TEST_CHAT_TITLE
      every { expirationTime } returns Date(System.currentTimeMillis() + 86400000L)
    }
  }

  /** Sets up the chat repository to throw a network unavailable error. */
  private fun setupOfflineRepository() {
    coEvery { chatRepository.getUserChats(any()) } throws
        IllegalStateException(NETWORK_UNAVAILABLE_ERROR)
  }

  /** Sets up the chat repository to return a list of chats with active requests. */
  private fun setupOnlineRepositoryWithChats(chats: List<Chat>) {
    coEvery { chatRepository.getUserChats(any()) } returns chats
    chats.forEach { chat ->
      coEvery { requestRepository.getRequest(chat.requestId) } returns
          createMockRequest(chat.requestId, RequestStatus.OPEN)
    }
  }

  /** Sets the compose content with MessagesScreen using mocked repositories. */
  private fun setContentWithViewModel() {
    composeTestRule.setContent {
      SampleAppTheme {
        MessagesScreen(
            onChatClick = { chatId -> clickedChatId = chatId },
            viewModel =
                MessagesViewModel(
                    chatRepository = chatRepository,
                    requestRepository = requestRepository,
                    firebaseAuth = firebaseAuth))
      }
    }
  }

  /** Waits for UI to stabilize and asserts node exists and is displayed. */
  private fun assertNodeExistsAndDisplayed(testTag: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(testTag).assertExists().assertIsDisplayed()
  }

  /** Waits for UI to stabilize and asserts node with text exists and is displayed. */
  private fun assertTextExistsAndDisplayed(text: String, substring: Boolean = false) {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(text, substring = substring).assertExists().assertIsDisplayed()
  }

  /** Waits for UI to stabilize and asserts node does not exist. */
  private fun assertNodeDoesNotExist(testTag: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(testTag).assertDoesNotExist()
  }

  // ============ Offline State Display Tests ============

  @Test
  fun messagesScreen_showsOfflineState_whenNetworkUnavailable() {
    // Given
    setupOfflineRepository()

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(OFFLINE_STATE_TAG)
  }

  @Test
  fun messagesScreen_displaysOfflineTitle_whenNetworkUnavailable() {
    // Given
    setupOfflineRepository()

    // When
    setContentWithViewModel()

    // Then
    assertTextExistsAndDisplayed(OFFLINE_STATE_TITLE)
  }

  @Test
  fun messagesScreen_displaysOfflineSubtitle_whenNetworkUnavailable() {
    // Given
    setupOfflineRepository()

    // When
    setContentWithViewModel()

    // Then
    assertTextExistsAndDisplayed(OFFLINE_STATE_SUBTITLE, substring = true)
  }

  // ============ State Exclusivity Tests ============

  @Test
  fun messagesScreen_hidesEmptyState_whenOffline() {
    // Given
    setupOfflineRepository()

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(OFFLINE_STATE_TAG)
    assertNodeDoesNotExist(EMPTY_STATE_TAG)
  }

  @Test
  fun messagesScreen_hidesChatList_whenOffline() {
    // Given
    setupOfflineRepository()

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(OFFLINE_STATE_TAG)
    assertNodeDoesNotExist(CHAT_LIST_TAG)
  }

  @Test
  fun messagesScreen_showsChatList_whenOnline() {
    // Given
    val chat = createTestChat()
    setupOnlineRepositoryWithChats(listOf(chat))

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(CHAT_LIST_TAG)
    assertNodeDoesNotExist(OFFLINE_STATE_TAG)
    assertNodeDoesNotExist(EMPTY_STATE_TAG)
  }

  @Test
  fun messagesScreen_showsEmptyState_whenOnlineWithNoChats() {
    // Given
    setupOnlineRepositoryWithChats(emptyList())

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(EMPTY_STATE_TAG)
    assertNodeDoesNotExist(OFFLINE_STATE_TAG)
    assertNodeDoesNotExist(CHAT_LIST_TAG)
  }

  // ============ User Interaction Tests ============

  @Test
  fun messagesScreen_displaysOfflineState_whenChatRepositoryThrowsNetworkError() {
    // Given - Start offline
    setupOfflineRepository()

    // When
    setContentWithViewModel()
    composeTestRule.waitForIdle()

    // Then - Verify offline state is displayed and no chats exist
    assertNodeExistsAndDisplayed(OFFLINE_STATE_TAG)
    assertNodeDoesNotExist(CHAT_LIST_TAG)
    assert(clickedChatId == null) { "No chat should be clicked in offline state" }
  }

  @Test
  fun messagesScreen_chatClickTriggers_whenOnline() {
    // Given
    val chat = createTestChat()
    setupOnlineRepositoryWithChats(listOf(chat))

    // When
    setContentWithViewModel()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(TEST_CHAT_TITLE, substring = true).performClick()

    // Then
    assert(clickedChatId == TEST_CHAT_ID) {
      "Expected chat ID '$TEST_CHAT_ID' but got '$clickedChatId'"
    }
  }

  // ============ Multiple Chat Tests ============

  @Test
  fun messagesScreen_displaysMultipleChats_whenOnline() {
    // Given
    val chat1 = createTestChat(chatId = "chat1", requestId = "req1", title = "Chat 1")
    val chat2 = createTestChat(chatId = "chat2", requestId = "req2", title = "Chat 2")
    val chat3 = createTestChat(chatId = "chat3", requestId = "req3", title = "Chat 3")
    setupOnlineRepositoryWithChats(listOf(chat1, chat2, chat3))

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(CHAT_LIST_TAG)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Chat 1", substring = true).assertExists()
    composeTestRule.onNodeWithText("Chat 2", substring = true).assertExists()
    composeTestRule.onNodeWithText("Chat 3", substring = true).assertExists()
  }

  // ============ Error Differentiation Tests ============

  @Test
  fun messagesScreen_doesNotShowOfflineState_whenGenericError() {
    // Given - Generic error (not network related)
    coEvery { chatRepository.getUserChats(any()) } throws Exception("Generic error")

    // When
    setContentWithViewModel()

    // Then - Should show empty state, not offline state
    composeTestRule.waitForIdle()
    assertNodeDoesNotExist(OFFLINE_STATE_TAG)
    // Error will be shown in Snackbar, but UI shows empty state
  }

  @Test
  fun messagesScreen_showsOfflineState_whenIllegalStateWithNetworkMessage() {
    // Given - IllegalStateException with "network unavailable" message
    coEvery { chatRepository.getUserChats(any()) } throws
        IllegalStateException("NETWORK UNAVAILABLE: some detailed message")

    // When
    setContentWithViewModel()

    // Then
    assertNodeExistsAndDisplayed(OFFLINE_STATE_TAG)
  }
}
