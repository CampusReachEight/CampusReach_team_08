import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.appPalette

@Composable
fun ProfileActions(onLogoutClick: () -> Unit = {}) {
    Column(
        modifier =
            Modifier.padding(horizontal = ProfileDimens.Horizontal)
                .testTag(ProfileTestTags.PROFILE_ACTIONS)) {
        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            color = appPalette().text,
            modifier = Modifier.padding(bottom = ProfileDimens.Vertical))
        ActionItem(
            icon = Icons.Default.Logout,
            title = "Log out",
            subtitle = "Further secure your account for safety",
            tag = ProfileTestTags.PROFILE_ACTION_LOG_OUT,
            onClick = onLogoutClick)
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
    tag: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = ProfileDimens.ActionVerticalPadding)
                .testTag(tag)
                .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = appPalette().surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ProfileDimens.ActionInternalPadding),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = ProfileDimens.Horizontal),
                tint = appPalette().accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = appPalette().text)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = appPalette().accent.copy(alpha = 0.6f))
            }
        }
    }
}