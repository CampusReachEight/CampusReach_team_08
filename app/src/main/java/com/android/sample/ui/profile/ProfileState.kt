package com.android.sample.ui.profile

data class ProfileState(
    // Header section
    val userName: String = "",
    val userEmail: String = "",
    val profileId: String = "",

    // Stats cards
    val kudosReceived: Int = 0,
    val helpReceived: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,

    // Information section
    val arrivalDate: String = "00/00/0000",
    val section: String = "None",

    // UI States
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false, // If you plan to add edit functionality later

    // Profile picture (optional, if you add it later)
    val profilePictureUrl: String? = null,

    // Logout dialog state
    val isLoggingOut: Boolean = false
) {

  companion object {
    // Default empty state
    fun empty() = ProfileState()

    // Default state with mock data
    fun default() =
        ProfileState(
            userName = "John Doe",
            userEmail = "john.doe@example.com",
            profileId = "123456",
            kudosReceived = 10,
            helpReceived = 5,
            followers = 3,
            following = 7,
            arrivalDate = "01/09/2025",
            section = "Computer Science")

    // State with loading state
    fun loading() = default().copy(isLoading = true)

    // State with error
    fun withError() = default().copy(errorMessage = "Failed to load profile data")

    fun loggingOut() = default().copy(isLoggingOut = true)
  }
}
