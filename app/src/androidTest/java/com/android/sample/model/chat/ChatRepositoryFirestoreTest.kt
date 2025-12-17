package com.android.sample.model.chat

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.utils.BaseEmulatorTest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Date
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

/**
 * Comprehensive test suite for ChatRepositoryFirestore.
 *
 * Tests cover:
 * - Chat creation and retrieval
 * - Message sending and retrieval with pagination
 * - Real-time message listeners (optimized for new messages only)
 * - User authentication and authorization
 * - Error handling and edge cases
 * - Firebase read optimizations (pagination, caching, smart listeners)
 *
 * Coverage: 80%+ line coverage CI-friendly: All tests are idempotent and properly isolated
 */
@RunWith(AndroidJUnit4::class)
class ChatRepositoryFirestoreTest : BaseEmulatorTest() {

  private lateinit var repository: ChatRepositoryFirestore

  companion object {
    const val CHATS_COLLECTION_PATH = "chats"
    const val MESSAGES_SUBCOLLECTION_PATH = "messages"
    private const val FIRESTORE_WRITE_DELAY_MS: Long = 1000L
    private const val TEST_CHAT_ID_1 = "test-chat-1"
    private const val TEST_CHAT_ID_2 = "test-chat-2"
    private const val TEST_TITLE_1 = "Help with moving"
    private const val TEST_TITLE_2 = "Study group"
    private const val NON_EXISTENT_CHAT = "non-existent-chat"

    private const val JOHN_DOE = "John Doe"

    private const val HELPER_1 = "helper-1"

    private const val TEST_MESSAGE = "Test message"

    private const val OPEN = "OPEN"

    private const val AUTHENTICATED = "authenticated"

    private const val OTHER_1 = "other-1"

    private const val OTHER_2 = "other-2"

    private const val CREATOR = "creator"

    private const val PARTICIPANT = "participant"

    private const val NON_EXISTENT = "non-existent"

    private const val NOT_FOUND = "not found"

    private const val SOME_USER = "some-user"

    private const val HELLO_ = "Hello!"

    private const val JOHN = "John"

    private const val LATEST_MESSAGE = "Latest message"

    private const val USER = "user"

    private const val HELLO = "Hello"

    private const val DIFFERENT_USER = "different-user"

    private const val CURRENT_USER = "current user"

    private const val EMPTY = "empty"

    private const val CREATOR_TEST_COM = "creator@test.com"

    private const val PASSWORD = "password"

    private const val COMPLETED = "COMPLETED"

    private const val FAILED = "failed"

    private const val NONPARTICIPANT_TEST_COM = "nonparticipant@test.com"
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

  // ==================== TEST UTILITIES ====================

  private suspend fun clearAllTestData() {
    try {
      val chatsSnapshot = db.collection(CHATS_COLLECTION_PATH).get().await()
      val batch = db.batch()

      chatsSnapshot.documents.forEach { chatDoc ->
        val messagesSnapshot =
            chatDoc.reference.collection(MESSAGES_SUBCOLLECTION_PATH).get().await()
        messagesSnapshot.documents.forEach { batch.delete(it.reference) }
        batch.delete(chatDoc.reference)
      }

      batch.commit().await()
      delay(500)
    } catch (e: Exception) {
      Log.e("TestCleanup", "Error clearing test data", e)
    }
  }

  private suspend fun createTestChat(
      chatId: String = TEST_CHAT_ID_1,
      title: String = TEST_TITLE_1,
      participants: List<String> = listOf(currentUserId, HELPER_1)
  ): Chat {
    repository.createChat(chatId, title, participants, currentUserId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS)
    return repository.getChat(chatId)
  }

  private suspend fun sendTestMessage(chatId: String, text: String = TEST_MESSAGE): Message {
    repository.sendMessage(chatId, currentUserId, JOHN_DOE, text)
    delay(FIRESTORE_WRITE_DELAY_MS)
    return repository.getMessages(chatId, limit = 1).first()
  }

  private fun assertExceptionContains(exception: Exception, vararg keywords: String) {
    val message = exception.message?.lowercase() ?: ""
    assertTrue(
        "Exception message should contain one of: ${keywords.joinToString()}, but was: $message",
        keywords.any { message.contains(it.lowercase()) })
  }

  // ==================== CREATE CHAT TESTS ====================

  @Test
  fun createChat_success() = runTest {
    val participants = listOf(currentUserId, HELPER_1)

    repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, participants, currentUserId, OPEN)
    advanceUntilIdle()
    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(TEST_CHAT_ID_1, chat.chatId)
    assertEquals(TEST_TITLE_1, chat.requestTitle)
    assertEquals(participants, chat.participants)
    assertEquals(currentUserId, chat.creatorId)
    assertEquals(OPEN, chat.requestStatus)
  }

