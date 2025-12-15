package com.android.sample.ui.map

import android.content.Context
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.map.FusedLocationProvider
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestOwnership
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.navigation.TopNavigationBar
import com.android.sample.ui.request.RequestSearchFilterViewModel
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MapTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val ZOOM_IN_BUTTON = "zoomInButton"
  const val ZOOM_OUT_BUTTON = "zoomOutButton"
  const val ZOOM_LEVEL = "zoomLevel"
  const val DRAG_DOWN_MENU = "dragDownMenu"
  const val DRAG = "drag"
  const val BUTTON_X = "buttonX"
  const val REQUEST_TITLE = "requestTitle"
  const val REQUEST_DESCRIPTION = "requestDescription"
  const val START_DATE = "startDate"
  const val END_DATE = "endDate"
  const val REQUEST_STATUS = "requestStatus"
  const val REQUEST_LOCATION_NAME = "requestLocationName"
  const val BUTTON_DETAILS = "buttonDetails"
  const val PROFILE_NAME = "profileName"
  const val PROFILE_KUDOS = "profileKudos"
  const val PROFILE_KUDOS_TEXT = "profileKudosText"
  const val PROFILE_SECTION = "profileSection"
  const val PROFILE_SECTION_TEXT = "profileSectionText"
  const val PROFILE_CREATION_DATE = "profileCreationDate"
  const val PROFILE_EDIT_BUTTON = "profileEditButton"
  const val MAP_LIST_REQUEST = "mapListRequest"
  const val MAP_FILTER_OWNER = "mapFilterOwner"
  const val MAP_LIST_FILTER = "mapListFilter"

  fun testTagForTab(tab: String): String {
    return "tag${tab}"
  }

  /** give test tag for RequestOwnerShip */
  fun testTagForRequestOwnership(filter: RequestOwnership): String {
    return "filter$filter"
  }
}

/**
 * Main screen displaying a Google Map with request markers and filtering capabilities. Handles
 * location permissions, request clustering, and displays detailed information about selected
 * requests in a bottom sheet.
 *
 * @param viewModel ViewModel managing map state and operations
 * @param navigationActions Navigation actions for screen transitions
 * @param searchFilterViewModel ViewModel for filtering and searching requests
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel =
        viewModel(
            factory =
                MapViewModelFactory(
                    locationProvider = FusedLocationProvider(LocalContext.current))),
    navigationActions: NavigationActions? = null,
    searchFilterViewModel: RequestSearchFilterViewModel = viewModel(),
    requestId: String? = null
) {
  val context = LocalContext.current
  val uiState: MapUIState by viewModel.uiState.collectAsState()
  val currentUserId = viewModel.getCurrentUserID()
  var showZoomDialog by remember { mutableStateOf(false) }

  var isMapReady by remember { mutableStateOf(false) }
  var isFirstTime by remember { mutableStateOf(true) }

  // Refresh UI on start
  LaunchedEffect(Unit) { viewModel.refreshUIState(requestId) }

  LaunchedEffect(requestId, isMapReady) {
    if (isMapReady && requestId != null) {
      viewModel.fromRequestDetailsToRequest(requestId)
    }
  }

  // Setup filters and requests
  val displayedRequests by searchFilterViewModel.displayedRequests.collectAsState()
  val finalFilteredRequests =
      setupRequestFiltering(
          displayedRequests,
          uiState.requestOwnership,
          currentUserId,
          viewModel,
          searchFilterViewModel,
          uiState.request,
          isFirstTime = isFirstTime,
          notFirstTime = { isFirstTime = false },
          hasTriedToGetLocation = uiState.hasTriedToGetLocation,
          currentLocation = uiState.currentLocation)

  // Setup permissions
  SetupLocationPermissions(context, viewModel)

  // Handle errors
  HandleErrorMessages(context, uiState.errorMsg, viewModel)

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.MAP_SCREEN),
      bottomBar = {
        BottomNavigationMenu(
            selectedNavigationTab = NavigationTab.Map, navigationActions = navigationActions)
      },
      topBar = {
        TopNavigationBar(
            selectedTab = NavigationTab.Map,
            onProfileClick = {
              navigationActions?.navigateTo(Screen.Profile("TODO"))
              viewModel.goOnAnotherScreen()
            },
            onZoomSettingsClick = { showZoomDialog = true })
      },
      content = { pd ->
        if (uiState.offlineMode) {
          Box(modifier = Modifier.fillMaxSize().padding(pd), contentAlignment = Alignment.Center) {
            Text(
                text = "Map is unavailable in offline mode",
                color = appPalette().error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp))
          }
          return@Scaffold
        }
        MapContent(
            paddingValues = pd,
            uiState = uiState,
            viewModel = viewModel,
            navigationActions = navigationActions,
            finalFilteredRequests = finalFilteredRequests,
            searchFilterViewModel = searchFilterViewModel,
            onMapReady = { isMapReady = true },
            isMapReady)
      })
  if (showZoomDialog) {
    ZoomSettingsDialog(
        currentPreference = uiState.zoomPreference,
        onPreferenceChange = { preference ->
          viewModel.updateZoomPreference(preference)
          showZoomDialog = false
        },
        onDismiss = { showZoomDialog = false })
  }
}

/**
 * Sets up location permission handling for the map screen. Checks if permissions have been
 * requested and launches the permission dialog if needed.
 *
 * @param context Android context for accessing system services
 * @param viewModel ViewModel to handle location updates
 */
