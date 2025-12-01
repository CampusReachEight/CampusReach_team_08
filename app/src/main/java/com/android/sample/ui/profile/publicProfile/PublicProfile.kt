package com.android.sample.ui.profile.publicProfile

/**
 * A lightweight, immutable representation of a user's public profile as shown in the UI.
 *
 * This model contains only the data required by the public profile screen and is safe to pass
 * between layers (ViewModel \-> UI) and to use in tests.
 *
 * @property userId Unique identifier of the user. Expected to be non-empty when representing a real
 *   user.
 * @property name Display name shown in the UI.
 * @property section Short subtitle/role or section text shown under the name.
 * @property arrivalDate Optional arrival date formatted for display (e.g. `"Jan 1, 2020"`). Use
 *   `null` when no date is available.
 * @property pictureUriString Optional stringified URI for the user's avatar. If present, parse with
 *   `android.net.Uri.parse(pictureUriString)` before loading into an image component.
 * @property kudosReceived Number of kudos the user has received (non-negative).
 * @property helpReceived Number of help acknowledgements the user has received (non-negative).
 * @property followers Number of followers (non-negative).
 * @property following Number of accounts the user is following (non-negative).
 */
data class PublicProfile(
    val userId: String,
    val email: String,
    val name: String,
    val section: String,
    val arrivalDate: String?, // formatted date or null
    val pictureUriString: String?,
    val kudosReceived: Int,
    val helpReceived: Int,
    val followers: Int,
    val following: Int
)
