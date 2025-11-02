package com.android.sample.model.map

import android.content.Context
import android.location.Location as AndroidLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

  // Test 1: Returns location when valid
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

  // Test 2: Exception handling
  @Test(timeout = 5000)
  fun testReturnsNullOnException() = runTest {
    every { mockFusedClient.requestLocationUpdates(any(), any<LocationCallback>(), null) } throws
        RuntimeException("GPS Error")
    every { mockFusedClient.lastLocation } throws RuntimeException("GPS Error")

    val result = provider.getCurrentLocation()

    assertNull(result)
  }

  // Test 3: Empty location list

  // Test 4: Location name format - Current Location
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

  // Test 5: High accuracy location
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

  // Test 6: First location used from multiple
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

  // Test 7: Different coordinates (Sydney)
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
}
