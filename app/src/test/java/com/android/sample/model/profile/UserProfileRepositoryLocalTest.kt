package com.android.sample.model.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Date
import kotlinx.coroutines.runBlocking
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
      email: String = "john@example.com",
      kudos: Int = 100
  ): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        lastName = "Doe",
        email = email,
        photo = null,
        kudos = kudos,
        section = Section.COMPUTER_SCIENCE,
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
  fun addAndGetUserProfile_successful() = runTest {
    val profile = createTestProfile()
    repository.addUserProfile(profile)

    val retrieved = repository.getUserProfile("user1")
    assertEquals(profile.id, retrieved.id)
    assertEquals(profile.name, retrieved.name)
  }

  @Test
  fun getUserProfile_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getUserProfile("nonexistent") }
    }
  }

  @Test
  fun getAllUserProfiles_returnsCorrectCount() = runTest {
    repository.addUserProfile(createTestProfile(id = "user1"))
    repository.addUserProfile(createTestProfile(id = "user2"))

    assertEquals(2, repository.getAllUserProfiles().size)
  }

  @Test
  fun updateUserProfile_successful() = runTest {
    val profile = createTestProfile()
    repository.addUserProfile(profile)

    val updated = profile.copy(kudos = 200)
    repository.updateUserProfile("user1", updated)

    assertEquals(200, repository.getUserProfile("user1").kudos)
  }

  @Test
  fun updateUserProfile_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.updateUserProfile("nonexistent", createTestProfile()) }
    }
  }

  @Test
  fun deleteUserProfile_successful() = runTest {
    repository.addUserProfile(createTestProfile())
    repository.deleteUserProfile("user1")

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getUserProfile("user1") }
    }
  }

  @Test
  fun deleteUserProfile_nonExistent_throwsException() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteUserProfile("nonexistent") }
    }
  }

  @Test
  fun clear_removesAllProfiles() = runTest {
    repository.addUserProfile(createTestProfile(id = "user1"))
    repository.addUserProfile(createTestProfile(id = "user2"))
    repository.clear()

    assertEquals(0, repository.getAllUserProfiles().size)
  }
}
