package com.android.sample.model.map

fun interface LocationProvider {
  suspend fun getCurrentLocation(): Location?
}
