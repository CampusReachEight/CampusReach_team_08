package com.android.sample.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.location.LocationManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.ui.map.MapViewModel.Companion.EPFL_LOCATION
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val ZOOM_FEW = 15f
private const val ZOOM_SEVERAL = 13f
private const val ZOOM_MANY = 10f

fun calculateZoomLevel(markerCount: Int): Float {
  return when {
    markerCount < 2 -> ZOOM_FEW
    markerCount < 5 -> ZOOM_SEVERAL
    else -> ZOOM_MANY
  }
}

/**
 * Determines the appropriate clustering radius based on the map zoom level. The radius decreases as
 * the zoom level increases, allowing for more detailed clustering at higher zoom levels. The radius
 * is also dynamically adjusted based on the exact zoom value.
 *
 * @param zoomLevel The current zoom level of the map (typically between 0 and 21)
 * @return The clustering radius in meters, adjusted for the current zoom level
 */
fun getClusterRadiusForZoom(zoomLevel: Float): Double {
  return when {
    zoomLevel < ConstantMap.MAX_ZOOM_ONE -> ConstantMap.ZOOM_LEVEL_WORLD // 300km - mondial view
    zoomLevel < ConstantMap.MAX_ZOOM_TWO -> ConstantMap.ZOOM_LEVEL_WL // 70km
    zoomLevel < ConstantMap.MAX_ZOOM_THREE -> ConstantMap.ZOOM_LEVEL_LAND // 40km - land view
    zoomLevel < ConstantMap.MAX_ZOOM_FOUR -> ConstantMap.ZOOM_LEVEL_REGION // 10km - region view
    zoomLevel < ConstantMap.MAX_ZOOM_FIVE -> ConstantMap.ZOOM_LEVEL_CITY // 3km - city view
    zoomLevel < ConstantMap.MAX_ZOOM_SIX -> ConstantMap.ZOOM_LEVEL_MID // 400m - view mid
    zoomLevel < ConstantMap.MAX_ZOOM_SEVEN -> ConstantMap.ZOOM_LEVEL_STREET_BIG // 50m - view street
    else -> ConstantMap.ZOOM_LEVEL_STREET_SMALL // 20m - vue street
  } / (zoomLevel / ConstantMap.ZOOM_DIVIDE) // change with current zoom
}

/**
 * Determines the appropriate clustering radius based on the map zoom level. The radius decreases as
 * the zoom level increases, allowing for more detailed clustering at higher zoom levels. The radius
 * is also dynamically adjusted based on the exact zoom value.
 *
 * @param zoomLevel The current zoom level of the map (typically between 0 and 21)
 * @return The clustering radius in meters, adjusted for the current zoom level
 */
fun getClusterRadiusForZoomForCurrentLocation(zoomLevel: Float): Double {
  return when {
    zoomLevel < ConstantMap.MAX_ZOOM_ONE ->
        ConstantMap.CURR_ZOOM_LEVEL_WORLD // 100km - mondial view
    zoomLevel < ConstantMap.MAX_ZOOM_TWO -> ConstantMap.CURR_ZOOM_LEVEL_WL // 40km
    zoomLevel < ConstantMap.MAX_ZOOM_THREE -> ConstantMap.CURR_ZOOM_LEVEL_LAND // 20km - land view
    zoomLevel < ConstantMap.MAX_ZOOM_FOUR -> ConstantMap.CURR_ZOOM_LEVEL_REGION // 5km - region view
    zoomLevel < ConstantMap.MAX_ZOOM_FIVE -> ConstantMap.CURR_ZOOM_LEVEL_CITY // 1km
    zoomLevel < ConstantMap.MAX_ZOOM_SIX -> ConstantMap.CURR_ZOOM_LEVEL_MID // 200m
    zoomLevel < ConstantMap.MAX_ZOOM_SEVEN -> ConstantMap.CURR_ZOOM_LEVEL_STREET_BIG // 35m
    else -> ConstantMap.CURR_ZOOM_LEVEL_STREET_SMALL // 15m
  } / (zoomLevel / ConstantMap.CURR_ZOOM_DIVIDE) // change with current zoom
}

fun Date.toDisplayStringWithoutHours(): String {
  return this.let { timestamp ->
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
  }
}

/**
 * Creates a marker icon with a number badge.
 *
 * @param count The number to display on the marker badge
 * @param isAtCurrentLocation is the cluster at same position as user
 * @return A BitmapDescriptor that can be used as a marker icon
 */
