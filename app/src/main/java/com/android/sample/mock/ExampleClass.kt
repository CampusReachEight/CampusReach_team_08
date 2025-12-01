package com.android.sample.mock

/** Mock data class to test Jacoco coverage */
data class ExampleClass(
    private val boolExample: Boolean,
    private val intExample: Int,
    private val stringExample: String
) {

  fun functionExample(): Any {
    return if (boolExample) {
      stringExample
    } else {
      intExample
    }
  }
}
