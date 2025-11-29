package com.android.sample

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  companion object {
    // Set this to true to fail the test
    const val TEST_FAIL = true
  }

  @Test
  fun addition_isCorrect() {
    assertTrue(TEST_FAIL.not())
  }
}
