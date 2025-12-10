package com.android.sample.ui.map

import android.content.Context
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.map.FusedLocationProvider
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.request.Request
import com.android.sample.model.request.RequestOwnership
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.navigation.TopNavigationBar
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.request.ConstantRequestList
import com.android.sample.ui.request.RequestListItem
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
    searchFilterViewModel: RequestSearchFilterViewModel = viewModel()
) {
  val context = LocalContext.current
  val uiState: MapUIState by viewModel.uiState.collectAsState()
  val currentUserId = viewModel.getCurrentUserID()
  var showZoomDialog by remember { mutableStateOf(false) }

  // Setup permissions
  SetupLocationPermissions(context, viewModel, uiState)

  // Setup filters and requests
  val displayedRequests by searchFilterViewModel.displayedRequests.collectAsState()
  val finalFilteredRequests =
      setupRequestFiltering(
          displayedRequests,
          uiState.requestOwnership,
          currentUserId,
          viewModel,
          searchFilterViewModel,
          uiState.request)

  // Handle errors
  HandleErrorMessages(context, uiState.errorMsg, viewModel)

  // Refresh UI on start
  LaunchedEffect(Unit) { viewModel.refreshUIState() }

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
        MapContent(
            paddingValues = pd,
            uiState = uiState,
            viewModel = viewModel,
            navigationActions = navigationActions,
            finalFilteredRequests = finalFilteredRequests,
            searchFilterViewModel = searchFilterViewModel)
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
 * @param uiState Current UI state containing permission status
 */
@Composable
private fun SetupLocationPermissions(
    context: Context,
    viewModel: MapViewModel,
    uiState: MapUIState
) {
  val locationManager = remember {
    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  }
  val permissionHandler =
      remember(viewModel) { MapPermissionResultHandler(viewModel, locationManager) }

  val permissionLauncher =
      rememberLauncherForActivityResult(
          ActivityResultContracts.RequestMultiplePermissions(),
          permissionHandler::handlePermissionResult)

  LaunchedEffect(Unit) {
    if (!uiState.hasAskedLocationPermission) {
      handleLocationPermissionCheck(context, viewModel, permissionLauncher)
    }
  }
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
    allRequests: List<Request>
): List<Request> {
  val finalFilteredRequests =
      remember(displayedRequests, requestOwnership, currentUserId) {
        viewModel.filterWithOwnerShip(displayedRequests, currentUserId)
      }

  LaunchedEffect(allRequests) { searchFilterViewModel.initializeWithRequests(allRequests) }

  LaunchedEffect(finalFilteredRequests) {
    if (finalFilteredRequests.isNotEmpty()) {
      viewModel.zoomOnRequest(finalFilteredRequests)
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
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
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
    searchFilterViewModel: RequestSearchFilterViewModel
) {
  val appPalette = appPalette()
  val coroutineScope = rememberCoroutineScope()

  // Camera setup
  val initialPosition =
      CameraPositionState(
          position =
              CameraPosition.fromLatLngZoom(
                  uiState.target, calculateZoomLevel(uiState.request.size)))
  val cameraPositionState = remember { initialPosition }
  val zoomLevel by remember { derivedStateOf { cameraPositionState.position.zoom } }

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
    // Google Map
    MapWithMarkers(
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        clusters = clusters,
        zoomLevel = zoomLevel,
        viewModel = viewModel,
        coroutineScope = coroutineScope,
        currentLocation = uiState.currentLocation)

    // Bottom Sheet for current request
    CurrentRequestBottomSheet(
        uiState = uiState,
        viewModel = viewModel,
        navigationActions = navigationActions,
        appPalette = appPalette,
        modifier = Modifier.align(Alignment.BottomCenter))

    // List of requests overlay
    ListOfRequest(
        uiState,
        viewModel,
        appPalette,
        Modifier.align(Alignment.BottomCenter),
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

    // Zoom level test tag
    ZoomLevelTestTag(cameraPositionState)

    // Auto-zoom animation
    AutoZoomEffect(uiState, cameraPositionState, viewModel)

    // Filter UI
    MapFilter(
        searchFilterViewModel = searchFilterViewModel,
        selectedOwnership = uiState.requestOwnership,
        viewModel = viewModel,
        modifier = Modifier.align(Alignment.TopCenter))
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
    currentLocation: LatLng?
) {
  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(MapTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState,
      uiSettings = uiSettings) {

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
              BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
            } else {
              BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            }
          } else {
            // Cluster marker with number
            createMarkerWithNumber(cluster.size, isAtCurrentLocation) // ← MODIFIER CETTE LIGNE
          },
      onClick = {
        coroutineScope.launch {
          if (isACluster) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(position, ConstantMap.ZOOM_AFTER_CHOSEN),
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

  Marker(
      state = MarkerState(position = location),
      icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
      title = ConstantMap.YOUR_LOCATION,
      snippet = ConstantMap.YOU_ARE_HERE)
}

/**
 * Displays an animated bottom sheet with detailed information about the selected request. Contains
 * tabs for request details and profile information.
 *
 * @param uiState Current UI state containing the selected request
 * @param viewModel ViewModel for map operations
 * @param navigationActions Navigation actions for screen transitions
 * @param appPalette Color palette for styling
 * @param modifier Modifier for positioning the sheet
 */
@Composable
private fun CurrentRequestBottomSheet(
    uiState: MapUIState,
    viewModel: MapViewModel,
    navigationActions: NavigationActions?,
    appPalette: AppPalette,
    modifier: Modifier = Modifier
) {
  uiState.currentRequest?.let { req ->
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(ConstantMap.DETAILS, ConstantMap.PROFILE)

    AnimatedBottomSheet(viewModel = viewModel, appPalette = appPalette, modifier = modifier) {

      // Tab Row
      BottomSheetTabRow(
          tabs = tabs,
          selectedTab = selectedTab,
          onTabSelected = { selectedTab = it },
          appPalette = appPalette)

      HorizontalDivider(
          thickness = ConstantMap.DIVIDER_THICKNESS,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = ConstantMap.ALPHA_DIVIDER))

      // Tab Content
      BottomSheetContent(
          selectedTab = selectedTab,
          request = req,
          uiState = uiState,
          navigationActions = navigationActions,
          viewModel = viewModel,
          appPalette = appPalette)
    }
  }
}

/**
 * Displays a scrollable row of tabs for switching between different views in the bottom sheet.
 *
 * @param tabs List of tab titles (e.g., ["Details", "Profile"])
 * @param selectedTab Index of the currently selected tab
 * @param onTabSelected Callback when a tab is selected
 * @param appPalette Color palette for styling
 */
@Composable
private fun BottomSheetTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    appPalette: AppPalette
) {
  ScrollableTabRow(
      selectedTabIndex = selectedTab,
      modifier = Modifier.fillMaxWidth(),
      edgePadding = ConstantMap.TAB_ROW_EDGE_PADDING,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = appPalette.accent)
      },
      divider = {}) {
        tabs.forEachIndexed { index, title ->
          Tab(
              selected = selectedTab == index,
              onClick = { onTabSelected(index) },
              text = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color =
                        if (selectedTab == index) appPalette.accent
                        else
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = ConstantMap.ALPHA_TEXT_UNSELECTED))
              },
              modifier = Modifier.testTag(MapTestTags.testTagForTab(title)))
        }
      }
}

