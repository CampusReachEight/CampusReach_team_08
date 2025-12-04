package com.android.sample.model.profile

import com.android.sample.ui.request_validation.HelpReceivedConstants
import com.android.sample.ui.request_validation.HelpReceivedException
import com.android.sample.ui.request_validation.KudosConstants
import com.android.sample.ui.request_validation.KudosException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import kotlin.text.get
import kotlinx.coroutines.tasks.await

const val PUBLIC_PROFILES_PATH = "public_profiles"
const val PRIVATE_PROFILES_PATH = "private_profiles"

private const val ZERO = 0

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

  override fun getCurrentUserId(): String = Firebase.auth.currentUser?.uid ?: ""

  // Fix: Document ID should be the authenticated user's UID
  override fun getNewUid(): String {
    if (Firebase.auth.currentUser == null) {
      notAuthenticated()
    }
    return Firebase.auth.currentUser!!.uid
  }

  // Retrieves all public user profiles
  override suspend fun getAllUserProfiles(): List<UserProfile> {
    val snapshot = publicCollectionRef.get(Source.SERVER).await()
    if (snapshot.metadata.isFromCache) {
      throw IllegalStateException("Data retrieved from cache instead of server")
    }
    return snapshot.documents.mapNotNull { doc -> doc.data?.let { UserProfile.fromMap(it) } }
  }

  // Retrieves a user profile by ID
  override suspend fun getUserProfile(userId: String): UserProfile {
    val currentUserId = Firebase.auth.currentUser?.uid
    val collectionRef = if (userId == currentUserId) privateCollectionRef else publicCollectionRef

    val snapshot = collectionRef.document(userId).get(Source.SERVER).await()
    if (snapshot.metadata.isFromCache) {
      throw IllegalStateException("Data retrieved from cache instead of server for user $userId")
    }
    return snapshot.data?.let { UserProfile.fromMap(it) }
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

    // Verify we're online by doing a quick server read
    val testSnapshot = publicCollectionRef.limit(1).get(Source.SERVER).await()
    if (testSnapshot.metadata.isFromCache) {
      throw IllegalStateException("Cannot add user profile while offline")
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

    // Verify we're online by doing a quick server read
    val testSnapshot = publicCollectionRef.limit(1).get(Source.SERVER).await()
    if (testSnapshot.metadata.isFromCache) {
      throw IllegalStateException("Cannot update user profile while offline")
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

    // Verify we're online by doing a quick server read
    val testSnapshot = publicCollectionRef.limit(1).get(Source.SERVER).await()
    if (testSnapshot.metadata.isFromCache) {
      throw IllegalStateException("Cannot delete user profile while offline")
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
              .get(Source.SERVER)
              .await()

      if (nameQuerySnapshot.metadata.isFromCache) {
        throw IllegalStateException("Search data retrieved from cache instead of server")
      }

      val byName = nameQuerySnapshot.documents.mapNotNull { it.data?.let(UserProfile::fromMap) }

      val remaining = limit - byName.size

      val byLastName =
          if (remaining > ZERO) {
            // Second: prefix search on lastNameLowercase if we still need more results
            val lastNameSnapshot =
                publicCollectionRef
                    .whereGreaterThanOrEqualTo("lastNameLowercase", normalizedQuery)
                    .whereLessThan("lastNameLowercase", normalizedQuery + "\uf8ff")
                    .limit(remaining.toLong())
                    .get(Source.SERVER)
                    .await()

            if (lastNameSnapshot.metadata.isFromCache) {
              throw IllegalStateException("Search data retrieved from cache instead of server")
            }

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

  override suspend fun awardKudos(userId: String, amount: Int) {
    // Validation
    if (amount <= ZERO) {
      throw KudosException.InvalidAmount(amount)
    }

    if (amount > KudosConstants.MAX_KUDOS_PER_TRANSACTION) {
      throw KudosException.InvalidAmount(amount)
    }

    try {
      // Check if user exists before awarding (check public profile)
      val userDoc = publicCollectionRef.document(userId).get(Source.SERVER).await()
      if (userDoc.metadata.isFromCache) {
        throw IllegalStateException(
            "User verification data retrieved from cache instead of server for user $userId")
      }
      if (!userDoc.exists()) {
        throw KudosException.UserNotFound(userId)
      }

      // Award kudos atomically using FieldValue.increment
      // Update both public and private profiles
      publicCollectionRef
          .document(userId)
          .update("kudos", FieldValue.increment(amount.toLong()))
          .await()

      privateCollectionRef
          .document(userId)
          .update("kudos", FieldValue.increment(amount.toLong()))
          .await()
    } catch (e: KudosException) {
      throw e
    } catch (e: Exception) {
      throw KudosException.TransactionFailed(userId, e)
    }
  }

  override suspend fun awardKudosBatch(awards: Map<String, Int>) {
    // Validate all amounts first
    awards.forEach { (_, amount) ->
      if (amount <= ZERO) {
        throw KudosException.InvalidAmount(amount)
      }
      if (amount > KudosConstants.MAX_KUDOS_PER_TRANSACTION) {
        throw KudosException.InvalidAmount(amount)
      }
    }

    // Calculate total kudos to prevent abuse
    val totalKudos = awards.values.sum()
    if (totalKudos > KudosConstants.MAX_KUDOS_PER_TRANSACTION) {
      throw KudosException.InvalidAmount(totalKudos)
    }

    try {
      // Verify all users exist BEFORE starting transaction
      awards.forEach { (userId, _) ->
        val userDoc =
            publicCollectionRef
                .document(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
        if (!userDoc.exists()) {
          throw KudosException.UserNotFound(userId)
        }
      }

      // Now perform atomic batch update (all users verified to exist)
      val batch = db.batch()

      awards.forEach { (userId, amount) ->
        val publicDocRef = publicCollectionRef.document(userId)
        val privateDocRef = privateCollectionRef.document(userId)

        batch.update(publicDocRef, "kudos", FieldValue.increment(amount.toLong()))
        batch.update(privateDocRef, "kudos", FieldValue.increment(amount.toLong()))
      }

      // Commit all updates atomically
      batch.commit().await()

      // Verify writes completed
      kotlinx.coroutines.delay(100)
    } catch (e: KudosException) {
      throw e
    } catch (e: Exception) {
      throw KudosException.TransactionFailed("batch_operation", e)
    }
  }

  /**
   * Records help received for a user by incrementing their public and private `helpReceived` fields
   * in Firestore. Validation is applied before any network operation:
   * - Amount must be greater than the minimum defined in
   *   [com.android.sample.ui.request_validation.HelpReceivedConstants].
   * - Amount must not exceed the per-transaction maximum defined in the same constants object.
   *
   * The increment is performed inside a Firestore transaction so the public and private documents
   * are updated atomically. Known help-related failures are propagated as specific
   * [com.android.sample.ui.request_validation.HelpReceivedException] subclasses; unexpected errors
   * are wrapped in a transaction failure exception.
   *
   * @param userId The id of the user receiving help.
   * @param amount The amount of help to record (must be positive and within safety limits).
   * @throws com.android.sample.ui.request_validation.HelpReceivedException.InvalidAmount when the
   *   provided amount is invalid (non-positive).
   * @throws com.android.sample.ui.request_validation.HelpReceivedException.AmountExceedsLimit when
   *   the provided amount exceeds the configured per-transaction maximum.
   * @throws com.android.sample.ui.request_validation.HelpReceivedException.UserNotFound when the
   *   user public/private profiles do not exist.
   * @throws com.android.sample.ui.request_validation.HelpReceivedException.TransactionFailed when
   *   the Firestore transaction fails unexpectedly.
   */
  override suspend fun receiveHelp(userId: String, amount: Int) {
    // Validate amount using constants to keep business rules centralized
    if (amount <= HelpReceivedConstants.MIN_HELP_RECEIVED) {
      throw HelpReceivedException.InvalidAmount(amount)
    }
    if (amount > HelpReceivedConstants.MAX_HELP_RECEIVED_PER_TRANSACTION) {
      throw HelpReceivedException.InvalidAmount(amount)
    }

    try {
      val publicDocRef = publicCollectionRef.document(userId)
      val privateDocRef = privateCollectionRef.document(userId)

      // Run transaction to ensure both docs exist and updates are atomic
      db.runTransaction { transaction ->
            val publicSnapshot = transaction[publicDocRef]
            val privateSnapshot = transaction[privateDocRef]

            if (!publicSnapshot.exists() || !privateSnapshot.exists()) {
              throw HelpReceivedException.UserNotFound(userId)
            }

            // Atomically increment helpReceived in both documents
            transaction.update(publicDocRef, "helpReceived", FieldValue.increment(amount.toLong()))
            transaction.update(privateDocRef, "helpReceived", FieldValue.increment(amount.toLong()))

            // runTransaction requires a return value; use null for Unit
            null
          }
          .await()
    } catch (e: HelpReceivedException) {
      // Propagate known help-related exceptions unchanged
      throw e
    } catch (e: Exception) {
      // Wrap unexpected failures in a documented help-specific exception
      throw HelpReceivedException.TransactionFailed("Failed to record help for user: $userId", e)
    }
  }
}
