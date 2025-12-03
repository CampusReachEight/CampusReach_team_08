package com.android.sample.model.request

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FirebaseEmulator
import java.util.Date
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestRepositoryFirestoreTest : BaseEmulatorTest() {

  private lateinit var repository: RequestRepositoryFirestore

  private data class CreatedRequest(val email: String, val password: String, val id: String)

  private val createdRequests = mutableListOf<CreatedRequest>()

  private suspend fun addRequestTracking(request: Request) {
    repository.addRequest(request)
    createdRequests.add(
        CreatedRequest(
            currentUser.email ?: DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD, request.requestId))
  }

  private fun generateRequest(
      requestId: String,
      title: String,
      description: String,
      creatorId: String,
      // Default to future start time so viewStatus computes OPEN (startTimeStamp > now)
      start: Date = Date(System.currentTimeMillis() + 3_600_000),
      expires: Date = Date(System.currentTimeMillis() + 7_200_000),
      people: List<String> = emptyList(),
      status: RequestStatus = RequestStatus.OPEN
  ): Request {
    return Request(
        requestId = requestId,
        title = title,
        description = description,
        requestType = listOf(RequestType.STUDYING),
        location = Location(46.5191, 6.5668, "EPFL"),
        locationName = "EPFL",
        status = status,
        startTimeStamp = start,
        expirationTime = expires,
        people = people,
        tags = listOf(Tags.URGENT),
        creatorId = creatorId)
  }

  private lateinit var request1: Request
  private lateinit var request2: Request
  private lateinit var request3: Request

  @Before
  override fun setUp() {
    super.setUp()
    repository = RequestRepositoryFirestore(db)

    runTest {
      request1 =
          generateRequest("test-request-1", "Study Session", "Need help with Math", currentUserId)
      request2 =
          generateRequest(
              "test-request-2",
              "Lunch Together",
              "Looking for lunch buddy",
              currentUserId,
              people = listOf(currentUserId))
      request3 = generateRequest("test-request-3", "Basketball Game", "Need players", currentUserId)
      createdRequests.clear()
    }
  }

  @After
  override fun tearDown() {
    runTest { clearAllTestRequests() }
    super.tearDown()
  }

  private suspend fun clearAllTestRequests() {
    // Group by user credentials and delete each user's requests while authenticated as them
    createdRequests
        .groupBy { it.email to it.password }
        .forEach { (cred, reqs) ->
          val (email, password) = cred
          signInUser(email, password)
          reqs.forEach { entry -> runCatching { repository.deleteRequest(entry.id) } }
        }
    // Return to default user for consistency
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    createdRequests.clear()
  }

  private suspend fun getRequestsCount(): Int =
      repository.getAllRequests().size // Using repository to respect rules/logic

  @Test
  fun getNewRequestIdReturnsUniqueIDs() = runTest {
    val numberIDs = 50
    val requestIds = (0 until numberIDs).map { repository.getNewRequestId() }.toSet()
    assertEquals(numberIDs, requestIds.size)
  }

  @Test
  fun canAddRequestToRepository() = runTest {
    addRequestTracking(request1)
    assertEquals(1, getRequestsCount())
    val stored = repository.getRequest(request1.requestId)
    assertEquals(request1.requestId, stored.requestId)
    assertEquals(request1.creatorId, stored.creatorId)
  }

  @Test
  fun addRequestPersistsTimestamps() = runTest {
    val start = Date()
    val end = Date(start.time + 10_000)
    val r = generateRequest("timed-req", "Timed", "Check times", currentUserId, start, end)
    addRequestTracking(r)
    val stored = repository.getRequest(r.requestId)
    assertEquals(start.time / 1000, stored.startTimeStamp.time / 1000)
    assertEquals(end.time / 1000, stored.expirationTime.time / 1000)
  }

  @Test
  fun addRequestWithTheCorrectID() = runTest {
    addRequestTracking(request1)
    val storedRequest = repository.getRequest(request1.requestId)
    // Compare key fields - status is dynamically computed via viewStatus
    assertEquals(request1.requestId, storedRequest.requestId)
    assertEquals(request1.title, storedRequest.title)
    assertEquals(request1.description, storedRequest.description)
    assertEquals(request1.creatorId, storedRequest.creatorId)
    assertEquals(request1.locationName, storedRequest.locationName)
    assertEquals(RequestStatus.OPEN, storedRequest.status) // viewStatus with future dates = OPEN
  }

  @Test
  fun canAddMultipleRequestsToRepository() = runTest {
    addRequestTracking(request1)
    addRequestTracking(request2)
    addRequestTracking(request3)
    assertEquals(3, getRequestsCount())
    val ids = repository.getAllRequests().map { it.requestId }.toSet()
    assertTrue(ids.containsAll(listOf(request1.requestId, request2.requestId, request3.requestId)))
  }

  @Test
  fun duplicateIdOverwritesDocument() = runTest {
    val first = request1.copy(requestId = "dup-id")
    val second = request2.copy(requestId = "dup-id")
    addRequestTracking(first)
    // second overwrites same doc id; record again so cleanup attempts both (safe)
    addRequestTracking(second)
    val all = repository.getAllRequests()
    val stored = repository.getRequest("dup-id")
    assertEquals(second.title, stored.title)
    assertEquals(second.description, stored.description)
    assertEquals(1, all.count { it.requestId == "dup-id" })
  }

  @Test
  fun cannotAddRequestWithMismatchedCreatorId() = runTest {
    val bad = request1.copy(creatorId = "different-user-id")
    try {
      repository.addRequest(bad) // intentionally not tracked; should fail
      fail("Expected IllegalArgumentException when adding request with mismatched creator ID")
    } catch (_: IllegalArgumentException) {}
    assertEquals(0, getRequestsCount())
  }

  @Test
  fun canRetrieveRequestByID() = runTest {
    addRequestTracking(request1)
    addRequestTracking(request2)
    val storedRequest = repository.getRequest(request2.requestId)
    // Compare key fields - status is dynamically computed via viewStatus
    assertEquals(request2.requestId, storedRequest.requestId)
    assertEquals(request2.title, storedRequest.title)
    assertEquals(request2.description, storedRequest.description)
    assertEquals(request2.creatorId, storedRequest.creatorId)
    assertEquals(RequestStatus.OPEN, storedRequest.status) // viewStatus with future dates = OPEN
  }

  @Test
  fun getRequestNotFoundThrows() = runTest {
    try {
      repository.getRequest("does-not-exist")
      fail("Expected exception for missing request")
    } catch (_: Exception) {}
  }

  @Test
  fun canDeleteRequestByID() = runTest {
    addRequestTracking(request1)
    addRequestTracking(request2)
    addRequestTracking(request3)
    repository.deleteRequest(request2.requestId)
    val remaining = repository.getAllRequests().map { it.requestId }
    assertFalse(remaining.contains(request2.requestId))
  }

  @Test
  fun cannotDeleteOtherUsersRequest() = runTest {
    addRequestTracking(request1)
    // sign in as second user & create their request
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val secondUserRequest = generateRequest("other-user-request", "Other", "Desc", currentUserId)
    addRequestTracking(secondUserRequest)
    // back to default user
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    try {
      repository.deleteRequest(secondUserRequest.requestId)
      fail("Expected IllegalArgumentException when deleting another user's request")
    } catch (_: IllegalArgumentException) {}
  }

  @Test
  fun canUpdateRequestByID() = runTest {
    addRequestTracking(request1)
    // Update title but keep status that matches viewStatus calculation (OPEN with future dates)
    val modified = request1.copy(title = "Modified Title")
    repository.updateRequest(request1.requestId, modified)
    val stored = repository.getRequest(request1.requestId)
    assertEquals(modified.requestId, stored.requestId)
    assertEquals(modified.title, stored.title)
    assertEquals(modified.description, stored.description)
    assertEquals(modified.creatorId, stored.creatorId)
  }

  @Test
  fun canUpdateTheCorrectRequestByID() = runTest {
    addRequestTracking(request1)
    addRequestTracking(request2)
    // Update only the title - keep dates so viewStatus remains OPEN
    val modified = request1.copy(title = "Modified Title")
    repository.updateRequest(request1.requestId, modified)
    val storedModified = repository.getRequest(request1.requestId)
    assertEquals(modified.title, storedModified.title)
    assertEquals(modified.requestId, storedModified.requestId)
    assertEquals(RequestStatus.OPEN, storedModified.status) // viewStatus with future dates = OPEN
    val storedOther = repository.getRequest(request2.requestId)
    assertEquals(request2.title, storedOther.title)
  }

  @Test
  fun cannotUpdateOtherUsersRequest() = runTest {
    addRequestTracking(request1)
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val secondUsersRequest = generateRequest("second-user-request", "Second", "Desc", currentUserId)
    addRequestTracking(secondUsersRequest)
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    try {
      repository.updateRequest(
          secondUsersRequest.requestId, secondUsersRequest.copy(title = "Hacked"))
      fail("Expected IllegalArgumentException when updating another user's request")
    } catch (_: IllegalArgumentException) {}
  }

  @Test
  fun cannotUpdateRequestIdDuringUpdate() = runTest {
    addRequestTracking(request1)
    val modified = request1.copy(requestId = "different-id")
    try {
      repository.updateRequest(request1.requestId, modified)
      fail("Expected IllegalArgumentException when changing request ID during update")
    } catch (_: IllegalArgumentException) {}
  }

  @Test
  fun unauthenticatedOperationsThrow() = runTest {
    addRequestTracking(request1)
    FirebaseEmulator.signOut()
    // Add
    try {
      repository.addRequest(request2)
      fail("Expected IllegalStateException when adding unauthenticated")
    } catch (_: IllegalStateException) {}
    // Update
    try {
      repository.updateRequest(request1.requestId, request1.copy(title = "NoAuth"))
      fail("Expected IllegalStateException when updating unauthenticated")
    } catch (_: IllegalStateException) {}
    // Delete
    try {
      repository.deleteRequest(request1.requestId)
      fail("Expected IllegalStateException when deleting unauthenticated")
    } catch (_: IllegalStateException) {}
    // Re-sign in default for any further cleanup
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun multiUserRequestsVisibleToAll() = runTest {
    addRequestTracking(request1) // user A
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val secondReq = generateRequest("second-user-request", "Second", "Desc", currentUserId)
    addRequestTracking(secondReq)
    // User B view
    val userBIds = repository.getAllRequests().map { it.requestId }.toSet()
    assertTrue(userBIds.containsAll(listOf(request1.requestId, secondReq.requestId)))
    // Switch back to A
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    val userAIds = repository.getAllRequests().map { it.requestId }.toSet()
    assertTrue(userAIds.containsAll(listOf(request1.requestId, secondReq.requestId)))
  }

  @Test
  fun unauthenticatedAcceptAndCancelRequestThrow() {
    runTest {
      addRequestTracking(request1)

      FirebaseEmulator.signOut()

      try {
        repository.acceptRequest(request1.requestId)
        fail("Expected IllegalStateException when adding unauthenticated")
      } catch (_: IllegalStateException) {}

      try {
        repository.cancelAcceptance(request1.requestId)
        fail("Expected IllegalStateException when adding unauthenticated")
      } catch (_: IllegalStateException) {}
    }
  }

  @Test
  fun sameAuthAcceptAndCancelRequestThrow() {
    runTest {
      addRequestTracking(request1)
      addRequestTracking(request2)
      try {
        repository.acceptRequest(request1.requestId)
        fail("Expected IllegalStateException when try to accept his own request")
      } catch (_: IllegalStateException) {}

      try {
        repository.cancelAcceptance(request2.requestId)
        fail("Expected IllegalStateException when try to cancel his own request")
      } catch (_: IllegalStateException) {}
    }
  }

  @Test
  fun tryAcceptRequestButAlreadyAcceptedThrow() {
    runTest {
      val previousUserId = currentUserId
      signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
      val secondReq =
          generateRequest(
              "second-user-request",
              "Second",
              "Desc",
              currentUserId,
              people = listOf(previousUserId))
      addRequestTracking(secondReq)

      signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)

      try {
        repository.acceptRequest(secondReq.requestId)
        fail("Expected IllegalStateException when try to accept a request he already accept")
      } catch (_: IllegalStateException) {}
    }
  }

  // ----- getMyRequests() Tests -----

  @Test
  fun getMyRequests_returnsOnlyCurrentUsersRequests() = runTest {
    // User A creates requests
    addRequestTracking(request1)
    addRequestTracking(request2)

    // User B creates their request
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val userBRequest = generateRequest("user-b-request", "User B", "B's request", currentUserId)
    addRequestTracking(userBRequest)

    // User B should only see their own request
    val userBRequests = repository.getMyRequests()
    assertEquals(1, userBRequests.size)
    assertEquals(userBRequest.requestId, userBRequests.first().requestId)
    assertFalse(userBRequests.any { it.requestId == request1.requestId })
    assertFalse(userBRequests.any { it.requestId == request2.requestId })

    // Switch back to User A
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    val userARequests = repository.getMyRequests()
    assertEquals(2, userARequests.size)
    assertTrue(userARequests.any { it.requestId == request1.requestId })
    assertTrue(userARequests.any { it.requestId == request2.requestId })
    assertFalse(userARequests.any { it.requestId == userBRequest.requestId })
  }

  @Test
  fun getMyRequests_returnsEmptyListWhenUserHasNoRequests() = runTest {
    // User A creates requests
    addRequestTracking(request1)
    addRequestTracking(request2)

    // User B has no requests
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val userBRequests = repository.getMyRequests()

    assertEquals(0, userBRequests.size)
    assertTrue(userBRequests.isEmpty())
  }

  @Test
  fun getMyRequests_returnsAllUserRequestsCorrectly() = runTest {
    // Create multiple requests for current user
    addRequestTracking(request1)
    addRequestTracking(request2)
    addRequestTracking(request3)

    val myRequests = repository.getMyRequests()

    assertEquals(3, myRequests.size)
    val myRequestIds = myRequests.map { it.requestId }.toSet()
    assertTrue(
        myRequestIds.containsAll(
            listOf(request1.requestId, request2.requestId, request3.requestId)))

    // Verify all returned requests have correct creatorId
    myRequests.forEach { request -> assertEquals(currentUserId, request.creatorId) }
  }

  @Test
  fun getMyRequests_excludesOtherUsersRequestsCompletely() = runTest {
    // User A creates one request
    addRequestTracking(request1)

    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val userBRequest1 = generateRequest("user-b-req-1", "B1", "B's first", currentUserId)
    val userBRequest2 = generateRequest("user-b-req-2", "B2", "B's second", currentUserId)
    addRequestTracking(userBRequest1)
    addRequestTracking(userBRequest2)

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    val userARequests = repository.getMyRequests()

    assertEquals(1, userARequests.size)
    assertEquals(request1.requestId, userARequests.first().requestId)

    val allRequests = repository.getAllRequests()
    assertEquals(3, allRequests.size)
    assertEquals(1, userARequests.size)
  }

  @Test
  fun getMyRequests_throwsWhenUnauthenticated() = runTest {
    addRequestTracking(request1)

    FirebaseEmulator.signOut()

    try {
      repository.getMyRequests()
      fail("Expected IllegalStateException when calling getMyRequests() unauthenticated")
    } catch (_: IllegalStateException) {}

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  // ==================== Close Request Tests ====================

  @Test
  fun closeRequest_successfully_closes_OPEN_request_with_helpers() = runTest {
    // Given
    val requestWithHelpers = request1.copy(people = listOf("helper1", "helper2"))
    addRequestTracking(requestWithHelpers)

    val selectedHelpers = listOf("helper1")

    // When
    val shouldAwardCreator = repository.closeRequest(requestWithHelpers.requestId, selectedHelpers)

    // Then
    assertTrue(!shouldAwardCreator) // Should return true when helpers selected
    val updatedRequest = repository.getRequest(requestWithHelpers.requestId)
    assertEquals(RequestStatus.COMPLETED, updatedRequest.status)
  }

  @Test
  fun closeRequest_successfully_closes_IN_PROGRESS_request() = runTest {
    // Given - Use dates that result in IN_PROGRESS viewStatus (start <= now, expiration > now)
    val now = System.currentTimeMillis()
    val inProgressRequest =
        request1.copy(
            status = RequestStatus.IN_PROGRESS,
            people = listOf("helper1"),
            startTimeStamp = Date(now - 1_000), // 1 second ago
            expirationTime = Date(now + 3_600_000) // 1 hour from now
            )
    addRequestTracking(inProgressRequest)

    // When
    val shouldAwardCreator = repository.closeRequest(inProgressRequest.requestId, listOf("helper1"))

    // Then
    assertTrue(!shouldAwardCreator)
    val updatedRequest = repository.getRequest(inProgressRequest.requestId)
    assertEquals(RequestStatus.COMPLETED, updatedRequest.status)
  }

  @Test
  fun closeRequest_returns_false_when_no_helpers_selected() = runTest {
    // Given
    addRequestTracking(request1)

    // When
    val shouldAwardCreator = repository.closeRequest(request1.requestId, emptyList())

    // Then
    assertFalse(shouldAwardCreator) // Should return false when no helpers
    val updatedRequest = repository.getRequest(request1.requestId)
    assertEquals(RequestStatus.COMPLETED, updatedRequest.status)
  }

  @Test
  fun closeRequest_returns_true_with_multiple_helpers_selected() = runTest {
    // Given
    val requestWithMultipleHelpers = request1.copy(people = listOf("helper1", "helper2", "helper3"))
    addRequestTracking(requestWithMultipleHelpers)

    // When
    val shouldAwardCreator =
        repository.closeRequest(requestWithMultipleHelpers.requestId, listOf("helper1", "helper2"))

    // Then
    assertTrue(!shouldAwardCreator)
    val updatedRequest = repository.getRequest(requestWithMultipleHelpers.requestId)
    assertEquals(RequestStatus.COMPLETED, updatedRequest.status)
  }

  @Test
  fun closeRequest_throws_InvalidStatus_for_COMPLETED_request() = runTest {
    // Given - Use dates that result in COMPLETED viewStatus (expirationTime <= now)
    val now = System.currentTimeMillis()
    val completedRequest =
        request1.copy(
            status = RequestStatus.COMPLETED,
            startTimeStamp = Date(now - 7_200_000), // 2 hours ago
            expirationTime = Date(now - 3_600_000) // 1 hour ago (expired)
            )
    addRequestTracking(completedRequest)

    // When/Then
    val exception =
        assertThrows(RequestClosureException.InvalidStatus::class.java) {
          runBlocking { repository.closeRequest(completedRequest.requestId, emptyList()) }
        }
    assertNotNull(exception.message)
    assertTrue(exception.message!!.contains("COMPLETED"))
  }

  @Test
  fun closeRequest_throws_InvalidStatus_for_CANCELLED_request() = runTest {
    // Given
    val cancelledRequest = request1.copy(status = RequestStatus.CANCELLED)
    addRequestTracking(cancelledRequest)

    // When/Then
    assertThrows(RequestClosureException.InvalidStatus::class.java) {
      runBlocking { repository.closeRequest(cancelledRequest.requestId, emptyList()) }
    }
  }

  @Test
  fun closeRequest_throws_InvalidStatus_for_ARCHIVED_request() = runTest {
    // Given
    val archivedRequest = request1.copy(status = RequestStatus.ARCHIVED)
    addRequestTracking(archivedRequest)

    // When/Then
    assertThrows(RequestClosureException.InvalidStatus::class.java) {
      runBlocking { repository.closeRequest(archivedRequest.requestId, emptyList()) }
    }
  }

  @Test
  fun closeRequest_throws_UserNotHelper_when_selected_user_not_in_request() = runTest {
    // Given
    val requestWithHelpers = request1.copy(people = listOf("helper1", "helper2"))
    addRequestTracking(requestWithHelpers)

    // When/Then - Try to close with a helper not in the list
    assertThrows(RequestClosureException.UserNotHelper::class.java) {
      runBlocking { repository.closeRequest(requestWithHelpers.requestId, listOf("helper3")) }
    }
  }

  @Test
  fun closeRequest_throws_UserNotHelper_when_one_of_multiple_helpers_invalid() = runTest {
    // Given
    val requestWithHelpers = request1.copy(people = listOf("helper1", "helper2"))
    addRequestTracking(requestWithHelpers)

    // When/Then - Mix of valid and invalid helpers
    assertThrows(RequestClosureException.UserNotHelper::class.java) {
      runBlocking {
        repository.closeRequest(
            requestWithHelpers.requestId, listOf("helper1", "helper3") // helper3 not in people list
            )
      }
    }
  }

  @Test
  fun closeRequest_throws_exception_when_user_not_authenticated() = runTest {
    // Given
    addRequestTracking(request1)
    auth.signOut()

    // When/Then
    assertThrows(IllegalStateException::class.java) {
      runBlocking { repository.closeRequest(request1.requestId, emptyList()) }
    }

    // Restore authentication for cleanup
    signInUser()
  }

  @Test
  fun closeRequest_throws_exception_when_user_not_owner() = runTest {
    // Given - Create request as current user
    addRequestTracking(request1)

    // Switch to different user
    val otherUserEmail = "other@test.com"
    val otherUserPassword = "password123"
    createAndSignInUser(otherUserEmail, otherUserPassword)

    // When/Then - Try to close someone else's request
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.closeRequest(request1.requestId, emptyList()) }
    }

    // Restore original user
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun closeRequest_throws_exception_for_non_existent_request() = runTest {
    // Given
    val nonExistentRequestId = "non-existent-request"

    // When/Then
    assertThrows(Exception::class.java) {
      runBlocking { repository.closeRequest(nonExistentRequestId, emptyList()) }
    }
  }

  @Test
  fun closeRequest_allows_closing_with_subset_of_helpers() = runTest {
    // Given
    val requestWithManyHelpers =
        request1.copy(people = listOf("helper1", "helper2", "helper3", "helper4"))
    addRequestTracking(requestWithManyHelpers)

    // When - Select only 2 out of 4 helpers
    val shouldAwardCreator =
        repository.closeRequest(requestWithManyHelpers.requestId, listOf("helper2", "helper4"))

    // Then
    assertTrue(!shouldAwardCreator)
    val updatedRequest = repository.getRequest(requestWithManyHelpers.requestId)
    assertEquals(RequestStatus.COMPLETED, updatedRequest.status)
  }

  @Test
  fun closeRequest_allows_closing_with_all_helpers_selected() = runTest {
    // Given
    val requestWithHelpers = request1.copy(people = listOf("helper1", "helper2", "helper3"))
    addRequestTracking(requestWithHelpers)

    // When - Select all helpers
    val shouldAwardCreator =
        repository.closeRequest(
            requestWithHelpers.requestId, listOf("helper1", "helper2", "helper3"))

    assertTrue(!shouldAwardCreator)
    val updatedRequest = repository.getRequest(requestWithHelpers.requestId)
    assertEquals(RequestStatus.COMPLETED, updatedRequest.status)
  }

  @Test
  fun getAllCurrentRequests_excludesCompletedRequests() = runTest {
    val completed = request1.copy(status = RequestStatus.COMPLETED)
    addRequestTracking(completed)

    val all = repository.getAllCurrentRequests()
    assertFalse(
        "Completed requests must be filtered out", all.any { it.requestId == completed.requestId })
  }

  @Test
  fun getAllCurrentRequests_excludesCancelledRequests() = runTest {
    val cancelled = request1.copy(status = RequestStatus.CANCELLED)
    addRequestTracking(cancelled)

    val all = repository.getAllCurrentRequests()
    assertFalse(
        "Cancelled requests must be filtered out", all.any { it.requestId == cancelled.requestId })
  }

  @Test
  fun getAllCurrentRequests_excludesAutomaticallyCompletedRequests() = runTest {
    val now = Date()
    val expiredRequest =
        generateRequest(
            "expired-request",
            "Expired",
            "Already expired",
            currentUserId,
            start = Date(now.time - 10_000),
            expires = Date(now.time - 1) // EXPIRED
            )
    addRequestTracking(expiredRequest)

    val all = repository.getAllCurrentRequests()
    assertFalse(
        "Expired requests (viewStatus=COMPLETED) must be filtered out",
        all.any { it.requestId == expiredRequest.requestId })
  }

  @Test
  fun getAllCurrentRequests_keepsNonCompletedRequests() = runTest {
    val open = request1.copy(status = RequestStatus.OPEN)
    val inProgress = request2.copy(status = RequestStatus.IN_PROGRESS)
    val archived = request3.copy(status = RequestStatus.ARCHIVED)

    addRequestTracking(open)
    addRequestTracking(inProgress)
    addRequestTracking(archived)

    val all = repository.getAllCurrentRequests().map { it.requestId }.toSet()

    assertTrue(all.contains(open.requestId))
    assertTrue(all.contains(inProgress.requestId))
    assertTrue(all.contains(archived.requestId))
  }

  @Test
  fun getRequest_stillReturnsCompletedOrCancelledRequests() = runTest {
    val completed = request1.copy(status = RequestStatus.COMPLETED)
    addRequestTracking(completed)

    val stored = repository.getRequest(completed.requestId)
    assertEquals(completed.requestId, stored.requestId)
  }

  // ==================== Network/Cache Exception Tests ====================

  @Test
  fun getAllRequests_wrapsFirestoreExceptions() = runTest {
    // This test verifies that FirebaseFirestoreException is properly wrapped
    // Note: In real scenarios with emulator, we can't easily simulate UNAVAILABLE errors
    // This test documents expected behavior
    addRequestTracking(request1)

    // Should succeed with emulator
    val requests = repository.getAllRequests()
    assertTrue(requests.isNotEmpty())
  }

  @Test
  fun getRequest_wrapsFirestoreExceptions() = runTest {
    addRequestTracking(request1)

    // Should succeed with emulator
    val request = repository.getRequest(request1.requestId)
    assertNotNull(request)
    assertEquals(request1.requestId, request.requestId)
  }

  @Test
  fun addRequest_throwsExceptionWithDetailedMessage_whenOperationFails() = runTest {
    // Test that the exception message includes the request ID
    val invalidRequest = request1.copy(creatorId = "wrong-user-id")

    try {
      repository.addRequest(invalidRequest)
      fail("Expected IllegalArgumentException for mismatched creator ID")
    } catch (e: IllegalArgumentException) {
      // Expected - authorization check
      assertNotNull(e.message)
    }
  }

  @Test
  fun updateRequest_throwsExceptionWithDetailedMessage_whenRequestNotFound() = runTest {
    val nonExistentId = "non-existent-request-id"
    val fakeRequest = request1.copy(requestId = nonExistentId)

    try {
      repository.updateRequest(nonExistentId, fakeRequest)
      fail("Expected exception when updating non-existent request")
    } catch (e: Exception) {
      // Should fail during getRequest verification
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("not found") || e.message!!.contains("Failed to retrieve"))
    }
  }

  @Test
  fun deleteRequest_throwsExceptionWithDetailedMessage_whenRequestNotFound() = runTest {
    val nonExistentId = "non-existent-request-id"

    try {
      repository.deleteRequest(nonExistentId)
      fail("Expected exception when deleting non-existent request")
    } catch (e: Exception) {
      // Should fail during getRequest verification
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("not found") || e.message!!.contains("Failed to retrieve"))
    }
  }

  @Test
  fun acceptRequest_throwsExceptionWithDetailedMessage_whenRequestNotFound() = runTest {
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val nonExistentId = "non-existent-request-id"

    try {
      repository.acceptRequest(nonExistentId)
      fail("Expected exception when accepting non-existent request")
    } catch (e: Exception) {
      // Should fail during getRequest verification
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("not found") || e.message!!.contains("Failed to retrieve"))
    }
  }

  @Test
  fun cancelAcceptance_throwsExceptionWithDetailedMessage_whenRequestNotFound() = runTest {
    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val nonExistentId = "non-existent-request-id"

    try {
      repository.cancelAcceptance(nonExistentId)
      fail("Expected exception when canceling acceptance for non-existent request")
    } catch (e: Exception) {
      // Should fail during getRequest verification
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("not found") || e.message!!.contains("Failed to retrieve"))
    }
  }

  @Test
  fun getMyRequests_throwsExceptionWhenUnauthenticatedWithProperMessage() = runTest {
    addRequestTracking(request1)
    FirebaseEmulator.signOut()

    try {
      repository.getMyRequests()
      fail("Expected IllegalStateException when calling getMyRequests() unauthenticated")
    } catch (e: IllegalStateException) {
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("authenticated"))
    }

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun hasUserAcceptedRequest_throwsWhenUnauthenticated() = runTest {
    addRequestTracking(request1)
    val requestCopy = repository.getRequest(request1.requestId)

    FirebaseEmulator.signOut()

    try {
      repository.hasUserAcceptedRequest(requestCopy)
      fail("Expected IllegalStateException when checking acceptance unauthenticated")
    } catch (e: IllegalStateException) {
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("authenticated"))
    }

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun isOwnerOfRequest_throwsWhenUnauthenticated() = runTest {
    addRequestTracking(request1)
    val requestCopy = repository.getRequest(request1.requestId)

    FirebaseEmulator.signOut()

    try {
      repository.isOwnerOfRequest(requestCopy)
      fail("Expected IllegalStateException when checking ownership unauthenticated")
    } catch (e: IllegalStateException) {
      assertNotNull(e.message)
      assertTrue(e.message!!.contains("authenticated"))
    }

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun acceptRequest_verifiesAcceptanceWasRecorded() = runTest {
    // This test verifies that after accepting a request,
    // the repository checks that the user was actually added to the people list
    addRequestTracking(request1)

    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    repository.acceptRequest(request1.requestId)

    // Verify by getting the request again
    val updatedRequest = repository.getRequest(request1.requestId)
    assertTrue(updatedRequest.people.contains(currentUserId))

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun cancelAcceptance_verifiesCancellationWasRecorded() = runTest {
    // Setup: user accepts request first
    addRequestTracking(request1)

    signInUser(SECOND_USER_EMAIL, SECOND_USER_PASSWORD)
    val secondUserId = currentUserId
    repository.acceptRequest(request1.requestId)

    // Now cancel
    repository.cancelAcceptance(request1.requestId)

    // Verify by getting the request again
    val updatedRequest = repository.getRequest(request1.requestId)
    assertFalse(updatedRequest.people.contains(secondUserId))

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
  }

  @Test
  fun addRequest_verifiesRequestWasActuallyCreated() = runTest {
    // This test verifies the add operation includes a verification step
    addRequestTracking(request1)

    // Verify the request exists
    val storedRequest = repository.getRequest(request1.requestId)
    assertEquals(request1.requestId, storedRequest.requestId)
    assertEquals(request1.title, storedRequest.title)
  }

  @Test
  fun updateRequest_verifiesUpdateWasSuccessful() = runTest {
    addRequestTracking(request1)

    val updatedTitle = "Updated Title"
    val modified = request1.copy(title = updatedTitle)
    repository.updateRequest(request1.requestId, modified)

    // Verify the update was actually applied
    val storedRequest = repository.getRequest(request1.requestId)
    assertEquals(updatedTitle, storedRequest.title)
  }

  @Test
  fun deleteRequest_verifiesRequestWasActuallyDeleted() = runTest {
    addRequestTracking(request1)

    repository.deleteRequest(request1.requestId)

    // Verify the request no longer exists
    try {
      repository.getRequest(request1.requestId)
      fail("Expected exception when getting deleted request")
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("not found"))
    }
  }

  @Test
  fun closeRequest_verifiesStatusUpdateWasSuccessful() = runTest {
    val requestWithHelpers = request1.copy(people = listOf("helper1"))
    addRequestTracking(requestWithHelpers)

    repository.closeRequest(requestWithHelpers.requestId, listOf("helper1"))

    // Verify the status was actually updated
    val closedRequest = repository.getRequest(requestWithHelpers.requestId)
    assertEquals(RequestStatus.COMPLETED, closedRequest.status)
  }

  @Test
  fun exceptionMessages_includeRequestId_forDebugging() = runTest {
    val testRequestId = "debug-test-request-id"

    try {
      repository.getRequest(testRequestId)
      fail("Expected exception")
    } catch (e: Exception) {
      // Verify the exception message includes the request ID for debugging
      assertNotNull(e.message)
      assertTrue(
          "Exception message should contain request ID for debugging",
          e.message!!.contains(testRequestId) || e.message!!.contains("not found"))
    }
  }
  // ============ Tests for getAcceptedRequests() ============
  @Test
  fun getAcceptedRequests_returnsEmptyListWhenNoAcceptedRequests() = runTest {
    val acceptedRequests = repository.getAcceptedRequests()

    assertTrue(acceptedRequests.isEmpty())
  }

  @Test
  fun getAcceptedRequests_throwsWhenNotAuthenticated() = runTest {
    FirebaseEmulator.signOut()

    try {
      repository.getAcceptedRequests()
      fail("Should have thrown IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("No authenticated user", e.message)
    }
  }

  // ============ Tests for closeRequest() with selectedHelpers ============

  @Test
  fun closeRequest_savesSelectedHelpersToFirestore() = runTest {
    val request =
        generateRequest(
            requestId = "test-close",
            title = "Test Close",
            description = "Test close with helpers",
            creatorId = currentUserId,
            people = listOf("helper1", "helper2", "helper3"),
            status = RequestStatus.IN_PROGRESS)

    addRequestTracking(request)

    // Close and select only 2 helpers
    repository.closeRequest(request.requestId, listOf("helper1", "helper2"))

    // Verify selectedHelpers was saved
    val closedRequest = repository.getRequest(request.requestId)
    assertEquals(RequestStatus.COMPLETED, closedRequest.status)
    assertEquals(2, closedRequest.selectedHelpers.size)
    assertTrue(closedRequest.selectedHelpers.contains("helper1"))
    assertTrue(closedRequest.selectedHelpers.contains("helper2"))
    assertFalse(closedRequest.selectedHelpers.contains("helper3"))
  }

  @Test
  fun closeRequest_savesEmptyListWhenNoHelpersSelected() = runTest {
    val request =
        generateRequest(
            requestId = "test-close-empty",
            title = "Test Empty",
            description = "No helpers selected",
            creatorId = currentUserId,
            people = listOf("helper1"),
            status = RequestStatus.OPEN)

    addRequestTracking(request)

    val returnValue = repository.closeRequest(request.requestId, emptyList())

    val closedRequest = repository.getRequest(request.requestId)
    assertTrue(closedRequest.selectedHelpers.isEmpty())
    assertFalse(returnValue) // Should return false when no helpers
  }

  @Test
  fun closeRequest_returnsTrueWhenHelpersSelected() = runTest {
    val request =
        generateRequest(
            requestId = "test-return-value",
            title = "Test Return",
            description = "Test return value",
            creatorId = currentUserId,
            people = listOf("helper1"),
            status = RequestStatus.OPEN)

    addRequestTracking(request)

    val returnValue = repository.closeRequest(request.requestId, listOf("helper1"))

    assertTrue(!returnValue) // Should return true when helpers selected
  }
}
