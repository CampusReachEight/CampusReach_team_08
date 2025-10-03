package com.android.sample.model.request

import com.android.sample.model.map.Location

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
    val startTimeStamp: Long,
    val expirationTime: Long,
    val people: List<String>,
    val tags: List<Tags>,
    val creatorId: String
)

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
