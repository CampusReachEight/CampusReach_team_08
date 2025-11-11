package com.android.sample.ui.request.edit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.model.map.FusedLocationProvider
import com.android.sample.model.map.NominatimLocationRepository
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.request.LocationSearchField
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import okhttp3.OkHttpClient

private val SCREEN_CONTENT_PADDING = 16.dp
private val CARD_CONTENT_PADDING = 12.dp
private val SPACING = 8.dp
private val CIRCULAR_PROGRESS_INDICATOR = 20.dp
private val SAVE_BUTTON_PADDING = 24.dp

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
  const val LOCATION_LOADING_SPINNER = "locationLoadingSpinner"
  const val USE_CURRENT_LOCATION_BUTTON = "useCurrentLocationButton"

  /**
   * Returns a stable test tag for the given [RequestType] to be used in UI tests.
   *
   * Example: `RequestType.SUPPLY` -> `"request_type_SUPPLY"`.
   *
   * @param type the request type
   * @return the test tag string derived from [type.name]
   */
  fun getTestTagForRequestType(type: RequestType): String = "request_type_${type.name}"

  /**
   * Returns a stable test tag for the given [Tags] value to be used in UI tests.
   *
   * Example: `Tags.URGENT` -> `"request_tags_URGENT"`.
   *
   * @param tag the tag enum value
   * @return the test tag string derived from [tag.name]
   */
  fun getTestTagForRequestTags(tag: Tags): String = "request_tags_${tag.name}"
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
                    locationRepository = NominatimLocationRepository(OkHttpClient()),
                    locationProvider = FusedLocationProvider(LocalContext.current)))
) {
  val context = LocalContext.current
  // Permission launcher for location permissions
  val permissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
          permissions ->
        when {
          permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
              permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
            if (isLocationEnabled(context)) {
              viewModel.getCurrentLocation()
            } else {
              viewModel.setLocationPermissionError()
            }
          }
          else -> {
            viewModel.setLocationPermissionError()
          }
        }
      }
  val isEditMode = requestId != null
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
          onClearLocationSearch = viewModel::clearLocationSearch,
          onDeleteClick = viewModel::confirmDelete,
          onConfirmDelete = { viewModel.deleteRequest(requestId ?: "") { onNavigateBack() } },
          onCancelDelete = viewModel::cancelDelete,
          onUseCurrentLocation = {
            handleLocationPermissionCheck(context, viewModel, permissionLauncher)
          })
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  stringResource(
                      if (isEditMode) R.string.edit_request_title
                      else R.string.create_request_title))
            },
            navigationIcon = {
              IconButton(
                  onClick = onNavigateBack,
                  modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel_button))
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
    actions: EditRequestActions
) {
  val pickerState =
      remember(actions.onStartTimeStampChange, actions.onExpirationTimeChange) {
        DateTimePickerState(
            onStartDateTimeChange = actions.onStartTimeStampChange,
            onExpirationDateTimeChange = actions.onExpirationTimeChange)
      }

  val dateFormat = remember { SimpleDateFormat(DateFormats.DATE_TIME_FORMAT, Locale.getDefault()) }
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(paddingValues)
              .padding(SCREEN_CONTENT_PADDING)
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(SCREEN_CONTENT_PADDING)) {
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
        Button(
            onClick = actions.onUseCurrentLocation,
            modifier =
                Modifier.fillMaxWidth()
                    .testTag(EditRequestScreenTestTags.USE_CURRENT_LOCATION_BUTTON),
            enabled = !uiState.isLoading && !uiState.isSearchingLocation) {
              if (uiState.isSearchingLocation) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(CIRCULAR_PROGRESS_INDICATOR)
                            .testTag(EditRequestScreenTestTags.LOCATION_LOADING_SPINNER))
              } else {
                Text(stringResource(R.string.use_current_location_button))
              }
            }
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
        Spacer(modifier = Modifier.height(SCREEN_CONTENT_PADDING))
        StartDateField(
            dateString = dateFormat.format(uiState.startTimeStamp),
            isLoading = uiState.isLoading,
            onClick = { pickerState.showStartDatePicker = true })
        ExpirationDateField(
            dateString = dateFormat.format(uiState.expirationTime),
            showDateOrderError = uiState.validationState.showDateOrderError,
            isLoading = uiState.isLoading,
            onClick = { pickerState.showExpirationDatePicker = true })
        TagsSection(
            tags = uiState.tags, isLoading = uiState.isLoading, onTagsChange = actions.onTagsChange)
        Spacer(modifier = Modifier.height(SPACING))
        SaveButton(
            isEditMode = uiState.isEditMode, isLoading = uiState.isLoading, onSave = actions.onSave)
        if (uiState.isEditMode) {
          DeleteButton(isDeleting = uiState.isDeleting, onDeleteClick = actions.onDeleteClick)
        }
        DeleteConfirmationDialog(
            showDialog = uiState.showDeleteConfirmation,
            isDeleting = uiState.isDeleting,
            onConfirmDelete = actions.onConfirmDelete,
            onCancelDelete = actions.onCancelDelete)
      }
  if (pickerState.showStartDatePicker) {
    MaterialDatePickerDialog(
        onDateSelected = pickerState::handleStartDateSelected,
        onDismiss = pickerState::handleStartDateDismiss,
        initialDate = uiState.startTimeStamp)
  }

  if (pickerState.showStartTimePicker) {
    MaterialTimePickerDialog(
        onTimeSelected = pickerState::handleStartTimeSelected,
        onDismiss = pickerState::handleStartTimeDismiss)
  }

  if (pickerState.showExpirationDatePicker) {
    MaterialDatePickerDialog(
        onDateSelected = pickerState::handleExpirationDateSelected,
        onDismiss = pickerState::handleExpirationDateDismiss,
        initialDate = uiState.expirationTime)
  }

  if (pickerState.showExpirationTimePicker) {
    MaterialTimePickerDialog(
        onTimeSelected = pickerState::handleExpirationTimeSelected,
        onDismiss = pickerState::handleExpirationTimeDismiss)
  }
}

