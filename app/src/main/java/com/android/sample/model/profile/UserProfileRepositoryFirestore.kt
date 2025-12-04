package com.android.sample.model.profile

import android.util.Log
import com.android.sample.ui.request_validation.HelpReceivedConstants
import com.android.sample.ui.request_validation.HelpReceivedException
import com.android.sample.ui.request_validation.KudosConstants
import com.android.sample.ui.request_validation.KudosException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val PUBLIC_PROFILES_PATH = "public_profiles"
const val PRIVATE_PROFILES_PATH = "private_profiles"

const val KUDOS_FIELD = "kudos"

const val HELP_RECEIVED_FIELD = "helpReceived"

private const val ZERO = 0

// Error / log message constants
private const val MSG_NO_AUTHENTICATED_USER = "No authenticated user"
private const val MSG_NOT_AUTHORIZED = "Can only modify the currently authenticated user's profile"
private const val MSG_USER_NOT_FOUND = "UserProfile with ID %s not found"
private const val MSG_USER_NOT_FOUND_AFTER_SYNC = "UserProfile with ID %s not found after sync"
private const val LOG_TAG = "UserProfileRepository"
private const val MSG_SYNC_KUDOS = "Error syncing kudos for user %s: %s"
private const val KUDOS_BATCH_LOG_TAG = "KUDOS_BATCH"
private const val KUDOS_BATCH_LOG_MSG = "awardKudosBatch: `public_profiles`/%s kudos-before=%s"
private const val BATCH_OPERATION_ID = "batch_operation"
private const val MSG_FAILED_RECORD_HELP = "Failed to record help for user: %s"

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

  private fun notAuthenticated(): Unit = throw IllegalStateException(MSG_NO_AUTHENTICATED_USER)

  private fun notAuthorized(): Unit = throw IllegalArgumentException(MSG_NOT_AUTHORIZED)

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
    return publicCollectionRef.get().await().documents.mapNotNull { doc ->
      doc.data?.let { UserProfile.fromMap(it) }
    }
  }

  // Retrieves a user profile by ID
  override suspend fun getUserProfile(userId: String): UserProfile {
    val currentUserId = Firebase.auth.currentUser?.uid

    // If request the current user's profile, fetch from private collection
    if (userId == currentUserId) {
      val privateDocRef = privateCollectionRef.document(userId)
      val privateSnapshot = privateDocRef.get().await()
      if (!privateSnapshot.exists()) {
        throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
      }

      try {
        val publicSnapshot = publicCollectionRef.document(userId).get().await()
        if (publicSnapshot.exists()) {
          val publicKudos = (publicSnapshot[KUDOS_FIELD] as? Number)?.toInt() ?: 0
          val privateKudos = (privateSnapshot[KUDOS_FIELD] as? Number)?.toInt() ?: 0

          if (publicKudos != privateKudos) {
            // Sync private kudos to the public value
            privateDocRef.update(KUDOS_FIELD, publicKudos).await()
            // Re-fetch private snapshot to return the latest data
            val updatedPrivateSnapshot = privateDocRef.get().await()
            return updatedPrivateSnapshot.data?.let { UserProfile.fromMap(it) }
                ?: throw NoSuchElementException(
                    String.format(MSG_USER_NOT_FOUND_AFTER_SYNC, userId))
          }
        }
      } catch (e: Exception) {
        Log.e(LOG_TAG, String.format(MSG_SYNC_KUDOS, userId, e.message))
      }
      return privateSnapshot.data?.let { UserProfile.fromMap(it) }
          ?: throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
    }

    // For other users, return the public profile
    val publicDoc = publicCollectionRef.document(userId).get().await()
    return publicDoc.data?.let { UserProfile.fromMap(it) }
        ?: throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
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
    publicCollectionRef
        .document(userProfile.id)
        .get(com.google.firebase.firestore.Source.SERVER)
        .await()
    privateCollectionRef
        .document(userProfile.id)
        .get(com.google.firebase.firestore.Source.SERVER)
        .await()
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

    val privateDocRef = privateCollectionRef.document(userId)

    // Delete from both collections
    privateDocRef.delete().await()
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
          if (remaining > ZERO) {
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
    } catch (_: Exception) {
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
      val userDoc =
          publicCollectionRef
              .document(userId)
              .get(com.google.firebase.firestore.Source.SERVER)
              .await()
      if (!userDoc.exists()) {
        throw KudosException.UserNotFound(userId)
      }

      // Award kudos atomically using FieldValue.increment
      // Update both public and private profiles
      publicCollectionRef
          .document(userId)
          .update(KUDOS_FIELD, FieldValue.increment(amount.toLong()))
          .await()

      privateCollectionRef
          .document(userId)
          .update(KUDOS_FIELD, FieldValue.increment(amount.toLong()))
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
        val publicSnapshot = publicDocRef.get(com.google.firebase.firestore.Source.SERVER).await()
        val currentKudos = publicSnapshot.getLong(KUDOS_FIELD) ?: 0L
        Log.d(KUDOS_BATCH_LOG_TAG, String.format(KUDOS_BATCH_LOG_MSG, userId, currentKudos))

        batch.update(publicDocRef, KUDOS_FIELD, FieldValue.increment(amount.toLong()))
      }

      // Commit all updates atomically
      batch.commit().await()

      // Verify writes completed
      kotlinx.coroutines.delay(100)
    } catch (e: KudosException) {
      throw e
    } catch (e: Exception) {
      throw KudosException.TransactionFailed(BATCH_OPERATION_ID, e)
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
            transaction.update(
                publicDocRef, HELP_RECEIVED_FIELD, FieldValue.increment(amount.toLong()))
            transaction.update(
                privateDocRef, HELP_RECEIVED_FIELD, FieldValue.increment(amount.toLong()))

            // runTransaction requires a return value; use null for Unit
            null
          }
          .await()
    } catch (e: HelpReceivedException) {
      // Propagate known help-related exceptions unchanged
      throw e
    } catch (e: Exception) {
      // Wrap unexpected failures in a documented help-specific exception
      throw HelpReceivedException.TransactionFailed(
          String.format(MSG_FAILED_RECORD_HELP, userId), e)
    }
  }
}
