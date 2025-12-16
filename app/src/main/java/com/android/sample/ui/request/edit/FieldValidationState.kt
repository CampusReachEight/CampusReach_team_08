package com.android.sample.ui.request.edit

/** Validation state for form fields in the Edit Request screen. */
data class FieldValidationState(
    val showTitleError: Boolean = false,
    val showDescriptionError: Boolean = false,
    val showRequestTypeError: Boolean = false,
    val showLocationNameError: Boolean = false,
    val dateOrderError: DateOrderError = DateOrderError.None,
    val showSuccessMessage: Boolean = false
)

sealed class DateOrderError {
    object None : DateOrderError()
    object ExpirationBeforeStart : DateOrderError()
    object ExpirationBeforeNow : DateOrderError()
}
