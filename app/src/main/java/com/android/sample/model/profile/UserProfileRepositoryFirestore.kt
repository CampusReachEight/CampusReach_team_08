package com.android.sample.model.profile

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
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

  private fun notAuthenticated(): Unit = throw IllegalStateException("No authenticated user")

  private fun notAuthorized(): Unit =
      throw IllegalArgumentException("Can only modify the currently authenticated user's profile")

  // Fix: Document ID should be the authenticated user's UID
  override fun getNewUid(): String {
    if (Firebase.auth.currentUser == null) {
      notAuthenticated()
    }
    return Firebase.auth.currentUser!!.uid
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
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()
    if (userProfile.id != currentUserId) {
      // We assume profile is added post-authentication and user can only add their own profile
      notAuthorized()
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
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()
    if (userId != currentUserId || updatedProfile.id != currentUserId) {
      notAuthorized()
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
    val currentUserId = Firebase.auth.currentUser?.uid ?: notAuthenticated()
    if (userId != currentUserId) {
      notAuthorized()
    }

    // Delete from both collections
    privateCollectionRef.document(userId).delete().await()
    publicCollectionRef.document(userId).delete().await()
  }

  /**
   * Searches public user profiles by first or last name using Firestore prefix range queries. Uses
   * the standard [query, query+"\uf8ff") bounds for efficient server-side filtering. Performs a
   * second query on lastNameLowercase if needed, deduplicates results and applies a client-side
   * contains() filter for substring matching, then enforces the limit.
   */
  override suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile> {
    if (query.length < 2) return emptyList()
    val normalizedQuery = query.lowercase().trim()
    if (normalizedQuery.isEmpty()) return emptyList()

    return try {
      // First: prefix search on nameLowercase
      val nameQuerySnapshot =
          publicCollectionRef
              .whereGreaterThanOrEqualTo("nameLowercase", normalizedQuery)
              .whereLessThan("nameLowercase", normalizedQuery + "\uf8ff")
              .limit(limit.toLong())
              .get()
              .await()

      val byName = nameQuerySnapshot.documents.mapNotNull { it.data?.let(UserProfile::fromMap) }

      val remaining = limit - byName.size

      val byLastName =
          if (remaining > 0) {
            // Second: prefix search on lastNameLowercase if we still need more results
            val lastNameSnapshot =
                publicCollectionRef
                    .whereGreaterThanOrEqualTo("lastNameLowercase", normalizedQuery)
                    .whereLessThan("lastNameLowercase", normalizedQuery + "\uf8ff")
                    .limit(remaining.toLong())
                    .get()
                    .await()
            lastNameSnapshot.documents.mapNotNull { it.data?.let(UserProfile::fromMap) }
          } else {
            emptyList()
          }

      // Combine, deduplicate and apply substring filtering for robustness
      (byName + byLastName)
          .distinctBy { it.id }
          .filter {
            it.nameLowercase.contains(normalizedQuery) ||
                it.lastNameLowercase.contains(normalizedQuery)
          }
          .take(limit)
    } catch (e: Exception) {
      // Graceful degradation: return empty list on any error
      emptyList()
    }
  }
}
