package com.android.sample.ui.request.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Date

/**
 * Manages state for date and time pickers. Eliminates the need for lambdas by encapsulating all
 * state updates.
 */
internal class DateTimePickerState(
    private val onStartDateTimeChange: (Date) -> Unit,
    private val onExpirationDateTimeChange: (Date) -> Unit
) {
  var showStartDatePicker by mutableStateOf(false)
  var showStartTimePicker by mutableStateOf(false)
  var showExpirationDatePicker by mutableStateOf(false)
  var showExpirationTimePicker by mutableStateOf(false)
  var tempSelectedDate by mutableStateOf<Date?>(null)

  /**
   * Called when the user selects a start date from the date picker.
   *
   * Stores the selected date temporarily in [tempSelectedDate], hides the start date picker and
   * opens the start time picker so the user can pick a time to combine with this date.
   *
   * @param selectedDate the date chosen by the user (time component is ignored here).
   */
  fun handleStartDateSelected(selectedDate: Date) {
    tempSelectedDate = selectedDate
    showStartDatePicker = false
    showStartTimePicker = true
  }

  /**
   * Called when the start date picker is dismissed without selection.
   *
   * Hides the start date picker. Does not alter [tempSelectedDate].
   */
  fun handleStartDateDismiss() {
    showStartDatePicker = false
  }

  /**
   * Called when the user selects a time for the previously chosen start date.
   *
   * Combines the stored [tempSelectedDate] with the provided `hour` and `minute` into a single Date
   * and invokes [onStartDateTimeChange] with the combined value. Clears [tempSelectedDate] and
   * hides the start time picker after handling.
   *
   * If [tempSelectedDate] is null this function is a no-op except for hiding the picker and
   * clearing the temp value.
   *
   * @param hour the selected hour of day (0-23).
   * @param minute the selected minute (0-59).
   */
  fun handleStartTimeSelected(hour: Int, minute: Int) {
    tempSelectedDate?.let { date ->
      val combinedDate = combineDateAndTime(date, hour, minute)
      onStartDateTimeChange(combinedDate)
    }
    showStartTimePicker = false
    tempSelectedDate = null
  }

  /**
   * Called when the start time picker is dismissed without selection.
   *
   * Hides the start time picker and clears any temporarily stored date.
   */
  fun handleStartTimeDismiss() {
    showStartTimePicker = false
    tempSelectedDate = null
  }

  /**
   * Called when the user selects an expiration date from the date picker.
   *
   * Stores the selected date temporarily in [tempSelectedDate], hides the expiration date picker
   * and opens the expiration time picker so the user can pick a time to combine with this date.
   *
   * @param selectedDate the date chosen by the user (time component is ignored here).
   */
  fun handleExpirationDateSelected(selectedDate: Date) {
    tempSelectedDate = selectedDate
    showExpirationDatePicker = false
    showExpirationTimePicker = true
  }

  /**
   * Called when the expiration date picker is dismissed without selection.
   *
   * Hides the expiration date picker. Does not alter [tempSelectedDate].
   */
  fun handleExpirationDateDismiss() {
    showExpirationDatePicker = false
  }

  /**
   * Called when the user selects a time for the previously chosen expiration date.
   *
   * Combines the stored [tempSelectedDate] with the provided `hour` and `minute` into a single Date
   * and invokes [onExpirationDateTimeChange] with the combined value. Clears [tempSelectedDate] and
   * hides the expiration time picker after handling.
   *
   * If [tempSelectedDate] is null this function is a no-op except for hiding the picker and
   * clearing the temp value.
   *
   * @param hour the selected hour of day (0-23).
   * @param minute the selected minute (0-59).
   */
  fun handleExpirationTimeSelected(hour: Int, minute: Int) {
    tempSelectedDate?.let { date ->
      val combinedDate = combineDateAndTime(date, hour, minute)
      onExpirationDateTimeChange(combinedDate)
    }
    showExpirationTimePicker = false
    tempSelectedDate = null
  }

  /**
   * Called when the expiration time picker is dismissed without selection.
   *
   * Hides the expiration time picker and clears any temporarily stored date.
   */
  fun handleExpirationTimeDismiss() {
    showExpirationTimePicker = false
    tempSelectedDate = null
  }
}
