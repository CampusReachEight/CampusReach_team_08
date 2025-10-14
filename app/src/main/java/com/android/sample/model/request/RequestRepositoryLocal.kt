package com.android.sample.model.request

import com.android.sample.model.map.Location
import com.android.sample.ui.map.MapViewModel.Companion.EPFL_LOCATION
import java.util.Date
import java.util.UUID
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Local in-memory implementation of RequestRepository for testing and development. All data is
 * stored in memory and will be lost when the app is closed.
 */
class RequestRepositoryLocal(
    val defaultRequests: Int = 0, // "Easy" mode for testing without custom data
    val requestData: Map<String, Map<String, List<String>>> =
        mapOf(), // Custom data for more control
) : RequestRepository {

  private val requests = mutableMapOf<String, Request>()

  companion object {
    private fun getNearbyLocation(
        location: Location,
        radius: Double = 500.0, // in meters
    ): Location {
      // Constants
      val earthRadius = 6371000.0 // meters

      // Convert radius to radians
      val radiusInRadians = radius / earthRadius

      // Generate random bearing (direction) and distance
      val randomBearing = Random.nextDouble(0.0, 2 * Math.PI)
      val randomDistance = radiusInRadians * sqrt(Random.nextDouble(0.0, 1.0))

      // Convert origin latitude/longitude to radians
      val lat1 = Math.toRadians(location.latitude)
      val lon1 = Math.toRadians(location.longitude)

      // Compute new latitude
      val lat2 =
          asin(
              sin(lat1) * cos(randomDistance) +
                  cos(lat1) * sin(randomDistance) * cos(randomBearing))

      // Compute new longitude
      val lon2 =
          lon1 +
              atan2(
                  sin(randomBearing) * sin(randomDistance) * cos(lat1),
                  cos(randomDistance) - sin(lat1) * sin(lat2))

      // Convert back to degrees
      val newLat = Math.toDegrees(lat2)
      val newLon = Math.toDegrees(lon2)

      // Optionally generate a synthetic name or reuse the original
      val newName = "${location.name}_nearby"

      return Location(newLat, newLon, newName)
    }

    fun generateRequestData(
        numberOfRequests: Int,
        numberOfUsers: Int
    ): Map<String, Map<String, List<String>>> {
      return (1..numberOfUsers)
          .map { "user$it" }
          .associateWith {
            (1..(numberOfRequests / numberOfUsers))
                .map { "request${it + (it - 1) * numberOfUsers}" }
                .associateWith { listOf<String>() } // No one has accepted the request by default
          }
    }

    fun generateRequests(
        numberOfRequests: Int = 1,
        numberOfUsers: Int = 1,
        requestData: Map<String, Map<String, List<String>>> =
            generateRequestData(numberOfRequests, numberOfUsers),
        centeredAround: Location? = null,
        radius: Double = 500.0, // in meters
        startTime: Date = Date(System.currentTimeMillis()),
        expirationTime: Date = Date(System.currentTimeMillis() + 3600000), // +1 hour
    ): List<Request> {
      val generatedRequests = mutableListOf<Request>()
      val baseLocation = centeredAround ?: EPFL_LOCATION

      for ((userId, requestsByUser) in requestData) {
        for ((requestId, acceptedBy) in requestsByUser) {
          val location = getNearbyLocation(baseLocation, radius)
          val request =
              Request(
                  requestId = requestId,
                  title = "Request $requestId by $userId",
                  description = "This is a description for $requestId created by $userId.",
                  requestType = listOf(RequestType.entries.toTypedArray().random()),
                  location = location,
                  locationName = location.name,
                  status = RequestStatus.entries.toTypedArray().random(),
                  startTime = startTime,
                  expirationTime = expirationTime,
                  people = acceptedBy,
                  tags = listOf(Tags.entries.toTypedArray().random()),
                  creatorId = userId)
          generatedRequests.add(request)
        }
      }

      return generatedRequests
    }
  }

  init {
    if (defaultRequests > 0 && requestData.isEmpty()) {
      generateRequests(defaultRequests, 1)
    } else if (requestData.isNotEmpty()) {
      for (request in
          generateRequests(
              numberOfRequests = requestData.values.sumOf { it.size },
              numberOfUsers = requestData.size,
              requestData = requestData)) {
        this.requests[request.requestId] = request
      }
    }
  }

  override fun getNewRequestId(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllRequests(): List<Request> {
    return requests.values.toList()
  }

  override suspend fun getRequest(requestId: String): Request {
    return requests[requestId]
        ?: throw NoSuchElementException("Request with ID $requestId not found")
  }

  override suspend fun addRequest(request: Request) {
    requests[request.requestId] = request
  }

  override suspend fun updateRequest(requestId: String, updatedRequest: Request) {
    val existingRequest =
        requests[requestId] ?: throw NoSuchElementException("Request with ID $requestId not found")

    require(requestId == updatedRequest.requestId) { "Request ID cannot be changed" }

    requests[requestId] = updatedRequest
  }

  override suspend fun deleteRequest(requestId: String) {
    if (!requests.containsKey(requestId)) {
      throw NoSuchElementException("Request with ID $requestId not found")
    }
    requests.remove(requestId)
  }

  override fun hasUserAcceptedRequest(request: Request): Boolean {
    // For local repository, always return false or implement simple logic
    // This can be overridden by tests as needed
    return false
  }

  override suspend fun acceptRequest(requestId: String) {
    val request =
        requests[requestId] ?: throw NoSuchElementException("Request with ID $requestId not found")

    // Simply add a placeholder user ID or do nothing
    // Tests can override this behavior
  }

  override suspend fun cancelAcceptance(requestId: String) {
    val request =
        requests[requestId] ?: throw NoSuchElementException("Request with ID $requestId not found")

    // Simply remove placeholder user ID or do nothing
    // Tests can override this behavior
  }

  /** Clears all requests from the repository. Useful for testing. */
  fun clear() {
    requests.clear()
  }
}
