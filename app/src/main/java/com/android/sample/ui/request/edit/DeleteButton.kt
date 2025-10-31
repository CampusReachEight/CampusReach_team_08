package com.android.sample.ui.request.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.sample.R

// Constants extracted to avoid magic numbers and repeated modifiers.
private val deleteProgressIndicatorSize = 24.dp
private val fullWidthModifier = Modifier.fillMaxWidth()

// Test tags for UI testing
const val DELETE_BUTTON_TEST_TAG = "delete_button"
const val DELETE_PROGRESS_INDICATOR_TEST_TAG = "delete_progress_indicator"
const val DELETE_CONFIRMATION_DIALOG_TEST_TAG = "delete_confirmation_dialog"
const val DELETE_CONFIRM_BUTTON_TEST_TAG = "delete_confirm_button"
const val DELETE_CANCEL_BUTTON_TEST_TAG = "delete_cancel_button"

/**
 * A full-width delete button that shows a progress indicator while a delete operation is in
 * progress.
 *
 * @param isDeleting true when the delete operation is ongoing; disables the button and shows a
 *   spinner.
 * @param onDeleteClick callback invoked when the button is clicked.
 */
@Composable
fun DeleteButton(isDeleting: Boolean, onDeleteClick: () -> Unit) {
  // Button uses the error color to indicate destructive action.
  Button(
      onClick = onDeleteClick,
      modifier = fullWidthModifier.testTag(DELETE_BUTTON_TEST_TAG),
      enabled = !isDeleting,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
        if (isDeleting) {
          // Show a small progress indicator inside the button while deleting.
          CircularProgressIndicator(
              modifier =
                  Modifier.size(deleteProgressIndicatorSize)
                      .testTag(DELETE_PROGRESS_INDICATOR_TEST_TAG),
              color = MaterialTheme.colorScheme.onError)
        } else {
          // Display the localized label for the delete action.
          Text(stringResource(R.string.delete_request_button_delete_request))
        }
      }
}

/**
 * Confirmation dialog shown before performing a destructive delete operation.
 *
 * @param showDialog controls whether the dialog is visible.
 * @param isDeleting when true, confirm and dismiss actions are disabled to prevent interruption.
 * @param onConfirmDelete callback invoked to proceed with deletion.
 * @param onCancelDelete callback invoked to cancel the delete flow.
 */
@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    isDeleting: Boolean,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
  if (showDialog) {
    AlertDialog(
        onDismissRequest = onCancelDelete,
        modifier = Modifier.testTag(DELETE_CONFIRMATION_DIALOG_TEST_TAG),
        // Title and text are provided as composable lambdas using localized strings.
        title = { Text(stringResource(R.string.delete_request_button_delete_request_question)) },
        text = { Text(stringResource(R.string.delete_request_button_are_you_sure)) },
        confirmButton = {
          TextButton(
              onClick = onConfirmDelete,
              enabled = !isDeleting,
              modifier = Modifier.testTag(DELETE_CONFIRM_BUTTON_TEST_TAG),
              colors =
                  ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.delete_request_button_delete))
              }
        },
        dismissButton = {
          TextButton(
              onClick = onCancelDelete,
              enabled = !isDeleting,
              modifier = Modifier.testTag(DELETE_CANCEL_BUTTON_TEST_TAG)) {
                Text(stringResource(R.string.delete_request_button_cancel))
              }
        })
  }
}
