package com.android.sample.model.profile

import android.graphics.Bitmap
import java.sql.Date

data class UserProfile(
    val id: String,
    val name: String,
    val lastName: String,
    val email: String,
    val photo: Bitmap,
    val kudos: Int,
    val section: Section,
    val arrivalDate: java.util.Date
)

enum class Section {
  INFORMATION_SYSTEMS,
  COMPUTER_SCIENCE,
  SOFTWARE_ENGINEERING,
  CYBER_SECURITY,
  MECHANICAL_ENGINEERING,
  ELECTRICAL_ENGINEERING,
  INDUSTRIAL_ENGINEERING,
  LITTERATURE,
  LIFE_SCIENCES,
  PHILOSOPHY,
  ECONOMICS,
  LAW,
  // ton add some stuff
  OTHER
}
