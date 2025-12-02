package com.android.sample.model.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.profile.UserSections
import com.android.sample.utils.BaseEmulatorTest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileFollowTest : BaseEmulatorTest() {

  private lateinit var repository: UserProfileRepositoryFirestore
  private lateinit var testUser1Id: String
  private lateinit var testUser2Id: String
  private lateinit var testUser3Id: String

  @Before
  override fun setUp() {
    super.setUp()
    repository = UserProfileRepositoryFirestore(FirebaseFirestore.getInstance())
  }

  private suspend fun createTestProfile(
      userId: String,
      name: String = "Test",
      lastName: String = "User"
  ): UserProfile {
    val profile =
        UserProfile(
            id = userId,
            name = name,
            lastName = lastName,
            email = "$name@example.com",
            photo = null,
            kudos = 0,
            helpReceived = 0,
            followers = emptyList(),
            following = emptyList(),
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())
    repository.addUserProfile(profile)
    return profile
  }

  // ============ Follow User Tests ============

  @Test
  fun followUser_success_updatesFollowersAndFollowing() = runTest {
    // Setup: Create two users
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    // Alice follows Bob
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)

    // Verify Alice's following list contains Bob
    val aliceProfile = repository.getUserProfile(testUser1Id)
    assertTrue(aliceProfile.following.contains(testUser2Id))
    assertEquals(1, aliceProfile.following.size)

    // Verify Bob's followers list contains Alice
    val bobProfile = repository.getUserProfile(testUser2Id)
    assertTrue(bobProfile.followers.contains(testUser1Id))
    assertEquals(1, bobProfile.followers.size)
  }

  @Test
  fun followUser_alreadyFollowing_doesNotDuplicate() = runTest {
    // Setup
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    // Alice follows Bob twice
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)
    repository.followUser(testUser2Id) // Second follow should be ignored

    // Verify no duplicates
    val aliceProfile = repository.getUserProfile(testUser1Id)
    assertEquals(1, aliceProfile.following.size)

    val bobProfile = repository.getUserProfile(testUser2Id)
    assertEquals(1, bobProfile.followers.size)
  }

  @Test(expected = IllegalArgumentException::class)
  fun followUser_throwsException_whenFollowingSelf() = runTest {
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    repository.followUser(testUser1Id) // Try to follow self
  }

  @Test(expected = IllegalStateException::class)
  fun followUser_throwsException_whenNotAuthenticated() = runTest {
    auth.signOut()
    repository.followUser("some-user-id")
  }

  // ============ Unfollow User Tests ============

  @Test
  fun unfollowUser_success_removesFollowersAndFollowing() = runTest {
    // Setup: Alice follows Bob
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)

    // Alice unfollows Bob
    repository.unfollowUser(testUser2Id)

    // Verify Alice's following list is empty
    val aliceProfile = repository.getUserProfile(testUser1Id)
    assertFalse(aliceProfile.following.contains(testUser2Id))
    assertEquals(0, aliceProfile.following.size)

    // Verify Bob's followers list is empty
    val bobProfile = repository.getUserProfile(testUser2Id)
    assertFalse(bobProfile.followers.contains(testUser1Id))
    assertEquals(0, bobProfile.followers.size)
  }

  @Test
  fun unfollowUser_notFollowing_doesNothing() = runTest {
    // Setup
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    // Alice unfollows Bob without following first
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.unfollowUser(testUser2Id) // Should not throw

    // Verify lists remain empty
    val aliceProfile = repository.getUserProfile(testUser1Id)
    assertEquals(0, aliceProfile.following.size)

    val bobProfile = repository.getUserProfile(testUser2Id)
    assertEquals(0, bobProfile.followers.size)
  }

  @Test(expected = IllegalArgumentException::class)
  fun unfollowUser_throwsException_whenUnfollowingSelf() = runTest {
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    repository.unfollowUser(testUser1Id) // Try to unfollow self
  }

  @Test(expected = IllegalStateException::class)
  fun unfollowUser_throwsException_whenNotAuthenticated() = runTest {
    auth.signOut()
    repository.unfollowUser("some-user-id")
  }

  // ============ Is Following Tests ============

  @Test
  fun isFollowing_returnsTrue_whenFollowing() = runTest {
    // Setup: Alice follows Bob
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)

    // Check
    val isFollowing = repository.isFollowing(testUser2Id)
    assertTrue(isFollowing)
  }

  @Test
  fun isFollowing_returnsFalse_whenNotFollowing() = runTest {
    // Setup
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    // Check without following
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    val isFollowing = repository.isFollowing(testUser2Id)
    assertFalse(isFollowing)
  }

  @Test(expected = IllegalStateException::class)
  fun isFollowing_throwsException_whenNotAuthenticated() = runTest {
    auth.signOut()
    repository.isFollowing("some-user-id")
  }

  // ============ Get Following Tests ============

  @Test
  fun getFollowing_returnsCorrectList() = runTest {
    // Setup: Alice follows Bob and Charlie
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    createAndSignInUser("charlie@example.com", "test123456")
    testUser3Id = currentUserId
    createTestProfile(testUser3Id, "Charlie", "Brown")

    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)
    repository.followUser(testUser3Id)

    // Get following list
    val following = repository.getFollowing()
    assertEquals(2, following.size)
    assertTrue(following.contains(testUser2Id))
    assertTrue(following.contains(testUser3Id))
  }

  @Test
  fun getFollowing_returnsEmptyList_whenNotFollowingAnyone() = runTest {
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    val following = repository.getFollowing()
    assertEquals(0, following.size)
  }

  @Test(expected = IllegalStateException::class)
  fun getFollowing_throwsException_whenNotAuthenticated() = runTest {
    auth.signOut()
    repository.getFollowing()
  }

  // ============ Get Followers Tests ============

  @Test
  fun getFollowers_returnsCorrectList() = runTest {
    // Setup: Bob and Charlie follow Alice
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")
    repository.followUser(testUser1Id) // Bob follows Alice

    createAndSignInUser("charlie@example.com", "test123456")
    testUser3Id = currentUserId
    createTestProfile(testUser3Id, "Charlie", "Brown")
    repository.followUser(testUser1Id) // Charlie follows Alice

    // Alice checks her followers
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    val followers = repository.getFollowers()
    assertEquals(2, followers.size)
    assertTrue(followers.contains(testUser2Id))
    assertTrue(followers.contains(testUser3Id))
  }

  @Test
  fun getFollowers_returnsEmptyList_whenNoFollowers() = runTest {
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    val followers = repository.getFollowers()
    assertEquals(0, followers.size)
  }

  @Test(expected = IllegalStateException::class)
  fun getFollowers_throwsException_whenNotAuthenticated() = runTest {
    auth.signOut()
    repository.getFollowers()
  }

  // ============ Complex Scenarios ============

  @Test
  fun followUnfollowCycle_maintainsDataIntegrity() = runTest {
    // Setup
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    // Alice follows Bob
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)
    assertTrue(repository.isFollowing(testUser2Id))

    // Alice unfollows Bob
    repository.unfollowUser(testUser2Id)
    assertFalse(repository.isFollowing(testUser2Id))

    // Alice follows Bob again
    repository.followUser(testUser2Id)
    assertTrue(repository.isFollowing(testUser2Id))

    // Verify final state
    assertEquals(1, repository.getFollowing().size)

    val bobProfile = repository.getUserProfile(testUser2Id)
    assertEquals(1, bobProfile.followers.size)
  }

  @Test
  fun multipleUsersFollowing_maintainsSeparateLists() = runTest {
    // Setup: Create 3 users
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    testUser1Id = currentUserId
    createTestProfile(testUser1Id, "Alice", "Smith")

    createAndSignInUser("bob@example.com", "test123456")
    testUser2Id = currentUserId
    createTestProfile(testUser2Id, "Bob", "Jones")

    createAndSignInUser("charlie@example.com", "test123456")
    testUser3Id = currentUserId
    createTestProfile(testUser3Id, "Charlie", "Brown")

    // Alice follows Bob
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    repository.followUser(testUser2Id)

    // Bob follows Charlie
    signInUser("bob@example.com", "test123456")
    repository.followUser(testUser3Id)

    // Charlie follows Alice
    signInUser("charlie@example.com", "test123456")
    repository.followUser(testUser1Id)

    // Verify each user's lists are correct
    signInUser(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD)
    val aliceFollowing = repository.getFollowing()
    val aliceFollowers = repository.getFollowers()
    assertEquals(1, aliceFollowing.size)
    assertTrue(aliceFollowing.contains(testUser2Id))
    assertEquals(1, aliceFollowers.size)
    assertTrue(aliceFollowers.contains(testUser3Id))

    signInUser("bob@example.com", "test123456")
    val bobFollowing = repository.getFollowing()
    val bobFollowers = repository.getFollowers()
    assertEquals(1, bobFollowing.size)
    assertTrue(bobFollowing.contains(testUser3Id))
    assertEquals(1, bobFollowers.size)
    assertTrue(bobFollowers.contains(testUser1Id))

    signInUser("charlie@example.com", "test123456")
    val charlieFollowing = repository.getFollowing()
    val charlieFollowers = repository.getFollowers()
    assertEquals(1, charlieFollowing.size)
    assertTrue(charlieFollowing.contains(testUser1Id))
    assertEquals(1, charlieFollowers.size)
    assertTrue(charlieFollowers.contains(testUser2Id))
  }
}
