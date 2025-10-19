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
    val startTimeStamp: Date = Date(),
    val expirationTime: Date = Date(),
    val tags: List<Tags> = emptyList(),

    // UI state
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Validation state
    val validationState: FieldValidationState = FieldValidationState(),

    // Location search
    val locationSearchResults: List<Location> = emptyList(),
    val isSearchingLocation: Boolean = false
)
