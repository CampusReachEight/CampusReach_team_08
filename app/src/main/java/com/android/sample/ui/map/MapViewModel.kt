package com.android.sample.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.google.android.gms.maps.model.LatLng
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
    val errorMsg: String? = null
)

/**
 * ViewModel responsible for managing the state of the Map screen.
 *
 * It handles fetching requests, managing the target location, and exposing the MapUIState to the UI
 * as a StateFlow.
 */
class MapViewModel(
    // add the repository
) : ViewModel() {
  companion object {
    private val EPFL_LOCATION = Location(46.5191, 6.5668, "EPFL")
  }

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    // Add Listener when it is implemented
    // Firebase.auth.addAuthStateListener {
    // if (it.currentUser != null) {
    fetchAcceptedRequest()
    // }
    // }
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

  /**
   * Fetches all accepted requests and updates the MapUIState. Sets the target location to the first
   * request with a valid location, or to EPFL if no request has a valid address.
   */
  private fun fetchAcceptedRequest() {
    viewModelScope.launch {
      try {
        // Temporary: since there is no repository yet,
        // we set the list of requests to an empty list and the location to EPFL.
        val loc = EPFL_LOCATION
        _uiState.value = MapUIState(target = LatLng(loc.latitude, loc.longitude))
      } catch (e: Exception) {
        setErrorMsg("Failed to load todos: ${e.message}")
      }
    }
  }
}
