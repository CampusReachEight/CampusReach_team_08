package com.android.sample.ui.profile.setup

sealed class SetupProfileEvent {
  object Load : SetupProfileEvent()

  object Save : SetupProfileEvent()
}
