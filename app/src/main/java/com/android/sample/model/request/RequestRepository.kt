package com.android.sample.model.request

import com.android.sample.model.map.Location
import com.android.sample.ui.map.MapViewModel
import java.util.Date
import java.util.UUID
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Repository interface for managing requests. */
interface RequestRepository {
  /** Generates and returns a new unique identifier for a request. */
  fun getNewRequestId(): String

  /**
   * Retrieves all requests from the repository.
   *
   * @return A list of all requests.
   */
  suspend fun getAllRequests(): List<Request>

  /**
   * Retrieves a specific request by its unique identifier.
   *
   * @param requestId The unique identifier of the request to retrieve.
   * @return The Request with the specified ID.
   * @throws Exception if the request is not found.
   */
  suspend fun getRequest(requestId: String): Request

  /**
   * Adds a new request to the repository.
   *
   * @param request The Request to add.
   */
  suspend fun addRequest(request: Request)

  /**
   * Updates an existing request in the repository.
   *
   * @param requestId The unique identifier of the request to update.
   * @param updatedRequest The new state of the request.
   * @throws Exception if the request is not found.
   */
  suspend fun updateRequest(requestId: String, updatedRequest: Request)

  /**
   * Deletes a request from the repository.
   *
   * @param requestId The unique identifier of the request to delete.
   * @throws Exception if the request is not found.
   */
  suspend fun deleteRequest(requestId: String)
}

class MockupRequestRepository : RequestRepository {

  private fun generateRequest(num: Int): List<Request> {
    /**
     * Generates a random location within the given radius (in meters) from the specified origin
     * location.
     */
    fun getNearbyLocation(location: Location, radius: Int): Location {
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

    val requests = mutableListOf<Request>()

    for (i in 1..num) {
      requests.add(
          Request(
              requestId = "request_$i",
              title = "Request $i",
              description = "Description for request $i",
              requestType = listOf(),
              location = getNearbyLocation(MapViewModel.EPFL_LOCATION, 500), // 500 meters radius
              locationName = "CM1",
              status = RequestStatus.IN_PROGRESS,
              startTimeStamp = Date(),
              expirationTime = Date(),
              people = listOf(),
              tags = listOf(),
              creatorId = "test_user"))
    }

    return requests
  }

  override fun getNewRequestId(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllRequests(): List<Request> {
    return generateRequest(20)
  }

  override suspend fun getRequest(requestId: String): Request {
    return generateRequest(1).first()
  }

  override suspend fun addRequest(request: Request) {
    // No-op
  }

  override suspend fun updateRequest(requestId: String, updatedRequest: Request) {
    // No-op
  }

  override suspend fun deleteRequest(requestId: String) {
    // No-op
  }
}
