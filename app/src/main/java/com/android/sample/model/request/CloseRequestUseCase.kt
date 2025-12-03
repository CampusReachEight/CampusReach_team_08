package com.android.sample.model.request

import android.util.Log
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.request_validation.HelpReceivedConstants
import com.android.sample.ui.request_validation.KudosConstants
import com.android.sample.ui.request_validation.KudosException

private const val TAG = "CloseRequestUseCase"

/**
 * Use case for closing a request and awarding kudos to helpers.
 *
 * This coordinates operations between RequestRepository and UserProfileRepository, keeping business
 * logic separate from data access layers.
 */
class CloseRequestUseCase(
    private val requestRepository: RequestRepository,
    private val userProfileRepository: UserProfileRepository
) {

  /**
   * Closes a request and awards kudos to selected helpers and optionally the creator.
   *
   * This operation:
   * 1. Closes the request (updates status and selectedHelpers)
   * 2. Awards kudos to selected helpers
   * 3. Awards kudos to creator if helpers were selected
   *
   * @param requestId The unique identifier of the request to close
   * @param selectedHelperIds List of user IDs who should receive kudos
   * @return Result indicating success or partial success with details
   */
  suspend fun execute(requestId: String, selectedHelperIds: List<String>): CloseRequestResult {
    return try {
      // Step 1: Close the request in repository
      val creatorShouldReceiveKudos = requestRepository.closeRequest(requestId, selectedHelperIds)

      val kudosResults = mutableListOf<KudosAwardResult>()

      // Step 2: Award kudos to helpers
      if (selectedHelperIds.isNotEmpty()) {
        val helperResult = awardKudosToHelpers(selectedHelperIds)
        kudosResults.add(helperResult)
      }

      // Step 3: Award kudos to creator if applicable
      if (creatorShouldReceiveKudos) {
        val request = requestRepository.getRequest(requestId)
        val creatorResult = awardKudosToCreator(request.creatorId)
        kudosResults.add(creatorResult)
      }

      // Step 4: Record help receive for creator
      if (selectedHelperIds.isNotEmpty()) {

        val request = requestRepository.getRequest(requestId)
        val creatorId = request.creatorId

        try {
          userProfileRepository.receiveHelp(creatorId, HelpReceivedConstants.HELP_RECEIVED_PER_HELP)
          Log.d(TAG, "Recorded help received for request: $creatorId")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to record help received for request $creatorId: ${e.message}", e)
          return CloseRequestResult.Failure(e)
        }
      }

      // Determine overall result
      val allKudosSucceeded = kudosResults.all { it is KudosAwardResult.Success }

      if (allKudosSucceeded) {
        CloseRequestResult.Success(
            helpersAwarded = selectedHelperIds.size, creatorAwarded = creatorShouldReceiveKudos)
      } else {
        CloseRequestResult.PartialSuccess(requestClosed = true, kudosResults = kudosResults)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close request: ${e.message}", e)
      CloseRequestResult.Failure(e)
    }
  }

  private suspend fun awardKudosToHelpers(helperIds: List<String>): KudosAwardResult {
    return try {
      val kudosMap = helperIds.associateWith { KudosConstants.KUDOS_PER_HELPER }
      userProfileRepository.awardKudosBatch(kudosMap)
      Log.d(TAG, "Successfully awarded kudos to ${helperIds.size} helpers")
      KudosAwardResult.Success(helperIds.size, isCreator = false)
    } catch (e: KudosException.UserNotFound) {
      Log.e(TAG, "Helper not found: ${e.message}", e)
      KudosAwardResult.Failed(helperIds, e)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to award kudos to helpers: ${e.message}", e)
      KudosAwardResult.Failed(helperIds, e)
    }
  }

  private suspend fun awardKudosToCreator(creatorId: String): KudosAwardResult {
    return try {
      userProfileRepository.awardKudos(creatorId, KudosConstants.KUDOS_FOR_CREATOR_RESOLUTION)
      Log.d(TAG, "Successfully awarded kudos to creator: $creatorId")
      KudosAwardResult.Success(1, isCreator = true)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to award kudos to creator: ${e.message}", e)
      KudosAwardResult.Failed(listOf(creatorId), e)
    }
  }
}

/** Result of closing a request with kudos awarding. */
sealed class CloseRequestResult {
  /** Request closed successfully and all kudos awarded. */
  data class Success(val helpersAwarded: Int, val creatorAwarded: Boolean) : CloseRequestResult()

  /** Request closed but some kudos awards failed. */
  data class PartialSuccess(val requestClosed: Boolean, val kudosResults: List<KudosAwardResult>) :
      CloseRequestResult()

  /** Failed to close the request. */
  data class Failure(val error: Exception) : CloseRequestResult()
}

/** Result of awarding kudos to a user or group of users. */
sealed class KudosAwardResult {
  data class Success(val count: Int, val isCreator: Boolean = false) : KudosAwardResult()

  data class Failed(val userIds: List<String>, val error: Exception) : KudosAwardResult()
}
