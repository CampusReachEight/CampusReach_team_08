package com.android.sample.mock

import kotlin.test.DefaultAsserter.assertEquals
import org.junit.Test

class ExampleClassTest {

  @Test
  fun simpleExampleTest() {
    val exampleClass = ExampleClass(true, 1, "1")
    assertEquals("Simple test", exampleClass.exampleFun(), 1)
  }
}
