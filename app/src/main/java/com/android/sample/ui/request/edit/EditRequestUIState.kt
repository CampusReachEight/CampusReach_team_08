package com.android.sample.ui.request.edit

import com.android.sample.model.map.Location
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.util.Date

/**
 * UI state for the Edit Request screen. Consolidates all form fields and UI state into a single
 * data class.
 */
data class EditRequestUiState(
    // Form fields
    val title: String = "",
    val description: String = "",
    val requestTypes: List<RequestType> = emptyList(),
    val location: Location? = null,
    val locationName: String = "",
    // Default to future times so new requests start as OPEN (not COMPLETED)
    val startTimeStamp: Date = Date(System.currentTimeMillis() + ConstantRequestEdit.ONE_HOUR_MS),
    val expirationTime: Date = Date(System.currentTimeMillis() + ConstantRequestEdit.TWO_HOURS_MS),
    val tags: List<Tags> = emptyList(),

    // UI state
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Validation state
    val validationState: FieldValidationState = FieldValidationState(),

    // Location search
    val locationSearchResults: List<Location> = emptyList(),
    val isSearchingLocation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false
)
