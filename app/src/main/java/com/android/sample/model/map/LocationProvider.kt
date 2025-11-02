package com.android.sample.model.map

interface LocationProvider {
  suspend fun getCurrentLocation(): Location?
}
