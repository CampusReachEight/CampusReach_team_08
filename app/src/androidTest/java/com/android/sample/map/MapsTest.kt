package com.android.sample.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.model.request.RequestRepositoryFirestore
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapTestTags
import com.android.sample.utils.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var repository: RequestRepositoryFirestore
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth

  private lateinit var currentUserId: String
  private var currentEmail: String = DEFAULT_USER_EMAIL
  private var currentPassword: String = DEFAULT_USER_PASSWORD

  companion object {
    private const val DEFAULT_USER_EMAIL = "test@example.com"
    private const val DEFAULT_USER_PASSWORD = "test123456"
  }

  private suspend fun signInUser(email: String, password: String) {
    FirebaseEmulator.signOut()
    FirebaseEmulator.signInTestUser(email, password)
    currentEmail = email
    currentPassword = password
    currentUserId = auth.currentUser?.uid ?: error("Failed to sign in user $email")
  }

  @Before
  fun setUp() {
    db = FirebaseEmulator.firestore
    auth = FirebaseEmulator.auth
    repository = RequestRepositoryFirestore(db)
    runTest {
      FirebaseEmulator.clearAuthEmulator()
      signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    }
    composeTestRule.setContent { MapScreen() }
  }

  @After
  fun tearDown() {
    // Clean both Firestore and Auth emulators to avoid password mismatch on next test run
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.signOut()
  }

  @Test
  fun mapScreen_exist() {
    composeTestRule
      .onNodeWithTag(MapTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
      .assertExists()
  }
}
