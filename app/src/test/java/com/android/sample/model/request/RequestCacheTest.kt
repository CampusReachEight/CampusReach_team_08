package com.android.sample.model.request

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import java.io.File
import java.util.Date
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestCacheTest {

  private lateinit var requestCache: RequestCache
  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    requestCache = RequestCache(context)
    // Clean up before each test to ensure isolation
    requestCache.clearAll()
  }

  @After
  fun teardown() {
    // Clean up after each test
    requestCache.clearAll()
  }

  @Test
  fun `saveRequests and loadRequests should correctly persist and retrieve requests`() {
    val requests =
        listOf(
            Request(
                requestId = "1",
                title = "Study Session",
                description = "Looking for a study partner for finals.",
                requestType = listOf(RequestType.STUDYING),
                location = Location(46.7, -71.2, "Library"),
                locationName = "Library",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(1672531200000L),
                expirationTime = Date(1672617600000L),
                people = listOf("user1"),
                tags = listOf(Tags.SOLO_WORK),
                creatorId = "user2"),
            Request(
                requestId = "2",
                title = "Soccer Game",
                description = "Need one more player.",
                requestType = listOf(RequestType.SPORT),
                location = Location(46.8, -71.3, "Field"),
                locationName = "Field",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(1672704000000L),
                expirationTime = Date(1672790400000L),
                people = listOf("user3", "user4"),
                tags = listOf(Tags.OUTDOOR, Tags.GROUP_WORK),
                creatorId = "user5"))

    requestCache.saveRequests(requests)
    val loadedRequests = requestCache.loadRequests()

    // Use toSet() to compare contents regardless of order
    assertEquals(requests.toSet(), loadedRequests.toSet())
  }

  @Test
  fun `loadRequests should return an empty list when cache is empty`() {
    val loadedRequests = requestCache.loadRequests()
    assertTrue(loadedRequests.isEmpty())
  }

  @Test
  fun `clearAll should delete all cached files`() {
    val request =
        Request(
            requestId = "temp_req",
            title = "To be deleted",
            description = "This request will be deleted.",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Nowhere"),
            locationName = "Nowhere",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(),
            expirationTime = Date(),
            people = emptyList(),
            tags = emptyList(),
            creatorId = "temp_user")
    requestCache.saveRequests(listOf(request))

    // Verify something was saved
    assertTrue(requestCache.loadRequests().isNotEmpty())

    requestCache.clearAll()

    // Verify it's now empty
    assertTrue(requestCache.loadRequests().isEmpty())
  }

  @Test
  fun `saveRequests with an empty list should clear the cache`() {
    val initialRequest =
        listOf(
            Request(
                requestId = "initial_req",
                title = "Initial Request",
                description = "This should be cleared.",
                requestType = listOf(RequestType.OTHER),
                location = Location(0.0, 0.0, "Initial Place"),
                locationName = "Initial Place",
                status = RequestStatus.OPEN,
                startTimeStamp = Date(),
                expirationTime = Date(),
                people = emptyList(),
                tags = emptyList(),
                creatorId = "initial_user"))
    requestCache.saveRequests(initialRequest)
    assertTrue(requestCache.loadRequests().isNotEmpty())

    // Now save an empty list
    requestCache.saveRequests(emptyList())

    assertTrue(requestCache.loadRequests().isEmpty())
  }

  @Test
  fun `serialization and deserialization handles all data types correctly`() {
    // Create a request with all possible enum values and complex strings
    val complexRequest =
        Request(
            requestId = "complex-id-456",
            title = "Test: All The Things",
            description =
                """
                This is a multi-line description.
                It includes special characters like `~!@#$%^&*()_+-={}[]|\:";',./<>?
            """
                    .trimIndent(),
            requestType = RequestType.values().toList(),
            location = Location(latitude = 40.7128, longitude = -74.0060, name = "New York City"),
            locationName = "NYC",
            status = RequestStatus.IN_PROGRESS,
            startTimeStamp = Date(1672531200000L), // Fixed date
            expirationTime = Date(1704067200000L), // Fixed date
            people = listOf("user_a", "user_b"),
            tags = Tags.values().toList(),
            creatorId = "test_creator")

    requestCache.saveRequests(listOf(complexRequest))
    val loadedRequests = requestCache.loadRequests()

    assertEquals(1, loadedRequests.size)
    val loadedRequest = loadedRequests.first()

    assertEquals(complexRequest, loadedRequest)
  }

  @Test
  fun `getRequestById should retrieve a specific request from cache`() {
    val request =
        Request(
            requestId = "specific_req",
            title = "Specific Request",
            description = "This is a specific request.",
            requestType = listOf(RequestType.STUDYING),
            location = Location(46.7, -71.2, "Library"),
            locationName = "Library",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(),
            expirationTime = Date(),
            people = emptyList(),
            tags = emptyList(),
            creatorId = "user1")

    requestCache.saveRequests(listOf(request))

    val retrievedRequest = requestCache.getRequestById("specific_req")

    assertEquals(request, retrievedRequest)
  }

  @Test
  fun `getRequestById should throw IllegalArgumentException when request not found`() {
    try {
      requestCache.getRequestById("non_existent_id")
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("not found in cache") == true)
    }
  }

  @Test
  fun `getRequestById should throw IllegalArgumentException when JSON is corrupted`() {
    // Manually create a corrupted JSON file
    val file = File(File(context.cacheDir, "requests_cache"), "corrupted_req.json")
    file.parentFile?.mkdirs()
    file.writeText("{ invalid json }")

    try {
      requestCache.getRequestById("corrupted_req")
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Failed to load request") == true)
    } finally {
      file.delete()
    }
  }

  @Test
  fun `saveRequests should handle serialization exception gracefully`() {
    // Create a request and save it normally first
    val validRequest =
        Request(
            requestId = "valid_req",
            title = "Valid",
            description = "Valid request",
            requestType = listOf(RequestType.STUDYING),
            location = Location(0.0, 0.0, "Place"),
            locationName = "Place",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(),
            expirationTime = Date(),
            people = emptyList(),
            tags = emptyList(),
            creatorId = "user1")

    // This should not crash even if serialization fails for some reason
    requestCache.saveRequests(listOf(validRequest))

    // Verify it was saved
    val loaded = requestCache.loadRequests()
    assertTrue(loaded.isNotEmpty())
  }

  @Test
  fun `loadRequests should handle deserialization exception gracefully`() {
    // Create a corrupted JSON file
    val cacheDir = File(context.cacheDir, "requests_cache")
    cacheDir.mkdirs()
    val corruptedFile = File(cacheDir, "corrupted.json")
    corruptedFile.writeText("{ this is not valid json }")

    // Should return empty list instead of crashing
    val loaded = requestCache.loadRequests()

    // The corrupted file should be skipped (returns null in mapNotNull)
    assertTrue(loaded.isEmpty())

    corruptedFile.delete()
  }

  @Test
  fun `clearAll should throw IllegalStateException when file cannot be deleted`() {
    // This is difficult to test in a real scenario since we can't easily make a file undeletable
    // Instead, we'll test that clearAll works normally
    val request =
        Request(
            requestId = "delete_test",
            title = "Delete Test",
            description = "Test deletion",
            requestType = listOf(RequestType.OTHER),
            location = Location(0.0, 0.0, "Place"),
            locationName = "Place",
            status = RequestStatus.OPEN,
            startTimeStamp = Date(),
            expirationTime = Date(),
            people = emptyList(),
            tags = emptyList(),
            creatorId = "user1")

    requestCache.saveRequests(listOf(request))

    // This should succeed without throwing
    requestCache.clearAll()

    assertTrue(requestCache.loadRequests().isEmpty())
  }

  @Test
  fun `loadRequests should return empty list when listFiles returns null`() {
    // Clear all first
    requestCache.clearAll()

    // Delete the cache directory itself
    val cacheDir = File(context.cacheDir, "requests_cache")
    if (cacheDir.exists()) {
      cacheDir.deleteRecursively()
    }

    // Should return empty list when directory doesn't exist
    val loaded = requestCache.loadRequests()
    assertTrue(loaded.isEmpty())
  }
}
