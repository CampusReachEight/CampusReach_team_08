package com.android.sample.model.request

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object RequestRepositoryProvider {
  private val _repository: RequestRepositoryFirestore by lazy {
    RequestRepositoryFirestore(Firebase.firestore)
  }

  // You will need to replace with `_repository` when you implement B2.
  var repository: RequestRepository = _repository
}