/**
 * Displays the content of the bottom sheet based on the selected tab.
 *
 * @param selectedTab The index of the currently selected tab (0 = Details, 1 = Profile)
 * @param request The request being displayed
 * @param uiState Current UI state containing ownership and profile info
 * @param navigationActions Actions for navigating between screens
 * @param viewModel ViewModel for map operations
 * @param appPalette Color palette for styling
 */
@Composable
private fun ColumnScope.BottomSheetContent(
    selectedTab: Int,
    request: Request,
    uiState: MapUIState,
    navigationActions: NavigationActions?,
    viewModel: MapViewModel,
    appPalette: AppPalette
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .weight(ConstantMap.WEIGHT_FILL)
              .verticalScroll(rememberScrollState())
              .padding(
                  horizontal = ConstantMap.PADDING_HORIZONTAL_STANDARD,
                  vertical = ConstantMap.PADDING_STANDARD)) {
        when (selectedTab) {
          0 -> { // Details
            RequestDetailsTab(
                request = request,
                uiState = uiState,
                navigationActions = navigationActions,
                viewModel = viewModel,
                appPalette = appPalette)
          }
          1 -> { // Profile
            CurrentProfileUI(
                uiState.isOwner,
                navigationActions,
                uiState.currentProfile,
                viewModel,
                this,
                request,
                appPalette)
          }
        }
      }
}

