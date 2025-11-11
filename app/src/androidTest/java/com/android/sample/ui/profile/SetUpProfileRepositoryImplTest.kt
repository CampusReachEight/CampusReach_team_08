package com.android.sample.model.profile.setup

import com.android.sample.ui.profile.setup.SetupProfileState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SetupProfileRepositoryImplTest {

  @Test
  fun getProfile_returnsDefault() = runTest {
    val repo = SetupProfileRepositoryImpl()
    val profile = repo.getProfile()
    assertEquals(SetupProfileState.default(), profile)
  }

  @Test
  fun saveProfile_withValidName_setsSavedTrue() = runTest {
    val repo = SetupProfileRepositoryImpl()
    val toSave =
        SetupProfileState.default()
            .copy(userName = "Alice", userEmail = "alice@example.com", saved = false)
    repo.saveProfile(toSave)

    val stored = repo.getProfile()
    assertTrue("Expected stored profile to be marked saved", stored.saved)
    assertEquals("Alice", stored.userName)
    assertEquals("alice@example.com", stored.userEmail)
  }

  @Test
  fun saveProfile_withBlankName_throwsIllegalArgumentException() = runTest {
    val repo = SetupProfileRepositoryImpl()
    val invalid = SetupProfileState.default().copy(userName = "")

    try {
      repo.saveProfile(invalid)
      fail("Expected IllegalArgumentException when saving profile with blank name")
    } catch (e: IllegalArgumentException) {
      // expected
      assertEquals("Name cannot be empty", e.message)
    }
  }
}
