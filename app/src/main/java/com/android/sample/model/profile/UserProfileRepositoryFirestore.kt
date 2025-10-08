package com.android.sample.model.profile

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

const val PUBLIC_PROFILES_PATH = "public_profiles"
const val PRIVATE_PROFILES_PATH = "private_profiles"

/**
 * Repository interface for managing user profiles.
 *
 * The repository uses two Firestore collections:
 * 1. "public_profiles": Contains user profiles with limited information (e.g., blurred email).
 * 2. "private_profiles": Contains full user profiles with all details. This separation ensures that
 *    sensitive information is only accessible to the profile owner. The parallel structure
 *    management is handled seamlessly within the repository methods.
 */
class UserProfileRepositoryFirestore(private val db: FirebaseFirestore) : UserProfileRepository {

  // Path structure: "public_profiles/{userId}" and "private_profiles/{userId}" (userId same in
  // both)

  private val publicCollectionRef = db.collection(PUBLIC_PROFILES_PATH)
  private val privateCollectionRef = db.collection(PRIVATE_PROFILES_PATH)

  // Fix: Document ID should be the authenticated user's UID
  override fun getNewUid(): String {
    return Firebase.auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")
  }

  // Retrieves all public user profiles
  override suspend fun getAllUserProfiles(): List<UserProfile> {
    return publicCollectionRef.get().await().documents.mapNotNull { doc ->
      doc.data?.let { UserProfile.fromMap(it) }
    }
  }

  // Retrieves a user profile by ID
  override suspend fun getUserProfile(userId: String): UserProfile {
    val currentUserId = Firebase.auth.currentUser?.uid
    val collectionRef = if (userId == currentUserId) privateCollectionRef else publicCollectionRef

    return collectionRef.document(userId).get().await().data?.let { UserProfile.fromMap(it) }
        ?: throw NoSuchElementException("UserProfile with ID $userId not found")
  }

  // Blurs email (and potentially other fields) for public profiles, keep full details in private
  // profile
  override suspend fun addUserProfile(userProfile: UserProfile) {
    val currentUserId =
        Firebase.auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")
    if (userProfile.id != currentUserId) {
      // We assume profile is added post-authentication and user can only add their own profile
      throw IllegalArgumentException("UserProfile ID must match the current user ID")
    }

    // Add to private collection with full details
    privateCollectionRef.document(userProfile.id).set(userProfile.toMap()).await()

    // Create a public version with blurred email
    // Blur other fields as needed here ...
    val publicProfile = userProfile.copy(email = null)

    // Add to public collection
    publicCollectionRef.document(userProfile.id).set(publicProfile.toMap()).await()
  }

  override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {
    val currentUserId =
        Firebase.auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")
    if (userId != currentUserId || updatedProfile.id != currentUserId) {
      throw IllegalArgumentException("Can only update the current user's profile")
    }

    // Update private profile with full details
    privateCollectionRef.document(userId).set(updatedProfile.toMap()).await()

    // Update public profile with blurred email
    // Blur other fields as needed here ...
    val publicProfile = updatedProfile.copy(email = null)

    // Update public profile
    publicCollectionRef.document(userId).set(publicProfile.toMap()).await()
  }

  override suspend fun deleteUserProfile(userId: String) {
    val currentUserId =
        Firebase.auth.currentUser?.uid ?: throw IllegalStateException("No authenticated user")
    if (userId != currentUserId) {
      throw IllegalArgumentException("Can only delete the current user's profile")
    }

    // Delete from both collections
    privateCollectionRef.document(userId).delete().await()
    publicCollectionRef.document(userId).delete().await()
  }

  /**
   * Searches for user profiles matching the given query and optional section filter.
   *
   * Algorithm:
   * 1. If section is provided, filter profiles by section.
   * 2. Split the query into keywords and search for matches in name or lastName fields.
   * 3. Combine filters to return profiles that match both section and query criteria.
   */
  override suspend fun searchUserProfiles(
      query: String,
      section: Section?,
      resultsPerView: Int,
  ): Flow<UserProfile> = flowOf() // TODO: Implement search with Firestore queries and return Flow
}
