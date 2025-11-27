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
}
