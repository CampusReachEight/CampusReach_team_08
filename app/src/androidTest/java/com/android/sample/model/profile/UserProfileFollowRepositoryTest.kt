package com.android.sample.model.profile

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.profile.UserSections
import com.android.sample.utils.BaseEmulatorTest
import com.google.firebase.firestore.Source
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for follower/following functionality in UserProfileRepositoryFirestore. Tests
 * the complete follow/unfollow flow including:
 * - Following and unfollowing users
 * - Checking follow relationships
 * - Getting follower/following counts
 * - Retrieving follower/following lists
 */
@RunWith(AndroidJUnit4::class)
class UserProfileFollowRepositoryTest : BaseEmulatorTest() {

  private lateinit var repository: UserProfileRepositoryFirestore

  private companion object {
    // Timing constants for reliable CI execution
    private const val FIRESTORE_WRITE_DELAY_MS: Long = 1000L
    private const val FOLLOWER_SETUP_DELAY_MS: Long = 500L
    private const val BATCH_OPERATION_DELAY_MS: Long = 1500L

    // Test user credentials
    private const val USER_A_EMAIL = "userA@example.com"
    private const val USER_A_PASSWORD = "password123"
    private const val USER_A_NAME = "Alice"
    private const val USER_A_LASTNAME = "Anderson"

    private const val USER_B_EMAIL = "userB@example.com"
    private const val USER_B_PASSWORD = "password123"
    private const val USER_B_NAME = "Bob"
    private const val USER_B_LASTNAME = "Brown"

    private const val USER_C_EMAIL = "userC@example.com"
    private const val USER_C_PASSWORD = "password123"
    private const val USER_C_NAME = "Charlie"
    private const val USER_C_LASTNAME = "Chen"

    private const val USER_D_EMAIL = "userD@example.com"
    private const val USER_D_PASSWORD = "password123"
    private const val USER_D_NAME = "Diana"
    private const val USER_D_LASTNAME = "Davis"

    // Initial counts
    private const val INITIAL_COUNT = 0
    private const val ONE_FOLLOWER = 1
    private const val TWO_FOLLOWERS = 2
    private const val THREE_FOLLOWERS = 3
  }

  @Before
  override fun setUp() {
    super.setUp()
    runBlocking {
      auth.signOut()
      signInUser()
      clearAllTestData()
    }
    repository = UserProfileRepositoryFirestore(db)
  }

  @After
  override fun tearDown() {
    runBlocking { clearAllTestData() }
    super.tearDown()
  }

  // ==================== HELPER FUNCTIONS ====================

