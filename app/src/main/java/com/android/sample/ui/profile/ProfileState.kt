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
    val leaderboardPosition: Int? = null,

    // Information section
    val arrivalDate: String = "00/00/0000",
    val userSection: String = "None",

    // UI States
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false, // If you plan to add edit functionality later
    val offlineMode: Boolean = false,

    // Profile picture (optional, if you add it later)
    val profilePictureUrl: String? = null,

    // Logout dialog state
    val isLoggingOut: Boolean = false
) {

  companion object {
    // Default empty state
    fun empty() = ProfileState()

    // Default state with mock data
    fun default() = empty()

    // State with loading state
    fun loading() = default().copy(isLoading = true)

    // State with error
    fun withError() = default().copy(errorMessage = "Failed to load profile data")

    fun loggingOut() = default().copy(isLoggingOut = true)
  }
}
