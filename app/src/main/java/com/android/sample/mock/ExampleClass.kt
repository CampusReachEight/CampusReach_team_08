package com.android.sample.mock

/** Mock data class to test Jacoco coverage */
data class ExampleClass(
    private val boolExample: Boolean,
    private val intExample: Int,
    private val stringExample: String
) {

  fun exampleFun(): Any {
    return if (boolExample) {
      intExample
    } else {
      stringExample
    }
  }
}
