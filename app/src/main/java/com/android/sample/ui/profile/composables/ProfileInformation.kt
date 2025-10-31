import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.android.sample.ui.profile.ProfileDimens
import com.android.sample.ui.profile.ProfileState
import com.android.sample.ui.profile.ProfileTestTags
import com.android.sample.ui.theme.appPalette

@Composable
fun InfoRow(label: String, value: String) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(ProfileDimens.InfoCornerRadius))
                .background(appPalette().surface)
                .padding(
                    vertical = ProfileDimens.CardElevation, horizontal = ProfileDimens.Horizontal)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = appPalette().text)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = appPalette().accent,
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
            color = appPalette().text,
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