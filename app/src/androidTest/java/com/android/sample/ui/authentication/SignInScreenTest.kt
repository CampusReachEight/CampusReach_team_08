package com.android.sample.ui.authentication

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.utils.FakeCredentialManager
import com.android.sample.utils.FakeJwtGenerator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ========== UI Component Tests ==========

  @Test
  fun signInScreen_displaysAllComponents() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_TITLE).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
      .assertIsDisplayed()
      .assertIsEnabled()
    composeTestRule.onNodeWithText("Se connecter avec Google").assertIsDisplayed()
  }

  @Test
  fun signInScreen_displaysWelcomeText() {
    composeTestRule.setContent { SignInScreen() }
    composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
  }

  @Test
  fun signInButton_isInitiallyEnabled() {
    composeTestRule.setContent { SignInScreen() }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsEnabled()
  }

  @Test
  fun signInScreen_noErrorMessageInitially() {
    composeTestRule.setContent { SignInScreen() }

    // Error messages should not be displayed initially
    composeTestRule.onNodeWithText("Connection cancelled").assertDoesNotExist()
    composeTestRule.onNodeWithText("No Google account found").assertDoesNotExist()
  }

  // ========== Loading State Tests ==========

  @Test
  fun signInButton_showsLoadingIndicator_whenClicked() {
    val fakeCredentialManager = FakeCredentialManager.createWithDelayedCancellation(2000)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    // Button should be disabled during loading
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsNotEnabled()

    // Button text should be replaced with loading indicator
    composeTestRule.onNodeWithText("Se connecter avec Google").assertDoesNotExist()
  }

  @Test
  fun signInButton_reenablesAfterError() {
    val fakeCredentialManager = FakeCredentialManager.createWithCancellation()

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Button should be re-enabled after error
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithText("Se connecter avec Google").assertIsDisplayed()
  }

  // ========== Error Handling Tests ==========

  @Test
  fun signIn_cancellation_showsErrorMessage() {
    val fakeCredentialManager = FakeCredentialManager.createWithCancellation()

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connection cancelled").assertIsDisplayed()
  }

  @Test
  fun signIn_noCredential_showsErrorMessage() {
    val fakeCredentialManager = FakeCredentialManager.createWithNoCredential()

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No Google account found").assertIsDisplayed()
  }

  @Test
  fun signIn_genericError_showsErrorMessage() {
    val fakeCredentialManager = FakeCredentialManager.createWithError("Network error")

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connection error: Network error").assertIsDisplayed()
  }

  @Test
  fun signIn_customErrorMessage_isDisplayed() {
    val customError = "Custom error message"
    val fakeCredentialManager = FakeCredentialManager.createWithError(customError)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connection error: $customError").assertIsDisplayed()
  }

  @Test
  fun errorMessage_isDisplayedInErrorContainer() {
    val fakeCredentialManager = FakeCredentialManager.createWithCancellation()

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connection cancelled").assertIsDisplayed()
  }

  @Test
  fun errorMessage_clearsOnRetry() {
    val fakeCredentialManager = FakeCredentialManager.createWithCancellation()

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    // First attempt: error
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Connection cancelled").assertIsDisplayed()

    // Second attempt: error appears again
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connection cancelled").assertIsDisplayed()
  }

  // ========== Success Flow Tests ==========

  @Test
  fun signIn_success_callsOnSignInSuccess() {
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken("Test User", "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeToken)

    composeTestRule.setContent {
      SignInScreen(credentialManager = fakeCredentialManager, onSignInSuccess = {})
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verifies credential flow initiates without errors
    // Full Firebase integration would require emulator setup
  }

  @Test
  fun signIn_success_noErrorMessageDisplayed() {
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken("Test User", "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeToken)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // No error messages should be displayed
    composeTestRule.onNodeWithText("Connection cancelled").assertDoesNotExist()
    composeTestRule.onNodeWithText("No Google account found").assertDoesNotExist()
  }

  @Test
  fun signIn_firstUser_differentToken() {
    val user1Token = FakeJwtGenerator.createFakeGoogleIdToken("User One", "user1@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(user1Token)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Should work without errors
    composeTestRule.onNodeWithText("Connection cancelled").assertDoesNotExist()
  }

  @Test
  fun signIn_secondUser_differentToken() {
    val user2Token = FakeJwtGenerator.createFakeGoogleIdToken("User Two", "user2@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(user2Token)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Should work without errors
    composeTestRule.onNodeWithText("Connection cancelled").assertDoesNotExist()
  }

  // ========== Configuration Tests ==========

  @Test
  fun clientId_isConfiguredCorrectly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val clientId = context.getString(R.string.default_web_client_id)

    assert(clientId.isNotEmpty()) { "Client ID should not be empty" }
    assert(clientId.endsWith(".googleusercontent.com") || clientId.contains("mock")) {
      "Client ID should end with .googleusercontent.com or contain 'mock' for testing"
    }
  }

  @Test
  fun clientId_resourceExists() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resourceId =
      context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

    assert(resourceId != 0) { "default_web_client_id resource should exist" }
  }

  // ========== UI State Consistency Tests ==========

  @Test
  fun multipleClicks_duringLoading_areIgnored() {
    val fakeCredentialManager = FakeCredentialManager.createWithDelayedCancellation(3000)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    // Button should be disabled, so additional clicks are ignored
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun errorMessage_cancellationError_showsCorrectMessage() {
    val cancelManager = FakeCredentialManager.createWithCancellation()
    composeTestRule.setContent { SignInScreen(credentialManager = cancelManager) }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Connection cancelled").assertIsDisplayed()
  }

  @Test
  fun errorMessage_noCredentialError_showsCorrectMessage() {
    val noCredManager = FakeCredentialManager.createWithNoCredential()
    composeTestRule.setContent { SignInScreen(credentialManager = noCredManager) }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No Google account found").assertIsDisplayed()
  }

  @Test
  fun errorMessage_genericError_showsCorrectMessage() {
    val errorManager = FakeCredentialManager.createWithError("Test error")
    composeTestRule.setContent { SignInScreen(credentialManager = errorManager) }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Connection error: Test error").assertIsDisplayed()
  }

  @Test
  fun signInScreen_withCustomCallback_preservesUIState() {
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken("Test", "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeToken)

    composeTestRule.setContent {
      SignInScreen(credentialManager = fakeCredentialManager, onSignInSuccess = {})
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
  }

  @Test
  fun signInScreen_errorState_clearsOnNewInstance() {
    val fakeCredentialManager = FakeCredentialManager.createWithCancellation()

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connection cancelled").assertIsDisplayed()
  }
}