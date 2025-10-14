package com.android.sample.model.profile

import java.util.UUID

/**
 * Local in-memory implementation of UserProfileRepository for testing and development. All data is
 * stored in memory and will be lost when the app is closed.
 */
class UserProfileRepositoryLocal : UserProfileRepository {

  private val profiles = mutableMapOf<String, UserProfile>()

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllUserProfiles(): List<UserProfile> {
    return profiles.values.toList()
  }

  override suspend fun getUserProfile(userId: String): UserProfile {
    return profiles[userId] ?: throw NoSuchElementException("UserProfile with ID $userId not found")
  }

  override suspend fun addUserProfile(userProfile: UserProfile) {
    profiles[userProfile.id] = userProfile
  }

  override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {
    if (!profiles.containsKey(userId)) {
      throw NoSuchElementException("UserProfile with ID $userId not found")
    }
    profiles[userId] = updatedProfile
  }

  override suspend fun deleteUserProfile(userId: String) {
    if (!profiles.containsKey(userId)) {
      throw NoSuchElementException("UserProfile with ID $userId not found")
    }
    profiles.remove(userId)
  }

  /** Clears all profiles from the repository. Useful for testing. */
  fun clear() {
    profiles.clear()
  }
}
