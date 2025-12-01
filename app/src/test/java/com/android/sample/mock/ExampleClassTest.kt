package com.android.sample.mock

import kotlin.test.assertEquals
import org.junit.Test

class ExampleClassTest {

  companion object {
    private const val NUMBER_ONE = 1
  }

  @Test
  fun simpleExampleTest() {
    val exampleClass = ExampleClass(true, NUMBER_ONE, "1")
    assertEquals(NUMBER_ONE, exampleClass.exampleFun())
  }
}
