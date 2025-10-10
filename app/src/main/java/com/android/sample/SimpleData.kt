package com.android.sample

import kotlin.math.sqrt

data class Point(val x: Double, val y: Double) {

  fun distanceTo(p: Point): Double {
    val dx = x - p.x
    val dy = y - p.y
    return sqrt(dx * dx + dy * dy)
  }

  fun coverageTest(): Double {
    var c: Double = 0.0
    c =
        if (x > y) {
          x - y
        } else {
          y - x
        }
    return if (c < 5) {
      c
    } else {
      -1.0
    }
  }
}