@Composable
private fun SetupLocationPermissions(context: Context, viewModel: MapViewModel) {
  val locationManager = remember {
    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  }
  val permissionHandler =
      remember(viewModel) { MapPermissionResultHandler(viewModel, locationManager) }

  val permissionLauncher =
      rememberLauncherForActivityResult(
          ActivityResultContracts.RequestMultiplePermissions(),
          permissionHandler::handlePermissionResult)

  LaunchedEffect(Unit) { handleLocationPermissionCheck(context, viewModel, permissionLauncher) }
}

/**
 * Sets up request filtering based on ownership and search criteria. Automatically zooms to filtered
 * requests when they change.
 *
 * @param displayedRequests List of requests after search filtering
 * @param requestOwnership Current ownership filter (ALL, OWN, OTHER, etc.)
 * @param currentUserId ID of the current user for ownership filtering
 * @param viewModel ViewModel for map operations
 * @param searchFilterViewModel ViewModel managing search and filter state
 * @param allRequests Complete list of all requests
 * @return List of requests after applying all filters
 */
@Composable
private fun setupRequestFiltering(
    displayedRequests: List<Request>,
    requestOwnership: RequestOwnership,
    currentUserId: String,
    viewModel: MapViewModel,
    searchFilterViewModel: RequestSearchFilterViewModel,
    allRequests: List<Request>,
    isFirstTime: Boolean,
    notFirstTime: () -> Unit,
    hasTriedToGetLocation: Boolean,
    currentLocation: LatLng?
): List<Request> {
  val finalFilteredRequests =
      remember(displayedRequests, requestOwnership, currentUserId) {
        viewModel.filterWithOwnerShip(displayedRequests, currentUserId)
      }

  LaunchedEffect(allRequests) { searchFilterViewModel.initializeWithRequests(allRequests) }

  LaunchedEffect(finalFilteredRequests) {
    if (isFirstTime) {
      notFirstTime()
      return@LaunchedEffect
    }
    if (hasTriedToGetLocation) {
      val noResultWithFilters = finalFilteredRequests.isEmpty() && allRequests.isNotEmpty()
      if (noResultWithFilters) {
        viewModel.setErrorMsg(ConstantMap.ERROR_MESSAGE_NO_REQUEST_WITH_FILTERS)
        if (currentLocation != null) {
          viewModel.zoomOnRequest(finalFilteredRequests)
        }
      } else {
        viewModel.zoomOnRequest(finalFilteredRequests)
      }
    }
  }

  return finalFilteredRequests
}

