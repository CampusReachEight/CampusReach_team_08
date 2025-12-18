package com.android.sample.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.Request
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.overview.toDisplayString
import com.android.sample.ui.request.ConstantRequestList
import com.android.sample.ui.request.RequestListItem
import com.android.sample.ui.theme.AppPalette
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
fun RequestDetailsTab(
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
  Box(modifier = Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)) {
    Material3RichText(
        modifier =
            Modifier.testTag(MapTestTags.REQUEST_DESCRIPTION).semantics {
              text = AnnotatedString(request.description)
            }) {
          Markdown(
              content = request.description,
              // Links are not interactive in bottom sheet for simplicity
              onLinkClicked = {})
        }
  }

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
            text = request.status.displayString(),
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
 * The button where you can go to EditScreen/AcceptScreen
 *
 * @param isOwner is the current user the owner of the request, can be null if a problem has
 *   occurred
 * @param navigationActions the actions of navigation
 * @param request the current request
 * @param mapViewModel the viewModel
 */
@Composable
private fun ButtonDetails(
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
              Modifier.padding(ConstantRequestList.ListPadding)
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