/**
 * Displays detailed information about a request including title, description, dates, status,
 * location, and action buttons.
 *
 * @param request The request to display
 * @param uiState Current UI state containing ownership info
 * @param navigationActions Navigation actions for screen transitions
 * @param viewModel ViewModel for map operations
 * @param appPalette Color palette for styling
 */
@Composable
private fun RequestDetailsTab(
    request: Request,
    uiState: MapUIState,
    navigationActions: NavigationActions?,
    viewModel: MapViewModel,
    appPalette: AppPalette
) {
  // Title
  Surface(
      color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_PRIMARY_SURFACE),
      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_SMALL),
      modifier = Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)) {
        Text(
            text = request.title,
            style = MaterialTheme.typography.titleMedium,
            color = appPalette.accent,
            modifier =
                Modifier.padding(
                        horizontal = ConstantMap.PADDING_STANDARD,
                        vertical = ConstantMap.SPACER_HEIGHT_SMALL)
                    .testTag(MapTestTags.REQUEST_TITLE))
      }

  // Description
  Text(
      text = request.description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
          Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)
              .testTag(MapTestTags.REQUEST_DESCRIPTION))

  // Dates Row
  RequestDatesRow(request)

  Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_LARGE))

  // Status
  RequestStatusChip(request)

  Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

  // Location
  RequestLocationChip(request, appPalette)

  Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

  // Action Buttons
  ButtonDetails(uiState.isOwner, navigationActions, request, viewModel, appPalette)
}

/**
 * Displays start and end dates of a request side-by-side in styled containers.
 *
 * @param request The request whose dates to display
 */
@Composable
private fun RequestDatesRow(request: Request) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ConstantMap.SPACER_HEIGHT_MEDIUM)) {
        // Start Date
        Surface(
            modifier = Modifier.weight(ConstantMap.WEIGHT_FILL),
            shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
            color = MaterialTheme.colorScheme.secondaryContainer) {
              Column(modifier = Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                Text(
                    text = ConstantMap.START_DATE,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = ConstantMap.ALPHA_ON_CONTAINER_MEDIUM))
                Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_SMALL))
                Text(
                    text = request.startTimeStamp.toDisplayString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag(MapTestTags.START_DATE))
              }
            }

        // End Date
        Surface(
            modifier = Modifier.weight(ConstantMap.WEIGHT_FILL),
            shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
            color = MaterialTheme.colorScheme.secondaryContainer) {
              Column(modifier = Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                Text(
                    text = ConstantMap.END_DATE,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = ConstantMap.ALPHA_ON_CONTAINER_MEDIUM))
                Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_SMALL))
                Text(
                    text = request.expirationTime.toDisplayString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag(MapTestTags.END_DATE))
              }
            }
      }
}

/**
 * Displays the status of a request (e.g., IN_PROGRESS, COMPLETED) as a styled chip.
 *
 * @param request The request whose status to display
 */
@Composable
private fun RequestStatusChip(request: Request) {
  Surface(
      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
      color = MaterialTheme.colorScheme.tertiaryContainer,
      modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) {
        Text(
            text = request.status.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier =
                Modifier.padding(
                        horizontal = ConstantMap.SPACER_HEIGHT_LARGE,
                        vertical = ConstantMap.SPACER_HEIGHT_MID)
                    .testTag(MapTestTags.REQUEST_STATUS))
      }
}

/**
 * Displays the location name of a request with a location icon in a styled chip.
 *
 * @param request The request whose location to display
 * @param appPalette Color palette for styling the icon
 */
