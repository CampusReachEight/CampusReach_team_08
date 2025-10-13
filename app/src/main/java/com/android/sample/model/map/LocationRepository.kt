package com.android.sample.model.map

interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
