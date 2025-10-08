package com.android.sample.model.request

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.map.Location
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestTest {

  private fun createTestRequest(
      requestId: String = "test-request-1",
      title: String = "Study Session",
      description: String = "Need help with Math",
      creatorId: String = "user123"
  ): Request {
    return Request(
        requestId = requestId,
        title = title,
        description = description,
        requestType = listOf(RequestType.STUDYING),
        location = Location(46.5191, 6.5668, "EPFL"),
        locationName = "EPFL",
        status = RequestStatus.OPEN,
        startTimeStamp = Date(System.currentTimeMillis()),
        expirationTime = Date(System.currentTimeMillis() + 3600000),
        people = listOf("user1", "user2"),
        tags = listOf(Tags.URGENT, Tags.INDOOR),
        creatorId = creatorId)
  }

  @Test
  fun toMap_and_fromMap_symmetry() {
    val request = createTestRequest()
    val map = request.toMap()

    // Verify map contains correct keys
    assertEquals(request.requestId, map["requestId"])
    assertEquals(request.title, map["title"])
    assertEquals(request.description, map["description"])
    assertEquals(request.locationName, map["locationName"])
    assertEquals(request.status.name, map["status"])
    assertEquals(request.creatorId, map["creatorId"])

    // Verify timestamps are Timestamp objects
    assertTrue(map["startTimeStamp"] is Timestamp)
    assertTrue(map["expirationTime"] is Timestamp)

    // Reconstruct from map
    val reconstructed = Request.fromMap(map)

    // Verify all fields match
    assertEquals(request.requestId, reconstructed.requestId)
    assertEquals(request.title, reconstructed.title)
    assertEquals(request.description, reconstructed.description)
    assertEquals(request.requestType, reconstructed.requestType)
    assertEquals(request.location.latitude, reconstructed.location.latitude, 0.0001)
    assertEquals(request.location.longitude, reconstructed.location.longitude, 0.0001)
    assertEquals(request.location.name, reconstructed.location.name)
    assertEquals(request.locationName, reconstructed.locationName)
    assertEquals(request.status, reconstructed.status)
    assertEquals(request.startTimeStamp.time / 1000, reconstructed.startTimeStamp.time / 1000)
    assertEquals(request.expirationTime.time / 1000, reconstructed.expirationTime.time / 1000)
    assertEquals(request.people, reconstructed.people)
    assertEquals(request.tags, reconstructed.tags)
    assertEquals(request.creatorId, reconstructed.creatorId)
  }

  @Test
  fun toMap_requestTypeList_correctlySerialized() {
    val request =
        createTestRequest().copy(requestType = listOf(RequestType.STUDYING, RequestType.EATING))
    val map = request.toMap()

    @Suppress("UNCHECKED_CAST") val typeList = map["requestType"] as List<String>
    assertEquals(2, typeList.size)
    assertTrue(typeList.contains("STUDYING"))
    assertTrue(typeList.contains("EATING"))
  }

  @Test
  fun fromMap_requestTypeList_correctlyDeserialized() {
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("STUDYING", "SPORT"),
            "location" to mapOf("latitude" to 46.5191, "longitude" to 6.5668, "name" to "EPFL"),
            "locationName" to "EPFL",
            "status" to "OPEN",
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to emptyList<String>(),
            "tags" to listOf("URGENT"),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertEquals(2, request.requestType.size)
    assertTrue(request.requestType.contains(RequestType.STUDYING))
    assertTrue(request.requestType.contains(RequestType.SPORT))
  }

  @Test
  fun toMap_locationObject_correctlySerialized() {
    val location = Location(47.3769, 8.5417, "Zurich")
    val request = createTestRequest().copy(location = location, locationName = "Zurich")
    val map = request.toMap()

    @Suppress("UNCHECKED_CAST") val locationMap = map["location"] as Map<String, Any?>
    assertEquals(47.3769, locationMap["latitude"] as Double, 0.0001)
    assertEquals(8.5417, locationMap["longitude"] as Double, 0.0001)
    assertEquals("Zurich", locationMap["name"])
  }

  @Test
  fun fromMap_locationObject_correctlyDeserialized() {
    val locationMap = mapOf("latitude" to 47.3769, "longitude" to 8.5417, "name" to "Zurich")
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("EATING"),
            "location" to locationMap,
            "locationName" to "Zurich",
            "status" to "OPEN",
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to emptyList<String>(),
            "tags" to listOf("INDOOR"),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertEquals(47.3769, request.location.latitude, 0.0001)
    assertEquals(8.5417, request.location.longitude, 0.0001)
    assertEquals("Zurich", request.location.name)
  }

  @Test
  fun toMap_tagsList_correctlySerialized() {
    val request =
        createTestRequest().copy(tags = listOf(Tags.URGENT, Tags.GROUP_WORK, Tags.OUTDOOR))
    val map = request.toMap()

    @Suppress("UNCHECKED_CAST") val tagsList = map["tags"] as List<String>
    assertEquals(3, tagsList.size)
    assertTrue(tagsList.contains("URGENT"))
    assertTrue(tagsList.contains("GROUP_WORK"))
    assertTrue(tagsList.contains("OUTDOOR"))
  }

  @Test
  fun fromMap_tagsList_correctlyDeserialized() {
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("OTHER"),
            "location" to mapOf("latitude" to 46.5191, "longitude" to 6.5668, "name" to "EPFL"),
            "locationName" to "EPFL",
            "status" to "OPEN",
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to emptyList<String>(),
            "tags" to listOf("EASY", "SOLO_WORK"),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertEquals(2, request.tags.size)
    assertTrue(request.tags.contains(Tags.EASY))
    assertTrue(request.tags.contains(Tags.SOLO_WORK))
  }

  @Test
  fun toMap_peopleList_correctlySerialized() {
    val request = createTestRequest().copy(people = listOf("user1", "user2", "user3"))
    val map = request.toMap()

    @Suppress("UNCHECKED_CAST") val peopleList = map["people"] as List<String>
    assertEquals(3, peopleList.size)
    assertTrue(peopleList.contains("user1"))
    assertTrue(peopleList.contains("user2"))
    assertTrue(peopleList.contains("user3"))
  }

  @Test
  fun fromMap_peopleList_correctlyDeserialized() {
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("HANGING_OUT"),
            "location" to mapOf("latitude" to 46.5191, "longitude" to 6.5668, "name" to "EPFL"),
            "locationName" to "EPFL",
            "status" to "IN_PROGRESS",
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to listOf("alice", "bob", "charlie"),
            "tags" to emptyList<String>(),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertEquals(3, request.people.size)
    assertTrue(request.people.contains("alice"))
    assertTrue(request.people.contains("bob"))
    assertTrue(request.people.contains("charlie"))
  }

  @Test
  fun toMap_timestampsAsFirebaseTimestamp() {
    val now = Date()
    val later = Date(now.time + 7200000) // 2 hours later
    val request = createTestRequest().copy(startTimeStamp = now, expirationTime = later)
    val map = request.toMap()

    val startTimestamp = map["startTimeStamp"] as Timestamp
    val expireTimestamp = map["expirationTime"] as Timestamp

    assertEquals(now.time / 1000, startTimestamp.toDate().time / 1000)
    assertEquals(later.time / 1000, expireTimestamp.toDate().time / 1000)
  }

  @Test
  fun fromMap_timestampsConvertedToDate() {
    val now = Date()
    val later = Date(now.time + 3600000)
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("STUDYING"),
            "location" to mapOf("latitude" to 46.5191, "longitude" to 6.5668, "name" to "EPFL"),
            "locationName" to "EPFL",
            "status" to "OPEN",
            "startTimeStamp" to Timestamp(now),
            "expirationTime" to Timestamp(later),
            "people" to emptyList<String>(),
            "tags" to emptyList<String>(),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertTrue(request.startTimeStamp is Date)
    assertTrue(request.expirationTime is Date)
    assertEquals(now.time / 1000, request.startTimeStamp.time / 1000)
    assertEquals(later.time / 1000, request.expirationTime.time / 1000)
  }

  @Test
  fun requestStatus_allValuesSerializeCorrectly() {
    RequestStatus.values().forEach { status ->
      val request = createTestRequest().copy(status = status)
      val map = request.toMap()
      assertEquals(status.name, map["status"])

      val reconstructed = Request.fromMap(map)
      assertEquals(status, reconstructed.status)
    }
  }

  @Test
  fun requestType_allValuesSerializeCorrectly() {
    RequestType.values().forEach { type ->
      val request = createTestRequest().copy(requestType = listOf(type))
      val map = request.toMap()

      @Suppress("UNCHECKED_CAST") val typeList = map["requestType"] as List<String>
      assertTrue(typeList.contains(type.name))

      val reconstructed = Request.fromMap(map)
      assertTrue(reconstructed.requestType.contains(type))
    }
  }

  @Test
  fun tags_allValuesSerializeCorrectly() {
    Tags.values().forEach { tag ->
      val request = createTestRequest().copy(tags = listOf(tag))
      val map = request.toMap()

      @Suppress("UNCHECKED_CAST") val tagsList = map["tags"] as List<String>
      assertTrue(tagsList.contains(tag.name))

      val reconstructed = Request.fromMap(map)
      assertTrue(reconstructed.tags.contains(tag))
    }
  }

  @Test
  fun fromMap_emptyLists_handledCorrectly() {
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to emptyList<String>(),
            "location" to mapOf("latitude" to 46.5191, "longitude" to 6.5668, "name" to "EPFL"),
            "locationName" to "EPFL",
            "status" to "OPEN",
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to emptyList<String>(),
            "tags" to emptyList<String>(),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertTrue(request.requestType.isEmpty())
    assertTrue(request.people.isEmpty())
    assertTrue(request.tags.isEmpty())
  }

  @Test
  fun fromMap_nullLocation_createsDefaultLocation() {
    val map =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("OTHER"),
            "location" to null,
            "locationName" to "Unknown",
            "status" to "OPEN",
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to emptyList<String>(),
            "tags" to emptyList<String>(),
            "creatorId" to "user123")

    val request = Request.fromMap(map)
    assertEquals(0.0, request.location.latitude, 0.0001)
    assertEquals(0.0, request.location.longitude, 0.0001)
    assertEquals("", request.location.name)
  }

  @Test(expected = NullPointerException::class)
  fun fromMap_missingRequiredKey_throwsException() {
    // Remove a required key (e.g. status)
    val badMap =
        mapOf(
            "requestId" to "req1",
            "title" to "Test",
            "description" to "Desc",
            "requestType" to listOf("OTHER"),
            "location" to mapOf("latitude" to 0.0, "longitude" to 0.0, "name" to "Nowhere"),
            "locationName" to "Nowhere",
            // "status" missing
            "startTimeStamp" to Timestamp(Date()),
            "expirationTime" to Timestamp(Date(System.currentTimeMillis() + 3600000)),
            "people" to emptyList<String>(),
            "tags" to emptyList<String>(),
            "creatorId" to "user123")
    // This should throw
    Request.fromMap(badMap)
  }

  @Test
  fun toMap_allFieldsPresent() {
    val request = createTestRequest()
    val map = request.toMap()

    // Verify all required fields are present
    assertTrue(map.containsKey("requestId"))
    assertTrue(map.containsKey("title"))
    assertTrue(map.containsKey("description"))
    assertTrue(map.containsKey("requestType"))
    assertTrue(map.containsKey("location"))
    assertTrue(map.containsKey("locationName"))
    assertTrue(map.containsKey("status"))
    assertTrue(map.containsKey("startTimeStamp"))
    assertTrue(map.containsKey("expirationTime"))
    assertTrue(map.containsKey("people"))
    assertTrue(map.containsKey("tags"))
    assertTrue(map.containsKey("creatorId"))
  }

  @Test
  fun request_copyWorks() {
    val original = createTestRequest()
    val copy = original.copy(title = "New Title", status = RequestStatus.COMPLETED)

    assertEquals("New Title", copy.title)
    assertEquals(RequestStatus.COMPLETED, copy.status)
    assertEquals(original.requestId, copy.requestId)
    assertEquals(original.description, copy.description)
    assertEquals(original.creatorId, copy.creatorId)
  }

  @Test
  fun request_equalityWorks() {
    val request1 = createTestRequest(requestId = "same-id")
    val request2 = createTestRequest(requestId = "same-id")
    val request3 = createTestRequest(requestId = "different-id")

    assertEquals(request1, request2)
    assertNotEquals(request1, request3)
  }
}
