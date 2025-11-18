package com.android.sample.ui.request_validation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestClosureException
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.displayString
import kotlinx.coroutines.launch

/** Represents the different states of the validation process. */
sealed class ValidationState {
  /** Initial state - loading request and user data. */
  object Loading : ValidationState()

  /** Ready state - user can select helpers. */
  data class Ready(
      val request: Request,
      val helpers: List<UserProfile>,
      val selectedHelperIds: Set<String> = emptySet()
  ) : ValidationState()

  /** Confirming state - showing confirmation dialog. */
  data class Confirming(
      val request: Request,
      val selectedHelpers: List<UserProfile>,
      val kudosToAward: Int,
      val creatorBonus: Int
  ) : ValidationState()

  /** Processing state - closing request and awarding kudos. */
  object Processing : ValidationState()

  /** Success state - operation completed. */
  object Success : ValidationState()

  /** Error state with message. */
  data class Error(val message: String, val canRetry: Boolean = true) : ValidationState()
}

/**
 * ViewModel for the request validation screen.
 *
 * Manages the business logic for:
 * - Loading request and helper profiles
 * - Selecting/deselecting helpers
 * - Validating selections
 * - Closing request and awarding kudos atomically
 *
 * @param requestId The ID of the request to validate
 * @param requestRepository Repository for request operations
 * @param userProfileRepository Repository for user profile operations
 */
class ValidateRequestViewModel(
    private val requestId: String,
    private val requestRepository: RequestRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

  var state by mutableStateOf<ValidationState>(ValidationState.Loading)
    private set

  init {
    loadRequestData()
  }

  /** Loads the request and all helper profiles. */
  private fun loadRequestData() {
    viewModelScope.launch {
      state = ValidationState.Loading

      try {
        // Load the request
        val request = requestRepository.getRequest(requestId)

        // Verify ownership
        if (!requestRepository.isOwnerOfRequest(request)) {
          state =
              ValidationState.Error(
                  message = "You are not the owner of this request.", canRetry = false)
          return@launch
        }

        // Verify status
        if (request.status != RequestStatus.OPEN && request.status != RequestStatus.IN_PROGRESS) {
          state =
              ValidationState.Error(
                  message =
                      "This request cannot be closed. Status: ${request.status.displayString()}",
                  canRetry = false)
          return@launch
        }

        // Load all helper profiles
        val helpers =
            if (request.people.isNotEmpty()) {
              request.people.mapNotNull { userId ->
                try {
                  userProfileRepository.getUserProfile(userId)
                } catch (e: Exception) {
                  // Log error but continue with other profiles
                  null
                }
              }
            } else {
              emptyList()
            }

        // Check if we have any helpers
        if (helpers.isEmpty() && request.people.isNotEmpty()) {
          state =
              ValidationState.Error(
                  message = "Could not load helper profiles. Please try again.", canRetry = true)
          return@launch
        }

        state =
            ValidationState.Ready(
                request = request, helpers = helpers, selectedHelperIds = emptySet())
      } catch (e: Exception) {
        state =
            ValidationState.Error(message = "Failed to load request: ${e.message}", canRetry = true)
      }
    }
  }

  /** Toggles selection of a helper. */
  fun toggleHelperSelection(userId: String) {
    val currentState = state
    if (currentState !is ValidationState.Ready) return

    val newSelection =
        if (userId in currentState.selectedHelperIds) {
          currentState.selectedHelperIds - userId
        } else {
          currentState.selectedHelperIds + userId
        }

    state = currentState.copy(selectedHelperIds = newSelection)
  }

  /** Initiates the confirmation process. */
  fun showConfirmation() {
    val currentState = state
    if (currentState !is ValidationState.Ready) return

    val selectedHelpers = currentState.helpers.filter { it.id in currentState.selectedHelperIds }

    val kudosPerHelper = KudosConstants.KUDOS_PER_HELPER
    val totalKudos = selectedHelpers.size * kudosPerHelper
    val creatorBonus =
        if (selectedHelpers.isNotEmpty()) {
          KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION
        } else {
          0
        }

    state =
        ValidationState.Confirming(
            request = currentState.request,
            selectedHelpers = selectedHelpers,
            kudosToAward = totalKudos,
            creatorBonus = creatorBonus)
  }

  /** Cancels the confirmation and returns to selection. */
  fun cancelConfirmation() {
    val currentState = state
    if (currentState !is ValidationState.Confirming) return

    // Get the selected IDs from the current state
    val selectedIds = currentState.selectedHelpers.map { it.id }.toSet()

    // Need to reload to get back to Ready state with helpers list
    loadRequestData()

    // After loading, restore the selection
    viewModelScope.launch {
      // Wait a bit for the state to update
      kotlinx.coroutines.delay(100)
      val readyState = state
      if (readyState is ValidationState.Ready) {
        state = readyState.copy(selectedHelperIds = selectedIds)
      }
    }
  }

  /** Confirms and executes the request closure with kudos awards. */
  fun confirmAndClose() {
    val currentState = state
    if (currentState !is ValidationState.Confirming) return

    viewModelScope.launch {
      state = ValidationState.Processing

      try {
        // Extract selected helper IDs
        val selectedHelperIds = currentState.selectedHelpers.map { it.id }

        // Close the request first and check if creator should receive kudos
        val shouldAwardCreator =
            requestRepository.closeRequest(
                requestId = requestId, selectedHelperIds = selectedHelperIds)

        // Prepare kudos awards map
        val kudosAwards = mutableMapOf<String, Int>()

        // Add kudos for selected helpers
        selectedHelperIds.forEach { helperId ->
          kudosAwards[helperId] = KudosConstants.KUDOS_PER_HELPER
        }

        // Add creator bonus if applicable
        if (shouldAwardCreator) {
          kudosAwards[currentState.request.creatorId] = KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION
        }

        // Award kudos (will rollback if any fails, but request is already closed)
        if (kudosAwards.isNotEmpty()) {
          try {
            userProfileRepository.awardKudosBatch(kudosAwards)
          } catch (e: KudosException) {
            android.util.Log.e(
                "ValidateRequestVM", "Failed to award kudos after closing request", e)
          }
        }

        state = ValidationState.Success
      } catch (e: RequestClosureException) {
        state =
            ValidationState.Error(
                message = "Failed to close request: ${e.message}", canRetry = true)
      } catch (e: KudosException) {
        state =
            ValidationState.Error(message = "Failed to award kudos: ${e.message}", canRetry = true)
      } catch (e: Exception) {
        state =
            ValidationState.Error(
                message = "An unexpected error occurred: ${e.message}", canRetry = true)
      }
    }
  }

  /** Retries loading after an error. */
  fun retry() {
    loadRequestData()
  }

  /** Resets to initial state (for navigation). */
  fun reset() {
    state = ValidationState.Loading
  }
}

/** Factory for creating ValidateRequestViewModel with dependencies. */
class ValidateRequestViewModelFactory(
    private val requestId: String,
    private val requestRepository: RequestRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ValidateRequestViewModel::class.java)) {
      return ValidateRequestViewModel(
          requestId = requestId,
          requestRepository = requestRepository,
          userProfileRepository = userProfileRepository)
          as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