@Composable
private fun RequestLocationChip(request: Request, appPalette: AppPalette) {
  Surface(
      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    horizontal = ConstantMap.PADDING_HORIZONTAL_STANDARD,
                    vertical = ConstantMap.PADDING_STANDARD)) {
              Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = ConstantMap.LOCATION,
                  tint = appPalette.primary,
                  modifier = Modifier.size(ConstantMap.ICON_SIZE_LOCATION))
              Spacer(modifier = Modifier.width(ConstantMap.SPACER_WIDTH_SMALL))
              Text(
                  text = request.locationName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag(MapTestTags.REQUEST_LOCATION_NAME))
            }
      }
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
  LaunchedEffect(uiState.needToZoom) {
    if (uiState.needToZoom) {
      cameraPositionState.animate(
          update =
              CameraUpdateFactory.newLatLngZoom(
                  uiState.target, calculateZoomLevel(uiState.request.size)),
          durationMs = ConstantMap.VERY_LONG_DURATION_ANIMATION)
      viewModel.zoomCompleted()
    }
  }
}

/**
 * The button where you can go to EditScreen/AcceptScreen
 *
 * @param isOwner is the current user the owner of the request, can be null if a problem has
 *   occurred
 * @param navigationActions the actions of navigation
 * @param request the current request
 * @param mapViewModel the viewModel
 */
@Composable
fun ButtonDetails(
    isOwner: Boolean?,
    navigationActions: NavigationActions?,
    request: Request,
    mapViewModel: MapViewModel,
    appPalette: AppPalette
) {
  Button(
      onClick = {
        // Always navigate to the view-only details (Accept) page; edit is accessible from there
        when (isOwner) {
          true,
          false -> {
            navigationActions?.navigateTo(Screen.RequestAccept(request.requestId))
            mapViewModel.goOnAnotherScreen()
          }
          else -> mapViewModel.isHisRequest(request)
        }
      },
      colors =
          ButtonDefaults.buttonColors(
              containerColor = appPalette.accent, contentColor = appPalette.primary),
      modifier =
          Modifier.fillMaxWidth()
              .padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)
              .testTag(MapTestTags.BUTTON_DETAILS)) {
        Text(
            when (isOwner) {
              true -> ConstantMap.TEXT_SEE_DETAILS
              false -> ConstantMap.TEXT_SEE_DETAILS
              else -> ConstantMap.PROBLEM_OCCUR
            })
      }
}

