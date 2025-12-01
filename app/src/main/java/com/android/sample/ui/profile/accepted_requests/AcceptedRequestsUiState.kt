package com.android.sample.ui.profile.accepted_requests

import com.android.sample.model.request.Request

/**
 * UI state for the Accepted Requests screen.
 *
 * Displays all requests the current user has accepted as a helper, along with their kudos status.
 */
data class AcceptedRequestsUiState(
    val requests: List<RequestWithKudosStatus> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Wraps a Request with its kudos status for display purposes.
 *
 * @param request The underlying request data
 * @param kudosStatus Whether the user received kudos for helping with this request
 */
data class RequestWithKudosStatus(val request: Request, val kudosStatus: KudosStatus)

/** Represents whether the current user received kudos for a request they accepted. */
enum class KudosStatus {
  /** User was selected by the creator and received kudos. Display: Green badge/indicator */
  RECEIVED,

  /** Request was closed but user was not selected for kudos. Display: Red badge/indicator */
  NOT_RECEIVED,

  /**
   * Request is still open or in progress, kudos status unknown. Display: Gray/neutral
   * badge/indicator
   */
  PENDING
}
