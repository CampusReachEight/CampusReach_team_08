package com.android.sample.model.profile

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.Blob
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
    val bmp = Bitmap.createBitmap(PHOTO_DEFAULT_SIZE, PHOTO_DEFAULT_SIZE, Bitmap.Config.ARGB_8888)
    val profile =
        UserProfile(
            id = "u1",
            name = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            photo = bmp,
            kudos = 42,
            section = Section.SOFTWARE_ENGINEERING,
            arrivalDate = Date())

    val map = profile.toMap()
    assertEquals("u1", map["id"])
    assertTrue(map["photo"] is Blob)

    val reconstructed = UserProfile.fromMap(map)
    assertEquals(profile.id, reconstructed.id)
    assertEquals(profile.name, reconstructed.name)
    assertEquals(profile.lastName, reconstructed.lastName)
    assertEquals(profile.email, reconstructed.email)
    assertEquals(profile.kudos, reconstructed.kudos)
    assertEquals(profile.section, reconstructed.section)
    assertEquals(profile.arrivalDate.time / 1000, reconstructed.arrivalDate.time / 1000)

    val photo = reconstructed.photo
    assertNotNull(photo)
    val p = photo!!
    assertEquals(PHOTO_DEFAULT_SIZE, p.width)
    assertEquals(PHOTO_DEFAULT_SIZE, p.height)
  }

  @Test
  fun toMap_withInvalidPhotoSize_photoFieldIsNull() {
    val invalid = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
    val profile =
        UserProfile(
            id = "u2",
            name = "Bad",
            lastName = "Size",
            email = "bad.size@example.com",
            photo = invalid,
            kudos = 0,
            section = Section.OTHER,
            arrivalDate = Date())
    val map = profile.toMap()
    assertNull(map["photo"])
  }

  @Test
  fun fromMap_withCorruptBlob_photoBecomesNull() {
    val corruptBlob = Blob.fromBytes(byteArrayOf(0x00, 0x01, 0x02))
    val date = Date()
    val map =
        mapOf(
            "id" to "bad1",
            "name" to "Bad",
            "lastName" to "Data",
            "email" to "bad@example.com",
            "photo" to corruptBlob,
            "kudos" to 0,
            "section" to Section.OTHER.name,
            "arrivalDate" to date)

    val reconstructed = UserProfile.fromMap(map)
    assertNull(reconstructed.photo)
  }

  @Test
  fun bitmapFromBlob_nullReturnsNull() {
    assertNull(UserProfile.bitmapFromBlob(null))
  }
}
