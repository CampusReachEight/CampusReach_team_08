package com.android.sample.ui.map

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapTestTags {
    const val GOOGLE_MAP_SCREEN = "mapScreen"
}


@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val errorMsg = uiState.errorMsg

    val context = LocalContext.current

    LaunchedEffect(errorMsg) {
        if (errorMsg != null) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMsg()
        }
    }

    Scaffold(
        bottomBar = {
            //Add the bottomBar
        },
        topBar = {
            //Add the topBar
        },
        content = { pd ->
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(uiState.target, 15f)
            }
            GoogleMap(
                modifier =
                    Modifier.fillMaxSize().padding(pd).testTag(MapTestTags.GOOGLE_MAP_SCREEN),
                cameraPositionState = cameraPositionState) {
                uiState.request.forEach { request ->
                    Marker(
                        state =
                            MarkerState(
                                position = LatLng(request.location.latitude, request.location.longitude)),
                        title = request.title,
                        snippet = request.description)
                }
            }
        })
}