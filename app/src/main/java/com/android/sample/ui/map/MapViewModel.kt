package com.android.sample.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationProvider
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
import kotlinx.coroutines.flow.update
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
    val target: LatLng = LatLng(ConstantMap.LATITUDE_EPFL, ConstantMap.LONGITUDE_EPFL),
    val request: List<Request> = emptyList(),
    val errorMsg: String? = null,
    var currentRequest: Request? = null,
    var isOwner: Boolean? = null,
    val currentProfile: UserProfile? = null,
    val currentListRequest: List<Request>? = null,
    val requestOwnership: RequestOwnership = RequestOwnership.ALL,
    val needToZoom: Boolean = true,
    val isLoadingLocation: Boolean = false,
    val currentLocation: LatLng? = null,
    val zoomPreference: MapZoomPreference = MapZoomPreference.NEAREST_REQUEST,
    val wasOnAnotherScreen: Boolean = true
)

/** A class for the preference of the user for automatic zoom */
enum class MapZoomPreference {
  NEAREST_REQUEST, // Zoom on the nearest request
  CURRENT_LOCATION, // Zoom on user's current location
  NO_AUTO_ZOOM // Don't auto-zoom
}

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
        UserProfileRepositoryFirestore(Firebase.firestore),
    private val locationProvider: LocationProvider
) : ViewModel() {
  companion object {
    val EPFL_LOCATION = Location(ConstantMap.LATITUDE_EPFL, ConstantMap.LONGITUDE_EPFL, "EPFL")
  }

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  private var isHisRequestJob: Job? = null
  private var updateCurrentProfileJob: Job? = null

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
  fun refreshUIState(requestId: String?) {
    fetchAcceptedRequest(requestId)
  }

  /** Refreshes the UI by updating the current request */
  fun updateCurrentRequest(request: Request) {
    val loc = LatLng(request.location.latitude, request.location.longitude)
    _uiState.value =
        _uiState.value.copy(currentRequest = request, currentListRequest = null, target = loc)
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

  /** Set wasOnAnotherScreen to true */
  fun goOnAnotherScreen() {
    _uiState.value = _uiState.value.copy(wasOnAnotherScreen = true, currentListRequest = null)
  }
  /** Set wasOnAnotherScreen to false */
  fun comeBackFromAnotherScreen() {
    _uiState.value = _uiState.value.copy(wasOnAnotherScreen = false)
  }

  /**
   * Set the target to the previous request the user has clicked on if he was in another screen.
   *
   * @return true if it works, false if the user wasn't on another screen or he hasn't clicked on a
   *   request
   */
  private fun setLocToCurrentRequest(): Boolean {
    if (_uiState.value.wasOnAnotherScreen && _uiState.value.currentRequest != null) {
      val loc =
          LatLng(
              _uiState.value.currentRequest!!.location.latitude,
              _uiState.value.currentRequest!!.location.longitude)
      _uiState.value = _uiState.value.copy(target = loc, needToZoom = true)
      return true
    }
    return false
  }

  /**
   * This function is used when you are coming from the accept request screen. It update the target
   * and the current request.
   *
   * @param requestId the id of the request you want to open
   */
  fun fromRequestDetailsToRequest(requestId: String) {
    viewModelScope.launch {
      try {
        val request = requestRepository.getRequest(requestId)

        val target = LatLng(request.location.latitude, request.location.longitude)
        _uiState.value =
            _uiState.value.copy(
                target = target,
                currentRequest = request,
                currentListRequest = null,
                needToZoom = true)

        updateCurrentProfile(request.creatorId)
        isHisRequest(request)
      } catch (e: Exception) {
        setErrorMsg(ConstantMap.FAIL_LOAD_REQUEST + "${e.message}")
      }
    }
  }

  /**
   * Update target depending on argument in UIState and the list of requests
   *
   * @param requests the lists of request
   */
  fun zoomOnRequest(requests: List<Request>) {
    if (!setLocToCurrentRequest()) {
      var location: Location?
      val currentLocIsNotNull = _uiState.value.currentLocation != null
      if (_uiState.value.zoomPreference == MapZoomPreference.NO_AUTO_ZOOM) {
        return
      }
      if (currentLocIsNotNull) {
        location = zoomIfCurrentLocNotNull(requests)
        if (location == null) {
          return
        }
      } else {
        location = requests.firstOrNull()?.location ?: EPFL_LOCATION
      }
      _uiState.value =
          _uiState.value.copy(
              target = LatLng(location.latitude, location.longitude), needToZoom = true)
    } else {
      comeBackFromAnotherScreen()
    }
  }

  /**
   * Give the corresponding location in function of some parameters
   *
   * @param requests the lists of request
   * @return a Location
   */
  private fun zoomIfCurrentLocNotNull(requests: List<Request>): Location? {
    return if (requests.isEmpty() ||
        _uiState.value.zoomPreference == MapZoomPreference.CURRENT_LOCATION) {
      Location(
          _uiState.value.currentLocation?.latitude ?: EPFL_LOCATION.latitude,
          _uiState.value.currentLocation?.longitude ?: EPFL_LOCATION.longitude,
          ConstantMap.CURR_POS_NAME)
    } else if (_uiState.value.zoomPreference == MapZoomPreference.NO_AUTO_ZOOM) {
      return null
    } else {
      findClosestRequest(_uiState.value.currentLocation, requests)
    }
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
   * Update zoom preference
   *
   * @param preference the preference
   */
  fun updateZoomPreference(preference: MapZoomPreference) {
    if (preference == MapZoomPreference.CURRENT_LOCATION &&
        _uiState.value.currentLocation == null) {
      setErrorMsg(ConstantMap.ERROR_MESSAGE_CURRENT_LOCATION)
    } else {
      _uiState.update { it.copy(zoomPreference = preference) }
    }
  }

  /** Get current location and center map on it */
  fun getCurrentLocation() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingLocation = true) }
      try {
        val location = locationProvider.getCurrentLocation()
        if (location != null) {
          val locLatLng = LatLng(location.latitude, location.longitude)
          if (_uiState.value.currentRequest == null && _uiState.value.currentListRequest == null) {
            _uiState.update {
              it.copy(
                  target = locLatLng,
                  needToZoom = true,
                  isLoadingLocation = false,
                  currentLocation = locLatLng)
            }
          } else {
            _uiState.update { it.copy(isLoadingLocation = false, currentLocation = locLatLng) }
          }
        } else {
          _uiState.update { it.copy(isLoadingLocation = false) }
          setErrorMsg(ConstantMap.ERROR_FAILED_TO_GET_CURRENT_LOCATION)
          if (_uiState.value.currentRequest == null && _uiState.value.currentListRequest == null) {
            zoomOnRequest(_uiState.value.request)
          }
        }
      } catch (e: Exception) {
        setErrorMsg(ConstantMap.ERROR_FAILED_TO_GET_CURRENT_LOCATION + " ${e.message}")
        _uiState.update { it.copy(isLoadingLocation = false) }
      }
    }
  }

  /** Set location permission error message */
  fun setLocationPermissionError() {
    _uiState.update {
      it.copy(errorMsg = ConstantMap.ERROR_MESSAGE_LOCATION_PERMISSION, isLoadingLocation = false)
    }
  }

  /**
   * Fetches all accepted requests and updates the MapUIState. Sets the target location to the first
   * request with a valid location, or to EPFL if no request has a valid address.
   */
  private fun fetchAcceptedRequest(requestId: String? = null) {
    viewModelScope.launch {
      try {
        val requests = requestRepository.getAllCurrentRequests()

        val matchingRequest = requests.firstOrNull { it.requestId == requestId }
        if (matchingRequest != null) {
          _uiState.value = _uiState.value.copy(request = requests)
          return@launch
        }

        val current = _uiState.value.currentRequest
        val updatedCurrent = current?.let { requests.find { it.requestId == current.requestId } }

        if (current == null || updatedCurrent == null) {

          val target = requests.firstOrNull()?.location ?: EPFL_LOCATION

          _uiState.value =
              _uiState.value.copy(
                  request = requests,
                  target = LatLng(target.latitude, target.longitude),
                  currentRequest = null,
                  isOwner = null)
        } else {
          // currentRequest found and up to date
          val location = LatLng(updatedCurrent.location.latitude, updatedCurrent.location.longitude)
          _uiState.value =
              _uiState.value.copy(
                  request = requests,
                  currentRequest = updatedCurrent,
                  isOwner = _uiState.value.isOwner,
                  target = location,
                  needToZoom = true)
        }
      } catch (e: Exception) {
        setErrorMsg("Failed to load requests: ${e.message}")
      }
    }
  }
}

class MapViewModelFactory(
    private val requestRepository: RequestRepository =
        RequestRepositoryFirestore(Firebase.firestore),
    private val profileRepository: UserProfileRepository =
        UserProfileRepositoryFirestore(Firebase.firestore),
    private val locationProvider: LocationProvider
) : ViewModelProvider.Factory {
  @Suppress(ConstantMap.UNCHECKED_CAST)
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
      return MapViewModel(requestRepository, profileRepository, locationProvider) as T
    }
    throw IllegalArgumentException(ConstantMap.ERROR_VIEW_MODEL_CLASS + "${modelClass.name}")
  }
}
