package com.android.sample.ui.request
// file for the composable of the edit request screen in the app
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.map.Location
import com.android.sample.model.map.NominatimLocationRepository
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.navigation.NavigationTestTags
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.OkHttpClient

/** Test tags for UI elements in EditRequestScreen. Used for UI testing and automation. */
object EditRequestScreenTestTags {
  const val INPUT_TITLE = "inputTitle"
  const val INPUT_DESCRIPTION = "inputDescription"
  const val INPUT_LOCATION_NAME = "inputLocationName"
  const val INPUT_START_DATE = "inputStartDate"
  const val INPUT_EXPIRATION_DATE = "inputExpirationDate"
  const val SAVE_BUTTON = "saveButton"
  const val ERROR_MESSAGE = "errorMessage"
  const val LOCATION_SEARCH = "locationSearch"
}

/**
 * Main screen for editing or creating a request.
 *
 * @param requestId If not null, loads and edits an existing request.
 * @param creatorId The ID of the request creator.
 * @param onNavigateBack Callback for navigation when user cancels or saves.
 * @param viewModel The ViewModel managing request state.
 * @param verbose If true, enables verbose logging for debugging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRequestScreen(
    requestId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: EditRequestViewModel =
        viewModel(
            factory =
                EditRequestViewModelFactory(
                    locationRepository = NominatimLocationRepository(OkHttpClient())))
) {
  val isEditMode = requestId != null

  // Load request data or initialize for creation
  LaunchedEffect(requestId) {
    if (requestId != null) {
      viewModel.loadRequest(requestId)
    } else {
      viewModel.initializeForCreate(Firebase.auth.currentUser?.uid ?: "")
    }
  }

  val navigationTag: String =
      if (isEditMode) {
        NavigationTestTags.EDIT_REQUEST_SCREEN
      } else {
        NavigationTestTags.ADD_REQUEST_SCREEN
      }

  // Collect ViewModel state
  val title by viewModel.title.collectAsState()
  val description by viewModel.description.collectAsState()
  val requestTypes by viewModel.requestTypes.collectAsState()
  val location by viewModel.location.collectAsState()
  val locationName by viewModel.locationName.collectAsState()
  val startTimeStamp by viewModel.startTimeStamp.collectAsState()
  val expirationTime by viewModel.expirationTime.collectAsState()
  val tags by viewModel.tags.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()
  val locationSearchResults by viewModel.locationSearchResults.collectAsState()
  val isSearchingLocation by viewModel.isSearchingLocation.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(if (isEditMode) "Edit Request" else "Create Request") },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
              }
            })
      },
      modifier = Modifier.testTag(navigationTag)) { paddingValues ->
        EditRequestContent(
            paddingValues = paddingValues,
            title = title,
            description = description,
            requestTypes = requestTypes,
            location = location,
            locationName = locationName,
            startTimeStamp = startTimeStamp,
            expirationTime = expirationTime,
            tags = tags,
            isEditMode = isEditMode,
            isLoading = isLoading,
            errorMessage = errorMessage,
            locationSearchResults = locationSearchResults,
            isSearchingLocation = isSearchingLocation,
            onTitleChange = { viewModel.updateTitle(it) },
            onDescriptionChange = { viewModel.updateDescription(it) },
            onRequestTypesChange = { viewModel.updateRequestTypes(it) },
            onLocationChange = { viewModel.updateLocation(it) },
            onLocationNameChange = { viewModel.updateLocationName(it) },
            onStartTimeStampChange = { viewModel.updateStartTimeStamp(it) },
            onExpirationTimeChange = { viewModel.updateExpirationTime(it) },
            onTagsChange = { viewModel.updateTags(it) },
            onSave = {
              viewModel.saveRequest(Firebase.auth.currentUser?.uid ?: "") { onNavigateBack() }
            },
            onClearError = { viewModel.clearError() },
            onSearchLocations = { viewModel.searchLocations(it) },
            onClearLocationSearch = { viewModel.clearLocationSearch() })
      }
}

/**
 * Content for the EditRequestScreen. Handles all input fields, validation, and save logic.
 *
 * @param paddingValues Padding from Scaffold.
 * @param title Request title.
 * @param description Request description.
 * @param requestTypes Selected request types.
 * @param location Selected location object.
 * @param locationName Name of the location.
 * @param startTimeStamp Start date and time.
 * @param expirationTime Expiration date and time.
 * @param tags Selected tags.
 * @param isEditMode True if editing, false if creating.
 * @param isLoading True if loading or saving.
 * @param errorMessage Error message to display.
 * @param onTitleChange Callback for title change.
 * @param onDescriptionChange Callback for description change.
 * @param onRequestTypesChange Callback for request types change.
 * @param onLocationChange Callback for location change.
 * @param onLocationNameChange Callback for location name change.
 * @param onStartTimeStampChange Callback for start date change.
 * @param onExpirationTimeChange Callback for expiration date change.
 * @param onTagsChange Callback for tags change.
 * @param onSave Callback for save action.
 * @param onClearError Callback to clear error message.
 */
