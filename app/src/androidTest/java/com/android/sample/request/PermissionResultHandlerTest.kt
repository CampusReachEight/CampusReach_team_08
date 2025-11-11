package com.android.sample.request

import android.Manifest
import android.content.Context
import android.location.LocationManager
import com.android.sample.ui.request.edit.EditRequestViewModel
import com.android.sample.ui.request.edit.PermissionResultHandler
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PermissionResultHandlerTest {
  private lateinit var mockContext: Context
  private lateinit var mockViewModel: EditRequestViewModel
  private lateinit var mockLocationManager: LocationManager
  private lateinit var handler: PermissionResultHandler

  @Before
  fun setup() {
    mockContext = Mockito.mock(Context::class.java)
    mockViewModel = Mockito.mock(EditRequestViewModel::class.java)
    mockLocationManager = Mockito.mock(LocationManager::class.java)

    Mockito.`when`(mockContext.getSystemService(Context.LOCATION_SERVICE))
        .thenReturn(mockLocationManager)
    handler = PermissionResultHandler(mockContext, mockViewModel)
  }

  @Test
  fun permissionGranted_locationEnabled_getsCurrentLocation() {
    val permissions = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)
    Mockito.`when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        .thenReturn(true)

    handler.handlePermissionResult(permissions)

    Mockito.verify(mockViewModel).getCurrentLocation()
    Mockito.verify(mockViewModel, Mockito.never()).setLocationPermissionError()
  }

  @Test
  fun permissionGranted_locationDisabled_setsError() {
    val permissions = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)

    Mockito.`when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        .thenReturn(false)
    Mockito.`when`(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        .thenReturn(false)

    handler.handlePermissionResult(permissions)

    Mockito.verify(mockViewModel).setLocationPermissionError()
    Mockito.verify(mockViewModel, Mockito.never()).getCurrentLocation()
  }

  @Test
  fun permissionDenied_setsError() {
    val permissions =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)

    handler.handlePermissionResult(permissions)

    Mockito.verify(mockViewModel).setLocationPermissionError()
    Mockito.verify(mockViewModel, Mockito.never()).getCurrentLocation()
  }

  @Test
  fun coarsePermissionGranted_locationEnabled_getsCurrentLocation() {
    val permissions = mapOf(Manifest.permission.ACCESS_COARSE_LOCATION to true)
    Mockito.`when`(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        .thenReturn(true)

    handler.handlePermissionResult(permissions)

    Mockito.verify(mockViewModel).getCurrentLocation()
  }
}
