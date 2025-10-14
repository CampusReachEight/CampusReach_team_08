package com.android.sample.model.map

/** Exception thrown when a location search operation fails. */
class LocationSearchException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

interface LocationRepository {
  suspend fun search(query: String, limit: Int = 10): List<Location>
}
