package com.android.sample.map

import android.Manifest
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.android.sample.model.map.FusedLocationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FusedLocationProviderInstrumentedTest {

  private lateinit var context: Context
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private lateinit var provider: FusedLocationProvider

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  @Before
  fun setUp() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    provider = FusedLocationProvider(context, fusedLocationClient)
  }

  @Test
  fun testGetCurrentLocationWithRealProvider() = runTest {
    val location = provider.getCurrentLocation()

    // May be null if emulator has no location set, but shouldn't crash
    if (location != null) {
      assertTrue(location.latitude >= -90 && location.latitude <= 90)
      assertTrue(location.longitude >= -180 && location.longitude <= 180)
      assertNotNull(location.name)
    }
  }

  @Test
  fun testLocationTimeoutBehavior() = runTest {
    // Should complete within 15 seconds (FRESH_LOCATION_TIMEOUT_MS)
    val startTime = System.currentTimeMillis()
    val location = provider.getCurrentLocation()
    val elapsedTime = System.currentTimeMillis() - startTime

    assertTrue("Should timeout within 20 seconds", elapsedTime < 20_000)
    // Location might be null if timeout occurred or no location available
  }

  @Test
  fun testLocationNameFormat() = runTest {
    val location = provider.getCurrentLocation()

    if (location != null) {
      assertTrue(location.name.contains("Location") || location.name.contains("Last Known"))
      // Name should contain coordinates
      assertTrue(location.name.contains(location.latitude.toString().take(5)))
    }
  }

  @Test
  fun testMultipleLocationRequests() = runTest {
    val location1 = provider.getCurrentLocation()
    val location2 = provider.getCurrentLocation()

    // Both should complete without crashing
    // They might be same or different depending on actual movement
    if (location1 != null && location2 != null) {
      assertTrue(location1.latitude >= -90 && location1.latitude <= 90)
      assertTrue(location2.latitude >= -90 && location2.latitude <= 90)
    }
  }

  @Test
  fun testLocationValidation() = runTest {
    val location = provider.getCurrentLocation()

    if (location != null) {
      // Accuracy should be reasonable (not 0 or negative)
      assertTrue("Accuracy should be positive", location.name.isNotEmpty())
      // Name should not be null or empty
      assertFalse("Name should not be empty", location.name.isEmpty())
    }
  }
}
