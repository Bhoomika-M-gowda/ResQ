package com.resq.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RescueDistanceTest {
    @Test
    fun sameCoordinateIsZero() {
        assertEquals(0.0, RescueDistance.meters(12.9716, 77.5946, 12.9716, 77.5946), 0.01)
    }

    @Test
    fun calculatesKnownApproximateDistance() {
        val distance = RescueDistance.meters(12.9716, 77.5946, 12.9816, 77.5946)
        assertTrue(distance in 1_100.0..1_125.0)
    }
}
