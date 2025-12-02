package com.android.sample.model.profile

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.profile.UserSections
import java.util.Date
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileTest {

  @Test
  fun bitmapToBlob_validSizedBitmap_producesBlobWithinLimit() {
    val bmp = Bitmap.createBitmap(PHOTO_DEFAULT_SIZE, PHOTO_DEFAULT_SIZE, Bitmap.Config.ARGB_8888)
    val blob = UserProfile.bitmapToBlob(bmp)
    assertNotNull(blob)
    val bytes = blob!!.toBytes()
    assertTrue(bytes.size <= MAX_PHOTO_SIZE_BYTES)
    val decoded = UserProfile.bitmapFromBlob(blob)
    assertNotNull(decoded)
    val d = decoded!!
    assertEquals(PHOTO_DEFAULT_SIZE, d.width)
    assertEquals(PHOTO_DEFAULT_SIZE, d.height)
  }

  @Test
  fun bitmapToBlob_invalidSize_returnsNull() {
    val invalid = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    assertNull(UserProfile.bitmapToBlob(invalid))
  }

  @Test
  fun bitmapToBlob_nullInput_returnsNull() {
    assertNull(UserProfile.bitmapToBlob(null))
  }

  @Test
  fun bitmapFromBlob_roundTrip_validBitmap() {
    val bmp = Bitmap.createBitmap(PHOTO_DEFAULT_SIZE, PHOTO_DEFAULT_SIZE, Bitmap.Config.ARGB_8888)
    val blob = UserProfile.bitmapToBlob(bmp)
    assertNotNull(blob)
    val restored = UserProfile.bitmapFromBlob(blob)
    assertNotNull(restored)
    val r = restored!!
    assertEquals(PHOTO_DEFAULT_SIZE, r.width)
    assertEquals(PHOTO_DEFAULT_SIZE, r.height)
  }

  @Test
  fun toMap_and_fromMap_symmetry_withValidPhoto() {
    val profile =
        UserProfile(
            id = "u1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = null,
            kudos = 42,
            helpReceived = 2,
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    val map = profile.toMap()
    assertEquals("u1", map["id"])

    val reconstructed = UserProfile.fromMap(map)
    assertEquals(profile.id, reconstructed.id)
    assertEquals(profile.name, reconstructed.name)
    assertEquals(profile.lastName, reconstructed.lastName)
    assertEquals(profile.email, reconstructed.email)
    assertEquals(profile.kudos, reconstructed.kudos)
    assertEquals(profile.section, reconstructed.section)
    assertEquals(profile.arrivalDate.time / 1000, reconstructed.arrivalDate.time / 1000)
  }

  @Test
  fun bitmapFromBlob_nullReturnsNull() {
    assertNull(UserProfile.bitmapFromBlob(null))
  }
  // ============ Followers/Following Serialization Tests ============

  @Test
  fun toMap_includesFollowersAndFollowing() {
    val profile =
        UserProfile(
            id = "u1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = null,
            kudos = 42,
            helpReceived = 5,
            followers = listOf("user2", "user3"),
            following = listOf("user4", "user5", "user6"),
            section = UserSections.COMPUTER_SCIENCE,
            arrivalDate = Date())

    val map = profile.toMap()

    // Verify followers and following are in the map
    assertTrue(map.containsKey("followers"))
    assertTrue(map.containsKey("following"))

    @Suppress("UNCHECKED_CAST") val followersFromMap = map["followers"] as List<String>
    @Suppress("UNCHECKED_CAST") val followingFromMap = map["following"] as List<String>

    assertEquals(2, followersFromMap.size)
    assertTrue(followersFromMap.contains("user2"))
    assertTrue(followersFromMap.contains("user3"))

    assertEquals(3, followingFromMap.size)
    assertTrue(followingFromMap.contains("user4"))
    assertTrue(followingFromMap.contains("user5"))
    assertTrue(followingFromMap.contains("user6"))
  }

  @Test
  fun fromMap_deserializesFollowersAndFollowing() {
    val map =
        mapOf(
            "id" to "u1",
            "name" to "Alice",
            "lastName" to "Smith",
            "email" to "alice@example.com",
            "photo" to null,
            "kudos" to 10,
            "helpReceived" to 3,
            "followers" to listOf("follower1", "follower2"),
            "following" to listOf("following1"),
            "section" to "COMPUTER_SCIENCE",
            "arrivalDate" to com.google.firebase.Timestamp.now())

    val profile = UserProfile.fromMap(map)

    assertEquals(2, profile.followers.size)
    assertTrue(profile.followers.contains("follower1"))
    assertTrue(profile.followers.contains("follower2"))

    assertEquals(1, profile.following.size)
    assertTrue(profile.following.contains("following1"))
  }

  @Test
  fun fromMap_handlesEmptyFollowersAndFollowing() {
    val map =
        mapOf(
            "id" to "u1",
            "name" to "Bob",
            "lastName" to "Jones",
            "email" to "bob@example.com",
            "photo" to null,
            "kudos" to 0,
            "helpReceived" to 0,
            "followers" to emptyList<String>(),
            "following" to emptyList<String>(),
            "section" to "PHYSICS",
            "arrivalDate" to com.google.firebase.Timestamp.now())

    val profile = UserProfile.fromMap(map)

    assertEquals(0, profile.followers.size)
    assertEquals(0, profile.following.size)
  }

  @Test
  fun fromMap_handlesMissingFollowersAndFollowing() {
    // Simulate old documents that don't have these fields
    val map =
        mapOf(
            "id" to "u1",
            "name" to "Charlie",
            "lastName" to "Brown",
            "email" to "charlie@example.com",
            "photo" to null,
            "kudos" to 5,
            "helpReceived" to 1,
            // followers and following are missing
            "section" to "MATHEMATICS",
            "arrivalDate" to com.google.firebase.Timestamp.now())

    val profile = UserProfile.fromMap(map)

    // Should default to empty lists
    assertEquals(0, profile.followers.size)
    assertEquals(0, profile.following.size)
  }

  @Test
  fun toMap_and_fromMap_roundTrip_withFollowersAndFollowing() {
    val original =
        UserProfile(
            id = "u123",
            name = "Test",
            lastName = "User",
            email = "test@example.com",
            photo = null,
            kudos = 15,
            helpReceived = 7,
            followers = listOf("follower1", "follower2", "follower3"),
            following = listOf("following1", "following2"),
            section = UserSections.ARCHITECTURE,
            arrivalDate = Date())

    val map = original.toMap()
    val reconstructed = UserProfile.fromMap(map)

    assertEquals(original.id, reconstructed.id)
    assertEquals(original.name, reconstructed.name)
    assertEquals(original.lastName, reconstructed.lastName)
    assertEquals(original.kudos, reconstructed.kudos)
    assertEquals(original.helpReceived, reconstructed.helpReceived)

    // Verify followers and following survived the round trip
    assertEquals(original.followers.size, reconstructed.followers.size)
    assertTrue(reconstructed.followers.containsAll(original.followers))

    assertEquals(original.following.size, reconstructed.following.size)
    assertTrue(reconstructed.following.containsAll(original.following))

    assertEquals(original.section, reconstructed.section)
  }
}
