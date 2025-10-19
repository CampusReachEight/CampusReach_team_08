package com.android.sample.ui.request.edit

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
import com.android.sample.model.map.NominatimLocationRepository
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.navigation.NavigationTestTags
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.OkHttpClient

/** Test tags for UI elements in EditRequestScreen. */
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

/** Main screen for editing or creating a request. */
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

  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()

  // Create actions object
  val actions =
      EditRequestActions(
          onTitleChange = viewModel::updateTitle,
          onDescriptionChange = viewModel::updateDescription,
          onRequestTypesChange = viewModel::updateRequestTypes,
          onLocationChange = viewModel::updateLocation,
          onLocationNameChange = viewModel::updateLocationName,
          onStartTimeStampChange = viewModel::updateStartTimeStamp,
          onExpirationTimeChange = viewModel::updateExpirationTime,
          onTagsChange = viewModel::updateTags,
          onSave = {
            viewModel.saveRequest(Firebase.auth.currentUser?.uid ?: "") { onNavigateBack() }
          },
          onClearError = viewModel::clearError,
          onClearSuccessMessage = viewModel::clearSuccessMessage,
          onSearchLocations = viewModel::searchLocations,
          onClearLocationSearch = viewModel::clearLocationSearch)

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
        EditRequestContent(paddingValues = paddingValues, uiState = uiState, actions = actions)
      }
}

/** Content for the EditRequestScreen. Now with only 3 parameters instead of 27! */
@Composable
fun EditRequestContent(
    paddingValues: PaddingValues,
    uiState: EditRequestUiState,
    actions: EditRequestActions,
    verbose: Boolean = false
) {
  val dateFormat = remember { SimpleDateFormat(DateFormats.DATE_TIME_FORMAT, Locale.getDefault()) }
  val dateValidator = remember(dateFormat) { DateValidator(dateFormat) }

  val startDateString =
      remember(uiState.startTimeStamp) { mutableStateOf(dateFormat.format(uiState.startTimeStamp)) }
  val expirationDateString =
      remember(uiState.expirationTime) { mutableStateOf(dateFormat.format(uiState.expirationTime)) }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(paddingValues)
              .padding(16.dp)
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ErrorMessageCard(uiState.errorMessage, actions.onClearError)

        SuccessMessageCard(
            uiState.validationState.showSuccessMessage,
            uiState.isEditMode,
            actions.onClearSuccessMessage)

        TitleField(
            title = uiState.title,
            showError = uiState.validationState.showTitleError,
            isLoading = uiState.isLoading,
            onTitleChange = actions.onTitleChange)

        DescriptionField(
            description = uiState.description,
            showError = uiState.validationState.showDescriptionError,
            isLoading = uiState.isLoading,
            onDescriptionChange = actions.onDescriptionChange)

        RequestTypeSection(
            requestTypes = uiState.requestTypes,
            showError = uiState.validationState.showRequestTypeError,
            isLoading = uiState.isLoading,
            onRequestTypesChange = actions.onRequestTypesChange)

        LocationSearchField(
            locationName = uiState.locationName,
            location = uiState.location,
            searchResults = uiState.locationSearchResults,
            isSearching = uiState.isSearchingLocation,
            isError = uiState.validationState.showLocationNameError,
            errorMessage = "Location name cannot be empty",
            enabled = !uiState.isLoading,
            onLocationNameChange = actions.onLocationNameChange,
            onLocationSelected = actions.onLocationChange,
            onSearchQueryChange = actions.onSearchLocations,
            onClearSearch = actions.onClearLocationSearch,
            modifier =
                Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_LOCATION_NAME))

        Spacer(modifier = Modifier.height(16.dp))

        StartDateField(
            dateString = startDateString.value,
            showError = uiState.validationState.showStartDateError,
            isLoading = uiState.isLoading,
            dateValidator = dateValidator,
            onDateChange = { newDateString ->
              startDateString.value = newDateString
              handleStartDateChange(
                  newDateString, dateValidator, actions.onStartTimeStampChange, verbose)
            })

        ExpirationDateField(
            dateString = expirationDateString.value,
            showError = uiState.validationState.showExpirationDateError,
            showDateOrderError = uiState.validationState.showDateOrderError,
            isLoading = uiState.isLoading,
            dateValidator = dateValidator,
            startTimeStamp = uiState.startTimeStamp,
            onDateChange = { newDateString ->
              expirationDateString.value = newDateString
              handleExpirationDateChange(
                  newDateString,
                  uiState.startTimeStamp,
                  dateValidator,
                  actions.onExpirationTimeChange)
            })

        TagsSection(
            tags = uiState.tags, isLoading = uiState.isLoading, onTagsChange = actions.onTagsChange)

        Spacer(modifier = Modifier.height(8.dp))

        SaveButton(
            isEditMode = uiState.isEditMode, isLoading = uiState.isLoading, onSave = actions.onSave)
      }
}

