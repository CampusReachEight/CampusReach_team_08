package com.android.sample.ui.communication.messages

import com.android.sample.model.chat.Chat
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.model.request.RequestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"
    const val OTHER_USER_ID = "otherUser456"
    const val CREATOR_ID = "creator789"

    const val CHAT_ID_1 = "chat1"
    const val CHAT_ID_2 = "chat2"
    const val CHAT_ID_3 = "chat3"

    const val REQUEST_TITLE_1 = "Help with moving"
    const val REQUEST_TITLE_2 = "Study group"
    const val REQUEST_TITLE_3 = "Fix my bike"

    const val LAST_MESSAGE_1 = "See you at 3pm!"
    const val LAST_MESSAGE_2 = "I'll bring notes"
    const val LAST_MESSAGE_3 = "Thanks for the help"

    const val ERROR_NO_AUTH = "No authenticated user"
    const val ERROR_FAILED_TO_LOAD = "Failed to load chats"

    const val EXPECTED_ZERO_CHATS = 0
    const val EXPECTED_ONE_CHAT = 1
    const val EXPECTED_TWO_CHATS = 2
    const val EXPECTED_THREE_CHATS = 3

    const val TIME_OFFSET_1_HOUR = 3_600_000L
    const val TIME_OFFSET_2_HOURS = 7_200_000L
    const val TIME_OFFSET_3_HOURS = 10_800_000L
  }

  // ============ Test Fixtures ============
  private lateinit var chatRepository: ChatRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: MessagesViewModel
  private lateinit var requestRepository: RequestRepositoryFirestore
  private val testDispatcher = StandardTestDispatcher()

  // ============ Test Lifecycle ============
  /** Creates a mock request with the given status. */
  private fun mockRequest(requestId: String, status: RequestStatus): Request {
    return mockk {
      every { this@mockk.requestId } returns requestId
      every { this@mockk.status } returns status
      every { viewStatus } returns status
      every { this@mockk.title } returns "Test Request"
      every { expirationTime } returns Date(System.currentTimeMillis() + 86400000L) // Future date
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    chatRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)
    requestRepository = mockk(relaxed = true)
    // Mock Firebase Auth
    every { firebaseAuth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns CURRENT_USER_ID
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ============ Helper Methods ============

  /**
   * Creates a test chat with default values.
   *
   * @param chatId The chat ID
   * @param requestTitle The request title
   * @param creatorId The creator's user ID
   * @param participants List of participant user IDs
   * @param lastMessage The last message preview
   * @param timeOffsetMs Time offset from now in milliseconds
   */
  private fun createTestChat(
      chatId: String,
      requestTitle: String,
      creatorId: String,
      participants: List<String>,
      lastMessage: String,
      timeOffsetMs: Long = TIME_OFFSET_1_HOUR
  ): Chat {
    val now = System.currentTimeMillis()
    return Chat(
        chatId = chatId,
        requestId = chatId,
        requestTitle = requestTitle,
        participants = participants,
        creatorId = creatorId,
        lastMessage = lastMessage,
        lastMessageTimestamp = Date(now - timeOffsetMs),
        requestStatus = "OPEN")
  }

  /** Creates a chat where current user is the creator. */
  private fun createChatAsCreator(
      chatId: String = CHAT_ID_1,
      requestTitle: String = REQUEST_TITLE_1,
      lastMessage: String = LAST_MESSAGE_1,
      timeOffsetMs: Long = TIME_OFFSET_1_HOUR
  ): Chat {
    return createTestChat(
        chatId = chatId,
        requestTitle = requestTitle,
        creatorId = CURRENT_USER_ID,
        participants = listOf(CURRENT_USER_ID, OTHER_USER_ID),
        lastMessage = lastMessage,
        timeOffsetMs = timeOffsetMs)
  }

  /** Creates a chat where current user is a helper. */
  private fun createChatAsHelper(
      chatId: String = CHAT_ID_2,
      requestTitle: String = REQUEST_TITLE_2,
      lastMessage: String = LAST_MESSAGE_2,
      timeOffsetMs: Long = TIME_OFFSET_2_HOURS
  ): Chat {
    return createTestChat(
        chatId = chatId,
        requestTitle = requestTitle,
        creatorId = CREATOR_ID,
        participants = listOf(CREATOR_ID, CURRENT_USER_ID),
        lastMessage = lastMessage,
        timeOffsetMs = timeOffsetMs)
  }

  /** Initializes ViewModel and waits for initial load to complete. */
  private suspend fun initializeViewModel() {
    viewModel = MessagesViewModel(chatRepository, requestRepository, firebaseAuth)
    testDispatcher.scheduler.advanceUntilIdle()
  }

  /** Asserts that the UI state matches expected values. */
  private fun assertUiState(
      expectedChatCount: Int,
      expectedIsLoading: Boolean,
      expectedErrorMessage: String? = null
  ) {
    val state = viewModel.uiState.value
    assertEquals("Unexpected chat count", expectedChatCount, state.chatItems.size)
    assertEquals("Unexpected loading state", expectedIsLoading, state.isLoading)
    assertEquals("Unexpected error message", expectedErrorMessage, state.errorMessage)
  }

  /** Asserts that chat at given index has expected creator status. */
  private fun assertIsCreator(index: Int, expectedIsCreator: Boolean) {
    val actualIsCreator = viewModel.uiState.value.chatItems[index].isCreator
    assertEquals("Unexpected creator status at index $index", expectedIsCreator, actualIsCreator)
  }

  /** Asserts that chats are sorted by most recent message first. */
  private fun assertSortedByMostRecent() {
    val chats = viewModel.uiState.value.chatItems.map { it.chat }
    for (i in 0 until chats.size - 1) {
      val current = chats[i].lastMessageTimestamp.time
      val next = chats[i + 1].lastMessageTimestamp.time
      assertTrue(
          "Chats should be sorted by most recent first (index $i and ${i + 1})", current >= next)
    }
  }

  // ============ Tests for Initialization ============

  @Test
  fun init_loadsChats_successfully() = runTest {
    // Given
    val chats = listOf(createChatAsCreator())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedChatCount = EXPECTED_ONE_CHAT, expectedIsLoading = false)
    coVerify(exactly = 1) { chatRepository.getUserChats(CURRENT_USER_ID) }
  }

  @Test
  fun init_setsLoadingState_thenCompletesSuccessfully() = runTest {
    // Given
    val chats = listOf(createChatAsCreator())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    viewModel = MessagesViewModel(chatRepository, requestRepository, firebaseAuth)

    // Then - Initially loading
    assertTrue("Should be loading initially", viewModel.uiState.value.isLoading)

    // Complete loading
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Loading complete
    assertUiState(expectedChatCount = EXPECTED_ONE_CHAT, expectedIsLoading = false)
  }

  @Test
  fun init_handlesEmptyList_successfully() = runTest {
    // Given
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns emptyList()

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedChatCount = EXPECTED_ZERO_CHATS, expectedIsLoading = false)
  }

  @Test
  fun init_handlesRepositoryError_setsErrorMessage() = runTest {
    // Given
    val errorMessage = "Network error"
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } throws Exception(errorMessage)

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedChatCount = EXPECTED_ZERO_CHATS,
        expectedIsLoading = false,
        expectedErrorMessage = errorMessage)
  }

  @Test
  fun init_handlesNoAuthenticatedUser_setsErrorMessage() = runTest {
    // Given
    every { firebaseAuth.currentUser } returns null
    coEvery { chatRepository.getUserChats(any()) } returns emptyList()

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedChatCount = EXPECTED_ZERO_CHATS,
        expectedIsLoading = false,
        expectedErrorMessage = ERROR_NO_AUTH)
    coVerify(exactly = 0) { chatRepository.getUserChats(any()) }
  }

  // ============ Tests for Chat Loading ============

  @Test
  fun loadChats_loadsMultipleChats_successfully() = runTest {
    // Given
    val chats =
        listOf(
            createChatAsCreator(CHAT_ID_1, REQUEST_TITLE_1, LAST_MESSAGE_1, TIME_OFFSET_1_HOUR),
            createChatAsHelper(CHAT_ID_2, REQUEST_TITLE_2, LAST_MESSAGE_2, TIME_OFFSET_2_HOURS),
            createChatAsCreator(CHAT_ID_3, REQUEST_TITLE_3, LAST_MESSAGE_3, TIME_OFFSET_3_HOURS))
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedChatCount = EXPECTED_THREE_CHATS, expectedIsLoading = false)
  }

  @Test
  fun loadChats_maintainsSortingByMostRecent() = runTest {
    // Given - Create chats with different timestamps (most recent first in list)
    val chats =
        listOf(
            createChatAsCreator(CHAT_ID_1, REQUEST_TITLE_1, LAST_MESSAGE_1, TIME_OFFSET_1_HOUR),
            createChatAsHelper(CHAT_ID_2, REQUEST_TITLE_2, LAST_MESSAGE_2, TIME_OFFSET_2_HOURS),
            createChatAsCreator(CHAT_ID_3, REQUEST_TITLE_3, LAST_MESSAGE_3, TIME_OFFSET_3_HOURS))
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertSortedByMostRecent()
  }

  // ============ Tests for User Role Detection ============

  @Test
  fun loadChats_detectsCreatorRole_correctly() = runTest {
    // Given - Chat where current user is creator
    val chats = listOf(createChatAsCreator())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertIsCreator(index = 0, expectedIsCreator = true)
  }

  @Test
  fun loadChats_detectsHelperRole_correctly() = runTest {
    // Given - Chat where current user is helper
    val chats = listOf(createChatAsHelper())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertIsCreator(index = 0, expectedIsCreator = false)
  }

  @Test
  fun loadChats_detectsMixedRoles_correctly() = runTest {
    // Given - Mix of creator and helper chats
    val chats =
        listOf(
            createChatAsCreator(CHAT_ID_1),
            createChatAsHelper(CHAT_ID_2),
            createChatAsCreator(CHAT_ID_3))
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertIsCreator(index = 0, expectedIsCreator = true)
    assertIsCreator(index = 1, expectedIsCreator = false)
    assertIsCreator(index = 2, expectedIsCreator = true)
  }

  // ============ Tests for Refresh ============

  @Test
  fun refresh_reloadsChats_successfully() = runTest {
    // Given
    val initialChats = listOf(createChatAsCreator())
    val refreshedChats = listOf(createChatAsCreator(), createChatAsHelper())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns initialChats

    initializeViewModel()
    assertUiState(expectedChatCount = EXPECTED_ONE_CHAT, expectedIsLoading = false)

    // When
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns refreshedChats
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedChatCount = EXPECTED_TWO_CHATS, expectedIsLoading = false)
    coVerify(exactly = 2) { chatRepository.getUserChats(CURRENT_USER_ID) }
  }

  @Test
  fun refresh_setsLoadingState_duringRefresh() = runTest {
    // Given
    val chats = listOf(createChatAsCreator())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    initializeViewModel()

    // When
    viewModel.refresh()

    // Then - Should be loading before completion
    assertTrue("Should be loading during refresh", viewModel.uiState.value.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Should not be loading after completion
    assertFalse("Should not be loading after refresh", viewModel.uiState.value.isLoading)
  }

  @Test
  fun refresh_handlesError_setsErrorMessage() = runTest {
    // Given
    val chats = listOf(createChatAsCreator())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    initializeViewModel()

    // When - Refresh fails
    val errorMessage = "Refresh failed"
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } throws Exception(errorMessage)
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(
        expectedChatCount = EXPECTED_ONE_CHAT, // Keeps old data
        expectedIsLoading = false,
        expectedErrorMessage = errorMessage)
  }

  // ============ Tests for Error Handling ============

  @Test
  fun clearError_removesErrorMessage() = runTest {
    // Given - Error state
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } throws Exception("Error")
    initializeViewModel()
    assertNotNull("Should have error message", viewModel.uiState.value.errorMessage)

    // When
    viewModel.clearError()

    // Then
    assertNull("Error message should be cleared", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun loadChats_withBlankErrorMessage_usesFriendlyMessage() = runTest {
    // Given
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } throws Exception("   ")

    // When
    initializeViewModel()

    // Then
    val errorMessage = viewModel.uiState.value.errorMessage
    assertNotNull("Should have error message", errorMessage)
    assertTrue(
        "Should use friendly error message", errorMessage?.contains("Failed to load chats") == true)
  }

  @Test
  fun loadChats_withNullErrorMessage_usesFriendlyMessage() = runTest {
    // Given
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } throws Exception(null as String?)

    // When
    initializeViewModel()

    // Then
    val errorMessage = viewModel.uiState.value.errorMessage
    assertNotNull("Should have error message", errorMessage)
    assertTrue(
        "Should use friendly error message", errorMessage?.contains("Failed to load chats") == true)
  }

  // ============ Tests for Edge Cases ============

  @Test
  fun loadChats_withSingleChat_loadsCorrectly() = runTest {
    // Given
    val chats = listOf(createChatAsCreator())
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns chats

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedChatCount = EXPECTED_ONE_CHAT, expectedIsLoading = false)
    assertEquals(
        "Chat title should match",
        REQUEST_TITLE_1,
        viewModel.uiState.value.chatItems[0].chat.requestTitle)
  }

  @Test
  fun loadChats_preservesChatDetails() = runTest {
    // Given
    val chat =
        createChatAsCreator(
            chatId = CHAT_ID_1, requestTitle = REQUEST_TITLE_1, lastMessage = LAST_MESSAGE_1)
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns listOf(chat)

    // When
    initializeViewModel()

    // Then
    val loadedChat = viewModel.uiState.value.chatItems[0].chat
    assertEquals("Chat ID should match", CHAT_ID_1, loadedChat.chatId)
    assertEquals("Request title should match", REQUEST_TITLE_1, loadedChat.requestTitle)
    assertEquals("Last message should match", LAST_MESSAGE_1, loadedChat.lastMessage)
    assertEquals("Creator ID should match", CURRENT_USER_ID, loadedChat.creatorId)
  }

  @Test
  fun loadChats_filtersOutExpiredRequests() = runTest {
    // Given
    val activeChat = createChatAsCreator(CHAT_ID_1)
    val expiredChat = createChatAsCreator(CHAT_ID_2)

    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns listOf(activeChat, expiredChat)

    // Mock: First request is active, second is expired
    coEvery { requestRepository.getRequest(CHAT_ID_1) } returns
        mockRequest(CHAT_ID_1, RequestStatus.OPEN)
    coEvery { requestRepository.getRequest(CHAT_ID_2) } returns
        mockRequest(CHAT_ID_2, RequestStatus.COMPLETED)

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedChatCount = EXPECTED_ONE_CHAT, // Only active chat shown
        expectedIsLoading = false)
  }

  @Test
  fun loadChats_filtersOutCancelledRequests() = runTest {
    // Given
    val activeChat = createChatAsCreator(CHAT_ID_1)
    val cancelledChat = createChatAsCreator(CHAT_ID_2)

    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns
        listOf(activeChat, cancelledChat)

    coEvery { requestRepository.getRequest(CHAT_ID_1) } returns
        mockRequest(CHAT_ID_1, RequestStatus.OPEN)
    coEvery { requestRepository.getRequest(CHAT_ID_2) } returns
        mockRequest(CHAT_ID_2, RequestStatus.CANCELLED)

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedChatCount = EXPECTED_ONE_CHAT, expectedIsLoading = false)
  }

  @Test
  fun loadChats_excludesChatWhenRequestNotFound() = runTest {
    // Given
    val chat = createChatAsCreator()
    coEvery { chatRepository.getUserChats(CURRENT_USER_ID) } returns listOf(chat)

    // Request doesn't exist - throw exception
    coEvery { requestRepository.getRequest(CHAT_ID_1) } throws Exception("Request not found")

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedChatCount = EXPECTED_ZERO_CHATS, expectedIsLoading = false)
  }
}
