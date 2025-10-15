package com.android.sample.model.request

import com.android.sample.model.map.Location
import com.google.firebase.Timestamp
import java.util.Date
import java.util.Locale

data class Request(
    val requestId: String,
    val title: String,
    val description: String,
    // request type enum has to be defined
    val requestType: List<RequestType>,
    // we need to create a class Location
    val location: Location,
    val locationName: String,
    val status: RequestStatus,
    val startTimeStamp: Date,
    val expirationTime: Date,
    val people: List<String>,
    val tags: List<Tags>,
    val creatorId: String
) {
  companion object {
    fun fromMap(map: Map<String, Any?>): Request {
      // Basic presence validation (will throw if missing)
      fun <T> req(key: String): T {
        try {
          return map[key] as T
        } catch (e: Exception) {
          // Catching ClassCastException, NullPointerException, etc.
          throw IllegalArgumentException("Missing or invalid required field: $key")
        }
      }

      val loc = map["location"]
      val location =
          if (loc is Map<*, *>) {
            Location(
                latitude = loc["latitude"] as Double,
                longitude = loc["longitude"] as Double,
                name = loc["name"] as String)
          } else {
            Location(0.0, 0.0, "")
          }

      return Request(
          requestId = req("requestId"),
          title = req("title"),
          description = req("description"),
          requestType = (req<List<*>>("requestType")).map { RequestType.valueOf(it as String) },
          location = location,
          locationName = req("locationName"),
          status = RequestStatus.valueOf(req("status")),
          startTimeStamp = (req<Timestamp>("startTimeStamp")).toDate(),
          expirationTime = (req<Timestamp>("expirationTime")).toDate(),
          people = (req<List<*>>("people")).map { it as String },
          tags = (req<List<*>>("tags")).map { Tags.valueOf(it as String) },
          creatorId = req("creatorId"))
    }
  }

  fun toMap(): Map<String, Any?> =
      mapOf(
          "requestId" to requestId,
          "title" to title,
          "description" to description,
          "requestType" to requestType.map { it.name },
          "location" to
              mapOf(
                  "latitude" to location.latitude,
                  "longitude" to location.longitude,
                  "name" to location.name),
          "locationName" to locationName,
          "status" to status.name,
          "startTimeStamp" to Timestamp(startTimeStamp),
          "expirationTime" to Timestamp(expirationTime),
          "people" to people,
          "tags" to tags.map { it.name },
          "creatorId" to creatorId)
}

enum class RequestStatus {
  OPEN,
  IN_PROGRESS,
  ARCHIVED,
  COMPLETED,
  CANCELLED
}

enum class RequestType {
  STUDYING,
  STUDY_GROUP,
  HANGING_OUT,
  EATING,
  SPORT,
  HARDWARE,
  LOST_AND_FOUND,
  OTHER
}

enum class Tags {
  URGENT,
  EASY,
  GROUP_WORK,
  SOLO_WORK,
  OUTDOOR,
  INDOOR
}

fun RequestStatus.displayString(): String =
    name.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

fun RequestType.displayString(): String =
    name.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

fun Tags.displayString(): String =
    name.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
