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
    every { mockLocation.latitude } returns 37.7749
    every { mockLocation.longitude } returns -122.4194
    every { mockLocation.accuracy } returns 50f
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
    assertEquals(37.7749, result?.latitude ?: 0.0, 0.01)
    assertEquals(-122.4194, result?.longitude ?: 0.0, 0.01)
  }

  @Test(timeout = 5000)
  fun testReturnsNullOnException() = runTest {
    every { mockFusedClient.requestLocationUpdates(any(), any<LocationCallback>(), null) } throws
        RuntimeException("GPS Error")
    every { mockFusedClient.lastLocation } throws RuntimeException("GPS Error")

    val result = provider.getCurrentLocation()

    assertNull(result)
  }

  @Test
  fun testLocationNameFormatCurrent() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 40.7128
    every { mockLocation.longitude } returns -74.0060
    every { mockLocation.accuracy } returns 50f
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
    assertTrue(result?.name?.contains("Location") == true)
    assertTrue(result?.name?.contains("40") == true)
  }

  @Test
  fun testHighAccuracyLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 35.6762
    every { mockLocation.longitude } returns 139.6503
    every { mockLocation.accuracy } returns 10f // Very good accuracy
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
    assertEquals(35.6762, result?.latitude ?: 0.0, 0.01)
  }

  @Test
  fun testUsesFirstLocationFromMultiple() = runTest {
    val firstLocation = mockk<AndroidLocation>()
    every { firstLocation.latitude } returns 10.0
    every { firstLocation.longitude } returns 20.0
    every { firstLocation.accuracy } returns 30f
    every { firstLocation.time } returns System.currentTimeMillis()

    val secondLocation = mockk<AndroidLocation>()
    every { secondLocation.latitude } returns 50.0
    every { secondLocation.longitude } returns 60.0

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
    assertEquals(10.0, result?.latitude ?: 0.0, 0.01)
    assertEquals(20.0, result?.longitude ?: 0.0, 0.01)
  }

  @Test
  fun testDifferentCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns -33.8688
    every { mockLocation.longitude } returns 151.2093
    every { mockLocation.accuracy } returns 45f
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
    assertEquals(-33.8688, result?.latitude ?: 0.0, 0.01)
    assertEquals(151.2093, result?.longitude ?: 0.0, 0.01)
  }

  // ===================== EMPTY/NULL LOCATION TESTS =====================

  // ===================== LAST KNOWN LOCATION FALLBACK TESTS =====================

  @Test
  fun testAcceptsLocationAtAccuracyBoundary() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 52.5200
    every { mockLocation.longitude } returns 13.4050
    every { mockLocation.accuracy } returns 100f // Exactly at threshold
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
    assertEquals(52.5200, result?.latitude ?: 0.0, 0.01)
  }

  // ===================== AGE/STALENESS VALIDATION TESTS =====================

  @Test
  fun testAcceptsRecentLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 47.6062
    every { mockLocation.longitude } returns -122.3321
    every { mockLocation.accuracy } returns 30f
    // Location is only 10 seconds old
    every { mockLocation.time } returns System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10)

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
    assertEquals(47.6062, result?.latitude ?: 0.0, 0.01)
  }

  // ===================== EDGE CASE TESTS =====================

  @Test
  fun testLocationNameIncludesCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 45.5017
    every { mockLocation.longitude } returns -122.6750
    every { mockLocation.accuracy } returns 50f
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
    assertTrue(result?.name?.contains("45.5017") == true)
    assertTrue(result?.name?.contains("-122.675") == true)
  }

  @Test
  fun testNegativeCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns -23.5505
    every { mockLocation.longitude } returns -46.6333
    every { mockLocation.accuracy } returns 50f
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
    assertEquals(-23.5505, result?.latitude ?: 0.0, 0.01)
    assertEquals(-46.6333, result?.longitude ?: 0.0, 0.01)
  }

  @Test
  fun testZeroCoordinates() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 0.0
    every { mockLocation.longitude } returns 0.0
    every { mockLocation.accuracy } returns 50f
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
    assertEquals(0.0, result?.latitude ?: -1.0, 0.01)
    assertEquals(0.0, result?.longitude ?: -1.0, 0.01)
  }

  // ===================== MULTIPLE CALLBACKS TESTS =====================

  @Test
  fun testHandlesMultipleLocationCallbacks() = runTest {
    val firstLocation = mockk<AndroidLocation>()
    every { firstLocation.latitude } returns 19.0760
    every { firstLocation.longitude } returns 72.8777
    every { firstLocation.accuracy } returns 40f
    every { firstLocation.time } returns System.currentTimeMillis()

    val secondLocation = mockk<AndroidLocation>()
    every { secondLocation.latitude } returns 19.0761
    every { secondLocation.longitude } returns 72.8778
    every { secondLocation.accuracy } returns 30f
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
    assertEquals(19.0760, result?.latitude ?: 0.0, 0.01)
  }

  // ===================== VERY HIGH PRECISION TESTS =====================

  @Test
  fun testVeryHighPrecisionLocation() = runTest {
    val mockLocation = mockk<AndroidLocation>()
    every { mockLocation.latitude } returns 1.352083
    every { mockLocation.longitude } returns 103.819836
    every { mockLocation.accuracy } returns 5f
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
    assertEquals(1.352083, result?.latitude ?: 0.0, 0.001)
    assertEquals(103.819836, result?.longitude ?: 0.0, 0.001)
  }
}
