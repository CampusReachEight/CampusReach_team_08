package com.android.sample.ui.map

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.theme.BottomNavigationMenu
import com.android.sample.ui.theme.NavigationTab
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

  fun getMarkerTag(requestId: String) = "marker_$requestId"
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
          GoogleMap(
              modifier = Modifier.fillMaxSize().testTag(MapTestTags.GOOGLE_MAP_SCREEN),
              cameraPositionState = cameraPositionState,
              uiSettings = uiSettings) {
                uiState.request.forEach { request ->
                  Marker(
                      state =
                          MarkerState(
                              position =
                                  LatLng(request.location.latitude, request.location.longitude)),
                      title = request.title,
                      snippet = request.description,
                      tag = MapTestTags.getMarkerTag(request.requestId))
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
                    modifier = Modifier.testTag(MapTestTags.ZOOM_IN_BUTTON)
                        .size(UiDimens.SpacingXxl)
                        .padding(UiDimens.SpacingXs),
                    containerColor = appPalette().accent,
                    contentColor = appPalette().surface
                ) {
                      Icon(imageVector = Icons.Default.Add,
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
                    contentColor = appPalette().surface
                ) {
                      Icon(imageVector = Icons.Default.Remove,
                          contentDescription = "Zoom Out")
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

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    MapScreen()
}
