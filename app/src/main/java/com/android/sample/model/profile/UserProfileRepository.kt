package com.android.sample.model.profile

import kotlinx.coroutines.flow.Flow

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

  /**
   * Searches for user profiles matching the given query and optional section filter. The
   * implementation should return results as a Flow to support real-time updates and not block the
   * main thread.
   *
   * @param query The search query string.
   * @param section Optional section to filter the search results.
   * @param resultsPerPage The maximum number of results to return per page (default is 20).
   * @return A Flow emitting UserProfile objects that match the search criteria.
   */
  suspend fun searchUserProfiles(
      query: String,
      section: Section?,
      resultsPerPage: Int = 20,
  ): Flow<UserProfile>
}
