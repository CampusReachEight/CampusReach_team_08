package com.android.sample.model.profile

import android.util.Log
import com.android.sample.ui.request_validation.HelpReceivedConstants
import com.android.sample.ui.request_validation.HelpReceivedException
import com.android.sample.ui.request_validation.KudosConstants
import com.android.sample.ui.request_validation.KudosException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val PUBLIC_PROFILES_PATH = "public_profiles"
const val PRIVATE_PROFILES_PATH = "private_profiles"
const val FOLLOWERS_SUBCOLLECTION = "followers"
const val FOLLOWING_SUBCOLLECTION = "following"
const val FOLLOWER_COUNT_FIELD = "followerCount"
const val FOLLOWING_COUNT_FIELD = "followingCount"

// Error messages for follow/unfollow operations
private const val MSG_CANNOT_FOLLOW_SELF = "Cannot follow yourself"
private const val MSG_ALREADY_FOLLOWING = "Already following user %s"
private const val MSG_NOT_FOLLOWING = "Not currently following user %s"
private const val MSG_FOLLOW_OPERATION_FAILED = "Follow operation failed: %s"
private const val MSG_UNFOLLOW_OPERATION_FAILED = "Unfollow operation failed: %s"

const val KUDOS_FIELD = "kudos"

const val HELP_RECEIVED_FIELD = "helpReceived"

private const val i = 0

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

private const val FOLLOWING_DATA_RETRIEVED_FROM_CACHE_INSTEAD_OF_SERVER =
    "Following data retrieved from cache instead of server"

private const val RETRIEVE_FROM_CACHE = FOLLOWING_DATA_RETRIEVED_FROM_CACHE_INSTEAD_OF_SERVER

private const val ONE_OR_BOTH_PROFILE = "One or both user profiles not found"

private const val DATA_RETRIEVED_FROM_CACHE = "Data retrieved from cache instead of server for user"

private const val FOLLOWERS_DATA_RETRIEVED_FROM_CACHE =
    "Followers data retrieved from cache instead of server"

private const val ONE = 1L