/**
 * Displays error messages as Toast notifications and automatically clears them.
 *
 * @param context Android context for showing toasts
 * @param errorMsg Current error message to display (null if no error)
 * @param viewModel ViewModel to clear error after display
 */
@Composable
private fun HandleErrorMessages(context: Context, errorMsg: String?, viewModel: MapViewModel) {
  var currentToast by remember { mutableStateOf<Toast?>(null) }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      currentToast?.cancel()
      currentToast = Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT)
      currentToast?.show()

      viewModel.clearErrorMsg()
    }
  }
}

/**
 * Main content container for the map screen. Manages camera position, clustering, and all UI
 * overlays (bottom sheet, zoom controls, filters).
 *
 * @param paddingValues Padding from the Scaffold
 * @param uiState Current UI state
 * @param viewModel ViewModel for map operations
 * @param navigationActions Navigation actions
 * @param finalFilteredRequests List of requests after all filters applied
 * @param searchFilterViewModel ViewModel for search/filter operations
 */
@Composable
private fun MapContent(
    paddingValues: PaddingValues,
    uiState: MapUIState,
    viewModel: MapViewModel,
    navigationActions: NavigationActions?,
    finalFilteredRequests: List<Request>,
    searchFilterViewModel: RequestSearchFilterViewModel,
    onMapReady: () -> Unit,
    isMapReady: Boolean
) {
  val appPalette = appPalette()
  val coroutineScope = rememberCoroutineScope()

  // Camera setup
  val initialPosition =
      CameraPositionState(
          position =
              CameraPosition.fromLatLngZoom(
                  uiState.target, calculateZoomLevel(finalFilteredRequests.size)))
  val cameraPositionState = remember { initialPosition }
  val zoomLevel by remember { derivedStateOf { cameraPositionState.position.zoom } }

  LaunchedEffect(cameraPositionState.isMoving) {
    if (!isMapReady) {
      onMapReady()
    }
  }

  // Clustering
  val clusters =
      remember(finalFilteredRequests, zoomLevel) {
        clusterRequestsByDistance(
            requests = finalFilteredRequests,
            clusterRadiusMeters = getClusterRadiusForZoom(zoomLevel))
      }

  // Map UI settings
  val uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }
  Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    var filterBarHeight by remember { mutableStateOf(ConstantMap.ZERO.dp) }
    val density = LocalDensity.current

    // Google Map
    Box(modifier = Modifier.fillMaxSize()) {
      MapWithMarkers(
          cameraPositionState = cameraPositionState,
          uiSettings = uiSettings,
          clusters = clusters,
          zoomLevel = zoomLevel,
          viewModel = viewModel,
          coroutineScope = coroutineScope,
          currentLocation = uiState.currentLocation,
          isMapReady = isMapReady)

      // Bottom Sheet
      CurrentRequestBottomSheet(
          uiState = uiState,
          viewModel = viewModel,
          navigationActions = navigationActions,
          appPalette = appPalette,
          modifier = Modifier.align(Alignment.BottomCenter).padding(top = filterBarHeight))

      // List of requests overlay
      ListOfRequest(
          uiState,
          viewModel,
          appPalette,
          Modifier.align(Alignment.BottomCenter).padding(top = filterBarHeight),
          coroutineScope,
          cameraPositionState,
          navigationActions)

      // Zoom controls
      ZoomControls(
          uiState = uiState,
          cameraPositionState = cameraPositionState,
          coroutineScope = coroutineScope,
          appPalette = appPalette,
          modifier = Modifier.align(Alignment.BottomEnd))

      ZoomLevelTestTag(cameraPositionState)
      AutoZoomEffect(uiState, cameraPositionState, viewModel)
    }

    MapFilter(
        searchFilterViewModel = searchFilterViewModel,
        selectedOwnership = uiState.requestOwnership,
        viewModel = viewModel,
        modifier = Modifier.fillMaxWidth().wrapContentHeight().align(Alignment.TopCenter),
        onFilterBarHeightChanged = { height -> filterBarHeight = with(density) { height.toDp() } })
  }
}

