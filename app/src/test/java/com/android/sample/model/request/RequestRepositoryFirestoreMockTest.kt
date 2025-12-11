package com.android.sample.model.request

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SnapshotMetadata
import com.google.firebase.firestore.Source
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RequestRepositoryFirestoreMockTest {

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockQuery: Query
  private lateinit var mockQuerySnapshot: QuerySnapshot
  private lateinit var mockDocumentSnapshot: DocumentSnapshot
  private lateinit var mockMetadata: SnapshotMetadata
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  private lateinit var repository: RequestRepositoryFirestore

  private val testUserId = "test-user-123"
  private val testRequestId = "test-request-456"

  @Before
  fun setup() {
    mockFirestore = mockk()
    mockCollection = mockk()
    mockDocument = mockk()
    mockQuery = mockk()
    mockQuerySnapshot = mockk()
    mockDocumentSnapshot = mockk()
    mockMetadata = mockk()
    mockAuth = mockk()
    mockUser = mockk()

    every { mockFirestore.collection(REQUESTS_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

    // Mock Firebase.auth static call
    mockkStatic(FirebaseAuth::class)
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    repository = RequestRepositoryFirestore(mockFirestore)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun getAllCurrentRequests_throwsWhenIsFromCache() = runTest {
    every { mockCollection.get(Source.SERVER) } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.metadata } returns mockMetadata
    every { mockMetadata.isFromCache } returns true

    try {
      repository.getAllCurrentRequests()
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("data from cache") == true)
    }
  }

  // Test 1: getAllRequests - isFromCache = true
  @Test
  fun getAllRequests_throwsWhenIsFromCache() = runTest {
    every { mockCollection.get(Source.SERVER) } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.metadata } returns mockMetadata
    every { mockMetadata.isFromCache } returns true

    try {
      repository.getAllRequests()
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("data from cache") == true)
    }
  }

  // Test 2: getAllRequests - UNAVAILABLE exception
  @Test
  fun getAllRequests_throwsOnUnavailableException() = runTest {
    val exception = mockk<FirebaseFirestoreException>()
    every { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
    every { mockCollection.get(Source.SERVER) } returns Tasks.forException(exception)

    try {
      repository.getAllRequests()
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("Network unavailable") == true)
    }
  }

  // Test 3: getAllRequests - other FirebaseFirestoreException
  @Test
  fun getAllRequests_wrapsOtherFirestoreException() = runTest {
    val exception = mockk<FirebaseFirestoreException>()
    every { exception.code } returns FirebaseFirestoreException.Code.PERMISSION_DENIED
    every { exception.message } returns "Permission denied"
    every { mockCollection.get(Source.SERVER) } returns Tasks.forException(exception)

    try {
      repository.getAllRequests()
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to retrieve all requests") == true)
      assertTrue(e.message?.contains("Permission denied") == true)
    }
  }

  // Test 4: getAllRequests - generic Exception
  @Test
  fun getAllRequests_wrapsGenericException() = runTest {
    every { mockCollection.get(Source.SERVER) } returns
        Tasks.forException(RuntimeException("Unknown error"))

    try {
      repository.getAllRequests()
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to retrieve all requests") == true)
    }
  }

  // Test 5: getRequest - isFromCache = true
  @Test
  fun getRequest_throwsWhenIsFromCache() = runTest {
    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns mockQuery
    every { mockQuery.get(Source.SERVER) } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.metadata } returns mockMetadata
    every { mockMetadata.isFromCache } returns true

    try {
      repository.getRequest(testRequestId)
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("data from cache") == true)
    }
  }

  // Test 6: getRequest - UNAVAILABLE exception
  @Test
  fun getRequest_throwsOnUnavailableException() = runTest {
    val exception = mockk<FirebaseFirestoreException>()
    every { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns mockQuery
    every { mockQuery.get(Source.SERVER) } returns Tasks.forException(exception)

    try {
      repository.getRequest(testRequestId)
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("Network unavailable") == true)
    }
  }

  // Test 7: getRequest - other FirebaseFirestoreException with message
  @Test
  fun getRequest_wrapsFirestoreExceptionWithMessage() = runTest {
    val exception = mockk<FirebaseFirestoreException>()
    every { exception.code } returns FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    every { exception.message } returns "Timeout"
    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns mockQuery
    every { mockQuery.get(Source.SERVER) } returns Tasks.forException(exception)

    try {
      repository.getRequest(testRequestId)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to retrieve request") == true)
      assertTrue(e.message?.contains("Timeout") == true)
    }
  }

  // Test 9: addRequest - verification isFromCache = true
  @Test
  fun addRequest_throwsWhenVerificationIsFromCache() = runTest {
    val request = createTestRequest()
    every { mockDocument.set(any()) } returns Tasks.forResult(null)
    every { mockDocument.get(Source.SERVER) } returns Tasks.forResult(mockDocumentSnapshot)
    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.metadata } returns mockMetadata
    every { mockMetadata.isFromCache } returns true

    try {
      repository.addRequest(request)
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("Cannot verify request creation") == true)
    }
  }

  // Test 10: addRequest - UNAVAILABLE during add
  @Test
  fun addRequest_throwsOnUnavailableException() = runTest {
    val request = createTestRequest()
    val exception = mockk<FirebaseFirestoreException>()
    every { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
    every { mockDocument.set(any()) } returns Tasks.forException(exception)

    try {
      repository.addRequest(request)
      fail("Should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("Network unavailable") == true)
    }
  }

  // Test 11: addRequest - other FirebaseFirestoreException with message
  @Test
  fun addRequest_wrapsFirestoreExceptionWithMessage() = runTest {
    val request = createTestRequest()
    val exception = mockk<FirebaseFirestoreException>()
    every { exception.code } returns FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
    every { exception.message } returns "Quota exceeded"
    every { mockDocument.set(any()) } returns Tasks.forException(exception)

    try {
      repository.addRequest(request)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to add request") == true)
      assertTrue(e.message?.contains("Quota exceeded") == true)
    }
  }

  // Test 12: addRequest - generic Exception
  @Test
  fun addRequest_wrapsGenericException() = runTest {
    val request = createTestRequest()
    every { mockDocument.set(any()) } returns Tasks.forException(RuntimeException("Error"))

    try {
      repository.addRequest(request)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to add request") == true)
    }
  }

  private fun createTestRequest(): Request {
    return Request(
        requestId = testRequestId,
        title = "Test",
        description = "Test",
        requestType = listOf(RequestType.STUDYING),
        location = com.android.sample.model.map.Location(0.0, 0.0, "Test"),
        locationName = "Test",
        status = RequestStatus.OPEN,
        startTimeStamp = Date(),
        expirationTime = Date(),
        people = emptyList(),
        tags = emptyList(),
        creatorId = testUserId)
  }
  // ========== updateRequest - FIXED ==========

  @Test
  fun updateRequest_throwsWhenVerificationDocDisappears() = runTest {
    val request = createTestRequest()

    // Mock initial getRequest call - succeeds
    val initialQuery = mockk<Query>(relaxed = true)
    val initialSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val initialDoc = mockk<DocumentSnapshot>(relaxed = true)
    val initialMetadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns initialQuery
    every { initialQuery.get(Source.SERVER) } returns Tasks.forResult(initialSnapshot)
    every { initialSnapshot.metadata } returns initialMetadata
    every { initialMetadata.isFromCache } returns false
    every { initialSnapshot.documents } returns listOf(initialDoc)
    every { initialDoc.data } returns request.toMap()
    every { initialDoc.exists() } returns true

    // Mock the set operation - succeeds
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // Mock verification get - document doesn't exist (verifyRequest.exists() == false)
    val verifyDoc = mockk<DocumentSnapshot>(relaxed = true)
    every { verifyDoc.exists() } returns false
    every { mockDocument.get(Source.SERVER) } returns Tasks.forResult(verifyDoc)

    try {
      repository.updateRequest(testRequestId, request)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to update request") == true)
      assertTrue(
          e.message?.contains("Request disappeared during update") == true ||
              e.cause?.message?.contains("Request disappeared during update") == true)
    }
  }

  @Test
  fun updateRequest_throwsWhenVerificationIsFromCache() = runTest {
    val request = createTestRequest()

    // Mock initial getRequest
    val initialQuery = mockk<Query>(relaxed = true)
    val initialSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val initialDoc = mockk<DocumentSnapshot>(relaxed = true)
    val initialMetadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns initialQuery
    every { initialQuery.get(Source.SERVER) } returns Tasks.forResult(initialSnapshot)
    every { initialSnapshot.metadata } returns initialMetadata
    every { initialMetadata.isFromCache } returns false
    every { initialSnapshot.documents } returns listOf(initialDoc)
    every { initialDoc.data } returns request.toMap()
    every { initialDoc.exists() } returns true

    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // Verification is from cache
    val verifyDoc = mockk<DocumentSnapshot>(relaxed = true)
    val verifyMetadata = mockk<SnapshotMetadata>(relaxed = true)
    every { verifyDoc.exists() } returns true
    every { verifyDoc.metadata } returns verifyMetadata
    every { verifyMetadata.isFromCache } returns true
    every { mockDocument.get(Source.SERVER) } returns Tasks.forResult(verifyDoc)

    try {
      repository.updateRequest(testRequestId, request)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(
          e is IllegalStateException || e.message?.contains("Failed to update request") == true)
      assertTrue(
          e.message?.contains("Cannot verify request update") == true ||
              e.cause?.message?.contains("Cannot verify request update") == true)
    }
  }

  @Test
  fun updateRequest_throwsOnNonUnavailableFirestoreException() = runTest {
    val request = createTestRequest()

    // Mock initial getRequest
    val query = mockk<Query>(relaxed = true)
    val snapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<DocumentSnapshot>(relaxed = true)
    val metadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns query
    every { query.get(Source.SERVER) } returns Tasks.forResult(snapshot)
    every { snapshot.metadata } returns metadata
    every { metadata.isFromCache } returns false
    every { snapshot.documents } returns listOf(doc)
    every { doc.data } returns request.toMap()
    every { doc.exists() } returns true

    val exception = mockk<FirebaseFirestoreException>(relaxed = true)
    every { exception.code } returns FirebaseFirestoreException.Code.PERMISSION_DENIED
    every { exception.message } returns "Permission denied"
    every { mockDocument.set(any()) } returns Tasks.forException(exception)

    try {
      repository.updateRequest(testRequestId, request)
      fail("Should throw Exception")
    } catch (e: Exception) {
      // Gets wrapped in "Failed to update request"
      assertTrue(e.message?.contains("Failed to update request") == true)
      assertTrue(e.cause is FirebaseFirestoreException)
    }
  }

  @Test
  fun updateRequest_throwsWhenNotOwner() = runTest {
    val request = createTestRequest().copy(creatorId = "different-user")

    val query = mockk<Query>(relaxed = true)
    val snapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<DocumentSnapshot>(relaxed = true)
    val metadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns query
    every { query.get(Source.SERVER) } returns Tasks.forResult(snapshot)
    every { snapshot.metadata } returns metadata
    every { metadata.isFromCache } returns false
    every { snapshot.documents } returns listOf(doc)
    every { doc.data } returns request.toMap()
    every { doc.exists() } returns true

    try {
      repository.updateRequest(testRequestId, request)
      fail("Should throw IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Can only modify") == true)
    }
  }

  // ========== deleteRequest - FIXED ==========

  @Test
  fun deleteRequest_throwsWhenVerificationDocStillExists() = runTest {
    val request = createTestRequest()

    // Mock initial getRequest
    val initialQuery = mockk<Query>(relaxed = true)
    val initialSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val initialDoc = mockk<DocumentSnapshot>(relaxed = true)
    val initialMetadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns initialQuery
    every { initialQuery.get(Source.SERVER) } returns Tasks.forResult(initialSnapshot)
    every { initialSnapshot.metadata } returns initialMetadata
    every { initialMetadata.isFromCache } returns false
    every { initialSnapshot.documents } returns listOf(initialDoc)
    every { initialDoc.data } returns request.toMap()
    every { initialDoc.exists() } returns true

    every { mockDocument.delete() } returns Tasks.forResult(null)

    // Verification - doc still exists!
    val verifyDoc = mockk<DocumentSnapshot>(relaxed = true)
    every { verifyDoc.exists() } returns true
    every { mockDocument.get(Source.SERVER) } returns Tasks.forResult(verifyDoc)

    try {
      repository.deleteRequest(testRequestId)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to") == true)
      assertTrue(
          e.message?.contains("verify request deletion") == true ||
              e.cause?.message?.contains("verify request deletion") == true)
    }
  }

  @Test
  fun deleteRequest_throwsWhenVerificationIsFromCache() = runTest {
    val request = createTestRequest()

    // Mock initial getRequest
    val initialQuery = mockk<Query>(relaxed = true)
    val initialSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val initialDoc = mockk<DocumentSnapshot>(relaxed = true)
    val initialMetadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns initialQuery
    every { initialQuery.get(Source.SERVER) } returns Tasks.forResult(initialSnapshot)
    every { initialSnapshot.metadata } returns initialMetadata
    every { initialMetadata.isFromCache } returns false
    every { initialSnapshot.documents } returns listOf(initialDoc)
    every { initialDoc.data } returns request.toMap()
    every { initialDoc.exists() } returns true

    every { mockDocument.delete() } returns Tasks.forResult(null)

    // Verification is from cache
    val verifyDoc = mockk<DocumentSnapshot>(relaxed = true)
    val verifyMetadata = mockk<SnapshotMetadata>(relaxed = true)
    every { verifyDoc.exists() } returns false
    every { verifyDoc.metadata } returns verifyMetadata
    every { verifyMetadata.isFromCache } returns true
    every { mockDocument.get(Source.SERVER) } returns Tasks.forResult(verifyDoc)

    try {
      repository.deleteRequest(testRequestId)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(
          e is IllegalStateException || e.message?.contains("Failed to delete request") == true)
      assertTrue(
          e.message?.contains("Cannot verify request deletion") == true ||
              e.cause?.message?.contains("Cannot verify request deletion") == true)
    }
  }

  @Test
  fun deleteRequest_throwsOnNonUnavailableFirestoreException() = runTest {
    val request = createTestRequest()

    val query = mockk<Query>(relaxed = true)
    val snapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<DocumentSnapshot>(relaxed = true)
    val metadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns query
    every { query.get(Source.SERVER) } returns Tasks.forResult(snapshot)
    every { snapshot.metadata } returns metadata
    every { metadata.isFromCache } returns false
    every { snapshot.documents } returns listOf(doc)
    every { doc.data } returns request.toMap()
    every { doc.exists() } returns true

    val exception = mockk<FirebaseFirestoreException>(relaxed = true)
    every { exception.code } returns FirebaseFirestoreException.Code.DATA_LOSS
    every { mockDocument.delete() } returns Tasks.forException(exception)

    try {
      repository.deleteRequest(testRequestId)
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to delete request") == true)
      assertTrue(e.cause is FirebaseFirestoreException)
    }
  }

  @Test
  fun deleteRequest_throwsWhenNotOwner() = runTest {
    val request = createTestRequest().copy(creatorId = "different-user")

    val query = mockk<Query>(relaxed = true)
    val snapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<DocumentSnapshot>(relaxed = true)
    val metadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns query
    every { query.get(Source.SERVER) } returns Tasks.forResult(snapshot)
    every { snapshot.metadata } returns metadata
    every { metadata.isFromCache } returns false
    every { snapshot.documents } returns listOf(doc)
    every { doc.data } returns request.toMap()
    every { doc.exists() } returns true

    try {
      repository.deleteRequest(testRequestId)
      fail("Should throw IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Can only modify") == true)
    }
  }

  // ========== getMyRequests - FIXED ==========

  @Test
  fun getMyRequests_throwsOnNonUnavailableFirestoreException() = runTest {
    val exception = mockk<FirebaseFirestoreException>(relaxed = true)
    every { exception.code } returns FirebaseFirestoreException.Code.INTERNAL

    val query = mockk<Query>(relaxed = true)
    every { mockCollection.whereEqualTo("creatorId", testUserId) } returns query
    every { query.get(Source.SERVER) } returns Tasks.forException(exception)

    try {
      repository.getMyRequests()
      fail("Should throw Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to retrieve requests for user") == true)
      assertTrue(e.cause is FirebaseFirestoreException)
    }
  }

  // ========== closeRequest - FIXED ==========

  @Test
  fun closeRequest_throwsWhenNotOwner() = runTest {
    val request =
        createTestRequest().copy(creatorId = "different-user", status = RequestStatus.IN_PROGRESS)

    val query = mockk<Query>(relaxed = true)
    val snapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<DocumentSnapshot>(relaxed = true)
    val metadata = mockk<SnapshotMetadata>(relaxed = true)

    every { mockCollection.whereEqualTo("requestId", testRequestId) } returns query
    every { query.get(Source.SERVER) } returns Tasks.forResult(snapshot)
    every { snapshot.metadata } returns metadata
    every { metadata.isFromCache } returns false
    every { snapshot.documents } returns listOf(doc)
    every { doc.data } returns request.toMap()
    every { doc.exists() } returns true

    try {
      repository.closeRequest(testRequestId, listOf("helper1"))
      fail("Should throw IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Can only modify") == true)
    }
  }
}
