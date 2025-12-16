package com.android.sample.ui.communication.chat

import androidx.lifecycle.ViewModel
import com.android.sample.model.chat.Chat
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.chat.Message
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.UserSections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

  // ============ Test Constants ============
  private companion object {
    const val CURRENT_USER_ID = "currentUser123"
    const val OTHER_USER_ID = "otherUser456"
    const val CREATOR_ID = "creator789"

    const val CHAT_ID = "test-chat-1"
    const val REQUEST_ID = "test-request-1"
    const val REQUEST_TITLE = "Help with moving"

    const val MESSAGE_ID_1 = "message1"
    const val MESSAGE_ID_2 = "message2"
    const val MESSAGE_ID_3 = "message3"

    const val MESSAGE_TEXT_1 = "Hello!"
    const val MESSAGE_TEXT_2 = "How are you?"
    const val MESSAGE_TEXT_3 = "See you at 3pm"

    const val USER_FIRST_NAME = "John"
    const val USER_LAST_NAME = "Doe"
    const val USER_EMAIL = "john.doe@example.com"
    const val SENDER_NAME = "$USER_FIRST_NAME $USER_LAST_NAME"

    const val ERROR_NO_AUTH = "No authenticated user"
    const val ERROR_FAILED_TO_LOAD = "Failed to load chat"
    const val ERROR_FAILED_TO_SEND = "Failed to send message"

    const val EXPECTED_ZERO_MESSAGES = 0
    const val EXPECTED_ONE_MESSAGE = 1
    const val EXPECTED_TWO_MESSAGES = 2
    const val EXPECTED_THREE_MESSAGES = 3

    const val TIME_OFFSET_1_MIN = 60_000L
    const val TIME_OFFSET_2_MIN = 120_000L
    const val TIME_OFFSET_3_MIN = 180_000L
  }

  // ============ Test Fixtures ============
  private lateinit var chatRepository: ChatRepository
  private lateinit var profileRepository: UserProfileRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: ChatViewModel

  private val testDispatcher = StandardTestDispatcher()

  // ============ Test Lifecycle ============

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    chatRepository = mockk(relaxed = true)
    profileRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

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

  /** Creates a test chat with default values. */
  private fun createTestChat(
      chatId: String = CHAT_ID,
      requestTitle: String = REQUEST_TITLE,
      creatorId: String = CREATOR_ID,
      participants: List<String> = listOf(CURRENT_USER_ID, OTHER_USER_ID)
  ): Chat {
    return Chat(
        chatId = chatId,
        requestId = REQUEST_ID,
        requestTitle = requestTitle,
        participants = participants,
        creatorId = creatorId,
        lastMessage = "Chat created",
        lastMessageTimestamp = Date(),
        requestStatus = "OPEN")
  }

  /** Creates a test message. */
  private fun createTestMessage(
      messageId: String,
      text: String,
      senderId: String = CURRENT_USER_ID,
      timeOffsetMs: Long = TIME_OFFSET_1_MIN
  ): Message {
    val now = System.currentTimeMillis()
    return Message(
        messageId = messageId,
        chatId = CHAT_ID,
        senderId = senderId,
        senderName = SENDER_NAME,
        text = text,
        timestamp = Date(now - timeOffsetMs))
  }

  /** Creates a test user profile. */
  private fun createTestUserProfile(
      userId: String = CURRENT_USER_ID,
      firstName: String = USER_FIRST_NAME,
      lastName: String = USER_LAST_NAME
  ): UserProfile {
    return UserProfile(
        id = userId,
        name = firstName,
        lastName = lastName,
        email = USER_EMAIL,
        photo = null,
        kudos = 0,
        helpReceived = 0,
        section = UserSections.COMPUTER_SCIENCE,
        arrivalDate = Date())
  }

  /** Sets up mocks for successful chat initialization. */
  private fun setupSuccessfulInitialization() {
    val chat = createTestChat()
    val userProfile = createTestUserProfile()
    val messages =
        listOf(
            createTestMessage(MESSAGE_ID_1, MESSAGE_TEXT_1, timeOffsetMs = TIME_OFFSET_3_MIN),
            createTestMessage(MESSAGE_ID_2, MESSAGE_TEXT_2, timeOffsetMs = TIME_OFFSET_2_MIN),
            createTestMessage(MESSAGE_ID_3, MESSAGE_TEXT_3, timeOffsetMs = TIME_OFFSET_1_MIN))

    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, any()) } returns flowOf(messages)
  }

  /** Initializes ViewModel with a chat and waits for completion. */
  private suspend fun initializeViewModel(chatId: String = CHAT_ID) {
    viewModel = ChatViewModel(chatRepository, profileRepository, firebaseAuth)
    viewModel.initializeChat(chatId)
    testDispatcher.scheduler.advanceUntilIdle()
  }

  /** Asserts that the UI state matches expected values. */
  private fun assertUiState(
      expectedMessageCount: Int,
      expectedIsLoading: Boolean,
      expectedIsSendingMessage: Boolean = false,
      expectedErrorMessage: String? = null
  ) {
    val state = viewModel.uiState.value
    assertEquals("Unexpected message count", expectedMessageCount, state.messages.size)
    assertEquals("Unexpected loading state", expectedIsLoading, state.isLoading)
    assertEquals("Unexpected sending state", expectedIsSendingMessage, state.isSendingMessage)
    assertEquals("Unexpected error message", expectedErrorMessage, state.errorMessage)
  }

  // ============ Tests for Initialization ============

  @Test
  fun initializeChat_loadsChat_successfully() = runTest {
    // Given
    setupSuccessfulInitialization()

    // When
    initializeViewModel()

    // Ensure coroutines are executed
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertUiState(expectedMessageCount = EXPECTED_THREE_MESSAGES, expectedIsLoading = false)
    assertNotNull("Chat should be loaded", viewModel.uiState.value.chat)
    assertNotNull("User profile should be loaded", viewModel.uiState.value.currentUserProfile)
    coVerify(exactly = 1) { chatRepository.getChat(CHAT_ID) }
    coVerify(exactly = 1) { profileRepository.getUserProfile(CURRENT_USER_ID) }
  }

  @Test
  fun initializeChat_setsLoadingState_thenCompletes() = runTest {
    // Given
    setupSuccessfulInitialization()

    // When
    viewModel = ChatViewModel(chatRepository, profileRepository, firebaseAuth)
    viewModel.initializeChat(CHAT_ID)

    // Then - Initially loading
    assertTrue("Should be loading initially", viewModel.uiState.value.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Loading complete
    assertFalse("Should not be loading after completion", viewModel.uiState.value.isLoading)
  }

  @Test
  fun initializeChat_loadsChatMetadata_correctly() = runTest {
    // Given
    setupSuccessfulInitialization()

    // When
    initializeViewModel()

    // Then
    val chat = viewModel.uiState.value.chat
    assertNotNull("Chat should be loaded", chat)
    assertEquals("Chat ID should match", CHAT_ID, chat?.chatId)
    assertEquals("Request title should match", REQUEST_TITLE, chat?.requestTitle)
  }

  @Test
  fun initializeChat_loadsUserProfile_correctly() = runTest {
    // Given
    setupSuccessfulInitialization()

    // When
    initializeViewModel()

    // Then
    val profile = viewModel.uiState.value.currentUserProfile
    assertNotNull("User profile should be loaded", profile)
    assertEquals("User ID should match", CURRENT_USER_ID, profile?.id)
    assertEquals("First name should match", USER_FIRST_NAME, profile?.name)
    assertEquals("Last name should match", USER_LAST_NAME, profile?.lastName)
  }

  @Test
  fun initializeChat_handlesNoAuthenticatedUser_setsErrorMessage() = runTest {
    // Given
    every { firebaseAuth.currentUser } returns null

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedMessageCount = EXPECTED_ZERO_MESSAGES,
        expectedIsLoading = false,
        expectedErrorMessage = ERROR_NO_AUTH)
    coVerify(exactly = 0) { chatRepository.getChat(any()) }
  }

  @Test
  fun initializeChat_handlesChatLoadError_setsErrorMessage() = runTest {
    // Given
    val errorMessage = "Chat not found"
    coEvery { chatRepository.getChat(CHAT_ID) } throws Exception(errorMessage)

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedMessageCount = EXPECTED_ZERO_MESSAGES,
        expectedIsLoading = false,
        expectedErrorMessage = errorMessage)
  }

  @Test
  fun initializeChat_handlesProfileLoadError_setsErrorMessage() = runTest {
    // Given
    val chat = createTestChat()
    val errorMessage = "Profile not found"
    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } throws Exception(errorMessage)

    // When
    initializeViewModel()

    // Then
    assertUiState(
        expectedMessageCount = EXPECTED_ZERO_MESSAGES,
        expectedIsLoading = false,
        expectedErrorMessage = errorMessage)
  }

  // ============ Tests for Message Loading ============

  @Test
  fun initializeChat_loadsMessages_inCorrectOrder() = runTest {
    // Given
    setupSuccessfulInitialization()

    // When
    initializeViewModel()

    // Then
    val messages = viewModel.uiState.value.messages
    assertEquals("Should have 3 messages", EXPECTED_THREE_MESSAGES, messages.size)
    assertEquals("First message should be oldest", MESSAGE_TEXT_1, messages[0].text)
    assertEquals("Second message", MESSAGE_TEXT_2, messages[1].text)
    assertEquals("Third message should be newest", MESSAGE_TEXT_3, messages[2].text)
  }

  @Test
  fun initializeChat_withNoMessages_loadsEmptyList() = runTest {
    // Given
    val chat = createTestChat()
    val userProfile = createTestUserProfile()
    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, Date()) } returns flowOf(emptyList())

    // When
    initializeViewModel()

    // Then
    assertUiState(expectedMessageCount = EXPECTED_ZERO_MESSAGES, expectedIsLoading = false)
  }

  @Test
  fun listenToMessages_updatesUiState_whenNewMessagesArrive() = runTest {
    // Given
    val chat = createTestChat()
    val userProfile = createTestUserProfile()
    val initialMessages = listOf(createTestMessage(MESSAGE_ID_1, MESSAGE_TEXT_1))
    val updatedMessages =
        listOf(
            createTestMessage(MESSAGE_ID_1, MESSAGE_TEXT_1),
            createTestMessage(MESSAGE_ID_2, MESSAGE_TEXT_2))

    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, any()) } returns
        flowOf(initialMessages, updatedMessages)

    // When
    initializeViewModel()

    // Then - Should have latest messages
    assertUiState(expectedMessageCount = EXPECTED_TWO_MESSAGES, expectedIsLoading = false)
  }

  // ============ Tests for Sending Messages ============

  @Test
  fun sendMessage_sendsMessage_successfully() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    coEvery {
      chatRepository.sendMessage(
          chatId = CHAT_ID,
          senderId = CURRENT_USER_ID,
          senderName = SENDER_NAME,
          text = MESSAGE_TEXT_1)
    } just Runs

    // When
    viewModel.sendMessage(MESSAGE_TEXT_1)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 1) {
      chatRepository.sendMessage(
          chatId = CHAT_ID,
          senderId = CURRENT_USER_ID,
          senderName = SENDER_NAME,
          text = MESSAGE_TEXT_1)
    }
    assertFalse("Should not be sending after completion", viewModel.uiState.value.isSendingMessage)
  }

  @Test
  fun sendMessage_clearsMessageInput_afterSuccessfulSend() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } just Runs

    // Set input text
    viewModel.updateMessageInput(MESSAGE_TEXT_1)
    assertEquals("Input should be set", MESSAGE_TEXT_1, viewModel.uiState.value.messageInput)

    // When
    viewModel.sendMessage(MESSAGE_TEXT_1)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertEquals("Input should be cleared", "", viewModel.uiState.value.messageInput)
  }

  @Test
  fun sendMessage_setsSendingState_duringOperation() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } just Runs

    // When
    viewModel.sendMessage(MESSAGE_TEXT_1)

    // Then - Should be sending before completion
    assertTrue("Should be sending", viewModel.uiState.value.isSendingMessage)

    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Should not be sending after completion
    assertFalse("Should not be sending after completion", viewModel.uiState.value.isSendingMessage)
  }

  @Test
  fun sendMessage_withBlankText_doesNotSend() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    // When
    viewModel.sendMessage("   ")
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any(), any()) }
  }

  @Test
  fun sendMessage_withEmptyText_doesNotSend() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    // When
    viewModel.sendMessage("")
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any(), any()) }
  }

  @Test
  fun sendMessage_handlesError_setsErrorMessage() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    val errorMessage = "Send failed"
    coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } throws
        Exception(errorMessage)

    // When
    viewModel.sendMessage(MESSAGE_TEXT_1)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertEquals("Should have error message", errorMessage, viewModel.uiState.value.errorMessage)
    assertFalse("Should not be sending", viewModel.uiState.value.isSendingMessage)
  }

  @Test
  fun sendMessage_beforeInitialization_setsErrorMessage() = runTest {
    // Given - ViewModel not initialized
    viewModel = ChatViewModel(chatRepository, profileRepository, firebaseAuth)

    // When
    viewModel.sendMessage(MESSAGE_TEXT_1)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertNotNull("Should have error message", viewModel.uiState.value.errorMessage)
    assertTrue(
        "Error should mention invalid state",
        viewModel.uiState.value.errorMessage?.contains("invalid state") == true)
    coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any(), any()) }
  }

  @Test
  fun sendMessage_withoutAuthenticatedUser_setsErrorMessage() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    // When - User signs out
    every { firebaseAuth.currentUser } returns null
    viewModel.sendMessage(MESSAGE_TEXT_1)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertNotNull("Should have error message", viewModel.uiState.value.errorMessage)
    coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any(), any()) }
  }

  @Test
  fun sendMessage_constructsSenderName_correctly() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } just Runs

    // When
    viewModel.sendMessage(MESSAGE_TEXT_1)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 1) {
      chatRepository.sendMessage(
          chatId = any(),
          senderId = any(),
          senderName = SENDER_NAME, // "John Doe"
          text = any())
    }
  }

  // ============ Tests for Message Input ============

  @Test
  fun updateMessageInput_updatesState() = runTest {
    // Given
    viewModel = ChatViewModel(chatRepository, profileRepository, firebaseAuth)

    // When
    viewModel.updateMessageInput(MESSAGE_TEXT_1)

    // Then
    assertEquals("Input should be updated", MESSAGE_TEXT_1, viewModel.uiState.value.messageInput)
  }

  @Test
  fun updateMessageInput_canUpdateMultipleTimes() = runTest {
    // Given
    viewModel = ChatViewModel(chatRepository, profileRepository, firebaseAuth)

    // When
    viewModel.updateMessageInput("First")
    assertEquals("First", viewModel.uiState.value.messageInput)

    viewModel.updateMessageInput("Second")
    assertEquals("Second", viewModel.uiState.value.messageInput)

    viewModel.updateMessageInput("Third")

    // Then
    assertEquals("Third", viewModel.uiState.value.messageInput)
  }

  // ============ Tests for Error Handling ============

  @Test
  fun clearError_removesErrorMessage() = runTest {
    // Given - Error state
    setupSuccessfulInitialization()
    coEvery { chatRepository.getChat(CHAT_ID) } throws Exception("Error")
    initializeViewModel()
    assertNotNull("Should have error message", viewModel.uiState.value.errorMessage)

    // When
    viewModel.clearError()

    // Then
    assertNull("Error message should be cleared", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun initializeChat_withBlankErrorMessage_usesFriendlyMessage() = runTest {
    // Given
    coEvery { chatRepository.getChat(CHAT_ID) } throws Exception("   ")

    // When
    initializeViewModel()

    // Then
    val errorMessage = viewModel.uiState.value.errorMessage
    assertNotNull("Should have error message", errorMessage)
    assertTrue(
        "Should use friendly error message", errorMessage?.contains("Failed to load chat") == true)
  }

  // ============ Tests for Refresh ============

  @Test
  fun refresh_reinitializesChat_successfully() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    // When
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 2) { chatRepository.getChat(CHAT_ID) }
    coVerify(exactly = 2) { profileRepository.getUserProfile(CURRENT_USER_ID) }
  }

  @Test
  fun refresh_beforeInitialization_doesNothing() = runTest {
    // Given - ViewModel not initialized
    viewModel = ChatViewModel(chatRepository, profileRepository, firebaseAuth)

    // When
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { chatRepository.getChat(any()) }
  }

  @Test
  fun loadMoreMessages_prependsOlderMessages_successfully() = runTest {
    // Given
    val chat = createTestChat()
    val userProfile = createTestUserProfile()

    val newestMessages =
        listOf(
            createTestMessage(MESSAGE_ID_2, MESSAGE_TEXT_2, timeOffsetMs = TIME_OFFSET_1_MIN),
            createTestMessage(MESSAGE_ID_3, MESSAGE_TEXT_3, timeOffsetMs = TIME_OFFSET_2_MIN))

    val olderMessages =
        listOf(createTestMessage(MESSAGE_ID_1, MESSAGE_TEXT_1, timeOffsetMs = TIME_OFFSET_3_MIN))

    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.getMessages(CHAT_ID, 30, null) } returns newestMessages
    coEvery { chatRepository.getMessages(CHAT_ID, 10, any()) } returns olderMessages
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, any()) } returns flowOf(emptyList())

    initializeViewModel()

    // When
    viewModel.loadMoreMessages()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val messages = viewModel.uiState.value.messages
    assertEquals(3, messages.size)
    assertEquals(MESSAGE_TEXT_1, messages.first().text) // oldest first
  }

  @Test
  fun loadMoreMessages_setsLoadingStateDuringFetch() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    coEvery { chatRepository.getMessages(CHAT_ID, 10, any()) } returns emptyList()

    // When
    viewModel.loadMoreMessages()

    // Then (immediate)
    assertTrue(viewModel.uiState.value.isLoadingMore)

    testDispatcher.scheduler.advanceUntilIdle()

    // Then (after completion)
    assertFalse(viewModel.uiState.value.isLoadingMore)
  }

  @Test
  fun loadMoreMessages_doesNothing_whenNoMessages() = runTest {
    // Given
    val chat = createTestChat()
    val userProfile = createTestUserProfile()

    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.getMessages(CHAT_ID, 30, null) } returns emptyList()
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, any()) } returns flowOf(emptyList())

    initializeViewModel()

    // When
    viewModel.loadMoreMessages()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { chatRepository.getMessages(CHAT_ID, 10, any()) }
  }

  @Test
  fun loadMoreMessages_handlesError_setsErrorMessage() = runTest {
    // Given
    setupSuccessfulInitialization()
    initializeViewModel()

    val errorMessage = "Network error"
    coEvery { chatRepository.getMessages(CHAT_ID, 10, any()) } throws Exception(errorMessage)

    // When
    viewModel.loadMoreMessages()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoadingMore)
    assertEquals(errorMessage, state.errorMessage)
  }

  @Test
  fun listenToNewMessages_handlesFlowError_setsErrorMessage() = runTest {
    // Given
    val chat = createTestChat()
    val userProfile = createTestUserProfile()
    val errorMessage = "Connection lost"

    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.getMessages(CHAT_ID, 30, null) } returns emptyList()
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, any()) } returns
        flow { throw Exception(errorMessage) }

    // When
    initializeViewModel()

    // Then
    assertEquals("Should have error message", errorMessage, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun listenToNewMessages_handlesFlowErrorWithBlankMessage_usesFriendlyMessage() = runTest {
    // Given
    val chat = createTestChat()
    val userProfile = createTestUserProfile()

    coEvery { chatRepository.getChat(CHAT_ID) } returns chat
    coEvery { profileRepository.getUserProfile(CURRENT_USER_ID) } returns userProfile
    coEvery { chatRepository.getMessages(CHAT_ID, 30, null) } returns emptyList()
    coEvery { chatRepository.listenToNewMessages(CHAT_ID, any()) } returns
        flow { throw Exception("   ") }

    // When
    initializeViewModel()

    // Then
    val errorMessage = viewModel.uiState.value.errorMessage
    assertNotNull("Should have error message", errorMessage)
    assertTrue(
        "Should use friendly error message",
        errorMessage?.contains("Failed to load messages") == true)
  }

  @Test
  fun factory_throwsException_forUnknownViewModelClass() {
    // Given
    val factory =
        ChatViewModelFactory(
            chatRepository = chatRepository,
            profileRepository = profileRepository,
            firebaseAuth = firebaseAuth)

    // Create a dummy ViewModel class that's not ChatViewModel
    class DummyViewModel : ViewModel()

    // When/Then
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          factory.create(DummyViewModel::class.java)
        }

    assertEquals(UNKNOWN_VIEW_MODEL_ERROR, exception.message)
  }
}
