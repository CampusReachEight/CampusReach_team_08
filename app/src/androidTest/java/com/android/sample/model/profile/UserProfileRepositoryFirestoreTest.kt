package com.android.sample.model.profile

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.profile.UserSections
import com.android.sample.utils.BaseEmulatorTest
import com.android.sample.utils.FirebaseEmulator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileRepositoryFirestoreTest : BaseEmulatorTest() {

  private lateinit var repository: UserProfileRepositoryFirestore

  fun generateProfile(
      id: String,
      name: String,
      lastName: String,
      email: String,
      section: UserSections
  ): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        lastName = lastName,
        email = email,
        section = section,
        photo = null,
        kudos = 0,
        arrivalDate = Date())
  }

  private val testProfile1 =
      generateProfile(
          id = "test-user-1",
          name = "John",
          lastName = "Doe",
          email = "john.doe@example.com",
          section = UserSections.COMPUTER_SCIENCE,
      )

  private val testProfile2 =
      generateProfile(
          id = "test-user-2",
          name = "Jane",
          lastName = "Smith",
          email = "jane.smith@example.com",
          section = UserSections.NONE)

  private val testProfile3 =
      generateProfile(
          id = "test-user-3",
          name = "Bob",
          lastName = "Johnson",
          email = "bob.johnson@example.com",
          section = UserSections.COMPUTER_SCIENCE)

  @Before
  override fun setUp() {
    super.setUp() // Initialize auth and db

    runTest {
      auth.signOut()
      signInUser()

      // CRITICAL: Clear any existing data BEFORE creating repository
      clearAllTestData()
    }

    repository = UserProfileRepositoryFirestore(db)
  }

  @After
  override fun tearDown() {
    runTest { clearAllTestData() }
    super.tearDown()
  }

  private suspend fun clearAllTestData() {
    try {
      val publicSnapshot = db.collection(PUBLIC_PROFILES_PATH).get().await()
      val privateSnapshot = db.collection(PRIVATE_PROFILES_PATH).get().await()

      val batch = db.batch()

      publicSnapshot.documents.forEach { doc -> batch.delete(doc.reference) }

      privateSnapshot.documents.forEach { doc -> batch.delete(doc.reference) }

      batch.commit().await()

      // Small delay to ensure deletion completes
      kotlinx.coroutines.delay(100)
    } catch (e: Exception) {
      Log.e("TestCleanup", "Error clearing test data", e)
    }
  }

  private suspend fun clearTestCollections() {
    val currentUserId = auth.currentUser?.uid ?: return

    // Only delete the current user's documents
    val publicDoc = db.collection(PUBLIC_PROFILES_PATH).document(currentUserId)
    val privateDoc = db.collection(PRIVATE_PROFILES_PATH).document(currentUserId)

    val batch = db.batch()

    // Check if documents exist before deleting
    val publicSnapshot = publicDoc.get().await()
    if (publicSnapshot.exists()) {
      batch.delete(publicDoc)
    }

    val privateSnapshot = privateDoc.get().await()
    if (privateSnapshot.exists()) {
      batch.delete(privateDoc)
    }

    batch.commit().await()
  }

  private suspend fun getPublicProfilesCount(): Int {
    return db.collection(PUBLIC_PROFILES_PATH).get().await().size()
  }

  private suspend fun getPrivateProfilesCount(id: String = currentUserId): Int {
    val doc = db.collection(PRIVATE_PROFILES_PATH).document(id).get().await()
    return if (doc.exists()) 1 else 0
  }

  @Test
  fun getNewIdReturnsCorrectId() = runTest { assertEquals(currentUserId, repository.getNewUid()) }

  @Test
  fun canAddUserProfileToRepository() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    assertEquals(1, getPublicProfilesCount())
    assertEquals(1, getPrivateProfilesCount())

    val publicProfiles = repository.getAllUserProfiles()
    assertEquals(1, publicProfiles.size)

    val storedProfile = publicProfiles.first()
    assertEquals(profile.id, storedProfile.id)
    assertEquals(profile.name, storedProfile.name)
    assertEquals(null, storedProfile.email)
  }

  @Test
  fun addUserProfileStoresFullDetailsInPrivateCollection() =
      runTest(timeout = 10.seconds) {
        val profile = testProfile1.copy(id = currentUserId)
        repository.addUserProfile(profile)

        // CRITICAL: Wait for Firestore write to complete
        advanceUntilIdle()

        val privateProfile = repository.getUserProfile(currentUserId)

        // Add better error message for debugging
        assertEquals(
            "Email mismatch. Expected: ${profile.email}, Got: ${privateProfile.email}",
            profile.email,
            privateProfile.email)
        assertEquals(profile, privateProfile)
      }

  @Test
  fun cannotAddProfileWithMismatchedUserId() = runTest {
    val profile = testProfile1.copy(id = "different-user-id")

    try {
      repository.addUserProfile(profile)
      fail("Expected IllegalArgumentException when adding profile with mismatched user ID")
    } catch (_: IllegalArgumentException) {
      // Expected exception
    }
  }

  @Test
  fun canGetUserProfileById() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    val retrievedProfile = repository.getUserProfile(currentUserId)
    assertEquals(profile, retrievedProfile)
  }

  @Test
  fun getUserProfileReturnsPublicVersionForOtherUsers() =
      runTest(timeout = 15.seconds) {
        // Store the original user ID BEFORE any operations
        val originalUserId = currentUserId
        val profile = testProfile1.copy(id = originalUserId)

        repository.addUserProfile(profile)
        advanceUntilIdle() // Wait for write

        // Sign in as different user
        signInUser("otheremail@mail.com", "password")
        advanceUntilIdle() // Wait for auth

        val otherUserId =
            auth.currentUser?.uid
                ?: throw IllegalStateException("No authenticated user after sign in")
        val otherUserProfile = testProfile2.copy(id = otherUserId)

        repository.addUserProfile(otherUserProfile)
        advanceUntilIdle() // Wait for write

        // Retrieve the ORIGINAL user's profile (not current user)
        val retrievedProfile = repository.getUserProfile(originalUserId)

        assertEquals("Expected original user ID", originalUserId, retrievedProfile.id)
        assertEquals("Expected original user name", profile.name, retrievedProfile.name)
        assertNull("Email should be null for other users", retrievedProfile.email)

        // Return to default user
        signInUser()
        advanceUntilIdle()
      }

  @Test
  fun canUpdateUserProfile() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    val updatedProfile = profile.copy(name = "UpdatedName", lastName = "UpdatedLastName")
    repository.updateUserProfile(currentUserId, updatedProfile)

    val retrievedProfile = repository.getUserProfile(currentUserId)
    assertEquals(updatedProfile, retrievedProfile)
  }

  @Test
  fun updateUserProfileUpdatesPublicVersion() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    val updatedProfile = profile.copy(name = "UpdatedName")
    repository.updateUserProfile(currentUserId, updatedProfile)

    val publicProfiles = repository.getAllUserProfiles()
    val publicProfile = publicProfiles.first()
    assertEquals("UpdatedName", publicProfile.name)
    assertEquals(null, publicProfile.email)
  }

  @Test
  fun cannotUpdateOtherUsersProfile() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    val otherProfile = testProfile2
    try {
      repository.updateUserProfile(otherProfile.id, otherProfile)
      fail("Expected IllegalArgumentException when updating another user's profile")
    } catch (_: IllegalArgumentException) {
      // Expected exception
    }
  }

  @Test
  fun canDeleteUserProfile() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    assertEquals(1, getPublicProfilesCount())
    assertEquals(1, getPrivateProfilesCount())

    repository.deleteUserProfile(currentUserId)

    assertEquals(0, getPublicProfilesCount())
    assertEquals(0, getPrivateProfilesCount())
  }

  @Test
  fun cannotDeleteOtherUsersProfile() = runTest {
    try {
      repository.deleteUserProfile("some-other-user-id")
      fail("Expected IllegalArgumentException when deleting another user's profile")
    } catch (_: IllegalArgumentException) {
      // Expected exception
    }
  }

  @Test
  fun cannotPerformOperationsWhenNotAuthenticated() = runTest {
    FirebaseEmulator.signOut()

    assertEquals(Firebase.auth.currentUser, null)

    val profile = testProfile1.copy(id = "some-user-id")

    try {
      repository.addUserProfile(profile)
      fail("Expected IllegalStateException when adding profile while not authenticated")
    } catch (_: IllegalStateException) {
      // Expected exception
    }

    try {
      repository.getUserProfile("some-user-id")
      fail("Expected IllegalStateException when getting profile while not authenticated")
    } catch (_: NoSuchElementException) {
      // Expected exception
    }

    try {
      repository.updateUserProfile("some-user-id", profile)
      fail("Expected IllegalStateException when updating profile while not authenticated")
    } catch (_: IllegalStateException) {
      // Expected exception
    }

    try {
      repository.deleteUserProfile("some-user-id")
      fail("Expected IllegalStateException when deleting profile while not authenticated")
    } catch (_: IllegalStateException) {
      Log.d("UserProfileRepoTest", "Caught expected exception")
    }
  }

  fun fullUserProfileLifecycle() = runTest {
    // Add profile
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    // Retrieve and verify
    var retrievedProfile = repository.getUserProfile(currentUserId)
    assertEquals(profile, retrievedProfile)

    // Update profile
    val updatedProfile = profile.copy(name = "UpdatedName", lastName = "UpdatedLastName")
    repository.updateUserProfile(currentUserId, updatedProfile)

    // Retrieve and verify update
    retrievedProfile = repository.getUserProfile(currentUserId)
    assertEquals(updatedProfile, retrievedProfile)

    // Delete profile
    repository.deleteUserProfile(currentUserId)
    try {
      repository.getUserProfile(currentUserId)
      fail("Expected NoSuchElementException when retrieving deleted profile")
    } catch (_: NoSuchElementException) {
      // Expected exception
    }
  }

  @Test
  fun unauthenticatedGetNewIdThrows() = runTest {
    FirebaseEmulator.signOut()
    try {
      repository.getNewUid()
      fail("Expected IllegalStateException when getting new ID unauthenticated")
    } catch (_: IllegalStateException) {
      // Expected exception
    } finally {
      signInUser()
    }
  }

  // --------------------------
  // Search tests (emulator-backed)
  // --------------------------

  private suspend fun addProfileFor(email: String, name: String, lastName: String) {
    // Switch auth context to a deterministic user and add their own profile (ID must match UID)
    signInUser(email, DEFAULT_USER_PASSWORD)
    val p =
        generateProfile(
            id = currentUserId,
            name = name,
            lastName = lastName,
            email = email,
            section = UserSections.NONE)
    repository.addUserProfile(p)
  }

  @Test
  fun search_returnsEmpty_forShortQueries() = runTest {
    // No need to seed data
    assertTrue(repository.searchUserProfiles("").isEmpty())
    assertTrue(repository.searchUserProfiles("a").isEmpty())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun search_findsByFirstNamePrefix() =
      runTest(timeout = 15.seconds) {
        // Seed two users
        addProfileFor(DEFAULT_USER_EMAIL, name = "John", lastName = "Doe")
        addProfileFor(SECOND_USER_EMAIL, name = "Jane", lastName = "Smith")

        advanceUntilIdle()

        kotlinx.coroutines.delay(500) // Real delay for Firestore indexing

        val results = repository.searchUserProfiles("joh")

        assertTrue(
            "Expected to find John Doe in results. Found: ${results.map { "${it.name} ${it.lastName}" }}",
            results.any { it.name == "John" && it.lastName == "Doe" })
        assertFalse(
            "Should not find Jane Smith in results",
            results.any { it.name == "Jane" && it.lastName == "Smith" })
      }

  @Test
  fun search_findsByLastNamePrefix_whenFirstNameDoesNotMatch() = runTest {
    // Seed user with last name matching prefix only
    addProfileFor(DEFAULT_USER_EMAIL, name = "Alice", lastName = "Smith")

    val results = repository.searchUserProfiles("smi")
    assertTrue(results.any { it.name == "Alice" && it.lastName == "Smith" })
    assertFalse(results.any { it.name == "John" && it.lastName == "Doe" })
  }

  @Test
  fun search_deduplicatesAcrossBothQueries() = runTest {
    // Create a user whose first and last names both match prefix "jo"
    addProfileFor(DEFAULT_USER_EMAIL, name = "Jo", lastName = "Johnson")

    val results = repository.searchUserProfiles("jo")
    val distinctIds = results.map { it.id }.toSet()
    assertEquals(distinctIds.size, results.size)
  }

  @Test
  fun search_respectsLimitParameter() = runTest {
    // Seed 5 users with first name starting with Test
    repeat(5) { idx ->
      addProfileFor("test_limit_$idx@example.com", name = "Test$idx", lastName = "User$idx")
    }
    val results = repository.searchUserProfiles("test", limit = 3)
    assertEquals(3, results.size)
  }

  @Test
  fun search_isCaseInsensitive() = runTest {
    addProfileFor(DEFAULT_USER_EMAIL, name = "John", lastName = "Doe")
    val results = repository.searchUserProfiles("JOHN")
    assertTrue(results.any { it.name == "John" && it.lastName == "Doe" })
  }

  @Test
  fun search_substringMatchForJohn_returnsJohnnyAndJohnson() = runTest {
    addProfileFor("johnny@example.com", name = "Johnny", lastName = "Alpha")
    addProfileFor("alice.johnson@example.com", name = "Alice", lastName = "Johnson")

    val results = repository.searchUserProfiles("john")

    assertTrue(results.any { it.name == "Johnny" && it.lastName == "Alpha" })
    assertTrue(results.any { it.lastName == "Johnson" && it.name == "Alice" })
  }

  @Test
  fun search_largeDatabase_performance() = runBlocking {
    // Real timeout, not virtual timeout
    withTimeout(30_000) {

      // Seed 200 users
      repeat(200) { idx ->
        val name = "$idx" + "User"
        val lastName = "LastName$idx"
        val email = "${name.lowercase()}.${lastName.lowercase()}@example.com"

        // sign in and add profile
        addProfileFor(email, name = name, lastName = lastName)
      }

      // Allow Firestore indexing to catch up
      delay(800)

      val start = System.currentTimeMillis()
      val results = repository.searchUserProfiles("11", limit = 9)
      val duration = System.currentTimeMillis() - start

      assertTrue("Query took too long: $duration ms", duration < 500)
      assertEquals(9, results.size)
    }
  }
}