/**
 * Renders the Google Map with clustered request markers and the user's current location.
 *
 * @param cameraPositionState State controlling the map's camera position and zoom
 * @param uiSettings Map UI settings (zoom controls, compass, etc.)
 * @param clusters List of request clusters to display as markers
 * @param zoomLevel Current zoom level of the map
 * @param viewModel ViewModel for handling marker clicks
 * @param coroutineScope Coroutine scope for animations
 * @param currentLocation User's current location to display (null if not available)
 */
@Composable
private fun MapWithMarkers(
    cameraPositionState: CameraPositionState,
    uiSettings: MapUiSettings,
    clusters: List<List<Request>>,
    zoomLevel: Float,
    viewModel: MapViewModel,
    coroutineScope: CoroutineScope,
    currentLocation: LatLng?,
    isMapReady: Boolean
) {
  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(MapTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState,
      uiSettings =
          uiSettings.copy(
              scrollGesturesEnabled = isMapReady,
              zoomGesturesEnabled = isMapReady,
              tiltGesturesEnabled = isMapReady,
              rotationGesturesEnabled = isMapReady)) {

        // Process clusters and merge with current location if needed
        val processedClusters =
            remember(clusters, currentLocation, zoomLevel) {
              if (currentLocation != null) {
                mergeCurrentLocationWithClusters(
                    clusters, currentLocation, getClusterRadiusForZoomForCurrentLocation(zoomLevel))
              } else {
                clusters.map { ClusterWithLocation(it, false) }
              }
            }

        // Request markers
        processedClusters.forEach { clusterData ->
          MapMarker(
              cluster = clusterData.cluster,
              zoomLevel = zoomLevel,
              viewModel = viewModel,
              cameraPositionState = cameraPositionState,
              coroutineScope = coroutineScope,
              isAtCurrentLocation = clusterData.isAtCurrentLocation)
        }

        // Current location marker (only if not merged with any cluster)
        if (currentLocation != null &&
            !isCurrentLocationNearAnyCluster(
                clusters, currentLocation, getClusterRadiusForZoomForCurrentLocation(zoomLevel))) {
          CurrentLocationMarker(location = currentLocation)
        }
      }

  // Overlay for blocking interaction
  if (!isMapReady) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(appPalette().white.copy(alpha = ConstantMap.ALPHA_BLOCK_CLICK))
                .clickable(enabled = false) {},
        contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
  }
}

/**
 * Renders a single marker on the map representing either a single request or a cluster. Handles
 * click events to either show request details or zoom into the cluster.
 *
 * @param cluster List of requests at this location (size 1 = single request, >1 = cluster)
 * @param zoomLevel Current zoom level for calculating cluster zoom target
 * @param viewModel ViewModel to update selected request/cluster
 * @param cameraPositionState State for animating camera position
 * @param coroutineScope Coroutine scope for animations
 * @param isAtCurrentLocation boolean who say if it's at position of current location
 */
@Composable
private fun MapMarker(
    cluster: List<Request>,
    zoomLevel: Float,
    viewModel: MapViewModel,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    isAtCurrentLocation: Boolean = false
) {
  val isACluster = cluster.size == ConstantMap.ONE
  val position =
      if (isACluster) {
        LatLng(cluster.first().location.latitude, cluster.first().location.longitude)
      } else {
        calculateClusterCenter(cluster)
      }

  Marker(
      state = MarkerState(position = position),
      icon =
          if (isACluster) {
            // Single request marker
            if (isAtCurrentLocation) {
              createMarkerWithNumber(cluster.size, true)
            } else {
              BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            }
          } else {
            // Cluster marker with number
            createMarkerWithNumber(cluster.size, isAtCurrentLocation)
          },
      onClick = {
        coroutineScope.launch {
          if (isACluster) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(position, zoomLevel),
                durationMs = ConstantMap.LONG_DURATION_ANIMATION)
            viewModel.updateCurrentRequest(cluster.first())
            viewModel.updateCurrentProfile(cluster.first().creatorId)
          } else {
            cameraPositionState.animate(
                update =
                    CameraUpdateFactory.newLatLngZoom(
                        position, zoomLevel + ConstantMap.ZOOM_LEVEL_TWO),
                durationMs = ConstantMap.LONG_DURATION_ANIMATION)
            viewModel.updateCurrentListRequest(cluster)
          }
        }
        true
      })
}

