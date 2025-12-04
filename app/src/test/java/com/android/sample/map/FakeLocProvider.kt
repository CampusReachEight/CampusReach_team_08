package com.android.sample.map

import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationProvider

class FakeLocProvider : LocationProvider {
  var locationToReturn: Location? = null
  var exceptionToThrow: Exception? = null
  var getCurrentLocationCalled = false

  override suspend fun getCurrentLocation(): Location? {
    getCurrentLocationCalled = true
    exceptionToThrow?.let { throw it }
    return locationToReturn
  }
}
