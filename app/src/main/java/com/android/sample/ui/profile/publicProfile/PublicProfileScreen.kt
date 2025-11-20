package com.android.sample.ui.profile.publicProfile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.model.profile.UserProfile
import com.android.sample.ui.profile.ProfilePicture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-off public profile screen: accepts a suspend loader that returns a `PublicProfile?`. Default
 * loader returns a deterministic fake for previews / quick wiring so callers don't need to provide
 * a repository when just rendering the UI.
 */
@Composable
fun PublicProfileScreen(
    profileId: String,
    modifier: Modifier = Modifier,
    profileLoader: suspend (String) -> PublicProfile? = { id ->
      // Simple default fake data to avoid unresolved references and allow previews
      // Disclaimer: this is not representative of real data!
      PublicProfile(
          userId = id,
          name = "Jane Doe",
          section = "Engineering",
          arrivalDate = "01/01/2020",
          pictureUriString = null,
          kudosReceived = 42,
          helpReceived = 5,
          followers = 128,
          following = 10)
    },
    onBack: () -> Unit = {}
) {
  val loadingState = remember { mutableStateOf(true) }
  val profileState = remember { mutableStateOf<PublicProfile?>(null) }
  val isFollowing = remember { mutableStateOf(false) }

  LaunchedEffect(profileId) {
    loadingState.value = true
    val p =
        try {
          withContext(Dispatchers.IO) { profileLoader(profileId) }
        } catch (_: Exception) {
          null
        }
    profileState.value = p
    loadingState.value = false
  }

  Column(
      modifier = modifier.padding(16.dp),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Top row: Back + placeholder Follow button (replaces edit button behavior)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically) {
              Button(onClick = onBack) { Text("Back") }
              Spacer(modifier = Modifier.width(8.dp))
              Spacer(modifier = Modifier.weight(1f))
              Button(onClick = { isFollowing.value = !isFollowing.value }) {
                Text(if (isFollowing.value) "Following" else "Follow")
              }
            }

        Spacer(modifier = Modifier.height(12.dp))

        if (loadingState.value) {
          Text("Loading...")
          return@Column
        }

        val profile = profileState.value
        if (profile == null) {
          Text("Profile not found")
          return@Column
        }

        ProfilePicture(
            profileId = profile.userId,
            onClick = {},
            withName = false,
            modifier = Modifier.size(128.dp))

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = profile.name)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = profile.section)
        profile.arrivalDate?.let { date ->
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "Joined: $date")
        }

        // Profile actions/stats removed for public view
      }
}

/**
 * Helper: map a `UserProfile` to `PublicProfile`. Use this in your caller when creating a loader
 * that reads from `UserProfileRepository`.
 */
fun userProfileToPublic(up: UserProfile): PublicProfile {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  val arrival =
      try {
        dateFormat.format(up.arrivalDate ?: Date())
      } catch (_: Exception) {
        null
      }
  return PublicProfile(
      userId = up.id,
      name = listOf(up.name, up.lastName).joinToString(" ").trim().ifEmpty { "Unknown" },
      section = up.section.name,
      arrivalDate = arrival,
      pictureUriString = up.photo?.toString(),
      kudosReceived = up.kudos,
      helpReceived = 0,
      followers = 0,
      following = 0)
}

@Preview(showBackground = true)
@Composable
fun PublicProfileScreenPreview() {
  PublicProfileScreen(profileId = "user123")
}
