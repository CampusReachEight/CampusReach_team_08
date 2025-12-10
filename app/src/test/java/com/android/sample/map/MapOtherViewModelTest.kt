package com.android.sample.map

import com.android.sample.model.map.Location
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestOwnership
import com.android.sample.model.request.RequestRepository
import com.android.sample.model.request.RequestStatus
import com.android.sample.model.request.RequestType
import com.android.sample.model.request.Tags
import com.android.sample.ui.map.ConstantMap
import com.android.sample.ui.map.MapViewModel
import com.android.sample.ui.map.MapZoomPreference
import com.google.android.gms.maps.model.LatLng
import java.util.Date
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MapOtherViewModelTest {
  private lateinit var viewModel: MapViewModel
  private lateinit var requestRepository: RequestRepository
  private lateinit var profileRepository: UserProfileRepository
  private lateinit var request1: Request
  private lateinit var request2: Request
  private lateinit var request3: Request
  private lateinit var list: List<Request>
  private lateinit var listWithout1: List<Request>
  private lateinit var listWithout2: List<Request>
  private lateinit var listWithout3: List<Request>
  private lateinit var listWithout12: List<Request>
  private lateinit var listWithout23: List<Request>
  private lateinit var listWithout13: List<Request>
  private lateinit var fakeFusedLocationProvider: FakeLocProvider

  private val ids = "id"
  private val titles = "title"
  private val descriptions = "description"
  private val reqType: List<RequestType> = listOf()
  private val loc = Location(0.0, 0.0, "")
  private val locName = "locName"
  private val statusList: RequestStatus = RequestStatus.OPEN
  private val arrDate: Date = Date(1672531200000)
  private val expDate: Date = Date(1672617600000)
  private val acceptList: List<String> = listOf()
  private val listTag: List<Tags> = listOf()
  private val creatorIds = "creatorId"

  private val creatorId1 = "creatorId1"
  private val creatorId2 = "creatorId2"
  private val id1 = "id1"
  private val id2 = "id2"
  private val id3 = "id3"
  private val people1 = listOf(creatorId2)
  private val people2: List<String> = listOf()
  private val people3 = listOf(creatorId1)

  private val testDispatcher = StandardTestDispatcher()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    requestRepository = mock(RequestRepository::class.java)
    profileRepository = mock(UserProfileRepository::class.java)
    fakeFusedLocationProvider = FakeLocProvider()
    viewModel = MapViewModel(requestRepository, profileRepository, fakeFusedLocationProvider)
    request1 = doARequest(id = id1, people = people1, creatorId = creatorId1)
    request2 = doARequest(id = id2, people = people2, creatorId = creatorId1)
    request3 = doARequest(id = id3, people = people3, creatorId = creatorId2)
    list = listOf(request1, request2, request3)
    listWithout1 = listOf(request2, request3)
    listWithout2 = listOf(request1, request3)
    listWithout3 = listOf(request1, request2)
    listWithout12 = listOf(request3)
    listWithout23 = listOf(request1)
    listWithout13 = listOf(request2)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun updateFilterOwnerShipWorks() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.ALL)
      assertEquals(RequestOwnership.ALL, viewModel.uiState.value.requestOwnership)

      viewModel.updateFilterOwnerShip(RequestOwnership.OTHER)
      assertEquals(RequestOwnership.OTHER, viewModel.uiState.value.requestOwnership)

      viewModel.updateFilterOwnerShip(RequestOwnership.NOT_ACCEPTED)
      assertEquals(RequestOwnership.NOT_ACCEPTED, viewModel.uiState.value.requestOwnership)

      viewModel.updateFilterOwnerShip(RequestOwnership.NOT_ACCEPTED_BY_ME)
      assertEquals(RequestOwnership.NOT_ACCEPTED_BY_ME, viewModel.uiState.value.requestOwnership)

      viewModel.updateFilterOwnerShip(RequestOwnership.ACCEPTED)
      assertEquals(RequestOwnership.ACCEPTED, viewModel.uiState.value.requestOwnership)

      viewModel.updateFilterOwnerShip(RequestOwnership.OWN)
      assertEquals(RequestOwnership.OWN, viewModel.uiState.value.requestOwnership)
    }
  }

  @Test
  fun allRequestAreDisplayed() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.ALL)
      val result = viewModel.filterWithOwnerShip(list, creatorId1)
      assertTrue(twoSameList(result, list))
    }
  }

  @Test
  fun myRequestsAreDisplayed() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.OWN)
      val result = viewModel.filterWithOwnerShip(list, creatorId1)
      assertTrue(twoSameList(result, listWithout3))

      val result2 = viewModel.filterWithOwnerShip(list, creatorId2)
      assertTrue(twoSameList(result2, listWithout12))
    }
  }

  @Test
  fun notMyRequestsAreDisplayed() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.OTHER)
      val result = viewModel.filterWithOwnerShip(list, creatorId1)
      assertTrue(twoSameList(result, listWithout12))

      val result2 = viewModel.filterWithOwnerShip(list, creatorId2)
      assertTrue(twoSameList(result2, listWithout3))
    }
  }

  @Test
  fun acceptedByMeRequestAreDisplayed() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.ACCEPTED)
      val result = viewModel.filterWithOwnerShip(list, creatorId1)
      assertTrue(twoSameList(result, listWithout12))

      val result2 = viewModel.filterWithOwnerShip(list, creatorId2)
      assertTrue(twoSameList(result2, listWithout23))
    }
  }

  @Test
  fun notAcceptedByMeRequestAreDisplayed() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.NOT_ACCEPTED_BY_ME)
      val result = viewModel.filterWithOwnerShip(list, creatorId1)
      assertTrue(twoSameList(result, listWithout3))

      val result2 = viewModel.filterWithOwnerShip(list, creatorId2)
      assertTrue(twoSameList(result2, listWithout1))
    }
  }

  @Test
  fun notAcceptedRequestAreDisplayed() {
    runTest {
      viewModel.updateFilterOwnerShip(RequestOwnership.NOT_ACCEPTED)
      val result = viewModel.filterWithOwnerShip(list, creatorId1)
      assertTrue(twoSameList(result, listWithout13))
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun fetchAcceptedRequestHaveCurrent() {
    runTest {
      `when`(requestRepository.getAllCurrentRequests()).thenReturn(list)

      viewModel.updateCurrentRequest(request1)

      advanceUntilIdle()

      viewModel.refreshUIState(null)

      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.needToZoom)
      assertEquals(request1, viewModel.uiState.value.currentRequest)
    }
  }

  @Test
  fun setLocationPermissionError_stopsLoadingAndSetsFlags() {

    viewModel.setLocationPermissionError()

    with(viewModel.uiState.value) {
      assertEquals(ConstantMap.ERROR_MESSAGE_LOCATION_PERMISSION, errorMsg)
      assertFalse(isLoadingLocation)
      assertTrue(hasTriedToGetLocation)
    }
  }

  @Test
  fun zoomOnRequest_whenCannotOtherZoom_doesNothing() {

    viewModel.cannotOtherZoomNow()
    val initialState = viewModel.uiState.value

    viewModel.zoomOnRequest(list)

    assertEquals(initialState, viewModel.uiState.value)
    assertFalse(viewModel.uiState.value.needToZoom)
  }

  @Test
  fun zoomOnRequest_whenWasOnAnotherScreen_comesBackFromAnotherScreen() {

    viewModel.goOnAnotherScreen()
    viewModel.updateCurrentRequest(request1)

    viewModel.zoomOnRequest(list)

    assertFalse(viewModel.uiState.value.wasOnAnotherScreen)
  }

  @Test
  fun zoomOnRequest_whenNoAutoZoom_doesNothing() {
    viewModel.updateZoomPreference(MapZoomPreference.NO_AUTO_ZOOM)
    val initialTarget = viewModel.uiState.value.target

    viewModel.zoomOnRequest(list)

    assertEquals(initialTarget, viewModel.uiState.value.target)
    assertFalse(viewModel.uiState.value.needToZoom)
  }

  @Test
  fun zoomOnRequest_withNoCurrentLocation_zoomsToFirstRequest() {

    viewModel.zoomOnRequest(list)

    val expectedTarget = LatLng(request1.location.latitude, request1.location.longitude)
    assertEquals(expectedTarget, viewModel.uiState.value.target)
    assertTrue(viewModel.uiState.value.needToZoom)
  }

  @Test
  fun zoomOnRequest_withNoCurrentLocation_andEmptyList_zoomsToEPFL() {

    viewModel.zoomOnRequest(emptyList())

    val expectedTarget =
        LatLng(MapViewModel.EPFL_LOCATION.latitude, MapViewModel.EPFL_LOCATION.longitude)
    assertEquals(expectedTarget, viewModel.uiState.value.target)
    assertTrue(viewModel.uiState.value.needToZoom)
  }

  @Test
  fun zoomOnRequest_whenWasOnAnotherScreenWithCurrentRequest_zoomsToCurrentRequest() {
    viewModel.updateCurrentRequest(request1)
    viewModel.goOnAnotherScreen()

    viewModel.zoomOnRequest(list)

    val expectedTarget = LatLng(request1.location.latitude, request1.location.longitude)
    assertEquals(expectedTarget, viewModel.uiState.value.target)
    assertTrue(viewModel.uiState.value.needToZoom)
    assertFalse(viewModel.uiState.value.wasOnAnotherScreen)
  }

  private fun doARequest(
      id: String = ids,
      title: String = titles,
      description: String = descriptions,
      requestType: List<RequestType> = reqType,
      location: Location = loc,
      locationName: String = locName,
      status: RequestStatus = statusList,
      arrivalDate: Date = arrDate,
      expirationDate: Date = expDate,
      people: List<String> = acceptList,
      tag: List<Tags> = listTag,
      creatorId: String = creatorIds
  ): Request {
    return Request(
        id,
        title,
        description,
        requestType,
        location,
        locationName,
        status,
        arrivalDate,
        expirationDate,
        people,
        tag,
        creatorId)
  }

  private fun twoSameList(list1: List<Request>, list2: List<Request>): Boolean {
    if (list1.size != list2.size) return false

    val set1 = list1.map { it.requestId }.toSet()
    val set2 = list2.map { it.requestId }.toSet()

    return set1 == set2
  }
}
