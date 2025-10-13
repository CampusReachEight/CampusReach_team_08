package com.android.sample.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.navigation.NavigationTestTags

object ProfileTestTags {
  const val PROFILE_HEADER = "profile_header"
  const val PROFILE_STATS = "profile_stats"
  const val PROFILE_INFORMATION = "profile_information"
  const val PROFILE_ACTIONS = "profile_actions"
}

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
      modifier = Modifier.testTag(NavigationTestTags.PROFILE_SCREEN),
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
fun ProfileHeader(state: ProfileState) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = HorizontalPadding)
              .testTag(ProfileTestTags.PROFILE_HEADER),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(HorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Profile picture placeholder
              Box(
                  modifier =
                      Modifier.size(ProfilePictureSize)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(IconSize),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                  }

              Spacer(modifier = Modifier.height(VerticalPadding))

              Text(
                  text = state.userName,
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.testTag("profile_header_name"))
              Text(
                  text = state.userEmail,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimary)
            }
      }
}

@Composable
fun InfoRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = CardElevation),
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("profile_info_$label".replace(" ", "_").lowercase()))
      }
}

@Composable
fun ProfileStats(state: ProfileState) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = HorizontalPadding)
              .testTag(ProfileTestTags.PROFILE_STATS),
      horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard(value = state.kudosReceived, label = "Kudos Received")
        StatCard(value = state.helpReceived, label = "Help Received")
        StatCard(value = state.followers, label = "Followers")
        StatCard(value = state.following, label = "Following")
      }
}

@Composable
fun StatCard(value: Int, label: String) {
  Card(
      modifier = Modifier.width(ProfilePictureSize).height(ProfilePictureSize)
      // .semantics{ testTag = ProfileTestTags.PROFILE_STATS }
      ,
      elevation = CardDefaults.cardElevation(CardElevation)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = value.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold)
              Text(
                  text = label,
                  style = MaterialTheme.typography.bodySmall,
                  textAlign = TextAlign.Center)
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
            modifier = Modifier.padding(bottom = VerticalPadding))

        InfoRow(label = "Name", value = state.userName)
        InfoRow(label = "Profile Id", value = state.profileId)
        InfoRow(label = "Arrival date", value = state.arrivalDate)
        InfoRow(label = "Section", value = state.section)
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
  Column {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = VerticalPadding),
        verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = icon,
              contentDescription = null,
              modifier = Modifier.padding(end = HorizontalPadding))

          Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
          }
        }
    Divider()
  }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
  ProfileScreen()
}
