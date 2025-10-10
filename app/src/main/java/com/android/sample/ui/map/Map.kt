package com.android.sample.ui.map

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.BottomNavigationMenu
import com.android.sample.ui.theme.NavigationTab
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

object MapTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
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
        // Add the topBar
      },
      content = { pd ->

        // initial position
        val initialPosition =
            CameraPositionState(
                position =
                    CameraPosition.fromLatLngZoom(
                        uiState.target, calculateZoomLevel(uiState.request.size)))
        val cameraPositionState = remember { initialPosition }

        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(pd).testTag(MapTestTags.GOOGLE_MAP_SCREEN),
            cameraPositionState = cameraPositionState) {
              uiState.request.forEach { request ->
                Marker(
                    state =
                        MarkerState(
                            position =
                                LatLng(request.location.latitude, request.location.longitude)),
                    title = request.title,
                    snippet = request.description)
              }
            }
        // if uiState.target change, do an animation to go to the location the new target
        LaunchedEffect(uiState.target) {
          cameraPositionState.animate(
              update =
                  CameraUpdateFactory.newLatLngZoom(
                      uiState.target, calculateZoomLevel(uiState.request.size)),
              durationMs = 1000)
        }
      })
}
