package com.resq.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapProjectorTest {
    @Test fun centerProjectsToMiddle() {
        val point = OfflineMapProjector.project(12.97, 77.59, 12.97, 77.59)
        assertEquals(.5f, point.x, .001f)
        assertEquals(.5f, point.y, .001f)
    }

    @Test fun northEastProjectsUpAndRight() {
        val point = OfflineMapProjector.project(12.975, 77.595, 12.97, 77.59)
        assertTrue(point.x > .5f)
        assertTrue(point.y < .5f)
    }

    @Test fun distantMarkersStayOnMap() {
        val point = OfflineMapProjector.project(50.0, 120.0, 12.97, 77.59)
        assertTrue(point.x in 0.04f..0.96f)
        assertTrue(point.y in 0.04f..0.96f)
    }
}
