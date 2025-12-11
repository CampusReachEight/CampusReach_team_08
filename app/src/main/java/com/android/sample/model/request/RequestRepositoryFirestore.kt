package com.android.sample.model.request

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val REQUESTS_COLLECTION_PATH = "requests"

class RequestRepositoryFirestore(private val db: FirebaseFirestore) : RequestRepository {
  // Path structure: "requests/{requestId}"

  private val collectionRef = db.collection(REQUESTS_COLLECTION_PATH)

  private fun notAuthenticated(): Nothing = throw IllegalStateException("No authenticated user")

  private fun notAuthorized(): Nothing =
      throw IllegalArgumentException("Can only modify the currently authenticated user's requests")

  override fun getNewRequestId(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllRequests(): List<Request> {
    return try {
      val snapshot = collectionRef.get(Source.SERVER).await()

      if (snapshot.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot retrieve requests: data from cache (network unavailable)")
      }

      snapshot.documents.mapNotNull { doc -> doc.data?.let { Request.fromMap(it) } }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve requests from server", e)
      }
      throw Exception("Failed to retrieve all requests: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to retrieve all requests", e)
    }
  }

  override suspend fun getAllCurrentRequests(): List<Request> {
    val snapshot = collectionRef.get(Source.SERVER).await()

    if (snapshot.metadata.isFromCache) {
      throw IllegalStateException(
          "Cannot retrieve current requests: data from cache (network unavailable)")
    }

    return snapshot.documents
        .mapNotNull { doc -> doc.data?.let { Request.fromMap(it) } }
        .filter { request ->
          // Exclude requests that are completed (either by status or by expiration) or cancelled
          val vs = request.viewStatus
          val s = request.status
          vs != RequestStatus.COMPLETED &&
              vs != RequestStatus.CANCELLED &&
              s != RequestStatus.COMPLETED
        }
  }

