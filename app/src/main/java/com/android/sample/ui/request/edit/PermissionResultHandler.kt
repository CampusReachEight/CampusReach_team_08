package com.android.sample.ui.request.edit

import android.Manifest
import android.content.Context

internal class PermissionResultHandler(
    private val context: Context,
    private val viewModel: EditRequestViewModel
) {
  fun handlePermissionResult(permissions: Map<String, Boolean>) {
    when {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        if (isLocationEnabled(context)) {
          viewModel.getCurrentLocation()
        } else {
          viewModel.setLocationPermissionError()
        }
      }
      else -> {
        viewModel.setLocationPermissionError()
      }
    }
  }
}
