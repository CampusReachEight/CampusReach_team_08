package com.android.sample.ui.profile.accepted_requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestCache
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryProvider
import com.android.sample.model.request.RequestStatus
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Accepted Requests screen.
 *
 * Manages the list of requests the current user has accepted as a helper, enriched with kudos
 * status information.
 */
class AcceptedRequestsViewModel(
    private val requestRepository: RequestRepository = RequestRepositoryProvider.repository,
    val requestCache: RequestCache? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(AcceptedRequestsUiState())
  val uiState: StateFlow<AcceptedRequestsUiState> = _uiState.asStateFlow()

  /**
   * Loads all requests that the current user has accepted as a helper. Enriches each request with
   * its kudos status and sorts by most recent first.
   */
  fun loadAcceptedRequests() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      try {
        val currentUserId =
            Firebase.auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")

        // Get all requests user has accepted
        val acceptedRequests =
            try {
              requestRepository.getAcceptedRequests()
            } catch (e: Exception) {
              requestCache?.loadRequests {
                it.people.contains(currentUserId) && it.creatorId != currentUserId
              } ?: emptyList()
            }

        // Enrich with kudos status and sort by most recent
        val requestsWithStatus =
            acceptedRequests
                .map { request ->
                  RequestWithKudosStatus(
                      request = request, kudosStatus = determineKudosStatus(request, currentUserId))
                }
                .sortedByDescending { it.request.startTimeStamp } // Most recent first

        _uiState.update { it.copy(requests = requestsWithStatus, isLoading = false) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMessage = "Failed to load accepted requests: ${e.localizedMessage}")
        }
      }
    }
  }

  /**
   * Determines the kudos status for a request based on the current user.
   *
   * Logic:
   * - PENDING: Request is still OPEN or IN_PROGRESS (not yet closed)
   * - RECEIVED: Request is COMPLETED and user is in selectedHelpers list
   * - NOT_RECEIVED: Request is COMPLETED but user is NOT in selectedHelpers list
   */
  private fun determineKudosStatus(request: Request, currentUserId: String): KudosStatus {
    return when {
      // Request not yet completed - kudos pending
      request.status != RequestStatus.COMPLETED -> KudosStatus.PENDING

      // Request completed and user was selected for kudos
      currentUserId in request.selectedHelpers -> KudosStatus.RECEIVED

      // Request completed but user was NOT selected for kudos
      else -> KudosStatus.NOT_RECEIVED
    }
  }

  /** Refreshes the list of accepted requests. Useful for pull-to-refresh functionality. */
  fun refresh() {
    loadAcceptedRequests()
  }

  /** Clears the current error message. */
  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }
}

/** Factory for creating AcceptedRequestsViewModel with dependencies. */
class AcceptedRequestsViewModelFactory(
    private val requestRepository: RequestRepository = RequestRepositoryProvider.repository,
    private val requestCache: RequestCache? = null
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AcceptedRequestsViewModel::class.java)) {
      return AcceptedRequestsViewModel(
          requestRepository = requestRepository, requestCache = requestCache)
          as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
