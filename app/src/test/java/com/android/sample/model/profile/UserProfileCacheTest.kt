package com.android.sample.model.profile

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.profile.setup.UserProfileCache
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
  fun saveProfileAndLoadProfileShouldCorrectlyPersistAndRetrieveAProfile() {
    val profile =
        UserProfile(
            id = "user1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = Uri.parse("content://media/image/1"),
            kudos = 100,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(profile)
    val loadedProfile = userProfileCache.loadProfile("user1")

    assertNotNull(loadedProfile)
    assertEquals(profile, loadedProfile)
  }

  @Test
  fun loadProfileShouldReturnNullWhenProfileDoesNotExist() {
    val loadedProfile = userProfileCache.loadProfile("non_existent_user")
    assertNull(loadedProfile)
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
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(originalProfile)

    val updatedProfile =
        originalProfile.copy(name = "Jane", kudos = 150, section = UserSections.MATHEMATICS)

    userProfileCache.saveProfile(updatedProfile)
    val loadedProfile = userProfileCache.loadProfile("user1")

    assertNotNull(loadedProfile)
    assertEquals(updatedProfile, loadedProfile)
    assertEquals("Jane", loadedProfile?.name)
    assertEquals(150, loadedProfile?.kudos)
    assertEquals(UserSections.MATHEMATICS, loadedProfile?.section)
  }

  @Test
  fun loadAllProfilesShouldReturnAllCachedProfiles() {
    val profiles =
        listOf(
            UserProfile(
                id = "user1",
                name = "Alice",
                lastName = "Smith",
                email = "alice@example.com",
                photo = null,
                kudos = 50,
                section = UserSections.COMPUTER_SCIENCE,
                arrivalDate = Date(1672531200000L)),
            UserProfile(
                id = "user2",
                name = "Bob",
                lastName = "Johnson",
                email = "bob@example.com",
                photo = Uri.parse("content://media/image/2"),
                kudos = 75,
                section = UserSections.PHYSICS,
                arrivalDate = Date(1672617600000L)),
            UserProfile(
                id = "user3",
                name = "Charlie",
                lastName = "Brown",
                email = null,
                photo = null,
                kudos = 25,
                section = UserSections.NONE,
                arrivalDate = Date(1672704000000L)))

    profiles.forEach { userProfileCache.saveProfile(it) }

    val loadedProfiles = userProfileCache.loadAllProfiles()

    assertEquals(profiles.size, loadedProfiles.size)
    assertEquals(profiles.toSet(), loadedProfiles.toSet())
  }

  @Test
  fun loadAllProfilesShouldReturnEmptyListWhenCacheIsEmpty() {
    val loadedProfiles = userProfileCache.loadAllProfiles()
    assertTrue(loadedProfiles.isEmpty())
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
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile)
    assertTrue(userProfileCache.hasProfile("user1"))

    val deleted = userProfileCache.deleteProfile("user1")

    assertTrue(deleted)
    assertFalse(userProfileCache.hasProfile("user1"))
    assertNull(userProfileCache.loadProfile("user1"))
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
            section = UserSections.PHYSICS,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile1)
    userProfileCache.saveProfile(profile2)

    userProfileCache.deleteProfile("user1")

    assertNull(userProfileCache.loadProfile("user1"))
    assertNotNull(userProfileCache.loadProfile("user2"))
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
    val profiles =
        listOf(
            UserProfile(
                id = "user1",
                name = "Alice",
                lastName = "Smith",
                email = "alice@example.com",
                photo = null,
                kudos = 50,
                section = UserSections.COMPUTER_SCIENCE,
                arrivalDate = Date()),
            UserProfile(
                id = "user2",
                name = "Bob",
                lastName = "Johnson",
                email = "bob@example.com",
                photo = null,
                kudos = 75,
                section = UserSections.PHYSICS,
                arrivalDate = Date()))

    profiles.forEach { userProfileCache.saveProfile(it) }
    assertTrue(userProfileCache.loadAllProfiles().isNotEmpty())

    userProfileCache.clearAll()

    assertTrue(userProfileCache.loadAllProfiles().isEmpty())
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
            email =
                """
                test+special.chars@example.com
            """
                    .trimIndent(),
            photo = Uri.parse("content://media/external/images/media/123"),
            kudos = 9999,
            section = UserSections.ARCHITECTURE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(complexProfile)
    val loadedProfile = userProfileCache.loadProfile("complex-user-456")

    assertNotNull(loadedProfile)
    assertEquals(complexProfile, loadedProfile)
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
            section = UserSections.NONE,
            arrivalDate = Date(1672531200000L))

    userProfileCache.saveProfile(profileWithNulls)
    val loadedProfile = userProfileCache.loadProfile("user_with_nulls")

    assertNotNull(loadedProfile)
    assertNull(loadedProfile?.email)
    assertNull(loadedProfile?.photo)
    assertEquals(profileWithNulls, loadedProfile)
  }

  @Test
  fun serializationHandlesAllUserSectionsEnumValues() {
    val sections = UserSections.values()

    sections.forEach { section ->
      val profile =
          UserProfile(
              id = "user_${section.name}",
              name = "Test",
              lastName = "User",
              email = "test@example.com",
              photo = null,
              kudos = 50,
              section = section,
              arrivalDate = Date(1672531200000L))

      userProfileCache.saveProfile(profile)
      val loadedProfile = userProfileCache.loadProfile("user_${section.name}")

      assertNotNull(loadedProfile)
      assertEquals(section, loadedProfile?.section)
    }
  }

  @Test
  fun loadProfileShouldHandleCorruptedJsonGracefully() {
    // Manually create a corrupted JSON file
    val file = File(File(context.cacheDir, "user_profiles_cache"), "corrupted_user.json")
    file.parentFile?.mkdirs()
    file.writeText("{ invalid json }")

    val loadedProfile = userProfileCache.loadProfile("corrupted_user")

    assertNull(loadedProfile)

    // Clean up
    file.delete()
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
            section = UserSections.PHYSICS,
            arrivalDate = Date(1672617600000L))

    userProfileCache.saveProfile(profile1)
    userProfileCache.saveProfile(profile2)

    // Update profile1
    val updatedProfile1 = profile1.copy(kudos = 150)
    userProfileCache.saveProfile(updatedProfile1)

    // Verify profile2 was not affected
    val loadedProfile2 = userProfileCache.loadProfile("user2")
    assertEquals(profile2, loadedProfile2)

    // Verify profile1 was updated
    val loadedProfile1 = userProfileCache.loadProfile("user1")
    assertEquals(150, loadedProfile1?.kudos)
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
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    userProfileCache.saveProfile(profile)
    val loadedProfile = userProfileCache.loadProfile("user-with-special_chars.123")

    assertNotNull(loadedProfile)
    assertEquals(profile, loadedProfile)
  }
}