package com.android.sample.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestRepositoryProvider
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the EditRequest screen.
 *
 * Handles both creating new requests and editing existing ones.
 *
 * @property requestRepository The repository used to manage Request items.
 */
class EditRequestViewModel(
    private val requestRepository: RequestRepository = RequestRepositoryProvider.repository,
) : ViewModel() {

  // Current request being edited (null for create mode)
  private val _currentRequest = MutableStateFlow<Request?>(null)
  val currentRequest: StateFlow<Request?> = _currentRequest.asStateFlow()

  // Form field states
  private val _title = MutableStateFlow("")
  val title: StateFlow<String> = _title.asStateFlow()

  private val _description = MutableStateFlow("")
  val description: StateFlow<String> = _description.asStateFlow()

  private val _requestTypes = MutableStateFlow<List<RequestType>>(emptyList())
  val requestTypes: StateFlow<List<RequestType>> = _requestTypes.asStateFlow()

  private val _location = MutableStateFlow<Location?>(null)
  val location: StateFlow<Location?> = _location.asStateFlow()

  private val _locationName = MutableStateFlow("")
  val locationName: StateFlow<String> = _locationName.asStateFlow()

  private val _startTimeStamp = MutableStateFlow(Date())
  val startTimeStamp: StateFlow<Date> = _startTimeStamp.asStateFlow()

  private val _expirationTime = MutableStateFlow(Date())
  val expirationTime: StateFlow<Date> = _expirationTime.asStateFlow()

  private val _tags = MutableStateFlow<List<Tags>>(emptyList())
  val tags: StateFlow<List<Tags>> = _tags.asStateFlow()

  private val _people = MutableStateFlow<List<String>>(emptyList())
  val people: StateFlow<List<String>> = _people.asStateFlow()

  // UI State
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _isEditMode = MutableStateFlow(false)
  val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

  /** Load an existing request for editing */
  fun loadRequest(requestId: String) {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      try {
        val request = requestRepository.getRequest(requestId)
        _currentRequest.value = request
        _isEditMode.value = true

        // Populate form fields
        _title.value = request.title
        _description.value = request.description
        _requestTypes.value = request.requestType
        _location.value = request.location
        _locationName.value = request.locationName
        _startTimeStamp.value = request.startTimeStamp
        _expirationTime.value = request.expirationTime
        _tags.value = request.tags
        _people.value = request.people
      } catch (e: Exception) {
        _errorMessage.value = "Failed to load request: ${e.localizedMessage}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Initialize for create mode */
  fun initializeForCreate(creatorId: String) {
    _isEditMode.value = false
    clearFields()
    _people.value = listOf(creatorId) // Creator is automatically added
  }

  /** Update form field values */
  fun updateTitle(value: String) {
    _title.value = value
  }

  fun updateDescription(value: String) {
    _description.value = value
  }

  fun updateRequestTypes(types: List<RequestType>) {
    _requestTypes.value = types
  }

  fun updateLocation(location: Location?) {
    _location.value = location
  }

  fun updateLocationName(name: String) {
    _locationName.value = name
  }

  fun updateStartTimeStamp(date: Date) {
    _startTimeStamp.value = date
  }

  fun updateExpirationTime(date: Date) {
    _expirationTime.value = date
  }

  fun updateTags(tags: List<Tags>) {
    _tags.value = tags
  }

  fun updatePeople(people: List<String>) {
    _people.value = people
  }

  /** Validate form fields */
  private fun validateFields(): String? {
    return when {
      _title.value.isBlank() -> "Title cannot be empty"
      _description.value.isBlank() -> "Description cannot be empty"
      _requestTypes.value.isEmpty() -> "Please select at least one request type"
      _location.value == null -> "Please select a location"
      _locationName.value.isBlank() -> "Location name cannot be empty"
      _expirationTime.value.before(_startTimeStamp.value) ->
          "Expiration time must be after start time"
      else -> null
    }
  }

  /** Save the request (create or update) */
  fun saveRequest(creatorId: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null

      // Validate
      val validationError = validateFields()
      if (validationError != null) {
        _errorMessage.value = validationError
        _isLoading.value = false
        return@launch
      }

      try {
        val request =
            Request(
                requestId = _currentRequest.value?.requestId ?: UUID.randomUUID().toString(),
                title = _title.value,
                description = _description.value,
                requestType = _requestTypes.value,
                location = _location.value!!,
                locationName = _locationName.value,
                status = _currentRequest.value?.status ?: RequestStatus.IN_PROGRESS,
                startTimeStamp = _startTimeStamp.value,
                expirationTime = _expirationTime.value,
                people = _people.value.ifEmpty { listOf(creatorId) },
                tags = _tags.value,
                creatorId = _currentRequest.value?.creatorId ?: creatorId)

        if (_isEditMode.value) {
          requestRepository.updateRequest(request.requestId, request)
        } else {
          requestRepository.addRequest(request)
        }

        onSuccess()
      } catch (e: Exception) {
        _errorMessage.value = "Failed to save request: ${e.localizedMessage}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Clear all form fields */
  private fun clearFields() {
    _currentRequest.value = null
    _title.value = ""
    _description.value = ""
    _requestTypes.value = emptyList()
    _location.value = null
    _locationName.value = ""
    _startTimeStamp.value = Date()
    _expirationTime.value = Date()
    _tags.value = emptyList()
    _people.value = emptyList()
  }

  /** Clear error message */
  fun clearError() {
    _errorMessage.value = null
  }

  /** Reset the ViewModel state */
  fun reset() {
    clearFields()
    _errorMessage.value = null
    _isLoading.value = false
    _isEditMode.value = false
  }
}