@Composable
fun CurrentProfileUI(
    isOwner: Boolean?,
    navigationActions: NavigationActions?,
    profile: UserProfile?,
    mapViewModel: MapViewModel,
    columnScope: ColumnScope,
    request: Request,
    appPalette: AppPalette
) {
  with(columnScope) {
    if (profile != null) {
      Surface(
          color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_PRIMARY_SURFACE),
          shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_SMALL),
          modifier = Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.padding(ConstantMap.PADDING_HORIZONTAL_STANDARD).fillMaxWidth()) {
                  ProfilePicture(
                      profileRepository = mapViewModel.profileRepository,
                      profileId = profile.id,
                      onClick = {},
                      modifier = Modifier.size(ConstantMap.REQUEST_ITEM_ICON_SIZE))

                  Text(
                      text = "${profile.lastName} ${profile.name}",
                      style =
                          MaterialTheme.typography.titleMedium.copy(
                              fontSize = ConstantMap.FONT_SIZE_BIG),
                      color = appPalette.onPrimary,
                      modifier =
                          Modifier.padding(start = ConstantMap.PADDING_STANDARD)
                              .testTag(MapTestTags.PROFILE_NAME))
                }
          }
      // Kudos + Section
      Surface(
          shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
          color = MaterialTheme.colorScheme.tertiaryContainer,
          modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(ConstantMap.SPACER_HEIGHT_LARGE),
                horizontalArrangement = Arrangement.spacedBy(ConstantMap.SPACER_HEIGHT_MEDIUM)) {
                  Surface(
                      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
                      color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_KUDOS_DIVIDER),
                      modifier = Modifier.weight(ConstantMap.WEIGHT_FILL)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                              Text(
                                  text = ConstantMap.KUDOS,
                                  style =
                                      MaterialTheme.typography.titleMedium.copy(
                                          fontSize = ConstantMap.FONT_SIZE_BIG),
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  modifier = Modifier.testTag(MapTestTags.PROFILE_KUDOS_TEXT))

                              Text(
                                  text = profile.kudos.toString(),
                                  style =
                                      MaterialTheme.typography.bodyMedium.copy(
                                          fontSize = ConstantMap.FONT_SIZE_MID),
                                  color = appPalette.accent,
                                  modifier = Modifier.testTag(MapTestTags.PROFILE_KUDOS))
                            }
                      }

                  Surface(
                      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
                      color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_KUDOS_DIVIDER),
                      modifier = Modifier.weight(ConstantMap.WEIGHT_FILL)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                              Text(
                                  text = ConstantMap.SECTION,
                                  style =
                                      MaterialTheme.typography.titleMedium.copy(
                                          fontSize = ConstantMap.FONT_SIZE_BIG),
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  modifier = Modifier.testTag(MapTestTags.PROFILE_SECTION_TEXT))
                              Text(
                                  text = profile.section.label,
                                  style =
                                      MaterialTheme.typography.labelMedium.copy(
                                          fontSize = ConstantMap.FONT_SIZE_MID),
                                  color = appPalette.accent,
                                  modifier = Modifier.testTag(MapTestTags.PROFILE_SECTION))
                            }
                      }
                }
          }

      Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

      // Location name
      Surface(
          shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.padding(
                        horizontal = ConstantMap.PADDING_HORIZONTAL_STANDARD,
                        vertical = ConstantMap.PADDING_STANDARD)) {
                  Icon(
                      imageVector = Icons.Default.AccessTime,
                      contentDescription = ConstantMap.LOCATION,
                      tint = appPalette.primary,
                      modifier = Modifier.size(ConstantMap.ICON_SIZE_LOCATION))
                  Spacer(modifier = Modifier.width(ConstantMap.SPACER_WIDTH_SMALL))
                  Text(
                      text = profile.arrivalDate.toDisplayStringWithoutHours(),
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.testTag(MapTestTags.PROFILE_CREATION_DATE))
                }
          }
      Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))
    }
    ButtonProfileDetails(isOwner, navigationActions, profile, mapViewModel, request, appPalette)
  }
}

@Composable
fun ButtonProfileDetails(
    isOwner: Boolean?,
    navigationActions: NavigationActions?,
    profile: UserProfile?,
    mapViewModel: MapViewModel,
    request: Request,
    appPalette: AppPalette
) {

  if (isOwner == false && profile != null) return

  val buttonText =
      when {
        profile == null -> ConstantMap.PROBLEM_OCCUR
        isOwner == true -> ConstantMap.TEXT_EDIT_PROFILE
        else -> ConstantMap.PROBLEM_OCCUR
      }

  val onClickAction: () -> Unit = {
    when {
      profile == null -> mapViewModel.updateCurrentProfile(request.creatorId)
      isOwner == true -> {
        navigationActions?.navigateTo(Screen.Profile(profile.id))
        mapViewModel.goOnAnotherScreen()
      }
      else -> mapViewModel.isHisRequest(request)
    }
  }

  Button(
      onClick = onClickAction,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = appPalette.accent, contentColor = appPalette.primary),
      modifier =
          Modifier.fillMaxWidth()
              .padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)
              .testTag(MapTestTags.PROFILE_EDIT_BUTTON)) {
        Text(buttonText)
      }
}

/**
 * A bottom sheet component with drag-to-resize functionality and smooth animations.
 *
 * @param viewModel The MapViewModel to handle state updates
 * @param appPalette The color palette for theming
 * @param modifier Optional modifier for customization
 * @param content The content to display inside the bottom sheet
 */
