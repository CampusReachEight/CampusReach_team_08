package com.android.sample.model.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.utils.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileRepositoryFirestoreTest {

  private lateinit var repository: UserProfileRepositoryFirestore
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var currentUserId: String

  fun generateProfile(
      id: String,
      name: String,
      lastName: String,
      email: String,
      section: Section
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
          section = Section.CYBER_SECURITY,
      )

  private val testProfile2 =
      generateProfile(
          id = "test-user-2",
          name = "Jane",
          lastName = "Smith",
          email = "jane.smith@example.com",
          section = Section.OTHER)

  private val testProfile3 =
      generateProfile(
          id = "test-user-3",
          name = "Bob",
          lastName = "Johnson",
          email = "bob.johnson@example.com",
          section = Section.CYBER_SECURITY)

  @Before
  fun setUp() {
    db = FirebaseEmulator.firestore
    auth = FirebaseEmulator.auth
    repository = UserProfileRepositoryFirestore(db)

    runTest {
      FirebaseEmulator.signInTestUser()
      currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")
      clearTestCollections()
    }
  }

  @After
  fun tearDown() {
    runTest { clearTestCollections() }
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.signOut()
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
  fun addUserProfileStoresFullDetailsInPrivateCollection() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    val privateProfile = repository.getUserProfile(currentUserId)
    assertEquals(profile.email, privateProfile.email)
    assertEquals(profile, privateProfile)
  }

  @Test
  fun cannotAddProfileWithMismatchedUserId() = runTest {
    val profile = testProfile1.copy(id = "different-user-id")

    try {
      repository.addUserProfile(profile)
      fail("Expected IllegalArgumentException when adding profile with mismatched user ID")
    } catch (e: IllegalArgumentException) {
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
  fun getUserProfileReturnsPublicVersionForOtherUsers() = runTest {
    val profile = testProfile1.copy(id = currentUserId)
    repository.addUserProfile(profile)

    FirebaseEmulator.signOut()
    FirebaseEmulator.signInTestUser("otheremail@mail.com", "password")
    val otherUserId = auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")
    val otherUserProfile = testProfile2.copy(id = otherUserId)
    repository.addUserProfile(otherUserProfile)

    val retrievedProfile = repository.getUserProfile(currentUserId)
    assertEquals(profile.id, retrievedProfile.id)
    assertEquals(profile.name, retrievedProfile.name)
    assertEquals(null, retrievedProfile.email) // Email should be blurred
    assertNotEquals(profile.email, retrievedProfile.email)
    FirebaseEmulator.signOut()
    FirebaseEmulator.signInTestUser()
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
    } catch (e: IllegalArgumentException) {
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
    } catch (e: IllegalArgumentException) {
      // Expected exception
    }
  }

  @Ignore("To re-add once implemented")
  @Test
  fun searchUserProfilesByName() = runTest {
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile1.id)
        .set(testProfile1.copy(email = null).toMap())
        .await()
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile2.id)
        .set(testProfile2.copy(email = null).toMap())
        .await()

    val results = repository.searchUserProfiles("John", null, 10).toList()
    assertTrue(results.any { it.name == "John" })
  }

  @Ignore("To re-add once implemented")
  @Test
  fun searchUserProfilesByLastName() = runTest {
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile1.id)
        .set(testProfile1.copy(email = null).toMap())
        .await()
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile2.id)
        .set(testProfile2.copy(email = null).toMap())
        .await()

    val results = repository.searchUserProfiles("Smith", null, 10).toList()
    assertTrue(results.any { it.lastName == "Smith" })
  }

  @Ignore("To re-add once implemented")
  @Test
  fun searchUserProfilesWithSectionFilter() = runTest {
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile1.id)
        .set(testProfile1.copy(email = null).toMap())
        .await()
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile2.id)
        .set(testProfile2.copy(email = null).toMap())
        .await()
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile3.id)
        .set(testProfile3.copy(email = null).toMap())
        .await()

    val results = repository.searchUserProfiles("", Section.CYBER_SECURITY, 10).toList()
    assertEquals(2, results.size)
    assertTrue(results.all { it.section == Section.CYBER_SECURITY })
  }

  @Ignore("To re-add once implemented")
  @Test
  fun searchReturnsEmptyListForEmptyQuery() = runTest {
    val results = repository.searchUserProfiles("", null, 10).toList()
    assertTrue(results.isEmpty())
  }

  @Ignore("To re-add once implemented")
  @Test
  fun searchRespectsResultsPerPageLimit() = runTest {
    // Add 5 profiles with same section
    repeat(5) { index ->
      val profile =
          generateProfile(
                  id = "test-user-$index",
                  name = "User$index",
                  lastName = "Test",
                  email = "user$index@example.com",
                  section = Section.CYBER_SECURITY)
              .copy(email = null)
      db.collection(PUBLIC_PROFILES_PATH).document(profile.id).set(profile.toMap()).await()
    }

    val results = repository.searchUserProfiles("", Section.CYBER_SECURITY, 3).toList()
    assertEquals(3, results.size)
  }

  @Ignore("To re-add once implemented")
  @Test
  fun searchMatchesMultipleKeywords() = runTest {
    db.collection(PUBLIC_PROFILES_PATH)
        .document(testProfile1.id)
        .set(testProfile1.copy(email = null).toMap())
        .await()

    val results = repository.searchUserProfiles("John Doe", null, 10).toList()
    assertTrue(results.any { it.name == "John" && it.lastName == "Doe" })
  }
}
