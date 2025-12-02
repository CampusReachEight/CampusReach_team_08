package com.android.sample.ui.profile.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.sample.model.profile.UserProfileRepository
import com.android.sample.ui.profile.UserSections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: PublicProfile? = null,
    val error: String? = null
)

class PublicProfileViewModel(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    fun loadPublicProfile(profileId: String) {
        if (profileId.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    profile = null,
                    error = PublicProfileErrors.EMPTY_PROFILE_ID)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val userProfile = userProfileRepository.getUserProfile(profileId)

                // Format the date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val formattedDate = try {
                    dateFormat.format(userProfile.arrivalDate)
                } catch (e: Exception) {
                    null
                }

                // Combine name and lastName
                val fullName = if (userProfile.lastName.isBlank()) {
                    userProfile.name
                } else {
                    "${userProfile.name} ${userProfile.lastName}"
                }

                // Get section label
                val sectionLabel = try {
                    UserSections.entries.firstOrNull {
                        it.name.equals(userProfile.section.toString(), ignoreCase = true)
                    }?.label ?: userProfile.section.toString()
                } catch (e: Exception) {
                    "None"
                }

                val publicProfile = PublicProfile(
                    userId = userProfile.id,
                    email = userProfile.email ?: "",
                    name = fullName,
                    section = sectionLabel,
                    arrivalDate = formattedDate,
                    pictureUriString = userProfile.photo?.toString(),
                    kudosReceived = userProfile.kudos,
                    helpReceived = userProfile.helpReceived,
                    followers = userProfile.followers,
                    following = userProfile.following    )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = publicProfile,
                    error = null)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = null,
                    error = "Failed to load profile: ${e.message}")
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun clear() {
        _uiState.value = PublicProfileUiState()
    }
}

class PublicProfileViewModelFactory(
    private val userProfileRepository: UserProfileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PublicProfileViewModel::class.java)) {
            return PublicProfileViewModel(userProfileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}