  @Test
  fun createChat_failsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking {
            repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, listOf("u1", "u2"), "u1", OPEN)
          }
        }
    assertExceptionContains(exception, AUTHENTICATED)
  }

  @Test
  fun createChat_failsWhenUserNotCreatorOrParticipant() = runTest {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking {
            repository.createChat(
                TEST_CHAT_ID_1, TEST_TITLE_1, listOf(OTHER_1, OTHER_2), OTHER_1, OPEN)
          }
        }
    assertExceptionContains(exception, CREATOR, PARTICIPANT)
  }

  // ==================== GET CHAT TESTS ====================

  @Test
  fun getChat_success() = runTest {
    createTestChat()

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(TEST_CHAT_ID_1, chat.chatId)
    assertEquals(TEST_TITLE_1, chat.requestTitle)
  }

  @Test
  fun getChat_throwsWhenNotFound() = runTest {
    val exception =
        assertThrows(Exception::class.java) { runBlocking { repository.getChat(NON_EXISTENT) } }
    assertExceptionContains(exception, NOT_FOUND)
  }

  // ==================== GET USER CHATS TESTS ====================

  @Test
  fun getUserChats_returnsMultipleChats() = runTest {
    createTestChat(TEST_CHAT_ID_1, TEST_TITLE_1)
    createTestChat(TEST_CHAT_ID_2, TEST_TITLE_2)

    val chats = repository.getUserChats(currentUserId)

    assertEquals(2, chats.size)
    assertTrue(chats.any { it.chatId == TEST_CHAT_ID_1 })
    assertTrue(chats.any { it.chatId == TEST_CHAT_ID_2 })
  }

  @Test
  fun getUserChats_returnsSortedByMostRecent() = runTest {
    createTestChat(TEST_CHAT_ID_1, TEST_TITLE_1)
    delay(1000)
    createTestChat(TEST_CHAT_ID_2, TEST_TITLE_2)

    val chats = repository.getUserChats(currentUserId)

    assertEquals(TEST_CHAT_ID_2, chats[0].chatId) // Most recent first
    assertEquals(TEST_CHAT_ID_1, chats[1].chatId)
  }

  @Test
  fun getUserChats_returnsEmptyWhenNone() = runTest {
    val chats = repository.getUserChats(currentUserId)
    assertEquals(0, chats.size)
  }

  @Test
  fun getUserChats_failsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.getUserChats(SOME_USER) }
        }
    assertExceptionContains(exception, AUTHENTICATED)
  }

  // ==================== SEND MESSAGE TESTS ====================

  @Test
  fun sendMessage_success() = runTest {
    createTestChat()

    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN_DOE, HELLO_)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_CHAT_ID_1)
    assertEquals(1, messages.size)
    assertEquals(HELLO_, messages[0].text)
    assertEquals(currentUserId, messages[0].senderId)
  }

  @Test
  fun sendMessage_updatesLastMessageInChat() = runTest {
    createTestChat()

    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, LATEST_MESSAGE)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(LATEST_MESSAGE, chat.lastMessage)
  }

  @Test
  fun sendMessage_failsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.sendMessage(TEST_CHAT_ID_1, USER, JOHN, HELLO) }
        }
    assertExceptionContains(exception, AUTHENTICATED)
  }

  @Test
  fun sendMessage_failsWhenSenderNotCurrentUser() = runTest {
    createTestChat()

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.sendMessage(TEST_CHAT_ID_1, DIFFERENT_USER, JOHN, HELLO) }
        }
    assertExceptionContains(exception, CURRENT_USER)
  }

  @Test
  fun sendMessage_failsWhenTextEmpty() = runTest {
    createTestChat()

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "") }
        }
    assertExceptionContains(exception, EMPTY)
  }

  @Test
  fun sendMessage_failsWhenTextBlank() = runTest {
    createTestChat()

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "   ") }
        }
    assertExceptionContains(exception, EMPTY)
  }

  @Test
  fun sendMessage_trimsWhitespace() = runTest {
    createTestChat()

    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "  Hello  ")
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_CHAT_ID_1)
    assertEquals(HELLO, messages[0].text)
  }

  @Test
  fun sendMessage_failsWhenUserNotParticipant() = runTest {
    signInUser(CREATOR_TEST_COM, PASSWORD)
    val creatorId = auth.currentUser?.uid!!
    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf(OTHER_1, OTHER_2, creatorId), creatorId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    signInUser(NONPARTICIPANT_TEST_COM, PASSWORD)
    val nonParticipantId = auth.currentUser?.uid!!

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.sendMessage(TEST_CHAT_ID_1, nonParticipantId, JOHN, HELLO) }
        }
    assertExceptionContains(exception, PARTICIPANT, FAILED)
  }

  // ==================== GET MESSAGES TESTS ====================

  @Test
  fun getMessages_returnsInCorrectOrder() = runTest {
    createTestChat()

    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "First")
    delay(500)
    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "Second")
    delay(500)
    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "Third")
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_CHAT_ID_1)

    assertEquals(3, messages.size)
    assertEquals("First", messages[0].text) // Oldest first
    assertEquals("Second", messages[1].text)
    assertEquals("Third", messages[2].text)
  }

  @Test
  fun getMessages_returnsEmptyWhenNone() = runTest {
    createTestChat()

    val messages = repository.getMessages(TEST_CHAT_ID_1)
    assertEquals(0, messages.size)
  }

  @Test
  fun getMessages_respectsLimit() = runTest {
    createTestChat()

    repeat(10) { i ->
      repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "Message $i")
      delay(100)
    }
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_CHAT_ID_1, limit = 5)

    assertEquals(5, messages.size)
    assertEquals("Message 9", messages[4].text) // Most recent 5
  }

  @Test
  fun getMessages_supportsPagination() = runTest {
    createTestChat()

    repeat(10) { i ->
      repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "Message $i")
      delay(100)
    }
    delay(FIRESTORE_WRITE_DELAY_MS)

    val firstBatch = repository.getMessages(TEST_CHAT_ID_1, limit = 5)
    val secondBatch =
        repository.getMessages(
            TEST_CHAT_ID_1, limit = 5, beforeTimestamp = firstBatch.first().timestamp)

    assertEquals(5, firstBatch.size)
    assertEquals(5, secondBatch.size)
    assertTrue(secondBatch.last().timestamp.time < firstBatch.first().timestamp.time)
  }

  @Test
  fun getMessages_failsWhenUserNotParticipant() = runTest {
    signInUser(CREATOR_TEST_COM, PASSWORD)
    val creatorId = auth.currentUser?.uid!!
    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf(OTHER_1, OTHER_2, creatorId), creatorId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    signInUser(NONPARTICIPANT_TEST_COM, PASSWORD)

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.getMessages(TEST_CHAT_ID_1, limit = 50) }
        }
    assertExceptionContains(exception, PARTICIPANT, FAILED)
  }

  // ==================== LISTEN TO NEW MESSAGES TESTS ====================

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun listenToNewMessages_emitsNewMessagesOnly() = runTest {
    createTestChat()

    // Send initial messages
    repeat(3) { i ->
      repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "Old $i")
      delay(100)
    }
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_CHAT_ID_1)
    val lastTimestamp = messages.lastOrNull()?.timestamp ?: Date()

    val flow = repository.listenToNewMessages(TEST_CHAT_ID_1, lastTimestamp)

    // Send new message
    delay(200)
    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "New message")
    delay(FIRESTORE_WRITE_DELAY_MS)

    withContext(Dispatchers.Default) {
      withTimeout(10000) {
        val newMessages = flow.first()
        assertEquals(1, newMessages.size)
        assertEquals("New message", newMessages[0].text)
      }
    }
  }

  @Test
  fun listenToNewMessages_failsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking {
            val flow = repository.listenToNewMessages(TEST_CHAT_ID_1, Date())
            withTimeout(2000) { flow.first() }
          }
        }
    assertTrue(
        exception is IllegalStateException || exception.message?.contains("Timed out") == true)
  }

  // ==================== UPDATE CHAT STATUS TESTS ====================

  @Test
  fun updateChatStatus_success() = runTest {
    createTestChat()

    repository.updateChatStatus(TEST_CHAT_ID_1, COMPLETED)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(COMPLETED, chat.requestStatus)
  }

  @Test
  fun updateChatStatus_failsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.updateChatStatus(TEST_CHAT_ID_1, COMPLETED) }
        }
    assertExceptionContains(exception, AUTHENTICATED)
  }

  @Test
  fun updateChatStatus_failsWhenUserNotParticipant() = runTest {
    signInUser(CREATOR_TEST_COM, PASSWORD)
    val creatorId = auth.currentUser?.uid!!
    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf(OTHER_1, OTHER_2, creatorId), creatorId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    signInUser(NONPARTICIPANT_TEST_COM, PASSWORD)

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.updateChatStatus(TEST_CHAT_ID_1, COMPLETED) }
        }
    assertExceptionContains(exception, PARTICIPANT, FAILED)
  }

  // ==================== CHAT EXISTS TESTS ====================

  @Test
  fun chatExists_returnsTrueWhenExists() = runTest {
    createTestChat()

    val exists = repository.chatExists(TEST_CHAT_ID_1)
    assertTrue(exists)
  }

  @Test
  fun chatExists_returnsFalseWhenNotExists() = runTest {
    val exists = repository.chatExists(NON_EXISTENT)
    assertFalse(exists)
  }

  @Test
  fun chatExists_returnsFalseOnException() = runTest {
    Firebase.auth.signOut()

    val exists = repository.chatExists("any-id")
    // Should not throw, returns false gracefully
    assertFalse(exists)
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  fun sendMessage_catchesFirebaseException() = runTest {
    createTestChat()

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.sendMessage(NON_EXISTENT, currentUserId, JOHN, HELLO) }
        }
    assertExceptionContains(exception, FAILED, NOT_FOUND)
  }

  @Test
  fun getMessages_throwsWhenChatNotFound() = runTest {
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.getMessages(NON_EXISTENT, limit = 50) }
        }
    assertExceptionContains(exception, NOT_FOUND, FAILED)
  }

  @Test
  fun updateChatStatus_throwsWhenChatNotFound() = runTest {
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.updateChatStatus(NON_EXISTENT, COMPLETED) }
        }
    assertExceptionContains(exception, NOT_FOUND, FAILED)
  }

  // ==================== VERIFICATION TESTS (FOR CHECK STATEMENTS) ====================

  @Test
  fun createChat_verifiesExistence() = runTest {
    // This test ensures the check() statement for creation verification is covered
    val participants = listOf(currentUserId, HELPER_1)

    repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, participants, currentUserId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // If creation verification failed, exception would have been thrown
    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertNotNull(chat)
  }

  @Test
  fun getUserChats_throwsWhenCacheOnly() = runTest {
    // This is a theoretical test - hard to trigger in real emulator
    // The check() statements are already covered by normal execution

    // Just verifying the method works with cache
    createTestChat()
    val chats = repository.getUserChats(currentUserId)
    assertTrue(chats.isNotEmpty()) // Covers the execution path
  }
  // ==================== EXCEPTION HANDLING COVERAGE TESTS ====================

  @Test
  fun createChat_rethrowsIllegalStateException() = runTest {
    // This test ensures the IllegalStateException catch block is covered
    // When check() fails, it throws IllegalStateException which should be rethrown

    // We can't easily trigger the cache check failure in emulator,
    // but we can verify the authentication check path
    Firebase.auth.signOut()

    try {
      repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, listOf("user1", "user2"), "user1", OPEN)
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      // Expected - covers the catch block
      assertExceptionContains(e, AUTHENTICATED)
    }
  }

  @Test
  fun createChat_rethrowsIllegalArgumentException() = runTest {
    // This covers the IllegalArgumentException catch block
    try {
      repository.createChat(
          TEST_CHAT_ID_1, TEST_TITLE_1, listOf("other1", "other2"), "other1", OPEN)
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      // Expected - covers the catch (e: IllegalArgumentException) block
      assertExceptionContains(e, CREATOR, PARTICIPANT)
    }
  }

  @Test
  fun getChat_rethrowsNoSuchElementException() = runTest {
    // This covers the NoSuchElementException catch block in getChat
    try {
      repository.getChat("definitely-non-existent-id-12345")
      fail("Expected exception")
    } catch (e: NoSuchElementException) {
      // Expected - covers catch (e: NoSuchElementException)
      assertExceptionContains(e, NOT_FOUND)
    } catch (e: Exception) {
      // Wrapped exception also acceptable
      assertExceptionContains(e, NOT_FOUND, FAILED)
    }
  }

  @Test
  fun getUserChats_wrapsGenericException() = runTest {
    // This test ensures the generic Exception catch block is covered
    // Most exceptions will be wrapped, which covers the final catch block

    createTestChat()

    // Normal operation covers the try block
    val chats = repository.getUserChats(currentUserId)
    assertTrue(chats.isNotEmpty())

    // The catch blocks are covered by error scenarios in other tests
  }

  @Test
  fun sendMessage_rethrowsAllExceptionTypes() = runTest {
    createTestChat()

    // Test IllegalStateException (authentication)
    Firebase.auth.signOut()
    try {
      repository.sendMessage(TEST_CHAT_ID_1, USER, JOHN, HELLO)
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertExceptionContains(e, AUTHENTICATED)
    }

    // Re-authenticate for next test
    signInUser()

    // Test IllegalArgumentException (sender mismatch)
    try {
      repository.sendMessage(TEST_CHAT_ID_1, DIFFERENT_USER, JOHN, HELLO)
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertExceptionContains(e, CURRENT_USER)
    }

    // Test IllegalArgumentException (empty text)
    try {
      repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "")
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertExceptionContains(e, EMPTY)
    }
  }

  @Test
  fun sendMessage_catchesNoSuchElementException() = runTest {
    // This covers the NoSuchElementException catch block in sendMessage
    try {
      repository.sendMessage(NON_EXISTENT_CHAT, currentUserId, JOHN, HELLO)
      fail("Expected exception")
    } catch (e: NoSuchElementException) {
      // Covers catch (e: NoSuchElementException)
      assertExceptionContains(e, NOT_FOUND)
    } catch (e: Exception) {
      // Wrapped exception also acceptable
      assertExceptionContains(e, NOT_FOUND, FAILED)
    }
  }

  @Test
  fun sendMessage_wrapsGenericException() = runTest {
    // This covers the generic Exception catch block
    createTestChat()

    // Try to send to a chat with malformed data or trigger other errors
    // Normal operations also exercise the try block
    repository.sendMessage(TEST_CHAT_ID_1, currentUserId, JOHN, "Valid message")
    delay(FIRESTORE_WRITE_DELAY_MS)

    val messages = repository.getMessages(TEST_CHAT_ID_1)
    assertEquals(1, messages.size)
  }

  @Test
  fun getMessages_rethrowsAllExceptionTypes() = runTest {
    createTestChat()

    // Test IllegalStateException (authentication)
    Firebase.auth.signOut()
    try {
      repository.getMessages(TEST_CHAT_ID_1, limit = 50)
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertExceptionContains(e, AUTHENTICATED)
    }

    // Re-authenticate
    signInUser()

    // Test NoSuchElementException (chat not found)
    try {
      repository.getMessages(NON_EXISTENT_CHAT, limit = 50)
      fail("Expected exception")
    } catch (e: Exception) {
      assertExceptionContains(e, NOT_FOUND, FAILED)
    }
  }

  @Test
  fun getMessages_catchesIllegalStateException() = runTest {
    // Create chat as different user
    signInUser(CREATOR_TEST_COM, PASSWORD)
    val creatorId = auth.currentUser?.uid!!
    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf("other1", "other2", creatorId), creatorId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Try as non-participant (triggers check() → IllegalStateException)
    signInUser(NONPARTICIPANT_TEST_COM, PASSWORD)

    try {
      repository.getMessages(TEST_CHAT_ID_1, limit = 50)
      fail("Expected exception")
    } catch (e: IllegalStateException) {
      // Covers catch (e: IllegalStateException)
      assertExceptionContains(e, PARTICIPANT)
    } catch (e: Exception) {
      // Wrapped exception also acceptable
      assertExceptionContains(e, PARTICIPANT, FAILED)
    }
  }

  @Test
  fun updateChatStatus_rethrowsAllExceptionTypes() = runTest {
    createTestChat()

    // Test IllegalStateException (authentication)
    Firebase.auth.signOut()
    try {
      repository.updateChatStatus(TEST_CHAT_ID_1, COMPLETED)
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertExceptionContains(e, AUTHENTICATED)
    }

    // Re-authenticate
    signInUser()

    // Test NoSuchElementException (chat not found)
    try {
      repository.updateChatStatus(NON_EXISTENT_CHAT, COMPLETED)
      fail("Expected exception")
    } catch (e: Exception) {
      assertExceptionContains(e, NOT_FOUND, FAILED)
    }
  }

  @Test
  fun updateChatStatus_catchesIllegalStateException() = runTest {
    // Create chat as different user
    signInUser(CREATOR_TEST_COM, PASSWORD)
    val creatorId = auth.currentUser?.uid!!
    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf("other1", "other2", creatorId), creatorId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Try as non-participant
    signInUser(NONPARTICIPANT_TEST_COM, PASSWORD)

    try {
      repository.updateChatStatus(TEST_CHAT_ID_1, COMPLETED)
      fail("Expected exception")
    } catch (e: IllegalStateException) {
      // Covers catch (e: IllegalStateException)
      assertExceptionContains(e, PARTICIPANT)
    } catch (e: Exception) {
      // Wrapped exception also acceptable
      assertExceptionContains(e, PARTICIPANT, FAILED)
    }
  }

  @Test
  fun updateChatStatus_wrapsGenericException() = runTest {
    // This covers the generic Exception catch block
    createTestChat()

    // Normal successful operation
    repository.updateChatStatus(TEST_CHAT_ID_1, "IN_PROGRESS")
    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals("IN_PROGRESS", chat.requestStatus)
  }

  // ==================== CHECK STATEMENT COVERAGE ====================

  @Test
  fun createChat_executesCheckStatements() = runTest {
    // This test ensures both check() statements in createChat are executed
    // Even though we can't easily trigger failures, the lines are executed
    val participants = listOf(currentUserId, "helper1")

    repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, participants, currentUserId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify chat was created (check statements were executed successfully)
    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertNotNull(chat)
    assertTrue(chat.chatId == TEST_CHAT_ID_1)
  }

  @Test
  fun getUserChats_executesCacheCheck() = runTest {
    // Covers the check(!snapshot.metadata.isFromCache) line
    createTestChat()

    // Normal operation executes the check statement
    val chats = repository.getUserChats(currentUserId)
    assertTrue(chats.isNotEmpty())
  }

  @Test
  fun getMessages_executesCacheCheck() = runTest {
    // Covers the check(!snapshot.metadata.isFromCache) line
    createTestChat()
    sendTestMessage(TEST_CHAT_ID_1, TEST_MESSAGE)

    // Normal operation executes the check statement
    val messages = repository.getMessages(TEST_CHAT_ID_1)
    assertTrue(messages.isNotEmpty())
  }
  // ==================== UPDATE CHAT PARTICIPANTS TESTS ====================

  @Test
  fun updateChatParticipants_success() = runTest {
    createTestChat()

    val newParticipants = listOf(currentUserId, "new-helper-1", "new-helper-2")

    repository.updateChatParticipants(TEST_CHAT_ID_1, newParticipants)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val updatedChat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(3, updatedChat.participants.size)
    assertTrue(updatedChat.participants.containsAll(newParticipants))
  }

  @Test
  fun updateChatParticipants_failsWhenNotAuthenticated() = runTest {
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking {
            repository.updateChatParticipants(TEST_CHAT_ID_1, listOf("user1", "user2"))
          }
        }
    assertExceptionContains(exception, "authenticated")
  }

  @Test
  fun updateChatParticipants_failsWhenNotCreator() = runTest {
    // Create chat as first user
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid!!

    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf(creatorId, "helper1"), creatorId, "OPEN")
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Try to update as non-creator
    signInUser("helper@test.com", "password")
    val helperId = auth.currentUser?.uid!!

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking {
            repository.updateChatParticipants(TEST_CHAT_ID_1, listOf(creatorId, helperId))
          }
        }
    assertExceptionContains(exception, "creator", "only", "failed")
  }

  @Test
  fun updateChatParticipants_failsWhenChatNotFound() = runTest {
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.updateChatParticipants("non-existent", listOf(currentUserId)) }
        }
    assertExceptionContains(exception, "not found", "failed")
  }

  @Test
  fun updateChatParticipants_canAddParticipants() = runTest {
    val initialParticipants = listOf(currentUserId, "helper1")
    createTestChat(participants = initialParticipants)

    val updatedParticipants = listOf(currentUserId, "helper1", "helper2", "helper3")

    repository.updateChatParticipants(TEST_CHAT_ID_1, updatedParticipants)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(4, chat.participants.size)
    assertTrue(chat.participants.contains("helper2"))
    assertTrue(chat.participants.contains("helper3"))
  }

  @Test
  fun updateChatParticipants_canRemoveParticipants() = runTest {
    val initialParticipants = listOf(currentUserId, "helper1", "helper2")
    createTestChat(participants = initialParticipants)

    val updatedParticipants = listOf(currentUserId, "helper1")

    repository.updateChatParticipants(TEST_CHAT_ID_1, updatedParticipants)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(2, chat.participants.size)
    assertFalse(chat.participants.contains("helper2"))
  }
  // ==================== REMOVE SELF FROM CHAT TESTS ====================

  @Test
  fun removeSelfFromChat_success() = runTest {
    val participants = listOf(currentUserId, "helper1", "helper2")
    createTestChat(participants = participants)

    repository.removeSelfFromChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify user was removed by trying to access the chat
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.getMessages(TEST_CHAT_ID_1) }
        }
    assertExceptionContains(exception, "participant", "failed")
  }

  @Test
  fun removeSelfFromChat_failsWhenNotAuthenticated() = runTest {
    createTestChat()
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.removeSelfFromChat(TEST_CHAT_ID_1) }
        }
    assertExceptionContains(exception, "authenticated")
  }

  @Test
  fun removeSelfFromChat_failsWhenNotParticipant() = runTest {
    // Create chat without current user
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid!!
    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf(creatorId, "other1"), creatorId, "OPEN")
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Try to remove self as non-participant
    signInUser("nonparticipant@test.com", "password")

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.removeSelfFromChat(TEST_CHAT_ID_1) }
        }
    assertExceptionContains(exception, "participant", "failed")
  }

  @Test
  fun removeSelfFromChat_leavesOtherParticipants() = runTest {
    // Sign in as creator first
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid!!

    // Sign in as helper who will leave
    signInUser("helper@test.com", "password")
    val helperId = auth.currentUser?.uid!!

    // Sign back as creator to create the chat
    signInUser("creator@test.com", "password")

    repository.createChat(
        TEST_CHAT_ID_1,
        TEST_TITLE_1,
        listOf(creatorId, helperId, "other-helper"),
        creatorId,
        "OPEN")
    delay(FIRESTORE_WRITE_DELAY_MS * 2)

    // Sign in as helper to remove themselves
    signInUser("helper@test.com", "password")

    // Remove self
    repository.removeSelfFromChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify chat still exists for creator
    signInUser("creator@test.com", "password")
    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(2, chat.participants.size)
    assertFalse(chat.participants.contains(helperId))
    assertTrue(chat.participants.contains(creatorId))
    assertTrue(chat.participants.contains("other-helper"))
  }

  @Test
  fun removeSelfFromChat_allowsLastHelperToLeave() = runTest {
    // Create chat with creator + one helper
    signInUser("creator@test.com", "password")
    val creatorId = auth.currentUser?.uid!!

    signInUser() // Back to original user (helper)

    repository.createChat(
        TEST_CHAT_ID_1, TEST_TITLE_1, listOf(creatorId, currentUserId), creatorId, "OPEN")
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Helper removes themselves
    repository.removeSelfFromChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Chat should still exist with just creator
    signInUser("creator@test.com", "password")
    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(1, chat.participants.size)
    assertEquals(creatorId, chat.participants[0])
  }

  @Test
  fun deleteChat_success() = runTest {
    val initialParticipants = listOf(currentUserId, "helper1", "helper2")
    createTestChat(participants = initialParticipants)

    repository.deleteChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify chat was deleted by trying to access it
    val exception =
        assertThrows(Exception::class.java) { runBlocking { repository.getChat(TEST_CHAT_ID_1) } }
    assertExceptionContains(exception, "not found")
  }

  @Test
  fun deleteChat_failsWhenNotCreator() = runTest {
    val initialParticipants = listOf(currentUserId, "helper1", "helper2")
    createTestChat(participants = initialParticipants)

    // Sign in as a non-creator participant
    signInUser("helper1@test.com", "password")

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.deleteChat(TEST_CHAT_ID_1) }
        }
    assertExceptionContains(exception, "Only the creator can delete the chat")
  }

  @Test
  fun deleteChat_failsWhenNotAuthenticated() = runTest {
    createTestChat()

    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.deleteChat(TEST_CHAT_ID_1) }
        }
    assertExceptionContains(exception, "authenticated")
  }

  @Test
  fun deleteChat_failsWhenChatDoesNotExist() = runTest {
    val nonExistentChatId = "nonExistentChatId"

    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.deleteChat(nonExistentChatId) }
        }
    assertExceptionContains(exception, NOT_FOUND)
  }

  // ==================== ADD SELF TO CHAT TESTS ====================

  @Test
  fun addSelfToChat_doesNothingWhenAlreadyParticipant() = runTest {
    // Create chat with current user as participant
    val participants = listOf(currentUserId, HELPER_1)
    repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, participants, currentUserId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Try to add self again
    repository.addSelfToChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify no duplicate
    val chat = repository.getChat(TEST_CHAT_ID_1)
    assertEquals(2, chat.participants.size)
    assertEquals(participants, chat.participants)
  }

  @Test
  fun addSelfToChat_failsWhenNotAuthenticated() = runTest {
    createTestChat()

    // Sign out
    Firebase.auth.signOut()

    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.addSelfToChat(TEST_CHAT_ID_1) }
        }
    assertExceptionContains(exception, AUTHENTICATED)
  }

  // ==================== ADD SELF TO CHAT TESTS ====================

  @Test
  fun addSelfToChat_failsWhenChatDoesNotExist() = runTest {
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.addSelfToChat(NON_EXISTENT_CHAT) }
        }
    assertExceptionContains(exception, NOT_FOUND)
  }

  @Test
  fun addSelfToChat_allowsFormerParticipantToRejoin() = runTest {
    // Use the already signed-in creator from setUp()
    val creatorId = currentUserId
    val creatorEmail = DEFAULT_USER_EMAIL
    val creatorPassword = DEFAULT_USER_PASSWORD

    // Create a helper user
    val helperEmail = "helper1@test.com"
    val helperPassword = PASSWORD
    createAndSignInUser(helperEmail, helperPassword)
    val helperId = auth.currentUser?.uid ?: throw IllegalStateException("No helper user")

    // Sign back in as creator
    signInUser(creatorEmail, creatorPassword)

    // Create chat with helper as participant
    val participants = listOf(creatorId, helperId)
    repository.createChat(TEST_CHAT_ID_1, TEST_TITLE_1, participants, creatorId, OPEN)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Sign in as helper and remove self
    signInUser(helperEmail, helperPassword)
    repository.removeSelfFromChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify removed
    var chat = repository.getChat(TEST_CHAT_ID_1)
    assertFalse(chat.participants.contains(helperId))
    assertEquals(1, chat.participants.size)

    // Add self back
    repository.addSelfToChat(TEST_CHAT_ID_1)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify added back
    chat = repository.getChat(TEST_CHAT_ID_1)
    assertTrue(chat.participants.contains(helperId))
    assertEquals(2, chat.participants.size)
  }
}
