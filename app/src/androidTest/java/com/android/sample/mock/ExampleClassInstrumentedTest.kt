package com.android.sample.mock

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for ExampleClass to verify coverage tracking works for integration tests. This
 * test should show up in Jacoco coverage reports when createDebugCoverageReport runs.
 */
@RunWith(AndroidJUnit4::class)
class ExampleClassInstrumentedTest {

  @Test
  fun testExampleFunWithTrueBranch() {
    // Test the true branch (returns int)
    val exampleClass = ExampleClass(boolExample = true, intExample = 42, stringExample = "test")
    val result = exampleClass.exampleFun()
    assertEquals(42, result)
  }
}
