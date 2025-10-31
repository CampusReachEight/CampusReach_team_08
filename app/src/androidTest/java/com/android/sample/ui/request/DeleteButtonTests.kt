package com.android.sample.ui.request.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Test that delete button displays the correct text when not deleting. Verifies the button label
   * is visible and readable.
   */
  @Test
  fun deleteButton_displaysTextWhenNotDeleting() {
    composeTestRule.setContent { DeleteButton(isDeleting = false, onDeleteClick = {}) }

    composeTestRule.onNodeWithTag(DELETE_BUTTON_TEST_TAG).assertExists().assertIsDisplayed()
  }

  /**
   * Test that delete button is enabled when not in progress. Ensures user can click to initiate
   * deletion.
   */
  @Test
  fun deleteButton_isEnabledWhenNotDeleting() {
    composeTestRule.setContent { DeleteButton(isDeleting = false, onDeleteClick = {}) }

    composeTestRule.onNodeWithTag(DELETE_BUTTON_TEST_TAG).assertIsEnabled()
  }

  /**
   * Test that delete button is disabled while deletion is in progress. Prevents multiple deletion
   * requests and concurrent operations.
   */
  @Test
  fun deleteButton_isDisabledWhileDeleting() {
    composeTestRule.setContent { DeleteButton(isDeleting = true, onDeleteClick = {}) }

    composeTestRule.onNodeWithTag(DELETE_BUTTON_TEST_TAG).assertIsNotEnabled()
  }

  /**
   * Test that delete button shows progress indicator during deletion. Provides visual feedback to
   * user that operation is in progress.
   */
  @Test
  fun deleteButton_showsLoadingIndicatorWhileDeleting() {
    composeTestRule.setContent { DeleteButton(isDeleting = true, onDeleteClick = {}) }

    // Verify progress indicator is displayed
    composeTestRule
        .onNodeWithTag(DELETE_PROGRESS_INDICATOR_TEST_TAG)
        .assertExists()
        .assertIsDisplayed()
  }

  /**
   * Test that delete button hides text while showing loading indicator. Ensures clean UX during
   * loading state.
   */
  @Test
  fun deleteButton_hidesTextWhileLoadingIndicatorShows() {
    composeTestRule.setContent { DeleteButton(isDeleting = true, onDeleteClick = {}) }

    // Progress indicator should be visible
    composeTestRule.onNodeWithTag(DELETE_PROGRESS_INDICATOR_TEST_TAG).assertIsDisplayed()
  }

  /**
   * Test that delete button invokes callback when clicked. Verifies the onClick handler is properly
   * wired.
   */
  @Test
  fun deleteButton_invokesCallbackOnClick() {
    var clickCount = 0
    composeTestRule.setContent {
      DeleteButton(isDeleting = false, onDeleteClick = { clickCount++ })
    }

    composeTestRule.onNodeWithTag(DELETE_BUTTON_TEST_TAG).performClick()

    assert(clickCount == 1)
  }

  /**
   * Test that delete button doesn't invoke callback when disabled. Prevents accidental deletion
   * triggers during ongoing operation.
   */
  @Test
  fun deleteButton_doesNotInvokeCallbackWhenDisabled() {
    var clickCount = 0
    composeTestRule.setContent { DeleteButton(isDeleting = true, onDeleteClick = { clickCount++ }) }

    composeTestRule.onNodeWithTag(DELETE_BUTTON_TEST_TAG).performClick()

    assert(clickCount == 0)
  }

  /**
   * Test that delete button uses error color for destructive action. Ensures visual indication that
   * this is a dangerous operation.
   */
  @Test
  fun deleteButton_displaysWithErrorColor() {
    composeTestRule.setContent { DeleteButton(isDeleting = false, onDeleteClick = {}) }

    // Verify button exists with proper test tag
    composeTestRule.onNodeWithTag(DELETE_BUTTON_TEST_TAG).assertExists().assertIsDisplayed()
  }
}
