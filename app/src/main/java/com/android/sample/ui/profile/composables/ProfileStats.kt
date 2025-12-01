package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

@Composable
fun StatGroupCard(
    labelTop: String,
    topValue: Int,
    labelBottom: String,
    bottomValue: Int,
    modifier: Modifier = Modifier,
    topTag: String,
    bottomTag: String,
    palette: AppPalette = appPalette()
) {
  Card(
      modifier = modifier.height(ProfileDimens.StatCardHeight),
      colors = CardDefaults.cardColors(containerColor = palette.secondary),
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
                  color = palette.text,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = topValue.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.accent,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(topTag))
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = labelBottom,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.text,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = bottomValue.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.accent,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(bottomTag))
            }
      }
}

@Composable
fun ProfileStats(state: ProfileState, palette: AppPalette = appPalette()) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(start = ProfileDimens.ProfilePicture, end = ProfileDimens.ProfilePicture)
              .testTag(ProfileTestTags.PROFILE_STATS),
      horizontalArrangement = Arrangement.spacedBy(ProfileDimens.Horizontal)) {
        StatGroupCard(
            labelTop = "Kudos",
            topValue = state.kudosReceived,
            labelBottom = "Help\u00A0Received",
            bottomValue = state.helpReceived,
            modifier = Modifier.weight(1f),
            topTag = ProfileTestTags.PROFILE_STAT_TOP_KUDOS,
            bottomTag = ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED,
            palette = palette)
        StatGroupCard(
            labelTop = "Followers",
            topValue = state.followers,
            labelBottom = "Following",
            bottomValue = state.following,
            modifier = Modifier.weight(1f),
            topTag = ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS,
            bottomTag = ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING,
            palette = palette)
      }
}
