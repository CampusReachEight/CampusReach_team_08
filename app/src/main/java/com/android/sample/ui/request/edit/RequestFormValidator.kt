package com.android.sample.ui.request.edit

import com.android.sample.model.map.Location
import com.android.sample.model.request.RequestType
import java.util.*

/** Validator for the entire request form. Checks all fields and returns validation state. */
class RequestFormValidator(
    private val title: String,
    private val description: String,
    private val requestTypes: List<RequestType>,
    private val location: Location?,
    private val locationName: String,
    private val startTimeStamp: Date,
    private val expirationTime: Date
) {
  fun validate(): FieldValidationState {
    val isTitleValid = title.isNotBlank()
    val isDescriptionValid = description.isNotBlank()
    val isRequestTypeValid = requestTypes.isNotEmpty()
    val isLocationValid = location != null
    val isLocationNameValid = locationName.isNotBlank()

    val currentDate = Date()
    val dateOrderError =
        when {
          expirationTime <= startTimeStamp -> DateOrderError.ExpirationBeforeStart
          expirationTime <= currentDate -> DateOrderError.ExpirationBeforeNow
          else -> DateOrderError.None
        }

    return FieldValidationState(
        showTitleError = !isTitleValid,
        showDescriptionError = !isDescriptionValid,
        showRequestTypeError = !isRequestTypeValid,
        showLocationNameError = !isLocationNameValid && !isLocationValid,
        dateOrderError = dateOrderError)
  }

  fun isValid(): Boolean {
    val state = validate()
    return !state.showTitleError &&
        !state.showDescriptionError &&
        !state.showRequestTypeError &&
        !state.showLocationNameError &&
        state.dateOrderError == DateOrderError.None
  }
}
