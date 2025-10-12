package com.android.sample.model.request

/** Repository interface for managing requests. */
interface RequestRepository {
  /** Generates and returns a new unique identifier for a request. */
  fun getNewRequestId(): String

  /**
   * Retrieves all requests from the repository.
   *
   * @return A list of all requests.
   */
  suspend fun getAllRequests(): List<Request>

  /**
   * Retrieves a specific request by its unique identifier.
   *
   * @param requestId The unique identifier of the request to retrieve.
   * @return The Request with the specified ID.
   * @throws Exception if the request is not found.
   */
  suspend fun getRequest(requestId: String): Request

  /**
   * Adds a new request to the repository.
   *
   * @param request The Request to add.
   */
  suspend fun addRequest(request: Request)

  /**
   * Updates an existing request in the repository.
   *
   * @param requestId The unique identifier of the request to update.
   * @param updatedRequest The new state of the request.
   * @throws Exception if the request is not found.
   */
  suspend fun updateRequest(requestId: String, updatedRequest: Request)

  /**
   * Deletes a request from the repository.
   *
   * @param requestId The unique identifier of the request to delete.
   * @throws Exception if the request is not found.
   */
  suspend fun deleteRequest(requestId: String)

  /**
   * Check if the current user has accepted this request.
   *
   * @param requestId The unique identifier of the request.
   */
  suspend fun hasUserAcceptedRequest(requestId: String): Boolean

  /**
   * Accept the request
   *
   * @param requestId The unique identifier of the request to accept.
   */
  suspend fun acceptRequest(requestId: String)

  /**
   * Cancel the acceptance of the request
   *
   * @param requestId The unique identifier of the request to cancel.
   */
  suspend fun cancelAcceptance(requestId: String)
}
