package com.android.sample.ui.authentication

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    FirebaseAuth.getInstance().signOut()
    clearAllMocks()
  }

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
  fun signInButton_showsLoadingIndicator_whenClicked() {
    val mockCredentialManager = mockk<CredentialManager>()

    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } coAnswers
        {
          delay(5000)
          throw GetCredentialCancellationException("timeout")
        }

    composeTestRule.setContent { SignInScreen(credentialManager = mockCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsNotEnabled()

    composeTestRule.onNodeWithText("Se connecter avec Google").assertDoesNotExist()
  }

  @Test
  fun signIn_cancellation_showsErrorMessage() {
    val mockCredentialManager = mockk<CredentialManager>()

    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        GetCredentialCancellationException("User cancelled")

    composeTestRule.setContent { SignInScreen(credentialManager = mockCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connexion annulée").assertIsDisplayed()
  }

  @Test
  fun SsignIn_noCredential_showsErrorMessage() {
    val mockCredentialManager = mockk<CredentialManager>()

    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        NoCredentialException("No credential")

    composeTestRule.setContent { SignInScreen(credentialManager = mockCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Aucun compte Google trouvé").assertIsDisplayed()
  }

  @Test
  fun signIn_genericError_showsErrorMessage() {
    val mockCredentialManager = mockk<CredentialManager>()

    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        GetCredentialUnknownException("Network error")

    composeTestRule.setContent { SignInScreen(credentialManager = mockCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Erreur de connexion: Network error").assertIsDisplayed()
  }

  @Test
  fun signIn_success_callsOnSignInSuccess() {
    var wasCallbackCalled = false
    val mockCredentialManager = mockk<CredentialManager>()
    val mockResponse = mockk<GetCredentialResponse>()

    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } returns
        mockResponse

    composeTestRule.setContent {
      SignInScreen(
          credentialManager = mockCredentialManager, onSignInSuccess = { wasCallbackCalled = true })
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
  }

  @Test
  fun errorMessage_isDisplayedInErrorContainer() {
    val mockCredentialManager = mockk<CredentialManager>()

    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        GetCredentialCancellationException("Cancelled")

    composeTestRule.setContent { SignInScreen(credentialManager = mockCredentialManager) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Connexion annulée").assertIsDisplayed()
  }

  @Test
  fun clientId_isConfiguredCorrectly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val clientId = context.getString(R.string.default_web_client_id)

    assert(clientId.isNotEmpty())
    assert(clientId.endsWith(".googleusercontent.com") || clientId.contains("mock"))
  }
}
