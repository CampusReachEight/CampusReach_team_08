package com.android.sample.model.profile

interface UserProfileRepository {
  /** Generates and returns a new unique identifier for a user profile. */
  fun getNewUid(): String

  /**
   * Retrieves all user profiles from the repository.
   *
   * @return A list of all user profiles.
   */
  suspend fun getAllUserProfiles(): List<UserProfile>

  /**
   * Retrieves a specific user profile by its unique identifier.
   *
   * @param userId The unique identifier of the user profile to retrieve.
   * @return The UserProfile with the specified ID.
   * @throws Exception if the user profile is not found.
   */
  suspend fun getUserProfile(userId: String): UserProfile

  /**
   * Adds a new user profile to the repository.
   *
   * @param userProfile The UserProfile to add.
   */
  suspend fun addUserProfile(userProfile: UserProfile)

  /**
   * Updates an existing user profile in the repository.
   *
   * @param userId The unique identifier of the user profile to update.
   * @param updatedProfile The new state of the user profile.
   * @throws Exception if the user profile is not found.
   */
  suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile)

  /**
   * Deletes a user profile from the repository.
   *
   * @param userId The unique identifier of the user profile to delete.
   * @throws Exception if the user profile is not found.
   */
  suspend fun deleteUserProfile(userId: String)
}
