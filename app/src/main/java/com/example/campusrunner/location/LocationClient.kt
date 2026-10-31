package com.example.campusrunner.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationClient(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult = suspendCancellableCoroutine { continuation ->
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
                                continuation.resume(LocationResult.Success(37.421999, -122.084000))
                            }
                        }.addOnFailureListener {
                            continuation.resume(LocationResult.Success(37.421999, -122.084000))
                        }
                    }
                }
                .addOnFailureListener {
                    continuation.resume(LocationResult.Success(37.421999, -122.084000))
                }
        } catch (e: Exception) {
            continuation.resume(LocationResult.Success(37.421999, -122.084000))
        }
    }
}

sealed class LocationResult {
    data class Success(val lat: Double, val lng: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
}
