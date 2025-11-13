package com.android.sample.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.displayString
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.overview.ConstantAcceptRequest.TEXT_SIZE
import com.android.sample.ui.theme.appPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AcceptRequestScreenTestTags {
  const val REQUEST_BUTTON = "requestButton"
  const val REQUEST_TITLE = "requestTitle"
  const val REQUEST_DESCRIPTION = "requestDescription"
  const val REQUEST_TAG = "requestTag"
  const val REQUEST_TYPE = "requestType"
  const val REQUEST_STATUS = "requestStatus"
  const val REQUEST_LOCATION_NAME = "requestLocationName"
  const val REQUEST_START_TIME = "requestStartTime"
  const val REQUEST_EXPIRATION_TIME = "requestExpirationTime"
  const val NO_REQUEST = "noRequest"
  const val REQUEST_COLUMN = "requestColumn"
  const val REQUEST_TOP_BAR = "requestTopBar"
  const val REQUEST_GO_BACK = "requestGoBack"
}

object AcceptRequestConstUI {
  val ICON_SIZE = 24.dp
  val ICON_PADDING = 2.dp
  val PADDING_ROW_HOR = 16.dp
  val PADDING_ROW_VER = 12.dp
  val SPACER_WIDTH = 24.dp
  val HORIZONTAL_DIVIDER_PADDING = 72.dp
  val BOTTOM_TEXT_PADDING = 2.dp
  val BOTTOM_TEXT_TRANSPARENCY = 0.6f
  val HORIZONTAL_DIVIDER_TRANSPARENCY = 0.1f
  val HORIZONTAL_DIVIDER_THICKNESS = 0.5.dp
  val INNER_COLUMN_WEIGHT = 1f
  val UPPER_TEXT_FONT_SIZE = 17.sp
  val BOTTOM_TEXT_FONT_SIZE = 15.sp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptRequestScreen(
    requestId: String,
    acceptRequestViewModel: AcceptRequestViewModel = viewModel(),
    onGoBack: () -> Unit = {}
) {
  LaunchedEffect(requestId) { acceptRequestViewModel.loadRequest(requestId) }

  val requestState by acceptRequestViewModel.uiState.collectAsState()
  val errorMessage = requestState.errorMsg

  val context = LocalContext.current

  LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
      Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
      acceptRequestViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.ACCEPT_REQUEST_SCREEN),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  requestState.request?.title ?: "",
                  Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_TOP_BAR))
            },
            navigationIcon = {
              IconButton(
                  onClick = { onGoBack() },
                  Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_GO_BACK)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back")
                  }
            })
      },
      content = { pd ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(pd)
                    .padding(
                        top = ConstantAcceptRequest.BIG_PADDING,
                        start = ConstantAcceptRequest.SMALL_PADDING,
                        end = ConstantAcceptRequest.SMALL_PADDING,
                        bottom = ConstantAcceptRequest.SMALL_PADDING)
                    .testTag(AcceptRequestScreenTestTags.REQUEST_COLUMN),
            verticalArrangement = Arrangement.spacedBy(ConstantAcceptRequest.SPACE_BETWEEN_TEXT)) {
              requestState.request?.let { request ->
                Column(modifier = Modifier.fillMaxWidth().background(appPalette().primary)) {
                  // Description
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_DESCRIPTION),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Description",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE).padding(top = 2.dp),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Description",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text)
                              Text(
                                  text = request.description,
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }

                  HorizontalDivider(
                      modifier =
                          Modifier.padding(start = AcceptRequestConstUI.HORIZONTAL_DIVIDER_PADDING),
                      thickness = AcceptRequestConstUI.HORIZONTAL_DIVIDER_THICKNESS,
                      color =
                          appPalette()
                              .text
                              .copy(alpha = AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY))

                  // Tags
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_TAG),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.LocalOffer,
                            contentDescription = "Tags",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE)
                                    .padding(top = AcceptRequestConstUI.ICON_PADDING),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Tags",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text,
                              )
                              Text(
                                  text = request.tags.joinToString(", ") { it.displayString() },
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }

                  HorizontalDivider(
                      modifier =
                          Modifier.padding(start = AcceptRequestConstUI.HORIZONTAL_DIVIDER_PADDING),
                      thickness = AcceptRequestConstUI.HORIZONTAL_DIVIDER_THICKNESS,
                      color =
                          appPalette()
                              .text
                              .copy(alpha = AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY))

                  // Request type
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_TYPE),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = "Request type",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE)
                                    .padding(top = AcceptRequestConstUI.ICON_PADDING),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Request type",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text,
                              )
                              Text(
                                  text =
                                      request.requestType.joinToString(", ") { it.displayString() },
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }

                  HorizontalDivider(
                      modifier =
                          Modifier.padding(start = AcceptRequestConstUI.HORIZONTAL_DIVIDER_PADDING),
                      thickness = AcceptRequestConstUI.HORIZONTAL_DIVIDER_THICKNESS,
                      color =
                          appPalette()
                              .text
                              .copy(alpha = AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY))

                  // Status
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_STATUS),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Status",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE)
                                    .padding(top = AcceptRequestConstUI.ICON_PADDING),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Status",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text,
                              )
                              Text(
                                  text = request.status.displayString(),
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }

                  HorizontalDivider(
                      modifier =
                          Modifier.padding(start = AcceptRequestConstUI.HORIZONTAL_DIVIDER_PADDING),
                      thickness = AcceptRequestConstUI.HORIZONTAL_DIVIDER_THICKNESS,
                      color =
                          appPalette()
                              .text
                              .copy(alpha = AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY))

                  // Location
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_LOCATION_NAME),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE)
                                    .padding(top = AcceptRequestConstUI.ICON_PADDING),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Location",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text)
                              Text(
                                  text = request.locationName,
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }

                  HorizontalDivider(
                      modifier =
                          Modifier.padding(start = AcceptRequestConstUI.HORIZONTAL_DIVIDER_PADDING),
                      thickness = AcceptRequestConstUI.HORIZONTAL_DIVIDER_THICKNESS,
                      color =
                          appPalette()
                              .text
                              .copy(alpha = AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY))

                  // Start time
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_START_TIME),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = "Start time",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE)
                                    .padding(top = AcceptRequestConstUI.ICON_PADDING),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Start time",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text)
                              Text(
                                  text = request.startTimeStamp.toDisplayString(),
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }

                  HorizontalDivider(
                      modifier =
                          Modifier.padding(start = AcceptRequestConstUI.HORIZONTAL_DIVIDER_PADDING),
                      thickness = AcceptRequestConstUI.HORIZONTAL_DIVIDER_THICKNESS,
                      color =
                          appPalette()
                              .text
                              .copy(alpha = AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY))

                  // Expiration time
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = AcceptRequestConstUI.PADDING_ROW_HOR,
                                  vertical = AcceptRequestConstUI.PADDING_ROW_VER)
                              .testTag(AcceptRequestScreenTestTags.REQUEST_EXPIRATION_TIME),
                      verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.WatchLater,
                            contentDescription = "Expiration time",
                            modifier =
                                Modifier.size(AcceptRequestConstUI.ICON_SIZE)
                                    .padding(top = AcceptRequestConstUI.ICON_PADDING),
                            tint = appPalette().text)
                        Spacer(modifier = Modifier.width(AcceptRequestConstUI.SPACER_WIDTH))
                        Column(
                            modifier = Modifier.weight(AcceptRequestConstUI.INNER_COLUMN_WEIGHT)) {
                              Text(
                                  text = "Expiration time",
                                  fontSize = AcceptRequestConstUI.UPPER_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color = appPalette().text)
                              Text(
                                  text = request.expirationTime.toDisplayString(),
                                  fontSize = AcceptRequestConstUI.BOTTOM_TEXT_FONT_SIZE,
                                  fontWeight = FontWeight.Normal,
                                  color =
                                      appPalette()
                                          .text
                                          .copy(
                                              alpha =
                                                  AcceptRequestConstUI.BOTTOM_TEXT_TRANSPARENCY),
                                  modifier =
                                      Modifier.padding(
                                          top = AcceptRequestConstUI.BOTTOM_TEXT_PADDING))
                            }
                      }
                }

                Spacer(modifier = Modifier.height(AcceptRequestConstUI.SPACER_WIDTH))

                Button(
                    onClick = {
                      if (requestState.accepted) {
                        acceptRequestViewModel.cancelAcceptanceToRequest(requestId)
                      } else {
                        acceptRequestViewModel.acceptRequest(requestId)
                      }
                    },
                    enabled = !requestState.isLoading,
                    modifier =
                        Modifier.align(Alignment.CenterHorizontally)
                            .testTag(AcceptRequestScreenTestTags.REQUEST_BUTTON)) {
                      if (requestState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ConstantAcceptRequest.CIRCULAR_LOAD_SIZE))
                      } else {
                        Text(if (requestState.accepted) "Cancel" else "Accept")
                      }
                    }
              }
                  ?: Text(
                      text = "An error occurred. Please, reload window or go back",
                      fontSize = TEXT_SIZE,
                      modifier = Modifier.testTag(AcceptRequestScreenTestTags.NO_REQUEST))
            }
      })
}

fun Date.toDisplayString(): String {
  return this.let { timestamp ->
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp)
  }
}