fun createMarkerWithNumber(count: Int, isAtCurrentLocation: Boolean = false): BitmapDescriptor {
  val bitmap = createMarkerBitmap(count, isAtCurrentLocation)
  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Creates a bitmap for a marker with a number badge.
 *
 * @param count The number to display on the badge
 * @param isAtCurrentLocation is the user at the same position as cluster
 * @return A Bitmap containing a blue circle with white border and the count number
 */
fun createMarkerBitmap(count: Int, isAtCurrentLocation: Boolean = false): Bitmap {
  val size = ConstantMap.SIZE_OF_MARKER
  val bitmap = createBitmap(size, size)
  val canvas = Canvas(bitmap)

  // Circle color: cyan if at current location, blue otherwise
  val circlePaint =
      Paint().apply {
        color =
            if (isAtCurrentLocation) {
              "#00BCD4".toColorInt() // Cyan color
            } else {
              "#4285F4".toColorInt() // Blue color
            }
        isAntiAlias = true
        style = Paint.Style.FILL
      }
  canvas.drawCircle(
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - ConstantMap.FIVE,
      circlePaint)

  // White border
  val borderPaint =
      Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = ConstantMap.THREE_FLOAT
      }
  canvas.drawCircle(
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - ConstantMap.FIVE,
      borderPaint)

  // Number of requests in cluster
  val textPaint = createTextPaint(count)

  val text = count.toString()
  val textBounds = Rect()
  textPaint.getTextBounds(text, ConstantMap.ZERO, text.length, textBounds)
  canvas.drawText(
      text,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - textBounds.exactCenterY(),
      textPaint)

  return bitmap
}

/**
 * Creates a Paint object for drawing text on marker badges.
 *
 * @param count The number to be displayed, used to determine appropriate text size
 * @return A Paint object configured for drawing white, bold, centered text
 */
fun createTextPaint(count: Int): Paint {
  return Paint().apply {
    color = Color.WHITE
    textSize = getTextSizeForCount(count)
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    isFakeBoldText = true
  }
}

/**
 * Determines the appropriate text size based on the number of digits in the count.
 *
 * @param count The number to be displayed
 * @return The text size in pixels: larger for single digits, smaller for longer numbers
 */
fun getTextSizeForCount(count: Int): Float {
  return when {
    count < ConstantMap.NUMBER_LENGTH_ONE -> ConstantMap.NUMBER_SIZE_ONE
    count < ConstantMap.NUMBER_LENGTH_TWO -> ConstantMap.NUMBER_SIZE_TWO
    else -> ConstantMap.NUMBER_SIZE_THREE
  }
}

/**
 * Calculates the distance between two geographic coordinates using the Haversine formula.
 *
 * @param lat1 Latitude of the first point in degrees
 * @param lon1 Longitude of the first point in degrees
 * @param lat2 Latitude of the second point in degrees
 * @param lon2 Longitude of the second point in degrees
 * @return The distance between the two points in meters
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val earthRadius = ConstantMap.EARTH_RADIUS // Radius of earth in meter

  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)

  val a =
      sin(dLat / ConstantMap.TWO) * sin(dLat / ConstantMap.TWO) +
          cos(Math.toRadians(lat1)) *
              cos(Math.toRadians(lat2)) *
              sin(dLon / ConstantMap.TWO) *
              sin(dLon / ConstantMap.TWO)

  val c = ConstantMap.TWO * atan2(sqrt(a), sqrt(ConstantMap.ONE - a))

  return earthRadius * c
}

/**
 * Finds the closest request to the given current position.
 *
 * @param currentPosition The current position as LatLng
 * @param requests List of requests to search through
 * @return The closest Request, or null if the list is empty
 */
fun findClosestRequest(currentPosition: LatLng?, requests: List<Request>): Location {
  if (currentPosition == null) {
    return requests.firstOrNull()?.location ?: EPFL_LOCATION
  }
  return requests
      .minByOrNull { request ->
        calculateDistance(
            currentPosition.latitude,
            currentPosition.longitude,
            request.location.latitude,
            request.location.longitude)
      }
      ?.location
      ?: Location(currentPosition.latitude, currentPosition.longitude, ConstantMap.CURR_POS_NAME)
}

/**
 * Calculates the geographic center point of a cluster of requests.
 *
 * @param requests The list of requests in the cluster
 * @return A LatLng representing the average position of all requests in the cluster
 */
fun calculateClusterCenter(requests: List<Request>): LatLng {
  val avgLat = requests.map { it.location.latitude }.average()
  val avgLng = requests.map { it.location.longitude }.average()
  return LatLng(avgLat, avgLng)
}

