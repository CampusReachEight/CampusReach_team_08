package com.android.sample.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.request.Request
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.request.ConstantRequestList
import com.android.sample.ui.request.RequestListItem
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.TopNavigationBar
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
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
  const val MAP_LIST_REQUEST = "map_list_request"

  fun testTagForTab(tab: String): String {
    return "tag${tab}"
  }
}

private const val ZOOM_FEW = 15f
private const val ZOOM_SEVERAL = 13f
private const val ZOOM_MANY = 10f

fun calculateZoomLevel(markerCount: Int): Float {
  return when {
    markerCount < 2 -> ZOOM_FEW
    markerCount < 5 -> ZOOM_SEVERAL
    else -> ZOOM_MANY
  }
}

/**
 * Determines the appropriate clustering radius based on the map zoom level. The radius decreases as
 * the zoom level increases, allowing for more detailed clustering at higher zoom levels. The radius
 * is also dynamically adjusted based on the exact zoom value.
 *
 * @param zoomLevel The current zoom level of the map (typically between 0 and 21)
 * @return The clustering radius in meters, adjusted for the current zoom level
 */
fun getClusterRadiusForZoom(zoomLevel: Float): Double {
  return when {
    zoomLevel < ConstantMap.MAX_ZOOM_ONE -> ConstantMap.ZOOM_LEVEL_WORLD // 300km - mondial view
    zoomLevel < ConstantMap.MAX_ZOOM_TWO -> ConstantMap.ZOOM_LEVEL_WL // 70km
    zoomLevel < ConstantMap.MAX_ZOOM_THREE -> ConstantMap.ZOOM_LEVEL_LAND // 40km - land view
    zoomLevel < ConstantMap.MAX_ZOOM_FOUR -> ConstantMap.ZOOM_LEVEL_REGION // 10km - region view
    zoomLevel < ConstantMap.MAX_ZOOM_FIVE -> ConstantMap.ZOOM_LEVEL_CITY // 3km - city view
    zoomLevel < ConstantMap.MAX_ZOOM_SIX -> ConstantMap.ZOOM_LEVEL_MID // 400m - view mid
    zoomLevel < ConstantMap.MAX_ZOOM_SEVEN -> ConstantMap.ZOOM_LEVEL_STREET_BIG // 50m - view street
    else -> ConstantMap.ZOOM_LEVEL_STREET_SMALL // 20m - vue street
  } / (zoomLevel / ConstantMap.ZOOM_DIVIDE) // change with current zoom
}

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(), navigationActions: NavigationActions? = null) {
  val uiState by viewModel.uiState.collectAsState()
  val appPalette = appPalette()

  val errorMsg = uiState.errorMsg

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      viewModel.clearErrorMsg()
    }
  }
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
            onProfileClick = { navigationActions?.navigateTo(Screen.Profile("TODO")) },
        )
      },
      content = { pd ->

        // initial position
        val initialPosition =
            CameraPositionState(
                position =
                    CameraPosition.fromLatLngZoom(
                        uiState.target, calculateZoomLevel(uiState.request.size)))
        val cameraPositionState = remember { initialPosition }

        val zoomLevel by remember { derivedStateOf { cameraPositionState.position.zoom } }

        val clusters =
            remember(uiState.request, zoomLevel) {
              clusterRequestsByDistance(
                  requests = uiState.request,
                  clusterRadiusMeters = getClusterRadiusForZoom(zoomLevel))
            }

        // Configure UI settings to disable built-in zoom controls
        val uiSettings = remember {
          MapUiSettings(
              zoomControlsEnabled = false,
          )
        }

        Box(modifier = Modifier.fillMaxSize().padding(pd)) {

          // Carte
          GoogleMap(
              modifier = Modifier.fillMaxSize().testTag(MapTestTags.GOOGLE_MAP_SCREEN),
              cameraPositionState = cameraPositionState,
              uiSettings = uiSettings) {
                clusters.forEach { cluster ->
                  val position =
                      if (cluster.size == ConstantMap.ONE) {
                        LatLng(
                            cluster.first().location.latitude, cluster.first().location.longitude)
                      } else {
                        calculateClusterCenter(cluster)
                      }
                  Marker(
                      state = MarkerState(position = position),
                      icon =
                          if (cluster.size == ConstantMap.ONE) {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                          } else {
                            createMarkerWithNumber(cluster.size)
                          },
                      onClick = {
                        coroutineScope.launch {
                          if (cluster.size == ConstantMap.ONE) {
                            // Zoom on one individual marker
                            cameraPositionState.animate(
                                update =
                                    CameraUpdateFactory.newLatLngZoom(
                                        position, ConstantMap.ZOOM_AFTER_CHOSEN),
                                durationMs = ConstantMap.LONG_DURATION_ANIMATION)
                            viewModel.updateCurrentRequest(cluster.first())
                            viewModel.updateCurrentProfile(cluster.first().creatorId)
                          } else {
                            // Zoom on cluster
                            cameraPositionState.animate(
                                update =
                                    CameraUpdateFactory.newLatLngZoom(
                                        position,
                                        zoomLevel + ConstantMap.ZOOM_LEVEL_TWO // Zoom 2 level
                                        ),
                                durationMs = ConstantMap.LONG_DURATION_ANIMATION)
                            viewModel.updateCurrentListRequest(cluster)
                          }
                        }

                        true
                      })
                }
              }

          // Bottom Sheet Draggable
          uiState.currentRequest?.let { req ->
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf(ConstantMap.DETAILS, ConstantMap.PROFILE)

            AnimatedBottomSheet(
                viewModel = viewModel,
                appPalette = appPalette,
                modifier = Modifier.align(Alignment.BottomCenter)) {

                  // Tabs
                  ScrollableTabRow(
                      selectedTabIndex = selectedTab,
                      modifier = Modifier.fillMaxWidth(),
                      edgePadding = ConstantMap.TAB_ROW_EDGE_PADDING,
                      indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = appPalette.accent)
                      },
                      divider = {}) {
                        tabs.forEachIndexed { index, title ->
                          Tab(
                              selected = selectedTab == index,
                              onClick = { selectedTab = index },
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

                  HorizontalDivider(
                      thickness = ConstantMap.DIVIDER_THICKNESS,
                      color =
                          MaterialTheme.colorScheme.onSurface.copy(
                              alpha = ConstantMap.ALPHA_DIVIDER))

                  // show text in function of tab selected
                  Column(
                      modifier =
                          Modifier.fillMaxWidth()
                              .weight(ConstantMap.WEIGHT_FILL)
                              .verticalScroll(rememberScrollState())
                              .padding(
                                  horizontal = ConstantMap.PADDING_HORIZONTAL_STANDARD,
                                  vertical = ConstantMap.PADDING_STANDARD)) {
                        when (selectedTab) {
                          0 -> { // Information
                            Surface(
                                color =
                                    appPalette.accent.copy(
                                        alpha = ConstantMap.ALPHA_PRIMARY_SURFACE),
                                shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_SMALL),
                                modifier =
                                    Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)) {
                                  Text(
                                      text = req.title,
                                      style = MaterialTheme.typography.titleMedium,
                                      color = appPalette.accent,
                                      modifier =
                                          Modifier.padding(
                                                  horizontal = ConstantMap.PADDING_STANDARD,
                                                  vertical = ConstantMap.SPACER_HEIGHT_SMALL)
                                              .testTag(MapTestTags.REQUEST_TITLE))
                                }

                            Text(
                                text = req.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)
                                        .testTag(MapTestTags.REQUEST_DESCRIPTION))

                            // Dates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(ConstantMap.SPACER_HEIGHT_MEDIUM)) {
                                  // Start Date
                                  Surface(
                                      modifier = Modifier.weight(ConstantMap.WEIGHT_FILL),
                                      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
                                      color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Column(
                                            modifier =
                                                Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                                              Text(
                                                  text = ConstantMap.START_DATE,
                                                  style = MaterialTheme.typography.labelSmall,
                                                  color =
                                                      MaterialTheme.colorScheme.onPrimaryContainer
                                                          .copy(
                                                              alpha =
                                                                  ConstantMap
                                                                      .ALPHA_ON_CONTAINER_MEDIUM))
                                              Spacer(
                                                  modifier =
                                                      Modifier.height(
                                                          ConstantMap.SPACER_HEIGHT_SMALL))
                                              Text(
                                                  text = req.startTimeStamp.toDisplayString(),
                                                  style = MaterialTheme.typography.bodyMedium,
                                                  fontWeight = FontWeight.SemiBold,
                                                  color =
                                                      MaterialTheme.colorScheme.onPrimaryContainer,
                                                  modifier =
                                                      Modifier.testTag(MapTestTags.START_DATE))
                                            }
                                      }

                                  // End Date
                                  Surface(
                                      modifier = Modifier.weight(ConstantMap.WEIGHT_FILL),
                                      shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
                                      color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Column(
                                            modifier =
                                                Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                                              Text(
                                                  text = ConstantMap.END_DATE,
                                                  style = MaterialTheme.typography.labelSmall,
                                                  color =
                                                      MaterialTheme.colorScheme.onSecondaryContainer
                                                          .copy(
                                                              alpha =
                                                                  ConstantMap
                                                                      .ALPHA_ON_CONTAINER_MEDIUM))
                                              Spacer(
                                                  modifier =
                                                      Modifier.height(
                                                          ConstantMap.SPACER_HEIGHT_SMALL))
                                              Text(
                                                  text = req.expirationTime.toDisplayString(),
                                                  style = MaterialTheme.typography.bodyMedium,
                                                  fontWeight = FontWeight.SemiBold,
                                                  color =
                                                      MaterialTheme.colorScheme
                                                          .onSecondaryContainer,
                                                  modifier = Modifier.testTag(MapTestTags.END_DATE))
                                            }
                                      }
                                }
                            Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_LARGE))

                            // Status
                            Surface(
                                shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                  Text(
                                      text = req.status.name,
                                      style = MaterialTheme.typography.labelMedium,
                                      fontWeight = FontWeight.Medium,
                                      color = MaterialTheme.colorScheme.onTertiaryContainer,
                                      modifier =
                                          Modifier.padding(
                                                  horizontal = ConstantMap.SPACER_HEIGHT_LARGE,
                                                  vertical = ConstantMap.SPACER_HEIGHT_MID)
                                              .testTag(MapTestTags.REQUEST_STATUS))
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
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = ConstantMap.LOCATION,
                                            tint = appPalette.primary,
                                            modifier =
                                                Modifier.size(ConstantMap.ICON_SIZE_LOCATION))
                                        Spacer(
                                            modifier =
                                                Modifier.width(ConstantMap.SPACER_WIDTH_SMALL))
                                        Text(
                                            text = req.locationName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier =
                                                Modifier.testTag(MapTestTags.REQUEST_LOCATION_NAME))
                                      }
                                }
                            Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

                            ButtonDetails(
                                uiState.isOwner, navigationActions, req, viewModel, appPalette)
                          }
                          1 -> { // Profile
                            CurrentProfileUI(
                                uiState.isOwner,
                                navigationActions,
                                uiState.currentProfile,
                                viewModel,
                                this,
                                req,
                                appPalette)
                          }
                        // if you want to add more tab, just put last number + 1 ->...
                        }
                      }
                }
          }

          ListOfRequest(
              uiState,
              viewModel,
              appPalette,
              Modifier.align(Alignment.BottomCenter),
              coroutineScope,
              cameraPositionState)

          // Zoom controls
          if (uiState.currentRequest == null && uiState.currentListRequest == null) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(UiDimens.SpacingMd),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  FloatingActionButton(
                      onClick = {
                        coroutineScope.launch {
                          cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                        }
                      },
                      modifier =
                          Modifier.testTag(MapTestTags.ZOOM_IN_BUTTON)
                              .size(UiDimens.SpacingXxl)
                              .padding(UiDimens.SpacingXs),
                      containerColor = appPalette.accent,
                      contentColor = appPalette.surface) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = appPalette.surface)
                      }

                  FloatingActionButton(
                      onClick = {
                        coroutineScope.launch {
                          cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                        }
                      },
                      modifier =
                          Modifier.testTag(MapTestTags.ZOOM_OUT_BUTTON)
                              .size(UiDimens.SpacingXxl)
                              .padding(UiDimens.SpacingXs),
                      containerColor = appPalette.accent,
                      contentColor = appPalette.surface) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
                      }
                }
          }

          // Invisible box to expose zoom level for testing
          Box(
              modifier =
                  Modifier.size(0.dp).semantics {
                    testTag = "${MapTestTags.ZOOM_LEVEL}:${cameraPositionState.position.zoom}"
                  })

          // if uiState.target change, do an animation to go to the location the new target
          LaunchedEffect(uiState.target) {
            cameraPositionState.animate(
                update =
                    CameraUpdateFactory.newLatLngZoom(
                        uiState.target, calculateZoomLevel(uiState.request.size)),
                durationMs = 1000)
          }
        }
      })
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
        when (isOwner) {
          true -> {
            navigationActions?.navigateTo(Screen.EditRequest(request.requestId))
          }
          false -> {
            navigationActions?.navigateTo(Screen.RequestAccept(request.requestId))
          }
          else -> {
            mapViewModel.isHisRequest(request)
          }
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
              true -> ConstantMap.TEXT_EDIT
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
      isOwner == true -> navigationActions?.navigateTo(Screen.Profile(profile.id))
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

fun Date.toDisplayStringWithoutHours(): String {
  return this.let { timestamp ->
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
  }
}

/**
 * Creates a marker icon with a number badge.
 *
 * @param count The number to display on the marker badge
 * @return A BitmapDescriptor that can be used as a marker icon
 */
fun createMarkerWithNumber(count: Int): BitmapDescriptor {
  val bitmap = createMarkerBitmap(count)
  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Creates a bitmap for a marker with a number badge.
 *
 * @param count The number to display on the badge
 * @return A Bitmap containing a blue circle with white border and the count number
 */
fun createMarkerBitmap(count: Int): Bitmap {
  val size = 100
  val bitmap = createBitmap(size, size)
  val canvas = Canvas(bitmap)

  // Circle blue
  val circlePaint =
      Paint().apply {
        color = "#4285F4".toColorInt() // Here I use directly color because I create a marker
        isAntiAlias = true
        style = Paint.Style.FILL
      }
  canvas.drawCircle(
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - ConstantMap.FIVE,
      circlePaint)

  // blank border
  val borderPaint =
      Paint().apply {
        color = Color.WHITE // Use directly Color because it's for a marker
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = ConstantMap.THREE_FLOAT
      }
  canvas.drawCircle(
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - ConstantMap.FIVE,
      borderPaint)

  // Number of request in cluster
  val textPaint = createTextPaint(count)

  val text = count.toString()
  val textBounds = Rect()
  textPaint.getTextBounds(text, ConstantMap.ZERO, text.length, textBounds)

  canvas.drawText(
      text,
      size / ConstantMap.TWO_FLOAT,
      size / ConstantMap.TWO_FLOAT - textBounds.exactCenterY(),
      textPaint)

  return bitmap
}

/**
 * Creates a Paint object for drawing text on marker badges.
 *
 * @param count The number to be displayed, used to determine appropriate text size
 * @return A Paint object configured for drawing white, bold, centered text
 */
fun createTextPaint(count: Int): Paint {
  return Paint().apply {
    color = Color.WHITE
    textSize = getTextSizeForCount(count)
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    isFakeBoldText = true
  }
}

/**
 * Determines the appropriate text size based on the number of digits in the count.
 *
 * @param count The number to be displayed
 * @return The text size in pixels: larger for single digits, smaller for longer numbers
 */
fun getTextSizeForCount(count: Int): Float {
  return when {
    count < ConstantMap.NUMBER_LENGTH_ONE -> ConstantMap.NUMBER_SIZE_ONE
    count < ConstantMap.NUMBER_LENGTH_TWO -> ConstantMap.NUMBER_SIZE_TWO
    else -> ConstantMap.NUMBER_SIZE_THREE
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
    cameraPositionState: CameraPositionState
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
                  })
            }
          }
    }
  }
}