// Simplified date change handlers (no validation state parameter needed)
private fun handleStartDateChange(
    dateString: String,
    dateValidator: DateValidator,
    onStartTimeStampChange: (Date) -> Unit,
    verbose: Boolean
) {
  val isValid = dateValidator.isValidFormat(dateString)

  if (isValid) {
    dateValidator.parseDate(dateString)?.let { parsedDate -> onStartTimeStampChange(parsedDate) }
        ?: run {
          if (verbose) {
            Log.d("EditRequest", "Error parsing start date")
          }
        }
  }
}

private fun handleExpirationDateChange(
    dateString: String,
    startTimeStamp: Date,
    dateValidator: DateValidator,
    onExpirationTimeChange: (Date) -> Unit
) {
  val isValid = dateValidator.isValidFormat(dateString, allowPast = true)

  if (isValid) {
    dateValidator.parseDate(dateString)?.let { parsedDate -> onExpirationTimeChange(parsedDate) }
  }
}

// Extracted composable components
@Composable
private fun ErrorMessageCard(errorMessage: String?, onClearError: () -> Unit) {
  errorMessage?.let { error ->
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
}

@Composable
private fun SuccessMessageCard(showSuccess: Boolean, isEditMode: Boolean, onDismiss: () -> Unit) {
  if (showSuccess) {
    Card(
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                TextButton(onClick = onDismiss) { Text("Dismiss") }
              }
        }
  }
}

@Composable
private fun TitleField(
    title: String,
    showError: Boolean,
    isLoading: Boolean,
    onTitleChange: (String) -> Unit
) {
  OutlinedTextField(
      value = title,
      onValueChange = onTitleChange,
      label = { Text("Title") },
      placeholder = { Text("Name your request") },
      isError = showError,
      supportingText = {
        if (showError) {
          Text(
              text = "Title cannot be empty",
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
        }
      },
      modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_TITLE),
      enabled = !isLoading)
}

@Composable
private fun DescriptionField(
    description: String,
    showError: Boolean,
    isLoading: Boolean,
    onDescriptionChange: (String) -> Unit
) {
  OutlinedTextField(
      value = description,
      onValueChange = onDescriptionChange,
      label = { Text("Description") },
      placeholder = { Text("Describe your request") },
      isError = showError,
      supportingText = {
        if (showError) {
          Text(
              text = "Description cannot be empty",
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
        }
      },
      minLines = 3,
      modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_DESCRIPTION),
      enabled = !isLoading)
}

@Composable
private fun RequestTypeSection(
    requestTypes: List<RequestType>,
    showError: Boolean,
    isLoading: Boolean,
    onRequestTypesChange: (List<RequestType>) -> Unit
) {
  Text("Request Types *", style = MaterialTheme.typography.labelLarge)
  RequestTypeChipGroup(
      selectedTypes = requestTypes, onSelectionChanged = onRequestTypesChange, enabled = !isLoading)
  if (showError) {
    Text(
        text = "Please select at least one request type",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
  }
}

@Composable
private fun StartDateField(
    dateString: String,
    showError: Boolean,
    isLoading: Boolean,
    dateValidator: DateValidator,
    onDateChange: (String) -> Unit
) {
  OutlinedTextField(
      value = dateString,
      onValueChange = onDateChange,
      label = { Text("Start Date & Time") },
      placeholder = { Text(DateFormats.DATE_TIME_FORMAT) },
      isError = showError,
      supportingText = {
        if (showError) {
          Text(
              text = "Invalid format (must be ${DateFormats.DATE_TIME_FORMAT})",
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
        }
      },
      modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_START_DATE),
      enabled = !isLoading)
}

@Composable
private fun ExpirationDateField(
    dateString: String,
    showError: Boolean,
    showDateOrderError: Boolean,
    isLoading: Boolean,
    dateValidator: DateValidator,
    startTimeStamp: Date,
    onDateChange: (String) -> Unit
) {
  OutlinedTextField(
      value = dateString,
      onValueChange = onDateChange,
      label = { Text("Expiration Date & Time") },
      placeholder = { Text(DateFormats.DATE_TIME_FORMAT) },
      isError = showError || showDateOrderError,
      supportingText = {
        when {
          showError -> {
            Text(
                text = "Invalid format (must be ${DateFormats.DATE_TIME_FORMAT})",
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
      modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE),
      enabled = !isLoading)
}

@Composable
private fun TagsSection(tags: List<Tags>, isLoading: Boolean, onTagsChange: (List<Tags>) -> Unit) {
  Text("Tags (Optional)", style = MaterialTheme.typography.labelLarge)
  TagsChipGroup(selectedTags = tags, onSelectionChanged = onTagsChange, enabled = !isLoading)
}

@Composable
private fun SaveButton(isEditMode: Boolean, isLoading: Boolean, onSave: () -> Unit) {
  Button(
      onClick = onSave,
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

/** Multi-select chip group for Request Types. */
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

/** Multi-select chip group for Tags. */
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
