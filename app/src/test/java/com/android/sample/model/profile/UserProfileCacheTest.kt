package com.android.sample.model.profile

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.profile.UserSections
import java.io.File
import java.util.Date
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileCacheTest {

  private lateinit var userProfileCache: UserProfileCache
  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    userProfileCache = UserProfileCache(context)
    // Clean up before each test to ensure isolation
    userProfileCache.clearAll()
  }

  @After
  fun teardown() {
    // Clean up after each test
    userProfileCache.clearAll()
  }

  @Test
  fun saveProfileAndGetProfileByIdShouldCorrectlyPersistAndRetrieveAProfile() {
    val profile =
        UserProfile(
            id = "user1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = Uri.parse("content://media/image/1"),
            kudos = 100,
            helpReceived = 5,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(profile)
    val retrievedProfile = userProfileCache.getProfileById("user1")

    assertEquals(profile, retrievedProfile)
  }

  @Test
  fun getProfileByIdShouldThrowNoSuchElementExceptionWhenProfileDoesNotExist() {
    try {
      userProfileCache.getProfileById("non_existent_user")
      fail("Expected NoSuchElementException")
    } catch (e: NoSuchElementException) {
      assertTrue(e.message?.contains("not found in cache") == true)
    }
  }

  @Test
  fun saveProfileShouldUpdateExistingProfileWhenSavedAgain() {
    val originalProfile =
        UserProfile(
            id = "user1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = null,
            kudos = 50,
            helpReceived = 2,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(originalProfile)

    val updatedProfile =
        originalProfile.copy(
            name = "Jane", kudos = 150, helpReceived = 10, section = UserSections.MATHEMATICS)

    userProfileCache.saveProfile(updatedProfile)
    val retrievedProfile = userProfileCache.getProfileById("user1")

    assertEquals(updatedProfile, retrievedProfile)
    assertEquals("Jane", retrievedProfile.name)
    assertEquals(150, retrievedProfile.kudos)
    assertEquals(10, retrievedProfile.helpReceived)
    assertEquals(UserSections.MATHEMATICS, retrievedProfile.section)
  }

  @Test
  fun saveProfileShouldHandleMultipleProfiles() {
    val profile1 =
        UserProfile(
            id = "user1",
            name = "Alice",
            lastName = "Smith",
            email = "alice@example.com",
            photo = null,
            kudos = 50,
            helpReceived = 3,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date(1672531200000L))

    val profile2 =
        UserProfile(
            id = "user2",
            name = "Bob",
            lastName = "Johnson",
            email = "bob@example.com",
            photo = Uri.parse("content://media/image/2"),
            kudos = 75,
            helpReceived = 7,
            section = UserSections.PHYSICS,
            arrivalDate = Date(1672617600000L))

    val profile3 =
        UserProfile(
            id = "user3",
            name = "Charlie",
            lastName = "Brown",
            email = null,
            photo = null,
            kudos = 25,
            helpReceived = 1,
            section = UserSections.NONE,
            arrivalDate = Date(1672704000000L))

    userProfileCache.saveProfile(profile1)
    userProfileCache.saveProfile(profile2)
    userProfileCache.saveProfile(profile3)

    assertEquals(profile1, userProfileCache.getProfileById("user1"))
    assertEquals(profile2, userProfileCache.getProfileById("user2"))
    assertEquals(profile3, userProfileCache.getProfileById("user3"))
  }

  @Test
  fun deleteProfileShouldRemoveASpecificProfileFromCache() {
    val profile =
        UserProfile(
            id = "user1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = null,
            kudos = 100,
            helpReceived = 8,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile)
    assertTrue(userProfileCache.hasProfile("user1"))

    val deleted = userProfileCache.deleteProfile("user1")

    assertTrue(deleted)
    assertFalse(userProfileCache.hasProfile("user1"))

    try {
      userProfileCache.getProfileById("user1")
      fail("Expected NoSuchElementException")
    } catch (e: NoSuchElementException) {
      assertTrue(e.message?.contains("not found in cache") == true)
    }
  }

  @Test
  fun deleteProfileShouldReturnFalseWhenProfileDoesNotExist() {
    val deleted = userProfileCache.deleteProfile("non_existent_user")
    assertFalse(deleted)
  }

  @Test
  fun deleteProfileShouldOnlyDeleteSpecifiedProfileAndKeepOthers() {
    val profile1 =
        UserProfile(
            id = "user1",
            name = "Alice",
            lastName = "Smith",
            email = "alice@example.com",
            photo = null,
            kudos = 50,
            helpReceived = 4,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    val profile2 =
        UserProfile(
            id = "user2",
            name = "Bob",
            lastName = "Johnson",
            email = "bob@example.com",
            photo = null,
            kudos = 75,
            helpReceived = 6,
            section = UserSections.PHYSICS,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile1)
    userProfileCache.saveProfile(profile2)

    userProfileCache.deleteProfile("user1")

    assertFalse(userProfileCache.hasProfile("user1"))
    assertTrue(userProfileCache.hasProfile("user2"))
    assertEquals(profile2, userProfileCache.getProfileById("user2"))
  }

  @Test
  fun hasProfileShouldReturnTrueWhenProfileExists() {
    val profile =
        UserProfile(
            id = "user1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = null,
            kudos = 100,
            helpReceived = 12,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile)

    assertTrue(userProfileCache.hasProfile("user1"))
  }

  @Test
  fun hasProfileShouldReturnFalseWhenProfileDoesNotExist() {
    assertFalse(userProfileCache.hasProfile("non_existent_user"))
  }

  @Test
  fun clearAllShouldDeleteAllCachedProfiles() {
    val profile1 =
        UserProfile(
            id = "user1",
            name = "Alice",
            lastName = "Smith",
            email = "alice@example.com",
            photo = null,
            kudos = 50,
            helpReceived = 2,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    val profile2 =
        UserProfile(
            id = "user2",
            name = "Bob",
            lastName = "Johnson",
            email = "bob@example.com",
            photo = null,
            kudos = 75,
            helpReceived = 9,
            section = UserSections.PHYSICS,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile1)
    userProfileCache.saveProfile(profile2)

    assertTrue(userProfileCache.hasProfile("user1"))
    assertTrue(userProfileCache.hasProfile("user2"))

    userProfileCache.clearAll()

    assertFalse(userProfileCache.hasProfile("user1"))
    assertFalse(userProfileCache.hasProfile("user2"))
  }

  @Test
  fun serializationAndDeserializationHandlesAllDataTypesCorrectly() {
    val complexProfile =
        UserProfile(
            id = "complex-user-456",
            name = "Test: All The Things",
            lastName = "O'Connor-Smith",
            email = "test+special.chars@example.com",
            photo = Uri.parse("content://media/external/images/media/123"),
            kudos = 9999,
            helpReceived = 150,
            section = UserSections.ARCHITECTURE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(complexProfile)
    val retrievedProfile = userProfileCache.getProfileById("complex-user-456")

    assertEquals(complexProfile, retrievedProfile)
  }

  @Test
  fun serializationHandlesNullValuesCorrectly() {
    val profileWithNulls =
        UserProfile(
            id = "user_with_nulls",
            name = "John",
            lastName = "Doe",
            email = null,
            photo = null,
            kudos = 0,
            helpReceived = 0,
            section = UserSections.NONE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(profileWithNulls)
    val retrievedProfile = userProfileCache.getProfileById("user_with_nulls")

    assertNull(retrievedProfile.email)
    assertNull(retrievedProfile.photo)
    assertEquals(profileWithNulls, retrievedProfile)
  }

  @Test
  fun serializationHandlesAllUserSectionsEnumValues() {
    val sections = UserSections.entries

    sections.forEach { section ->
      val profile =
          UserProfile(
              id = "user_${section.name}",
              name = "Test",
              lastName = "User",
              email = "test@example.com",
              photo = null,
              kudos = 50,
              helpReceived = 3,
              section = section,
              arrivalDate = Date(1672531200000L))

      userProfileCache.saveProfile(profile)
      val retrievedProfile = userProfileCache.getProfileById("user_${section.name}")

      assertEquals(section, retrievedProfile.section)
    }
  }

  @Test
  fun getProfileByIdShouldThrowSerializationExceptionWhenJsonIsCorrupted() {
    // Manually create a corrupted JSON file
    val file = File(File(context.cacheDir, "user_profiles_cache"), "corrupted_user.json")
    file.parentFile?.mkdirs()
    file.writeText("{ invalid json }")

    try {
      userProfileCache.getProfileById("corrupted_user")
      fail("Expected Exception")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Failed to read profile") == true)
    } finally {
      file.delete()
    }
  }

  @Test
  fun saveProfileShouldNotAffectOtherCachedProfiles() {
    val profile1 =
        UserProfile(
            id = "user1",
            name = "Alice",
            lastName = "Smith",
            email = "alice@example.com",
            photo = null,
            kudos = 50,
            helpReceived = 5,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date(1672531200000L))

    val profile2 =
        UserProfile(
            id = "user2",
            name = "Bob",
            lastName = "Johnson",
            email = "bob@example.com",
            photo = null,
            kudos = 75,
            helpReceived = 11,
            section = UserSections.PHYSICS,
            arrivalDate = Date(1672617600000L))

    userProfileCache.saveProfile(profile1)
    userProfileCache.saveProfile(profile2)

    // Update profile1
    val updatedProfile1 = profile1.copy(kudos = 150)
    userProfileCache.saveProfile(updatedProfile1)

    // Verify profile2 was not affected
    val retrievedProfile2 = userProfileCache.getProfileById("user2")
    assertEquals(profile2, retrievedProfile2)

    // Verify profile1 was updated
    val retrievedProfile1 = userProfileCache.getProfileById("user1")
    assertEquals(150, retrievedProfile1.kudos)
  }

  @Test
  fun cacheShouldHandleSpecialCharactersInProfileIds() {
    val profile =
        UserProfile(
            id = "user-with-special_chars.123",
            name = "John",
            lastName = "Doe",
            email = "john@example.com",
            photo = null,
            kudos = 100,
            helpReceived = 20,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile)
    val retrievedProfile = userProfileCache.getProfileById("user-with-special_chars.123")

    assertEquals(profile, retrievedProfile)
  }
}
