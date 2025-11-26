package com.android.sample.ui.profile.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

@Composable
fun InfoRow(label: String, value: String, palette: AppPalette = appPalette()) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .testTag("profile_info_row_${label.replace(" ", "_").lowercase()}"),
      colors = CardDefaults.cardColors(containerColor = palette.surface),
      shape = RoundedCornerShape(ProfileDimens.InfoCornerRadius)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        vertical = ProfileDimens.CardElevation,
                        horizontal = ProfileDimens.Horizontal),
            verticalAlignment = Alignment.Top) {
              Text(
                  text = label,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.text,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  modifier =
                      Modifier.weight(0.35f, fill = true)
                          .testTag("profile_info_label_${label.replace(" ", "_").lowercase()}"))

              Spacer(modifier = Modifier.width(12.dp))

              Text(
                  text = value,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.accent,
                  softWrap = true,
                  textAlign = TextAlign.End,
                  modifier =
                      Modifier.weight(0.65f, fill = true)
                          .testTag("profile_info_${label.replace(" ", "_").lowercase()}"))
            }
      }
}

@Composable
fun ProfileInformation(state: ProfileState, palette: AppPalette = appPalette()) {
  Column(
      modifier =
          Modifier.padding(horizontal = ProfileDimens.Horizontal)
              .testTag(ProfileTestTags.PROFILE_INFORMATION)) {
        Text(
            text = "Information",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
            modifier = Modifier.padding(bottom = ProfileDimens.Vertical))
        InfoRow(label = "Name", value = state.userName, palette = palette)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Profile Id", value = state.profileId, palette = palette)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Arrival date", value = state.arrivalDate, palette = palette)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Section", value = state.userSection, palette = palette)
        Spacer(modifier = Modifier.height(ProfileDimens.InfoSpacer))
        InfoRow(label = "Email", value = state.userEmail, palette = palette)
      }
}
