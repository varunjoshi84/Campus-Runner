package com.example.campusrunner.data.remote

data class GoogleLoginRequest(val idToken: String)
data class DevLoginRequest(val name: String?, val email: String)
data class SignupRequest(val name: String, val email: String, val password: String, val phone: String? = null)
data class CustomLoginRequest(val email: String, val password: String)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class RunDto(
    val id: String,
    val runnerId: String?,
    val storeName: String,
    val lat: Double,
    val lng: Double,
    val expiresAt: String,
    val runnerName: String?
)

data class CreateRunRequest(
    val storeName: String,
    val lat: Double,
    val lng: Double
)

data class RequestDto(
    val id: String,
    val runId: String,
    val requesterId: String?,
    val item: String,
    val tip: Int,
    val status: String, // OPEN, ACCEPTED, BOUGHT, DELIVERED, CANCELLED
    val createdAt: String,
    val storeName: String?,
    val requesterName: String?,
    val requesterPhone: String?,
    val runnerName: String?,
    val runnerPhone: String?
)

data class CreateRequestRequest(
    val runId: String,
    val item: String,
    val tip: Int
)

data class UpdateStatusRequest(
    val status: String
)

data class UpdatePhoneRequest(
    val phone: String
)
