package com.example.campusrunner.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationResult
}

open class LocationClient(context: Context) : LocationProvider {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(LocationResult.Success(location.latitude, location.longitude))
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                continuation.resume(LocationResult.Success(lastLoc.latitude, lastLoc.longitude))
                            } else {
                                // Default fallback coordinates (e.g., campus center)
                                continuation.resume(LocationResult.Success(31.256, 75.705))
                            }
                        }.addOnFailureListener {
                            continuation.resume(LocationResult.Success(31.256, 75.705))
                        }
                    }
                }
                .addOnFailureListener {
                    continuation.resume(LocationResult.Success(31.256, 75.705))
                }
        } catch (e: Exception) {
            continuation.resume(LocationResult.Success(31.256, 75.705))
        }
    }
}

sealed class LocationResult {
    data class Success(val lat: Double, val lng: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
}
