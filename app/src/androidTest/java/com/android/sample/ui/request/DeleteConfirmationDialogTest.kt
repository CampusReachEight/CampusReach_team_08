package com.android.sample.ui.request

// Tests for DeleteConfirmationDialog component in DeleteButton.kt file
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.request.edit.DELETE_CANCEL_BUTTON_TEST_TAG
import com.android.sample.ui.request.edit.DELETE_CONFIRMATION_DIALOG_TEST_TAG
import com.android.sample.ui.request.edit.DELETE_CONFIRM_BUTTON_TEST_TAG
import com.android.sample.ui.request.edit.DeleteConfirmationDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteConfirmationDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Test that confirmation dialog appears when showDialog is true. Verifies the dialog is rendered
   * and visible to the user.
   */
  @Test
  fun deleteConfirmationDialog_appearsWhenShowDialogIsTrue() {
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true, isDeleting = false, onConfirmDelete = {}, onCancelDelete = {})
    }

    composeTestRule
        .onNodeWithTag(DELETE_CONFIRMATION_DIALOG_TEST_TAG)
        .assertExists()
        .assertIsDisplayed()
  }

  /**
   * Test that confirmation dialog is hidden when showDialog is false. Ensures dialog doesn't appear
   * unexpectedly.
   */
  @Test
  fun deleteConfirmationDialog_doesNotAppearWhenShowDialogIsFalse() {
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = false, isDeleting = false, onConfirmDelete = {}, onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CONFIRMATION_DIALOG_TEST_TAG).assertDoesNotExist()
  }

  /**
   * Test that confirm button is enabled when not deleting. Allows user to proceed with deletion.
   */
  @Test
  fun deleteConfirmationDialog_confirmButtonEnabledWhenNotDeleting() {
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true, isDeleting = false, onConfirmDelete = {}, onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CONFIRM_BUTTON_TEST_TAG).assertIsEnabled()
  }

  /** Test that confirm button is disabled while deleting. Prevents multiple delete requests. */
  @Test
  fun deleteConfirmationDialog_confirmButtonDisabledWhileDeleting() {
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true, isDeleting = true, onConfirmDelete = {}, onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CONFIRM_BUTTON_TEST_TAG).assertIsNotEnabled()
  }

  /** Test that cancel button is enabled when not deleting. Allows user to abort the delete flow. */
  @Test
  fun deleteConfirmationDialog_cancelButtonEnabledWhenNotDeleting() {
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true, isDeleting = false, onConfirmDelete = {}, onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CANCEL_BUTTON_TEST_TAG).assertIsEnabled()
  }

  /**
   * Test that cancel button is disabled while deleting. Prevents user interaction during deletion.
   */
  @Test
  fun deleteConfirmationDialog_cancelButtonDisabledWhileDeleting() {
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true, isDeleting = true, onConfirmDelete = {}, onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CANCEL_BUTTON_TEST_TAG).assertIsNotEnabled()
  }

  /**
   * Test that confirm button invokes callback when clicked. Verifies deletion proceeds when user
   * confirms.
   */
  @Test
  fun deleteConfirmationDialog_confirmButtonInvokesCallback() {
    var confirmCount = 0
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true,
          isDeleting = false,
          onConfirmDelete = { confirmCount++ },
          onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CONFIRM_BUTTON_TEST_TAG).performClick()

    assert(confirmCount == 1)
  }

  /**
   * Test that cancel button invokes callback when clicked. Verifies user can abort the deletion.
   */
  @Test
  fun deleteConfirmationDialog_cancelButtonInvokesCallback() {
    var cancelCount = 0
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true,
          isDeleting = false,
          onConfirmDelete = {},
          onCancelDelete = { cancelCount++ })
    }

    composeTestRule.onNodeWithTag(DELETE_CANCEL_BUTTON_TEST_TAG).performClick()

    assert(cancelCount == 1)
  }

  /**
   * Test that confirm button doesn't invoke callback when disabled. Prevents deletion triggers
   * during ongoing operation.
   */
  @Test
  fun deleteConfirmationDialog_confirmButtonDoesNotInvokeCallbackWhenDisabled() {
    var confirmCount = 0
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true,
          isDeleting = true,
          onConfirmDelete = { confirmCount++ },
          onCancelDelete = {})
    }

    composeTestRule.onNodeWithTag(DELETE_CONFIRM_BUTTON_TEST_TAG).performClick()

    assert(confirmCount == 0)
  }

  /**
   * Test that cancel button doesn't invoke callback when disabled. Prevents user interaction during
   * deletion.
   */
  @Test
  fun deleteConfirmationDialog_cancelButtonDoesNotInvokeCallbackWhenDisabled() {
    var cancelCount = 0
    composeTestRule.setContent {
      DeleteConfirmationDialog(
          showDialog = true,
          isDeleting = true,
          onConfirmDelete = {},
          onCancelDelete = { cancelCount++ })
    }

    composeTestRule.onNodeWithTag(DELETE_CANCEL_BUTTON_TEST_TAG).performClick()

    assert(cancelCount == 0)
  }
}
