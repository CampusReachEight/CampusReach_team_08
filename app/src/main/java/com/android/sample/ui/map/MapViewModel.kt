package com.android.sample.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryFirestore
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    var isOwner: Boolean = false
)

/**
 * ViewModel responsible for managing the state of the Map screen.
 *
 * It handles fetching requests, managing the target location, and exposing the MapUIState to the UI
 * as a StateFlow.
 */
class MapViewModel(
    private val requestRepository: RequestRepository =
        RequestRepositoryFirestore(Firebase.firestore)
) : ViewModel() {
  companion object {
    val EPFL_LOCATION = Location(46.5191, 6.5668, "EPFL")
  }

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

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
  fun updateCurrentRequest(request: Request?) {
    _uiState.value = _uiState.value.copy(currentRequest = request)
    isHisRequest()
  }

  /** Refreshes the UI by updating the isOwner */
  fun isHisRequest() {
    viewModelScope.launch {
      try {
        if (_uiState.value.currentRequest == null) {
          _uiState.value = _uiState.value.copy(isOwner = false)
        } else {
          _uiState.value =
              _uiState.value.copy(
                  isOwner = requestRepository.isOwnerOfRequest(_uiState.value.currentRequest!!))
        }
      } catch (e: Exception) {
        setErrorMsg("Failed to get current user: ${e.message}")
        _uiState.value = _uiState.value.copy(isOwner = false)
      }
    }
  }

  /**
   * Fetches all accepted requests and updates the MapUIState. Sets the target location to the first
   * request with a valid location, or to EPFL if no request has a valid address.
   */
  private fun fetchAcceptedRequest() {
    viewModelScope.launch {
      try {
        val requests = requestRepository.getAllRequests()

        val current = _uiState.value.currentRequest
        // if the currentRequest has been deleted
        if (current == null || requests.none { it.requestId == current.requestId }) {
          _uiState.value = _uiState.value.copy(currentRequest = null, isOwner = false)
        }
        // reassigned the updated request to current request
        else {
          val updatedCurrent = requests.find { it.requestId == current.requestId }
          _uiState.value =
              _uiState.value.copy(currentRequest = updatedCurrent, isOwner = _uiState.value.isOwner)
        }

        val loc =
            _uiState.value.currentRequest?.location
                ?: requests.firstOrNull()?.location
                ?: EPFL_LOCATION
        _uiState.value =
            MapUIState(target = LatLng(loc.latitude, loc.longitude), request = requests)
      } catch (e: Exception) {
        setErrorMsg("Failed to load requests: ${e.message}")
      }
    }
  }
}
