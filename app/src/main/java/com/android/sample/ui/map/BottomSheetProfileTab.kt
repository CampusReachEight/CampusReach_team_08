package com.android.sample.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.android.sample.model.profile.UserProfile
import com.android.sample.model.request.Request
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.profile.ProfilePicture
import com.android.sample.ui.theme.AppPalette

@Composable
fun CurrentProfileUI(
    isOwner: Boolean?,
    navigationActions: NavigationActions?,
    profile: UserProfile?,
    mapViewModel: MapViewModel,
    columnScope: ColumnScope,
    request: Request,
    appPalette: AppPalette
) {
    with(columnScope) {
        if (profile != null) {
            Surface(
                color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_PRIMARY_SURFACE),
                shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_SMALL),
                modifier = Modifier.padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.padding(ConstantMap.PADDING_HORIZONTAL_STANDARD).fillMaxWidth()) {
                    ProfilePicture(
                        profileRepository = mapViewModel.profileRepository,
                        profileId = profile.id,
                        onClick = {},
                        modifier = Modifier.size(ConstantMap.REQUEST_ITEM_ICON_SIZE))

                    Text(
                        text = "${profile.lastName} ${profile.name}",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontSize = ConstantMap.FONT_SIZE_BIG),
                        color = appPalette.onPrimary,
                        modifier =
                            Modifier.padding(start = ConstantMap.PADDING_STANDARD)
                                .testTag(MapTestTags.PROFILE_NAME))
                }
            }
            // Kudos + Section
            Surface(
                shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(ConstantMap.SPACER_HEIGHT_LARGE),
                    horizontalArrangement = Arrangement.spacedBy(ConstantMap.SPACER_HEIGHT_MEDIUM)) {
                    Surface(
                        shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
                        color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_KUDOS_DIVIDER),
                        modifier = Modifier.weight(ConstantMap.WEIGHT_FILL)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                            Text(
                                text = ConstantMap.KUDOS,
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontSize = ConstantMap.FONT_SIZE_BIG),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(MapTestTags.PROFILE_KUDOS_TEXT))

                            Text(
                                text = profile.kudos.toString(),
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = ConstantMap.FONT_SIZE_MID),
                                color = appPalette.accent,
                                modifier = Modifier.testTag(MapTestTags.PROFILE_KUDOS))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_LARGE),
                        color = appPalette.accent.copy(alpha = ConstantMap.ALPHA_KUDOS_DIVIDER),
                        modifier = Modifier.weight(ConstantMap.WEIGHT_FILL)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(ConstantMap.PADDING_STANDARD)) {
                            Text(
                                text = ConstantMap.SECTION,
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontSize = ConstantMap.FONT_SIZE_BIG),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(MapTestTags.PROFILE_SECTION_TEXT))
                            Text(
                                text = profile.section.label,
                                style =
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontSize = ConstantMap.FONT_SIZE_MID),
                                color = appPalette.accent,
                                modifier = Modifier.testTag(MapTestTags.PROFILE_SECTION))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))

            // Location name
            Surface(
                shape = RoundedCornerShape(ConstantMap.CORNER_RADIUS_MEDIUM),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.padding(
                            horizontal = ConstantMap.PADDING_HORIZONTAL_STANDARD,
                            vertical = ConstantMap.PADDING_STANDARD)) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = ConstantMap.LOCATION,
                        tint = appPalette.primary,
                        modifier = Modifier.size(ConstantMap.ICON_SIZE_LOCATION))
                    Spacer(modifier = Modifier.width(ConstantMap.SPACER_WIDTH_SMALL))
                    Text(
                        text = profile.arrivalDate.toDisplayStringWithoutHours(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(MapTestTags.PROFILE_CREATION_DATE))
                }
            }
            Spacer(modifier = Modifier.height(ConstantMap.SPACER_HEIGHT_MEDIUM))
        }
        ButtonProfileDetails(isOwner, navigationActions, profile, mapViewModel, request, appPalette)
    }
}

@Composable
fun ButtonProfileDetails(
    isOwner: Boolean?,
    navigationActions: NavigationActions?,
    profile: UserProfile?,
    mapViewModel: MapViewModel,
    request: Request,
    appPalette: AppPalette
) {

    if (isOwner == false && profile != null) return

    val buttonText =
        when {
            profile == null -> ConstantMap.PROBLEM_OCCUR
            isOwner == true -> ConstantMap.TEXT_EDIT_PROFILE
            else -> ConstantMap.PROBLEM_OCCUR
        }

    val onClickAction: () -> Unit = {
        when {
            profile == null -> mapViewModel.updateCurrentProfile(request.creatorId)
            isOwner == true -> {
                navigationActions?.navigateTo(Screen.Profile(profile.id))
                mapViewModel.goOnAnotherScreen()
            }
            else -> mapViewModel.isHisRequest(request)
        }
    }

    Button(
        onClick = onClickAction,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = appPalette.accent, contentColor = appPalette.primary),
        modifier =
            Modifier.fillMaxWidth()
                .padding(bottom = ConstantMap.SPACER_HEIGHT_LARGE)
                .testTag(MapTestTags.PROFILE_EDIT_BUTTON)) {
        Text(buttonText)
    }
}