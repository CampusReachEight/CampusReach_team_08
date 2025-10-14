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
import com.android.sample.ui.navigation.NavigationTestTags

object ProfileTestTags {
  const val PROFILE_HEADER = "profile_header"
  const val PROFILE_STATS = "profile_stats"
  const val PROFILE_INFORMATION = "profile_information"
  const val PROFILE_ACTIONS = "profile_actions"
  const val PROFILE_HEADER_NAME = "profile_header_name"
  const val PROFILE_HEADER_EMAIL = "profile_header_email"
  const val PROFILE_STAT_TOP_KUDOS = "profile_stat_top_kudos"
  const val PROFILE_STAT_BOTTOM_HELP_RECEIVED = "profile_stat_bottom_help_received"
  const val PROFILE_STAT_TOP_FOLLOWERS = "profile_stat_top_followers"
  const val PROFILE_STAT_BOTTOM_FOLLOWING = "profile_stat_bottom_following"
  const val PROFILE_ACTION_LOG_OUT = "profile_action_log_out"
  const val PROFILE_ACTION_ABOUT_APP = "profile_action_about_app"
}

val PrimaryColor = Color(0xFFF0F4FF)
val SecondaryColor = Color(0xFFD8E4FF)
val AccentColor = Color(0xFF1247F8)
val BlackColor = Color(0xFF1F242F)
val WhiteColor = Color(0xFFFFFFFF)

object ProfileDimens {
  val Horizontal = 16.dp
  val Vertical = 8.dp
  val CardElevation = 4.dp
  val ProfilePicture = 80.dp
  val HeaderPadding = 16.dp
  val HeaderSpacer = 16.dp
  val StatCardHeight = 150.dp
  val StatCardVerticalPadding = 20.dp
  val StatCardHorizontalPadding = 16.dp
  val StatCardSpacer = 6.dp
  val InfoCornerRadius = 4.dp
  val InfoSpacer = 4.dp
  val ActionVerticalPadding = 10.dp
  val ActionInternalPadding = 10.dp
  val IconSize = 40.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onBackClick: () -> Unit = {}) {
  val state by viewModel.state.collectAsState()
  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.PROFILE_SCREEN),
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
                        Modifier.fillMaxWidth()
                            .padding(ProfileDimens.Horizontal)
                            .testTag("profile_error"),
                    textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(ProfileDimens.Vertical))
              }
              ProfileHeader(state = state)
              Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
              ProfileStats(state = state)
              Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
              ProfileInformation(state = state)
              Spacer(modifier = Modifier.height(ProfileDimens.Horizontal))
              ProfileActions()
            }
          }
        }
      }
}

@Composable
fun ProfileHeader(state: ProfileState, onEditClick: () -> Unit = {}) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(ProfileDimens.HeaderPadding)
              .testTag(ProfileTestTags.PROFILE_HEADER),
      colors = CardDefaults.cardColors(containerColor = AccentColor),
      elevation = CardDefaults.cardElevation(defaultElevation = ProfileDimens.CardElevation)) {
        Box(modifier = Modifier.padding(ProfileDimens.HeaderPadding)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile Picture",
                modifier =
                    Modifier.size(ProfileDimens.ProfilePicture)
                        .clip(CircleShape)
                        .background(WhiteColor),
                tint = AccentColor)
            Spacer(modifier = Modifier.width(ProfileDimens.HeaderSpacer))
            Column {
              Text(
                  text = state.userName,
                  style = MaterialTheme.typography.titleMedium,
                  color = WhiteColor,
                  modifier = Modifier.testTag(ProfileTestTags.PROFILE_HEADER_NAME))
              Text(
                  text = state.userEmail,
                  style = MaterialTheme.typography.bodyMedium,
                  color = WhiteColor,
                  modifier = Modifier.testTag(ProfileTestTags.PROFILE_HEADER_EMAIL))
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
    modifier: Modifier = Modifier,
    topTag: String,
    bottomTag: String
) {
  Card(
      modifier = modifier.height(ProfileDimens.StatCardHeight),
      colors = CardDefaults.cardColors(containerColor = SecondaryColor),
      elevation = CardDefaults.cardElevation(defaultElevation = ProfileDimens.CardElevation)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        vertical = ProfileDimens.StatCardVerticalPadding,
                        horizontal = ProfileDimens.StatCardHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  text = labelTop,
                  style = MaterialTheme.typography.bodySmall,
                  color = BlackColor,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = topValue.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = AccentColor,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(topTag))
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = labelBottom,
                  style = MaterialTheme.typography.bodySmall,
                  color = BlackColor,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = bottomValue.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = AccentColor,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(bottomTag))
            }
      }
}

