package com.android.sample.ui.map

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.ui.navigation.BottomNavigationMenu
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTab
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.ui.theme.TopNavigationBar
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
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
  const val PROFILE_TEXT = "profileText"

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(), navigationActions: NavigationActions? = null) {
  val uiState by viewModel.uiState.collectAsState()

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
                uiState.request.forEach { request ->
                  val markerState =
                      MarkerState(
                          position = LatLng(request.location.latitude, request.location.longitude))
                  Marker(
                      state = markerState,
                      onClick = {
                        viewModel.updateCurrentRequest(request)
                        true
                      })
                }
              }

          // Bottom Sheet Draggable
          uiState.currentRequest?.let { req ->
            var selectedTab by remember { mutableIntStateOf(0) }
            val minHeight = ConstantMap.MIN_HEIGHT

            val density = LocalDensity.current
            val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
            val minHeightPx = with(density) { minHeight.toPx() }
            val midScreen = screenHeight / 2
            var offsetY by remember { mutableFloatStateOf(midScreen) }

            val currentHeight by
                animateDpAsState(
                    targetValue =
                        with(density) {
                          (screenHeight -
                                  offsetY.coerceIn(
                                      ConstantMap.MIN_OFFSET_Y, screenHeight - minHeightPx))
                              .toDp()
                        },
                    animationSpec = tween(durationMillis = ConstantMap.DURATION_ANIMATION))

            // list of all tabs
            val tabs = listOf(ConstantMap.DETAILS, ConstantMap.PROFILE)

            Box(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .testTag(MapTestTags.DRAG_DOWN_MENU)) {
                  Surface(
                      modifier = Modifier.fillMaxWidth().height(currentHeight),
                      shape =
                          RoundedCornerShape(
                              topStart = ConstantMap.BOTTOM_SHEET_SHAPE,
                              topEnd = ConstantMap.BOTTOM_SHEET_SHAPE),
                      color = MaterialTheme.colorScheme.surface,
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
                                            shape =
                                                RoundedCornerShape(
                                                    ConstantMap.DRAG_HANDLE_CORNER_RADIUS))
                                        .align(Alignment.Center)
                                        .pointerInput(Unit) {
                                          detectVerticalDragGestures(
                                              onVerticalDrag = { _, dragAmount ->
                                                offsetY =
                                                    (offsetY + dragAmount).coerceIn(
                                                        ConstantMap.MIN_OFFSET_Y,
                                                        screenHeight - minHeightPx)
                                              })
                                        }
                                        .testTag(MapTestTags.DRAG))

                            // Button X
                            IconButton(
                                onClick = { viewModel.updateCurrentRequest(null) },
                                modifier =
                                    Modifier.align(Alignment.CenterEnd)
                                        .testTag(MapTestTags.BUTTON_X)) {
                                  Icon(
                                      imageVector = Icons.Default.Close,
                                      contentDescription = ConstantMap.CLOSE_BUTTON_DESCRIPTION,
                                      tint =
                                          MaterialTheme.colorScheme.onSurface.copy(
                                              alpha = ConstantMap.CLOSE_BUTTON_ALPHA))
                                }
                          }

                          // Tabs
                          ScrollableTabRow(
                              selectedTabIndex = selectedTab,
                              modifier = Modifier.fillMaxWidth(),
                              edgePadding = ConstantMap.TAB_ROW_EDGE_PADDING,
                              indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary)
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
                                                if (selectedTab == index)
                                                    MaterialTheme.colorScheme.primary
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
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = ConstantMap.ALPHA_PRIMARY_SURFACE),
                                        shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_SMALL),
                                        modifier =
                                            Modifier.padding(
                                                bottom = ConstantMap.SPACER_HEIGHT_LARGE)) {
                                          Text(
                                              text = req.title,
                                              style = MaterialTheme.typography.titleMedium,
                                              color = MaterialTheme.colorScheme.primary,
                                              modifier =
                                                  Modifier.padding(
                                                          horizontal = ConstantMap.PADDING_STANDARD,
                                                          vertical =
                                                              ConstantMap.SPACER_HEIGHT_SMALL)
                                                      .testTag(MapTestTags.REQUEST_TITLE))
                                        }

                                    Text(
                                        text = req.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier =
                                            Modifier.padding(
                                                    bottom = ConstantMap.SPACER_HEIGHT_LARGE)
                                                .testTag(MapTestTags.REQUEST_DESCRIPTION))

                                    // Dates
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                ConstantMap.SPACER_HEIGHT_MEDIUM)) {
                                          // Start Date
                                          Surface(
                                              modifier = Modifier.weight(ConstantMap.WEIGHT_FILL),
                                              shape =
                                                  RoundedCornerShape(
                                                      ConstantMap.CORNER_RADIUS_MEDIUM),
                                              color = MaterialTheme.colorScheme.primaryContainer) {
                                                Column(
                                                    modifier =
                                                        Modifier.padding(
                                                            ConstantMap.PADDING_STANDARD)) {
                                                      Text(
                                                          text = ConstantMap.START_DATE,
                                                          style =
                                                              MaterialTheme.typography.labelSmall,
                                                          color =
                                                              MaterialTheme.colorScheme
                                                                  .onPrimaryContainer
                                                                  .copy(
                                                                      alpha =
                                                                          ConstantMap
                                                                              .ALPHA_ON_CONTAINER_MEDIUM))
                                                      Spacer(
                                                          modifier =
                                                              Modifier.height(
                                                                  ConstantMap.SPACER_HEIGHT_SMALL))
                                                      Text(
                                                          text =
                                                              req.startTimeStamp.toDisplayString(),
                                                          style =
                                                              MaterialTheme.typography.bodyMedium,
                                                          fontWeight = FontWeight.SemiBold,
                                                          color =
                                                              MaterialTheme.colorScheme
                                                                  .onPrimaryContainer,
                                                          modifier =
                                                              Modifier.testTag(
                                                                  MapTestTags.START_DATE))
                                                    }
                                              }

                                          // End Date
                                          Surface(
                                              modifier = Modifier.weight(ConstantMap.WEIGHT_FILL),
                                              shape =
                                                  RoundedCornerShape(
                                                      ConstantMap.CORNER_RADIUS_MEDIUM),
                                              color =
                                                  MaterialTheme.colorScheme.secondaryContainer) {
                                                Column(
                                                    modifier =
                                                        Modifier.padding(
                                                            ConstantMap.PADDING_STANDARD)) {
                                                      Text(
                                                          text = ConstantMap.END_DATE,
                                                          style =
                                                              MaterialTheme.typography.labelSmall,
                                                          color =
                                                              MaterialTheme.colorScheme
                                                                  .onSecondaryContainer
                                                                  .copy(
                                                                      alpha =
                                                                          ConstantMap
                                                                              .ALPHA_ON_CONTAINER_MEDIUM))
                                                      Spacer(
                                                          modifier =
                                                              Modifier.height(
                                                                  ConstantMap.SPACER_HEIGHT_SMALL))
                                                      Text(
                                                          text =
                                                              req.expirationTime.toDisplayString(),
                                                          style =
                                                              MaterialTheme.typography.bodyMedium,
                                                          fontWeight = FontWeight.SemiBold,
                                                          color =
                                                              MaterialTheme.colorScheme
                                                                  .onSecondaryContainer,
                                                          modifier =
                                                              Modifier.testTag(
                                                                  MapTestTags.END_DATE))
                                                    }
                                              }
                                        }
                                    Spacer(
                                        modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_LARGE))

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
                                                          horizontal =
                                                              ConstantMap.SPACER_HEIGHT_LARGE,
                                                          vertical = ConstantMap.SPACER_HEIGHT_MID)
                                                      .testTag(MapTestTags.REQUEST_STATUS))
                                        }

                                    Spacer(
                                        modifier =
                                            Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

                                    // Location name
                                    Surface(
                                        shape =
                                            RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                          Row(
                                              verticalAlignment = Alignment.CenterVertically,
                                              modifier =
                                                  Modifier.padding(
                                                      horizontal =
                                                          ConstantMap.PADDING_HORIZONTAL_STANDARD,
                                                      vertical = ConstantMap.PADDING_STANDARD)) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = ConstantMap.LOCATION,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier =
                                                        Modifier.size(
                                                            ConstantMap.ICON_SIZE_LOCATION))
                                                Spacer(
                                                    modifier =
                                                        Modifier.width(
                                                            ConstantMap.SPACER_WIDTH_SMALL))
                                                Text(
                                                    text = req.locationName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier =
                                                        Modifier.testTag(
                                                            MapTestTags.REQUEST_LOCATION_NAME))
                                              }
                                        }
                                    Spacer(
                                        modifier =
                                            Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

                                    ButtonDetails(
                                        uiState.isOwner, navigationActions, req, viewModel)
                                  }
                                  1 -> { // Profile
                                    Text(
                                        text = "Profile information would go here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag(MapTestTags.PROFILE_TEXT))
                                  }
                                // if you want to add more tab, just put last number + 1 ->...
                                }
                              }
                        }
                      }
                }
          }

          // Zoom controls
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
                    containerColor = appPalette().accent,
                    contentColor = appPalette().surface) {
                      Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = "Zoom In",
                          tint = appPalette().surface)
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
                    containerColor = appPalette().accent,
                    contentColor = appPalette().surface) {
                      Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
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
    mapViewModel: MapViewModel
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
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary),
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

@Preview(showBackground = true)
@Composable
fun MapPreview() {
  MapScreen()
}
