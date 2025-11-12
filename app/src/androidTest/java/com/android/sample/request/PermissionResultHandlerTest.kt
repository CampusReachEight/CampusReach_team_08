package com.android.sample.request

import android.Manifest
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.android.sample.ui.request.edit.EditRequestViewModel
import com.android.sample.ui.request.edit.PermissionResultHandler
import io.mockk.*
import org.junit.Before
import org.junit.Test

class PermissionResultHandlerTest {
  private lateinit var mockViewModel: EditRequestViewModel
  private lateinit var mockLocationManager: LocationManager
  private lateinit var handler: PermissionResultHandler

  @Before
  fun setup() {
    mockViewModel = mockk(relaxed = true)
    mockLocationManager = mockk(relaxed = true)

    // Use real Context from test environment instead of mocking it
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    handler = PermissionResultHandler(mockViewModel, mockLocationManager)
  }

  @Test
  fun permissionGranted_locationEnabled_getsCurrentLocation() {
    val permissions = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)
    every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true

    handler.handlePermissionResult(permissions)

    verify { mockViewModel.getCurrentLocation() }
    verify(exactly = 0) { mockViewModel.setLocationPermissionError() }
  }

  @Test
  fun permissionGranted_locationDisabled_setsError() {
    val permissions = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)

    every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
    every { mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false

    handler.handlePermissionResult(permissions)

    verify { mockViewModel.setLocationPermissionError() }
    verify(exactly = 0) { mockViewModel.getCurrentLocation() }
  }

  @Test
  fun permissionDenied_setsError() {
    val permissions =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)

    handler.handlePermissionResult(permissions)

    verify { mockViewModel.setLocationPermissionError() }
    verify(exactly = 0) { mockViewModel.getCurrentLocation() }
  }

  @Test
  fun coarsePermissionGranted_locationEnabled_getsCurrentLocation() {
    val permissions = mapOf(Manifest.permission.ACCESS_COARSE_LOCATION to true)
    every { mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true

    handler.handlePermissionResult(permissions)

    verify { mockViewModel.getCurrentLocation() }
  }
}