@Composable
fun AnimatedBottomSheet(
    viewModel: MapViewModel,
    appPalette: AppPalette,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
  val minHeight = ConstantMap.MIN_HEIGHT

  val density = LocalDensity.current
  val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
  val minHeightPx = with(density) { minHeight.toPx() }
  val thirdOfScreen = screenHeight * ConstantMap.PROPORTION_FOR_INITIALIZE_SHEET
  var offsetY by remember { mutableFloatStateOf(thirdOfScreen) }

  val currentHeight by
      animateDpAsState(
          targetValue =
              with(density) {
                (screenHeight -
                        offsetY.coerceIn(ConstantMap.MIN_OFFSET_Y, screenHeight - minHeightPx))
                    .toDp()
              },
          animationSpec = tween(durationMillis = ConstantMap.DURATION_ANIMATION))

  Box(modifier = modifier.fillMaxWidth().testTag(MapTestTags.DRAG_DOWN_MENU)) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(currentHeight),
        shape =
            RoundedCornerShape(
                topStart = ConstantMap.BOTTOM_SHEET_SHAPE, topEnd = ConstantMap.BOTTOM_SHEET_SHAPE),
        color = appPalette.primary,
        shadowElevation = ConstantMap.BOTTOM_SHEET_ELEVATION) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Drag Handle + Button
            Box(modifier = Modifier.fillMaxWidth()) {
              // Drag Handle
              Box(
                  modifier =
                      Modifier.width(ConstantMap.DRAG_HANDLE_WIDTH)
                          .height(ConstantMap.DRAG_HANDLE_HEIGHT)
                          .background(
                              color =
                                  MaterialTheme.colorScheme.onSurface.copy(
                                      alpha = ConstantMap.DRAG_HANDLE_ALPHA),
                              shape = RoundedCornerShape(ConstantMap.DRAG_HANDLE_CORNER_RADIUS))
                          .align(Alignment.Center)
                          .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                  offsetY =
                                      (offsetY + dragAmount).coerceIn(
                                          ConstantMap.MIN_OFFSET_Y, screenHeight - minHeightPx)
                                })
                          }
                          .testTag(MapTestTags.DRAG))

              // Button X
              IconButton(
                  onClick = { viewModel.updateNoRequests() },
                  modifier = Modifier.align(Alignment.CenterEnd).testTag(MapTestTags.BUTTON_X)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = ConstantMap.CLOSE_BUTTON_DESCRIPTION,
                        tint =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = ConstantMap.CLOSE_BUTTON_ALPHA))
                  }
            }

            content()
          }
        }
  }
}

/**
 * Displays a list of requests in an animated bottom sheet.
 *
 * @param uiState The current UI state containing the list of requests
 * @param viewModel The MapViewModel to handle user interactions
 * @param appPalette The color palette for theming
 * @param modifier Optional modifier for customization
 * @param coroutineScope The coroutine scope for launching async operations
 * @param cameraPositionState The camera position state for map animations
 */
@Composable
fun ListOfRequest(
    uiState: MapUIState,
    viewModel: MapViewModel,
    appPalette: AppPalette,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    cameraPositionState: CameraPositionState,
    navigationActions: NavigationActions?
) {
  uiState.currentListRequest?.let { list ->
    AnimatedBottomSheet(viewModel, appPalette, modifier) {
      LazyColumn(
          modifier =
              modifier
                  .padding(ConstantRequestList.ListPadding)
                  .testTag(MapTestTags.MAP_LIST_REQUEST)) {
            items(list.size) { index ->
              val request = list[index]
              RequestListItem(
                  viewModel = viewModel(),
                  request = request,
                  onClick = {
                    coroutineScope.launch {
                      cameraPositionState.animate(
                          update =
                              CameraUpdateFactory.newLatLngZoom(
                                  LatLng(request.location.latitude, request.location.longitude),
                                  ConstantMap.ZOOM_AFTER_CHOSEN),
                          durationMs = ConstantMap.LONG_DURATION_ANIMATION)
                    }
                    viewModel.updateCurrentRequest(request)
                    viewModel.updateCurrentProfile(request.creatorId)
                  },
                  navigationActions = navigationActions,
                  state = com.android.sample.ui.request.RequestListState())
            }
          }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
  MapScreen()
}
