package com.android.sample.model.request

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import java.util.Date
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
      description: String = "Need help with Math",
      creatorId: String = "user1",
      people: List<String> = emptyList(),
      status: RequestStatus = RequestStatus.OPEN
  ): Request {
    val currentTime = System.currentTimeMillis()
    return Request(
        requestId = requestId,
        title = title,
        description = description,
        requestType = listOf(RequestType.STUDYING),
        location = Location(46.5191, 6.5668, "EPFL"),
        locationName = "EPFL",
        status = status,
        startTime = Date(currentTime),
        expirationTime = Date(currentTime + 3600000),
        people = people,
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
  fun addRequest_addsRequestSuccessfully() = runTest {
    val request = createTestRequest()
    repository.addRequest(request)

    val retrieved = repository.getRequest("request1")
    assertEquals(request.requestId, retrieved.requestId)
    assertEquals(request.title, retrieved.title)
    assertEquals(request.creatorId, retrieved.creatorId)
  }

  @Test
  fun getRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.getRequest("nonexistent") }
    }
  }

  @Test
  fun getAllRequests_returnsAllRequests() = runTest {
    val request1 = createTestRequest(requestId = "request1")
    val request2 = createTestRequest(requestId = "request2", title = "Basketball Game")

    repository.addRequest(request1)
    repository.addRequest(request2)

    val allRequests = repository.getAllRequests()
    assertEquals(2, allRequests.size)
  }

  @Test
  fun updateRequest_updatesSuccessfully() = runTest {
    val request = createTestRequest(requestId = "request1", creatorId = "user1")
    repository.addRequest(request)

    val updated = request.copy(title = "Updated Title")
    repository.updateRequest("request1", updated)

    val retrieved = repository.getRequest("request1")
    assertEquals("Updated Title", retrieved.title)
  }

  @Test
  fun updateRequest_nonExistent_throwsException() = runTest {
    val request = createTestRequest(requestId = "request1")

    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.updateRequest("request1", request) }
    }
  }

  @Test
  fun updateRequest_changingId_throwsException() = runTest {
    val request = createTestRequest(requestId = "request1", creatorId = "user1")
    repository.addRequest(request)

    val updated = request.copy(requestId = "different-id")

    assertThrows(IllegalArgumentException::class.java) {
      runTest { repository.updateRequest("request1", updated) }
    }
  }

  @Test
  fun deleteRequest_deletesSuccessfully() = runTest {
    val request = createTestRequest(requestId = "request1", creatorId = "user1")
    repository.addRequest(request)

    repository.deleteRequest("request1")

    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.getRequest("request1") }
    }
  }

  @Test
  fun deleteRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.deleteRequest("nonexistent") }
    }
  }

  @Test
  fun hasUserAcceptedRequest_returnsFalse() {
    val request = createTestRequest(creatorId = "user1", people = emptyList())
    assertFalse(repository.hasUserAcceptedRequest(request))
  }

  @Test
  fun hasUserAcceptedRequest_withPeople_returnsFalse() {
    val request = createTestRequest(creatorId = "user1", people = listOf("user2"))
    assertFalse(repository.hasUserAcceptedRequest(request))
  }

  @Test
  fun acceptRequest_requestExists_doesNotThrow() = runTest {
    val request = createTestRequest(requestId = "request1", creatorId = "user2")
    repository.addRequest(request)

    // Should not throw an exception
    repository.acceptRequest("request1")
  }

  @Test
  fun acceptRequest_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.acceptRequest("nonexistent") }
    }
  }

  @Test
  fun cancelAcceptance_requestExists_doesNotThrow() = runTest {
    val request =
        createTestRequest(requestId = "request1", creatorId = "user2", people = listOf("user1"))
    repository.addRequest(request)

    // Should not throw an exception
    repository.cancelAcceptance("request1")
  }

  @Test
  fun cancelAcceptance_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.cancelAcceptance("nonexistent") }
    }
  }

  @Test
  fun clear_removesAllRequests() = runTest {
    val request1 = createTestRequest(requestId = "request1")
    val request2 = createTestRequest(requestId = "request2")

    repository.addRequest(request1)
    repository.addRequest(request2)

    repository.clear()

    val allRequests = repository.getAllRequests()
    assertEquals(0, allRequests.size)
  }

  // ========== Generation Tests ==========

  @Test
  fun generateRequestData_createsCorrectStructure() {
    val data = RequestRepositoryLocal.generateRequestData(numberOfRequests = 6, numberOfUsers = 3)

    assertEquals(3, data.size) // 3 users
    assertEquals(3, data.keys.toSet().size) // All unique user IDs

    // Each user should have 2 requests (6 total / 3 users)
    data.values.forEach { requestsByUser ->
      assertEquals(2, requestsByUser.size)
      // Each request should have empty acceptance list by default
      requestsByUser.values.forEach { acceptedBy -> assertTrue(acceptedBy.isEmpty()) }
    }
  }

  @Test
  fun generateRequestData_singleUserSingleRequest() {
    val data = RequestRepositoryLocal.generateRequestData(numberOfRequests = 1, numberOfUsers = 1)

    assertEquals(1, data.size)
    val user = data.keys.first()
    assertEquals("user1", user)
    assertEquals(1, data[user]!!.size)
  }

  @Test
  fun generateRequestData_multipleUsers() {
    val data = RequestRepositoryLocal.generateRequestData(numberOfRequests = 10, numberOfUsers = 5)

    assertEquals(5, data.size)
    // Verify user IDs are correct
    assertTrue(data.containsKey("user1"))
    assertTrue(data.containsKey("user2"))
    assertTrue(data.containsKey("user3"))
    assertTrue(data.containsKey("user4"))
    assertTrue(data.containsKey("user5"))
  }

  @Test
  fun generateRequests_createsCorrectNumberOfRequests() {
    val data = RequestRepositoryLocal.generateRequestData(3, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 3, numberOfUsers = 1, requestData = data)

    assertEquals(3, requests.size)
  }

  @Test
  fun generateRequests_requestsHaveCorrectCreators() {
    val data: Map<String, Map<String, List<String>>> =
        mapOf(
            "user1" to mapOf("req1" to emptyList<String>(), "req2" to emptyList<String>()),
            "user2" to mapOf("req3" to emptyList<String>()))

    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 3, numberOfUsers = 2, requestData = data)

    assertEquals(3, requests.size)

    val user1Requests = requests.filter { it.creatorId == "user1" }
    val user2Requests = requests.filter { it.creatorId == "user2" }

    assertEquals(2, user1Requests.size)
    assertEquals(1, user2Requests.size)
  }

  @Test
  fun generateRequests_requestsHaveUniqueIds() {
    val data = RequestRepositoryLocal.generateRequestData(10, 2)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 10, numberOfUsers = 2, requestData = data)

    val requestIds = requests.map { it.requestId }.toSet()
    assertEquals(10, requestIds.size) // All IDs are unique
  }

  @Test
  fun generateRequests_requestsHaveValidProperties() {
    val data = RequestRepositoryLocal.generateRequestData(1, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 1, numberOfUsers = 1, requestData = data)

    val request = requests.first()
    assertNotNull(request.requestId)
    assertNotNull(request.title)
    assertNotNull(request.description)
    assertNotNull(request.location)
    assertNotNull(request.locationName)
    assertTrue(request.title.isNotEmpty())
    assertTrue(request.description.isNotEmpty())
  }

  @Test
  fun generateRequests_withAcceptedUsers() {
    val data: Map<String, Map<String, List<String>>> =
        mapOf("user1" to mapOf("req1" to listOf("user2", "user3")))

    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 1, numberOfUsers = 1, requestData = data)

    val request = requests.first()
    assertEquals(2, request.people.size)
    assertTrue(request.people.contains("user2"))
    assertTrue(request.people.contains("user3"))
  }

  @Test
  fun generateRequests_withCustomLocation() {
    val customLocation = Location(40.7128, -74.0060, "New York")
    val data = RequestRepositoryLocal.generateRequestData(5, 1)

    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 5,
            numberOfUsers = 1,
            requestData = data,
            centeredAround = customLocation,
            radius = 1000.0)

    assertEquals(5, requests.size)
    // All requests should be near the custom location (within radius)
    requests.forEach { request ->
      assertNotNull(request.location)
      assertTrue(request.locationName.contains("New York_nearby"))
    }
  }

  @Test
  fun generateRequests_withCustomTimeRange() {
    val startTime = Date(System.currentTimeMillis())
    val expirationTime = Date(System.currentTimeMillis() + 7200000) // +2 hours
    val data = RequestRepositoryLocal.generateRequestData(2, 1)

    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 2,
            numberOfUsers = 1,
            requestData = data,
            startTime = startTime,
            expirationTime = expirationTime)

    requests.forEach { request ->
      assertEquals(startTime, request.startTime)
      assertEquals(expirationTime, request.expirationTime)
    }
  }

  @Test
  fun repositoryInitialization_withDefaultRequests() = runTest {
    val repoWithDefaults = RequestRepositoryLocal(defaultRequests = 5)

    val allRequests = repoWithDefaults.getAllRequests()
    assertEquals(5, allRequests.size)
  }

  @Test
  fun repositoryInitialization_withCustomRequestData() = runTest {
    val customData =
        mapOf(
            "user1" to mapOf("req1" to listOf("user2"), "req2" to emptyList()),
            "user2" to mapOf("req3" to emptyList()))

    val repoWithCustomData = RequestRepositoryLocal(requestData = customData)

    val allRequests = repoWithCustomData.getAllRequests()
    assertEquals(3, allRequests.size)

    val req1 = allRequests.find { it.requestId == "req1" }
    assertNotNull(req1)
    assertEquals("user1", req1?.creatorId)
    assertTrue(req1?.people?.contains("user2") ?: false)
  }

  @Test
  fun repositoryInitialization_emptyByDefault() = runTest {
    val emptyRepo = RequestRepositoryLocal()

    val allRequests = emptyRepo.getAllRequests()
    assertEquals(0, allRequests.size)
  }

  @Test
  fun repositoryInitialization_defaultRequestsOverridesCustomData() = runTest {
    // When both are provided, defaultRequests should be ignored if requestData is not empty
    val customData = RequestRepositoryLocal.generateRequestData(1, 1)
    val repo = RequestRepositoryLocal(defaultRequests = 10, requestData = customData)

    val allRequests = repo.getAllRequests()
    // Should use customData, not defaultRequests
    assertEquals(1, allRequests.size)
  }

  @Test
  fun generateRequests_locationsAreNearby() {
    val baseLocation = Location(46.5191, 6.5668, "EPFL")
    val data = RequestRepositoryLocal.generateRequestData(10, 1)

    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 10,
            numberOfUsers = 1,
            requestData = data,
            centeredAround = baseLocation,
            radius = 500.0)

    // Check that generated locations are within a reasonable range
    requests.forEach { request ->
      val latDiff = Math.abs(request.location.latitude - baseLocation.latitude)
      val lonDiff = Math.abs(request.location.longitude - baseLocation.longitude)

      // Rough check: should be within ~0.01 degrees (approximately 1km)
      assertTrue(latDiff < 0.01)
      assertTrue(lonDiff < 0.01)
    }
  }

  @Test
  fun generateRequests_hasRandomRequestTypes() {
    val data = RequestRepositoryLocal.generateRequestData(20, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 20, numberOfUsers = 1, requestData = data)

    // With 20 requests, we should have at least some variety in request types
    val requestTypes = requests.flatMap { it.requestType }.toSet()
    assertTrue(requestTypes.isNotEmpty())
    assertTrue(requests.all { it.requestType.isNotEmpty() })
  }

  @Test
  fun generateRequests_hasRandomStatuses() {
    val data = RequestRepositoryLocal.generateRequestData(20, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 20, numberOfUsers = 1, requestData = data)

    // With 20 requests, we should have variety in statuses
    val statuses = requests.map { it.status }.toSet()
    assertTrue(statuses.isNotEmpty())
  }

  @Test
  fun generateRequests_hasRandomTags() {
    val data = RequestRepositoryLocal.generateRequestData(20, 1)
    val requests =
        RequestRepositoryLocal.generateRequests(
            numberOfRequests = 20, numberOfUsers = 1, requestData = data)

    // With 20 requests, we should have variety in tags
    val tags = requests.flatMap { it.tags }.toSet()
    assertTrue(tags.isNotEmpty())
    assertTrue(requests.all { it.tags.isNotEmpty() })
  }
}
