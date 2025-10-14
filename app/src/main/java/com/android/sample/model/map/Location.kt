package com.android.sample.model.map

/** A data class representing a geographical location with latitude, longitude, and a name. */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String,
)

val EMPTY_LOCATION = Location(
    latitude = 0.0,
    longitude = 0.0,
    name = ""
)

/**
 * Extension function to check if a location is empty/null.
 */
fun Location.isEmpty(): Boolean {
    return this.latitude == 0.0 && this.longitude == 0.0
}

/**
 * Extension function to check if a location is valid.
 */
fun Location.isValid(): Boolean {
    return !isEmpty()
}