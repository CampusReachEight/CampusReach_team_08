package com.android.sample.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

/**
 * Base class for all emulator tests, providing common setup and utility functions.
 *
 * Handles automatic sign-in and cleanup of Firebase emulators. Tests extending this class should
 * override [setUp] and [tearDown] if they need additional setup/cleanup, making sure to call
 * super.setUp()/super.tearDown().
 */
const val UI_WAIT_TIMEOUT = 5_000L

abstract class BaseEmulatorTest {

  protected lateinit var db: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var currentUserId: String

  protected val currentUser: FirebaseUser
    get() = auth.currentUser ?: error("No authenticated user")

  companion object {
    const val DEFAULT_USER_EMAIL = "test@example.com"
    const val DEFAULT_USER_PASSWORD = "test123456"
    const val SECOND_USER_EMAIL = "secondUser@example.com"
    const val SECOND_USER_PASSWORD = "test123456"
  }

  /** Signs in a test user with the given credentials. Creates the user if they don't exist. */
  protected suspend fun signInUser(
      email: String = DEFAULT_USER_EMAIL,
      password: String = DEFAULT_USER_PASSWORD
  ) {
    FirebaseEmulator.signOut()
    FirebaseEmulator.signInTestUser(email, password)
    currentUserId = auth.currentUser?.uid ?: error("Failed to sign in user $email")
  }

  @Before
  open fun setUp() {
    db = FirebaseEmulator.firestore
    auth = FirebaseEmulator.auth

    //maybe remove
    runTest {
      // Clear auth state to avoid password mismatches from previous runs
      FirebaseEmulator.clearAuthEmulator()
      signInUser()
    }
  }

  @After
  open fun tearDown() {
    // Clean both Firestore and Auth emulators
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.signOut()
  }
}
