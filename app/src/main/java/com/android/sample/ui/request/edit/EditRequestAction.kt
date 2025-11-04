package com.android.sample.ui.request.edit

import com.android.sample.model.map.Location
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import java.util.Date

/**
 * Actions/callbacks for the Edit Request screen. Groups all callback functions into a single data
 * class.
 */
data class EditRequestActions(
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onRequestTypesChange: (List<RequestType>) -> Unit,
    val onLocationChange: (Location) -> Unit,
    val onLocationNameChange: (String) -> Unit,
    val onStartTimeStampChange: (Date) -> Unit,
    val onExpirationTimeChange: (Date) -> Unit,
    val onTagsChange: (List<Tags>) -> Unit,
    val onSave: () -> Unit,
    val onClearError: () -> Unit,
    val onClearSuccessMessage: () -> Unit,
    val onSearchLocations: (String) -> Unit,
    val onClearLocationSearch: () -> Unit,
    val onDeleteClick: () -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {},
    val onUseCurrentLocation: () -> Unit = {}
)
