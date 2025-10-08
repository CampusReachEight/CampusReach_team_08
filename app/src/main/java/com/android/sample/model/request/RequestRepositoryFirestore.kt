package com.android.sample.model.request

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val REQUESTS_COLLECTION_PATH = "requests"

class RequestRepositoryFirestore(
    private val db: FirebaseFirestore,
) : RequestRepository {
  // Path structure: "requests/{requestId}"

  private val collectionRef = db.collection(REQUESTS_COLLECTION_PATH)

  private fun notAuthenticated(): Unit = throw IllegalStateException("No authenticated user")

  private fun notAuthorized(): Unit =
      throw IllegalArgumentException("Can only modify the currently authenticated user's requests")

  override fun getNewRequestId(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllRequests(): List<Request> {
    return collectionRef.get().await().documents.mapNotNull { doc ->
      doc.data?.let { Request.fromMap(it) }
    }
  }

  override suspend fun getRequest(requestId: String): Request {
    return collectionRef
        .whereEqualTo("requestId", requestId)
        .get()
        .await()
        .documents
        .firstNotNullOfOrNull { doc -> doc.data?.let { Request.fromMap(it) } }
        ?: throw Exception("Request with ID $requestId not found")
  }

  override suspend fun addRequest(request: Request) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    if (request.creatorId != currentUserId) {
      notAuthorized()
    }

    collectionRef.document(request.requestId).set(request.toMap()).await()
  }

  override suspend fun updateRequest(requestId: String, updatedRequest: Request) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify the request exists
    val existingRequest = getRequest(requestId)

    if (existingRequest.creatorId != currentUserId) {
      notAuthorized()
    }

    if (requestId != updatedRequest.requestId) {
      throw IllegalArgumentException("Request ID cannot be changed")
    }

    collectionRef.document(requestId).set(updatedRequest.toMap()).await()
  }

  override suspend fun deleteRequest(requestId: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify the request exists before deleting
    val existingRequest = getRequest(requestId)

    if (existingRequest.creatorId != currentUserId) {
      notAuthorized()
    }

    collectionRef.document(requestId).delete().await()
  }
}
