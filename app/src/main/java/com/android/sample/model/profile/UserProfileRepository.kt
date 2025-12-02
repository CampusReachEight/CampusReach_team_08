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

  /**
   * Searches public user profiles by name (first/last). Uses Firestore queries to minimize data
   * transfer.
   *
   * @param query Search query (minimum 2 characters)
   * @param limit Maximum number of results (default 20)
   * @return List of matching user profiles (without loading photos to save bandwidth)
   */
  suspend fun searchUserProfiles(query: String, limit: Int = 20): List<UserProfile>

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

  /**
   * Follow a user. Adds currentUserId to targetUserId's followers list, and adds targetUserId to
   * currentUserId's following list.
   *
   * @param targetUserId The user to follow
   * @throws IllegalStateException if no authenticated user
   * @throws IllegalArgumentException if trying to follow yourself
   */
  suspend fun followUser(targetUserId: String)

  /**
   * Unfollow a user. Removes currentUserId from targetUserId's followers list, and removes
   * targetUserId from currentUserId's following list.
   *
   * @param targetUserId The user to unfollow
   * @throws IllegalStateException if no authenticated user
   * @throws IllegalArgumentException if trying to unfollow yourself
   */
  suspend fun unfollowUser(targetUserId: String)

  /**
   * Check if the current user is following a specific user.
   *
   * @param targetUserId The user to check
   * @return true if current user follows target user, false otherwise
   * @throws IllegalStateException if no authenticated user
   */
  suspend fun isFollowing(targetUserId: String): Boolean

  /**
   * Get the list of users the current user is following.
   *
   * @return List of user IDs
   * @throws IllegalStateException if no authenticated user
   */
  suspend fun getFollowing(): List<String>

  /**
   * Get the list of users following the current user.
   *
   * @return List of user IDs
   * @throws IllegalStateException if no authenticated user
   */
  suspend fun getFollowers(): List<String>
}
