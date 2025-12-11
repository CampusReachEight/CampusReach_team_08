package com.android.sample.model.profile

private const val LIMIT = 20

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
  suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile>

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
   * Follows a target user. Creates a follower/following relationship between two users. Updates
   * both users' follower/following counts atomically.
   *
   * This operation:
   * - Adds currentUserId to targetUser's followers subcollection
   * - Adds targetUserId to currentUser's following subcollection
   * - Increments targetUser's followerCount
   * - Increments currentUser's followingCount
   *
   * @param currentUserId The ID of the user who wants to follow
   * @param targetUserId The ID of the user to be followed
   * @throws IllegalArgumentException if currentUserId == targetUserId (cannot follow yourself)
   * @throws IllegalStateException if already following the target user
   * @throws NoSuchElementException if either user profile is not found
   */
  suspend fun followUser(currentUserId: String, targetUserId: String)

  /**
   * Unfollows a target user. Removes the follower/following relationship between two users. Updates
   * both users' follower/following counts atomically.
   *
   * This operation:
   * - Removes currentUserId from targetUser's followers subcollection
   * - Removes targetUserId from currentUser's following subcollection
   * - Decrements targetUser's followerCount
   * - Decrements currentUser's followingCount
   *
   * @param currentUserId The ID of the user who wants to unfollow
   * @param targetUserId The ID of the user to be unfollowed
   * @throws IllegalArgumentException if currentUserId == targetUserId (cannot unfollow yourself)
   * @throws IllegalStateException if not currently following the target user
   * @throws NoSuchElementException if either user profile is not found
   */
  suspend fun unfollowUser(currentUserId: String, targetUserId: String)

  /**
   * Checks if a user is following another user.
   *
   * @param currentUserId The ID of the potential follower
   * @param targetUserId The ID of the potential followee
   * @return true if currentUserId follows targetUserId, false otherwise
   */
  suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean

  /**
   * Retrieves the number of followers for a specific user.
   *
   * @param userId The unique identifier of the user
   * @return The number of followers
   * @throws NoSuchElementException if the user profile is not found
   */
  suspend fun getFollowerCount(userId: String): Int

  /**
   * Retrieves the number of users that a specific user is following.
   *
   * @param userId The unique identifier of the user
   * @return The number of users being followed
   * @throws NoSuchElementException if the user profile is not found
   */
  suspend fun getFollowingCount(userId: String): Int

  /**
   * Retrieves a list of user profiles who are following the specified user. Returns full
   * UserProfile objects for easy display in UI.
   *
   * @param userId The unique identifier of the user whose followers to retrieve
   * @return List of UserProfile objects representing followers
   * @throws NoSuchElementException if the user profile is not found
   */
  suspend fun getFollowerIds(userId: String): List<String>

  /**
   * Retrieves a list of user profiles that the specified user is following. Returns full
   * UserProfile objects for easy display in UI.
   *
   * @param userId The unique identifier of the user whose following list to retrieve
   * @return List of UserProfile objects representing users being followed
   * @throws NoSuchElementException if the user profile is not found
   */
  suspend fun getFollowingIds(userId: String): List<String>
}
