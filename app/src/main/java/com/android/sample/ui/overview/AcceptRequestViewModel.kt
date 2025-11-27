package com.android.sample.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestCache
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AcceptRequestUIState(
    val request: Request? = null,
    val creatorName: String? = null,
    val errorMsg: String? = null,
    val accepted: Boolean = false,
    val isLoading: Boolean = false,
    val offlineMode: Boolean = false
)

class AcceptRequestViewModel(
    private val requestRepository: RequestRepository =
        RequestRepositoryFirestore(Firebase.firestore),
    private val userProfileRepository: UserProfileRepository? = null,
    private val requestCache: RequestCache? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(AcceptRequestUIState())
  val uiState: StateFlow<AcceptRequestUIState> = _uiState.asStateFlow()

  /**
   * Updates the UI state with an error message.
   *
   * @param errorMsg The error message to display.
   */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Clears any error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Loads a request by its Id, fetches creator information, and updates the UI state
   *
   * @param requestID the id of the request to load
   */
  fun loadRequest(requestID: String) {
    viewModelScope.launch {
      try {
        val request = requestRepository.getRequest(requestID)
        val accept = requestRepository.hasUserAcceptedRequest(request)

        Log.d("AcceptRequest", "=== DEBUG START ===")
        Log.d("AcceptRequest", "Creator ID: ${request.creatorId}")
        Log.d("AcceptRequest", "Repository null? ${userProfileRepository == null}")

        // Fetch creator name if repository is available
        var creatorName: String? = null
        if (userProfileRepository != null) {
          try {
            Log.d("AcceptRequest", "Attempting to fetch user profile...")
            val creatorProfile = userProfileRepository.getUserProfile(request.creatorId)
            Log.d(
                "AcceptRequest",
                "Profile fetched - name: ${creatorProfile.name}, lastName: ${creatorProfile.lastName}")
            creatorName = "${creatorProfile.name} ${creatorProfile.lastName}".trim()
            Log.d("AcceptRequest", "SUCCESS - Final creator name: $creatorName")
          } catch (e: Exception) {
            Log.e("AcceptRequest", "FAILED - Exception: ${e.message}", e)
            // Fallback to creatorId if profile fetch fails
            creatorName = request.creatorId
          }
        } else {
          Log.e("AcceptRequest", "FAILED - Repository is NULL")
          // If no repository provided, use creatorId as fallback
          creatorName = request.creatorId
        }
        Log.d("AcceptRequest", "=== DEBUG END ===")

        _uiState.value =
            AcceptRequestUIState(
                request = request,
                accepted = accept,
                creatorName = creatorName,
                offlineMode = false)
      } catch (e: Exception) {
        try {
          val cachedRequest = requestCache?.getRequestById(requestID)
          _uiState.update {
            it.copy(
                request = cachedRequest,
                accepted = requestRepository.hasUserAcceptedRequest(cachedRequest!!),
                offlineMode = true)
          }
        } catch (e: Exception) {
          Log.e("AcceptRequestViewModel", "Failed to load request: ${e.message}", e)
          setErrorMsg("Failed to load request: ${e.message}")
        }
      }
    }
  }

  /**
   * Cancel the acceptance of the request, and update the UI state
   *
   * @param requestID the id of the request to cancel
   */
  fun cancelAcceptanceToRequest(requestID: String) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        requestRepository.cancelAcceptance(requestID)
        _uiState.value = _uiState.value.copy(accepted = false)
      } catch (e: Exception) {
        Log.e("AcceptRequestViewModel", "Failed to cancel request: ${e.message}", e)
        setErrorMsg("Failed to cancel acceptance to request: ${e.message}")
        loadRequest(requestID)
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  /**
   * Accept the request by its id, and update the UI state
   *
   * @param requestID the id of the request to accept
   */
  fun acceptRequest(requestID: String) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        requestRepository.acceptRequest(requestID)
        _uiState.value = _uiState.value.copy(accepted = true)
      } catch (e: Exception) {
        Log.e("AcceptRequestViewModel", "Failed to accept request: ${e.message}", e)
        setErrorMsg("Failed to accept request: ${e.message}")
        loadRequest(requestID)
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}

class AcceptRequestViewModelFactory(
    private val requestRepository: RequestRepository,
    private val userProfileRepository: UserProfileRepository?,
    private val requestCache: RequestCache
) : androidx.lifecycle.ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AcceptRequestViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return AcceptRequestViewModel(
          requestRepository = requestRepository,
          userProfileRepository = userProfileRepository,
          requestCache = requestCache)
          as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
  /**
   * FOR TESTING ONLY - Manually sets offline mode state This method is used exclusively in UI tests
   * to simulate offline scenarios
   */
}
