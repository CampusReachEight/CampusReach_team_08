// kotlin
package com.android.sample.ui.request.edit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import com.android.sample.ui.getTextFieldColors
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette
import java.util.Date
import org.jetbrains.annotations.VisibleForTesting

private fun colorSchemeFromPalette(palette: AppPalette, base: ColorScheme): ColorScheme {
  return base.copy(
      primary = palette.accent,
      onPrimary = palette.onAccent,
      background = palette.background,
      onBackground = palette.onBackground,
      surface = palette.surface,
      onSurface = palette.onSurface,
      secondary = palette.secondary,
      onSecondary = palette.onSurface,
      error = palette.error,
      onError = palette.onErrorContainer)
}

@VisibleForTesting
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    initialDate: Date? = null
) {
  val palette = appPalette()
  val baseScheme = MaterialTheme.colorScheme
  val themeScheme = colorSchemeFromPalette(palette, baseScheme)

  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate?.time)

  MaterialTheme(colorScheme = themeScheme) {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        colors =
            DatePickerDefaults.colors(
                containerColor = palette.surface,
                titleContentColor = palette.onSurface,
                headlineContentColor = palette.onSurface,
                weekdayContentColor = palette.onSurface,
                navigationContentColor = palette.onSurface,
                yearContentColor = palette.onSurface,
                disabledYearContentColor = palette.onSurface.copy(alpha = 0.38f),
                currentYearContentColor = palette.accent,
                selectedYearContentColor = palette.onAccent,
                disabledSelectedYearContentColor = palette.onSurface.copy(alpha = 0.38f),
                selectedYearContainerColor = palette.accent,
                disabledSelectedYearContainerColor = palette.onSurface.copy(alpha = 0.12f),
                dayContentColor = palette.onSurface,
                disabledDayContentColor = palette.onSurface.copy(alpha = 0.38f),
                selectedDayContentColor = palette.onAccent,
                disabledSelectedDayContentColor = palette.onSurface.copy(alpha = 0.38f),
                selectedDayContainerColor = palette.accent,
                disabledSelectedDayContainerColor = palette.onSurface.copy(alpha = 0.12f),
                todayContentColor = palette.accent,
                todayDateBorderColor = palette.accent,
                dayInSelectionRangeContentColor = palette.onAccent,
                dayInSelectionRangeContainerColor = palette.accent.copy(alpha = 0.12f),
                dividerColor = palette.onSurface.copy(alpha = 0.12f),
                dateTextFieldColors = getTextFieldColors())) {
          DatePicker(state = datePickerState)
        }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTimePickerDialog(
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
  val palette = appPalette()
  val baseScheme = MaterialTheme.colorScheme
  val themeScheme = colorSchemeFromPalette(palette, baseScheme)

  val timePickerState = rememberTimePickerState()

  MaterialTheme(colorScheme = themeScheme) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
          TextButton(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
            Text("OK")
          }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
          TimePicker(
              state = timePickerState,
              colors =
                  TimePickerDefaults.colors(
                      clockDialColor = palette.surface,
                      clockDialSelectedContentColor = palette.onAccent,
                      clockDialUnselectedContentColor = palette.onSurface,
                      selectorColor = palette.accent,
                      containerColor = palette.surface,
                      periodSelectorBorderColor = palette.onSurface,
                      periodSelectorSelectedContainerColor = palette.accent,
                      periodSelectorUnselectedContainerColor = palette.surface,
                      periodSelectorSelectedContentColor = palette.onAccent,
                      periodSelectorUnselectedContentColor = palette.onSurface,
                      timeSelectorSelectedContainerColor = palette.accent,
                      timeSelectorUnselectedContainerColor = palette.secondary,
                      timeSelectorSelectedContentColor = palette.onAccent,
                      timeSelectorUnselectedContentColor = palette.onSurface))
        },
        containerColor = palette.surface)
  }
}
