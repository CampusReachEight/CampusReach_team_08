package com.android.sample.model.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location as AndroidLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class FusedLocationProvider(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
) : LocationProvider {

  companion object {
    // Maximum acceptable age of location data (2 minutes)
    private val MAX_LOCATION_AGE_MS = TimeUnit.MINUTES.toMillis(2)

    // Minimum acceptable accuracy in meters (100m threshold)
    private const val MIN_ACCEPTABLE_ACCURACY = 100f

    // Timeout for requesting fresh location (15 seconds)
    private const val FRESH_LOCATION_TIMEOUT_MS = 15000L
  }

  /**
   * Gets the current location of the device.
   *
   * IMPORTANT PERMISSIONS REQUIRED:
   * - android.permission.ACCESS_FINE_LOCATION (recommended for high accuracy)
   * - OR android.permission.ACCESS_COARSE_LOCATION (lower accuracy fallback)
   *
   * Caller MUST verify that one of these permissions is granted before calling this method. This
   * typically involves checking via ContextCompat.checkSelfPermission() at runtime.
   *
   * @return Location object with latitude, longitude, and name, or null if location cannot be
   *   determined
   */
  @SuppressLint("MissingPermission")
  override suspend fun getCurrentLocation(): Location? {
    return try {
      val freshLocation = requestFreshLocation()

      if (freshLocation != null) {
        /**
         * Log.d( "FusedLocationProvider", "Got fresh location: ${freshLocation.latitude},
         * ${freshLocation.longitude}")
         */
        Location(
            latitude = freshLocation.latitude,
            longitude = freshLocation.longitude,
            name = "Current Location (${freshLocation.latitude}, ${freshLocation.longitude})")
      } else {
        /**
         * Log.d("FusedLocationProvider", "Fresh location unavailable, trying last known location")
         */
        val lastLocation = fusedLocationClient.lastLocation.await()

        if (lastLocation != null && isLocationValid(lastLocation)) {
          /**
           * Log.d( "FusedLocationProvider", "Got last known location: ${lastLocation.latitude},
           * ${lastLocation.longitude}")
           */
          Location(
              latitude = lastLocation.latitude,
              longitude = lastLocation.longitude,
              name = "Last Known Location (${lastLocation.latitude}, ${lastLocation.longitude})")
        } else {
          /** Log.d("FusedLocationProvider", "No valid location available") */
          null
        }
      }
    } catch (e: Exception) {
      /** Log.e("FusedLocationProvider", "Error getting current location", e) */
      null
    }
  }

  /**
   * Requests a fresh location update with high accuracy and reasonable timeout. Uses
   * LocationRequest to actively request updates instead of relying on potentially stale cached
   * data.
   *
   * @return Fresh, validated Location or null if unable to obtain within timeout
   */
  @SuppressLint("MissingPermission")
  private suspend fun requestFreshLocation(): AndroidLocation? {
    return suspendCancellableCoroutine { continuation ->
      val locationRequest =
          LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
              .apply {
                setMaxUpdates(1)
                setDurationMillis(FRESH_LOCATION_TIMEOUT_MS)
              }
              .build()

      var locationCallback: LocationCallback? = null

      locationCallback =
          object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
              val location = locationResult.locations.firstOrNull()
              if (location != null && isLocationValid(location)) {
                /**
                 * Log.d( "FusedLocationProvider", "Fresh location received with accuracy:
                 * ${location.accuracy}m")
                 */
                locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
                continuation.resume(location)
              }
            }
          }

      try {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
      } catch (e: Exception) {
        continuation.resume(null)
      }

      // Handle cancellation - clean up location updates
      continuation.invokeOnCancellation {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
      }
    }
  }

  /**
   * Validates location based on accuracy and age criteria. Filters out inaccurate or stale location
   * data to ensure quality.
   *
   * @param location The location to validate
   * @return true if location meets both accuracy and age requirements, false otherwise
   */
  private fun isLocationValid(location: AndroidLocation): Boolean {
    // Check accuracy - ignore locations with poor accuracy
    if (location.accuracy > MIN_ACCEPTABLE_ACCURACY) {
      /**
       * Log.d( "FusedLocationProvider", "Location accuracy too low: ${location.accuracy}m
       * (threshold: ${MIN_ACCEPTABLE_ACCURACY}m)")
       */
      return false
    }

    // Check age - ignore very old locations
    val locationAge = System.currentTimeMillis() - location.time
    if (locationAge > MAX_LOCATION_AGE_MS) {
      /**
       * Log.d( "FusedLocationProvider", "Location too stale: ${locationAge}ms old (threshold:
       * ${MAX_LOCATION_AGE_MS}ms)")
       */
      return false
    }

    /**
     * Log.d( "FusedLocationProvider", "Location validation passed - accuracy:
     * ${location.accuracy}m, age: ${locationAge}ms")
     */
    return true
  }
}
