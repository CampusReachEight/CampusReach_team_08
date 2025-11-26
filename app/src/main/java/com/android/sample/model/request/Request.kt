package com.android.sample.model.request

import com.android.sample.model.date.DateSerializer
import com.android.sample.model.map.Location
import com.google.firebase.Timestamp
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
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
    @Serializable(with = DateSerializer::class) val startTimeStamp: Date,
    @Serializable(with = DateSerializer::class) val expirationTime: Date,
    val people: List<String>,
    val tags: List<Tags>,
    val creatorId: String
) {

  val viewStatus: RequestStatus
    get() {
      val now = Date()

      return when {
        // Preserve terminal statuses (manual or automatic)
        status == RequestStatus.CANCELLED ||
            status == RequestStatus.ARCHIVED ||
            status == RequestStatus.COMPLETED -> status

        // COMPLETED when expirationTime <= now
        !expirationTime.after(now) -> RequestStatus.COMPLETED

        // OPEN when start > now
        startTimeStamp.after(now) -> RequestStatus.OPEN
        else -> RequestStatus.IN_PROGRESS
      }
    }

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

      val request =
          Request(
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

      return request.copy(status = request.viewStatus) // Fetching from db now returns "True" status
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

  // Build a robust searchable text derived from toMap, including display variants for enums
  fun toSearchText(): String {
    val map = this.toMap()

    fun String.withDisplayVariantIfNeeded(): String =
        if (this.any { it == '_' }) this + " " + this.replace('_', ' ') else this

    fun Any?.flattenToText(sb: StringBuilder) {
      when (this) {
        null -> Unit
        is String -> {
          val t = this.trim()
          if (t.isNotEmpty()) {
            sb.append(t.withDisplayVariantIfNeeded())
            sb.append('\n')
          }
        }
        is Number,
        is Boolean -> {
          sb.append(this.toString())
          sb.append('\n')
        }
        is Map<*, *> -> this.values.forEach { it.flattenToText(sb) }
        is Iterable<*> -> this.forEach { it.flattenToText(sb) }
        else -> {
          val t = this.toString().trim()
          if (t.isNotEmpty()) {
            sb.append(t.withDisplayVariantIfNeeded())
            sb.append('\n')
          }
        }
      }
    }

    return buildString {
      // Prioritize commonly searched fields first
      listOf(
              "title",
              "description",
              "locationName",
              "requestType",
              "tags",
              "status",
              "people",
              "creatorId",
              "location",
              "requestId",
              "startTimeStamp",
              "expirationTime",
          )
          .forEach { key -> map[key].flattenToText(this) }

      // Include any other fields that may be added in the future
      map.keys
          .filterNot {
            it in
                setOf(
                    "title",
                    "description",
                    "locationName",
                    "requestType",
                    "tags",
                    "status",
                    "people",
                    "creatorId",
                    "location",
                    "requestId",
                    "startTimeStamp",
                    "expirationTime")
          }
          .sorted()
          .forEach { key -> map[key].flattenToText(this) }
    }
  }
}

enum class RequestStatus {
  OPEN,
  IN_PROGRESS,
  ARCHIVED,
  COMPLETED,
  CANCELLED;

  companion object {
    override fun toString(): String {
      return "Status"
    }
  }
}

enum class RequestType {
  STUDYING,
  STUDY_GROUP,
  HANGING_OUT,
  EATING,
  SPORT,
  HARDWARE,
  LOST_AND_FOUND,
  OTHER;

  companion object {
    override fun toString(): String {
      return "Type"
    }
  }
}

enum class Tags {
  URGENT,
  EASY,
  GROUP_WORK,
  SOLO_WORK,
  OUTDOOR,
  INDOOR;

  companion object {
    override fun toString(): String {
      return "Tags"
    }
  }
}

enum class RequestOwnership {
  ALL,
  OWN,
  OTHER,
  ACCEPTED,
  NOT_ACCEPTED,
  NOT_ACCEPTED_BY_ME;

  fun displayString(): String =
      when (this) {
        ALL -> "All Requests"
        OWN -> "My Requests"
        OTHER -> "Other Requests"
        ACCEPTED -> "Accepted by Me"
        NOT_ACCEPTED -> "Nobody Accepted"
        NOT_ACCEPTED_BY_ME -> "Not Accepted by Me"
      }
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