/**
 * Calculates the distance between two geographic coordinates using the Haversine formula.
 *
 * @param lat1 Latitude of the first point in degrees
 * @param lon1 Longitude of the first point in degrees
 * @param lat2 Latitude of the second point in degrees
 * @param lon2 Longitude of the second point in degrees
 * @return The distance between the two points in meters
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val earthRadius = ConstantMap.EARTH_RADIUS // Radius of earth in meter

  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)

  val a =
      sin(dLat / ConstantMap.TWO) * sin(dLat / ConstantMap.TWO) +
          cos(Math.toRadians(lat1)) *
              cos(Math.toRadians(lat2)) *
              sin(dLon / ConstantMap.TWO) *
              sin(dLon / ConstantMap.TWO)

  val c = ConstantMap.TWO * atan2(sqrt(a), sqrt(ConstantMap.ONE - a))

  return earthRadius * c
}

/**
 * Calculates the geographic center point of a cluster of requests.
 *
 * @param requests The list of requests in the cluster
 * @return A LatLng representing the average position of all requests in the cluster
 */
fun calculateClusterCenter(requests: List<Request>): LatLng {
  val avgLat = requests.map { it.location.latitude }.average()
  val avgLng = requests.map { it.location.longitude }.average()
  return LatLng(avgLat, avgLng)
}

