package com.android.sample.ui.request.edit

import android.Manifest
import android.location.LocationManager

internal class PermissionResultHandler(
    private val viewModel: EditRequestViewModel,
    private val locationManager: LocationManager
) {
  fun handlePermissionResult(permissions: Map<String, Boolean>) {
    when {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        if (isLocationEnabled(locationManager)) { // ← Use injected instance
          viewModel.getCurrentLocation()
        } else {
          viewModel.setLocationPermissionError()
        }
      }
      else -> viewModel.setLocationPermissionError()
    }
  }

  private fun isLocationEnabled(lm: LocationManager): Boolean {
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }
}