@Composable
fun EditRequestContent(
    paddingValues: PaddingValues,
    title: String,
    description: String,
    requestTypes: List<RequestType>,
    location: Location?,
    locationName: String,
    startTimeStamp: Date,
    expirationTime: Date,
    tags: List<Tags>,
    isEditMode: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRequestTypesChange: (List<RequestType>) -> Unit,
    onLocationChange: (Location) -> Unit,
    onLocationNameChange: (String) -> Unit,
    onStartTimeStampChange: (Date) -> Unit,
    onExpirationTimeChange: (Date) -> Unit,
    onTagsChange: (List<Tags>) -> Unit,
    onSave: () -> Unit,
    onClearError: () -> Unit,
    locationSearchResults: List<Location>,
    isSearchingLocation: Boolean,
    onSearchLocations: (String) -> Unit,
    onClearLocationSearch: () -> Unit,
    verbose: Boolean = false
) {
  // Local validation states for each field
  var showTitleError by remember { mutableStateOf(false) }
  var showDescriptionError by remember { mutableStateOf(false) }
  var showRequestTypeError by remember { mutableStateOf(false) }
  var showLocationNameError by remember { mutableStateOf(false) }
  var showStartDateError by remember { mutableStateOf(false) }
  var showExpirationDateError by remember { mutableStateOf(false) }
  var showDateOrderError by remember { mutableStateOf(false) }
  var showSuccessMessage by remember { mutableStateOf(false) }

  // Date formatters and state
  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
  var startDateString by remember { mutableStateOf(dateFormat.format(startTimeStamp)) }
  var expirationDateString by remember { mutableStateOf(dateFormat.format(expirationTime)) }

  // Location search state

  // Update local states when props change
  LaunchedEffect(startTimeStamp) { startDateString = dateFormat.format(startTimeStamp) }

  /** Validates date string format and optionally checks if date is in the past. */
  fun isValidDate(dateString: String, allowPast: Boolean = false): Boolean {
    if (dateString.isBlank()) return false
    return try {
      val parsedDate = dateFormat.parse(dateString) ?: return false

      if (!allowPast) {
        val currentDate = Date()
        parsedDate.after(currentDate) || parsedDate == currentDate
      } else {
        true
      }
    } catch (e: Exception) {
      false
    }
  }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(paddingValues)
              .padding(16.dp)
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Show global error message from ViewModel
        errorMessage?.let { error ->
          Card(
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer),
              modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          text = error,
                          color = MaterialTheme.colorScheme.onErrorContainer,
                          style = MaterialTheme.typography.bodyMedium,
                          modifier = Modifier.weight(1f))
                      TextButton(onClick = onClearError) { Text("Dismiss") }
                    }
              }
        }
        if (showSuccessMessage) {
          Card(
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.primaryContainer),
              modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          text =
                              if (isEditMode) "Request updated successfully!"
                              else "Request created successfully!",
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          style = MaterialTheme.typography.bodyMedium,
                          modifier = Modifier.weight(1f))
                      TextButton(onClick = { showSuccessMessage = false }) { Text("Dismiss") }
                    }
              }
        }

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = {
              onTitleChange(it)
              showTitleError = it.isBlank()
            },
            label = { Text("Title") },
            placeholder = { Text("Name your request") },
            isError = showTitleError,
            supportingText = {
              if (showTitleError) {
                Text(
                    text = "Title cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_TITLE),
            enabled = !isLoading)

        // Description Field
        OutlinedTextField(
            value = description,
            onValueChange = {
              onDescriptionChange(it)
              showDescriptionError = it.isBlank()
            },
            label = { Text("Description") },
            placeholder = { Text("Describe your request") },
            isError = showDescriptionError,
            supportingText = {
              if (showDescriptionError) {
                Text(
                    text = "Description cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
              }
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_DESCRIPTION),
            enabled = !isLoading)

        // Request Type Selection
        Text("Request Types *", style = MaterialTheme.typography.labelLarge)
        RequestTypeChipGroup(
            selectedTypes = requestTypes,
            onSelectionChanged = {
              onRequestTypesChange(it)
              showRequestTypeError = it.isEmpty()
            },
            enabled = !isLoading)
        if (showRequestTypeError) {
          Text(
              text = "Please select at least one request type",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
        }

        // location search field
        // separate class
        LocationSearchField(
            locationName = locationName,
            location = location,
            searchResults = locationSearchResults,
            isSearching = isSearchingLocation,
            isError = showLocationNameError,
            errorMessage = "Location name cannot be empty",
            enabled = !isLoading,
            onLocationNameChange = onLocationNameChange,
            onLocationSelected = onLocationChange,
            onSearchQueryChange = onSearchLocations,
            onClearSearch = onClearLocationSearch,
            modifier =
                Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_LOCATION_NAME))

        Spacer(modifier = Modifier.height(16.dp))

        // Start Date Field
        OutlinedTextField(
            value = startDateString,
            onValueChange = {
              startDateString = it
              val isValid = isValidDate(it)
              showStartDateError = it.isNotBlank() && !isValid
              if (isValid) {
                try {
                  val parsedDate = dateFormat.parse(it)
                  if (parsedDate != null) {
                    onStartTimeStampChange(parsedDate)
                  }
                } catch (e: Exception) {
                  if (verbose) {
                    Log.d("EditRequest", "Error parsing start date: ${e.localizedMessage}")
                  }
                }
              }
            },
            label = { Text("Start Date & Time") },
            placeholder = { Text("dd/MM/yyyy HH:mm") },
            isError = showStartDateError,
            supportingText = {
              if (showStartDateError) {
                Text(
                    text = "Invalid format (must be dd/MM/yyyy HH:mm)",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_START_DATE),
            enabled = !isLoading)

        // Expiration Date Field
        OutlinedTextField(
            value = expirationDateString,
            onValueChange = {
              expirationDateString = it
              val isValid = isValidDate(it, true)
              showExpirationDateError = it.isNotBlank() && !isValid

              // Clear date order error if format is invalid
              if (!isValid) {
                showDateOrderError = false
              }

              // Check date order only if format is valid
              if (isValid) {
                showExpirationDateError = false
                try {
                  val parsedDate = dateFormat.parse(it)
                  if (parsedDate != null) {
                    onExpirationTimeChange(parsedDate)

                    // Check if expiration is after start date
                    if (parsedDate.before(startTimeStamp)) {
                      showDateOrderError = true
                    } else {
                      showDateOrderError = false
                    }
                  }
                } catch (e: Exception) {
                  // Handle parsing error
                }
              }
            },
            label = { Text("Expiration Date & Time") },
            placeholder = { Text("dd/MM/yyyy HH:mm") },
            isError = showExpirationDateError || showDateOrderError,
            supportingText = {
              when {
                showExpirationDateError -> {
                  Text(
                      text = "Invalid format (must be dd/MM/yyyy HH:mm)",
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
                }
                showDateOrderError -> {
                  Text(
                      text = "Expiration date must be after start date",
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
                }
              }
            },
            modifier =
                Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE),
            enabled = !isLoading)

        // Tags Selection
        Text("Tags (Optional)", style = MaterialTheme.typography.labelLarge)
        TagsChipGroup(selectedTags = tags, onSelectionChanged = onTagsChange, enabled = !isLoading)

        Spacer(modifier = Modifier.height(8.dp))

        // Save/Create Button
        Button(
            onClick = {
              // Validate all fields before saving
              val isTitleValid = title.isNotBlank()
              val isDescriptionValid = description.isNotBlank()
              val isRequestTypeValid = requestTypes.isNotEmpty()
              val isLocationValid = location != null
              val isLocationNameValid = locationName.isNotBlank()
              val isStartDateValid = startDateString.isNotBlank()
              val isExpirationDateValid = expirationDateString.isNotBlank()

              // ← ADD: Date order validation
              val isDateOrderValid = !expirationTime.before(startTimeStamp)

              Log.d("EditRequest", "=== Validation Check ===")
              Log.d("EditRequest", "title: $isTitleValid (value: '$title')")
              Log.d("EditRequest", "description: $isDescriptionValid (value: '$description')")
              Log.d("EditRequest", "requestTypes: $isRequestTypeValid (value: $requestTypes)")
              Log.d("EditRequest", "locationName: $isLocationNameValid (value: '$locationName')")
              Log.d("EditRequest", "startDate: $isStartDateValid (value: '$startDateString')")
              Log.d(
                  "EditRequest",
                  "expirationDate: $isExpirationDateValid (value: '$expirationDateString')")
              Log.d("EditRequest", "dateOrder: $isDateOrderValid") // ← ADD THIS

              showTitleError = !isTitleValid
              showDescriptionError = !isDescriptionValid
              showRequestTypeError = !isRequestTypeValid
              showLocationNameError = !isLocationNameValid
              showStartDateError = !isStartDateValid
              showExpirationDateError = !isExpirationDateValid
              showDateOrderError = !isDateOrderValid // ← ADD THIS

              if (isTitleValid &&
                  isDescriptionValid &&
                  isRequestTypeValid &&
                  isLocationValid &&
                  isLocationNameValid &&
                  isStartDateValid &&
                  isExpirationDateValid &&
                  isDateOrderValid) {

                showSuccessMessage = true
                onSave()
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.SAVE_BUTTON),
            enabled = !isLoading) {
              if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
              } else {
                Text(if (isEditMode) "Update Request" else "Create Request")
              }
            }
      }
}

/**
 * Multi-select chip group for Request Types.
 *
 * Displays all available RequestType enum values as FilterChips in a flowing layout that wraps to
 * multiple lines. Users can select/deselect multiple types.
 *
 * @param selectedTypes List of currently selected RequestType values
 * @param onSelectionChanged Callback invoked when selection changes, receives the new list
 * @param enabled Whether the chips are interactive (default: true)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestTypeChipGroup(
    selectedTypes: List<RequestType>,
    onSelectionChanged: (List<RequestType>) -> Unit,
    enabled: Boolean = true
) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    RequestType.entries.forEach { type ->
      FilterChip(
          selected = selectedTypes.contains(type),
          onClick = {
            val newSelection =
                if (selectedTypes.contains(type)) {
                  selectedTypes - type
                } else {
                  selectedTypes + type
                }
            onSelectionChanged(newSelection)
          },
          label = { Text(type.name.replace("_", " ")) },
          enabled = enabled)
    }
  }
}

/**
 * Multi-select chip group for Tags.
 *
 * Displays all available Tags enum values as FilterChips in a flowing layout. Similar to
 * RequestTypeChipGroup but for optional tags like URGENT, EASY, etc.
 *
 * **What it does:**
 * - Shows chips like: [URGENT] [EASY] [OUTDOOR] [INDOOR]...
 * - Selected chips are highlighted
 * - Clicking toggles selection on/off
 * - Multiple tags can be selected simultaneously
 *
 * @param selectedTags List of currently selected Tags values
 * @param onSelectionChanged Callback invoked when selection changes, receives the new list
 * @param enabled Whether the chips are interactive (default: true)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsChipGroup(
    selectedTags: List<Tags>,
    onSelectionChanged: (List<Tags>) -> Unit,
    enabled: Boolean = true
) {
  FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()) {
        Tags.entries.forEach { tag ->
          FilterChip(
              selected = selectedTags.contains(tag),
              onClick = {
                val newSelection =
                    if (selectedTags.contains(tag)) {
                      selectedTags - tag
                    } else {
                      selectedTags + tag
                    }
                onSelectionChanged(newSelection)
              },
              label = { Text(tag.name.replace("_", " ")) },
              enabled = enabled)
        }
      }
}
