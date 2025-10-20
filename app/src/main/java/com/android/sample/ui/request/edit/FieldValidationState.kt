package com.android.sample.ui.request.edit

/** Validation state for form fields in the Edit Request screen. */
data class FieldValidationState(
    val showTitleError: Boolean = false,
    val showDescriptionError: Boolean = false,
    val showRequestTypeError: Boolean = false,
    val showLocationNameError: Boolean = false,
    val showStartDateError: Boolean = false,
    val showExpirationDateError: Boolean = false,
    val showDateOrderError: Boolean = false,
    val showSuccessMessage: Boolean = false
)
