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

  fun handleStartDateSelected(selectedDate: Date) {
    tempSelectedDate = selectedDate
    showStartDatePicker = false
    showStartTimePicker = true
  }

  fun handleStartDateDismiss() {
    showStartDatePicker = false
  }

  fun handleStartTimeSelected(hour: Int, minute: Int) {
    tempSelectedDate?.let { date ->
      val combinedDate = combineDateAndTime(date, hour, minute)
      onStartDateTimeChange(combinedDate)
    }
    showStartTimePicker = false
    tempSelectedDate = null
  }

  fun handleStartTimeDismiss() {
    showStartTimePicker = false
    tempSelectedDate = null
  }

  fun handleExpirationDateSelected(selectedDate: Date) {
    tempSelectedDate = selectedDate
    showExpirationDatePicker = false
    showExpirationTimePicker = true
  }

  fun handleExpirationDateDismiss() {
    showExpirationDatePicker = false
  }

  fun handleExpirationTimeSelected(hour: Int, minute: Int) {
    tempSelectedDate?.let { date ->
      val combinedDate = combineDateAndTime(date, hour, minute)
      onExpirationDateTimeChange(combinedDate)
    }
    showExpirationTimePicker = false
    tempSelectedDate = null
  }

  fun handleExpirationTimeDismiss() {
    showExpirationTimePicker = false
    tempSelectedDate = null
  }
}
