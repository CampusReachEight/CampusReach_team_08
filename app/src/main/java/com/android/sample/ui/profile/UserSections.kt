package com.android.sample.ui.profile

enum class UserSections(val label: String) {
  ARCHITECTURE("Architecture"),
  CHEMISTRY_AND_CHEMICAL_ENGINEERING("Chemistry and Chemical Engineering"),
  CIVIL_ENGINEERING("Civil Engineering"),
  COMMUNICATION_SCIENCE("Communication Science"),
  COMPUTER_SCIENCE("Computer Science"),
  DIGITAL_HUMANITIES("Digital Humanities"),
  ELECTRICAL_ENGINEERING("Electrical Engineering"),
  ENVIRONMENTAL_SCIENCES_AND_ENGINEERING("Environmental Sciences and Engineering"),
  FINANCIAL_ENGINEERING("Financial Engineering"),
  LIFE_SCIENCES_ENGINEERING("Life Sciences Engineering"),
  MANAGEMENT_OF_TECHNOLOGY("Management of Technology"),
  MATERIALS_SCIENCE_AND_ENGINEERING("Materials Science and Engineering"),
  MATHEMATICS("Mathematics"),
  MECHANICAL_ENGINEERING("Mechanical Engineering"),
  MICROENGINEERING("Microengineering"),
  NEURO_X("Neuro-X"),
  PHYSICS("Physics"),
  QUANTUM_SCIENCE_AND_ENGINEERING("Quantum Science and Engineering");

  companion object {
    fun labels(): List<String> {
      return values().map { it.label }
    }

    fun fromLabel(label: String): UserSections? {
      return values().find { it.label == label }
    }
  }
}