@Composable
private fun ErrorMessageCard(errorMessage: String?, onClearError: () -> Unit) {
  errorMessage?.let { error ->
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(CARD_CONTENT_PADDING),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onClearError) { Text(stringResource(R.string.dismiss_button)) }
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
              modifier = Modifier.fillMaxWidth().padding(CARD_CONTENT_PADDING),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        stringResource(
                            if (isEditMode) R.string.success_message_edit
                            else R.string.success_message_create),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss_button)) }
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
      label = { Text(stringResource(R.string.title_field_label)) },
      placeholder = { Text(stringResource(R.string.title_field_placeholder)) },
      isError = showError,
      supportingText = {
        if (showError) {
          Text(
              text = stringResource(R.string.title_error_empty),
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
      label = { Text(stringResource(R.string.description_field_label)) },
      placeholder = { Text(stringResource(R.string.description_field_placeholder)) },
      isError = showError,
      supportingText = {
        if (showError) {
          Text(
              text = stringResource(R.string.description_error_empty),
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
  Text(stringResource(R.string.request_types_label), style = MaterialTheme.typography.labelLarge)
  RequestTypeChipGroup(
      selectedTypes = requestTypes, onSelectionChanged = onRequestTypesChange, enabled = !isLoading)
  if (showError) {
    Text(
        text = stringResource(R.string.request_types_error_empty),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
  }
}

/**
 * Checks if location services are enabled on the device.
 *
 * @param context The context to access system services.
 * @return True if location services are enabled, false otherwise.
 */
internal fun isLocationEnabled(context: Context): Boolean {
  val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

/**
 * Handles location permission check and requests current location if granted.
 *
 * @param context The context to access system services.
 * @param viewModel The EditRequestViewModel to update location state.
 * @param permissionLauncher The launcher to request location permissions.
 */
private fun handleLocationPermissionCheck(
    context: Context,
    viewModel: EditRequestViewModel,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
  val hasFineLocation =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
  val hasCoarseLocation =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
  if (hasFineLocation || hasCoarseLocation) {
    if (isLocationEnabled(context)) {
      viewModel.getCurrentLocation()
    } else {
      viewModel.setLocationPermissionError()
    }
  } else {
    permissionLauncher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
  }
}

@Composable
/**
 * Start date field with format error handling.
 *
 * @param dateString The current start date string.
 * @param showError Whether to show a format error.
 * @param isLoading Whether the field is in a loading state. composable.
 */
private fun StartDateField(dateString: String, isLoading: Boolean, onClick: () -> Unit) {
  Box {
    OutlinedTextField(
        value = dateString,
        onValueChange = {},
        label = { Text(stringResource(R.string.start_date_field_label)) },
        placeholder = { Text(DateFormats.DATE_TIME_FORMAT) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_START_DATE),
        enabled = !isLoading)

    Box(modifier = Modifier.matchParentSize().clickable(enabled = !isLoading) { onClick() })
  }
}

@Composable
/**
 * Expiration date field with additional date order error handling.
 *
 * @param dateString The current expiration date string.
 * @param showError Whether to show a format error.
 * @param showDateOrderError Whether to show a date order error.
 * @param isLoading Whether the field is in a loading state. composable.
 */
private fun ExpirationDateField(
    dateString: String,
    showDateOrderError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
  Box {
    OutlinedTextField(
        value = dateString,
        onValueChange = {},
        label = { Text(stringResource(R.string.expiration_date_field_label)) },
        placeholder = { Text(DateFormats.DATE_TIME_FORMAT) },
        readOnly = true,
        isError = showDateOrderError,
        supportingText = {
          if (showDateOrderError) {
            Text(
                text = stringResource(R.string.date_order_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(EditRequestScreenTestTags.ERROR_MESSAGE))
          }
        },
        modifier = Modifier.fillMaxWidth().testTag(EditRequestScreenTestTags.INPUT_EXPIRATION_DATE),
        enabled = !isLoading)

    Box(modifier = Modifier.matchParentSize().clickable(enabled = !isLoading) { onClick() })
  }
}

@Composable
private fun TagsSection(tags: List<Tags>, isLoading: Boolean, onTagsChange: (List<Tags>) -> Unit) {
  Text(stringResource(R.string.tags_label), style = MaterialTheme.typography.labelLarge)
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
              modifier = Modifier.size(SAVE_BUTTON_PADDING),
              color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text(
              stringResource(
                  if (isEditMode) R.string.save_button_edit else R.string.save_button_create))
        }
      }
}
/**
 * Combines a date with a specific time, zeroing out seconds and milliseconds.
 *
 * @param date The base date
 * @param hour Hour of day (0-23)
 * @param minute Minute (0-59)
 * @return New date with the specified time and zeroed seconds/milliseconds
 */
internal fun combineDateAndTime(date: Date, hour: Int, minute: Int): Date {
  return Calendar.getInstance()
      .apply {
        time = date
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      .time
}
/** Multi-select chip group for Request Types. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestTypeChipGroup(
    selectedTypes: List<RequestType>,
    onSelectionChanged: (List<RequestType>) -> Unit,
    enabled: Boolean = true
) {
  FlowRow(
      horizontalArrangement = Arrangement.spacedBy(SPACING), modifier = Modifier.fillMaxWidth()) {
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
              enabled = enabled,
              modifier = Modifier.testTag(EditRequestScreenTestTags.getTestTagForRequestType(type)))
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
      horizontalArrangement = Arrangement.spacedBy(SPACING),
      verticalArrangement = Arrangement.spacedBy(SPACING),
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
              enabled = enabled,
              modifier = Modifier.testTag(EditRequestScreenTestTags.getTestTagForRequestTags(tag)))
        }
      }
}
