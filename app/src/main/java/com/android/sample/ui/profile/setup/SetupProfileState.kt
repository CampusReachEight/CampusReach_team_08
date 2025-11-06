package com.android.sample.ui.profile.setup

data class SetupProfileState(
    val userName: String = "",
    val userEmail: String = "",
    val profileId: String = "",

    val kudosReceived: Int = 0,
    val helpReceived: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,

    val arrivalDate: String = "00/00/0000",
    val section: String = "None",

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,

    val profilePictureUrl: String? = null,

    // Setup-specific states
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false
) {
    companion object {
        fun empty() = SetupProfileState()
        fun default() =
            SetupProfileState(
                userName = "John Doe",
                userEmail = "john.doe@example.com",
                profileId = "123456",
                kudosReceived = 0,
                helpReceived = 0,
                followers = 0,
                following = 0,
                arrivalDate = "01/09/2025",
                section = "Computer Science"
            )
    }
}