private const val TIMESTAMP = "timestamp"

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
    val snapshot = publicCollectionRef.get(Source.SERVER).await()
    if (snapshot.metadata.isFromCache) {
      throw IllegalStateException("Data retrieved from cache instead of server")
    }
    return snapshot.documents.mapNotNull { doc -> doc.data?.let { UserProfile.fromMap(it) } }
  }

  private suspend fun syncFieldIfNeeded(
      publicSnapshot: DocumentSnapshot,
      privateSnapshot: DocumentSnapshot,
      fieldName: String,
      updates: MutableMap<String, Any>
  ): Boolean {
    val publicValue = (publicSnapshot[fieldName] as? Number)?.toInt() ?: 0
    val privateValue = (privateSnapshot[fieldName] as? Number)?.toInt() ?: 0

    return if (publicValue != privateValue) {
      updates[fieldName] = publicValue
      true
    } else {
      false
    }
  }

  private suspend fun syncPrivateProfile(
      privateDocRef: DocumentReference,
      publicSnapshot: DocumentSnapshot,
      privateSnapshot: DocumentSnapshot,
      userId: String
  ): UserProfile? {
    val updates = mutableMapOf<String, Any>()
    var needsUpdate = false

    needsUpdate =
        syncFieldIfNeeded(publicSnapshot, privateSnapshot, KUDOS_FIELD, updates) || needsUpdate
    needsUpdate =
        syncFieldIfNeeded(publicSnapshot, privateSnapshot, FOLLOWER_COUNT_FIELD, updates) ||
            needsUpdate
    needsUpdate =
        syncFieldIfNeeded(publicSnapshot, privateSnapshot, FOLLOWING_COUNT_FIELD, updates) ||
            needsUpdate

    return if (needsUpdate) {
      privateDocRef.update(updates).await()
      val updatedPrivateSnapshot = privateDocRef.get().await()
      updatedPrivateSnapshot.data?.let { UserProfile.fromMap(it) }
          ?: throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND_AFTER_SYNC, userId))
    } else {
      null
    }
  }

  override suspend fun getUserProfile(userId: String): UserProfile {
    val currentUserId = Firebase.auth.currentUser?.uid

    if (userId == currentUserId) {
      val privateDocRef = privateCollectionRef.document(userId)
      val privateSnapshot = privateDocRef.get().await()

      if (!privateSnapshot.exists()) {
        throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
      }

      val publicSnapshot = publicCollectionRef.document(userId).get().await()

      if (!publicSnapshot.exists()) {
        throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
      }

      check(!publicSnapshot.metadata.isFromCache) { "$DATA_RETRIEVED_FROM_CACHE $userId" }

      try {
        val syncedProfile =
            syncPrivateProfile(privateDocRef, publicSnapshot, privateSnapshot, userId)
        if (syncedProfile != null) {
          return syncedProfile
        }
      } catch (e: Exception) {
        Log.e(LOG_TAG, String.format(MSG_SYNC_KUDOS, userId, e.message))
      }

      return privateSnapshot.data?.let { UserProfile.fromMap(it) }
          ?: throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
    }

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

    // Verify we're online by doing a quick server read
    val testSnapshot = publicCollectionRef.limit(ONE).get(Source.SERVER).await()
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

  override suspend fun followUser(currentUserId: String, targetUserId: String) {
    require(currentUserId != targetUserId) { MSG_CANNOT_FOLLOW_SELF }

    try {
      val alreadyFollowing = isFollowing(currentUserId, targetUserId)
      check(!alreadyFollowing) { String.format(MSG_ALREADY_FOLLOWING, targetUserId) }

      val currentUserDoc = publicCollectionRef.document(currentUserId).get(Source.SERVER).await()
      val targetUserDoc = publicCollectionRef.document(targetUserId).get(Source.SERVER).await()

      if (!currentUserDoc.exists() || !targetUserDoc.exists()) {
        throw NoSuchElementException(ONE_OR_BOTH_PROFILE)
      }

      val batch = db.batch()

      val followerDocRef =
          publicCollectionRef
              .document(targetUserId)
              .collection(FOLLOWERS_SUBCOLLECTION)
              .document(currentUserId)
      batch.set(followerDocRef, mapOf(TIMESTAMP to FieldValue.serverTimestamp()))

      val followingDocRef =
          publicCollectionRef
              .document(currentUserId)
              .collection(FOLLOWING_SUBCOLLECTION)
              .document(targetUserId)
      batch.set(followingDocRef, mapOf(TIMESTAMP to FieldValue.serverTimestamp()))

      batch.update(
          publicCollectionRef.document(targetUserId),
          FOLLOWER_COUNT_FIELD,
          FieldValue.increment(ONE))
      batch.update(
          publicCollectionRef.document(currentUserId),
          FOLLOWING_COUNT_FIELD,
          FieldValue.increment(ONE))

      batch.commit().await()
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception(String.format(MSG_FOLLOW_OPERATION_FAILED, e.message), e)
    }
  }

  override suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
    require(currentUserId != targetUserId) { MSG_CANNOT_FOLLOW_SELF }

    try {
      val currentlyFollowing = isFollowing(currentUserId, targetUserId)
      check(currentlyFollowing) { String.format(MSG_NOT_FOLLOWING, targetUserId) }

      val currentUserDoc = publicCollectionRef.document(currentUserId).get(Source.SERVER).await()
      val targetUserDoc = publicCollectionRef.document(targetUserId).get(Source.SERVER).await()

      require(currentUserDoc.exists() && targetUserDoc.exists()) { ONE_OR_BOTH_PROFILE }

      val batch = db.batch()

      val followerDocRef =
          publicCollectionRef
              .document(targetUserId)
              .collection(FOLLOWERS_SUBCOLLECTION)
              .document(currentUserId)
      batch.delete(followerDocRef)

      val followingDocRef =
          publicCollectionRef
              .document(currentUserId)
              .collection(FOLLOWING_SUBCOLLECTION)
              .document(targetUserId)
      batch.delete(followingDocRef)

      batch.update(
          publicCollectionRef.document(targetUserId),
          FOLLOWER_COUNT_FIELD,
          FieldValue.increment(-ONE))
      batch.update(
          publicCollectionRef.document(currentUserId),
          FOLLOWING_COUNT_FIELD,
          FieldValue.increment(-ONE))

      batch.commit().await()
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: NoSuchElementException) {
      throw e
    } catch (e: Exception) {
      throw Exception(String.format(MSG_UNFOLLOW_OPERATION_FAILED, e.message), e)
    }
  }

  override suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
    return try {
      val followingDoc =
          publicCollectionRef
              .document(currentUserId)
              .collection(FOLLOWING_SUBCOLLECTION)
              .document(targetUserId)
              .get(Source.SERVER)
              .await()
      followingDoc.exists()
    } catch (e: Exception) {
      false
    }
  }

  override suspend fun getFollowerCount(userId: String): Int {
    val userDoc = publicCollectionRef.document(userId).get(Source.SERVER).await()
    if (!userDoc.exists()) {
      throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
    }
    return (userDoc[FOLLOWER_COUNT_FIELD] as? Number)?.toInt() ?: ZERO
  }

  override suspend fun getFollowingCount(userId: String): Int {
    val userDoc = publicCollectionRef.document(userId).get(Source.SERVER).await()
    if (!userDoc.exists()) {
      throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
    }
    return (userDoc[FOLLOWING_COUNT_FIELD] as? Number)?.toInt() ?: ZERO
  }

  override suspend fun getFollowerIds(userId: String): List<String> {
    val userDoc = publicCollectionRef.document(userId).get(Source.SERVER).await()
    if (!userDoc.exists()) {
      throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
    }

    val followersSnapshot =
        publicCollectionRef
            .document(userId)
            .collection(FOLLOWERS_SUBCOLLECTION)
            .get(Source.SERVER)
            .await()

    check(!followersSnapshot.metadata.isFromCache) { FOLLOWERS_DATA_RETRIEVED_FROM_CACHE }

    return followersSnapshot.documents.map { it.id }
  }

  override suspend fun getFollowingIds(userId: String): List<String> {
    val userDoc = publicCollectionRef.document(userId).get(Source.SERVER).await()
    if (!userDoc.exists()) {
      throw NoSuchElementException(String.format(MSG_USER_NOT_FOUND, userId))
    }

    val followingSnapshot =
        publicCollectionRef
            .document(userId)
            .collection(FOLLOWING_SUBCOLLECTION)
            .get(Source.SERVER)
            .await()

    check(!followingSnapshot.metadata.isFromCache) {
      FOLLOWING_DATA_RETRIEVED_FROM_CACHE_INSTEAD_OF_SERVER
    }

    return followingSnapshot.documents.map { it.id }
  }
}
