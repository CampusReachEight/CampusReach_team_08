package com.android.sample.ui.request.edit

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Tests for MaterialDatePickerDialog and MaterialTimePickerDialog components.
 *
 * Note: We primarily test that dialogs display and dismiss correctly. The full date/time selection
 * flow is thoroughly tested in EditRequestScreenTests integration tests, which verify the actual
 * user workflow works correctly.
 */
class MaterialDateTimePickerDialogTest {
  @get:Rule val composeTestRule = createComposeRule()

  // ========== DATE PICKER TESTS ==========

  @Test
  fun materialDatePickerDialog_displays() {
    composeTestRule.setContent { MaterialDatePickerDialog(onDateSelected = {}, onDismiss = {}) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun materialDatePickerDialog_onCancelClick_callsOnDismiss() {
    var dismissCalled = false

    composeTestRule.setContent {
      MaterialDatePickerDialog(onDateSelected = {}, onDismiss = { dismissCalled = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    assert(dismissCalled)
  }

  // ========== TIME PICKER TESTS ==========

  @Test
  fun materialTimePickerDialog_displays() {
    composeTestRule.setContent {
      MaterialTimePickerDialog(onTimeSelected = { _, _ -> }, onDismiss = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()
  }

  @Test
  fun materialTimePickerDialog_onOkClick_callsOnTimeSelected() {
    var hourSelected: Int? = null
    var minuteSelected: Int? = null

    composeTestRule.setContent {
      MaterialTimePickerDialog(
          onTimeSelected = { hour, minute ->
            hourSelected = hour
            minuteSelected = minute
          },
          onDismiss = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    // TimePicker always has valid default values
    assert(hourSelected != null)
    assert(minuteSelected != null)
    assert(hourSelected!! in 0..23)
    assert(minuteSelected!! in 0..59)
  }

  @Test
  fun materialTimePickerDialog_onCancelClick_callsOnDismiss() {
    var dismissCalled = false

    composeTestRule.setContent {
      MaterialTimePickerDialog(onTimeSelected = { _, _ -> }, onDismiss = { dismissCalled = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    assert(dismissCalled)
  }
}
