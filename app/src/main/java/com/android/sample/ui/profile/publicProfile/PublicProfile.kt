package com.android.sample.ui.profile.publicProfile

data class PublicProfile (
    val userId: String,
    val name: String,
    val section: String,
    val arrivalDate: String?, // formatted date or null
    val pictureUriString: String?,
    val kudosReceived: Int,
    val helpReceived: Int,
    val followers: Int,
    val following: Int

)