/**
 * Groups requests into clusters based on geographic distance. Uses a density-based algorithm where
 * points with more neighbors are processed first, and each point is only included in one cluster.
 *
 * @param requests The list of requests to cluster
 * @param clusterRadiusMeters The maximum distance in meters for requests to be in the same cluster
 * @return A list of clusters, where each cluster is a list of nearby requests
 */
fun clusterRequestsByDistance(
    requests: List<Request>,
    clusterRadiusMeters: Double
): List<List<Request>> {
  if (requests.isEmpty()) return emptyList()

  val adjustedRadius = clusterRadiusMeters

  // Calculate the density of each point
  val densityMap =
      requests.associateWith { request ->
        requests.count { other ->
          calculateDistance(
              request.location.latitude,
              request.location.longitude,
              other.location.latitude,
              other.location.longitude) <= adjustedRadius
        }
      }

  // Sort by decreasing density (points with the most neighbours first)
  val sortedRequests = requests.sortedByDescending { densityMap[it] }

  val clusters = mutableListOf<MutableList<Request>>()
  val processed = mutableSetOf<Request>()

  sortedRequests.forEach { request ->
    if (request in processed) return@forEach

    val cluster = mutableListOf(request)
    processed.add(request)

    // Find all nearby requests and sort them by distance
    val nearbyRequests =
        requests
            .filter { other -> other !in processed }
            .map { other ->
              val distance =
                  calculateDistance(
                      request.location.latitude,
                      request.location.longitude,
                      other.location.latitude,
                      other.location.longitude)
              other to distance
            }
            .filter { (_, distance) -> distance <= adjustedRadius }
            .sortedBy { (_, distance) -> distance }

    nearbyRequests.forEach { (other, _) ->
      cluster.add(other)
      processed.add(other)
    }

    clusters.add(cluster)
  }

  return clusters
}

/** Data class representing a cluster and whether it's at the user's current location. */
data class ClusterWithLocation(val cluster: List<Request>, val isAtCurrentLocation: Boolean)

/**
 * Checks if current location is near any cluster.
 *
 * @param clusters the list of cluster
 * @param currentLocation the current position
 * @return if a cluster is near currentPosition
 */
fun isCurrentLocationNearAnyCluster(
    clusters: List<List<Request>>,
    currentLocation: LatLng,
    clusterRadiusMeters: Double
): Boolean {
  val proximityThreshold = clusterRadiusMeters

  return clusters.any { cluster ->
    val clusterPosition =
        if (cluster.size == ConstantMap.ONE) {
          LatLng(cluster.first().location.latitude, cluster.first().location.longitude)
        } else {
          calculateClusterCenter(cluster)
        }

    calculateDistance(
        currentLocation.latitude,
        currentLocation.longitude,
        clusterPosition.latitude,
        clusterPosition.longitude) <= proximityThreshold
  }
}

/**
 * Merges current location with nearby clusters (within 50 meters).
 *
 * @param clusters the list of cluster
 * @param currentLocation the current position
 * @return the list of cluster with the location
 */
fun mergeCurrentLocationWithClusters(
    clusters: List<List<Request>>,
    currentLocation: LatLng,
    clusterRadiusMeters: Double
): List<ClusterWithLocation> {
  val proximityThreshold = clusterRadiusMeters

  return clusters.map { cluster ->
    val clusterPosition =
        if (cluster.size == ConstantMap.ONE) {
          LatLng(cluster.first().location.latitude, cluster.first().location.longitude)
        } else {
          calculateClusterCenter(cluster)
        }

    val distance =
        calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            clusterPosition.latitude,
            clusterPosition.longitude)
    val isNear = distance <= proximityThreshold

    ClusterWithLocation(cluster, isNear)
  }
}

internal class MapPermissionResultHandler(
    private val viewModel: MapViewModel,
    private val locationManager: LocationManager
) {
  fun handlePermissionResult(permissions: Map<String, Boolean>) {
    when {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
        if (isLocationEnabled(locationManager)) {
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

  private fun isLocationEnabled(lm: LocationManager): Boolean {
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
  }
}

fun handleLocationPermissionCheck(
    context: Context,
    viewModel: MapViewModel,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
  val hasFineLocation =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED

  val hasCoarseLocation =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED

  if (hasFineLocation || hasCoarseLocation) {
    if (isLocationEnabled(context)) {
      viewModel.getCurrentLocation()
    } else {
      viewModel.setLocationPermissionError()
    }
  } else {
    permissionLauncher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
  }
}

internal fun isLocationEnabled(context: Context): Boolean {
  val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
