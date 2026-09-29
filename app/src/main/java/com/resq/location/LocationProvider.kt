package com.resq.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val capturedAt: Long = System.currentTimeMillis(),
    val fromLastKnown: Boolean = false
) {
    val isStale: Boolean
        get() = fromLastKnown || System.currentTimeMillis() - capturedAt > STALE_AFTER_MS

    companion object {
        private const val STALE_AFTER_MS = 2 * 60 * 1000L
    }
}

class LocationProvider(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Result<LocationFix> {
        if (!hasPermission()) return Result.failure(SecurityException("Location permission is required"))
        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .addOnSuccessListener { location ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    if (location != null) {
                        continuation.resume(Result.success(location.toFix(fromLastKnown = false)))
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (!continuation.isActive) return@addOnSuccessListener
                                if (last != null) continuation.resume(Result.success(last.toFix(fromLastKnown = true)))
                                else continuation.resume(Result.failure(IllegalStateException("Location unavailable. Enable GPS and retry.")))
                            }
                            .addOnFailureListener { error ->
                                if (continuation.isActive) continuation.resume(Result.failure(error))
                            }
                    }
                }
                .addOnFailureListener { error ->
                    if (!continuation.isActive) return@addOnFailureListener
                    client.lastLocation
                        .addOnSuccessListener { last ->
                            if (!continuation.isActive) return@addOnSuccessListener
                            if (last != null) continuation.resume(Result.success(last.toFix(fromLastKnown = true)))
                            else continuation.resume(Result.failure(error))
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(Result.failure(error))
                        }
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationFix> = callbackFlow {
        if (!hasPermission()) {
            close(SecurityException("Location permission is required"))
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15_000L)
            .setMinUpdateIntervalMillis(7_500L)
            .setMinUpdateDistanceMeters(10f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toFix(fromLastKnown = false)) }
            }
        }
        client.lastLocation.addOnSuccessListener { last ->
            last?.let { trySend(it.toFix(fromLastKnown = true)) }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun android.location.Location.toFix(fromLastKnown: Boolean) = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        capturedAt = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        fromLastKnown = fromLastKnown
    )
}