/**
 * Groups requests into clusters based on geographic distance. Uses a density-based algorithm where
 * points with more neighbors are processed first, and each point is only included in one cluster.
 *
 * @param requests The list of requests to cluster
 * @param clusterRadiusMeters The maximum distance in meters for requests to be in the same cluster
 * @return A list of clusters, where each cluster is a list of nearby requests
 */
fun clusterRequestsByDistance(
    requests: List<Request>,
    clusterRadiusMeters: Double
): List<List<Request>> {
  if (requests.isEmpty()) return emptyList()

  val adjustedRadius = clusterRadiusMeters

  // Calculate the density of each point
  val densityMap =
      requests.associateWith { request ->
        requests.count { other ->
          calculateDistance(
              request.location.latitude,
              request.location.longitude,
              other.location.latitude,
              other.location.longitude) <= adjustedRadius
        }
      }

  // Sort by decreasing density (points with the most neighbours first)
  val sortedRequests = requests.sortedByDescending { densityMap[it] }

  val clusters = mutableListOf<MutableList<Request>>()
  val processed = mutableSetOf<Request>()

  sortedRequests.forEach { request ->
    if (request in processed) return@forEach

    val cluster = mutableListOf(request)
    processed.add(request)

    // Find all nearby requests and sort them by distance
    val nearbyRequests =
        requests
            .filter { other -> other !in processed }
            .map { other ->
              val distance =
                  calculateDistance(
                      request.location.latitude,
                      request.location.longitude,
                      other.location.latitude,
                      other.location.longitude)
              other to distance
            }
            .filter { (_, distance) -> distance <= adjustedRadius }
            .sortedBy { (_, distance) -> distance }

    nearbyRequests.forEach { (other, _) ->
      cluster.add(other)
      processed.add(other)
    }

    clusters.add(cluster)
  }

  return clusters
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
  MapScreen()
}
