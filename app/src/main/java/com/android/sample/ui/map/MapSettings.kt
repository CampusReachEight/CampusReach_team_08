package com.android.sample.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.theme.appPalette

object MapSettingsTestTags {
  const val TITLE_MAP_SETTINGS = "titleMapSettings"
  const val DESCRIPTION_MAP_SETTINGS = "descriptionMapSettings"
  const val CLOSE_BUTTON_SETTINGS = "closeButtonSettings"
  const val NEAREST_REQUEST_SETTINGS = "nearestRequestSettings"
  const val CURRENT_LOCATION_SETTINGS = "currentLocationSettings"
  const val NO_ZOOM_SETTINGS = "noZoomSettings"
}

/** Dialog to select zoom preference. Displayed when user clicks the settings button. */
@Composable
fun ZoomSettingsDialog(
    currentPreference: MapZoomPreference,
    onPreferenceChange: (MapZoomPreference) -> Unit,
    onDismiss: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = ConstantMap.AUTO_ZOOM_SETTINGS,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(MapSettingsTestTags.TITLE_MAP_SETTINGS))
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(ConstantMap.BUTTON_SPACING)) {
          Text(
              text = ConstantMap.DESCRIPTION_REQUEST,
              style = MaterialTheme.typography.bodyMedium,
              color = appPalette().onSurface.copy(alpha = ConstantMap.ALPHA_DIVIDER_SETTINGS),
              modifier = Modifier.testTag(MapSettingsTestTags.DESCRIPTION_MAP_SETTINGS))

          Spacer(modifier = Modifier.height(ConstantMap.CARD_VERTICAL_PADDING))

          // Option 1: Nearest Request
          ZoomOptionItem(
              icon = Icons.Default.LocationOn,
              title = ConstantMap.TITLE_NEARBY_REQUEST,
              description = ConstantMap.DESCRIPTION_NEARBY_REQUEST,
              isSelected = currentPreference == MapZoomPreference.NEAREST_REQUEST,
              onClick = { onPreferenceChange(MapZoomPreference.NEAREST_REQUEST) },
              modifier = Modifier.testTag(MapSettingsTestTags.NEAREST_REQUEST_SETTINGS))

          // Option 2: Current Location
          ZoomOptionItem(
              icon = Icons.Default.MyLocation,
              title = ConstantMap.TITLE_CURR_LOCATION,
              description = ConstantMap.DESCRIPTION_CURRENT_LOCATION,
              isSelected = currentPreference == MapZoomPreference.CURRENT_LOCATION,
              onClick = { onPreferenceChange(MapZoomPreference.CURRENT_LOCATION) },
              modifier = Modifier.testTag(MapSettingsTestTags.CURRENT_LOCATION_SETTINGS))

          // Option 3: No Auto Zoom
          ZoomOptionItem(
              icon = Icons.Default.Block,
              title = ConstantMap.TITLE_NO_ZOOM,
              description = ConstantMap.DESCRIPTION_NO_ZOOM,
              isSelected = currentPreference == MapZoomPreference.NO_AUTO_ZOOM,
              onClick = { onPreferenceChange(MapZoomPreference.NO_AUTO_ZOOM) },
              modifier = Modifier.testTag(MapSettingsTestTags.NO_ZOOM_SETTINGS))
        }
      },
      confirmButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(MapSettingsTestTags.CLOSE_BUTTON_SETTINGS)) {
              Text(ConstantMap.CLOSE_BUTTON_DESCRIPTION)
            }
      })
}

/** Single option item in the zoom settings dialog. */
@Composable
private fun ZoomOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
  Surface(
      onClick = onClick,
      shape = RoundedCornerShape(ConstantMap.ROUND_CORNER),
      color =
          if (isSelected) {
            appPalette().primary.copy(alpha = ConstantMap.ALPHA_DIVIDER_SETTINGS_LITTLE)
          } else {
            Color.Transparent
          },
      border =
          if (isSelected) {
            BorderStroke(ConstantMap.TWO_DP, appPalette().accent)
          } else {
            BorderStroke(
                ConstantMap.ONE_DP,
                appPalette().onSurface.copy(alpha = ConstantMap.ALPHA_DIVIDER_SETTINGS_MID))
          },
      modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(ConstantMap.SIXTEEN_DP),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ConstantMap.TWELVE_DP)) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = if (isSelected) appPalette().accent else appPalette().onSurface,
                  modifier = Modifier.size(ConstantMap.TWENTY_FOUR_DP))

              Column(modifier = Modifier.weight(ConstantMap.WEIGHT_FILL)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) appPalette().accent else appPalette().onSurface)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isSelected) appPalette().accent
                        else
                            appPalette()
                                .onSurface
                                .copy(alpha = ConstantMap.ALPHA_DIVIDER_SETTINGS_BIG))
              }

              if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = ConstantMap.SELECTED_ZOOM,
                    tint = appPalette().accent,
                    modifier = Modifier.size(ConstantMap.TWENTY_DP))
              }
            }
      }
}
