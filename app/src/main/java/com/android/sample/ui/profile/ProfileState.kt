package com.android.sample.ui.profile

data class ProfileState(
    // Header section
    val userName: String = "John Doe",
    val userEmail: String = "john.doe@epfl.ch",
    val profileId: String = "000000",

    // Stats cards
    val kudosReceived: Int = 0,
    val helpReceived: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,

    // Information section
    val arrivalDate: String = "01/01/2025",
    val section: String = "None",

    // Actions section (for expandable states)
    val isLogoutExpanded: Boolean = false,
    val isAboutAppExpanded: Boolean = false,

    // UI States
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false, // If you plan to add edit functionality later

    // Profile picture (optional, if you add it later)
    val profilePictureUrl: String? = null
)

sealed class ProfileEvent {
    // Data loading
    data object LoadProfile : ProfileEvent()

    // UI interactions
    data object ToggleLogoutExpand : ProfileEvent()
    data object ToggleAboutAppExpand : ProfileEvent()
    data object ToggleEditMode : ProfileEvent()

    // Data updates (if editable)
    data class UpdateName(val newName: String) : ProfileEvent()
    data class UpdateEmail(val newEmail: String) : ProfileEvent()
    // Add other update events as needed
}
