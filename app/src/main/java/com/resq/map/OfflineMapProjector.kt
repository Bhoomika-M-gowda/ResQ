package com.resq.map

import kotlin.math.cos

data class MapPoint(val x: Float, val y: Float)

/** Converts nearby GPS coordinates to a stable 0..1 offline-map position. */
object OfflineMapProjector {
    fun project(
        latitude: Double,
        longitude: Double,
        centerLatitude: Double,
        centerLongitude: Double,
        spanKm: Double = 3.0
    ): MapPoint {
        val latKm = (latitude - centerLatitude) * 111.32
        val lngKm = (longitude - centerLongitude) * 111.32 * cos(Math.toRadians(centerLatitude))
        return MapPoint(
            x = (0.5 + lngKm / spanKm).coerceIn(0.04, 0.96).toFloat(),
            y = (0.5 - latKm / spanKm).coerceIn(0.04, 0.96).toFloat()
        )
    }
}
