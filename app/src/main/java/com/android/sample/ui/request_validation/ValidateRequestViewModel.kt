package com.android.sample.ui.request_validation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.chat.ChatRepository
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.CloseRequestResult
import com.android.sample.model.request.CloseRequestUseCase
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestClosureException
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.displayString
import kotlinx.coroutines.launch

/** Represents the different states of the validation process. */
sealed class ValidationState {
  /** Initial state loading request and user data. */
  object Loading : ValidationState()

  /** Ready state user can select helpers. */
  data class Ready(
      val request: Request,
      val helpers: List<UserProfile>,
      val selectedHelperIds: Set<String> = emptySet()
  ) : ValidationState()

  /** Confirming state showing confirmation dialog. */
  data class Confirming(
      val request: Request,
      val selectedHelpers: List<UserProfile>,
      val kudosToAward: Int
  ) : ValidationState()

  /** Processing state closing request and awarding kudos. */
  object Processing : ValidationState()

  /** Success state - operation completed. */
  object Success : ValidationState()

  /** Error state with message. */
  data class Error(val message: String, val canRetry: Boolean = true) : ValidationState()
}

private const val NOT_OWNER = "You are not the owner of this request."

private const val CANNOT_BE_CLOSED = "This request cannot be closed. Status:"

private const val NOT_LOAD_PROFILES_ERROR = "Could not load helper profiles. Please try again."

private const val FAILED_TO_LOAD_REQUESTS = "Failed to load request:"

private const val WAIT_TIME_100_MS = 100L
private const val VALIDATE_REQUEST = "ValidateRequestVM"

private const val UNEXPECTED_ERROR = "An unexpected error occurred:"

private const val FAILED_TO_AWARD_KUDOS_ERROR = "Failed to award kudos after closing request"

private const val FAILED_TO_CLOSE_REQUEST_ERROR = "Failed to close request:"
const val TAG = "ValidationViewModel"

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
    private val userProfileRepository: UserProfileRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

  var state by mutableStateOf<ValidationState>(ValidationState.Loading)
    private set

  init {
    loadRequestData()
  }

  private val closeRequestUseCase = CloseRequestUseCase(requestRepository, userProfileRepository)
  /** Loads the request and all helper profiles. */
  private fun loadRequestData() {
    viewModelScope.launch {
      state = ValidationState.Loading

      try {
        val request = requestRepository.getRequest(requestId)

        // Extract validation to separate method
        validateRequestForClosure(request)?.let { errorState ->
          state = errorState
          return@launch
        }

        // Extract helper loading to separate method
        val helpers = loadHelperProfiles(request.people)

        // Validate helpers loaded successfully
        if (helpers.isEmpty() && request.people.isNotEmpty()) {
          state = ValidationState.Error(message = NOT_LOAD_PROFILES_ERROR, canRetry = true)
          return@launch
        }

        state =
            ValidationState.Ready(
                request = request, helpers = helpers, selectedHelperIds = emptySet())
      } catch (e: Exception) {
        state =
            ValidationState.Error(
                message = "$FAILED_TO_LOAD_REQUESTS ${e.message}", canRetry = true)
      }
    }
  }
  /**
   * Validates that the request can be closed.
   *
   * @return Error state if validation fails, null if valid
   */
  private suspend fun validateRequestForClosure(request: Request): ValidationState.Error? {
    if (!requestRepository.isOwnerOfRequest(request)) {
      return ValidationState.Error(message = NOT_OWNER, canRetry = false)
    }

    if (request.status != RequestStatus.OPEN && request.status != RequestStatus.IN_PROGRESS) {
      return ValidationState.Error(
          message = "$CANNOT_BE_CLOSED ${request.status.displayString()}", canRetry = false)
    }

    return null
  }

  /**
   * Loads all helper profiles from the list of user IDs.
   *
   * @return List of successfully loaded profiles (empty if none could be loaded)
   */
  private suspend fun loadHelperProfiles(peopleIds: List<String>): List<UserProfile> {
    if (peopleIds.isEmpty()) {
      return emptyList()
    }

    return peopleIds.mapNotNull { userId ->
      try {
        userProfileRepository.getUserProfile(userId)
      } catch (e: Exception) {
        null
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

    state =
        ValidationState.Confirming(
            request = currentState.request,
            selectedHelpers = selectedHelpers,
            kudosToAward = totalKudos)
  }

  /** Cancels the confirmation and returns to selection. */
  fun cancelConfirmation() {
    val currentState = state
    if (currentState !is ValidationState.Confirming) return

    // Get the selected IDs from the current state
    val selectedIds = currentState.selectedHelpers.map { it.id }.toSet()

    // Need to reload to get back to Ready state with helpers list
    loadRequestData()

    viewModelScope.launch {
      kotlinx.coroutines.delay(WAIT_TIME_100_MS)
      val readyState = state
      if (readyState is ValidationState.Ready) {
        state = readyState.copy(selectedHelperIds = selectedIds)
      }
    }
  }

  /** Confirms and executes the request closure with kudos awards. */
  /** Confirms and executes the request closure with kudos awards. */
  fun confirmAndClose() {
    val currentState = state
    if (currentState !is ValidationState.Confirming) return

    viewModelScope.launch {
      state = ValidationState.Processing

      try {
        // Extract selected helper IDs
        val selectedHelperIds = currentState.selectedHelpers.map { it.id }

        // Use the use case to close request and award kudos
        val result = closeRequestUseCase.execute(requestId, selectedHelperIds)

        when (result) {
          is CloseRequestResult.Success,
          is CloseRequestResult.PartialSuccess -> {
            deleteChatSafely()
            state = ValidationState.Success
          }
          is CloseRequestResult.Failure -> {
            throw result.error
          }
        }
      } catch (e: RequestClosureException) {
        state =
            ValidationState.Error(
                message = "$FAILED_TO_CLOSE_REQUEST_ERROR ${e.message}", canRetry = true)
      } catch (e: KudosException) {
        state =
            ValidationState.Error(
                message = "$FAILED_TO_AWARD_KUDOS_ERROR ${e.message}", canRetry = true)
      } catch (e: Exception) {
        state = ValidationState.Error(message = "$UNEXPECTED_ERROR ${e.message}", canRetry = true)
      }
    }
  }

  /**
   * Safely deletes the chat associated with the request. Failures are logged but don't affect the
   * overall operation success.
   */
  private suspend fun deleteChatSafely() {
    try {
      chatRepository.deleteChat(requestId)
    } catch (e: IllegalStateException) {
      android.util.Log.w(TAG, "Failed to delete chat $requestId: ${e.message}")
    } catch (e: Exception) {
      android.util.Log.e(TAG, "Unexpected error deleting chat $requestId", e)
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

private const val UNKNOWN_VIEW_MODEL_CLASS = "Unknown ViewModel class"

/** Factory for creating ValidateRequestViewModel with dependencies. */
class ValidateRequestViewModelFactory(
    private val requestId: String,
    private val requestRepository: RequestRepository,
    private val userProfileRepository: UserProfileRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ValidateRequestViewModel::class.java)) {
      return ValidateRequestViewModel(
          requestId = requestId,
          requestRepository = requestRepository,
          userProfileRepository = userProfileRepository,
          chatRepository = chatRepository)
          as T
    }
    throw IllegalArgumentException(UNKNOWN_VIEW_MODEL_CLASS)
  }
}
