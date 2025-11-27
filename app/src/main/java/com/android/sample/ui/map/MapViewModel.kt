package com.android.sample.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.profile.UserProfileRepositoryFirestore
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestOwnership
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the Map screen.
 *
 * @property target The current target location shown on the map.
 * @property request The list of accepted requests to display (currently empty until repository is
 *   implemented).
 * @property errorMsg An optional error message to display in the UI.
 */
data class MapUIState(
    val target: LatLng = LatLng(0.0, 0.0),
    val request: List<Request> = emptyList(),
    val errorMsg: String? = null,
    var currentRequest: Request? = null,
    var isOwner: Boolean? = null,
    val currentProfile: UserProfile? = null,
    val currentListRequest: List<Request>? = null,
    val requestOwnership: RequestOwnership = RequestOwnership.ALL,
    val needToZoom: Boolean = true
)

/**
 * ViewModel responsible for managing the state of the Map screen.
 *
 * It handles fetching requests, managing the target location, and exposing the MapUIState to the UI
 * as a StateFlow.
 */
class MapViewModel(
    private val requestRepository: RequestRepository =
        RequestRepositoryFirestore(Firebase.firestore),
    val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore)
) : ViewModel() {
  companion object {
    val EPFL_LOCATION = Location(46.5191, 6.5668, "EPFL")
  }

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  private var isHisRequestJob: Job? = null
  private var updateCurrentProfileJob: Job? = null

  init {
    fetchAcceptedRequest()
  }

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

  /** Refreshes the UI by fetching accepted requests. */
  fun refreshUIState() {
    fetchAcceptedRequest()
  }

  /** Refreshes the UI by updating the current request */
  fun updateCurrentRequest(request: Request) {
    _uiState.value = _uiState.value.copy(currentRequest = request, currentListRequest = null)
    isHisRequest(request)
  }

  /** Refreshes the UI by updating the current list of current request */
  fun updateCurrentListRequest(requestList: List<Request>) {
    _uiState.value =
        _uiState.value.copy(
            currentRequest = null, currentListRequest = requestList, currentProfile = null)
  }

  /** Set current request, profile and listOfRequest to null */
  fun updateNoRequests() {
    _uiState.value =
        _uiState.value.copy(currentRequest = null, currentListRequest = null, currentProfile = null)
  }

  /**
   * Update the UI by updating the current profile
   *
   * @param profileId the id of the owner of the current request
   */
  fun updateCurrentProfile(profileId: String?) {
    updateCurrentProfileJob?.cancel()
    updateCurrentProfileJob =
        viewModelScope.launch {
          try {
            if (profileId == null) {
              _uiState.value = _uiState.value.copy(currentProfile = null)
            } else {
              val currentProfile = profileRepository.getUserProfile(profileId)
              _uiState.value = _uiState.value.copy(currentProfile = currentProfile)
            }
          } catch (e: Exception) {
            setErrorMsg("Failed to get current profile: ${e.message}")
            _uiState.value = _uiState.value.copy(currentProfile = null)
          }
        }
  }

  /** Refreshes the UI by updating the isOwner */
  fun isHisRequest(request: Request?) {
    isHisRequestJob?.cancel()
    isHisRequestJob =
        viewModelScope.launch {
          try {
            if (request == null) {
              _uiState.value = _uiState.value.copy(isOwner = null)
            } else {
              val isOwner = requestRepository.isOwnerOfRequest(request)
              _uiState.value = _uiState.value.copy(isOwner = isOwner)
            }
          } catch (e: Exception) {
            setErrorMsg("Failed to get current user: ${e.message}")
            _uiState.value = _uiState.value.copy(isOwner = null)
          }
        }
  }

  /** get current user id */
  fun getCurrentUserID(): String {
    return profileRepository.getCurrentUserId()
  }

  /**
   * Update target at the first request of this list
   *
   * @param requests the lists of request
   */
  fun zoomOnRequest(requests: List<Request>) {
    val loc = requests.firstOrNull()?.location ?: EPFL_LOCATION
    _uiState.value =
        _uiState.value.copy(target = LatLng(loc.latitude, loc.longitude), needToZoom = true)
  }

  /** Set the needToZoom to false */
  fun zoomCompleted() {
    _uiState.value = _uiState.value.copy(needToZoom = false)
  }

  /** Update the filter of the request */
  fun updateFilterOwnerShip(filter: RequestOwnership) {
    _uiState.value = _uiState.value.copy(requestOwnership = filter)
  }

  /**
   * Filter the list of request that is given with the current RequestOwnership
   *
   * @param requests list of requests
   * @param userId the id of the current user
   * @return the list filtered
   */
  fun filterWithOwnerShip(requests: List<Request>, userId: String): List<Request> {
    return when (_uiState.value.requestOwnership) {
      RequestOwnership.ALL -> requests
      RequestOwnership.OWN -> requests.filter { it.creatorId == userId }
      RequestOwnership.OTHER -> requests.filter { it.creatorId != userId }
      RequestOwnership.ACCEPTED -> requests.filter { it.people.contains(userId) }
      RequestOwnership.NOT_ACCEPTED_BY_ME -> requests.filter { !it.people.contains(userId) }
      RequestOwnership.NOT_ACCEPTED -> requests.filter { it.people.isEmpty() }
    }
  }

  /**
   * Fetches all accepted requests and updates the MapUIState. Sets the target location to the first
   * request with a valid location, or to EPFL if no request has a valid address.
   */
  private fun fetchAcceptedRequest() {
    viewModelScope.launch {
      try {
        val requests = requestRepository.getAllCurrentRequests()

        val current = _uiState.value.currentRequest
        val updatedCurrent = current?.let { requests.find { it.requestId == current.requestId } }

        if (current == null || updatedCurrent == null) {
          // currentRequest has been deleted
          _uiState.value = _uiState.value.copy(currentRequest = null, isOwner = null)
        } else {
          // currentRequest found and up to date
          _uiState.value =
              _uiState.value.copy(currentRequest = updatedCurrent, isOwner = _uiState.value.isOwner)
        }

        val loc =
            _uiState.value.currentRequest?.location
                ?: requests.firstOrNull()?.location
                ?: EPFL_LOCATION
        _uiState.value =
            _uiState.value.copy(
                target = LatLng(loc.latitude, loc.longitude), request = requests, needToZoom = true)
      } catch (e: Exception) {
        setErrorMsg("Failed to load requests: ${e.message}")
      }
    }
  }
}
