package com.android.sample.model.profile

interface UserProfileRepository {
  /** Generates and returns a new unique identifier for a user profile. */
  fun getNewUid(): String

  /** Gives the current user id. */
  fun getCurrentUserId(): String

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

  // ============================================================================================
  // DEPRECATED: searchUserProfiles
  // ============================================================================================
  // This method is commented out in favor of local Lucene-based search via
  // LuceneProfileSearchEngine.
  //
  // Rationale:
  // 1. Server-side Firestore prefix queries (nameLowercase >= query, < query+\uf8ff) cannot be
  //    combined with client-side range/facet filters without the "limit mismatch" problem:
  //    - If we request 20 profiles from server, then apply kudos range [100, 500] + section filter,
  //      we may end up with only 2 results — incomplete data for the user.
  //    - Over-fetching (requesting 200 to filter to 20) wastes bandwidth and is unpredictable.
  //
  // 2. Local Lucene approach (same as LuceneRequestSearchEngine for Requests):
  //    - Load all profiles once via getAllUserProfiles()
  //    - Index locally with Lucene on name, lastName, section
  //    - All filtering (search + section facet + kudos/help range) happens client-side in one pass
  //    - Provides instant search latency and full offline support
  //
  // See: LuceneProfileSearchEngine.kt for the replacement implementation.
  // ============================================================================================
  //
  // /**
  //  * Searches public user profiles by name (first/last). Uses Firestore queries to minimize data
  //  * transfer.
  //  *
  //  * @param query Search query (minimum 2 characters)
  //  * @param limit Maximum number of results (default 20)
  //  * @return List of matching user profiles (without loading photos to save bandwidth)
  //  */
  // suspend fun searchUserProfiles(query: String, limit: Int = 20): List<UserProfile>

  /**
   * Awards kudos to a user by incrementing their kudos count.
   *
   * @param userId The unique identifier of the user to award kudos to.
   * @param amount The amount of kudos to award (must be positive).
   * @throws IllegalArgumentException if amount is not positive or exceeds safety limits.
   * @throws NoSuchElementException if the user profile is not found.
   */
  suspend fun awardKudos(userId: String, amount: Int)

  /**
   * Awards kudos to multiple users in a single atomic transaction. If any award fails, all awards
   * are rolled back.
   *
   * @param awards Map of userId to kudos amount.
   * @throws IllegalArgumentException if any amount is invalid.
   * @throws NoSuchElementException if any user profile is not found.
   * @throws Exception if the transaction fails.
   */
  suspend fun awardKudosBatch(awards: Map<String, Int>)

  /**
   * Records help received by a user by incrementing their help received count.
   *
   * @param userId The unique identifier of the user receiving help.
   * @param amount The amount of help to record (must be positive).
   * @throws IllegalArgumentException if amount is not positive or exceeds safety limits.
   * @throws NoSuchElementException if the user profile is not found.
   */
  suspend fun receiveHelp(userId: String, amount: Int)
}
