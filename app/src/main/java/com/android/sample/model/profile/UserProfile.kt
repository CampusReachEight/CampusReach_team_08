package com.android.sample.model.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.firestore.Blob
import java.io.ByteArrayOutputStream
import java.util.Date

const val PHOTO_QUALITY = 80 // Compression quality for JPEG (0-100)
const val PHOTO_DEFAULT_SIZE = 512 // 512x512 pixels
const val MAX_PHOTO_SIZE_BYTES = 100 * 1024 // 100KB

/**
 * Data class representing a user profile.
 *
 * Fields:
 * - id: Unique identifier for the user profile.
 * - name: First name of the user.
 * - lastName: Last name of the user.
 * - email: Email address of the user (nullable if not shared).
 * - photo: User's profile photo as a Bitmap (nullable if not set).
 * - kudos: Integer representing user's kudos points.
 * - section: Enum representing user's section/department.
 * - arrivalDate: Date when the user joined/arrived.
 *
 * The class includes methods for serialization to/from Firestore document format, handling Bitmap
 * to Blob conversion with size constraints, and ensuring data integrity.
 *
 * Other possible fields (not implemented here) could include:
 * - phoneNumber: String? (nullable if not shared)
 * - bio: String? (nullable user biography)
 * - username: String? (nullable if not set) - different from first/last name (e.g: "johndoe123",
 *   "tinyMike" etc.)
 */
data class UserProfile(
    val id: String,
    val name: String,
    val lastName: String,
    val email:
        String?, // User (or default settings) can choose to not share email with others -> nullable
    val photo: Bitmap?, // Nullable Bitmap for user photo in case user hasn't set one
    val kudos: Int,
    val section: Section,
    val arrivalDate: Date
) {
  companion object {
    // Allows for deserialization from Firestore document data
    fun fromMap(data: Map<String, Any?>): UserProfile {
      return UserProfile(
          id = data["id"] as String,
          name = data["name"] as String,
          lastName = data["lastName"] as String,
          email = data["email"] as String,
          photo = bitmapFromBlob(data["photo"] as Blob),
          kudos = (data["kudos"] as Int),
          section = Section.valueOf(data["section"] as String),
          arrivalDate = (data["arrivalDate"] as Date))
    }

    // Converts Firestore Blob to Bitmap, ensuring size constraints
    fun bitmapFromBlob(blob: Blob?): Bitmap? {
      if (blob == null) return null
      val bytes = blob.toBytes()
      return try {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        // Ensure bitmap is the expected size
        if (bitmap.height != PHOTO_DEFAULT_SIZE || bitmap.width != PHOTO_DEFAULT_SIZE) {
          null
        } else {
          bitmap
        }
      } catch (e: Exception) {
        // Decoding failed / Possibly corrupted data
        null
      }
    }

    // Converts Bitmap to Firestore Blob, ensuring size constraints
    fun bitmapToBlob(bitmap: Bitmap?): Blob? {
      if (bitmap == null) return null
      if (bitmap.height != PHOTO_DEFAULT_SIZE || bitmap.width != PHOTO_DEFAULT_SIZE) {
        return null // Invalid size
      }

      // Step 1: Compress to under 100KB
      val stream = ByteArrayOutputStream()
      var quality = PHOTO_QUALITY
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

      // Step 2: Reduce quality until under 100KB or min quality reached
      while (stream.size() > MAX_PHOTO_SIZE_BYTES && quality > 60) {
        stream.reset()
        quality -= 5
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
      }

      // Step 3: Convert to Firestore Blob
      return Blob.fromBytes(stream.toByteArray())
    }
  }

  // Allows for serialization to Firestore document data
  fun toMap(): Map<String, Any?> =
      mapOf(
          "id" to id,
          "name" to name,
          "lastName" to lastName,
          "email" to email,
          "photo" to bitmapToBlob(photo),
          "kudos" to kudos,
          "section" to section.name,
          "arrivalDate" to arrivalDate)
}

enum class Section {
  INFORMATION_SYSTEMS,
  COMPUTER_SCIENCE,
  SOFTWARE_ENGINEERING,
  CYBER_SECURITY,
  MECHANICAL_ENGINEERING,
  ELECTRICAL_ENGINEERING,
  INDUSTRIAL_ENGINEERING,
  LITTERATURE,
  LIFE_SCIENCES,
  PHILOSOPHY,
  ECONOMICS,
  LAW,
  // ton add some stuff
  OTHER
}