  override suspend fun getRequest(requestId: String): Request {
    return try {
      val snapshot = collectionRef.whereEqualTo("requestId", requestId).get(Source.SERVER).await()

      if (snapshot.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot retrieve request: data from cache (network unavailable)")
      }

      snapshot.documents.firstNotNullOfOrNull { doc -> doc.data?.let { Request.fromMap(it) } }
          ?: throw Exception("Request with ID $requestId not found")
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot retrieve request from server", e)
      }
      throw Exception("Failed to retrieve request with ID $requestId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      if (e.message?.contains("not found") == true) {
        throw e
      }
      throw Exception("Failed to retrieve request with ID $requestId", e)
    }
  }

  override suspend fun addRequest(request: Request) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    if (request.creatorId != currentUserId) {
      notAuthorized()
    }

    try {
      collectionRef.document(request.requestId).set(request.toMap()).await()

      // Verify the request was actually added
      val addedRequest = collectionRef.document(request.requestId).get(Source.SERVER).await()
      if (!addedRequest.exists()) {
        throw Exception("Failed to verify request creation for ID ${request.requestId}")
      }
      if (addedRequest.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot verify request creation: data from cache (network unavailable)")
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot add request to server", e)
      }
      throw Exception("Failed to add request with ID ${request.requestId}: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to add request with ID ${request.requestId}", e)
    }
  }

  override suspend fun updateRequest(requestId: String, updatedRequest: Request) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify the request exists
    val existingRequest = getRequest(requestId)

    if (existingRequest.creatorId != currentUserId) {
      notAuthorized()
    }

    require(requestId == updatedRequest.requestId) { "Request ID cannot be changed" }

    try {
      collectionRef.document(requestId).set(updatedRequest.toMap()).await()

      // Verify the update was successful
      val verifyRequest = collectionRef.document(requestId).get(Source.SERVER).await()
      if (!verifyRequest.exists()) {
        throw Exception("Request disappeared during update for ID $requestId")
      }
      if (verifyRequest.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot verify request update: data from cache (network unavailable)")
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot update request on server", e)
      }
      throw Exception("Failed to update request with ID $requestId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to update request with ID $requestId", e)
    }
  }

  override suspend fun deleteRequest(requestId: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify the request exists before deleting
    val existingRequest = getRequest(requestId)

    if (existingRequest.creatorId != currentUserId) {
      notAuthorized()
    }

    try {
      collectionRef.document(requestId).delete().await()

      // Verify the request was actually deleted
      val deletedRequest = collectionRef.document(requestId).get(Source.SERVER).await()
      if (deletedRequest.exists()) {
        throw Exception("Failed to verify request deletion for ID $requestId")
      }
      if (deletedRequest.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot verify request deletion: data from cache (network unavailable)")
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot delete request from server", e)
      }
      throw Exception("Failed to delete request with ID $requestId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to delete request with ID $requestId", e)
    }
  }

  override fun hasUserAcceptedRequest(request: Request): Boolean {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()
    return request.people.contains(currentUserId)
  }

  override suspend fun acceptRequest(requestId: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    val request = getRequest(requestId)

    check(!hasUserAcceptedRequest(request)) { "You have already accepted this request" }
    check(request.creatorId != currentUserId) { "You cannot accept your own request" }

    try {
      val list = request.people + currentUserId
      collectionRef.document(requestId).update("people", list).await()

      // Verify the acceptance was recorded
      val updatedRequest = getRequest(requestId)
      if (!updatedRequest.people.contains(currentUserId)) {
        throw Exception("Failed to verify request acceptance for ID $requestId")
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot accept request on server", e)
      }
      throw Exception("Failed to accept request with ID $requestId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to accept request with ID $requestId", e)
    }
  }

  override suspend fun cancelAcceptance(requestId: String) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    val request = getRequest(requestId)

    check(hasUserAcceptedRequest(request)) { "You haven't accepted this request" }
    check(request.creatorId != currentUserId) {
      "You cannot revoke acceptance on a request you created"
    }

    try {
      val list = request.people - currentUserId
      collectionRef.document(requestId).update("people", list).await()

      // Verify the cancellation was recorded
      val updatedRequest = getRequest(requestId)
      if (updatedRequest.people.contains(currentUserId)) {
        throw Exception("Failed to verify acceptance cancellation for ID $requestId")
      }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot cancel acceptance on server", e)
      }
      throw Exception("Failed to cancel acceptance for request with ID $requestId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to cancel acceptance for request with ID $requestId", e)
    }
  }

  override suspend fun isOwnerOfRequest(request: Request): Boolean {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()
    return currentUserId == request.creatorId
  }

  override suspend fun getMyRequests(): List<Request> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    return try {
      val snapshot =
          collectionRef.whereEqualTo("creatorId", currentUserId).get(Source.SERVER).await()

      if (snapshot.metadata.isFromCache) {
        throw IllegalStateException(
            "Cannot retrieve user requests: data from cache (network unavailable)")
      }

      snapshot.documents.mapNotNull { doc -> doc.data?.let { Request.fromMap(it) } }
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException(
            "Network unavailable: cannot retrieve user requests from server", e)
      }
      throw Exception("Failed to retrieve requests for user $currentUserId: ${e.message}", e)
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      throw Exception("Failed to retrieve requests for user $currentUserId", e)
    }
  }

  override suspend fun getAcceptedRequests(): List<Request> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    val snapshot =
        collectionRef.whereArrayContains("people", currentUserId).get(Source.SERVER).await()

    if (snapshot.metadata.isFromCache) {
      throw IllegalStateException(
          "Cannot retrieve accepted requests: data from cache (network unavailable)")
    }

    return snapshot.documents
        .mapNotNull { doc -> doc.data?.let { Request.fromMap(it) } }
        .filter { it.creatorId != currentUserId } // Exclude own requests
  }

  override suspend fun closeRequest(requestId: String, selectedHelperIds: List<String>): Boolean {
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()

    // Verify the request exists
    val existingRequest = getRequest(requestId)

    // Check ownership
    if (existingRequest.creatorId != currentUserId) {
      notAuthorized()
    }

    // Check status - can only close OPEN or IN_PROGRESS requests
    if (existingRequest.status != RequestStatus.OPEN &&
        existingRequest.status != RequestStatus.IN_PROGRESS) {
      throw RequestClosureException.InvalidStatus(existingRequest.status)
    }

    // Validate that all selected helpers actually accepted the request
    selectedHelperIds.forEach { helperId ->
      if (helperId !in existingRequest.people) {
        throw RequestClosureException.UserNotHelper(helperId)
      }
    }

    try {
      // Update status to COMPLETED and save selectedHelpers
      val updatedRequest =
          existingRequest.copy(
              status = RequestStatus.COMPLETED,
              selectedHelpers = selectedHelperIds // NEW: Save who received kudos
              )
      collectionRef.document(requestId).set(updatedRequest.toMap()).await()

      // Verify the status was updated
      val verifiedRequest = getRequest(requestId)
      if (verifiedRequest.status != RequestStatus.COMPLETED) {
        throw Exception("Failed to verify request closure status update for ID $requestId")
      }

      // Return true if creator should receive kudos (at least one helper selected)
      return false
    } catch (e: RequestClosureException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
        throw IllegalStateException("Network unavailable: cannot close request on server", e)
      }
      throw RequestClosureException.UpdateFailed(requestId, e)
    } catch (e: Exception) {
      throw RequestClosureException.UpdateFailed(requestId, e)
    }
  }
}
