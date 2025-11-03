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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

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
    private const val TAG = "FusedLocationProvider"

    private const val CURRENT_LOCATION_PREFIX = "Current Location"
    private const val LAST_KNOWN_LOCATION_PREFIX = "Last Known Location"
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
      coroutineScope {
        // New add : Fetch lastLocation in parallel to avoid race condition
        val lastLocationDeferred = async {
          try {
            fusedLocationClient.lastLocation.await()
          } catch (e: Exception) {
            null
          }
        }

        // Request fresh location with built-in timeout
        val freshLocation = requestFreshLocation()

        if (freshLocation != null) {
          Location(
              latitude = freshLocation.latitude,
              longitude = freshLocation.longitude,
              name =
                  "$CURRENT_LOCATION_PREFIX (${freshLocation.latitude}, ${freshLocation.longitude})")
        } else {
          // Use last location fetched in parallel (no race condition)
          val lastLocation = lastLocationDeferred.await()
          if (lastLocation != null && isLocationValid(lastLocation)) {
            Location(
                latitude = lastLocation.latitude,
                longitude = lastLocation.longitude,
                name =
                    "$LAST_KNOWN_LOCATION_PREFIX (${lastLocation.latitude}, ${lastLocation.longitude})")
          } else {
            null
          }
        }
      }
    } catch (e: Exception) {
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
    return withTimeoutOrNull(FRESH_LOCATION_TIMEOUT_MS.milliseconds) {
      suspendCancellableCoroutine { continuation ->
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
                locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

                if (location != null && isLocationValid(location)) {
                  continuation.resume(location)
                } else {
                  continuation.resume(null)
                }
              }
            }

        try {
          fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: Exception) {
          continuation.resume(null)
        }

        continuation.invokeOnCancellation {
          locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        }
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
