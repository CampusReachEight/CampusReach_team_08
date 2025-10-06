package com.android.sample.utils

import android.content.Context
import android.util.Base64
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Utility object for generating fake JWT tokens for testing authentication flows.
 */
object FakeJwtGenerator {
  private var _counter = 0
  private val counter
    get() = _counter++

  /**
   * Encodes the given byte array to a Base64 URL-safe string without padding or line wraps.
   *
   * @param input The byte array to encode.
   * @return The Base64 URL-safe encoded string.
   */
  private fun base64UrlEncode(input: ByteArray): String {
    return Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  /**
   * Creates a fake Google ID token (JWT) with the provided name and email.
   * The token is unsigned and suitable for use in emulator or test environments.
   *
   * @param name The user's display name.
   * @param email The user's email address.
   * @return A fake Google ID token as a String.
   */
  fun createFakeGoogleIdToken(name: String, email: String): String {
    val header = JSONObject(mapOf("alg" to "none"))
    val payload =
      JSONObject(
        mapOf(
          "sub" to counter.toString(),
          "email" to email,
          "name" to name,
          "picture" to "http://example.com/avatar.png"))

    val headerEncoded = base64UrlEncode(header.toString().toByteArray())
    val payloadEncoded = base64UrlEncode(payload.toString().toByteArray())

    // Signature can be anything, emulator doesn't check it
    val signature = "sig"

    return "$headerEncoded.$payloadEncoded.$signature"
  }
}

/**
 * Fake implementation and factory for [CredentialManager] used in authentication tests.
 * Provides various static methods to create mocked [CredentialManager] instances
 * with different behaviors for testing success and error scenarios.
 */
class FakeCredentialManager private constructor(private val context: Context) :
  CredentialManager by CredentialManager.create(context) {
  companion object {
    /**
     * Creates a mock [CredentialManager] that always returns a [CustomCredential]
     * containing the given fakeUserIdToken when [getCredential] is called.
     *
     * @param fakeUserIdToken The fake ID token to return.
     * @return A mocked [CredentialManager] instance.
     */
    fun create(fakeUserIdToken: String): CredentialManager {
      mockkObject(GoogleIdTokenCredential)
      val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
      every { googleIdTokenCredential.idToken } returns fakeUserIdToken
      every { GoogleIdTokenCredential.createFrom(any()) } returns googleIdTokenCredential
      val fakeCredentialManager = mockk<FakeCredentialManager>()
      val mockGetCredentialResponse = mockk<GetCredentialResponse>()

      val fakeCustomCredential =
        CustomCredential(
          type = TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
          data = bundleOf("id_token" to fakeUserIdToken))

      every { mockGetCredentialResponse.credential } returns fakeCustomCredential
      coEvery {
        fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
      } returns mockGetCredentialResponse

      return fakeCredentialManager
    }

    /**
     * Creates a mock [CredentialManager] that throws [GetCredentialCancellationException]
     * when [getCredential] is called, simulating user cancellation.
     *
     * @return A mocked [CredentialManager] instance.
     */
    fun createWithCancellation(): CredentialManager {
      val fakeCredentialManager = mockk<FakeCredentialManager>()
      coEvery {
        fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
      } throws GetCredentialCancellationException("User cancelled")

      return fakeCredentialManager
    }

    /**
     * Creates a mock [CredentialManager] that throws [NoCredentialException]
     * when [getCredential] is called, simulating no credentials found.
     *
     * @return A mocked [CredentialManager] instance.
     */
    fun createWithNoCredential(): CredentialManager {
      val fakeCredentialManager = mockk<FakeCredentialManager>()
      coEvery {
        fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
      } throws NoCredentialException("No credential")

      return fakeCredentialManager
    }

    /**
     * Creates a mock [CredentialManager] that throws [GetCredentialUnknownException]
     * with a custom error message when [getCredential] is called.
     *
     * @param errorMessage The error message for the exception.
     * @return A mocked [CredentialManager] instance.
     */
    fun createWithError(errorMessage: String = "Unknown error"): CredentialManager {
      val fakeCredentialManager = mockk<FakeCredentialManager>()
      coEvery {
        fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
      } throws GetCredentialUnknownException(errorMessage)

      return fakeCredentialManager
    }

    /**
     * Creates a mock [CredentialManager] that delays for [delayMs] milliseconds
     * and then throws [GetCredentialCancellationException], useful for testing loading states.
     *
     * @param delayMs The delay in milliseconds before throwing the exception.
     * @return A mocked [CredentialManager] instance.
     */
    fun createWithDelayedCancellation(delayMs: Long = 5000): CredentialManager {
      val fakeCredentialManager = mockk<FakeCredentialManager>()
      coEvery {
        fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
      } coAnswers
              {
                delay(delayMs)
                throw GetCredentialCancellationException("timeout")
              }

      return fakeCredentialManager
    }
  }
}
