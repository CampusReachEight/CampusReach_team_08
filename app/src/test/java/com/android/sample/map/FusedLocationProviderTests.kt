package com.android.sample.model.map

import android.content.Context
import android.location.Location as AndroidLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FusedLocationProviderTest {

  private lateinit var mockContext: Context
  private lateinit var mockFusedClient: FusedLocationProviderClient
  private lateinit var provider: FusedLocationProvider

  companion object {
    // ===== COORDINATES =====
    // San Francisco
    private const val SF_LATITUDE = 37.7749
    private const val SF_LONGITUDE = -122.4194

    // New York
    private const val NY_LATITUDE = 40.7128
    private const val NY_LONGITUDE = -74.0060

    // Tokyo
    private const val TOKYO_LATITUDE = 35.6762
    private const val TOKYO_LONGITUDE = 139.6503

    // Sydney
    private const val SYDNEY_LATITUDE = -33.8688
    private const val SYDNEY_LONGITUDE = 151.2093

    // Berlin
    private const val BERLIN_LATITUDE = 52.5200
    private const val BERLIN_LONGITUDE = 13.4050

    // Seattle
    private const val SEATTLE_LATITUDE = 47.6062
    private const val SEATTLE_LONGITUDE = -122.3321

    // Portland
    private const val PORTLAND_LATITUDE = 45.5017
    private const val PORTLAND_LONGITUDE = -122.6750

    // São Paulo
    private const val SAO_PAULO_LATITUDE = -23.5505
    private const val SAO_PAULO_LONGITUDE = -46.6333

    // Mumbai
    private const val MUMBAI_LATITUDE = 19.0760
    private const val MUMBAI_LONGITUDE = 72.8777

    // Mumbai alternate
    private const val MUMBAI_LATITUDE_ALT = 19.0761
    private const val MUMBAI_LONGITUDE_ALT = 72.8778

    // Singapore (high precision)
    private const val SINGAPORE_LATITUDE = 1.352083
    private const val SINGAPORE_LONGITUDE = 103.819836

    // Generic test coordinates
    private const val TEST_LATITUDE_1 = 10.0
    private const val TEST_LONGITUDE_1 = 20.0
    private const val TEST_LATITUDE_2 = 50.0
    private const val TEST_LONGITUDE_2 = 60.0

    // Zero/Null Island
    private const val ZERO_COORDINATE = 0.0

    // ===== ACCURACY VALUES =====
    private const val EXCELLENT_ACCURACY = 5f
    private const val VERY_GOOD_ACCURACY = 10f
    private const val GOOD_ACCURACY = 30f
    private const val ACCEPTABLE_ACCURACY = 40f
    private const val MODERATE_ACCURACY = 45f
    private const val BOUNDARY_ACCURACY = 50f
    private const val THRESHOLD_ACCURACY = 100f

    // ===== ASSERTION DELTAS =====
    private const val DELTA_STANDARD = 0.01
    private const val DELTA_HIGH_PRECISION = 0.001

    // ===== TIME VALUES =====
    private const val TEN_SECONDS_MS = 10L

    // ===== FALLBACK VALUES =====
    private const val FALLBACK_COORDINATE = -1.0

    // ===== STRING CONSTANTS =====
    private const val ERROR_MESSAGE_GPS = "GPS Error"
    private const val KEYWORD_LOCATION = "Location"
    private const val COORDINATE_PREFIX = "40"
    private const val PORTLAND_LAT_STRING = "45.5017"
    private const val PORTLAND_LON_STRING = "-122.675"

    // ===== TIMEOUT VALUES =====
    private const val TEST_TIMEOUT_MS = 5000L
  }

  @Before
  fun setup() {
    mockContext = mockk()
    mockFusedClient = mockk()
    provider = FusedLocationProvider(mockContext, mockFusedClient)
  }

  // ===================== FRESH LOCATION TESTS =====================

  @Test
  fun testReturnsLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns SF_LATITUDE
    every { mockLocation.longitude } returns SF_LONGITUDE
    every { mockLocation.accuracy } returns BOUNDARY_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(SF_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
    assertEquals(SF_LONGITUDE, result?.longitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  @Test(timeout = TEST_TIMEOUT_MS)
  fun testReturnsNullOnException() = runTest {
    every { mockFusedClient.requestLocationUpdates(any(), any<LocationCallback>(), null) } throws
        RuntimeException(ERROR_MESSAGE_GPS)
    every { mockFusedClient.lastLocation } throws RuntimeException(ERROR_MESSAGE_GPS)

    val result = provider.getCurrentLocation()

    assertNull(result)
  }

  @Test
  fun testLocationNameFormatCurrent() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns NY_LATITUDE
    every { mockLocation.longitude } returns NY_LONGITUDE
    every { mockLocation.accuracy } returns BOUNDARY_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertTrue(result?.name?.contains(KEYWORD_LOCATION) == true)
    assertTrue(result?.name?.contains(COORDINATE_PREFIX) == true)
  }

  @Test
  fun testHighAccuracyLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns TOKYO_LATITUDE
    every { mockLocation.longitude } returns TOKYO_LONGITUDE
    every { mockLocation.accuracy } returns VERY_GOOD_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(TOKYO_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  @Test
  fun testUsesFirstLocationFromMultiple() = runTest {
    val firstLocation = mockk<AndroidLocation>()
    every { firstLocation.latitude } returns TEST_LATITUDE_1
    every { firstLocation.longitude } returns TEST_LONGITUDE_1
    every { firstLocation.accuracy } returns GOOD_ACCURACY
    every { firstLocation.time } returns System.currentTimeMillis()

    val secondLocation = mockk<AndroidLocation>()
    every { secondLocation.latitude } returns TEST_LATITUDE_2
    every { secondLocation.longitude } returns TEST_LONGITUDE_2

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(firstLocation, secondLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(TEST_LATITUDE_1, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
    assertEquals(TEST_LONGITUDE_1, result?.longitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  @Test
  fun testDifferentCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns SYDNEY_LATITUDE
    every { mockLocation.longitude } returns SYDNEY_LONGITUDE
    every { mockLocation.accuracy } returns MODERATE_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(SYDNEY_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
    assertEquals(SYDNEY_LONGITUDE, result?.longitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  // ===================== EMPTY/NULL LOCATION TESTS =====================

  // ===================== LAST KNOWN LOCATION FALLBACK TESTS =====================

  @Test
  fun testAcceptsLocationAtAccuracyBoundary() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns BERLIN_LATITUDE
    every { mockLocation.longitude } returns BERLIN_LONGITUDE
    every { mockLocation.accuracy } returns THRESHOLD_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(BERLIN_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  // ===================== AGE/STALENESS VALIDATION TESTS =====================

  @Test
  fun testAcceptsRecentLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns SEATTLE_LATITUDE
    every { mockLocation.longitude } returns SEATTLE_LONGITUDE
    every { mockLocation.accuracy } returns GOOD_ACCURACY
    every { mockLocation.time } returns
        System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(TEN_SECONDS_MS)

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(SEATTLE_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  // ===================== EDGE CASE TESTS =====================

  @Test
  fun testLocationNameIncludesCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns PORTLAND_LATITUDE
    every { mockLocation.longitude } returns PORTLAND_LONGITUDE
    every { mockLocation.accuracy } returns BOUNDARY_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertTrue(result?.name?.contains(PORTLAND_LAT_STRING) == true)
    assertTrue(result?.name?.contains(PORTLAND_LON_STRING) == true)
  }

  @Test
  fun testNegativeCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns SAO_PAULO_LATITUDE
    every { mockLocation.longitude } returns SAO_PAULO_LONGITUDE
    every { mockLocation.accuracy } returns BOUNDARY_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(SAO_PAULO_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
    assertEquals(SAO_PAULO_LONGITUDE, result?.longitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  @Test
  fun testZeroCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns ZERO_COORDINATE
    every { mockLocation.longitude } returns ZERO_COORDINATE
    every { mockLocation.accuracy } returns BOUNDARY_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(ZERO_COORDINATE, result?.latitude ?: FALLBACK_COORDINATE, DELTA_STANDARD)
    assertEquals(ZERO_COORDINATE, result?.longitude ?: FALLBACK_COORDINATE, DELTA_STANDARD)
  }

  // ===================== MULTIPLE CALLBACKS TESTS =====================

  @Test
  fun testHandlesMultipleLocationCallbacks() = runTest {
    val firstLocation = mockk<AndroidLocation>()
    every { firstLocation.latitude } returns MUMBAI_LATITUDE
    every { firstLocation.longitude } returns MUMBAI_LONGITUDE
    every { firstLocation.accuracy } returns ACCEPTABLE_ACCURACY
    every { firstLocation.time } returns System.currentTimeMillis()

    val secondLocation = mockk<AndroidLocation>()
    every { secondLocation.latitude } returns MUMBAI_LATITUDE_ALT
    every { secondLocation.longitude } returns MUMBAI_LONGITUDE_ALT
    every { secondLocation.accuracy } returns GOOD_ACCURACY
    every { secondLocation.time } returns System.currentTimeMillis()

    var callbackCount = 0
    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackCount++
          // First callback with valid location
          val firstResult = mockk<LocationResult>()
          every { firstResult.locations } returns listOf(firstLocation)
          callbackSlot.captured.onLocationResult(firstResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    // Should use first location
    assertEquals(MUMBAI_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
  }

  // ===================== VERY HIGH PRECISION TESTS =====================

  @Test
  fun testVeryHighPrecisionLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns SINGAPORE_LATITUDE
    every { mockLocation.longitude } returns SINGAPORE_LONGITUDE
    every { mockLocation.accuracy } returns EXCELLENT_ACCURACY
    every { mockLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(mockLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(SINGAPORE_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_HIGH_PRECISION)
    assertEquals(SINGAPORE_LONGITUDE, result?.longitude ?: ZERO_COORDINATE, DELTA_HIGH_PRECISION)
  }

  @Test
  fun testRejectsPoorAccuracyFreshLocationAndFallsBackToNull() = runTest {
    // Fresh location with accuracy > 100m
    val poorAccuracyLocation = mockk<AndroidLocation>()
    every { poorAccuracyLocation.latitude } returns SF_LATITUDE
    every { poorAccuracyLocation.longitude } returns SF_LONGITUDE
    every { poorAccuracyLocation.accuracy } returns 150f // Poor accuracy
    every { poorAccuracyLocation.time } returns System.currentTimeMillis()

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(poorAccuracyLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    // Last location also has poor accuracy
    every { mockFusedClient.lastLocation } throws RuntimeException("No last location")

    val result = provider.getCurrentLocation()

    assertNull(result)
  }

  @Test
  fun testRejectsStaleLocationAndReturnsNull() = runTest {
    // Location that's over 2 minutes old
    val staleLocation = mockk<AndroidLocation>()
    every { staleLocation.latitude } returns TOKYO_LATITUDE
    every { staleLocation.longitude } returns TOKYO_LONGITUDE
    every { staleLocation.accuracy } returns GOOD_ACCURACY
    every { staleLocation.time } returns
        System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5) // 5 minutes old

    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns listOf(staleLocation)

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()
    every { mockFusedClient.lastLocation } throws RuntimeException("No last location")

    val result = provider.getCurrentLocation()

    assertNull(result)
  }

  @Test
  fun testHandlesEmptyLocationListFromCallback() = runTest {
    val mockLocationResult = mockk<LocationResult>()
    every { mockLocationResult.locations } returns emptyList() // Empty list

    val callbackSlot = slot<LocationCallback>()
    every { mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), null) } answers
        {
          callbackSlot.captured.onLocationResult(mockLocationResult)
          mockk()
        }
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()
    every { mockFusedClient.lastLocation } throws RuntimeException("No last location")

    val result = provider.getCurrentLocation()

    assertNull(result)
  }

  @Test
  fun testFallsBackToLastKnownLocationWhenFreshLocationFails() = runTest {
    every { mockFusedClient.requestLocationUpdates(any(), any<LocationCallback>(), null) } throws
        SecurityException("Location permission denied")

    val lastKnownLocation = mockk<AndroidLocation>()
    every { lastKnownLocation.latitude } returns NY_LATITUDE
    every { lastKnownLocation.longitude } returns NY_LONGITUDE
    every { lastKnownLocation.accuracy } returns GOOD_ACCURACY
    every { lastKnownLocation.time } returns
        System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30)

    val mockTask = mockk<com.google.android.gms.tasks.Task<AndroidLocation?>>(relaxed = true)
    every { mockTask.isComplete } returns true
    every { mockTask.isSuccessful } returns true
    every { mockTask.result } returns lastKnownLocation
    every { mockTask.isCanceled } returns false
    every { mockTask.exception } returns null
    every { mockFusedClient.lastLocation } returns mockTask

    val result = provider.getCurrentLocation()

    assertNotNull(result)
    assertEquals(NY_LATITUDE, result?.latitude ?: ZERO_COORDINATE, DELTA_STANDARD)
    assertEquals(NY_LONGITUDE, result?.longitude ?: ZERO_COORDINATE, DELTA_STANDARD)
    assertTrue(result?.name?.startsWith("Last Known Location") == true)
  }
}
