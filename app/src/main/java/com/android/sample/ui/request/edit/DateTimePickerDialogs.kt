/**
 * Dialog helpers for selecting date and time using Material3 pickers.
 *
 * These composables provide simple dialog-based wrappers around the Material3 `DatePicker` and
 * `TimePicker` components used in the edit-request UI.
 *
 * The functions are marked internal because they are UI helpers scoped to the module.
 */
package com.android.sample.ui.request.edit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.util.Date
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Shows a Material3 date picker dialog and returns the selected date.
 *
 * The dialog hosts a `DatePicker` and exposes two callbacks:
 * - `onDateSelected` is invoked with a `java.util.Date` when the user confirms a selection.
 * - `onDismiss` is invoked when the dialog is dismissed or the dismiss button is pressed.
 *
 * The currently selected date is read from `rememberDatePickerState()` and converted from
 * milliseconds to `Date` when confirmed. If no date is selected, the confirm action is a no-op.
 *
 * @param onDateSelected Callback invoked with the selected `Date` when the user confirms.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
internal fun MaterialDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    initialDate: Date? = null
) {
  val datePickerState = rememberDatePickerState()

  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
            onClick = {
              datePickerState.selectedDateMillis?.let { millis -> onDateSelected(Date(millis)) }
            }) {
              Text("OK")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) {
        DatePicker(state = datePickerState)
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Shows a Material3 time picker dialog and returns the selected hour and minute.
 *
 * The dialog hosts a `TimePicker` and exposes two callbacks:
 * - `onTimeSelected` is invoked with `(hour, minute)` when the user confirms a selection.
 * - `onDismiss` is invoked when the dialog is dismissed or the dismiss button is pressed.
 *
 * The currently selected time is read from `rememberTimePickerState()` and passed to the
 * `onTimeSelected` callback when the confirm button is pressed.
 *
 * @param onTimeSelected Callback invoked with the chosen hour and minute when the user confirms.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
internal fun MaterialTimePickerDialog(
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
  val timePickerState = rememberTimePickerState()

  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
          Text("OK")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
      text = { TimePicker(state = timePickerState) })
}
