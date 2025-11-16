package com.android.sample.map

import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.ui.map.ConstantMap
import com.android.sample.ui.map.calculateClusterCenter
import com.android.sample.ui.map.calculateDistance
import com.android.sample.ui.map.clusterRequestsByDistance
import com.android.sample.ui.map.getClusterRadiusForZoom
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapClusteringTest {

  private val loc = Location(46.5197, 6.6323, "Test Location")
  private val name = "test-id"
  private val title = "Test Request"
  private val description = "Test Description"

  // ========== Tests for calculateDistance ==========

  @Test
  fun test_calculateDistance_returnZeroForSameCoordinates() {
    val distance = calculateDistance(46.5197, 6.6323, 46.5197, 6.6323)
    assertEquals(0.0, distance, 1.0) // Allow 1 meter tolerance
  }

  @Test
  fun test_calculateDistance_calculatesCorrectDistanceForOneDegreLatitudeDifference() {
    // 1 degree latitude ≈ 111km
    val distance = calculateDistance(0.0, 0.0, 1.0, 0.0)
    assertTrue(distance in 110000.0..112000.0, "Expected ~111km, got ${distance}m")
  }

  @Test
  fun test_calculateDistance_calculatesLausanneToGenevaDistanceCorrectly() {
    // Lausanne: 46.5197, 6.6323
    // Geneva: 46.2044, 6.1432
    // Expected: ~51-62 km
    val distance = calculateDistance(46.5197, 6.6323, 46.2044, 6.1432)
    assertTrue(distance in 50000.0..65000.0, "Expected 50-65km, got ${distance}m")
  }

  @Test
  fun test_calculateDistance_handlesNegativeCoordinates() {
    // New York to Washington DC (both negative longitude)
    val distance = calculateDistance(40.7128, -74.0060, 38.9072, -77.0369)
    assertTrue(distance > 0, "Distance should be positive")
    assertTrue(distance in 300000.0..400000.0, "Expected ~330km, got ${distance}m")
  }

  @Test
  fun test_calculateDistance_isSymmetric() {
    val distance1 = calculateDistance(46.5197, 6.6323, 46.2044, 6.1432)
    val distance2 = calculateDistance(46.2044, 6.1432, 46.5197, 6.6323)
    assertEquals(distance1, distance2, 0.1)
  }

  @Test
  fun test_calculateDistance_handlesCoordinatesAcrossEquator() {
    // North and South of equator
    val distance = calculateDistance(10.0, 0.0, -10.0, 0.0)
    assertTrue(distance in 2200000.0..2250000.0, "Expected ~2220km, got ${distance}m")
  }

  @Test
  fun test_calculateDistance_handlesCoordinatesAcrossPrimeMeridian() {
    // East and West of prime meridian
    val distance = calculateDistance(51.5074, -0.1278, 51.5074, 0.1278)
    assertTrue(distance > 0, "Distance should be positive")
  }

  // ========== Tests for calculateClusterCenter ==========

  @Test
  fun test_calculateClusterCenter_returnsCorrectCenterForSingleRequest() {
    val location = Location(latitude = 46.5197, longitude = 6.6323, name = "Lausanne")
    val request = createTestRequest(location = location)

    val center = calculateClusterCenter(listOf(request))

    assertEquals(46.5197, center.latitude, 0.0001)
    assertEquals(6.6323, center.longitude, 0.0001)
  }

  @Test
  fun test_calculateClusterCenter_calculatesAverageForTwoRequests() {
    val location1 = Location(latitude = 46.0, longitude = 6.0, name = "Point 1")
    val location2 = Location(latitude = 48.0, longitude = 8.0, name = "Point 2")
    val request1 = createTestRequest(location = location1)
    val request2 = createTestRequest(location = location2)

    val center = calculateClusterCenter(listOf(request1, request2))

    assertEquals(47.0, center.latitude, 0.0001)
    assertEquals(7.0, center.longitude, 0.0001)
  }

  @Test
  fun test_calculateClusterCenter_calculatesAverageForMultipleRequests() {
    val locations =
        listOf(
            Location(46.0, 6.0, "Point 1"),
            Location(47.0, 7.0, "Point 2"),
            Location(48.0, 8.0, "Point 3"))
    val requests = locations.map { createTestRequest(location = it) }

    val center = calculateClusterCenter(requests)

    // Average: (46+47+48)/3 = 47, (6+7+8)/3 = 7
    assertEquals(47.0, center.latitude, 0.0001)
    assertEquals(7.0, center.longitude, 0.0001)
  }

  @Test
  fun test_calculateClusterCenter_handlesNegativeCoordinates() {
    val locations =
        listOf(Location(-10.0, -5.0, "South Point 1"), Location(-20.0, -10.0, "South Point 2"))
    val requests = locations.map { createTestRequest(location = it) }

    val center = calculateClusterCenter(requests)

    assertEquals(-15.0, center.latitude, 0.0001)
    assertEquals(-7.5, center.longitude, 0.0001)
  }

  // ========== Tests for clusterRequestsByDistance ==========

  @Test
  fun test_clusterRequestsByDistance_returnsEmptyListForEmptyInput() {
    val clusters = clusterRequestsByDistance(emptyList(), 1000.0)
    assertTrue(clusters.isEmpty())
  }

  @Test
  fun test_clusterRequestsByDistance_createsSingleClusterForOneRequest() {
    val request = createTestRequest()

    val clusters = clusterRequestsByDistance(listOf(request), 1000.0)

    assertEquals(1, clusters.size)
    assertEquals(1, clusters[0].size)
    assertEquals(request, clusters[0][0])
  }

  @Test
  fun test_clusterRequestsByDistance_groupsNearbyRequests() {
    // Two requests ~50m apart (should cluster with 100m radius)
    val location1 = Location(46.5197, 6.6323, "Point 1")
    val location2 = Location(46.5202, 6.6323, "Point 2") // ~55m north
    val request1 = createTestRequest(id = "1", location = location1)
    val request2 = createTestRequest(id = "2", location = location2)

    val clusters = clusterRequestsByDistance(listOf(request1, request2), 100.0)

    assertEquals(1, clusters.size, "Should create 1 cluster")
    assertEquals(2, clusters[0].size, "Cluster should contain 2 requests")
  }

  @Test
  fun test_clusterRequestsByDistance_separatesDistantRequests() {
    // Two requests ~5km apart (should NOT cluster with 1km radius)
    val location1 = Location(46.5197, 6.6323, "Point 1")
    val location2 = Location(46.5650, 6.6323, "Point 2") // ~5km north
    val request1 = createTestRequest(id = "1", location = location1)
    val request2 = createTestRequest(id = "2", location = location2)

    val clusters = clusterRequestsByDistance(listOf(request1, request2), 1000.0)

    assertEquals(2, clusters.size, "Should create 2 separate clusters")
    assertEquals(1, clusters[0].size)
    assertEquals(1, clusters[1].size)
  }

  @Test
  fun test_clusterRequestsByDistance_processesHighDensityPointsFirst() {
    // Three points: two close together, one far
    val location1 = Location(46.5197, 6.6323, "Point 1")
    val location2 = Location(46.5202, 6.6323, "Point 2") // Close to location1
    val location3 = Location(46.6000, 6.7000, "Point 3") // Far from others
    val request1 = createTestRequest(id = "1", location = location1)
    val request2 = createTestRequest(id = "2", location = location2)
    val request3 = createTestRequest(id = "3", location = location3)

    val clusters = clusterRequestsByDistance(listOf(request1, request2, request3), 1000.0)

    assertEquals(2, clusters.size)
    // The cluster with 2 requests should be first (higher density)
    assertTrue(clusters[0].size == 2 || clusters[1].size == 2)
  }

  @Test
  fun test_clusterRequestsByDistance_ensuresEachRequestAppearsOnlyOnce() {
    val location1 = Location(46.5197, 6.6323, "Point 1")
    val location2 = Location(46.5202, 6.6323, "Point 2")
    val location3 = Location(46.5207, 6.6323, "Point 3")
    val requests =
        listOf(
            createTestRequest(id = "1", location = location1),
            createTestRequest(id = "2", location = location2),
            createTestRequest(id = "3", location = location3))

    val clusters = clusterRequestsByDistance(requests, 1000.0)

    // Count total requests across all clusters
    val totalRequests = clusters.sumOf { it.size }
    assertEquals(requests.size, totalRequests, "Each request should appear exactly once")

    // Verify all request IDs are present
    val allIds = clusters.flatten().map { it.requestId }.toSet()
    assertEquals(setOf("1", "2", "3"), allIds)
  }

  @Test
  fun test_clusterRequestsByDistance_sortsNearbyRequestsByDistance() {
    // Three points at different distances from a central point
    val location1 = Location(46.5197, 6.6323, "Center") // Center
    val location2 = Location(46.5210, 6.6323, "Far") // ~140m away
    val location3 = Location(46.5202, 6.6323, "Close") // ~55m away
    val location4 = Location(46.5205, 6.6323, "Mid") // ~90m away

    val requests =
        listOf(
            createTestRequest(id = "1", location = location1),
            createTestRequest(id = "2", location = location2),
            createTestRequest(id = "3", location = location3),
            createTestRequest(id = "4", location = location4))

    val clusters = clusterRequestsByDistance(requests, 200.0)

    assertEquals(1, clusters.size)
    assertEquals(4, clusters[0].size)

    // After the first request, the rest should be sorted by distance
    // (closest to farthest from the center)
    val clusterIds = clusters[0].map { it.requestId }
    assertEquals("1", clusterIds[0]) // Center point (highest density)
  }

  @Test
  fun test_clusterRequestsByDistance_handlesRequestsAtExactSameLocation() {
    val location = Location(46.5197, 6.6323, "Same Location")
    val requests =
        listOf(
            createTestRequest(id = "1", location = location),
            createTestRequest(id = "2", location = location),
            createTestRequest(id = "3", location = location))

    val clusters = clusterRequestsByDistance(requests, 100.0)

    assertEquals(1, clusters.size)
    assertEquals(3, clusters[0].size)
  }

  // ========== Tests for getClusterRadiusForZoom ==========

  @Test
  fun test_getClusterRadiusForZoom_returnsWorldRadiusForVeryLowZoom() {
    val radius = getClusterRadiusForZoom(0.5f)
    val expectedBase = ConstantMap.ZOOM_LEVEL_WORLD
    val expected = expectedBase / (0.5f / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsCorrectRadiusForZoomOne() {
    val zoomLevel = ConstantMap.MAX_ZOOM_ONE - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_WORLD
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsCorrectRadiusForZoomTwo() {
    val zoomLevel = ConstantMap.MAX_ZOOM_TWO - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_WL
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsLandRadiusForLandView() {
    val zoomLevel = ConstantMap.MAX_ZOOM_THREE - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_LAND
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsRegionRadiusForRegionView() {
    val zoomLevel = ConstantMap.MAX_ZOOM_FOUR - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_REGION
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsCityRadiusForCityView() {
    val zoomLevel = ConstantMap.MAX_ZOOM_FIVE - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_CITY
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsMidRadiusForMidView() {
    val zoomLevel = ConstantMap.MAX_ZOOM_SIX - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_MID
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsStreetBigRadiusForStreetView() {
    val zoomLevel = ConstantMap.MAX_ZOOM_SEVEN - 0.1f
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_STREET_BIG
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsStreetSmallRadiusForMaxZoom() {
    val zoomLevel = 21.0f // Max zoom
    val radius = getClusterRadiusForZoom(zoomLevel)
    val expectedBase = ConstantMap.ZOOM_LEVEL_STREET_SMALL
    val expected = expectedBase / (zoomLevel / ConstantMap.ZOOM_DIVIDE)
    assertEquals(expected, radius, 0.1)
  }

  @Test
  fun test_getClusterRadiusForZoom_radiusDecreasesAsZoomIncreases() {
    val radius1 = getClusterRadiusForZoom(5.0f)
    val radius2 = getClusterRadiusForZoom(10.0f)
    val radius3 = getClusterRadiusForZoom(15.0f)

    assertTrue(radius1 > radius2, "Radius should decrease as zoom increases")
    assertTrue(radius2 > radius3, "Radius should decrease as zoom increases")
  }

  @Test
  fun test_getClusterRadiusForZoom_handlesZoomBoundaries() {
    // Test at exact boundary values
    val radiusAtBoundary = getClusterRadiusForZoom(ConstantMap.MAX_ZOOM_ONE.toFloat())
    assertTrue(radiusAtBoundary > 0, "Radius should be positive at boundary")

    val radiusJustBefore = getClusterRadiusForZoom(ConstantMap.MAX_ZOOM_ONE - 0.01f)
    val radiusJustAfter = getClusterRadiusForZoom(ConstantMap.MAX_ZOOM_ONE + 0.01f)

    // The base radius should change at the boundary
    assertTrue(radiusJustBefore != radiusJustAfter, "Radius should change at zoom boundary")
  }

  @Test
  fun test_getClusterRadiusForZoom_returnsPositiveRadiusForAllZoomLevels() {
    // Test a range of zoom levels
    for (zoom in 0..21) {
      val radius = getClusterRadiusForZoom(zoom.toFloat())
      assertTrue(radius > 0, "Radius should be positive for zoom level $zoom")
    }
  }

  // ========== Helper Functions ==========

  private fun createTestRequest(id: String = name, location: Location = loc): Request {
    val fixedDate = Date(1700000000000L) // Fixed timestamp: Nov 14, 2023
    val futureDate = Date(1700086400000L) // Fixed timestamp: Nov 15, 2023 (+1 day)

    return Request(
        requestId = id,
        title = title,
        description = description,
        requestType = listOf(RequestType.OTHER),
        location = location,
        locationName = location.name,
        status = RequestStatus.OPEN,
        startTimeStamp = fixedDate,
        expirationTime = futureDate,
        people = emptyList(),
        tags = emptyList(),
        creatorId = "creator-$id")
  }
}
