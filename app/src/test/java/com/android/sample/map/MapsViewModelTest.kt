package com.android.sample.map

import com.android.sample.model.map.Location
import com.android.sample.model.profile.Section
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.map.MapViewModel
import com.google.android.gms.maps.model.LatLng
import java.util.Date
import java.util.UUID
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapsViewModelTest {

  // Fake repository used for testing/development because the real one is not implemented yet
  private class FakeRequestRepository : RequestRepository {
    private var shouldThrowError = false
    private var requestsToReturn: MutableList<Request> = mutableListOf()

    fun setShouldThrowError(shouldTrow: Boolean) {
      shouldThrowError = shouldTrow
    }

    override fun getNewRequestId(): String {
      return UUID.randomUUID().toString()
    }

    override suspend fun getAllRequests(): List<Request> {
      if (shouldThrowError) {
        throw RuntimeException("Network error")
      }
      return requestsToReturn
    }

    override suspend fun getRequest(requestId: String): Request {
      // not the real implementation, just for running
      return Request(
          "",
          "",
          "",
          emptyList(),
          Location(0.0, 0.0, ""),
          "",
          RequestStatus.ARCHIVED,
          Date(0L),
          Date(0L),
          emptyList(),
          emptyList(),
          "")
    }

    override suspend fun addRequest(request: Request) {
      requestsToReturn.add(request)
    }

    override suspend fun updateRequest(requestId: String, updatedRequest: Request) {
      val index = requestsToReturn.indexOfFirst { it.requestId == requestId }
      if (index != -1) {
        requestsToReturn[index] = updatedRequest
      }
    }

    override suspend fun deleteRequest(requestId: String) {
      val requestToDelete = requestsToReturn.find { it.requestId == requestId }
      if (requestToDelete != null) {
        requestsToReturn.remove(requestToDelete)
      }
    }

    override fun hasUserAcceptedRequest(request: Request): Boolean {
      return true
    }

    override suspend fun acceptRequest(requestId: String) {}

    override suspend fun cancelAcceptance(requestId: String) {}

    override suspend fun isOwnerOfRequest(request: Request): Boolean = false
  }

  private class FakeProfileRepository : UserProfileRepository {
    override fun getNewUid(): String {
      return ""
    }

    override suspend fun getAllUserProfiles(): List<UserProfile> {
      return listOf()
    }

    override suspend fun getUserProfile(userId: String): UserProfile {
      return UserProfile("", "", "", "", null, 0, Section.COMPUTER_SCIENCE, Date(0L))
    }

    override suspend fun addUserProfile(userProfile: UserProfile) {}

    override suspend fun updateUserProfile(userId: String, updatedProfile: UserProfile) {}

    override suspend fun deleteUserProfile(userId: String) {}

    override suspend fun searchUserProfiles(query: String, limit: Int): List<UserProfile> {
      return listOf()
    }
  }

  private lateinit var fakeRepository: FakeRequestRepository
  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var viewModel: MapViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val request1 =
      Request(
          "1",
          "test1",
          "",
          emptyList(),
          Location(10.0, 10.0, "test1"),
          "",
          RequestStatus.ARCHIVED,
          Date(0L),
          Date(0L),
          emptyList(),
          emptyList(),
          "")
  private val request2 =
      Request(
          "2",
          "test2",
          "",
          emptyList(),
          Location(20.0, 20.0, "test2"),
          "",
          RequestStatus.ARCHIVED,
          Date(0L),
          Date(0L),
          emptyList(),
          emptyList(),
          "")
  private val request3 =
      Request(
          "3",
          "test3",
          "",
          emptyList(),
          Location(0.0, 50.0, "test3"),
          "",
          RequestStatus.ARCHIVED,
          Date(0L),
          Date(0L),
          emptyList(),
          emptyList(),
          "")

  private val EPFL_LOCATION = Location(46.5191, 6.5668, "EPFL")

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    fakeRepository = FakeRequestRepository()
    fakeProfileRepository = FakeProfileRepository()
    Dispatchers.setMain(testDispatcher)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun handleError_is_good() {
    fakeRepository.setShouldThrowError(false)

    viewModel = MapViewModel(fakeRepository, fakeProfileRepository)

    TestCase.assertNull(viewModel.uiState.value.errorMsg)

    TestCase.assertNull(viewModel.uiState.value.errorMsg)
    fakeRepository.setShouldThrowError(true)

    runTest {
      viewModel.refreshUIState()
      advanceUntilIdle()
      TestCase.assertNotNull(viewModel.uiState.value.errorMsg)
      viewModel.clearErrorMsg()
      advanceUntilIdle()
      TestCase.assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  // test if adding, editing or delete a request is working
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun baseRequestsIsGood() {
    fakeRepository.setShouldThrowError(false)
    viewModel = MapViewModel(fakeRepository, fakeProfileRepository)

    runTest {
      viewModel.refreshUIState()
      advanceUntilIdle()

      Assert.assertEquals(
          LatLng(EPFL_LOCATION.latitude, EPFL_LOCATION.longitude), viewModel.uiState.value.target)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun addRequestsIsGood() {
    fakeRepository.setShouldThrowError(false)
    viewModel = MapViewModel(fakeRepository, fakeProfileRepository)
    runTest {
      fakeRepository.addRequest(request1)
      viewModel.refreshUIState()
      advanceUntilIdle()
      Assert.assertEquals(LatLng(10.0, 10.0), viewModel.uiState.value.target)
      Assert.assertEquals(listOf(request1), viewModel.uiState.value.request)
      fakeRepository.deleteRequest(request1.requestId)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun editRequestsIsGood() {
    fakeRepository.setShouldThrowError(false)
    viewModel = MapViewModel(fakeRepository, fakeProfileRepository)
    runTest {
      fakeRepository.addRequest(request1)
      fakeRepository.updateRequest("1", request3)
      viewModel.refreshUIState()
      advanceUntilIdle()
      Assert.assertEquals(LatLng(0.0, 50.0), viewModel.uiState.value.target)
      Assert.assertEquals(listOf(request3), viewModel.uiState.value.request)
      fakeRepository.deleteRequest(request3.requestId)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun deleteRequestsIsGood() {
    fakeRepository.setShouldThrowError(false)
    viewModel = MapViewModel(fakeRepository, fakeProfileRepository)
    runTest {
      fakeRepository.addRequest(request3)
      fakeRepository.addRequest(request2)
      fakeRepository.deleteRequest("3")
      viewModel.refreshUIState()
      advanceUntilIdle()
      Assert.assertEquals(LatLng(20.0, 20.0), viewModel.uiState.value.target)
      Assert.assertEquals(listOf(request2), viewModel.uiState.value.request)
      fakeRepository.deleteRequest(request2.requestId)
    }
  }
}
