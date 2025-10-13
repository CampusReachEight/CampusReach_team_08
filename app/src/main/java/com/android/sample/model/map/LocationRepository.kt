package com.android.sample.model.map

interface LocationRepository {
  suspend fun search(query: String, limit: Int = 10): List<Location>
}
