package com.android.sample.model.request

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import java.util.Date
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestRepositoryLocalTest {

  private lateinit var repository: RequestRepositoryLocal

  private fun createTestRequest(
      requestId: String = "request1",
      title: String = "Study Session",
      creatorId: String = "user1"
  ): Request {
    val currentTime = System.currentTimeMillis()
    return Request(
        requestId = requestId,
        title = title,
        description = "Test description",
        requestType = listOf(RequestType.STUDYING),
        location = Location(46.5191, 6.5668, "EPFL"),
        locationName = "EPFL",
        status = RequestStatus.OPEN,
        startTime = Date(currentTime),
        expirationTime = Date(currentTime + 3600000),
        people = emptyList(),
        tags = listOf(Tags.URGENT),
        creatorId = creatorId)
  }

  @Before
  fun setup() {
    repository = RequestRepositoryLocal()
    repository.clear()
  }

  @Test
  fun getNewRequestId_returnsValidId() {
    val id = repository.getNewRequestId()
    assertNotNull(id)
    assertTrue(id.isNotEmpty())
  }

  @Test
  fun addAndGetRequest_successful() = runTest {
    val request = createTestRequest()
    repository.addRequest(request)

    val retrieved = repository.getRequest("request1")
    assertEquals(request.requestId, retrieved.requestId)
    assertEquals(request.title, retrieved.title)
  }

  @Test
  fun getRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getRequest("nonexistent") }
    }
  }

  @Test
  fun getAllRequests_returnsCorrectCount() = runTest {
    repository.addRequest(createTestRequest(requestId = "req1"))
    repository.addRequest(createTestRequest(requestId = "req2"))

    assertEquals(2, repository.getAllRequests().size)
  }

  @Test
  fun updateRequest_successful() = runTest {
    val request = createTestRequest()
    repository.addRequest(request)

    val updated = request.copy(title = "Updated")
    repository.updateRequest("request1", updated)

    assertEquals("Updated", repository.getRequest("request1").title)
  }

  @Test
  fun updateRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.updateRequest("nonexistent", createTestRequest()) }
    }
  }

  @Test
  fun updateRequest_wrongId_throwsException() = runTest {
    repository.addRequest(createTestRequest(requestId = "req1"))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.updateRequest("req1", createTestRequest(requestId = "different")) }
    }
  }

  @Test
  fun deleteRequest_successful() = runTest {
    repository.addRequest(createTestRequest())
    repository.deleteRequest("request1")

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getRequest("request1") }
    }
  }

  @Test
  fun deleteRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteRequest("nonexistent") }
    }
  }

  @Test
  fun hasUserAcceptedRequest_returnsFalse() {
    assertFalse(repository.hasUserAcceptedRequest(createTestRequest()))
  }

  @Test
  fun acceptRequest_successful() = runTest {
    repository.addRequest(createTestRequest())
    repository.acceptRequest("request1") // Should not throw
  }

  @Test
  fun acceptRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.acceptRequest("nonexistent") }
    }
  }

  @Test
  fun cancelAcceptance_successful() = runTest {
    repository.addRequest(createTestRequest())
    repository.cancelAcceptance("request1") // Should not throw
  }

  @Test
  fun cancelAcceptance_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.cancelAcceptance("nonexistent") }
    }
  }

  @Test
  fun clear_removesAllRequests() = runTest {
    repository.addRequest(createTestRequest(requestId = "req1"))
    repository.addRequest(createTestRequest(requestId = "req2"))
    repository.clear()

    assertEquals(0, repository.getAllRequests().size)
  }

  @Test
  fun generateRequestData_createsCorrectStructure() {
    val data = RequestRepositoryLocal.generateRequestData(6, 3)

    assertEquals(3, data.size)
    data.values.forEach { assertEquals(2, it.size) }
  }

  @Test
  fun generateRequests_createsCorrectCount() {
    val data = RequestRepositoryLocal.generateRequestData(3, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 3, numberOfUsers = 1, requestData = data)

    assertEquals(3, requests.size)
  }

  @Test
  fun generateRequests_withCustomData_correctCreators() {
    val data =
        mapOf(
            "user1" to mapOf("req1" to emptyList<String>()),
            "user2" to mapOf("req2" to emptyList<String>()))
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 2, numberOfUsers = 2, requestData = data)

    assertEquals(2, requests.size)
    assertEquals(1, requests.count { it.creatorId == "user1" })
    assertEquals(1, requests.count { it.creatorId == "user2" })
  }

  @Test
  fun generateRequests_withAcceptedUsers() {
    val data = mapOf("user1" to mapOf("req1" to listOf("user2", "user3")))
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 1, numberOfUsers = 1, requestData = data)

    assertEquals(2, requests.first().people.size)
  }

  @Test
  fun generateRequests_withCustomLocation() {
    val customLocation = Location(40.7128, -74.0060, "NYC")
    val data = RequestRepositoryLocal.generateRequestData(2, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 2,
            numberOfUsers = 1,
            requestData = data,
            centeredAround = customLocation)

    requests.forEach { assertTrue(it.locationName.contains("NYC_nearby")) }
  }

  @Test
  fun generateRequests_withCustomTimeRange() {
    val startTime = Date(System.currentTimeMillis())
    val expirationTime = Date(System.currentTimeMillis() + 7200000)
    val data = RequestRepositoryLocal.generateRequestData(1, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 1,
            numberOfUsers = 1,
            requestData = data,
            startTime = startTime,
            expirationTime = expirationTime)

    assertEquals(startTime, requests.first().startTime)
    assertEquals(expirationTime, requests.first().expirationTime)
  }

  @Test
  fun repositoryInitialization_withDefaultRequests() = runTest {
    val repo = RequestRepositoryLocal(defaultRequests = 3)
    assertEquals(3, repo.getAllRequests().size)
  }

  @Test
  fun repositoryInitialization_withCustomData() = runTest {
    val data = mapOf("user1" to mapOf("req1" to emptyList<String>(), "req2" to emptyList<String>()))
    val repo = RequestRepositoryLocal(requestData = data)

    assertEquals(2, repo.getAllRequests().size)
  }

  @Test
  fun repositoryInitialization_emptyByDefault() = runTest {
    val repo = RequestRepositoryLocal()
    assertEquals(0, repo.getAllRequests().size)
  }

  @Test
  fun repositoryInitialization_customDataOverridesDefault() = runTest {
    val data = RequestRepositoryLocal.generateRequestData(2, 1)
    val repo = RequestRepositoryLocal(defaultRequests = 10, requestData = data)

    assertEquals(2, repo.getAllRequests().size)
  }
}
