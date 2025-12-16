package com.android.sample.model.chat

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.utils.BaseEmulatorTest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryFirestoreTest : BaseEmulatorTest() {

  private lateinit var repository: ChatRepositoryFirestore

  private companion object {
    private const val FIRESTORE_WRITE_DELAY_MS: Long = 1000L
    private const val TEST_REQUEST_ID_1 = "test-request-1"
    private const val TEST_REQUEST_ID_2 = "test-request-2"
    private const val TEST_REQUEST_TITLE_1 = "Help with moving"
    private const val TEST_REQUEST_TITLE_2 = "Study group"
  }

  @Before
  override fun setUp() {
    super.setUp()
    runBlocking {
      auth.signOut()
      signInUser()
      clearAllTestData()
    }
    repository = ChatRepositoryFirestore(db)
  }

  @After
  override fun tearDown() {
    runBlocking { clearAllTestData() }
    super.tearDown()
  }

  private suspend fun clearAllTestData() {
    try {
      val chatsSnapshot = db.collection(CHATS_COLLECTION_PATH).get().await()

      val batch = db.batch()

      // Delete all chats and their messages
      chatsSnapshot.documents.forEach { chatDoc ->
        // Delete messages subcollection
        val messagesSnapshot =
            chatDoc.reference.collection(MESSAGES_SUBCOLLECTION_PATH).get().await()
        messagesSnapshot.documents.forEach { messageDoc -> batch.delete(messageDoc.reference) }

        // Delete chat document
        batch.delete(chatDoc.reference)
      }

      batch.commit().await()
      delay(500)
    } catch (e: Exception) {
      Log.e("TestCleanup", "Error clearing test data", e)
    }
  }

  private suspend fun getChatsCount(): Int {
    return db.collection(CHATS_COLLECTION_PATH).get().await().size()
  }

  private suspend fun getMessagesCount(chatId: String): Int {
    return db.collection(CHATS_COLLECTION_PATH)
        .document(chatId)
        .collection(MESSAGES_SUBCOLLECTION_PATH)
        .get()
        .await()
        .size()
  }

  // ==================== CREATE CHAT TESTS ====================

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun canCreateChatSuccessfully() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS * 2) // Increased delay

    assertEquals(1, getChatsCount())

    val chat = repository.getChat(TEST_REQUEST_ID_1)
    assertEquals(TEST_REQUEST_ID_1, chat.chatId)
    assertEquals(TEST_REQUEST_TITLE_1, chat.requestTitle)
    assertEquals(participants, chat.participants)
    assertEquals(currentUserId, chat.creatorId)
    assertEquals("OPEN", chat.requestStatus)
  }

  @Test
  fun createChatFailsWhenUserNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    try {
      repository.createChat(
          requestId = TEST_REQUEST_ID_1,
          requestTitle = TEST_REQUEST_TITLE_1,
          participants = listOf("user1", "user2"),
          creatorId = "user1",
          requestStatus = "OPEN")
      fail("Expected IllegalStateException when not authenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("No authenticated user") == true)
    }
  }

  @Test
  fun createChatFailsWhenUserNotCreatorOrParticipant() = runTest {
    try {
      repository.createChat(
          requestId = TEST_REQUEST_ID_1,
          requestTitle = TEST_REQUEST_TITLE_1,
          participants = listOf("other-user-1", "other-user-2"),
          creatorId = "other-user-1",
          requestStatus = "OPEN")
      fail("Expected IllegalArgumentException when user is not creator or participant")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Current user must be the creator or a participant") == true)
    }
  }

  // ==================== GET CHAT TESTS ====================

  @Test
  fun canGetChatById() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    val chat = repository.getChat(TEST_REQUEST_ID_1)

    assertEquals(TEST_REQUEST_ID_1, chat.chatId)
    assertEquals(TEST_REQUEST_TITLE_1, chat.requestTitle)
    assertEquals(currentUserId, chat.creatorId)
  }

  @Test
  fun getChatThrowsExceptionWhenChatNotFound() = runTest {
    try {
      repository.getChat("non-existent-chat-id")
      fail("Expected Exception when chat doesn't exist")
    } catch (e: Exception) {
      // Accept either NoSuchElementException or generic Exception (due to permission rules)
      assertTrue(
          e is NoSuchElementException ||
              e.message?.contains("Chat with ID") == true ||
              e.message?.contains("not found") == true)
    }
  }

  // ==================== GET USER CHATS TESTS ====================

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun canGetUserChats() = runTest {
    val participants1 = listOf(currentUserId, "helper-user-1")
    val participants2 = listOf(currentUserId, "helper-user-2")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants1,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.createChat(
        requestId = TEST_REQUEST_ID_2,
        requestTitle = TEST_REQUEST_TITLE_2,
        participants = participants2,
        creatorId = currentUserId,
        requestStatus = "IN_PROGRESS")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    val userChats = repository.getUserChats(currentUserId)

    assertEquals(2, userChats.size)
    assertTrue(userChats.any { it.chatId == TEST_REQUEST_ID_1 })
    assertTrue(userChats.any { it.chatId == TEST_REQUEST_ID_2 })
  }

  @Test
  fun getUserChatsReturnsSortedByMostRecent() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    // Create first chat
    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    // Create second chat (more recent)
    repository.createChat(
        requestId = TEST_REQUEST_ID_2,
        requestTitle = TEST_REQUEST_TITLE_2,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    val userChats = repository.getUserChats(currentUserId)

    assertEquals(2, userChats.size)
    // Most recent should be first
    assertEquals(TEST_REQUEST_ID_2, userChats[0].chatId)
    assertEquals(TEST_REQUEST_ID_1, userChats[1].chatId)
  }

  @Test
  fun getUserChatsReturnsEmptyListWhenNoChats() = runTest {
    val userChats = repository.getUserChats(currentUserId)
    assertEquals(0, userChats.size)
  }

  @Test
  fun getUserChatsFailsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    try {
      repository.getUserChats("some-user-id")
      fail("Expected IllegalStateException when not authenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("No authenticated user") == true)
    }
  }

  // ==================== SEND MESSAGE TESTS ====================

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun canSendMessageSuccessfully() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John Doe",
        text = "Hello, world!")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    assertEquals(1, getMessagesCount(TEST_REQUEST_ID_1))

    val messages = repository.getMessages(TEST_REQUEST_ID_1)
    assertEquals(1, messages.size)
    assertEquals("Hello, world!", messages[0].text)
    assertEquals(currentUserId, messages[0].senderId)
    assertEquals("John Doe", messages[0].senderName)
  }

  @Test
  fun sendMessageUpdatesLastMessageInChat() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John Doe",
        text = "This is the latest message")

    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_REQUEST_ID_1)
    assertEquals("This is the latest message", chat.lastMessage)
  }

  @Test
  fun sendMessageFailsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    try {
      repository.sendMessage(
          chatId = TEST_REQUEST_ID_1, senderId = "some-user", senderName = "John", text = "Hello")
      fail("Expected IllegalStateException when not authenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("No authenticated user") == true)
    }
  }

  @Test
  fun sendMessageFailsWhenSenderNotCurrentUser() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    try {
      repository.sendMessage(
          chatId = TEST_REQUEST_ID_1,
          senderId = "different-user",
          senderName = "Impostor",
          text = "Fake message")
      fail("Expected IllegalArgumentException when sender is not current user")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Can only send messages as the current user") == true)
    }
  }

  @Test
  fun sendMessageFailsWhenTextIsEmpty() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    try {
      repository.sendMessage(
          chatId = TEST_REQUEST_ID_1, senderId = currentUserId, senderName = "John", text = "")
      fail("Expected IllegalArgumentException when text is empty")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Message text cannot be empty") == true)
    }
  }

  @Test
  fun sendMessageFailsWhenTextIsBlank() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    try {
      repository.sendMessage(
          chatId = TEST_REQUEST_ID_1, senderId = currentUserId, senderName = "John", text = "   ")
      fail("Expected IllegalArgumentException when text is blank")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Message text cannot be empty") == true)
    }
  }

  @Test
  fun sendMessageFailsWhenUserNotParticipant() = runTest {
    // Sign in as different user to create chat
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid ?: fail("No authenticated user")

    // Explicitly typed list - creatorId is guaranteed non-null after the ?: fail()
    val participants: List<String> = listOf("other-user-1", "other-user-2", creatorId as String)

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = creatorId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Sign in as a THIRD user (not creator, not in participants)
    signInUser("nonparticipant@test.com", "password")
    val nonParticipantUserId = auth.currentUser?.uid ?: fail("No authenticated user")

    // Verify this user is NOT in participants
    assertFalse(participants.contains(nonParticipantUserId))

    try {
      repository.sendMessage(
          chatId = TEST_REQUEST_ID_1,
          senderId = nonParticipantUserId as String,
          senderName = "John",
          text = "Hello")
      fail("Expected exception when user is not a participant")
    } catch (e: Exception) {
      assertTrue(
          e is IllegalArgumentException ||
              e.message?.contains("not a participant") == true ||
              e.message?.contains("Failed to") == true)
    }
  }

  @Test
  fun sendMessageTrimsWhitespace() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John",
        text = "  Hello with spaces  ")

    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_REQUEST_ID_1)
    assertEquals("Hello with spaces", messages[0].text)
  }

  // ==================== GET MESSAGES TESTS ====================

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun canGetMessagesInCorrectOrder() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Send multiple messages
    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John",
        text = "First message")
    delay(500)

    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John",
        text = "Second message")
    delay(500)

    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John",
        text = "Third message")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_REQUEST_ID_1)

    assertEquals(3, messages.size)
    // Should be ordered oldest first
    assertEquals("First message", messages[0].text)
    assertEquals("Second message", messages[1].text)
    assertEquals("Third message", messages[2].text)
  }

  @Test
  fun getMessagesReturnsEmptyListWhenNoMessages() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_REQUEST_ID_1)
    assertEquals(0, messages.size)
  }

  @Test
  fun getMessagesFailsWhenUserNotParticipant() = runTest {
    // Sign in as user to create chat
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid ?: fail("No authenticated user")

    // Explicitly typed list
    val participants: List<String> = listOf("other-user-1", "other-user-2", creatorId as String)

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = creatorId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Sign in as a DIFFERENT user (not in participants)
    signInUser("nonparticipant@test.com", "password")
    val nonParticipantUserId = auth.currentUser?.uid ?: fail("No authenticated user")

    // Verify this user is NOT in participants
    assertFalse(participants.contains(nonParticipantUserId))

    try {
      repository.getMessages(TEST_REQUEST_ID_1)
      fail("Expected exception when user is not a participant")
    } catch (e: Exception) {
      assertTrue(
          e is IllegalArgumentException ||
              e.message?.contains("not a participant") == true ||
              e.message?.contains("Failed to") == true)
    }
  }

  @Test
  fun getMessagesFailsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    try {
      repository.getMessages(TEST_REQUEST_ID_1)
      fail("Expected IllegalStateException when not authenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("No authenticated user") == true)
    }
  }

  // ==================== LISTEN TO MESSAGES TESTS ====================

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun listenToMessagesReturnsFlowWithMessages() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.sendMessage(
        chatId = TEST_REQUEST_ID_1,
        senderId = currentUserId,
        senderName = "John",
        text = "Test message")

    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS * 2) // Extra delay for message to be written

    val flow = repository.listenToMessages(TEST_REQUEST_ID_1)

    // Use longer timeout and don't use virtual time
    withContext(Dispatchers.Default) {
      withTimeout(10000) { // 10 seconds real time
        val messages = flow.first()
        assertTrue("Expected at least one message", messages.isNotEmpty())
        assertEquals("Test message", messages[0].text)
      }
    }
  }

  @Test
  fun listenToMessagesFailsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    try {
      val flow = repository.listenToMessages(TEST_REQUEST_ID_1)
      withTimeout(2000) { flow.first() }
      fail("Expected exception when not authenticated")
    } catch (e: Exception) {
      // Expected - could be IllegalStateException or timeout
      assertTrue(e is IllegalStateException || e.message?.contains("Timed out") == true)
    }
  }

  // ==================== UPDATE CHAT STATUS TESTS ====================

  @Test
  fun canUpdateChatStatus() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.updateChatStatus(TEST_REQUEST_ID_1, "COMPLETED")

    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_REQUEST_ID_1)
    assertEquals("COMPLETED", chat.requestStatus)
  }

  @Test
  fun updateChatStatusFailsWhenUserNotParticipant() = runTest {
    // Sign in as user to create chat
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid ?: fail("No authenticated user")

    // Explicitly typed list
    val participants: List<String> = listOf("other-user-1", "other-user-2", creatorId as String)

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = creatorId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Sign in as a DIFFERENT user (not in participants)
    signInUser("nonparticipant@test.com", "password")
    val nonParticipantUserId = auth.currentUser?.uid ?: fail("No authenticated user")

    // Verify this user is NOT in participants
    assertFalse(participants.contains(nonParticipantUserId))

    try {
      repository.updateChatStatus(TEST_REQUEST_ID_1, "COMPLETED")
      fail("Expected exception when user is not a participant")
    } catch (e: Exception) {
      assertTrue(
          e is IllegalArgumentException ||
              e.message?.contains("not a participant") == true ||
              e.message?.contains("Failed to") == true)
    }
  }

  @Test
  fun updateChatStatusFailsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    try {
      repository.updateChatStatus(TEST_REQUEST_ID_1, "COMPLETED")
      fail("Expected IllegalStateException when not authenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("No authenticated user") == true)
    }
  }

  // ==================== CHAT EXISTS TESTS ====================

  @Test
  fun chatExistsReturnsTrueWhenChatExists() = runTest {
    val participants = listOf(currentUserId, "helper-user-1")

    repository.createChat(
        requestId = TEST_REQUEST_ID_1,
        requestTitle = TEST_REQUEST_TITLE_1,
        participants = participants,
        creatorId = currentUserId,
        requestStatus = "OPEN")

    delay(FIRESTORE_WRITE_DELAY_MS)

    val exists = repository.chatExists(TEST_REQUEST_ID_1)
    assertTrue(exists)
  }

  @Test
  fun chatExistsReturnsFalseWhenChatDoesNotExist() = runTest {
    val exists = repository.chatExists("non-existent-chat")
    assertFalse(exists)
  }
}
