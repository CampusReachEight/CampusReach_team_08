package com.android.sample.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.theme.appPalette

object FollowListTestTags {
  const val FOLLOW_LIST_SCREEN = "followListScreen"
  const val FOLLOW_LIST_TITLE = "followListTitle"
  const val FOLLOW_LIST = "followList"
  const val FOLLOW_LIST_ITEM = "followListItem"
  const val FOLLOW_LIST_EMPTY = "followListEmpty"
  const val FOLLOW_LIST_LOADING = "followListLoading"
  const val FOLLOW_LIST_ERROR = "followListError"
}

enum class FollowListType {
  FOLLOWERS,
  FOLLOWING
}

/**
 * Screen displaying a list of users (either followers or following). Clicking on a user navigates
 * to their public profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    listType: FollowListType,
    viewModel: FollowListViewModel =
        viewModel(factory = FollowListViewModelFactory(userId, listType)),
    navigationActions: NavigationActions? = null,
    onBackClick: () -> Unit = {}
) {
  val state by viewModel.state.collectAsState()

  LaunchedEffect(Unit) { viewModel.loadUsers() }

  Scaffold(
      modifier = Modifier.testTag(FollowListTestTags.FOLLOW_LIST_SCREEN),
      containerColor = appPalette().background,
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = if (listType == FollowListType.FOLLOWERS) "Followers" else "Following",
                  modifier = Modifier.testTag(FollowListTestTags.FOLLOW_LIST_TITLE))
            },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = appPalette().primary,
                    titleContentColor = appPalette().onPrimary))
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          when {
            state.isLoading -> {
              CircularProgressIndicator(
                  modifier =
                      Modifier.align(Alignment.Center)
                          .testTag(FollowListTestTags.FOLLOW_LIST_LOADING))
            }
            state.errorMessage != null -> {
              Text(
                  text = state.errorMessage ?: "An error occurred",
                  color = appPalette().error,
                  textAlign = TextAlign.Center,
                  modifier =
                      Modifier.align(Alignment.Center)
                          .padding(16.dp)
                          .testTag(FollowListTestTags.FOLLOW_LIST_ERROR))
            }
            state.users.isEmpty() -> {
              Text(
                  text =
                      if (listType == FollowListType.FOLLOWERS) "No followers yet"
                      else "Not following anyone yet",
                  textAlign = TextAlign.Center,
                  modifier =
                      Modifier.align(Alignment.Center)
                          .testTag(FollowListTestTags.FOLLOW_LIST_EMPTY))
            }
            else -> {
              LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag(FollowListTestTags.FOLLOW_LIST),
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.users.size) { index ->
                      val user = state.users[index]
                      FollowListItem(
                          user = user,
                          onClick = {
                            navigationActions?.navigateTo(Screen.PublicProfile(user.id))
                          })
                    }
                  }
            }
          }
        }
      }
}

@Composable
fun FollowListItem(user: UserProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .testTag(FollowListTestTags.FOLLOW_LIST_ITEM),
      colors = CardDefaults.cardColors(containerColor = appPalette().surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              ProfilePicture(
                  profileId = user.id, onClick = { onClick() }, modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                val fullName =
                    if (user.lastName.isBlank()) {
                      user.name
                    } else {
                      "${user.name} ${user.lastName}"
                    }
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = appPalette().text)
                if (user.section != UserSections.NONE) {
                  Text(
                      text = user.section.label,
                      style = MaterialTheme.typography.bodySmall,
                      color = appPalette().text.copy(alpha = 0.7f))
                }
              }
            }
      }
}
