package com.android.sample.ui.profile.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextDecoration
import com.android.sample.ui.leaderboard.LeaderboardBadgeThemes.forRank
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.UiDimens
import com.android.sample.ui.theme.appPalette

private const val WEIGHT_1F = 1f

@Composable
fun StatGroupCard(
    labelTop: String,
    topValue: Int,
    labelBottom: String,
    bottomValue: Int,
    modifier: Modifier = Modifier,
    topTag: String,
    bottomTag: String,
    onTopClick: (() -> Unit)? = null,
    onBottomClick: (() -> Unit)? = null,
    palette: AppPalette = appPalette()
) {
  Card(
      modifier = modifier.height(ProfileDimens.StatCardHeight),
      colors = CardDefaults.cardColors(containerColor = palette.secondary),
      elevation = CardDefaults.cardElevation(defaultElevation = ProfileDimens.CardElevation)) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        vertical = ProfileDimens.StatCardVerticalPadding,
                        horizontal = ProfileDimens.StatCardHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Top half (weighted) to ensure vertical symmetry across cards
              Column(
                  modifier =
                      Modifier.testTag(topTag + ProfileTestTags.BUTTON_SUFFIX)
                          .weight(WEIGHT_1F)
                          .then(
                              if (onTopClick != null) Modifier.clickable(onClick = onTopClick)
                              else Modifier),
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
                  }

              // Bottom half (weighted) to ensure vertical symmetry across cards
              Column(
                  modifier =
                      Modifier.testTag(bottomTag + ProfileTestTags.BUTTON_SUFFIX)
                          .weight(WEIGHT_1F)
                          .then(
                              if (onBottomClick != null) Modifier.clickable(onClick = onBottomClick)
                              else Modifier),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
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
}

@Composable
fun PositionCard(state: ProfileState, palette: AppPalette = appPalette(), modifier: Modifier) {
  // Custom rank card with "#" prefix
  Card(
      modifier = modifier,
      colors = CardDefaults.cardColors(containerColor = palette.secondary),
      elevation = CardDefaults.cardElevation(defaultElevation = ProfileDimens.CardElevation)) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        vertical = ProfileDimens.StatCardVerticalPadding,
                        horizontal = ProfileDimens.StatCardHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  text = "Helper's\nRank",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.text,
                  textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(ProfileDimens.StatCardSpacer))
              Text(
                  text = "#${state.leaderboardPosition}",
                  style =
                      MaterialTheme.typography.titleLarge.copy(
                          textDecoration = TextDecoration.Underline),
                  fontWeight = FontWeight.Bold,
                  color =
                      forRank(state.leaderboardPosition ?: Int.MAX_VALUE)?.primaryColor
                          ?: palette.accent,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(ProfileTestTags.PROFILE_STAT_TOP_RANK))
            }
      }
}

@Composable
fun ProfileStats(
    state: ProfileState,
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null,
    palette: AppPalette = appPalette()
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(start = UiDimens.SpacingXxl, end = UiDimens.SpacingXxl)
              .testTag(ProfileTestTags.PROFILE_STATS),
      horizontalArrangement = Arrangement.spacedBy(ProfileDimens.Horizontal)) {
        StatGroupCard(
            labelTop = "Kudos",
            topValue = state.kudosReceived,
            labelBottom = "Help\nReceived",
            bottomValue = state.helpReceived,
            modifier = Modifier.weight(WEIGHT_1F),
            topTag = ProfileTestTags.PROFILE_STAT_TOP_KUDOS,
            bottomTag = ProfileTestTags.PROFILE_STAT_BOTTOM_HELP_RECEIVED,
            palette = palette)

        StatGroupCard(
            labelTop = "Followers",
            topValue = state.followers,
            labelBottom = "Following",
            bottomValue = state.following,
            modifier = Modifier.weight(WEIGHT_1F),
            topTag = ProfileTestTags.PROFILE_STAT_TOP_FOLLOWERS,
            bottomTag = ProfileTestTags.PROFILE_STAT_BOTTOM_FOLLOWING,
            onTopClick = onFollowersClick,
            onBottomClick = onFollowingClick,
            palette = palette)

        if (state.leaderboardPosition != null) {
          PositionCard(
              state = state,
              palette = palette,
              modifier = Modifier.weight(WEIGHT_1F).height(ProfileDimens.StatCardHeight))
        }
      }
}