@Composable
fun ProfileStats(state: ProfileState) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(start = ProfileDimens.ProfilePicture, end = ProfileDimens.ProfilePicture)
              .testTag(ProfileTestTags.PROFILE_STATS),
      horizontalArrangement = Arrangement.spacedBy(ProfileDimens.Horizontal)) {
        StatGroupCard(
            labelTop = "Kudos",
            topValue = state.kudosReceived,
            labelBottom = "Help Received",
            bottomValue = state.helpReceived,
            modifier = Modifier.weight(1f),
            topTag = ProfileTestTags.PROFILE_STAT_TOP_KUDOS,
            bottomTag = ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED)
        StatGroupCard(
            labelTop = "Followers",
            topValue = state.followers,
            labelBottom = "Following",
            bottomValue = state.following,
            modifier = Modifier.weight(1f),
            topTag = ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS,
            bottomTag = ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING)
      }
}

@Composable
fun InfoRow(label: String, value: String) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(ProfileDimens.InfoCornerRadius))
              .background(WhiteColor)
              .padding(
                  vertical = ProfileDimens.CardElevation, horizontal = ProfileDimens.Horizontal)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = label, style = MaterialTheme.typography.bodyMedium, color = BlackColor)
          Text(
              text = value,
              style = MaterialTheme.typography.bodyMedium,
              color = AccentColor,
              modifier = Modifier.testTag("profile_info_${label.replace(" ", "_").lowercase()}"))
        }
      }
}

@Composable
fun ProfileInformation(state: ProfileState) {
  Column(
      modifier =
          Modifier.padding(horizontal = ProfileDimens.Horizontal)
              .testTag(ProfileTestTags.PROFILE_INFORMATION)) {
        Text(
            text = "Information",
            style = MaterialTheme.typography.titleMedium,
            color = BlackColor,
            modifier = Modifier.padding(bottom = ProfileDimens.Vertical))
        InfoRow(label = "Name", value = state.userName)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Profile Id", value = state.profileId)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Arrival date", value = state.arrivalDate)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Section", value = state.section)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Email", value = state.userEmail)
      }
}

@Composable
fun ProfileActions() {
  Column(
      modifier =
          Modifier.padding(horizontal = ProfileDimens.Horizontal)
              .testTag(ProfileTestTags.PROFILE_ACTIONS)) {
        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            color = BlackColor,
            modifier = Modifier.padding(bottom = ProfileDimens.Vertical))
        ActionItem(
            icon = Icons.Default.Logout,
            title = "Log out",
            subtitle = "Further secure your account for safety",
            tag = ProfileTestTags.PROFILE_ACTION_LOG_OUT)
        ActionItem(
            icon = Icons.Default.Info,
            title = "About App",
            subtitle = "Find out more about CampusReach",
            tag = ProfileTestTags.PROFILE_ACTION_ABOUT_APP)
      }
}

@Composable
fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tag: String
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = ProfileDimens.ActionVerticalPadding)
              .testTag(tag),
      colors = CardDefaults.cardColors(containerColor = WhiteColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ProfileDimens.ActionInternalPadding),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  modifier = Modifier.padding(end = ProfileDimens.Horizontal),
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
