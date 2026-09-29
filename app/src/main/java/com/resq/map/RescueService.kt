package com.resq.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class RescueServiceCategory(val label: String, val layerId: String, val kinds: Set<String>) {
    MEDICAL("Hospital / Clinic", "poi-medical", setOf("hospital", "clinic", "doctors", "dentist")),
    FIRE("Fire Station", "poi-fire", setOf("fire_station")),
    POLICE("Police Station", "poi-police", setOf("police")),
    PHARMACY("Pharmacy", "poi-supplies", setOf("pharmacy")),
    SUPPLIES("Fuel / Water", "poi-supplies", setOf("fuel", "drinking_water", "water_point")),
    SHELTER(
        "Shelter / Support",
        "poi-support",
        setOf("shelter", "social_facility", "community_centre", "ranger_station", "emergency_phone", "ambulance_station", "helipad")
    );

    companion object {
        fun fromKind(kind: String): RescueServiceCategory? = entries.firstOrNull { kind in it.kinds }
    }
}

data class NearbyRescueService(
    val name: String,
    val kind: String,
    val category: RescueServiceCategory,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double?
)

object RescueDistance {
    fun meters(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Double {
        val radius = 6_371_000.0
        val dLat = Math.toRadians(toLat - fromLat)
        val dLng = Math.toRadians(toLng - fromLng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) *
            sin(dLng / 2) * sin(dLng / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
