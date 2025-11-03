package com.android.sample.ui.request.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.map.LocationProvider
import com.android.sample.model.map.LocationRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryProvider
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOCATION_PERMISSION_REQUIRED =
    "Location permission is required to use current location"

/**
 * ViewModel for the EditRequest screen. Handles both creating new requests and editing existing
 * ones.
 */
class EditRequestViewModel(
    private val requestRepository: RequestRepository = RequestRepositoryProvider.repository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

  // Date formatter for validation
  private val dateFormat = SimpleDateFormat(DateFormats.DATE_TIME_FORMAT, Locale.getDefault())

  // Current request being edited (null for create mode)
  private val _currentRequest = MutableStateFlow<Request?>(null)
  val currentRequest: StateFlow<Request?> = _currentRequest.asStateFlow()

  // People list (not exposed in UI state as it's managed separately)
  private val _people = MutableStateFlow<List<String>>(emptyList())
  val people: StateFlow<List<String>> = _people.asStateFlow()

  // Single UI state instead of many individual states
  private val _uiState = MutableStateFlow(EditRequestUiState())
  val uiState: StateFlow<EditRequestUiState> = _uiState.asStateFlow()

  /** Load an existing request for editing */
  fun loadRequest(requestId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      try {
        val request = requestRepository.getRequest(requestId)
        _currentRequest.value = request
        _people.value = request.people

        _uiState.update {
          it.copy(
              isEditMode = true,
              title = request.title,
              description = request.description,
              requestTypes = request.requestType,
              location = request.location,
              locationName = request.locationName,
              startTimeStamp = request.startTimeStamp,
              expirationTime = request.expirationTime,
              tags = request.tags,
              isLoading = false)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, errorMessage = "Failed to load request: ${e.localizedMessage}")
        }
      }
    }
  }

  fun deleteRequest(requestId: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

      try {
        requestRepository.deleteRequest(requestId)
        _uiState.update { it.copy(isDeleting = false, showDeleteConfirmation = false) }
        onSuccess()
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isDeleting = false,
              showDeleteConfirmation = false,
              errorMessage = "Failed to delete request: ${e.localizedMessage}")
        }
      }
    }
  }

  /** Show delete confirmation dialog */
  fun confirmDelete() {
    _uiState.update { it.copy(showDeleteConfirmation = true) }
  }

  /** Hide delete confirmation dialog */
  fun cancelDelete() {
    _uiState.update { it.copy(showDeleteConfirmation = false) }
  }

  /** Clear location search results */
  fun clearLocationSearch() {
    _uiState.update { it.copy(locationSearchResults = emptyList()) }
  }

  /**
   * Search for locations matching the query
   *
   * @param query Search query string
   */
  fun searchLocations(query: String) {
    if (query.length < 3) {
      _uiState.update { it.copy(locationSearchResults = emptyList()) }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSearchingLocation = true) }

      try {
        val results = locationRepository.search(query, limit = 5)
        _uiState.update { it.copy(locationSearchResults = results, isSearchingLocation = false) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              errorMessage = "Location search failed: ${e.message}",
              locationSearchResults = emptyList(),
              isSearchingLocation = false)
        }
      }
    }
  }

  /** Initialize for create mode */
  fun initializeForCreate(creatorId: String) {
    _uiState.value = EditRequestUiState(isEditMode = false)
    _currentRequest.value = null
    _people.value = listOf(creatorId)
  }

  /**
   * Update form field values with validation
   *
   * @param value New title
   */
  fun updateTitle(value: String) {
    _uiState.update { state ->
      state.copy(
          title = value,
          validationState = state.validationState.copy(showTitleError = value.isBlank()))
    }
  }
  /**
   * Update description with validation
   *
   * @param value New description
   */
  fun updateDescription(value: String) {
    _uiState.update { state ->
      state.copy(
          description = value,
          validationState = state.validationState.copy(showDescriptionError = value.isBlank()))
    }
  }

  /**
   * Update request types with validation
   *
   * @param types New list of request types
   */
  fun updateRequestTypes(types: List<RequestType>) {
    _uiState.update { state ->
      state.copy(
          requestTypes = types,
          validationState = state.validationState.copy(showRequestTypeError = types.isEmpty()))
    }
  }
  /**
   * Update location
   *
   * @param location New location
   */
  fun updateLocation(location: Location?) {
    _uiState.update { it.copy(location = location) }
  }

  /**
   * Update location name with validation
   *
   * @param name New location name
   */
  fun updateLocationName(name: String) {
    _uiState.update { state ->
      state.copy(
          locationName = name,
          validationState = state.validationState.copy(showLocationNameError = name.isBlank()))
    }
  }

  /**
   * Update start time with validation
   *
   * @param date New start date
   */
  fun updateStartTimeStamp(date: Date) {
    _uiState.update { state ->
      state.copy(
          startTimeStamp = date,
          validationState =
              state.validationState.copy(showDateOrderError = state.expirationTime.before(date)))
    }
  }

  /**
   * Update expiration time with validation
   *
   * @param date New expiration date
   */
  fun updateExpirationTime(date: Date) {
    _uiState.update { state ->
      state.copy(
          expirationTime = date,
          validationState =
              state.validationState.copy(showDateOrderError = date.before(state.startTimeStamp)))
    }
  }

  fun updateTags(tags: List<Tags>) {
    _uiState.update { it.copy(tags = tags) }
  }

  fun updatePeople(people: List<String>) {
    _people.value = people
  }

  /** Validate all form fields using RequestFormValidator */
  private fun validateAllFields(): Boolean {
    val state = _uiState.value
    val startDateString = dateFormat.format(state.startTimeStamp)
    val expirationDateString = dateFormat.format(state.expirationTime)

    val validator =
        RequestFormValidator(
            title = state.title,
            description = state.description,
            requestTypes = state.requestTypes,
            location = state.location,
            locationName = state.locationName,
            startDateString = startDateString,
            expirationDateString = expirationDateString,
            startTimeStamp = state.startTimeStamp,
            expirationTime = state.expirationTime)

    _uiState.update { it.copy(validationState = validator.validate()) }
    return validator.isValid()
  }

  /** Save the request (create or update) */
  fun saveRequest(creatorId: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      delay(2000)

      // Validate all fields
      if (!validateAllFields()) {
        _uiState.update { it.copy(isLoading = false) }
        return@launch
      }

      try {
        val state = _uiState.value
        val request =
            Request(
                requestId = _currentRequest.value?.requestId ?: UUID.randomUUID().toString(),
                title = state.title,
                description = state.description,
                requestType = state.requestTypes,
                location = state.location!!,
                locationName = state.locationName,
                status = _currentRequest.value?.status ?: RequestStatus.IN_PROGRESS,
                startTimeStamp = state.startTimeStamp,
                expirationTime = state.expirationTime,
                people = _people.value.ifEmpty { listOf(creatorId) },
                tags = state.tags,
                creatorId = _currentRequest.value?.creatorId ?: creatorId)

        if (state.isEditMode) {
          requestRepository.updateRequest(request.requestId, request)
        } else {
          requestRepository.addRequest(request)
        }

        _uiState.update { state ->
          state.copy(
              isLoading = false,
              validationState = state.validationState.copy(showSuccessMessage = true))
        }
        onSuccess()
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, errorMessage = "Failed to save request: ${e.localizedMessage}")
        }
      }
    }
  }

  /** Get current location and update location fields */
  fun getCurrentLocation() {
    // Use location provider to get current location
    viewModelScope.launch {
      _uiState.update { it.copy(isSearchingLocation = true) }
      try {
        // Get current location
        val location = locationProvider.getCurrentLocation()
        if (location != null) {
          updateLocation(location)
          updateLocationName(location.name)
        }
        // Update UI state
        _uiState.update { it.copy(isSearchingLocation = false) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              errorMessage = "Failed to get current location: ${e.message}",
              isSearchingLocation = false)
        }
      }
    }
  }

  /** Set location permission error message */
  fun setLocationPermissionError() {
    _uiState.update {
      it.copy(errorMessage = LOCATION_PERMISSION_REQUIRED, isSearchingLocation = false)
    }
  }

  /** Clear error message */
  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  /** Clear success message */
  fun clearSuccessMessage() {
    _uiState.update { state ->
      state.copy(validationState = state.validationState.copy(showSuccessMessage = false))
    }
  }

  /** Reset the ViewModel state */
  fun reset() {
    _uiState.value = EditRequestUiState()
    _currentRequest.value = null
    _people.value = emptyList()
  }
}

/** Factory for creating EditRequestViewModel with dependencies. */
class EditRequestViewModelFactory(
    private val requestRepository: RequestRepository = RequestRepositoryProvider.repository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(EditRequestViewModel::class.java)) {
      return EditRequestViewModel(requestRepository, locationRepository, locationProvider) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
