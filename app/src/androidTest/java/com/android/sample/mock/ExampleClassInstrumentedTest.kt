package com.android.sample.mock

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for ExampleClass.
 *
 * This test runs on an Android device/emulator and tests the same ExampleClass as the unit test,
 * but exercises different code paths to verify coverage from instrumented tests.
 */
@RunWith(AndroidJUnit4::class)
class ExampleClassInstrumentedTest {
  companion object {
    private const val NUMBER_ONE = 1
  }

  @Test
  fun testExampleFunWithTrueCondition() {
    // Test the true branch (returns intExample)
    val exampleClass = ExampleClass(true, NUMBER_ONE, "unused")
    assertEquals(NUMBER_ONE, exampleClass.exampleFun())
  }
}
