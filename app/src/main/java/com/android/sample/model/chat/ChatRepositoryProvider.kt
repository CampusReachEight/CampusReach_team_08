package com.android.sample.model.chat

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Provides a single instance of the chat repository in the app. `repository` is mutable for testing
 * purposes.
 */
object ChatRepositoryProvider {
  private var _repository: ChatRepository? = null

  val repository: ChatRepository
    get() {
      return _repository ?: ChatRepositoryFirestore(Firebase.firestore)
    }

  // For testing purposes
  fun setTestRepository(repository: ChatRepository) {
    _repository = repository
  }

  // For testing purposes
  fun reset() {
    _repository = null
  }
}
