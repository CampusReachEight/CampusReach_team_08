package com.android.sample.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.request.displayString
import com.android.sample.ui.overview.ConstantAcceptRequest.TEXT_SIZE
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
                Text(
                    text = "Title : " + (request.title),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_TITLE))
                Text(
                    text = "Description : " + (request.description),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_DESCRIPTION))
                Text(
                    text = "Tags: " + (request.tags.joinToString(", ") { it.displayString() }),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_TAG))
                Text(
                    text =
                        "Request type: " +
                            (request.requestType.joinToString(", ") { it.displayString() }),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_TYPE))
                Text(
                    text = "Status: " + (request.status.displayString()),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_STATUS))
                Text(
                    text = "Location : " + (request.locationName),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_LOCATION_NAME))
                Text(
                    text = "Start time : " + request.startTimeStamp.toDisplayString(),
                    fontSize = TEXT_SIZE,
                    modifier = Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_START_TIME))
                Text(
                    text = "Expiration time : " + request.expirationTime.toDisplayString(),
                    fontSize = TEXT_SIZE,
                    modifier =
                        Modifier.testTag(AcceptRequestScreenTestTags.REQUEST_EXPIRATION_TIME))

                Spacer(modifier = Modifier.height(24.dp))

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
