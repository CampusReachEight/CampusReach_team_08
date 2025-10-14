package com.android.sample.model.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileRepositoryLocalTest {

  private lateinit var repository: UserProfileRepositoryLocal

  private fun createTestProfile(
      id: String = "user1",
      name: String = "John",
      lastName: String = "Doe",
      email: String = "john@example.com",
      kudos: Int = 100,
      section: Section = Section.COMPUTER_SCIENCE
  ): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        lastName = lastName,
        email = email,
        photo = null,
        kudos = kudos,
        section = section,
        arrivalDate = Date())
  }

  @Before
  fun setup() {
    repository = UserProfileRepositoryLocal()
    repository.clear()
  }

  @Test
  fun getNewUid_returnsValidUid() {
    val uid = repository.getNewUid()
    assertNotNull(uid)
    assertTrue(uid.isNotEmpty())
  }

  @Test
  fun addUserProfile_addsProfileSuccessfully() = runTest {
    val profile = createTestProfile()
    repository.addUserProfile(profile)

    val retrieved = repository.getUserProfile("user1")
    assertEquals(profile.id, retrieved.id)
    assertEquals(profile.name, retrieved.name)
    assertEquals(profile.email, retrieved.email)
  }

  @Test
  fun getUserProfile_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.getUserProfile("nonexistent") }
    }
  }

  @Test
  fun getAllUserProfiles_returnsAllProfiles() = runTest {
    val profile1 = createTestProfile(id = "user1")
    val profile2 = createTestProfile(id = "user2", name = "Jane")

    repository.addUserProfile(profile1)
    repository.addUserProfile(profile2)

    val allProfiles = repository.getAllUserProfiles()
    assertEquals(2, allProfiles.size)
  }

  @Test
  fun getAllUserProfiles_returnsFullProfileData() = runTest {
    val profile1 = createTestProfile(id = "user1", email = "user1@example.com")
    val profile2 = createTestProfile(id = "user2", email = "user2@example.com")

    repository.addUserProfile(profile1)
    repository.addUserProfile(profile2)

    val allProfiles = repository.getAllUserProfiles()

    val firstProfile = allProfiles.find { it.id == "user1" }
    val secondProfile = allProfiles.find { it.id == "user2" }

    assertNotNull(firstProfile?.email)
    assertNotNull(secondProfile?.email)
    assertEquals("user1@example.com", firstProfile?.email)
    assertEquals("user2@example.com", secondProfile?.email)
  }

  @Test
  fun getUserProfile_returnsFullProfileData() = runTest {
    val profile = createTestProfile(id = "user1", email = "user1@example.com")
    repository.addUserProfile(profile)

    val retrieved = repository.getUserProfile("user1")

    assertEquals("user1@example.com", retrieved.email)
  }

  @Test
  fun updateUserProfile_updatesSuccessfully() = runTest {
    val profile = createTestProfile(id = "user1", kudos = 100)
    repository.addUserProfile(profile)

    val updated = profile.copy(kudos = 200)
    repository.updateUserProfile("user1", updated)

    val retrieved = repository.getUserProfile("user1")
    assertEquals(200, retrieved.kudos)
  }

  @Test
  fun updateUserProfile_nonExistent_throwsException() = runTest {
    val profile = createTestProfile(id = "user1")

    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.updateUserProfile("user1", profile) }
    }
  }

  @Test
  fun deleteUserProfile_deletesSuccessfully() = runTest {
    val profile = createTestProfile(id = "user1")
    repository.addUserProfile(profile)

    repository.deleteUserProfile("user1")

    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.getUserProfile("user1") }
    }
  }

  @Test
  fun deleteUserProfile_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runTest { repository.deleteUserProfile("nonexistent") }
    }
  }

  @Test
  fun clear_removesAllProfiles() = runTest {
    val profile1 = createTestProfile(id = "user1")
    val profile2 = createTestProfile(id = "user2")

    repository.addUserProfile(profile1)
    repository.addUserProfile(profile2)

    repository.clear()

    val allProfiles = repository.getAllUserProfiles()
    assertEquals(0, allProfiles.size)
  }
}