/**
 * Displays a special marker indicating the user's current location on the map. Uses a distinct cyan
 * color and includes a small accuracy circle.
 *
 * @param location The user's current GPS coordinates
 */
@Composable
private fun CurrentLocationMarker(location: LatLng) {

  var isInfoWindowShown by remember { mutableStateOf(false) }

  Marker(
      state = MarkerState(position = location),
      icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
      title = ConstantMap.YOUR_LOCATION,
      snippet = ConstantMap.YOU_ARE_HERE,
      onClick = {
        isInfoWindowShown = !isInfoWindowShown
        if (isInfoWindowShown) {
          it.showInfoWindow()
        } else {
          it.hideInfoWindow()
        }
        true
      })
}

/**
 * Displays floating action buttons for zooming in and out of the map. Only visible when no request
 * or request list is selected.
 *
 * @param uiState Current UI state to check if controls should be visible
 * @param cameraPositionState Camera state for zoom animations
 * @param coroutineScope Coroutine scope for animations
 * @param appPalette Color palette for button styling
 * @param modifier Modifier for positioning the controls
 */
@Composable
private fun ZoomControls(
    uiState: MapUIState,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    appPalette: AppPalette,
    modifier: Modifier = Modifier
) {
  if (uiState.currentRequest == null && uiState.currentListRequest == null) {
    Column(
        modifier = modifier.padding(UiDimens.SpacingMd),
        horizontalAlignment = Alignment.CenterHorizontally) {
          // Zoom In
          FloatingActionButton(
              onClick = {
                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) }
              },
              modifier =
                  Modifier.testTag(MapTestTags.ZOOM_IN_BUTTON)
                      .size(UiDimens.SpacingXxl)
                      .padding(UiDimens.SpacingXs),
              containerColor = appPalette.accent,
              contentColor = appPalette.surface) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = ConstantMap.ZOOM_IN,
                    tint = appPalette.surface)
              }

          // Zoom Out
          FloatingActionButton(
              onClick = {
                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) }
              },
              modifier =
                  Modifier.testTag(MapTestTags.ZOOM_OUT_BUTTON)
                      .size(UiDimens.SpacingXxl)
                      .padding(UiDimens.SpacingXs),
              containerColor = appPalette.accent,
              contentColor = appPalette.surface) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = ConstantMap.ZOOM_OUT)
              }
        }
  }
}

/**
 * Invisible box exposing the current zoom level via semantics for testing purposes.
 *
 * @param cameraPositionState Camera state containing the current zoom level
 */
@Composable
private fun ZoomLevelTestTag(cameraPositionState: CameraPositionState) {
  Box(
      modifier =
          Modifier.size(ConstantMap.ZERO.dp).semantics {
            testTag = "${MapTestTags.ZOOM_LEVEL}:${cameraPositionState.position.zoom}"
          })
}

/**
 * Automatically animates the camera to zoom to a target location when needed. Triggers when
 * uiState.needToZoom is true and marks zoom as completed afterward.
 *
 * @param uiState Current UI state containing zoom target and flag
 * @param cameraPositionState Camera state for zoom animation
 * @param viewModel ViewModel to mark zoom as completed
 */
@Composable
private fun AutoZoomEffect(
    uiState: MapUIState,
    cameraPositionState: CameraPositionState,
    viewModel: MapViewModel
) {
  LaunchedEffect(uiState.triggerZoomOnCurrentLoc) {
    cameraPositionState.animate(
        update =
            CameraUpdateFactory.newLatLngZoom(
                uiState.target, calculateZoomLevel(uiState.request.size)),
        durationMs = ConstantMap.VERY_LONG_DURATION_ANIMATION)
  }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
  MapScreen()
}