  /** Generates a UserProfile with the given parameters. */
  private fun generateProfile(
      id: String,
      name: String,
      lastName: String,
      email: String,
      section: UserSections = UserSections.COMPUTER_SCIENCE
  ): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        lastName = lastName,
        email = email,
        section = section,
        photo = null,
        kudos = 0,
        helpReceived = 0,
        followerCount = 0,
        followingCount = 0,
        arrivalDate = Date())
  }

  /**
   * Creates and signs in a user, then adds their profile to the repository. Returns the userId of
   * the created user.
   */
  private suspend fun setupUserWithProfile(
      email: String,
      password: String,
      name: String,
      lastName: String
  ): String {
    createAndSignInUser(email, password)
    val userId = auth.currentUser?.uid ?: error("Failed to create user with email: $email")

    val profile = generateProfile(id = userId, name = name, lastName = lastName, email = email)

    repository.addUserProfile(profile)
    delay(FIRESTORE_WRITE_DELAY_MS)

    return userId
  }

  /**
   * Helper to setup two users for follow/unfollow tests. Returns Pair(userAId, userBId) where userA
   * will be the follower.
   */
  private suspend fun setupTwoUsers(): Pair<String, String> {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val userBId = setupUserWithProfile(USER_B_EMAIL, USER_B_PASSWORD, USER_B_NAME, USER_B_LASTNAME)

    // Sign back in as userA for follow operations
    signInUser(USER_A_EMAIL, USER_A_PASSWORD)
    delay(FOLLOWER_SETUP_DELAY_MS)

    return Pair(userAId, userBId)
  }

  /**
   * Helper to setup three users for multi-follower tests. Returns Triple(userAId, userBId,
   * userCId).
   */
  private suspend fun setupThreeUsers(): Triple<String, String, String> {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val userBId = setupUserWithProfile(USER_B_EMAIL, USER_B_PASSWORD, USER_B_NAME, USER_B_LASTNAME)

    val userCId = setupUserWithProfile(USER_C_EMAIL, USER_C_PASSWORD, USER_C_NAME, USER_C_LASTNAME)

    signInUser(USER_A_EMAIL, USER_A_PASSWORD)
    delay(FOLLOWER_SETUP_DELAY_MS)

    return Triple(userAId, userBId, userCId)
  }

  /**
   * Clears all test data from Firestore including profiles and follower/following subcollections.
   */
  private suspend fun clearAllTestData() {
    try {
      val publicSnapshot = db.collection(PUBLIC_PROFILES_PATH).get().await()
      val privateSnapshot = db.collection(PRIVATE_PROFILES_PATH).get().await()

      val batch = db.batch()

      // Delete all public profiles and their subcollections
      for (doc in publicSnapshot.documents) {
        // Delete followers subcollection
        val followersSnapshot = doc.reference.collection(FOLLOWERS_SUBCOLLECTION).get().await()
        followersSnapshot.documents.forEach { batch.delete(it.reference) }

        // Delete following subcollection
        val followingSnapshot = doc.reference.collection(FOLLOWING_SUBCOLLECTION).get().await()
        followingSnapshot.documents.forEach { batch.delete(it.reference) }

        // Delete the profile document itself
        batch.delete(doc.reference)
      }

      // Delete all private profiles
      privateSnapshot.documents.forEach { batch.delete(it.reference) }

      batch.commit().await()
      delay(FOLLOWER_SETUP_DELAY_MS)
    } catch (e: Exception) {
      Log.e("TestCleanup", "Error clearing test data", e)
    }
  }

  /**
   * Verifies that follower/following counts match expected values in both public and private
   * profiles.
   */
  private suspend fun verifyUserCounts(
      userId: String,
      expectedFollowerCount: Int,
      expectedFollowingCount: Int,
      userEmail: String,
      userPassword: String
  ) {
    // Check public profile
    val publicDoc = db.collection(PUBLIC_PROFILES_PATH).document(userId).get(Source.SERVER).await()
    val publicFollowerCount = (publicDoc[FOLLOWER_COUNT_FIELD] as? Number)?.toInt() ?: 0
    val publicFollowingCount = (publicDoc[FOLLOWING_COUNT_FIELD] as? Number)?.toInt() ?: 0

    assertEquals(
        "Public follower count mismatch for user $userId",
        expectedFollowerCount,
        publicFollowerCount)
    assertEquals(
        "Public following count mismatch for user $userId",
        expectedFollowingCount,
        publicFollowingCount)

    // Check private profile (need to sign in as that user)
    signInUser(userEmail, userPassword)
    val privateProfile = repository.getUserProfile(userId)

    assertEquals(
        "Private follower count mismatch for user $userId",
        expectedFollowerCount,
        privateProfile.followerCount)
    assertEquals(
        "Private following count mismatch for user $userId",
        expectedFollowingCount,
        privateProfile.followingCount)
  }

  // ==================== FOLLOW USER TESTS ====================

  @Test
  fun followUser_successfullyCreatesFollowRelationship() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val isFollowing = repository.isFollowing(userAId, userBId)
    assertTrue("UserA should be following UserB", isFollowing)
  }

  @Test
  fun followUser_incrementsFollowerAndFollowingCounts() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify UserA's following count
    verifyUserCounts(
        userId = userAId,
        expectedFollowerCount = INITIAL_COUNT,
        expectedFollowingCount = ONE_FOLLOWER,
        userEmail = USER_A_EMAIL,
        userPassword = USER_A_PASSWORD)

    // Verify UserB's follower count
    verifyUserCounts(
        userId = userBId,
        expectedFollowerCount = ONE_FOLLOWER,
        expectedFollowingCount = INITIAL_COUNT,
        userEmail = USER_B_EMAIL,
        userPassword = USER_B_PASSWORD)
  }

  @Test
  fun followUser_cannotFollowYourself() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.followUser(userAId, userAId) }
        }

    assertTrue(
        "Exception should mention self-follow",
        exception.message?.contains("Cannot follow yourself") == true)
  }

  @Test
  fun followUser_throwsExceptionWhenAlreadyFollowing() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // Follow once
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Try to follow again
    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.followUser(userAId, userBId) }
        }

    assertTrue(
        "Exception should mention already following",
        exception.message?.contains("Already following") == true)
  }

  @Test
  fun followUser_throwsExceptionWhenTargetUserNotFound() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val nonExistentUserId = "non-existent-user-id-12345"

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.followUser(userAId, nonExistentUserId) }
    }
  }

  @Test
  fun followUser_throwsExceptionWhenCurrentUserNotFound() = runTest {
    val userBId = setupUserWithProfile(USER_B_EMAIL, USER_B_PASSWORD, USER_B_NAME, USER_B_LASTNAME)

    val nonExistentUserId = "non-existent-user-id-12345"

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.followUser(nonExistentUserId, userBId) }
    }
  }

  @Test
  fun followUser_createsDocumentsInBothSubcollections() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify follower document exists in UserB's followers subcollection
    val followerDoc =
        db.collection(PUBLIC_PROFILES_PATH)
            .document(userBId)
            .collection(FOLLOWERS_SUBCOLLECTION)
            .document(userAId)
            .get(Source.SERVER)
            .await()

    assertTrue("Follower document should exist in UserB's followers", followerDoc.exists())

    // Verify following document exists in UserA's following subcollection
    val followingDoc =
        db.collection(PUBLIC_PROFILES_PATH)
            .document(userAId)
            .collection(FOLLOWING_SUBCOLLECTION)
            .document(userBId)
            .get(Source.SERVER)
            .await()

    assertTrue("Following document should exist in UserA's following", followingDoc.exists())
  }

  // ==================== UNFOLLOW USER TESTS ====================

  @Test
  fun unfollowUser_successfullyRemovesFollowRelationship() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // First follow
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Then unfollow
    repository.unfollowUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val isFollowing = repository.isFollowing(userAId, userBId)
    assertFalse("UserA should not be following UserB after unfollow", isFollowing)
  }

  @Test
  fun unfollowUser_decrementsFollowerAndFollowingCounts() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // Follow first
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Then unfollow
    repository.unfollowUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify counts are back to zero
    verifyUserCounts(
        userId = userAId,
        expectedFollowerCount = INITIAL_COUNT,
        expectedFollowingCount = INITIAL_COUNT,
        userEmail = USER_A_EMAIL,
        userPassword = USER_A_PASSWORD)

    verifyUserCounts(
        userId = userBId,
        expectedFollowerCount = INITIAL_COUNT,
        expectedFollowingCount = INITIAL_COUNT,
        userEmail = USER_B_EMAIL,
        userPassword = USER_B_PASSWORD)
  }

  @Test
  fun unfollowUser_cannotUnfollowYourself() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.unfollowUser(userAId, userAId) }
        }

    assertTrue(
        "Exception should mention self-unfollow",
        exception.message?.contains("Cannot follow yourself") == true)
  }

  @Test
  fun unfollowUser_throwsExceptionWhenNotCurrentlyFollowing() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // Try to unfollow without following first
    val exception =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { repository.unfollowUser(userAId, userBId) }
        }

    assertTrue(
        "Exception should mention not following",
        exception.message?.contains("Not currently following") == true)
  }

  @Test
  fun unfollowUser_removesDocumentsFromBothSubcollections() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // Follow first
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Then unfollow
    repository.unfollowUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify follower document removed from UserB's followers subcollection
    val followerDoc =
        db.collection(PUBLIC_PROFILES_PATH)
            .document(userBId)
            .collection(FOLLOWERS_SUBCOLLECTION)
            .document(userAId)
            .get(Source.SERVER)
            .await()

    assertFalse("Follower document should be removed from UserB's followers", followerDoc.exists())

    // Verify following document removed from UserA's following subcollection
    val followingDoc =
        db.collection(PUBLIC_PROFILES_PATH)
            .document(userAId)
            .collection(FOLLOWING_SUBCOLLECTION)
            .document(userBId)
            .get(Source.SERVER)
            .await()

    assertFalse(
        "Following document should be removed from UserA's following", followingDoc.exists())
  }

  // ==================== IS FOLLOWING TESTS ====================

  @Test
  fun isFollowing_returnsTrueWhenFollowing() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val result = repository.isFollowing(userAId, userBId)
    assertTrue("isFollowing should return true when following", result)
  }

  @Test
  fun isFollowing_returnsFalseWhenNotFollowing() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    val result = repository.isFollowing(userAId, userBId)
    assertFalse("isFollowing should return false when not following", result)
  }

  @Test
  fun isFollowing_returnsFalseAfterUnfollow() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // Follow then unfollow
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.unfollowUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val result = repository.isFollowing(userAId, userBId)
    assertFalse("isFollowing should return false after unfollow", result)
  }

  @Test
  fun isFollowing_returnsFalseForNonExistentUser() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)
    val nonExistentUserId = "non-existent-user-id-12345"

    val result = repository.isFollowing(userAId, nonExistentUserId)
    assertFalse("isFollowing should return false for non-existent user", result)
  }

  // ==================== GET FOLLOWER/FOLLOWING COUNT TESTS ====================

  @Test
  fun getFollowerCount_returnsZeroInitially() = runTest {
    val userBId = setupUserWithProfile(USER_B_EMAIL, USER_B_PASSWORD, USER_B_NAME, USER_B_LASTNAME)

    val count = repository.getFollowerCount(userBId)
    assertEquals("Initial follower count should be 0", INITIAL_COUNT, count)
  }

  @Test
  fun getFollowerCount_returnsCorrectCountAfterFollow() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val count = repository.getFollowerCount(userBId)
    assertEquals("Follower count should be 1 after one follow", ONE_FOLLOWER, count)
  }

  @Test
  fun getFollowerCount_returnsCorrectCountWithMultipleFollowers() = runTest {
    val (userAId, userBId, userCId) = setupThreeUsers()

    // UserA follows UserB
    signInUser(USER_A_EMAIL, USER_A_PASSWORD)
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // UserC follows UserB
    signInUser(USER_C_EMAIL, USER_C_PASSWORD)
    repository.followUser(userCId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val count = repository.getFollowerCount(userBId)
    assertEquals("Follower count should be 2 with two followers", TWO_FOLLOWERS, count)
  }

  @Test
  fun getFollowerCount_throwsExceptionForNonExistentUser() = runTest {
    val nonExistentUserId = "non-existent-user-id-12345"

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getFollowerCount(nonExistentUserId) }
    }
  }

  @Test
  fun getFollowingCount_returnsZeroInitially() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val count = repository.getFollowingCount(userAId)
    assertEquals("Initial following count should be 0", INITIAL_COUNT, count)
  }

  @Test
  fun getFollowingCount_returnsCorrectCountAfterFollow() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val count = repository.getFollowingCount(userAId)
    assertEquals("Following count should be 1 after one follow", ONE_FOLLOWER, count)
  }

  @Test
  fun getFollowingCount_returnsCorrectCountWithMultipleFollowing() = runTest {
    val (userAId, userBId, userCId) = setupThreeUsers()

    // UserA follows UserB
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // UserA follows UserC
    repository.followUser(userAId, userCId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val count = repository.getFollowingCount(userAId)
    assertEquals("Following count should be 2 when following two users", TWO_FOLLOWERS, count)
  }

  @Test
  fun getFollowingCount_throwsExceptionForNonExistentUser() = runTest {
    val nonExistentUserId = "non-existent-user-id-12345"

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getFollowingCount(nonExistentUserId) }
    }
  }

  // ==================== GET FOLLOWERS LIST TESTS ====================

  @Test
  fun getFollowerIds_returnsEmptyListWhenNoFollowers() = runTest {
    val userBId = setupUserWithProfile(USER_B_EMAIL, USER_B_PASSWORD, USER_B_NAME, USER_B_LASTNAME)

    val followerIds = repository.getFollowerIds(userBId)
    assertTrue("Follower IDs list should be empty initially", followerIds.isEmpty())
  }

  @Test
  fun getFollowerIds_returnsCorrectFollowerId() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val followerIds = repository.getFollowerIds(userBId)

    assertEquals("Should have exactly one follower", ONE_FOLLOWER, followerIds.size)
    assertEquals("Follower ID should match UserA", userAId, followerIds.first())
  }

  @Test
  fun getFollowerIds_returnsMultipleFollowers() = runTest {
    val (userAId, userBId, userCId) = setupThreeUsers()

    // UserA follows UserB
    signInUser(USER_A_EMAIL, USER_A_PASSWORD)
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // UserC follows UserB
    signInUser(USER_C_EMAIL, USER_C_PASSWORD)
    repository.followUser(userCId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val followerIds = repository.getFollowerIds(userBId)

    assertEquals("Should have exactly two followers", TWO_FOLLOWERS, followerIds.size)
    assertTrue("Should contain UserA as follower", followerIds.contains(userAId))
    assertTrue("Should contain UserC as follower", followerIds.contains(userCId))
  }

  @Test
  fun getFollowerIds_throwsExceptionForNonExistentUser() = runTest {
    val nonExistentUserId = "non-existent-user-id-12345"

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getFollowerIds(nonExistentUserId) }
    }
  }

  // ==================== GET FOLLOWING LIST TESTS ====================

  @Test
  fun getFollowingIds_returnsEmptyListWhenNotFollowingAnyone() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)

    val followingIds = repository.getFollowingIds(userAId)
    assertTrue("Following IDs list should be empty initially", followingIds.isEmpty())
  }

  @Test
  fun getFollowingIds_returnsCorrectFollowingId() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val followingIds = repository.getFollowingIds(userAId)

    assertEquals("Should be following exactly one user", ONE_FOLLOWER, followingIds.size)
    assertEquals("Following ID should match UserB", userBId, followingIds.first())
  }

  @Test
  fun getFollowingIds_returnsMultipleFollowing() = runTest {
    val (userAId, userBId, userCId) = setupThreeUsers()

    // UserA follows UserB
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // UserA follows UserC
    repository.followUser(userAId, userCId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    val followingIds = repository.getFollowingIds(userAId)

    assertEquals("Should be following exactly two users", TWO_FOLLOWERS, followingIds.size)
    assertTrue("Should be following UserB", followingIds.contains(userBId))
    assertTrue("Should be following UserC", followingIds.contains(userCId))
  }

  @Test
  fun getFollowingIds_throwsExceptionForNonExistentUser() = runTest {
    val nonExistentUserId = "non-existent-user-id-12345"

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getFollowingIds(nonExistentUserId) }
    }
  }

  // ==================== COMPLEX INTEGRATION TESTS ====================

  @Test
  fun followUnfollowCycle_maintainsDataIntegrity() = runTest {
    val (userAId, userBId) = setupTwoUsers()

    // Follow
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify following
    assertTrue("Should be following after follow", repository.isFollowing(userAId, userBId))
    assertEquals("Following count should be 1", ONE_FOLLOWER, repository.getFollowingCount(userAId))
    assertEquals("Follower count should be 1", ONE_FOLLOWER, repository.getFollowerCount(userBId))

    // Unfollow
    repository.unfollowUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify unfollowing
    assertFalse("Should not be following after unfollow", repository.isFollowing(userAId, userBId))
    assertEquals(
        "Following count should be 0 after unfollow",
        INITIAL_COUNT,
        repository.getFollowingCount(userAId))
    assertEquals(
        "Follower count should be 0 after unfollow",
        INITIAL_COUNT,
        repository.getFollowerCount(userBId))
  }

  @Test
  fun multipleFollowersAndFollowing_countsAreAccurate() = runTest {
    val userAId = setupUserWithProfile(USER_A_EMAIL, USER_A_PASSWORD, USER_A_NAME, USER_A_LASTNAME)
    val userBId = setupUserWithProfile(USER_B_EMAIL, USER_B_PASSWORD, USER_B_NAME, USER_B_LASTNAME)
    val userCId = setupUserWithProfile(USER_C_EMAIL, USER_C_PASSWORD, USER_C_NAME, USER_C_LASTNAME)

    // UserA follows UserB and UserC
    signInUser(USER_A_EMAIL, USER_A_PASSWORD)
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    repository.followUser(userAId, userCId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // UserB follows UserC
    signInUser(USER_B_EMAIL, USER_B_PASSWORD)
    repository.followUser(userBId, userCId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify UserA: following 2, followers 0
    assertEquals(
        "UserA should be following 2 users", TWO_FOLLOWERS, repository.getFollowingCount(userAId))
    assertEquals(
        "UserA should have 0 followers", INITIAL_COUNT, repository.getFollowerCount(userAId))

    // Verify UserB: following 1, followers 1
    assertEquals(
        "UserB should be following 1 user", ONE_FOLLOWER, repository.getFollowingCount(userBId))
    assertEquals("UserB should have 1 follower", ONE_FOLLOWER, repository.getFollowerCount(userBId))

    // Verify UserC: following 0, followers 2
    assertEquals(
        "UserC should be following 0 users", INITIAL_COUNT, repository.getFollowingCount(userCId))
    assertEquals(
        "UserC should have 2 followers", TWO_FOLLOWERS, repository.getFollowerCount(userCId))
  }

  @Test
  fun followerListsAreIndependent_followingOneUserDoesNotAffectOthers() = runTest {
    val (userAId, userBId, userCId) = setupThreeUsers()

    // UserA follows UserB
    repository.followUser(userAId, userBId)
    delay(FIRESTORE_WRITE_DELAY_MS)

    // Verify UserC has no followers
    val userCFollowerIds = repository.getFollowerIds(userCId)
    assertTrue("UserC should have no followers", userCFollowerIds.isEmpty())

    // Verify UserB has one follower
    val userBFollowerIds = repository.getFollowerIds(userBId)
    assertEquals("UserB should have one follower", ONE_FOLLOWER, userBFollowerIds.size)
  }
}
