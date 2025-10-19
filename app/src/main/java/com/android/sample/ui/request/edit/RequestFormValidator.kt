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
    private val startDateString: String,
    private val expirationDateString: String,
    private val startTimeStamp: Date,
    private val expirationTime: Date
) {
  fun validate(): FieldValidationState {
    val isTitleValid = title.isNotBlank()
    val isDescriptionValid = description.isNotBlank()
    val isRequestTypeValid = requestTypes.isNotEmpty()
    val isLocationValid = location != null
    val isLocationNameValid = locationName.isNotBlank()
    val isStartDateValid = startDateString.isNotBlank()
    val isExpirationDateValid = expirationDateString.isNotBlank()
    val isDateOrderValid = !expirationTime.before(startTimeStamp)

    return FieldValidationState(
        showTitleError = !isTitleValid,
        showDescriptionError = !isDescriptionValid,
        showRequestTypeError = !isRequestTypeValid,
        showLocationNameError = !isLocationNameValid,
        showStartDateError = !isStartDateValid,
        showExpirationDateError = !isExpirationDateValid,
        showDateOrderError = !isDateOrderValid)
  }

  fun isValid(): Boolean {
    val state = validate()
    return !state.showTitleError &&
        !state.showDescriptionError &&
        !state.showRequestTypeError &&
        !state.showLocationNameError &&
        !state.showStartDateError &&
        !state.showExpirationDateError &&
        !state.showDateOrderError
  }
}
