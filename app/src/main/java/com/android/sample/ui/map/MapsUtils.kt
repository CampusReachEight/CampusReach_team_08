package com.android.sample.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.android.sample.model.request.Request
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

fun Date.toDisplayStringWithoutHours(): String {
  return this.let { timestamp ->
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
  }
}

/**
 * Creates a marker icon with a number badge.
 *
 * @param count The number to display on the marker badge
 * @return A BitmapDescriptor that can be used as a marker icon
 */
fun createMarkerWithNumber(count: Int): BitmapDescriptor {
  val bitmap = createMarkerBitmap(count)
  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Creates a bitmap for a marker with a number badge.
 *
 * @param count The number to display on the badge
 * @return A Bitmap containing a blue circle with white border and the count number
 */
fun createMarkerBitmap(count: Int): Bitmap {
  val size = ConstantMap.SIZE_OF_MARKER
  val bitmap = createBitmap(size, size)
  val canvas = Canvas(bitmap)

  // Circle blue
  val circlePaint =
      Paint().apply {
        color = "#4285F4".toColorInt() // Here I use directly color because I create a marker
        isAntiAlias = true
        style = Paint.Style.FILL
      }
  canvas.drawCircle(
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - ConstantMap.FIVE,
      circlePaint)

  // blank border
  val borderPaint =
      Paint().apply {
        color = Color.WHITE // Use directly Color because it's for a marker
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = ConstantMap.THREE_FLOAT
      }
  canvas.drawCircle(
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - ConstantMap.FIVE,
      borderPaint)

  // Number of request in cluster
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
