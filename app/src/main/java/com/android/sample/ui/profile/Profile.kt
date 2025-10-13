package com.android.sample.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object ProfileTestTags {
  const val PROFILE_HEADER = "profile_header"
  const val PROFILE_STATS = "profile_stats"
  const val PROFILE_INFORMATION = "profile_information"
  const val PROFILE_ACTIONS = "profile_actions"
}

// Define custom colors
val PrimaryColor = Color(0xFFF0F4FF)
val SecondaryColor = Color(0xFFD8E4FF)
val AccentColor = Color(0xFF1247F8)
val BlackColor = Color(0xFF1F242F)
val WhiteColor = Color(0xFFFFFFFF)

private val HorizontalPadding = 16.dp
private val VerticalPadding = 8.dp
private val CardElevation = 4.dp
private val ProfilePictureSize = 80.dp
private val IconSize = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onBackClick: () -> Unit = {}) {
  val state by viewModel.state.collectAsState()
  Scaffold(
      containerColor = PrimaryColor,
      topBar = {
        TopAppBar(
            title = { Text("Profile") },
            navigationIcon = {
              IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            })
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).testTag("profile_loading"))
          } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
              if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.fillMaxWidth().padding(HorizontalPadding).testTag("profile_error"),
                    textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(VerticalPadding))
              }
              ProfileHeader(state = state)
              Spacer(modifier = Modifier.height(HorizontalPadding))
              ProfileStats(state = state)
              Spacer(modifier = Modifier.height(HorizontalPadding))
              ProfileInformation(state = state)
              Spacer(modifier = Modifier.height(HorizontalPadding))
              ProfileActions()
            }
          }
        }
      }
}

@Composable
// ProfileHeader with accent card and edit icon
fun ProfileHeader(state: ProfileState, onEditClick: () -> Unit = {}) {
  Card(
      modifier = Modifier.fillMaxWidth().padding(HorizontalPadding),
      colors = CardDefaults.cardColors(containerColor = AccentColor),
      elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)) {
        Box(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile Picture",
                modifier =
                    Modifier.size(ProfilePictureSize).clip(CircleShape).background(WhiteColor),
                tint = AccentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                  text = state.userName,
                  style = MaterialTheme.typography.titleMedium,
                  color = WhiteColor)
              Text(
                  text = state.userEmail,
                  style = MaterialTheme.typography.bodyMedium,
                  color = WhiteColor)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onEditClick) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", tint = WhiteColor)
            }
          }
        }
      }
}

@Composable
fun StatGroupCard(
    labelTop: String,
    topValue: Int,
    labelBottom: String,
    bottomValue: Int,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier.height(150.dp),
      colors = CardDefaults.cardColors(containerColor = SecondaryColor),
      elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  text = labelTop,
                  style = MaterialTheme.typography.bodySmall,
                  color = BlackColor,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                  text = topValue.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = AccentColor,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                  text = labelBottom,
                  style = MaterialTheme.typography.bodySmall,
                  color = BlackColor,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                  text = bottomValue.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = AccentColor,
                  textAlign = TextAlign.Center)
            }
      }
}

@Composable
fun ProfileStats(state: ProfileState) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(start = 80.dp, end = 80.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatGroupCard(
            labelTop = "Kudos",
            topValue = state.kudosReceived,
            labelBottom = "Help Received",
            bottomValue = state.helpReceived,
            modifier = Modifier.weight(1f))
        StatGroupCard(
            labelTop = "Followers",
            topValue = state.followers,
            labelBottom = "Following",
            bottomValue = state.following,
            modifier = Modifier.weight(1f))
      }
}

@Composable
fun InfoRow(label: String, value: String) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(4.dp))
              .background(WhiteColor) // Set your desired color here
              .padding(vertical = CardElevation, horizontal = HorizontalPadding)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = label, style = MaterialTheme.typography.bodyMedium, color = BlackColor)
          Text(
              text = value,
              style = MaterialTheme.typography.bodyMedium,
              color = AccentColor,
              modifier = Modifier.testTag("profile_info_$label".replace(" ", "_").lowercase()))
        }
      }
}

@Composable
fun ProfileInformation(state: ProfileState) {
  Column(
      modifier =
          Modifier.padding(horizontal = HorizontalPadding)
              .testTag(ProfileTestTags.PROFILE_INFORMATION)) {
        Text(
            text = "Information",
            style = MaterialTheme.typography.titleMedium,
            color = BlackColor,
            modifier = Modifier.padding(bottom = VerticalPadding))
        InfoRow(label = "Name", value = state.userName)
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(label = "Profile Id", value = state.profileId)
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(label = "Arrival date", value = state.arrivalDate)
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(label = "Section", value = state.section)
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(label = "Email", value = state.userEmail)
      }
}

@Composable
fun ProfileActions() {
  Column(
      modifier =
          Modifier.padding(horizontal = HorizontalPadding)
              .testTag(ProfileTestTags.PROFILE_ACTIONS)) {
        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            color = BlackColor,
            modifier = Modifier.padding(bottom = VerticalPadding))
        ActionItem(
            icon = Icons.Default.Logout,
            title = "Log out",
            subtitle = "Further secure your account for safety")
        ActionItem(
            icon = Icons.Default.Info,
            title = "About App",
            subtitle = "Find out more about CampusReach")
      }
}

@Composable
fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
  Card(
      modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
      colors = CardDefaults.cardColors(containerColor = WhiteColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  modifier = Modifier.padding(end = HorizontalPadding),
                  tint = AccentColor)
              Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = BlackColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentColor.copy(alpha = 0.6f))
              }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
  ProfileScreen()
}
