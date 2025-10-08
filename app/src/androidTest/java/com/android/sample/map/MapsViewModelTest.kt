package com.android.sample.map

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.model.map.Location
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.ui.map.MapScreen
import com.android.sample.ui.map.MapViewModel
import com.google.android.gms.maps.model.LatLng
import java.util.UUID
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapsViewModelTest {

  @get:Rule val composeTestRule = createComposeRule()

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
          0L,
          0L,
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
  }

  private lateinit var fakeRepository: FakeRequestRepository
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
          0L,
          0L,
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
          0L,
          0L,
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
          0L,
          0L,
          emptyList(),
          emptyList(),
          "")

  private val EPFL_LOCATION = Location(46.5191, 6.5668, "EPFL")

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    fakeRepository = FakeRequestRepository()
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

    viewModel = MapViewModel(fakeRepository)

    assertNull(viewModel.uiState.value.errorMsg)

    composeTestRule.setContent { MapScreen(viewModel) }
    assertNull(viewModel.uiState.value.errorMsg)
    fakeRepository.setShouldThrowError(true)

    runTest {
      viewModel.refreshUIState()
      advanceUntilIdle()
      assertNotNull(viewModel.uiState.value.errorMsg)
      viewModel.clearErrorMsg()
      advanceUntilIdle()
      assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  // test if adding, editing or delete a request is working
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun baseRequestsIsGood() {
      fakeRepository.setShouldThrowError(false)
      viewModel = MapViewModel(fakeRepository)
      composeTestRule.setContent { MapScreen(viewModel) }

      runTest {
          viewModel.refreshUIState()
          advanceUntilIdle()

          assertEquals(
              LatLng(EPFL_LOCATION.latitude, EPFL_LOCATION.longitude),
              viewModel.uiState.value.target
          )
      }
  }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun addRequestsIsGood() {
        fakeRepository.setShouldThrowError(false)
        viewModel = MapViewModel(fakeRepository)
        composeTestRule.setContent { MapScreen(viewModel) }
        runTest {
            fakeRepository.addRequest(request1)
            viewModel.refreshUIState()
            advanceUntilIdle()
            assertEquals(LatLng(10.0, 10.0), viewModel.uiState.value.target)
            assertEquals(listOf(request1), viewModel.uiState.value.request)
            fakeRepository.deleteRequest(request1.requestId)
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun editRequestsIsGood() {
        fakeRepository.setShouldThrowError(false)
        viewModel = MapViewModel(fakeRepository)
        composeTestRule.setContent { MapScreen(viewModel) }
        runTest {
            fakeRepository.addRequest(request1)
            fakeRepository.updateRequest("1", request3)
            viewModel.refreshUIState()
            advanceUntilIdle()
            assertEquals(LatLng(0.0, 50.0), viewModel.uiState.value.target)
            assertEquals(listOf(request3), viewModel.uiState.value.request)
            fakeRepository.deleteRequest(request3.requestId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteRequestsIsGood() {
        fakeRepository.setShouldThrowError(false)
        viewModel = MapViewModel(fakeRepository)
        composeTestRule.setContent { MapScreen(viewModel) }
        runTest {
            fakeRepository.addRequest(request3)
            fakeRepository.addRequest(request2)
            fakeRepository.deleteRequest("3")
            viewModel.refreshUIState()
            advanceUntilIdle()
            assertEquals(LatLng(20.0, 20.0), viewModel.uiState.value.target)
            assertEquals(listOf(request2), viewModel.uiState.value.request)
            fakeRepository.deleteRequest(request2.requestId)
        }
    }

}
